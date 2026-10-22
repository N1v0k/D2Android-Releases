package com.winlator.d2.test;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class QuestLauf {
    public interface Umgebung {
        void event(String art, String text);
        Zustand.Figur figur();
        boolean testfigur(Zustand.Figur f);
        void fremdeFigur(Zustand.Figur f, Zustand.Layout l) throws InterruptedException;
        Set<Long> getoetet();
        void beenden();
    }

    public final QuestFahrer questFahrer;
    private final Gehirn gehirn;
    private final Fahrer fahrer;
    private final Stadtgang stadtgang;
    private final LeerenLauf ll;
    private final Umgebung u;
    public volatile int questWort = -1;
    private volatile long questAm;
    private int questZeilen;
    private boolean questVorAkara;
    private long questNachAkaraAb, rueckzugGemeldet;
    private int questVergessen;
    private String letzteQuestMeldung = "";
    static final long QUEST_WORT_MS = 15_000, QUEST_NACH_AKARA_MS = 1500;
    private final Map<Long, double[]> feindOrte = new HashMap<>();
    private static final double NACHSUCHE_RADIUS = 15;

    public QuestLauf(QuestZeile zeile, Gehirn gehirn, Fahrer fahrer, Stadtgang stadtgang, LeerenLauf ll, Umgebung u) {
        this(zeile, QuestFahrer.MAX_TODE, gehirn, fahrer, stadtgang, ll, u);
    }

    public QuestLauf(QuestZeile zeile, int maxTode, Gehirn gehirn, Fahrer fahrer, Stadtgang stadtgang, LeerenLauf ll, Umgebung u) {
        this.gehirn = gehirn;
        this.fahrer = fahrer;
        this.stadtgang = stadtgang;
        this.ll = ll;
        this.u = u;
        questFahrer = new QuestFahrer(zeile, maxTode);
        gehirn.gebietLeeren = false;
        fahrer.verhalten.todWeiter = questFahrer.maxTode > 1;
        fahrer.verhalten.pauseBeenden = true;
        fahrer.verhalten.fensterSchliessen = true;
        stadtgang.mitTruhe = false;
    }

    private void event(String art, String text) {
        u.event(art, text);
    }

    public void worte(int[] w, long am) {
        boolean nullen = w != null;
        for (int i = 0; w != null && i < w.length && nullen; i++) nullen = w[i] == 0;
        boolean ersteNullen = nullen && questZeilen++ == 0;
        QuestZeile qz = questFahrer.zeile;
        if (!ersteNullen && w != null && w.length > qz.wort) {
            int neu = w[qz.wort];
            if (neu != questWort) event("quest", "Quest-Wort " + qz.ort + " " + Integer.toHexString(neu));
            questWort = neu;
            questAm = am;
        }
    }

    public void paket(int[] w, long am) {
        if (w == null || am == paketAm) return;
        paketAm = am;
        worte(w, am);
    }

    private long paketAm = Long.MIN_VALUE;

    public int wortGueltig(long now) {
        return now - questAm <= QUEST_WORT_MS ? questWort : -1;
    }

    public boolean vorSchritt(Zustand z, Zustand.Layout l, long now) throws InterruptedException {
        if (!questSchritt(z, l, now)) return false;
        return !ll.leeren || ll.leerenSchritt(z, l, now);
    }

    public boolean punkteHalten() {
        QuestFahrer.Phase p = questFahrer.phase();
        return p == QuestFahrer.Phase.ABGEBEN || logVorAbgabe.laeuft()
            || p == QuestFahrer.Phase.FERTIG && questFahrer.endeCode == QuestFahrer.Ende.ABGESCHLOSSEN && !logNachAbschluss.fertig();
    }

    public boolean imStadtgang() {
        return stadtgang.aktiv();
    }

    public boolean imRueckzug() {
        return ll.leeren && ll.imRueckzug();
    }

    public boolean imKampf() {
        return gehirn.imKampf();
    }

    public QuestFahrer.Phase phase() {
        return questFahrer.phase();
    }

    public final java.util.Set<QuestZeile> abgeschlossen = new java.util.HashSet<>();

    static final int UI_QUEST = 0x0F;
    static final long LOG_MS = 2500;
    static final long RUHE_MS = 600;
    static final long OFFEN_MS = 300;
    static final long ZU_ESC_MS = 1000;
    static final long ESC_ABSTAND_MS = 1500;
    static final int LOG_VERSUCHE = 2;

    private final class QuestLog {
        private final String wozu;
        private int schritt, versuche;
        private long seit, ruheAb, escAm = Long.MIN_VALUE / 2;
        private boolean esc;

        QuestLog(String wozu) {
            this.wozu = wozu;
        }

        boolean fertig() {
            return schritt >= 3;
        }

        boolean laeuft() {
            return schritt > 0 && schritt < 3;
        }

        boolean schritt(Zustand z, Zustand.Layout l, long now) {
            if (schritt >= 3) return false;
            boolean offen = fensterOffen(z, UI_QUEST);
            switch (schritt) {
                case 0:
                    if (!z.stadt) {
                        schritt = 3;
                        event("quest", "Quest-Log (" + wozu + "): nicht in der Stadt, ausgelassen");
                        return false;
                    }
                    if (seit == 0) {
                        fahrer.allesLoslassen();
                        seit = now;
                        ruheAb = now;
                    }
                    if (offen) {
                        event("quest", "Quest-Log offen (" + wozu + ")");
                        schritt = 1;
                        seit = now - OFFEN_MS;
                        return true;
                    }
                    if ((z.panel && !offen) || z.npc) {
                        ruheAb = now;
                        if (now - escAm >= ESC_ABSTAND_MS && fahrer.tippen("hidden0", "Fenster schließen vor dem Quest-Log", l, now)) escAm = now;
                        if (now - seit < 3 * LOG_MS) return true;
                        event("quest", "Quest-Log (" + wozu + "): ein Fenster bleibt offen (" + z.fenster + ")");
                        schritt = 3;
                        return false;
                    }
                    if (now - ruheAb < RUHE_MS) return true;
                    if (fahrer.tippen("menu_quests", "Quest-Log öffnen (" + wozu + ")", l, now)) {
                        versuche++;
                        schritt = 1;
                        seit = now;
                        return true;
                    }
                    if (now - ruheAb < RUHE_MS + LOG_MS) return true;
                    event("quest", "Quest-Log: Knopf nicht erreichbar");
                    schritt = 3;
                    return false;
                case 1:
                    if (!offen) {
                        if (now - seit < LOG_MS) return true;
                        if (versuche < LOG_VERSUCHE) {
                            event("quest", "Quest-Log ging nicht auf: noch einmal");
                            schritt = 0;
                            ruheAb = now;
                            return true;
                        }
                        event("quest", "Quest-Log ging nicht auf");
                        schritt = 3;
                        return false;
                    }
                    if (now - seit < OFFEN_MS) return true;
                    if (!fahrer.tippen("menu_quests", "Quest-Log schließen", l, now)) {
                        if (now - seit < LOG_MS) return true;
                        event("quest", "Quest-Log offen, Schließen-Knopf nicht erreichbar");
                    }
                    else event("quest", "Quest-Log offen");
                    schritt = 2;
                    seit = now;
                    return true;
                default:
                    if (offen) {
                        if (!esc && now - seit >= ZU_ESC_MS && fahrer.tippen("hidden0", "Quest-Log schließen (Esc)", l, now)) esc = true;
                        if (now - seit < LOG_MS) return true;
                        event("quest", "Quest-Log blieb offen");
                    }
                    else event("quest", "Quest-Log zu");
                    schritt = 3;
                    return false;
            }
        }
    }

    private final QuestLog logNachAbschluss = new QuestLog("Belohnung ins Quest-Wort");
    private QuestLog logVorAbgabe = new QuestLog("vor der Abgabe: schon belohnt?");

    private boolean questLog(Zustand z, Zustand.Layout l, long now) {
        if (questFahrer.endeCode != QuestFahrer.Ende.ABGESCHLOSSEN) return false;
        abgeschlossen.add(questFahrer.zeile);
        return logNachAbschluss.schritt(z, l, now);
    }

    private static boolean fensterOffen(Zustand z, int ui) {
        try {
            return (Long.parseUnsignedLong(z.fenster, 16) >>> ui & 1) != 0;
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public Gehirn.Absicht denken(Zustand z, Zustand.Ziele ziele, Zustand.Figur figur, long now) {
        return stadtgang.aktiv() ? stadtgang.denken(z, figur, now) : gehirn.denken(z, ziele, now);
    }

    private void loslassen(String warum) {
        fahrer.loslassen(warum);
    }

    boolean questSchritt(Zustand z, Zustand.Layout l, long now) throws InterruptedException {
        Zustand.Figur f = u.figur();
        boolean frisch = f != null && now - f.ms < 3000;
        if (frisch && !u.testfigur(f)) {
            u.fremdeFigur(f, l);
            return false;
        }
        int wort = wortGueltig(now);
        QuestFahrer.Auftrag tot = questFahrer.tot(z.tot(), now);
        if (tot == QuestFahrer.Auftrag.ENDE) return questEnde(z, l, now);
        if (tot == QuestFahrer.Auftrag.TOT) {
            if (ll.leeren) ll.questLeerenZurueck();
            questMeldung(z);
            return true;
        }
        int zielGebiet = questFahrer.zeile.zielGebiet();
        if (z.gebiet == zielGebiet && z.weltlage()) {
            List<Zustand.Ding> feinde = z.feinde();
            for (Zustand.Ding d : feinde) feindOrte.put(d.id, Gehirn.weltlage(z, d));
            if (!feinde.isEmpty()) questFahrer.feindGesehen(now);
        }
        if (ll.leeren && ll.imRueckzug()) return true;
        if (stadtgang.aktiv()) questAbgabe(f, frisch, wort, now);
        QuestFahrer.Auftrag a = questFahrer.auftrag(z.gebiet, frisch ? f.stufe : -1, wort, now);
        questMeldung(z);
        switch (a) {
            case WARTEN:
                loslassen("Quest " + questFahrer.zeile.nummer + ": wartet auf die Figur");
                return false;
            case ENDE:
                return questEnde(z, l, now);
            case LEEREN:
                if (!ll.leeren) ll.questLeerenAn();
                if (questFahrer.rueckkehr > questVergessen && z.gebiet == zielGebiet && gehirn.abdeckung() != null
                    && gehirn.abdeckung().gebiet == zielGebiet) {
                    questVergessen = questFahrer.rueckkehr;
                    gehirn.abdeckung().vergessen();
                    event("quest", "neue Abdeckung (" + questFahrer.zeile.ort + ", Rückkehr " + questFahrer.rueckkehr + ")");
                }
                return true;
            case HINGEHEN:
                if (ll.leeren) ll.questLeerenZurueck();
                gehirn.zielGebiet = questFahrer.zielGebiet();
                return true;
            default:
                if (ll.leeren) ll.questLeerenZurueck();
                if (!stadtgang.aktiv() && stadtgang.abbruch != null) {
                    String grund = stadtgang.abbruch;
                    stadtgang.abbruch = null;
                    questFahrer.stadtgangAbbruch(grund, now);
                    questMeldung(z);
                    if (questFahrer.ende != null) return questEnde(z, l, now);
                    return true;
                }
                if (!stadtgang.aktiv() && questFahrer.phase() == QuestFahrer.Phase.ABGEBEN) {
                    if (!logVorAbgabe.fertig() && wort >= 0 && (wort & Quests.BELOHNUNG_OFFEN) != 0 && (wort & Quests.ERLEDIGT) == 0
                        && logVorAbgabe.schritt(z, l, now)) return false;
                    questVorAkara = false;
                    questNachAkaraAb = 0;
                    stadtgang.starten(now);
                }
                return true;
        }
    }

    private boolean questEnde(Zustand z, Zustand.Layout l, long now) {
        if (questLog(z, l, now)) return false;
        stadtgang.beenden();
        fahrer.allesLoslassen();
        event("ende", "Quest " + questFahrer.zeile.nummer + ": " + questFahrer.ende);
        u.beenden();
        return false;
    }

    private void questAbgabe(Zustand.Figur f, boolean frisch, int wort, long now) {
        if (questFahrer.phase() != QuestFahrer.Phase.ABGEBEN) return;
        Stadtgang.Schritt s = stadtgang.schritt();
        if (s == Stadtgang.Schritt.REDEN && !questVorAkara && frisch) {
            questVorAkara = true;
            questFahrer.vorAbgabe(f.fertigkeitPunkte);
            event("quest", "vor " + questFahrer.zeile.abgabe + ": Fertigkeitspunkte " + f.fertigkeitPunkte + ", Quest-Wort " + Integer.toHexString(wort));
        }
        boolean danach = s == Stadtgang.Schritt.ZUR_TRUHE || s == Stadtgang.Schritt.TRUHE || s == Stadtgang.Schritt.ZUM_PORTAL
            || s == Stadtgang.Schritt.ZURUECK;
        if (questVorAkara && danach) {
            if (questNachAkaraAb == 0) questNachAkaraAb = now + QUEST_NACH_AKARA_MS;
            if (now >= questNachAkaraAb && frisch && f.ms >= questNachAkaraAb - QUEST_NACH_AKARA_MS) {
                questVorAkara = false;
                questNachAkaraAb = 0;
                event("quest", "nach " + questFahrer.zeile.abgabe + ": Fertigkeitspunkte " + f.fertigkeitPunkte + ", Quest-Wort " + Integer.toHexString(wort));
                questFahrer.nachAbgabe(f.fertigkeitPunkte, wort, now);
                if (questFahrer.phase() == QuestFahrer.Phase.ABGEBEN) {
                    stadtgang.beenden();
                    logVorAbgabe = new QuestLog("nach " + questFahrer.zeile.abgabe + ": Belohnung im Quest-Wort?");
                }
            }
        }
    }

    public boolean questLeerenEnde(String grund) {
        if (grund.startsWith("Fenster offen") || grund.startsWith("Pause")) {
            fahrer.allesLoslassen();
            return true;
        }
        if (grund.startsWith("Leben unter")) {
            long now = SystemClock.uptimeMillis();
            if (now - rueckzugGemeldet > 10_000) {
                rueckzugGemeldet = now;
                event("rueckzug", "Quest " + questFahrer.zeile.nummer + ": " + grund + ": weiter leeren");
            }
            return true;
        }
        fahrer.allesLoslassen();
        int gebiet = ll.leerenGebiet;
        event("leeren", "beendet (Quest " + questFahrer.zeile.nummer + "): " + grund);
        ll.questLeerenZurueck();
        long now = SystemClock.uptimeMillis();
        if (!grund.startsWith("tot")) questFahrer.leerenEnde(grund, gebiet, wortGueltig(now), now);
        if (questFahrer.nachsucheAbholen()) questNachsuche(now);
        return false;
    }

    private void questNachsuche(long now) {
        java.util.List<double[]> orte = new ArrayList<>();
        for (Map.Entry<Long, double[]> e : feindOrte.entrySet()) if (!u.getoetet().contains(e.getKey())) orte.add(e.getValue());
        Abdeckung ab = gehirn.abdeckung();
        int n = ab != null && ab.gebiet == questFahrer.zeile.zielGebiet() ? ab.nachsuchen(orte, NACHSUCHE_RADIUS) : 0;
        event("quest", "Nachsuche " + questFahrer.nachsuchen + ": " + n + " Blöcke wieder offen (" + orte.size() + " Monster-Orte)");
        if (n == 0) questFahrer.nachsucheLeer(now);
    }

    private void questMeldung(Zustand z) {
        if (questFahrer.meldung.equals(letzteQuestMeldung)) return;
        letzteQuestMeldung = questFahrer.meldung;
        event("quest", letzteQuestMeldung + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : "") + " · Gebiet " + z.gebiet);
    }
}
