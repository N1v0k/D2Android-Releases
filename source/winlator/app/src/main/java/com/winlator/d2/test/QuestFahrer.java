package com.winlator.d2.test;

import java.util.EnumMap;
import java.util.Map;














public final class QuestFahrer {




    public enum Phase { VORBEREITEN, REISEN, ERLEDIGEN, ABGEBEN, FERTIG }





    public enum Auftrag { WARTEN, TOT, LEEREN, HINGEHEN, STADTGANG, ENDE }


    public static final int MAX_TODE = 3, MAX_RUECKKEHR = 2, MAX_ABBRUECHE = 3;

    public enum Ende { ABGESCHLOSSEN, SCHON_ERLEDIGT, ZU_OFT_GESTORBEN, NICHT_LEER, KEINE_BELOHNUNG, NICHT_ERREICHT, ZEIT_UM, KAPUTT }




    public static final long HINGEHEN_OHNE_WECHSEL_MS = 4 * 60_000;
    static final int ERLEDIGT = Quests.ERLEDIGT, BELOHNUNG_OFFEN = Quests.BELOHNUNG_OFFEN;

    public static final String ABGESCHLOSSEN = "Quest abgeschlossen", SCHON_ERLEDIGT = "schon erledigt",
        ZU_OFT_GESTORBEN = "zu oft gestorben";

    public final String nichtLeer, keineBelohnung, nichtErreicht;


    public final QuestZeile zeile;

    private Phase phase;
    private long phaseSeit;
    private final Map<Phase, Long> dauer = new EnumMap<>(Phase.class);

    public String ende;
    public Ende endeCode;




    private int letztesGebiet = -1, letztesZiel = -1;
    private long wechselAm;
    private Auftrag letzterAuftrag;

    private boolean abgabeGesprochen;

    public final int maxTode;

    public int tode, rueckkehr;

    public int abbrueche;
    private boolean totGezaehlt;
    private boolean leerErsatz;






    public static final long NACHSUCHE_OHNE_FEIND_MS = 4 * 60_000;

    public static final int NACHSUCHE_MAX_RUNDEN = 5;
    public static final long NACHSUCHE_MAX_MS = 8 * 60_000;
    private long nachsucheSeit, letzterFeindAm;
    private int nachsucheRunden;
    private boolean nachsucheAngefordert;

    public int nachsuchen;

    private boolean zuFuss;

    private int punkteVorher = -1;

    public String meldung = "";

    public QuestFahrer(QuestZeile zeile) {
        this(zeile, MAX_TODE);
    }

    public QuestFahrer(QuestZeile zeile, int maxTode) {
        if (!zeile.kann()) throw new IllegalArgumentException(zeile + ": target type " + zeile.zieltyp + " not driven yet");
        this.zeile = zeile;
        this.maxTode = Math.max(1, maxTode);
        nichtLeer = zeile.ort + " nicht leer";
        keineBelohnung = "keine Belohnung bei " + zeile.abgabe;
        nichtErreicht = zeile.abgabe + " nicht erreicht";
    }

    public Phase phase() {
        return phase;
    }


    public Map<Phase, Long> dauer(long now) {
        Map<Phase, Long> m = new EnumMap<>(dauer);
        if (phase != null && phase != Phase.FERTIG) m.merge(phase, now - phaseSeit, Long::sum);
        return m;
    }


    public void starten(int stufe, int wort, long now) {
        if (wort >= 0 && (wort & ERLEDIGT) != 0) beenden(Ende.SCHON_ERLEDIGT, SCHON_ERLEDIGT, now);
        else if (wort >= 0 && (wort & BELOHNUNG_OFFEN) != 0) wechseln(Phase.ABGEBEN, now, zeile.ort + " schon leer: zu " + zeile.abgabe);
        else if (stufe >= 0 && stufe < zeile.mindeststufe) wechseln(Phase.VORBEREITEN, now, "Leveln (" + zeile.levelName + ") bis Stufe " + zeile.mindeststufe);
        else wechseln(Phase.REISEN, now, "Ziel: " + zeile.name);
    }


    public Auftrag auftrag(int gebiet, int stufe, int wort, long now) {
        if (phase == null) {
            if (stufe < 0) return Auftrag.WARTEN;
            starten(stufe, wort, now);
        }
        if (phase == Phase.FERTIG) return Auftrag.ENDE;
        if (wort >= 0 && (wort & ERLEDIGT) != 0) {
            if (phase == Phase.ABGEBEN && abgabeGesprochen) beenden(Ende.ABGESCHLOSSEN, ABGESCHLOSSEN, now);
            else beenden(Ende.SCHON_ERLEDIGT, SCHON_ERLEDIGT, now);
            return Auftrag.ENDE;
        }
        if (gebiet != letztesGebiet) {
            letztesGebiet = gebiet;
            wechselAm = now;
        }
        Auftrag a = schritt(gebiet, stufe, wort, now);
        int ziel = zielGebiet();
        if (a == Auftrag.HINGEHEN && (letzterAuftrag != Auftrag.HINGEHEN || ziel != letztesZiel)) wechselAm = now;
        letzterAuftrag = a;
        letztesZiel = ziel;
        if (a == Auftrag.HINGEHEN && now - wechselAm > HINGEHEN_OHNE_WECHSEL_MS) {
            beenden(Ende.ZEIT_UM, "kein Fortschritt: " + HINGEHEN_OHNE_WECHSEL_MS / 60_000 + " min ohne Gebietswechsel auf dem Weg nach Gebiet "
                + zielGebiet() + " (Phase " + phase + ")", now);
            return Auftrag.ENDE;
        }
        return a;
    }

    private Auftrag schritt(int gebiet, int stufe, int wort, long now) {
        if (phase != Phase.ABGEBEN && wort >= 0 && (wort & BELOHNUNG_OFFEN) != 0) wechseln(Phase.ABGEBEN, now, zeile.ort + " leer (Quest-Stand): zu " + zeile.abgabe);
        int ziel = zeile.zielGebiet();
        switch (phase) {
            case VORBEREITEN:
                if (stufe >= zeile.mindeststufe) {
                    wechseln(Phase.REISEN, now, "Stufe " + stufe + ": Ziel " + zeile.name);
                    return gebiet == ziel ? weiterImZiel(now) : Auftrag.HINGEHEN;
                }
                return gebiet == zeile.levelGebiet ? Auftrag.LEEREN : Auftrag.HINGEHEN;
            case REISEN:
                return gebiet == ziel ? weiterImZiel(now) : Auftrag.HINGEHEN;
            case ERLEDIGEN:
                return gebiet == ziel ? Auftrag.LEEREN : Auftrag.HINGEHEN;
            default:

                return zuFuss && gebiet != zeile.stadt ? Auftrag.HINGEHEN : Auftrag.STADTGANG;
        }
    }


    public void keinPortal() {
        if (phase != Phase.ABGEBEN || zuFuss) return;
        zuFuss = true;
        meldung = "kein Stadtportal: zu Fuß in die Stadt, dann zu " + zeile.abgabe;
    }

    private Auftrag weiterImZiel(long now) {
        wechseln(Phase.ERLEDIGEN, now, "im Zielgebiet (" + zeile.ort + "): leeren");
        return Auftrag.LEEREN;
    }


    public int zielGebiet() {
        return phase == Phase.VORBEREITEN ? zeile.levelGebiet : phase == Phase.ABGEBEN ? zeile.stadt : zeile.zielGebiet();
    }


    public void leerenEnde(String grund, int gebiet, long now) {
        leerenEnde(grund, gebiet, -1, now);
    }






    public void leerenEnde(String grund, int gebiet, int wort, long now) {
        if (!"Gebiet leer".equals(grund)) return;
        if (phase == Phase.VORBEREITEN && gebiet == zeile.levelGebiet) wechseln(Phase.REISEN, now, zeile.levelName + " leer: Ziel " + zeile.name);
        else if (phase == Phase.ERLEDIGEN && gebiet == zeile.zielGebiet()) {
            String notausgang = null;
            if (wort >= 0 && (wort & (BELOHNUNG_OFFEN | ERLEDIGT)) == 0) {
                if (nachsucheSeit == 0) {
                    nachsucheSeit = now;
                    letzterFeindAm = now;
                    nachsucheRunden = 0;
                }
                if (nachsucheRunden >= NACHSUCHE_MAX_RUNDEN) notausgang = NACHSUCHE_MAX_RUNDEN + " Nachsuchen";
                else if (now - nachsucheSeit >= NACHSUCHE_MAX_MS) notausgang = NACHSUCHE_MAX_MS / 60_000 + " min Nachsuche";
                else if (now - letzterFeindAm >= NACHSUCHE_OHNE_FEIND_MS) notausgang = NACHSUCHE_OHNE_FEIND_MS / 60_000 + " min ohne Monster";
                else {
                    nachsucheAngefordert = true;
                    nachsuchen++;
                    nachsucheRunden++;
                    meldung = "Abdeckung leer, Quest-Stand noch nicht: Nachsuche " + nachsuchen + " (" + zeile.ort + ")";
                    return;
                }
            }
            zurAbgabeErsatz(now, notausgang);
        }
    }


    public boolean nachsucheAbholen() {
        boolean r = nachsucheAngefordert;
        nachsucheAngefordert = false;
        return r;
    }


    public void nachsucheLeer(long now) {
        if (phase == Phase.ERLEDIGEN) zurAbgabeErsatz(now, "nichts mehr offen");
    }


    public void feindGesehen(long now) {
        letzterFeindAm = now;
    }


    private void zurAbgabeErsatz(long now, String notausgang) {
        leerErsatz = true;
        nachsucheSeit = 0;
        wechseln(Phase.ABGEBEN, now, zeile.ort + " leer (Abdeckung" + (nachsuchen > 0 ? ", nach " + nachsuchen + " Nachsuchen" : "")
            + (notausgang != null ? ", Notausgang: " + notausgang : "") + "): zu " + zeile.abgabe);
    }






    public Auftrag tot(boolean tot, long now) {
        if (!tot) {
            totGezaehlt = false;
            return phase == Phase.FERTIG ? Auftrag.ENDE : null;
        }
        if (!totGezaehlt) {
            totGezaehlt = true;
            tod(now);
        }
        return phase == Phase.FERTIG ? Auftrag.ENDE : Auftrag.TOT;
    }






    public void stadtgangAbbruch(String grund, long now) {
        if (phase != Phase.ABGEBEN) return;
        if (grund.startsWith("kein Portal") || grund.startsWith("Portal nimmt nicht an")) {
            if (!zuFuss) {
                keinPortal();
                return;
            }
        }
        if (++abbrueche >= MAX_ABBRUECHE) beenden(Ende.NICHT_ERREICHT, nichtErreicht + " (" + grund + ")", now);
        else meldung = "Stadtgang abgebrochen (" + grund + "): noch einmal (" + abbrueche + " von " + MAX_ABBRUECHE + ")";
    }


    public void tod(long now) {
        if (phase == Phase.FERTIG) return;
        tode++;
        if (tode >= maxTode) beenden(Ende.ZU_OFT_GESTORBEN, ZU_OFT_GESTORBEN, now);
        else meldung = "Tod " + tode + " in Phase " + phase + ": weiter in derselben Phase";
    }


    public void vorAbgabe(int punkte) {
        abgabeGesprochen = true;
        punkteVorher = punkte;
    }






    public void nachAbgabe(int punkte, int wort, long now) {
        if (phase != Phase.ABGEBEN) return;
        abgabeGesprochen = true;
        boolean belohnt = (wort >= 0 && (wort & ERLEDIGT) != 0) || (punkteVorher >= 0 && punkte > punkteVorher);
        punkteVorher = -1;
        if (belohnt) {
            beenden(Ende.ABGESCHLOSSEN, ABGESCHLOSSEN, now);
            return;
        }
        boolean leer = wort >= 0 && (wort & BELOHNUNG_OFFEN) != 0;
        if (++rueckkehr > MAX_RUECKKEHR) {
            if (leer) beenden(Ende.KEINE_BELOHNUNG, keineBelohnung, now);
            else beenden(Ende.NICHT_LEER, nichtLeer, now);
            return;
        }
        if (leer) {
            meldung = zeile.abgabe + " ohne Belohnung, das Ziel ist laut Quest-Stand erledigt: noch einmal ansprechen (" + rueckkehr + " von " + MAX_RUECKKEHR + ")";
            return;
        }
        leerErsatz = false;
        wechseln(Phase.ERLEDIGEN, now, zeile.abgabe + " ohne Belohnung: zurück ins Zielgebiet (" + rueckkehr + " von " + MAX_RUECKKEHR + ")");
    }


    public boolean leerNurErsatz() {
        return leerErsatz;
    }

    private void wechseln(Phase neu, long now, String text) {
        if (phase != null && phase != Phase.FERTIG) dauer.merge(phase, now - phaseSeit, Long::sum);
        phase = neu;
        phaseSeit = now;
        meldung = text;
    }


    public void kaputt(String grund, long now) {
        if (phase != Phase.FERTIG) beenden(Ende.KAPUTT, grund, now);
    }

    private void beenden(Ende code, String grund, long now) {
        endeCode = code;
        wechseln(Phase.FERTIG, now, "Ende: " + grund);
        ende = grund;
    }
}
