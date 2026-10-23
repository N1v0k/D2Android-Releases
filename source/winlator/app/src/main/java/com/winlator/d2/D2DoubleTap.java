package com.winlator.d2;











final class D2DoubleTap {

    static final float SAME_DP = 28;

    static final int CLICK = 0, WAIT = 1, SHIFT_CLICK = 2;

    private D2DoubleTap() {}


    static boolean potion(int category) {
        return category == D2ModItems.CAT_HEAL || category == D2ModItems.CAT_MANA || category == D2ModItems.CAT_REJUV
            || category == D2ModItems.CAT_POTION_OTHER;
    }


    static boolean waits(int category, int place, int cellX, int cellY, boolean shopOpen) {
        return potion(category) && place == 0 && cellX >= 0 && cellY >= 0 && !shopOpen;
    }








    static int decide(boolean second, float distanceDp, int firstX, int firstY, int category, int place, int cellX, int cellY,
                      boolean shopOpen) {
        boolean waits = waits(category, place, cellX, cellY, shopOpen);
        if (second && waits && distanceDp <= SAME_DP && cellX == firstX && cellY == firstY) return SHIFT_CLICK;
        return waits ? WAIT : CLICK;
    }
}
