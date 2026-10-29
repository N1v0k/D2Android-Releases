










#define ORD_InitPlayerData   10404
#define SG_SIZE_AT           0x48
#define SG_CALL_AT           0x4D
#define SG_PLAYERDATA_SIZE   0x16C
#define SG_GOLD              4
#define SG_UNKNOWN           0xFFFFFFFFu
#define SG_UNIT_DATA         0x14

static int sharedGoldState;

static int inModule(HMODULE module, DWORD addr);





static int sharedGoldActive(HMODULE plugy) {
    char path[MAX_PATH];
    DWORD n = GetModuleFileNameA(plugy, path, sizeof(path));
    if (n == 0 || n >= sizeof(path) - 16) return 0;
    char* slash = strrchr(path, '\\');
    strcpy(slash ? slash + 1 : path, "PlugY.ini");
    return GetPrivateProfileIntA("STASH", "ActiveMultiPageStash", 0, path) != 0
        && GetPrivateProfileIntA("STASH", "ActiveSharedStash", 0, path) != 0
        && GetPrivateProfileIntA("STASH", "ActiveSharedGold", 1, path) != 0;
}


static int sgPlayerDataHooked(HMODULE plugy) {
    const BYTE* f = (const BYTE*) GetProcAddress((HMODULE) common, (LPCSTR) ORD_InitPlayerData);
    return plugy && f && readable(f, SG_CALL_AT + 5) && f[SG_SIZE_AT] == 0xBA
        && *(const DWORD*) (f + SG_SIZE_AT + 1) == SG_PLAYERDATA_SIZE && f[SG_CALL_AT] == 0xE8
        && inModule(plugy, (DWORD) f + SG_CALL_AT + 5 + *(const DWORD*) (f + SG_CALL_AT + 1));
}


static int sharedGoldVerified(void) {
    if (sharedGoldState) return sharedGoldState > 0;
    if (!plugyLoaded || !common) return 0;
    HMODULE plugy = GetModuleHandleA("PlugY.dll");
    int active = plugy && sharedGoldActive(plugy);
    int ok = active && sgPlayerDataHooked(plugy);
    sharedGoldState = ok ? 1 : -1;
    logLine(ok ? "shared gold: PlugY's player data verified" : active ? "shared gold: PlugY's player data not as expected"
        : "shared gold: off (no PlugY or off in its PlugY.ini)");
    return ok;
}


static DWORD sharedGoldNow(void* hero) {
    if (!hero || !sharedGoldVerified() || !readable((const BYTE*) hero + SG_UNIT_DATA, 4)) return SG_UNKNOWN;
    const BYTE* data = *(const BYTE* const*) ((const BYTE*) hero + SG_UNIT_DATA);
    const BYTE* gold = data ? data + SG_PLAYERDATA_SIZE + SG_GOLD : NULL;
    return gold && readable(gold, 4) ? *(const DWORD*) gold : SG_UNKNOWN;
}
