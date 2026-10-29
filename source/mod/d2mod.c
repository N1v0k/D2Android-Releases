


















#define WIN32_LEAN_AND_MEAN
#include <winsock2.h>
#include <windows.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdarg.h>



#define GAMELOOP_PATCH_RVA      0x45280
#define RVA_GetPlayerUnit       0x613C0
#define RVA_GetUiVar_I          0x17C50
#define RVA_GetSelectedUnit     0x17280
#define RVA_SetSelectedUnit_I   0x17060
#define RVA_ClickMap            0x2B420
#define SELECTED_CALL_RVA       0x2B494




#define RVA_ClickTakenThisFrame 0x11D2CC
#define RVA_ClickTakenCheck     0x2B42F
#define RVA_FindServerSideUnit  0x620D0
#define RVA_FindClientSideUnit  0x620B0
#define RVA_ViewportX           0x106844
#define RVA_ViewportY           0x106840
#define RVA_AlwaysRun           0x11D234
#define RVA_GetMouseXOffset     0x5BC20
#define RVA_GetUnitX            0x1210
#define RVA_GetUnitY            0x1240
#define RVA_GetUnitName_I       0x622E0
#define RVA_GetMouseYOffset     0x5BC30
#define RVA_ScreenSizeX         0xF7034
#define RVA_ScreenSizeY         0xF7038
#define RVA_MouseX              0x11C950
#define RVA_MouseY              0x11C94C

#define RVA_DrawStatueLeft      0x6D549
#define RVA_DrawStatueRight     0x6D626
#define RVA_GfxDrawThunk        0xCFCE
#define RVA_DrawCursorCall      0x14EF9
#define RVA_DrawCursorCall2     0x14D77




#define RVA_DrawHelpButtonCall  0x1D5C7
#define RVA_DrawHelpButton      0xAB830
#define RVA_DrawQuestButtonCall 0x1D5FC
#define RVA_DrawQuestButton     0x8CEF0
#define RVA_UiVarHelpButton     0x11C918



#define RVA_HelpButtonDown      0xAA640
#define RVA_HelpButtonUp        0xAB621
#define RVA_HelpButtonLock      0x11CAE8

#define RVA_QuestButtonDownHit  0x8C62F
#define RVA_QuestButtonUpHit    0x8E210
#define RVA_QuestButtonHit      0x8B3D0
#define RVA_DrawManaOrbCall     0x6F349
#define RVA_DrawLifeOrbCall     0x6F34E
#define RVA_DrawManaOrb         0x6DDE0
#define RVA_DrawLifeOrb         0x6DCA0
#define RVA_DrawDurabilityCall  0x1D481
#define RVA_DrawDurability      0x6E950
#ifndef DURA_WARN_PERCENT
#define DURA_WARN_PERCENT 20
#endif


#define RVA_UiVarGameMenu       0x11C8B4


#define RVA_UiVars              0x11C890
#define UI_SHOWITEMS            0x0D



#define RVA_EscCloseList        0xD4078
#define RVA_EscCloseListRead    0x1851D



#define RVA_LabelsTipClearCall  0x4EA3B
#define RVA_SetHoverTextThunk   0xD28C
#define RVA_GetItemUseSound     0xB9A30
#define RVA_PlaySound           0x26270
#define RVA_SoundCheckVar       0x11D2F0


#define RVA_NpcMenuTable        0xF1A90
#define RVA_NpcMenuCount        0xF21E0
#define RVA_NpcMenuSearch       0x7B580
#define RVA_GetCurrentNpc       0x790D0
#define RVA_GetLocaleText       0xD10C
#define RVA_SetPlayersThunk     0xCF5C
#define RVA_SetPlayersImport    0xCFA6C
#define RVA_GamePlayers         0x111C44
#define RVA_GameType            0x11D1DC
#define RVA_NpcDialog           0x101A58
#define NPCMENU_SIZE            0x27
#define RVA_DrawGameMenuCall    0x1D43F
#define RVA_DrawGameMenu        0xC34D0

#define RVA_HotkeySkill         0x101410
#define RVA_HotkeyLeft          0x101500


#define RVA_GameRunDrainCall    0x931D4
#define RVA_GameRunDrain        0x91B30
#define STAT_STAMINA            10
#define STAT_STATPOINTS         4
#define STAT_SKILLPOINTS        5
#define STAT_QUANTITY           70
#define STAT_MAXSTAMINA         11


#define STATE_HEALTHPOT         100
#define STATE_MANAPOT           106
#define STATE_STAMINAPOT        180

#define RVA_GfxLoadRenderer     0xAA03
#define RVA_GfxLoadLibraryIat   0xD11C





#define RVA_StormStreamFill     0x2B620
#define RVA_StormStreamFillRead 0x2B632
#define RVA_StormStreamFillExit 0x2B76B
#define RVA_StormStreamLock     0x55934


#define RVA_GameSaveFlag        0xBE6D4

#define RVA_GamePlugYPacket     0x676C3


#define RVA_RevealAutomapRoom   0x73160
#define RVA_InitAutomapLayer    0x733D0
#define RVA_NewAutomapCell      0x703C0
#define RVA_AddAutomapCell      0x71EA0
#define RVA_AutomapCellPool     0x11CF20
#define RVA_AutomapLayerList    0x11CF24
#define RVA_AutomapLayer        0x11CF28
#define RVA_AutomapDivisor      0xF34F8
#define RVA_AutomapOffsetX      0x11CF5C
#define RVA_AutomapOffsetY      0x11CF60
#define RVA_AutomapMode         0x11CF14
#define RVA_AutomapCorner       0x11CFA4
#define RVA_AutomapCornerTop    0x11CE34
#define RVA_DrawAutomapCall     0x1D49C
#define RVA_DrawAutomap         0x73420
#define ORD_GetLevel            10122
#define ORD_InitLevel           10736
#define ORD_AddRoomData         10890
#define ORD_RemoveRoomData      10208
#define ORD_GetLevelText        10142

#define ORD_GetLevelDef         10087
#define ORD_GetObjectTxt        10319
#define ORD_DrawText            10076
#define ORD_GetTextWidth        10150
#define ORD_SetTextSize         10047
#define ORD_GetUnitStat         10550
#define ORD_GetUnitState        10706
#define ORD_GetItemText         10994
#define ORD_MapToAbsScreen      10115
#define ORD_SendPacket          10015

static const BYTE GAMELOOP_ORIGINAL[7] = {0x83, 0xEC, 0x20, 0x89, 0x4C, 0x24, 0x0C};


#define U_TYPE        0x00
#define U_TXTNO       0x04
#define U_ID          0x0C
#define U_MODE        0x10
#define U_DATA        0x14
#define U_ACT         0x1C
#define U_PATH        0x2C
#define U_INVENTORY   0x60
#define U_INFO        0xA8
#define INFO_FIRST    0x04
#define INFO_LEFT     0x08
#define INFO_RIGHT    0x0C
#define SKILL_NEXT    0x04
#define SKILL_ITEM    0x34
#define U_LISTNEXT    0xE8
#define PATH_X        0x00
#define PATH_Y        0x04
#define PATH_ROOM1    0x1C
#define PATH_DIR      0x64
#define IPATH_X       0x0C
#define IPATH_Y       0x10
#define ROOM1_ROOM2   0x10
#define ROOM1_UNITS   0x74
#define ROOM1_NEXT    0x7C
#define ROOM2_LEVEL   0x58
#define LEVEL_NO      0x1D0
#define ACT_ROOM1     0x10

#define ACT_SEED      0x0C
#define ACT_MISC      0x48
#define MISC_LEVELS   0x47C
#define LEVEL_ROOM2   0x10
#define ROOM2_NEAR    0x08
#define ROOM2_NEXT    0x24
#define ROOM2_NNEAR   0x2C
#define ROOM2_ROOM1   0x30
#define ROOM2_X       0x34
#define ROOM2_Y       0x38
#define ROOM2_W       0x3C
#define ROOM2_H       0x40
#define ROOM2_TILES   0x4C
#define ROOM2_PRESETS 0x5C
#define ROOM1_COLL    0x20
#define ROOM1_SUBX    0x4C
#define LTXT_NAME     0x16E
#define OTXT_NAME     0x40
#define OTXT_AUTOMAP  0x1BC
#define COLL_BLOCKED  0x0021
#define INV_FIRST     0x0C
#define IDATA_QUALITY 0x00
#define IDATA_LOCATION 0x45
#define IDATA_NEXTINV 0x64
#define IDATA_NODEPAGE 0x69
#define IDATA_BODYLOC 0x44
#define MDATA_FLAGS   0x16
#define MDATA_TXT     0x00
#define ODATA_TXT     0x00
#define OTXT_SELECTABLE0 0xC4
#define OTXT_SUBCLASS 0x167
#define OTXT_OPERATEFN 0x1B3
#define ITXT_CODE     0x80

#define UNIT_PLAYER 0
#define UNIT_MONSTER 1
#define UNIT_OBJECT 2
#define UNIT_ITEM 4
#define UNIT_TILE 5

#define RD8(p, off)  (*(BYTE*)((BYTE*)(p) + (off)))
#define RD16(p, off) (*(WORD*)((BYTE*)(p) + (off)))
#define RD32(p, off) (*(DWORD*)((BYTE*)(p) + (off)))
#define RDP(p, off)  (*(void**)((BYTE*)(p) + (off)))



#define APP_PORT 7958
#define MOD_PORT 7959
#define PROTOCOL_VERSION 2
#define PKT_STATUS 1
#define PKT_STATE 2
#define PKT_GOALS 3
#define PKT_MENU 5
#define PKT_EFFECTS 6
#define CMD_DRINK 1
#define CMD_UNIT 2
#define PHASE_DOWN 0
#define PHASE_HOLD 1
#define PHASE_UP 2
#define PHASE_CLICK 3
#define CMD_HIGHLIGHT 3
#define CMD_WALK 4
#define CMD_PORTAL 7
#define CMD_NPC 8
#define CMD_PLUGY 10
#define CMD_SELECT_SKILL 13

#define CMD_PLAYERS 9
#define CMD_PAUSE 6
#define CMD_HUD 5






#define STATUS_WAITING 0
#define STATUS_HOOKED 1
#define STATUS_WRONG_VERSION 2
#define STATUS_FAILED 3

#define MAX_MONSTERS 40
#define MAX_ITEMS 32
#define MAX_OBJECTS 24
#define MAX_CORPSES 24
#define MAX_CANDIDATES 256



typedef void* (__stdcall *GetPlayerUnit_t)(void);
typedef DWORD (__fastcall *DwordById_t)(DWORD);
typedef DWORD (__fastcall *DwordVoid_t)(void);
typedef int (__fastcall *UnitCoord_t)(void*);
typedef void* (__fastcall *FindUnit_t)(DWORD, DWORD);
typedef void (__stdcall *ClickMap_t)(DWORD, DWORD, DWORD, DWORD);
typedef DWORD (__stdcall *GetUnitStat_t)(void*, DWORD, DWORD);
typedef BOOL (__stdcall *GetUnitState_t)(void*, DWORD);
static GetUnitState_t fGetUnitState;
typedef void* (__stdcall *GetItemText_t)(DWORD);
typedef void (__stdcall *MapToAbsScreen_t)(long*, long*);
typedef void (__stdcall *SendPacket_t)(size_t, DWORD, BYTE*);

static HMODULE hSelf;
static DWORD client, common, net;
static GetPlayerUnit_t fGetPlayerUnit;
static GetPlayerUnit_t fGetSelectedUnit;
static DwordVoid_t fGetMouseXOffset, fGetMouseYOffset;
static UnitCoord_t fGetUnitX, fGetUnitY;
static FindUnit_t fFindServerSideUnit, fFindClientSideUnit;
static ClickMap_t fClickMap;
static DWORD fSetSelectedUnit;

static volatile int forcing, unitActions;
static void* forcedUnit;

static BYTE* selectedTrampoline;
static DWORD highlightType, highlightId = 0xFFFFFFFF, highlightUntil;
static struct {
    int active, button, shift;
    DWORD type, id, expireAt;
    long lastX, lastY;
} held;

static struct {
    int active;
    int dx, dy;
    DWORD expireAt;
    int pressPending;
    DWORD clickWait;
    int stopping;
    int pressDx, pressDy;
    DWORD pressAt;
    int turnPresses;
} walk;
static int clickFlagKnown;
static DWORD fGetUiVar, fGetUnitName;

static struct {
    DWORD rva, target;
    BYTE original[5], replacement[5];
    int verified;
} hudSites[11] = {
    {RVA_DrawStatueLeft, RVA_GfxDrawThunk, {0}, {0x83, 0xC4, 0x18, 0x90, 0x90}, 0},
    {RVA_DrawStatueRight, RVA_GfxDrawThunk, {0}, {0x83, 0xC4, 0x18, 0x90, 0x90}, 0},
    {RVA_DrawManaOrbCall, RVA_DrawManaOrb, {0}, {0x90, 0x90, 0x90, 0x90, 0x90}, 0},
    {RVA_DrawLifeOrbCall, RVA_DrawLifeOrb, {0}, {0x90, 0x90, 0x90, 0x90, 0x90}, 0},
    {RVA_DrawDurabilityCall, RVA_DrawDurability, {0}, {0x90, 0x90, 0x90, 0x90, 0x90}, 0},
    {RVA_DrawCursorCall, RVA_GfxDrawThunk, {0}, {0x83, 0xC4, 0x18, 0x90, 0x90}, 0},
    {RVA_DrawCursorCall2, RVA_GfxDrawThunk, {0}, {0x83, 0xC4, 0x18, 0x90, 0x90}, 0},
    {RVA_DrawHelpButtonCall, RVA_DrawHelpButton, {0}, {0x90, 0x90, 0x90, 0x90, 0x90}, 0},
    {RVA_DrawQuestButtonCall, RVA_DrawQuestButton, {0}, {0x90, 0x90, 0x90, 0x90, 0x90}, 0},

    {RVA_QuestButtonDownHit, RVA_QuestButtonHit, {0}, {0x33, 0xC0, 0x83, 0xC4, 0x08}, 0},
    {RVA_QuestButtonUpHit, RVA_QuestButtonHit, {0}, {0x33, 0xC0, 0x83, 0xC4, 0x08}, 0},
};


#define HUD_SITES 11
static int hudAvailable, hudHidden, duraAvailable, duraHidden, cursorAvailable, cursorHidden, edgeAvailable,
    edgeHidden;
static DWORD hudHideUntil, duraHideUntil, cursorHideUntil, edgeHideUntil;


static DWORD labelsUntil;

static DWORD playtestUntil;
static int labelsAvailable, labelsApplied;

static int escListAvailable, escListPatched;

static int labelTipsKept;

static DWORD game, staminaUntil, staminaCheckedAt;
static BYTE drainCallOriginal[5];
static int staminaChecked, staminaAvailable, staminaUnlimited;

static BYTE menuCallOriginal[5];
static int pauseAvailable, pauseWanted, pauseApplied, menuHidden, soundAvailable;

static int pauseOverOwnMenu;

static int menuHandedOver;

static volatile int plugyLoaded;
static DWORD pauseRefreshedAt;
static GetUnitStat_t fGetUnitStat;
static GetItemText_t fGetItemText;
static MapToAbsScreen_t fMapToAbsScreen;
static SendPacket_t fSendPacket;

static SOCKET sock = INVALID_SOCKET, cmdSock = INVALID_SOCKET;
static struct sockaddr_in appAddr;

static int appPort = APP_PORT, modPort = MOD_PORT, portsRead;
static volatile LONG status = STATUS_WAITING;
static char statusText[96] = "startet";
static DWORD lastSend, lastTick, seq;








static volatile LONG outOfGameSeen;


volatile LONG worldFrames;
static int worldHooked;
static volatile LONG heroVarOk;

static BYTE lastDrink, commandsReceived;
static char logPath[MAX_PATH];















#define LOG_LINE 2048
static CRITICAL_SECTION logLock;
static volatile LONG logReady;
static HANDLE logFile = INVALID_HANDLE_VALUE;
static char logBuf[64 * 1024];
static int logLen;


static void writeLogBuffer(void) {
    if (!logLen) return;
    DWORD written;
    if (logFile != INVALID_HANDLE_VALUE) WriteFile(logFile, logBuf, (DWORD) logLen, &written, NULL);
    logLen = 0;
}

static void flushLog(void) {
    if (!logReady) return;
    EnterCriticalSection(&logLock);
    writeLogBuffer();
    LeaveCriticalSection(&logLock);
}


static int formatLogLine(char* line, const char* fmt, va_list args) {
    SYSTEMTIME t;
    GetLocalTime(&t);
    int n = snprintf(line, LOG_LINE, "%02d:%02d:%02d.%03d ", t.wHour, t.wMinute, t.wSecond, t.wMilliseconds);
    int m = vsnprintf(line + n, LOG_LINE - n - 1, fmt, args);
    if (m < 0) m = 0;
    if (n + m > LOG_LINE - 2) {
        n = LOG_LINE - 5;
        memcpy(line + n, "\xE2\x80\xA6", 3);
        n += 3;
    }
    else n += m;
    line[n++] = '\n';
    return n;
}


static void appendLogLine(const char* line, int n) {
    if (logLen + n > (int) sizeof(logBuf)) writeLogBuffer();
    memcpy(logBuf + logLen, line, n);
    logLen += n;
}

static void logLine(const char* fmt, ...) {
    char line[LOG_LINE];
    va_list args;
    va_start(args, fmt);
    int n = formatLogLine(line, fmt, args);
    va_end(args);
    if (!logReady) return;
    EnterCriticalSection(&logLock);
    appendLogLine(line, n);
    LeaveCriticalSection(&logLock);
}


static void openLog(void) {
    logFile = CreateFileA(logPath, FILE_APPEND_DATA, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE, NULL,
                          OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    InitializeCriticalSection(&logLock);
    logReady = 1;
}


static int tryLogLock(int maxMs) {
    for (int i = 0; ; i++) {
        if (TryEnterCriticalSection(&logLock)) return 1;
        if (i >= maxMs) return 0;
        Sleep(1);
    }
}



static void logProcessEnd(int processEnds, ...) {
    char line[LOG_LINE];
    va_list args;
    va_start(args, processEnds);
    int n = formatLogLine(line, "process ends", args);
    va_end(args);
    if (tryLogLock(0)) {
        appendLogLine(line, n);
        writeLogBuffer();
        LeaveCriticalSection(&logLock);
    }
    else if (processEnds) {
        appendLogLine(line, n);
        writeLogBuffer();
    }
}

static LPTOP_LEVEL_EXCEPTION_FILTER logPreviousFilter;
static volatile LONG logInFilter;
























static DWORD gameThreadId, exeBase;
static const char* volatile crashStep;
#define STEP(call) do { crashStep = #call; call; } while (0)
static BYTE lastCommands[8];
static volatile LONG lastCommandNext, crashCount;
static DWORD periodicSince[2];

static void noteCommand(int cmd) {
    if (cmd == CMD_HUD || cmd == 12) {
        periodicSince[cmd == 12]++;
        return;
    }
    lastCommands[(lastCommandNext++) & 7] = (BYTE) cmd;
    periodicSince[0] = periodicSince[1] = 0;
}


static int crashReadable(const void* p, DWORD size) {
    MEMORY_BASIC_INFORMATION m;
    if (!p || !VirtualQuery(p, &m, sizeof(m)) || m.State != MEM_COMMIT) return 0;
    if (!m.Protect || (m.Protect & (PAGE_NOACCESS | PAGE_GUARD))) return 0;
    return (DWORD) p + size <= (DWORD) m.BaseAddress + m.RegionSize;
}


static DWORD crashModule(DWORD addr, char* name, int size) {
    MEMORY_BASIC_INFORMATION m;
    name[0] = 0;
    if (!addr || !VirtualQuery((void*) addr, &m, sizeof(m)) || m.State != MEM_COMMIT || m.Type != MEM_IMAGE) return 0;
    BYTE* base = (BYTE*) m.AllocationBase;
    lstrcpynA(name, (DWORD) base == exeBase ? "Game.exe" : "?", size);
    if ((DWORD) base == exeBase || !crashReadable(base, sizeof(IMAGE_DOS_HEADER))
        || ((IMAGE_DOS_HEADER*) base)->e_magic != IMAGE_DOS_SIGNATURE) return (DWORD) base;
    IMAGE_NT_HEADERS32* nt = (IMAGE_NT_HEADERS32*) (base + ((IMAGE_DOS_HEADER*) base)->e_lfanew);
    if (!crashReadable(nt, sizeof(*nt)) || nt->Signature != IMAGE_NT_SIGNATURE
        || nt->OptionalHeader.NumberOfRvaAndSizes <= IMAGE_DIRECTORY_ENTRY_EXPORT) return (DWORD) base;
    DWORD rva = nt->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_EXPORT].VirtualAddress;
    IMAGE_EXPORT_DIRECTORY* exports = (IMAGE_EXPORT_DIRECTORY*) (base + rva);
    if (!rva || !crashReadable(exports, sizeof(*exports)) || !exports->Name) return (DWORD) base;
    const char* s = (const char*) (base + exports->Name);
    int i = 0;
    while (i < size - 1 && crashReadable(s + i, 1) && s[i] > 0x20 && s[i] < 0x7F) {
        name[i] = s[i];
        i++;
    }
    if (i) name[i] = 0;
    else lstrcpynA(name, "?", size);
    return (DWORD) base;
}


static void crashWhere(char* out, int size, DWORD addr) {
    char name[40];
    DWORD base = crashModule(addr, name, sizeof(name));
    if (base) snprintf(out, size, "%s+%lX", name, (unsigned long) (addr - base));
    else snprintf(out, size, "%08lX", (unsigned long) addr);
}


static void logCrashLines(EXCEPTION_POINTERS* info, int first) {
    EXCEPTION_RECORD* rec = info ? info->ExceptionRecord : NULL;
    CONTEXT* ctx = info ? info->ContextRecord : NULL;
    DWORD code = rec ? rec->ExceptionCode : 0, at = rec ? (DWORD) rec->ExceptionAddress : 0;
    DWORD thread = GetCurrentThreadId();
    char where[64], access[48] = "", step[48], commands[72];
    crashWhere(where, sizeof(where), at);
    if (rec && code == EXCEPTION_ACCESS_VIOLATION && rec->NumberParameters >= 2) {
        ULONG_PTR kind = rec->ExceptionInformation[0];
        snprintf(access, sizeof(access), ", %s %08lX", kind == 0 ? "read" : kind == 1 ? "write" : kind == 8 ? "execute" : "access",
                 (unsigned long) rec->ExceptionInformation[1]);
    }
    const char* s = crashStep;
    if (s) snprintf(step, sizeof(step), "%.*s", (int) strcspn(s, "("), s);
    else lstrcpynA(step, "none (not in the mod's tick)", sizeof(step));
    int n = 0;
    LONG next = lastCommandNext;
    for (int i = next > 8 ? next - 8 : 0; i < next; i++)
        n += snprintf(commands + n, sizeof(commands) - n, "%s%u", n ? " " : "", lastCommands[i & 7]);
    if (!n) n = snprintf(commands, sizeof(commands), "none");
    snprintf(commands + n, sizeof(commands) - n, " (then 5x%lu 12x%lu)", (unsigned long) periodicSince[0],
             (unsigned long) periodicSince[1]);
    logLine("crash: exception %08lX at %08lX = %s%s, thread %04lX (%s), mod step %s, last commands %s",
            (unsigned long) code, (unsigned long) at, where, access, (unsigned long) thread,
            !gameThreadId ? "game thread not known yet" : thread == gameThreadId ? "game thread" : "not the game thread",
            step, commands);
    writeLogBuffer();
    if (!first || !ctx) return;
    logLine("crash: registers eax %08lX ebx %08lX ecx %08lX edx %08lX esi %08lX edi %08lX ebp %08lX esp %08lX eip %08lX "
            "flags %08lX", ctx->Eax, ctx->Ebx, ctx->Ecx, ctx->Edx, ctx->Esi, ctx->Edi, ctx->Ebp, ctx->Esp, ctx->Eip,
            ctx->EFlags);
    char stack[16 * 64];
    int len = 0, words = 0;
    for (; words < 16; words++) {
        DWORD* p = (DWORD*) ctx->Esp + words;
        if (!crashReadable(p, 4)) break;
        DWORD v = *p;
        char name[40];
        DWORD base = crashModule(v, name, sizeof(name));
        if (base) len += snprintf(stack + len, sizeof(stack) - len, " %08lX=%s+%lX", (unsigned long) v, name,
                                  (unsigned long) (v - base));
        else len += snprintf(stack + len, sizeof(stack) - len, " %08lX", (unsigned long) v);
        if (len >= (int) sizeof(stack)) break;
    }
    logLine("crash: stack at %08lX (%d words):%s", ctx->Esp, words, words ? stack : " not readable");
}


static LONG WINAPI logCrashFilter(EXCEPTION_POINTERS* info) {
    LONG result = EXCEPTION_CONTINUE_SEARCH;

    if (InterlockedExchange(&logInFilter, 1)) return result;
    DWORD code = info && info->ExceptionRecord ? info->ExceptionRecord->ExceptionCode : 0;
    if (logReady && code != EXCEPTION_STACK_OVERFLOW && tryLogLock(100)) {
        writeLogBuffer();
        logCrashLines(info, InterlockedIncrement(&crashCount) == 1);
        writeLogBuffer();
        LeaveCriticalSection(&logLock);
    }
    if (logPreviousFilter) result = logPreviousFilter(info);
    InterlockedExchange(&logInFilter, 0);
    return result;
}


static void keepCrashFilter(void) {
    LPTOP_LEVEL_EXCEPTION_FILTER current = SetUnhandledExceptionFilter(logCrashFilter);
    if (current != logCrashFilter) logPreviousFilter = current;
}


static BYTE potionsWorking(void* player) {
    if (!fGetUnitState) return 0;
    return (fGetUnitState(player, STATE_HEALTHPOT) ? 4 : 0) | (fGetUnitState(player, STATE_MANAPOT) ? 8 : 0)
        | (fGetUnitState(player, STATE_STAMINAPOT) ? 16 : 0);
}





static DWORD traceSecond, traceCount, traceSkipped;

static void trace(const char* fmt, ...) {
    DWORD second = GetTickCount() / 1000;
    if (second != traceSecond) {
        if (traceSkipped) logLine("trace: %lu lines left out", (unsigned long) traceSkipped);
        traceSecond = second;
        traceCount = traceSkipped = 0;
    }
    if (++traceCount > 40) {
        traceSkipped++;
        return;
    }
    char line[512];
    va_list args;
    va_start(args, fmt);
    vsnprintf(line, sizeof(line), fmt, args);
    va_end(args);
    logLine("%s", line);
}

static void setStatus(LONG code, const char* text) {
    status = code;
    lstrcpynA(statusText, text, sizeof(statusText));
    logLine("status %ld: %s", code, text);
}



typedef struct {
    BYTE data[4096];
    int len;
} Packet;

static void putU8(Packet* p, unsigned v) { if (p->len < (int) sizeof(p->data)) p->data[p->len++] = (BYTE) v; }
static void putU16(Packet* p, unsigned v) { putU8(p, v & 0xFF); putU8(p, (v >> 8) & 0xFF); }
static void putU32(Packet* p, DWORD v) { putU16(p, v & 0xFFFF); putU16(p, v >> 16); }
static void putHeader(Packet* p, int type) {
    p->len = 0;
    putU8(p, 'D'); putU8(p, '2'); putU8(p, 'A'); putU8(p, 'M');
    putU8(p, PROTOCOL_VERSION);
    putU8(p, type);
    putU16(p, (unsigned) InterlockedExchangeAdd((volatile LONG*) &seq, 1));
}


static volatile LONG socketsBroken;

static void sendPacket(Packet* p) {
    if (sock == INVALID_SOCKET) return;
    if (sendto(sock, (const char*) p->data, p->len, 0, (struct sockaddr*) &appAddr, sizeof(appAddr)) == SOCKET_ERROR) {
        int error = WSAGetLastError();

        if ((error == WSANOTINITIALISED || error == WSAENOTSOCK) && InterlockedExchange(&socketsBroken, 1) == 0) {
            logLine("sockets lost (error %d)", error);
        }
    }
}




static DWORD getUiVar(DWORD no) {
    DWORD a = no, c = no;
    __asm__ volatile ("call *%2" : "+a"(a), "+c"(c) : "r"(fGetUiVar) : "edx", "memory", "cc");
    return a;
}

static const wchar_t* unitName(void* unit) {
    DWORD a = (DWORD) unit, c = (DWORD) unit;
    __asm__ volatile ("call *%2" : "+a"(a), "+c"(c) : "r"(fGetUnitName) : "edx", "memory", "cc");
    return (const wchar_t*) a;
}

static const char* itemCode(void* item) {
    BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
    return txt ? (const char*) (txt + ITXT_CODE) : "";
}


static int potionKind(const char* code) {
    if (code[0] == 'h' && code[1] == 'p' && code[2] >= '1' && code[2] <= '5') return 1;
    if (code[0] == 'm' && code[1] == 'p' && code[2] >= '1' && code[2] <= '5') return 2;
    if (code[0] == 'r' && code[1] == 'v' && (code[2] == 's' || code[2] == 'l')) return 3;
    return 4;
}

static int isStaminaPotion(const char* code) {
    return code[0] == 'v' && code[1] == 'p' && code[2] == 's';
}


static int potionTier(const char* code) {
    if (code[0] == 'r' && code[1] == 'v') return code[2] == 's' ? 1 : 2;
    return code[2] - '0';
}


static int groundItemKind(const char* code) {
    if (code[0] == 'g' && code[1] == 'l' && code[2] == 'd') return 1;
    int potion = potionKind(code);
    if (potion >= 1 && potion <= 3) return 2;
    if (code[0] == 'r' && code[1] >= '0' && code[1] <= '3' && code[2] >= '0' && code[2] <= '9') return 3;

    if (code[0] == 'g' && strchr("cfslzp", code[1]) && code[1] && code[2] && strchr("vybgrw", code[2])) return 4;
    if (code[0] == 's' && code[1] == 'k' && code[2] && strchr("cfulz", code[2])) return 4;
    return 0;
}

static int isTown(DWORD levelNo) {
    return levelNo == 1 || levelNo == 40 || levelNo == 75 || levelNo == 103 || levelNo == 109;
}

static DWORD unitLevel(void* unit) {
    void* path = RDP(unit, U_PATH);
    if (!path) return 0;
    void* room1 = RDP(path, PATH_ROOM1);
    if (!room1) return 0;
    void* room2 = RDP(room1, ROOM1_ROOM2);
    if (!room2) return 0;
    void* level = RDP(room2, ROOM2_LEVEL);
    return level ? RD32(level, LEVEL_NO) : 0;
}



#include "d2companions.h"

static const wchar_t* localeText(WORD id);
static int npcMenuAvailable;




static int isHelperUnitName(const wchar_t* name) {
    if (!npcMenuAvailable) return lstrcmpiW(name, L"an evil force") == 0 || lstrcmpiW(name, L"eine b\u00f6se Macht") == 0;
    for (int i = 0; i < 5; i++) {
        const wchar_t* text = localeText((WORD) (5382 + i));
        if (text && lstrcmpiW(name, text) == 0) return 1;
    }
    return 0;
}


#include "d2monrules.h"
#include "d2bosses.c"

static int readable(const void* p, UINT_PTR size);


static BYTE* monStatsRow(void* unit) {
    void* data = RDP(unit, U_DATA);
    BYTE* row = readable(data, 4) ? (BYTE*) RDP(data, MDATA_TXT) : NULL;
    if (!readable(row, MONSTATS_FLAGS + 4)) return NULL;
    return RD16(row, MONSTATS_ID) == (WORD) RD32(unit, U_TXTNO) ? row : NULL;
}





static void* findMercenary(void* player) {
    void* act = RDP(player, U_ACT);
    int units = 0;
    for (void* room = act ? RDP(act, ACT_ROOM1) : NULL; room && units < 4096; room = RDP(room, ROOM1_NEXT)) {
        for (void* unit = RDP(room, ROOM1_UNITS); unit && units < 4096; unit = RDP(unit, U_LISTNEXT), units++) {
            if (RD32(unit, U_TYPE) != UNIT_MONSTER || !isMercenary(RD32(unit, U_TXTNO))) continue;
            DWORD mode = RD32(unit, U_MODE);
            if (mode == 0 || mode == 12) continue;
            if (fGetUnitStat(unit, 6, 0) < 256) continue;
            return unit;
        }
    }
    return NULL;
}


static int isFixedListMonster(DWORD txt) {
    static const DWORD bad[] = {227, 283, 326, 327, 328, 329, 330, 410, 411, 412, 413, 414, 415, 416, 366, 406,
        351, 352, 353, 266, 408, 516, 517, 518, 519, 522, 523, 543, 545};
    for (unsigned i = 0; i < sizeof(bad) / sizeof(bad[0]); i++) if (txt == bad[i]) return 1;
    return 0;
}


static int monsterLeftOut(void* unit) {
    DWORD mode = RD32(unit, U_MODE), txt = RD32(unit, U_TXTNO);
    if (mode == 0 || mode == 12) return 1;
    if (txt == 608 && mode == 8) return 1;
    if (txt == 68 && mode == 14) return 1;
    if ((txt == 258 || txt == 261) && mode == 14) return 1;
    if (isFixedListMonster(txt)) return 1;
    if (isMercenary(txt)) return 1;



    return isSummon(txt);
}






static int monsterReportOf(void* unit, int town) {
    if (monsterLeftOut(unit)) return MON_OUT;
    int ally = fGetUnitStat(unit, 172, 0) == 2;


    BYTE* row = monStatsRow(unit);
    if (row) return monsterReport(town, ally, RD32(unit, U_TXTNO), RD32(row, MONSTATS_FLAGS), RD16(row, MONSTATS_NAMESTR));
    if (!town && ally) return MON_OUT;
    const wchar_t* name = unitName(unit);
    if (name && isHelperUnitName(name)) return MON_OUT;
    return town ? MON_PERSON : MON_TARGET;
}



static int isValidMonster(void* unit, int town) {
    int report = monsterReportOf(unit, town);
    return town ? report != MON_OUT : report == MON_TARGET;
}

typedef struct {
    long offX, offY;
} View;


static void toScreen(const View* view, double x, double y, int* sx, int* sy) {
    long ix = (long) x, iy = (long) y;
    long ax = ix, ay = iy;
    fMapToAbsScreen(&ax, &ay);
    double fx = x - ix, fy = y - iy;
    *sx = (int) (ax + (fx - fy) * 16.0 - view->offX);
    *sy = (int) (ay + (fx + fy) * 8.0 - view->offY);
}

static void unitPosition(void* unit, double* x, double* y) {
    void* path = RDP(unit, U_PATH);
    DWORD type = RD32(unit, U_TYPE);
    if (!path) {
        *x = *y = 0;
        return;
    }
    if (type == UNIT_ITEM || type == UNIT_OBJECT || type == UNIT_TILE) {
        *x = fGetUnitX(unit);
        *y = fGetUnitY(unit);
    }
    else {
        *x = RD32(path, PATH_X) / 65536.0;
        *y = RD32(path, PATH_Y) / 65536.0;
    }
}

static void putName(Packet* p, void* unit) {
    char utf8[96];
    const wchar_t* name = unitName(unit);
    int len = 0;
    if (name) {
        int n = 0;
        while (n < 40 && name[n]) n++;
        len = WideCharToMultiByte(CP_UTF8, 0, name, n, utf8, sizeof(utf8), NULL, NULL);
        if (len < 0) len = 0;
    }
    putU8(p, len);
    for (int i = 0; i < len; i++) putU8(p, (BYTE) utf8[i]);
}



static void* findUnit(DWORD id, DWORD type) {
    void* unit = fFindServerSideUnit(id, type);
    return unit ? unit : fFindClientSideUnit(id, type);
}

static void setSelectedUnit(void* unit) {
    DWORD a = (DWORD) unit, c = (DWORD) unit;
    __asm__ volatile ("call *%2" : "+a"(a), "+c"(c) : "r"(fSetSelectedUnit) : "edx", "memory", "cc");
}


static void* originalSelectedUnit(void) {
    if (!selectedTrampoline) return fGetSelectedUnit();
    void* (__stdcall *original)(void) = (void* (__stdcall *)(void)) selectedTrampoline;
    return original();
}

static int teleportIgnoresHover(void* hovered);



static void* __cdecl selectedUnitIntercept(void) {
    if (forcing) return forcedUnit;
    void* hovered = originalSelectedUnit();
    return teleportIgnoresHover(hovered) ? NULL : hovered;
}


static void* __cdecl selectedUnitHighlight(void) {
    if (forcing) return forcedUnit;
    if (highlightId != 0xFFFFFFFF && (int) (GetTickCount() - highlightUntil) < 0) {
        void* unit = findUnit(highlightId, highlightType);
        if (unit) return unit;
    }
    return originalSelectedUnit();
}












static DWORD hideLabelsForClick(void) {
    if (!labelsAvailable) return 0;
    DWORD* var = (DWORD*) (client + RVA_UiVars) + UI_SHOWITEMS;
    DWORD old = *var;
    *var = 0;
    return old;
}

static void restoreLabels(DWORD old) {
    if (labelsAvailable) ((DWORD*) (client + RVA_UiVars))[UI_SHOWITEMS] = old;
}









static int clickFree(DWORD* waitingSince, DWORD now) {
    if (!clickFlagKnown || RD32(client, RVA_ClickTakenThisFrame) == 0) {
        *waitingSince = 0;
        return 1;
    }
    if (!*waitingSince) {
        *waitingSince = now | 1;
        return 0;
    }

    if ((int) (now - *waitingSince) < 250) return 0;
    trace("click flag still set after %d ms: cleared", (int) (now - *waitingSince));
    *(DWORD*) (client + RVA_ClickTakenThisFrame) = 0;
    *waitingSince = 0;
    return 1;
}

static void clickUnit(void* unit, int clickType, int shift) {
    long x = held.lastX, y = held.lastY;
    if (unit) {
        x = fGetUnitX(unit);
        y = fGetUnitY(unit);
        fMapToAbsScreen(&x, &y);
        x -= *(int*) (client + RVA_ViewportX);
        y -= *(int*) (client + RVA_ViewportY);
        held.lastX = x;
        held.lastY = y;
    }
    DWORD* mouseX = (DWORD*) (client + RVA_MouseX);
    DWORD* mouseY = (DWORD*) (client + RVA_MouseY);
    DWORD oldX = *mouseX, oldY = *mouseY;
    *mouseX = 0;
    *mouseY = 0;
    DWORD flags = shift ? 0x0C : (*(DWORD*) (client + RVA_AlwaysRun) ? 0x08 : 0);
    forcedUnit = unit;
    forcing = 1;
    DWORD labels = hideLabelsForClick();
    fClickMap(clickType, x, y, flags);
    restoreLabels(labels);
    if (unit) setSelectedUnit(NULL);
    forcing = 0;
    forcedUnit = NULL;
    *mouseX = oldX;
    *mouseY = oldY;
}


static void clickGround(int clickType, long x, long y) {
    DWORD* mouseX = (DWORD*) (client + RVA_MouseX);
    DWORD* mouseY = (DWORD*) (client + RVA_MouseY);
    DWORD oldX = *mouseX, oldY = *mouseY;
    *mouseX = 0;
    *mouseY = 0;
    DWORD flags = *(DWORD*) (client + RVA_AlwaysRun) ? 0x08 : 0;
    forcedUnit = NULL;
    forcing = 1;
    DWORD labels = hideLabelsForClick();
    fClickMap(clickType, x, y, flags);
    restoreLabels(labels);
    forcing = 0;
    *mouseX = oldX;
    *mouseY = oldY;
}









static void walkPoint(void* player, long* x, long* y) {
    *x = fGetUnitX(player);
    *y = fGetUnitY(player);
    fMapToAbsScreen(x, y);
    *x -= (long) fGetMouseXOffset();
    *y -= (long) fGetMouseYOffset();
    long w = (long) RD32(client, RVA_ScreenSizeX), h = (long) RD32(client, RVA_ScreenSizeY);
    double scale = 1;
    if (w > 0 && h > 0 && *x >= 1 && *x <= w - 2 && *y >= 1 && *y <= h - 2) {
        if (walk.dx > 0 && *x + walk.dx > w - 2) scale = (double) (w - 2 - *x) / walk.dx;
        if (walk.dx < 0 && *x + walk.dx < 1) scale = (double) (1 - *x) / walk.dx;
        if (walk.dy > 0 && *y + walk.dy > h - 2 && (double) (h - 2 - *y) / walk.dy < scale) scale = (double) (h - 2 - *y) / walk.dy;
        if (walk.dy < 0 && *y + walk.dy < 1 && (double) (1 - *y) / walk.dy < scale) scale = (double) (1 - *y) / walk.dy;
    }
    *x += (long) (walk.dx * scale);
    *y += (long) (walk.dy * scale);
    if (w > 0 && h > 0) {
        if (*x < 1) *x = 1;
        if (*x > w - 2) *x = w - 2;
        if (*y < 1) *y = 1;
        if (*y > h - 2) *y = h - 2;
    }
}


static void stopWalk(void* player) {
    if (walk.stopping == 2 && player) {

        long x, y;
        walk.dx = walk.dy = 0;
        walkPoint(player, &x, &y);
        clickGround(2, x, y);
    }
    walk.stopping = 0;
    if (!walk.active) return;
    walk.active = 0;
    if (walk.pressPending) {

        walk.pressPending = 0;
        return;
    }


    long x = 0, y = 0;
    if (player) {
        walk.dx = walk.dy = 0;
        walkPoint(player, &x, &y);
        clickGround(1, x, y);
    }
    clickGround(2, x, y);
}

static void releaseHeld(const char* why) {
    if (!held.active) return;
    held.active = 0;
    trace("held: let go of %lu (%s)", (unsigned long) held.id, why);
    clickUnit(findUnit(held.id, held.type), held.button ? 5 : 2, held.shift);
}


static DWORD playerMode(void) {
    void* player = fGetPlayerUnit();
    return player ? RD32(player, U_MODE) : 0xFFFFFFFF;
}


static int playerBusy(void) {
    DWORD mode = playerMode();
    return (mode >= 7 && mode <= 16) || mode == 18;
}









static struct {
    int active, button, shift, waitSwing, click;
    DWORD type, id, since, clickWait, expireAt;
} pendingPress;

static void setPendingPress(int button, int phase, int shift, DWORD type, DWORD id, DWORD now, int waitSwing) {
    pendingPress.active = 1;
    pendingPress.button = button;
    pendingPress.click = phase == PHASE_CLICK;
    pendingPress.shift = shift;
    pendingPress.type = type;
    pendingPress.id = id;
    pendingPress.since = now;
    pendingPress.expireAt = now + 600;
    pendingPress.waitSwing = waitSwing;
    pendingPress.clickWait = 0;
}







#define NO_REPRESS 0xFFFFFFFF
static DWORD heldRepressId = NO_REPRESS, heldRepressWait;

static void pressUnit(void* unit, int button, int click, int shift, DWORD type, DWORD id, DWORD now) {
    heldRepressId = NO_REPRESS;
    held.active = 1;
    held.button = button;
    held.shift = shift;
    held.type = type;
    held.id = id;
    held.expireAt = now + (click ? 80 : 600);
    clickUnit(unit, button ? 3 : 0, shift);
}


static DWORD lastUpId = 0xFFFFFFFF, lastUpAt;
static int lastUpKnown;







static int holdPressesAgain(int button, DWORD type, DWORD id, DWORD now) {
    if (held.active || pendingPress.active) return 0;
    if (lastUpKnown && lastUpId == id && (int) (now - lastUpAt) < 1000) return 0;
    void* unit = findUnit(id, type);
    if (!unit) return 0;
    DWORD mode = RD32(unit, U_MODE);
    if (RD32(unit, U_TYPE) == UNIT_MONSTER && (mode == 0 || mode == 12)) return 0;
    trace("unit hold %lu: nothing held, pressed again (%s)", (unsigned long) id, button ? "right" : "left");
    return 1;
}

static void unitAction(int button, int phase, int shift, DWORD type, DWORD id, DWORD now) {
    if (!unitActions) return;
    if (phase == PHASE_UP) {
        lastUpId = id;
        lastUpAt = now;
        lastUpKnown = 1;
        if (pendingPress.active && pendingPress.id == id) {
            pendingPress.active = 0;
            trace("press on %lu dropped (app: up)", (unsigned long) id);
        }
        if (held.active && held.id == id) releaseHeld("app: up");
        else trace("unit up %lu ignored (holding %lu, active %d)", (unsigned long) id, (unsigned long) held.id, held.active);
        return;
    }
    if (phase == PHASE_HOLD) {
        if (pendingPress.active && pendingPress.id == id) {
            pendingPress.expireAt = now + 600;
            return;
        }
        if (held.active && held.id == id && held.button == button) {
            held.expireAt = now + 600;
            return;
        }
        if (!holdPressesAgain(button, type, id, now)) {
            static DWORD lastIgnored;
            if (id != lastIgnored) trace("unit hold %lu ignored (holding %lu, active %d)", (unsigned long) id, (unsigned long) held.id, held.active);
            lastIgnored = id;
            return;
        }
        phase = PHASE_DOWN;
    }
    void* unit = findUnit(id, type);
    trace("unit %s %s type %lu id %lu%s (holding %lu, active %d, walking %d, player mode %lu)", button ? "right" : "left",
        phase == PHASE_CLICK ? "click" : "down", (unsigned long) type, (unsigned long) id, unit ? "" : " NOT FOUND",
        (unsigned long) held.id, held.active, walk.active, (unsigned long) playerMode());
    if (!unit) return;
    stopWalk(fGetPlayerUnit());
    pendingPress.active = 0;
    if (held.active && held.id != id && phase == PHASE_DOWN && playerBusy()) {
        releaseHeld("app: other unit, the press waits for the swing");
        setPendingPress(button, phase, shift, type, id, now, 1);
        return;
    }
    releaseHeld("app: other unit");
    DWORD wait = 0;
    if (!clickFree(&wait, now)) {
        trace("press on %lu waits for the next frame (the game took a click in this one)", (unsigned long) id);
        setPendingPress(button, phase, shift, type, id, now, 0);
        pendingPress.clickWait = wait;
        return;
    }
    pressUnit(unit, button, phase == PHASE_CLICK, shift, type, id, now);
}





static int heldClickType(DWORD now) {
    int press = held.button ? 3 : 0;
    if (heldRepressId == NO_REPRESS || heldRepressId != held.id) {
        heldRepressId = NO_REPRESS;
        return press + 1;
    }
    if (!clickFree(&heldRepressWait, now)) return -1;
    heldRepressId = NO_REPRESS;
    trace("held %lu: pressed again after the game's own release", (unsigned long) held.id);
    return press;
}






static void updateHeld(DWORD now) {
    static DWORD prevMode;
    DWORD curMode = playerMode();
    int blockEnded = prevMode == 9 && curMode != 9;
    prevMode = curMode;
    if (pendingPress.active) {
        if ((int) (now - pendingPress.expireAt) > 0) {
            pendingPress.active = 0;
            trace("press on %lu dropped after %d ms (no hold from the app for 600 ms)", (unsigned long) pendingPress.id,
                (int) (now - pendingPress.since));
            return;
        }

        if (pendingPress.waitSwing && playerBusy() && (int) (now - pendingPress.since) <= 1500) return;
        if (blockEnded) {
            trace("press on %lu waits a tick (block just ended)", (unsigned long) pendingPress.id);
            return;
        }
        if (!clickFree(&pendingPress.clickWait, now)) return;
        pendingPress.active = 0;
        void* unit = findUnit(pendingPress.id, pendingPress.type);
        trace("press on %lu after %d ms (player mode %lu)%s", (unsigned long) pendingPress.id,
            (int) (now - pendingPress.since), (unsigned long) playerMode(), unit ? "" : ", gone");
        if (unit) pressUnit(unit, pendingPress.button, pendingPress.click, pendingPress.shift, pendingPress.type, pendingPress.id, now);
        return;
    }
    if (!held.active) return;
    void* unit = findUnit(held.id, held.type);
    DWORD mode = unit ? RD32(unit, U_MODE) : 0;
    int dead = !unit || (RD32(unit, U_TYPE) == UNIT_MONSTER && (mode == 0 || mode == 12));
    if (dead || (int) (now - held.expireAt) > 0) {
        releaseHeld(dead ? "unit dead or gone" : "no hold from the app for 600 ms");
        return;
    }
    if (blockEnded) {
        trace("held %lu: hold waits a tick (block just ended)", (unsigned long) held.id);
        return;
    }
    int clickType = heldClickType(now);
    if (clickType >= 0) clickUnit(unit, clickType, held.shift);
    static DWORD lastHeldTrace;
    if (now - lastHeldTrace >= 500) {
        lastHeldTrace = now;
        trace("held %lu: player mode %lu, unit mode %lu", (unsigned long) held.id, (unsigned long) playerMode(), (unsigned long) mode);
    }
}










#define RVA_ClickBlocked 0x11C8B4
static volatile int replaying;
static int realHeld;
static struct {
    int active;
    int type, released, releaseDue;
    long x, y;
    DWORD flags, since, wait;
} gameLost;
BYTE* clickMapTrampoline __attribute__((used));


static int clickTakenNow(void) {
    return clickFlagKnown && RD32(client, RVA_ClickTakenThisFrame) != 0;
}

static int clickBlocked(void) {
    return RD32(client, RVA_ClickBlocked) != 0;
}


static void replayClick(int clickType, long x, long y, DWORD flags) {
    replaying = 1;
    fClickMap(clickType, x, y, flags);
    replaying = 0;
}







static int modHoldsButton(int bit) {
    return (held.active && (held.button ? 2 : 1) == bit) || (pendingPress.active && (pendingPress.button ? 2 : 1) == bit);
}






static void noteGameClick(DWORD clickType, DWORD x, DWORD y, DWORD flags) {
    if (forcing || replaying || !clickFlagKnown || clickBlocked() || clickType > 5) return;
    int bit = clickType >= 3 ? 2 : 1;
    if (clickType == 0 || clickType == 3) {
        if (clickTakenNow() && (x || y) && modHoldsButton(bit)) {
            trace("click: the game's %s press (%lu) at %lu,%lu dropped, the mod holds a unit with that button: not pressed "
                "again", bit == 2 ? "right" : "left", (unsigned long) clickType, (unsigned long) x, (unsigned long) y);
            if ((gameLost.type >= 3 ? 2 : 1) == bit) gameLost.active = gameLost.releaseDue = 0;
        }
        else if (clickTakenNow() && (x || y)) {
            gameLost.active = 1;
            gameLost.type = (int) clickType;
            gameLost.x = (long) x;
            gameLost.y = (long) y;
            gameLost.flags = flags;
            gameLost.released = gameLost.releaseDue = 0;
            gameLost.since = GetTickCount();
            gameLost.wait = 0;
            trace("click: the game's %s press (%lu) at %lu,%lu dropped, the frame was taken",
                bit == 2 ? "right" : "left", (unsigned long) clickType, (unsigned long) x, (unsigned long) y);
        }
        else {
            trace("click: the game's %s press (%lu) at %lu,%lu", bit == 2 ? "right" : "left",
                (unsigned long) clickType, (unsigned long) x, (unsigned long) y);
            realHeld |= bit;

            if ((gameLost.type >= 3 ? 2 : 1) == bit) gameLost.active = gameLost.releaseDue = 0;
        }
    }
    else if (clickType == 2 || clickType == 5) {
        trace("click: the game's %s release (%lu) at %lu,%lu%s", bit == 2 ? "right" : "left",
            (unsigned long) clickType, (unsigned long) x, (unsigned long) y, clickTakenNow() && (x || y) ? ", dropped" : "");
        realHeld &= ~bit;
        if (gameLost.active && (gameLost.type >= 3 ? 2 : 1) == bit) gameLost.released = 1;
        if (held.active) {



            trace("click: the game's release (%lu) while the mod holds %lu: its press follows again",
                (unsigned long) clickType, (unsigned long) held.id);
            heldRepressId = held.id;
            heldRepressWait = 0;
        }
    }
}



static volatile int clickMapType = -1;


__attribute__((force_align_arg_pointer, used)) void __cdecl d2ClickMapEntered(DWORD clickType, DWORD x, DWORD y, DWORD flags) {
    clickMapType = (int) clickType;
    noteGameClick(clickType, x, y, flags);
}



__asm__(
    ".text\n"
    ".globl _d2ClickMapStub\n"
    "_d2ClickMapStub:\n"
    "    pushal\n"
    "    pushfl\n"
    "    pushl 52(%esp)\n"
    "    pushl 52(%esp)\n"
    "    pushl 52(%esp)\n"
    "    pushl 52(%esp)\n"
    "    call _d2ClickMapEntered\n"
    "    addl $16, %esp\n"
    "    popfl\n"
    "    popal\n"
    "    jmp *_clickMapTrampoline\n");
extern void d2ClickMapStub(void);





static void installClickMapEntryHook(void) {
    BYTE* site = (BYTE*) (client + RVA_ClickMap);
    if (!clickFlagKnown || !readable(site, 5) || site[0] != 0xA1 || *(DWORD*) (site + 1) != client + RVA_ClickBlocked) {
        logLine("click entry hook: not installed (%02X): dropped presses are not pressed again", site[0]);
        return;
    }
    BYTE* tramp = (BYTE*) VirtualAlloc(NULL, 16, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
    if (!tramp) return;
    memcpy(tramp, site, 5);
    tramp[5] = 0xE9;
    *(DWORD*) (tramp + 6) = (DWORD) (site + 5) - ((DWORD) tramp + 10);
    FlushInstructionCache(GetCurrentProcess(), tramp, 10);
    clickMapTrampoline = tramp;
    DWORD old;
    if (!VirtualProtect(site, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    site[0] = 0xE9;
    *(DWORD*) (site + 1) = (DWORD) d2ClickMapStub - ((DWORD) site + 5);
    VirtualProtect(site, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), site, 5);
    logLine("click entry hook installed");
}






static void repressLost(DWORD now) {
    if ((gameLost.active || gameLost.releaseDue) && modHoldsButton(gameLost.type >= 3 ? 2 : 1)) {

        trace("click: the mod holds a unit with that button: the lost press is not pressed again");
        gameLost.active = gameLost.releaseDue = 0;
        return;
    }
    if (gameLost.releaseDue) {
        if (!clickFree(&gameLost.wait, now)) return;
        gameLost.releaseDue = 0;
        trace("click: release of the replayed press");
        replayClick(gameLost.type + 2, gameLost.x, gameLost.y, gameLost.flags);
        if (held.active) {

            heldRepressId = held.id;
            heldRepressWait = 0;
        }
        return;
    }
    if (!gameLost.active) return;
    if ((int) (now - gameLost.since) > 250) {
        trace("click: the game's lost press is 250 ms old: forgotten");
        gameLost.active = 0;
        return;
    }
    if (!clickFree(&gameLost.wait, now)) return;
    gameLost.active = 0;
    trace("click: %s press (%d) at %ld,%ld pressed again for the game", gameLost.type >= 3 ? "right" : "left",
        gameLost.type, gameLost.x, gameLost.y);
    replayClick(gameLost.type, gameLost.x, gameLost.y, gameLost.flags);
    if (gameLost.released) {
        gameLost.releaseDue = 1;
        gameLost.wait = 0;
    }
    else realHeld |= gameLost.type >= 3 ? 2 : 1;
}


static void pressWalk(void* player, DWORD now) {
    if (!clickFree(&walk.clickWait, now)) {
        if (!walk.pressPending) trace("walk: press waits for the next frame (the game took a click in this one)");
        walk.pressPending = 1;
        return;
    }
    if (walk.pressPending) trace("walk: press now (player mode %lu)", (unsigned long) playerMode());
    walk.pressPending = 0;
    long x, y;
    walkPoint(player, &x, &y);
    clickGround(0, x, y);
    walk.pressDx = walk.dx;
    walk.pressDy = walk.dy;
    walk.pressAt = now;
}










#define TURN_PRESS_MS  80
#define TURN_PRESS_COS 0.990

static int turnPressDue(DWORD now) {
    double dot = (double) walk.dx * walk.pressDx + (double) walk.dy * walk.pressDy;
    double lens = __builtin_sqrt(((double) walk.dx * walk.dx + (double) walk.dy * walk.dy)
        * ((double) walk.pressDx * walk.pressDx + (double) walk.pressDy * walk.pressDy));
    if (lens <= 0 || dot >= TURN_PRESS_COS * lens) return 0;
    if ((int) (now - walk.pressAt) < (dot < 0 ? TURN_PRESS_MS / 2 : TURN_PRESS_MS)) return 0;
    return !clickTakenNow() && !realHeld && !gameLost.active && !gameLost.releaseDue;
}








#define STOP_STEP_PX 24.0

static void endWalk(void) {
    if (!walk.active) return;
    walk.active = 0;
    if (walk.pressPending) {
        walk.pressPending = 0;
        return;
    }
    walk.stopping = 1;
    walk.clickWait = 0;
}

static void finishStop(void* player, DWORD now) {
    if (realHeld || gameLost.active || gameLost.releaseDue) {


        trace(walk.stopping == 1 ? "walk: stop dropped, the game's own press is held"
                                 : "walk: release of the stop left out, the game's own press is held");
        walk.stopping = 0;
        walk.clickWait = 0;
        return;
    }
    if (!clickFree(&walk.clickWait, now)) return;
    long x, y;

    double len = __builtin_sqrt((double) walk.dx * walk.dx + (double) walk.dy * walk.dy);
    if (len > STOP_STEP_PX) {
        walk.dx = (int) (walk.dx * STOP_STEP_PX / len);
        walk.dy = (int) (walk.dy * STOP_STEP_PX / len);
    }
    walkPoint(player, &x, &y);
    if (walk.stopping == 1) trace("walk: stop, last target %ld,%ld", x, y);
    clickGround(walk.stopping == 1 ? 0 : 2, x, y);
    walk.stopping = walk.stopping == 1 ? 2 : 0;
}

static void walkAction(void* player, int phase, int dx, int dy, DWORD now) {
    if (!unitActions) return;
    if (phase == PHASE_UP) {
        if (walk.active) trace("walk: stop (app), %d presses for turns", walk.turnPresses);
        endWalk();
        return;
    }
    walk.dx = dx;
    walk.dy = dy;
    walk.expireAt = now + 600;
    if (walk.active) return;
    trace("walk: start %d,%d (holding %lu, active %d)", dx, dy, (unsigned long) held.id, held.active);

    if (pendingPress.active) trace("press on %lu dropped (app: walk)", (unsigned long) pendingPress.id);
    pendingPress.active = 0;
    releaseHeld("app: walk");
    realHeld = 0;
    walk.stopping = 0;
    walk.active = 1;
    walk.pressPending = 0;
    walk.clickWait = 0;
    walk.turnPresses = 0;
    pressWalk(player, now);
}


static void updateWalk(void* player, DWORD now) {
    if (walk.stopping) {
        finishStop(player, now);
        return;
    }
    if (!walk.active) return;
    if ((int) (now - walk.expireAt) > 0) {
        trace("walk: stop (no news from the app for 600 ms)");
        endWalk();
        return;
    }
    if (walk.pressPending) {
        pressWalk(player, now);
        return;
    }
    if (turnPressDue(now)) {
        if ((double) walk.dx * walk.pressDx + (double) walk.dy * walk.pressDy < 0)
            trace("walk: turned back, pressed again");
        walk.turnPresses++;
        pressWalk(player, now);
        return;
    }
    long x, y;
    walkPoint(player, &x, &y);
    clickGround(1, x, y);
}




static int readsHelpLock(DWORD rva) {
    BYTE* at = (BYTE*) (client + rva);
    return at[0] == 0xA1 && *(DWORD*) (at + 1) == client + RVA_HelpButtonLock && at[5] == 0x85 && at[6] == 0xC0;
}


static void checkHudSites(void) {
    int ok = 1;
    for (int i = 0; i < HUD_SITES; i++) {
        BYTE* at = (BYTE*) (client + hudSites[i].rva);
        DWORD target = (DWORD) at + 5 + *(DWORD*) (at + 1);
        hudSites[i].verified = at[0] == 0xE8 && target == client + hudSites[i].target;
        if (hudSites[i].verified) memcpy(hudSites[i].original, at, 5);
        else if (i < 4) ok = 0;
    }
    hudAvailable = ok;
    duraAvailable = hudSites[4].verified;
    cursorAvailable = hudSites[5].verified && hudSites[6].verified;


    BYTE* helpCmp = (BYTE*) (client + RVA_DrawHelpButtonCall - 8);
    edgeAvailable = hudSites[7].verified && hudSites[8].verified && hudSites[9].verified && hudSites[10].verified
        && helpCmp[0] == 0x39 && helpCmp[1] == 0x1D
        && *(DWORD*) (helpCmp + 2) == client + RVA_UiVarHelpButton && helpCmp[6] == 0x74
        && readsHelpLock(RVA_HelpButtonDown) && readsHelpLock(RVA_HelpButtonUp);
    logLine(ok ? "HUD sites verified" : "HUD sites not as expected: own HUD off");
    logLine(cursorAvailable ? "pointer sites verified" : "pointer sites not as expected: pointer stays");
    logLine(edgeAvailable ? "help/quest button sites verified" : "help/quest button sites not as expected: they stay");
}

static void patchSite(int i, int hide);




static void applyEdgeButtons(int hide) {
    if (edgeAvailable) {
        for (int i = 7; i <= 10; i++) patchSite(i, hide);
    }
    edgeHidden = hide;
}

static void patchSite(int i, int hide) {
    BYTE* at = (BYTE*) (client + hudSites[i].rva);
    DWORD old;
    if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    memcpy(at, hide ? hudSites[i].replacement : hudSites[i].original, 5);
    VirtualProtect(at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 5);
}

static void applyCursor(int hide) {
    if (cursorAvailable) {
        patchSite(5, hide);
        patchSite(6, hide);
    }
    cursorHidden = hide;
}

static void applyDurability(int hide) {
    if (duraAvailable) patchSite(4, hide);
    duraHidden = hide;
}


static void applyHud(int hide) {
    for (int i = 0; i < 4; i++) {
        if (!hudSites[i].verified) continue;
        BYTE* at = (BYTE*) (client + hudSites[i].rva);
        DWORD old;
        if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) continue;
        memcpy(at, hide ? hudSites[i].replacement : hudSites[i].original, 5);
        VirtualProtect(at, 5, old, &old);
        FlushInstructionCache(GetCurrentProcess(), at, 5);
    }
    hudHidden = hide;
    logLine(hide ? "own HUD: statues and orbs left out" : "own HUD off: original statues and orbs");
}



static void checkPauseSite(void) {
    BYTE* at = (BYTE*) (client + RVA_DrawGameMenuCall);
    DWORD target = (DWORD) at + 5 + *(DWORD*) (at + 1);
    pauseAvailable = at[0] == 0xE8 && target == client + RVA_DrawGameMenu;
    if (pauseAvailable) memcpy(menuCallOriginal, at, 5);
    logLine(pauseAvailable ? "pause site verified" : "pause site not as expected: no pause");
}

static void setMenuHidden(int hide) {
    if (hide == menuHidden) return;
    BYTE* at = (BYTE*) (client + RVA_DrawGameMenuCall);
    static const BYTE nops[5] = {0x90, 0x90, 0x90, 0x90, 0x90};
    DWORD old;
    if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    memcpy(at, hide ? nops : menuCallOriginal, 5);
    VirtualProtect(at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 5);
    menuHidden = hide;
}








static void updatePause(void* player, DWORD now) {
    static DWORD lastCallAt;
    static void* lastPlayer;
    if (!pauseAvailable) return;
    DWORD* menu = (DWORD*) (client + RVA_UiVarGameMenu);

    if (pauseApplied && player && player == lastPlayer && now - lastCallAt > 1000) pauseRefreshedAt += now - lastCallAt;
    lastCallAt = now;
    lastPlayer = player;
    if (*menu == 0) menuHandedOver = 0;
    if (pauseWanted && (int) (now - pauseRefreshedAt) >= 5000) {

        pauseWanted = 0;
        if (pauseApplied) {
            setMenuHidden(0);
            pauseApplied = 0;
            menuHandedOver = !pauseOverOwnMenu;
            pauseOverOwnMenu = 0;
            logLine("no news from the app for 5 s: pause handed to the game menu (variable %lu)", (unsigned long) *menu);
        }
    }
    if (pauseWanted && player) {
        if (!pauseApplied) {



            endWalk();
            pendingPress.active = 0;
            releaseHeld("pause");
            pauseApplied = 1;

            pauseOverOwnMenu = *menu != 0 && !menuHandedOver;
            logLine(pauseOverOwnMenu ? "paused over the game's own menu" : "paused");
        }
        else if (*menu == 0) {

            pauseOverOwnMenu = 0;
            logLine("menu variable cleared while paused: set again");
        }

        *menu = 1;
        setMenuHidden(1);
    }
    else if (pauseApplied) {



        if (!pauseWanted && !pauseOverOwnMenu) *menu = 0;
        setMenuHidden(0);
        pauseApplied = 0;
        logLine(pauseOverOwnMenu && !pauseWanted ? "continued, the game's own menu stays open" : "continued");
        pauseOverOwnMenu = 0;
    }
}



static int readable(const void* p, UINT_PTR size);


static void checkClickFlagSite(void) {
    BYTE* at = (BYTE*) (client + RVA_ClickTakenCheck);
    clickFlagKnown = readable(at, 6) && at[0] == 0x39 && at[1] == 0x2D && *(DWORD*) (at + 2) == client + RVA_ClickTakenThisFrame;
    logLine(clickFlagKnown ? "click flag site verified" : "click flag site not as expected: presses are not delayed");
}


static void checkLabelsSite(void) {
    BYTE* at = (BYTE*) (client + RVA_GetUiVar_I + 0x24);
    labelsAvailable = readable(at, 7) && at[0] == 0x8B && at[1] == 0x04 && at[2] == 0x85 && *(DWORD*) (at + 3) == client + RVA_UiVars;
    logLine(labelsAvailable ? "item names site verified" : "item names site not as expected: the app holds Alt");

    BYTE* read = (BYTE*) (client + RVA_EscCloseListRead);
    DWORD* entry = (DWORD*) (client + RVA_EscCloseList) + UI_SHOWITEMS;
    escListAvailable = labelsAvailable && readable(read, 7) && read[0] == 0x39 && read[1] == 0x3C && read[2] == 0xB5
        && *(DWORD*) (read + 3) == client + RVA_EscCloseList && readable(entry, 4) && *entry == 1;
    logLine(escListAvailable ? "esc list site verified" : "esc list site not as expected: Esc stays blocked while item names are shown");
}





static void setEscClosesLabels(int closes) {
    if (!escListAvailable || escListPatched == !closes) return;
    DWORD* entry = (DWORD*) (client + RVA_EscCloseList) + UI_SHOWITEMS;
    DWORD old;
    if (!VirtualProtect(entry, 4, PAGE_READWRITE, &old)) {
        logLine("esc list: VirtualProtect failed");
        escListAvailable = 0;
        return;
    }
    *entry = closes ? 1 : 0;
    VirtualProtect(entry, 4, old, &old);
    escListPatched = !closes;
    logLine(closes ? "esc list: item names closed by Esc again" : "esc list: item names off the Esc list");
}







static void keepLabelTips(void) {
    BYTE* at = (BYTE*) (client + RVA_LabelsTipClearCall);
    BYTE* pushes = at - 0x1E;
    static const BYTE push3[6] = {0x6A, 0x00, 0x6A, 0x00, 0x6A, 0x00};
    int ok = readable(pushes, 0x1E + 5) && memcmp(pushes, push3, 6) == 0 && at[0] == 0xE8
        && (DWORD) at + 5 + *(DWORD*) (at + 1) == client + RVA_SetHoverTextThunk;
    if (ok) {
        static const BYTE pop3[5] = {0x83, 0xC4, 0x0C, 0x90, 0x90};
        DWORD old;
        if (VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) {
            memcpy(at, pop3, 5);
            VirtualProtect(at, 5, old, &old);
            FlushInstructionCache(GetCurrentProcess(), at, 5);
            labelTipsKept = 1;
        }
    }
    logLine(labelTipsKept ? "label tooltip site patched: tooltips stay with item names" : "label tooltip site not as expected: no tooltips with item names");
}


static void updateLabels(DWORD now) {
    if (!labelsAvailable) return;
    DWORD* var = (DWORD*) (client + RVA_UiVars) + UI_SHOWITEMS;

    static DWORD seen;
    if (*var != seen) trace("show-items %lu -> %lu (game)", (unsigned long) seen, (unsigned long) *var);
    int want = (int) (now - labelsUntil) < 0;
    if (want) {
        if (*var != 1) *var = 1;
        if (!labelsApplied) logLine("item names on");
        labelsApplied = 1;
        setEscClosesLabels(0);
    }
    else if (labelsApplied) {

        *var = (GetAsyncKeyState(VK_MENU) & 0x8000) ? 1 : 0;
        labelsApplied = 0;
        setEscClosesLabels(1);
        logLine("item names off");
    }
    seen = *var;
}



static int readable(const void* p, UINT_PTR size);


static void checkStaminaSite(void) {
    game = (DWORD) GetModuleHandleA("D2Game.dll");
    if (!game) return;
    staminaChecked = 1;
    BYTE* at = (BYTE*) (game + RVA_GameRunDrainCall);
    staminaAvailable = readable(at, 5) && at[0] == 0xE8 && (DWORD) at + 5 + *(DWORD*) (at + 1) == game + RVA_GameRunDrain;
    if (staminaAvailable) memcpy(drainCallOriginal, at, 5);
    logLine(staminaAvailable ? "stamina site verified" : "stamina site not as expected: no unlimited stamina");
}





static void applyStamina(int unlimited) {
    static const BYTE stillRunning[5] = {0xB8, 0x01, 0x00, 0x00, 0x00};
    const BYTE* code = unlimited ? stillRunning : drainCallOriginal;
    BYTE* at = (BYTE*) (game + RVA_GameRunDrainCall);
    DWORD old;
    if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    *(volatile WORD*) at = 0xFEEB;
    FlushInstructionCache(GetCurrentProcess(), at, 5);
    memcpy(at + 2, code + 2, 3);
    *(volatile WORD*) at = *(const WORD*) code;
    VirtualProtect(at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 5);
    staminaUnlimited = unlimited;
    logLine(unlimited ? "unlimited stamina on" : "unlimited stamina off: the game's stamina again");
}

static void updateStamina(DWORD now) {
    if (!staminaChecked && now - staminaCheckedAt > 1000) {
        staminaCheckedAt = now;
        checkStaminaSite();
    }
    if (!staminaAvailable) return;
    int want = (int) (now - staminaUntil) < 0;
    if (want != staminaUnlimited) applyStamina(want);
}






static int plugySaveChecked;
static int plugySavePatched;

static void updatePlugYSave(void) {
    static const BYTE site[] = {0x6A, 0x00, 0x8D, 0x6F, 0x0D, 0x55, 0xE8};
    if (plugySaveChecked || !plugyLoaded) return;
    DWORD gameBase = (DWORD) GetModuleHandleA("D2Game.dll");
    if (!gameBase) return;
    plugySaveChecked = 1;
    BYTE* at = (BYTE*) (gameBase + RVA_GameSaveFlag);
    DWORD old;
    if (!readable(at, sizeof(site)) || memcmp(at, site, sizeof(site)) != 0
        || !VirtualProtect(at + 1, 1, PAGE_EXECUTE_READWRITE, &old)) {
        logLine("PlugY save site not as expected: stash pages only saved on Save&Exit");
        return;
    }
    at[1] = 0x01;
    plugySavePatched = 1;
    VirtualProtect(at + 1, 1, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 2);
    logLine("PlugY save site patched: saving includes the stash pages");
}










#define RVA_PlugYWindowStartX 0x31B48
#define PLUGY_STATS_PAGE_WIDTH 320
static const DWORD plugyPageChecks[] = {0x197A8, 0x19910};
static int plugyPagesState;
static DWORD plugyPagesLimit = 0x190;
static DWORD plugyPagesFailed;
static int plugyPagesProtectWarned;

static void updatePlugYPages(void) {
    if (!plugyLoaded || plugyPagesState < 0) return;
    BYTE* plugy = (BYTE*) GetModuleHandleA("PlugY.dll");
    if (!plugy) return;
    if (plugyPagesState == 0) {
        static const BYTE cmpEdi[] = {0x81, 0xFF, 0x90, 0x01, 0x00, 0x00, 0x0F, 0x83};
        static const BYTE cmpEsi[] = {0x81, 0xFE, 0x29, 0x02, 0x00, 0x00, 0x0F, 0x83};
        int ok = readable(plugy + RVA_PlugYWindowStartX, 4);
        for (int i = 0; ok && i < 2; i++) {
            BYTE* at = plugy + plugyPageChecks[i];
            ok = readable(at, 20) && memcmp(at, cmpEdi, sizeof(cmpEdi)) == 0 && memcmp(at + 12, cmpEsi, sizeof(cmpEsi)) == 0;
        }
        plugyPagesState = ok ? 1 : -1;
        logLine(ok ? "PlugY page sites verified" : "PlugY page sites not as expected: extra stats pages only to x 400");
        if (!ok) return;
    }
    DWORD* startX = *(DWORD**) (plugy + RVA_PlugYWindowStartX);
    if (!startX || !readable(startX, 4) || *startX > 4000) return;
    DWORD limit = *startX + PLUGY_STATS_PAGE_WIDTH;





    int done = 1;
    for (int i = 0; i < 2; i++) {
        BYTE* at = plugy + plugyPageChecks[i] + 2;
        if (*(DWORD*) at == limit) continue;
        DWORD old;
        if (!VirtualProtect(at, 4, PAGE_EXECUTE_READWRITE, &old)) {
            done = 0;
            continue;
        }
        *(DWORD*) at = limit;
        DWORD ignored;
        if (!VirtualProtect(at, 4, old, &ignored) && !plugyPagesProtectWarned) {
            plugyPagesProtectWarned = 1;
            logLine("PlugY stats pages: page protection not restored (the site stays writable)");
        }
        FlushInstructionCache(GetCurrentProcess(), at, 4);
    }
    if (!done) {
        if (plugyPagesFailed != limit) logLine("PlugY stats pages: write for x %lu failed, trying again", limit);
        plugyPagesFailed = limit;
        return;
    }
    plugyPagesFailed = 0;
    if (limit == plugyPagesLimit) return;
    plugyPagesLimit = limit;
    logLine("PlugY stats pages take clicks up to x %lu", limit);
}











typedef void* (__stdcall *GetLevel_t)(void*, DWORD);
typedef void (__stdcall *InitLevel_t)(void*);
typedef void (__stdcall *RoomData_t)(void*, DWORD, DWORD, DWORD, void*);
typedef BYTE* (__fastcall *GetLevelDef_t)(DWORD);
typedef BYTE* (__stdcall *GetLevelText_t)(DWORD);
typedef BYTE* (__stdcall *GetObjectTxt_t)(DWORD);
typedef void (__stdcall *RevealRoom_t)(void*, DWORD, void*);
typedef BYTE* (__stdcall *NewCell_t)(void);
typedef void (__fastcall *AddCell_t)(void*, void*);
typedef void (__fastcall *DrawText_t)(const wchar_t*, int, int, int, int);
typedef int (__fastcall *TextWidth_t)(const wchar_t*);
typedef int (__fastcall *SetTextSize_t)(int);
static GetLevel_t fGetLevel;
static InitLevel_t fInitLevel;
static RoomData_t fAddRoomData, fRemoveRoomData;
static GetLevelDef_t fGetLevelDef;
static GetLevelText_t fGetLevelText;
static GetObjectTxt_t fGetObjectTxt;
static RevealRoom_t fRevealAutomapRoom;
static NewCell_t fNewAutomapCell;
static AddCell_t fAddAutomapCell;
static DrawText_t fDrawText;
static TextWidth_t fGetTextWidth;
static SetTextSize_t fSetTextSize;

static DWORD* objectTxtCount;
static int mapAvailable, mapShown;
static DWORD mapUntil, mapTick;

#define LABEL_ENTRANCE 0
#define LABEL_PASSAGE  1
#define LABEL_WAYPOINT 2
#define MAX_LABELS 160
typedef struct {
    WORD from, to;
    BYTE kind;
    DWORD layer;
    long sumX, sumY, weight;
    long minX, minY, maxX, maxY;
    const wchar_t* name;
    signed char outX, outY;
} MapLabel;
static MapLabel labels[MAX_LABELS];
static int labelCount;

static MapLabel exits[MAX_LABELS];
static int exitCount;

#include "d2reveal.c"







static struct {
    void* act;
    DWORD seed, first, end, since, ticks, levelTicks[REVEAL_LEVELS];
    unsigned long long spent, levelSpent[REVEAL_LEVELS], lastTickUs, lastOwnUs;
    int paceReady, settled;
    RevealPlan plan;
    RevealPace pace;
} reveal;


static unsigned long long revealNowUs(void) {
    static LARGE_INTEGER freq;
    LARGE_INTEGER count;
    if (!freq.QuadPart && (!QueryPerformanceFrequency(&freq) || !freq.QuadPart)) freq.QuadPart = 1;
    if (freq.QuadPart == 1 || !QueryPerformanceCounter(&count)) return (unsigned long long) GetTickCount() * 1000;
    unsigned long long c = (unsigned long long) count.QuadPart, f = (unsigned long long) freq.QuadPart;
    return c / f * 1000000 + c % f * 1000000 / f;
}


static int revealDone(void) {
    return reveal.act && reveal.settled;
}

static int bytesAre(DWORD at, const BYTE* expected, int length) {
    return readable((void*) at, length) && memcmp((void*) at, expected, length) == 0;
}


static int addressAt(DWORD module, DWORD rva, DWORD target) {
    return readable((void*) (module + rva), 4) && *(DWORD*) (module + rva) == module + target;
}

static int callAt(DWORD module, DWORD rva, DWORD target) {
    BYTE* at = (BYTE*) (module + rva);
    return readable(at, 5) && at[0] == 0xE8 && (DWORD) at + 5 + *(DWORD*) (at + 1) == module + target;
}

static void* initLayer(DWORD layerNo) {
    DWORD a = layerNo;
    __asm__ volatile ("call *%1" : "+a"(a) : "r"(client + RVA_InitAutomapLayer) : "ecx", "edx", "memory", "cc");
    return (void*) a;
}





static const wchar_t* levelName(DWORD levelNo) {
    BYTE* txt = fGetLevelText(levelNo);
    return readable(txt, LTXT_NAME + 2) ? (const wchar_t*) (txt + LTXT_NAME) : NULL;
}

static DWORD levelLayer(DWORD levelNo) {
    BYTE* def = fGetLevelDef(levelNo);
    return def ? RD32(def, 8) : 0xFFFFFFFF;
}

static void __cdecl automapDrawn(void);


static void checkMapSites(void) {
    HMODULE win = GetModuleHandleA("D2Win.dll");
    fGetLevel = (GetLevel_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetLevel);
    fInitLevel = (InitLevel_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_InitLevel);
    fAddRoomData = (RoomData_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_AddRoomData);
    fRemoveRoomData = (RoomData_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_RemoveRoomData);
    fGetLevelDef = (GetLevelDef_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetLevelDef);
    fGetLevelText = (GetLevelText_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetLevelText);
    fGetObjectTxt = (GetObjectTxt_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetObjectTxt);
    fDrawText = win ? (DrawText_t) GetProcAddress(win, (LPCSTR) ORD_DrawText) : NULL;
    fGetTextWidth = win ? (TextWidth_t) GetProcAddress(win, (LPCSTR) ORD_GetTextWidth) : NULL;
    fSetTextSize = win ? (SetTextSize_t) GetProcAddress(win, (LPCSTR) ORD_SetTextSize) : NULL;
    fRevealAutomapRoom = (RevealRoom_t) (client + RVA_RevealAutomapRoom);
    fNewAutomapCell = (NewCell_t) (client + RVA_NewAutomapCell);
    fAddAutomapCell = (AddCell_t) (client + RVA_AddAutomapCell);
    static const BYTE getLevel[] = {0x8B, 0x4C, 0x24, 0x04, 0x8B, 0x81, 0x7C, 0x04, 0x00, 0x00};
    static const BYTE initLevel[] = {0x56, 0x8B, 0x74, 0x24, 0x08, 0x8B, 0x86, 0xB4, 0x01, 0x00, 0x00};
    static const BYTE addRoom[] = {0x8B, 0x54, 0x24, 0x04, 0x85, 0xD2};
    static const BYTE removeRoom[] = {0x8B, 0x4C, 0x24, 0x04, 0x85, 0xC9};
    static const BYTE levelDef[] = {0x8B, 0xC1, 0x85, 0xC0, 0x7C}, levelDefSize[] = {0x69, 0xC0, 0x9C, 0x00, 0x00, 0x00};
    static const BYTE levelText[] = {0x8B, 0x44, 0x24, 0x04, 0x85, 0xC0, 0x7E}, levelTextSize[] = {0x69, 0xC0, 0x20, 0x02, 0x00, 0x00};
    static const BYTE objectTxt[] = {0x8B, 0x44, 0x24, 0x04, 0x3B, 0x05}, objectTxtSize[] = {0x69, 0xC0, 0xC0, 0x01, 0x00, 0x00};
    static const BYTE drawText[] = {0x53, 0x8B, 0x5C, 0x24, 0x10, 0x56, 0x33, 0xC0};
    static const BYTE textWidth[] = {0x56, 0x8B, 0xF1, 0xFF, 0x15};
    static const BYTE textSize[] = {0x83, 0xEC, 0x5C, 0x53, 0x55, 0x57};
    static const BYTE revealRoom[] = {0x51, 0x53, 0x55, 0x56, 0x8B, 0x74, 0x24, 0x14, 0x33, 0xED, 0x57};
    static const BYTE addCell[] = {0x51, 0x53, 0x55, 0x56, 0x8B, 0xEA, 0x8B, 0x75, 0x00, 0x33, 0xDB};

    int checks[] = {
        fGetLevel && bytesAre((DWORD) fGetLevel, getLevel, sizeof(getLevel)),
        fInitLevel && bytesAre((DWORD) fInitLevel, initLevel, sizeof(initLevel)),
        fAddRoomData && bytesAre((DWORD) fAddRoomData, addRoom, sizeof(addRoom)),
        fRemoveRoomData && bytesAre((DWORD) fRemoveRoomData, removeRoom, sizeof(removeRoom)),
        fGetLevelDef && bytesAre((DWORD) fGetLevelDef, levelDef, sizeof(levelDef))
            && bytesAre((DWORD) fGetLevelDef + 0x1A, levelDefSize, sizeof(levelDefSize)),
        fGetLevelText && bytesAre((DWORD) fGetLevelText, levelText, sizeof(levelText))
            && bytesAre((DWORD) fGetLevelText + 0x1C, levelTextSize, sizeof(levelTextSize)),
        fGetObjectTxt && bytesAre((DWORD) fGetObjectTxt, objectTxt, sizeof(objectTxt))
            && bytesAre((DWORD) fGetObjectTxt + 0x3C, objectTxtSize, sizeof(objectTxtSize)),


        fDrawText && (bytesAre((DWORD) fDrawText, drawText, sizeof(drawText))
            || (RD8(fDrawText, 0) == 0xE9 && bytesAre((DWORD) fDrawText + 5, drawText + 5, sizeof(drawText) - 5))),
        fGetTextWidth && bytesAre((DWORD) fGetTextWidth, textWidth, sizeof(textWidth)),
        fSetTextSize && bytesAre((DWORD) fSetTextSize, textSize, sizeof(textSize)),
    };
    const void* checked[] = {fGetLevel, fInitLevel, fAddRoomData, fRemoveRoomData, fGetLevelDef, fGetLevelText, fGetObjectTxt, fDrawText, fGetTextWidth, fSetTextSize};
    int functions = 1, failed = 0;
    for (unsigned i = 0; i < sizeof(checks) / sizeof(checks[0]); i++) {
        if (!checks[i]) {
            const BYTE* b = (const BYTE*) checked[i];
            functions = 0;
            failed |= 1 << i;
            if (readable(b, 8)) logLine("map hack: function %u at %p starts %02X %02X %02X %02X %02X %02X %02X %02X", i, b,
                b[0], b[1], b[2], b[3], b[4], b[5], b[6], b[7]);
        }
    }
    int client1 = bytesAre(client + RVA_RevealAutomapRoom, revealRoom, sizeof(revealRoom))
        && bytesAre(client + RVA_AddAutomapCell, addCell, sizeof(addCell))
        && RD8(client, RVA_InitAutomapLayer) == 0x56 && addressAt(client, RVA_InitAutomapLayer + 3, RVA_AutomapLayerList)
        && addressAt(client, RVA_InitAutomapLayer + 0x25, RVA_AutomapLayer)
        && RD8(client, RVA_NewAutomapCell) == 0x8B && addressAt(client, RVA_NewAutomapCell + 2, RVA_AutomapCellPool);

    int client2 = callAt(client, RVA_DrawAutomapCall, RVA_DrawAutomap)
        && addressAt(client, 0x70D6C, RVA_AutomapDivisor) && addressAt(client, 0x70D7C, RVA_AutomapOffsetX)
        && addressAt(client, 0x70D8E, RVA_AutomapOffsetY) && addressAt(client, 0x70E60, RVA_AutomapMode)
        && addressAt(client, 0x70F4A, RVA_AutomapCorner) && addressAt(client, 0x70F54, RVA_AutomapCornerTop)
        && addressAt(client, 0x737F1, RVA_AutomapLayer);
    if (functions) objectTxtCount = *(DWORD**) ((BYTE*) fGetObjectTxt + 6);
    if (!functions || !client1 || !client2 || !readable(objectTxtCount, 4)) {
        logLine("map hack sites not as expected (functions failed %X, D2Client %d/%d): no map hack", failed, client1, client2);
        return;
    }

    BYTE* at = (BYTE*) (client + RVA_DrawAutomapCall);
    DWORD old;
    if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    *(DWORD*) (at + 1) = (DWORD) automapDrawn - ((DWORD) at + 5);
    VirtualProtect(at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 5);
    mapAvailable = 1;
    logLine("map hack sites verified, automap drawing hooked (%lu objects)", (unsigned long) *objectTxtCount);
}





static MapLabel* addLabel(DWORD from, DWORD to, int kind, long x, long y, long weight, long x0, long y0, long x1, long y1,
                          DWORD layer, const wchar_t* name) {
    if (!name || !name[0] || weight <= 0) return NULL;
    long gap = kind == LABEL_PASSAGE ? 12 : 3;
    for (int i = 0; i < labelCount; i++) {
        MapLabel* l = &labels[i];
        if (l->from != from || l->to != to || l->kind != kind) continue;
        if (x0 > l->maxX + gap || x1 < l->minX - gap || y0 > l->maxY + gap || y1 < l->minY - gap) continue;
        l->sumX += x * weight;
        l->sumY += y * weight;
        l->weight += weight;
        if (x0 < l->minX) l->minX = x0;
        if (y0 < l->minY) l->minY = y0;
        if (x1 > l->maxX) l->maxX = x1;
        if (y1 > l->maxY) l->maxY = y1;
        return l;
    }
    if (labelCount >= MAX_LABELS) return NULL;
    MapLabel* l = &labels[labelCount++];
    l->from = (WORD) from;
    l->to = (WORD) to;
    l->kind = (BYTE) kind;
    l->layer = layer;
    l->sumX = x * weight;
    l->sumY = y * weight;
    l->weight = weight;
    l->minX = x0;
    l->minY = y0;
    l->maxX = x1;
    l->maxY = y1;
    l->name = name;
    l->outX = l->outY = 0;
    return l;
}


static void addCell(void* layer, DWORD cellNo, long x, long y) {
    BYTE* cell = fNewAutomapCell();
    if (!cell) return;
    *(WORD*) (cell + 4) = (WORD) cellNo;
    *(WORD*) (cell + 6) = (WORD) (((x - y) * 16) / 10 + 1);
    *(WORD*) (cell + 8) = (WORD) (((x + y) * 8) / 10 - 3);
    fAddAutomapCell(cell, (BYTE*) layer + 0x10);
}


static DWORD warpDestination(void* room2, DWORD txt) {
    DWORD first = 0;
    int guard = 0;
    for (void* t = RDP(room2, ROOM2_TILES); readable(t, 0x14) && guard < 16; t = RDP(t, 4), guard++) {
        void* dest = RDP(t, 0);
        void* level = readable(dest, 0x60) ? RDP(dest, ROOM2_LEVEL) : NULL;
        DWORD no = readable(level, LEVEL_NO + 4) ? RD32(level, LEVEL_NO) : 0;
        if (!first) first = no;
        DWORD* number = (DWORD*) RDP(t, 0x10);
        if (readable(number, 4) && *number == txt) return no;
    }
    return guard == 1 ? first : 0;
}


static void revealPresets(void* room2, DWORD levelNo, void* layer, DWORD layerNo) {
    long baseX = (long) RD32(room2, ROOM2_X) * 5, baseY = (long) RD32(room2, ROOM2_Y) * 5;
    int guard = 0;
    for (void* preset = RDP(room2, ROOM2_PRESETS); readable(preset, 0x1C) && guard < 512; preset = RDP(preset, 0x0C), guard++) {
        DWORD type = RD32(preset, 0x14), txt = RD32(preset, 0x04);
        long x = baseX + (long) RD32(preset, 0x08), y = baseY + (long) RD32(preset, 0x18);
        if (type == UNIT_OBJECT && txt < *objectTxtCount) {
            BYTE* objTxt = fGetObjectTxt(txt);
            if (!readable(objTxt, 0x1C0)) continue;
            DWORD cellNo = RD32(objTxt, OTXT_AUTOMAP);

            if (txt == 267 || txt == 366 || (txt == 402 && levelNo != 74)) cellNo = 0;
            if (cellNo > 0 && cellNo < 1258) addCell(layer, cellNo, x, y);
            if (objTxt[OTXT_SUBCLASS] & 64) addLabel(levelNo, 0, LABEL_WAYPOINT, x, y, 1, x, y, x, y, layerNo, (const wchar_t*) (objTxt + OTXT_NAME));

            if (objTxt[OTXT_SUBCLASS] & 32) trace("well preset: level %lu txt %lu at %ld,%ld", (unsigned long) levelNo, (unsigned long) txt, x, y);
        }
        else if (type == UNIT_TILE) {
            DWORD dest = warpDestination(room2, txt);
            if (dest && dest != levelNo) addLabel(levelNo, dest, LABEL_ENTRANCE, x, y, 1, x, y, x, y, layerNo, levelName(dest));
        }
    }
}


static int walkable(void* room1, long x, long y) {
    BYTE* grid = RDP(room1, ROOM1_COLL);
    long rx = (long) RD32(room1, ROOM1_SUBX), ry = (long) RD32(room1, ROOM1_SUBX + 4);
    long w = (long) RD32(room1, ROOM1_SUBX + 8), h = (long) RD32(room1, ROOM1_SUBX + 12);
    if (!grid || x < rx || y < ry || x >= rx + w || y >= ry + h) return 0;
    WORD* flags = (WORD*) RDP(grid, 0x20);
    long stride = (long) RD32(grid, 8);
    return flags && !(flags[(y - ry) * stride + (x - rx)] & COLL_BLOCKED);
}

static int gridReadable(void* room1) {
    if (!readable(room1, ROOM1_SUBX + 16)) return 0;
    BYTE* grid = RDP(room1, ROOM1_COLL);
    if (!readable(grid, 0x24)) return 0;
    long stride = (long) RD32(grid, 8), h = (long) RD32(room1, ROOM1_SUBX + 12);
    return stride > 0 && stride >= (long) RD32(room1, ROOM1_SUBX + 8) && h > 0 && stride * h < 0x40000
        && readable(RDP(grid, 0x20), stride * h * 2);
}

#include "d2crossing.c"






static void borderPassages(void* act, void* room2, DWORD levelNo, DWORD layerNo) {
    void** around = (void**) RDP(room2, ROOM2_NEAR);
    DWORD count = RD32(room2, ROOM2_NNEAR);
    void* room1 = RDP(room2, ROOM2_ROOM1);
    if (!around || count == 0 || count > 64 || !readable(around, count * 4) || !gridReadable(room1)) return;
    long rx = (long) RD32(room2, ROOM2_X), ry = (long) RD32(room2, ROOM2_Y);
    long rw = (long) RD32(room2, ROOM2_W), rh = (long) RD32(room2, ROOM2_H);
    for (DWORD i = 0; i < count; i++) {
        void* other = around[i];
        if (other == room2 || !readable(other, 0x60)) continue;
        void* otherLevel = RDP(other, ROOM2_LEVEL);
        DWORD to = readable(otherLevel, LEVEL_NO + 4) ? RD32(otherLevel, LEVEL_NO) : 0;
        if (to == levelNo || to == 0 || to > 136) continue;
        long nx = (long) RD32(other, ROOM2_X), ny = (long) RD32(other, ROOM2_Y);
        long nw = (long) RD32(other, ROOM2_W), nh = (long) RD32(other, ROOM2_H);

        int vertical;
        long inside, outside, from, end;
        if (rx + rw == nx || nx + nw == rx) {
            vertical = 1;
            inside = rx + rw == nx ? nx * 5 - 1 : rx * 5;
            outside = rx + rw == nx ? nx * 5 : rx * 5 - 1;
            from = (ry > ny ? ry : ny) * 5;
            end = (ry + rh < ny + nh ? ry + rh : ny + nh) * 5;
        }
        else if (ry + rh == ny || ny + nh == ry) {
            vertical = 0;
            inside = ry + rh == ny ? ny * 5 - 1 : ry * 5;
            outside = ry + rh == ny ? ny * 5 : ry * 5 - 1;
            from = (rx > nx ? rx : nx) * 5;
            end = (rx + rw < nx + nw ? rx + rw : nx + nw) * 5;
        }
        else continue;
        if (end <= from) continue;
        int added = 0;
        if (!RDP(other, ROOM2_ROOM1)) {
            fAddRoomData(act, to, (DWORD) nx, (DWORD) ny, NULL);
            added = 1;
        }
        void* otherRoom1 = RDP(other, ROOM2_ROOM1);
        if (gridReadable(otherRoom1)) {
            BYTE* hereGrid = RDP(room1, ROOM1_COLL), * thereGrid = RDP(otherRoom1, ROOM1_COLL);
            CrossRoom here = {(const unsigned short*) RDP(hereGrid, 0x20), (long) RD32(hereGrid, 8), (long) RD32(room1, ROOM1_SUBX),
                (long) RD32(room1, ROOM1_SUBX + 4), (long) RD32(room1, ROOM1_SUBX + 8), (long) RD32(room1, ROOM1_SUBX + 12), COLL_BLOCKED};
            CrossRoom there = {(const unsigned short*) RDP(thereGrid, 0x20), (long) RD32(thereGrid, 8),
                (long) RD32(otherRoom1, ROOM1_SUBX), (long) RD32(otherRoom1, ROOM1_SUBX + 4),
                (long) RD32(otherRoom1, ROOM1_SUBX + 8), (long) RD32(otherRoom1, ROOM1_SUBX + 12), COLL_BLOCKED};
            long run = -1;
            for (long t = from; t <= end; t++) {
                int open = t < end && (vertical ? walkable(room1, inside, t) && walkable(otherRoom1, outside, t)
                    : walkable(room1, t, inside) && walkable(otherRoom1, t, outside));
                if (open && run < 0) run = t;
                if (!open && run >= 0) {
                    long length = t - run, middle = run + length / 2;
                    int blocked = length < 2 ? -1 : crossingBlocked(&here, &there, vertical, inside, outside, run, t - 1);
                    if (blocked > 0)
                        logLine("map: border %lu -> %lu at %s %ld, %ld-%ld open but %s: no passage", (unsigned long) levelNo,
                            (unsigned long) to, vertical ? "x" : "y", inside, run, t - 1, blocked == 1 ? "no way on behind it" : "a pocket on this side");
                    MapLabel* l = blocked != 0 ? NULL : addLabel(levelNo, to, LABEL_PASSAGE, vertical ? inside : middle,
                        vertical ? middle : inside, length, vertical ? inside : run, vertical ? run : inside,
                        vertical ? inside : t - 1, vertical ? t - 1 : inside, layerNo, levelName(to));
                    if (l) {
                        l->outX = (signed char) (vertical ? (outside > inside ? 1 : -1) : 0);
                        l->outY = (signed char) (vertical ? 0 : (outside > inside ? 1 : -1));
                    }
                    run = -1;
                }
            }
        }
        if (added) fRemoveRoomData(act, to, (DWORD) nx, (DWORD) ny, RDP(other, ROOM2_ROOM1));
    }
}








static int revealRooms(void* act, DWORD levelNo, int* next, unsigned long long start, unsigned budget) {
    void* misc = RDP(act, ACT_MISC);
    if (!readable(misc, MISC_LEVELS + 4)) return 1;
    BYTE* level = fGetLevel(misc, levelNo);
    if (!readable(level, LEVEL_NO + 4) || RD32(level, LEVEL_NO) != levelNo) return 1;
    if (!RDP(level, LEVEL_ROOM2)) fInitLevel(level);
    DWORD layerNo = levelLayer(levelNo);
    void* layer = layerNo != 0xFFFFFFFF ? initLayer(layerNo) : NULL;
    if (!layer) return 1;
    int index = 0, from = *next;
    for (void* room2 = RDP(level, LEVEL_ROOM2); readable(room2, 0x60) && index < 2048; room2 = RDP(room2, ROOM2_NEXT), index++) {
        if (index < from) continue;
        if (index > from && revealNowUs() - start >= budget) return 0;
        DWORD x = RD32(room2, ROOM2_X), y = RD32(room2, ROOM2_Y);
        int added = 0;
        if (!RDP(room2, ROOM2_ROOM1)) {
            fAddRoomData(act, levelNo, x, y, NULL);
            added = 1;
        }
        void* room1 = RDP(room2, ROOM2_ROOM1);
        if (room1) {
            fRevealAutomapRoom(room1, 1, layer);
            revealPresets(room2, levelNo, layer, layerNo);
            borderPassages(act, room2, levelNo, layerNo);
        }
        if (added) fRemoveRoomData(act, levelNo, x, y, RDP(room2, ROOM2_ROOM1));
        *next = index + 1;
    }
    return 1;
}

static int actOf(DWORD levelNo, DWORD* first, DWORD* end) {
    static const DWORD start[] = {1, 40, 75, 103, 109, 137};
    for (int i = 0; i < 5; i++) {
        if (levelNo >= start[i] && levelNo < start[i + 1]) {
            *first = start[i];
            *end = start[i + 1];
            return 1;
        }
    }
    return 0;
}






static void mergePassages(DWORD levelNo) {
    for (int i = 0; i < labelCount; i++) {
        MapLabel* a = &labels[i];
        if (a->kind != LABEL_PASSAGE || a->from != levelNo) continue;
        for (int j = i + 1; j < labelCount; j++) {
            MapLabel* b = &labels[j];
            if (b->kind != LABEL_PASSAGE || b->from != a->from || b->to != a->to
                || labs(a->sumX / a->weight - b->sumX / b->weight) > 150
                || labs(a->sumY / a->weight - b->sumY / b->weight) > 150) continue;
            a->sumX += b->sumX;
            a->sumY += b->sumY;
            a->weight += b->weight;
            if (b->minX < a->minX) a->minX = b->minX;
            if (b->minY < a->minY) a->minY = b->minY;
            if (b->maxX > a->maxX) a->maxX = b->maxX;
            if (b->maxY > a->maxY) a->maxY = b->maxY;
            labels[j] = labels[--labelCount];
            j = i;
        }
    }
}

static int singlePlayerGame(void);






static void updateMap(void* player, DWORD now) {
    if (!mapAvailable) return;

    unsigned long long tickUs = revealNowUs();
    if (!reveal.paceReady) {
        char fps[16] = "";
        DWORD n = GetEnvironmentVariableA("DXVK_FRAME_RATE", fps, sizeof(fps));
        revealPaceStart(&reveal.pace, n > 0 && n < sizeof(fps) ? revealFpsFromText(fps) : 0);
        reveal.paceReady = 1;
        logLine("map: reveal budget %u%% of the frame, frame rate limit %u fps", REVEAL_SHARE_PCT, reveal.pace.limitFps);
    }
    else {
        unsigned long long gap = tickUs - reveal.lastTickUs;
        revealPaceNote(&reveal.pace, gap < 0xFFFFFFFFull ? (unsigned) gap : 0xFFFFFFFFu, (unsigned) reveal.lastOwnUs);
    }
    reveal.lastTickUs = tickUs;
    reveal.lastOwnUs = 0;
    int want = (int) (now - mapUntil) < 0;
#ifdef D2MOD_MAPHACK_ALWAYS
    want = 1;
#endif
    if (!singlePlayerGame()) want = 0;
    mapShown = want;

    if ((int) (now - playtestUntil) < 0 && singlePlayerGame()) want = 1;

    if (now - mapTick > 2000) reveal.act = NULL;
    mapTick = now;
    void* act = player ? RDP(player, U_ACT) : NULL;
    DWORD levelNo = player ? unitLevel(player) : 0, first, end;
    if (!readable(act, ACT_MISC + 4) || !actOf(levelNo, &first, &end)) return;
    DWORD seed = RD32(act, ACT_SEED);
    if (act != reveal.act || seed != reveal.seed || first != reveal.first) {
        reveal.act = act;
        reveal.seed = seed;
        reveal.first = first;
        reveal.end = end;
        reveal.since = now;
        reveal.spent = reveal.ticks = 0;
        revealPlanStart(&reveal.plan, first, end);
        for (DWORD lv = first; lv < end && lv - first < REVEAL_LEVELS; lv++) {
            reveal.plan.layer[lv - first] = levelLayer(lv);
            reveal.levelSpent[lv - first] = 0;
            reveal.levelTicks[lv - first] = 0;
        }
        reveal.settled = 0;
        labelCount = 0;
        exitCount = 0;
    }

    void* shown = RDP(client, RVA_AutomapLayer);
    if (!want || revealPlanFinished(&reveal.plan) || now - reveal.since < 1500 || !readable(shown, 4)) return;
    DWORD shownNo = RD32(shown, 0);
    unsigned long long start = revealNowUs();
    unsigned budget = revealBudgetUs(&reveal.pace);
    int worked = 0;
    while (revealNowUs() - start < budget) {
        DWORD lv = revealPlanLevel(&reveal.plan, levelNo, shownNo);
        if (!lv) break;
        int i = (int) (lv - reveal.first);

        if (worked && reveal.plan.room[i] == 0) break;
        if (!worked) reveal.ticks++;
        worked = 1;
        unsigned long long t = revealNowUs();
        int done = revealRooms(act, lv, &reveal.plan.room[i], start, budget);
        reveal.levelSpent[i] += revealNowUs() - t;
        reveal.levelTicks[i]++;
        if (!done) break;

        for (int k = 0; k < labelCount && exitCount < MAX_LABELS; k++)
            if (labels[k].from == lv) exits[exitCount++] = labels[k];
        mergePassages(lv);
        trace("map: level %lu, %d rooms, %lu ms in %lu ticks, %d labels", (unsigned long) lv, reveal.plan.room[i],
            (unsigned long) (reveal.levelSpent[i] / 1000), (unsigned long) reveal.levelTicks[i], labelCount);
        revealPlanLevelDone(&reveal.plan, lv);
    }
    if (worked) {

        if (readable(RDP(client, RVA_AutomapLayer), 4) && RD32(RDP(client, RVA_AutomapLayer), 0) != shownNo) initLayer(shownNo);
        reveal.lastOwnUs = revealNowUs() - start;
        reveal.spent += reveal.lastOwnUs;
    }
    int settled = revealPlanSettled(&reveal.plan, levelNo, shownNo);
    if (settled && !reveal.settled) {
        int waiting = 0;
        for (DWORD lv = reveal.first; lv < reveal.end; lv++) waiting += !revealPlanIsDone(&reveal.plan, lv);
        logLine("map: act with levels %lu-%lu revealed on layer %lu in %lu ms (%lu ticks, %lu ms after the act came; frame %lu us, "
            "budget %u us), %d labels, %d levels wait for the player in their own layer", (unsigned long) reveal.first,
            (unsigned long) (reveal.end - 1), (unsigned long) shownNo, (unsigned long) (reveal.spent / 1000),
            (unsigned long) reveal.ticks, (unsigned long) (now - reveal.since), (unsigned long) revealFrameUs(&reveal.pace),
            revealBudgetUs(&reveal.pace), labelCount, waiting);
        for (int i = 0; i < labelCount; i++) {
            MapLabel* l = &labels[i];
            if (l->layer != shownNo) continue;
            char name[64];
            int n = WideCharToMultiByte(CP_UTF8, 0, l->name, -1, name, sizeof(name), NULL, NULL);
            if (n <= 0) name[0] = 0;
            logLine("  label %d: level %u -> %u at %ld,%ld (layer %lu, weight %ld) %s", l->kind, l->from, l->to,
                l->sumX / l->weight, l->sumY / l->weight, (unsigned long) l->layer, l->weight, name);
        }
    }
    reveal.settled = settled;
}


static int hasReverse(const MapLabel* label) {
    for (int i = 0; i < labelCount; i++) {
        const MapLabel* l = &labels[i];
        if (l->kind == LABEL_PASSAGE && l->from == label->to && l->to == label->from
            && labs(l->sumX / l->weight - label->sumX / label->weight) < 60
            && labs(l->sumY / l->weight - label->sumY / label->weight) < 60) return 1;
    }
    return 0;
}






static void drawLabels(void) {
    void* layer = RDP(client, RVA_AutomapLayer);
    void* player = fGetPlayerUnit();
    long divisor = (long) RD32(client, RVA_AutomapDivisor);
    if (!readable(layer, 4) || !player || divisor <= 0 || labelCount == 0) return;
    DWORD shown = RD32(layer, 0), here = unitLevel(player);
    long offX = (long) RD32(client, RVA_AutomapOffsetX), offY = (long) RD32(client, RVA_AutomapOffsetY);
    long screenW = (long) RD32(client, RVA_ScreenSizeX), screenH = (long) RD32(client, RVA_ScreenSizeY);
    long left = 0, top = 0, right = screenW - 1, bottom = screenH - 1;
    int mini = RD32(client, RVA_AutomapMode) == 1;
    if (mini) {
        if (RD32(client, RVA_AutomapCorner)) {
            left = 0;
            top = RD32(client, RVA_AutomapCornerTop) == 2 ? -0x15 : 0x4B;
        }
        else {
            left = screenW - 0x119;
            top = 0x39;
        }
        right = left + 0x117;
        bottom = top + 0xE1;
    }
    int oldFont = fSetTextSize(mini ? 6 : 1);
    for (int i = 0; i < labelCount; i++) {
        MapLabel* l = &labels[i];
        if (l->layer != shown) continue;
        if (l->kind == LABEL_PASSAGE && l->from != here && (l->to == here || l->from > l->to) && hasReverse(l)) continue;
        long x = l->sumX / l->weight, y = l->sumY / l->weight;
        long sx = (((x - y) * 16) / 10 + 1) * 10 / divisor - offX;
        long sy = (((x + y) * 8) / 10 - 3) * 10 / divisor - offY;
        int width = fGetTextWidth(l->name);
        long tx = sx - width / 2, ty = sy + (mini ? 3 : 6);
        if (tx < left || tx + width > right || ty - (mini ? 7 : 14) < top || ty > bottom) continue;

        fDrawText(l->name, (int) tx + 1, (int) ty + 1, 6, 0);
        fDrawText(l->name, (int) tx, (int) ty, l->kind == LABEL_WAYPOINT ? 3 : 4, 0);
    }
    fSetTextSize(oldFont);
}


__attribute__((force_align_arg_pointer)) static void __cdecl automapDrawn(void) {
    ((void (__cdecl *)(void)) (client + RVA_DrawAutomap))();
    if (mapShown) drawLabels();
}



static void checkSoundSites(void) {
    static const BYTE useSound[] = {0x57, 0x33, 0xFF, 0x85, 0xF6};
    BYTE* use = (BYTE*) (client + RVA_GetItemUseSound);
    BYTE* play = (BYTE*) (client + RVA_PlaySound);
    soundAvailable = memcmp(use, useSound, sizeof(useSound)) == 0 && play[0] == 0xA1
        && *(DWORD*) (play + 1) == client + RVA_SoundCheckVar;
    logLine(soundAvailable ? "sound sites verified" : "sound sites not as expected: drinking without sound");
}


static int playUseSound(void* item) {
    if (!soundAvailable || !item) return -1;
    DWORD fn = client + RVA_GetItemUseSound;
    int sound;
    void* it = item;
    __asm__ volatile ("call *%%eax" : "=a"(sound), "+S"(it) : "0"(fn) : "ebx", "ecx", "edx", "edi", "memory", "cc");
    if (sound <= 0) return sound;
    DWORD play = client + RVA_PlaySound;
    int id = sound;
    __asm__ volatile ("pushl $0\n\tpushl $0\n\tpushl $0\n\tpushl $0\n\tcall *%%eax"
        : "+a"(play), "+b"(id) : : "ecx", "edx", "esi", "edi", "memory", "cc");
    return sound;
}


static void restartEffect(DWORD state);


static int beltHasKind(void* player, int kind) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = inventory ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!data) break;
        if (RD8(data, IDATA_LOCATION) == 0xFF && RD8(data, IDATA_NODEPAGE) == 2 && potionKind(itemCode(item)) == kind) return 1;
        item = RDP(data, IDATA_NEXTINV);
    }
    return 0;
}






static BYTE potionTierAt[2];
static DWORD potionDrankAt[2];
#define POTION_OWN_MAX 4
static struct { DWORD at; BYTE hero; } potionOwn[2][POTION_OWN_MAX];
static int potionOwnCount[2];

static int potionConfirmed[2] = {-1, -1}, potionLower[2] = {-1, -1};







static BYTE potionTierKnown(int k) {
    if (potionLower[k] >= 0 && potionConfirmed[k] - potionLower[k] > potionOwnCount[k]) return 0;
    return potionTierAt[k];
}


static int potionActive(void* player, int k) {
    if (fGetUnitState && fGetUnitState(player, k ? STATE_MANAPOT : STATE_HEALTHPOT)) return 1;
    return potionDrankAt[k] && GetTickCount() - potionDrankAt[k] < 1000;
}

static void potionOwnShift(int k) {
    for (int i = 1; i < potionOwnCount[k]; i++) potionOwn[k][i - 1] = potionOwn[k][i];
    potionOwnCount[k]--;
}








static void trackPotionTiers(void* player, const int count[2]) {
    int* confirmed = potionConfirmed;
    int* lower = potionLower;
    DWORD now = GetTickCount();
    for (int k = 0; k < 2; k++) {
        while (potionOwnCount[k] && now - potionOwn[k][0].at >= 1000) {
            if (potionOwn[k][0].hero && potionTierAt[k]) {
                logLine("potion kind %d: own drink not taken, tier at work unknown", k + 1);
                potionTierAt[k] = 0;
            }
            potionOwnShift(k);
        }
        if (confirmed[k] < 0 || count[k] >= confirmed[k]) {
            confirmed[k] = count[k];
            lower[k] = -1;
        }
        else if (lower[k] != count[k]) lower[k] = count[k];
        else {
            int drop = confirmed[k] - count[k];
            while (drop > 0 && potionOwnCount[k]) {
                potionOwnShift(k);
                drop--;
            }
            if (drop > 0 && potionTierAt[k]) {
                logLine("potion kind %d used elsewhere: tier at work unknown", k + 1);
                potionTierAt[k] = 0;
            }
            confirmed[k] = count[k];
            lower[k] = -1;
        }
        if (potionTierAt[k] && !potionActive(player, k)) potionTierAt[k] = 0;
    }
}






static void drinkPotion(void* player, int kind, int allowInventory, int toMerc, int largest, int onlyBigger) {
    void* inventory = RDP(player, U_INVENTORY);
    if (!inventory) return;
    int k = kind == 1 ? 0 : kind == 2 ? 1 : -1;
    if (k < 0 || toMerc) largest = onlyBigger = 0;
    int limit = 0;
    if (onlyBigger && potionActive(player, k)) {
        limit = potionTierKnown(k);
        if (!limit) {
            logLine("drink kind %d: tier at work unknown, nothing stacked", kind);
            lastDrink = 4;
            return;
        }
    }
    DWORD bestId = 0;
    void* bestItem = NULL;
    int bestBelt = 0, bestTier = 127;
    int guard = 0;
    for (void* item = RDP(inventory, INV_FIRST); item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!data) break;
        BYTE location = RD8(data, IDATA_LOCATION), page = RD8(data, IDATA_NODEPAGE);
        int belt = location == 0xFF && page == 2;
        int inInventory = allowInventory && location == 0x00;
        if (belt || inInventory) {
            const char* code = itemCode(item);

            if (kind == 5 ? isStaminaPotion(code) : potionKind(code) == kind) {

                int tier = potionTier(code);
                int better = largest ? tier > bestTier || (tier == bestTier && belt && !bestBelt)
                                     : (belt && !bestBelt) || (belt == bestBelt && tier < bestTier);
                if (tier > limit && (!bestId || better)) {
                    bestId = RD32(item, U_ID);
                    bestItem = item;
                    bestBelt = belt;
                    bestTier = tier;
                }
            }
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    if (!bestId) {
        if (limit) logLine("drink kind %d: none bigger than tier %d", kind, limit);
        else logLine("drink kind %d: none", kind);
        lastDrink = limit ? 4 : 3;
        return;
    }
    lastDrink = bestBelt ? 1 : 2;
    if (k >= 0) {
        if (!toMerc) {

            if (!potionActive(player, k)) potionTierAt[k] = (BYTE) bestTier;
            else if (potionTierAt[k] && bestTier > potionTierAt[k]) potionTierAt[k] = (BYTE) bestTier;
            potionDrankAt[k] = GetTickCount();
        }
        if (potionOwnCount[k] == POTION_OWN_MAX) potionOwnShift(k);
        potionOwn[k][potionOwnCount[k]].at = GetTickCount();
        potionOwn[k][potionOwnCount[k]].hero = (BYTE) !toMerc;
        potionOwnCount[k]++;
    }

    if (!toMerc) restartEffect(kind == 1 ? STATE_HEALTHPOT : kind == 2 ? STATE_MANAPOT : kind == 5 ? STATE_STAMINAPOT : 0);
    BYTE packet[13] = {0};
    if (bestBelt) {
        packet[0] = 0x26;
        *(DWORD*) (packet + 1) = bestId;
        *(DWORD*) (packet + 5) = toMerc ? 1 : 0;
    }
    else {
        void* path = RDP(player, U_PATH);
        packet[0] = 0x20;
        *(DWORD*) (packet + 1) = bestId;
        *(WORD*) (packet + 5) = path ? (WORD) (RD32(path, PATH_X) >> 16) : 0;
        *(WORD*) (packet + 9) = path ? (WORD) (RD32(path, PATH_Y) >> 16) : 0;
    }
    fSendPacket(13, 1, packet);
    int sound = playUseSound(bestItem);
    logLine("drink kind %d tier %d from the %s%s%s, sound %d", kind, bestTier, bestBelt ? "belt" : "inventory",
        largest ? " (largest)" : "", limit ? " (bigger)" : "", sound);
}

static int readable(const void* p, UINT_PTR size);

#include "d2npcdialog.c"

static void checkNpcMenuSites(void) {
    BYTE* search = (BYTE*) (client + RVA_NpcMenuSearch);
    BYTE* thunk = (BYTE*) (client + RVA_GetLocaleText);
    npcMenuAvailable = readable(search, 17) && readable(thunk, 6) && readable((void*) (client + RVA_NpcMenuCount), 4)
        && search[0] == 0x8B && search[1] == 0x15 && *(DWORD*) (search + 2) == client + RVA_NpcMenuCount
        && search[12] == 0xB9 && *(DWORD*) (search + 13) == client + RVA_NpcMenuTable
        && thunk[0] == 0xFF && thunk[1] == 0x25 && RD32(client, RVA_NpcMenuCount) < 128;
    logLine(npcMenuAvailable ? "NPC menu sites verified" : "NPC menu sites not as expected: no NPC menu help");
    checkNpcDialogSites();
    if (npcMenuAvailable) {
        const wchar_t* t = localeText(5382);
        logLine("helper unit name (string 5382): %d chars, first U+%04X", t ? lstrlenW(t) : -1, t && t[0] ? (unsigned) t[0] : 0u);
    }
}


static BYTE* npcMenuEntry(void) {
    if (!npcMenuAvailable || !getUiVar(8)) return NULL;
    void* (*currentNpc)(void) = (void* (*)(void)) (client + RVA_GetCurrentNpc);
    void* npc = currentNpc();
    if (!readable(npc, 0x10)) return NULL;
    DWORD cls = RD32(npc, U_TXTNO), count = RD32(client, RVA_NpcMenuCount);
    for (DWORD i = 0; i < count; i++) {
        BYTE* e = (BYTE*) (client + RVA_NpcMenuTable + i * NPCMENU_SIZE);
        if (*(DWORD*) e == cls) return *(DWORD*) (e + 4) >= 1 && *(DWORD*) (e + 4) <= 6 ? e : NULL;
    }
    return NULL;
}

static const wchar_t* localeText(WORD id) {
    DWORD c = id, a;
    __asm__ volatile ("call *%2" : "=a"(a), "+c"(c) : "r"(client + RVA_GetLocaleText) : "edx", "memory", "cc");
    return (const wchar_t*) a;
}


static void chooseNpcEntry(int index) {
    if (chooseNpcSubEntry(index)) return;
    BYTE* e = npcMenuEntry();
    if (!e || index < 0 || index >= (int) *(DWORD*) (e + 4) - 1 || index >= 5) return;
    DWORD handler = *(DWORD*) (e + 0x12 + index * 4);
    if (!handler) return;
    logLine("NPC menu: entry %d (string %u)", index, (unsigned) *(WORD*) (e + 8 + index * 2));
    DWORD a;
    __asm__ volatile ("call *%1" : "=a"(a) : "r"(handler) : "ebx", "ecx", "edx", "esi", "edi", "memory", "cc");
}





static void* findPortal(void* player, int* where, int* charges) {
    void* inventory = RDP(player, U_INVENTORY);
    void* tome = NULL, *beltScroll = NULL, *scroll = NULL;
    int total = 0, guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        BYTE location = RD8(data, IDATA_LOCATION), page = RD8(data, IDATA_NODEPAGE);
        int belt = location == 0xFF && page == 2, inInventory = location == 0x00;
        const char* code = itemCode(item);
        if (code[0] == 't' && code[1] == 'b' && code[2] == 'k' && inInventory) {
            int q = (int) fGetUnitStat(item, 70, 0);
            total += q;
            if (q > 0 && !tome) tome = item;
        }
        else if (code[0] == 't' && code[1] == 's' && code[2] == 'c' && (belt || inInventory)) {
            total++;
            if (belt && !beltScroll) beltScroll = item;
            if (!belt && !scroll) scroll = item;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    if (charges) *charges = total;
    void* best = tome ? tome : (beltScroll ? beltScroll : scroll);
    if (where) *where = best && best == beltScroll ? 1 : 0;
    return best;
}


static void openPortal(void* player) {
    int belt = 0;
    void* item = findPortal(player, &belt, NULL);
    if (!item) {
        logLine("portal: no tome or scroll");
        return;
    }
    BYTE packet[13] = {0};
    packet[0] = belt ? 0x26 : 0x20;
    *(DWORD*) (packet + 1) = RD32(item, U_ID);
    if (!belt) {
        void* path = RDP(player, U_PATH);
        *(WORD*) (packet + 5) = path ? (WORD) (RD32(path, PATH_X) >> 16) : 0;
        *(WORD*) (packet + 9) = path ? (WORD) (RD32(path, PATH_Y) >> 16) : 0;
    }
    fSendPacket(13, 1, packet);
    int sound = playUseSound(item);
    logLine("portal from the %s, sound %d", belt ? "belt" : "inventory", sound);
}





static void setPlayers(int count) {
    BYTE* thunk = (BYTE*) (client + RVA_SetPlayersThunk);
    DWORD type = RD32(client, RVA_GameType);
    if (!readable(thunk, 6) || thunk[0] != 0xFF || thunk[1] != 0x25 || *(DWORD*) (thunk + 2) != client + RVA_SetPlayersImport) {
        logLine("players: thunk not as expected");
        return;
    }
    if (type != 0 && type != 1 && type != 6 && type != 8) {
        logLine("players: not a single player game (type %lu)", (unsigned long) type);
        return;
    }
    DWORD c = (DWORD) count;
    __asm__ volatile ("call *%1" : "+c"(c) : "r"(thunk) : "eax", "edx", "memory", "cc");
    logLine("players set to %d", count);
}








static void resumeCommand(void* player, const BYTE* buf, int n);
static int resumeSaveGate(void);
static void putResumeFields(Packet* p);
static void putResumeExitFields(Packet* p);

static void plugyCommand(BYTE code) {
    int gate = code == 0x24 ? resumeSaveGate() : 0;
    if (gate < 0) {
        logLine("PlugY save dropped while resuming; the app saves again later");
        return;
    }
    int known = (code >= 0x14 && code <= 0x22) || code == 0x24 || code == 0x26 || code == 0x27;
    if (!plugyLoaded || !GetModuleHandleA("PlugY.dll") || !known) {
        logLine("PlugY command 0x%02X dropped (%s)", code, plugyLoaded ? "unknown" : "no PlugY");
        return;
    }

    BYTE* hook = (BYTE*) ((DWORD) GetModuleHandleA("D2Game.dll") + RVA_GamePlugYPacket);
    if (!GetModuleHandleA("D2Game.dll") || !readable(hook, 2) || hook[0] != 0x57 || hook[1] != 0xE8) {
        logLine("PlugY command 0x%02X dropped (server not hooked by PlugY)", code);
        return;
    }
    BYTE packet[3] = {0x3A, code, 0x00};
    fSendPacket(3, 1, packet);
    if (code != 0x24) logLine("PlugY command 0x%02X", code);
}






static WORD skillId(void* skill);





static int classSkill(DWORD cls, WORD skill) {
    static const WORD first[7] = {6, 36, 66, 96, 126, 221, 251};
    return skill == 0 || skill == 2 || skill == 4 || skill == 5 || (cls < 7 && skill >= first[cls] && skill < first[cls] + 30);
}

static void selectSkill(void* player, int left, WORD skill) {
    void* info = RDP(player, U_INFO);
    if (!readable(info, 0x10)) return;


    if (!classSkill(RD32(player, U_TXTNO), skill)) {
        logLine("select: skill %u not in the class's tree, nothing sent", skill);
        return;
    }
    int own = 0, guard = 0;
    for (void* s = RDP(info, INFO_FIRST); readable(s, SKILL_ITEM + 4) && guard < 400; s = RDP(s, SKILL_NEXT), guard++) {
        if (skillId(s) != skill) continue;
        DWORD item = RD32(s, SKILL_ITEM);
        logLine("select: skill %u in the list, item 0x%08lX", skill, (unsigned long) item);
        if (item == 0xFFFFFFFF) own = 1;
    }
    if (!own) {
        logLine("select: skill %u not own, nothing sent", skill);
        return;
    }
    BYTE packet[9] = {0x3C, (BYTE) skill, (BYTE) (skill >> 8), 0x00, left ? 0x80 : 0x00, 0xFF, 0xFF, 0xFF, 0xFF};
    fSendPacket(9, 1, packet);
    logLine("select: skill %u %s", skill, left ? "left" : "right");
}




static void putPlayerName(Packet* p, void* player) {
    void* pdata = RDP(player, U_DATA);
    for (int k = 0; k < 16; k++) putU8(p, readable(pdata, 16) ? RD8(pdata, k) : 0);
}










static int isClosedGate(const BYTE* txt, DWORD mode) {
    if (mode != 0 || txt[OTXT_SELECTABLE0]) return 0;
    for (int m = 1; m < 8; m++) if (txt[OTXT_SELECTABLE0 + m]) return 1;
    return 0;
}

#define GATE_RADIUS 2
#define MAX_GATES   16


static int tileUnderGate(int (*gates)[2], int nGates, int x, int y) {
    for (int i = 0; i < nGates; i++) {
        int dx = gates[i][0] - x, dy = gates[i][1] - y;
        if (dx >= -GATE_RADIUS && dx <= GATE_RADIUS && dy >= -GATE_RADIUS && dy <= GATE_RADIUS) return 1;
    }
    return 0;
}


static int lastGates[MAX_GATES][2], nLastGates;

#include "d2labels.c"
#include "d2take.c"

static int shopAvailable;
static int sellAvailable;
static DWORD shopJobKey(int part);
static void putShopJob(Packet* p);
#include "d2items.c"
#include "d2loot.c"
#include "d2shop.c"
#include "d2compare.c"
#include "d2layers.c"
#include "d2biginv.c"
#include "d2sell.c"
#include "d2buffs.c"
#include "d2equip.c"
#include "d2sort.c"
#include "d2abgabe.c"
#include "d2move.c"
#include "d2cube.c"
#include "d2cubeopen.c"


#define CMD_STASH 21
static void stashCommand(void* player, BYTE kind, LONG a1, LONG a2);
static void stashBusyDrop(void);
#ifdef D2MOD_TESTCMDS
static void stashTest(BYTE op, BYTE value);
#endif
#include "d2sharedgold.c"
#include "d2stashpages.c"
#include "d2points.c"
#include "d2playtest.c"
#include "d2map.c"
#include "d2quest.c"
#include "d2teleport.c"
#include "d2objects.c"
#include "d2value.c"
#include "d2diff.c"
#include "d2statprobe.c"
#include "d2sound.c"

#ifdef D2MOD_TESTCMDS
static void fakeEffect(DWORD state, DWORD seconds);
#endif

#ifdef D2MOD_TESTCMDS
static volatile int srvTestWanted;
static volatile int srvProbeWanted;
static volatile LONG srvProbeX, srvProbeY;
#endif

static void damageCommand(int numbers, int dps, int element, int appDps, DWORD now);
static void selfCheckCommand(const BYTE* buf, int n);
static void xpBarCommand(const BYTE* buf, int n, DWORD now);

static void handleCommands(void* player, int stale) {
    if (cmdSock == INVALID_SOCKET) return;
    BYTE buf[256];
    for (int i = 0; i < 64; i++) {
        int n = recv(cmdSock, (char*) buf, sizeof(buf), 0);
        if (n <= 0) return;
        if (stale) continue;
        commandsReceived++;
        if (n < 8 || buf[0] != 'D' || buf[1] != '2' || buf[2] != 'A' || buf[3] != 'C') continue;


        if (player && !singlePlayerGame()) continue;
        int cmd = buf[5];
        noteCommand(cmd);

        if (pauseApplied && (cmd == CMD_DRINK || cmd == CMD_WALK || cmd == CMD_UNIT || cmd == CMD_PORTAL || cmd == CMD_BELT || cmd == CMD_TAKE
            || cmd == CMD_SHOP_POTIONS || cmd == CMD_REPAIR || cmd == CMD_SELL || cmd == CMD_SELL_LOOT || cmd == CMD_STASH_LOOT)) continue;



        if (sortBusy() && (cmd == CMD_TAKE || cmd == CMD_BELT || cmd == CMD_EQUIP || cmd == CMD_PORTAL || cmd == CMD_UNIT
            || cmd == CMD_SHOP_POTIONS || cmd == CMD_STASH || cmd == CMD_SELL || cmd == CMD_SELL_LOOT || cmd == CMD_STASH_LOOT
            || cmd == CMD_CUBE)) {
            if (cmd == CMD_STASH) stashBusyDrop();
            continue;
        }
        if (cmd == CMD_PORTAL && player) openPortal(player);
        if (cmd == CMD_PLAYERS && n >= 9 && player && buf[8] >= 1 && buf[8] <= 8) setPlayers(buf[8]);
        if (cmd == CMD_PLUGY && n >= 9 && player) plugyCommand(buf[8]);
        if (cmd == CMD_SELECT_SKILL && n >= 11 && player && !pauseApplied) selectSkill(player, buf[8] == 0, *(WORD*) (buf + 9));
        if (cmd == CMD_NPC && n >= 9 && player) chooseNpcEntry(buf[8]);
        if (cmd == CMD_BELT && n >= 9 && player && buf[8] <= 2) moveToBelt(player, buf[8] + 1);
        if (cmd == CMD_STACKS && n >= 9 && player && buf[8] <= 1) setLargeStacks(buf[8] && singlePlayerGame());
        if (cmd == CMD_POINTS && n >= 11 && player) pointsCommand(player, buf, n);
        if ((cmd == CMD_SHOP_POTIONS || cmd == CMD_REPAIR || cmd == CMD_SELL_LOOT || cmd == CMD_STASH_LOOT) && player) shopCommand(player, buf, n);
        if (cmd == CMD_SELL && player) sellCommand(player, buf, n);
        if (cmd == CMD_MOVE && player) moveCommand(player, buf, n);
        if (cmd == CMD_CUBE && player) cubeCommand(player, buf, n);
        if (cmd == CMD_CUBE_OPEN && player) cubeOpenCommand(player);
        if (cmd == CMD_LABELS && n >= 9) labelFilterCommand(buf, n);
        if (cmd == CMD_VALUE) valueCommand(buf, n);
        if (cmd == CMD_FRAMES) framesCommand(buf, n);
        if (cmd == CMD_MAP) mapCommand(buf, n);
        if (cmd == CMD_TELEPORT) teleportCommand(buf, n);
        if (cmd == 32) selfCheckCommand(buf, n);
        if (cmd == CMD_LAYER) layerCommand(buf, n);
        if (cmd == CMD_SOUND && player) soundCommand(buf, n);
        if (cmd == CMD_DRINK && n >= 9 && player) {
            int kind = buf[8] == 1 ? 2 : (buf[8] == 2 ? 3 : (buf[8] == 3 ? 5 : 1));


            int toMerc = n >= 10 && (buf[9] & 2);
            if (toMerc && kind == 1 && !beltHasKind(player, 1)) kind = 3;

            int largest = n >= 10 && (buf[9] & 4), onlyBigger = n >= 10 && (buf[9] & 8);

            drinkPotion(player, kind, toMerc ? 0 : !(n >= 10 && (buf[9] & 1)) && !sortBusy(), toMerc, largest, onlyBigger);
        }
#ifdef D2MOD_TESTCMDS

        else if (cmd == 92 && player) srvTestWanted = 1;


        else if (cmd == 102) {
            int lines = n >= 9 && buf[8] ? buf[8] : 5;
            for (int i = 1; i <= lines; i++) logLine("test crash: line %d of %d", i, lines);
            volatile int* nowhere = NULL;
            *nowhere = 1;
        }

        else if (cmd == 90 && player && n >= 13 && buf[8] == 5) {
            srvProbeX = *(WORD*) (buf + 9);
            srvProbeY = *(WORD*) (buf + 11);
            srvProbeWanted = 5;
        }

        else if (cmd == 90 && player && n >= 14 && buf[8] == 8) {
            srvProbeX = *(WORD*) (buf + 10);
            srvProbeY = *(WORD*) (buf + 12);
            srvProbeWanted = 8 | buf[9] << 8;
        }
        else if (cmd == 90 && player && n >= 9) srvProbeWanted = buf[8] | (n >= 10 ? buf[9] << 8 : 0);


        else if (cmd == 100 && player) statProbe(player);

        else if (cmd == 95 && player && n >= 9) fakeEffect(buf[8], n >= 10 ? buf[9] : 0);

        else if (cmd == 93 && player) sortFaultNext = 1;


        else if (cmd == 91 && n >= 10) stashTest(buf[8], buf[9]);
        else if (cmd == 94 && player && n > 8 && fSendPacket) {
            fSendPacket((size_t) (n - 8), 1, buf + 8);
            logLine("test: packet %02X, %d bytes", buf[8], n - 8);
        }

        else if (cmd == 96 && player && fGetUnitState) {
            char line[512];
            int len = snprintf(line, sizeof(line), "states:");
            for (DWORD s = 0; s < 300 && len < (int) sizeof(line) - 8; s++) {
                if (fGetUnitState(player, s)) len += snprintf(line + len, sizeof(line) - len, " %lu", (unsigned long) s);
            }
            BYTE working = potionsWorking(player);
            logLine("%s; stamina %lu/%lu; potions working: healing %d mana %d stamina %d", line,
                (unsigned long) (fGetUnitStat(player, STAT_STAMINA, 0) >> 8),
                (unsigned long) (fGetUnitStat(player, STAT_MAXSTAMINA, 0) >> 8),
                (working & 4) != 0, (working & 8) != 0, (working & 16) != 0);
        }

        else if (cmd == 97 && player) {
            void* path = RDP(player, U_PATH);
            void* room1 = path ? RDP(path, PATH_ROOM1) : NULL;
            void* room2 = room1 ? RDP(room1, ROOM1_ROOM2) : NULL;
            void* level = room2 ? RDP(room2, 0x58) : NULL;
            logLine("where: player %lu,%lu level %lu", (unsigned long) (RD32(path, PATH_X) >> 16),
                (unsigned long) (RD32(path, PATH_Y) >> 16), (unsigned long) (level ? RD32(level, 0x1D0) : 0));
            void* act = RDP(player, U_ACT);
            void* misc = act ? RDP(act, 0x48) : NULL;
            int n = 0;
            for (void* lv = misc ? RDP(misc, 0x47C) : NULL; lv && n < 64; lv = RDP(lv, 0x1AC), n++) {
                logLine("  level %lu at %lu,%lu size %lu,%lu", (unsigned long) RD32(lv, 0x1D0), (unsigned long) RD32(lv, 0x1C),
                    (unsigned long) RD32(lv, 0x20), (unsigned long) RD32(lv, 0x24), (unsigned long) RD32(lv, 0x28));
            }
            n = 0;
            int loaded = 0, presets = 0;
            for (void* r = level ? RDP(level, 0x10) : NULL; r && n < 2048; r = RDP(r, 0x24), n++) {
                if (RDP(r, 0x30)) loaded++;
                for (void* pu = RDP(r, 0x5C); pu && presets < 4096; pu = RDP(pu, 0x0C)) {
                    presets++;
                    if (RD32(pu, 0x14) == UNIT_MONSTER && RD32(level, 0x1D0) == 1) {
                        logLine("  preset monster txt %lu at room %lu,%lu + %lu,%lu", (unsigned long) RD32(pu, 0x04),
                            (unsigned long) RD32(r, 0x34), (unsigned long) RD32(r, 0x38), (unsigned long) RD32(pu, 0x08), (unsigned long) RD32(pu, 0x18));
                    }
                    if (RD32(pu, 0x14) == UNIT_TILE) {
                        logLine("  preset tile txt %lu at room %lu,%lu + %lu,%lu", (unsigned long) RD32(pu, 0x04),
                            (unsigned long) RD32(r, 0x34), (unsigned long) RD32(r, 0x38), (unsigned long) RD32(pu, 0x08), (unsigned long) RD32(pu, 0x18));
                    }
                }
                for (void* t = RDP(r, 0x4C); t; t = RDP(t, 0x04)) {
                    void* dest = RDP(t, 0x00);
                    void* destLevel = dest ? RDP(dest, 0x58) : NULL;
                    logLine("  exit room %lu,%lu size %lu,%lu -> level %lu", (unsigned long) RD32(r, 0x34), (unsigned long) RD32(r, 0x38),
                        (unsigned long) RD32(r, 0x3C), (unsigned long) RD32(r, 0x40), (unsigned long) (destLevel ? RD32(destLevel, 0x1D0) : 0));
                }
            }
            logLine("  rooms %d, loaded %d, presets %d", n, loaded, presets);
            n = 0;
            for (void* r = act ? RDP(act, ACT_ROOM1) : NULL; r && n < 512; r = RDP(r, ROOM1_NEXT), n++) {
                for (void* u = RDP(r, ROOM1_UNITS); u; u = RDP(u, U_LISTNEXT)) {
                    if (RD32(u, U_TYPE) == UNIT_TILE) {
                        logLine("  tile unit id %lu txt %lu at %ld,%ld", (unsigned long) RD32(u, U_ID), (unsigned long) RD32(u, U_TXTNO),
                            (long) fGetUnitX(u), (long) fGetUnitY(u));
                    }
                }
            }
        }

        else if (cmd == 98 && n >= 17) {
            DWORD where = *(DWORD*) (buf + 8), length = *(DWORD*) (buf + 12);
            BYTE module = buf[16];
            DWORD gameBase = (DWORD) GetModuleHandleA("D2Game.dll");
            BYTE* at = (BYTE*) (module == 0 ? client + where : (module == 1 ? common + where : (module == 3 ? gameBase + where : where)));
            char path[MAX_PATH];
            lstrcpyA(path, logPath);
            char* slash = strrchr(path, '\\');
            if (slash) lstrcpyA(slash + 1, "d2mod-dump.bin");
            HANDLE f = CreateFileA(path, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, 0, NULL);
            if (f != INVALID_HANDLE_VALUE) {
                DWORD written = 0;
                if (!IsBadReadPtr(at, length)) WriteFile(f, at, length, &written, NULL);
                CloseHandle(f);
                logLine("test dump %08lX + %lu: %lu bytes", (unsigned long) (DWORD) at, (unsigned long) length, (unsigned long) written);
            }
        }

        else if (cmd == 99 && n >= 16 && buf[12] <= 32 && 16 + buf[12] <= n) {
            BYTE* at = (BYTE*) (client + *(DWORD*) (buf + 8));
            DWORD old;
            if (VirtualProtect(at, buf[12], PAGE_EXECUTE_READWRITE, &old)) {
                memcpy(at, buf + 16, buf[12]);
                VirtualProtect(at, buf[12], old, &old);
                FlushInstructionCache(GetCurrentProcess(), at, buf[12]);
                logLine("test poke D2Client+%05lX, %d bytes", (unsigned long) *(DWORD*) (buf + 8), buf[12]);
            }
        }
#endif
        else if (cmd == CMD_PAUSE && n >= 9) {
            pauseWanted = buf[8] & 1;
            pauseRefreshedAt = GetTickCount();
        }
        else if (cmd == CMD_HUD && n >= 9) {
            static int lastHudBits = -1;
            if (buf[8] != lastHudBits) trace("hud bits %d -> %d", lastHudBits, buf[8]);
            lastHudBits = buf[8];



            DWORD t = GetTickCount(), off = t - 60000;
            hudHideUntil = (buf[8] & 1) ? t + 2000 : off;
            duraHideUntil = (buf[8] & 2) ? t + 2000 : off;
            cursorHideUntil = (buf[8] & 4) ? t + 2000 : off;
            staminaUntil = (buf[8] & 8) ? t + 2000 : off;
            labelsUntil = (buf[8] & 16) ? t + 2000 : off;
            mapUntil = (buf[8] & 32) ? t + 2000 : off;
            playtestUntil = (buf[8] & 64) ? t + 2000 : off;
            edgeHideUntil = (buf[8] & 128) ? t + 2000 : off;
            compareUntil = n >= 10 && (buf[9] & 1) ? t + 2000 : off;
            diffUntil = n >= 10 && (buf[9] & 16) ? t + 2000 : off;
            damageCommand(n >= 10 && (buf[9] & 2), n >= 10 && (buf[9] & 4), n >= 10 && (buf[9] & 8), n >= 10 && (buf[9] & 32), t);
            xpBarCommand(buf, n, t);
        }
        else if (cmd == CMD_WALK && n >= 14 && player) {
            walkAction(player, buf[8], *(short*) (buf + 10), *(short*) (buf + 12), GetTickCount());
        }
        else if (cmd == CMD_TAKE && n >= 16 && player) {
            takeSentAt = GetTickCount();
            takeUnit(player, buf[8], *(DWORD*) (buf + 12), GetTickCount());
        }

        else if (cmd == CMD_EQUIP && n >= 16 && player) equipStart(player, buf[8], *(DWORD*) (buf + 12), GetTickCount());

        else if (cmd == CMD_SORT && n >= 10 && player) sortStart(player, buf[8], buf[9], GetTickCount());
        else if (cmd == 25 && player) resumeCommand(player, buf, n);
        else if (cmd == CMD_STASH && n >= 20 && player) stashCommand(player, buf[8], *(LONG*) (buf + 12), *(LONG*) (buf + 16));
        else if (cmd == CMD_UNIT && n >= 20 && player) {
            unitAction(buf[8], buf[9], buf[10], *(DWORD*) (buf + 12), *(DWORD*) (buf + 16), GetTickCount());
        }
        else if (cmd == CMD_HIGHLIGHT && n >= 16) {
            if (*(DWORD*) (buf + 12) != highlightId) trace("highlight %lu type %lu", (unsigned long) *(DWORD*) (buf + 12), (unsigned long) *(DWORD*) (buf + 8));
            highlightType = *(DWORD*) (buf + 8);
            highlightId = *(DWORD*) (buf + 12);
            highlightUntil = GetTickCount() + 1000;
        }
    }
}



typedef struct {
    void* unit;
    int x, y, distance;
} Candidate;


static int isOwnCorpse(void* unit, void* player) {
    if (unit == player) return 0;
    DWORD mode = RD32(unit, U_MODE);
    if (mode != 0 && mode != 17) return 0;
    void* other = RDP(unit, U_DATA);
    void* own = RDP(player, U_DATA);
    return readable(other, 16) && readable(own, 16) && memcmp(other, own, 16) == 0;
}

static void addCandidate(Candidate* list, int* count, void* unit, int x, int y, int px, int py) {
    Candidate* c = &list[(*count)++];
    c->unit = unit;
    c->x = x;
    c->y = y;
    int dx = x - px, dy = 2 * (y - py);
    c->distance = dx * dx + dy * dy;
}


static int keepNearest(Candidate* list, int count, int keep) {
    if (count <= keep) return count;
    for (int i = 0; i < keep; i++) {
        int best = i;
        for (int j = i + 1; j < count; j++) if (list[j].distance < list[best].distance) best = j;
        Candidate t = list[i];
        list[i] = list[best];
        list[best] = t;
    }
    return keep;
}


static int readable(const void* p, UINT_PTR size) {
    return p && !IsBadReadPtr(p, size);
}

static WORD skillId(void* skill) {
    if (!readable(skill, 4)) return 0xFFFF;
    void* skillInfo = RDP(skill, 0);
    return readable(skillInfo, 2) ? *(WORD*) skillInfo : 0xFFFF;
}






static int isUsableCorpse(void* unit, int town) {
    static const DWORD used[] = {1, 96, 99, 104, 107, 118};
    if (town || !fGetUnitState || RD32(unit, U_MODE) != 12) return 0;
    if (isMercenary(RD32(unit, U_TXTNO))) return 0;
    for (unsigned i = 0; i < sizeof(used) / sizeof(used[0]); i++) if (fGetUnitState(unit, used[i])) return 0;
    return 1;
}















static int isOpenQuestEntrance(const BYTE* txt, DWORD mode) {
    BYTE operate = txt[OTXT_OPERATEFN];
    return mode != 0 && txt[OTXT_SUBCLASS] == 0 && (operate == 43 || operate == 70 || operate == 73);
}








static int isQuestObjectInUse(const BYTE* txt, DWORD mode) {
    return mode != 0 && txt[OTXT_SUBCLASS] == 0 && txt[OTXT_OPERATEFN] == 49;
}







static int objectReported(const BYTE* txt, DWORD mode) {
    if (!txt[OTXT_SELECTABLE0 + (mode < 8 ? mode : 0)]) return 0;
    int alwaysUsable = (txt[OTXT_SUBCLASS] & (4 | 16 | 32 | 64)) != 0 || isOpenQuestEntrance(txt, mode)
        || isQuestObjectInUse(txt, mode);
    return mode == 0 || alwaysUsable;
}











static const struct { WORD state, frames; } EFFECTS[] = {
    {128, 2400}, {129, 2400}, {130, 3600}, {131, 3600}, {132, 3600}, {133, 3600}, {134, 2400}, {135, 2400},
    {136, 4800}, {137, 3600},
    {STATE_HEALTHPOT, 192}, {STATE_MANAPOT, 128}, {STATE_STAMINAPOT, 750}, {178, 750}, {179, 750},
};
#define EFFECT_COUNT (sizeof(EFFECTS) / sizeof(EFFECTS[0]))
#define STATE_COUNT  183
#define EFFECTS_SENT 32






static const BYTE EFFECTS_LEFT_OUT[] = {0, 18, 54, 86, 92, 105};






static const BYTE EFFECTS_POINTS[] = {14, 151};
#define STAT_BONEARMOR    132
#define STAT_BONEARMORMAX 133
#define EFFECT_POINTS_BIT 0x8000

static DWORD effectElapsed[256];
static DWORD effectFull[256];
static BYTE effectOn[256];
static BYTE effectSynced[256];
static DWORD effectLoggedAt[256];


static DWORD effectPointsLeft, effectPointsMost, effectPointsAt;
#ifdef D2MOD_TESTCMDS

static DWORD effectFakeLeft[256];
static BYTE effectFake[256];
#endif


static DWORD effectDefaultFrames(DWORD state) {
    for (unsigned i = 0; i < EFFECT_COUNT; i++) if (EFFECTS[i].state == state) return EFFECTS[i].frames;
    return 0;
}

static int effectLeftOut(DWORD state) {
    for (unsigned i = 0; i < sizeof(EFFECTS_LEFT_OUT); i++) if (EFFECTS_LEFT_OUT[i] == state) return 1;
    return 0;
}

static int effectPoints(DWORD state) {
    for (unsigned i = 0; i < sizeof(EFFECTS_POINTS); i++) if (EFFECTS_POINTS[i] == state) return 1;
    return 0;
}


static BYTE effectsCapabilities2(void) {
    return 4;
}

#ifdef D2MOD_TESTCMDS
static void fakeEffect(DWORD state, DWORD seconds) {
    if (state >= 256) return;
    DWORD frames = effectDefaultFrames(state), full = frames ? frames * 40 : seconds * 1000;
    effectFakeLeft[state] = seconds && seconds * 1000 < full ? seconds * 1000 : full;
    effectFake[state] = 1;
    effectOn[state] = 0;
    effectFull[state] = frames ? 0 : full;
    logLine("test: effect %lu for %lu ms", (unsigned long) state, (unsigned long) effectFakeLeft[state]);
}
#endif










static int singlePlayerGame(void) {
    DWORD type = RD32(client, RVA_GameType);
    return type == 0 || type == 1;
}

static int worldPaused(void) {




    if (!singlePlayerGame()) return 0;
    return pauseApplied || (fGetUiVar && (getUiVar(0x09) || getUiVar(0x0B)));
}


static DWORD effectFullMs(DWORD state) {
    return effectFull[state] ? effectFull[state] : effectDefaultFrames(state) * 40u;
}





static void restartEffect(DWORD state) {
    if (state >= 256 || !effectOn[state]) return;
    DWORD full = effectFullMs(state), left = effectElapsed[state] < full ? full - effectElapsed[state] : 0;
    effectFull[state] = left + effectDefaultFrames(state) * 40u;
    effectElapsed[state] = 0;
    logLine("effect again: state %lu, %lu ms left before", (unsigned long) state, (unsigned long) left);
}






static void syncEffect(DWORD state, DWORD serverLeft) {
    if (state >= 256 || !effectOn[state]) return;
    DWORD full = effectFullMs(state), left = effectElapsed[state] < full ? full - effectElapsed[state] : 0;
    if (!effectSynced[state] || !full) {
        effectSynced[state] = 1;
        effectFull[state] = serverLeft + effectElapsed[state];
        logLine("effect state %lu: %lu ms by the game timer, %lu ms left", (unsigned long) state,
                (unsigned long) effectFull[state], (unsigned long) serverLeft);
    }
    else if (serverLeft > left + 400) {
        effectFull[state] = serverLeft;
        effectElapsed[state] = 0;
        logLine("effect again: state %lu, %lu ms left by the game timer (counted %lu)", (unsigned long) state,
                (unsigned long) serverLeft, (unsigned long) left);
    }
    else effectElapsed[state] = full > serverLeft ? full - serverLeft : full;
}


static void effectSwitch(DWORD state, int on, DWORD now) {
    if (on) {
        effectElapsed[state] = 0;
        effectFull[state] = 0;
        effectSynced[state] = 0;
        if (effectPoints(state)) effectPointsAt = effectPointsLeft = effectPointsMost = 0;
    }

    if (!effectDefaultFrames(state) && now - effectLoggedAt[state] < 10000 && effectLoggedAt[state]) return;
    effectLoggedAt[state] = now;
    if (on) logLine("effect on: state %lu", (unsigned long) state);
    else logLine("effect off: state %lu after %lu ms", (unsigned long) state, (unsigned long) effectElapsed[state]);
}





static void trackEffects(void* player, DWORD dt, int scanAll) {
    int paused = worldPaused();
    DWORD now = GetTickCount();
    for (DWORD state = 0; state < STATE_COUNT; state++) {
        int listed = effectDefaultFrames(state) != 0;
#ifdef D2MOD_TESTCMDS
        if (effectFake[state]) {
            if (!effectOn[state]) {
                DWORD full = effectFullMs(state);
                effectElapsed[state] = full > effectFakeLeft[state] ? full - effectFakeLeft[state] : 0;
                effectOn[state] = 1;
                logLine("effect on: state %lu (test)", (unsigned long) state);
                continue;
            }
            if (!paused) {
                effectFakeLeft[state] = effectFakeLeft[state] > dt ? effectFakeLeft[state] - dt : 0;
                effectElapsed[state] += dt;
            }
            if (!effectFakeLeft[state] && effectFullMs(state)) effectFake[state] = effectOn[state] = 0;
            continue;
        }
#endif
        if (!listed && !scanAll) {
            if (effectOn[state] && !paused) effectElapsed[state] += dt;
            continue;
        }
        int on = player && fGetUnitState && !effectLeftOut(state) && fGetUnitState(player, state);
        if (on != effectOn[state]) effectSwitch(state, on, now);
        else if (on && !paused) effectElapsed[state] += dt;
        effectOn[state] = (BYTE) on;
    }
}






static void putEffect(Packet* p, void* player, DWORD state) {
    (void) player;
    if (effectPoints(state)) {
        int fresh = singlePlayerGame() && effectPointsAt;
        DWORD left = fresh ? effectPointsLeft >> 8 : 0, most = fresh ? effectPointsMost >> 8 : 0;
        putU16(p, (WORD) (most ? state | EFFECT_POINTS_BIT : state));
        putU16(p, (WORD) (left > 0xFFFF ? 0xFFFF : left));
        putU16(p, (WORD) (most > 0xFFFF ? 0xFFFF : most));
        return;
    }
    DWORD full = effectFullMs(state), left = full && effectElapsed[state] < full ? full - effectElapsed[state] : 0;
    putU16(p, (WORD) state);
    putU16(p, (WORD) (full ? left / 100 : 0));
    putU16(p, (WORD) (full / 100));
}






static void putEffects(Packet* p, void* player) {
    BYTE* count = p->data + p->len;
    putU8(p, 0);
    BYTE sent[256] = {0};
    for (unsigned i = 0; i < EFFECT_COUNT && *count < EFFECTS_SENT; i++) {
        DWORD state = EFFECTS[i].state;
        if (!effectOn[state]) continue;
        putEffect(p, player, state);
        sent[state] = 1;
        (*count)++;
    }
    for (int pass = 0; pass < 2; pass++) {
        for (DWORD state = 0; state < 256 && *count < EFFECTS_SENT; state++) {
            if (!effectOn[state] || sent[state]) continue;
            int timed = effectFull[state] != 0;
            if (pass == 0 ? !timed : timed) continue;
            putEffect(p, player, state);
            sent[state] = 1;
            (*count)++;
        }
    }
}








static void sendEffects(void* player) {
    static DWORD lastAt, lastTrack, lastRaw = 0xFFFFFFFF, lastType = 0xFFFFFFFF;
    DWORD now = GetTickCount();
    DWORD dt = lastTrack && now - lastTrack < 1000 ? now - lastTrack : 0;
    lastTrack = now;
    trackEffects(player, dt, now - lastAt >= 500);

    int pointsChanged = pointsTick(player) | equipReportDue() | sortReportDue(player);
    if (now - lastAt < 500 && !pointsChanged) return;
    lastAt = now;
    DWORD game = (DWORD) GetModuleHandleA("D2Game.dll");
    DWORD* stored = game ? (DWORD*) (game + RVA_GamePlayers) : NULL;
    DWORD raw = stored && readable(stored, 4) ? *stored : 0xFFFFFFFE;
    if (raw != lastRaw) logLine("players: D2Game+0x111C44 = %ld", (long) raw);
    lastRaw = raw;
    DWORD type = RD32(client, RVA_GameType);
    if (type != lastType) logLine("game type %lu: %s", (unsigned long) type,
        singlePlayerGame() ? "single player" : "multiplayer, commands of the app dropped and no map hack");
    lastType = type;
    Packet p;
    putHeader(&p, PKT_EFFECTS);



    putU8(&p, raw >= 1 && raw <= 8 ? (BYTE) raw : (raw == 0 && singlePlayerGame() ? 1 : 0));
    putEffects(&p, player);


    DWORD heroMode = RD32(player, U_MODE);
    putU8(&p, (singlePlayerGame() ? 1 : 0) | (heroMode == 0 || heroMode == 17 ? 2 : 0) | (heroMode == 17 ? 4 : 0));
    putPlayerName(&p, player);


    void* merc = findMercenary(player);
    DWORD mercLife = merc ? fGetUnitStat(merc, 6, 0) >> 8 : 0;
    putU8(&p, merc ? 1 : 0);
    putU8(&p, (BYTE) (mercLife >= 128 ? 100 : mercLife * 100 / 128));
    putBuffsAndLevels(&p, player);
    putPointsBlock(&p);
    sendPacket(&p);
}





static void sendGoals(void* player, DWORD levelNo) {
    static DWORD lastLevel, lastCount = 0xFFFF, lastAt, lastStand = 0xFF;
    DWORD now = GetTickCount();
    if ((int) (now - playtestUntil) >= 0) return;

    DWORD stand = mapAvailable ? (revealDone() ? 2 : 1) : 0;
    if (levelNo == lastLevel && (DWORD) exitCount == lastCount && stand == lastStand && now - lastAt < 5000) return;
    lastLevel = levelNo;
    lastCount = (DWORD) exitCount;
    lastStand = stand;
    lastAt = now;
    void* path = RDP(player, U_PATH);
    void* room1 = path ? RDP(path, PATH_ROOM1) : NULL;
    void* room2 = room1 ? RDP(room1, ROOM1_ROOM2) : NULL;
    void* level = room2 ? RDP(room2, ROOM2_LEVEL) : NULL;
    Packet p;
    putHeader(&p, PKT_GOALS);
    putU16(&p, levelNo);
    putU8(&p, (BYTE) stand);
    putU16(&p, level ? RD32(level, 0x1C) * 5 : 0);
    putU16(&p, level ? RD32(level, 0x20) * 5 : 0);
    putU16(&p, level ? RD32(level, 0x24) * 5 : 0);
    putU16(&p, level ? RD32(level, 0x28) * 5 : 0);
    int countAt = p.len, count = 0;
    putU8(&p, 0);
    for (int i = 0; i < exitCount && count < 64; i++) {
        const MapLabel* l = &exits[i];
        if (l->from != levelNo || l->weight <= 0) continue;
        putU8(&p, l->kind);
        putU8(&p, (BYTE) l->outX);
        putU8(&p, (BYTE) l->outY);
        putU16(&p, l->to);
        putU16(&p, l->sumX / l->weight);
        putU16(&p, l->sumY / l->weight);
        putU16(&p, l->minX);
        putU16(&p, l->minY);
        putU16(&p, l->maxX);
        putU16(&p, l->maxY);
        count++;
    }
    p.data[countAt] = (BYTE) count;
    sendPacket(&p);
}

#include "d2selfcheck.c"
#include "d2selectable.c"


static DWORD seenId[32], seenMode[32], seenNext;

static void collectAndSend(void* player) {
    void* act = RDP(player, U_ACT);
    void* path = RDP(player, U_PATH);
    if (!act || !path) return;

    View view;
    view.offX = (long) fGetMouseXOffset();
    view.offY = (long) fGetMouseYOffset();
    int width = (int) RD32(client, RVA_ScreenSizeX), height = (int) RD32(client, RVA_ScreenSizeY);
    DWORD levelNo = unitLevel(player);

    Packet p;
    putHeader(&p, PKT_STATE);
    putU16(&p, width);
    putU16(&p, height);
    putU16(&p, (WORD) (short) RD32(client, RVA_MouseX));
    putU16(&p, (WORD) (short) RD32(client, RVA_MouseY));
    DWORD uiLow = 0, uiHigh = 0;
    for (DWORD no = 1; no <= 0x25; no++) {
        if (getUiVar(no)) {
            if (no < 32) uiLow |= 1u << no;
            else uiHigh |= 1u << (no - 32);
        }
    }
    putU32(&p, uiLow);
    putU32(&p, uiHigh);
    putU16(&p, levelNo);
    putU8(&p, isTown(levelNo) ? 1 : 0);



    putU8(&p, (unitActions ? 5 : 0) | (selectedTrampoline ? 2 : 0) | (hudAvailable ? 8 : 0) | (hudHidden ? 16 : 0)
        | (pauseAvailable ? 32 : 0) | (pauseApplied ? 64 : 0) | (duraHidden ? 128 : 0));

    double px, py;
    int psx, psy;
    unitPosition(player, &px, &py);
    toScreen(&view, px, py, &psx, &psy);
    putU16(&p, (WORD) (short) psx);
    putU16(&p, (WORD) (short) psy);
    DWORD life = fGetUnitStat(player, 6, 0) >> 8, maxLife = fGetUnitStat(player, 7, 0) >> 8;
    DWORD mana = fGetUnitStat(player, 8, 0) >> 8, maxMana = fGetUnitStat(player, 9, 0) >> 8;
    putU8(&p, maxLife ? (life * 100 / maxLife > 100 ? 100 : life * 100 / maxLife) : 0);
    putU8(&p, maxMana ? (mana * 100 / maxMana > 100 ? 100 : mana * 100 / maxMana) : 0);

    void* hovered = fGetSelectedUnit();
    putU8(&p, hovered ? RD32(hovered, U_TYPE) : 0xFF);
    putU32(&p, hovered ? RD32(hovered, U_ID) : 0xFFFFFFFF);
    putU8(&p, lastDrink);
    putU8(&p, commandsReceived);


    BYTE belt[16] = {0};
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = inventory ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!data) break;
        if (RD8(data, IDATA_LOCATION) == 0xFF && RD8(data, IDATA_NODEPAGE) == 2) {
            void* ipath = RDP(item, U_PATH);
            DWORD slot = ipath ? RD32(ipath, IPATH_X) : 99;
            if (slot < 16) belt[slot] = (BYTE) potionKind(itemCode(item));
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    for (int i = 0; i < 16; i++) putU8(&p, belt[i]);


    static Candidate monsterList[MAX_CANDIDATES], itemList[MAX_CANDIDATES], objectList[MAX_CANDIDATES], corpseList[MAX_CANDIDATES];
    int nMonsters = 0, nItems = 0, nObjects = 0, nCorpses = 0, rawMonsters = 0;
    Candidate corpse = {0};
    int units = 0;
    int gates[MAX_GATES][2], nGates = 0;
    for (void* room = RDP(act, ACT_ROOM1); room && units < 4096; room = RDP(room, ROOM1_NEXT)) {
        for (void* unit = RDP(room, ROOM1_UNITS); unit && units < 4096; unit = RDP(unit, U_LISTNEXT), units++) {
            DWORD type = RD32(unit, U_TYPE);
            if (type == UNIT_PLAYER && !corpse.unit && isOwnCorpse(unit, player)) {

                double cx, cy;
                unitPosition(unit, &cx, &cy);
                if (cx == 0 && cy == 0) continue;
                toScreen(&view, cx, cy, &corpse.x, &corpse.y);
                if (corpse.x < -80 || corpse.y < -80 || corpse.x > width + 80 || corpse.y > height + 80) continue;
                corpse.unit = unit;
                continue;
            }
            if (type != UNIT_MONSTER && type != UNIT_ITEM && type != UNIT_OBJECT && type != UNIT_TILE) continue;
            double x, y;
            int ux, uy;
            unitPosition(unit, &x, &y);
            if (x == 0 && y == 0) continue;
            toScreen(&view, x, y, &ux, &uy);
            if (ux < -80 || uy < -80 || ux > width + 80 || uy > height + 80) continue;
            if (type == UNIT_MONSTER && nCorpses < MAX_CANDIDATES && isUsableCorpse(unit, isTown(levelNo))) {
                addCandidate(corpseList, &nCorpses, unit, ux, uy, psx, psy);
                continue;
            }
            if (type == UNIT_MONSTER && nMonsters < MAX_CANDIDATES) {
                rawMonsters++;
                if (monsterVerdict(unit, isTown(levelNo), levelNo) == MON_OUT) continue;
                addCandidate(monsterList, &nMonsters, unit, ux, uy, psx, psy);
            }
            else if (type == UNIT_ITEM && nItems < MAX_CANDIDATES) {
                DWORD mode = RD32(unit, U_MODE);
                noteItemMode(unit, mode, GetTickCount());
                if (mode != 3) continue;
                addCandidate(itemList, &nItems, unit, ux, uy, psx, psy);
            }
            else if (type == UNIT_OBJECT && nObjects < MAX_CANDIDATES) {
                void* data = RDP(unit, U_DATA);
                BYTE* txt = data ? (BYTE*) RDP(data, ODATA_TXT) : NULL;

                DWORD omode = RD32(unit, U_MODE);

                if (txt && (txt[OTXT_SUBCLASS] & (1 | 32))) {

                    DWORD oid = RD32(unit, U_ID), slot = 32;
                    for (DWORD k = 0; k < 32; k++) if (seenId[k] == oid + 1) slot = k;
                    if (slot == 32 || seenMode[slot] != omode) {
                        if (slot == 32) {
                            slot = seenNext;
                            seenNext = (seenNext + 1) % 32;
                        }
                        seenId[slot] = oid + 1;
                        seenMode[slot] = omode;
                        trace("object %lu txt %lu subclass %u operate %u mode %lu selectable %u", (unsigned long) oid,
                            (unsigned long) RD32(unit, U_TXTNO), txt[OTXT_SUBCLASS], txt[OTXT_OPERATEFN], (unsigned long) omode,
                            txt[OTXT_SELECTABLE0 + (omode < 8 ? omode : 0)]);
                    }
                }
                if (txt && nGates < MAX_GATES && isClosedGate(txt, omode)) {
                    gates[nGates][0] = (int) x;
                    gates[nGates][1] = (int) y;
                    nGates++;
                }
                if (!txt || !objectVerdict(unit, txt, omode, levelNo)) continue;
                addCandidate(objectList, &nObjects, unit, ux, uy, psx, psy);
            }
            else if (type == UNIT_TILE && nObjects < MAX_CANDIDATES) {

                addCandidate(objectList, &nObjects, unit, ux, uy, psx, psy);
            }
        }
    }

    memcpy(lastGates, gates, sizeof(gates));
    nLastGates = nGates;
    if (nGates) {
        static DWORD gateTileLogged;
        int kept = 0;
        for (int i = 0; i < nObjects; i++) {
            void* unit = objectList[i].unit;
            if (RD32(unit, U_TYPE) == UNIT_TILE) {
                double tx, ty;
                unitPosition(unit, &tx, &ty);
                if (tileUnderGate(gates, nGates, (int) tx, (int) ty)) {
                    if (gateTileLogged != RD32(unit, U_ID) + 1) {
                        gateTileLogged = RD32(unit, U_ID) + 1;
                        logLine("entrance %lu at %d,%d left out: a closed object stands on it", (unsigned long) RD32(unit, U_ID),
                            (int) tx, (int) ty);
                    }
                    continue;
                }
            }
            objectList[kept++] = objectList[i];
        }
        nObjects = kept;
    }
    nMonsters = keepNearest(monsterList, nMonsters, MAX_MONSTERS);
    nItems = keepNearest(itemList, nItems, MAX_ITEMS);
    nObjects = keepNearest(objectList, nObjects, MAX_OBJECTS);
    nCorpses = keepNearest(corpseList, nCorpses, MAX_CORPSES);

    putU8(&p, rawMonsters > 255 ? 255 : rawMonsters);
    putU8(&p, nMonsters);
    for (int i = 0; i < nMonsters; i++) {
        void* unit = monsterList[i].unit;
        void* data = RDP(unit, U_DATA);
        BYTE bits = data ? RD8(data, MDATA_FLAGS) : 0;
        BYTE flags = 0;
        if (bits & 0x04) flags |= 1;
        if (bits & 0x08) flags |= 2;
        if (bits & 0x10) flags |= 4;
        if (bits & 0x02) flags |= 8;

        if (monsterVerdict(unit, isTown(levelNo), levelNo) == MON_PERSON) {
            flags |= 16;

            static DWORD personLogged[16], personLevel, personNext;
            if (personLevel != levelNo) {
                memset(personLogged, 0, sizeof(personLogged));
                personLevel = levelNo;
            }
            int seen = 0;
            for (int k = 0; k < 16; k++) if (personLogged[k] == RD32(unit, U_ID) + 1) seen = 1;
            if (!isTown(levelNo) && !seen) {
                personLogged[personNext] = RD32(unit, U_ID) + 1;
                personNext = (personNext + 1) % 16;
                logLine("person outside town: monstats %lu id %lu in level %lu, ally %d", (unsigned long) RD32(unit, U_TXTNO),
                    (unsigned long) RD32(unit, U_ID), (unsigned long) levelNo, fGetUnitStat(unit, 172, 0) == 2);
            }
        }
        if (isActBoss(RD32(unit, U_TXTNO))) flags |= 128;
        putU32(&p, RD32(unit, U_ID));
        putU16(&p, (WORD) (short) monsterList[i].x);
        putU16(&p, (WORD) (short) monsterList[i].y);

        DWORD hp = fGetUnitStat(unit, 6, 0), maxHp = fGetUnitStat(unit, 7, 0);
        DWORD percent = maxHp ? (DWORD) ((unsigned long long) hp * 100 / maxHp) : 100;
        putU8(&p, percent > 100 ? 100 : percent);
        putU8(&p, flags);
        putName(&p, unit);
    }
    putU8(&p, nItems);
    for (int i = 0; i < nItems; i++) {
        void* unit = itemList[i].unit;
        void* data = RDP(unit, U_DATA);
        putU32(&p, RD32(unit, U_ID));
        putU16(&p, (WORD) (short) itemList[i].x);
        putU16(&p, (WORD) (short) itemList[i].y);
        putU8(&p, groundItemKind(itemCode(unit)));
        putU8(&p, data ? RD32(data, IDATA_QUALITY) : 0);
    }

    if (corpse.unit && nObjects == MAX_OBJECTS) nObjects--;
    putU8(&p, nObjects + (corpse.unit ? 1 : 0));
    if (corpse.unit) {
        putU32(&p, RD32(corpse.unit, U_ID));
        putU16(&p, (WORD) (short) corpse.x);
        putU16(&p, (WORD) (short) corpse.y);
        putU8(&p, 7);
        putU8(&p, 0);
    }
    for (int i = 0; i < nObjects; i++) {
        void* unit = objectList[i].unit;
        int tile = RD32(unit, U_TYPE) == UNIT_TILE;
        BYTE* txt = tile ? NULL : (BYTE*) RDP(RDP(unit, U_DATA), ODATA_TXT);
        BYTE subclass = txt ? txt[OTXT_SUBCLASS] : 0, operate = txt ? txt[OTXT_OPERATEFN] : 0;
        int kind = 0;
        if (tile) kind = 6;
        else if (subclass & 1) kind = 2;
        else if (subclass & 64) kind = 3;
        else if (subclass & 4) kind = 5;

        else if (txt && isOpenQuestEntrance(txt, RD32(unit, U_MODE))) kind = 5;
        else if (subclass & 32) kind = 4;
        else if (operate == 1 || operate == 3 || operate == 4 || operate == 5 || operate == 7 || operate == 14
            || operate == 19 || operate == 20 || operate == 33 || operate == 48 || operate == 51 || operate == 68) kind = 1;
        putU32(&p, RD32(unit, U_ID));
        putU16(&p, (WORD) (short) objectList[i].x);
        putU16(&p, (WORD) (short) objectList[i].y);
        putU8(&p, kind);
        putU8(&p, operate);
    }

    void* info = RDP(player, U_INFO);
    if (!readable(info, 0x10)) info = NULL;
    putU8(&p, (BYTE) RD32(player, U_TXTNO));
    putU16(&p, skillId(info ? RDP(info, INFO_LEFT) : NULL));
    putU16(&p, skillId(info ? RDP(info, INFO_RIGHT) : NULL));
    for (int i = 0; i < 16; i++) {
        DWORD skill = RD32(client, RVA_HotkeySkill + i * 4);
        putU16(&p, skill >= 0xFFFF ? 0xFFFF : (WORD) skill);
        putU8(&p, RD32(client, RVA_HotkeyLeft + i * 4) == 1 ? 1 : 0);
    }

    BYTE duraLoc[12], duraState[12];
    int nDura = 0;

    DWORD handQuantity[2] = {0xFFFF, 0xFFFF};
    void* equipment = RDP(player, U_INVENTORY);
    int steps = 0;
    for (void* item = readable(equipment, 0x10) ? RDP(equipment, INV_FIRST) : NULL; item && steps < 512 && nDura < 12; steps++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD8(data, IDATA_NODEPAGE) == 3) {
            BYTE location = RD8(data, IDATA_BODYLOC);

            if (location == 4 || location == 5) {
                DWORD quantity = fGetUnitStat(item, STAT_QUANTITY, 0);
                if (quantity > 0) handQuantity[location - 4] = quantity > 0xFFFE ? 0xFFFE : quantity;
            }
            DWORD max = fGetUnitStat(item, 73, 0), dur = fGetUnitStat(item, 72, 0);
            DWORD percentMax = fGetUnitStat(item, 75, 0), indestructible = fGetUnitStat(item, 152, 0);
            max = max * (100 + percentMax) / 100;
            if (max > 0 && !indestructible) {
                int percent = (int) (dur * 100 / max);
                if (percent <= DURA_WARN_PERCENT) {
                    duraLoc[nDura] = RD8(data, IDATA_BODYLOC);
                    duraState[nDura] = percent <= 0 ? 2 : 1;
                    nDura++;
                }
            }
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    putU8(&p, nDura);
    for (int i = 0; i < nDura; i++) {
        putU8(&p, duraLoc[i]);
        putU8(&p, duraState[i]);
    }

    putU16(&p, life > 0xFFFF ? 0xFFFF : (WORD) life);
    putU16(&p, maxLife > 0xFFFF ? 0xFFFF : (WORD) maxLife);
    putU16(&p, mana > 0xFFFF ? 0xFFFF : (WORD) mana);
    putU16(&p, maxMana > 0xFFFF ? 0xFFFF : (WORD) maxMana);





    DWORD purseLevel = fGetUnitStat(player, 12, 0), purseGold = fGetUnitStat(player, 14, 0);
    int purseFull = purseLevel > 0 && purseGold >= 10000 * purseLevel;
    putU8(&p, (RD32(client, RVA_AlwaysRun) ? 1 : 0) | (cursorAvailable ? 2 : 0) | (cursorHidden ? 4 : 0)
        | (RD32(client, RVA_NpcDialog) ? 8 : 0) | (worldHooked && heroVarOk ? 16 : 0)
        | (worldHooked && heroVarOk && worldFrames > 0 ? 32 : 0) | (purseFull ? 64 : 0));

    int charges = 0;
    findPortal(player, NULL, &charges);
    putU8(&p, charges > 255 ? 255 : charges);


    const wchar_t* npcSubText[NPC_SUB_MAX];
    int npcSub = npcSubTexts(npcSubText);
    BYTE* npcMenu = npcSub < 0 ? npcMenuEntry() : NULL;
    int npcLines = npcSub >= 0 ? npcSub : npcMenu ? (int) *(DWORD*) (npcMenu + 4) - 1 : 0;
    if (npcLines > 5 && npcSub < 0) npcLines = 5;


    putU8(&p, nCorpses);
    for (int i = 0; i < nCorpses; i++) {
        putU32(&p, RD32(corpseList[i].unit, U_ID));
        putU16(&p, (WORD) (short) corpseList[i].x);
        putU16(&p, (WORD) (short) corpseList[i].y);
    }


    BYTE tierBelt[3] = {0}, tierInv[3] = {0}, countBelt[3] = {0}, countInv[3] = {0};
    BYTE largestBelt[3] = {0}, largestInv[3] = {0};
    int potionSteps = 0, staminaPotions = 0, rejuvInv = 0, potionCount[2] = {0, 0};
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && potionSteps < 512; potionSteps++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        BYTE location = RD8(data, IDATA_LOCATION), page = RD8(data, IDATA_NODEPAGE);
        int inBelt = location == 0xFF && page == 2, inInv = location == 0x00;
        const char* code = itemCode(item);
        int kind = potionKind(code);
        if ((kind == 1 || kind == 2) && (inBelt || inInv)) {
            int k = kind, tier = potionTier(code);
            BYTE* best = inBelt ? &tierBelt[k] : &tierInv[k];
            BYTE* largest = inBelt ? &largestBelt[k] : &largestInv[k];
            BYTE* count = inBelt ? &countBelt[k] : &countInv[k];
            if (*best == 0 || tier < *best) *best = (BYTE) tier;
            if (tier > *largest) *largest = (BYTE) tier;
            if (*count < 255) (*count)++;
            potionCount[k - 1]++;
        }
        else if (kind == 3 && inInv) rejuvInv++;
        else if (isStaminaPotion(code) && (inBelt || inInv)) staminaPotions++;
        item = RDP(data, IDATA_NEXTINV);
    }
    putU8(&p, npcLines < 0 ? 0 : npcLines);
    for (int i = 0; i < npcLines; i++) {
        WORD id = npcSub >= 0 ? 0 : *(WORD*) (npcMenu + 8 + i * 2);
        putU16(&p, id);
        const wchar_t* text = npcSub >= 0 ? npcSubText[i] : localeText(id);
        char utf8[64];
        int len = 0, k = 0;
        if (text) {
            while (k < 30 && text[k]) k++;
            len = WideCharToMultiByte(CP_UTF8, 0, text, k, utf8, sizeof(utf8), NULL, NULL);
            if (len < 0) len = 0;
        }
        putU8(&p, len);
        for (int j = 0; j < len; j++) putU8(&p, (BYTE) utf8[j]);
    }
    for (int k = 1; k <= 2; k++) {
        putU8(&p, tierBelt[k]);
        putU8(&p, tierInv[k]);
        putU8(&p, countBelt[k]);
        putU8(&p, countInv[k]);
    }


    DWORD stamina = fGetUnitStat(player, STAT_STAMINA, 0) >> 8, maxStamina = fGetUnitStat(player, STAT_MAXSTAMINA, 0) >> 8;
    putU16(&p, stamina > 0xFFFF ? 0xFFFF : (WORD) stamina);
    putU16(&p, maxStamina > 0xFFFF ? 0xFFFF : (WORD) maxStamina);
    putU8(&p, staminaPotions > 255 ? 255 : staminaPotions);


    putU8(&p, (staminaAvailable ? 1 : 0) | (staminaUnlimited ? 2 : 0) | potionsWorking(player) | 32 | 64 | 128);




    putU8(&p, (fGetUnitStat(player, STAT_STATPOINTS, 0) > 0 ? 1 : 0) | (fGetUnitStat(player, STAT_SKILLPOINTS, 0) > 0 ? 2 : 0)
        | (plugyLoaded ? 4 : 0) | (labelsAvailable ? 8 : 0) | (labelTipsKept ? 16 : 0) | (mapAvailable ? 32 : 0)
        | (mapAvailable && mapShown ? 64 : 0) | (edgeHidden ? 128 : 0));
    putU16(&p, handQuantity[0]);
    putU16(&p, handQuantity[1]);


    if ((int) (GetTickCount() - playtestUntil) < 0) {
        DWORD x = RD32(path, PATH_X), y = RD32(path, PATH_Y);
        putU16(&p, x >> 16);
        putU16(&p, y >> 16);
        putU8(&p, (x >> 8) & 0xFF);
        putU8(&p, (y >> 8) & 0xFF);
        putU8(&p, RD32(player, U_MODE));
        putU8(&p, fGetUnitStat(player, 12, 0));
        putU32(&p, fGetUnitStat(player, 13, 0));
        putU32(&p, fGetUnitStat(player, 14, 0));
        putU32(&p, fGetUnitStat(player, 15, 0));
    }



    trackPotionTiers(player, potionCount);
    for (int k = 1; k <= 2; k++) {
        putU8(&p, largestBelt[k]);
        putU8(&p, largestInv[k]);
        putU8(&p, potionActive(player, k - 1) ? potionTierKnown(k - 1) : 0);
    }
    putU8(&p, rejuvInv > 255 ? 255 : rejuvInv);
    putHoverItem(&p, player);

    putU8(&p, path && readable((BYTE*) path + PATH_DIR, 1) && RD8(path, PATH_DIR) < 64 ? RD8(path, PATH_DIR) : 0xFF);
    sendPacket(&p);

    void* itemUnits[MAX_ITEMS];
    for (int i = 0; i < nItems; i++) itemUnits[i] = itemList[i].unit;
    logNewItems(itemUnits, nItems);
    sendItems(player, itemUnits, nItems);

    void* objectUnits[MAX_OBJECTS];
    for (int i = 0; i < nObjects; i++) objectUnits[i] = objectList[i].unit;
    sendObjectDetails(objectUnits, nObjects);
    selfCheck(player, &view, width, height, levelNo, monsterList, nMonsters, itemList, nItems, objectList, nObjects,
        gates, nGates);
    sendGoals(player, levelNo);
    sendPlaytest(player, act, levelNo);
    sendEffects(player);
    sendQuests(player);
}

#include "d2server.c"
#include "d2resume.c"
#include "d2damage.c"
#include "d2xpbar.c"
#include "d2stash.c"
#include "d2gold.c"
#include "d2spielweise.c"

static void installClickHook(void);
static void installHighlightHook(void);







static DWORD gameSeed;

#include "d2hudkeep.c"



static void keepSettingsOverPause(DWORD now) {
    DWORD ended = lastTick;
    int kept = 0;
    hudHideUntil = hudKept(hudHideUntil, ended, now, &kept);
    duraHideUntil = hudKept(duraHideUntil, ended, now, &kept);
    cursorHideUntil = hudKept(cursorHideUntil, ended, now, &kept);
    staminaUntil = hudKept(staminaUntil, ended, now, &kept);
    labelsUntil = hudKept(labelsUntil, ended, now, &kept);
    mapUntil = hudKept(mapUntil, ended, now, &kept);
    playtestUntil = hudKept(playtestUntil, ended, now, &kept);
    edgeHideUntil = hudKept(edgeHideUntil, ended, now, &kept);
    compareUntil = hudKept(compareUntil, ended, now, &kept);
    diffUntil = hudKept(diffUntil, ended, now, &kept);
    damageNumbersUntil = hudKept(damageNumbersUntil, ended, now, &kept);
    damageDpsUntil = hudKept(damageDpsUntil, ended, now, &kept);
    damageElementUntil = hudKept(damageElementUntil, ended, now, &kept);
    damageAppDpsUntil = hudKept(damageAppDpsUntil, ended, now, &kept);
    xpBarUntil = hudKept(xpBarUntil, ended, now, &kept);
    xpLevelUntil = hudKept(xpLevelUntil, ended, now, &kept);
    xpCompactUntil = hudKept(xpCompactUntil, ended, now, &kept);
    labelFilterUntil = hudKept(labelFilterUntil, ended, now, &kept);

    for (int f = 0; f < 3; f++)
        diffFrameTables[f].until = hudKeptTable(diffFrameTables[f].have, diffFrameTables[f].until, ended, now, &kept);
    valueUntil = hudKeptTable(valueHave, valueUntil, ended, now, &kept);
    if (kept) logLine("hud: %d settings of the app kept over the pause of %lu ms", kept, (unsigned long) (now - ended));
}

static void noteGame(void* player, DWORD now) {
    void* act = player ? RDP(player, U_ACT) : NULL;
    DWORD seed = readable(act, ACT_SEED + 4) ? RD32(act, ACT_SEED) : gameSeed;
    if (now - lastTick <= 2000 && seed == gameSeed) return;
    trace("game: new or back after %lu ms (seed %08lX, was %08lX): unit caches cleared", (unsigned long) (now - lastTick),
        (unsigned long) seed, (unsigned long) gameSeed);
    gameSeed = seed;
    keepSettingsOverPause(now);
    forgetItems();
    selfCheckNewGame();
    memset(seenId, 0, sizeof(seenId));
    memset(seenMode, 0, sizeof(seenMode));
    seenNext = 0;
}









#define RVA_WorldUiCall  0x453CB
#define RVA_WorldUi      0x1D3E0
#define RVA_WorldUiFlag  0x11C8D8
DWORD worldDrawUi;
void worldDrawStub(void);
__asm__(
    ".text\n"
    ".globl _worldDrawStub\n"
    "_worldDrawStub:\n"
    "    lock incl _worldFrames\n"
    "    jmp *_worldDrawUi\n"
);


static int inModule(HMODULE module, DWORD addr) {
    const BYTE* base = (const BYTE*) module;
    if (!base || !readable(base, 0x40)) return 0;
    const BYTE* nt = base + *(const DWORD*) (base + 0x3C);
    if (!readable(nt, 0x58) || *(const DWORD*) nt != 0x4550) return 0;
    DWORD size = *(const DWORD*) (nt + 0x50);
    return addr >= (DWORD) base && addr < (DWORD) base + size;
}






static void installWorldDrawHook(void) {
    BYTE* site = (BYTE*) (client + RVA_WorldUiCall);
    const BYTE* fn = (const BYTE*) (client + RVA_WorldUi);

    DWORD target = readable(site - 1, 6) && site[0] == 0xE8 ? (DWORD) site + 5 + *(DWORD*) (site + 1) : 0;
    int game = target == client + RVA_WorldUi, plugy = !game && target && inModule(GetModuleHandleA("PlugY.dll"), target);
    if (!target || (!game && !plugy) || site[-1] != 0x52 || !readable(fn, 9) || fn[0] != 0x51 || fn[1] != 0xA1
        || *(const DWORD*) (fn + 2) != client + RVA_WorldUiFlag || fn[6] != 0x53 || fn[7] != 0x33 || fn[8] != 0xDB) {
        logLine("world: the interface call is not as known (%02X, target %08lX), no picture count",
                readable(site, 1) ? site[0] : 0, (unsigned long) target);
        return;
    }
    worldDrawUi = target;
    DWORD old;
    if (!VirtualProtect(site, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    *(DWORD*) (site + 1) = (DWORD) worldDrawStub - ((DWORD) site + 5);
    VirtualProtect(site, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), site, 5);
    worldHooked = 1;
    logLine("world: picture count at D2Client+0x453CB installed (on to %s %08lX)", plugy ? "PlugY" : "the game",
            (unsigned long) target);
}



static void noteWorldFrames(DWORD now) {
    static DWORD firstTickAt, ticks;
    if (!worldHooked) return;
    const BYTE* site = (const BYTE*) (client + RVA_WorldUiCall);
    DWORD target = site[0] == 0xE8 ? (DWORD) site + 5 + *(const DWORD*) (site + 1) : 0;
    if (target != (DWORD) worldDrawStub) {
        worldHooked = 0;
        logLine("world: the call at D2Client+0x453CB was bent again (%02X, target %08lX): no picture signal any more",
                site[0], (unsigned long) target);
        return;
    }
    if (worldFrames == 0) {
        if (!firstTickAt) firstTickAt = now, ticks = 0;
        ticks++;
    }
    else if (firstTickAt) {
        logLine("world: first picture %lu ms and %lu ticks after the first tick of this game",
                (unsigned long) (now - firstTickAt), (unsigned long) ticks);
        firstTickAt = 0;
    }
}


#ifdef D2MOD_TESTCMDS
#define U_GFXFRAME    0x44
#define PATH_TARGET   0x58









static void swingTrace(void* player) {
    static DWORD lastMode = 0xFFFFFFFF, lastFrame, lastTarget = 0xFFFFFFFF, lastTargetMode;
    if (!player) {
        lastMode = lastTarget = 0xFFFFFFFF;
        return;
    }


    void* last = lastTarget != 0xFFFFFFFF ? findUnit(lastTarget, UNIT_MONSTER) : NULL;
    if (last) {
        DWORD m = RD32(last, U_MODE);
        if (m != lastTargetMode && (m == 6 || m == 3))
            trace("swing: monster %lu %s", (unsigned long) lastTarget, m == 6 ? "blocks" : "gets hit");
        lastTargetMode = m;
    }
    DWORD mode = RD32(player, U_MODE), frame = RD32(player, U_GFXFRAME);
    int swinging = (mode >= 7 && mode <= 16 && mode != 9) || mode == 18;
    int start = swinging && (mode != lastMode || frame < lastFrame);
    lastMode = mode;
    lastFrame = frame;
    if (!start) return;
    double px, py, x, y;
    unitPosition(player, &px, &py);
    void* path = RDP(player, U_PATH);
    DWORD ttype = path ? RD32(path, PATH_TARGET + 4) : 0, tid = path ? RD32(path, PATH_TARGET + 8) : 0;
    void* target = path ? RDP(path, PATH_TARGET) : NULL;
    double tdist = -1;
    if (target) {

        ttype = RD32(target, U_TYPE);
        tid = RD32(target, U_ID);
        unitPosition(target, &x, &y);
        tdist = __builtin_sqrt((x - px) * (x - px) + (y - py) * (y - py));
    }
    double hdist = -1;
    void* heldUnit = held.active ? findUnit(held.id, held.type) : NULL;
    if (heldUnit) {
        unitPosition(heldUnit, &x, &y);
        hdist = __builtin_sqrt((x - px) * (x - px) + (y - py) * (y - py));
    }
    DWORD nearId = 0xFFFFFFFF;
    double ndist = -1;
    void* act = RDP(player, U_ACT);
    int units = 0;
    for (void* room = act ? RDP(act, ACT_ROOM1) : NULL; room && units < 4096; room = RDP(room, ROOM1_NEXT)) {
        for (void* unit = RDP(room, ROOM1_UNITS); unit && units < 4096; unit = RDP(unit, U_LISTNEXT), units++) {
            if (RD32(unit, U_TYPE) != UNIT_MONSTER || !isValidMonster(unit, 0)) continue;
            unitPosition(unit, &x, &y);
            double d = __builtin_sqrt((x - px) * (x - px) + (y - py) * (y - py));
            if (ndist < 0 || d < ndist) {
                ndist = d;
                nearId = RD32(unit, U_ID);
            }
        }
    }
    if (target && ttype == UNIT_MONSTER && tid != lastTarget) {
        lastTarget = tid;
        lastTargetMode = RD32(target, U_MODE);
    }
    trace("swing: mode %lu, direction %u, path target %s type %lu id %lu at %.1f, held %lu at %.1f, nearest monster %lu at %.1f subtiles",
        (unsigned long) mode, path ? (unsigned) RD8(path, PATH_DIR) : 0xFFu, target ? "unit" : "none", (unsigned long) ttype, (unsigned long) tid, tdist,
        (unsigned long) (held.active ? held.id : 0xFFFFFFFF), hdist, (unsigned long) nearId, ndist);
}
#endif

static void gameTick(void) {
    static int moreHooks;
    if (!moreHooks) {

        moreHooks = 1;
        crashStep = "first tick: hooks and site checks";
        installClickHook();
        installHighlightHook();
        installWorldDrawHook();
        checkHudSites();
        checkPauseSite();
        checkLabelsSite();
        checkClickFlagSite();
        installClickMapEntryHook();
        keepLabelTips();
        checkSoundSites();
        checkGameSoundSite();
        checkNpcMenuSites();
        checkMapSites();
        checkItemSites();
        checkTakeSites();
        checkEquipSites();
        checkPointSites();
        checkShopSites();
        checkLabelFilterSite();
        checkCompareSites();
        checkLayerSites();
        checkValueSites();
        checkDiffSites();
        checkDamageSites();
        checkXpBarSites();
        checkGoldSites();
        if (!unitActions) setStatus(STATUS_HOOKED, "connected (without direct clicks)");
    }
    void* player = fGetPlayerUnit();
    DWORD now = GetTickCount();
    STEP(noteGame(player, now));
    STEP(noteWorldFrames(now));
    if (!player || !singlePlayerGame()) STEP(setLargeStacks(0));
    STEP(spielweiseTick(player, now));
    STEP(shopNewPlayer(player));
    STEP(handleCommands(player, !player || now - lastTick > 1000));
    lastTick = now;
    STEP(roomMapTick(player));
    STEP(teleportTick(player));
    STEP(goldDialogTick(player));
    if (hudAvailable) {
        int want = (int) (now - hudHideUntil) < 0;
        if (want != hudHidden) STEP(applyHud(want));
    }
    if (duraAvailable) {
        int want = (int) (now - duraHideUntil) < 0;
        if (want != duraHidden) STEP(applyDurability(want));
    }
    if (cursorAvailable) {
        int want = (int) (now - cursorHideUntil) < 0;
        if (want != cursorHidden) STEP(applyCursor(want));
    }
    if (edgeAvailable) {
        DWORD* shown = (DWORD*) (client + RVA_UiVarHelpButton);
        static DWORD shownSaved;
        int want = (int) (now - edgeHideUntil) < 0;




        if (edgeHidden && *shown) shownSaved = *shown;
        if (want != edgeHidden) {
            if (want) shownSaved = *shown;
            else *shown = shownSaved;
            STEP(applyEdgeButtons(want));
        }




        if (edgeHidden) {
            *shown = 0;
            *(DWORD*) (client + RVA_HelpButtonLock) = 1;
        }
    }
    STEP(updatePause(player, now));
    STEP(updateLabels(now));
    STEP(updateLabelFilter(now));
    STEP(updateCompare(now));
    STEP(updateStamina(now));
    STEP(updatePlugYSave());
    STEP(updatePlugYPages());
    STEP(updateServerRepairs(player, now));
    STEP(updateServerEffects(player, now));
    STEP(updateResume(player, now));
    STEP(updateDamage(player, now));
#ifdef D2MOD_TESTCMDS
    STEP(updateServerQueueTest(now));
    STEP(swingTrace(player));
#endif
    STEP(stashTick(player));
    STEP(updateMap(player, now));
    STEP(equipTick(player, now));
    STEP(sortTick(player, now));
    STEP(moveTick(player, now));
    STEP(cubeTick(player, now));
    if (player && !pauseApplied) {
        STEP(repressLost(now));
        STEP(updateHeld(now));
        STEP(updateWalk(player, now));
        STEP(updateShop(player, now));
    }
    else {
        held.active = walk.active = pendingPress.active = 0;
        gameLost.active = gameLost.releaseDue = 0;
        realHeld = 0;
        heldRepressId = NO_REPRESS;

        if (!player) walk.stopping = 0;
    }
    if (now - lastSend < 40) return;
    lastSend = now;
    if (player) STEP(collectAndSend(player));
}



__attribute__((force_align_arg_pointer)) void __cdecl onGameLoop(void) {
    if (!gameThreadId) gameThreadId = GetCurrentThreadId();
    gameTick();
    crashStep = NULL;
}


void gameLoopHook(void);
__asm__(
    ".text\n"
    ".globl _gameLoopHook\n"
    "_gameLoopHook:\n"
    "    pushal\n"
    "    call _onGameLoop\n"
    "    popal\n"
    "    popl %eax\n"
    "    subl $0x20, %esp\n"
    "    movl %ecx, 0x0C(%esp)\n"
    "    pushl %eax\n"
    "    ret\n"
);



static int resolve(void) {
    common = (DWORD) GetModuleHandleA("D2Common.dll");
    net = (DWORD) GetModuleHandleA("D2Net.dll");
    if (!common || !net) return 0;
    fGetPlayerUnit = (GetPlayerUnit_t) (client + RVA_GetPlayerUnit);
    fGetUiVar = client + RVA_GetUiVar_I;
    fGetUnitName = client + RVA_GetUnitName_I;
    fGetSelectedUnit = (GetPlayerUnit_t) (client + RVA_GetSelectedUnit);
    fGetMouseXOffset = (DwordVoid_t) (client + RVA_GetMouseXOffset);
    fGetMouseYOffset = (DwordVoid_t) (client + RVA_GetMouseYOffset);
    fGetUnitX = (UnitCoord_t) (client + RVA_GetUnitX);
    fGetUnitY = (UnitCoord_t) (client + RVA_GetUnitY);
    fFindServerSideUnit = (FindUnit_t) (client + RVA_FindServerSideUnit);
    fFindClientSideUnit = (FindUnit_t) (client + RVA_FindClientSideUnit);
    fClickMap = (ClickMap_t) (client + RVA_ClickMap);
    fSetSelectedUnit = client + RVA_SetSelectedUnit_I;
    fGetUnitStat = (GetUnitStat_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetUnitStat);
    fGetUnitState = (GetUnitState_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetUnitState);
    fGetSkillLevel = (GetSkillLevel_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetSkillLevel);
    fGetItemText = (GetItemText_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_GetItemText);
    fMapToAbsScreen = (MapToAbsScreen_t) GetProcAddress((HMODULE) common, (LPCSTR) ORD_MapToAbsScreen);
    fSendPacket = (SendPacket_t) GetProcAddress((HMODULE) net, (LPCSTR) ORD_SendPacket);
    return fGetUnitStat && fGetItemText && fMapToAbsScreen && fSendPacket;
}


static void installClickHook(void) {
    BYTE* site = (BYTE*) (client + SELECTED_CALL_RVA);
    DWORD target = (DWORD) site + 5 + *(DWORD*) (site + 1);
    if (site[0] != 0xE8 || target != client + RVA_GetSelectedUnit) {
        logLine("click hook: unexpected code at ClickMap (%02X, target %08lX)", site[0], target);
        return;
    }
    DWORD old;
    if (!VirtualProtect(site, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    *(DWORD*) (site + 1) = (DWORD) selectedUnitIntercept - ((DWORD) site + 5);
    VirtualProtect(site, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), site, 5);
    unitActions = 1;
    logLine("click hook installed");
}





static void installHighlightHook(void) {
    BYTE* site = (BYTE*) (client + RVA_GetSelectedUnit);
    if (site[0] != 0xA1) {
        logLine("highlight hook: unexpected code at GetSelectedUnit (%02X)", site[0]);
        return;
    }
    BYTE* tramp = (BYTE*) VirtualAlloc(NULL, 16, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
    if (!tramp) return;
    memcpy(tramp, site, 5);
    tramp[5] = 0xE9;
    *(DWORD*) (tramp + 6) = (DWORD) (site + 5) - ((DWORD) tramp + 10);
    FlushInstructionCache(GetCurrentProcess(), tramp, 10);
    selectedTrampoline = tramp;
    DWORD old;
    if (!VirtualProtect(site, 5, PAGE_EXECUTE_READWRITE, &old)) {
        selectedTrampoline = NULL;
        return;
    }
    site[0] = 0xE9;
    *(DWORD*) (site + 1) = (DWORD) selectedUnitHighlight - ((DWORD) site + 5);
    VirtualProtect(site, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), site, 5);
    logLine("highlight hook installed");
}

static int installHook(void) {
    BYTE* site = (BYTE*) (client + GAMELOOP_PATCH_RVA);
    if (memcmp(site, GAMELOOP_ORIGINAL, sizeof(GAMELOOP_ORIGINAL)) != 0) {
        char text[96];
        snprintf(text, sizeof(text), "wrong version (%02X %02X %02X %02X %02X %02X %02X)",
            site[0], site[1], site[2], site[3], site[4], site[5], site[6]);
        setStatus(STATUS_WRONG_VERSION, text);
        return 0;
    }
    DWORD old;
    if (!VirtualProtect(site, 7, PAGE_EXECUTE_READWRITE, &old)) {
        setStatus(STATUS_FAILED, "VirtualProtect failed");
        return 0;
    }
    BYTE code[7];
    code[0] = 0xE8;
    *(DWORD*) (code + 1) = (DWORD) gameLoopHook - ((DWORD) site + 5);
    code[5] = 0x90;
    code[6] = 0x90;

    *(volatile WORD*) site = 0xFEEB;
    FlushInstructionCache(GetCurrentProcess(), site, 7);
    memcpy(site + 2, code + 2, 5);
    *(volatile WORD*) site = *(WORD*) code;
    VirtualProtect(site, 7, old, &old);
    FlushInstructionCache(GetCurrentProcess(), site, 7);
    return 1;
}




static int portFromEnv(const char* name, int fallback) {
    char value[16] = "";
    SetLastError(0);
    DWORD n = GetEnvironmentVariableA(name, value, sizeof(value));
    if (n == 0 && GetLastError() == ERROR_ENVVAR_NOT_FOUND) return fallback;
    int port = 0, ok = n > 0 && n < sizeof(value);
    for (DWORD i = 0; ok && i < n; i++) {
        if (value[i] < '0' || value[i] > '9') ok = 0;
        else if ((port = port * 10 + (value[i] - '0')) > 65535) ok = 0;
    }
    if (!ok || port < 1) {
        if (n >= sizeof(value)) value[0] = 0;
        logLine("%s=\"%s\" invalid, port %d", name, value, fallback);
        return fallback;
    }
    logLine("%s=%d", name, port);
    return port;
}

static void readPorts(void) {
    if (portsRead) return;
    portsRead = 1;
    appPort = portFromEnv("D2MOD_APP_PORT", APP_PORT);
    modPort = portFromEnv("D2MOD_CMD_PORT", MOD_PORT);
    if (appPort == modPort) {

        logLine("D2MOD_APP_PORT and D2MOD_CMD_PORT both %d: ports %d/%d", appPort, APP_PORT, MOD_PORT);
        appPort = APP_PORT;
        modPort = MOD_PORT;
    }
}

static void openSockets(void) {
    readPorts();
    WSADATA wsa;
    if (WSAStartup(MAKEWORD(2, 2), &wsa) != 0) {
        logLine("WSAStartup failed");
        return;
    }
    sock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    memset(&appAddr, 0, sizeof(appAddr));
    appAddr.sin_family = AF_INET;
    appAddr.sin_port = htons((u_short) appPort);
    appAddr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    cmdSock = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    struct sockaddr_in local;
    memset(&local, 0, sizeof(local));
    local.sin_family = AF_INET;
    local.sin_port = htons((u_short) modPort);
    local.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    u_long nonBlocking = 1;
    if (cmdSock == INVALID_SOCKET || bind(cmdSock, (struct sockaddr*) &local, sizeof(local)) != 0
        || ioctlsocket(cmdSock, FIONBIO, &nonBlocking) != 0) {
        logLine("command socket failed: %d", WSAGetLastError());
        if (cmdSock != INVALID_SOCKET) closesocket(cmdSock);
        cmdSock = INVALID_SOCKET;
    }
    logLine("sockets ready (app %d, commands %d)", appPort, cmdSock != INVALID_SOCKET ? modPort : -1);
}

static void sendStatus(void) {
    Packet p;
    putHeader(&p, PKT_STATUS);
    putU8(&p, (BYTE) status);
    int len = (int) strlen(statusText);
    putU8(&p, len);
    for (int i = 0; i < len; i++) putU8(&p, (BYTE) statusText[i]);
    sendPacket(&p);
}






#define RVA_WinFocusedControl 0x8DB44
#define CONTROL_EDITBOX 1

#define CONTROL_TEXT 0x5C
#define CONTROL_EDITFLAGS 0x260
#define MENU_TEXT_MAX 48

#include "d2menu.h"

static int menuFieldSent[5] = {-1};
static char menuTextSent[MENU_TEXT_MAX + 1];

static void checkMenuField(int force) {
    static DWORD win;
    if (!win) win = (DWORD) GetModuleHandleA("D2Win.dll");
    int field[5] = {0, 0, 0, 0, 0};
    char text[MENU_TEXT_MAX + 1] = "";
    DWORD* focus = win ? (DWORD*) (win + RVA_WinFocusedControl) : NULL;
    if (focus && readable(focus, 4)) {
        DWORD* control = (DWORD*) *focus;
        if (control && readable(control, CONTROL_EDITFLAGS + 4) && control[0] == CONTROL_EDITBOX) {
            int x = (int) control[3], bottom = (int) control[4], w = (int) control[5], h = (int) control[6];
            if (w > 0 && w <= 800 && h > 0 && h <= 600) {
                field[0] = 1;
                field[1] = x;
                field[2] = bottom - h;
                field[3] = w;
                field[4] = h;


                if (menuFieldSecret(control[CONTROL_EDITFLAGS / 4])) field[0] |= 2;
                else {
                    WCHAR wide[MENU_TEXT_MAX / 2 + 1];
                    lstrcpynW(wide, (const WCHAR*) ((BYTE*) control + CONTROL_TEXT), MENU_TEXT_MAX / 2 + 1);
                    if (!WideCharToMultiByte(CP_UTF8, 0, wide, -1, text, sizeof(text), NULL, NULL)) text[0] = 0;
                }
            }
        }
    }
    if (!force && memcmp(field, menuFieldSent, sizeof(field)) == 0 && strcmp(text, menuTextSent) == 0) return;
    memcpy(menuFieldSent, field, sizeof(field));
    lstrcpynA(menuTextSent, text, sizeof(menuTextSent));
    Packet p;
    putHeader(&p, PKT_MENU);
    putU8(&p, (BYTE) field[0]);
    for (int i = 1; i < 5; i++) putU16(&p, (unsigned) (field[i] & 0xFFFF));
    int len = (int) strlen(text);
    putU8(&p, (BYTE) len);
    for (int i = 0; i < len; i++) putU8(&p, (BYTE) text[i]);
    sendPacket(&p);
}












#define RVA_LaunchBattleNet   0x12350
#define RVA_LaunchMultiplayer 0xF790
#define MENU_ENTRY_SIZE       0x30
static const struct { DWORD entry, y, text, press; } MENU_LOCKS[] = {
    {0x21270, 0x16E, 0x13F3, RVA_LaunchBattleNet},
    {0x212D0, 0x1B1, 0x13F4, RVA_LaunchMultiplayer},
    {0x21120, 0x10A, 0x13F3, RVA_LaunchBattleNet},
    {0x21180, 0x14D, 0x13F4, RVA_LaunchMultiplayer},
};
static volatile LONG menuLockChecked;
static volatile LONG menuLockPresses;

static BOOL __stdcall lockedMenuPress(void* event) {
    (void) event;
    if (InterlockedIncrement(&menuLockPresses) <= 3) logLine("multiplayer menu: press ignored (single player only)");
    return TRUE;
}


static void lockMultiplayerMenu(void) {
    if (menuLockChecked) return;
    DWORD launch = (DWORD) GetModuleHandleA("D2Launch.dll");
    if (!launch || InterlockedExchange(&menuLockChecked, 1)) return;
    const int count = (int) (sizeof(MENU_LOCKS) / sizeof(MENU_LOCKS[0]));
    for (int i = 0; i < count; i++) {
        const DWORD* e = (const DWORD*) (launch + MENU_LOCKS[i].entry);
        if (!readable(e, MENU_ENTRY_SIZE) || e[2] != 6 || e[3] != 0x108 || e[4] != MENU_LOCKS[i].y || e[5] != 0x110
            || e[6] != 0x23 || e[8] != MENU_LOCKS[i].text || e[10] != launch + MENU_LOCKS[i].press) {
            logLine("multiplayer menu: entry %d not as expected, nothing locked (games with others still get no commands)", i);
            return;
        }
    }
    DWORD old;
    BYTE* first = (BYTE*) (launch + 0x21120);
    if (!VirtualProtect(first, 0x21300 - 0x21120, PAGE_READWRITE, &old)) {
        logLine("multiplayer menu: table not writable, nothing locked");
        return;
    }
    for (int i = 0; i < count; i++) {
        InterlockedExchange((volatile LONG*) (launch + MENU_LOCKS[i].entry + 0x28), (LONG) (DWORD) lockedMenuPress);
    }
    VirtualProtect(first, 0x21300 - 0x21120, old, &old);
    logLine("multiplayer menu: Battle.net and Other Multiplayer locked");
}









#define RVA_LaunchExitEntry 0x21210
#define RVA_LaunchExit      0xA580
static volatile LONG exitEscChecked;

static void unbindMainMenuEsc(void) {
    if (exitEscChecked) return;
    DWORD launch = (DWORD) GetModuleHandleA("D2Launch.dll");
    if (!launch || InterlockedExchange(&exitEscChecked, 1)) return;
    DWORD* e = (DWORD*) (launch + RVA_LaunchExitEntry);
    if (!readable(e, MENU_ENTRY_SIZE) || e[2] != 6 || e[3] != 0x108 || e[4] != 0x238 || e[5] != 0x110 || e[6] != 0x23
        || e[7] != 0x1B || e[8] != 0x13F5 || e[10] != launch + RVA_LaunchExit) {
        logLine("main menu: the Exit entry is not as expected, Esc still exits");
        return;
    }
    DWORD old;
    if (!VirtualProtect(e + 7, sizeof(DWORD), PAGE_READWRITE, &old)) {
        logLine("main menu: Exit entry not writable, Esc still exits");
        return;
    }
    InterlockedExchange((volatile LONG*) (e + 7), 0);
    VirtualProtect(e + 7, sizeof(DWORD), old, &old);
    logLine("main menu: Esc no longer exits Diablo II, the Exit button still does");
}

#include "d2direct.c"

static void fixStormStreamRace(void);
typedef HMODULE (WINAPI* LoadLibraryA_t)(LPCSTR);
static LoadLibraryA_t gameLoadLibraryA;
static void logStormStreamSkips(void);





#define RVA_PlayerUnitVar 0x11D050
static void* volatile* heroVar(void) {
    static int checked;
    static void* volatile* var;
    if (!checked && client) {
        checked = 1;
        const BYTE* fn = (const BYTE*) (client + RVA_GetPlayerUnit);
        if (readable(fn, 6) && fn[0] == 0xA1 && *(const DWORD*) (fn + 1) == client + RVA_PlayerUnitVar && fn[5] == 0xC3)
            var = (void* volatile*) (client + RVA_PlayerUnitVar);
        logLine(var ? "setup: hero pointer D2Client+0x11D050 as known"
                    : "setup: GetPlayerUnit not as known, out of a game by the ticks alone");
    }
    return var;
}

static DWORD WINAPI setupThread(LPVOID arg) {
    (void) arg;
    fixStormStreamRace();
    openSockets();
    setStatus(STATUS_WAITING, "waiting for D2Client.dll");
    directBegin();
    for (int i = 0; !client; i++) {
        lockMultiplayerMenu();
        unbindMainMenuEsc();
        client = (DWORD) GetModuleHandleA("D2Client.dll");
        if (!client) {
            if (i % 5 == 0) sendStatus();
            Sleep(200);
            flushLog();
            keepCrashFilter();
        }
    }
    lockMultiplayerMenu();
    unbindMainMenuEsc();
    biInstallPanel(client);

    if (!menuLockChecked || !exitEscChecked)
        logLine(gameLoadLibraryA ? "setup: D2Launch not loaded yet, the menu patches follow when Game.exe loads it"
                                 : "setup: D2Launch after D2Client and no LoadLibraryA hook: menus not patched");
    Sleep(500);
    if (!resolve()) setStatus(STATUS_FAILED, "functions not found");
    else if (installHook()) setStatus(STATUS_HOOKED, "connected");


    void* volatile* hero = heroVar();
    if (hero) InterlockedExchange(&heroVarOk, 1);
    directHeroVar = hero;
    DWORD lastBeat = 0;
    int heroWithoutTick = 0;
    for (;;) {
        sendStatus();
        DWORD beat = GetTickCount();
        if (lastTick && beat - lastTick > 1000 && lastBeat) {
            void* volatile* var = heroVar();
            void* hero = var ? *var : NULL;
            if (beat - lastBeat >= 2500)
                logLine("setup: the process stood still for %lu ms, no game tick yet: not counted as out of a game",
                        (unsigned long) (beat - lastBeat));
            else if (var && hero) {
                if (!heroWithoutTick) logLine("setup: no game tick for %lu ms, but the hero is there: the same game",
                                              (unsigned long) (beat - lastTick));
                heroWithoutTick = 1;
            }
            else {
                if (var) InterlockedExchange(&worldFrames, 0);
                if (!InterlockedExchange(&outOfGameSeen, 1))
                    logLine("setup: out of a game (%s)", var ? "no tick, no hero unit" : "no tick");
            }
        }
        else heroWithoutTick = 0;
        lastBeat = beat;
        checkMenuField(1);
        for (int i = 0; i < 4; i++) {
            Sleep(200);
            checkMenuField(0);
            flushLog();
            keepCrashFilter();
        }
        logStormStreamSkips();
        if (socketsBroken) {



            openSockets();
            socketsBroken = 0;
        }
        Sleep(200);
        flushLog();
        keepCrashFilter();
    }
    return 0;
}



static void loadPlugY(void) {
    char path[MAX_PATH];
    lstrcpynA(path, logPath, sizeof(path));
    char* slash = strrchr(path, '\\');
    lstrcpyA(slash ? slash + 1 : path, "PlugY.dll");
    HMODULE plugy = LoadLibraryA(path);
    if (!plugy) {
        logLine("PlugY.dll not loaded: %lu", GetLastError());
        return;
    }
    typedef void (__stdcall *PlugYInit_t)(LPVOID);
    PlugYInit_t init = (PlugYInit_t) GetProcAddress(plugy, "_Init@4");
    if (!init) init = (PlugYInit_t) GetProcAddress(plugy, (LPCSTR) 10000);
    if (!init) {
        logLine("PlugY.dll has no Init");
        return;
    }
    init(NULL);
    plugyLoaded = 1;
    logLine("PlugY loaded and initialised");
}


static HMODULE WINAPI loadRendererThenPlugY(LPCSTR name) {
    HMODULE renderer = LoadLibraryA(name);
    logLine("renderer %s loaded", name ? name : "?");
    static int once;
    if (!once) {
        once = 1;
        loadPlugY();
    }
    return renderer;
}






static void installPlugYHook(void) {
    char path[MAX_PATH];
    lstrcpynA(path, logPath, sizeof(path));
    char* slash = strrchr(path, '\\');
    lstrcpyA(slash ? slash + 1 : path, "PlugY.dll");
    if (GetFileAttributesA(path) == INVALID_FILE_ATTRIBUTES) return;
    DWORD gfx = (DWORD) GetModuleHandleA("D2gfx.dll");
    if (!gfx || GetModuleHandleA("D2Glide.dll") || GetModuleHandleA("glide3x.dll")) {
        logLine("PlugY: too late or no D2gfx (loaded after the game started)");
        return;
    }
    BYTE* at = (BYTE*) (gfx + RVA_GfxLoadRenderer);
    if (!readable(at, 6) || at[0] != 0xFF || at[1] != 0x15 || *(DWORD*) (at + 2) != gfx + RVA_GfxLoadLibraryIat) {
        logLine("PlugY: D2gfx renderer call not as expected, no PlugY");
        return;
    }
    DWORD old;
    if (!VirtualProtect(at, 6, PAGE_EXECUTE_READWRITE, &old)) return;
    at[0] = 0x90;
    at[1] = 0xE8;
    *(DWORD*) (at + 2) = (DWORD) loadRendererThenPlugY - ((DWORD) at + 6);
    VirtualProtect(at, 6, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 6);
    logLine("PlugY: D2gfx renderer load hooked");
}












#define RVA_LaunchMovieCheck 0x12114
static const BYTE launchMovieCheck[] = {0x85, 0xC0, 0x74, 0x6C, 0xE8};
typedef HMODULE (WINAPI* LoadLibraryA_t)(LPCSTR);
static LoadLibraryA_t gameLoadLibraryA;

static int skipIntroWanted;

static void skipIntroMovies(HMODULE launch) {
    BYTE* at = (BYTE*) launch + RVA_LaunchMovieCheck;
    if (!readable(at, sizeof(launchMovieCheck))) return;
    if (at[2] == 0xEB && at[3] == 0x6C) return;
    if (memcmp(at, launchMovieCheck, sizeof(launchMovieCheck)) != 0) {
        logLine("D2Launch movie check not as expected: the opening movies play");
        return;
    }
    DWORD old;
    if (!VirtualProtect(at + 2, 1, PAGE_EXECUTE_READWRITE, &old)) return;
    at[2] = 0xEB;
    VirtualProtect(at + 2, 1, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at + 2, 1);
    logLine("D2Launch: opening movies skipped");
}








static HMODULE WINAPI loadModuleHook(LPCSTR name) {
    HMODULE module = gameLoadLibraryA(name);
    if (module && name) {
        const char* base = strrchr(name, '\\');
        if (lstrcmpiA(base ? base + 1 : name, "D2Client.dll") == 0) biInstallPanel((DWORD) module);
        if (lstrcmpiA(base ? base + 1 : name, "D2Launch.dll") == 0) {
            if (skipIntroWanted) skipIntroMovies(module);
            lockMultiplayerMenu();
            unbindMainMenuEsc();
        }
    }
    return module;
}


static LoadLibraryA_t* gameLoadLibrarySlot(void) {
    BYTE* base = (BYTE*) GetModuleHandleA(NULL);
    IMAGE_NT_HEADERS32* nt = (IMAGE_NT_HEADERS32*) (base + ((IMAGE_DOS_HEADER*) base)->e_lfanew);
    DWORD rva = nt->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT].VirtualAddress;
    if (!rva) return NULL;
    for (IMAGE_IMPORT_DESCRIPTOR* d = (IMAGE_IMPORT_DESCRIPTOR*) (base + rva); d->Name; d++) {
        if (lstrcmpiA((const char*) base + d->Name, "KERNEL32.dll") != 0 || !d->OriginalFirstThunk) continue;
        DWORD* names = (DWORD*) (base + d->OriginalFirstThunk);
        LoadLibraryA_t* slots = (LoadLibraryA_t*) (base + d->FirstThunk);
        for (int i = 0; names[i]; i++) {
            if (!(names[i] & 0x80000000) && strcmp((const char*) base + names[i] + 2, "LoadLibraryA") == 0) return &slots[i];
        }
    }
    return NULL;
}

static void installIntroSkip(void) {
    char path[MAX_PATH];
    lstrcpynA(path, logPath, sizeof(path));
    char* slash = strrchr(path, '\\');
    lstrcpyA(slash ? slash + 1 : path, "d2android-skipintro");
    skipIntroWanted = GetFileAttributesA(path) != INVALID_FILE_ATTRIBUTES;
    HMODULE launch = GetModuleHandleA("D2Launch.dll");
    if (launch) {

        if (skipIntroWanted) logLine("opening movies: D2Launch already running (loaded late), not changed");
        return;
    }

    LoadLibraryA_t* slot = gameLoadLibrarySlot();
    if (!slot || !*slot) {
        logLine("D2Launch: LoadLibraryA import of Game.exe not found, opening movies play, menus only patched if D2Launch comes before D2Client");
        return;
    }
    DWORD old;
    if (!VirtualProtect(slot, sizeof(*slot), PAGE_READWRITE, &old)) {
        logLine("D2Launch: LoadLibraryA import of Game.exe not writable (%lu), no hook", GetLastError());
        return;
    }
    gameLoadLibraryA = *slot;
    *slot = loadModuleHook;
    VirtualProtect(slot, sizeof(*slot), old, &old);
    logLine(skipIntroWanted ? "opening movies: D2Launch is changed when Game.exe loads it"
                            : "D2Launch: its menus are patched when Game.exe loads it");
}



static volatile LONG stormFixDone, stormStreamSkips;
static LONG stormStreamSkipsLogged;













static void fixStormStreamRace(void) {
    if (stormFixDone) return;
    DWORD storm = (DWORD) GetModuleHandleA("Storm.dll");
    if (!storm) return;
    stormFixDone = 1;
    static const BYTE entry[] = {0x51, 0x53, 0x55, 0x8B, 0x6C, 0x24, 0x10, 0x68};
    static const BYTE read[] = {0x8B, 0x45, 0x28, 0x8B, 0x08, 0x6A, 0x00};
    BYTE* fill = (BYTE*) (storm + RVA_StormStreamFill);
    BYTE* site = (BYTE*) (storm + RVA_StormStreamFillRead);
    BYTE* leave = (BYTE*) (storm + RVA_StormStreamFillExit);
    DWORD lock = storm + RVA_StormStreamLock;
    if (!readable(fill, 0x160) || memcmp(fill, entry, sizeof(entry)) != 0 || *(DWORD*) (fill + 8) != lock
        || memcmp(site, read, sizeof(read)) != 0 || leave[0] != 0x68 || *(DWORD*) (leave + 1) != lock
        || leave[5] != 0xFF || leave[6] != 0x15) {
        logLine("Storm stream fill not as expected: not fixed");
        return;
    }
    BYTE* stub = (BYTE*) VirtualAlloc(NULL, 32, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
    if (!stub) return;
    BYTE code[] = {
        0x8B, 0x45, 0x28,
        0x85, 0xC0,
        0x74, 0x07,
        0x8B, 0x08,
        0xE9, 0, 0, 0, 0,
        0xF0, 0xFF, 0x05, 0, 0, 0, 0,
        0xE9, 0, 0, 0, 0,
    };
    *(DWORD*) (code + 10) = (DWORD) (site + 5) - ((DWORD) stub + 14);
    *(DWORD*) (code + 17) = (DWORD) &stormStreamSkips;
    *(DWORD*) (code + 22) = (DWORD) leave - ((DWORD) stub + 26);
    memcpy(stub, code, sizeof(code));
    FlushInstructionCache(GetCurrentProcess(), stub, sizeof(code));
    DWORD old;
    if (!VirtualProtect(site, 5, PAGE_EXECUTE_READWRITE, &old)) return;
    CRITICAL_SECTION* cs = (CRITICAL_SECTION*) lock;
    int locked = cs->DebugInfo || cs->LockCount || cs->OwningThread;
    if (locked) EnterCriticalSection(cs);
    site[0] = 0xE9;
    *(DWORD*) (site + 1) = (DWORD) stub - ((DWORD) site + 5);
    FlushInstructionCache(GetCurrentProcess(), site, 5);
    if (locked) LeaveCriticalSection(cs);
    VirtualProtect(site, 5, old, &old);
    logLine("Storm stream fill: buffer checked under the lock, patched %s",
            locked ? "holding the stream lock" : "before the stream lock exists");
}


static void logStormStreamSkips(void) {
    LONG n = stormStreamSkips;
    if (n == stormStreamSkipsLogged) return;
    stormStreamSkipsLogged = n;
    logLine("Storm stream fill skipped, buffer already released (%ld so far)", (long) n);
}

BOOL WINAPI DllMain(HINSTANCE instance, DWORD reason, LPVOID reserved) {
    (void) reserved;
    if (reason == DLL_PROCESS_ATTACH) {
        hSelf = instance;
        exeBase = (DWORD) GetModuleHandleA(NULL);
        DisableThreadLibraryCalls(instance);
        GetModuleFileNameA(instance, logPath, sizeof(logPath));
        char* slash = strrchr(logPath, '\\');
        if (slash) strcpy(slash + 1, "d2android-mod.log");
        else strcpy(logPath, "d2android-mod.log");
        openLog();
        keepCrashFilter();
        logLine("D2Android mod loaded");
        installPlugYHook();
        installIntroSkip();
        biInstall();
        HANDLE thread = CreateThread(NULL, 0, setupThread, NULL, 0, NULL);
        if (thread) CloseHandle(thread);
    }
    else if (reason == DLL_PROCESS_DETACH && logReady) {

        logProcessEnd(reserved != NULL);
    }
    return TRUE;
}
