





















#include "d2layerrules.c"

#define CMD_LAYER            33
#define CAP2_LAYERS          0x40
#define CAP2_LAYERS_L        0x80
#define RVA_LoadCell         0xA9480
#define RVA_LoadCellCaller   0x19238
#define RVA_CellDrawImport   0xCFCE
#define ORD_GfxDrawCell      10042
#define ORD_GfxDrawRect      10028
#define LYR_CELLS            192
#define LYR_CACHE_SLOTS      64
#define LYR_DRAWN_MS         500

typedef void (__stdcall *LyrDrawRect_t)(int, int, int, int, DWORD, DWORD);
typedef void (__stdcall *LyrDrawCell_t)(void*, int, int, DWORD, DWORD, void*);

typedef struct {
    BYTE pic, frame, used, kind;
    WORD sx, sy, sw, sh, dw, dh, outH;
    BYTE* file;
} LyrCell;

static LyrDrawRect_t fLyrRect;
static LyrDrawCell_t fLyrCell;
static int layersAvailable;
static int layersStageL;
static void layersDrawStage(int stage);
static LayerList lyrShown[LYR_LAYERS], lyrIncoming[LYR_LAYERS];
static DWORD lyrUntil[LYR_LAYERS], lyrDrawnAt[LYR_LAYERS];
static char lyrPaths[LYR_PICTURES][LYR_PATH], lyrLoadedPath[LYR_PICTURES][LYR_PATH];
static void* lyrFiles[LYR_PICTURES];
static LyrCell lyrCells[LYR_CELLS];
static int lyrCellNext;
static LARGE_INTEGER lyrFreq;
static double lyrTimeSum, lyrTimeMax;
static int lyrTimeFrames;
static DWORD lyrTimeSince;

static void checkLayerSites(void) {
    static const BYTE loader[] = {0x8D, 0x94, 0x24, 0xFC, 0xFE, 0xFF, 0xFF, 0x81, 0xEC, 0x08, 0x01, 0x00, 0x00};
    HMODULE gfx = GetModuleHandleA("D2gfx.dll");
    fLyrRect = gfx ? (LyrDrawRect_t) GetProcAddress(gfx, (LPCSTR) ORD_GfxDrawRect) : NULL;
    fLyrCell = gfx ? (LyrDrawCell_t) GetProcAddress(gfx, (LPCSTR) ORD_GfxDrawCell) : NULL;
    const BYTE* thunk = (const BYTE*) (client + RVA_CellDrawImport);
    int drawIsImport = fLyrCell && readable(thunk, 6) && thunk[0] == 0xFF && thunk[1] == 0x25
        && readable((void*) RD32(thunk, 2), 4) && RD32((void*) RD32(thunk, 2), 0) == (DWORD) fLyrCell;
    layersAvailable = compareAvailable && fLyrRect && drawIsImport && fDrawText && fSetTextSize && fGetTextWidth
        && bytesAre(client + RVA_LoadCell, loader, sizeof(loader)) && callAt(client, RVA_LoadCellCaller, RVA_LoadCell)
        && readable((void*) (client + RVA_ScreenSizeX), 8);
    QueryPerformanceFrequency(&lyrFreq);
    logLine(layersAvailable ? "layers sites verified: drawn before the tooltip"
                            : "layers sites not as expected: the app draws its layers itself");
}

static BYTE layersCapabilities2(void) {
    return (layersAvailable ? CAP2_LAYERS : 0) | (layersAvailable && layersStageL ? CAP2_LAYERS_L : 0);
}


static BYTE layersDrawnMask(void) {
    DWORD now = GetTickCount();
    BYTE m = 0;
    for (int i = 0; i < 8; i++) if (lyrDrawnAt[i] && now - lyrDrawnAt[i] < LYR_DRAWN_MS) m |= (BYTE) (1 << i);
    return m;
}

static int layersWanted(DWORD now) {
    if (!layersAvailable) return 0;
    for (int i = 0; i < LYR_LAYERS; i++) if ((int) (now - lyrUntil[i]) < 0) return 1;
    return 0;
}






static int layersCoverHover(DWORD now, const long* box) {
    if (!layersAvailable) return 0;
    const int book = 8;
    const LayerList* l = &lyrShown[book];
    if ((int) (now - lyrUntil[book]) >= 0 || l->count == 0 || l->stage != LYR_STAGE_T) return 0;
    for (int k = 0; k < l->count; k++) {
        const LayerOp* op = &l->ops[k];
        if (op->kind != LOP_RECT && op->kind != LOP_PIC && op->kind != LOP_LINE) continue;
        if (op->x < box[0] + box[2] && box[0] < op->x + op->w && op->y < box[1] + box[3] && box[1] < op->y + op->h)
            return 1;
    }
    return 0;
}


static void layerCommand(const BYTE* buf, int n) {
    if (!layersAvailable || n < LYR_HEAD) return;
    int layer = buf[8], stage = buf[9], part = buf[10], parts = buf[11];
    if (layer >= LYR_LAYERS) return;
    DWORD stand = lyrU32(buf + 12), now = GetTickCount();
    LayerList* in = &lyrIncoming[layer];
    if (parts == 0) {
        lyrUntil[layer] = now;
        lyrShown[layer].count = 0;
        in->have = 0;
        return;
    }
    if (part >= parts || parts > LYR_PARTS || (stage != LYR_STAGE_T && !(stage == LYR_STAGE_L && layersStageL))) return;
    if (in->stand != stand || in->parts != parts || !in->have) {
        in->stand = stand;
        in->parts = (BYTE) parts;
        in->have = 0;
        in->count = 0;
    }
    if (in->have & (1u << part)) return;
    if (part != __builtin_popcount(in->have)) {
        in->have = 0;
        return;
    }
    in->stage = (BYTE) stage;
    in->validMs = lyrU16(buf + 16);
    in->width = lyrU16(buf + 18);
    in->height = lyrU16(buf + 20);
    if (layerParse(buf, n, in, lyrPaths) < 0) {
        static DWORD loggedAt;
        if (now - loggedAt > 5000) logLine("layers: layer %d stand %lu part %d broken, dropped", layer, (unsigned long) stand, part);
        loggedAt = now;
        in->have = 0;
        return;
    }
    in->have |= 1u << part;
    if (__builtin_popcount(in->have) < parts) return;
    lyrShown[layer] = *in;
    in->have = 0;
    DWORD valid = lyrShown[layer].validMs;
    lyrUntil[layer] = now + (valid < 100 ? 100 : valid > 5000 ? 5000 : valid);
}







static void lyrRepointed(int no) {
    if (!lyrFiles[no] || strcmp(lyrLoadedPath[no], lyrPaths[no]) == 0) return;
    lyrFiles[no] = NULL;
    for (int i = 0; i < LYR_CELLS; i++) {
        LyrCell* c = &lyrCells[i];
        if (c->used && c->pic == no) {
            HeapFree(GetProcessHeap(), 0, c->file);
            c->used = 0;
        }
    }
}

static void* lyrLoad(int no) {
    lyrRepointed(no);
    if (lyrFiles[no]) return lyrFiles[no] == (void*) 1 ? NULL : lyrFiles[no];
    lstrcpynA(lyrLoadedPath[no], lyrPaths[no], LYR_PATH);
    if (!lyrPaths[no][0]) return NULL;
    DWORD result, path = (DWORD) lyrPaths[no];
    __asm__ volatile ("push $0\n\tcall *%2" : "=a"(result) : "a"(path), "r"(client + RVA_LoadCell)
                      : "ecx", "edx", "memory", "cc");
    lyrFiles[no] = result && readable((void*) result, 28) && RD32(result, 0) == 6 ? (void*) result : (void*) 1;
    logLine(lyrFiles[no] != (void*) 1 ? "layers: picture %d %s loaded" : "layers: picture %d %s not loaded", no, lyrPaths[no]);
    return lyrFiles[no] == (void*) 1 ? NULL : lyrFiles[no];
}


static const BYTE* lyrFrame(const BYTE* file, int index) {
    if (!readable(file, 24 + 4 * (index + 1)) || RD32(file, 0) != 6 || RD32(file, 16) < 1 || RD32(file, 20) <= (DWORD) index) return NULL;
    DWORD f = RD32(file, 24 + 4 * index);
    const BYTE* frame = f < 0x100000 ? file + f : (const BYTE*) f;
    if (!readable(frame, 32)) return NULL;
    DWORD w = RD32(frame, 4), h = RD32(frame, 8), length = RD32(frame, 28);
    if (w == 0 || h == 0 || w > LYR_MAX_PIC || h > LYR_MAX_PIC || length > 0x40000 || !readable(frame + 32, length)) return NULL;
    return frame;
}





static BYTE* lyrCellFor(const LayerOp* op, int* height) {
    lyrRepointed(op->a);
    for (int i = 0; i < LYR_CELLS; i++) {
        LyrCell* c = &lyrCells[i];
        if (c->used && c->kind == op->kind && c->pic == op->a && c->frame == op->b && c->sx == op->sx && c->sy == op->sy
            && c->sw == op->sw && c->sh == op->sh && c->dw == (WORD) op->w && c->dh == (WORD) op->h) {
            *height = c->outH;
            return c->file;
        }
    }
    const BYTE* file = (const BYTE*) lyrLoad(op->a);
    const BYTE* frame = file ? lyrFrame(file, op->b) : NULL;
    if (!frame) return NULL;
    int w = (int) RD32(frame, 4), h = (int) RD32(frame, 8);
    static short src[LYR_MAX_PIC * LYR_MAX_PIC], dst[LYR_MAX_PIC * LYR_MAX_PIC];
    if (!lyrDecodeRle(frame + 32, RD32(frame, 28), RD32(frame, 0) != 0, w, h, src)) return NULL;
    int outW = op->w, outH = op->h;
    if (op->kind == LOP_GLYPH) {
        outW = w;
        outH = h;
        for (int i = 0; i < w * h; i++) dst[i] = src[i] >= op->sx && src[i] <= op->sy ? (short) op->sw : -1;
    }
    else {
        int sw = op->sw ? op->sw : w, sh = op->sh ? op->sh : h;
        lyrScale(src, w, h, op->sx, op->sy, sw, sh, dst, outW, outH);
    }





    BYTE* slots = (BYTE*) HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY, LYR_CACHE_SLOTS);
    if (!slots) return NULL;
    int room = outH * (outW + outW / 127 + 3) + 8;

    BYTE* cell = (BYTE*) HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY, 28 + 32 + room + 3);
    if (!cell) {
        HeapFree(GetProcessHeap(), 0, slots);
        return NULL;
    }
    int length = lyrEncodeRle(dst, outW, outH, cell + 28 + 32, room);
    if (length < 0) {
        HeapFree(GetProcessHeap(), 0, cell);
        HeapFree(GetProcessHeap(), 0, slots);
        return NULL;
    }
    BYTE* f = cell + 28;
    *(DWORD*) (cell + 0) = 6;
    *(DWORD*) (cell + 4) = RD32(file, 4);
    *(DWORD*) (cell + 8) = RD32(file, 8);
    *(DWORD*) (cell + 12) = RD32(file, 12);
    *(DWORD*) (cell + 16) = 1;
    *(DWORD*) (cell + 20) = 1;
    *(DWORD*) (cell + 24) = (DWORD) f;
    *(DWORD*) (f + 0) = 0;
    *(DWORD*) (f + 4) = (DWORD) outW;
    *(DWORD*) (f + 8) = (DWORD) outH;


    DWORD next = RD32(frame, 24);
    *(DWORD*) (f + 20) = RD32(frame, 20);
    *(DWORD*) (f + 24) = (DWORD) slots;
    *(DWORD*) (f + 28) = (DWORD) length;
    LyrCell* c = &lyrCells[lyrCellNext];
    if (c->used) HeapFree(GetProcessHeap(), 0, c->file);
    lyrCellNext = (lyrCellNext + 1) % LYR_CELLS;
    c->used = 1;
    c->kind = op->kind;
    c->outH = (WORD) outH;
    c->pic = op->a;
    c->frame = op->b;
    c->sx = op->sx;
    c->sy = op->sy;
    c->sw = op->sw;
    c->sh = op->sh;
    c->dw = (WORD) op->w;
    c->dh = (WORD) op->h;
    c->file = cell;
    static int logged;
    if (logged++ < 3) logLine("layers: cell built for picture %d frame %d (%dx%d of %dx%d), source words %08lx %08lx, own cache slots %08lx",
        op->a, op->b, outW, outH, w, h, (unsigned long) RD32(frame, 20), (unsigned long) next, (unsigned long) (DWORD) slots);
    *height = outH;
    return cell;
}



static const DWORD LYR_MODES[4] = {0, 1, 2, 5};


static void lyrRect(int x1, int y1, int x2, int y2, DWORD colour, DWORD mode, int sw, int sh) {
    if (x1 < 0) x1 = 0;
    if (y1 < 0) y1 = 0;
    if (x2 > sw) x2 = sw;
    if (y2 > sh) y2 = sh;
    if (x1 < x2 && y1 < y2) fLyrRect(x1, y1, x2, y2, colour, mode);
}

static void lyrDrawOp(const LayerOp* op, int sw, int sh) {
    if (op->kind == LOP_RECT) lyrRect(op->x, op->y, op->x + op->w, op->y + op->h, op->a, LYR_MODES[op->b], sw, sh);
    else if (op->kind == LOP_LINE) {
        int x2 = op->x + op->w, y2 = op->y + op->h;
        lyrRect(op->x, op->y, x2, op->y + 1, op->a, 5, sw, sh);
        lyrRect(op->x, y2 - 1, x2, y2, op->a, 5, sw, sh);
        lyrRect(op->x, op->y + 1, op->x + 1, y2 - 1, op->a, 5, sw, sh);
        lyrRect(x2 - 1, op->y + 1, x2, y2 - 1, op->a, 5, sw, sh);
    }
    else if (op->kind == LOP_TEXT) {
        int old = fSetTextSize(op->a), x = op->x;
        if (op->c) {
            int w = fGetTextWidth((const wchar_t*) op->text);
            x -= op->c == 1 ? w / 2 : w;
        }
        fDrawText((const wchar_t*) op->text, x, op->y, op->b, 0);
        fSetTextSize(old);
    }
    else if (op->kind == LOP_PIC || op->kind == LOP_GLYPH) {
        if (op->x >= sw || op->y >= sh || (op->kind == LOP_PIC && (op->x + op->w <= 0 || op->y + op->h <= 0))) return;
        int height = 0;
        BYTE* cell = lyrCellFor(op, &height);
        if (!cell) return;
        DWORD context[18] = {0};
        context[0x34 / 4] = (DWORD) cell;

        fLyrCell(context, op->x, op->kind == LOP_GLYPH ? op->y : op->y + height, 0xFFFFFFFF, LYR_MODES[op->c], NULL);
    }
}






#define LYR_INPUT_BOX        9
static int layersHideTooltip(void) {
    if (!layersAvailable) return 0;
    LayerList* l = &lyrShown[LYR_INPUT_BOX];
    if ((int) (GetTickCount() - lyrUntil[LYR_INPUT_BOX]) >= 0 || l->count == 0) return 0;
    return l->width == (int) RD32(client, RVA_ScreenSizeX) && l->height == (int) RD32(client, RVA_ScreenSizeY);
}


static void layersDraw(void) {
    layersDrawStage(LYR_STAGE_T);
}


static void layersDrawStage(int stage) {
    if (!layersAvailable) return;
    DWORD now = GetTickCount();
    int sw = (int) RD32(client, RVA_ScreenSizeX), sh = (int) RD32(client, RVA_ScreenSizeY);
    LARGE_INTEGER t0, t1;
    int any = 0;
    QueryPerformanceCounter(&t0);
    for (int i = 0; i < LYR_LAYERS; i++) {
        LayerList* l = &lyrShown[i];
        if ((int) (now - lyrUntil[i]) >= 0 || l->count == 0 || l->stage != stage) continue;
        if (l->width != sw || l->height != sh) continue;
        for (int k = 0; k < l->count; k++) lyrDrawOp(&l->ops[k], sw, sh);
        lyrDrawnAt[i] = now ? now : 1;
        any = 1;
    }
    if (!any) return;
    QueryPerformanceCounter(&t1);
    double ms = lyrFreq.QuadPart ? (double) (t1.QuadPart - t0.QuadPart) * 1000.0 / (double) lyrFreq.QuadPart : 0;
    lyrTimeSum += ms;
    if (ms > lyrTimeMax) lyrTimeMax = ms;
    lyrTimeFrames++;
    if (now - lyrTimeSince >= 10000) {
        logLine("layers: %d frames, %.3f ms per frame on average, at most %.3f ms", lyrTimeFrames,
            lyrTimeSum / lyrTimeFrames, lyrTimeMax);
        lyrTimeSince = now;
        lyrTimeSum = lyrTimeMax = 0;
        lyrTimeFrames = 0;
    }
}
