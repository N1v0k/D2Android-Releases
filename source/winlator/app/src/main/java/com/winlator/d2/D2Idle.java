package com.winlator.d2;

import com.winlator.R;

import android.os.SystemClock;
import android.view.MotionEvent;

import com.winlator.d2.test.Fahrer;
import com.winlator.d2.test.Gehirn;
import com.winlator.d2.test.LeerenLauf;
import com.winlator.d2.test.QuestFahrer;
import com.winlator.d2.test.QuestLauf;
import com.winlator.d2.test.QuestZeile;
import com.winlator.d2.test.Quests;
import com.winlator.d2.test.Rueckzug;
import com.winlator.d2.test.Stadtgang;
import com.winlator.d2.test.Zustand;

import org.json.JSONObject;

















final class D2Idle {

    static final int DEVICE_ID = -81;
    static final long WISH_MS = 3000, PREPARE_MS = 15_000, NO_GOALS_MS = 6000, NO_STATE_MS = 3000, GOALS_MAX_AGE_MS = 6000;

    static final int DURATION_MIN = 15, RETREAT_PERCENT = 25;

    static final long TELL_LATER_MS = 10 * 60_000;

    enum Phase { OFF, WANTED, PREPARING, ACTIVE, RETREAT, RETURNING }


    static final long LOAD_MS = 20_000, FLAGS_AFTER_LOAD_MS = 5000;

    static final long PAUSE_LATER_MS = 120_000;


    interface Host {
        D2ModState state();
        D2ModLink link();

        String layout();
        boolean pauseWanted();
        void setPaused(boolean paused);
        void dispatch(MotionEvent event);

        void message(String text);

        String text(int id, Object... args);
        void redraw();

        com.winlator.d2.test.Fertigkeiten skills();

        D2Tabellen tabellen();

        void ignoreTargets(java.util.Set<Long> ids);

        boolean pressHotkey(int index);

        java.util.Set<Long> leftOutObjects();

        D2PointPlan.Build build(int characterClass, String name);

        boolean buildChosen(int characterClass, String name);

        java.io.File saveDir();
    }

    private final Host host;
    private final D2IdleFinger finger;
    private Phase phase = Phase.OFF;
    private long since, goalsOkAt, startedAt, durationMs = DURATION_MIN * 60_000L;
    private int retreatPercent = RETREAT_PERCENT, kills;

    private final java.util.Set<Long> seenAlive = new java.util.HashSet<>(), counted = new java.util.HashSet<>();
    private boolean focused = true;
    private final D2IdleUntold untold = new D2IdleUntold(TELL_LATER_MS);
    private boolean pausedByMenu;
    private int seqAfterUnpause = -1, level = -1;
    private Gehirn gehirn;

    private com.winlator.d2.test.Objekte objekte;
    private long keyCheckedAt;

    private com.winlator.d2.test.Anlegen anlegen;

    private volatile boolean equipBusy;

    static final long MANA_WISH_MS = 500;

    private long manaWishAt = -MANA_WISH_MS;
    private Fahrer fahrer;
    private String reason = "";
    private Rueckzug rueckzug;

    private long repairNotedAt;

    private boolean pauseLater;
    private long pauseLaterUntil, levelChangedAt;
    private int lastLevel = -1;

    private final D2IdleRight right = new D2IdleRight();

    private D2PointSpend points = new D2PointSpend();




    static final double POINTS_NO_ENEMY = 500;

    private D2PointPlan.Build rolesBuild;
    private int rolesLevel = -1;




    private QuestZeile questZeile;
    private QuestLauf quest;
    private LeerenLauf questLeeren;

    static final int QUEST_MAX_TODE = 1;

    static final long QUEST_NO_GOALS_MS = 20_000;
    private boolean questBeendet, questOhneDurchgaenge;
    private Zustand.Figur questFigur;
    private long questFigurAt, questGoalsAt;
    private Zustand.Ziele questGoals;
    private final D2IdleRight.Actions rightActions = new D2IdleRight.Actions() {
        @Override public boolean pressHotkey(int index) { return host.pressHotkey(index); }
        @Override public void selectRight(int skill) { host.link().selectSkill(false, skill); }
        @Override public void trace(String text) { D2Idle.trace(text); }
    };

    D2Idle(Host host) {
        this.host = host;
        this.finger = new D2IdleFinger(host::dispatch);
    }

    Phase phase() {
        return phase;
    }

    boolean running() {
        return phase != Phase.OFF && phase != Phase.WANTED;
    }


    boolean equipping() {
        return equipBusy && phase == Phase.ACTIVE;
    }


    int drinkPercent() {
        return Gehirn.trinkSchwelle(retreatPercent);
    }


    boolean manaWanted(long now) {
        return running() && now - manaWishAt < MANA_WISH_MS;
    }


    boolean clearing() {
        if (questZeile != null) return phase == Phase.ACTIVE && questOhneDurchgaenge;
        return phase == Phase.PREPARING || phase == Phase.ACTIVE;
    }


    String blocked() {
        D2ModState s = host.state();
        D2ModLink link = host.link();
        if (s == null) return host.text(R.string.idle_only_in_game);


        String name = link.currentCharacterName();
        Boolean hardcore = name != null ? D2SaveHeader.hardcore(host.saveDir(), name) : null;
        D2IdleStart.Sperre why = D2IdleStart.pruefen(link.singlePlayer(), name, hardcore, s.inTown, s.levelNo,
            s.inTown ? link.questWorte() : null, s.life > 0, windowOpen(s));
        if (why == null) return null;
        switch (why) {
            case NUR_EINZELSPIELER: return host.text(R.string.idle_only_single);
            case FIGUR_LAEDT: return host.text(R.string.idle_character_loading);
            case HC_UNBEKANNT: return host.text(R.string.idle_hardcore_unknown);
            case HARDCORE: return host.text(R.string.idle_not_hardcore);
            case QUEST_NUR_AKT1: return host.text(R.string.idle_quest_only_act1);
            case QUEST_UNBEKANNT: return host.text(R.string.idle_quest_unknown);
            case QUEST_KEINE: return host.text(R.string.idle_quest_none);
            case TOT: return host.text(R.string.idle_not_dead);
            default: return host.text(R.string.idle_close_window);
        }
    }





    private static boolean windowOpen(D2ModState s) {
        return (s.panelFlags() & ~(1L << D2ModState.UI_ESCMENU)) != 0 || s.npcTalking;
    }





    private static boolean otherWindowOpen(D2ModState s) {
        long own = (1L << D2ModState.UI_ESCMENU) | (1L << D2ModState.UI_NPCMENU) | (1L << D2ModState.UI_NPCSHOP)
            | (1L << D2ModState.UI_INVENTORY);
        return (s.panelFlags() & ~own) != 0;
    }


    void onRealTouch() {
        if (pauseLater) trace("pause later dropped: the player took over");
        pauseLater = false;
        if (phase == Phase.OFF) right.takeover(rightActions);
    }


    void request(boolean pausedBefore) {
        request(pausedBefore, DURATION_MIN, RETREAT_PERCENT);
    }


    void request(boolean pausedBefore, int minutes, int retreat) {
        if (running()) return;
        durationMs = Math.max(1, minutes) * 60_000L;
        retreatPercent = Math.max(5, Math.min(90, retreat));
        String why = blocked();
        if (why != null) {
            host.message(host.text(R.string.idle_prefix, why));
            return;
        }
        if (pausedBefore) {
            host.message(host.text(R.string.idle_prefix, host.text(R.string.idle_end_pause_first)));
            return;
        }
        phase = Phase.WANTED;
        pauseLater = false;
        since = SystemClock.uptimeMillis();
        trace("wanted (" + minutes + " min, retreat " + retreatPercent + " %, drink " + Gehirn.trinkSchwelle(retreatPercent) + " %)");
        host.redraw();
    }


    void onFocus(boolean focused) {
        long now = SystemClock.uptimeMillis();
        this.focused = focused;
        if (!focused) {
            if (running()) stop(host.text(R.string.idle_focus_lost));
            return;
        }

        String late = untold.take(SystemClock.elapsedRealtime());
        if (late != null) host.message(late);
        if (phase == Phase.OFF) restoreRight(now);
        if (phase != Phase.WANTED) return;
        if (now - since > WISH_MS) {
            end(host.text(R.string.idle_not_started_menu));
            return;
        }
        String why = blocked();
        if (why != null) {
            end(host.text(R.string.idle_prefix, why));
            return;
        }

        pausedByMenu = host.pauseWanted();
        if (pausedByMenu) host.setPaused(false);
        D2ModState s = host.state();
        seqAfterUnpause = s != null ? s.sequence : -1;
        level = s != null ? s.levelNo : -1;

        questZeile = s != null && s.inTown ? Quests.naechsteOffene(host.link().questWorte()) : null;
        quest = null;
        questLeeren = null;
        host.link().setIdleData(true);
        phase = Phase.PREPARING;
        since = now;
        trace("preparing: seq " + seqAfterUnpause + ", level " + level + ", pausedByMenu " + pausedByMenu
            + ", state paused " + (s != null && s.paused()));
        host.redraw();
    }


    void step() {
        long now = SystemClock.uptimeMillis();
        if (phase == Phase.OFF) {
            makeUpPause(now);
            restoreRight(now);
            return;
        }
        if (phase == Phase.WANTED) {
            if (now - since > WISH_MS) end(host.text(R.string.idle_not_started_focus));
            return;
        }
        D2ModState s = host.state();
        D2ModLink link = host.link();
        boolean retreat = phase == Phase.RETREAT;
        if (s == null) {


            boolean loading = questZeile != null || (retreat && rueckzug.portalGenommen());
            long grace = loading ? LOAD_MS : NO_STATE_MS;
            if (fahrer != null) fahrer.allesLoslassen();
            if (now - Math.max(since, link.lastStateAt()) > grace) {


                boolean pauseOwed = !retreat || loading || !rueckzug.inStadt();
                stop(host.text(R.string.idle_no_state));

                if (pauseOwed) {
                    pauseLater = true;
                    pauseLaterUntil = now + PAUSE_LATER_MS;
                    trace("pause later: no state outside");
                }
            }
            return;
        }
        if (s.levelNo != lastLevel) {
            lastLevel = s.levelNo;
            levelChangedAt = now;
        }


        boolean afterLoad = (retreat || phase == Phase.RETURNING || (questZeile != null && phase == Phase.ACTIVE))
            && link.gameFlagsAt() < levelChangedAt;
        if (afterLoad) {
            if (now - levelChangedAt > FLAGS_AFTER_LOAD_MS) {
                stop(host.text(R.string.idle_multiplayer));
                return;
            }
            if (fahrer != null) fahrer.allesLoslassen();
            return;
        }
        if (!link.singlePlayer()) {
            stop(host.text(R.string.idle_multiplayer));
            return;
        }
        if (questZeile != null) {
            questStep(s, link, now);
            return;
        }
        if (retreat) {
            retreatStep(s, link, now);
            return;
        }
        if (s.inTown) {
            stop(host.text(R.string.idle_town));
            return;
        }
        if (s.levelNo != level) {
            stop(host.text(R.string.idle_left_area));
            return;
        }
        if (s.life <= 0) {
            stop(host.text(R.string.idle_death));
            return;
        }
        if (D2IdleStart.pauseEndet(host.pauseWanted(), s.paused(), phase == Phase.PREPARING, now - since)) {

            trace("pause in " + phase + ": pauseWanted " + host.pauseWanted() + ", state paused " + s.paused()
                + ", seq " + s.sequence + " (at unpause " + seqAfterUnpause + ")");
            stop(host.text(R.string.idle_pause));
            return;
        }
        if (windowOpen(s)) {
            stop(host.text(R.string.idle_window));
            return;
        }
        Zustand.Ziele ziele = validGoals(link, s, now);
        if (phase == Phase.PREPARING) {

            boolean fresh = (seqAfterUnpause < 0 || ((s.sequence - seqAfterUnpause) & 0xFFFF) != 0) && !s.paused();
            if (fresh && ziele != null) {
                phase = Phase.ACTIVE;
                startedAt = goalsOkAt = now;
                kills = 0;
                seenAlive.clear();
                counted.clear();
                gehirn = new Gehirn(now);
                anlegen = new com.winlator.d2.test.Anlegen();
                equipBusy = false;
                points = new D2PointSpend();
                gehirn.gebietLeeren = true;
                objekte = new com.winlator.d2.test.Objekte(gehirn);
                gehirn.zwischenziele.add(objekte);
                gehirn.zwischenziele.add(new com.winlator.d2.test.Jagd(gehirn));
                gehirn.trinkenPct = Gehirn.trinkSchwelle(retreatPercent);
                gehirn.zielGebiet = s.levelNo;
                gehirn.kampfwahl.tabelle(host.skills());
                gehirn.zielwahlUebergehtIgnorierte = true;
                fahrer = new Fahrer(finger, (art, text) -> D2Trace.log("idle " + art + ": " + text));
                fahrer.verhalten.todWeiter = false;
                fahrer.verhalten.pauseBeenden = false;
                fahrer.verhalten.fensterSchliessen = false;
                rueckzug = new Rueckzug();
                right.remember(s.rightSkill, s.characterClass, link.characterName(), s.hotkeySkill, s.hotkeyLeft);
                trace("active in level " + level + ", seq " + s.sequence + ", right skill " + s.rightSkill);
                rolesBuild = null;
                tellBuild(s, link);
                host.redraw();
            }
            else if (now - since > PREPARE_MS) stop(host.text(R.string.idle_no_area_data));
            return;
        }
        if (phase == Phase.RETURNING) {

            if (fahrer != null) fahrer.loslassen("zurück, warte auf Gebietsdaten");
            if (ziele != null) {
                phase = Phase.ACTIVE;
                goalsOkAt = now;
                trace("active again after retreat " + rueckzug.anzahl());
                host.redraw();
            }
            else if (now - since > PREPARE_MS) stop(host.text(R.string.idle_no_area_data));
            return;
        }

        if (ziele != null) goalsOkAt = now;
        else if (now - goalsOkAt > NO_GOALS_MS) {
            stop(host.text(R.string.idle_no_area_data));
            return;
        }
        if (now - startedAt > durationMs) {
            stop(host.text(R.string.idle_time_up, durationMs / 60_000, kills));
            host.setPaused(true);
            return;
        }
        try {

            String line = D2SpieltestPort.toJson(s, now, link, host.leftOutObjects());
            Zustand z = Zustand.parse(new JSONObject(line), line);

            if (gehirn.kampfwahl.tabelle() == null) gehirn.kampfwahl.tabelle(host.skills());
            if (s.maxLife > 0 && s.lifePercent < retreatPercent) {
                Rueckzug.Grund why = rueckzug.warumNicht(z, s.portals, now);
                if (why != null) {

                    trace("no retreat through the portal: " + Rueckzug.text(why));
                    stop(host.text(R.string.idle_life_low, retreatPercent) + " (" + retreatText(why) + ")");
                    host.setPaused(true);
                    return;
                }
                fahrer.loslassen("Rückzug");
                rueckzug.starten(z, now);
                phase = Phase.RETREAT;
                since = now;
                trace("retreat " + rueckzug.anzahl() + " from level " + s.levelNo + " at life " + s.lifePercent + " %, portals " + s.portals);
                host.redraw();
                return;
            }

            if (Rueckzug.reparaturFaellig(z)) {
                Rueckzug.Grund why = rueckzug.warumNicht(z, s.portals, now);
                if (why == null) {
                    fahrer.loslassen("Rückzug");
                    rueckzug.starten(z, now);
                    phase = Phase.RETREAT;
                    since = now;
                    trace("retreat " + rueckzug.anzahl() + " to repair (a broken item) from level " + s.levelNo + ", portals " + s.portals);
                    host.redraw();
                    return;
                }
                if (now - repairNotedAt > 60_000) {
                    repairNotedAt = now;
                    trace("broken item, no retreat: " + Rueckzug.text(why));
                }
            }
            countKills(z);
            stepPoints(s, link, z, now, false);
            Zustand.Layout l = Zustand.Layout.parse(new JSONObject(host.layout()));
            fahrer.takt(now);
            if (!fahrer.vorbereiten(z, l, now, now)) return;
            if (ziele == null) {
                fahrer.loslassen("keine Gebietsdaten");
                return;
            }

            com.winlator.d2.test.Karte karte = host.link().mapFetch().karte();
            if (karte != null && gehirn.karte() != karte) gehirn.setKarte(karte);
            gehirn.layout = l;
            if (equipStep(link, z, now)) return;
            keyStep(link, now);
            Gehirn.Absicht a = gehirn.denken(z, ziele, now);
            traceNavigation();

            if (gehirn.gebietLeer(now)) {
                trace("level clear: " + gehirn.abdeckungText(now) + ", kills " + kills);
                stop(host.text(R.string.idle_area_clear, kills));
                host.setPaused(true);
                return;
            }
            if ("mana".equals(a.tippen)) manaWishAt = now;
            fahrer.anwenden(a, l, now);
            host.ignoreTargets(gehirn.ignorierte(now));
        }
        catch (Exception e) {
            stop(host.text(R.string.idle_error, e.getMessage()));
        }
    }


    private String lastNavigation = "";

    private void traceNavigation() {
        if (gehirn.navigation.equals(lastNavigation)) return;
        lastNavigation = gehirn.navigation;
        trace("navigation: " + lastNavigation);
    }


    private void keyStep(D2ModLink link, long now) {
        if (objekte == null || now - keyCheckedAt < 5000) return;
        keyCheckedAt = now;
        byte[] data = link.figur();
        boolean key = false;
        if (data != null) {
            try {
                String fl = D2SpieltestPort.figurJson(data, data.length, now);
                if (fl != null) key = com.winlator.d2.test.Objekte.hatSchluessel(com.winlator.d2.test.Zustand.Figur.parse(new JSONObject(fl)));
            }
            catch (org.json.JSONException e) {
                key = false;
            }
        }
        objekte.schluessel = key;
    }







    private boolean equipStep(D2ModLink link, Zustand z, long now) {
        D2Points points = link.points(3000);
        int caps = link.capabilities();
        com.winlator.d2.test.Zustand.Figur figur = null;
        byte[] data = anlegen.brauchtFigur(now) ? link.figur() : null;
        if (data != null) {
            try {
                String fl = D2SpieltestPort.figurJson(data, data.length, now);
                if (fl != null) figur = com.winlator.d2.test.Zustand.Figur.parse(new JSONObject(fl));
            }
            catch (org.json.JSONException e) {
                figur = null;
            }
        }
        com.winlator.d2.test.Ausruestung.Figur f = figur != null ? com.winlator.d2.test.Ausruestung.figur(figur, z.klasse,
            points != null && points.baseStatsKnown() ? points.baseStats : null) : null;
        com.winlator.d2.test.Anlegen.Schritt step = anlegen.schritt(host.tabellen(), figur, f, gehirn.imKampf(),
            caps >= 0 && (caps & D2Points.CAP_EQUIP) != 0, points != null ? points.equipCounter : -1,
            points != null ? points.equipResult : -1, now);
        equipBusy = anlegen.beschaeftigt(now);
        switch (step.art) {
            case SENDEN:
                equipBusy = true;
                fahrer.loslassen("Anlegen");
                trace("equip " + step.grund + " (id " + step.id + ")");
                link.equip(step.id, com.winlator.d2.test.Ausruestung.RECHTE_HAND);
                return true;
            case HALTEN:
                fahrer.loslassen("Anlegen");
                if (!step.grund.isEmpty()) trace("equip " + step.id + ": " + step.grund + ", holding still");
                return true;
            case ENDE:
                fahrer.loslassen("Anlegen");
                trace("equip ended: " + (step.keineAntwort ? "no answer, " : "item on the cursor, ") + step.grund);
                stop(step.keineAntwort ? host.text(R.string.idle_equip_no_answer) : host.text(R.string.idle_equip_stuck, step.grund));
                host.setPaused(true);
                return true;
            default:
                if (!step.grund.isEmpty()) trace("equip " + step.id + ": " + step.grund);
                return false;
        }
    }





    private void retreatStep(D2ModState s, D2ModLink link, long now) {
        if (s.life <= 0) {
            stop(host.text(R.string.idle_death));
            return;
        }
        if (host.pauseWanted() || s.paused()) {
            stop(host.text(R.string.idle_pause));
            return;
        }
        if (rueckzug.fensterErlaubt() ? otherWindowOpen(s) : windowOpen(s)) {
            stop(host.text(R.string.idle_window));
            return;
        }
        try {

            String line = D2SpieltestPort.toJson(s, now, link, host.leftOutObjects());
            Zustand z = Zustand.parse(new JSONObject(line), line);

            if (now - startedAt > durationMs) {
                if (z.stadt) {
                    rueckzug.abbrechen();
                    stop(host.text(R.string.idle_time_up, durationMs / 60_000, kills));
                    return;
                }
                if (!rueckzug.portalGenommen()) {
                    rueckzug.abbrechen();
                    stop(host.text(R.string.idle_time_up, durationMs / 60_000, kills));
                    host.setPaused(true);
                    return;
                }
            }
            Rueckzug.Ergebnis e = rueckzug.pruefen(z, now);
            if (e == Rueckzug.Ergebnis.ENDE_STADT) {
                trace("retreat ended in town: " + rueckzug.grund());
                stop(host.text(R.string.idle_retreat_ended, retreatText(rueckzug.grundArt())));
                return;
            }
            if (e == Rueckzug.Ergebnis.ENDE_DRAUSSEN) {
                trace("retreat ended outside: " + rueckzug.grund());
                stop(host.text(R.string.idle_retreat_ended, retreatText(rueckzug.grundArt())));
                host.setPaused(true);
                return;
            }
            if (e == Rueckzug.Ergebnis.ZURUECK) {
                fahrer.verhalten.fensterDurchlassen = false;
                phase = Phase.RETURNING;
                since = now;
                level = s.levelNo;
                trace("retreat " + rueckzug.anzahl() + " done, life " + s.lifePercent + " %");
                host.redraw();
                return;
            }


            if (z.stadt && link.figurAt() < levelChangedAt) {
                fahrer.allesLoslassen();
                if (now - levelChangedAt > FLAGS_AFTER_LOAD_MS) stop(host.text(R.string.idle_retreat_ended, host.text(R.string.idle_rz_no_town_data)));
                return;
            }
            Zustand.Figur f = null;
            byte[] data = link.figur();
            if (data != null) {
                String fl = D2SpieltestPort.figurJson(data, data.length, now);
                if (fl != null) f = Zustand.Figur.parse(new JSONObject(fl));
            }
            Zustand.Layout l = Zustand.Layout.parse(new JSONObject(host.layout()));
            fahrer.takt(now);
            fahrer.verhalten.fensterDurchlassen = rueckzug.fensterErlaubt();
            if (!fahrer.vorbereiten(z, l, now, now)) return;
            Gehirn.Absicht a = rueckzug.denken(z, f, now);
            if (a.modAuftrag != null) a.getippt = modOrder(link, a.modAuftrag);

            if (a.tippen == null && z.stadt && fahrer.leisteAufgeklappt && l.sichtbar("menu_toggle")) {
                a.tippen = "menu_toggle";
                a.grund = "fold the menu row back";
                fahrer.leisteAufgeklappt = false;
            }
            fahrer.anwenden(a, l, now);
        }
        catch (Exception ex) {
            stop(host.text(R.string.idle_error, ex.getMessage()));
        }
    }








    private void questStep(D2ModState s, D2ModLink link, long now) {
        if (D2IdleStart.pauseEndet(host.pauseWanted(), s.paused(), phase == Phase.PREPARING, now - since)) {
            trace("pause in " + phase + ": pauseWanted " + host.pauseWanted() + ", state paused " + s.paused()
                + ", seq " + s.sequence + " (at unpause " + seqAfterUnpause + ")");
            stop(host.text(R.string.idle_pause));
            return;
        }
        Zustand.Ziele goals = validGoals(link, s, now);
        if (goals != null) {
            questGoals = goals;
            questGoalsAt = now;
        }
        if (phase == Phase.PREPARING) {
            boolean fresh = (seqAfterUnpause < 0 || ((s.sequence - seqAfterUnpause) & 0xFFFF) != 0) && !s.paused();
            if (fresh && goals != null) questStart(s, link, now);
            else if (now - since > PREPARE_MS) stop(host.text(R.string.idle_no_area_data));
            return;
        }
        if (phase != Phase.ACTIVE) return;
        if (now - startedAt > durationMs) {
            stop(host.text(R.string.idle_time_up, durationMs / 60_000, kills));
            if (!s.inTown) host.setPaused(true);
            return;
        }
        try {
            String line = D2SpieltestPort.toJson(s, now, link, host.leftOutObjects());
            Zustand z = Zustand.parse(new JSONObject(line), line);
            if (gehirn.kampfwahl.tabelle() == null) gehirn.kampfwahl.tabelle(host.skills());
            questData(link, now);
            countKills(z);
            stepPoints(s, link, z, now, quest.punkteHalten());
            Zustand.Layout l = Zustand.Layout.parse(new JSONObject(host.layout()));
            fahrer.takt(now);
            boolean weiter = quest.vorSchritt(z, l, now);
            if (questBeendet) {
                questEnd(s);
                return;
            }
            if (!weiter) return;
            if (!fahrer.vorbereiten(z, l, now, now)) return;
            if (goals == null) {
                fahrer.loslassen("keine Gebietsdaten");
                if (now - questGoalsAt > QUEST_NO_GOALS_MS) stop(host.text(R.string.idle_no_area_data));
                return;
            }
            com.winlator.d2.test.Karte karte = link.mapFetch().karte();
            if (karte != null && gehirn.karte() != karte) gehirn.setKarte(karte);
            gehirn.layout = l;
            if (!quest.imStadtgang() && !quest.imRueckzug() && !z.stadt && equipStep(link, z, now)) return;
            keyStep(link, now);
            Gehirn.Absicht a = quest.denken(z, goals, questFigur, now);
            traceNavigation();
            if (questLeeren.gebietLeerEnde(quest.imStadtgang(), l, now)) {
                if (questBeendet) questEnd(s);
                return;
            }
            if ("mana".equals(a.tippen)) manaWishAt = now;
            fahrer.anwenden(a, l, now);
            host.ignoreTargets(gehirn.ignorierte(now));
        }
        catch (Exception e) {
            stop(host.text(R.string.idle_error, e.getMessage()));
        }
    }


    private void questStart(D2ModState s, D2ModLink link, long now) {
        phase = Phase.ACTIVE;
        startedAt = goalsOkAt = questGoalsAt = now;
        kills = 0;
        seenAlive.clear();
        counted.clear();
        questBeendet = false;
        questOhneDurchgaenge = false;
        questFigur = null;
        questFigurAt = 0;
        gehirn = new Gehirn(now);
        anlegen = new com.winlator.d2.test.Anlegen();
        equipBusy = false;
        points = new D2PointSpend();
        gehirn.trinkenPct = Gehirn.trinkSchwelle(retreatPercent);
        gehirn.kampfwahl.tabelle(host.skills());
        gehirn.zielwahlUebergehtIgnorierte = true;

        objekte = new com.winlator.d2.test.Objekte(gehirn);
        keyCheckedAt = 0;
        gehirn.zwischenziele.add(objekte);

        gehirn.zwischenziele.add(new com.winlator.d2.test.Jagd(gehirn));

        fahrer = new Fahrer(finger, (art, text) -> D2Trace.log("idle " + art + ": " + text));
        Stadtgang stadtgang = new Stadtgang();
        stadtgang.alleMs = 0;
        stadtgang.minHeil = 0;
        questLeeren = new LeerenLauf(gehirn, fahrer, new LeerenLauf.Umgebung() {
            @Override public void event(String art, String text) { trace("quest " + art + ": " + text); }
            @Override public Zustand.Ziele ziele() { return questGoals; }
            @Override public long zieleAm() { return questGoalsAt; }
            @Override public int portale() { D2ModState st = host.state(); return st != null ? st.portals : -1; }
            @Override public Zustand.Figur figur() { return questFigur; }
            @Override public long totSeit() { return SystemClock.uptimeMillis(); }
            @Override public int kills() { return kills; }
            @Override public String gebietName(int gebiet) { return Integer.toString(gebiet); }
            @Override public void ohneDurchgaenge(boolean ohne) { questOhneDurchgaenge = ohne; }
            @Override public boolean leerenBeenden(String grund, boolean pause, Zustand.Layout l) { return quest.questLeerenEnde(grund); }
            @Override public boolean modAuftrag(String was) { return modOrder(link, was); }
        });
        questLeeren.rueckzugPct = retreatPercent;
        quest = new QuestLauf(questZeile, QUEST_MAX_TODE, gehirn, fahrer, stadtgang, questLeeren, new QuestLauf.Umgebung() {
            @Override public void event(String art, String text) { trace("quest " + art + ": " + text); }
            @Override public Zustand.Figur figur() { return questFigur; }
            @Override public boolean testfigur(Zustand.Figur f) { return true; }
            @Override public void fremdeFigur(Zustand.Figur f, Zustand.Layout l) { }
            @Override public java.util.Set<Long> getoetet() { return counted; }
            @Override public void beenden() { questBeendet = true; }
        });
        right.remember(s.rightSkill, s.characterClass, link.characterName(), s.hotkeySkill, s.hotkeyLeft);
        trace("quest mode active: " + questZeile + ", seq " + s.sequence);
        rolesBuild = null;
        tellBuild(s, link);
        host.redraw();
    }


    private void questData(D2ModLink link, long now) throws org.json.JSONException {
        long at = link.figurAt();
        if (at != questFigurAt) {
            byte[] data = link.figur();
            if (data != null) {
                String fl = D2SpieltestPort.figurJson(data, data.length, now);
                if (fl != null) {
                    questFigur = Zustand.Figur.parse(new JSONObject(fl));
                    questFigurAt = at;
                }
            }
        }
        if (questFigur != null) questFigur.ms = questFigurAt;

        quest.paket(link.questWorte(), link.questsAt());
    }


    private void questEnd(D2ModState s) {
        QuestFahrer f = quest.questFahrer;
        String name = questName(questZeile);
        String why;
        QuestFahrer.Ende code = f.endeCode;
        if (code == null) why = host.text(R.string.idle_quest_stopped, name);
        else {
            switch (code) {
                case ABGESCHLOSSEN: why = host.text(R.string.idle_quest_done, name); break;
                case SCHON_ERLEDIGT: why = host.text(R.string.idle_quest_already, name); break;
                case ZU_OFT_GESTORBEN: why = host.text(R.string.idle_quest_died, name); break;
                case NICHT_LEER: why = host.text(R.string.idle_quest_not_clear, name); break;
                case KEINE_BELOHNUNG: why = host.text(R.string.idle_quest_no_reward, name, questZeile.abgabe); break;
                case NICHT_ERREICHT: why = host.text(R.string.idle_quest_not_reached, name, questZeile.abgabe); break;
                case ZEIT_UM: why = host.text(R.string.idle_quest_stuck, name, QuestFahrer.HINGEHEN_OHNE_WECHSEL_MS / 60_000); break;
                case KAPUTT: why = host.text(R.string.idle_retreat_ended, host.text(R.string.idle_rz_broken)); break;
                default: why = host.text(R.string.idle_quest_stopped, name); break;
            }
        }
        trace("quest ended: " + f.ende + " (" + code + ")");
        stop(why);

        if (!s.inTown && code != QuestFahrer.Ende.ZU_OFT_GESTORBEN) host.setPaused(true);
    }


    String questPreview() {
        D2ModState s = host.state();
        if (s == null || !s.inTown) return null;
        int[] w = host.link().questWorte();
        if (Quests.start(true, s.levelNo, w) != Quests.Start.QUEST) return null;
        return questName(Quests.naechsteOffene(w));
    }


    private String questName(QuestZeile z) {
        if (z.akt == 1 && z.nummer == 1) return host.text(R.string.idle_quest_name_a1q1);
        return z.toString();
    }





    private static boolean modOrder(D2ModLink link, String order) {
        String[] w = order.split(" ");
        if (link == null || w.length != 2) return false;
        try {
            long n = Long.parseLong(w[1]);
            trace("order " + order);
            if (w[0].equals("npc") && n >= 0 && n < 16) {
                link.npc((int) n);
                return true;
            }
            if (w[0].equals("reparieren") && n > 0) {
                link.repair(n);
                return true;
            }
        }
        catch (NumberFormatException ignored) {}
        return false;
    }


    private String retreatText(Rueckzug.Grund g) {
        if (g == null) return "";
        switch (g) {
            case KEIN_PORTAL: return host.text(R.string.idle_rz_no_portal);
            case NUR_AKT_1_2: return host.text(R.string.idle_rz_act);
            case ZU_OFT: return host.text(R.string.idle_rz_too_often);
            case GEBIET_VERLASSEN: return host.text(R.string.idle_left_area);
            case NICHT_GEHEILT: return host.text(R.string.idle_rz_not_healed);
            case NICHT_AM_PORTAL: return host.text(R.string.idle_rz_not_at_portal);
            case KAPUTT: return host.text(R.string.idle_rz_broken);
            case STADT_OHNE_PORTAL: case STADT_VERLASSEN_OHNE_PORTAL: case FALSCHE_STADT: return host.text(R.string.idle_rz_wrong_way);
            default: return host.text(R.string.idle_rz_round_failed);
        }
    }


    private void makeUpPause(long now) {
        if (!pauseLater) return;
        if (now > pauseLaterUntil) {
            pauseLater = false;
            trace("pause later dropped: time up");
            return;
        }
        D2ModState s = host.state();
        if (s == null) return;
        pauseLater = false;
        if (s.inTown) {
            trace("pause later dropped: in town");
            return;
        }
        trace("pause made up for");
        host.setPaused(true);
    }





    private static Zustand.Ziele validGoals(D2ModLink link, D2ModState s, long now) {
        byte[] data = link.goals();
        if (data == null) return null;
        String line = D2SpieltestPort.goalsJson(data, data.length, now);
        if (line == null) return null;
        try {
            Zustand.Ziele z = Zustand.Ziele.parse(new JSONObject(line));
            return z.gebiet == s.levelNo && z.bereit == 2 ? z : null;
        }
        catch (Exception e) {
            return null;
        }
    }

    private void countKills(Zustand z) {
        for (Zustand.Ding m : z.monster) if (m.leben > 0) seenAlive.add(m.id);
        for (Zustand.Ding d : z.leichen) if (seenAlive.contains(d.id) && counted.add(d.id)) kills++;
    }





    private void stepPoints(D2ModState s, D2ModLink link, Zustand z, long now, boolean hold) {
        boolean enemyNear = false;
        for (Zustand.Ding d : z.feinde()) {
            if (z.abstand(d) < POINTS_NO_ENEMY) {
                enemyNear = true;
                break;
            }
        }
        String name = link.characterName();
        D2PointPlan.Build build = name != null ? host.build(s.characterClass, name) : null;
        D2Points p = link.points(1500);
        points.step(build, host.skills(), p, !enemyNear && !hold, now, pointsOut);

        if (p != null && (build != rolesBuild || p.level != rolesLevel)) {
            rolesBuild = build;
            rolesLevel = p.level;
            gehirn.kampfwahl.rollen(build != null ? D2PointPlan.rollen(build, p.level) : null);
            trace("roles of " + (build != null ? build.id : "no build") + " at level " + p.level);
        }
    }


    private void tellBuild(D2ModState s, D2ModLink link) {
        String name = link.characterName();
        D2PointPlan.Build build = name != null && s.characterClass >= 0 ? host.build(s.characterClass, name) : null;
        if (build == null) return;
        if (!host.buildChosen(s.characterClass, name)) host.message(host.text(R.string.idle_build_suggested));

        else if (D2PointPlan.IDLE_NO.equals(build.idle)) {
            host.message(host.text(com.winlator.d2.test.Kampfwahl.eigeneListe(s.characterClass) ? R.string.idle_build_not_played
                : R.string.idle_build_not_played_attack));
        }
    }

    private final D2PointSpend.Out pointsOut = new D2PointSpend.Out() {
        @Override
        public void send(int kind, int what) {
            trace("points: " + (kind == D2Points.KIND_SKILL ? "skill " : "stat ") + what + " (" + points + ")");
            host.link().points(kind, what, -1);
        }

        @Override
        public void log(String text) {
            trace(text);
        }
    };


    private String pointsNote() {
        D2Points p = host.link().points(3000);
        boolean open = p != null && (p.skillPoints > 0 || p.statPoints > 0);
        String why = points.why();
        String spent = points.skillsSpent() + points.statsSpent() > 0
            ? host.text(R.string.idle_points_spent, points.skillsSpent(), points.statsSpent()) : null;
        String left = open && why != null ? host.text(R.string.idle_points_open, host.text("no build".equals(why)
            ? R.string.idle_points_no_build : "no effect".equals(why) ? R.string.idle_points_no_effect
            : R.string.idle_points_not_possible)) : null;
        return spent == null ? left : left == null ? spent : spent + ", " + left;
    }


    void stop(String why) {
        if (phase == Phase.OFF) return;
        end(host.text(R.string.idle_ended, why));
    }





    void stopInBackground(String why) {
        if (phase == Phase.OFF) return;
        end(host.text(R.string.idle_ended, why), true);
    }

    private void end(String text) {
        end(text, false);
    }

    private void end(String text, boolean unseen) {
        Phase was = phase;
        String pointsText = was == Phase.OFF || was == Phase.WANTED ? null : pointsNote();
        if (pointsText != null) text += " · " + pointsText;
        boolean wasRunning = running();
        phase = Phase.OFF;
        finger.releaseAll();
        host.ignoreTargets(null);
        if (wasRunning) host.link().setIdleData(false);
        if (rueckzug != null) rueckzug.abbrechen();

        if (gehirn != null) right.owe(SystemClock.uptimeMillis(), TELL_LATER_MS);
        gehirn = null;
        fahrer = null;
        questZeile = null;
        quest = null;
        questLeeren = null;
        reason = text;
        trace("end in " + was + (unseen ? " (background)" : "") + ": " + text);
        host.message(text);

        if (unseen || !focused) untold.queue(text, SystemClock.elapsedRealtime());
        if (!unseen) restoreRight(SystemClock.uptimeMillis());
        host.redraw();
    }


    private void restoreRight(long now) {
        if (!right.pending()) return;
        D2ModState s = host.state();
        D2IdleRight.Result r = s != null
            ? right.step(now, focused, true, s.rightSkill, s.characterClass, host.link().characterName(), s.hotkeySkill, s.hotkeyLeft, rightActions)
            : right.step(now, focused, false, -1, -1, null, null, null, rightActions);
        if (r == D2IdleRight.Result.FAILED) host.message(host.text(R.string.idle_skill_not_restored));
    }


    private static void trace(String text) {
        D2Trace.log("idle: " + text);
        android.util.Log.i("D2Idle", text);
    }


    String board() {

        if (phase == Phase.PREPARING) return host.text(R.string.idle_board_preparing);
        if (phase == Phase.RETREAT || phase == Phase.RETURNING) return host.text(R.string.idle_board_retreat, rueckzug.anzahl());
        if (phase != Phase.ACTIVE) return null;
        long min = (SystemClock.uptimeMillis() - startedAt) / 60_000;
        return host.text(R.string.idle_board, min, durationMs / 60_000, kills);
    }
}
