package com.winlator.d2;








public final class D2UiScale {
    private D2UiScale() {}


    public static final int LARGE_FROM_DP = 600;

    public static final float REF_WIDTH_DP = 2480 / 3f, REF_HEIGHT_DP = 1116 / 3f;




    public static final float LAUNCHER_MAX_UNIT_DP = 0.6f;











    public static float overlay(int widthPx, int heightPx, float density, int smallestWidthDp) {
        if (smallestWidthDp < LARGE_FROM_DP || density <= 0 || widthPx <= 0 || heightPx <= 0) return 1f;
        float longDp = Math.max(widthPx, heightPx) / density, shortDp = Math.min(widthPx, heightPx) / density;
        return Math.max(1f, Math.min(longDp / REF_WIDTH_DP, shortDp / REF_HEIGHT_DP));
    }







    public static float captionFit(int textWidth, float room) {
        return room / (textWidth + 4f);
    }






    public static float captionScale(int k, float fit, float overlayFactor) {
        if (overlayFactor <= 1f || fit >= k) return k;
        int whole = (int) fit;
        if (whole >= 1 && whole >= 0.9f * fit) return whole;
        return Math.max(1f, fit);
    }


    public static float captionPadding(float kc, float dp, float overlayFactor) {
        return overlayFactor <= 1f ? 8 * dp : 4 * kc;
    }


    static final int HUD_CENTER_HALF = 235, HUD_PIECE_W = 117;




    static final int HUD_CENTER_H = 64;

    static final int HUD_PIECE_TOP = 114;







    public static float[][] hudRects(boolean compact, int gameW, int gameH, float k, float x0, float y0) {
        int mid = gameW / 2, pieceTop = gameH - HUD_PIECE_TOP, centerTop = gameH - HUD_CENTER_H;
        int[][] g = compact
            ? new int[][]{{mid - HUD_CENTER_HALF - HUD_PIECE_W, pieceTop, mid - HUD_CENTER_HALF, gameH},
                {mid - HUD_CENTER_HALF, centerTop, mid + HUD_CENTER_HALF, gameH},
                {mid + HUD_CENTER_HALF, pieceTop, mid + HUD_CENTER_HALF + HUD_PIECE_W, gameH}}
            : new int[][]{{0, pieceTop, HUD_PIECE_W, gameH}, {HUD_PIECE_W, centerTop, gameW - HUD_PIECE_W, gameH},
                {gameW - HUD_PIECE_W, pieceTop, gameW, gameH}};
        float[][] out = new float[g.length][];
        for (int i = 0; i < g.length; i++) {
            out[i] = new float[]{x0 + g[i][0] * k, y0 + g[i][1] * k, x0 + g[i][2] * k, y0 + g[i][3] * k};
        }
        return out;
    }






    public static float liftClear(float cx, float cy, float r, float below, float[][] rects, float margin) {
        float lift = 0;
        for (float[] rect : rects) {
            float dx = cx < rect[0] ? rect[0] - cx : cx > rect[2] ? cx - rect[2] : 0;
            if (dx < r) lift = Math.max(lift, cy + (float) Math.sqrt(r * r - dx * dx) + margin - rect[1]);
            if (below > 0 && cx + r > rect[0] && cx - r < rect[2]) lift = Math.max(lift, cy + r + below + margin - rect[1]);
        }
        return Math.max(0, lift);
    }


    public static float tabInside(float cx, float w, float viewWidth) {
        return Math.max(w / 2, Math.min(viewWidth - w / 2, cx));
    }





    public static float launcherUnit(float u, float density, int smallestWidthDp) {
        if (smallestWidthDp < LARGE_FROM_DP || density <= 0) return u;
        return Math.min(u, LAUNCHER_MAX_UNIT_DP * density);
    }
}
