


















#include "d2stashcore.c"

#define RVA_GameSave          0x39C20
#define STASH_CODE_DEPOSIT    SRV_CODE_STASH_DEPOSIT
#define STASH_CODE_WITHDRAW   SRV_CODE_STASH_WITHDRAW
#define STASH_CODE_DEPOSITALL SRV_CODE_STASH_ALL

enum { STASH_OK = 1, STASH_FULL, STASH_NOT_ALLOWED, STASH_LOCKED, STASH_SAVING, STASH_NOT_YET, STASH_CURSOR };
enum { SAVE_NONE, SAVE_OK, SAVE_NOT_SAVED, SAVE_NOT_CONFIRMED };

enum { STASH_WHY_NONE, STASH_WHY_BROKEN, STASH_WHY_UNSAVED, STASH_WHY_UNREADABLE, STASH_WHY_MISSING, STASH_WHY_THREAD,
    STASH_WHY_VERSION };

static int singlePlayerGame(void);

static StashState stash;
static int stashHooked;
static DWORD stashHookedGame;
static int stashLoadedOk;
static int stashInUse;
static int stashBroken;
static int stashBrokenWhy;
static int stashUnsaved;
static volatile LONG stashSaving;
static DWORD stashThread;
static int stashOtherThread;
int stashGo;
static int stashLastSave;
static char stashDir[MAX_PATH];
static char stashName[17];
static int stashHardcore;
DWORD stashGameSave;


static void stashFindDir(void) {
    HMODULE adv = LoadLibraryA("advapi32.dll");
    typedef LONG (WINAPI *RegOpen_t)(HKEY, LPCSTR, DWORD, REGSAM, HKEY*);
    typedef LONG (WINAPI *RegQuery_t)(HKEY, LPCSTR, LPDWORD, LPDWORD, LPBYTE, LPDWORD);
    typedef LONG (WINAPI *RegClose_t)(HKEY);
    RegOpen_t open = adv ? (RegOpen_t) GetProcAddress(adv, "RegOpenKeyExA") : NULL;
    RegQuery_t query = adv ? (RegQuery_t) GetProcAddress(adv, "RegQueryValueExA") : NULL;
    RegClose_t close = adv ? (RegClose_t) GetProcAddress(adv, "RegCloseKey") : NULL;
    HKEY key;
    DWORD type, size = sizeof(stashDir) - 2;
    stashDir[0] = 0;
    if (open && query && close
        && open(HKEY_CURRENT_USER, "Software\\Blizzard Entertainment\\Diablo II", 0, KEY_READ, &key) == 0) {
        if (query(key, "Save Path", NULL, &type, (LPBYTE) stashDir, &size) != 0 || type != REG_SZ) stashDir[0] = 0;
        else stashDir[sizeof(stashDir) - 2] = 0;
        close(key);
    }
    if (!stashDir[0]) {
        DWORD n = GetModuleFileNameA(NULL, stashDir, sizeof(stashDir) - 8);
        while (n && stashDir[n - 1] != '\\') n--;
        stashDir[n] = 0;
        strcat(stashDir, "save\\");
    }
    size_t len = strlen(stashDir);
    if (len && stashDir[len - 1] != '\\' && len < sizeof(stashDir) - 1) strcat(stashDir, "\\");
}

static void stashPath(char* out, const char* suffix) {
    snprintf(out, MAX_PATH, "%s_LOD_%sD2Android_Materialien.d2l%s", stashDir, stashHardcore ? "HC_" : "", suffix);
}


static int stashReadFile(const char* path, char* buf, int size) {
    HANDLE h = CreateFileA(path, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, 0, NULL);
    if (h == INVALID_HANDLE_VALUE) return GetLastError() == ERROR_FILE_NOT_FOUND ? -1 : -2;
    DWORD got = 0;
    BOOL ok = ReadFile(h, buf, (DWORD) size, &got, NULL);
    CloseHandle(h);
    return ok && got < (DWORD) size ? (int) got : -2;
}





static int stashWrite(const unsigned* v) {
    char text[4096], back[4096], path[MAX_PATH], tmp[MAX_PATH];
    unsigned check[STASH_KINDS];
    int n = stashFormat(v, text, sizeof(text));
    if (n < 0) return 0;
    stashPath(path, "");
    stashPath(tmp, "~");
    HANDLE h = CreateFileA(tmp, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    if (h == INVALID_HANDLE_VALUE) return 0;
    DWORD put = 0;
    BOOL ok = WriteFile(h, text, (DWORD) n, &put, NULL) && put == (DWORD) n && FlushFileBuffers(h);
    if (!CloseHandle(h)) ok = FALSE;
    int got = ok ? stashReadFile(tmp, back, sizeof(back)) : -2;
    if (got != n || memcmp(text, back, (size_t) n) != 0 || !stashParse(back, got, check)
        || memcmp(check, v, sizeof(check)) != 0) return 0;
    return MoveFileExA(tmp, path, MOVEFILE_REPLACE_EXISTING | MOVEFILE_WRITE_THROUGH) ? 1 : -1;
}


static int stashBrokenAside(void) {
    char pattern[MAX_PATH];
    WIN32_FIND_DATAA found;
    stashPath(pattern, ".kaputt*");
    HANDLE h = FindFirstFileA(pattern, &found);
    if (h == INVALID_HANDLE_VALUE) return 0;
    FindClose(h);
    return 1;
}


static void stashLoad(void* player) {
    void* pdata = RDP(player, U_DATA);
    char name[17] = {0};
    if (!readable(pdata, 16)) return;
    memcpy(name, pdata, 16);

    if (!name[0] || strcmp(name, stashName) == 0) return;
    memset(&stash, 0, sizeof(stash));
    stashLoadedOk = stashInUse = stashBroken = stashUnsaved = 0;
    stashBrokenWhy = STASH_WHY_NONE;
    stashLastSave = SAVE_NONE;
    strcpy(stashName, name);
    if (!stashDir[0]) stashFindDir();
    char path[MAX_PATH], head[64];
    snprintf(path, sizeof(path), "%s%s.d2s", stashDir, stashName);
    HANDLE h = CreateFileA(path, GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE, NULL, OPEN_EXISTING, 0, NULL);
    DWORD got = 0;
    int ok = h != INVALID_HANDLE_VALUE && ReadFile(h, head, sizeof(head), &got, NULL);
    if (h != INVALID_HANDLE_VALUE) CloseHandle(h);
    int hardcore = 0, header = ok ? stashD2sHeader((const unsigned char*) head, got, &hardcore) : 0;
    if (header == 0) {
        stashBroken = 1;
        stashBrokenWhy = STASH_WHY_UNREADABLE;
        logLine("stash: %s not readable, no Materialien", path);
        return;
    }

    if (header < 0) {
        stashBroken = 1;
        stashBrokenWhy = STASH_WHY_VERSION;
        logLine("stash: %s has version 0x%lx, not 0x%x (1.10+): no Materialien", path, (unsigned long) *(DWORD*) (head + 4),
            STASH_D2S_VERSION);
        return;
    }
    stashHardcore = hardcore;
    char text[4096];
    unsigned v[STASH_KINDS] = {0};
    stashPath(path, "");
    int n = stashReadFile(path, text, sizeof(text));
    if (n == -1 && stashBrokenAside()) {
        stashBroken = 1;
        stashBrokenWhy = STASH_WHY_BROKEN;
        logLine("stash: no file but a broken one lies aside (.kaputt-*): Materialien locked");
        return;
    }
    if (n == -1) {
        stashLoaded(&stash, v);
        stashLoadedOk = 1;
        logLine("stash: no file for %s (%s), empty", stashName, stashHardcore ? "hardcore" : "softcore");
        return;
    }
    if (n < 0 || !stashParse(text, n, v)) {
        char aside[MAX_PATH], suffix[48];
        SYSTEMTIME t;
        GetLocalTime(&t);

        for (int k = 0; k < 100; k++) {
            snprintf(suffix, sizeof(suffix), ".kaputt-%04u%02u%02u-%02u%02u%02u%s%.0d", t.wYear, t.wMonth, t.wDay,
                t.wHour, t.wMinute, t.wSecond, k ? "-" : "", k);
            stashPath(aside, suffix);
            if (GetFileAttributesA(aside) == INVALID_FILE_ATTRIBUTES) break;
        }
        BOOL moved = MoveFileExA(path, aside, 0);
        stashBroken = 1;
        stashBrokenWhy = STASH_WHY_BROKEN;
        logLine("stash: %s broken, %s %s; Materialien locked", path, moved ? "set aside as" : "could not be set aside as",
            aside);
        return;
    }
    stashLoaded(&stash, v);
    stashLoadedOk = stashInUse = 1;
    logLine("stash: loaded for %s (%s)", stashName, stashHardcore ? "hardcore" : "softcore");
}


static DWORD stashD2sSum(const BYTE* d, DWORD n) {
    DWORD sum = 0;
    for (DWORD i = 0; i < n; i++) sum = ((sum << 1) | (sum >> 31)) + (i >= 12 && i < 16 ? 0 : d[i]);
    return sum;
}

typedef struct { FILETIME time; DWORD size; int there; } StashFileMark;

static void stashMark(const char* path, StashFileMark* m) {
    WIN32_FILE_ATTRIBUTE_DATA a;
    m->there = GetFileAttributesExA(path, GetFileExInfoStandard, &a) != 0;
    m->time = m->there ? a.ftLastWriteTime : (FILETIME) {0, 0};
    m->size = m->there ? a.nFileSizeLow : 0;
}


static int stashConfirmed(const char* path, const StashFileMark* before, int kind) {
    StashFileMark now;
    stashMark(path, &now);
    if (!now.there || (before->there && CompareFileTime(&now.time, &before->time) == 0)) return 0;
    static BYTE d[65536];
    HANDLE h = CreateFileA(path, GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE, NULL, OPEN_EXISTING, 0, NULL);
    if (h == INVALID_HANDLE_VALUE) return 0;
    DWORD got = 0;
    BOOL ok = ReadFile(h, d, sizeof(d), &got, NULL);
    CloseHandle(h);
    if (!ok || got < 8) return 0;
    if (kind == 0)
        return got >= 16 && got < sizeof(d) && *(DWORD*) d == 0xAA55AA55u && *(DWORD*) (d + 8) == got
            && *(DWORD*) (d + 12) == stashD2sSum(d, got);
    return memcmp(d, kind == 1 ? "CSTM" : "SSS\0", 4) == 0;
}

#ifdef D2MOD_TESTCMDS


static int stashTestHalt, stashTestNoConfirm;
static void stashTest(BYTE op, BYTE value) {
    if (op == 1) stashTestHalt = value;
    else if (op == 2) stashTestNoConfirm = value;
    logLine("stash test: halt %d, no confirm %d", stashTestHalt, stashTestNoConfirm);
}
static void stashTestHere(int point) {
    if (stashTestHalt != point) return;
    logLine("stash test: halted at %d, waiting for kill -9", point);
    for (;;) Sleep(1000);
}
#else
#define stashTestHere(point) ((void) 0)
#define stashTestNoConfirm 0
#endif


__attribute__((used)) int __stdcall stashBeforeSave(void);
__attribute__((used)) int __stdcall stashBeforeSave(void) {
    if (stashThread && GetCurrentThreadId() != stashThread && !stashOtherThread) {
        stashOtherThread = 1;
        logLine("stash: the save runs on another thread than the game, no more orders");
    }
    if (!singlePlayerGame()) return 1;
    if (!stashLoadedOk || stashBroken || !stashInUse) return 1;
    int deposits = 0, changed = stashUnsaved, wasUnsaved = stashUnsaved;
    for (int i = 0; i < STASH_KINDS; i++) {
        if (stash.deposits[i]) deposits = 1;
        if (stash.count[i] != stash.disk[i]) changed = 1;
    }
    if (!changed && !deposits) return 1;
    InterlockedExchange(&stashSaving, 1);
    unsigned pre[STASH_KINDS];
    stashPreValues(&stash, pre);
    int w = stashWrite(pre);
    if (w == 1 || w == -1) stashDiskIsPre(&stash, pre);
    if (w != 1) {
        stashUnsaved = 1;
        if (deposits || wasUnsaved) {
            stashLastSave = SAVE_NOT_SAVED;
            InterlockedExchange(&stashSaving, 0);
            logLine("stash: pre-write failed (%d): the game is NOT saved, Materialien locked", w);
            return 0;
        }


        logLine("stash: pre-write failed (%d), only withdrawals: the game saves, Materialien locked", w);
    }
    stashTestHere(1);
    return 1;
}

static StashFileMark stashMarks[3];
static char stashFiles[3][MAX_PATH];

__attribute__((used)) void __stdcall stashMarkFiles(void);
__attribute__((used)) void __stdcall stashMarkFiles(void) {
    snprintf(stashFiles[0], MAX_PATH, "%s%s.d2s", stashDir, stashName);
    snprintf(stashFiles[1], MAX_PATH, "%s%s.d2x", stashDir, stashName);

    snprintf(stashFiles[2], MAX_PATH, "%s_LOD_%sSharedStashSave.sss", stashDir, stashHardcore ? "HC_" : "");
    for (int i = 0; i < 3; i++) stashMark(stashFiles[i], &stashMarks[i]);
}

static int stashSharedSss = -1;







static int stashSharedWritten(void) {
    if (stashSharedSss >= 0) return stashSharedSss;
    HMODULE plugy = GetModuleHandleA("PlugY.dll");
    char path[MAX_PATH];
    DWORD n = plugy ? GetModuleFileNameA(plugy, path, sizeof(path)) : 0;
    if (n == 0 || n >= sizeof(path) - 16) stashSharedSss = 1;
    else {
        char* slash = strrchr(path, '\\');
        strcpy(slash ? slash + 1 : path, "PlugY.ini");
        stashSharedSss = GetPrivateProfileIntA("STASH", "ActiveSharedStash", 1, path) != 0
            && GetPrivateProfileIntA("STASH", "ActiveMultiPageStash", 1, path) != 0;
    }
    logLine("stash: PlugY's shared stash %s", stashSharedSss ? "on (.sss confirmed)" : "off (.sss untouched, not confirmed)");
    return stashSharedSss;
}


__attribute__((used)) void __stdcall stashAfterSave(void);
__attribute__((used)) void __stdcall stashAfterSave(void) {
    if (!stashSaving) return;

    int files = plugyLoaded && plugySavePatched ? (stashSharedWritten() ? 3 : 2) : 1, all = 1;
    for (int i = 0; i < files; i++) if (!stashConfirmed(stashFiles[i], &stashMarks[i], i)) all = 0;
    if (stashTestNoConfirm) all = 0;
    stashTestHere(2);
    if (!all) {
        stashUnsaved = 1;
        stashLastSave = SAVE_NOT_CONFIRMED;
        logLine("stash: save not confirmed, the pre-write stays, Materialien locked");
    }
    else {
        unsigned exact[STASH_KINDS];
        memcpy(exact, stash.count, sizeof(exact));
        int w = stashWrite(exact);
        if (w == 1) {
            stashDiskIsExact(&stash);
            stashUnsaved = 0;
            stashLastSave = SAVE_OK;
        }
        else {
            stashUnsaved = 1;
            stashLastSave = SAVE_NOT_CONFIRMED;
            logLine("stash: post-write failed (%d), the pre-write stays, Materialien locked", w);
        }
    }
    InterlockedExchange(&stashSaving, 0);
}





void stashSaveTramp(void);
__asm__(
    ".globl _stashSaveTramp\n"
    "_stashSaveTramp:\n"
    "  pushal\n"
    "  call _stashBeforeSave@0\n"
    "  movl %eax, _stashGo\n"
    "  popal\n"
    "  cmpl $0, _stashGo\n"
    "  je 1f\n"
    "  pushal\n"
    "  call _stashMarkFiles@0\n"
    "  popal\n"
    "  pushl 8(%esp)\n"
    "  pushl 8(%esp)\n"
    "  call *_stashGameSave\n"
    "  pushal\n"
    "  call _stashAfterSave@0\n"
    "  popal\n"
    "  ret $8\n"
    "1:\n"
    "  xorl %eax, %eax\n"
    "  ret $8\n");


#define RVA_QuestCreateItem 0xD2F50
#define RVA_RemoveItem      0xB4530
#define RVA_SendPacket      0xDB780
#define PDATA_CLIENT        0x9C
#define ITEMQUAL_NORMAL     2

static DWORD stashFnCreate, stashFnRemove, stashFnSend;


static int stashCheckItemFns(DWORD game) {
    static const BYTE create[] = {0x83, 0xEC, 0x08, 0x53, 0x56, 0x57, 0x8B, 0x7C, 0x24, 0x18, 0x8B, 0xF0};
    static const BYTE remove[] = {0x85, 0xF6, 0x53, 0x57, 0x8B, 0xF8, 0x74, 0x07};
    static const BYTE send[] = {0x53, 0x55, 0x8B, 0x6C, 0x24, 0x10, 0x56, 0x8B, 0xF0};
    BYTE *c = (BYTE*) (game + RVA_QuestCreateItem), *r = (BYTE*) (game + RVA_RemoveItem), *d = (BYTE*) (game + RVA_SendPacket);
    if (!readable(c, sizeof(create)) || memcmp(c, create, sizeof(create)) != 0 || !readable(r, sizeof(remove))
        || memcmp(r, remove, sizeof(remove)) != 0 || !readable(d, sizeof(send)) || memcmp(d, send, sizeof(send)) != 0) {
        logLine("stash: server item functions not as expected, no orders");
        return 0;
    }
    stashFnCreate = (DWORD) c;
    stashFnRemove = (DWORD) r;
    stashFnSend = (DWORD) d;
    return 1;
}






static void* stashCreate(void* game, void* player, DWORD code) {
    void* item;
    DWORD c = code, p = (DWORD) player;
    __asm__ volatile(
        "pushl $0\n\t"
        "pushl %[q]\n\t"
        "pushl %[g]\n\t"
        "call *%[fn]\n\t"
        : "=a"(item), "+c"(c), "+d"(p)
        : [fn] "m"(stashFnCreate), [g] "r"(game), [q] "i"(ITEMQUAL_NORMAL), "a"(0)
        : "memory", "cc");
    return item;
}

static void stashRemove(void* game, void* player, void* item) {
    DWORD g = (DWORD) game, it = (DWORD) item;
    __asm__ volatile(
        "pushl %[p]\n\t"
        "call *%[fn]\n\t"
        : "+a"(g), "+S"(it)
        : [fn] "m"(stashFnRemove), [p] "r"(player)
        : "ecx", "edx", "memory", "cc");
}


static void stashSendRemove(void* player, DWORD id) {
    void* pdata = RDP(player, U_DATA);
    void* netClient = readable(pdata, PDATA_CLIENT + 4) ? RDP(pdata, PDATA_CLIENT) : NULL;
    if (!netClient) return;
    BYTE packet[6] = {0x0A, 4};
    *(DWORD*) (packet + 2) = id;
    DWORD c = (DWORD) netClient;
    __asm__ volatile(
        "pushl $6\n\t"
        "pushl %[d]\n\t"
        "call *%[fn]\n\t"
        : "+a"(c)
        : [fn] "m"(stashFnSend), [d] "r"(packet)
        : "ecx", "edx", "memory", "cc");
}


static void* stashServerItem(void* player, DWORD id, int* kind) {
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        if (RD32(item, U_ID) == id) {
            BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
            DWORD flags = RD32(data, IDATA_FLAGS);
            if (RD8(data, IDATA_LOCATION) != 0x00 || RD8(data, IDATA_NODEPAGE) != 1 || !readable(txt, 0x90)
                || (flags & (IFLAG_SOCKETED | IFLAG_ETHEREAL | IFLAG_RUNEWORD))) return NULL;
            *kind = stashKind((const char*) txt + ITXT_CODE);
            return *kind >= 0 ? item : NULL;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return NULL;
}


static void stashHook(void) {
    static const DWORD sites[] = {0x39D11, 0x93C3F, 0xBE6DA, 0xBE77A, 0xBE7F6, 0xBF3AC, 0xBF5D9, 0xBFB9E};
    DWORD game = (DWORD) GetModuleHandleA("D2Game.dll");
    if (!game || (stashHooked && stashHookedGame == game)) return;
    stashHookedGame = game;
    stashHooked = -1;
    stashThread = GetCurrentThreadId();
    stashGameSave = game + RVA_GameSave;
    BYTE* fn = (BYTE*) stashGameSave;
    static const BYTE start[] = {0x83, 0xEC, 0x38, 0x85, 0xF6, 0x75, 0x08};
    if (!readable(fn, sizeof(start)) || memcmp(fn, start, sizeof(start)) != 0) {
        logLine("stash: save function not as expected, no Materialien");
        return;
    }
    for (unsigned i = 0; i < sizeof(sites) / sizeof(sites[0]); i++) {
        BYTE* at = (BYTE*) (game + sites[i]);
        if (!readable(at, 5) || at[0] != 0xE8 || (DWORD) (at + 5) + *(DWORD*) (at + 1) != stashGameSave) {
            logLine("stash: save call at D2Game+0x%lx not as expected, no Materialien", (unsigned long) sites[i]);
            return;
        }
    }
    DWORD original[sizeof(sites) / sizeof(sites[0])];
    unsigned done = 0;
    for (; done < sizeof(sites) / sizeof(sites[0]); done++) {
        BYTE* at = (BYTE*) (game + sites[done]);
        DWORD old;
        original[done] = *(DWORD*) (at + 1);
        if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) break;
        *(DWORD*) (at + 1) = (DWORD) stashSaveTramp - (DWORD) (at + 5);
        VirtualProtect(at, 5, old, &old);
    }
    if (done < sizeof(sites) / sizeof(sites[0])) {

        for (unsigned i = 0; i < done; i++) {
            BYTE* at = (BYTE*) (game + sites[i]);
            DWORD old;
            if (VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) {
                *(DWORD*) (at + 1) = original[i];
                VirtualProtect(at, 5, old, &old);
            }
        }
        logLine("stash: VirtualProtect failed at D2Game+0x%lx: save calls put back, no Materialien",
            (unsigned long) sites[done]);
        return;
    }
    FlushInstructionCache(GetCurrentProcess(), NULL, 0);
    stashHooked = 1;
    stashCheckItemFns(game);
    logLine("stash: save wrapped at 8 call sites");
}


static LONG stashPending;
static DWORD stashPendingCode;
static int stashLastResult;
static DWORD stashAllIds[INV_MAX_CELLS];
static int stashAllLeft;
static int stashOrderOpen;
static BYTE stashDone;


static void stashAnswer(int result) {
    stashLastResult = result;
    stashDone++;
}


static void stashBusyDrop(void) {
    logLine("stash: command refused: the inventory is being sorted");
    stashAnswer(STASH_SAVING);
}


static void stashOrderOver(int result) {
    stashOrderOpen = 0;
    stashAnswer(result);
}

static int stashSubmit(DWORD code, LONG a1, LONG a2) {
    if (srvState == 0 || (srvState == 1 && srvGame != (DWORD) GetModuleHandleA("D2Game.dll"))) srvCheck();
    if (srvState != 1) return 0;
    stashPending = srvSubmit(code, a1, a2);
    stashPendingCode = code;
    return stashPending != 0;
}




static void stashCommand(void* player, BYTE kind, LONG a1, LONG a2) {
    int open = getUiVar(0x19) || (kind == 2 && getUiVar(0x1A) && cubeStashNear(player));
    const char* why = stashHooked != 1 || !stashFnCreate ? "not available" : !stashLoadedOk || stashBroken ? "no stash file"
        : !singlePlayerGame() ? "not single player" : worldPaused() ? "paused" : !open ? "stash closed"
        : stashUnsaved ? "locked (last save)" : stashOrderOpen || stashPending || stashAllLeft ? "busy"
        : equipCursorItem(player) ? "an item on the cursor" : NULL;
    if (why) {

        stashAnswer(!strcmp(why, "busy") ? STASH_SAVING : !strcmp(why, "locked (last save)") ? STASH_LOCKED
            : !strcmp(why, "an item on the cursor") ? STASH_CURSOR : STASH_NOT_ALLOWED);
        logLine("stash: command %u refused: %s", kind, why);
        return;
    }
    stashOrderOpen = 1;
    if (kind == 1 || kind == 2) {
        if (!stashSubmit(kind == 1 ? STASH_CODE_DEPOSIT : STASH_CODE_WITHDRAW, a1, kind == 1 ? 0 : a2)) {
            logLine("stash: command %u: the server queue took no order", kind);
            stashOrderOver(STASH_NOT_ALLOWED);
        }
    }
    else if (kind != 3) stashOrderOver(STASH_NOT_ALLOWED);
    else {

        void* inventory = RDP(player, U_INVENTORY);
        int guard = 0;
        stashAllLeft = 0;
        for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
            void* data = RDP(item, U_DATA);
            if (!readable(data, 0x70)) break;
            BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
            if (RD8(data, IDATA_LOCATION) == 0x00 && RD8(data, IDATA_NODEPAGE) == 1 && readable(txt, 0x90)
                && stashKind((const char*) txt + ITXT_CODE) >= 0 && stashAllLeft < INV_MAX_CELLS)
                stashAllIds[stashAllLeft++] = RD32(item, U_ID);
            item = RDP(data, IDATA_NEXTINV);
        }
        logLine("stash: deposit all, %d items", stashAllLeft);
        if (!stashAllLeft) stashOrderOver(STASH_OK);
    }
}







static void stashNewGame(void) {
    static DWORD resets;
    if (resets == srvResets) return;
    resets = srvResets;
    stashPending = 0;
    stashAllLeft = 0;
    if (stashOrderOpen) stashOrderOver(STASH_NOT_ALLOWED);
    if (stashLoadedOk) logLine("stash: server queue reset (new game): orders dropped, stash kept");
}


static void stashForget(void) {
    static int told;
    if (stashLoadedOk && stashHasChanges(&stash, stashUnsaved)) {
        if (!told++) logLine("stash: no player, but unsaved changes in memory: kept");
        return;
    }
    told = 0;
    stashLoadedOk = 0;
    stashName[0] = 0;
}

static void stashOrders(void) {
    LONG answer, count;
    if (stashPending && srvTake(stashPending, &answer, &count)) {
        logLine("stash: order %lx answered %ld, moved %ld", (unsigned long) stashPendingCode, (long) answer, (long) count);
        stashPending = 0;
        if (answer != STASH_OK) stashAllLeft = 0;
        if (!stashAllLeft) stashOrderOver((int) answer);
        else stashLastResult = (int) answer;
    }

    if (!stashPending && stashAllLeft && !sortBusy() && !stashSubmit(STASH_CODE_DEPOSIT, (LONG) stashAllIds[--stashAllLeft], 0)) {
        stashAllLeft = 0;
        stashOrderOver(STASH_NOT_ALLOWED);
    }
}









#define PKT_STASH 10
static void sendStash(void* player) {
    static DWORD lastHash, lastAt;
    static Packet p;
    if (!player || stashHooked != 1 || !singlePlayerGame()) return;
    BYTE inv[STASH_KINDS] = {0};
    void* inventory = RDP(player, U_INVENTORY);
    int guard = 0;
    for (void* item = readable(inventory, 0x10) ? RDP(inventory, INV_FIRST) : NULL; item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) break;
        BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
        int k = readable(txt, 0x90) && RD8(data, IDATA_LOCATION) == 0x00 && RD8(data, IDATA_NODEPAGE) == 1
            ? stashKind((const char*) txt + ITXT_CODE) : -1;
        if (k >= 0 && inv[k] < 255) inv[k]++;
        item = RDP(data, IDATA_NEXTINV);
    }
    BYTE caps = (stashFnCreate && stashLoadedOk && !stashBroken ? 1 : 0) | 2 | (getUiVar(0x19) ? 4 : 0)
        | (stashUnsaved || stashBroken || stashOtherThread ? 8 : 0);
    BYTE why = !stashFnCreate ? STASH_WHY_MISSING : stashBroken ? (BYTE) stashBrokenWhy : stashUnsaved ? STASH_WHY_UNSAVED
        : stashOtherThread ? STASH_WHY_THREAD : STASH_WHY_NONE;






    short x = -32768, y = -32768;
    if (skillTreeKnown) {
        x = (short) ((LONG) RD32(client, RVA_PanelOffX) - 80);
        y = (short) ((LONG) RD32(client, RVA_ScreenH) - 540 + (LONG) RD32(client, RVA_PanelOffY));
    }
    DWORD hash = 2166136261u ^ caps ^ (DWORD) stashLastResult << 8 ^ (DWORD) stashLastSave << 16 ^ (DWORD) stashDone << 24;
    hash = (hash ^ (WORD) x ^ (DWORD) (WORD) y << 16) * 16777619u;
    hash = (hash ^ why) * 16777619u;
    int kinds = 0;
    for (int i = 0; i < STASH_KINDS; i++) {
        hash = (hash ^ stash.count[i] ^ (DWORD) inv[i] << 16) * 16777619u;
        if (stash.count[i] || inv[i]) kinds++;
    }
    DWORD now = GetTickCount();
    if (hash == lastHash && now - lastAt < 2000) return;
    lastHash = hash;
    lastAt = now;
    putHeader(&p, PKT_STASH);
    putU8(&p, caps);
    putU8(&p, (BYTE) stashLastResult);
    putU8(&p, (BYTE) stashLastSave);
    putU8(&p, (BYTE) kinds);
    for (int i = 0; i < STASH_KINDS; i++) {
        if (!stash.count[i] && !inv[i]) continue;
        for (int c = 0; c < 4; c++) putU8(&p, c < 3 ? (BYTE) stashCodes[i][c] : ' ');
        putU16(&p, (WORD) (stash.count[i] > 65535 ? 65535 : stash.count[i]));
        putU8(&p, inv[i]);
    }
    putU16(&p, (WORD) x);
    putU16(&p, (WORD) y);
    putU8(&p, stashDone);
    putU8(&p, why);
    sendPacket(&p);
}


static void stashTick(void* player) {
    if (!player) {
        stashPending = 0;
        stashAllLeft = 0;
        if (stashOrderOpen) stashOrderOver(STASH_NOT_ALLOWED);
        stashForget();
        return;
    }
    if (!singlePlayerGame()) return;
    stashNewGame();
    stashHook();
    if (stashHooked == 1) stashLoad(player);
    stashOrders();
    sendStash(player);
}






static LONG stashServer(DWORD code, LONG arg1, LONG arg2, void* game, void* unit, LONG* moved) {
    *moved = 0;
    if (stashHooked != 1 || !stashLoadedOk || stashBroken || !singlePlayerGame() || !stashFnCreate) return STASH_NOT_ALLOWED;
    if (stashSaving) return STASH_SAVING;
    if (stashOtherThread || GetCurrentThreadId() != stashThread) return STASH_NOT_ALLOWED;
    if (stashUnsaved) return STASH_LOCKED;
    if (!game || !unit || RD32(unit, U_TYPE) != 0) return STASH_NOT_ALLOWED;





    if (equipCursorItem(unit)) {
        logLine("stash: order %lx refused: an item on the cursor", (unsigned long) code);
        return STASH_CURSOR;
    }
    if (code == STASH_CODE_DEPOSIT) {
        int kind = -1;
        void* item = stashServerItem(unit, (DWORD) arg1, &kind);
        if (!item) return STASH_NOT_ALLOWED;
        if (stash.count[kind] >= STASH_MAX) return STASH_FULL;
        stashSendRemove(unit, (DWORD) arg1);
        stashRemove(game, unit, item);
        stashDeposit(&stash, kind);
        stashInUse = 1;
        *moved = 1;
        logLine("stash: deposited %.3s (id %lu), now %u", stashCodes[kind], (unsigned long) arg1, stash.count[kind]);
        return STASH_OK;
    }
    if (code == STASH_CODE_WITHDRAW) {
        if (arg1 < 0 || arg1 >= STASH_KINDS) return STASH_NOT_ALLOWED;
        LONG want = arg2 > 0 ? arg2 : invGridW * invGridH;
        DWORD c = (DWORD) (BYTE) stashCodes[arg1][0] | (DWORD) (BYTE) stashCodes[arg1][1] << 8
            | (DWORD) (BYTE) stashCodes[arg1][2] << 16 | 0x20u << 24;
        while (*moved < want && stash.count[arg1]) {
            stashWithdraw(&stash, (int) arg1);
            if (!stashCreate(game, unit, c)) {
                stash.count[arg1]++;
                logLine("stash: withdraw %.3s: no room after %ld", stashCodes[arg1], (long) *moved);
                return *moved ? STASH_OK : STASH_FULL;
            }
            ++*moved;
        }
        logLine("stash: withdrew %ld %.3s, now %u", (long) *moved, stashCodes[arg1], stash.count[arg1]);
        return *moved ? STASH_OK : STASH_NOT_ALLOWED;
    }
    return STASH_NOT_ALLOWED;
}
