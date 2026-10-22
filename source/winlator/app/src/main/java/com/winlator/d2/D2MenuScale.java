package com.winlator.d2;

public final class D2MenuScale {
    private D2MenuScale() {}

    public static final int AUTO_LINES = 720;

    public static int[] expectedGame(int desktopWidth, int desktopHeight, int lines) {
        if (desktopWidth <= 0 || desktopHeight <= 0) return null;
        if (lines <= 0) lines = AUTO_LINES;
        int width = (int) (lines * (desktopWidth / (float) desktopHeight));
        width -= width % 2;
        return new int[]{width, lines};
    }

    public static float text(float aspect, int desktopWidth, int desktopHeight, int gameWidth, int gameHeight, int viewHeight) {
        if (gameWidth <= 0 || gameHeight <= 0 || desktopWidth <= 0 || desktopHeight <= 0) return viewHeight / 540f;
        return Math.min(desktopWidth / (float) gameWidth, desktopHeight / (float) gameHeight) * aspect;
    }
}
