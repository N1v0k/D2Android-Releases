package com.winlator.d2;









final class D2StartPad {
    private D2StartPad() {}





    static final int NOTHING = 0, CARD = 1, CARD_LOCKED = 2, OTHER = 3, CARD_GRADES = 4;

    static final int DELETED = 5, MENU = 6;

    static final int IGNORE = 0, SWALLOW = 1, CARD_MENU = 2, SETTINGS = 3, CLOSE_MENU = 4;

    static final int GLYPH_A = 1, GLYPH_Y = 2, GLYPH_START = 3, GLYPH_B = 4, GLYPH_AA = 5;
    static final int TEXT_PLAY = 1, TEXT_MENU = 2, TEXT_OPEN = 3, TEXT_SETTINGS = 4, TEXT_WHY = 5, TEXT_CHOOSE = 6,
        TEXT_BACK = 7, TEXT_RESTORE = 8, TEXT_PURGE = 9;


    static int onPad(int action, int focus) {
        if (focus == MENU) {
            return action == D2PadKeys.B ? CLOSE_MENU : action == D2PadKeys.A ? IGNORE : SWALLOW;
        }
        switch (action) {
            case D2PadKeys.B: return SWALLOW;
            case D2PadKeys.START: return SETTINGS;

            case D2PadKeys.Y: return focus == CARD || focus == CARD_GRADES || focus == CARD_LOCKED || focus == DELETED ? CARD_MENU : SWALLOW;
            default: return IGNORE;
        }
    }


    static int[] hints(int focus) {
        switch (focus) {
            case CARD_GRADES:
            case CARD: return new int[]{GLYPH_A, TEXT_PLAY, GLYPH_Y, TEXT_MENU, GLYPH_START, TEXT_SETTINGS};
            case CARD_LOCKED: return new int[]{GLYPH_A, TEXT_WHY, GLYPH_Y, TEXT_MENU, GLYPH_START, TEXT_SETTINGS};
            case DELETED: return new int[]{GLYPH_AA, TEXT_RESTORE, GLYPH_Y, TEXT_PURGE};
            case MENU: return new int[]{GLYPH_A, TEXT_CHOOSE, GLYPH_B, TEXT_BACK};
            case OTHER: return new int[]{GLYPH_A, TEXT_OPEN, GLYPH_START, TEXT_SETTINGS};
            default: return new int[]{GLYPH_START, TEXT_SETTINGS};
        }
    }
}
