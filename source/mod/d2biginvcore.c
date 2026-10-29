

















#define BI_VERSION      1
#define BI_RECORDS      32
#define BI_RECORD_SIZE  240
#define BI_SLOTS        10
#define BI_PANEL_W      320
#define BI_PANEL_H      432
#define BI_CELL         29
#define BI_COLS         10
#define BI_ROWS         8
#define BI_ROWS_ORIG    4


#define BIR_INV       0
#define BIR_GRID_X    16
#define BIR_GRID      20
#define BIR_BOX       36
#define BIR_SLOT      40


static const int biHeroRows[14] = {0, 1, 2, 3, 4, 14, 15, 16, 17, 18, 19, 20, 30, 31};
#define BI_ROW_CUBE   25
#define BI_ROW_STASH  28
#define BI_HIRES_FIRST 16

typedef struct { short left, top, w, h; } BiBox;


static const BiBox biOrig[BI_SLOTS] = {
    {20, 47, 55, 112}, {133, 77, 56, 82}, {251, 47, 55, 112}, {135, 8, 54, 51}, {209, 35, 23, 24},
    {95, 180, 23, 24}, {209, 180, 23, 24}, {136, 179, 52, 25}, {252, 182, 54, 52}, {21, 181, 54, 53},
};

static const BiBox biNew[BI_SLOTS] = {
    {8, 25, 55, 112}, {132, 8, 54, 82}, {255, 25, 55, 112}, {72, 8, 52, 51}, {208, 8, 23, 24},
    {195, 43, 23, 24}, {223, 43, 23, 24}, {133, 101, 52, 25}, {194, 78, 52, 52}, {72, 72, 52, 53},
};
#define BI_GRID_LEFT      19
#define BI_GRID_TOP_ORIG  255
#define BI_GRID_TOP       145

static int biRd32(const unsigned char* p, int off) {
    int v;
    memcpy(&v, p + off, 4);
    return v;
}

static void biWr32(unsigned char* p, int off, int v) {
    memcpy(p + off, &v, 4);
}




static int biParse(const char* text, int len) {
    int version = -1, on = 0, at = 0;
    while (at < len) {
        int end = at;
        while (end < len && text[end] != '\n') end++;
        int e = end;
        if (e > at && text[e - 1] == '\r') e--;
        if (e - at == 9 && !memcmp(text + at, "version=1", 9)) version = 1;
        else if (e - at > 8 && !memcmp(text + at, "version=", 8)) version = 0;
        else if (e - at == 11 && !memcmp(text + at, "inventory=", 10)) on = text[at + 10] == '1';
        at = end + 1;
    }
    return version == BI_VERSION && on;
}




static int biRecordIs(const unsigned char* r, int ox, int oy, int big) {
    if (biRd32(r, BIR_INV) != ox || biRd32(r, BIR_INV + 4) != ox + BI_PANEL_W || biRd32(r, BIR_INV + 8) != oy
        || biRd32(r, BIR_INV + 12) != oy + 441) return 0;
    int rows = big ? BI_ROWS : BI_ROWS_ORIG, top = oy + (big ? BI_GRID_TOP : BI_GRID_TOP_ORIG), left = ox + BI_GRID_LEFT;
    if (r[BIR_GRID_X] != BI_COLS || r[BIR_GRID_X + 1] != rows || r[BIR_BOX] != BI_CELL || r[BIR_BOX + 1] != BI_CELL) return 0;
    if (biRd32(r, BIR_GRID) != left || biRd32(r, BIR_GRID + 4) != left + BI_COLS * BI_CELL - 3 || biRd32(r, BIR_GRID + 8) != top
        || biRd32(r, BIR_GRID + 12) != top + rows * BI_CELL - 3) return 0;
    for (int s = 0; s < BI_SLOTS; s++) {
        const BiBox* b = big ? &biNew[s] : &biOrig[s];
        const unsigned char* f = r + BIR_SLOT + 20 * s;
        if (biRd32(f, 0) != ox + b->left || biRd32(f, 4) != ox + b->left + b->w || biRd32(f, 8) != oy + b->top
            || biRd32(f, 12) != oy + b->top + b->h || f[16] != b->w || f[17] != b->h) return 0;
    }
    return 1;
}

static void biRecordWrite(unsigned char* r, int ox, int oy) {
    int top = oy + BI_GRID_TOP, left = ox + BI_GRID_LEFT;
    r[BIR_GRID_X] = BI_COLS;
    r[BIR_GRID_X + 1] = BI_ROWS;
    biWr32(r, BIR_GRID, left);
    biWr32(r, BIR_GRID + 4, left + BI_COLS * BI_CELL - 3);
    biWr32(r, BIR_GRID + 8, top);
    biWr32(r, BIR_GRID + 12, top + BI_ROWS * BI_CELL - 3);
    for (int s = 0; s < BI_SLOTS; s++) {
        const BiBox* b = &biNew[s];
        unsigned char* f = r + BIR_SLOT + 20 * s;
        biWr32(f, 0, ox + b->left);
        biWr32(f, 4, ox + b->left + b->w);
        biWr32(f, 8, oy + b->top);
        biWr32(f, 12, oy + b->top + b->h);
        f[16] = (unsigned char) b->w;
        f[17] = (unsigned char) b->h;
    }
}





static int biTableApply(unsigned char* table, int count) {
    if (!table || count != BI_RECORDS) return -1;
    int todo = 0;
    for (int i = 0; i < 14; i++) {
        int row = biHeroRows[i], hi = row >= BI_HIRES_FIRST;
        unsigned char* r = table + row * BI_RECORD_SIZE;
        int ox = hi ? 400 : 320, oy = hi ? 60 : 0;
        if (biRecordIs(r, ox, oy, 0)) todo++;
        else if (!biRecordIs(r, ox, oy, 1)) return -1;
    }
    for (int i = 0; i < 14 && todo; i++) {
        int row = biHeroRows[i], hi = row >= BI_HIRES_FIRST;
        unsigned char* r = table + row * BI_RECORD_SIZE;
        int ox = hi ? 400 : 320, oy = hi ? 60 : 0;
        if (biRecordIs(r, ox, oy, 0)) biRecordWrite(r, ox, oy);
    }
    return todo;
}


static void biGridOf(const unsigned char* table, int count, int row, unsigned char* w, unsigned char* h) {
    *w = *h = 0;
    if (!table || count != BI_RECORDS || row < 0 || row >= BI_RECORDS) return;
    *w = table[row * BI_RECORD_SIZE + BIR_GRID_X];
    *h = table[row * BI_RECORD_SIZE + BIR_GRID_X + 1];
}


static int biHeroRow(int cls) {
    return cls >= 0 && cls < 7 ? biHeroRows[7 + cls] : -1;
}



static const short* biSrc;
static short* biDst;


static void biBlit(int sx, int sy, int w, int h, int dx, int dy) {
    for (int y = 0; y < h; y++) {
        int ty = dy + y, fy = sy + y;
        if (ty < 0 || ty >= BI_PANEL_H || fy < 0 || fy >= BI_PANEL_H) continue;
        for (int x = 0; x < w; x++) {
            int tx = dx + x, fx = sx + x;
            if (tx >= 0 && tx < BI_PANEL_W && fx >= 0 && fx < BI_PANEL_W) biDst[ty * BI_PANEL_W + tx] = biSrc[fy * BI_PANEL_W + fx];
        }
    }
}





static void biBox(const BiBox* o, const BiBox* n, int left, int top, int right, int bottom) {
    int half = n->w / 2;
    biBlit(o->left - left, o->top - top, left + half, o->h + top + bottom, n->left - left, n->top - top);
    int rest = n->w - half + right;
    biBlit(o->left + o->w + right - rest, o->top - top, rest, o->h + top + bottom, n->left + half, n->top - top);
}





static int biCompose(const short* in, short* out) {

    for (int x = 4; x < BI_PANEL_W - 4; x++) if (in[(BI_GRID_TOP_ORIG - 4) * BI_PANEL_W + x] < 0) return 0;
    biSrc = in;
    biDst = out;
    memcpy(out, in, sizeof(short) * BI_PANEL_W * BI_PANEL_H);
    int topEnd = BI_GRID_TOP - 7;

    static const short patches[2][4] = {{82, 66, 46, 100}, {197, 66, 47, 100}};
    unsigned int seed = 1107;
    for (int ty = 0; ty < topEnd; ty += 50) {
        for (int tx = 4; tx < BI_PANEL_W - 4; tx += 23) {
            seed = seed * 1103515245u + 12345u;
            const short* p = patches[(seed >> 16) & 1];
            seed = seed * 1103515245u + 12345u;
            int ox = (int) ((seed >> 16) % (unsigned) (p[2] - 23));
            seed = seed * 1103515245u + 12345u;
            int oy = (int) ((seed >> 16) % (unsigned) (p[3] - 50));
            int w = BI_PANEL_W - 4 - tx < 23 ? BI_PANEL_W - 4 - tx : 23, h = topEnd - ty < 50 ? topEnd - ty : 50;
            biBlit(p[0] + ox, p[1] + oy, w, h, tx, ty);
        }
    }
    biBlit(0, 0, BI_PANEL_W, 3, 0, 0);
    biBlit(0, 0, 4, topEnd, 0, 0);
    biBlit(BI_PANEL_W - 4, 0, 4, topEnd, BI_PANEL_W - 4, 0);

    int y = topEnd;
    biBlit(0, BI_GRID_TOP_ORIG - 7, BI_PANEL_W, 36, 0, y);
    y += 36;
    for (int k = 0; k < BI_ROWS - 2; k++) {
        biBlit(0, BI_GRID_TOP_ORIG + 29 + 29 * (k % 2), BI_PANEL_W, 29, 0, y);
        y += 29;
    }
    biBlit(0, BI_GRID_TOP_ORIG + 87, BI_PANEL_W, 31, 0, y);
    y += 31;
    if (y > 379) return 0;
    if (y < 379) biBlit(0, 373, BI_PANEL_W, 379 - y, 0, y);

    for (int s = 0; s < 3; s += 2) biBox(&biOrig[s], &biNew[s], 5, 141 - 4 - biOrig[s].h, 67 - 5 - biOrig[s].w, 4);
    static const int others[8] = {3, 1, 9, 7, 4, 5, 6, 8};
    for (int shadow = 1; shadow >= 0; shadow--)
        for (int i = 0; i < 8; i++) biBox(&biOrig[others[i]], &biNew[others[i]], shadow ? 5 : 2, 5, 4, 4);
    biBlit(0, 0, BI_PANEL_W, 2, 0, 0);
    return 1;
}
