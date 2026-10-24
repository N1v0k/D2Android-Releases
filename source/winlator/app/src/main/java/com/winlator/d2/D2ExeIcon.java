package com.winlator.d2;








final class D2ExeIcon {
    static final int RT_ICON = 3;

    static final int MAX_EXE = 16 << 20;


    static final class Picture {
        final int width, height;
        final int[] argb;
        final byte[] png;

        Picture(int width, int height, int[] argb, byte[] png) {
            this.width = width;
            this.height = height;
            this.argb = argb;
            this.png = png;
        }
    }

    private D2ExeIcon() {}


    static Picture read(byte[] exe) {
        try {
            return readOrThrow(exe);
        }
        catch (RuntimeException e) {
            return null;
        }
    }

    private static Picture readOrThrow(byte[] b) {
        if (b == null || b.length < 0x40 || b[0] != 'M' || b[1] != 'Z') return null;
        int pe = s32(b, 0x3C);
        if (pe <= 0 || pe + 24 > b.length || s32(b, pe) != 0x00004550) return null;
        int sections = u16(b, pe + 6), optSize = u16(b, pe + 20), opt = pe + 24;
        int magic = u16(b, opt);
        if (magic != 0x10B && magic != 0x20B) return null;
        int dirs = opt + (magic == 0x20B ? 112 : 96);
        if (dirs + 3 * 8 > opt + optSize) return null;
        int resRva = s32(b, dirs + 2 * 8);
        if (resRva == 0) return null;
        int[][] sect = new int[sections][];
        for (int i = 0; i < sections; i++) {
            int at = opt + optSize + i * 40;
            sect[i] = new int[]{s32(b, at + 12), Math.max(s32(b, at + 8), s32(b, at + 16)), s32(b, at + 20)};
        }
        int root = offset(sect, resRva);
        if (root < 0) return null;


        int icons = sub(b, root, 0, RT_ICON);
        if (icons < 0) return null;
        int at = root + icons, n = Math.min(4096, u16(b, at + 12) + u16(b, at + 14));
        Picture best = null;
        int bestSeen = -1;
        for (int i = 0; i < n; i++) {
            int to = s32(b, at + 16 + i * 8 + 4);
            if (to >= 0) continue;
            int[] d = data(b, sect, root, to & 0x7FFFFFFF);
            Picture p = d == null ? null : decode(b, d[0], d[1]);
            int seen = p == null ? 0 : p.png != null ? p.width * p.height : visible(p);
            if (seen == 0) continue;
            if (best == null || p.width > best.width || p.width == best.width && seen > bestSeen) {
                best = p;
                bestSeen = seen;
            }
        }
        return best;
    }


    static int visible(Picture p) {
        int n = 0;
        for (int c : p.argb) if ((c >>> 24) != 0) n++;
        return n;
    }





    static int sub(byte[] b, int root, int dir, int id) {
        if (dir < 0) return -1;
        int at = root + dir, n = u16(b, at + 12) + u16(b, at + 14);
        if (n > 4096) return -1;
        for (int i = 0; i < n; i++) {
            int e = at + 16 + i * 8, name = s32(b, e), to = s32(b, e + 4);
            if (to >= 0) continue;
            if (id < 0 || name == id) return to & 0x7FFFFFFF;
        }
        return -1;
    }


    static int[] data(byte[] b, int[][] sect, int root, int dir) {
        if (dir < 0) return null;
        int at = root + dir;
        if (u16(b, at + 12) + u16(b, at + 14) == 0) return null;
        int to = s32(b, at + 16 + 4);
        if (to < 0) return null;
        int entry = root + to, off = offset(sect, s32(b, entry)), size = s32(b, entry + 4);
        if (off < 0 || size <= 0 || off + size > b.length) return null;
        return new int[]{off, size};
    }


    static Picture decode(byte[] b, int at, int size) {
        if (size >= 24 && (b[at] & 0xFF) == 0x89 && b[at + 1] == 'P' && b[at + 2] == 'N' && b[at + 3] == 'G') {
            return new Picture(be32(b, at + 16), be32(b, at + 20), null, java.util.Arrays.copyOfRange(b, at, at + size));
        }
        if (size < 40) return null;
        int head = s32(b, at), w = s32(b, at + 4), h = s32(b, at + 8) / 2, bits = u16(b, at + 14), comp = s32(b, at + 16);
        int used = s32(b, at + 32);
        if (head < 40 || head > size || w <= 0 || w > 256 || h <= 0 || h > 256 || comp != 0) return null;
        if (bits != 1 && bits != 4 && bits != 8 && bits != 24 && bits != 32) return null;
        int colours = bits <= 8 ? (used > 0 && used <= 1 << bits ? used : 1 << bits) : 0;
        int pal = at + head, pixels = pal + colours * 4;
        int stride = (w * bits + 31) / 32 * 4, maskStride = (w + 31) / 32 * 4, mask = pixels + stride * h;
        if (mask > at + size) return null;
        boolean hasMask = mask + maskStride * h <= at + size;
        int[] out = new int[w * h];
        boolean ownAlpha = false;
        for (int y = 0; y < h; y++) {
            int row = pixels + (h - 1 - y) * stride;
            for (int x = 0; x < w; x++) {
                int p, c;
                if (bits == 32) {
                    p = row + x * 4;
                    c = (b[p + 3] & 0xFF) << 24 | (b[p + 2] & 0xFF) << 16 | (b[p + 1] & 0xFF) << 8 | (b[p] & 0xFF);
                    ownAlpha |= (c >>> 24) != 0;
                }
                else if (bits == 24) {
                    p = row + x * 3;
                    c = 0xFF000000 | (b[p + 2] & 0xFF) << 16 | (b[p + 1] & 0xFF) << 8 | (b[p] & 0xFF);
                }
                else {
                    int bit = x * bits, v = (b[row + bit / 8] & 0xFF) >> (8 - bits - bit % 8) & ((1 << bits) - 1);
                    if (v >= colours) return null;
                    p = pal + v * 4;
                    c = 0xFF000000 | (b[p + 2] & 0xFF) << 16 | (b[p + 1] & 0xFF) << 8 | (b[p] & 0xFF);
                }
                out[y * w + x] = c;
            }
        }

        if (bits == 32 && ownAlpha) return new Picture(w, h, out, null);
        for (int y = 0; y < h; y++) {
            int row = mask + (h - 1 - y) * maskStride;
            for (int x = 0; x < w; x++) {
                boolean clear = hasMask && ((b[row + x / 8] & 0xFF) >> (7 - x % 8) & 1) != 0;
                out[y * w + x] = clear ? 0 : out[y * w + x] | 0xFF000000;
            }
        }
        return new Picture(w, h, out, null);
    }

    private static int offset(int[][] sect, int rva) {
        for (int[] s : sect) if (rva >= s[0] && rva < s[0] + s[1]) return rva - s[0] + s[2];
        return -1;
    }

    private static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }

    private static int s32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    private static int be32(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | (b[at + 3] & 0xFF);
    }
}
