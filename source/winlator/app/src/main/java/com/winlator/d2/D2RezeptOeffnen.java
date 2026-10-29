package com.winlator.d2;

import java.util.List;






final class D2RezeptOeffnen {
    private D2RezeptOeffnen() {}


    static final String MENU_ID = "recipes";

    static final int CMD_CUBE_OPEN = 37;

    static final long OPEN_WAIT_MS = 1500;


    static final int BOOK = 0;

    static final int SEND = 1;

    static final int READ_STASH = 2;

    static final int READ_NONE = 3;





    static int plan(List<D2RezeptBestand.Stueck> items, boolean cubeOpen, boolean stashOpen, boolean paused) {
        if (cubeOpen) return BOOK;


        if (!paused) return SEND;
        return where(items) == D2RezeptBestand.PLACE_STASH && !stashOpen ? READ_STASH : READ_NONE;
    }


    static int where(List<D2RezeptBestand.Stueck> items) {
        if (items == null) return -1;
        int found = -1;
        for (D2RezeptBestand.Stueck s : items) {
            if (!"box".equals(s.code)) continue;
            if (s.place == D2RezeptBestand.PLACE_INVENTORY) return s.place;
            if (s.place == D2RezeptBestand.PLACE_STASH) found = s.place;
        }
        return found;
    }


    static byte[] payload() {
        return new byte[4];
    }
}
