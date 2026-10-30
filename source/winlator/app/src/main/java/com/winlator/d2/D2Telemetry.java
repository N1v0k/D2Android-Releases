package com.winlator.d2;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.Display;
import android.view.InputDevice;

import org.json.JSONObject;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;











public final class D2Telemetry {
    private static final String PREFS = "d2_telemetry", KEY_QUIET = "quiet";

    private static final String SESSION = UUID.randomUUID().toString();
    private static final Object LOCK = new Object();
    private static final java.util.concurrent.atomic.AtomicBoolean RUNNING = new java.util.concurrent.atomic.AtomicBoolean();

    private D2Telemetry() {}

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }





    private static final class Prefs implements D2TelemetryState.Store {
        final SharedPreferences prefs;
        final SharedPreferences.Editor edit;
        final Map<String, String> changed = new java.util.HashMap<>();

        Prefs(Context context) {
            prefs = prefs(context);
            edit = prefs.edit();
        }

        @Override public String get(String key) {
            return changed.containsKey(key) ? changed.get(key) : prefs.getString(key, null);
        }

        @Override public void put(String key, String value) {
            changed.put(key, value);
            edit.putString(key, value);
        }

        @Override public void remove(String key) {
            changed.put(key, null);
            edit.remove(key);
        }

        void done(boolean now) {
            if (now) edit.commit();
            else edit.apply();
        }
    }

    private static String key() {
        return com.winlator.BuildConfig.D2_APTABASE_KEY;
    }


    static boolean sends() {
        return D2TelemetryEvents.active(com.winlator.BuildConfig.D2_INTERNAL, key(), true);
    }


    static boolean enabled(Context context) {
        return D2TelemetryEvents.active(com.winlator.BuildConfig.D2_INTERNAL, key(), D2Settings.telemetry(context));
    }

    private static boolean collecting(Context context) {
        return enabled(context) && !prefs(context).getBoolean(KEY_QUIET, false);
    }


    public static void clear(Context context) {
        synchronized (LOCK) {
            Prefs p = new Prefs(context);
            D2TelemetryState.clear(p);
            p.done(false);
        }
    }





    static void systemExit(Context context, int reason, int status, long timestamp) {
        String code = D2TelemetryEvents.exitReason(reason);
        if (code == null || !collecting(context)) return;
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("reason", code);

        props.put("status", status);
        keepCrash(context, props, timestamp, false);
    }


    private static void keepCrash(Context context, Map<String, Object> props, long time, boolean now) {
        synchronized (LOCK) {
            D2TelemetryState.run(new Prefs(context), props, time);
        }
        props.putAll(deviceProps(context));
        JSONObject event = D2TelemetryEvents.event(D2TelemetryEvents.CRASH, time, SESSION, device(context), props);
        synchronized (LOCK) {
            Prefs p = new Prefs(context);
            D2TelemetryState.keepCrash(p, event, System.currentTimeMillis());
            p.done(now);
        }
    }


    private static volatile boolean gameActive;





    static void gameStarted(Context context, boolean worldSignal) {
        if (!collecting(context)) return;
        gameActive = true;
        synchronized (LOCK) {
            Prefs p = new Prefs(context);
            D2TelemetryState.gameStarted(p, System.currentTimeMillis(), worldSignal);
            p.done(true);
        }
    }






    static void phase(Context context, String phase) {
        if (!collecting(context)) return;
        synchronized (LOCK) {
            Prefs p = new Prefs(context);
            D2TelemetryState.phase(p, phase, System.currentTimeMillis());
            p.done(true);
        }
    }


    private static volatile int audioState = -1;


    public static void audioTrack(boolean ok) {
        if (ok) audioState = 1;
        else if (audioState < 0) audioState = 0;
    }






    static GameWatch watchGame(Context context, java.util.function.BooleanSupplier inWorld) {

        boolean store = collecting(context);
        if (!store && !com.winlator.BuildConfig.D2_INTERNAL) return null;
        GameWatch w = new GameWatch(context.getApplicationContext(), inWorld, store);
        w.start();
        return w;
    }

    static final class GameWatch extends Thread {
        private static final long TICK_MS = 5000;
        private final Context context;
        private final java.util.function.BooleanSupplier inWorld;
        private final long startedAt = android.os.SystemClock.elapsedRealtime();
        private volatile boolean stopped;

        private boolean store;
        private int gamePid = -1;

        GameWatch(Context context, java.util.function.BooleanSupplier inWorld, boolean store) {
            super("statistics-game");
            setDaemon(true);
            this.context = context;
            this.inWorld = inWorld;
            this.store = store;
        }


        void end() {
            stopped = true;
            interrupt();
        }

        @Override
        public void run() {
            boolean world = false;
            long nextPss = startedAt + D2TelemetryEvents.PSS_EVERY_MS;
            while (!stopped) {
                try {
                    Thread.sleep(TICK_MS);
                }
                catch (InterruptedException e) {
                    return;
                }
                if (stopped) return;
                try {
                    if (!world && inWorld.getAsBoolean()) {
                        world = true;
                        D2Trace.log("statistics: phase game after " + (android.os.SystemClock.elapsedRealtime() - startedAt) / 1000 + " s");
                        if (store) phase(context, D2TelemetryEvents.PHASE_GAME);
                    }
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (now < nextPss) continue;
                    nextPss = now + D2TelemetryEvents.PSS_EVERY_MS;

                    if (store && !collecting(context)) store = false;
                    if (!store && !com.winlator.BuildConfig.D2_INTERNAL) return;
                    int mb = D2TelemetryEvents.mb(gamePss());
                    int minute = (int) ((now - startedAt) / 60_000);
                    D2Trace.log("statistics: Game.exe pss " + mb + " MB at minute " + minute + ", audio track " + audioState);
                    if (!store) continue;
                    synchronized (LOCK) {
                        if (!collecting(context)) {
                            store = false;
                            if (!com.winlator.BuildConfig.D2_INTERNAL) return;
                            continue;
                        }
                        Prefs p = new Prefs(context);
                        D2TelemetryState.pss(p, minute, mb);

                        D2TelemetryState.audio(p, audioState > 0);
                        p.done(false);
                    }
                }
                catch (RuntimeException e) {
                    D2Trace.log("statistics: game watch " + e.getClass().getSimpleName());
                }
            }
        }


        private long gamePss() {
            long pss = gamePid > 0 ? pss(gamePid) : -1;
            if (pss >= 0) return pss;
            File proc = new File("/proc");
            List<D2GuestProcs.Proc> guests = D2GuestProcs.find(proc, D2Runtime.rootDir(context), android.os.Process.myUid(),
                android.os.Process.myPid(), null);
            int[] pids = new int[guests.size()];
            for (int i = 0; i < pids.length; i++) pids[i] = guests.get(i).pid;
            gamePid = D2StallSnapshot.gamePid(proc, pids);
            return gamePid > 0 ? pss(gamePid) : -1;
        }

        private static long pss(int pid) {
            try {
                return D2DeviceInfo.pssBytes(new String(java.nio.file.Files.readAllBytes(
                    new File("/proc/" + pid + "/smaps_rollup").toPath()), java.nio.charset.StandardCharsets.UTF_8));
            }
            catch (java.io.IOException | RuntimeException e) {
                return -1;
            }
        }
    }





    static void gameEnded(Context context, int status, boolean byApp, long playedMs) {
        if (!collecting(context)) return;
        int end = D2TelemetryEvents.sessionEnd(status, byApp);
        gameActive = false;
        synchronized (LOCK) {
            Prefs p = new Prefs(context);
            D2TelemetryState.gameEnded(p, end, D2TelemetryEvents.minutes(playedMs));
            p.done(true);
        }
        if (end == 2) {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("reason", D2TelemetryEvents.R_GAME);
            props.put("status", Math.max(-1, Math.min(255, status)));
            keepCrash(context, props, System.currentTimeMillis(), true);
        }
        phase(context, D2TelemetryEvents.PHASE_APP);
    }






    public static void start(Context context, boolean playtester) {
        Context app = context.getApplicationContext();
        if (com.winlator.BuildConfig.D2_INTERNAL && !playtester) dump(app);
        prefs(app).edit().putBoolean(KEY_QUIET, playtester).apply();
        if (!D2Settings.telemetry(app)) {
            clear(app);
            return;
        }
        if (playtester) {
            synchronized (LOCK) {
                Prefs p = new Prefs(app);
                D2TelemetryState.playtester(p);
                p.done(false);
            }
            return;
        }
        if (!enabled(app)) return;
        phase(app, D2TelemetryEvents.PHASE_APP);

        if (!RUNNING.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                run(app);
            }
            catch (RuntimeException e) {
                D2Trace.log("statistics: " + e.getClass().getSimpleName());
            }
            finally {
                RUNNING.set(false);
            }
        }, "statistics").start();
    }

    private static void run(Context app) {
        refreshDevice(app);
        long now = System.currentTimeMillis();
        String queue;
        synchronized (LOCK) {
            if (!enabled(app)) return;
            Prefs p = new Prefs(app);
            queue = D2TelemetryState.prepare(p, now,
                (sessions) -> D2TelemetryEvents.event(D2TelemetryEvents.DAILY, now, SESSION, device(app), daily(app, sessions)),
                !gameActive);
            p.done(false);
        }
        String body = D2TelemetryEvents.body(D2TelemetryEvents.fresh(queue, now));
        if (body == null || !online(app)) return;

        if (!enabled(app) || prefs(app).getString(D2TelemetryState.QUEUE, null) == null) return;
        int code = D2TelemetryEvents.post(D2TelemetryEvents.endpoint(key()), key(), body, D2TelemetryEvents.TIMEOUT_MS);
        D2Trace.log("statistics: sent, answer " + code);
        synchronized (LOCK) {
            Prefs p = new Prefs(app);
            D2TelemetryState.sent(p, queue, code);
            p.done(false);
        }
    }

    private static boolean online(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo n = cm != null ? cm.getActiveNetworkInfo() : null;
            return n != null && n.isConnected();
        }
        catch (RuntimeException e) {
            return false;
        }
    }


    private static D2TelemetryEvents.Device device(Context context) {
        String lang;
        try {
            lang = D2Text.language(context);
        }
        catch (RuntimeException e) {
            lang = "";
        }
        return new D2TelemetryEvents.Device(Build.VERSION.RELEASE, lang, D2Updates.installedVersion(context),
            D2Updates.installedCode(context), Build.MODEL);
    }


    private static Map<String, Object> daily(Context context, D2TelemetryEvents.Sessions sessions) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.putAll(deviceProps(context));
        float[] rates = refreshRates(context);
        float max = 0;
        for (float r : rates) max = Math.max(max, r);
        p.put("hz", Math.round(max));
        p.put("fps", D2Settings.frameRate(context, rates));
        p.put("lines", D2Settings.gameLines(context));
        p.put("aim", D2Settings.aimAssist(context));
        p.put("resume", D2Settings.resume(context));
        p.put("lang", D2Settings.appLanguage(context));
        String gameLang = D2Settings.gameLanguage(context);
        p.put("gameLang", gameLang != null ? gameLang : "none");
        p.put("padOn", D2Settings.padEnabled(context));
        p.put("pad", padConnected());
        List<D2SaveHeader.Info> characters;
        try {
            File dir = GameLocator.gameDir(context);
            characters = dir != null ? D2SaveHeader.list(D2Saves.saveDir(dir)) : new java.util.ArrayList<>();
        }
        catch (RuntimeException e) {
            characters = new java.util.ArrayList<>();
        }
        D2TelemetryEvents.saves(characters, p);
        sessions.put(p);
        return p;
    }



    private static final String KEY_DEVICE = "device", KEY_DEVICE_FOR = "deviceFor", KEY_VK_PROBE = "vkProbe";

    private static final long VULKAN_TIMEOUT_MS = 2000;


    private static String deviceFor(Context context) {
        return D2Updates.installedCode(context) + "/" + Build.VERSION.SDK_INT + "/" + Build.VERSION.INCREMENTAL;
    }





    static Map<String, Object> deviceProps(Context context) {
        Map<String, Object> out = new LinkedHashMap<>();
        String stored = prefs(context).getString(KEY_DEVICE, null);
        if (stored != null && deviceFor(context).equals(prefs(context).getString(KEY_DEVICE_FOR, null))) {
            try {
                JSONObject o = new JSONObject(stored);
                for (java.util.Iterator<String> i = o.keys(); i.hasNext(); ) {
                    String k = i.next();
                    out.put(k, o.get(k));
                }
                return out;
            }
            catch (org.json.JSONException ignored) {
                out.clear();
            }
        }
        basics(context, out);
        return out;
    }


    private static void basics(Context context, Map<String, Object> out) {
        out.put("maker", Build.MANUFACTURER);
        out.put("model", Build.MODEL);
        long ram = -1;
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                android.app.ActivityManager.MemoryInfo memory = new android.app.ActivityManager.MemoryInfo();
                am.getMemoryInfo(memory);
                ram = memory.totalMem;
            }
        }
        catch (RuntimeException ignored) {
        }
        out.put("ramMb", D2TelemetryEvents.ramMb(ram));
        String socMaker = "", soc = "";
        if (Build.VERSION.SDK_INT >= 31) {
            socMaker = Build.SOC_MANUFACTURER;
            soc = Build.SOC_MODEL;
        }
        if (soc == null || soc.isEmpty() || Build.UNKNOWN.equals(soc)) soc = Build.HARDWARE;
        out.put("socMaker", socMaker != null && !Build.UNKNOWN.equals(socMaker) ? socMaker : "");
        out.put("soc", soc != null ? soc : "");
        out.put("abi", Build.SUPPORTED_ABIS.length > 0 ? Build.SUPPORTED_ABIS[0] : "");
        out.put("sdk", Build.VERSION.SDK_INT);

        int burst = -1, rate = -1;
        try {
            android.media.AudioManager audio = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                burst = number(audio.getProperty(android.media.AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER));
                rate = number(audio.getProperty(android.media.AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE));
            }
        }
        catch (RuntimeException ignored) {
        }
        out.put("audioBurst", burst);
        out.put("audioRate", rate);
    }

    private static int number(String text) {
        try {
            return text != null ? Integer.parseInt(text.trim()) : -1;
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }






    private static void refreshDevice(Context app) {
        String key = deviceFor(app);
        if (key.equals(prefs(app).getString(KEY_DEVICE_FOR, null)) && prefs(app).getString(KEY_DEVICE, null) != null) return;
        Map<String, Object> out = new LinkedHashMap<>();
        basics(app, out);
        D2DeviceProbe probe = D2DeviceProbe.basics();
        out.put("pageKb", probe.pageSize > 0 ? probe.pageSize / 1024 : -1);
        if (!probe.ruledOutEarly()) probe.readGpu();
        String gpu = probe.gpuName();
        out.put("gpu", gpu != null ? gpu : "unknown");
        out.put("glVer", probe.glVersion.isEmpty() ? "unknown" : D2TelemetryEvents.glVersion(probe.glVersion));
        vulkan(app, key, probe, out);
        D2DeviceCheck.Result check = probe.classify();
        out.put("check", check.level.name());
        out.put("checkWhy", check.reason.name());
        out.put("gpuPath", gpuPath(check));
        try {
            JSONObject o = new JSONObject();
            for (Map.Entry<String, Object> e : out.entrySet()) o.put(e.getKey(), e.getValue());
            prefs(app).edit().putString(KEY_DEVICE, o.toString()).putString(KEY_DEVICE_FOR, key).apply();
        }
        catch (org.json.JSONException ignored) {
        }
    }





    private static void dump(Context app) {
        if (!RUNNING.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                refreshDevice(app);
                D2Trace.log("statistics (internal, never sent): device " + new JSONObject(deviceProps(app)));
            }
            catch (RuntimeException e) {
                D2Trace.log("statistics: " + e.getClass().getSimpleName());
            }
            finally {
                RUNNING.set(false);
            }
        }, "statistics").start();
    }





    static String gpuPath(D2DeviceCheck.Result check) {
        return D2TelemetryEvents.gpuPath(check.level.name(), D2DeviceCheck.vortek(check));
    }







    private static void vulkan(Context app, String key, D2DeviceProbe probe, Map<String, Object> out) {
        String api = "unknown", driver = "unknown", vendor = "unknown";
        if (probe.ruledOutEarly()) {
            api = driver = vendor = "skipped";
        }
        else if (key.equals(prefs(app).getString(KEY_VK_PROBE, null))) {
            api = driver = vendor = "failed";
        }
        else {
            prefs(app).edit().putString(KEY_VK_PROBE, key).commit();
            long[][] got = {null};
            java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
            Thread t = new Thread(() -> {
                try {
                    got[0] = com.winlator.core.GPUHelper.vkGetDeviceVersions();
                }
                catch (Throwable ignored) {

                }
                finally {
                    done.countDown();
                }
            }, "statistics-vulkan");
            t.setDaemon(true);
            t.start();
            boolean inTime = false;
            try {
                inTime = done.await(VULKAN_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!inTime) api = driver = vendor = "timeout";
            else {
                long[] v = got[0];
                if (v != null && v.length == 3) {
                    probe.vulkanVersion = (int) v[0];
                    api = D2TelemetryEvents.vkVersion(v[0]);
                    driver = D2TelemetryEvents.vkVersion(v[1]);
                    vendor = "0x" + Long.toHexString(v[2]);
                }
                else api = driver = vendor = "none";
                prefs(app).edit().remove(KEY_VK_PROBE).apply();
            }
        }
        out.put("vkApi", api);
        out.put("vkDrv", driver);
        out.put("vkVendor", vendor);
    }

    private static float[] refreshRates(Context context) {
        try {
            DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            Display d = dm != null ? dm.getDisplay(Display.DEFAULT_DISPLAY) : null;
            if (d == null) return new float[0];
            Display.Mode[] modes = d.getSupportedModes();
            float[] out = new float[modes.length];
            for (int i = 0; i < modes.length; i++) out[i] = modes[i].getRefreshRate();
            return out;
        }
        catch (RuntimeException e) {
            return new float[0];
        }
    }

    private static boolean padConnected() {
        int pad = InputDevice.SOURCE_GAMEPAD, joystick = InputDevice.SOURCE_JOYSTICK;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null || d.isVirtual()) continue;
            int s = d.getSources();
            if ((s & pad) == pad || (s & joystick) == joystick) return true;
        }
        return false;
    }
}
