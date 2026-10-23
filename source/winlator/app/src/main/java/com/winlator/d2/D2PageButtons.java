package com.winlator.d2;










final class D2PageButtons {






    static final int INVENTORY_PAGE_RIGHT = 721, CHARACTER_PAGE_LEFT = 80, ROW_Y = 461;

    static final int GAP = 4;






    static final int GLOBE_TEXT = 112;

    static final int STONE_W = 96, STONE_H = 32;

    static final int SORT_INSIDE_X = 642, SORT_INSIDE_Y = 446, RESPEC_INSIDE_X = 95, RESPEC_INSIDE_Y = 449;

    private D2PageButtons() {}





    static float[] stoneSize(float viewPerGame) {
        int s = D2StoneButton.scale(STONE_H * viewPerGame, STONE_H);
        return new float[]{STONE_W * s / viewPerGame, STONE_H * s / viewPerGame};
    }





    static void sort(int gameWidth, int gameHeight, float viewPerGame, float[] out) {
        float left = (gameWidth - 800) / 2f, top = (gameHeight - 600) / 2f;
        float[] size = stoneSize(viewPerGame);
        float x = left + INVENTORY_PAGE_RIGHT + GAP, y = row(gameHeight, top, size[1]);
        if (x + size[0] > gameWidth) {
            x = left + SORT_INSIDE_X;
            y = top + SORT_INSIDE_Y;
        }
        set(out, x, y, size);
    }





    static void respec(int gameWidth, int gameHeight, float viewPerGame, float[] out) {
        float left = (gameWidth - 800) / 2f, top = (gameHeight - 600) / 2f;
        float[] size = stoneSize(viewPerGame);
        float x = left + CHARACTER_PAGE_LEFT - GAP - size[0], y = row(gameHeight, top, size[1]);
        if (x < 0) {
            x = left + RESPEC_INSIDE_X;
            y = top + RESPEC_INSIDE_Y;
        }
        set(out, x, y, size);
    }


    private static float row(int gameHeight, float top, float height) {
        return Math.min(top + ROW_Y - height / 2f, gameHeight - GLOBE_TEXT - height);
    }

    private static void set(float[] out, float x, float y, float[] size) {
        out[0] = x;
        out[1] = y;
        out[2] = size[0];
        out[3] = size[1];
    }
}
