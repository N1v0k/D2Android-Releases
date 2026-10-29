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





    static void systemExit(Context context, int reason, long timestamp) {
        String code = D2TelemetryEvents.exitReason(reason);
        if (code == null || !collecting(context)) return;
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("reason", code);
        keepCrash(context, props, timestamp, false);
    }

    private static void keepCrash(Context context, Map<String, Object> props, long time, boolean now) {
        JSONObject event = D2TelemetryEvents.event(D2TelemetryEvents.CRASH, time, SESSION, device(context), props);
        synchronized (LOCK) {
            Prefs p = new Prefs(context);
            D2TelemetryState.keepCrash(p, event, System.currentTimeMillis());
            p.done(now);
        }
    }


    static void gameStarted(Context context) {
        if (!collecting(context)) return;
        synchronized (LOCK) {
            Prefs p = new Prefs(context);
            D2TelemetryState.gameStarted(p);
            p.done(false);
        }
    }





    static void gameEnded(Context context, int status, boolean byApp, long playedMs) {
        if (!collecting(context)) return;
        int end = D2TelemetryEvents.sessionEnd(status, byApp);
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
    }






    public static void start(Context context, boolean playtester) {
        Context app = context.getApplicationContext();
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
        long now = System.currentTimeMillis();
        String queue;
        synchronized (LOCK) {
            if (!enabled(app)) return;
            Prefs p = new Prefs(app);
            queue = D2TelemetryState.prepare(p, now,
                (sessions) -> D2TelemetryEvents.event(D2TelemetryEvents.DAILY, now, SESSION, device(app), daily(app, sessions)));
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
        p.put("maker", Build.MANUFACTURER);
        D2DeviceProbe probe = D2DeviceProbe.basics();
        probe.readGpu();
        String gpu = probe.gpuName();
        p.put("gpu", gpu != null ? gpu : "unknown");
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
