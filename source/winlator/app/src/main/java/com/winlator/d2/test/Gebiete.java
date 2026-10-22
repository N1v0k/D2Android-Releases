package com.winlator.d2.test;

public final class Gebiete {
    public static final int LAGER = 1, BLUTMOOR = 2, KALTE_EBENE = 3, STEINFELD = 4, DUNKLER_WALD = 5, SCHWARZES_MOOR = 6,
        HOCHLAND = 7, HOEHLE_DES_BOESEN = 8, UNTERIRDISCHER_GANG_1 = 10, KLOSTERTOR = 26, AEUSSERES_KLOSTER = 27, KASERNE = 28,
        GEFAENGNIS_1 = 29, GEFAENGNIS_2 = 30, GEFAENGNIS_3 = 31, INNERER_KREUZGANG = 32, KATHEDRALE = 33,
        KATAKOMBEN_1 = 34, KATAKOMBEN_2 = 35, KATAKOMBEN_3 = 36, KATAKOMBEN_4 = 37;

    public static final int[] KURS = {LAGER, BLUTMOOR, KALTE_EBENE, STEINFELD, UNTERIRDISCHER_GANG_1, DUNKLER_WALD, SCHWARZES_MOOR,
        HOCHLAND, KLOSTERTOR, AEUSSERES_KLOSTER, KASERNE, GEFAENGNIS_1, GEFAENGNIS_2, GEFAENGNIS_3, INNERER_KREUZGANG, KATHEDRALE,
        KATAKOMBEN_1, KATAKOMBEN_2, KATAKOMBEN_3, KATAKOMBEN_4};

    private Gebiete() {}

    public static int stufe(int gebiet) {
        for (int i = 0; i < KURS.length; i++) if (KURS[i] == gebiet) return i;
        return -1;
    }

    public static int naechstes(int gebiet) {
        int i = stufe(gebiet);
        return i < 0 || i + 1 >= KURS.length ? 0 : KURS[i + 1];
    }

    public static int voriges(int gebiet) {
        int i = stufe(gebiet);
        return i <= 0 ? 0 : KURS[i - 1];
    }

    private static final int[][] NEBEN = {
        {8, 2}, {9, 3}, {13, 9}, {17, 3}, {18, 17}, {19, 17}, {11, 6}, {15, 11}, {12, 7}, {16, 12}, {14, 10},
        {20, 6}, {21, 20}, {22, 21}, {23, 22}, {24, 23}, {25, 24}};

    public static int eingangVon(int gebiet) {
        for (int[] n : NEBEN) if (n[0] == gebiet) return n[1];
        return 0;
    }

    public static int naechstesZu(int von, int ziel) {
        if (von <= 0 || ziel <= 0 || von == ziel) return 0;
        for (int g = ziel, unten = 0; g != 0; unten = g, g = eingangVon(g)) if (g == von) return unten;
        int oben = eingangVon(von);
        if (oben != 0) return oben;
        int k = ziel;
        while (eingangVon(k) != 0) k = eingangVon(k);
        if (stufe(k) < 0 || stufe(von) < 0) return 0;
        return stufe(k) > stufe(von) ? naechstes(von) : voriges(von);
    }

    public static boolean weiter(int von, int ziel) {
        return stufe(von) >= 0 && stufe(ziel) > stufe(von);
    }
}
