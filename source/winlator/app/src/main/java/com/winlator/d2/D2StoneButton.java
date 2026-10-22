package com.winlator.d2;

public final class D2StoneButton {
    public static final int EDGE = 16;

    private D2StoneButton() {}

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
