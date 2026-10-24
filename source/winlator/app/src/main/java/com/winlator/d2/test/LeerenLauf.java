package com.winlator.d2.test;







public final class LeerenLauf {

    public interface Umgebung {
        void event(String art, String text);
        Zustand.Ziele ziele();
        long zieleAm();
        int portale();
        Zustand.Figur figur();
        long totSeit();
        int kills();
        String gebietName(int gebiet);
        void ohneDurchgaenge(boolean ohne);
        boolean leerenBeenden(String grund, boolean pause, Zustand.Layout l) throws InterruptedException;





        default boolean modAuftrag(String was) {
            return false;
        }
    }

    private final Gehirn gehirn;
    private final Fahrer fahrer;
    private final Umgebung u;


    boolean leeren;

    public int rueckzugPct = RUECKZUG_PCT;
    private static final int RUECKZUG_PCT = Spieltester.RUECKZUG_PCT;

    int leerenGebiet = -1, leerenKillsStart;
    long leerenSeit;
    private long leerenVorbereitet, zieleOkAm, rueckzugGemeldet, reparaturGemeldet;

    final Rueckzug rueckzug = new Rueckzug();

    final LeerenMessung messung = new LeerenMessung();
    private boolean imRueckzug;
    private String letzteRueckzugMeldung = "";
    private static final long LEEREN_VORBEREITEN_MS = 15_000, LEEREN_OHNE_ZIELE_MS = 6_000;

    public LeerenLauf(Gehirn gehirn, Fahrer fahrer, Umgebung u) {
        this.gehirn = gehirn;
        this.fahrer = fahrer;
        this.u = u;
    }

    public boolean imRueckzug() {
        return imRueckzug;
    }


    public boolean aktiv() {
        return leeren;
    }





    public boolean gebietLeerEnde(boolean stadtgangAktiv, Zustand.Layout l, long now) throws InterruptedException {
        if (leeren && !stadtgangAktiv && gehirn.gebietLeer(now)) {
            event("leeren", "Gebiet leer: " + gehirn.abdeckungText(now));
            u.leerenBeenden("Gebiet leer", true, l);
            return true;
        }
        return false;
    }

    private void event(String art, String text) {
        u.event(art, text);
    }

    private void loslassen(String warum) {
        fahrer.loslassen(warum);
    }







    boolean leerenSchritt(Zustand z, Zustand.Layout l, long now) throws InterruptedException {

        u.ohneDurchgaenge(!imRueckzug);
        Zustand.Ziele zl = u.ziele();
        boolean gueltig = zl != null && zl.gebiet == z.gebiet && zl.bereit == 2 && now - u.zieleAm() <= LEEREN_OHNE_ZIELE_MS;
        if (leerenGebiet < 0) {
            if (leerenVorbereitet == 0) leerenVorbereitet = now;
            if (z.stadt) return u.leerenBeenden("Idle nur außerhalb der Stadt", false, l);
            if (z.tot()) return u.leerenBeenden("tot", false, l);
            if (!gueltig) {
                loslassen("Idle bereitet vor");
                return now - leerenVorbereitet > LEEREN_VORBEREITEN_MS ? u.leerenBeenden("keine Gebietsdaten (Vorbereitung)", false, l) : false;
            }
            leerenGebiet = z.gebiet;
            gehirn.zielGebiet = z.gebiet;
            leerenSeit = zieleOkAm = now;
            leerenKillsStart = u.kills();
            messung.start(now, gehirn.hindernisse);
            event("leeren", "aktiv in Gebiet " + z.gebiet + " " + u.gebietName(z.gebiet) + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : ""));
        }
        if (imRueckzug) {
            messung.rueckzug(now);
            return rueckzugSchritt(z, l, now);
        }
        if (z.gebiet != leerenGebiet) return u.leerenBeenden("Gebiet verlassen (" + u.gebietName(z.gebiet) + ")", false, l);
        if (z.stadt) return u.leerenBeenden("Stadt", false, l);
        if (z.tot()) {

            event("tod", "Tod beim Gebiet leeren nach " + (now - leerenSeit) / 1000 + " s · Lage " + z.wx + "," + z.wy);
            return u.leerenBeenden("tot", false, l);
        }
        if (l.pauseGewollt || z.pause) return u.leerenBeenden("Pause", false, l);
        if (z.panel || z.npc) return u.leerenBeenden("Fenster offen", false, l);
        if (gueltig) zieleOkAm = now;
        else if (now - zieleOkAm > LEEREN_OHNE_ZIELE_MS) return u.leerenBeenden("keine Gebietsdaten", false, l);
        if (z.lebenMax > 0 && z.lebenPct < rueckzugPct) {
            Rueckzug.Grund warum = rueckzug.warumNicht(z, u.portale(), now);
            if (warum != null) return u.leerenBeenden("Leben unter " + rueckzugPct + " % (" + z.lebenPct + " %), " + Rueckzug.text(warum), true, l);
            fahrer.allesLoslassen();
            rueckzug.starten(z, now);
            imRueckzug = true;
            event("rueckzug", "Rückzug " + rueckzug.anzahl() + " bei Leben " + z.lebenPct + " %, Portale " + u.portale()
                + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : ""));
            return false;
        }

        if (Rueckzug.reparaturFaellig(z)) {
            Rueckzug.Grund warum = rueckzug.warumNicht(z, u.portale(), now);
            if (warum == null) {
                fahrer.allesLoslassen();
                rueckzug.starten(z, now);
                imRueckzug = true;
                event("rueckzug", "Rückzug " + rueckzug.anzahl() + " zum Reparieren (Teil kaputt), Portale " + u.portale()
                    + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : ""));
                return false;
            }
            if (now - reparaturGemeldet > 60_000) {
                reparaturGemeldet = now;
                event("rueckzug", "Teil kaputt, kein Rückzug: " + Rueckzug.text(warum));
            }
        }
        if (rueckzugPct == 0 && z.lebenMax > 0 && z.lebenPct < RUECKZUG_PCT && now - rueckzugGemeldet > 10_000) {
            rueckzugGemeldet = now;
            event("rueckzug", "Leben " + z.lebenPct + " %: der Idle-Modus hätte hier (unter " + RUECKZUG_PCT + " %) beendet");
        }
        messung.zustand(now, gehirn.imKampf(), z.weltlage() ? z.wx : Double.NaN, z.weltlage() ? z.wy : Double.NaN);
        if (!gueltig) {
            loslassen("keine Gebietsdaten");
            return false;
        }
        return true;
    }





    private boolean rueckzugSchritt(Zustand z, Zustand.Layout l, long now) throws InterruptedException {
        if (z.tot()) {
            event("tod", "Tod im Rückzug · Schritt " + rueckzug.stadtgang.schritt());
            return u.leerenBeenden("tot (Rückzug)", false, l);
        }
        if (l.pauseGewollt || z.pause) return u.leerenBeenden("Pause (Rückzug)", false, l);
        if ((z.panel || z.npc) && !rueckzug.fensterErlaubt()) return u.leerenBeenden("Fenster offen (Rückzug)", false, l);
        Rueckzug.Ergebnis e = rueckzug.pruefen(z, now);
        if (!rueckzug.stadtgang.meldung.equals(letzteRueckzugMeldung)) {
            letzteRueckzugMeldung = rueckzug.stadtgang.meldung;
            event("stadtgang", letzteRueckzugMeldung + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : ""));
        }
        switch (e) {
            case ENDE_STADT:
                return u.leerenBeenden("Rückzug: " + rueckzug.grund(), false, l);
            case ENDE_DRAUSSEN:
                return u.leerenBeenden("Rückzug: " + rueckzug.grund(), true, l);
            case ZURUECK:
                imRueckzug = false;
                fahrer.verhalten.fensterDurchlassen = false;
                zieleOkAm = now;
                event("rueckzug", "Rückzug " + rueckzug.anzahl() + " fertig, Leben " + z.lebenPct + " %"
                    + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : ""));
                return false;
            default:
                break;
        }
        fahrer.verhalten.fensterDurchlassen = rueckzug.fensterErlaubt();
        if (!fahrer.vorbereiten(z, l, now, u.totSeit())) return false;
        Gehirn.Absicht a = rueckzug.denken(z, u.figur(), now);
        if (a.modAuftrag != null) {
            a.getippt = u.modAuftrag(a.modAuftrag);
            event("auftrag", a.modAuftrag + (a.getippt ? "" : ": nicht möglich"));
        }
        fahrer.anwenden(a, l, now);
        return false;
    }

    void questLeerenZurueck() {
        leeren = false;
        leerenGebiet = -1;
        leerenVorbereitet = 0;
        imRueckzug = false;
        gehirn.gebietLeeren = false;
        u.ohneDurchgaenge(false);
    }


    void questLeerenAn() {
        leeren = true;
        leerenGebiet = -1;
        leerenVorbereitet = 0;
        gehirn.gebietLeeren = true;
    }
}
