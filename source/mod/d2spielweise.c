










#include "d2spielweisecore.c"

#define SW_MARKER "d2android-spielweise"
#define RVA_ItemRatio      0xA4EA4
#define DT_UNIQUES         0xC24
#define DT_EXPERIENCE      0xC78
#define DT_DIFFICULTY      0xC7C
#define DT_TREASURE        0x9BC



#define RVA_GameNoDropCmp  0xE41BB
#define RVA_GameNoDropOn   0xE41C4
#define RVA_GameNoDropSkip 0xE428F

static unsigned char swStage[SW_REGULATORS];
static int swRead;
static DWORD swLast;
static void* swFailed[SW_REGULATORS];
static void* swDone[SW_REGULATORS];



volatile LONG swNoDropFloorNow = 1;
DWORD swNoDropOn, swNoDropSkip;
void swNoDropStub(void);
__asm__(
    ".text\n"
    ".globl _swNoDropStub\n"
    "_swNoDropStub:\n"
    "    cmpl _swNoDropFloorNow, %edi\n"
    "    jge 1f\n"
    "    movl _swNoDropFloorNow, %edi\n"
    "1:  cmpl $1, %edi\n"
    "    jle 2f\n"
    "    jmp *_swNoDropOn\n"
    "2:  jmp *_swNoDropSkip\n"
);
static int swNoDropSite = -1;
static LONG swNoDropRequest;
static DWORD swNoDropResets;

static const BYTE swNoDropBytes[9] = {0x83, 0xFF, 0x01, 0x0F, 0x8E, 0xCB, 0x00, 0x00, 0x00};


static int swNoDropPatched(DWORD game) {
    const BYTE* at = (const BYTE*) (game + RVA_GameNoDropCmp);
    return readable(at, 9) && at[0] == 0xE9 && game + RVA_GameNoDropCmp + 5 + *(const DWORD*) (at + 1) == (DWORD) swNoDropStub
        && at[5] == 0x90 && at[6] == 0x90 && at[7] == 0x90 && at[8] == 0x90;
}

#define GAME_MONREGIONS 0xF0



static LONG spielweiseDensity(void* game, LONG stage) {
    if (!readable(game, GAME_MONREGIONS + 4 * SW_LEVELS) || !readable(game, GAME_DIFFICULTY + 1)) return -1;
    int diff = *((BYTE*) game + GAME_DIFFICULTY);
    if (diff > 2) return -1;
    for (int pass = 0; pass < 2; pass++) {
        LONG written = 0;
        for (int l = 1; l < SW_LEVELS; l++) {
            BYTE* region = *(BYTE**) ((BYTE*) game + GAME_MONREGIONS + 4 * l);
            if (!readable(region, SW_REGION_SIZE)) return -1;
            int r = swDensityRegion(region, l, diff, (int) stage, pass);
            if (r < 0) return -1;
            written += r;
        }
        if (pass) return written;
    }
    return -1;
}



static LONG spielweiseServer(LONG arg, LONG arg2, void* pGame) {
    if (arg == 1) return spielweiseDensity(pGame, arg2);
    DWORD game = (DWORD) GetModuleHandleA("D2Game.dll");
    if (!game) return 0;
    if (swNoDropPatched(game)) return 1;
    if (!bytesAre(game + RVA_GameNoDropCmp, swNoDropBytes, sizeof(swNoDropBytes))) return 0;
    BYTE* at = (BYTE*) (game + RVA_GameNoDropCmp);
    DWORD old;
    if (!VirtualProtect(at, 9, PAGE_EXECUTE_READWRITE, &old)) return 0;
    swNoDropOn = game + RVA_GameNoDropOn;
    swNoDropSkip = game + RVA_GameNoDropSkip;
    at[0] = 0xE9;
    *(DWORD*) (at + 1) = (DWORD) swNoDropStub - (game + RVA_GameNoDropCmp + 5);
    at[5] = at[6] = at[7] = at[8] = 0x90;
    VirtualProtect(at, 9, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 9);
    return 1;
}

static LONG swDensityRequest;
static DWORD swDensityResets = (DWORD) -1;



static void swDensityTick(void) {
    if (!swStage[SW_DENSITY] || srvState != 1) return;
    LONG answer;
    if (swDensityRequest && swDensityResets != srvResets) swDensityRequest = 0;
    if (!swDensityRequest && swDensityResets != srvResets) {
        swDensityRequest = srvSubmit(SRV_CODE_SPIELWEISE, 1, swStage[SW_DENSITY]);
        swDensityResets = srvResets;
    }
    else if (swDensityRequest && srvTake(swDensityRequest, &answer, NULL)) {
        swDensityRequest = 0;
        if (answer < 0) logLine("spielweise: monster density NOT as expected (another mod or version?), left as is");
        else logLine("spielweise: monster density stage %d %s (%ld levels)", swStage[SW_DENSITY], answer ? "written" : "already so", answer);
    }
}


static void swNoDropTick(void) {
    int floor = swNoDropFloor(swStage[SW_NODROP]);
    InterlockedExchange(&swNoDropFloorNow, floor);
    DWORD game = (DWORD) GetModuleHandleA("D2Game.dll");
    if (!game || floor <= 1) return;
    if (swNoDropSite < 0) {
        swNoDropSite = swNoDropPatched(game) ? 2 : bytesAre(game + RVA_GameNoDropCmp, swNoDropBytes, sizeof(swNoDropBytes)) ? 1 : 0;
        logLine(swNoDropSite ? "spielweise: drop site verified%s" : "spielweise: drop site NOT as expected, NoDrop as in 1.13d",
            swNoDropSite == 2 ? " (stub already in place)" : "");
    }
    if (swNoDropSite != 1 || srvState != 1) return;
    LONG answer;
    if (swNoDropRequest && swNoDropResets != srvResets) swNoDropRequest = 0;
    if (!swNoDropRequest) {
        swNoDropRequest = srvSubmit(SRV_CODE_SPIELWEISE, 0, 0);
        swNoDropResets = srvResets;
    }
    else if (srvTake(swNoDropRequest, &answer, NULL)) {
        swNoDropRequest = 0;
        swNoDropSite = answer ? 2 : 0;
        logLine(answer ? "spielweise: fewer empty drops stage %d (at least %d) patched" : "spielweise: drop site NOT patched",
            swStage[SW_NODROP], floor);
    }
}

static void swReadFile(void) {
    swRead = 1;
    char path[MAX_PATH];
    lstrcpynA(path, logPath, sizeof(path));
    char* slash = strrchr(path, '\\');
    if (!slash || (size_t) (slash + 1 - path) + sizeof(SW_MARKER) > sizeof(path)) return;
    lstrcpyA(slash + 1, SW_MARKER);
    HANDLE h = CreateFileA(path, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, 0, NULL);
    if (h == INVALID_HANDLE_VALUE) {
        logLine("spielweise: no %s, everything as in 1.13d", SW_MARKER);
        return;
    }
    char buf[512];
    DWORD n = 0;
    if (!ReadFile(h, buf, sizeof(buf) - 1, &n, NULL)) n = 0;
    CloseHandle(h);
    if (swParse(buf, (int) n, swStage) < 0) {
        logLine("spielweise: %s not version %d, everything as in 1.13d", SW_MARKER, SW_VERSION);
        return;
    }
    logLine("spielweise: quality %d, fewer empty drops %d, high runes %d, gambling %d, experience %d, density %d, "
        "ladder uniques %d, boss loot %d, uniques more than once %d", swStage[SW_QUALITY], swStage[SW_NODROP], swStage[SW_RUNES],
        swStage[SW_GAMBLE], swStage[SW_EXP], swStage[SW_DENSITY], swStage[SW_LADDER], swStage[SW_BOSS], swStage[SW_UNIQUES]);
}


static void swNote(int n, const char* name, void* table, int result) {
    if (result < 0) {
        if (swFailed[n] != table) logLine("spielweise: %s NOT as expected (another mod or version?), left as is", name);
        swFailed[n] = table;
        return;
    }

    if (swDone[n] != table && (result > 0 || swStage[n]))
        logLine("spielweise: %s stage %d %s", name, swStage[n], result > 0 ? "written" : "already so");
    swDone[n] = table;
}


static void spielweiseTick(void* player, DWORD now) {
    if (!swRead) swReadFile();
    if (!player || !singlePlayerGame()) {
        InterlockedExchange(&swNoDropFloorNow, 1);
        return;
    }
    swNoDropTick();
    swDensityTick();
    if (now - swLast < 500) return;
    swLast = now;
    if (!common || !readable((void*) (common + RVA_DataTables), 4)) return;
    BYTE* tables = RDP(common, RVA_DataTables);
    if (!readable(tables, DT_DIFFICULTY + 8)) return;
    BYTE* ratio = readable((void*) (common + RVA_ItemRatio), 4) ? RDP(common, RVA_ItemRatio) : NULL;
    if (readable(ratio, SW_RATIO_RECORDS * SW_RATIO_SIZE) && swFailed[SW_QUALITY] != ratio)
        swNote(SW_QUALITY, "loot quality", ratio, swApplyRatio(ratio, swStage[SW_QUALITY]));
    BYTE* diff = RDP(tables, DT_DIFFICULTY);
    int diffCount = (int) RD32(tables, DT_DIFFICULTY + 4);
    if (readable(diff, SW_DIFF_RECORDS * SW_DIFF_SIZE) && swFailed[SW_GAMBLE] != diff)
        swNote(SW_GAMBLE, "gambling", diff, swApplyGamble(diff, diffCount, swStage[SW_GAMBLE]));
    BYTE* exp = RDP(tables, DT_EXPERIENCE);
    if (readable(exp, (SW_EXP_FIRST + SW_EXP_LEVELS + 1) * SW_EXP_SIZE) && swFailed[SW_EXP] != exp)
        swNote(SW_EXP, "experience from level 70", exp, swApplyExp(exp, swStage[SW_EXP]));
    BYTE* tc = RDP(tables, DT_TREASURE);
    int tcCount = (int) RD32(tables, DT_TREASURE + 4);
    if (tcCount == SW_TC_COUNT && readable(tc, SW_TC_COUNT * SW_TC_SIZE)) {
        if (swFailed[SW_RUNES] != tc) {
            unsigned int start[SW_RUNE_TCS];
            int ok = 1;
            for (int i = 0; i < SW_RUNE_TCS && ok; i++) {
                BYTE* rec = tc + (SW_RUNE_FIRST + i) * SW_TC_SIZE;
                BYTE* entries = RDP(rec, SW_TC_ENTRIES);
                DWORD n = RD32(rec, SW_TC_ITEMS);
                ok = n >= 1 && n <= 8 && readable(entries, n * SW_TC_ENTRY_SIZE);
                if (ok) start[i] = RD32(entries, (n - 1) * SW_TC_ENTRY_SIZE + SW_TC_ENTRY_START);
            }
            swNote(SW_RUNES, "high runes", tc, ok ? swApplyRunes(tc, tcCount, start, swStage[SW_RUNES]) : -1);
        }
        if (swFailed[SW_BOSS] != tc) swNote(SW_BOSS, "boss loot", tc, swApplyBoss(tc, tcCount, swStage[SW_BOSS]));
    }
    BYTE* uniques = RDP(tables, DT_UNIQUES);
    int uniqueCount = (int) RD32(tables, DT_UNIQUES + 4);
    if (uniqueCount == SW_UNIQUE_COUNT && readable(uniques, SW_UNIQUE_COUNT * SW_UNIQUE_SIZE)) {
        if (swFailed[SW_LADDER] != uniques)
            swNote(SW_LADDER, "ladder uniques", uniques, swApplyLadder(uniques, uniqueCount, swStage[SW_LADDER]));
        if (swFailed[SW_UNIQUES] != uniques)
            swNote(SW_UNIQUES, "uniques more than once", uniques, swApplyNolimit(uniques, uniqueCount, swStage[SW_UNIQUES]));
    }
}
