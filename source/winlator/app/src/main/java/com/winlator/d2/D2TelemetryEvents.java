package com.winlator.d2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;










public final class D2TelemetryEvents {
    public static final String EVENTS_PATH = "/api/v0/events";
    public static final String DAILY = "daily", CRASH = "crash";
    public static final long DAY_MS = 24L * 60 * 60 * 1000;

    public static final long MAX_AGE_MS = 23L * 60 * 60 * 1000;

    public static final int MAX_QUEUE = 10;

    public static final int CRASHES_PER_DAY = 3;

    public static final int TIMEOUT_MS = 5000;

    static final int MAX_KEY = 40, MAX_TEXT = 60;

    public static final String R_JAVA = "java", R_NATIVE = "native", R_ANR = "anr", R_LOW_MEMORY = "lowMemory",
        R_RESOURCES = "resources", R_SIGNALED = "signaled", R_DEPENDENCY = "dependency", R_OTHER = "other",
        R_GAME = "gameError";

    public static final String[] CLASSES = {"ama", "sor", "nec", "pal", "bar", "dru", "ass"};

    private D2TelemetryEvents() {}





    public static String endpoint(String key) {
        if (key == null || !key.matches("A-(EU|US)-[0-9]{6,20}")) return null;
        return "https://" + (key.startsWith("A-EU-") ? "eu" : "us") + ".aptabase.com" + EVENTS_PATH;
    }


    public static boolean active(boolean internal, String key, boolean switchOn) {
        return !internal && switchOn && endpoint(key) != null;
    }


    public static boolean dailyDue(long lastDaily, long now) {
        return lastDaily <= 0 || now - lastDaily >= DAY_MS || now < lastDaily;
    }


    public static boolean crashAllowed(long dayStart, int count, long now) {
        return dailyDue(dayStart, now) || count < CRASHES_PER_DAY;
    }





    public static String exitReason(int reason) {
        switch (reason) {
            case 4: return R_JAVA;
            case 5: return R_NATIVE;
            case 6: return R_ANR;
            case 3: return R_LOW_MEMORY;
            case 9: return R_RESOURCES;
            case 2: return R_SIGNALED;
            case 12: return R_DEPENDENCY;
            case 13: return R_OTHER;
            default: return null;
        }
    }


    public static int levelStep(int level) {
        if (level <= 0) return 0;
        if (level >= 99) return 99;
        return Math.max(1, level / 5 * 5);
    }






    static void saves(java.util.List<D2SaveHeader.Info> characters, Map<String, Object> props) {
        int hc = 0, max = 0;
        int[] perClass = new int[CLASSES.length];
        for (D2SaveHeader.Info c : characters) {
            if (c.hardcore()) hc++;
            max = Math.max(max, c.level);
            if (c.characterClass >= 0 && c.characterClass < CLASSES.length) perClass[c.characterClass]++;
        }
        props.put("chars", characters.size());
        props.put("hcChars", hc);
        props.put("maxLvl", levelStep(max));
        for (int i = 0; i < CLASSES.length; i++) props.put(CLASSES[i], perClass[i]);
        if (characters.isEmpty()) return;
        D2SaveHeader.Info last = characters.get(0);
        props.put("lastCls", last.characterClass >= 0 && last.characterClass < CLASSES.length ? CLASSES[last.characterClass] : "unknown");
        props.put("lastLvl", levelStep(last.level));
        props.put("lastAct", last.lastAct + 1);
        props.put("lastDiff", Math.max(0, last.lastDifficulty));
        props.put("lastHc", last.hardcore());
    }


    public static final class Sessions {
        public final int started, endGame, endApp, endError, minutes;

        public Sessions(int started, int endGame, int endApp, int endError, int minutes) {
            this.started = Math.max(0, started);
            this.endGame = Math.max(0, endGame);
            this.endApp = Math.max(0, endApp);
            this.endError = Math.max(0, endError);
            this.minutes = Math.max(0, minutes);
        }

        void put(Map<String, Object> props) {
            props.put("sessions", started);
            props.put("endGame", endGame);
            props.put("endApp", endApp);
            props.put("endError", endError);

            props.put("endLost", Math.max(0, started - endGame - endApp - endError));
            props.put("minutes", minutes);
        }
    }


    public static int sessionEnd(int status, boolean byApp) {
        return byApp ? 1 : status == 0 ? 0 : 2;
    }


    public static int minutes(long ms) {
        return (int) Math.max(0, Math.min(7L * 24 * 60, ms / 60_000));
    }


    public static String clean(String text) {
        if (text == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.length() && b.length() < MAX_TEXT; i++) {
            char c = text.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                || c == ' ' || c == '.' || c == '_' || c == '(' || c == ')' || c == '-';
            b.append(ok ? c : ' ');
        }
        return b.toString().replaceAll(" +", " ").trim();
    }


    public static String time(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(millis));
    }


    public static final class Device {
        final String osVersion, locale, appVersion, appBuild, deviceModel;

        public Device(String osVersion, String locale, String appVersion, long appBuild, String deviceModel) {
            this.osVersion = clean(osVersion);
            this.locale = clean(locale);
            this.appVersion = clean(appVersion);
            this.appBuild = String.valueOf(appBuild);
            this.deviceModel = clean(deviceModel);
        }
    }





    public static JSONObject event(String name, long millis, String sessionId, Device sys, Map<String, ?> props) {
        try {
            JSONObject s = new JSONObject();

            s.put("osName", "Android");

            s.put("osVersion", sys.osVersion);
            s.put("locale", sys.locale);
            s.put("appVersion", sys.appVersion);
            s.put("appBuildNumber", sys.appBuild);
            s.put("deviceModel", sys.deviceModel);
            s.put("isDebug", false);
            s.put("sdkVersion", "d2android@1");
            JSONObject p = new JSONObject();
            if (props != null) for (Map.Entry<String, ?> e : props.entrySet()) {
                String k = e.getKey();
                if (k == null || k.isEmpty() || k.length() > MAX_KEY || !k.matches("[A-Za-z0-9_]+")) continue;
                Object v = e.getValue();
                if (v instanceof Boolean) p.put(k, (Boolean) v ? 1 : 0);
                else if (v instanceof Integer || v instanceof Long || v instanceof Short) p.put(k, ((Number) v).longValue());
                else if (v instanceof String) p.put(k, clean((String) v));
            }
            JSONObject o = new JSONObject();
            o.put("timestamp", time(millis));
            o.put("sessionId", clean(sessionId));
            o.put("eventName", clean(name));
            o.put("systemProps", s);
            o.put("props", p);
            o.put("t", millis);
            return o;
        }
        catch (JSONException e) {
            return null;
        }
    }


    public static JSONArray fresh(String queue, long now) {
        JSONArray out = new JSONArray();
        if (queue == null || queue.isEmpty()) return out;
        try {
            JSONArray in = new JSONArray(queue);
            for (int i = 0; i < in.length(); i++) {
                JSONObject o = in.optJSONObject(i);
                if (o == null) continue;
                long t = o.optLong("t", 0);
                if (t > 0 && t <= now + 60_000 && now - t < MAX_AGE_MS) out.put(o);
            }
        }
        catch (JSONException e) {

        }
        return out;
    }


    public static String add(String queue, JSONObject event, long now) {
        JSONArray q = fresh(queue, now);
        if (event != null && fresh(new JSONArray().put(event).toString(), now).length() == 1) q.put(event);
        JSONArray out = new JSONArray();
        for (int i = Math.max(0, q.length() - MAX_QUEUE); i < q.length(); i++) out.put(q.opt(i));
        return out.toString();
    }


    public static String body(JSONArray events) {
        if (events == null || events.length() == 0) return null;
        try {
            JSONArray out = new JSONArray();
            for (int i = 0; i < events.length(); i++) {
                JSONObject o = new JSONObject(events.getJSONObject(i).toString());
                o.remove("t");
                out.put(o);
            }
            return out.toString();
        }
        catch (JSONException e) {
            return null;
        }
    }


    public static boolean keepAfter(int code) {
        return code < 200 || code >= 500 || code == 429;
    }


    public static int post(String url, String key, String body, int timeoutMs) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(timeoutMs);
            c.setReadTimeout(timeoutMs);
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setUseCaches(false);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("App-Key", key);
            c.setRequestProperty("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = c.getOutputStream()) {
                out.write(bytes);
            }
            int code = c.getResponseCode();
            drain(code >= 400 ? c.getErrorStream() : c.getInputStream());
            return code;
        }
        catch (Exception e) {
            return -1;
        }
        finally {
            if (c != null) c.disconnect();
        }
    }

    private static void drain(InputStream in) {
        if (in == null) return;
        try (InputStream i = in) {
            ByteArrayOutputStream sink = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int n; (n = i.read(buffer)) > 0 && sink.size() < 16 * 1024; ) sink.write(buffer, 0, n);
        }
        catch (Exception ignored) {

        }
    }
}
