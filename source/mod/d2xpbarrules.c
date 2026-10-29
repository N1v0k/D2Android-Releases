





#define XPB_EDGE 4
#define XPB_PIECE_W 40






static int xpbDecodeRle(const unsigned char* d, unsigned length, int flip, int w, int h, short* out) {
    for (int i = 0; i < w * h; i++) out[i] = -1;
    int x = 0, y = flip ? 0 : h - 1, rows = 0;
    for (unsigned k = 0; k < length && y >= 0 && y < h;) {
        unsigned char b = d[k++];
        if (b == 0x80) {
            x = 0;
            y += flip ? 1 : -1;
            rows++;
        }
        else if (b & 0x80) x += b & 0x7F;
        else {
            for (int i = 0; i < b && k < length; i++, x++, k++) if (x < w) out[y * w + x] = d[k];
        }
    }
    return rows;
}


static int xpbPieceColumn(int c, int w) {
    int mid = XPB_PIECE_W - 2 * XPB_EDGE;
    return c < XPB_EDGE ? c : c >= w - XPB_EDGE ? XPB_PIECE_W - (w - c) : XPB_EDGE + (c - XPB_EDGE) % mid;
}



static int xpbFilled(int inLeft, int inRight, unsigned xp, unsigned from, unsigned to, int last) {
    if (last) return inRight;
    if (to <= from || xp < from) return inLeft;
    if (xp >= to) return inRight;
    return inLeft + (int) ((long long) (inRight - inLeft) * (xp - from) / (to - from));
}


static int xpbPercent(unsigned xp, unsigned from, unsigned to, int last) {
    if (last) return 100;
    if (to <= from || xp < from) return 0;
    if (xp >= to) return 100;
    return (int) (((long long) (xp - from) * 200 / (to - from) + 1) / 2);
}
