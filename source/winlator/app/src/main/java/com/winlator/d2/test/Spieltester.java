package com.winlator.d2.test;

import android.os.Looper;
import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;









public final class Spieltester {




    static final String PACKAGE = paket();
    static final String STOP_FILE = "/data/local/tmp/spieltest.stop";

    private static final String STOP = System.getProperty("d2t.stop", STOP_FILE);
    private static final String STATUS = System.getProperty("d2t.status", "/data/local/tmp/spieltest.status");




    static final String LAEUFT = "/data/local/tmp/d2android-spieltester.laeuft";
    private long laeuftAm;




    static final int PORT = System.getProperty("d2t.wine") != null ? ModPorts.SPIELTEST : 7960;
    private static final int F_JOYSTICK = Fahrer.F_JOYSTICK, F_ANGRIFF = Fahrer.F_ANGRIFF;
    private static final Map<Integer, String> GEBIETE = new HashMap<>();
    static {
        GEBIETE.put(1, "Lager der Jägerinnen");
        GEBIETE.put(2, "Blutmoor");
        GEBIETE.put(3, "Kalte Ebene");
        GEBIETE.put(4, "Steinfeld");
        GEBIETE.put(5, "Dunkler Wald");
        GEBIETE.put(8, "Höhle des Bösen 1");
        GEBIETE.put(17, "Friedhof");
    }

    private final File dir;
    private final long ende;
    private final Writer events;
    private final ArrayDeque<String> lastStates = new ArrayDeque<>(), lastTrace = new ArrayDeque<>(), lastModLog = new ArrayDeque<>();
    private volatile Zustand zustand;

    private volatile Zustand.Ziele ziele;

    private final Writer modLog;

    private long inStadtSeit = -1;
    private final List<Long> lagerZeiten = new ArrayList<>();
    private int zieleGebiet = -1;
    private String letzteNavigation = "";
    private int kampfMeldungen;
    private volatile Zustand.Layout layout = new Zustand.Layout();
    private volatile long zustandAm, verbundenAm;
    private volatile boolean vordergrund = true, beenden;
    private volatile String modStatus = "";
    private final Finger touch;


    private final WineFinger wine;

    private final Fahrer fahrer;
    private final Gehirn gehirn;
    private final Waechter waechter = new Waechter();
    private final List<String> befunde = new ArrayList<>();
    private String letzterPufferBefund;

    private boolean tastenGemeldet;

    private com.winlator.d2.D2Tabellen tabellen;
    private final Anlegen anlegen = new Anlegen();
    private final Set<Long> lebendGesehen = new HashSet<>(), getoetet = new HashSet<>();
    private final Map<Integer, Long> zeitInGebiet = new HashMap<>();
    private final long start = SystemClock.uptimeMillis();
    private final String startZeit;
    private int befundNr, bildNr, zustaende;
    private int startGebiet = -1, letztesGebiet = -1;
    private long letzterZustandAm;
    private long letzterBericht, letztesBild, ohneZustandGemeldet, letzteSeq = -1;

    private boolean nurLager;

    private volatile Zustand.Figur figur;

    private String testfigur = "st-zauberin";

    private final Stadtgang stadtgang = new Stadtgang();




    private final Wegpunkte wegpunkte = new Wegpunkte();
    private String letzteWegpunktMeldung = "";
    private String letzteStadtMeldung = "";

    private final LeerenLauf ll;
    static final int RUECKZUG_PCT = 25;

    private long zieleAm;
    private String leerenEnde = "";

    private volatile int portale = -1;

    private QuestFahrer questFahrer;



    private QuestLauf quest;





    private Writer portMitschrift;




    private Auftrag auftrag;
    private long auftragBis, auftragStatusAm;
    private String auftragStatus = "", endeGrund = "";
    private final boolean abspielModus;
    private final Writer fingerMitschrift;


    private Spieltester(File dir, int minuten, long seed) throws Exception {
        this(dir, minuten, seed, false);
    }

    private Spieltester(File dir, int minuten, long seed, boolean abspielen) throws Exception {
        this.dir = dir;
        ende = start + minuten * 60_000L;
        startZeit = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date());
        events = new FileWriter(new File(dir, "ereignisse.jsonl"), true);
        modLog = new FileWriter(new File(dir, "mod.log"), true);
        abspielModus = abspielen;
        fingerMitschrift = abspielen ? new FileWriter(new File(dir, "finger.txt")) : null;
        wine = !abspielen && System.getProperty("d2t.wine") != null ? new WineFinger(this::event) : null;
        touch = abspielen ? new AufzeichnungsFinger(fingerMitschrift, start) : wine != null ? wine : new Touch();
        fahrer = new Fahrer(touch, this::event);
        gehirn = new Gehirn(seed);
        ll = new LeerenLauf(gehirn, fahrer, new LeerenLauf.Umgebung() {
            @Override public void event(String art, String text) { Spieltester.this.event(art, text); }
            @Override public Zustand.Ziele ziele() { return ziele; }
            @Override public long zieleAm() { return zieleAm; }
            @Override public int portale() { return portale; }
            @Override public Zustand.Figur figur() { return figur; }
            @Override public long totSeit() { return waechter.totSeit(); }
            @Override public int kills() { return getoetet.size(); }
            @Override public String gebietName(int gebiet) { return Spieltester.gebietName(gebiet); }
            @Override public void ohneDurchgaenge(boolean ohne) { if (wine != null) wine.ohneDurchgaenge = ohne; }
            @Override public boolean leerenBeenden(String grund, boolean pause, Zustand.Layout l) throws InterruptedException {
                return Spieltester.this.leerenBeenden(grund, pause, l);
            }
        });
        if (wine != null) gehirn.kampfwahl.tabelle(wineFertigkeiten(System.getProperty("d2t.spiel")));
        if (wine != null) wine.fertigkeiten = gehirn.kampfwahl.tabelle();
        if (wine != null) tabellen = wineTabellen(System.getProperty("d2t.spiel"));
        gehirn.zielwahlUebergehtIgnorierte = wine != null;
    }






    private com.winlator.d2.D2Tabellen wineTabellen(String spiel) {
        if (spiel == null) return null;
        try (com.winlator.d2.D2Sprites s = new com.winlator.d2.D2Sprites(new File(spiel))) {
            com.winlator.d2.D2Tabellen t = com.winlator.d2.D2Tabellen.lesen(s.read("data\\global\\excel\\weapons.bin"),
                s.read("data\\global\\excel\\itemtypes.bin"));
            event("anlegen", t == null ? "weapons.bin/itemtypes.bin nicht lesbar: kein Anlegen" : "Waffentabellen gelesen");
            return t;
        }
        catch (Exception e) {
            event("anlegen", "Waffentabellen: " + e + ": kein Anlegen");
            return null;
        }
    }

    private Fertigkeiten wineFertigkeiten(String spiel) {
        if (spiel == null) {
            event("fertigkeiten", "kein Spielordner (-Dd2t.spiel): keine Fertigkeiten");
            return null;
        }
        try (com.winlator.d2.D2Sprites s = new com.winlator.d2.D2Sprites(new File(spiel))) {
            Fertigkeiten f = Fertigkeiten.lesen(s.read("data\\global\\excel\\skills.bin"));
            event("fertigkeiten", f == null ? "skills.bin nicht lesbar: keine Fertigkeiten" : f.anzahl() + " Fertigkeiten aus " + spiel);
            return f;
        }
        catch (Exception e) {
            event("fertigkeiten", "skills.bin: " + e + ": keine Fertigkeiten");
            return null;
        }
    }

    public static void main(String[] args) throws Exception {
        if (Looper.myLooper() == null) Looper.prepareMainLooper();
        int minuten = 30, zielGebiet = 3, quest = 0;
        boolean nurLager = false, leeren = false;
        int rueckzug = RUECKZUG_PCT;
        String testfigur = "st-zauberin";
        String build = null;
        long stadtgangAlle = 0;
        int wegpunkt = -1;
        long seed = System.currentTimeMillis();
        String aus = null;
        boolean mitschreiben = false;
        File abspielen = null;
        String ziel = null;
        int zeit = 180;
        boolean friedlich = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--minuten") && i + 1 < args.length) minuten = Integer.parseInt(args[++i]);
            else if (args[i].equals("--mitschreiben")) mitschreiben = true;
            else if (args[i].equals("--abspielen") && i + 1 < args.length) abspielen = new File(args[++i]);
            else if (args[i].equals("--aus") && i + 1 < args.length) aus = args[++i];
            else if (args[i].equals("--seed") && i + 1 < args.length) seed = Long.parseLong(args[++i]);
            else if (args[i].equals("--bis") && i + 1 < args.length) zielGebiet = Integer.parseInt(args[++i]);
            else if (args[i].equals("--nur-lager")) nurLager = true;
            else if (args[i].equals("--figur") && i + 1 < args.length) testfigur = args[++i];
            else if (args[i].equals("--build") && i + 1 < args.length) build = args[++i];
            else if (args[i].equals("--stadtgang-alle") && i + 1 < args.length) stadtgangAlle = Long.parseLong(args[++i]) * 60_000L;
            else if (args[i].equals("--leeren")) leeren = true;
            else if (args[i].equals("--wegpunkt") && i + 1 < args.length) wegpunkt = Integer.parseInt(args[++i]);
            else if (args[i].equals("--rueckzug") && i + 1 < args.length) rueckzug = Integer.parseInt(args[++i]);
            else if (args[i].equals("--quest") && i + 1 < args.length) quest = Integer.parseInt(args[++i]);
            else if (args[i].equals("--ziel") && i + 1 < args.length) ziel = args[++i];
            else if (args[i].equals("--zeit") && i + 1 < args.length) zeit = Integer.parseInt(args[++i]);
            else if (args[i].equals("--friedlich")) friedlich = true;
            else if (args[i].equals("--probe")) {
                probe();
                return;
            }
        }
        Auftrag auftrag = null;
        if (ziel != null) {

            try {
                auftrag = Auftrag.lesen(ziel);
            }
            catch (IllegalArgumentException e) {
                System.out.println("--ziel: " + e.getMessage());
                writeStatus("beendet · ergebnis falscher Auftrag: " + e.getMessage().split("\n")[0]);
                System.exit(3);
                return;
            }
            minuten = zeit / 60 + 2;
        }
        if (aus == null) {
            aus = "/sdcard/Download/D2Android/Spieltest/" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        }
        File dir = new File(aus);
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("kann " + dir + " nicht anlegen");
        List<String> abspielZeilen = null;
        if (abspielen != null) {

            abspielZeilen = java.nio.file.Files.readAllLines(abspielen.toPath(), StandardCharsets.UTF_8);
            if (abspielZeilen.isEmpty()) throw new IOException(abspielen + " ist leer");
            uhrStellen(Long.parseLong(abspielZeilen.get(0).substring(0, abspielZeilen.get(0).indexOf('\t'))));
            seed = abspielSeed(args, seed);
        }
        else new File(STOP).delete();
        if (!testfigur.startsWith(Zustand.Figur.TEST_PREFIX)) {
            System.out.println("--figur muss mit " + Zustand.Figur.TEST_PREFIX + " beginnen: " + testfigur);
            return;
        }
        QuestZeile questZeile = quest > 0 ? Quests.akt1(quest) : null;
        if (quest > 0 && (questZeile == null || !questZeile.kann())) {

            System.out.println("--quest " + quest + ": der Quest-Fahrer kann in Akt 1 bisher " + Quests.AKT1);
            return;
        }
        if (wegpunkt != -1 && !Wegpunkte.kursMoeglich(wegpunkt)) {

            System.out.println("--wegpunkt " + wegpunkt + ": kein Wegpunkt in Akt 1 außerhalb des Lagers (möglich: 3, 4, 5, 6, 27, 29, 32, 35)");
            return;
        }
        Spieltester t = new Spieltester(dir, minuten, seed, abspielZeilen != null);
        if (mitschreiben && abspielZeilen == null) t.portMitschrift = new FileWriter(new File(dir, "port.jsonl"));
        t.gehirn.zielGebiet = zielGebiet;
        t.nurLager = nurLager;
        t.testfigur = testfigur;
        t.stadtgang.alleMs = stadtgangAlle;
        if (leeren || questZeile != null) t.wieIdle(rueckzug);
        if (questZeile != null) t.alsQuest(questZeile);
        if (wegpunkt > 0) {
            t.wegpunkte.kursZiel = wegpunkt;
            t.gehirn.zielGebiet = wegpunkt;
            t.stadtgang.alleMs = 0;
            t.stadtgang.minHeil = 0;
        }
        if (auftrag != null) {

            t.auftrag = auftrag;
            t.auftragBis = t.start + zeit * 1000L;
            t.stadtgang.alleMs = 0;
            t.stadtgang.minHeil = 0;
            t.gehirn.aufheben = false;
            t.gehirn.friedlich = friedlich;
        }
        if (build != null) t.build(build);
        if (abspielZeilen != null) t.abspielen(abspielZeilen);
        else t.run();
    }


    private static long abspielSeed(String[] args, long seed) {
        for (String a : args) if (a.equals("--seed")) return seed;
        return 1;
    }


    private static void uhrStellen(long ms) {
        try {
            SystemClock.class.getMethod("fest", long.class).invoke(null, ms);
        }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("--abspielen nur unter der JVM mit den Test-Stubs", e);
        }
    }





    private void abspielen(List<String> zeilen) throws Exception {
        event("start", "Abspielen: " + zeilen.size() + " Port-Zeilen, " + (ende - start) / 60000 + " min, "
            + (questFahrer != null ? "Quest " + questFahrer.zeile.nummer + " " + questFahrer.zeile.name
                : ll.leeren ? "Gebiet leeren wie der Idle-Modus" : "bis Gebiet " + gehirn.zielGebiet));
        int i = 0;
        long now = start;
        try {
            while (!beenden) {
                while (i < zeilen.size()) {
                    String z = zeilen.get(i);
                    int tab = z.indexOf('\t');
                    long am = Long.parseLong(z.substring(0, tab));
                    if (am > now) break;
                    uhrStellen(am);
                    zeile(z.substring(tab + 1));
                    i++;
                }
                if (i >= zeilen.size()) {
                    event("ende", "Abspielen: keine Zeilen mehr");
                    break;
                }
                uhrStellen(now);
                if (now >= ende) {
                    event("ende", "Zeit um");
                    break;
                }
                schritt(now);
                now += 40;
            }
        }
        catch (Throwable e) {
            event("fehler", String.valueOf(e));
        }
        finally {
            fahrer.allesLoslassen();
            bericht(true);
            events.close();
            fingerMitschrift.close();
            modLog.close();
        }
    }





    private void build(String id) throws IOException {
        File f = new File(System.getProperty("d2t.wine", "."), "../../winlator/app/src/main/assets/builds.json");
        String json = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        for (com.winlator.d2.D2PointPlan.Build b : com.winlator.d2.D2PointPlan.parse(json)) {
            if (!b.id.equals(id)) continue;
            build = b;
            event("build", id);
            return;
        }
        throw new IOException("Build unbekannt: " + id);
    }






    private void rollenSchritt(Zustand z) {
        if (build == null) return;
        if (z.stufe > 0 && z.stufe != buildStufe) {
            buildStufe = z.stufe;
            gehirn.kampfwahl.rollen(com.winlator.d2.D2PointPlan.rollen(build, z.stufe));
            event("build", build.id + ", Rollen für Stufe " + z.stufe);
        }
        if (wine == null) return;
        long now = SystemClock.uptimeMillis();
        com.winlator.d2.D2Points p = punkteFrisch.block(z, now);
        boolean feindNah = false;
        for (Zustand.Ding d : z.feinde()) feindNah |= z.abstand(d) < 500;

        boolean halten = quest != null && quest.punkteHalten();
        if (halten != punkteGehalten) {
            punkteGehalten = halten;
            event("punkte", halten ? "Punkte warten: Quest-Abgabe läuft" : "Punkte wieder frei");
        }
        punkte.step(build, gehirn.kampfwahl.tabelle(), p, !feindNah && !halten, now, punkteAus);
        Zustand.Figur f = figur;
        tasten.step(build, p, z.klasse, f != null && !f.name.isEmpty() ? f.name : null, z.fkFertigkeit, true, now, tastenAus);
    }

    private final PunkteFrisch punkteFrisch = new PunkteFrisch();
    private boolean punkteGehalten;
    private final com.winlator.d2.D2PointSpend punkte = new com.winlator.d2.D2PointSpend();
    private final com.winlator.d2.D2PointAuto tasten = new com.winlator.d2.D2PointAuto();
    private final com.winlator.d2.D2PointSpend.Out punkteAus = new com.winlator.d2.D2PointSpend.Out() {
        @Override
        public void send(int kind, int what) {
            wine.punkte(com.winlator.d2.D2Points.command(kind, what, -1), (kind == 1 ? "Fertigkeitspunkt " : "Wertpunkt ") + what);
            event("punkte", (kind == 1 ? "Fertigkeitspunkt " : "Wertpunkt ") + what + " (" + punkte + ")");
        }

        @Override
        public void log(String text) {
            event("punkte", text);
        }
    };
    private final com.winlator.d2.D2PointAuto.Out tastenAus = new com.winlator.d2.D2PointAuto.Out() {
        @Override
        public void bind(int skill, int key) {
            wine.punkte(com.winlator.d2.D2Points.command(com.winlator.d2.D2Points.KIND_KEY, skill, key), "Fertigkeit " + skill + " auf F" + (key + 1));
            event("taste", "Fertigkeit " + skill + " auf F" + (key + 1));
        }

        @Override
        public void noKey(int skill) {
            event("taste", "Fertigkeit " + skill + " gelernt: keine F-Taste frei");
        }
    };

    private com.winlator.d2.D2PointPlan.Build build;
    private int buildStufe = -1;


    private void wieIdle(int rueckzug) {
        ll.leeren = true;
        ll.rueckzugPct = rueckzug;
        gehirn.gebietLeeren = true;
        gehirn.trinkenPct = Gehirn.trinkSchwelle(rueckzug);
        fahrer.verhalten.todWeiter = false;
        fahrer.verhalten.pauseBeenden = false;
        fahrer.verhalten.fensterSchliessen = false;


        stadtgang.alleMs = 0;
        stadtgang.minHeil = 0;
    }






    private void alsQuest(QuestZeile zeile) {
        quest = new QuestLauf(zeile, gehirn, fahrer, stadtgang, ll, new QuestLauf.Umgebung() {
            @Override public void event(String art, String text) { Spieltester.this.event(art, text); }
            @Override public Zustand.Figur figur() { return figur; }
            @Override public boolean testfigur(Zustand.Figur f) { return f.istTestfigur(testfigur); }
            @Override public void fremdeFigur(Zustand.Figur f, Zustand.Layout l) throws InterruptedException { Spieltester.this.fremdeFigur(f, l); }
            @Override public Set<Long> getoetet() { return getoetet; }
            @Override public void beenden() { beenden = true; }
        });
        questFahrer = quest.questFahrer;
        ll.leeren = false;
    }


    private static void probe() throws Exception {
        if (!PACKAGE.equals(resumedPackage())) {
            System.out.println("D2-App nicht im Vordergrund: " + resumedPackage());
            return;
        }
        Touch t = new Touch();
        t.down(F_JOYSTICK, 370, 760);
        Thread.sleep(50);
        t.move(F_JOYSTICK, 450, 760);
        t.move(F_JOYSTICK, 530, 760);
        Thread.sleep(300);
        t.down(F_ANGRIFF, 2180, 756);
        Thread.sleep(3000);
        t.releaseAll();
        System.out.println("probe ok");
    }

    private void run() throws Exception {
        event("start", "Spieltester v1, " + (ende - start) / 60000 + " min, "
            + (questFahrer != null ? "Quest " + questFahrer.zeile.nummer + " " + questFahrer.zeile.name + " (Rückzug unter " + ll.rueckzugPct + " %)"
                : ll.leeren ? "Gebiet leeren wie der Idle-Modus (Rückzug unter " + ll.rueckzugPct + " %)"
                : auftrag != null ? "Auftrag \"" + auftrag.text + "\" (" + (auftragBis - start) / 1000 + " s" + (gehirn.friedlich ? ", friedlich" : "") + ")"
                : "bis Gebiet " + gehirn.zielGebiet)
            + (nurLager ? ", nur Lager verlassen" : "") + ", Ordner " + dir);
        Thread reader = new Thread(this::readLoop, "port");
        reader.setDaemon(true);
        reader.start();
        Thread fg = new Thread(this::foregroundLoop, "vordergrund");
        fg.setDaemon(true);
        fg.start();
        try {
            while (!beenden) {
                long now = SystemClock.uptimeMillis();
                if (now >= ende) {
                    event("ende", "Zeit um");
                    endeGrund = "Zeit des Laufs um";
                    break;
                }
                if (new File(STOP).exists()) {
                    event("ende", "Stopp-Datei");
                    endeGrund = "Stopp-Datei";
                    break;
                }
                if (wine == null && !abspielModus && now - laeuftAm >= 1000) {
                    laeuftAm = now;
                    laeuftMarke();
                }
                schritt(now);
                if (now - letzterBericht > 30_000) {
                    letzterBericht = now;
                    bericht(false);
                }
                Thread.sleep(40);
            }
        }
        catch (Throwable e) {
            event("fehler", String.valueOf(e));
            endeGrund = "Fehler " + e;
        }
        finally {
            try {
                fahrer.allesLoslassen();
            }
            catch (Throwable ignored) {}
            if (auftrag != null && !auftrag.fertig()) {
                auftrag.ende(Auftrag.Ergebnis.ABGEBROCHEN, endeGrund.isEmpty() ? "Lauf beendet" : endeGrund);
                event("auftrag", auftrag.status());
            }
            if (wine == null && !abspielModus) new File(LAEUFT).delete();
            bericht(true);
            events.close();
            try {
                modLog.close();
            }
            catch (IOException ignored) {}
            new File(STOP).delete();
        }
        System.exit(auftrag != null && auftrag.fertig() ? auftrag.ergebnis.exitCode : 0);
    }



    private void schritt(long now) throws Exception {
        fahrer.takt(now);
        if (auftrag != null && !auftrag.fertig() && now >= auftragBis) {

            auftrag.ende(Auftrag.Ergebnis.ZEIT_UM, (auftragBis - start) / 1000 + " s, zuletzt " + auftrag.unterwegs);
            auftragMelden(now);
            fahrer.allesLoslassen();
            beenden = true;
            return;
        }
        if (!vordergrund) {
            loslassen("D2-App nicht im Vordergrund");
            return;
        }
        Zustand z = zustand;
        if (z == null || now - zustandAm > 1000) {
            loslassen("kein Zustand (Menü, Laden, Pause)");
            if (now - Math.max(zustandAm, start) > 20_000 && now - ohneZustandGemeldet > 120_000) {
                ohneZustandGemeldet = now;
                befund("kein_zustand", "seit " + (now - Math.max(zustandAm, start)) / 1000 + " s kein Spielzustand; Mod: " + modStatus);
            }
            return;
        }
        boolean neu = z.seq != letzteSeq;
        letzteSeq = z.seq;
        if (neu) beobachten(z, now);
        if (stadtgang.aktiv()) stadtgang.beobachten(z);


        if (neu) {
            for (Waechter.Befund b : waechter.pruefen(z, fahrer.laeuft(), now)) {
                if (b.art.equals("festgefahren")) gehirn.festgefahren(now);
                if (b.auffaellig) befund(b.art, b.text);
                else event(b.art, b.text);
            }
        }
        Zustand.Layout l = layout;
        if (auftrag != null && auftragVorab(z, l, now)) return;
        if (quest != null ? !quest.vorSchritt(z, l, now) : ll.leeren && !ll.leerenSchritt(z, l, now)) return;


        if (wegpunkte.kursZiel > 0 || auftrag != null) fahrer.verhalten.fensterDurchlassen = wegpunkte.aktiv();

        if (!fahrer.vorbereiten(z, l, now, waechter.totSeit())) return;


        if (!stadtgang.aktiv() && stadtgang.faellig(z, now)) {
            Zustand.Figur f = figur;
            if (f != null && now - f.ms < 3000) {
                if (!f.istTestfigur(testfigur)) {
                    fremdeFigur(f, l);
                    return;
                }
                stadtgang.starten(now);
            }
        }
        gehirn.layout = l;
        if (wine != null) z.ausgelassen.addAll(wine.ausgelassen());
        if (!tastenGemeldet && Zustand.Layout.TRAENKE_TASTEN.equals(l.traenke)) {


            tastenGemeldet = true;
            befund("traenke_tasten", "Tränke auf festen Tasten („Taste 1/2“): Heil- und Mana-Tipps fallen weg, es trinkt nur "
                + "die Automatik der App mit den Schwellen der Spielhilfen (nicht unter " + gehirn.trinkenPct + " % wie Idle). "
                + "Für Läufe mit Trinkschwelle „nur Gürtel“ oder „Gürtel, sonst Inventar“ wählen");
        }
        if (wine != null) gehirn.gehaltenesZiel = wine.gehaltenesZiel();
        if (ll.leeren && wine != null && !stadtgang.aktiv() && !z.stadt && anlegenSchritt(z, l, now)) return;
        rollenSchritt(z);
        Gehirn.Absicht a = auftrag != null ? null : wegpunkte.kurs(z, now);
        if (auftrag != null) {

            if (gehirn.imKampf()) auftrag.kampfAm = now;
            a = auftrag.schritt(z, ziele, figur, gehirn, wegpunkte, now);
            auftragMelden(now);
            if (auftrag.fertig()) {
                fahrer.allesLoslassen();
                beenden = true;
                return;
            }
        }
        if (a == null) a = quest != null ? quest.denken(z, ziele, figur, now) : stadtgang.aktiv() ? stadtgang.denken(z, figur, now) : gehirn.denken(z, ziele, now);
        if (!wegpunkte.meldung.equals(letzteWegpunktMeldung)) {
            letzteWegpunktMeldung = wegpunkte.meldung;
            event("wegpunkt", letzteWegpunktMeldung + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : ""));
            if (letzteWegpunktMeldung.startsWith("Wegpunkt abgebrochen")) befund("wegpunkt", wegpunkte.abbruch);
            if (letzteWegpunktMeldung.startsWith("Kurs abgebrochen")) befund("wegpunkt", wegpunkte.kursEnde);
        }

        if (ll.gebietLeerEnde(stadtgang.aktiv(), l, now)) return;
        fahrer.anwenden(a, l, now);
        if (wine != null) wine.ignorieren(gehirn.ignorierte(now));
        Puffer puffer = gehirn.kampfwahl.puffer;
        if (puffer.befund != null && !puffer.befund.equals(letzterPufferBefund)) {
            letzterPufferBefund = puffer.befund;
            befund("puffer", puffer.befund);
        }
        if (!stadtgang.meldung.equals(letzteStadtMeldung)) {
            letzteStadtMeldung = stadtgang.meldung;
            event("stadtgang", letzteStadtMeldung + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : ""));
            if (letzteStadtMeldung.startsWith("Stadtgang abgebrochen")) befund("stadtgang", stadtgang.abbruch);
        }
        if (gehirn.kampfMeldungen != kampfMeldungen) {
            kampfMeldungen = gehirn.kampfMeldungen;
            event("kampf", gehirn.kampfMeldung);
        }
        if (!gehirn.navigation.equals(letzteNavigation)) {
            letzteNavigation = gehirn.navigation;
            event("navigation", letzteNavigation + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy + " Modus " + z.modus : ""));
        }
        if (now - letztesBild > 5 * 60_000) {
            letztesBild = now;
            bild("regel-" + (++bildNr));
        }
    }

    private void loslassen(String warum) {
        fahrer.loslassen(warum);
    }





    private boolean auftragVorab(Zustand z, Zustand.Layout l, long now) throws InterruptedException {
        if (auftrag.fertig()) return true;
        if (z.tot()) auftrag.ende(Auftrag.Ergebnis.ABGEBROCHEN, "Figur tot");
        else if (now >= auftragBis) auftrag.ende(Auftrag.Ergebnis.ZEIT_UM, (auftragBis - start) / 1000 + " s, zuletzt " + auftrag.unterwegs);
        if (auftrag.fertig()) {
            auftragMelden(now);
            fahrer.allesLoslassen();
            beenden = true;
            return true;
        }
        Zustand.Figur f = figur;
        if (f == null || now - f.ms > 3000) {
            loslassen("Auftrag: warte auf die Figur (Typ 7)");
            auftrag.unterwegs = "warte auf die Figur";
            auftragMelden(now);
            return true;
        }
        if (!f.name.startsWith(Zustand.Figur.TEST_PREFIX)) {
            auftrag.ende(Auftrag.Ergebnis.ABGEBROCHEN, "Figur „" + f.name + "“ ist keine Testfigur (st-…)");
            auftragMelden(now);
            fremdeFigur(f, l);
            return true;
        }
        return false;
    }


    private void auftragMelden(long now) {
        String s = auftrag.status();
        if (s.equals(auftragStatus) || !auftrag.fertig() && now - auftragStatusAm < 2000) return;
        auftragStatus = s;
        auftragStatusAm = now;
        event("auftrag", s);
        writeStatus((auftrag.fertig() ? "beendet" : "läuft") + " " + dir + " · " + (now - start) / 1000 + " s · " + s);
    }


    private static void laeuftMarke() {
        File f = new File(LAEUFT);
        try {
            if (!f.exists()) new FileWriter(f).close();
            f.setLastModified(System.currentTimeMillis());
        }
        catch (IOException ignored) {}
    }

    private static void writeStatus(String line) {
        try (Writer w = new FileWriter(STATUS)) {
            w.write(line + "\n");
        }
        catch (IOException ignored) {}
    }






    private boolean anlegenSchritt(Zustand z, Zustand.Layout l, long now) throws InterruptedException {
        int[] an = z.anlegen;
        Zustand.Figur f = figur;
        if (f != null && now - f.ms > 3000) f = null;
        int[] grund = an != null && an[3] >= 0 ? new int[]{an[3], an[4], an[5], an[6]} : null;
        Ausruestung.Figur af = f != null ? Ausruestung.figur(f, z.klasse, grund) : null;
        Anlegen.Schritt s = anlegen.schritt(tabellen, f, af, gehirn.imKampf(),
            an != null && an[0] >= 0 && (an[0] & 4) != 0, an != null ? an[1] : -1, an != null ? an[2] : -1, now);
        switch (s.art) {
            case SENDEN:
                fahrer.loslassen("Anlegen");
                event("anlegen", "lege " + s.grund + " an (ID " + s.id + ")");
                wine.anlegen(s.id);
                return true;
            case HALTEN:
                fahrer.loslassen("Anlegen");
                if (!s.grund.isEmpty()) event("anlegen", "ID " + s.id + ": " + s.grund + ", halte still");
                return true;
            case ENDE:
                fahrer.loslassen("Anlegen");
                leerenBeenden("Anlegen: " + (s.keineAntwort ? "keine Antwort, " : "Gegenstand am Zeiger, ") + s.grund, true, l);
                return true;
            default:
                if (!s.grund.isEmpty()) event("anlegen", "ID " + s.id + ": " + s.grund);
                return false;
        }
    }

    private boolean leerenBeenden(String grund, boolean pause, Zustand.Layout l) throws InterruptedException {
        if (quest != null) return quest.questLeerenEnde(grund);
        fahrer.allesLoslassen();
        leerenEnde = grund;
        if (pause) {
            fahrer.tippen("menu_pause", "Pause: " + grund, l, SystemClock.uptimeMillis());
            Thread.sleep(200);
            fahrer.takt(SystemClock.uptimeMillis() + 1000);
        }
        event("ende", "Gebiet leeren beendet: " + grund);
        beenden = true;
        return false;
    }





    private void fremdeFigur(Zustand.Figur f, Zustand.Layout l) throws InterruptedException {
        fahrer.allesLoslassen();
        long now = SystemClock.uptimeMillis();
        fahrer.tippen("menu_pause", "Pause: fremde Figur", l, now);
        boolean pausiert = false;


        long bis = now + 2000;
        for (int i = 0; i < 80 && !pausiert && (SystemClock.uptimeMillis() < bis || fahrer.nachholenOffen()); i++) {
            Thread.sleep(100);
            long jetzt = SystemClock.uptimeMillis();
            fahrer.takt(jetzt);
            Zustand.Layout neu = layout;
            fahrer.nachholen(neu != null ? neu : l, jetzt);
            Zustand z = zustand;
            pausiert = z != null && z.pause;
        }
        befund("fremde_figur", "Figur „" + f.name + "“ ist nicht die Testfigur „" + testfigur + "“: Finger los, "
            + (pausiert ? "pausiert" : "Pause NICHT bestätigt") + ", Ende");
        beenden = true;
    }



    private void beobachten(Zustand z, long now) {
        zustaende++;
        if (startGebiet < 0) startGebiet = z.gebiet;

        if (letzterZustandAm > 0 && letztesGebiet >= 0) {
            Long t = zeitInGebiet.get(letztesGebiet);
            zeitInGebiet.put(letztesGebiet, (t == null ? 0 : t) + Math.min(2000, now - letzterZustandAm));
        }
        letzterZustandAm = now;
        if (z.gebiet != letztesGebiet) {
            String extra = "";
            if (z.stadt) inStadtSeit = now;
            else if (inStadtSeit >= 0 && letztesGebiet >= 0) {
                long ms = now - inStadtSeit;
                lagerZeiten.add(ms);
                extra = String.format(Locale.ROOT, " · Lager verlassen nach %.1f s", ms / 1000.0);
                inStadtSeit = -1;
                if (nurLager) beenden = true;
            }
            event("gebiet", z.gebiet + " " + gebietName(z.gebiet) + extra + (z.weltlage() ? " · Lage " + z.wx + "," + z.wy : ""));
            letztesGebiet = z.gebiet;
        }
        if (inStadtSeit < 0 && z.stadt) inStadtSeit = now;
        for (Zustand.Ding d : z.feinde()) lebendGesehen.add(d.id);
        for (Zustand.Ding d : z.leichen) {
            if (lebendGesehen.contains(d.id) && getoetet.add(d.id)) event("kill", "Monster " + d.id);
        }
    }



    private void befund(String art, String text) {
        int nr = ++befundNr;
        String name = String.format(Locale.ROOT, "befund-%02d-%s", nr, art);
        event("befund", art + ": " + text);
        try (Writer w = new FileWriter(new File(dir, name + ".txt"))) {
            w.write("# " + art + "\n" + text + "\n\n## Letzte Zustandszeilen\n");
            synchronized (lastStates) {
                for (String s : lastStates) w.write(s + "\n");
            }
            w.write("\n## Letzte Spur der App\n");
            synchronized (lastTrace) {
                for (String s : lastTrace) w.write(s + "\n");
            }
            w.write("\n## Mod-Log (letzte Zeilen, ganz in mod.log)\n");
            synchronized (lastModLog) {
                for (String s : lastModLog) w.write(s + "\n");
            }
        }
        catch (IOException ignored) {}
        bild(name);
        befunde.add(String.format(Locale.ROOT, "%s · **%s**: %s ([Bild](%s.png), [Zustand](%s.txt))", uhr(), art,
            text, name, name));
    }

    private void bild(String name) {
        if (abspielModus) return;
        final File file = new File(dir, name + ".png");
        final String wineDir = System.getProperty("d2t.wine");
        Thread t = new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec(wineDir != null
                    ? new String[]{"bash", wineDir + "/bild.sh", file.getPath()}
                    : new String[]{"screencap", "-p", file.getPath()});
                p.waitFor();
            }
            catch (Exception ignored) {}
        }, "screencap");
        t.setDaemon(true);
        t.start();
    }

    private synchronized void event(String art, String text) {
        try {
            JSONObject o = new JSONObject().put("zeit", uhr()).put("ms", SystemClock.uptimeMillis() - start).put("art", art).put("text", text);
            events.write(o + "\n");
            events.flush();
        }
        catch (Exception ignored) {}
    }

    private static String uhr() {
        return new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date());
    }

    private static boolean stadt(int no) {
        return no == 1 || no == 40 || no == 75 || no == 103 || no == 109;
    }

    private static String gebietName(int no) {
        String n = GEBIETE.get(no);
        return n != null ? n : "Gebiet " + no;
    }

    private synchronized void bericht(boolean fertig) {
        long dauer = SystemClock.uptimeMillis() - start;
        Zustand z = zustand;
        long draussen = 0;
        for (Map.Entry<Integer, Long> e : zeitInGebiet.entrySet()) if (!stadt(e.getKey())) draussen += e.getValue();
        StringBuilder b = new StringBuilder();
        b.append("# Spieltest-Bericht (Spieltester v1)\n\n");
        b.append("- Start: ").append(startZeit).append(fertig ? ", beendet" : ", läuft noch").append(" · Dauer ")
            .append(dauer / 60000).append(" min ").append(dauer / 1000 % 60).append(" s\n");
        b.append("- Gebiet: Start ").append(startGebiet < 0 ? "–" : gebietName(startGebiet)).append(", zuletzt ")
            .append(z == null ? "–" : gebietName(z.gebiet)).append("\n");
        b.append("- Zeit je Gebiet: ");
        for (Map.Entry<Integer, Long> e : zeitInGebiet.entrySet()) b.append(gebietName(e.getKey())).append(" ").append(e.getValue() / 1000).append(" s; ");
        b.append("\n- Außerhalb der Stadt: ").append(draussen / 60000).append(" min ").append(draussen / 1000 % 60).append(" s\n");
        b.append("- Lager verlassen nach: ");
        if (lagerZeiten.isEmpty()) b.append("–");
        for (long ms : lagerZeiten) b.append(String.format(Locale.ROOT, "%.1f s; ", ms / 1000.0));
        b.append("\n- Stufe: ").append(z == null || z.stufe < 0 ? "–" : String.valueOf(z.stufe)).append(" · Erfahrung ")
            .append(z == null || z.erfahrung < 0 ? "–" : String.valueOf(z.erfahrung)).append(" · Gold ")
            .append(z == null || z.gold < 0 ? "–" : String.valueOf(z.gold)).append("\n");
        b.append("- Navigation: Hindernisse ").append(gehirn.hindernisse).append(", Ziele gewechselt ").append(gehirn.zieleGewechselt)
            .append(", ganz festgefahren ").append(gehirn.ausweichFehlschlaege).append(", Joystick neu gedrückt ")
            .append(gehirn.neuGedrueckt).append("\n");
        Puffer pf = gehirn.kampfwahl.puffer;
        b.append("- Puffer: Tipps ").append(pf.tipps).append(", bestätigt ").append(pf.bestaetigt)
            .append(", Pausen nach Fehlversuchen ").append(pf.pausen).append("\n");
        b.append("- Kämpfe: ").append(gehirn.kaempfe).append(", davon gegen starke Gruppen ").append(gehirn.starkeKaempfe).append("\n");
        Zustand.Layout zl = gehirn.layout;
        b.append("- Zielhilfe: Reichweite ").append(zl == null || zl.zielweite <= 0 ? "–" : String.valueOf(Math.round(zl.zielweite)))
            .append(zl != null && zl.zielweiteNah > 0 ? " (Nahkampf " + Math.round(zl.zielweiteNah) + ", Fernkampf " + Math.round(zl.zielweiteFern) + ")" : "")
            .append(", Kegel ").append(zl == null || zl.zielkegel <= 0 ? "–" : Math.round(zl.zielkegel) + "°")
            .append(", Schritte heran ").append(gehirn.heranSchritte)
            .append(wine != null ? ", Drücke ins Leere " + wine.leerDruecke : "").append("\n");
        if (ll.leeren) b.append("- Gebiet leeren an der Grenze: Rückzüge von der Grenze ").append(gehirn.grenzRueckzuege)
            .append(", Monster im Ausgangsbereich übergangen ").append(gehirn.jenseitsUebergangen).append(", Blöcke ")
            .append(gehirn.abdeckungText(SystemClock.uptimeMillis())).append(", Wege ohne Ziel ").append(gehirn.wegeOhne).append("\n");
        b.append("- Wächter: Joystick ohne Wirkung ").append(waechter.joystickOhneWirkung).append(", Angriff ohne Wirkung ")
            .append(waechter.angriffOhneWirkung).append("\n");
        b.append("- Leben zuletzt: ").append(z == null ? "–" : z.leben + "/" + z.lebenMax).append(" · Heiltränke im Gürtel ")
            .append(z == null ? "–" : String.valueOf(z.heilGuertel)).append("\n");
        b.append("- Kills (Leichen zuvor lebend gesehener Monster): ").append(getoetet.size()).append(" · Tode: ").append(waechter.tode).append("\n");
        if (wegpunkte.kursZiel > 0) b.append("- Wegpunkte: Ziel Gebiet ").append(wegpunkte.kursZiel).append(", Phase ")
            .append(wegpunkte.kursPhase).append("/3, aktiviert ").append(wegpunkte.aktiviert).append(", Reisen ").append(wegpunkte.reisen)
            .append(", abgebrochen ").append(wegpunkte.abbrueche)
            .append(wegpunkte.kursEnde != null ? " · Kurs abgebrochen: " + wegpunkte.kursEnde : "").append("\n");
        if (questFahrer != null) {

            QuestZeile qz = questFahrer.zeile;
            b.append("- Quest ").append(qz.nummer).append(' ').append(qz.name).append(": ")
                .append(questFahrer.ende == null ? "läuft (" + questFahrer.phase() + ")" : questFahrer.ende)
                .append(" · Quest-Wort ").append(qz.ort).append(' ').append(quest.questWort < 0 ? "unbekannt" : Integer.toHexString(quest.questWort))
                .append(" · Tode ").append(questFahrer.tode).append(" · Rückkehr ").append(questFahrer.rueckkehr)
                .append(" · Nachsuchen ").append(questFahrer.nachsuchen)
                .append(questFahrer.leerNurErsatz() ? " · " + qz.ort + " leer nur per Abdeckung" : "").append("\n");
            long ohneLeveln = 0;
            StringBuilder phasen = new StringBuilder();
            for (Map.Entry<QuestFahrer.Phase, Long> e : questFahrer.dauer(SystemClock.uptimeMillis()).entrySet()) {
                if (e.getKey() != QuestFahrer.Phase.VORBEREITEN) ohneLeveln += e.getValue();
                phasen.append(e.getKey()).append(' ').append(e.getValue() / 1000).append(" s; ");
            }
            b.append("  - Zeit ohne Leveln: ").append(ohneLeveln / 60000).append(" min ").append(ohneLeveln / 1000 % 60)
                .append(" s (Richtwert 20–30 min) · je Phase: ").append(phasen).append("\n");
        }
        b.append("- Stadtgänge: ").append(stadtgang.gaenge).append(" (abgebrochen ").append(stadtgang.abbrueche)
            .append(") · Leben beim Heiler zuletzt ").append(stadtgang.lebenVorher).append(" → ").append(stadtgang.lebenNachher)
            .append(" · Testfigur ").append(testfigur).append("\n");
        b.append("- Tipps: ").append(fahrer.tipps).append(" (davon Nehmen ").append(fahrer.nehmenTipps).append(", aufgehoben ")
            .append(gehirn.aufgehoben).append(", aufgegeben ").append(gehirn.nehmenAufgegeben).append(") · Richtungswechsel: ")
            .append(gehirn.richtungswechsel).append("\n");
        b.append("- Zielprüfung: ").append(gehirn.zielpruefungText()).append("\n");
        b.append("- Türen: geöffnet ").append(gehirn.tuerenGeoeffnet).append(", Fehlversuche ").append(gehirn.tuer.fehlschlaege)
            .append(", gesperrt ").append(gehirn.tuerenGesperrt).append(", unterbrochen ").append(gehirn.tuerenUnterbrochen).append("\n");
        b.append("- Zustände: ").append(zustaende).append(" · Bewegung messbar in ").append(waechter.messbar).append(", nicht messbar in ")
            .append(waechter.unmessbar).append(" (ohne Weltlage und ohne feste Dinge in Sicht)\n");
        if (ll.leeren) {
            long aktiv = ll.leerenGebiet < 0 ? 0 : SystemClock.uptimeMillis() - ll.leerenSeit;
            int kills = getoetet.size() - ll.leerenKillsStart;
            b.append("- Gebiet leeren (wie Idle, --leeren): ").append(ll.leerenGebiet < 0 ? "nicht aktiv geworden" : gebietName(ll.leerenGebiet))
                .append(" · aktiv ").append(aktiv / 60000).append(" min ").append(aktiv / 1000 % 60).append(" s · Kills ").append(kills)
                .append(String.format(Locale.ROOT, " (%.1f/min)", aktiv > 0 ? kills * 60000.0 / aktiv : 0))
                .append(ll.messung.text(aktiv, kills, gehirn.hindernisse))
                .append(" · Rückzug unter ").append(ll.rueckzugPct).append(" % · Ende: ").append(!leerenEnde.isEmpty() ? leerenEnde : fertig ? "Zeit um bzw. Stopp" : "läuft").append("\n");
        }
        if (auftrag != null) b.append("- Auftrag: ").append(auftrag.text).append(" → ").append(auftrag.status()).append("\n");
        b.append("- Mod: ").append(modStatus).append("\n\n## Auffälligkeiten\n\n");
        if (befunde.isEmpty()) b.append("keine\n");
        for (String s : befunde) b.append("- ").append(s).append("\n");
        b.append("\nEreignisse: `ereignisse.jsonl` · Mod-Log: `mod.log`\n");
        try (Writer w = new FileWriter(new File(dir, "bericht.md"))) {
            w.write(b.toString());
        }
        catch (IOException ignored) {}
        if (auftrag != null) {

            writeStatus((fertig ? "beendet" : "läuft") + " " + dir + " · " + dauer / 1000 + " s · " + auftrag.status()
                + (z == null ? "" : " · Gebiet " + z.gebiet + (z.weltlage() ? " · Lage " + Math.round(z.wx) + "," + Math.round(z.wy) : "")));
            return;
        }
        try (Writer w = new FileWriter(STATUS)) {
            w.write((fertig ? "beendet" : "läuft") + " " + dir + " · " + dauer / 1000 + " s · Gebiet " + (z == null ? "–" : gebietName(z.gebiet))
                + " · draußen " + draussen / 1000 + " s · Lager " + lagerZeiten + " ms · Kills " + getoetet.size() + " · Tode " + waechter.tode
                + " · Befunde " + befunde.size() + "\n");
        }
        catch (IOException ignored) {}
    }




    private void zeile(String line) throws IOException {
        JSONObject o;
        try {
            o = new JSONObject(line);
        }
        catch (Exception e) {
            return;
        }
        String t = o.optString("t");
        if (portMitschrift != null && !t.equals("modlog") && !t.equals("spur")) mitschreiben(line);
        if (t.equals("zustand")) {
            zustand = Zustand.parse(o, line);
            portale = o.optInt("portale", -1);
            if (wine != null) wine.zustand(zustand);
            zustandAm = SystemClock.uptimeMillis();
            remember(lastStates, line, 20);
        }
        else if (t.equals("layout")) {
            layout = Zustand.Layout.parse(o);
            if (wine != null) wine.layout(layout);
        }
        else if (t.equals("figur")) figur = Zustand.Figur.parse(o);
        else if (t.equals("quest")) {

            JSONArray w = o.optJSONArray("woerter");
            if (quest != null) {
                int[] worte = null;
                if (w != null) {
                    worte = new int[w.length()];
                    for (int i = 0; i < worte.length; i++) worte[i] = w.optInt(i);
                }
                quest.worte(worte, SystemClock.uptimeMillis());
            }
        }
        else if (t.equals("fertigkeiten") && wine == null) {

            Fertigkeiten f = Fertigkeiten.ausJson(o);
            gehirn.kampfwahl.tabelle(f);
            event("fertigkeiten", f == null ? "keine Fertigkeiten" : "Klasse " + o.optInt("klasse") + ": " + o.optJSONArray("liste").length() + " Fertigkeiten");
        }
        else if (t.equals("ziele")) {
            Zustand.Ziele zl = Zustand.Ziele.parse(o);
            ziele = zl;
            zieleAm = SystemClock.uptimeMillis();
            int key = zl.gebiet * 4 + zl.bereit;
            if (key != zieleGebiet) {
                zieleGebiet = key;
                StringBuilder list = new StringBuilder();
                for (Zustand.Ziele.Ausgang e : zl.ausgaenge) list.append(e).append("; ");
                event("ziele", "Gebiet " + zl.gebiet + " bereit " + zl.bereit + ", Box " + java.util.Arrays.toString(zl.box) + ": " + list);
            }
        }
        else if (t.equals("karte")) {


            int gebiet = o.optInt("gebiet");
            Karte k = gehirn.karte();
            if (k == null || k.level != gebiet) {
                k = new Karte(gebiet);
                gehirn.setKarte(k);
                event("karte", "Gebiet " + gebiet + ": Karte wird gelesen");
            }
            if (o.has("raeume")) {
                org.json.JSONArray arr = o.optJSONArray("raeume");
                java.util.List<Karte.Raum> list = new java.util.ArrayList<>();
                for (int i = 0; arr != null && i < arr.length(); i++) {
                    org.json.JSONArray r = arr.optJSONArray(i);
                    if (r != null) list.add(new Karte.Raum(r.optInt(0), r.optInt(1), r.optInt(2), r.optInt(3), r.optInt(4), r.optInt(5) != 0));
                }
                k.raumliste(o.optInt("gesamt"), list);
            }
            else if (o.has("raum")) {
                byte[] bits = java.util.Base64.getDecoder().decode(o.optString("bits"));
                k.raumTeil(o.optInt("raum"), o.optInt("x"), o.optInt("y"), o.optInt("w"), o.optInt("h"), o.optInt("zeile"), o.optInt("zeilen"), bits, 0);
            }
        }
        else if (t.equals("modlog")) {
            String l = o.optString("l");
            remember(lastModLog, l, 80);
            synchronized (modLog) {
                modLog.write(l + "\n");
                modLog.flush();
            }
        }
        else if (t.equals("spur")) remember(lastTrace, o.optString("l"), 60);
        else if (t.equals("status")) {
            modStatus = o.optString("text");
            verbundenAm = SystemClock.uptimeMillis();
        }
    }


    private void mitschreiben(String line) {
        try {
            synchronized (portMitschrift) {
                portMitschrift.write(SystemClock.uptimeMillis() + "\t" + line + "\n");
                portMitschrift.flush();
            }
        }
        catch (IOException ignored) {}
    }

    private void readLoop() {
        while (!beenden) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", PORT), 2000);
                event("port", "verbunden mit 127.0.0.1:" + PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = in.readLine()) != null && !beenden) {
                    zeile(line);
                }
                event("port", "Verbindung beendet");
            }
            catch (IOException e) {

            }
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }

    private static void remember(ArrayDeque<String> list, String line, int max) {
        synchronized (list) {
            list.addLast(line);
            while (list.size() > max) list.removeFirst();
        }
    }

    private void foregroundLoop() {
        if (wine != null) return;
        boolean was = true;
        while (!beenden) {
            boolean now = PACKAGE.equals(resumedPackage());
            if (now != was) event("vordergrund", now ? "D2-App wieder vorn" : "D2-App nicht vorn: " + resumedPackage());
            was = now;
            vordergrund = now;
            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                return;
            }
        }
    }


    private static String paket() {
        String p = System.getProperty("d2t.paket");
        if (p == null || p.isEmpty()) p = System.getenv("D2T_PAKET");
        return p == null || p.isEmpty() ? "at.d2android" : p;
    }

    private static String resumedPackage() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity'"});
            String line;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                line = r.readLine();
            }
            p.waitFor();
            if (line == null) return "";
            int slash = line.indexOf('/');
            if (slash < 0) return "";
            int space = line.lastIndexOf(' ', slash);
            return line.substring(space + 1, slash).trim();
        }
        catch (Exception e) {
            return "";
        }
    }
}
