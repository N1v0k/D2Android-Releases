package com.winlator.d2;











public final class D2StoneButton {




    public static final int EDGE = 16;







    public static final int PLAIN_SQUARE = 32, PLAIN_EDGE = 8, PLAIN_TILE = PLAIN_SQUARE - 2 * PLAIN_EDGE,
        PLAIN_MIN_TILES = 7, PLAIN_MAX_TILES = 16, PLAIN_PAD = 12;

    private D2StoneButton() {}


    public static int plainTiles(int labelWidth) {
        int room = Math.max(0, labelWidth) + 2 * PLAIN_PAD - 2 * PLAIN_EDGE;
        int tiles = (room + PLAIN_TILE - 1) / PLAIN_TILE;
        return Math.max(PLAIN_MIN_TILES, Math.min(PLAIN_MAX_TILES, tiles));
    }





    public static int[] widen(int[] square, int side, int tiles) {
        if (square == null || side != PLAIN_SQUARE || square.length < side * side || tiles < 1) return null;
        int e = PLAIN_EDGE, t = PLAIN_TILE, w = 2 * e + tiles * t;
        int[] px = new int[w * side];
        for (int y = 0; y < side; y++) {
            System.arraycopy(square, y * side, px, y * w, e);
            for (int i = 0; i < tiles; i++) System.arraycopy(square, y * side + e, px, y * w + e + i * t, t);
            System.arraycopy(square, y * side + e + t, px, y * w + w - e, e);
        }
        return px;
    }


    public static int plainWidth(int tiles) {
        return 2 * PLAIN_EDGE + tiles * PLAIN_TILE;
    }






    public static int scale(float boxHeight, int stoneHeight) {
        if (stoneHeight <= 0 || !(boxHeight > 0)) return 1;
        return Math.max(1, Math.round(boxHeight / stoneHeight));
    }





    public static int[] stone(float cx, float cy, int stoneWidth, int stoneHeight, int s) {
        int w = stoneWidth * s, h = stoneHeight * s;
        return new int[]{Math.round(cx - w / 2f), Math.round(cy - h / 2f), w, h};
    }





    public static int labelScale(int labelWidth, int stoneWidth, int s) {
        int room = stoneWidth - 2 * EDGE;
        int ls = Math.max(1, s);
        while (ls > 1 && labelWidth * ls > room * s) ls--;
        return ls;
    }
}
