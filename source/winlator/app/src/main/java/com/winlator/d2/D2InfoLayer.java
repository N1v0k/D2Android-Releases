package com.winlator.d2;








final class D2InfoLayer {

    static final int PIC_CTRL = 82, PIC_ARROW = 83;
    static final String PATH_CTRL = "data\\global\\ui\\PANEL\\800ctrlpnl7", PATH_ARROW = "data\\global\\ui\\PANEL\\menubutton";

    static final int DOWN_FONT8 = 7, DOWN_FONT6 = 5;


    interface Art extends D2MaterialsLayer.Art {

        boolean barPieces();


        int[] arrowCrop(int index);
    }


    static final class State {
        boolean grid, box, arrow, boxOpen, onBar;

        final String[] gridText = new String[D2InfoGrid.SLOTS];
        final boolean[] gridWhite = new boolean[D2InfoGrid.SLOTS];
        String[] boxText = new String[0];
        boolean[] boxWhite = new boolean[0];
    }


    static final int BOX_PAD = 5, BOX_GAP = 8;

    private D2InfoLayer() {}






    static final int SMALL_EXTRA = 6;


    static String small(String t, java.util.function.ToIntFunction<String> width, int room) {
        return D2Materials.fit(t, width, room + SMALL_EXTRA);
    }

    static void build(D2Layer l, Art art, int gameWidth, int gameHeight, State st) {
        l.stage(D2Layer.STAGE_BAR);
        int c = Math.round(gameWidth / 2f);
        if (st.grid) {

            int left = c + D2InfoGrid.LEFT, right = c + D2InfoGrid.RIGHT + 1, top = gameHeight - D2InfoGrid.TOP,
                bottom = gameHeight - D2InfoGrid.BOTTOM;
            if (art.barPieces()) {
                stretch(l, STONE, 0, left, gameHeight - D2InfoGrid.STONE_TOP, right - left, D2InfoGrid.STONE_TOP - D2InfoGrid.TOP, -1);
                stretch(l, BUTTON_BOX, 4, left, top, right - left, bottom - top, art.colour(0x000000));
            }
            else D2MaterialsLayer.box(l, art, left, top, right - left, bottom - top, 0x000000);
            float in = 4, halfW = (right - left - 2 * in) / 2, rowH = (bottom - top - 2 * in) / 2;
            for (int i = 0; i < D2InfoGrid.SLOTS; i++) {
                String t = st.gridText[i];
                if (t == null || t.isEmpty()) continue;
                int cx = Math.round(left + in + halfW * (i % 2) + halfW / 2), cy = Math.round(top + in + rowH * (i / 2) + rowH / 2);
                int room = Math.round(halfW - 4), colour = st.gridWhite[i] ? D2Layer.WHITE : D2Layer.GOLD;

                if (art.width(D2Layer.FONT8, t) <= room) l.text(cx, cy + DOWN_FONT8, D2Layer.FONT8, colour, D2Layer.CENTRE, t);
                else l.text(cx, cy + DOWN_FONT6, D2Layer.FONT6, colour, D2Layer.CENTRE,
                    small(t, s -> art.width(D2Layer.FONT6, s), room));
            }
        }
        if (st.box && st.boxOpen) {
            int w = 2 * BOX_PAD, n = 0;
            for (String t : st.boxText) {
                if (t == null || t.isEmpty()) continue;
                w += art.width(D2Layer.FONT8, t);
                n++;
            }
            if (n > 0) {
                w += (n - 1) * BOX_GAP;
                float[] b = D2InfoGrid.box(gameWidth, gameHeight, w, st.onBar);
                int bx = Math.round(b[0]), by = Math.round(b[1]), bh = Math.round(b[3] - b[1]);
                if (art.barPieces()) stretch(l, THIN_BOX, 2, bx, by, w, bh, art.colour(0x000000));
                else D2MaterialsLayer.box(l, art, bx, by, w, bh, 0x000000);
                int x = bx + BOX_PAD, cy = by + bh / 2;
                for (int i = 0; i < st.boxText.length; i++) {
                    String t = st.boxText[i];
                    if (t == null || t.isEmpty()) continue;
                    l.text(x, cy + DOWN_FONT8, D2Layer.FONT8, st.boxWhite[i] ? D2Layer.WHITE : D2Layer.GOLD, D2Layer.LEFT, t);
                    x += art.width(D2Layer.FONT8, t) + BOX_GAP;
                }
            }
        }
        if (st.arrow) {
            int index = st.boxOpen ? 2 : 0;
            int[] a = art.arrowCrop(index);
            float[] r = D2InfoGrid.arrow(gameWidth, gameHeight);
            if (a != null) {
                int x = Math.round((r[0] + r[2]) / 2f - a[4] / 2f), y = Math.round((r[1] + r[3]) / 2f - a[5] / 2f);
                l.picture(PIC_ARROW, PATH_ARROW, index, a[2], a[3], a[4], a[5], x, y, a[4], a[5]);
            }
        }
    }






    static final class Piece {
        final int[] frame, sx, sy;

        Piece(int[] frame, int[] sx, int[] sy) {
            this.frame = frame;
            this.sx = sx;
            this.sy = sy;
        }

        static Piece crop(int frame, int x, int y, int w, int h) {
            int[] f = new int[w], sx = new int[w], sy = new int[h];
            for (int i = 0; i < w; i++) {
                f[i] = frame;
                sx[i] = x + i;
            }
            for (int i = 0; i < h; i++) sy[i] = y + i;
            return new Piece(f, sx, sy);
        }
    }

    static final Piece STONE = Piece.crop(3, 0, 9, 124, 6), BUTTON_BOX = Piece.crop(1, 40, 15, 33, 33), THIN_BOX = thinBox();

    private static Piece thinBox() {
        int[] f = new int[40], sx = new int[40], sy = new int[6];
        for (int i = 0; i < 40; i++) {
            f[i] = i < 36 ? 1 : 2;
            sx[i] = i < 36 ? 89 + i : 80 + i - 36;
        }
        for (int i = 0; i < 6; i++) sy[i] = 14 + i;
        return new Piece(f, sx, sy);
    }






    static void stretch(D2Layer l, Piece p, int edge, int x, int y, int w, int h, int fill) {
        int pw = p.sx.length, ph = p.sy.length;
        int[] col = map(pw, edge, w), row = map(ph, edge, h);
        if (edge > 0 && fill >= 0 && w > 2 * edge && h > 2 * edge) l.rect(x + edge, y + edge, w - 2 * edge, h - 2 * edge, fill, D2Layer.OPAQUE);
        for (int r0 = 0; r0 < h; ) {
            int r1 = r0 + 1;
            while (r1 < h && p.sy[row[r1]] == p.sy[row[r1 - 1]] + 1 && middle(r1, edge, h) == middle(r0, edge, h)) r1++;
            for (int c0 = 0; c0 < w; ) {
                int c1 = c0 + 1;
                while (c1 < w && p.frame[col[c1]] == p.frame[col[c0]] && p.sx[col[c1]] == p.sx[col[c1 - 1]] + 1
                    && middle(c1, edge, w) == middle(c0, edge, w)) c1++;
                boolean inside = edge > 0 && middle(r0, edge, h) && middle(c0, edge, w);
                if (!inside) {
                    l.picture(PIC_CTRL, PATH_CTRL, p.frame[col[c0]], p.sx[col[c0]], p.sy[row[r0]], c1 - c0, r1 - r0,
                        x + c0, y + r0, c1 - c0, r1 - r0);
                }
                c0 = c1;
            }
            r0 = r1;
        }
    }

    private static boolean middle(int i, int edge, int n) {
        return i >= edge && i < n - edge;
    }


    static int[] map(int pw, int edge, int n) {
        int mid = Math.max(1, pw - 2 * edge);
        int[] m = new int[n];
        for (int i = 0; i < n; i++) m[i] = i < edge ? i : i >= n - edge ? pw - (n - i) : edge + (i - edge) % mid;
        return m;
    }
}
