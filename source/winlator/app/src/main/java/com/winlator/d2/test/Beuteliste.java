package com.winlator.d2.test;











public final class Beuteliste {





    public enum Antwort { LIEGEN, VERKAUFEN, BEHALTEN, TRUHE }


    static final int K_RUNE = 10, K_EDELSTEIN = 11, K_JUWEL = 12, K_RING = 13, K_AMULETT = 14, K_ZAUBER = 15, K_WAFFE = 16,
        K_RUESTUNG = 17, K_QUEST = 20;

    static final int Q_MAGISCH = 4, Q_SET = 5, Q_SELTEN = 6, Q_EINZIGARTIG = 7, Q_HANDGEFERTIGT = 8;


    public static final int RUNEN = 0, EDELSTEINE = 1, JUWELEN = 2, ZAUBER = 3, SCHMUCK = 4, EINZIGARTIG = 5, SELTEN = 6,
        MAGISCH = 7, BASEN = 8, WEISS = 9, QUEST = 10, GRUPPEN = 11;
    public static final String[] NAMEN = {"Runen", "Edelsteine", "Juwelen", "Zauber", "Ringe/Amulette", "Einzigartig/Set",
        "Selten", "Magisch", "Basen mit Sockeln", "Weiß", "Quest"};






    private static final Antwort[] VORGABEN = {Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.BEHALTEN,
        Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.VERKAUFEN, Antwort.LIEGEN, Antwort.LIEGEN,
        Antwort.BEHALTEN};

    private static final Antwort[] VORGABEN_SPIELER = {Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.BEHALTEN,
        Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.BEHALTEN, Antwort.BEHALTEN,
        Antwort.BEHALTEN, Antwort.BEHALTEN};


    public static final Beuteliste VORGABE = new Beuteliste(VORGABEN, true);

    public static final Beuteliste SPIELER = new Beuteliste(VORGABEN_SPIELER, true);

    private final Antwort[] stufen;

    private final int[] abWertklasse = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    private final boolean fest;


    public Beuteliste() {
        this(VORGABEN, false);
    }

    private Beuteliste(Antwort[] vorgaben, boolean fest) {
        stufen = vorgaben.clone();
        this.fest = fest;
    }


    public static Beuteliste spieler() {
        return new Beuteliste(VORGABEN_SPIELER, false);
    }


    public static Antwort vorgabe(int gruppe) {
        return VORGABEN[gruppe];
    }

    public Antwort stufe(int gruppe) {
        return stufen[gruppe];
    }

    public int abWertklasse(int gruppe) {
        return abWertklasse[gruppe];
    }


    public void setzen(int gruppe, Antwort a) {
        if (fest) throw new UnsupportedOperationException("feste Vorgabe");
        stufen[gruppe] = a;
    }


    public void abWertklasse(int gruppe, int klasse) {
        if (fest) throw new UnsupportedOperationException("feste Vorgabe");
        abWertklasse[gruppe] = klasse;
    }


    public static int gruppe(int kategorie, int qualitaet, int sockel) {
        switch (kategorie) {
            case K_RUNE: return RUNEN;
            case K_EDELSTEIN: return EDELSTEINE;
            case K_JUWEL: return JUWELEN;
            case K_ZAUBER: return ZAUBER;
            case K_RING: case K_AMULETT: return SCHMUCK;
            case K_QUEST: return QUEST;
            case K_WAFFE: case K_RUESTUNG:
                if (qualitaet == Q_EINZIGARTIG || qualitaet == Q_SET) return EINZIGARTIG;
                if (qualitaet == Q_SELTEN || qualitaet == Q_HANDGEFERTIGT) return SELTEN;
                if (qualitaet == Q_MAGISCH) return MAGISCH;
                return sockel > 0 ? BASEN : WEISS;
            default: return -1;
        }
    }


    static boolean verkaeuflich(int gruppe) {
        return gruppe == SCHMUCK || gruppe == EINZIGARTIG || gruppe == SELTEN || gruppe == MAGISCH || gruppe == BASEN || gruppe == WEISS;
    }






    public Antwort antwort(int kategorie, int qualitaet, int sockel, int wertklasse) {
        int g = gruppe(kategorie, qualitaet, sockel);
        if (g < 0) return null;
        Antwort a = stufen[g];
        int ab = abWertklasse[g];
        if (ab < 0 || wertklasse < 0) return a;
        if (wertklasse >= ab) return a == Antwort.TRUHE ? a : Antwort.BEHALTEN;
        if (a == Antwort.LIEGEN) return a;
        return verkaeuflich(g) ? Antwort.VERKAUFEN : Antwort.LIEGEN;
    }

    public static final int BREITE = 10, HOEHE = 4;





    public static boolean passt(long belegt, int breite, int hoehe) {
        return passt(belegt, 0, HOEHE, breite, hoehe);
    }





    public static boolean passt(long belegt, long belegt2, int rasterHoehe, int breite, int hoehe) {
        if (breite <= 0 || hoehe <= 0) return true;
        for (int y = 0; y + hoehe <= rasterHoehe; y++) {
            for (int x = 0; x + breite <= BREITE; x++) {
                boolean frei = true;
                for (int dy = 0; dy < hoehe && frei; dy++) {
                    for (int dx = 0; dx < breite && frei; dx++) {
                        int i = (y + dy) * BREITE + x + dx;
                        frei = ((i < 64 ? belegt >> i : belegt2 >> (i - 64)) & 1) == 0;
                    }
                }
                if (frei) return true;
            }
        }
        return false;
    }
}
