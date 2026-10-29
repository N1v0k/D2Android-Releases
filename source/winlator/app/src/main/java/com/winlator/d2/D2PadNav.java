package com.winlator.d2;

import java.util.List;





final class D2PadNav {
    private D2PadNav() {}





    static String nextGroup(List<String> groups, String current, int direction) {
        if (groups.isEmpty()) return null;
        int at = current == null ? -1 : groups.indexOf(current);
        if (at < 0) return direction > 0 ? groups.get(0) : groups.get(groups.size() - 1);
        int n = groups.size();
        return groups.get(((at + direction) % n + n) % n);
    }


    static final int GLYPH_A = 1, GLYPH_B = 2, GLYPH_X = 3, GLYPH_Y = 4, GLYPH_LB = 5, GLYPH_RB = 6, GLYPH_START = 7,
        GLYPH_LEFT_RIGHT = 8;

    static final int WORD_STEP = 1, WORD_CHOOSE = 2, WORD_GROUP = 3, WORD_BACK = 4, WORD_PLAY = 5;





    static int[] settingsHints(boolean stepper, boolean focus, boolean play) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        if (stepper) add(out, GLYPH_LEFT_RIGHT, WORD_STEP);
        if (focus) add(out, GLYPH_A, WORD_CHOOSE);
        add(out, GLYPH_LB, 0);
        add(out, GLYPH_RB, WORD_GROUP);
        add(out, GLYPH_B, WORD_BACK);
        if (play) add(out, GLYPH_START, WORD_PLAY);
        int[] a = new int[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        return a;
    }

    private static void add(List<Integer> out, int glyph, int word) {
        out.add(glyph);
        out.add(word);
    }
}
