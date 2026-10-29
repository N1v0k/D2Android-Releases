package com.winlator.d2;

import android.os.SystemClock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;






public final class D2ModLink {
    public static final int APP_PORT = 7958, MOD_PORT = 7959;
    public static final int CMD_DRINK = 1, CMD_UNIT = 2, CMD_HIGHLIGHT = 3, CMD_WALK = 4, CMD_HUD = 5, CMD_PAUSE = 6, CMD_PORTAL = 7, CMD_NPC = 8, CMD_PLAYERS = 9, CMD_PLUGY = 10, CMD_BELT = 11, CMD_LABELS = 12, CMD_SELECT_SKILL = 13, CMD_TAKE = 15, CMD_STACKS = 20, CMD_EQUIP = 22, CMD_SORT = 24, CMD_RESUME = 25, CMD_VALUE = 26;
    public static final int BUTTON_LEFT = 0, BUTTON_RIGHT = 1;
    public static final int PHASE_DOWN = 0, PHASE_HOLD = 1, PHASE_UP = 2, PHASE_CLICK = 3;
    public static final long NO_UNIT = 0xFFFFFFFFL;
    public static final int DRINK_HEAL = 0, DRINK_MANA = 1, DRINK_REJUV = 2, DRINK_STAMINA = 3;

    private static final long STATE_TIMEOUT_MS = 500;





    static final long GAME_GAP_MS = 1000;

    private volatile long gameGapAt;

    private static final long GOALS_TIMEOUT_MS = 6000;
    private static final String TAG = "D2ModLink";

    public interface Listener {

        void onModState(D2ModState state);
    }





    private static final class Stamped<T> {
        final T data;
        final long time;

        Stamped(T data, long time) {
            this.data = data;
            this.time = time;
        }


        static <T> T fresh(Stamped<T> stamped, long maxAgeMs) {
            return stamped != null && SystemClock.uptimeMillis() - stamped.time < maxAgeMs ? stamped.data : null;
        }
    }


    private static final class GameInfo {

        final int players;

        final D2Effects.Effect[] effects;

        final int flags;

        final String characterName;

        final int mercLife;

        final int[] buffs, hotkeyLevels;

        final int capabilities;

        final int capabilities2;

        final D2Points points;
        final long time;

        GameInfo(int players, D2Effects.Effect[] effects, int flags, String characterName, int mercLife, int[] buffs,
                 int[] hotkeyLevels, int capabilities, int capabilities2, D2Points points, long time) {
            this.players = players;
            this.effects = effects;
            this.flags = flags;
            this.characterName = characterName;
            this.mercLife = mercLife;
            this.buffs = buffs;
            this.hotkeyLevels = hotkeyLevels;
            this.capabilities = capabilities;
            this.capabilities2 = capabilities2;
            this.points = points;
            this.time = time;
        }
    }


    private static final class MenuFocus {

        final int[] field;
        final String text;
        final boolean secret;
        final long time;

        MenuFocus(int[] field, String text, boolean secret, long time) {
            this.field = field;
            this.text = text;
            this.secret = secret;
            this.time = time;
        }
    }


    private static final class Status {
        final int code;
        final String text;
        final long time;

        Status(int code, String text, long time) {
            this.code = code;
            this.text = text;
            this.time = time;
        }
    }

    private final Listener listener;
    private volatile DatagramSocket socket;
    private volatile Thread thread;
    private volatile Stamped<D2ModState> state;
    private volatile Stamped<D2ModItems> items;

    private volatile GameInfo gameInfo;

    private volatile D2ResumeState resumeState;
    private volatile long resumeStateAt;

    private volatile int attackKind = D2AimReach.ATTACK_UNKNOWN;

    private volatile int[] gridFacts;

    private volatile long[] experience;
    private volatile long gridFactsAt;

    private volatile int drawnLayers = -1;
    private volatile int[] grids;

    private byte[] gridMask;
    private int gridMaskSize;
    private long gridMaskAt;

    private volatile long sharedGold = -1;

    private volatile boolean appDps;

    private volatile int xpBarBits;
    private volatile long attackKindAt;

    private volatile Stamped<byte[]> figur;

    private volatile Stamped<byte[]> goals;

    private volatile Stamped<D2Quests> quests;





    private volatile long spielSeit = -1, questErstesAm;
    private volatile int questPakete;
    private long questSpiel = -2;
    private String goalsLogged = "";

    private volatile boolean idleData;

    private final D2MapFetch mapFetch = new D2MapFetch((kind, index, part) ->
        send(command(D2MapFetch.CMD_MAP, 4).put((byte) kind).putShort((short) index).put((byte) part)));
    private String mapLogged = "";

    public D2MapFetch mapFetch() { return mapFetch; }
    private volatile Status status = new Status(-1, "", 0);
    private volatile int received;

    private volatile MenuFocus menu = new MenuFocus(null, "", false, 0);

    private final ExecutorService sender = Executors.newSingleThreadExecutor();

    public D2ModLink(Listener listener) {
        this.listener = listener;
    }

    public synchronized void start() {
        if (thread != null) return;
        thread = new Thread(this::run, "D2ModLink");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        Thread current = thread;
        thread = null;
        DatagramSocket current2 = socket;
        if (current2 != null) current2.close();
        if (current != null) current.interrupt();
    }

    private void run() {
        byte[] buffer = new byte[8192];
        DatagramSocket mine = null;
        try (DatagramSocket s = new DatagramSocket(null)) {
            mine = s;
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), APP_PORT));
            s.setSoTimeout(1000);
            socket = s;
            while (thread == Thread.currentThread()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    s.receive(packet);
                }
                catch (SocketTimeoutException e) {
                    continue;
                }
                handle(packet.getData(), packet.getLength());
            }
        }
        catch (IOException e) {
            Status last = status;
            status = new Status(last.code, "port " + APP_PORT + " in use: " + e.getMessage(), last.time);
        }
        finally {
            if (socket == mine) socket = null;
        }
    }






    void handle(byte[] data, int length) {
        int type = D2ModState.packetType(data, length);
        if (type == D2ModState.PKT_STATE) {
            D2ModState parsed = D2ModState.parse(data, length);
            if (parsed != null) {


                Stamped<D2ModState> last = state;
                long now = SystemClock.uptimeMillis();
                if (last != null && now - last.time > 5000) mapFetch.reset();


                if (last != null && now - last.time > GAME_GAP_MS) gameGapAt = now;
                if (last == null || now - last.time > GAME_GAP_MS) spielSeit = now;
                state = new Stamped<>(parsed, now);
                statusesSinceState = 0;
                received++;
                if (D2SpieltestPort.active()) D2SpieltestPort.state(parsed, this);
                if (listener != null) listener.onModState(parsed);
                if (mapFetch.tick(now, D2SpieltestPort.active() || idleData, parsed.worldX, parsed.worldY)) {
                    String st = mapFetch.stand();
                    if (!st.equals(mapLogged)) { mapLogged = st; D2Trace.log("map: " + st); }
                }
            }
        }
        else if (type == D2ModState.PKT_GRID && length >= 11) {
            int w = data[8] & 0xFF, h = data[9] & 0xFF, n = data[10] & 0xFF;
            if (length >= 11 + n) {
                gridMask = java.util.Arrays.copyOfRange(data, 11, 11 + n);
                gridMaskSize = w << 8 | h;
                gridMaskAt = SystemClock.uptimeMillis();
            }
        }
        else if (type == D2ModState.PKT_ITEMS) {
            D2ModItems parsed = D2ModItems.parse(data, length);

            if (parsed != null && gridMask != null && SystemClock.uptimeMillis() - gridMaskAt < 1000)
                parsed.attachGrid(gridMaskSize >> 8, gridMaskSize & 0xFF, gridMask);
            gridMask = null;
            if (parsed != null) items = new Stamped<>(parsed, SystemClock.uptimeMillis());
        }
        else if (type == D2ModState.PKT_EFFECTS && length >= 9) {
            int flags = parseGameFlags(data, length);
            GameInfo last = gameInfo;
            if (flags != (last != null ? last.flags : -1)) android.util.Log.i(TAG, "game flags " + flags + (flags >= 0 && (flags & 1) != 0 ? " (single player)" : ""));
            int extension = extensionAt(data, length);
            D2Points points = D2Points.parse(data, length, extension);
            resumeState = D2ResumeState.parse(data, length, extension);
            resumeStateAt = SystemClock.uptimeMillis();
            attackKind = D2Points.parseAttack(data, length, extension);
            attackKindAt = resumeStateAt;
            gridFacts = D2Points.parseGrid(data, length, extension);
            gridFactsAt = resumeStateAt;
            experience = D2Points.parseExperience(data, length, extension);
            drawnLayers = D2Points.parseDrawnLayers(data, length, extension);
            grids = D2Points.parseGrids(data, length, extension);
            sharedGold = D2Points.parseSharedGold(data, length, extension);
            if (points != null && (last == null || last.points == null || last.points.counter != points.counter)) {
                android.util.Log.i(TAG, points.toString());
            }
            gameInfo = new GameInfo(data[8] & 0xFF, D2Effects.parse(data, length), flags,
                parseCharacterName(data, length), parseMercLife(data, length),
                parseBuffs(data, length), parseLevels(data, length),
                D2Points.parseCapabilities(data, length, extension), D2Points.parseCapabilities2(data, length, extension),
                points, SystemClock.uptimeMillis());
        }
        else if (type == D2ModState.PKT_PLAYTEST) {


            byte[] copy = java.util.Arrays.copyOf(data, length);
            figur = new Stamped<>(copy, SystemClock.uptimeMillis());
            if (D2SpieltestPort.active()) D2SpieltestPort.figur(copy, copy.length);
        }
        else if (type == D2ModState.PKT_GOALS) {

            byte[] copy = java.util.Arrays.copyOf(data, length);
            goals = new Stamped<>(copy, SystemClock.uptimeMillis());
            if (copy.length >= 10) mapFetch.level((copy[8] & 0xFF) | ((copy[9] & 0xFF) << 8));
            logGoals(copy);
            if (D2SpieltestPort.active()) D2SpieltestPort.goals(copy, copy.length);
        }
        else if (type == D2Quests.PKT_QUESTS) {

            D2Quests q = D2Quests.parse(data, length);
            if (q != null) {
                long now = SystemClock.uptimeMillis();
                if (questSpiel != spielSeit) {
                    questSpiel = spielSeit;
                    questPakete = 0;
                    questErstesAm = now;
                }
                questPakete++;
                quests = new Stamped<>(q, now);
                if (D2SpieltestPort.active()) D2SpieltestPort.quests(q, (data[6] & 0xFF) | (data[7] & 0xFF) << 8);
            }
        }
        else if (type == D2TeleportAim.PKT_TELEPORT) {

            D2TeleportAim.Answer answer = D2TeleportAim.parse(data, length, SystemClock.uptimeMillis());
            if (answer != null) teleportAnswer = answer;
        }
        else if (type == D2Materials.PKT_MATERIALS) {

            D2Materials.Snapshot parsed = D2Materials.parse(data, length, SystemClock.uptimeMillis());
            if (parsed != null) materials = parsed;
        }
        else if (type == PKT_OBJECTS) {

            long[] parsed = parseObjectDetails(data, length);
            if (parsed != null) objectDetails = new Stamped<>(parsed, SystemClock.uptimeMillis());
        }
        else if (type == D2MapFetch.PKT_MAP) {

            String line = mapFetch.packet(data, length, SystemClock.uptimeMillis());
            if (line != null && D2SpieltestPort.active()) D2SpieltestPort.karte(line);
        }
        else if (type == D2ModState.PKT_MENU) {
            int[] field = D2ModState.parseMenuField(data, length);
            String text = D2ModState.parseMenuText(data, length);
            boolean secret = D2ModState.parseMenuSecret(data, length);
            MenuFocus last = menu;
            boolean changed = !java.util.Arrays.equals(field, last.field) || !text.equals(last.text) || secret != last.secret;
            menu = new MenuFocus(field, text, secret, SystemClock.uptimeMillis());
            if (changed && listener != null) listener.onModState(null);
        }
        else if (type == D2DirectStart.PKT_DIRECT) {

            D2DirectStart.Report report = D2DirectStart.Report.parse(data, length);
            if (report != null) {
                D2DirectStart.Report last = directReport;
                if (last == null || last.state != report.state || last.step != report.step) D2Trace.log("direct start: " + report);
                directReport = report;
            }
        }
        else if (type == D2ModState.PKT_STATUS) {
            Object[] parsed = D2ModState.parseStatus(data, length);
            if (parsed != null) {
                status = new Status((Integer) parsed[0], (String) parsed[1], SystemClock.uptimeMillis());
                statusesSinceState++;
                if (D2SpieltestPort.active()) D2SpieltestPort.state(null, this);
                if (listener != null) listener.onModState(null);
            }
        }
    }


    public long lastStateAt() {
        Stamped<D2ModState> current = state;
        return current != null ? current.time : 0;
    }


    public D2ModState state() {
        return Stamped.fresh(state, STATE_TIMEOUT_MS);
    }


    private GameInfo gameInfo(long maxAgeMs) {
        GameInfo current = gameInfo;
        return current != null && SystemClock.uptimeMillis() - current.time < maxAgeMs ? current : null;
    }


    public int players() {
        GameInfo info = gameInfo(2500);
        return info != null ? info.players : 0;
    }


    public boolean heroDead() {
        GameInfo info = gameInfo(3000);
        return info != null && info.flags >= 0 && (info.flags & 2) != 0;
    }


    public boolean heroWaitsForEsc() {
        GameInfo info = gameInfo(3000);
        return info != null && info.flags >= 0 && (info.flags & 4) != 0;
    }


    public long gameInfoTime() {
        GameInfo info = gameInfo;
        return info != null ? info.time : 0;
    }


    public boolean heroDeathKnown() {
        GameInfo info = gameInfo(3000);
        return info != null && info.flags >= 0;
    }






    public boolean singlePlayer() {
        GameInfo info = gameInfo(2500);
        return info != null && info.flags >= 0 && (info.flags & 1) != 0;
    }


    public byte[] figur() {
        return Stamped.fresh(figur, 3000);
    }


    public long figurAt() {
        Stamped<byte[]> current = figur;
        return current != null ? current.time : 0;
    }


    public long gameFlagsAt() {
        GameInfo current = gameInfo;
        return current != null ? current.time : 0;
    }


    public D2Quests quests() {
        return Stamped.fresh(quests, 12_000);
    }


    public long questsAt() {
        Stamped<D2Quests> q = quests;
        return q != null ? q.time : 0;
    }





    public int[] questWorte() {
        D2Quests q = quests();
        if (q == null || !com.winlator.d2.test.Quests.worteBekannt(questPakete, questErstesAm, spielSeit)) return null;
        return q.woerter();
    }





    public byte[] goals() {
        return Stamped.fresh(goals, GOALS_TIMEOUT_MS);
    }


    static int parseGameFlags(byte[] d, int length) {
        if (length < 10) return -1;
        int at = 10 + (d[9] & 0xFF) * 6;
        return length > at ? d[at] & 0xFF : -1;
    }


    static String parseCharacterName(byte[] d, int length) {
        if (length < 10) return null;
        int at = 10 + (d[9] & 0xFF) * 6 + 1;
        if (length < at + 16) return null;
        int n = 0;
        while (n < 16 && d[at + n] != 0) n++;
        return n > 0 ? new String(d, at, n, java.nio.charset.StandardCharsets.ISO_8859_1) : null;
    }


    static int parseMercLife(byte[] d, int length) {
        if (length < 10) return -1;
        int at = 10 + (d[9] & 0xFF) * 6 + 1 + 16;
        if (length < at + 2 || d[at] == 0) return -1;
        return d[at + 1] & 0xFF;
    }


    private static int buffsAt(byte[] d, int length) {
        if (length < 10) return -1;
        int at = 10 + (d[9] & 0xFF) * 6 + 1 + 16 + 2;
        return length > at ? at : -1;
    }





    static int[] parseBuffs(byte[] d, int length) {
        int at = buffsAt(d, length);
        if (at < 0) return null;
        int n = d[at] & 0xFF;
        if (length < at + 1 + n) return null;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = d[at + 1 + i] & 0xFF;
        return out;
    }


    static int[] parseLevels(byte[] d, int length) {
        int at = buffsAt(d, length);
        if (at < 0) return null;
        at += 1 + (d[at] & 0xFF);
        if (length < at + 16) return null;
        int[] out = new int[16];
        for (int i = 0; i < 16; i++) out[i] = d[at + i] & 0xFF;
        return out;
    }





    static int extensionAt(byte[] d, int length) {
        int at = buffsAt(d, length);
        if (at < 0) return -1;
        at += 1 + (d[at] & 0xFF) + 16;
        return length > at ? at : -1;
    }





    public D2Points points(long maxAgeMs) {
        GameInfo info = gameInfo(maxAgeMs);
        return info != null ? info.points : null;
    }


    public static final int CAP_COMPARE = 2;

    public static final int CAP_VALUE = 0x40;

    public static final int CAP_TELEPORT = 0x80;

    public static final int CAP2_DIFF = 0x01;


    private volatile D2TeleportAim.Answer teleportAnswer;

    public D2TeleportAim.Answer teleportAnswer() {
        return teleportAnswer;
    }


    private volatile D2Materials.Snapshot materials;

    public D2Materials.Snapshot materials() {
        return materials;
    }


    public void materials(int order, int arg, int count) {
        D2Trace.log("-> materials order " + order + " arg " + arg + " count " + count);
        send(command(D2Materials.CMD_MATERIALS, 12).put(D2Materials.payload(order, arg, count)));
    }


    static final int PKT_OBJECTS = 12;

    private volatile Stamped<long[]> objectDetails;





    public long[] objectDetails(long maxAgeMs) {
        Stamped<long[]> current = objectDetails;
        return Stamped.fresh(current, maxAgeMs);
    }


    static long[] parseObjectDetails(byte[] d, int length) {
        if (length < 9) return null;
        int n = d[8] & 0xFF;
        if (length < 9 + 7 * n) return null;
        long[] out = new long[3 * n];
        for (int i = 0, at = 9; i < n; i++, at += 7) {
            out[3 * i] = (d[at] & 0xFFL) | (d[at + 1] & 0xFFL) << 8 | (d[at + 2] & 0xFFL) << 16 | (d[at + 3] & 0xFFL) << 24;
            out[3 * i + 1] = (d[at + 4] & 0xFF) | (d[at + 5] & 0xFF) << 8;
            out[3 * i + 2] = d[at + 6] & 0xFF;
        }
        return out;
    }


    public boolean teleportAvailable() {
        int caps = capabilities();
        return caps >= 0 && (caps & CAP_TELEPORT) != 0;
    }


    public void teleport(byte[] args) {
        if (args == null || args.length != 4) return;
        send(command(D2TeleportAim.CMD_TELEPORT, 4).put(args));
    }


    public static final int CAP_DAMAGE = 8;


    public int capabilities() {
        GameInfo info = gameInfo(3000);
        return info != null ? info.capabilities : -1;
    }





    public static final int CAP2_TELE_HOVER = 0x02;

    public static final int CAP2_LAYERS = 0x40;

    public static final int CAP2_LAYERS_BAR = 0x80;


    public static final int CAP2_XP_BAR = 0x20;


    public int capabilities2() {
        GameInfo info = gameInfo(3000);
        return info != null ? info.capabilities2 : -1;
    }


    public boolean capable2(int bit) {
        int caps = capabilities2();
        return caps >= 0 && (caps & bit) != 0;
    }


    public int[] buffs() {
        GameInfo info = gameInfo(3000);
        return info != null ? info.buffs : null;
    }


    public int[] hotkeyLevels() {
        GameInfo info = gameInfo(3000);
        return info != null ? info.hotkeyLevels : null;
    }


    public int mercLife() {
        GameInfo info = gameInfo(3000);
        return info != null ? info.mercLife : -1;
    }


    public String characterName() {
        GameInfo info = gameInfo(3000);
        return info != null ? info.characterName : null;
    }






    public String currentCharacterName() {
        GameInfo info = gameInfo(3000);
        return info != null && info.time >= gameGapAt ? info.characterName : null;
    }


    private void logGoals(byte[] d) {
        if (d.length < 20) return;
        int level = (d[8] & 0xFF) | (d[9] & 0xFF) << 8;
        String line = "goals: level " + level + ", ready " + (d[10] & 0xFF) + ", exits " + (d[19] & 0xFF);
        if (line.equals(goalsLogged)) return;
        goalsLogged = line;
        android.util.Log.i(TAG, line);
    }


    public D2Effects.Effect[] effects() {
        GameInfo info = gameInfo(2500);
        return info != null ? info.effects : new D2Effects.Effect[0];
    }


    public D2ModItems items() {
        return Stamped.fresh(items, 2500);
    }


    private MenuFocus focus() {
        MenuFocus current = menu;
        return current.field != null && SystemClock.uptimeMillis() - current.time < 2500 ? current : null;
    }





    public int[] menuField() {
        MenuFocus focus = focus();
        return focus != null ? focus.field : null;
    }


    public String menuText() {
        MenuFocus focus = focus();
        return focus != null && !focus.secret ? focus.text : "";
    }


    public boolean menuSecret() {
        MenuFocus focus = focus();
        return focus != null && focus.secret;
    }

    public boolean isConnected() {
        Status current = status;
        return current.code == 1 && SystemClock.uptimeMillis() - current.time < 3000;
    }





    public boolean isAlive() {
        Status current = status;
        return (current.code == 0 || current.code == 1) && SystemClock.uptimeMillis() - current.time < 3000;
    }






    public boolean reportsWithoutState() {
        return isAlive() && D2InstallFlow.menusWithoutState(state != null, statusesSinceState);
    }


    private volatile int statusesSinceState;


    public String describe() {
        Status current = status;
        if (current.code < 0) return "Mod: no connection" + (current.text.isEmpty() ? "" : " (" + current.text + ")");
        String age = SystemClock.uptimeMillis() - current.time < 3000 ? "" : " (stale)";
        return "Mod: " + current.text + age + ", packets " + received;
    }


    public void npc(int index) {
        send(command(CMD_NPC, 1).put((byte) index));
    }


    public void portal() {
        send(command(CMD_PORTAL, 1).put((byte) 0));
    }


    public void setIdleData(boolean on) {
        idleData = on;
    }


    public void pause(boolean paused) {
        D2Trace.log("-> pause " + paused);
        send(command(CMD_PAUSE, 1).put((byte) (paused ? 1 : 0)));
    }












    public void hud(boolean statues, boolean durability, boolean pointer, boolean unlimitedStamina, boolean itemNames, boolean mapHack,
                    boolean compare, int damageStyle, boolean damageDps, boolean diffLines) {
        int bits = (statues ? 1 : 0) | (durability ? 2 : 0) | (pointer ? 4 : 0) | (unlimitedStamina ? 8 : 0) | (itemNames ? 16 : 0)
            | (mapHack ? 32 : 0) | (D2SpieltestPort.active() || idleData ? 64 : 0)
            | 128;


        boolean damageNumbers = D2DamageStyle.numbers(damageStyle), byElement = damageNumbers && D2DamageStyle.byElement(damageStyle);
        int extra = (compare ? 1 : 0) | (damageNumbers ? 2 : 0) | (damageDps ? 4 : 0) | (byElement ? 8 : 0) | (diffLines ? 16 : 0)
            | (appDps ? 32 : 0);
        int xp = xpBarBits & 7;
        if (bits != lastHudBits || extra != lastHudExtra || xp != lastHudXp) {
            D2Trace.log("-> hud bits " + bits + (itemNames ? " (item names)" : "") + (mapHack ? " (map hack)" : "") + (compare ? " (compare)" : "")
                + (damageNumbers ? (byElement ? " (damage numbers by element)" : " (damage numbers)") : "") + (damageDps ? " (DPS)" : "") + (diffLines ? " (comparison lines)" : "")
                + (xp != 0 ? " (xp bar " + xp + ")" : ""));
        }
        lastHudBits = bits;
        lastHudExtra = extra;
        lastHudXp = xp;
        send(command(CMD_HUD, 3).put((byte) bits).put((byte) extra).put((byte) xp));
    }


    public void stacks(boolean large) {
        send(command(CMD_STACKS, 1).put((byte) (large ? 1 : 0)));
    }


    public void players(int count) {
        send(command(CMD_PLAYERS, 1).put((byte) count));
    }


    public static final int PLUGY_UNASSIGN_STR = 0x14, PLUGY_UNASSIGN_ENE = 0x15, PLUGY_UNASSIGN_DEX = 0x16,
        PLUGY_UNASSIGN_VIT = 0x17, PLUGY_UNASSIGN_SKILLS = 0x18, PLUGY_PAGE_PREVIOUS = 0x19, PLUGY_PAGE_NEXT = 0x1A,
        PLUGY_SELF_STASH = 0x1B, PLUGY_SHARED_STASH = 0x1C, PLUGY_INDEX_PREVIOUS = 0x1D, PLUGY_INDEX_NEXT = 0x1E,
        PLUGY_SAVE = 0x24;


    public void plugy(int code) {
        send(command(CMD_PLUGY, 1).put((byte) code));
    }





    public void valueRules(byte[] payload) {
        send(command(CMD_VALUE, payload.length).put(payload));
    }


    public void frames(byte[] payload) {
        send(command(D2Breakpoints.CMD_FRAMES, payload.length).put(payload));
    }





    public void labelFilter(int bits, int seconds, int radius) {
        labelFilter(bits, seconds, radius, 0, 0);
    }


    public void labelFilter(int bits, int seconds, int radius, int lootMask, int lootGold) {
        if (bits != lastLabelBits || lootMask != lastLootMask) D2Trace.log("-> label filter bits " + bits + ", loot 0x" + Integer.toHexString(lootMask));
        lastLabelBits = bits;
        lastLootMask = lootMask;
        send(command(CMD_LABELS, 7).put((byte) bits).put((byte) seconds).put((byte) radius)
            .putShort((short) lootMask).putShort((short) Math.min(lootGold, 0xFFFF)));
    }


    public static final int CAP_LOOT = 0x20;

    private int lastLabelBits = -1, lastLootMask = -1;





    public void selectSkill(boolean left, int skill) {
        D2Trace.log("-> select skill " + skill + (left ? " left" : " right"));
        send(command(CMD_SELECT_SKILL, 3).put((byte) (left ? 0 : 1)).putShort((short) skill));
    }





    public void points(int kind, int what, int key) {
        points(kind, what, key, false);
    }


    public void points(int kind, int what, int key, boolean move) {
        D2Trace.log("-> points kind " + kind + " " + what + (kind == D2Points.KIND_KEY ? " key F" + (key + 1) : "")
            + (move ? " move" : ""));
        send(ByteBuffer.wrap(D2Points.command(kind, what, key, move)));
    }


    public void belt(int which) {
        D2Trace.log("-> belt " + which);
        send(command(CMD_BELT, 1).put((byte) which));
    }


    public void drink(int which, boolean beltOnly) {
        drink(which, beltOnly, false, false);
    }






    public void drink(int which, boolean beltOnly, boolean largest, boolean onlyBigger) {
        D2Trace.log("-> drink " + which + (largest ? " largest" : "") + (onlyBigger ? " bigger" : ""));
        send(command(CMD_DRINK, 2).put((byte) which).put((byte) ((beltOnly ? 1 : 0) | (largest ? 4 : 0) | (onlyBigger ? 8 : 0))));
    }


    public void potionForMercenary() {
        D2Trace.log("-> drink heal merc");
        send(command(CMD_DRINK, 2).put((byte) DRINK_HEAL).put((byte) 3));
    }





    public void walk(int phase, int dx, int dy) {
        if (phase != PHASE_HOLD) D2Trace.log("-> walk " + phaseName(phase) + " " + dx + "," + dy);
        send(command(CMD_WALK, 6).put((byte) phase).put((byte) 0).putShort((short) dx).putShort((short) dy));
    }


    public void unit(int button, int phase, int type, long id) {
        if (phase != PHASE_HOLD) D2Trace.log("-> unit " + (button == BUTTON_RIGHT ? "right " : "left ") + phaseName(phase) + " type " + type + " id " + id);
        send(command(CMD_UNIT, 12).put((byte) button).put((byte) phase).put((byte) 0).put((byte) 0)
            .putInt(type).putInt((int) id));
    }





    public void take(int type, long id) {
        D2Trace.log("-> take type " + type + " id " + id);
        send(command(CMD_TAKE, 8).put((byte) type).put((byte) 0).put((byte) 0).put((byte) 0).putInt((int) id));
    }





    public void refillPotions(long job, D2PotionShop.Need need, int minGold) {
        D2Trace.log("-> refill potions job " + job + ": heal " + need.heal + " (" + need.healBelt + " belt), mana " + need.mana
            + " (" + need.manaBelt + " belt), min gold " + minGold);
        send(command(D2PotionShop.COMMAND, 12).put((byte) Math.min(80, need.heal)).put((byte) Math.min(80, need.mana))
            .put((byte) Math.min(40, need.healBelt)).put((byte) Math.min(40, need.manaBelt)).putInt(minGold).putInt((int) job));
    }






    public void buyPortals(long job, int count, int minGold, boolean tome) {
        D2Trace.log("-> buy " + (tome ? "tome" : "scrolls") + " job " + job + ": " + count + ", min gold " + minGold);
        send(command(D2PotionShop.COMMAND, 13).put((byte) Math.max(0, Math.min(40, count))).put((byte) 0).put((byte) 0).put((byte) 0)
            .putInt(minGold).putInt((int) job).put((byte) (tome ? 2 : 1)));
    }





    public void lootJob(boolean sell, long job, long[] ids, long gold) {
        int n = Math.min(48, ids.length);
        D2Trace.log("-> " + (sell ? "sell" : "stash") + " loot job " + job + ": " + n + " items" + (gold > 0 ? ", gold " + gold : ""));

        ByteBuffer b = command(sell ? 17 : 18, 12 + 4 * n).put((byte) n).put((byte) 0).put((byte) 0).put((byte) 0)
            .putInt(sell ? 0 : (int) Math.min(Integer.MAX_VALUE, Math.max(0, gold))).putInt((int) job);
        for (int i = 0; i < n; i++) b.putInt((int) ids[i]);
        send(b);
    }






    public void buyWare(long job, long wareId, int count, int minGold) {
        D2Trace.log("-> buy ware " + wareId + " job " + job + ": " + count + ", min gold " + minGold);
        send(command(D2PotionShop.COMMAND, 17).put((byte) Math.max(0, Math.min(D2ShopGestures.MAX, count))).put((byte) 0)
            .put((byte) 0).put((byte) 0).putInt(minGold).putInt((int) job).put((byte) 3).putInt((int) wareId));
    }

    private int sellRequest;
    private int moveRequest;





    public void move(int place, int x, int y) {
        int request = ++moveRequest;
        D2Trace.log("-> move place " + place + " cell " + x + "," + y + " request " + request);
        send(command(D2ItemMoves.CMD_MOVE, 8).put(D2ItemMoves.movePayload(place, x, y, request)));
    }

    private int cubeRequest;





    public void cube(int[] ids) {
        int request = ++cubeRequest;
        D2Trace.log("-> cube " + java.util.Arrays.toString(ids) + " request " + request);
        byte[] payload = D2RezeptEinlegen.payload(ids, request);
        send(command(D2RezeptEinlegen.CMD_CUBE, payload.length).put(payload));
    }





    public void cubeOpen() {
        D2Trace.log("-> cube open");
        byte[] payload = D2RezeptOeffnen.payload();
        send(command(D2RezeptOeffnen.CMD_CUBE_OPEN, payload.length).put(payload));
    }





    public void sell(int x, int y) {
        int request = ++sellRequest;
        D2Trace.log("-> sell cell " + x + "," + y + " request " + request);
        send(command(D2Gamepad.CMD_SELL, 8).put(D2Gamepad.sellPayload(x, y, request)));
    }





    public void repair(long job) {
        D2Trace.log("-> repair job " + job);
        send(command(D2PotionShop.REPAIR_COMMAND, 12).put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0).putInt(0).putInt((int) job));
    }





    public void equip(long id, int bodyLoc) {
        D2Trace.log("-> equip " + id + " to " + bodyLoc);
        send(command(CMD_EQUIP, 8).put((byte) bodyLoc).put((byte) 0).put((byte) 0).put((byte) 0).putInt((int) id));
    }





    public void sortInventory(boolean charmsFixed) {
        D2Trace.log("-> sort inventory, charms " + (charmsFixed ? "fixed" : "moving"));
        send(command(CMD_SORT, 2).put((byte) 0).put((byte) (charmsFixed ? 1 : 0)));
    }


    public int[] gridFacts() {
        return SystemClock.uptimeMillis() - gridFactsAt < 3000 ? gridFacts : null;
    }


    public int drawnLayers() {
        return SystemClock.uptimeMillis() - gridFactsAt < 3000 ? drawnLayers : -1;
    }





    public int[] grids() {
        return SystemClock.uptimeMillis() - gridFactsAt < 3000 ? grids : null;
    }


    public long sharedGold() {
        return SystemClock.uptimeMillis() - gridFactsAt < 3000 ? sharedGold : -1;
    }


    public long drawnLayersAt() {
        return gridFactsAt;
    }


    public static final int CMD_SOUND = 35, SOUND_BUTTON_CLICK = 4;





    public void playSound(int id) {
        D2Trace.log("-> sound " + id);
        send(command(CMD_SOUND, 2).putShort((short) id));
    }


    public void sendLayer(byte[] packet) {
        if (packet == null || packet.length < 8) return;
        send(ByteBuffer.wrap(packet.clone()));
    }


    public long[] experience() {
        return SystemClock.uptimeMillis() - gridFactsAt < 3000 ? experience : null;
    }


    public void setAppDps(boolean on) {
        appDps = on;
    }


    public void setXpBar(int bits) {
        xpBarBits = bits;
    }







    public int attackKind() {
        return SystemClock.uptimeMillis() - attackKindAt < 2000 || isAlive() ? attackKind : D2AimReach.ATTACK_UNKNOWN;
    }


    private volatile D2DirectStart.Report directReport;

    public D2DirectStart.Report directReport() {
        return directReport;
    }


    public D2ResumeState resumeState() {
        return SystemClock.uptimeMillis() - resumeStateAt < 3000 ? resumeState : null;
    }


    public void resume(int level, int x, int y, long seed, int roomX, int roomY, int roomW, int roomH, int difficulty) {
        D2Trace.log("-> resume to level " + level + " at " + x + "," + y);
        send(command(CMD_RESUME, 20).put((byte) 1).putShort((short) level).putShort((short) x).putShort((short) y)
            .putInt((int) seed).putShort((short) roomX).putShort((short) roomY).putShort((short) roomW).putShort((short) roomH)
            .put((byte) difficulty));
    }


    public void resumeExit(int kind) {
        D2Trace.log("-> resume way out " + (kind == 2 ? "to town" : "to the waypoint"));
        send(command(CMD_RESUME, 1).put((byte) kind));
    }


    public boolean canTake() {
        D2ModItems current = items();
        return current != null && current.takePossible;
    }


    public void highlight(int type, long id) {
        if (id != lastHighlight) D2Trace.log("-> highlight type " + type + " id " + id);
        lastHighlight = id;
        send(command(CMD_HIGHLIGHT, 8).putInt(type).putInt((int) id));
    }

    private long lastHighlight = -2;
    private int lastHudBits = -1, lastHudExtra = -1, lastHudXp = -1;

    private static String phaseName(int phase) {
        return phase == PHASE_DOWN ? "down" : phase == PHASE_HOLD ? "hold" : phase == PHASE_UP ? "up" : phase == PHASE_CLICK ? "click" : "phase " + phase;
    }

    private static ByteBuffer command(int command, int payload) {
        ByteBuffer b = ByteBuffer.allocate(8 + payload).order(ByteOrder.LITTLE_ENDIAN);
        b.put((byte) 'D').put((byte) '2').put((byte) 'A').put((byte) 'C').put((byte) 1).put((byte) command).putShort((short) 0);
        return b;
    }

    private void send(final ByteBuffer buffer) {
        final DatagramSocket s = socket;
        if (s == null) return;
        final byte[] data = buffer.array();
        sender.execute(() -> {
            try {
                s.send(new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), MOD_PORT));
            }
            catch (IOException ignored) {}
        });
    }
}
