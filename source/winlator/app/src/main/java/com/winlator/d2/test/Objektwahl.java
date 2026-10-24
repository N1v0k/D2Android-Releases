package com.winlator.d2.test;










public final class Objektwahl {
    private Objektwahl() {}


    public static final int ART_BEHAELTER = 1, ART_SCHREIN = 2, ART_BRUNNEN = 4;


    public static final int AUFFUELLEN = 1, LEBEN = 2, MANA = 3, TAUSCH_LEBEN = 4, TAUSCH_MANA = 5, RUESTUNG = 6, KAMPF = 7,
        WIDERSTAND_FEUER = 8, WIDERSTAND_KAELTE = 9, WIDERSTAND_BLITZ = 10, WIDERSTAND_GIFT = 11, FERTIGKEIT = 12,
        MANA_AUFLADUNG = 13, AUSDAUER = 14, ERFAHRUNG = 15, ENIRHS = 16, PORTAL = 17, EDELSTEIN = 18, FEUER = 19,
        MONSTER = 20, EXPLOSION = 21, GIFT = 22;



    static final int[] BEHAELTER_JA = {1, 3, 4, 5, 14, 19, 20, 51};





    public static final boolean FALLEN_BELEGT = false;


    public static final int SCHREIN_PCT = 80;

    public static final int BRUNNEN_PCT = 70;

    public static final int FALLE_LEBEN_PCT = 70;


    public static final class Lage {
        public int lebenPct = 100, manaPct = 100;
        public boolean vergiftet, hardcore, schluessel, assassine;

        public boolean feindNah;

        public int[] schreinZustaende = new int[0];
    }





    public static String grund(int art, int operate, int[] details, Lage l) {
        if (details == null || details.length < 2) return "ohne Typ-12-Zeile";
        if (art == ART_SCHREIN) return schrein(details[1], l);
        if (art == ART_BRUNNEN) {
            if (l.lebenPct < BRUNNEN_PCT || l.manaPct < BRUNNEN_PCT || l.vergiftet) return null;
            return "Brunnen nicht nötig";
        }
        if (art != ART_BEHAELTER) return "keine Truhe, kein Schrein, kein Brunnen";
        if (!enthalten(BEHAELTER_JA, operate)) return "Behälter " + operate + " gesperrt";
        if (l.hardcore) return "Hardcore: keine Behälter";
        int interact = details[1];
        if ((interact & 0x80) != 0 && !l.schluessel && !l.assassine) return "verschlossen, kein Schlüssel";
        boolean falle = !FALLEN_BELEGT || (interact & 0x7F) != 0;
        if (falle && l.lebenPct < FALLE_LEBEN_PCT) return "vielleicht Falle, Leben unter " + FALLE_LEBEN_PCT + " %";
        if (falle && l.feindNah) return "vielleicht Falle, Feind in der Nähe";
        return null;
    }


    static String schrein(int zeile, Lage l) {
        switch (zeile) {
            case AUFFUELLEN:
                return l.lebenPct < SCHREIN_PCT || l.manaPct < SCHREIN_PCT ? null : "Auffüllen nicht nötig";
            case LEBEN:
                return l.lebenPct < SCHREIN_PCT ? null : "Leben voll";
            case MANA:
                return l.manaPct < SCHREIN_PCT ? null : "Mana voll";
            case EDELSTEIN:
                return null;
            default:
                break;
        }
        int rang = rang(zeile);
        if (rang == 0) return "Schrein " + zeile + " gesperrt";
        for (int zustand : l.schreinZustaende) {
            if (rang(zeileVonZustand(zustand)) > rang) return "besserer Schrein wirkt (" + zustand + ")";
        }
        return null;
    }





    static int rang(int zeile) {
        switch (zeile) {
            case ERFAHRUNG: return 6;
            case FERTIGKEIT: case KAMPF: return 5;
            case RUESTUNG: return 4;
            case MANA_AUFLADUNG: return 3;
            case WIDERSTAND_FEUER: case WIDERSTAND_KAELTE: case WIDERSTAND_BLITZ: case WIDERSTAND_GIFT: return 2;
            case AUSDAUER: return 1;
            default: return 0;
        }
    }


    static int zeileVonZustand(int zustand) {
        switch (zustand) {
            case 128: return RUESTUNG;
            case 129: return KAMPF;
            case 130: return WIDERSTAND_BLITZ;
            case 131: return WIDERSTAND_FEUER;
            case 132: return WIDERSTAND_KAELTE;
            case 133: return WIDERSTAND_GIFT;
            case 134: return FERTIGKEIT;
            case 135: return MANA_AUFLADUNG;
            case 136: return AUSDAUER;
            case 137: return ERFAHRUNG;
            default: return 0;
        }
    }



    static final int[] NEHMEN_NIE = {7, 68, 30, 33, 48};









    public static boolean nehmenNie(int art, int operate, int[] details) {
        if (art == 6 || art == 7) return false;
        if (enthalten(NEHMEN_NIE, operate)) return true;
        if (art != ART_SCHREIN) return false;
        if (details == null || details.length < 2) return true;
        int zeile = details[1];
        return rang(zeile) == 0 && zeile != AUFFUELLEN && zeile != LEBEN && zeile != MANA && zeile != EDELSTEIN;
    }

    private static boolean enthalten(int[] a, int v) {
        for (int x : a) if (x == v) return true;
        return false;
    }
}
