package com.winlator.d2;







final class D2SkillButton {

    static final int NEAR = 117, FAR = 165, HIGH = 48;

    private D2SkillButton() {}


    static float[] center(boolean left, int gameWidth, int gameHeight) {
        float half = (NEAR + FAR) / 2f;
        return new float[]{left ? half : gameWidth - half, gameHeight - HIGH / 2f};
    }


    static boolean hit(boolean left, int x, int y, int gameWidth, int gameHeight) {
        int x0 = left ? NEAR : gameWidth - FAR, x1 = left ? FAR : gameWidth - NEAR;
        return x >= x0 && x <= x1 && y > gameHeight - HIGH && y <= gameHeight;
    }
}
