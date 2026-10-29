package com.winlator.d2;

















final class D2WindowInfo {

    enum Window { SHOP, MERC, STASH, CUBE, INVENTORY }


    interface Texts {

        String lines(Window window, boolean pad, boolean stash);
    }


    static final class Open {
        boolean inventory, stash, cube, merc, shop;

        boolean materialsWindow, materialsEntry;

        boolean pad;

        boolean any() {
            return inventory || stash || cube || merc || shop;
        }
    }


    static final long SHOW_MS = 10000;

    static final int Q = 32, Q_Y = 446;

    static final int CLOSE_INVENTORY_X = 418, CLOSE_STASH_X = 352;

    static final int GAP = 4;




    static final int Q_CUBE_X = 355, Q_CUBE_Y = 446 - 2 * D2MaterialsLayer.FRAME - GAP - Q;

    static final int Q_INVENTORY_X = 726;




    static final int Q_STASH_FRAME_X = 42;

    static final int SHOP_RIGHT = 388, SHOP_BOTTOM = 105, MERC_RIGHT = 344, MERC_BOTTOM = 460, LINE = 13;

    static final int SHOP_ROOM = 296, MERC_ROOM = 256;

    static final int BOX_PAD = 5, BOX_LINE = 12;

    private Window showing;
    private long shownAt;


    static int[] question(Window w, Open o) {
        switch (w) {
            case STASH:
                if (!o.stash || o.materialsWindow) return null;
                return new int[]{o.materialsEntry ? Q_STASH_FRAME_X : CLOSE_STASH_X - GAP - Q, Q_Y};
            case CUBE:
                return o.cube ? new int[]{Q_CUBE_X, Q_CUBE_Y} : null;
            case INVENTORY:

                return o.inventory && !o.shop ? new int[]{Q_INVENTORY_X, Q_Y} : null;
            default:
                return null;
        }
    }





    static boolean inventoryShown(boolean inventory, boolean stash, boolean cube) {
        return inventory || stash || cube;
    }


    Window showing(long now) {
        if (showing != null && now - shownAt >= SHOW_MS) showing = null;
        return showing;
    }






    Window update(Open o, Seen seen, long now) {
        Window s = showing(now);
        if (s != null && question(s, o) == null) showing = null;
        if (showing != null) return null;
        for (Window w : new Window[]{Window.STASH, Window.CUBE, Window.INVENTORY}) {
            if (question(w, o) != null && !seen.seen(w)) {
                showing = w;
                shownAt = now;
                return w;
            }
        }
        return null;
    }


    interface Seen {
        boolean seen(Window w);
    }





    boolean tap(float lx, float ly, Open o, long now, int[] box) {
        Window s = showing(now);
        if (s != null && box != null && lx >= box[0] && lx < box[0] + box[2] && ly >= box[1] && ly < box[1] + box[3]) {
            showing = null;
            return true;
        }
        for (Window w : Window.values()) {
            int[] q = question(w, o);
            if (q == null || lx < q[0] - GAP || lx >= q[0] + Q + GAP || ly < q[1] - GAP || ly >= q[1] + Q + GAP) continue;
            if (s == w) {
                showing = null;
            } else {
                showing = w;
                shownAt = now;
            }
            return true;
        }
        return false;
    }


    void hide() {
        showing = null;
    }

    static String[] split(String lines) {
        return lines == null || lines.isEmpty() ? new String[0] : lines.split("\\|");
    }





    D2Layer layer(D2MaterialsLayer.Art art, Open o, Texts t, int gameWidth, int gameHeight, long now, int[] box) {
        if (box != null) box[2] = 0;
        if (!o.any() || gameWidth <= 0 || gameHeight <= 0) return null;
        int ox = Math.round((gameWidth - 800) / 2f), oy = Math.round((gameHeight - 600) / 2f);
        D2Layer l = new D2Layer();
        if (o.shop) lines(l, split(t.lines(Window.SHOP, o.pad, false)), ox + SHOP_RIGHT, oy + SHOP_BOTTOM);
        else if (o.merc) lines(l, split(t.lines(Window.MERC, o.pad, false)), ox + MERC_RIGHT, oy + MERC_BOTTOM);
        for (Window w : Window.values()) {
            int[] q = question(w, o);
            if (q == null) continue;
            D2MaterialsLayer.stoneFrame(l, art, ox + q[0], oy + q[1], Q, Q);
            l.picture(D2MaterialsLayer.PIC_BLANK, D2MaterialsLayer.PATH_BLANK, 0, 0, 0, 0, 0, ox + q[0], oy + q[1], Q, Q);
            D2MaterialsLayer.stoneLabel(l, art, "?", ox + q[0] + Q / 2, oy + q[1] + Q / 2, Q - 8);
        }
        Window s = showing(now);
        int[] q = s != null ? question(s, o) : null;
        if (q != null) {
            String[] lines = split(t.lines(s, o.pad, o.stash));
            int w = 0;
            for (String line : lines) w = Math.max(w, art.width(D2Layer.FONT8, line));
            w += 2 * BOX_PAD;
            int h = lines.length * BOX_LINE + 2 * BOX_PAD - 2;
            int x = Math.max(0, q[0] + Q - w), y = q[1] - GAP - h;
            l.rect(ox + x, oy + y, w, h, art.colour(0x000000), D2Layer.ALPHA_75);
            int bottom = y + BOX_PAD + BOX_LINE - 1;
            for (String line : lines) {
                l.text(ox + x + w / 2, oy + bottom, D2Layer.FONT8, D2Layer.WHITE, D2Layer.CENTRE, line);
                bottom += BOX_LINE;
            }
            if (box != null) {
                box[0] = x;
                box[1] = y;
                box[2] = w;
                box[3] = h;
            }
        }
        return l.isEmpty() ? null : l;
    }

    private static void lines(D2Layer l, String[] lines, int right, int bottom) {
        for (String line : lines) {
            l.text(right, bottom, D2Layer.FONT8, D2Layer.GOLD, D2Layer.RIGHT, line);
            bottom += LINE;
        }
    }
}
