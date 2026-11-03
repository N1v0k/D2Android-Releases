

















#define RVA_D2DXFpsFix 0x45EA1
static const BYTE D2DX_FPSFIX_ORIGINAL[4] = {0x39, 0x1D, 0xF0, 0x46};
#define D2DX_WAIT_MS 3000
#define PATCH_GUARD_MAX 200

static volatile DWORD patchGuardThread;
static BYTE* volatile patchGuardAt;
static volatile SIZE_T patchGuardSize;
static volatile LONG patchGuardAgain;

static LONG CALLBACK patchGuardHandler(EXCEPTION_POINTERS* info) {
    EXCEPTION_RECORD* rec = info->ExceptionRecord;
    if (!rec || rec->ExceptionCode != EXCEPTION_ACCESS_VIOLATION || rec->NumberParameters < 2
        || rec->ExceptionInformation[0] != 1 || GetCurrentThreadId() != patchGuardThread)
        return EXCEPTION_CONTINUE_SEARCH;
    BYTE* at = patchGuardAt;
    BYTE* addr = (BYTE*) rec->ExceptionInformation[1];
    if (!at || addr < at || addr >= at + patchGuardSize) return EXCEPTION_CONTINUE_SEARCH;

    if (patchGuardAgain >= PATCH_GUARD_MAX) return EXCEPTION_CONTINUE_SEARCH;
    if (patchGuardAgain >= 10) Sleep(1);
    DWORD old;
    if (!VirtualProtect(at, patchGuardSize, PAGE_EXECUTE_READWRITE, &old)) return EXCEPTION_CONTINUE_SEARCH;
    patchGuardAgain++;
    return EXCEPTION_CONTINUE_EXECUTION;
}

#ifdef D2MOD_TESTCMDS








#define RACE_BETWEEN 1
#define RACE_D2DX 2
#define RACE_THREAD 3
static int patchRaceMode(void) {
    char value[8] = "";
    DWORD n = GetEnvironmentVariableA("D2T_PATCHRACE", value, sizeof(value));
    if (!n || n >= sizeof(value)) return 0;
    return lstrcmpiA(value, "d2dx") == 0 ? RACE_D2DX : lstrcmpiA(value, "thread") == 0 ? RACE_THREAD
        : lstrcmpA(value, "1") == 0 ? RACE_BETWEEN : 0;
}

static DWORD WINAPI patchRaceThread(LPVOID arg) {
    int mode = (int) (INT_PTR) arg;
    BYTE* at = (BYTE*) (client + RVA_D2DXFpsFix);
    DWORD start = GetTickCount(), rounds = 0, old;
    while (GetTickCount() - start < (mode == RACE_D2DX ? 1000u : 2000u)) {
        if (VirtualProtect(at, 4, PAGE_EXECUTE_READWRITE, &old)) VirtualProtect(at, 4, PAGE_EXECUTE_READ, &old);
        rounds++;
    }
    if (mode == RACE_D2DX && memcmp(at, D2DX_FPSFIX_ORIGINAL, 4) == 0
        && VirtualProtect(at, 8, PAGE_EXECUTE_READWRITE, &old)) {
        *(DWORD*) at = 0x90909090;
        *(DWORD*) (at + 4) = 0x90909090;
        VirtualProtect(at, 8, PAGE_EXECUTE_READ, &old);
    }
    logLine("test: patch race over, %lu rounds of VirtualProtect at D2Client+0x45EA1%s", (unsigned long) rounds,
            mode == RACE_D2DX ? ", then D2DX's fps fix" : "");
    return 0;
}

static void patchRaceStart(void) {
    int mode = patchRaceMode();
    if (!mode) return;
    logLine("test: patch race %s", mode == RACE_BETWEEN ? "between the hook's writes"
                                   : mode == RACE_D2DX ? "like D2DX started" : "with VirtualProtect started");
    if (mode == RACE_BETWEEN) return;
    HANDLE thread = CreateThread(NULL, 0, patchRaceThread, (LPVOID) (INT_PTR) mode, 0, NULL);
    if (thread) CloseHandle(thread);
    if (mode == RACE_THREAD) Sleep(20);
}

static void patchRaceBetween(void* at) {
    DWORD old;
    if (patchRaceMode() == RACE_BETWEEN && VirtualProtect(at, 1, PAGE_EXECUTE_READ, &old))
        logLine("test: hook's page read-only again after the first write");
}
#else
static int patchRaceMode(void) { return 0; }
static void patchRaceStart(void) {}
static void patchRaceBetween(void* at) { (void) at; }
#endif

static int startedWith3dfx(void) {
    if (patchRaceMode() == 2) return 1;
    const char* line = GetCommandLineA();
    for (const char* p = line ? line : ""; *p; p++)
        if (p[0] == '-' && (p[1] | 0x20) == '3' && (p[2] | 0x20) == 'd' && (p[3] | 0x20) == 'f' && (p[4] | 0x20) == 'x')
            return 1;
    return 0;
}


static void patchWaitForD2DX(void) {
    if (!startedWith3dfx()) return;
    const BYTE* fix = (const BYTE*) (client + RVA_D2DXFpsFix);
    DWORD start = GetTickCount();
    for (;;) {
        DWORD waited = GetTickCount() - start;
        if (*(const volatile DWORD*) fix == 0x90909090) {
            Sleep(50);
            logLine("setup: D2DX's fps fix in D2Client after %lu ms of waiting, now the game loop hook",
                    (unsigned long) waited);
            return;
        }
        if (memcmp(fix, D2DX_FPSFIX_ORIGINAL, sizeof(D2DX_FPSFIX_ORIGINAL)) != 0) {
            logLine("setup: D2Client+0x45EA1 neither original nor D2DX's fps fix, game loop hook without waiting");
            return;
        }
        if (waited >= D2DX_WAIT_MS) {
            logLine("setup: D2DX's fps fix not seen within %d ms, game loop hook anyway", D2DX_WAIT_MS);
            return;
        }
        Sleep(20);
    }
}


static PVOID patchGuardBegin(void* at, SIZE_T size) {
    patchGuardAgain = 0;
    patchGuardSize = size;
    patchGuardAt = (BYTE*) at;
    patchGuardThread = GetCurrentThreadId();
    PVOID handler = AddVectoredExceptionHandler(1, patchGuardHandler);
    __asm__ __volatile__("" ::: "memory");
    return handler;
}


static LONG patchGuardEnd(PVOID handler) {
    __asm__ __volatile__("" ::: "memory");
    patchGuardThread = 0;
    patchGuardAt = NULL;
    if (handler) RemoveVectoredExceptionHandler(handler);
    return patchGuardAgain;
}
