package com.winlator.d2.test;

import java.util.HashSet;
import java.util.Set;









public final class Wegpunkte {
    public enum Schritt { AUS, OEFFNEN, FENSTER, REISE, SCHLIESSEN }


    static final int[] AKT1 = {1, 3, 4, 5, 6, 27, 29, 32, 35};






    static final int[][] AKTE = {AKT1, {40, 48, 42, 57, 43, 44, 52, 74, 46}, {75, 76, 77, 78, 79, 80, 81, 83, 101},
        {103, 106, 107}, {109, 111, 112, 113, 115, 123, 117, 118, 129}};

    static final String[][] NAMEN = {
        {"Rogue Encampment", "Cold Plains", "Stony Field", "Dark Wood", "Black Marsh", "Outer Cloister", "Jail Level 1",
            "Inner Cloister", "Catacombs Level 2"},
        {"Lut Gholein", "Sewers Level 2", "Dry Hills", "Halls of the Dead Level 2", "Far Oasis", "Lost City",
            "Palace Cellar Level 1", "Arcane Sanctuary", "Canyon of the Magi"},
        {"Kurast Docks", "Spider Forest", "Great Marsh", "Flayer Jungle", "Lower Kurast", "Kurast Bazaar", "Upper Kurast",
            "Travincal", "Durance of Hate Level 2"},
        {"Pandemonium Fortress", "City of the Damned", "River of Flame"},
        {"Harrogath", "Frigid Highlands", "Arreat Plateau", "Crystalline Passage", "Glacial Trail", "Halls of Pain",
            "Frozen Tundra", "The Ancients' Way", "Worldstone Keep Level 2"}};

    static final int REITER_X0 = 115, REITER_DX = 63, REITER_Y = 75;

    private static final long REITER_WARTEN_MS = 600;

    static final int ZEILE_X = 200, ZEILE_Y0 = 138, ZEILE_DY = 35;





    static final int WEGPUNKT = 3;
    static final double REICHWEITE = 0.55;
    private static final long TIPP_ABSTAND_MS = 2500, REISE_WARTEN_MS = 4000, SCHRITT_MAX_MS = 30_000;

    static final int UI_WAYPOINT = 0x14;

    static final int KURS_ABBRUECHE = 3;
    static final long KURS_WIEDER_MS = 3000;


    public final Set<Integer> aktiv = new HashSet<>();
    public int reisen, aktiviert, abbrueche;
    public String meldung = "", abbruch;





    public int kursZiel = -1, kursPhase;
    public String kursEnde;
    private int gesehenAbbrueche, kursEsc;
    private long kursWiederAb, kursEscAm;

    private Schritt schritt = Schritt.AUS;
    private int ziel = -1, startGebiet = -1, versuche;
    private long seit, letzterTipp, reiterAm;

    public Wegpunkte() {
        aktiv.add(1);
    }

    public boolean aktiv() {
        return schritt != Schritt.AUS;
    }

    public Schritt schritt() {
        return schritt;
    }


    public static boolean kursMoeglich(int n) {
        if (n == 1) return false;
        for (int g : AKT1) if (g == n) return true;
        return false;
    }


    public static int akt(int gebiet) {
        return gebiet <= 0 ? -1 : gebiet < 40 ? 0 : gebiet < 75 ? 1 : gebiet < 103 ? 2 : gebiet < 109 ? 3 : gebiet < 137 ? 4 : -1;
    }


    public static boolean hatWegpunkt(int n) {
        return zeile(n) >= 0;
    }


    public static int gebietZu(String text) {

        String t = text.trim().toLowerCase(java.util.Locale.ROOT).replace("'", "");
        try {
            int n = Integer.parseInt(t);
            return hatWegpunkt(n) ? n : -1;
        }
        catch (NumberFormatException ignored) {}
        int treffer = -1, anzahl = 0;
        for (int a = 0; a < AKTE.length; a++) {
            for (int i = 0; i < AKTE[a].length; i++) {
                String n = NAMEN[a][i].toLowerCase(java.util.Locale.ROOT).replace("'", "");
                if (n.equals(t)) return AKTE[a][i];
                if (!t.isEmpty() && n.startsWith(t)) {
                    treffer = AKTE[a][i];
                    anzahl++;
                }
            }
        }
        return anzahl == 1 ? treffer : -1;
    }


    public static String liste() {
        StringBuilder b = new StringBuilder();
        for (int a = 0; a < AKTE.length; a++) {
            b.append("Akt ").append(a + 1).append(':');
            for (int i = 0; i < AKTE[a].length; i++) b.append(i == 0 ? " " : ", ").append(AKTE[a][i]).append(' ').append(NAMEN[a][i]);
            b.append('\n');
        }
        return b.toString();
    }


    static boolean fensterOffen(Zustand z) {
        try {
            return (Long.parseUnsignedLong(z.fenster, 16) >>> UI_WAYPOINT & 1) != 0;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }





    public Gehirn.Absicht kurs(Zustand z, long now) {
        if (aktiv()) return denken(z, now);
        if (kursZiel < 0 || kursPhase >= 3 || kursEnde != null) return null;
        if (abbrueche > gesehenAbbrueche) {

            gesehenAbbrueche = abbrueche;
            kursPhase--;
            if (abbrueche >= KURS_ABBRUECHE) {
                kursEnde = abbrueche + " Abbrüche, zuletzt: " + abbruch;
                meldung = "Kurs abgebrochen: " + kursEnde;
                return null;
            }
            kursWiederAb = now + KURS_WIEDER_MS;
        }
        if (kursPhase == 2 && z.gebiet == kursZiel) {
            kursPhase = 3;
            meldung = "Kurs fertig: Gebiet " + kursZiel + " aktiviert, per Wegpunkt ins Lager und zurück";
            return null;
        }
        if (inReichweite(z) == null) return null;
        boolean fenster = z.panel || z.npc;
        if (now < kursWiederAb || fenster && kursEsc < 4) {
            Gehirn.Absicht a = new Gehirn.Absicht();
            a.grund = "Wegpunkt: warten auf den nächsten Versuch";
            if (fenster && now - kursEscAm > 1500) {

                kursEsc++;
                kursEscAm = now;
                a.tippen = "menu_esc";
                a.grund = "Wegpunkt: Fenster schließen vor dem nächsten Versuch";
            }
            return a;
        }
        kursEsc = 0;
        if (kursPhase == 0 && z.gebiet == kursZiel) {
            kursPhase = 1;
            starten(z, 1, now);
            return denken(z, now);
        }
        if (kursPhase == 1 && z.gebiet == 1) {
            kursPhase = 2;
            starten(z, kursZiel, now);
            return denken(z, now);
        }
        return null;
    }


    public static Zustand.Ding inReichweite(Zustand z) {
        Zustand.Ding best = null;
        double reach = (z.gh > 0 ? z.gh : 600) * REICHWEITE;
        for (Zustand.Ding d : z.objekte) {
            if (d.art != WEGPUNKT || z.bodenAbstand(d) > reach) continue;
            if (best == null || z.bodenAbstand(d) < z.bodenAbstand(best)) best = d;
        }
        return best;
    }


    public void starten(Zustand z, int ziel, long now) {
        this.ziel = ziel;
        startGebiet = z.gebiet;
        abbruch = null;
        weiter(Schritt.OEFFNEN, now, "Wegpunkt öffnen (Gebiet " + z.gebiet + " → " + ziel + ")");
    }

    public Gehirn.Absicht denken(Zustand z, long now) {
        Gehirn.Absicht a = new Gehirn.Absicht();
        a.grund = "Wegpunkt: " + schritt;
        if (schritt != Schritt.AUS && now - seit > SCHRITT_MAX_MS) return abbrechen(a, "Zeit um bei " + schritt, now);
        switch (schritt) {
            case OEFFNEN: {
                if (fensterOffen(z)) {
                    if (aktiv.add(z.gebiet)) aktiviert++;
                    return weiterIn(a, ziel == z.gebiet ? Schritt.SCHLIESSEN : Schritt.FENSTER, now,
                        "Fenster offen, Wegpunkt Gebiet " + z.gebiet + " aktiv");
                }
                if (now - letzterTipp > TIPP_ABSTAND_MS) {
                    if (z.panel) return abbrechen(a, "anderes Fenster offen (Fenster " + z.fenster + "), kein Wegpunktfenster", now);
                    if (versuche >= 3) return abbrechen(a, "Wegpunkt öffnet sich nicht", now);
                    versuche++;
                    letzterTipp = now;
                    a.tippen = "interact";
                    a.grund = "Wegpunkt: Nehmen (Versuch " + versuche + ")";
                }
                return a;
            }
            case FENSTER: {
                int zeile = zeile(ziel);
                if (zeile < 0) return abbrechen(a, "Gebiet " + ziel + " hat keinen Wegpunkt", now);

                if (!fensterOffen(z)) return abbrechen(a, "Wegpunktfenster zu vor dem Tipp auf die Zeile", now);




                int akt = akt(ziel);
                if (reiterAm == 0) {
                    reiterAm = now;
                    a.tippSpiel = fensterPunkt(REITER_X0 + akt * REITER_DX, REITER_Y, z);
                    a.grund = "Wegpunkt: Reiter Akt " + (akt + 1);
                    meldung = "Wegpunkt: Reiter Akt " + (akt + 1) + " angetippt";
                    return a;
                }
                if (reiterAm > 0 && now - reiterAm < REITER_WARTEN_MS) return a;
                versuche++;
                letzterTipp = now;
                a.tippSpiel = fensterPunkt(ZEILE_X, ZEILE_Y0 + zeile * ZEILE_DY, z);
                a.grund = "Wegpunkt: Zeile " + zeile + " (Gebiet " + ziel + ", Tipp " + versuche + ")";
                schritt = Schritt.REISE;
                meldung = "Wegpunkt: Zeile " + zeile + " angetippt" + (aktiv.contains(ziel) ? "" : " (in diesem Lauf nicht aktiviert)");
                return a;
            }
            case REISE: {
                if (z.gebiet == ziel) {
                    reisen++;
                    schritt = Schritt.AUS;
                    meldung = "Wegpunkt: Reise " + startGebiet + " → " + ziel + " fertig (" + reisen + ")";
                    return a;
                }
                if (z.gebiet > 0 && z.gebiet != startGebiet) return abbrechen(a, "Reise nach Gebiet " + z.gebiet + " statt " + ziel
                    + " (Akt " + (akt(ziel) + 1) + " noch nicht erreicht?)", now);
                if (now - letzterTipp > REISE_WARTEN_MS) {

                    if (!fensterOffen(z)) return abbrechen(a, "Zeile ohne Reise, Fenster zu (Gebiet " + ziel + " nicht aktiv?)", now);
                    if (versuche >= 2) return abbrechen(a, "Zeile ohne Reise nach 2 Tipps", now);
                    schritt = Schritt.FENSTER;
                }
                return a;
            }
            case SCHLIESSEN: {
                if (!fensterOffen(z)) {
                    schritt = Schritt.AUS;
                    meldung = "Wegpunkt Gebiet " + z.gebiet + " aktiviert, Fenster zu";
                    return a;
                }
                if (now - letzterTipp > 1500) {
                    if (++versuche > 6) return abbrechen(a, "Fenster geht nicht zu", now);
                    letzterTipp = now;
                    a.tippen = "menu_esc";
                    a.grund = "Wegpunkt: Fenster schließen";
                }
                return a;
            }
            default:
                return a;
        }
    }








    static int[] fensterPunkt(int x, int y, Zustand z) {
        int w = z.gw > 0 ? z.gw : 800, h = z.gh > 0 ? z.gh : 600;
        return new int[]{x + (w - 800) / 2, y + (h - 600) / 2, w, h};
    }


    static int zeile(int gebiet) {
        int a = akt(gebiet);
        if (a < 0) return -1;
        for (int i = 0; i < AKTE[a].length; i++) if (AKTE[a][i] == gebiet) return i;
        return -1;
    }

    private Gehirn.Absicht weiterIn(Gehirn.Absicht a, Schritt s, long now, String text) {
        weiter(s, now, text);
        return a;
    }

    private void weiter(Schritt s, long now, String text) {
        schritt = s;
        seit = now;
        versuche = 0;
        letzterTipp = 0;
        reiterAm = 0;
        meldung = "Wegpunkt: " + text;
    }

    private Gehirn.Absicht abbrechen(Gehirn.Absicht a, String warum, long now) {
        abbruch = warum;
        abbrueche++;
        schritt = Schritt.AUS;
        meldung = "Wegpunkt abgebrochen: " + warum;
        return a;
    }
}
