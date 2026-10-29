package com.winlator.d2.test;









public final class Rueckzug {
    public enum Ergebnis {

        LAEUFT,

        ZURUECK,

        ENDE_STADT,

        ENDE_DRAUSSEN,

        IN_STADT
    }


    public enum Grund {
        KEIN_PORTAL, NUR_AKT_1_2, ZU_OFT, NICHT_GESTARTET, STADT_OHNE_PORTAL, STADT_VERLASSEN_OHNE_PORTAL, FALSCHE_STADT,
        GEBIET_VERLASSEN, NICHT_GEHEILT, STADTGANG_ABGEBROCHEN, NICHT_AM_PORTAL,

        KAPUTT,

        TRAENKE,

        ROLLEN,

        TRUHE_VOLL,

        INVENTAR_VOLL
    }


    public static String text(Grund g) {
        switch (g) {
            case KEIN_PORTAL: return "kein Stadtportal";
            case NUR_AKT_1_2: return "Rückzug per Portal erst in Akt 1 und 2";
            case ZU_OFT: return "Rückzug zu oft";
            case NICHT_GESTARTET: return "Rückzug nicht gestartet";
            case STADT_OHNE_PORTAL: return "Stadt ohne Portal erreicht";
            case STADT_VERLASSEN_OHNE_PORTAL: return "Stadt ohne Portal verlassen";
            case FALSCHE_STADT: return "falsche Stadt";
            case GEBIET_VERLASSEN: return "Gebiet verlassen";
            case NICHT_GEHEILT: return "Heilen hat nicht gewirkt";
            case STADTGANG_ABGEBROCHEN: return "Stadtgang abgebrochen";
            case KAPUTT: return "Teil kaputt, Reparieren ging nicht";
            case TRAENKE: return "keine Heiltränke gekauft";
            case ROLLEN: return "keine Portalrolle gekauft";
            case TRUHE_VOLL: return "Truhe voll";
            case INVENTAR_VOLL: return "Inventar voll";
            default: return "nicht am eigenen Portal zurück";
        }
    }





    public static final long MIN_ABSTAND_MS = 60_000;

    public static final int MIN_HEIL = 2;





    public static final int VOLL_ZELLEN = 4;
    public static final long VOLL_ABSTAND_MS = 5 * 60_000, VOLL_GESEHEN_MS = 10_000;
    private long letzterVollGang = Long.MIN_VALUE / 2;

    public static final int GEHEILT_PCT = 90;

    public static final double PORTAL_NAH = 10;

    public final Stadtgang stadtgang = new Stadtgang();
    private int startGebiet = -1, anzahl;
    private long letzterStart = Long.MIN_VALUE / 2;
    private double portalX = Double.NaN, portalY = Double.NaN;
    private boolean inStadt;

    private boolean zumWegpunkt;
    private String grund = "";
    private Grund grundArt;

    public Rueckzug() {
        stadtgang.mitTruhe = false;
        stadtgang.alleMs = 0;
        stadtgang.mitSchmied = true;
    }





    public static boolean reparaturFaellig(Zustand z) {
        if (z.stadt || !z.rot()) return false;
        for (Zustand.Ding d : z.feinde()) if (z.bodenAbstand(d) < 600) return false;
        return true;
    }






    public static boolean traenkeFaellig(Zustand z, boolean einkaufMoeglich) {
        if (!einkaufMoeglich || z.stadt || z.heilInventar < 0 || z.heilGuertel + z.heilInventar >= MIN_HEIL) return false;
        for (Zustand.Ding d : z.feinde()) if (z.bodenAbstand(d) < 600) return false;
        return true;
    }

    public boolean aktiv() {
        return stadtgang.aktiv();
    }


    public boolean inStadt() {
        return inStadt;
    }


    public int anzahl() {
        return anzahl;
    }


    public String grund() {
        return grund;
    }


    public Grund grundArt() {
        return grundArt;
    }


    public static int akt(int gebiet) {
        if (gebiet >= 109) return 5;
        if (gebiet >= 103) return 4;
        if (gebiet >= 75) return 3;
        if (gebiet >= 40) return 2;
        return 1;
    }


    public static int stadt(int akt) {
        switch (akt) {
            case 2: return 40;
            case 3: return 75;
            case 4: return 103;
            case 5: return 109;
            default: return 1;
        }
    }






    public Grund warumNicht(Zustand z, int portale, long now) {
        if (portale <= 0) return Grund.KEIN_PORTAL;

        if (akt(z.gebiet) > 2) return Grund.NUR_AKT_1_2;
        if (now - letzterStart < MIN_ABSTAND_MS) return Grund.ZU_OFT;
        return null;
    }


    public void starten(Zustand z, long now) {
        startGebiet = z.gebiet;
        portalX = z.wx;
        portalY = z.wy;
        inStadt = false;
        anzahl++;
        letzterStart = now;
        zumWegpunkt = false;
        grund = "";
        grundArt = null;
        stadtgang.starten(now);
    }





    public void startenZumWegpunkt(Zustand z, long now) {
        long vorher = letzterStart;
        int n = anzahl;
        starten(z, now);
        letzterStart = vorher;
        anzahl = n;
        zumWegpunkt = true;
    }





    public boolean vollFaellig(Zustand z, boolean eigeneBeute, long passtNichtAm, long now) {

        if (!eigeneBeute || z.stadt || now - letzterVollGang < VOLL_ABSTAND_MS) return false;
        int frei = z.freieZellen();
        boolean voll = (frei >= 0 && frei < VOLL_ZELLEN) || (passtNichtAm > 0 && now - passtNichtAm < VOLL_GESEHEN_MS);
        if (!voll) return false;
        for (Zustand.Ding d : z.feinde()) if (z.bodenAbstand(d) < 600) return false;
        return true;
    }


    public void startenWegenVoll(Zustand z, long now) {
        starten(z, now);
        letzterVollGang = now;
        stadtgang.vollPflicht = true;
    }


    public void startenZumEinkauf(Zustand z, long now) {
        starten(z, now);
        stadtgang.traenkePflicht = true;
    }


    public Gehirn.Absicht denken(Zustand z, Zustand.Figur f, long now) {
        return stadtgang.denken(z, f, now);
    }


    public boolean fensterErlaubt() {
        return stadtgang.aktiv() && stadtgang.fensterErlaubt();
    }


    public boolean portalGenommen() {
        return stadtgang.aktiv() && stadtgang.portalGetippt();
    }





    public Ergebnis pruefen(Zustand z, long now) {
        if (startGebiet < 0) return ende(Ergebnis.ENDE_DRAUSSEN, Grund.NICHT_GESTARTET, "");
        boolean stadtJetzt = z.stadt;
        if (stadtJetzt != inStadt) {

            if (!stadtgang.portalGetippt()) {
                return stadtJetzt ? ende(Ergebnis.ENDE_STADT, Grund.STADT_OHNE_PORTAL, "")
                    : ende(Ergebnis.ENDE_DRAUSSEN, Grund.STADT_VERLASSEN_OHNE_PORTAL, "");
            }
            stadtgang.portalAngekommen();
            inStadt = stadtJetzt;
        }
        if (stadtJetzt) {
            if (z.gebiet != stadt(akt(startGebiet))) return ende(Ergebnis.ENDE_STADT, Grund.FALSCHE_STADT, " (" + z.gebiet + ")");
        }
        else {
            if (z.gebiet != startGebiet) return ende(Ergebnis.ENDE_DRAUSSEN, Grund.GEBIET_VERLASSEN, " (" + z.gebiet + ")");
        }


        if (stadtJetzt && stadtgang.schritt() == Stadtgang.Schritt.ZUM_PORTAL && z.rot()) {
            return ende(Ergebnis.ENDE_STADT, Grund.KAPUTT, " (" + stadtgang.reparatur + ")");
        }

        if (stadtJetzt && stadtgang.schritt() == Stadtgang.Schritt.ZUM_PORTAL && stadtgang.ohneHeiltrank) {
            return ende(Ergebnis.ENDE_STADT, Grund.TRAENKE, " (" + stadtgang.einkauf + ")");
        }
        if (stadtJetzt && stadtgang.schritt() == Stadtgang.Schritt.ZUM_PORTAL && stadtgang.ohneRollen) {
            return ende(Ergebnis.ENDE_STADT, Grund.ROLLEN, " (" + stadtgang.einkauf + ")");
        }

        if (stadtJetzt && stadtgang.schritt() == Stadtgang.Schritt.ZUM_PORTAL && stadtgang.truheVoll) {
            return ende(Ergebnis.ENDE_STADT, Grund.TRUHE_VOLL, " (" + stadtgang.abgabe + ")");
        }
        if (stadtJetzt && stadtgang.schritt() == Stadtgang.Schritt.ZUM_PORTAL && stadtgang.vollPflicht
            && z.freieZellen() >= 0 && z.freieZellen() < VOLL_ZELLEN) {
            return ende(Ergebnis.ENDE_STADT, Grund.INVENTAR_VOLL, " (" + z.freieZellen() + " Zellen frei; " + stadtgang.abgabe + ")");
        }

        if (stadtJetzt && stadtgang.schritt() == Stadtgang.Schritt.ZUM_PORTAL && z.lebenMax > 0 && z.lebenPct < GEHEILT_PCT) {
            return ende(Ergebnis.ENDE_STADT, Grund.NICHT_GEHEILT, " (" + z.lebenPct + " %)");
        }

        if (zumWegpunkt && stadtJetzt && stadtgang.schritt() == Stadtgang.Schritt.ZUM_PORTAL) {
            stadtgang.beenden();
            stadtgang.gaenge++;
            startGebiet = -1;
            zumWegpunkt = false;
            return Ergebnis.IN_STADT;
        }
        if (stadtgang.aktiv()) return Ergebnis.LAEUFT;
        if (stadtgang.abbruch != null) {
            return ende(stadtJetzt ? Ergebnis.ENDE_STADT : Ergebnis.ENDE_DRAUSSEN, Grund.STADTGANG_ABGEBROCHEN, ": " + stadtgang.abbruch);
        }

        if (stadtJetzt) return ende(Ergebnis.ENDE_STADT, Grund.STADTGANG_ABGEBROCHEN, ": endete in der Stadt");
        if (!z.weltlage() || Math.hypot(z.wx - portalX, z.wy - portalY) > PORTAL_NAH) {
            return ende(Ergebnis.ENDE_DRAUSSEN, Grund.NICHT_AM_PORTAL, "");
        }
        startGebiet = -1;
        return Ergebnis.ZURUECK;
    }


    public void abbrechen() {
        if (stadtgang.aktiv()) stadtgang.beenden();
        startGebiet = -1;
    }

    private Ergebnis ende(Ergebnis e, Grund g, String detail) {
        grundArt = g;
        grund = text(g) + detail;
        if (stadtgang.aktiv()) stadtgang.beenden();
        startGebiet = -1;
        return e;
    }
}
