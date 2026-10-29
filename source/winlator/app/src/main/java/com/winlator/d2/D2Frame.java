package com.winlator.d2;

import java.io.IOException;











public final class D2Frame {

    public static final int TILE = 12, LINE = 3;







    public static int menuInset(int inside, int unit, int k) {
        if (unit <= 0 || k <= 0 || inside < unit) return 0;
        int step = k % 2 == 0 ? k / 2 : k;
        return (inside % unit) / 2 / step * step;
    }
    private static final String FILE = "data\\global\\ui\\MENU\\boxpieces.dc6";
    private static final int TOP_LEFT = 0, TOP_RIGHT = 1, BOTTOM_LEFT = 8, BOTTOM_RIGHT = 9;
    private static final int TOP = 2, LEFT = 10, RIGHT = 13, BOTTOM = 16;


    private static final class Piece {
        final int width, height;
        final int[] argb;

        final int frame, x0, y0;

        Piece(D2Sprites.Frame f, int frame) {
            this.frame = frame;
            int minX = f.width, minY = f.height, maxX = -1, maxY = -1;
            for (int y = 0; y < f.height; y++) {
                for (int x = 0; x < f.width; x++) {
                    if (f.argb[y * f.width + x] == 0) continue;
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
            if (maxX < 0) minX = minY = maxX = maxY = 0;
            x0 = minX;
            y0 = minY;
            width = maxX - minX + 1;
            height = maxY - minY + 1;
            argb = new int[width * height];
            for (int y = 0; y < height; y++) System.arraycopy(f.argb, (minY + y) * f.width + minX, argb, y * width, width);
        }
    }

    private final Piece topLeft, topRight, bottomLeft, bottomRight;
    private final Piece[] top = new Piece[6], bottom = new Piece[6], left = new Piece[3], right = new Piece[3];

    private D2Frame(D2Sprites.Frame[] f) {
        topLeft = new Piece(f[TOP_LEFT], TOP_LEFT);
        topRight = new Piece(f[TOP_RIGHT], TOP_RIGHT);
        bottomLeft = new Piece(f[BOTTOM_LEFT], BOTTOM_LEFT);
        bottomRight = new Piece(f[BOTTOM_RIGHT], BOTTOM_RIGHT);
        for (int i = 0; i < 6; i++) {
            top[i] = new Piece(f[TOP + i], TOP + i);
            bottom[i] = new Piece(f[BOTTOM + i], BOTTOM + i);
        }
        for (int i = 0; i < 3; i++) {
            left[i] = new Piece(f[LEFT + i], LEFT + i);
            right[i] = new Piece(f[RIGHT + i], RIGHT + i);
        }
    }


    public static D2Frame load(D2Sprites sprites) throws IOException {
        return of(sprites.dc6(FILE));
    }


    static D2Frame of(D2Sprites.Frame[] f) {
        return f != null && f.length >= 22 ? new D2Frame(f) : null;
    }






    public int[] compose(int width, int height, int fill) {
        int w = Math.max(2 * LINE, width), h = Math.max(2 * LINE, height);
        int[] out = new int[w * h];
        if (fill != 0) {
            for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) out[y * w + x] = fill;
        }
        walk(w, h, (p, sx, sy, n, m, dx, dy) -> put(out, w, h, p, sx, sy, n, m, dx, dy));
        return out;
    }


    public interface PieceSink {

        void piece(int frame, int sx, int sy, int w, int h, int dx, int dy);
    }





    public void pieces(int width, int height, PieceSink sink) {
        int w = Math.max(2 * LINE, width), h = Math.max(2 * LINE, height);
        walk(w, h, (p, sx, sy, n, m, dx, dy) -> {
            int x0 = Math.max(0, Math.max(-sx, -dx)), y0 = Math.max(0, Math.max(-sy, -dy));
            int x1 = Math.min(n, Math.min(p.width - sx, w - dx)), y1 = Math.min(m, Math.min(p.height - sy, h - dy));
            if (x1 > x0 && y1 > y0) sink.piece(p.frame, p.x0 + sx + x0, p.y0 + sy + y0, x1 - x0, y1 - y0, dx + x0, dy + y0);
        });
    }

    private interface Put {
        void put(Piece p, int sx, int sy, int n, int m, int dx, int dy);
    }


    private void walk(int w, int h, Put put) {
        int corner = Math.max(LINE, Math.min(TILE, Math.min(w / 2, h / 2)));

        for (int x = corner, i = 0; x < w - corner; x += TILE, i++) {
            int n = Math.min(TILE, w - corner - x);
            put.put(pick(top, i, 1), 0, 0, n, LINE, x, 0);
            put.put(pick(bottom, i, 2), 0, 0, n, LINE, x, h - LINE);
        }
        for (int y = corner, i = 0; y < h - corner; y += TILE, i++) {
            int n = Math.min(TILE, h - corner - y);
            put.put(pick(left, i, 3), 0, 0, LINE, n, 0, y);
            put.put(pick(right, i, 4), 0, 0, LINE, n, w - LINE, y);
        }

        put.put(topLeft, 0, 0, corner, corner, 0, 0);
        put.put(topRight, TILE - corner, 0, corner, corner, w - corner, 0);
        put.put(bottomLeft, 0, TILE - corner, corner, corner, 0, h - corner);
        put.put(bottomRight, TILE - corner, TILE - corner, corner, corner, w - corner, h - corner);
    }





    public int[][] lines() {
        int w = 2 * LINE + 2, h = 2 * TILE + 3 * TILE;
        int[] box = compose(w, h, 0);
        int[][] lines = new int[LINE][];
        for (int x = 0; x < LINE; x++) {
            int[] colours = new int[h];
            int n = 0;
            for (int y = TILE; y < h - TILE; y++) {
                int c = box[y * w + x];
                if (c != 0) colours[n++] = c;
            }
            if (n == 0) return null;
            lines[x] = java.util.Arrays.copyOf(colours, n);
        }
        return lines;
    }







    public static int[] ring(int diameter, int[][] lines) {
        int d = Math.max(2 * LINE + 2, diameter);
        int[] out = new int[d * d];
        double c = d / 2.0;
        for (int y = 0; y < d; y++) {
            for (int x = 0; x < d; x++) {
                double dx = x + 0.5 - c, dy = y + 0.5 - c, depth = c - Math.hypot(dx, dy);
                if (depth < 0 || depth >= LINE) continue;
                int[] colours = lines[Math.min(lines.length - 1, (int) depth)];
                int along = (int) ((Math.atan2(dy, dx) + Math.PI) * c);
                out[y * d + x] = colours[along % colours.length];
            }
        }
        return out;
    }


    private static Piece pick(Piece[] variants, int i, int edge) {
        int hash = (i * 0x9E3779B1 + edge * 0x85EBCA6B) >>> 16;
        return variants[hash % variants.length];
    }


    private static void put(int[] out, int w, int h, Piece p, int sx, int sy, int n, int m, int dx, int dy) {
        for (int y = 0; y < m; y++) {
            int py = sy + y, oy = dy + y;
            if (py < 0 || py >= p.height || oy < 0 || oy >= h) continue;
            for (int x = 0; x < n; x++) {
                int px = sx + x, ox = dx + x;
                if (px < 0 || px >= p.width || ox < 0 || ox >= w) continue;
                int c = p.argb[py * p.width + px];
                if (c != 0) out[oy * w + ox] = c;
            }
        }
    }
}
