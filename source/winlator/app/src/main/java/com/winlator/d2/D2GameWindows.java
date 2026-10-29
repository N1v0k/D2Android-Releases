package com.winlator.d2;













final class D2GameWindows {

    static final int BACK_APP = 0, BACK_ESC = 1, BACK_MAP = 2;


    static final long UNDER_LIFE = 1L << D2ModState.UI_CHARACTER | 1L << D2ModState.UI_CUBE | 1L << D2ModState.UI_STASH;

    static final long UNDER_MANA = 1L << D2ModState.UI_INVENTORY;

    private D2GameWindows() {}






    static int back(long panelFlags, boolean npcTalking, boolean automap, boolean paused) {
        if (paused) return BACK_APP;
        if ((panelFlags & ~(1L << D2ModState.UI_ESCMENU)) != 0 || npcTalking) return BACK_ESC;
        return automap ? BACK_MAP : BACK_APP;
    }


    static boolean lifeNumbers(long panelFlags, boolean recipeBook) {
        return (panelFlags & UNDER_LIFE) == 0 && !recipeBook;
    }


    static boolean manaNumbers(long panelFlags) {
        return (panelFlags & UNDER_MANA) == 0;
    }
}
