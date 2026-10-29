






























#define RVA_XpBarCall       0x1D4A1
#define RVA_XpBarCallee     0x18E20
#define RVA_XpPanelCall     0x1D596
#define RVA_XpPanelCallee   0x6F300
#define XPB_MIDDLE_HALF 64
#define RVA_CtrlPanelCells  0x103B70
#define RVA_CtrlPanelLoad   0x6D512
#define ORD_GfxDrawRect     10028
#define XPB_HALF 283
#define XPB_HALF_COMPACT 235
#define XPB_TOP 53
#define XPB_ROWS 6
#define XPB_SECTIONS 10
#define XPB_WHITE 0xFF
#define XPB_TEXT_COLOR 7
#define XPB_FONT 6
#define XPB_DRAW_OPAQUE 5
#define XPB_DRAW_DIM 1
#define XPB_LEVEL_STRING 4057

#include "d2xpbarrules.c"

typedef void (__stdcall *GfxDrawRect_t)(int, int, int, int, DWORD, DWORD);
static GfxDrawRect_t fGfxDrawRect;
static int xpBarSites;
static int xpBarPatched;
static DWORD xpBarUntil, xpLevelUntil, xpCompactUntil;
static int xpPieceState;
static short xpPiece[XPB_ROWS][XPB_PIECE_W];

static void __cdecl xpBarDrawn(void);
static void __cdecl xpPanelDrawn(void);


static int xpRedirect(DWORD rva, DWORD target) {
    BYTE* at = (BYTE*) (client + rva);
    DWORD old;
    if (!VirtualProtect(at, 5, PAGE_EXECUTE_READWRITE, &old)) return 0;
    *(DWORD*) (at + 1) = target - ((DWORD) at + 5);
    VirtualProtect(at, 5, old, &old);
    FlushInstructionCache(GetCurrentProcess(), at, 5);
    return 1;
}

static void checkXpBarSites(void) {
    static const BYTE callee[] = {0xA1};
    static const BYTE calleeRest[] = {0x81, 0xEC, 0x28, 0x01, 0x00, 0x00};
    static const BYTE load[] = {0xA1};
    HMODULE gfx = GetModuleHandleA("D2gfx.dll");
    fGfxDrawRect = gfx ? (GfxDrawRect_t) GetProcAddress(gfx, (LPCSTR) ORD_GfxDrawRect) : NULL;
    xpBarSites = callAt(client, RVA_XpBarCall, RVA_XpBarCallee) && callAt(client, RVA_XpPanelCall, RVA_XpPanelCallee) && fGetUiVar
        && bytesAre(client + RVA_XpBarCallee, callee, 1) && bytesAre(client + RVA_XpBarCallee + 5, calleeRest, sizeof(calleeRest))
        && bytesAre(client + RVA_CtrlPanelLoad, load, 1) && addressAt(client, RVA_CtrlPanelLoad + 1, RVA_CtrlPanelCells)
        && fGfxDrawRect && fDrawText && fSetTextSize && fGetTextWidth && readable((void*) (client + RVA_ScreenSizeX), 8);
    if (!xpBarSites) {
        logLine("xp bar sites not as expected: no experience bar in the game");
        return;
    }
    if (!xpRedirect(RVA_XpBarCall, (DWORD) xpBarDrawn) || !xpRedirect(RVA_XpPanelCall, (DWORD) xpPanelDrawn)) {
        xpBarSites = 0;
        logLine("xp bar: calls not redirected, no experience bar in the game");
        return;
    }
    xpBarPatched = 1;
    layersStageL = 1;
    logLine("xp bar sites verified, drawn after the automap and its middle after the bottom bar");
}


static void xpBarCommand(const BYTE* buf, int n, DWORD now) {
    DWORD off = now - 60000;
    int bits = n >= 11 ? buf[10] : 0;
    xpBarUntil = (bits & 1) ? now + 2000 : off;
    xpLevelUntil = (bits & 2) ? now + 2000 : off;
    xpCompactUntil = (bits & 4) ? now + 2000 : off;
}

static BYTE xpBarCapabilities2(void) {
    return xpBarSites ? 32 : 0;
}







static int xpDecodeFrame(const BYTE* file, DWORD index, short* out, int w, int h) {
    if (!readable(file, 24 + 4 * (index + 1)) || RD32(file, 0) != 6 || RD32(file, 16) != 1 || RD32(file, 20) <= index) return 0;
    DWORD f = RD32(file, 24 + 4 * index);
    const BYTE* frame = f < 0x100000 ? file + f : (const BYTE*) f;
    if (!readable(frame, 32)) return 0;
    DWORD flip = RD32(frame, 0), fw = RD32(frame, 4), fh = RD32(frame, 8), length = RD32(frame, 28);
    if ((int) fw != w || (int) fh != h || length > 0x40000 || !readable(frame + 32, length)) return 0;
    return xpbDecodeRle(frame + 32, length, flip != 0, w, h, out) > 0;
}


static int xpFrameSize(const BYTE* file, DWORD index, int* w, int* h) {
    if (!readable(file, 24 + 4 * (index + 1)) || RD32(file, 0) != 6 || RD32(file, 20) <= index) return 0;
    DWORD f = RD32(file, 24 + 4 * index);
    const BYTE* frame = f < 0x100000 ? file + f : (const BYTE*) f;
    if (!readable(frame, 32)) return 0;
    *w = (int) RD32(frame, 4);
    *h = (int) RD32(frame, 8);
    return *w > 0 && *w <= 256 && *h > 0 && *h <= 256;
}


static void xpLoadPiece(void) {
    const BYTE* file = (const BYTE*) RD32(client, RVA_CtrlPanelCells);
    static short buf[256 * 256];
    int w1, h1, w2, h2;
    xpPieceState = -1;
    if (!file || !xpFrameSize(file, 1, &w1, &h1) || !xpFrameSize(file, 2, &w2, &h2) || w1 < 125 || w2 < 84 || h1 < 20 || h2 < 20) {
        logLine("xp bar: the bottom bar's picture not as expected, a black box instead");
        return;
    }
    if (!xpDecodeFrame(file, 1, buf, w1, h1)) return;
    for (int r = 0; r < XPB_ROWS; r++) for (int c = 0; c < 36; c++) xpPiece[r][c] = buf[(14 + r) * w1 + 89 + c];
    if (!xpDecodeFrame(file, 2, buf, w2, h2)) return;
    for (int r = 0; r < XPB_ROWS; r++) for (int c = 0; c < 4; c++) xpPiece[r][36 + c] = buf[(14 + r) * w2 + 80 + c];
    xpPieceState = 1;
    logLine("xp bar: frame piece read from the bottom bar's picture");
}


static void xpRect(int x1, int y1, int x2, int y2, DWORD colour, DWORD mode, int clipL, int clipR) {
    if (x1 < clipL) x1 = clipL;
    if (x2 > clipR) x2 = clipR;
    if (x1 < x2) fGfxDrawRect(x1, y1, x2, y2, colour, mode);
}

static void xpBarFrame(int left, int top, int w, int clipL, int clipR) {
    if (xpPieceState == 0) xpLoadPiece();
    if (xpPieceState != 1) {
        xpRect(left, top, left + w, top + XPB_ROWS, 0, XPB_DRAW_OPAQUE, clipL, clipR);
        return;
    }
    for (int r = 0; r < XPB_ROWS; r++) {
        int start = 0, colour = xpPiece[r][xpbPieceColumn(0, w)];
        for (int c = 1; c <= w; c++) {
            int next = c < w ? xpPiece[r][xpbPieceColumn(c, w)] : -2;
            if (next == colour) continue;
            if (colour >= 0) xpRect(left + start, top + r, left + c, top + r + 1, (DWORD) colour, XPB_DRAW_OPAQUE, clipL, clipR);
            start = c;
            colour = next;
        }
    }
}






static void drawXpBar(int middleOnly) {
    void* hero = fGetPlayerUnit();
    if (!hero) return;
    DWORD xp = fGetUnitStat(hero, 13, 0), level = fGetUnitStat(hero, 12, 0);
    if (level < 1 || level > 99) return;
    DWORD from = XP_LEVEL[level - 1], to = level < 99 ? XP_LEVEL[level] : 0;
    if (xp < from || (level < 99 && xp >= to)) return;
    int width = (int) RD32(client, RVA_ScreenSizeX), height = (int) RD32(client, RVA_ScreenSizeY);
    DWORD now = GetTickCount();
    int half = (int) (now - xpCompactUntil) < 0 ? XPB_HALF_COMPACT : XPB_HALF;
    int middle = width / 2, left = middle - half, top = height - XPB_TOP, w = 2 * half;
    if (width < 2 * half || height < 2 * XPB_TOP) return;
    int clipL = middleOnly ? middle - XPB_MIDDLE_HALF : left, clipR = middleOnly ? middle + XPB_MIDDLE_HALF : left + w;
    xpBarFrame(left, top, w, clipL, clipR);

    int inLeft = left + 1, inRight = left + w - 1;
    int filled = xpbFilled(inLeft, inRight, xp, from, to, level == 99);
    if (filled > inLeft) xpRect(inLeft, top + 2, filled, top + 4, XPB_WHITE, XPB_DRAW_OPAQUE, clipL, clipR);
    for (int i = 1; i < XPB_SECTIONS; i++) {
        int x = inLeft + (inRight - inLeft) * i / XPB_SECTIONS;
        xpRect(x, top + 1, x + 2, top + 5, x <= filled ? 0 : XPB_WHITE, XPB_DRAW_DIM, clipL, clipR);
    }
    if (middleOnly) return;
    int percent = xpbPercent(xp, from, to, level == 99);
    wchar_t text[48];
    int oldFont = fSetTextSize(XPB_FONT);
    wsprintfW(text, L"%d%%", percent);
    fDrawText(text, left + w - fGetTextWidth(text), top - 2, XPB_TEXT_COLOR, 0);
    if ((int) (now - xpLevelUntil) < 0) {
        const wchar_t* name = localeText(XPB_LEVEL_STRING);
        if (name && readable(name, 2) && lstrlenW(name) < 24) wsprintfW(text, L"%s %lu", name, (unsigned long) level);
        else wsprintfW(text, L"%lu", (unsigned long) level);
        fDrawText(text, left, top - 2, XPB_TEXT_COLOR, 0);
    }
    fSetTextSize(oldFont);
}


__attribute__((force_align_arg_pointer)) static void __cdecl xpBarDrawn(void) {
    ((void (__cdecl *)(void)) (client + RVA_XpBarCallee))();
    if ((int) (GetTickCount() - xpBarUntil) < 0) drawXpBar(0);
}



static int xpWindowOpen(void) {
    static const BYTE windows[] = {0x01, 0x02, 0x04, 0x08, 0x0C, 0x0E, 0x0F, 0x10, 0x14, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1F, 0x24, 0x25};
    for (unsigned i = 0; i < sizeof(windows); i++) if (getUiVar(windows[i])) return 1;
    return 0;
}



__attribute__((force_align_arg_pointer)) static void __cdecl xpPanelDrawn(void) {
    ((void (__cdecl *)(void)) (client + RVA_XpPanelCallee))();
    if ((int) (GetTickCount() - xpBarUntil) < 0 && !xpWindowOpen()) drawXpBar(1);
    layersDrawStage(LYR_STAGE_L);
}
