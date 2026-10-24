package com.winlator.d2;

import com.winlator.d2.test.Fertigkeiten;

import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;











public final class D2SpieltestPort {
    public static final int PORT = 7960;
    public static final String EXTRA = "d2_spieltest";
    private static final long STATE_INTERVAL_MS = 100, LAYOUT_INTERVAL_MS = 2000;

    private static final int QUEUE_LIMIT = 400;

    private static volatile boolean active;
    private static volatile ServerSocket server;
    private static final CopyOnWriteArrayList<Client> clients = new CopyOnWriteArrayList<>();
    private static volatile long lastState, lastLayout;

    private static volatile String lastGoals;

    private static volatile String lastFigur;

    private static volatile String lastQuest;

    private static volatile File saveDir;
    private static volatile String lastSaves;
    private static volatile File modLog;

    private D2SpieltestPort() {}







    public static final String ADB_TOKEN = "/data/local/tmp/d2android-spieltest.ok";
    private static final int SHELL_UID = 2000;
    static final long TOKEN_MAX_AGE_S = 60;





    public static boolean requested(android.content.Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA, false)) return false;
        return tokenFresh("spieltest");
    }


    public static boolean tokenFresh(String who) {
        try {
            android.system.StructStat st = android.system.Os.stat(ADB_TOKEN);
            long age = System.currentTimeMillis() / 1000 - st.st_mtime;
            String why = st.st_uid != SHELL_UID ? "not from the shell user" : (st.st_mode & 0077) != 0 ? "readable or writable by others"
                : age > TOKEN_MAX_AGE_S || age < -5 ? "too old (" + age + " s)" : null;
            D2Trace.log(who + ": adb token " + (why == null ? "ok" : why + ", refused"));
            return why == null;
        }
        catch (Exception e) {
            D2Trace.log(who + ": no adb token (" + e.getMessage() + "), refused");
            return false;
        }
    }

    public static boolean active() {
        return active;
    }








    public static final String TESTER_MARKER = "/data/local/tmp/d2android-spieltester.laeuft";
    static final long TESTER_MAX_AGE_MS = 3000;
    private static volatile long testerCheckedAt;
    private static volatile boolean testerRunning;


    public static boolean playing() {
        if (!active) return false;
        long now = SystemClock.uptimeMillis();
        if (now - testerCheckedAt >= 500) {
            testerCheckedAt = now;
            boolean running = markerFresh(TESTER_MARKER, System.currentTimeMillis());
            if (running != testerRunning) D2Trace.log("spieltest: playtester " + (running ? "running: its touch-layer ways on" : "gone: normal controls"));
            testerRunning = running;
        }
        return testerRunning;
    }


    static boolean markerFresh(String path, long nowMs) {
        try {
            android.system.StructStat st = android.system.Os.stat(path);
            long age = nowMs - st.st_mtime * 1000L;
            return st.st_uid == SHELL_UID && age <= TESTER_MAX_AGE_MS && age > -5000;
        }
        catch (Exception e) {
            return false;
        }
    }


    public static synchronized void start(File log) {
        if (active) return;
        active = true;
        modLog = log;
        Thread t = new Thread(D2SpieltestPort::acceptLoop, "D2SpieltestPort");
        t.setDaemon(true);
        t.start();
        if (log != null) {
            Thread tail = new Thread(D2SpieltestPort::tailLoop, "D2SpieltestModLog");
            tail.setDaemon(true);
            tail.start();
        }
        D2Trace.log("spieltest port " + PORT + " on");
    }

    public static synchronized void stop() {
        active = false;
        testerRunning = false;
        testerCheckedAt = 0;
        leftOut = Collections.emptySet();
        saveDir = null;
        lastSaves = null;
        ServerSocket s = server;
        server = null;
        if (s != null) try { s.close(); } catch (IOException ignored) {}
        for (Client c : clients) c.close();
        clients.clear();
    }

    private static void acceptLoop() {
        try (ServerSocket s = new ServerSocket()) {
            s.setReuseAddress(true);
            s.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT), 2);
            server = s;
            while (active) {
                Socket socket = s.accept();

                socket.shutdownInput();
                Client c = new Client(socket);
                clients.add(c);
                c.offer(new JSONObject().put("t", "hallo").put("v", 2).put("ms", SystemClock.uptimeMillis()).toString());
                String goals = lastGoals;
                if (goals != null) c.offer(goals);
                String figur = lastFigur;
                if (figur != null) c.offer(figur);
                String quest = lastQuest;
                if (quest != null) c.offer(quest);
                String skillsLine = lastSkills;
                if (skillsLine != null) c.offer(skillsLine);
                String saves = lastSaves;
                if (saves != null) c.offer(withTime(saves));
                lastLayout = 0;

                D2ModLink link = mapLink;
                if (link != null) link.mapFetch().reset();
                c.start();
            }
        }
        catch (Exception e) {
            if (active) D2Trace.log("spieltest port: " + e.getMessage());
        }
    }


    private static volatile D2ModLink mapLink;

    private static volatile Fertigkeiten skills;
    private static volatile String lastSkills;
    private static volatile int skillsClass = Integer.MIN_VALUE;





    public static void skills(Fertigkeiten table) {
        skills = table;
        skillsClass = Integer.MIN_VALUE;

        lastSkills = null;
    }

    public static void state(D2ModState s, D2ModLink link) {
        mapLink = link;
        if (!active || clients.isEmpty()) return;
        long now = SystemClock.uptimeMillis();
        if (s == null) {
            if (link != null) send(new JSONObjectBuilder("status").put("ms", now).put("verbunden", link.isConnected()).put("text", link.describe()).text());
            return;
        }
        Fertigkeiten table = skills;
        if (table != null && s.characterClass >= 0 && s.characterClass != skillsClass) {
            JSONObject line = table.alsJson(s.characterClass, now);
            if (line != null) {
                skillsClass = s.characterClass;
                lastSkills = line.toString();
                send(lastSkills);
            }
        }
        if (now - lastState < STATE_INTERVAL_MS) return;
        lastState = now;
        send(toJson(s, now, link));
    }






    public static void goals(byte[] data, int length) {
        if (!active) return;
        String line = goalsJson(data, length, SystemClock.uptimeMillis());
        if (line == null) return;
        lastGoals = line;
        send(line);
    }


    public static void quests(D2Quests q, int seq) {
        if (!active) return;
        String line = D2Quests.json(q, seq, SystemClock.uptimeMillis());
        if (line == null) return;
        lastQuest = line;
        send(line);
    }


    public static void karte(String line) {
        if (active) send(line);
    }


    public static void figur(byte[] data, int length) {
        if (!active) return;
        String line = figurJson(data, length, SystemClock.uptimeMillis());
        if (line == null) return;
        lastFigur = line;
        send(line);
    }









    static String figurJson(byte[] data, int length, long now) {
        try {
            java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(data, 0, length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            b.position(6);
            int seq = b.getShort() & 0xFFFF;
            JSONObject o = new JSONObject().put("t", "figur").put("ms", now).put("seq", seq);
            o.put("gen", b.getShort() & 0xFFFF).put("modMs", b.getInt() & 0xFFFFFFFFL);
            byte[] name = new byte[16];
            b.get(name);
            int end = 0;
            while (end < 16 && name[end] != 0) end++;
            o.put("name", new String(name, 0, end, java.nio.charset.StandardCharsets.ISO_8859_1));
            JSONArray werte = new JSONArray();
            for (int i = 0; i < 4; i++) werte.put(b.getShort() & 0xFFFF);
            o.put("werte", werte);
            o.put("punkte", new JSONArray().put(b.getShort() & 0xFFFF).put(b.getShort() & 0xFFFF));
            o.put("stufe", b.get() & 0xFF).put("gold", b.getInt() & 0xFFFFFFFFL).put("goldTruhe", b.getInt() & 0xFFFFFFFFL);
            o.put("zeiger", b.getInt() & 0xFFFFFFFFL);
            int n = b.get() & 0xFF;
            JSONArray orte = new JSONArray();
            for (int i = 0; i < n; i++) {
                int art = b.get() & 0xFF, txt = b.getShort() & 0xFFFF;
                long id = b.getInt() & 0xFFFFFFFFL;
                orte.put(new JSONArray().put(art).put(txt).put(id).put(b.getShort() & 0xFFFF).put(b.getShort() & 0xFFFF));
            }
            o.put("orte", orte);
            boolean leiche = b.get() != 0;
            int lx = b.getShort() & 0xFFFF, ly = b.getShort() & 0xFFFF;
            o.put("leiche", leiche ? new JSONArray().put(lx).put(ly) : JSONObject.NULL);
            o.put("gegenstaende", itemsJson(b));
            o.put("laden", itemsJson(b));

            o.put("voll", b.hasRemaining() ? b.get() & 0xFF : 0);
            D2ItemDetails.Item[] details = D2ItemDetails.parse(data, length);
            if (details != null) {
                JSONArray list = new JSONArray();
                for (D2ItemDetails.Item d : details) list.put(new JSONArray().put(d.sockets).put(d.filled).put(d.tier).put(d.level));
                o.put("details", list);
            }
            return o.toString();
        }
        catch (Exception e) {
            return null;
        }
    }

    private static JSONArray itemsJson(java.nio.ByteBuffer b) throws org.json.JSONException {
        int n = b.get() & 0xFF;
        JSONArray list = new JSONArray();
        for (int i = 0; i < n; i++) {
            long id = b.getInt() & 0xFFFFFFFFL;
            byte[] code = new byte[4];
            b.get(code);
            int kat = b.get() & 0xFF, q = b.get() & 0xFF, flags = b.get() & 0xFF, platz = b.get() & 0xFF, koerper = b.get() & 0xFF;
            int x = b.get() & 0xFF, y = b.get() & 0xFF, size = b.get() & 0xFF;
            list.put(new JSONArray().put(id).put(new String(code, java.nio.charset.StandardCharsets.US_ASCII).trim()).put(kat).put(q)
                .put(flags).put(platz).put(koerper).put(x).put(y).put(size >> 4).put(size & 15));
        }
        return list;
    }

    static String goalsJson(byte[] data, int length, long now) {
        try {
            java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(data, 0, length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            b.position(6);
            int seq = b.getShort() & 0xFFFF;
            JSONObject o = new JSONObject().put("t", "ziele").put("ms", now).put("seq", seq);
            o.put("gebiet", b.getShort() & 0xFFFF).put("bereit", b.get() & 0xFF);
            o.put("box", new JSONArray().put(b.getShort() & 0xFFFF).put(b.getShort() & 0xFFFF).put(b.getShort() & 0xFFFF).put(b.getShort() & 0xFFFF));
            int n = b.get() & 0xFF;
            JSONArray list = new JSONArray();
            for (int i = 0; i < n; i++) {
                int kind = b.get() & 0xFF, outX = b.get(), outY = b.get(), to = b.getShort() & 0xFFFF;
                int x = b.getShort() & 0xFFFF, y = b.getShort() & 0xFFFF;
                int x0 = b.getShort() & 0xFFFF, y0 = b.getShort() & 0xFFFF, x1 = b.getShort() & 0xFFFF, y1 = b.getShort() & 0xFFFF;
                list.put(new JSONArray().put(kind).put(to).put(x).put(y).put(outX).put(outY).put(x0).put(y0).put(x1).put(y1));
            }
            o.put("ausgaenge", list);
            return o.toString();
        }
        catch (Exception e) {
            return null;
        }
    }


    private static void tailLoop() {
        long pos = -1;
        StringBuilder partial = new StringBuilder();
        while (active) {
            try {
                File f = modLog;
                if (f != null && f.isFile()) {
                    long size = f.length();
                    if (pos < 0 || size < pos) pos = pos < 0 ? size : 0;
                    if (size > pos) {
                        try (RandomAccessFile r = new RandomAccessFile(f, "r")) {
                            r.seek(pos);
                            byte[] buf = new byte[(int) Math.min(size - pos, 256 * 1024)];
                            r.readFully(buf);
                            pos += buf.length;
                            partial.append(new String(buf, StandardCharsets.UTF_8));
                        }
                        int nl;
                        while ((nl = partial.indexOf("\n")) >= 0) {
                            String line = partial.substring(0, nl).replace("\r", "");
                            partial.delete(0, nl + 1);
                            if (!clients.isEmpty()) send(new JSONObjectBuilder("modlog").put("l", line).text());
                        }
                    }
                }
                Thread.sleep(500);
            }
            catch (InterruptedException e) {
                return;
            }
            catch (Exception ignored) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
            }
        }
    }


    public static synchronized void saves(File dir) {
        if (!active || dir == null) return;
        boolean first = saveDir == null;
        saveDir = dir;
        if (!first) return;
        Thread t = new Thread(D2SpieltestPort::savesLoop, "D2SpieltestSaves");
        t.setDaemon(true);
        t.start();
    }

    private static void savesLoop() {
        while (active) {
            try {
                File dir = saveDir;
                String line = dir != null ? savesJson(dir, dir.listFiles()) : null;
                if (line != null && !line.equals(lastSaves)) {
                    lastSaves = line;
                    send(withTime(line));
                }
                Thread.sleep(2000);
            }
            catch (InterruptedException e) {
                return;
            }
            catch (Exception ignored) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
            }
        }
    }


    private static String withTime(String line) {
        try {
            return new JSONObject(line).put("ms", SystemClock.uptimeMillis()).toString();
        }
        catch (Exception e) {
            return line;
        }
    }





    static String savesJson(File dir, File[] files) throws org.json.JSONException {
        java.util.List<File> list = new java.util.ArrayList<>();
        if (files != null) {
            for (File f : files) if (f.isFile() && f.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".d2s")) list.add(f);
        }
        java.util.Collections.sort(list, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        JSONArray a = new JSONArray();
        for (File f : list) {
            String name = f.getName();
            a.put(new JSONArray().put(name.substring(0, name.length() - 4)).put(f.lastModified()));
        }
        return new JSONObject().put("t", "spielstaende").put("ordner", dir.getAbsolutePath()).put("liste", a).toString();
    }


    public static void trace(String line) {
        if (!active || clients.isEmpty()) return;
        send(new JSONObjectBuilder("spur").put("l", line).text());
    }


    public static void layout(Supplier<String> supplier) {
        if (!active || clients.isEmpty()) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastLayout < LAYOUT_INTERVAL_MS) return;
        lastLayout = now;
        String line = supplier.get();
        if (line != null) send(line);
    }

    private static void send(String line) {
        for (Client c : clients) c.offer(line);
    }





    private static volatile Set<Long> leftOut = Collections.emptySet();


    static void leftOut(Set<Long> ids) {
        leftOut = ids == null || ids.isEmpty() ? Collections.<Long>emptySet() : Collections.unmodifiableSet(new HashSet<>(ids));
    }

    private static byte[] waffenPaket;
    private static int waffenZahl = -1;


    static synchronized int waffen(byte[] figur) {
        if (figur == null) return -1;
        if (figur != waffenPaket) {
            waffenPaket = figur;
            String line = figurJson(figur, figur.length, 0);
            try {
                waffenZahl = line == null ? -1 : com.winlator.d2.test.Zustand.Figur.parse(new JSONObject(line)).waffenInHaenden();
            }
            catch (org.json.JSONException e) {
                waffenZahl = -1;
            }
        }
        return waffenZahl;
    }

    static String toJson(D2ModState s, long now) {
        return toJson(s, now, null, null);
    }

    static String toJson(D2ModState s, long now, D2ModLink link) {
        return toJson(s, now, link, leftOut);
    }






    static String toJson(D2ModState s, long now, D2ModLink link, Set<Long> ausgelassen) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "zustand").put("ms", now).put("seq", s.sequence);
            o.put("gw", s.gameWidth).put("gh", s.gameHeight);
            o.put("gebiet", s.levelNo).put("stadt", s.inTown).put("pause", s.paused());
            o.put("fenster", Long.toHexString(s.uiFlags)).put("panel", s.isPanelOpen()).put("npc", s.npcTalking);
            o.put("px", s.playerX).put("py", s.playerY);
            o.put("lebenPct", s.lifePercent).put("manaPct", s.manaPercent);
            o.put("portale", s.portals);
            o.put("leben", s.life).put("lebenMax", s.maxLife).put("mana", s.mana).put("manaMax", s.maxMana);
            o.put("klasse", s.characterClass).put("links", s.leftSkill).put("rechts", s.rightSkill);
            o.put("heilGuertel", s.potionCountBelt[0]).put("manaGuertel", s.potionCountBelt[1]);
            o.put("heilWirkt", s.healingPotionWorking).put("manaWirkt", s.manaPotionWorking).put("letzterTrank", s.lastDrink);

            JSONArray hotkeys = new JSONArray();
            for (int i = 0; i < 16; i++) hotkeys.put(new JSONArray().put(s.hotkeySkill[i]).put(s.hotkeyLeft[i] ? 1 : 0));
            o.put("fk", hotkeys);
            int[] buffs = link != null ? link.buffs() : null, levels = link != null ? link.hotkeyLevels() : null;
            if (buffs != null) {
                JSONArray b = new JSONArray();
                for (int st : buffs) b.put(st);
                o.put("puffer", b);
            }
            if (levels != null) {
                JSONArray b = new JSONArray();
                for (int lv : levels) b.put(lv);
                o.put("fkStufe", b);
            }
            o.put("punkte", (s.statPointsLeft ? 1 : 0) | (s.skillPointsLeft ? 2 : 0));
            o.put("hover", s.hoverType == 0xFF ? -1 : s.hoverType).put("hoverId", s.hoverId);

            int waffen = link != null ? waffen(link.figur()) : -1;
            if (waffen >= 0) o.put("waffen", waffen);
            if (s.worldKnown) {
                o.put("wx", Math.round(s.worldX * 100) / 100.0).put("wy", Math.round(s.worldY * 100) / 100.0);
                o.put("modus", s.playerMode).put("stufe", s.characterLevel).put("erfahrung", s.experience)
                    .put("gold", s.gold).put("goldTruhe", s.goldStash);
            }
            JSONArray monsters = new JSONArray();
            for (D2ModState.Monster m : s.monsters) {
                monsters.put(new JSONArray().put(m.id).put(m.x).put(m.y).put(m.hpPercent).put(m.flags).put(m.name));
            }
            o.put("monster", monsters);
            o.put("items", things(s.items));

            D2ModItems details = link != null ? link.items() : null;
            if (details != null) {
                JSONArray beute = new JSONArray();
                for (D2ModItems.Item it : details.items) {
                    beute.put(new JSONArray().put(it.id).put(it.category).put(it.quality)
                        .put((it.ethereal ? 1 : 0) | (it.socketed ? 2 : 0) | (it.identified ? 4 : 0) | (it.runeword ? 8 : 0))
                        .put(it.sockets).put(it.tier).put(it.width).put(it.height).put(-1));
                }
                o.put("beute", beute).put("zellen", details.usedCells);
            }
            o.put("objekte", things(s.objects));

            D2Effects.Effect[] effects = link != null ? link.effects() : null;
            if (effects != null) {
                JSONArray sr = new JSONArray();
                for (D2Effects.Effect e : effects) if (e.state >= 128 && e.state <= 137) sr.put(e.state);
                o.put("schreine", sr);
            }


            long[] objectDetails = link != null ? link.objectDetails(2000) : null;
            if (objectDetails != null) {
                JSONArray od = new JSONArray();
                for (int i = 0; i + 2 < objectDetails.length; i += 3) {
                    od.put(new JSONArray().put(objectDetails[i]).put(objectDetails[i + 1]).put(objectDetails[i + 2]));
                }
                o.put("objektDetails", od);
            }


            JSONArray hb = new JSONArray();
            for (int i = 0; i < s.damagedLocation.length; i++) hb.put(new JSONArray().put(s.damagedLocation[i]).put(s.damagedState[i]));
            o.put("haltbarkeit", hb);
            JSONArray mn = new JSONArray();
            if (s.npcMenuIds != null) for (int id : s.npcMenuIds) mn.put(id);
            o.put("menue", mn);
            D2PotionShop.Job job = details != null ? details.shopJob : null;
            if (job != null && job.command == D2PotionShop.REPAIR_COMMAND) {
                o.put("reparatur", new JSONArray().put(job.id).put(job.state).put(job.reason).put(job.a).put(job.b));
            }
            o.put("leichen", things(s.corpses));
            if (ausgelassen != null && !ausgelassen.isEmpty()) {
                JSONArray left = new JSONArray();
                for (long id : ausgelassen) left.put(id);
                o.put("ausgelassen", left);
            }
            return o.toString();
        }
        catch (Exception e) {
            return "{\"t\":\"fehler\",\"text\":" + JSONObject.quote(String.valueOf(e)) + "}";
        }
    }

    private static JSONArray things(java.util.List<D2ModState.Thing> list) {
        JSONArray a = new JSONArray();
        if (list == null) return a;
        for (D2ModState.Thing t : list) a.put(new JSONArray().put(t.id).put(t.x).put(t.y).put(t.kind).put(t.extra));
        return a;
    }


    static final class JSONObjectBuilder {
        private final JSONObject o = new JSONObject();

        JSONObjectBuilder(String type) {
            put("t", type);
        }

        JSONObjectBuilder put(String key, Object value) {
            try {
                o.put(key, value);
            }
            catch (Exception ignored) {}
            return this;
        }

        String text() {
            return o.toString();
        }
    }

    private static final class Client extends Thread {
        private final Socket socket;
        private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        private volatile boolean closed;

        Client(Socket socket) {
            super("D2SpieltestClient");
            setDaemon(true);
            this.socket = socket;
        }

        void offer(String line) {
            if (closed) return;
            if (queue.size() >= QUEUE_LIMIT) queue.poll();
            queue.offer(line);
        }

        void close() {
            closed = true;
            try { socket.close(); } catch (IOException ignored) {}
            interrupt();
        }

        @Override
        public void run() {
            try (OutputStream out = socket.getOutputStream()) {
                while (!closed && active) {
                    String line = queue.take();
                    out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    if (queue.isEmpty()) out.flush();
                }
            }
            catch (Exception ignored) {}
            finally {
                closed = true;
                clients.remove(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
