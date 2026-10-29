








#define LYR_LAYERS      16

#define LYR_OPS         400
#define LYR_TEXT        64
#define LYR_PICTURES    256

#define LYR_PATH        64
#define LYR_STAGE_L     1
#define LYR_STAGE_T     2
#define LYR_HEAD        22
#define LYR_PARTS       32
#define LYR_MAX_SIZE    2048
#define LYR_MAX_PIC     256

#define LOP_RECT   1
#define LOP_TEXT   2
#define LOP_PIC    3
#define LOP_LINE   4
#define LOP_GLYPH  5
#define LOP_TABLE  7

typedef struct {
    unsigned char kind;
    unsigned char a, b, c;
    short x, y, w, h;
    unsigned short sx, sy, sw, sh;
    unsigned short len;
    unsigned short text[LYR_TEXT];
} LayerOp;

typedef struct {
    unsigned char stage, parts;
    unsigned int have;
    unsigned int stand;
    unsigned short validMs, width, height;
    int count;
    LayerOp ops[LYR_OPS];
} LayerList;

static unsigned short lyrU16(const unsigned char* b) {
    return (unsigned short) (b[0] | b[1] << 8);
}

static unsigned int lyrU32(const unsigned char* b) {
    return (unsigned int) b[0] | (unsigned int) b[1] << 8 | (unsigned int) b[2] << 16 | (unsigned int) b[3] << 24;
}







static int layerParse(const unsigned char* buf, int n, LayerList* list, char (*table)[LYR_PATH]) {
    int at = LYR_HEAD, added = 0, count = list->count;
    while (at < n) {
        unsigned char kind = buf[at];
        if (kind == LOP_TABLE) {
            if (at + 3 > n) return -1;
            int no = buf[at + 1], len = buf[at + 2];
            if (no >= LYR_PICTURES || len == 0 || len >= LYR_PATH || at + 3 + len > n) return -1;
            for (int i = 0; i < len; i++) {
                unsigned char ch = buf[at + 3 + i];
                if (ch < 0x20 || ch > 0x7E) return -1;
            }
            if (table) {
                for (int i = 0; i < len; i++) table[no][i] = (char) buf[at + 3 + i];
                table[no][len] = 0;
            }
            at += 3 + len;
            continue;
        }
        if (count + added >= LYR_OPS) return -1;
        LayerOp* op = &list->ops[count + added];
        op->kind = kind;
        if (kind == LOP_RECT) {
            if (at + 11 > n) return -1;
            op->x = (short) lyrU16(buf + at + 1);
            op->y = (short) lyrU16(buf + at + 3);
            op->w = (short) lyrU16(buf + at + 5);
            op->h = (short) lyrU16(buf + at + 7);
            op->a = buf[at + 9];
            op->b = buf[at + 10];
            if (op->w <= 0 || op->h <= 0 || op->w > LYR_MAX_SIZE || op->h > LYR_MAX_SIZE || op->b > 3) return -1;
            at += 11;
        }
        else if (kind == LOP_GLYPH) {

            if (at + 10 > n) return -1;
            op->a = buf[at + 1];
            op->b = buf[at + 2];
            op->x = (short) lyrU16(buf + at + 3);
            op->y = (short) lyrU16(buf + at + 5);
            op->sx = buf[at + 7];
            op->sy = buf[at + 8];
            op->sw = buf[at + 9];
            op->sh = 0;
            op->w = op->h = 0;
            op->c = 3;
            if (op->a >= LYR_PICTURES || op->sx > op->sy) return -1;
            at += 10;
        }
        else if (kind == LOP_LINE) {
            if (at + 10 > n) return -1;
            op->x = (short) lyrU16(buf + at + 1);
            op->y = (short) lyrU16(buf + at + 3);
            op->w = (short) lyrU16(buf + at + 5);
            op->h = (short) lyrU16(buf + at + 7);
            op->a = buf[at + 9];
            op->b = 3;
            if (op->w <= 0 || op->h <= 0 || op->w > LYR_MAX_SIZE || op->h > LYR_MAX_SIZE) return -1;
            at += 10;
        }
        else if (kind == LOP_TEXT) {
            if (at + 9 > n) return -1;
            op->x = (short) lyrU16(buf + at + 1);
            op->y = (short) lyrU16(buf + at + 3);
            op->a = buf[at + 5];
            op->b = buf[at + 6];
            op->c = buf[at + 7];
            op->len = buf[at + 8];
            if (op->len == 0 || op->len > LYR_TEXT - 1 || op->a > 13 || op->c > 2 || at + 9 + 2 * op->len > n) return -1;
            for (int i = 0; i < op->len; i++) {
                unsigned short ch = lyrU16(buf + at + 9 + 2 * i);
                if (ch < 0x20) return -1;
                op->text[i] = ch;
            }
            op->text[op->len] = 0;
            at += 9 + 2 * op->len;
        }
        else if (kind == LOP_PIC) {
            if (at + 20 > n) return -1;
            op->a = buf[at + 1];
            op->b = buf[at + 2];
            op->sx = lyrU16(buf + at + 3);
            op->sy = lyrU16(buf + at + 5);
            op->sw = lyrU16(buf + at + 7);
            op->sh = lyrU16(buf + at + 9);
            op->x = (short) lyrU16(buf + at + 11);
            op->y = (short) lyrU16(buf + at + 13);
            op->w = (short) lyrU16(buf + at + 15);
            op->h = (short) lyrU16(buf + at + 17);
            op->c = buf[at + 19];
            if (op->a >= LYR_PICTURES || op->c > 3 || op->w <= 0 || op->h <= 0 || op->w > LYR_MAX_PIC || op->h > LYR_MAX_PIC
                || op->sw > LYR_MAX_PIC || op->sh > LYR_MAX_PIC) return -1;
            at += 20;
        }
        else return -1;
        added++;
    }
    list->count = count + added;
    return added;
}






static int lyrDecodeRle(const unsigned char* data, unsigned length, int flipped, int w, int h, short* out) {
    for (int i = 0; i < w * h; i++) out[i] = -1;
    int x = 0, row = 0;
    for (unsigned i = 0; i < length && row < h;) {
        unsigned char b = data[i++];
        if (b == 0x80) {
            x = 0;
            row++;
            continue;
        }
        if (b & 0x80) {
            x += b & 0x7F;
            continue;
        }
        if (i + b > length) return 0;
        int y = flipped ? row : h - 1 - row;
        for (int k = 0; k < b; k++, x++) if (x < w && y >= 0) out[y * w + x] = data[i + k];
        i += b;
    }
    return 1;
}





static int lyrEncodeRle(const short* px, int w, int h, unsigned char* out, int room) {
    int n = 0;
    for (int row = 0; row < h; row++) {
        const short* line = px + (h - 1 - row) * w;
        int end = w;
        while (end > 0 && line[end - 1] < 0) end--;
        for (int x = 0; x < end;) {
            if (line[x] < 0) {
                int run = 0;
                while (x < end && line[x] < 0 && run < 127) { x++; run++; }
                if (n + 1 > room) return -1;
                out[n++] = (unsigned char) (0x80 | run);
            }
            else {
                int run = 0;
                while (x + run < end && line[x + run] >= 0 && run < 127) run++;
                if (n + 1 + run > room) return -1;
                out[n++] = (unsigned char) run;
                for (int k = 0; k < run; k++) out[n++] = (unsigned char) line[x + k];
                x += run;
            }
        }
        if (n + 1 > room) return -1;
        out[n++] = 0x80;
    }
    return n;
}





static void lyrScale(const short* src, int w, int h, int sx, int sy, int sw, int sh, short* out, int dw, int dh) {
    for (int y = 0; y < dh; y++) {
        int fy = sy + (int) ((long) y * sh / dh);
        for (int x = 0; x < dw; x++) {
            int fx = sx + (int) ((long) x * sw / dw);
            out[y * dw + x] = fx >= 0 && fx < w && fy >= 0 && fy < h ? src[fy * w + fx] : -1;
        }
    }
}
