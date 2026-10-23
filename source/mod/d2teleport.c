













#define CMD_TELEPORT 27
#define PKT_TELEPORT 11
#define TELE_STEPS 48
#define TELE_STALE_MS 1000
#define TELE_OK 0
#define TELE_CORRECTED 1
#define TELE_NONE 2







#define TELE_ALLOWED (0x0010 | 0x8000)
static int teleWanted, teleActive;
static int teleDx, teleDy, teleDist;
static DWORD teleAskedAt, teleChecks, teleMicrosMax, teleMicrosSum;
static long teleAnsweredX = -1, teleAnsweredY = -1;
static long teleAnsweredSx, teleAnsweredSy;
static int teleAnsweredFound;





static int teleBusy, teleAnsweredBusy = -1;
static BYTE teleBusyStarts;


static long teleSeenX = -1, teleSeenY = -1;

static long teleTraced[6] = {-1, -1, -1, -1, -1, -1};
static DWORD teleTracedAt;


static void teleportCommand(const BYTE* buf, int n) {
    if (n < 12) return;
    teleDx = (signed char) buf[8];
    teleDy = (signed char) buf[9];
    teleDist = buf[10] | buf[11] << 8;
    teleWanted = 1;
    teleActive = 1;
    teleAskedAt = GetTickCount();
}


static void* teleportRoom(void* level, long x, long y) {
    int i = 0;
    for (void* r2 = RDP(level, LEVEL_ROOM2); readable(r2, 0x60) && i < 4096; r2 = RDP(r2, ROOM2_NEXT), i++) {
        long tx = (long) RD32(r2, ROOM2_X) * 5, ty = (long) RD32(r2, ROOM2_Y) * 5;
        long tw = (long) RD32(r2, ROOM2_W) * 5, th = (long) RD32(r2, ROOM2_H) * 5;
        if (x < tx || y < ty || x >= tx + tw || y >= ty + th) continue;
        void* r1 = RDP(r2, ROOM2_ROOM1);
        return gridReadable(r1) ? r1 : NULL;
    }
    return NULL;
}



static WORD teleportFlags(void* level, long x, long y) {
    void* r1 = teleportRoom(level, x, y);
    if (!r1) return 0xFFFF;
    BYTE* grid = RDP(r1, ROOM1_COLL);
    long rx = (long) RD32(r1, ROOM1_SUBX), ry = (long) RD32(r1, ROOM1_SUBX + 4);
    long w = (long) RD32(r1, ROOM1_SUBX + 8), h = (long) RD32(r1, ROOM1_SUBX + 12);
    WORD* flags = (WORD*) RDP(grid, 0x20);
    long stride = (long) RD32(grid, 8);
    if (!flags || x < rx || y < ry || x - rx >= w || y - ry >= h) return 0xFFFF;
    return flags[(y - ry) * stride + (x - rx)];
}

static int teleportFree(void* level, long x, long y) {
    WORD f = teleportFlags(level, x, y);
    return f != 0xFFFF && !(f & (WORD) ~TELE_ALLOWED);
}






static int teleportFits(int (*isFree)(void*, long, long), void* ctx, long x, long y) {
    return isFree(ctx, x, y) && isFree(ctx, x - 1, y) && isFree(ctx, x + 1, y) && isFree(ctx, x, y - 1) && isFree(ctx, x, y + 1);
}






static int teleportSearch(double px, double py, double wx, double wy, int steps, int (*isFree)(void*, long, long), void* ctx,
                          long* tx, long* ty) {
    for (int k = steps; k >= 1; k--) {
        long x = (long) (px + wx * k / steps), y = (long) (py + wy * k / steps);
        if (teleportFits(isFree, ctx, x, y)) {
            *tx = x;
            *ty = y;
            return k == steps ? TELE_OK : TELE_CORRECTED;
        }
    }
    return TELE_NONE;
}

static int teleportFreeIn(void* level, long x, long y) {
    return teleportFree(level, x, y);
}


static void teleportTick(void* player) {
    if (!teleActive) return;
    if (GetTickCount() - teleAskedAt > TELE_STALE_MS) {
        teleActive = teleWanted = 0;
        teleAnsweredX = teleAnsweredY = -1;
        teleAnsweredBusy = -1;
        teleSeenX = teleSeenY = -1;
        teleAnsweredFound = 0;
        return;
    }
    if (!player || pauseApplied || !singlePlayerGame()) return;
    DWORD mode = RD32(player, U_MODE);
    int busy = mode != 1 && mode != 5;
    if (busy && !teleBusy) teleBusyStarts++;
    teleBusy = busy;
    double px, py;
    unitPosition(player, &px, &py);
    long pxs = (long) px, pys = (long) py;
    int moved = pxs != teleSeenX || pys != teleSeenY;
    teleSeenX = pxs;
    teleSeenY = pys;
    if (moved) return;
    if (!teleWanted && pxs == teleAnsweredX && pys == teleAnsweredY && busy == teleAnsweredBusy) return;
    teleWanted = 0;
    DWORD levelNo = 0;
    void* level = playerLevel(player, &levelNo);
    if (!level) return;
    LARGE_INTEGER freq, t0, t1;
    QueryPerformanceFrequency(&freq);
    QueryPerformanceCounter(&t0);
    double len = __builtin_sqrt((double) teleDx * teleDx + (double) teleDy * teleDy);
    if (len < 1) return;

    double sx = teleDx / len * teleDist, sy = teleDy / len * teleDist;
    double wx = sy / 16 + sx / 32, wy = sy / 16 - sx / 32;
    int steps = (int) (2 * __builtin_sqrt(wx * wx + wy * wy) + 0.5);
    if (steps > TELE_STEPS) {
        wx *= (double) TELE_STEPS / steps;
        wy *= (double) TELE_STEPS / steps;
        steps = TELE_STEPS;
    }
    if (steps < 1) steps = 1;
    long wantX = (long) (px + wx), wantY = (long) (py + wy);
    WORD wantFlags = teleportFlags(level, wantX, wantY);
    long tx = 0, ty = 0;
    int status = teleportSearch(px, py, wx, wy, steps, teleportFreeIn, level, &tx, &ty);
    int found = status != TELE_NONE;
    View view;
    view.offX = (long) fGetMouseXOffset();
    view.offY = (long) fGetMouseYOffset();
    int ssx = 0, ssy = 0;
    if (found) toScreen(&view, tx + 0.5, ty + 0.5, &ssx, &ssy);
    teleAnsweredFound = found;
    teleAnsweredSx = ssx;
    teleAnsweredSy = ssy;
    QueryPerformanceCounter(&t1);
    DWORD micros = (DWORD) ((t1.QuadPart - t0.QuadPart) * 1000000 / freq.QuadPart);
    teleChecks++;
    teleMicrosSum += micros;
    if (micros > teleMicrosMax) teleMicrosMax = micros;
    if (teleChecks % 500 == 0) {
        logLine("teleport: %lu checks, last %lu us, max %lu us, mean %lu us", (unsigned long) teleChecks,
            (unsigned long) micros, (unsigned long) teleMicrosMax, (unsigned long) (teleMicrosSum / teleChecks));
    }
    long now6[6] = {status, tx, ty, pxs, pys, (teleDx * 256 + teleDy) * 2 + busy};
    if (__builtin_memcmp(now6, teleTraced, sizeof(now6)) || GetTickCount() - teleTracedAt >= 1000) {
        trace("teleport: dir %d,%d dist %d -> status %d at %ld,%ld screen %d,%d (%lu us) from %ld,%ld want %ld,%ld flags 0x%04x "
            "mode %lu busy %d starts %u", teleDx, teleDy, teleDist, status, tx, ty, ssx, ssy, (unsigned long) micros, pxs, pys,
            wantX, wantY, wantFlags, (unsigned long) mode, busy, (unsigned) teleBusyStarts);
        __builtin_memcpy(teleTraced, now6, sizeof(now6));
        teleTracedAt = GetTickCount();
    }
    teleAnsweredX = pxs;
    teleAnsweredY = pys;
    teleAnsweredBusy = busy;
    Packet p;
    putHeader(&p, PKT_TELEPORT);
    putU8(&p, status);
    putU16(&p, (WORD) (short) ssx);
    putU16(&p, (WORD) (short) ssy);
    putU16(&p, (WORD) tx);
    putU16(&p, (WORD) ty);
    putU16(&p, (WORD) pxs);
    putU16(&p, (WORD) pys);
    putU16(&p, (WORD) (micros > 0xFFFF ? 0xFFFF : micros));
    putU8(&p, busy);
    putU8(&p, teleBusyStarts);
    sendPacket(&p);
}

#include "d2telehover.c"
