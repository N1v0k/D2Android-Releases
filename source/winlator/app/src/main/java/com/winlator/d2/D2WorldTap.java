package com.winlator.d2;

public final class D2WorldTap {
    public static final int UI_MINIPANEL = 0x15, UI_BELT = 0x1F;
    static final int MINI_HALF = 80, MINI_HIGH = 26, BELT_WIDE = 180, BELT_HIGH = 100;

    static final int EDGE_LEFT = 20, EDGE_RIGHT = 105, EDGE_TOP = 170, EDGE_BOTTOM = 85;
    static final int CENTER_HALF = 235;

    static final int MERC_LEFT = 10, MERC_TOP = 10, MERC_RIGHT = 66, MERC_BOTTOM = 68;

    private D2WorldTap() {
    }

    public static final class Conditions {
        public boolean tapMoves;
        public boolean ownFinger, playtester;
        public boolean stateFresh;
        public boolean controlsShown;
        public long panels;
        public boolean npcTalking;
        public boolean cursorTaken;
        public boolean heroDead, paused;
        public boolean onGameControl;
    }

    public static boolean toGame(Conditions c) {
        if (c.tapMoves || c.ownFinger || c.playtester) return true;
        if (!c.stateFresh || !c.controlsShown) return true;
        if (c.panels != 0) return true;
        if (c.npcTalking || c.cursorTaken || c.heroDead || c.paused) return true;
        return c.onGameControl;
    }

    public static boolean onGameControl(float x, float y, int gameWidth, int gameHeight, int strip, boolean edgeButton,
                                        boolean miniPanel, boolean beltRows) {
        return onGameControl(x, y, gameWidth, gameHeight, strip, edgeButton, miniPanel, beltRows, false);
    }

    public static boolean onGameControl(float x, float y, int gameWidth, int gameHeight, int strip, boolean edgeButton,
                                        boolean miniPanel, boolean beltRows, boolean ownStrip) {
        if (gameWidth <= 0 || gameHeight <= 0) return true;
        float middle = gameWidth / 2f;
        if (y >= gameHeight - strip) return !ownStrip || Math.abs(x - middle) <= CENTER_HALF;
        if (miniPanel && Math.abs(x - middle) <= MINI_HALF && y >= gameHeight - strip - MINI_HIGH) return true;
        if (beltRows && x >= middle && x <= middle + BELT_WIDE && y >= gameHeight - strip - BELT_HIGH) return true;
        return edgeButton && x >= EDGE_LEFT && x <= EDGE_RIGHT && y >= gameHeight - EDGE_TOP && y <= gameHeight - EDGE_BOTTOM;
    }

    public static final class Portrait {
        public boolean stateFresh;
        public boolean panelOpen;
        public boolean singlePlayer;
        public int mercLife = -1;
        public boolean cursorTaken;
        public boolean cursorKnown = true;
        public boolean npcTalking, heroDead;
        public boolean ownFinger, playtester;
    }

    public static boolean mercPortraitTap(float x, float y, Portrait c) {
        return mercPortrait(x, y, c) == PORTRAIT_KEY;
    }

    public static final int PORTRAIT_NONE = 0, PORTRAIT_KEY = 1, PORTRAIT_GAME = 2;

    public static int mercPortrait(float x, float y, Portrait c) {
        if (!c.stateFresh || c.panelOpen || !c.singlePlayer || c.mercLife < 0 || c.ownFinger || c.playtester) return PORTRAIT_NONE;
        if (x < MERC_LEFT || x > MERC_RIGHT || y < MERC_TOP || y > MERC_BOTTOM) return PORTRAIT_NONE;
        if (!c.cursorKnown) return PORTRAIT_NONE;
        if (c.cursorTaken) return PORTRAIT_GAME;
        return c.npcTalking || c.heroDead ? PORTRAIT_NONE : PORTRAIT_KEY;
    }

    public static final long MERC_KEY_PAUSE_MS = 400;

    public static boolean mercKeyAllowed(long now, long lastAt) {
        return lastAt == 0 || now - lastAt >= MERC_KEY_PAUSE_MS;
    }
}
