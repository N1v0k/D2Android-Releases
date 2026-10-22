package com.winlator.d2;

public final class D2PointSpots {
    public static final int BOX = 48;
    private static final int TAB_X = 217, TAB_W = 88, TAB_TOP = 46, TAB_H = 108;
    private static final int PLUS = 32, PLUS_X = 141;
    private static final int[] PLUS_Y = {92, 302, 154, 240};

    private D2PointSpots() {}

    public static boolean skillBox(D2Points p, int column, int row, int[] out) {
        if (p == null || !p.skillTreeKnown() || column < 1 || column > 3 || row < 1 || row > 6) return false;
        return set(out, p.columnX[column - 1], p.rowY[row - 1] - BOX, BOX, BOX);
    }

    public static boolean tab(D2Points p, int page, int[] out) {
        if (p == null || !p.skillTreeKnown() || page < 1 || page > 3) return false;
        return set(out, p.columnX[0] + TAB_X, p.rowY[0] + TAB_TOP + (3 - page) * TAB_H, TAB_W, TAB_H - 1);
    }

    public static boolean statPlus(D2Points p, int gameWidth, int stat, int[] out) {
        if (p == null || !p.skillTreeKnown() || gameWidth <= 0 || stat < 0 || stat > 3) return false;
        int left = gameWidth - (p.columnX[0] + 305), top = p.rowY[0] - 63;
        return set(out, left + PLUS_X - PLUS / 2, top + PLUS_Y[stat] - PLUS / 2, PLUS, PLUS);
    }

    public static final int FRAME_GAP = 1, FRAME_LINE = 6;

    public static void frame(int[] spot, int[] out) {
        int d = FRAME_GAP + FRAME_LINE;
        set(out, spot[0] - d, spot[1] - d, spot[2] + 2 * d, spot[3] + 2 * d);
    }

    private static boolean set(int[] out, int x, int y, int width, int height) {
        out[0] = x;
        out[1] = y;
        out[2] = width;
        out[3] = height;
        return true;
    }
}
