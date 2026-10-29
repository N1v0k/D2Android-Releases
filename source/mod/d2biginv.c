




















#include "d2biginvcore.c"

#define BI_MARKER          "d2android-biginv"
#define CAP3_BIGINV        0x01
#define RVA_InvTable       0xA4CAC
#define RVA_InvCount       0xA4CA8
#define RVA_InvTableStore  0x6CC29
#define RVA_PanelLoadCall  0x6DFF3
#define RVA_PanelPath6     0xD5AB0
#define RVA_PanelPath      0xD5AA0
#define RVA_PanelCell      0x103B60
#define RVA_UiPicture      0x191F0
#define ORD_LangGetLanguage 10001

static int biWanted;
static int biPanelSite;
static volatile LONG biBig;
static int biPanelFailed;
static BYTE* biPending;
static DWORD biCommon, biClient;
DWORD biPanelOriginal;

static void biReadFile(void) {
    char path[MAX_PATH];
    lstrcpynA(path, logPath, sizeof(path));
    char* slash = strrchr(path, '\\');
    if (!slash || (size_t) (slash + 1 - path) + sizeof(BI_MARKER) > sizeof(path)) return;
    lstrcpyA(slash + 1, BI_MARKER);
    HANDLE h = CreateFileA(path, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, 0, NULL);
    if (h == INVALID_HANDLE_VALUE) return;
    char buf[256];
    DWORD n = 0;
    if (!ReadFile(h, buf, sizeof(buf) - 1, &n, NULL)) n = 0;
    CloseHandle(h);
    biWanted = biParse(buf, (int) n);
    logLine(biWanted ? "big inventory: wanted (%s)" : "big inventory: %s not version %d with inventory=1, 10 x 4", BI_MARKER,
        BI_VERSION);
}



static const int biPieces[4][2] = {{0, 0}, {256, 0}, {0, 256}, {256, 256}};


static BYTE* biPiece(BYTE* cell, int k) {
    BYTE* f = (BYTE*) lyrFrame(cell, 4 + k);
    if (!f || RD32(f, 0) != 0 || RD32(f, 4) != (k % 2 ? 64u : 256u) || RD32(f, 8) != (k < 2 ? 256u : 176u)) return NULL;
    return f;
}





static int biPanelCompose(BYTE* cell) {
    if (!readable(cell, 24) || RD32(cell, 0) != 6 || RD32(cell, 16) != 1 || RD32(cell, 20) != 8) return 0;
    static short in[BI_PANEL_W * BI_PANEL_H], out[BI_PANEL_W * BI_PANEL_H], px[LYR_MAX_PIC * LYR_MAX_PIC];
    static BYTE data[4][256 * (256 + 256 / 127 + 3)];
    for (int i = 0; i < BI_PANEL_W * BI_PANEL_H; i++) in[i] = -1;
    for (int k = 0; k < 4; k++) {
        BYTE* f = biPiece(cell, k);
        if (!f) return 0;
        int w = (int) RD32(f, 4), h = (int) RD32(f, 8);
        if (!lyrDecodeRle(f + 32, RD32(f, 28), 0, w, h, px)) return 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int X = biPieces[k][0] + x, Y = biPieces[k][1] + y;
                if (X < BI_PANEL_W && Y < BI_PANEL_H) in[Y * BI_PANEL_W + X] = px[y * w + x];
            }
    }
    if (!biCompose(in, out)) return 0;
    int length[4];
    for (int k = 0; k < 4; k++) {
        BYTE* f = biPiece(cell, k);
        int w = (int) RD32(f, 4), h = (int) RD32(f, 8);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int X = biPieces[k][0] + x, Y = biPieces[k][1] + y;
                px[y * w + x] = X < BI_PANEL_W && Y < BI_PANEL_H ? out[Y * BI_PANEL_W + X] : -1;
            }
        length[k] = lyrEncodeRle(px, w, h, data[k], (int) sizeof(data[k]));
        if (length[k] < 0 || (DWORD) length[k] != RD32(f, 28)) {
            logLine("big inventory: frame %d of the new panel %d bytes, the original's %lu", 4 + k, length[k],
                (unsigned long) RD32(f, 28));
            return 0;
        }
    }
    for (int k = 0; k < 4; k++) memcpy(biPiece(cell, k) + 32, data[k], (size_t) length[k]);
    return 1;
}


static int biPanelReady(void) {
    if (biPending) return 1;
    if (biPanelFailed || !biClient) return 0;
    DWORD file;
    __asm__ volatile ("call *%1" : "=a"(file) : "r"(biClient + RVA_UiPicture), "S"(biClient + RVA_PanelPath6)
                      : "ecx", "edx", "memory", "cc");
    if (file && biPanelCompose((BYTE*) file)) {
        biPending = (BYTE*) file;
        return 1;
    }
    biPanelFailed = 1;
    logLine("big inventory: the original panel not as expected (%08lx), the grid stays 10 x 4", (unsigned long) file);
    return 0;
}


void* __cdecl biPanelLoad(const char* path) {
    int ours = (DWORD) path == biClient + RVA_PanelPath6 || (DWORD) path == biClient + RVA_PanelPath;
    if (!ours || !biBig || !biPending) {
        if (ours && biBig) logLine("big inventory: the composed panel missing, the game's own under the big grid");
        return NULL;
    }
    BYTE* cell = biPending;
    biPending = NULL;
    static int logged;
    if (logged++ < 3) logLine("big inventory: the game got the composed panel (%s)", path);
    return cell;
}

void biPanelStub(void);
__asm__(
    ".text\n"
    ".globl _biPanelStub\n"
    "_biPanelStub:\n"
    "    pushl %esi\n"
    "    call _biPanelLoad\n"
    "    addl $4, %esp\n"
    "    testl %eax, %eax\n"
    "    jz 1f\n"
    "    ret\n"
    "1:  jmp *_biPanelOriginal\n"
);


static int biPatchCall(DWORD at, void* stub) {
    DWORD old;
    if (!VirtualProtect((void*) at, 5, PAGE_EXECUTE_READWRITE, &old)) return 0;
    *(BYTE*) at = 0xE8;
    *(DWORD*) (at + 1) = (DWORD) stub - (at + 5);
    VirtualProtect((void*) at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), (void*) at, 5);
    return 1;
}


static void biInstallPanel(DWORD clientBase) {
    if (!biWanted || biPanelSite || !clientBase) return;
    static const BYTE jne[2] = {0x75, 0x05};
    BYTE* at = (BYTE*) clientBase;

    int ok = readable(at + 0x6DFE7, 0x1B) && at[0x6DFE7] == 0xBE && RD32(at, 0x6DFE8) == clientBase + RVA_PanelPath6
        && memcmp(at + 0x6DFEC, jne, 2) == 0 && at[0x6DFEE] == 0xBE && RD32(at, 0x6DFEF) == clientBase + RVA_PanelPath
        && callAt(clientBase, RVA_PanelLoadCall, RVA_UiPicture) && at[0x6DFF8] == 0xBE && at[0x6DFFD] == 0xA3
        && RD32(at, 0x6DFFE) == clientBase + RVA_PanelCell
        && bytesAre(clientBase + RVA_PanelPath6, (const BYTE*) "Panel\\InvChar6", 15);
    if (ok) {
        biClient = clientBase;
        biPanelOriginal = clientBase + RVA_UiPicture;
        ok = biPatchCall(clientBase + RVA_PanelLoadCall, biPanelStub);
    }
    biPanelSite = ok;
    logLine(ok ? "big inventory: panel site verified" : "big inventory: panel site NOT as expected, the grid stays 10 x 4");
}



static LONG biLanguage(void) {
    HMODULE lang = GetModuleHandleA("D2Lang.dll");
    FARPROC get = lang ? GetProcAddress(lang, (LPCSTR) ORD_LangGetLanguage) : NULL;
    return get ? (LONG) ((DWORD (__stdcall*)(void)) get)() : -1;
}


void __cdecl biTableLoaded(BYTE* table) {
    *(BYTE**) (biCommon + RVA_InvTable) = table;
    int count = (int) RD32(biCommon, RVA_InvCount);
    LONG language = biLanguage();
    int result = -2;
    if (biPanelSite && language >= 0 && (language < 6 || language > 9) && biPanelReady())
        result = biTableApply(table, count);
    InterlockedExchange(&biBig, result >= 0);
    invGridH = result >= 0 ? BI_ROWS : INV_H;
    static int logged;
    if (logged++ < 20 || result < 0)
        logLine("big inventory: table loaded (language %ld): %s", language,
            result > 0 ? "10 x 8 written" : result == 0 ? "already 10 x 8" : result == -1 ? "NOT as in 1.13d, left 10 x 4" : "left 10 x 4");
}

void biTableStub(void);
__asm__(
    ".text\n"
    ".globl _biTableStub\n"
    "_biTableStub:\n"
    "    pushal\n"
    "    pushl %eax\n"
    "    call _biTableLoaded\n"
    "    addl $4, %esp\n"
    "    popal\n"
    "    ret\n"
);


static void biInstall(void) {
    biReadFile();
    if (!biWanted) return;
    biCommon = (DWORD) GetModuleHandleA("D2Common.dll");
    BYTE* at = (BYTE*) (biCommon + RVA_InvTableStore);

    int ok = biCommon && readable(at, 12) && at[0] == 0xA3 && RD32(at, 1) == biCommon + RVA_InvTable && at[5] == 0x83
        && at[6] == 0x3D && RD32(at, 7) == biCommon + RVA_InvCount && at[11] == BI_RECORDS
        && readable((void*) (biCommon + RVA_InvCount), 8);
    if (ok && RD32(biCommon, RVA_InvTable)) {
        ok = 0;
        logLine("big inventory: the table is loaded already");
    }
    if (ok) ok = biPatchCall(biCommon + RVA_InvTableStore, biTableStub);
    logLine(ok ? "big inventory: table site verified" : "big inventory: table site NOT as expected, 10 x 4");
    if (!ok) biWanted = 0;
    biInstallPanel((DWORD) GetModuleHandleA("D2Client.dll"));
}




static BYTE biExtraItems(void* hero) {
    void* inventory = hero ? RDP(hero, U_INVENTORY) : NULL;
    if (!readable(inventory, 0x10) || !fGetItemText) return 0xFF;
    int n = 0, guard = 0;
    for (void* item = RDP(inventory, INV_FIRST); item && guard < 512; guard++) {
        void* data = RDP(item, U_DATA);
        if (!readable(data, 0x70)) return 0xFF;
        if (RD8(data, IDATA_LOCATION) == 0x00 && RD8(data, IDATA_NODEPAGE) == 1) {
            void* path = RDP(item, U_PATH);
            BYTE* txt = (BYTE*) fGetItemText(RD32(item, U_TXTNO));
            if (!readable(path, 0x14) || !readable(txt, 0x120)) return 0xFF;
            int x = (int) RD32(path, IPATH_X), y = (int) RD32(path, IPATH_Y);
            if (x + txt[ITXT_WIDTH] > INV_W || y + txt[ITXT_HEIGHT] > INV_H) n++;
        }
        item = RDP(data, IDATA_NEXTINV);
    }
    return (BYTE) (n > 254 ? 254 : n);
}

static int stashPagesKnown(void);


static void putBigInvFields(Packet* p, void* hero) {
    putU8(p, (biBig ? CAP3_BIGINV : 0) | (stashPagesKnown() ? 0x02 : 0));
    const BYTE* table = common && readable((void*) (common + RVA_InvTable), 8) ? RDP(common, RVA_InvTable) : NULL;
    int count = table ? (int) RD32(common, RVA_InvCount) : 0;
    if (!readable(table, BI_RECORDS * BI_RECORD_SIZE)) table = NULL;
    BYTE w, h;
    biGridOf(table, count, hero ? biHeroRow((int) RD32(hero, U_TXTNO)) : -1, &w, &h);
    putU8(p, w);
    putU8(p, h);
    biGridOf(table, count, BI_ROW_CUBE, &w, &h);
    putU8(p, w);
    putU8(p, h);
    biGridOf(table, count, BI_ROW_STASH, &w, &h);
    putU8(p, w);
    putU8(p, h);
    putU8(p, biExtraItems(hero));
}
