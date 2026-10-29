package com.winlator.d2.test;

import java.util.HashSet;
import java.util.Set;










public final class Weiterziehen {
    public enum Ergebnis {

        LAEUFT,

        ANGEKOMMEN,

        ENDE
    }






    static final int[][] FOLGE = {{3, 4, 5, 6, 27, 29, 32, 35}, {48, 42, 57, 43, 44, 52, 74, 46},
        {76, 77, 78, 79, 80, 81, 83, 101}, {106, 107}, {111, 112, 113, 115, 123, 117, 118, 129}};


    public enum EndeArt { KEIN_GEBIET, WEG }

    public EndeArt endeArt;
    public final Wegpunkte wegpunkte = new Wegpunkte();
    public int reisen, fehlversuche;

    public String meldung = "", grund = "";

    private Auftrag auftrag;
    private int von = -1, ziel = -1;
    private final Set<Integer> versucht = new HashSet<>();
    private int abbruecheVorher;


    public static int naechstes(int gebiet, Set<Integer> ausser) {
        int akt = Wegpunkte.akt(gebiet);
        if (akt < 0) return -1;
        int[] f = FOLGE[akt];


        int start = -1;
        for (int i = 0; i < f.length; i++) if (f[i] == gebiet) start = i + 1;
        if (start < 0) {
            start = 0;
            int best = Integer.MAX_VALUE;
            for (int i = 0; i < f.length; i++) {
                if (f[i] > gebiet && f[i] < best) {
                    best = f[i];
                    start = i;
                }
            }
        }
        for (int k = 0; k < f.length; k++) {
            int g = f[(start + k) % f.length];
            if (g != gebiet && (ausser == null || !ausser.contains(g))) return g;
        }
        return -1;
    }


    public void starten(int von) {
        this.von = von;
        versucht.clear();
        versucht.add(von);
        wegpunkte.beenden();
        auftrag = null;
        ziel = -1;
        grund = "";
        ergebnis = Ergebnis.LAEUFT;
        meldung = "weiter nach Gebiet " + von + " leer";
    }


    public int ziel() {
        return ziel;
    }





    public boolean fensterErlaubt() {
        return ergebnis == Ergebnis.LAEUFT;
    }

    public Ergebnis ergebnis = Ergebnis.LAEUFT;





    public Gehirn.Absicht schritt(Zustand z, Zustand.Ziele ziele, Zustand.Figur figur, Gehirn g, long now) {
        if (ergebnis != Ergebnis.LAEUFT) return new Gehirn.Absicht();
        if (auftrag == null) {
            ziel = naechstes(von, versucht);
            if (ziel < 0) return ende(EndeArt.KEIN_GEBIET, "kein weiteres Gebiet mit Wegpunkt in Akt " + (Wegpunkte.akt(von) + 1), g);
            auftrag = Auftrag.lesen("wegpunkt " + ziel);
            abbruecheVorher = wegpunkte.abbrueche;
            meldung = "per Wegpunkt nach Gebiet " + ziel;
        }
        Gehirn.Absicht a = auftrag.schritt(z, ziele, figur, g, wegpunkte, now);
        if (auftrag.ergebnis == null) {

            meldung = "nach Gebiet " + ziel + ": " + auftrag.unterwegs.replaceAll(",? noch \\d+ Subtiles", "");
            return a;
        }
        if (auftrag.ergebnis == Auftrag.Ergebnis.ANGEKOMMEN && z.gebiet == ziel) {
            reisen++;
            wegpunkte.beenden();
            g.vorgeben(Double.NaN, 0, "", false);
            ergebnis = Ergebnis.ANGEKOMMEN;
            meldung = "per Wegpunkt in Gebiet " + ziel;
            return new Gehirn.Absicht();
        }
        if (wegpunkte.abbrueche > abbruecheVorher && !z.tot()) {

            fehlversuche++;
            versucht.add(ziel);
            meldung = "Gebiet " + ziel + " ging nicht (" + auftrag.grund + "), das nächste";
            auftrag = null;
            return new Gehirn.Absicht();
        }
        return ende(EndeArt.WEG, "Weg zum Wegpunkt: " + auftrag.ergebnis.text + (auftrag.grund.isEmpty() ? "" : " (" + auftrag.grund + ")"), g);
    }

    private Gehirn.Absicht ende(EndeArt art, String warum, Gehirn g) {
        endeArt = art;
        grund = warum;
        meldung = "Weiterziehen beendet: " + warum;
        ergebnis = Ergebnis.ENDE;
        g.vorgeben(Double.NaN, 0, "", false);
        return new Gehirn.Absicht();
    }
}
