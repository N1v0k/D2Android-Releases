package com.winlator.d2;













final class D2InputBox {

    interface Listener {

        void ok(int number, String text);


        void cancel();
    }





    static final int W = 300, H = 156;




    static final int FIELD_W = 169, FIELD_H = 26, FIELD_Y = 50, SQUARE = 32, SQUARE_GAP = 6, BUTTON_W = 128, BUTTON_H = 35,
        BUTTON_Y = 108, OK_X = 14, BACK_X = 158;

    static final int SUB_BOTTOM = 26, LABEL_BOTTOM = FIELD_Y - 4, HINT_BOTTOM = FIELD_Y + FIELD_H + 20;

    static final int TEXT_DOWN = 21, TEXT_LEFT = 8;

    static final int DOWN_EXOCET = 6;

    static final int FILL = 0x0A0A0A;

    static final int TEXT_MAX = 24;

    static final int HIT_NONE = 0, HIT_MINUS = 1, HIT_PLUS = 2, HIT_FIELD = 3, HIT_OK = 4, HIT_BACK = 5, HIT_INSIDE = 6,
        HIT_OUTSIDE = 7;






    static final int PIC_FIELD = 89, PIC_SQUARE = D2MaterialsLayer.PIC_BLANK, PIC_BUTTON = D2MaterialsLayer.PIC_MEDIUM;
    static final String PATH_FIELD = "data\\global\\ui\\FrontEnd\\textbox2", PATH_SQUARE = D2MaterialsLayer.PATH_BLANK,
        PATH_BUTTON = D2MaterialsLayer.PATH_MEDIUM;

    final boolean number;
    final String title, subtitle;
    final int min, max;
    private final Listener listener;
    private String hint, okLabel = "OK", backLabel = "";
    private int value;
    private final StringBuilder text = new StringBuilder();

    private boolean replaceOnType = true;

    private int pressed = HIT_NONE;
    private boolean closed;

    private boolean busy;
    private int done, total;

    private int lift;

    private D2InputBox(boolean number, String title, String subtitle, int min, int max, int value, String text, Listener listener) {
        this.number = number;
        this.title = title;
        this.subtitle = subtitle;
        this.min = min;
        this.max = Math.max(min, max);
        this.value = clamp(value);
        this.listener = listener;
        if (text != null) this.text.append(text.length() > TEXT_MAX ? text.substring(0, TEXT_MAX) : text);
    }


    static D2InputBox number(String title, String subtitle, int min, int max, int value, Listener listener) {
        return new D2InputBox(true, title, subtitle, min, max, value, null, listener);
    }


    static D2InputBox text(String title, String subtitle, String text, Listener listener) {
        return new D2InputBox(false, title, subtitle, 0, 0, 0, text != null ? text : "", listener);
    }





    static D2InputBox progress(String title, String subtitle, int total) {
        D2InputBox b = new D2InputBox(false, title, subtitle, 0, 0, 0, "", null);
        b.busy = true;
        b.total = Math.max(1, total);
        return b;
    }

    boolean busy() {
        return busy;
    }


    void progress(int done) {
        this.done = Math.max(0, Math.min(total, done));
    }

    int done() {
        return done;
    }

    int total() {
        return total;
    }


    void finish() {
        closed = true;
    }


    static final int KEYBOARD_GAP = 4;


    static int liftFor(float keyboardTopGame, int gameHeight) {
        if (Float.isNaN(keyboardTopGame)) return 0;
        int bottom = originY(gameHeight) + H;
        return Math.max(0, (int) Math.ceil(bottom - (keyboardTopGame - KEYBOARD_GAP)));
    }


    void lift(int gamePixels) {
        lift = Math.max(0, gamePixels);
    }


    D2InputBox hint(String hint) {
        this.hint = hint;
        return this;
    }


    D2InputBox labels(String ok, String back) {
        if (ok != null) okLabel = ok;
        if (back != null) backLabel = back;
        return this;
    }

    int value() {
        return value;
    }

    String text() {
        return text.toString();
    }

    String hint() {
        return hint;
    }

    String okLabel() {
        return okLabel;
    }

    String backLabel() {
        return backLabel;
    }

    int pressed() {
        return pressed;
    }

    boolean closed() {
        return closed;
    }


    String shown() {
        if (busy) return done + " / " + total;
        return number ? String.valueOf(value) : text.toString();
    }

    private int clamp(int v) {
        return Math.max(min, Math.min(max, v));
    }




    static int originX(int gameWidth) {
        return (gameWidth - W) / 2;
    }

    static int originY(int gameHeight) {
        return (gameHeight - H) / 2;
    }


    int top(int gameHeight) {
        return Math.max(0, originY(gameHeight) - lift);
    }


    int[] rect(int part, int gameWidth, int gameHeight) {
        int ox = originX(gameWidth), oy = top(gameHeight), fx = ox + (W - FIELD_W) / 2, fy = oy + FIELD_Y;
        if (busy && part != HIT_FIELD) return null;
        switch (part) {
            case HIT_MINUS: return number ? new int[]{fx - SQUARE_GAP - SQUARE, fy - 3, SQUARE, SQUARE} : null;
            case HIT_PLUS: return number ? new int[]{fx + FIELD_W + SQUARE_GAP, fy - 3, SQUARE, SQUARE} : null;
            case HIT_FIELD: return new int[]{fx, fy, FIELD_W, FIELD_H};
            case HIT_OK: return new int[]{ox + OK_X, oy + BUTTON_Y, BUTTON_W, BUTTON_H};
            case HIT_BACK: return new int[]{ox + BACK_X, oy + BUTTON_Y, BUTTON_W, BUTTON_H};
            default: return null;
        }
    }





    int hit(float gx, float gy, int gameWidth, int gameHeight, float slack) {
        int best = HIT_NONE;
        float bestDistance = Float.MAX_VALUE;
        for (int part = HIT_MINUS; part <= HIT_BACK && !busy; part++) {
            int[] r = rect(part, gameWidth, gameHeight);
            if (r == null) continue;
            float dx = Math.max(Math.max(r[0] - gx, gx - (r[0] + r[2])), 0), dy = Math.max(Math.max(r[1] - gy, gy - (r[1] + r[3])), 0);
            if (dx > slack || dy > slack) continue;
            float d = dx + dy;
            if (d < bestDistance) {
                bestDistance = d;
                best = part;
            }
        }
        if (best != HIT_NONE) return best;
        int ox = originX(gameWidth), oy = top(gameHeight);
        return gx >= ox && gx < ox + W && gy >= oy && gy < oy + H ? HIT_INSIDE : HIT_OUTSIDE;
    }




    void press(int part) {
        pressed = busy ? HIT_NONE : part == HIT_MINUS || part == HIT_PLUS || part == HIT_OK || part == HIT_BACK ? part : HIT_NONE;
    }





    boolean release(int part) {
        int was = pressed;
        pressed = HIT_NONE;
        if (busy) return false;
        if (part == HIT_OUTSIDE) return cancel();
        if (part != was) return false;
        switch (part) {
            case HIT_MINUS: step(-1); return false;
            case HIT_PLUS: step(1); return false;
            case HIT_OK: return ok();
            case HIT_BACK: return cancel();
            default: return false;
        }
    }


    void step(int delta) {
        if (!number || closed) return;
        value = clamp(value + delta);
        replaceOnType = true;
    }





    void type(char c) {
        if (closed || busy) return;
        if (number) {
            if (c < '0' || c > '9') return;
            long v = replaceOnType ? c - '0' : value * 10L + (c - '0');
            replaceOnType = false;
            value = (int) Math.min(v, max);
            return;
        }
        if (c < 0x20 || c == 0x7F || text.length() >= TEXT_MAX) return;
        text.append(c);
    }


    void backspace() {
        if (closed || busy) return;
        if (number) {
            value /= 10;
            replaceOnType = false;
        }
        else if (text.length() > 0) text.setLength(text.length() - 1);
    }


    boolean ok() {
        if (closed) return true;
        if (busy) return false;
        if (!number && text.toString().trim().isEmpty()) return false;
        closed = true;
        value = clamp(value);
        if (listener != null) listener.ok(value, text.toString().trim());
        return true;
    }


    boolean cancel() {
        if (closed) return true;
        if (busy) return false;
        closed = true;
        if (listener != null) listener.cancel();
        return true;
    }





    static final int PAD_LEFT = 0, PAD_RIGHT = 1, PAD_UP = 2, PAD_DOWN = 3, PAD_A = 4, PAD_B = 5;

    boolean pad(int button) {
        if (busy) return true;
        switch (button) {
            case PAD_LEFT: step(-1); return true;
            case PAD_RIGHT: step(1); return true;
            case PAD_UP: step(10); return true;
            case PAD_DOWN: step(-10); return true;
            case PAD_A: ok(); return true;
            case PAD_B: cancel(); return true;
            default: return false;
        }
    }









    void build(D2Layer l, D2MaterialsLayer.Art art, int gameWidth, int gameHeight) {
        int ox = originX(gameWidth), oy = top(gameHeight), cx = ox + W / 2;
        l.stage(D2Layer.STAGE_TOOLTIP);
        D2MaterialsLayer.box(l, art, ox, oy, W, H, FILL);
        int[] f = rect(HIT_FIELD, gameWidth, gameHeight);
        if (subtitle != null && !subtitle.isEmpty()) {
            l.text(cx, oy + SUB_BOTTOM, D2Layer.FONT16, D2Layer.WHITE, D2Layer.CENTRE, fit(art, D2Layer.FONT16, subtitle, W - 24));
        }
        int room = W - 12 - (f[0] - ox);
        l.text(f[0] + 1, oy + LABEL_BOTTOM, D2Layer.FONT16, D2Layer.GOLD, D2Layer.LEFT, fit(art, D2Layer.FONT16, title, room));
        l.picture(PIC_FIELD, PATH_FIELD, 0, 0, 0, 0, 0, f[0], f[1], FIELD_W, FIELD_H);
        boolean formal = art == null || art.width(D2Layer.FORMAL12, "0") > 0;
        int font = formal ? D2Layer.FORMAL12 : D2Layer.FONT16;
        l.text(f[0] + TEXT_LEFT, f[1] + (formal ? TEXT_DOWN : FIELD_H / 2 + 8), font, D2Layer.WHITE, D2Layer.LEFT,
            fitEnd(art, font, busy ? shown() : shown() + "_", FIELD_W - 2 * TEXT_LEFT));
        if (busy) {
            int[] bar = bar(gameWidth, gameHeight);
            int edge = art != null ? art.colour(0x7B704D) : 0, gold = art != null ? art.colour(0xC7B377) : 0;
            l.rect(bar[0], bar[1], bar[2], bar[3], edge, D2Layer.OPAQUE);
            l.rect(bar[0] + 1, bar[1] + 1, bar[2] - 2, bar[3] - 2, art != null ? art.ink() : 0, D2Layer.OPAQUE);
            int fill = barFill(bar[2] - 4);
            if (fill > 0) l.rect(bar[0] + 2, bar[1] + 2, fill, bar[3] - 4, gold, D2Layer.OPAQUE);
            return;
        }
        if (number) {
            square(l, art, rect(HIT_MINUS, gameWidth, gameHeight), false, pressed == HIT_MINUS);
            square(l, art, rect(HIT_PLUS, gameWidth, gameHeight), true, pressed == HIT_PLUS);
        }
        if (hint != null && !hint.isEmpty()) {
            l.text(f[0] + 1, oy + HINT_BOTTOM, D2Layer.FONT16, D2Layer.GOLD, D2Layer.LEFT, fit(art, D2Layer.FONT16, hint, room));
        }
        button(l, art, rect(HIT_OK, gameWidth, gameHeight), okLabel, pressed == HIT_OK);
        button(l, art, rect(HIT_BACK, gameWidth, gameHeight), backLabel, pressed == HIT_BACK);
    }


    int[] bar(int gameWidth, int gameHeight) {
        return new int[]{originX(gameWidth) + OK_X, top(gameHeight) + BUTTON_Y + (BUTTON_H - BAR_H) / 2, W - 2 * OK_X, BAR_H};
    }


    int barFill(int inside) {
        return total <= 0 ? 0 : (int) ((long) inside * done / total);
    }

    static final int BAR_H = 14;






    private static void square(D2Layer l, D2MaterialsLayer.Art art, int[] r, boolean plus, boolean down) {
        l.picture(PIC_SQUARE, PATH_SQUARE, down ? 1 : 0, 0, 0, 0, 0, r[0], r[1], SQUARE, SQUARE);
        int d = down ? 1 : 0, cx = r[0] + SQUARE / 2 + d, cy = r[1] + SQUARE / 2 + d;
        int core = art != null ? art.colour(D2Layer.STONE_CORE_RGB) : 0, shade = art != null ? art.colour(D2Layer.STONE_SHADE_RGB) : 0;
        for (int pass = 0; pass < 2; pass++) {
            int o = pass == 0 ? 1 : 0, c = pass == 0 ? shade : core;
            l.rect(cx - SIGN_ARM - o, cy - 1 - o, 2 * SIGN_ARM + 2 * o, 3 + 2 * o, c, D2Layer.OPAQUE);
            if (plus) l.rect(cx - 1 - o, cy - SIGN_ARM - o, 3 + 2 * o, 2 * SIGN_ARM + 2 * o, c, D2Layer.OPAQUE);
        }
    }


    static final int SIGN_ARM = 7;

    private static void button(D2Layer l, D2MaterialsLayer.Art art, int[] r, String label, boolean down) {
        l.picture(PIC_BUTTON, PATH_BUTTON, down ? 1 : 0, 0, 0, 0, 0, r[0], r[1], BUTTON_W, BUTTON_H);
        int d = down ? 1 : 0;
        D2MaterialsLayer.stoneLabel(l, art, label, r[0] + BUTTON_W / 2 + d, r[1] + BUTTON_H / 2 + d, D2MaterialsLayer.MEDIUM_ROOM);
    }


    static String fit(D2MaterialsLayer.Art art, int font, String text, int room) {
        if (text == null) return "";
        if (art == null || art.width(font, text) <= room) return text;
        String t = text;
        while (t.length() > 1 && art.width(font, t + "...") > room) t = t.substring(0, t.length() - 1);
        return t + "...";
    }


    static String fitEnd(D2MaterialsLayer.Art art, int font, String text, int room) {
        if (art == null) return text;
        String t = text;
        while (t.length() > 1 && art.width(font, t) > room) t = t.substring(1);
        return t;
    }
}
