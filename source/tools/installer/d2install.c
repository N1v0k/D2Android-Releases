
















#include <windows.h>
#include <stdio.h>
#include <string.h>
static int found, wrongKey;

static void note(const char *what, long value) {
    char path[MAX_PATH]; DWORD n = GetModuleFileNameA(NULL, path, MAX_PATH);
    if (!n || n >= MAX_PATH) return;
    char *slash = strrchr(path, '\\'); if (!slash) return;
    strcpy(slash + 1, "d2install.log");
    FILE *f = fopen(path, "a"); if (!f) return;
    fprintf(f, "%lu %s %ld\n", (unsigned long)GetTickCount(), what, value); fclose(f);
}




static const char *GO[] = {"Agree", "I Agree", "OK", "Install", "Ja", "Zustimmen", "Installieren",
                           "J'accepte", "Installer", "Aceptar", "Instalar", "S\xec", "Installa", NULL};
static int isGoButton(const char *txt) {
    char plain[256]; int n = 0;
    for (const char *p = txt; *p && n < 255; p++) if (*p != '&') plain[n++] = *p;
    plain[n] = 0;
    for (int i = 0; GO[i]; i++) if (!_stricmp(plain, GO[i])) return 1;
    return 0;
}




static int wrongKeyText(const char *txt) {
    return strstr(txt, "not correct") || strstr(txt, "invalid") || strstr(txt, "nicht korrekt") || strstr(txt, "ung\xfcltig")
        || strstr(txt, "pas correctes") || strstr(txt, "est incorrecte")
        || strstr(txt, "no es correcta") || (strstr(txt, "clave") && strstr(txt, "no es v\xe1lida"))
        || strstr(txt, "non sono corrette") || (strstr(txt, "chiave") && strstr(txt, "non \xe8 valida"));
}
static BOOL CALLBACK child(HWND w, LPARAM l) {
    char cls[64], txt[256];
    GetClassNameA(w, cls, sizeof cls); GetWindowTextA(w, txt, sizeof txt);
    if (wrongKeyText(txt)) wrongKey = 1;
    if (!_stricmp(cls, "Button") && isGoButton(txt)) {
        EnableWindow(w, TRUE);
        SendMessageA(w, BM_CLICK, 0, 0);
        printf("clicked '%s'\n", txt); fflush(stdout); note(txt, 0);
        found = 1; return FALSE;
    }
    return TRUE;
}



static unsigned long long folderBytes(const char *dir, int depth) {
    char pattern[MAX_PATH + 4], sub[MAX_PATH]; WIN32_FIND_DATAA d; unsigned long long sum = 0;
    if (strlen(dir) + 3 >= sizeof pattern) return 0;
    strcpy(pattern, dir); strcat(pattern, "\\*");
    HANDLE f = FindFirstFileA(pattern, &d);
    if (f == INVALID_HANDLE_VALUE) return 0;
    do {
        if (!(d.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) sum += ((unsigned long long)d.nFileSizeHigh << 32) | d.nFileSizeLow;
        else if (depth > 0 && strcmp(d.cFileName, ".") && strcmp(d.cFileName, "..") && strlen(dir) + strlen(d.cFileName) + 2 < sizeof sub) {
            strcpy(sub, dir); strcat(sub, "\\"); strcat(sub, d.cFileName);
            sum += folderBytes(sub, depth - 1);
        }
    } while (FindNextFileA(f, &d));
    FindClose(f);
    return sum;
}

static int menuVisible;

static int others;
static BOOL CALLBACK count(HWND w, LPARAM l) {
    char cls[64], txt[256]; DWORD wp = 0;
    if (!IsWindowVisible(w)) return TRUE;
    GetWindowThreadProcessId(w, &wp);
    if (wp == GetCurrentProcessId()) return TRUE;
    GetClassNameA(w, cls, sizeof cls); GetWindowTextA(w, txt, sizeof txt);
    if (!txt[0] || !_stricmp(cls, "ConsoleWindowClass")) return TRUE;
    char self[MAX_PATH] = ""; GetModuleFileNameA(NULL, self, MAX_PATH);
    if (self[0] && strstr(txt, self)) return TRUE;
    others++;
    return TRUE;
}
static BOOL CALLBACK top(HWND w, LPARAM l) {
    char txt[256]; DWORD pid = (DWORD)l, wp = 0;
    if (!IsWindowVisible(w)) return TRUE;
    GetWindowThreadProcessId(w, &wp);
    if (pid && wp != pid) return TRUE;
    GetWindowTextA(w, txt, sizeof txt);
    if (wrongKeyText(txt)) wrongKey = 1;
    char cls[64]; GetClassNameA(w, cls, sizeof cls);
    if (!strncmp(cls, "(Blizzard Entertainment)", 24) && strstr(cls, "Installer")) menuVisible = 1;
    EnumChildWindows(w, child, 0);
    return !found;
}
int main(int argc, char **argv) {
    int secs = argc > 1 ? atoi(argv[1]) : 600;
    DWORD pid = 0;
    HANDLE child = NULL;
    int next = 2, fromStdin = 0, linger = 0, lingering = 0, quiet = 0, lingered = 0, lingerClicks = 0;
    int noStart = 0, lastClick = 0, progressed = 0, reported = 0, menuReported = 0, menuSince = -1;
    char key[64] = "", dest[MAX_PATH] = "";
    unsigned long long destStart = 0;
    if (argc > next + 1 && !strcmp(argv[next], "--no-start")) { noStart = atoi(argv[next + 1]); next += 2; }
    if (argc > next && !strcmp(argv[next], "--keys-from-stdin")) {
        fromStdin = 1; next++;
        if (!fgets(key, sizeof key, stdin)) { printf("no key on stdin\n"); return 5; }
        key[strcspn(key, "\r\n")] = 0;
        if (!key[0]) { printf("empty key on stdin\n"); return 5; }
    }




    else if (argc > next + 1 && !strcmp(argv[next], "--key-from")) {
        FILE *pipe = fopen(argv[next + 1], "r");
        if (!pipe) { note("pipe-open-failed", (long)GetLastError()); printf("cannot open the key pipe\n"); return 5; }
        char *got = fgets(key, sizeof key, pipe);
        fclose(pipe);
        if (!got) { note("pipe-empty", 0); printf("no key in the pipe\n"); return 5; }
        key[strcspn(key, "\r\n")] = 0;
        if (!key[0]) { printf("empty key in the pipe\n"); return 5; }
        note("key-read-length", (long)strlen(key));
        fromStdin = 1; next += 2;
    }
    if (argc > next && !strcmp(argv[next], "--linger")) { linger = 1; next++; }


    if (argc > next + 1 && !strcmp(argv[next], "--run")) {
        char cmd[4096] = "";
        for (int i = next + 1; i < argc; i++) {
            if (!strncmp(argv[i], "--destination=", 14) && strlen(argv[i] + 14) < sizeof dest) strcpy(dest, argv[i] + 14);
            if (i > next + 1) strcat(cmd, " ");
            if (strchr(argv[i], ' ')) { strcat(cmd, "\""); strcat(cmd, argv[i]); strcat(cmd, "\""); }
            else strcat(cmd, argv[i]);
        }
        if (fromStdin) { strcat(cmd, " --key1="); strcat(cmd, key); }
        STARTUPINFOA si; memset(&si, 0, sizeof si); si.cb = sizeof si;
        PROCESS_INFORMATION pi;


        char dir[MAX_PATH] = "";
        const char *slash = strrchr(argv[next + 1], '\\');
        if (slash && (size_t)(slash - argv[next + 1]) < sizeof dir) {
            memcpy(dir, argv[next + 1], slash - argv[next + 1]); dir[slash - argv[next + 1]] = 0;
            SetCurrentDirectoryA(dir);
        }
        note("working-folder-set", dir[0] ? 1 : 0);
        if (!CreateProcessA(NULL, cmd, NULL, NULL, FALSE, 0, NULL, dir[0] ? dir : NULL, &si, &pi)) { SecureZeroMemory(cmd, sizeof cmd); SecureZeroMemory(key, sizeof key); note("start-failed", (long)GetLastError()); printf("cannot start %s\n", argv[next + 1]); return 4; }
        note("started", (long)pi.dwProcessId);
        if (noStart && dest[0]) { destStart = folderBytes(dest, 6); note("watching-destination", noStart); }
        SecureZeroMemory(cmd, sizeof cmd); SecureZeroMemory(key, sizeof key);
        pid = pi.dwProcessId; child = pi.hProcess; CloseHandle(pi.hThread);
        printf("started %s\n", argv[next + 1]); fflush(stdout);
    }
    else if (argc > next) pid = (DWORD)atoi(argv[next]);
    for (int i = 0; i < secs; i++) {
        found = 0; menuVisible = 0;
        EnumWindows(top, (LPARAM)pid);
        if (wrongKey) {

            if (child) TerminateProcess(child, 3);
            note("wrong-key", 0); printf("wrong key\n"); return 3;
        }
        if (found) lastClick = i;


        if (child && noStart && dest[0] && !progressed) {
            menuSince = menuVisible ? (menuSince < 0 ? i : menuSince) : -1;
            if (folderBytes(dest, 6) >= destStart + 1048576) { progressed = 1; note("progress", i); }
            else {
                if (!menuReported && menuSince >= 0 && i - menuSince >= 10 && i - lastClick >= 20) {
                    menuReported = 1; note("start-menu", i - lastClick); printf("start menu, no data in the destination\n"); fflush(stdout);
                }
                if (!reported && i - lastClick >= noStart) {
                    reported = 1; note("no-progress", i - lastClick); printf("no progress in the destination yet\n"); fflush(stdout);
                }
            }
        }
        if (child) {
            if (WaitForSingleObject(child, 0) == WAIT_OBJECT_0) {
                DWORD rc = 0; GetExitCodeProcess(child, &rc); printf("ended %lu\n", rc); fflush(stdout); note("child-ended", (long)rc);
                if (!linger) return 0;


                child = NULL; pid = 0; lingering = 1;
            }
        }
        else if (lingering) {


            if (found) lingerClicks++;
            others = 0;
            EnumWindows(count, 0);
            quiet = others ? 0 : quiet + 1;
            lingered++;
            if ((quiet >= 5 && lingerClicks > 0) || (quiet >= 5 && lingered >= 180)) {
                note("linger-done", lingerClicks); printf("no windows left\n"); return 0;
            }
        }
        else if (pid) { HANDLE h = OpenProcess(SYNCHRONIZE, FALSE, pid); if (!h) return 0; if (WaitForSingleObject(h, 0) == WAIT_OBJECT_0) { CloseHandle(h); return 0; } CloseHandle(h); }
        Sleep(1000);
    }
    return 2;
}
