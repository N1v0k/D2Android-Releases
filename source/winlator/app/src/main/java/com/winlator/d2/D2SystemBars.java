package com.winlator.d2;
















final class D2SystemBars {

    static final long LATE_MS = 400, LATER_MS = 1200;

    static final long[] SCREEN_ON_MS = {300, 1000, 2500};

    static final long INSETS_MS = 3000;

    static final long TOGGLE_MS = 150;




    static final int MAX_TOGGLES = 2;

    private D2SystemBars() {}


    static long[] hideAfterFocus(boolean hasFocus, boolean gameWindow) {
        if (!hasFocus || !gameWindow) return new long[0];
        return new long[] {0, LATE_MS, LATER_MS};
    }





    static boolean toggleFirst(boolean statusBarVisible, boolean focused, int toggles) {
        return toggleFirst(statusBarVisible, focused, toggles, false);
    }






    static boolean toggleFirst(boolean statusBarVisible, boolean focused, int toggles, boolean afterScreenOn) {
        return (statusBarVisible || afterScreenOn) && focused && toggles < MAX_TOGGLES;
    }
}
