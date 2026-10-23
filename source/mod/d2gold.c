














#define RVA_GoldDialogAction    0x11C86C
#define RVA_GoldDialogControls  0x11C874
#define RVA_GoldDialogAmount    0x11D568
#define GOLD_ACTION_WITHDRAW    4
#define D2GFX_ORD_GET_HWND      10007

static int goldSitesOk;
static DWORD goldDialogSeen;

static void checkGoldSites(void) {
    static const BYTE movAmount[] = {0xA3};
    static const BYTE cmpAction[] = {0x83, 0x3D};
    static const BYTE movControls[] = {0x8B, 0x0D};
    goldSitesOk = bytesAre(client + 0x181BE, movControls, 2) && addressAt(client, 0x181C0, RVA_GoldDialogControls)
        && bytesAre(client + 0x181DE, movAmount, 1) && addressAt(client, 0x181DF, RVA_GoldDialogAmount)
        && bytesAre(client + 0x181E3, cmpAction, 2) && addressAt(client, 0x181E5, RVA_GoldDialogAction)
        && readable((void*) (client + 0x181E9), 1) && *(BYTE*) (client + 0x181E9) == GOLD_ACTION_WITHDRAW;
    logLine(goldSitesOk ? "gold dialog sites verified" : "gold dialog sites not as expected: withdraw stays empty");
}


static HWND goldWindow(void) {
    HMODULE gfx = GetModuleHandleA("D2Gfx.dll");
    const BYTE* f = gfx ? (const BYTE*) GetProcAddress(gfx, (LPCSTR) (DWORD_PTR) D2GFX_ORD_GET_HWND) : NULL;
    if (!f || !readable(f, 6) || f[0] != 0xA1 || f[5] != 0xC3) return NULL;
    const HWND* var = *(HWND* const*) (f + 1);
    if ((DWORD) var < (DWORD) gfx || (DWORD) var >= (DWORD) gfx + 0x100000 || !readable(var, 4)) return NULL;
    return *var;
}


static void goldDialogTick(void* player) {
    if (!goldSitesOk) return;
    DWORD controls = RD32(client, RVA_GoldDialogControls);
    if (!controls) {
        goldDialogSeen = 0;
        return;
    }
    if (controls == goldDialogSeen) return;
    goldDialogSeen = controls;
    if (!player || !singlePlayerGame() || !getUiVar(0x19)) return;
    if (RD32(client, RVA_GoldDialogAction) != GOLD_ACTION_WITHDRAW || RD32(client, RVA_GoldDialogAmount) != 0) return;
    DWORD level = fGetUnitStat(player, 12, 0), gold = fGetUnitStat(player, 14, 0), stash = fGetUnitStat(player, 15, 0);
    DWORD cap = 10000 * level, room = cap > gold ? cap - gold : 0, amount = stash < room ? stash : room;
    if (!amount) return;
    HWND window = goldWindow();
    if (!window) {
        logLine("gold: withdraw dialog, no game window: left empty");
        return;
    }
    char digits[12];
    wsprintfA(digits, "%lu", (unsigned long) amount);
    for (const char* c = digits; *c; c++) PostMessageA(window, WM_CHAR, (WPARAM) (BYTE) *c, 1);
    logLine("gold: withdraw dialog filled with %lu (stash %lu, purse %lu of %lu)", (unsigned long) amount,
            (unsigned long) stash, (unsigned long) gold, (unsigned long) cap);
}
