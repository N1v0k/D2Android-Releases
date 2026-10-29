package com.winlator.d2;

import com.winlator.d2.test.Zustand;








final class D2ShopGestures {

    static final int MAX = 99;

    private D2ShopGestures() {}








    static boolean endless(int category) {
        switch (category) {
            case D2ModItems.CAT_HEAL:
            case D2ModItems.CAT_MANA:
            case D2ModItems.CAT_REJUV:
            case D2ModItems.CAT_POTION_OTHER:
            case D2ModItems.CAT_TP_SCROLL:
            case D2ModItems.CAT_ID_SCROLL:
            case D2ModItems.CAT_AMMO:
            case D2ModItems.CAT_KEY:
                return true;
            default:
                return false;
        }
    }


    static final int PLACE_SHOP = 5;






    static Zustand.Figur.Teil wareAt(Zustand.Figur figur, int hoverPlace, int hoverCategory, int cellX, int cellY) {
        if (figur == null || hoverPlace != PLACE_SHOP || cellX < 0 || cellY < 0) return null;
        for (Zustand.Figur.Teil t : figur.laden) {
            if (t.x == cellX && t.y == cellY && t.kategorie == hoverCategory) return t;
        }
        return null;
    }





    static int capacity(Zustand.Figur.Teil ware, int freeCells, int tomeCharges, int tomeLimit) {

        if (stacks(ware.kategorie)) return MAX;
        int cells = Math.max(1, ware.breite * ware.hoehe);
        int n = Math.max(0, freeCells) / cells;
        boolean scroll = ware.kategorie == D2ModItems.CAT_TP_SCROLL || ware.kategorie == D2ModItems.CAT_ID_SCROLL;
        if (scroll && tomeCharges >= 0) n += Math.max(0, tomeLimit - tomeCharges);
        return Math.max(1, Math.min(MAX, n));
    }





    static boolean stacks(int category) {
        return category == D2ModItems.CAT_KEY;
    }


    static int start(Zustand.Figur.Teil ware, int capacity) {
        boolean potion = ware.kategorie == D2ModItems.CAT_HEAL || ware.kategorie == D2ModItems.CAT_MANA
            || ware.kategorie == D2ModItems.CAT_POTION_OTHER;
        return Math.min(capacity, potion ? 4 : 1);
    }
}
