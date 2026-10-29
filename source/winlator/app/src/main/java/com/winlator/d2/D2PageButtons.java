package com.winlator.d2;













final class D2PageButtons {






    static final int INVENTORY_PAGE_RIGHT = 721, CHARACTER_PAGE_LEFT = 80, ROW_Y = 461;

    static final int SQUARE = 32, EDGE = 8, TILE = SQUARE - 2 * EDGE, TILES = 5, STONE_W = 2 * EDGE + TILES * TILE, STONE_H = SQUARE;

    static final int SORT_INSET = 5;

    static final int RESPEC_X = 92, RESPEC_Y = 406;

    private D2PageButtons() {}


    static void sort(int gameWidth, int gameHeight, float[] out) {
        float left = (gameWidth - 800) / 2f, top = (gameHeight - 600) / 2f;
        set(out, left + INVENTORY_PAGE_RIGHT - SORT_INSET - STONE_W, top + ROW_Y - STONE_H / 2 + 1);
    }


    static void respec(int gameWidth, int gameHeight, float[] out) {
        float left = (gameWidth - 800) / 2f, top = (gameHeight - 600) / 2f;
        set(out, left + RESPEC_X, top + RESPEC_Y);
    }





    static boolean respecShown(int free) {
        return free <= 0;
    }

    private static void set(float[] out, float x, float y) {
        out[0] = x;
        out[1] = y;
        out[2] = STONE_W;
        out[3] = STONE_H;
    }
}
