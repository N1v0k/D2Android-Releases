package com.winlator.d2;






public final class D2AssignBar {
    public static final int NONE = -1, CLEAR = 0;

    private final float[] bar = new float[4], title = new float[2], clear = new float[4];
    private boolean laid, hasClear;






    public float[] layout(float cx, float top, float pad, float row, float gap, float titleWidth, float clearWidth) {
        laid = true;
        hasClear = clearWidth > 0;
        float inner = Math.max(titleWidth, hasClear ? clearWidth : 0);
        bar[0] = cx - inner / 2 - pad;
        bar[1] = top;
        bar[2] = cx + inner / 2 + pad;
        bar[3] = top + pad + row + (hasClear ? gap + row : 0) + pad;
        title[0] = cx;
        title[1] = top + pad + row / 2;
        float rowTop = top + pad + row + gap;
        clear[0] = cx - clearWidth / 2;
        clear[1] = rowTop;
        clear[2] = cx + clearWidth / 2;
        clear[3] = rowTop + row;
        return bar;
    }

    public boolean hasClear() {
        return hasClear;
    }


    public float[] clearButton() {
        return clear;
    }

    public float[] titleCenter() {
        return title;
    }


    public boolean inside(float x, float y) {
        return laid && x >= bar[0] && x <= bar[2] && y >= bar[1] && y <= bar[3];
    }


    public int hit(float x, float y) {
        return hasClear && x >= clear[0] && x <= clear[2] && y >= clear[1] && y <= clear[3] ? CLEAR : NONE;
    }
}
