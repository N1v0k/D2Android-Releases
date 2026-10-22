package com.winlator.d2;

public final class D2DamageStyle {
    public static final int OFF = 0;
    public static final int SIMPLE = 1;
    public static final int ELEMENT = 2;
    public static final int DEFAULT = ELEMENT;

    private D2DamageStyle() {
    }

    public static int of(boolean hasStyle, int style, boolean hasOld, boolean oldOn) {
        if (hasStyle) return style >= OFF && style <= ELEMENT ? style : DEFAULT;
        if (hasOld) return oldOn ? ELEMENT : OFF;
        return DEFAULT;
    }

    public static boolean numbers(int style) {
        return style == SIMPLE || style == ELEMENT;
    }

    public static boolean byElement(int style) {
        return style == ELEMENT;
    }
}
