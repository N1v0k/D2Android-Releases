#define CMD_RESUME           25
#define RESUME_STEP          35
#define RESUME_STEP_MIN      9
#define RESUME_CHAIN_MS      5000
#define RESUME_CHAIN_TICKS   125
#define RESUME_CHAIN_MAX_MS  12000
#define RESUME_ARRIVE_MS     3000
#define RESUME_ARRIVE_TICKS  75
#define RESUME_ARRIVE_MAX_MS 8000
#define RESUME_HOP_SEEN_MS   600
#define RESUME_HOP_SEEN_TICKS 15
#define RESUME_HOP_SEEN_MAX_MS 3000
#define RESUME_GUARD_MS      5000
#define RESUME_GUARD_MAX_MS  15000
#define RESUME_OFF_REPEAT_MS 200
#define RESUME_MAX_FRAME     1500

#define RS_NONE     0
#define RS_RUNNING  1
#define RS_SPOT     2
#define RS_WAYPOINT 3
#define RS_REFUSED  4
#define RR_NONE        0
#define RR_MULTIPLAYER 1
#define RR_NOT_READY   2
#define RR_DIFFICULTY  3
#define RR_SEED        4
#define RR_LEVEL       5
#define RR_FUNCTIONS   6
#define RR_WARP        7
#define RR_ROOM        8
#define RR_WALL        9
#define RR_JUMP        10
#define RR_TIME        11
#define RR_CANCELLED   12
#define RR_LATE        13

static struct {
    int phase;
    BYTE state, reason;
    int usedGame;
    WORD level, x, y;
    DWORD seed;
    WORD rx, ry, rw, rh;
    BYTE difficulty;
    LONG seq;
    LONG dropSeq[4];
    DWORD started, phaseAt;
    DWORD ticks, phaseTicks;
    int rejump;
    int waitLoad;
    DWORD loadAt, loadTicks;
    int step;
    long hopX, hopY;
    int backTries;
    int arrived;
    int guardOn;
    LONG onSeq, offSeq;
    DWORD guardUntil, guardHard, offAskedAt;
    DWORD lastTick;
    int offWanted;
} rsm;
static void* resumePlayer;
static DWORD resumePlayerId;
static int resumeDifficulty = -1;
static LONG resumeDiffSeq;
static DWORD resumeGames;

static void resumeLog(const char* what) {
    logLine("resume: %s", what);
}

static int resumeWaitOver(DWORD since, DWORD ticks, int ms, DWORD minTicks, int maxMs, DWORD now) {
    int elapsed = (int) (now - since);
    return (elapsed > ms && ticks > minTicks) || elapsed > maxMs;
}

static void resumeBack(BYTE reason, const char* why, DWORD now) {
    rsm.reason = reason;
    rsm.phase = 4;
    logLine("resume: %s after %lu ms / %lu ticks (phase %lu ms / %lu ticks): back to the waypoint", why,
            (unsigned long) (now - rsm.started), (unsigned long) rsm.ticks, (unsigned long) (now - rsm.phaseAt),
            (unsigned long) rsm.phaseTicks);
}

static void resumeDrop(LONG* seq) {
    if (*seq && !srvCancel(*seq)) {
        for (int i = 0; i < 4; i++) if (!rsm.dropSeq[i]) { rsm.dropSeq[i] = *seq; break; }
    }
    *seq = 0;
}

static void resumeEnd(BYTE state, BYTE reason, const char* what) {
    rsm.phase = 0;
    rsm.state = state;
    rsm.reason = reason;
    resumeDrop(&rsm.seq);
    if (!rsm.arrived && rsm.guardOn) {
        rsm.offWanted = 1;
        rsm.offAskedAt = 0;
    }
    logLine("resume: %s – state %d, reason %d", what, state, reason);
}

static int resumeHeroAt(void* player, long* x, long* y) {
    void* path = player ? RDP(player, U_PATH) : NULL;
    if (!readable(path, PATH_Y + 4)) return 0;
    *x = (long) (RD32(path, PATH_X) >> 16);
    *y = (long) (RD32(path, PATH_Y) >> 16);
    return *x > 0 && *y > 0;
}

static DWORD resumeSeed(void* player) {
    void* act = player ? RDP(player, U_ACT) : NULL;
    return readable(act, ACT_SEED + 4) ? RD32(act, ACT_SEED) : 0;
}

static int resumeHeroRoom(void* player, WORD* rx, WORD* ry, WORD* rw, WORD* rh) {
    void* path = player ? RDP(player, U_PATH) : NULL;
    void* room1 = path ? RDP(path, PATH_ROOM1) : NULL;
    void* room2 = readable(room1, ROOM1_ROOM2 + 4) ? RDP(room1, ROOM1_ROOM2) : NULL;
    if (!readable(room2, ROOM2_H + 4)) return 0;
    *rx = (WORD) (RD32(room2, ROOM2_X) * 5);
    *ry = (WORD) (RD32(room2, ROOM2_Y) * 5);
    *rw = (WORD) (RD32(room2, ROOM2_W) * 5);
    *rh = (WORD) (RD32(room2, ROOM2_H) * 5);
    return 1;
}

static int resumeRoomSame(void* player) {
    DWORD levelNo;
    void* level = playerLevel(player, &levelNo);
    if (!level || levelNo != rsm.level) return 0;
    int rooms = 0;
    for (void* room2 = RDP(level, LEVEL_ROOM2); readable(room2, 0x60) && rooms < 2048; room2 = RDP(room2, ROOM2_NEXT), rooms++) {
        long x = (long) RD32(room2, ROOM2_X) * 5, y = (long) RD32(room2, ROOM2_Y) * 5;
        long w = (long) RD32(room2, ROOM2_W) * 5, h = (long) RD32(room2, ROOM2_H) * 5;
        if (rsm.x < x || rsm.y < y || rsm.x >= x + w || rsm.y >= y + h) continue;
        return x == rsm.rx && y == rsm.ry && w == rsm.rw && h == rsm.rh;
    }
    return 0;
}

static void* resumeRoom1At(void* player, long x, long y) {
    void* path = player ? RDP(player, U_PATH) : NULL;
    void* room1 = path ? RDP(path, PATH_ROOM1) : NULL;
    void* room2 = readable(room1, ROOM1_ROOM2 + 4) ? RDP(room1, ROOM1_ROOM2) : NULL;
    if (!readable(room2, 0x60)) return NULL;
    void** around = (void**) RDP(room2, ROOM2_NEAR);
    DWORD count = RD32(room2, ROOM2_NNEAR);
    for (DWORD i = 0; i <= count && i < 64; i++) {
        void* r2 = i < count ? (readable(&around[i], 4) ? around[i] : NULL) : room2;
        void* r1 = readable(r2, ROOM2_ROOM1 + 4) ? RDP(r2, ROOM2_ROOM1) : NULL;
        if (!r1 || !gridReadable(r1)) continue;
        long rx = (long) RD32(r1, ROOM1_SUBX), ry = (long) RD32(r1, ROOM1_SUBX + 4);
        long w = (long) RD32(r1, ROOM1_SUBX + 8), h = (long) RD32(r1, ROOM1_SUBX + 12);
        if (x >= rx && y >= ry && x < rx + w && y < ry + h) return r1;
    }
    return NULL;
}

static int resumeWalkable(void* player, long x, long y) {
    int loaded = 1;
    for (int dy = -1; dy <= 1; dy++)
        for (int dx = -1; dx <= 1; dx++) {
            void* r1 = resumeRoom1At(player, x + dx, y + dy);
            if (!r1) loaded = 0;
            else if (!walkable(r1, x + dx, y + dy)) return 0;
        }
    return loaded ? 1 : -1;
}

static const int UI_PANELS[] = {0x01, 0x02, 0x03, 0x04, 0x05, 0x08, 0x09, 0x0B, 0x0C, 0x0E, 0x0F, 0x14, 0x16, 0x17, 0x18, 0x19,
    0x1A, 0x21, 0x24, 0x25};

static int resumeCalm(void* player) {
    DWORD mode = RD32(player, U_MODE);
    if (mode == 0 || mode == 17 || !isTown(unitLevel(player)) || worldPaused() || equipCursorItem(player)) return 0;
    if (fGetUiVar) for (unsigned i = 0; i < sizeof(UI_PANELS) / sizeof(UI_PANELS[0]); i++) if (getUiVar(UI_PANELS[i])) return 0;
    return 1;
}

static void resumeGuardOn(DWORD now);

static void resumeCommand(void* player, const BYTE* buf, int n) {
    if (n >= 9 && buf[8] == 0) {
        if (rsm.phase) resumeEnd(rsm.phase >= 3 ? RS_WAYPOINT : RS_REFUSED, RR_CANCELLED, "cancelled by the app");
        return;
    }
    if (n < 28 || buf[8] != 1 || !player) return;
    if (rsm.phase) return;
    rsm.level = *(WORD*) (buf + 9);
    rsm.x = *(WORD*) (buf + 11);
    rsm.y = *(WORD*) (buf + 13);
    rsm.seed = *(DWORD*) (buf + 15);
    rsm.rx = *(WORD*) (buf + 19);
    rsm.ry = *(WORD*) (buf + 21);
    rsm.rw = *(WORD*) (buf + 23);
    rsm.rh = *(WORD*) (buf + 25);
    rsm.difficulty = buf[27];
    DWORD first, end, firstHere, endHere;
    if (rsm.usedGame) {
        logLine("resume: command 25 again in this game, ignored (state %d)", rsm.state);
        return;
    }
    rsm.usedGame = 1;
    rsm.arrived = 0;
    if (!singlePlayerGame()) { resumeEnd(RS_REFUSED, RR_MULTIPLAYER, "not single player"); return; }
    if (!resumeCalm(player)) { resumeEnd(RS_REFUSED, RR_NOT_READY, "hero not ready (dead, not in town, window, cursor, pause)"); return; }
    if (resumeDifficulty < 0 || resumeDifficulty != rsm.difficulty) { resumeEnd(RS_REFUSED, RR_DIFFICULTY, "difficulty unknown or not the saved one"); return; }
    if (!rsm.seed || resumeSeed(player) != rsm.seed) { resumeEnd(RS_REFUSED, RR_SEED, "another map (act seed)"); return; }
    if (!isWaypointLevel(rsm.level) || isTown(rsm.level) || !actOf(rsm.level, &first, &end)
        || !actOf(unitLevel(player), &firstHere, &endHere) || first != firstHere) {
        resumeEnd(RS_REFUSED, RR_LEVEL, "level without a waypoint of its own, a town or another act");
        return;
    }
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1 || !resumeFunctionsKnown()) { resumeEnd(RS_REFUSED, RR_FUNCTIONS, "server functions not as known"); return; }
    resumeGuardOn(GetTickCount());
    rsm.seq = srvSubmit(SRV_CODE_WARP, rsm.level, RESUME_MAX_FRAME);
    if (!rsm.seq) { resumeEnd(RS_REFUSED, RR_FUNCTIONS, "server queue full"); return; }
    rsm.phase = 1;
    rsm.state = RS_RUNNING;
    rsm.reason = RR_NONE;
    rsm.started = rsm.phaseAt = GetTickCount();
    rsm.ticks = rsm.phaseTicks = 0;
    rsm.rejump = rsm.waitLoad = 0;
    rsm.backTries = 0;
    logLine("resume: to level %u at %u,%u (room %u,%u %ux%u)", rsm.level, rsm.x, rsm.y, rsm.rx, rsm.ry, rsm.rw, rsm.rh);
}

static int resumeSaveGate(void) {
    return rsm.phase != 0 ? -1 : 0;
}


static void resumeGuardOn(DWORD now) {
    if (rsm.guardOn) return;
    rsm.guardOn = 1;
    rsm.offWanted = 0;
    rsm.guardUntil = now + RESUME_GUARD_MAX_MS;
    rsm.guardHard = now + RESUME_GUARD_MAX_MS;
    rsm.onSeq = srvSubmit(SRV_CODE_INVUL_ON, 0, 0);
}

static void resumeArrived(DWORD now, BYTE state, BYTE reason, const char* what) {
    resumeEnd(state, reason, what);
    if (state == RS_REFUSED && !rsm.arrived) return;
    if (!rsm.guardOn) resumeGuardOn(now);
    if (!rsm.offWanted) {
        rsm.guardUntil = now + RESUME_GUARD_MS;
        if ((int) (rsm.guardUntil - rsm.guardHard) > 0) rsm.guardHard = rsm.guardUntil;
    }
}

static void resumeJump(void* player, long x, long y, DWORD now) {
    double dx = rsm.x - x, dy = rsm.y - y, d = __builtin_sqrt(dx * dx + dy * dy);
    long nx = rsm.x, ny = rsm.y;
    if (d > rsm.step) {
        nx = x + (long) (dx * rsm.step / d + (dx >= 0 ? 0.5 : -0.5));
        ny = y + (long) (dy * rsm.step / d + (dy >= 0 ? 0.5 : -0.5));
    }
    else {
        int walk = resumeWalkable(player, rsm.x, rsm.y);
        if (walk == 0) {
            resumeBack(RR_WALL, "the place or a neighbour not walkable", now);
            return;
        }
        if (walk < 0) {
            if (!rsm.waitLoad) {
                rsm.waitLoad = 1;
                rsm.loadAt = now;
                rsm.loadTicks = 0;
                logLine("resume: rooms at the place not loaded yet, waiting");
            }
            if (resumeWaitOver(rsm.loadAt, rsm.loadTicks, RESUME_HOP_SEEN_MS, RESUME_HOP_SEEN_TICKS, RESUME_HOP_SEEN_MAX_MS, now))
                resumeBack(RR_WALL, "the rooms at the place did not load", now);
            else rsm.rejump = 1;
            return;
        }
        rsm.waitLoad = 0;
    }
    rsm.hopX = nx;
    rsm.hopY = ny;
    rsm.seq = srvSubmit(SRV_CODE_PLACE, nx, ny);
    rsm.phaseAt = now;
    rsm.phaseTicks = 0;
}

static void updateResume(void* player, DWORD now) {
    DWORD playerId = player ? RD32(player, U_ID) : 0;
    if (player && (player != resumePlayer || playerId != resumePlayerId)) {
        logLine("resume: hero unit %p id %lu (before %p id %lu): a new game", player, (unsigned long) playerId,
                resumePlayer, (unsigned long) resumePlayerId);
        resumePlayer = player;
        resumePlayerId = playerId;
        memset(&rsm, 0, sizeof(rsm));
        resumeDifficulty = -1;
        resumeDiffSeq = 0;
        resumeGames = srvResets;
    }
    if (srvResets != resumeGames) {
        int out = player && !isTown(unitLevel(player));
        int away = out || rsm.phase == 1 || rsm.phase == 2;
        logLine("resume: server queue emptied (hero %p id %lu, %s, phase %d, protection %d)", player,
                (unsigned long) playerId, out ? "out of town" : "in town or none", rsm.phase, rsm.guardOn);
        resumeGames = srvResets;
        rsm.seq = rsm.onSeq = rsm.offSeq = 0;
        memset(rsm.dropSeq, 0, sizeof(rsm.dropSeq));
        resumeDiffSeq = 0;
        resumeDifficulty = -1;
        if (rsm.guardOn && away && !rsm.offWanted) rsm.onSeq = srvSubmit(SRV_CODE_INVUL_ON, 0, 0);
        else if (rsm.guardOn) {
            rsm.offWanted = 1;
            rsm.offAskedAt = 0;
        }
        if (!rsm.phase) {
            rsm.usedGame = 0;
            rsm.state = RS_NONE;
            rsm.reason = RR_NONE;
        }
        else if (rsm.phase >= 3 && (!player || unitLevel(player) != rsm.level)) {
            resumeEnd(RS_REFUSED, RR_TIME, "stalled: not on the level any more");
        }
        else if (rsm.phase == 3) {
            rsm.rejump = 1;
            rsm.phaseAt = now;
            rsm.phaseTicks = 0;
            logLine("resume: queue emptied while jumping: on from where the hero is");
        }
        else if (rsm.phase == 4) rsm.phaseAt = now;
        else {
            rsm.arrived = 1;
            rsm.phase = 2;
            rsm.phaseAt = now;
            rsm.phaseTicks = 0;
        }
    }
    if (!player || !singlePlayerGame()) return;
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1) return;
    LONG answer, count;
    if (resumeDifficulty < 0 && !resumeDiffSeq) resumeDiffSeq = srvSubmit(SRV_CODE_DIFFICULTY, 0, 0);
    if (resumeDiffSeq && srvTake(resumeDiffSeq, &answer, NULL)) {
        resumeDiffSeq = 0;
        resumeDifficulty = answer >= 0 && answer <= 2 ? answer : -1;
        if (resumeDifficulty < 0) resumeDiffSeq = 0;
    }
    for (int i = 0; i < 4; i++) if (rsm.dropSeq[i] && srvTake(rsm.dropSeq[i], &answer, NULL)) rsm.dropSeq[i] = 0;
    if (rsm.lastTick && (now - rsm.lastTick > 500 || worldPaused())) {
        DWORD gap = now - rsm.lastTick;
        if (rsm.guardOn) rsm.guardUntil += gap, rsm.guardHard += gap;
        if (rsm.phase) rsm.phaseAt += gap, rsm.started += gap;
        if (rsm.waitLoad) rsm.loadAt += gap;
    }
    else if (rsm.phase) rsm.ticks++, rsm.phaseTicks++, rsm.loadTicks++;
    rsm.lastTick = now;
    long hx, hy;
    DWORD mode = RD32(player, U_MODE);
    switch (rsm.phase) {
    case 1:
        if (srvTake(rsm.seq, &answer, &count)) {
            rsm.seq = 0;
            logLine("resume: warp answered %ld at game frame %ld (limit %d)", (long) answer, (long) count, RESUME_MAX_FRAME);
            if (answer != 1) {
                resumeEnd(RS_REFUSED, answer == 3 ? RR_FUNCTIONS : answer == 4 ? RR_LATE : RR_WARP,
                          answer == 4 ? "warp refused: the game runs longer than 60 s" : "warp refused");
                break;
            }
            rsm.arrived = 1;
            rsm.phase = 2;
            rsm.phaseAt = now;
            rsm.phaseTicks = 0;
        }
        else if (resumeWaitOver(rsm.phaseAt, rsm.phaseTicks, RESUME_ARRIVE_MS, RESUME_ARRIVE_TICKS, RESUME_ARRIVE_MAX_MS, now))
            resumeEnd(RS_REFUSED, RR_TIME, "warp: no answer");
        break;
    case 2:
        if (unitLevel(player) == rsm.level && resumeHeroAt(player, &hx, &hy)) {
            rsm.arrived = 1;
            resumeGuardOn(now);
            if (!resumeRoomSame(player)) { resumeArrived(now, RS_WAYPOINT, RR_ROOM, "room at the place not the saved one: at the waypoint"); break; }
            logLine("resume: level shown after %lu ms / %lu ticks, jumping", (unsigned long) (now - rsm.started),
                    (unsigned long) rsm.ticks);
            rsm.phase = 3;
            rsm.step = RESUME_STEP;
            resumeJump(player, hx, hy, now);
        }
        else if (resumeWaitOver(rsm.phaseAt, rsm.phaseTicks, RESUME_ARRIVE_MS, RESUME_ARRIVE_TICKS, RESUME_ARRIVE_MAX_MS, now)) {
            int inTown = isTown(unitLevel(player));
            resumeArrived(now, inTown ? RS_REFUSED : RS_WAYPOINT, RR_TIME,
                          inTown ? "warp: level not shown in time, still in town" : "warp: level not shown in time, at the waypoint");
        }
        break;
    case 3:
        if (resumeWaitOver(rsm.started, rsm.ticks, RESUME_CHAIN_MS, RESUME_CHAIN_TICKS, RESUME_CHAIN_MAX_MS, now)) {
            resumeDrop(&rsm.seq);
            resumeBack(RR_TIME, "warp and jumps took too long", now);
            break;
        }
        if (rsm.rejump) {
            if (!resumeHeroAt(player, &hx, &hy)) break;
            rsm.rejump = 0;
            rsm.step = RESUME_STEP;
            resumeJump(player, hx, hy, now);
            break;
        }
        if (rsm.seq) {
            if (!srvTake(rsm.seq, &answer, NULL)) break;
            rsm.seq = 0;
            if (answer != 1) {
                rsm.step = rsm.step > RESUME_STEP_MIN ? (rsm.step + 1) / 2 : 0;
                if (!rsm.step || !resumeHeroAt(player, &hx, &hy)) { resumeBack(RR_JUMP, "jump refused at the shortest step", now); break; }
                resumeJump(player, hx, hy, now);
                break;
            }
            rsm.phaseAt = now;
            rsm.phaseTicks = 0;
        }
        if (!resumeHeroAt(player, &hx, &hy)) break;
        if (labs(hx - rsm.hopX) > 2 || labs(hy - rsm.hopY) > 2) {
            if (resumeWaitOver(rsm.phaseAt, rsm.phaseTicks, RESUME_HOP_SEEN_MS, RESUME_HOP_SEEN_TICKS, RESUME_HOP_SEEN_MAX_MS, now)) {
                logLine("resume: hero at %ld,%ld, the jump went to %ld,%ld", hx, hy, rsm.hopX, rsm.hopY);
                resumeBack(RR_JUMP, "the client did not show the jump", now);
            }
            break;
        }
        if (labs(hx - rsm.x) <= 2 && labs(hy - rsm.y) <= 2) {
            logLine("resume: at the place after %lu ms / %lu ticks", (unsigned long) (now - rsm.started), (unsigned long) rsm.ticks);
            resumeArrived(now, RS_SPOT, RR_NONE, "at the place");
            break;
        }
        rsm.step = RESUME_STEP;
        resumeJump(player, hx, hy, now);
        break;
    case 4:
        if (rsm.seq) {
            if (srvTake(rsm.seq, &answer, NULL)) {
                rsm.seq = 0;
                if (answer == 1) {
                    BYTE why = rsm.reason ? rsm.reason : RR_JUMP;
                    resumeArrived(now, RS_WAYPOINT, why, "back at the waypoint");
                }
            }
            else if ((int) (now - rsm.phaseAt) > RESUME_ARRIVE_MS && srvCancel(rsm.seq)) rsm.seq = 0;
            break;
        }
        if (rsm.backTries >= 3) { resumeArrived(now, RS_REFUSED, rsm.reason ? rsm.reason : RR_JUMP, "back to the waypoint failed"); break; }
        if (rsm.backTries && (int) (now - rsm.phaseAt) < 1000) break;
        if (unitLevel(player) != rsm.level) { resumeEnd(RS_REFUSED, RR_TIME, "back: not on the level any more"); break; }
        rsm.backTries++;
        rsm.phaseAt = now;
        rsm.seq = srvSubmit(SRV_CODE_WARP, rsm.level, 0);
        break;
    }
    if (rsm.onSeq && srvTake(rsm.onSeq, &answer, &count)) {
        rsm.onSeq = 0;
        resumeLog(answer ? "invulnerable" : "invulnerable: refused");
    }
    if (rsm.offSeq && srvTake(rsm.offSeq, &answer, &count)) {
        rsm.offSeq = 0;
        if (answer == 1 || answer == 0) {
            rsm.guardOn = 0;
            rsm.offWanted = 0;
            resumeDrop(&rsm.onSeq);
            resumeLog(answer ? "invulnerability off" : "invulnerability: hero dead, the game keeps the flags");
        }
    }
    if (rsm.guardOn && !rsm.offWanted) {
        int attack = mode == 7 || mode == 8 || (mode >= 10 && mode <= 16) || mode == 18;
        if ((int) (now - rsm.guardUntil) > 0 || (int) (now - rsm.guardHard) > 0 || attack || mode == 0 || mode == 17) {
            rsm.offWanted = 1;
            rsm.offAskedAt = 0;
        }
    }
    if (rsm.guardOn && rsm.offWanted) {
        if (!rsm.offSeq && now - rsm.offAskedAt >= RESUME_OFF_REPEAT_MS) {
            rsm.offAskedAt = now;
            rsm.offSeq = srvSubmit(SRV_CODE_INVUL_OFF, 0, 0);
        }
        else if (rsm.offSeq && now - rsm.offAskedAt >= 1000 && srvCancel(rsm.offSeq)) rsm.offSeq = 0;
    }
}

static void putResumeFields(Packet* p) {
    void* player = fGetPlayerUnit();
    long x = 0, y = 0;
    WORD rx = 0, ry = 0, rw = 0, rh = 0;
    int ok = player && singlePlayerGame() && resumeHeroAt(player, &x, &y) && resumeHeroRoom(player, &rx, &ry, &rw, &rh);
    putU16(p, ok ? (WORD) unitLevel(player) : 0);
    putU16(p, (WORD) x);
    putU16(p, (WORD) y);
    putU32(p, ok ? resumeSeed(player) : 0);
    putU16(p, rx);
    putU16(p, ry);
    putU16(p, rw);
    putU16(p, rh);
    putU8(p, resumeDifficulty >= 0 ? (BYTE) resumeDifficulty : 0xFF);
    putU8(p, (srvState == 1 ? 1 : 0) | (resumeFunctionsKnown() ? 6 : 0));
    putU8(p, rsm.state);
    putU8(p, rsm.reason);
    DWORD now = GetTickCount();
    long left = rsm.guardOn && !rsm.offWanted ? (long) (rsm.guardUntil - now) : 0;
    putU8(p, left > 0 ? (BYTE) (left / 100 > 255 ? 255 : left / 100) : 0);
}
