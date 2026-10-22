#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <tlhelp32.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>

static char logPath[MAX_PATH];

static void logLine(const char* fmt, ...) {
    FILE* f = fopen(logPath, "a");
    if (!f) return;
    SYSTEMTIME t;
    GetLocalTime(&t);
    fprintf(f, "%02d:%02d:%02d.%03d ", t.wHour, t.wMinute, t.wSecond, t.wMilliseconds);
    va_list args;
    va_start(args, fmt);
    vfprintf(f, fmt, args);
    va_end(args);
    fputc('\n', f);
    fclose(f);
}

static void keepAsPrevious(const char* path, const char* base) {
    char prev[MAX_PATH];
    const char* slash = strrchr(path, '\\');
    int dirLength = slash ? (int) (slash - path + 1) : 0;
    snprintf(prev, sizeof(prev), "%.*s%s.prev.log", dirLength, path, base);
    if (!MoveFileExA(path, prev, MOVEFILE_REPLACE_EXISTING)) {
        if (GetLastError() == ERROR_FILE_NOT_FOUND) DeleteFileA(prev);
        DeleteFileA(path);
    }
}

static const char* passedArguments(void) {
    const char* cmd = GetCommandLineA();
    if (*cmd == '"') {
        cmd++;
        while (*cmd && *cmd != '"') cmd++;
        if (*cmd == '"') cmd++;
    }
    else {
        while (*cmd && *cmd != ' ' && *cmd != '\t') cmd++;
    }
    while (*cmd == ' ' || *cmd == '\t') cmd++;
    return cmd;
}

typedef struct {
    DWORD pid;
    int found;
} WindowSearch;

static BOOL CALLBACK findWindow(HWND hwnd, LPARAM lparam) {
    WindowSearch* search = (WindowSearch*) lparam;
    DWORD pid = 0;
    GetWindowThreadProcessId(hwnd, &pid);
    if (pid == search->pid && IsWindowVisible(hwnd)) {
        search->found = 1;
        return FALSE;
    }
    return TRUE;
}

static FARPROC remoteLoadLibrary(DWORD pid) {
    HMODULE local = GetModuleHandleA("kernel32.dll");
    FARPROC fn = GetProcAddress(local, "LoadLibraryA");
    HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid);
    if (snap == INVALID_HANDLE_VALUE) return fn;
    MODULEENTRY32 entry;
    entry.dwSize = sizeof(entry);
    FARPROC result = fn;
    if (Module32First(snap, &entry)) {
        do {
            if (lstrcmpiA(entry.szModule, "kernel32.dll") == 0) {
                result = (FARPROC) ((BYTE*) entry.modBaseAddr + ((BYTE*) fn - (BYTE*) local));
                break;
            }
        } while (Module32Next(snap, &entry));
    }
    CloseHandle(snap);
    return result;
}

static int inject(HANDLE process, DWORD pid, const char* dllPath) {
    SIZE_T size = strlen(dllPath) + 1;
    void* remote = VirtualAllocEx(process, NULL, size, MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
    if (!remote) {
        logLine("VirtualAllocEx failed: %lu", GetLastError());
        return 0;
    }
    if (!WriteProcessMemory(process, remote, dllPath, size, NULL)) {
        logLine("WriteProcessMemory failed: %lu", GetLastError());
        return 0;
    }
    FARPROC loadLibrary = remoteLoadLibrary(pid);
    HANDLE thread = CreateRemoteThread(process, NULL, 0, (LPTHREAD_START_ROUTINE) loadLibrary, remote, 0, NULL);
    if (!thread) {
        logLine("CreateRemoteThread failed: %lu", GetLastError());
        return 0;
    }
    DWORD wait = WaitForSingleObject(thread, 15000);
    DWORD module = 0;
    GetExitCodeThread(thread, &module);
    CloseHandle(thread);
    logLine("LoadLibraryA in game: wait=%lu module=0x%08lx", wait, module);
    return wait == WAIT_OBJECT_0 && module != 0 && module != STILL_ACTIVE;
}

static int injectByThread(HANDLE process, HANDLE mainThread, DWORD pid, const char* dllPath) {
    SIZE_T pathSize = strlen(dllPath) + 1;
    BYTE* remote = (BYTE*) VirtualAllocEx(process, NULL, 64 + pathSize, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
    if (!remote) {
        logLine("hijack: VirtualAllocEx failed: %lu", GetLastError());
        return 0;
    }
    if (SuspendThread(mainThread) == (DWORD) -1) {
        logLine("hijack: SuspendThread failed: %lu", GetLastError());
        return 0;
    }
    CONTEXT ctx;
    memset(&ctx, 0, sizeof(ctx));
    ctx.ContextFlags = CONTEXT_CONTROL;
    if (!GetThreadContext(mainThread, &ctx)) {
        logLine("hijack: GetThreadContext failed: %lu", GetLastError());
        ResumeThread(mainThread);
        return 0;
    }
    DWORD path = (DWORD) remote + 64, loadLibrary = (DWORD) remoteLoadLibrary(pid);
    BYTE code[64];
    int n = 0;
    code[n++] = 0x9C;
    code[n++] = 0x60;
    code[n++] = 0x68; memcpy(code + n, &path, 4); n += 4;
    code[n++] = 0xB8; memcpy(code + n, &loadLibrary, 4); n += 4;
    code[n++] = 0xFF; code[n++] = 0xD0;
    code[n++] = 0x61;
    code[n++] = 0x9D;
    code[n++] = 0x68; memcpy(code + n, &ctx.Eip, 4); n += 4;
    code[n++] = 0xC3;
    if (!WriteProcessMemory(process, remote, code, n, NULL) || !WriteProcessMemory(process, remote + 64, dllPath, pathSize, NULL)) {
        logLine("hijack: WriteProcessMemory failed: %lu", GetLastError());
        ResumeThread(mainThread);
        return 0;
    }
    FlushInstructionCache(process, remote, n);
    DWORD oldEip = ctx.Eip;
    ctx.Eip = (DWORD) remote;
    if (!SetThreadContext(mainThread, &ctx)) {
        logLine("hijack: SetThreadContext failed: %lu", GetLastError());
        ResumeThread(mainThread);
        return 0;
    }
    ResumeThread(mainThread);
    logLine("hijack: main thread redirected (eip 0x%08lx)", oldEip);
    return 1;
}

static DWORD findLoadLibraryIat(const BYTE* file, DWORD size, DWORD* imageBase, DWORD* entry) {
    if (size < 0x400 || file[0] != 'M' || file[1] != 'Z') return 0;
    DWORD pe = *(const DWORD*) (file + 0x3C);
    if (pe + 0xF8 > size || memcmp(file + pe, "PE\0\0", 4) != 0) return 0;
    const IMAGE_NT_HEADERS32* nt = (const IMAGE_NT_HEADERS32*) (file + pe);
    *imageBase = nt->OptionalHeader.ImageBase;
    *entry = nt->OptionalHeader.AddressOfEntryPoint;
    const IMAGE_SECTION_HEADER* sections = IMAGE_FIRST_SECTION(nt);
    int count = nt->FileHeader.NumberOfSections;
    #define OFFSET(rva, out) do { out = 0; for (int s_ = 0; s_ < count; s_++) { \
        DWORD va_ = sections[s_].VirtualAddress, vs_ = sections[s_].Misc.VirtualSize > sections[s_].SizeOfRawData ? sections[s_].Misc.VirtualSize : sections[s_].SizeOfRawData; \
        if ((rva) >= va_ && (rva) < va_ + vs_) { out = (rva) - va_ + sections[s_].PointerToRawData; break; } } } while (0)
    DWORD importRva = nt->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_IMPORT].VirtualAddress, at;
    OFFSET(importRva, at);
    if (!at) return 0;
    for (const IMAGE_IMPORT_DESCRIPTOR* d = (const IMAGE_IMPORT_DESCRIPTOR*) (file + at); (const BYTE*) (d + 1) <= file + size && d->Name; d++) {
        DWORD nameAt;
        OFFSET(d->Name, nameAt);
        if (!nameAt || lstrcmpiA((const char*) file + nameAt, "KERNEL32.dll") != 0) continue;
        DWORD thunkAt;
        OFFSET(d->OriginalFirstThunk ? d->OriginalFirstThunk : d->FirstThunk, thunkAt);
        if (!thunkAt) return 0;
        for (DWORD i = 0; thunkAt + 4 * i + 4 <= size; i++) {
            DWORD thunk = *(const DWORD*) (file + thunkAt + 4 * i);
            if (!thunk) break;
            if (thunk & 0x80000000) continue;
            DWORD byName;
            OFFSET(thunk, byName);
            if (byName && strcmp((const char*) file + byName + 2, "LoadLibraryA") == 0) return *imageBase + d->FirstThunk + 4 * i;
        }
    }
    #undef OFFSET
    return 0;
}

static int injectAtEntry(HANDLE process, const char* gamePath, const char* dllPath) {
    HANDLE f = CreateFileA(gamePath, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, 0, NULL);
    if (f == INVALID_HANDLE_VALUE) return 0;
    static BYTE file[1 << 20];
    DWORD size = 0;
    ReadFile(f, file, sizeof(file), &size, NULL);
    CloseHandle(f);
    DWORD imageBase = 0, entryRva = 0;
    DWORD iat = findLoadLibraryIat(file, size, &imageBase, &entryRva);
    if (!iat || !entryRva) {
        logLine("early: LoadLibraryA import not found in Game.exe");
        return 0;
    }
    DWORD entry = imageBase + entryRva;
    BYTE original[5];
    SIZE_T got = 0;
    if (!ReadProcessMemory(process, (void*) entry, original, 5, &got) || got != 5) {
        logLine("early: entry point not readable: %lu", GetLastError());
        return 0;
    }
    SIZE_T pathSize = strlen(dllPath) + 1;
    BYTE* remote = (BYTE*) VirtualAllocEx(process, NULL, 128 + pathSize, MEM_COMMIT | MEM_RESERVE, PAGE_EXECUTE_READWRITE);
    if (!remote) {
        logLine("early: VirtualAllocEx failed: %lu", GetLastError());
        return 0;
    }
    DWORD saved = (DWORD) remote + 96, path = (DWORD) remote + 128;
    BYTE code[96];
    int n = 0;
    code[n++] = 0x60;
    code[n++] = 0x9C;
    code[n++] = 0x68; memcpy(code + n, &path, 4); n += 4;
    code[n++] = 0xFF; code[n++] = 0x15; memcpy(code + n, &iat, 4); n += 4;
    code[n++] = 0xBE; memcpy(code + n, &saved, 4); n += 4;
    code[n++] = 0xBF; memcpy(code + n, &entry, 4); n += 4;
    code[n++] = 0xB9; code[n++] = 5; code[n++] = 0; code[n++] = 0; code[n++] = 0;
    code[n++] = 0xFC;
    code[n++] = 0xF3; code[n++] = 0xA4;
    code[n++] = 0x9D;
    code[n++] = 0x61;
    code[n++] = 0x68; memcpy(code + n, &entry, 4); n += 4;
    code[n++] = 0xC3;
    BYTE jump[5] = {0xE9};
    DWORD rel = (DWORD) remote - (entry + 5);
    memcpy(jump + 1, &rel, 4);
    DWORD old;
    if (!WriteProcessMemory(process, remote, code, n, NULL) || !WriteProcessMemory(process, (void*) saved, original, 5, NULL)
        || !WriteProcessMemory(process, (void*) path, dllPath, pathSize, NULL)) {
        logLine("early: WriteProcessMemory failed: %lu", GetLastError());
        return 0;
    }
    if (!VirtualProtectEx(process, (void*) entry, 5, PAGE_EXECUTE_READWRITE, &old) || !WriteProcessMemory(process, (void*) entry, jump, 5, NULL)) {
        logLine("early: entry point not patchable: %lu", GetLastError());
        return 0;
    }
    FlushInstructionCache(process, (void*) entry, 5);
    FlushInstructionCache(process, remote, n);
    logLine("early: entry point 0x%08lx jumps to the loader stub (LoadLibraryA at [0x%08lx])", entry, iat);
    return 1;
}

int main(void) {
    char dir[MAX_PATH], gamePath[MAX_PATH], dllPath[MAX_PATH], cmdLine[2048];
    GetModuleFileNameA(NULL, dir, sizeof(dir));
    char* slash = strrchr(dir, '\\');
    if (slash) *slash = 0;
    snprintf(logPath, sizeof(logPath), "%s\\d2android-loader.log", dir);
    snprintf(gamePath, sizeof(gamePath), "%s\\Game.exe", dir);
    snprintf(dllPath, sizeof(dllPath), "%s\\d2android-mod.dll", dir);
    snprintf(cmdLine, sizeof(cmdLine), "\"%s\" %s", gamePath, passedArguments());
    keepAsPrevious(logPath, "d2android-loader");
    char modLog[MAX_PATH];
    snprintf(modLog, sizeof(modLog), "%s\\d2android-mod.log", dir);
    keepAsPrevious(modLog, "d2android-mod");
    logLine("D2Android loader: %s", cmdLine);

    STARTUPINFOA si;
    PROCESS_INFORMATION pi;
    memset(&si, 0, sizeof(si));
    si.cb = sizeof(si);
    memset(&pi, 0, sizeof(pi));
    if (!CreateProcessA(gamePath, cmdLine, NULL, NULL, FALSE, CREATE_SUSPENDED, NULL, dir, &si, &pi)) {
        logLine("CreateProcess failed: %lu", GetLastError());
        return 1;
    }
    logLine("Game.exe started, pid %lu", pi.dwProcessId);

    char plugyPath[MAX_PATH], introPath[MAX_PATH];
    snprintf(plugyPath, sizeof(plugyPath), "%s\\PlugY.dll", dir);
    snprintf(introPath, sizeof(introPath), "%s\\d2android-skipintro", dir);
    int haveMod = GetFileAttributesA(dllPath) != INVALID_FILE_ATTRIBUTES;
    int wantEarly = GetFileAttributesA(plugyPath) != INVALID_FILE_ATTRIBUTES
        || GetFileAttributesA(introPath) != INVALID_FILE_ATTRIBUTES;
    int early = haveMod && wantEarly && injectAtEntry(pi.hProcess, gamePath, dllPath);
    ResumeThread(pi.hThread);

    if (!haveMod) {
        logLine("no mod DLL, game runs without it");
    }
    else if (early) {
        logLine("mod loaded at the entry point");
    }
    else {
        WindowSearch search = {pi.dwProcessId, 0};
        for (int i = 0; i < 300 && !search.found; i++) {
            if (WaitForSingleObject(pi.hProcess, 100) == WAIT_OBJECT_0) break;
            EnumWindows(findWindow, (LPARAM) &search);
        }
        logLine("game window %s", search.found ? "found" : "not found");
        if (WaitForSingleObject(pi.hProcess, 0) == WAIT_TIMEOUT) {
            char mode[16] = "";
            GetEnvironmentVariableA("D2ANDROID_INJECT", mode, sizeof(mode));
            int loaded = strcmp(mode, "thread") != 0 && inject(pi.hProcess, pi.dwProcessId, dllPath);
            if (!loaded) loaded = injectByThread(pi.hProcess, pi.hThread, pi.dwProcessId, dllPath);
            logLine(loaded ? "mod loaded" : "mod not loaded, game runs without it");
        }
    }

    WaitForSingleObject(pi.hProcess, INFINITE);
    DWORD code = 0;
    GetExitCodeProcess(pi.hProcess, &code);
    logLine("Game.exe ended with %lu", code);
    CloseHandle(pi.hThread);
    CloseHandle(pi.hProcess);
    return (int) code;
}
