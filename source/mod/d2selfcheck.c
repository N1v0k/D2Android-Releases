































#include "d2selfrules.h"

#define RVA_UnitSelectable 0x63180
#define RVA_PanelSide      0x11D070
#define CMD_SELECTABLE     32
#define PKT_SELECTABLE     16
#define SC_REPORT_MS       5000
#define SC_ENTRIES         255

typedef int (__stdcall *UnitSelectable_t)(void* unit, int x, int y, int hitTest);
static UnitSelectable_t fUnitSelectable;
static int selfCheckSite = -1;
static DWORD scReportUntil;
static int scReportOnce;
static int scNewGame = 1;
static int selNewGame = 1;


static void selfCheckNewGame(void) {
    scNewGame = selNewGame = 1;
}


static int monsterVerdict(void* unit, int town, DWORD level);
static int objectVerdict(void* unit, const BYTE* txt, DWORD mode, DWORD level);
static const char* exclusionOf(void* unit, DWORD level);

static void checkSelfCheckSite(void) {

    static const BYTE start[] = {0xA1, 0, 0, 0, 0, 0x83, 0xEC, 0x28, 0x48, 0x55, 0x8B, 0x6C, 0x24, 0x30};
    const BYTE* site = (const BYTE*) (client + RVA_UnitSelectable);
    selfCheckSite = readable(site, sizeof(start)) && site[0] == start[0]
        && *(const DWORD*) (site + 1) == client + RVA_PanelSide && memcmp(site + 5, start + 5, sizeof(start) - 5) == 0;
    if (selfCheckSite) fUnitSelectable = (UnitSelectable_t) site;
    if (selfCheckSite) logLine("self-check ready (D2Client+0x63180)");
    else logLine("self-check off: unexpected code at D2Client+0x63180");
}


static void selfCheckCommand(const BYTE* buf, int n) {
    if (n < 9) return;
    if (buf[8] == 0) {
        scReportUntil = GetTickCount();
        scReportOnce = 0;
    }
    else if (buf[8] == 1) scReportOnce = 1;
    else if (buf[8] == 2) scReportUntil = GetTickCount() + SC_REPORT_MS;
}







static int scInList(const Candidate* list, int n, void* unit) {
    for (int i = 0; i < n; i++) if (list[i].unit == unit) return 1;
    return 0;
}


static int scTileUnder(void* unit, const Candidate* objects, int nObjects) {
    double x, y, tx, ty;
    unitPosition(unit, &x, &y);
    for (int i = 0; i < nObjects; i++) {
        if (RD32(objects[i].unit, U_TYPE) != UNIT_TILE) continue;
        unitPosition(objects[i].unit, &tx, &ty);
        if (tx - x >= -GATE_RADIUS && tx - x <= GATE_RADIUS && ty - y >= -GATE_RADIUS && ty - y <= GATE_RADIUS) return 1;
    }
    return 0;
}


static int scWhy(void* unit, DWORD type, int town, DWORD level, int (*gates)[2], int nGates, const Candidate* objects,
                 int nObjects) {
    if (type == UNIT_MONSTER) return monsterVerdict(unit, town, level) == MON_OUT ? SC_WHY_MONSTER : SC_WHY_LIMIT;
    if (type == UNIT_ITEM) return RD32(unit, U_MODE) == 3 ? SC_WHY_LIMIT : SC_WHY_ITEM;
    if (type == UNIT_OBJECT) {
        void* data = RDP(unit, U_DATA);
        BYTE* txt = data ? (BYTE*) RDP(data, ODATA_TXT) : NULL;
        if (txt && objectVerdict(unit, txt, RD32(unit, U_MODE), level)) return SC_WHY_LIMIT;
        return scTileUnder(unit, objects, nObjects) ? SC_WHY_TILE : SC_WHY_OBJECT;
    }
    double x, y;
    unitPosition(unit, &x, &y);
    return tileUnderGate(gates, nGates, (int) x, (int) y) ? SC_WHY_GATE : SC_WHY_LIMIT;
}

static void scLog(int verdict, DWORD type, DWORD txt, DWORD mode, DWORD level, int why, const char* onPurpose) {
    switch (verdict) {
        case SC_MISSING:
            logLine("selectable but not reported: type %lu txt %lu mode %lu level %lu why %s", (unsigned long) type,
                (unsigned long) txt, (unsigned long) mode, (unsigned long) level, scWhyName(why));
            break;
        case SC_ON_PURPOSE:
            logLine("selectable, left out on purpose: type %lu txt %lu mode %lu level %lu (%s)", (unsigned long) type,
                (unsigned long) txt, (unsigned long) mode, (unsigned long) level, onPurpose);
            break;
        case SC_OVER_LIMIT:
            logLine("selectable, over the packet's limit: type %lu txt %lu mode %lu level %lu", (unsigned long) type,
                (unsigned long) txt, (unsigned long) mode, (unsigned long) level);
            break;
        case SC_EXTRA:
            logLine("reported but not selectable: type %lu txt %lu mode %lu level %lu", (unsigned long) type,
                (unsigned long) txt, (unsigned long) mode, (unsigned long) level);
            break;
    }
}





static void selfCheck(void* player, const View* view, int width, int height, DWORD levelNo, const Candidate* monsters,
                      int nMonsters, const Candidate* items, int nItems, const Candidate* objects, int nObjects,
                      int (*gates)[2], int nGates) {
    static ScPending pending[5];
    static ScOnce once;
    static int lines[5];
    static DWORD packetNo;
    static Packet p;
    if (selfCheckSite < 0) checkSelfCheckSite();
    if (!selfCheckSite) return;
    void* act = RDP(player, U_ACT);
    if (!readable(act, ACT_ROOM1 + 4)) return;
    if (scNewGame) {
        scNewGame = 0;
        memset(pending, 0, sizeof(pending));
        memset(&once, 0, sizeof(once));
        memset(lines, 0, sizeof(lines));
    }
    packetNo++;
    DWORD now = GetTickCount();
    int report = scReportOnce || (int) (now - scReportUntil) < 0;
    int panels = RD32(client, RVA_PanelSide) != 0;
    if (panels && !report) return;
    int town = isTown(levelNo), n = 0, countAt = 0;
#ifdef D2MOD_TESTCMDS

    static LARGE_INTEGER freq, sum, most;
    static DWORD runs, asked;
    LARGE_INTEGER t0, t1;
    if (!freq.QuadPart) QueryPerformanceFrequency(&freq);
    QueryPerformanceCounter(&t0);
#endif
    if (report) {
        scReportOnce = 0;
        putHeader(&p, PKT_SELECTABLE);
        putU16(&p, levelNo);
        putU8(&p, panels ? 1 : 0);
        countAt = p.len;
        putU8(&p, 0);
    }
    int units = 0;
    for (void* room = RDP(act, ACT_ROOM1); room && units < 4096; room = RDP(room, ROOM1_NEXT)) {
        for (void* unit = RDP(room, ROOM1_UNITS); unit && units < 4096; unit = RDP(unit, U_LISTNEXT), units++) {
            DWORD type = RD32(unit, U_TYPE), mode = RD32(unit, U_MODE);
            if (type != UNIT_MONSTER && type != UNIT_ITEM && type != UNIT_OBJECT && type != UNIT_TILE) continue;
            if (type == UNIT_MONSTER && (mode == 0 || mode == 12)) continue;
            if (type == UNIT_ITEM && mode != 3) continue;
            double x, y;
            int ux, uy;
            unitPosition(unit, &x, &y);
            if (x == 0 && y == 0) continue;
            toScreen(view, x, y, &ux, &uy);
            if (ux < -80 || uy < -80 || ux > width + 80 || uy > height + 80) continue;
            if (panels) continue;
            DWORD id = RD32(unit, U_ID), txt = RD32(unit, U_TXTNO);
            int selectable = fUnitSelectable(unit, 0, 0, 0) != 0;
#ifdef D2MOD_TESTCMDS
            asked++;
#endif
            int reported = type == UNIT_MONSTER ? scInList(monsters, nMonsters, unit)
                : type == UNIT_ITEM ? scInList(items, nItems, unit) : scInList(objects, nObjects, unit);
            int why = selectable && !reported ? scWhy(unit, type, town, levelNo, gates, nGates, objects, nObjects) : 0;

            const char* onPurpose = why ? exclusionOf(unit, levelNo) : NULL;
            if (why && !onPurpose) onPurpose = scOnPurpose(type, txt, mode, why, type == UNIT_MONSTER && isFixedListMonster(txt),
                type == UNIT_MONSTER && isAncientStatue(txt), type == UNIT_MONSTER && (isMercenary(txt) || isSummon(txt)));
            int verdict = scVerdict(selectable, reported, why, onPurpose);
            if (report && n < SC_ENTRIES && (selectable || reported)) {
                putU8(&p, (BYTE) type);
                putU32(&p, id);
                putU16(&p, (WORD) txt);
                putU8(&p, (BYTE) mode);
                putU16(&p, (WORD) (short) ux);
                putU16(&p, (WORD) (short) uy);
                putU8(&p, (selectable ? 1 : 0) | (reported ? 2 : 0) | (onPurpose ? 4 : 0));
                putU8(&p, (BYTE) why);
                n++;
            }
            if (verdict == SC_FINE) continue;
            unsigned long long key = scKey(type, id, mode, levelNo, verdict);
            if (scSeen(&pending[verdict], key, packetNo) < 2) continue;
            if (lines[verdict] >= scCap(verdict) || !scOnce(&once, key)) continue;
            lines[verdict]++;
            scLog(verdict, type, txt, mode, levelNo, why, onPurpose);
        }
    }
    if (report) {
        p.data[countAt] = (BYTE) n;
        sendPacket(&p);
    }
#ifdef D2MOD_TESTCMDS
    QueryPerformanceCounter(&t1);
    LONGLONG spent = t1.QuadPart - t0.QuadPart;
    sum.QuadPart += spent;
    if (spent > most.QuadPart) most.QuadPart = spent;
    if (++runs == 1000) {
        logLine("self-check: 1000 runs, %lu units asked, on average %lu us, at most %lu us", (unsigned long) asked,
            (unsigned long) (sum.QuadPart * 1000000 / freq.QuadPart / runs), (unsigned long) (most.QuadPart * 1000000 / freq.QuadPart));
        runs = asked = 0;
        sum.QuadPart = most.QuadPart = 0;
    }
#endif
}
