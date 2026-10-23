package com.winlator.d2;















final class D2InfoGrid {

    static final int EMPTY = 0, FPS = 1, BATTERY = 2, PLAYERS = 3, CLOCK = 4, LEVEL = 5, DPS = 6, GOLD = 7, KINDS = 8;

    static final int[] DEFAULT_SLOTS = {FPS, BATTERY, PLAYERS, LEVEL};
    static final int SLOTS = 4;




    static final int LEFT = -146, RIGHT = -23, TOP = 39, BOTTOM = 6;

    static final int STONE_TOP = 45;


    static final int XP_HALF = 283, XP_TOP = 53, XP_BOTTOM = 47, XP_SECTIONS = 10;


    static final int XP_HALF_COMPACT = 235;

    static final int ARROW_HALF = 12, ARROW_TOP = 48, ARROW_BOTTOM = 14;


    static final int BOX_BOTTOM_XP = 53, BOX_BOTTOM = 47, BOX_HEIGHT = 18;

    static final int[] DEFAULT_BOX = {CLOCK, DPS, GOLD};
    static final int BOX_SLOTS = 3;

    static final long XP_UNKNOWN = 0xFFFFFFFFL;

    private D2InfoGrid() {}


    static float[] block(int gameWidth, int gameHeight) {
        float c = gameWidth / 2f;
        return new float[]{c + LEFT, gameHeight - TOP, c + RIGHT + 1, gameHeight - BOTTOM};
    }


    static final String[] HIGHLIGHT_KEYS = {null, "hiFps", "hiBattery", "hiPlayers", "hiClock", "hiLevel", "hiDps", "hiGold"};


    static boolean highlightDefault(int kind) {
        return kind == CLOCK;
    }





    static int[] stretch(int[] src, int sw, int sh, int edge, int w, int h, int fill) {
        int[] out = new int[w * h];
        int midW = Math.max(1, sw - 2 * edge), midH = Math.max(1, sh - 2 * edge);
        for (int y = 0; y < h; y++) {
            boolean top = y < edge, bottom = y >= h - edge;
            int sy = top ? y : bottom ? sh - (h - y) : edge + (y - edge) % midH;
            for (int x = 0; x < w; x++) {
                boolean left = x < edge, right = x >= w - edge;
                if (edge > 0 && !top && !bottom && !left && !right) {
                    out[y * w + x] = fill;
                    continue;
                }
                int sx = left ? x : right ? sw - (w - x) : edge + (x - edge) % midW;
                out[y * w + x] = src[sy * sw + sx];
            }
        }
        return out;
    }


    static int[] stretchWide(int[] src, int sw, int sh, int edge, int w) {
        int[] out = new int[w * sh];
        int midW = Math.max(1, sw - 2 * edge);
        for (int y = 0; y < sh; y++) {
            for (int x = 0; x < w; x++) {
                int sx = x < edge ? x : x >= w - edge ? sw - (w - x) : edge + (x - edge) % midW;
                out[y * w + x] = src[y * sw + sx];
            }
        }
        return out;
    }


    static float[] xpBar(int gameWidth, int gameHeight, boolean compact) {
        float c = gameWidth / 2f, half = compact ? XP_HALF_COMPACT : XP_HALF;
        return new float[]{c - half, gameHeight - XP_TOP, c + half, gameHeight - XP_BOTTOM};
    }


    static float[] arrow(int gameWidth, int gameHeight) {
        float c = gameWidth / 2f;
        return new float[]{c - ARROW_HALF, gameHeight - ARROW_TOP, c + ARROW_HALF, gameHeight - ARROW_BOTTOM};
    }


    static float[] box(int gameWidth, int gameHeight, float width, boolean xpBar) {
        float c = gameWidth / 2f, bottom = gameHeight - (xpBar ? BOX_BOTTOM_XP : BOX_BOTTOM);
        return new float[]{c - width / 2, bottom - BOX_HEIGHT, c + width / 2, bottom};
    }

    static boolean inside(float[] r, float x, float y) {
        return r != null && x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3];
    }


    static boolean onBlock(float x, float y, int gameWidth, int gameHeight) {
        if (gameWidth <= 0 || gameHeight <= 0) return false;
        return inside(block(gameWidth, gameHeight), x, y);
    }





    static float xpFilled(long xp, long from, long to) {
        if (from == XP_UNKNOWN) return -1f;
        if (to == XP_UNKNOWN) return 1f;
        if (to <= from) return -1f;
        return Math.max(0f, Math.min(1f, (xp - from) / (float) (to - from)));
    }


    static int boxSlot(int stored, int place) {
        return stored >= 0 && stored < KINDS ? stored : DEFAULT_BOX[place];
    }


    static int slot(int stored, int place) {
        return stored >= 0 && stored < KINDS ? stored : DEFAULT_SLOTS[place];
    }






    static boolean takesTap(boolean drawn, boolean cursorKnown, boolean cursorTaken, boolean ownFinger, boolean playtester,
                            boolean onIt) {
        return drawn && cursorKnown && !cursorTaken && !ownFinger && !playtester && onIt;
    }


    static boolean showsDps(int[] slots) {
        for (int s : slots) if (s == DPS) return true;
        return false;
    }


    static String gold(int gold) {
        if (gold < 100_000) return Integer.toString(Math.max(0, gold));
        if (gold < 10_000_000) return gold / 1000 + "k";
        return gold / 1_000_000 + "M";
    }


    static String clock(int hour, int minute) {
        return (hour < 10 ? "0" : "") + hour + ":" + (minute < 10 ? "0" : "") + minute;
    }
}
