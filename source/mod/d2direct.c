#define PKT_DIRECT             15
#define DIRECT_MARKER          "d2android-direct"
#define RVA_LaunchSinglePlayer 0x13A00
#define RVA_LaunchCharOk       0x199D0
#define RVA_LaunchDiffPress0   0x19D50
#define RVA_LaunchStartGame    0x19930
#define RVA_LaunchCharList     0x25AE0
#define RVA_LaunchCharSelected 0x20BD8
#define RVA_LaunchModeVar      0x25B48
#define RVA_LaunchBusyVar      0x25AC4
#define RVA_LaunchNewChar      0x111A0
#define RVA_LaunchToClasses    0xF830
#define RVA_LaunchTitlePress   0x11190
#define RVA_LaunchToMainMenu   0x107A0
#define D2GFX_GET_HWND         10007
#define DIRECT_NEW             ":neu"
#define CHAR_NAME              0x000
#define CHAR_STATUS            0x324
#define CHAR_NEXT              0x34C
#define CHAR_NAME_MAX          16
#define RVA_WinFirstControl    0x8DB34
#define CONTROL_FLAGS          0x08
#define CONTROL_ENABLED        0x01
#define CONTROL_PRESS          0x34
#define CONTROL_NEXT           0x3C
#define DIRECT_MSG             (WM_APP + 0x700)
#define DIRECT_ROUND_MS        50
#define DIRECT_MENU_LOOKS      2400
#define DIRECT_STEP_LOOKS      400
#define DIRECT_LIST_LOOKS      40
#define DIRECT_LOAD_ROUNDS     2400
#define DIRECT_WINDOW_ROUNDS   2400
#define DIRECT_CLASS_ROUNDS    200
#define DIRECT_PENDING_ROUNDS  2400
#define DIRECT_AFTER_ROUNDS    100
#define DIRECT_REPORT_ROUNDS   20
#define DIRECT_WATCH_MS        200

enum { DS_OFF, DS_RUNNING, DS_IN_GAME, DS_STOPPED, DS_LEFT };
enum { DSTEP_WINDOW, DSTEP_MAIN_MENU, DSTEP_SELECTION, DSTEP_DIFFICULTY, DSTEP_NEW, DSTEP_WORLD };
enum { DR_NONE, DR_MARKER, DR_VERSION, DR_NOT_LISTED, DR_DIFFICULTY, DR_TIME, DR_WINDOW, DR_NOT_PLAYABLE };

static volatile LONG directState, directStepNo, directReason, directEnded;
static char directName[CHAR_NAME_MAX + 1];
static int directDifficulty;
static int directNew;
static DWORD directLaunch, directWin;
static void* volatile* volatile directHeroVar;
static HWND directWindow;
static WNDPROC directOriginalProc;
static volatile LONG directPosted;
static volatile LONG directLoading;
static volatile LONG directPressedOk;
static int directChecked;
static int directInStep;
static int directMenuSeen, directLooks, directListLooks, directDialog, directPressedDiff;
static int directTitleOk;
static int directTitlePresses;
static DWORD* directTitlePressed;
static int directHeroSeen;
static volatile LONG directMenuNow;

static void directStop(int state, int reason, const char* why) {
    if (InterlockedExchange(&directEnded, 1)) return;
    InterlockedExchange(&directReason, reason);
    InterlockedExchange(&directState, state);
    logLine(state == DS_IN_GAME ? "direct start: %s" : "direct start: stopped, %s; the menus stay", why);
}

static int directLine(char** at, char* out, int max) {
    char* s = *at;
    char* end = strchr(s, '\n');
    int len = end ? (int) (end - s) : (int) strlen(s);
    *at = end ? end + 1 : s + len;
    if (len > 0 && s[len - 1] == '\r') len--;
    if (len > max) return -1;
    memcpy(out, s, len);
    out[len] = 0;
    return len;
}

static int directReadMarker(void) {
    char path[MAX_PATH];
    lstrcpynA(path, logPath, sizeof(path));
    char* slash = strrchr(path, '\\');
    lstrcpyA(slash ? slash + 1 : path, DIRECT_MARKER);
    HANDLE h = CreateFileA(path, GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_DELETE, NULL, OPEN_EXISTING, 0, NULL);
    if (h == INVALID_HANDLE_VALUE) return 0;
    char buf[160] = {0};
    DWORD n = 0;
    if (!ReadFile(h, buf, sizeof(buf) - 1, &n, NULL)) n = 0;
    CloseHandle(h);
    if (!DeleteFileA(path)) logLine("direct start: marker not deleted (%lu)", (unsigned long) GetLastError());
    buf[n] = 0;
    char* at = buf;
    char diff[4];
    int nameLen = directLine(&at, directName, CHAR_NAME_MAX - 1);
    int diffLen = directLine(&at, diff, 3);
    directState = DS_RUNNING;
    if (nameLen == 4 && strcmp(directName, DIRECT_NEW) == 0 && diffLen == 1 && diff[0] == '0' && n < sizeof(buf) - 1) {
        directNew = 1;
        logLine("direct start: new character");
        return 1;
    }
    if (nameLen < 2 || strpbrk(directName, "\\/:.") || diffLen != 1 || diff[0] < '0' || diff[0] > '2' || n >= sizeof(buf) - 1) {
        directStop(DS_STOPPED, DR_MARKER, "marker not as expected");
        return 1;
    }
    directDifficulty = diff[0] - '0';
    logLine("direct start: %s.d2s, difficulty %d", directName, directDifficulty);
    return 1;
}

static int directSitesOk(void) {
    DWORD L = directLaunch;
    const BYTE* single = (const BYTE*) (L + RVA_LaunchSinglePlayer);
    const BYTE* ok = (const BYTE*) (L + RVA_LaunchCharOk);
    const BYTE* start = (const BYTE*) (L + RVA_LaunchStartGame);
    if (!readable(single, 10) || !readable(ok, 5) || !readable(start, 6)) return 0;
    if (single[0] != 0xC7 || single[1] != 0x05 || *(const DWORD*) (single + 2) != L + RVA_LaunchModeVar
        || *(const DWORD*) (single + 6) != 0) return 0;
    if (ok[0] != 0xA1 || *(const DWORD*) (ok + 1) != L + RVA_LaunchBusyVar) return 0;
    if (start[0] != 0x8B || start[1] != 0x0D || *(const DWORD*) (start + 2) != L + RVA_LaunchCharSelected) return 0;
    for (int d = 0; d < 3; d++) {
        const BYTE* press = (const BYTE*) (L + RVA_LaunchDiffPress0 - 0x10 * d);
        if (!readable(press, 7) || press[0] != 0x6A || press[1] != d || press[2] != 0xE8
            || (DWORD) (press + 7) + *(const DWORD*) (press + 3) != L + RVA_LaunchStartGame) return 0;
    }
    const BYTE* newChar = (const BYTE*) (L + RVA_LaunchNewChar);
    if (!readable(newChar, 13) || newChar[0] != 0xE8 || (DWORD) (newChar + 5) + *(const DWORD*) (newChar + 1) != L + RVA_LaunchToClasses
        || newChar[5] != 0xB8 || *(const DWORD*) (newChar + 6) != 1 || newChar[10] != 0xC2) return 0;
    return readable((const void*) (L + RVA_LaunchCharList), 4) && readable((const void*) (L + RVA_LaunchCharSelected), 4)
        && readable((const void*) (directWin + RVA_WinFirstControl), 4);
}

static DWORD* directControl(DWORD rva) {
    DWORD* c = *(DWORD**) (directWin + RVA_WinFirstControl);
    for (int i = 0; c && i < 200; i++) {
        if (!readable(c, CONTROL_NEXT + 4)) return NULL;
        if (c[CONTROL_PRESS / 4] == directLaunch + rva) return c;
        c = (DWORD*) c[CONTROL_NEXT / 4];
    }
    return NULL;
}

static int directFind(BYTE** entry) {
    BYTE* e = *(BYTE**) (directLaunch + RVA_LaunchCharList);
    int found = -1;
    for (int i = 0; e && i < 256; i++) {
        if (!readable(e, CHAR_NEXT + 4)) return -1;
        char name[CHAR_NAME_MAX + 1];
        memcpy(name, e + CHAR_NAME, CHAR_NAME_MAX);
        name[CHAR_NAME_MAX] = 0;
        if (lstrcmpiA(name, directName) == 0) {
            if (found >= 0) return -2;
            found = i;
            *entry = e;
        }
        e = *(BYTE**) (e + CHAR_NEXT);
    }
    return found;
}

typedef BOOL (__stdcall* PressFn)(void*);

static void directPress(DWORD rva) {
    directLooks = 0;
    ((PressFn) (directLaunch + rva))(NULL);
}

static void directSelect(DWORD* ok) {
    InterlockedExchange(&directStepNo, DSTEP_SELECTION);
    BYTE* e = NULL;
    int index = directFind(&e);
    int enabled = (ok[CONTROL_FLAGS / 4] & CONTROL_ENABLED) != 0;
    if (index == -2) { directStop(DS_STOPPED, DR_NOT_LISTED, "character twice in the list"); return; }
    if (index < 0 || !enabled) {
        if (++directListLooks < DIRECT_LIST_LOOKS) return;
        if (index < 0) directStop(DS_STOPPED, DR_NOT_LISTED, "character not in the list");
        else directStop(DS_STOPPED, DR_NOT_PLAYABLE, "OK not enabled");
        return;
    }
    unsigned status = *(const WORD*) (e + CHAR_STATUS);
    if ((status & 0x0C) == 0x0C) { directStop(DS_STOPPED, DR_NOT_PLAYABLE, "a dead hardcore character"); return; }
    int open = ((status >> 8) & 0x1F) / (status & 0x20 ? 5 : 4);
    if (directDifficulty > open) { directStop(DS_STOPPED, DR_DIFFICULTY, "difficulty not open for this character"); return; }
    directDialog = open > 0;
    *(LONG*) (directLaunch + RVA_LaunchCharSelected) = index;
    InterlockedExchange(&directPressedOk, 1);
    if (!directDialog) InterlockedExchange(&directLoading, 1);
    logLine("direct start: entry %d selected (status %04X), OK pressed%s", index, status,
            directDialog ? ", a dialog for the difficulty" : "");
    directPress(RVA_LaunchCharOk);
    if (!directDialog && directState == DS_RUNNING) InterlockedExchange(&directStepNo, DSTEP_WORLD);
}

static void directPressDifficulty(DWORD* diff, int expected) {
    InterlockedExchange(&directStepNo, DSTEP_DIFFICULTY);
    if (!(diff[CONTROL_FLAGS / 4] & CONTROL_ENABLED)) {
        directStop(DS_STOPPED, DR_DIFFICULTY, "difficulty not open for this character");
        return;
    }
    directPressedDiff = 1;
    InterlockedExchange(&directLoading, 1);
    logLine("direct start: difficulty %d pressed%s", directDifficulty, expected ? "" : " (a dialog not expected)");
    directPress(RVA_LaunchDiffPress0 - 0x10 * directDifficulty);
    if (directState == DS_RUNNING) InterlockedExchange(&directStepNo, DSTEP_WORLD);
}

static int directPressedNew;

static void directStepNew(void) {
    if (directPressedNew) {
        if (!directControl(RVA_LaunchNewChar) && !directControl(RVA_LaunchCharOk)) {
            InterlockedExchange(&directStepNo, DSTEP_NEW);
            directStop(DS_IN_GAME, DR_NONE, "at the class selection of a new character");
        }
        return;
    }
    DWORD* newChar = directControl(RVA_LaunchNewChar);
    if (newChar) {
        InterlockedExchange(&directStepNo, DSTEP_SELECTION);
        if (!(newChar[CONTROL_FLAGS / 4] & CONTROL_ENABLED)) {
            if (++directListLooks >= DIRECT_LIST_LOOKS) directStop(DS_STOPPED, DR_NOT_PLAYABLE, "New Character not enabled");
            return;
        }
        directPressedNew = 1;
        logLine("direct start: New Character pressed");
        directPress(RVA_LaunchNewChar);
        return;
    }
    if (directControl(RVA_LaunchCharOk)) {
        InterlockedExchange(&directStepNo, DSTEP_SELECTION);
        if (++directListLooks >= DIRECT_LIST_LOOKS) directStop(DS_STOPPED, DR_NOT_PLAYABLE, "no New Character in the selection");
        return;
    }
    if (!directMenuSeen) {
        if (directControl(RVA_LaunchSinglePlayer)) {
            InterlockedExchange(&directStepNo, DSTEP_MAIN_MENU);
            directMenuSeen = 1;
            logLine("direct start: Single Player pressed");
            directPress(RVA_LaunchSinglePlayer);
        }
        return;
    }
    if (!directControl(RVA_LaunchSinglePlayer) && ++directListLooks >= DIRECT_LIST_LOOKS) {
        InterlockedExchange(&directStepNo, DSTEP_NEW);
        directStop(DS_IN_GAME, DR_NONE, "at the class selection (no characters yet)");
    }
}

static void directStep(void) {
    if (directState != DS_RUNNING) return;
    if (!directChecked) {
        directChecked = directSitesOk() ? 1 : -1;
        if (directChecked < 0) { directStop(DS_STOPPED, DR_VERSION, "D2Launch not as known (another version?)"); return; }
        const BYTE* t = (const BYTE*) (directLaunch + RVA_LaunchTitlePress);
        directTitleOk = readable(t, 13) && t[0] == 0xE8 && (DWORD) (t + 5) + *(const DWORD*) (t + 1) == directLaunch + RVA_LaunchToMainMenu
            && t[5] == 0xB8 && *(const DWORD*) (t + 6) == 1 && t[10] == 0xC2 && t[11] == 4 && t[12] == 0;
        if (!directTitleOk) logLine("direct start: title screen press not as known, it goes on by itself");
    }
    DWORD diffPress = RVA_LaunchDiffPress0 - 0x10 * directDifficulty;
    if (directLoading) {
        if (directControl(RVA_LaunchSinglePlayer))
            directStop(DS_STOPPED, DR_NOT_PLAYABLE, "back in the main menu, the game did not take the character");
        else if (!directPressedDiff && directControl(diffPress)) directPressDifficulty(directControl(diffPress), 0);
        return;
    }
    if (++directLooks > (directMenuSeen ? DIRECT_STEP_LOOKS : DIRECT_MENU_LOOKS)) {
        directStop(DS_STOPPED, DR_TIME, directMenuSeen ? "no progress" : "no main menu");
        return;
    }
    DWORD* title = !directMenuSeen && directTitleOk && directTitlePresses < 3 ? directControl(RVA_LaunchTitlePress) : NULL;
    if (title && title != directTitlePressed) {
        directTitlePressed = title;
        directTitlePresses++;
        logLine("direct start: title screen pressed");
        directPress(RVA_LaunchTitlePress);
        return;
    }
    if (directNew) {
        directStepNew();
        return;
    }
    if (directPressedOk) {
        DWORD* diff = directDialog && !directPressedDiff ? directControl(diffPress) : NULL;
        if (diff) directPressDifficulty(diff, 1);
        return;
    }
    DWORD* ok = directControl(RVA_LaunchCharOk);
    if (ok) {
        directSelect(ok);
        return;
    }
    if (!directMenuSeen && directControl(RVA_LaunchSinglePlayer)) {
        InterlockedExchange(&directStepNo, DSTEP_MAIN_MENU);
        directMenuSeen = 1;
        logLine("direct start: Single Player pressed");
        directPress(RVA_LaunchSinglePlayer);
    }
}

static LRESULT CALLBACK directProc(HWND window, UINT msg, WPARAM w, LPARAM l) {
    if (msg == DIRECT_MSG) {
        InterlockedExchange(&directPosted, 0);
        if (directState == DS_IN_GAME) {
            InterlockedExchange(&directMenuNow, directControl(RVA_LaunchSinglePlayer) || directControl(RVA_LaunchCharOk) ? 1 : 0);
            return 0;
        }
        if (!directInStep) {
            directInStep = 1;
            directStep();
            directInStep = 0;
        }
        return 0;
    }
    return CallWindowProcA(directOriginalProc, window, msg, w, l);
}

static HWND directGfxWindow(void) {
    static const BYTE* fn;
    static int checked;
    HMODULE gfx = GetModuleHandleA("D2Gfx.dll");
    if (!gfx) return NULL;
    if (!checked) {
        checked = 1;
        const BYTE* f = (const BYTE*) GetProcAddress(gfx, (LPCSTR) (DWORD_PTR) D2GFX_GET_HWND);
        DWORD var = f && readable(f, 6) ? *(const DWORD*) (f + 1) : 0;
        if (f && f[0] == 0xA1 && f[5] == 0xC3 && var > (DWORD) gfx && var < (DWORD) gfx + 0x100000) fn = f;
        logLine(fn ? "direct start: D2Gfx's window variable as known" : "direct start: D2Gfx GetHwnd not as known, window by class");
    }
    const HWND* var = fn ? *(HWND* const*) (fn + 1) : NULL;
    return var && readable(var, 4) ? *var : NULL;
}

typedef struct { int anyClass; HWND window; } DirectWindowSearch;

static BOOL CALLBACK directFindWindow(HWND window, LPARAM arg) {
    DirectWindowSearch* search = (DirectWindowSearch*) arg;
    DWORD pid = 0;
    GetWindowThreadProcessId(window, &pid);
    if (pid != GetCurrentProcessId() || !IsWindowVisible(window)) return TRUE;
    char cls[32] = {0};
    GetClassNameA(window, cls, sizeof(cls));
    if (!search->anyClass && strcmp(cls, "Diablo II") != 0) return TRUE;
    search->window = window;
    return FALSE;
}

static void directSend(void) {
    Packet p;
    putHeader(&p, PKT_DIRECT);
    putU8(&p, (unsigned) directState);
    putU8(&p, (unsigned) directStepNo);
    putU8(&p, (unsigned) directReason);
    sendPacket(&p);
}

static DWORD WINAPI directThread(LPVOID arg) {
    (void) arg;
    int loadRounds = 0, pendingRounds = 0, ended = 0, lastSent = -DIRECT_REPORT_ROUNDS;
    LONG sentState = -1, sentStep = -1;
    for (int rounds = 0;; rounds++) {
        if (directState == DS_RUNNING) {
            if (!directLaunch) directLaunch = (DWORD) GetModuleHandleA("D2Launch.dll");
            if (!directWin) directWin = (DWORD) GetModuleHandleA("D2Win.dll");
            void* volatile* hero = directHeroVar;
            if (directPressedOk && ((hero && *hero) || (!hero && lastTick))) directStop(DS_IN_GAME, DR_NONE, "in the game");
            else if (directLoading && ++loadRounds > DIRECT_LOAD_ROUNDS) directStop(DS_STOPPED, DR_TIME, "no game after the start");
        }
        if (directState == DS_RUNNING && !directWindow) {
            DirectWindowSearch search = {rounds >= DIRECT_CLASS_ROUNDS, NULL};
            HWND own = directGfxWindow();
            DWORD ownPid = 0;
            if (own && IsWindow(own)) GetWindowThreadProcessId(own, &ownPid);
            if (directLaunch && directWin && own && ownPid == GetCurrentProcessId() && IsWindowVisible(own)) search.window = own;
            else if (directLaunch && directWin) EnumWindows(directFindWindow, (LPARAM) &search);
            if (search.window) {
                char cls[32] = {0};
                GetClassNameA(search.window, cls, sizeof(cls));
                directOriginalProc = (WNDPROC) SetWindowLongA(search.window, GWL_WNDPROC, (LONG) directProc);
                if (directOriginalProc) {
                    directWindow = search.window;
                    logLine("direct start: game window found (class %s%s)", cls, search.window == own ? ", D2Gfx's" : "");
                }
                else directStop(DS_STOPPED, DR_WINDOW, "game window not subclassed");
            }
            else if (rounds > DIRECT_WINDOW_ROUNDS) directStop(DS_STOPPED, DR_WINDOW, "no game window");
        }
        if (directState == DS_RUNNING && directWindow) {
            if (!directPosted) {
                pendingRounds = 0;
                InterlockedExchange(&directPosted, 1);
                if (!PostMessageA(directWindow, DIRECT_MSG, 0, 0)) directStop(DS_STOPPED, DR_WINDOW, "game window gone");
            }
            else if (++pendingRounds > DIRECT_PENDING_ROUNDS) directStop(DS_STOPPED, DR_TIME, "the game does not look");
        }
        if (directState == DS_IN_GAME && directHeroVar) {
            void* volatile* hero = directHeroVar;
            if (*hero) {
                directHeroSeen = 1;
                InterlockedExchange(&directMenuNow, 0);
            }
            else if (directHeroSeen && directMenuNow) {
                directHeroSeen = 0;
                InterlockedExchange(&directStepNo, 0);
                InterlockedExchange(&directState, DS_LEFT);
                logLine("direct start: the game left, in the menus");
                ended = 0;
            }
            else if (directHeroSeen && directWindow && !directPosted) {
                InterlockedExchange(&directPosted, 1);
                if (!PostMessageA(directWindow, DIRECT_MSG, 0, 0)) InterlockedExchange(&directPosted, 0);
            }
        }
        int watching = directState == DS_IN_GAME && directHeroVar;
        if (directState != DS_RUNNING && !ended) ended = rounds ? rounds : 1;
        if (watching && ended && rounds - ended >= DIRECT_AFTER_ROUNDS) {
            sentState = directState;
            sentStep = directStepNo;
            lastSent = rounds;
            Sleep(DIRECT_WATCH_MS);
            continue;
        }
        if (directState != sentState || directStepNo != sentStep || rounds - lastSent >= DIRECT_REPORT_ROUNDS) {
            directSend();
            sentState = directState;
            sentStep = directStepNo;
            lastSent = rounds;
        }
        if (ended && rounds - ended >= DIRECT_AFTER_ROUNDS) break;
        Sleep(DIRECT_ROUND_MS);
    }
    return 0;
}

static void directBegin(void) {
    if (!directReadMarker()) return;
    HANDLE thread = CreateThread(NULL, 0, directThread, NULL, 0, NULL);
    if (thread) CloseHandle(thread);
    else directStop(DS_STOPPED, DR_WINDOW, "no thread");
}
