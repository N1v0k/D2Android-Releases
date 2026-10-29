package com.winlator.d2;

import com.winlator.d2.test.Zustand;







final class D2ItemMoves {

    static final int CTX_PLAIN = 0, CTX_SHOP = 1, CTX_TRADE = 2, CTX_STASH = 3;

    static final int PLACE_INVENTORY = 0, PLACE_STASH = 4;

    static final int CMD_MOVE = 34;

    private D2ItemMoves() {}


    static int context(boolean shop, boolean trade, boolean stash) {
        return shop ? CTX_SHOP : trade ? CTX_TRADE : stash ? CTX_STASH : CTX_PLAIN;
    }






    static boolean waits(int ctx, int category, int place, int cellX, int cellY, boolean sellPossible, boolean movePossible) {
        if (cellX < 0 || cellY < 0 || category < 0) return false;
        switch (ctx) {
            case CTX_PLAIN: return D2DoubleTap.potion(category) && place == PLACE_INVENTORY;
            case CTX_SHOP: return sellPossible && place == PLACE_INVENTORY && category != D2ModItems.CAT_QUEST;
            case CTX_STASH: return movePossible && (place == PLACE_INVENTORY || place == PLACE_STASH) && category != D2ModItems.CAT_QUEST;
            default: return false;
        }
    }





    static int decide(int ctx, boolean second, float distanceDp, int firstPlace, int firstX, int firstY, int category, int place,
                      int cellX, int cellY, boolean sellPossible, boolean movePossible) {
        boolean waits = waits(ctx, category, place, cellX, cellY, sellPossible, movePossible);
        if (second && waits && distanceDp <= D2DoubleTap.SAME_DP && place == firstPlace && cellX == firstX && cellY == firstY) {
            return ctx == CTX_SHOP ? D2DoubleTap.SELL : ctx == CTX_STASH ? D2DoubleTap.MOVE : D2DoubleTap.SHIFT_CLICK;
        }
        return waits ? D2DoubleTap.WAIT : D2DoubleTap.CLICK;
    }


    static byte[] movePayload(int place, int x, int y, int request) {
        return new byte[]{(byte) place, (byte) x, (byte) y, 0, (byte) request, (byte) (request >> 8), (byte) (request >> 16),
            (byte) (request >> 24)};
    }


    static Zustand.Figur.Teil itemAt(Zustand.Figur figur, int place, int x, int y) {
        if (figur == null) return null;
        for (Zustand.Figur.Teil t : figur.gegenstaende) if (t.platz == place && t.x == x && t.y == y) return t;
        return null;
    }





    static boolean fits(Zustand.Figur figur, int place, int gridW, int gridH, int w, int h) {

        if (figur == null || gridW <= 0 || gridH <= 0 || (figur.voll & Zustand.Figur.VOLL_GEGENSTAENDE) != 0) return true;
        boolean[] used = new boolean[gridW * gridH];
        for (Zustand.Figur.Teil t : figur.gegenstaende) {
            if (t.platz != place) continue;
            for (int y = t.y; y < t.y + Math.max(1, t.hoehe) && y < gridH; y++)
                for (int x = t.x; x < t.x + Math.max(1, t.breite) && x < gridW; x++) if (x >= 0 && y >= 0) used[y * gridW + x] = true;
        }
        for (int y = 0; y + h <= gridH; y++) {
            for (int x = 0; x + w <= gridW; x++) {
                boolean free = true;
                for (int j = 0; j < h && free; j++) for (int i = 0; i < w && free; i++) if (used[(y + j) * gridW + x + i]) free = false;
                if (free) return true;
            }
        }
        return false;
    }
}
