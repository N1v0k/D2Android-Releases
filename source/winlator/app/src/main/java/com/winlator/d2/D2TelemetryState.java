package com.winlator.d2;

import org.json.JSONArray;
import org.json.JSONObject;






public final class D2TelemetryState {
    static final String QUEUE = "queue", CRASHES = "crashes", LAST_DAILY = "lastDaily", CRASH_DAY = "crashDay",
        CRASH_COUNT = "crashCount", STARTED = "started", END_GAME = "endGame", END_APP = "endApp", END_ERROR = "endError",
        MINUTES = "minutes";
    private static final String[] GAMES = {STARTED, END_GAME, END_APP, END_ERROR, MINUTES};


    public interface Store {
        String get(String key);
        void put(String key, String value);
        void remove(String key);
    }


    public interface Daily {
        JSONObject build(D2TelemetryEvents.Sessions sessions);
    }

    private D2TelemetryState() {}

    private static long number(Store s, String key) {
        try {
            String v = s.get(key);
            return v != null ? Long.parseLong(v) : 0;
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void add(Store s, String key, long by) {
        s.put(key, String.valueOf(number(s, key) + by));
    }


    public static void clear(Store s) {
        s.remove(QUEUE);
        s.remove(CRASHES);
        for (String k : GAMES) s.remove(k);
    }


    public static void playtester(Store s) {
        s.remove(CRASHES);
    }


    public static void keepCrash(Store s, JSONObject event, long now) {
        if (event != null) s.put(CRASHES, D2TelemetryEvents.add(s.get(CRASHES), event, now));
    }

    public static void gameStarted(Store s) {
        add(s, STARTED, 1);
    }


    public static void gameEnded(Store s, int end, int minutes) {
        add(s, end == 0 ? END_GAME : end == 1 ? END_APP : END_ERROR, 1);
        add(s, MINUTES, Math.max(0, minutes));
    }

    public static D2TelemetryEvents.Sessions sessions(Store s) {
        return new D2TelemetryEvents.Sessions((int) number(s, STARTED), (int) number(s, END_GAME), (int) number(s, END_APP),
            (int) number(s, END_ERROR), (int) number(s, MINUTES));
    }





    public static String prepare(Store s, long now, Daily daily) {
        String queue = s.get(QUEUE);
        JSONArray crashes = D2TelemetryEvents.fresh(s.get(CRASHES), now);
        long day = number(s, CRASH_DAY);
        int count = (int) number(s, CRASH_COUNT);
        for (int i = 0; i < crashes.length(); i++) {
            if (!D2TelemetryEvents.crashAllowed(day, count, now)) break;
            if (D2TelemetryEvents.dailyDue(day, now)) {
                day = now;
                count = 0;
            }
            count++;
            queue = D2TelemetryEvents.add(queue, crashes.optJSONObject(i), now);
        }
        s.remove(CRASHES);
        s.put(CRASH_DAY, String.valueOf(day));
        s.put(CRASH_COUNT, String.valueOf(count));
        if (D2TelemetryEvents.dailyDue(number(s, LAST_DAILY), now)) {
            JSONObject event = daily.build(sessions(s));
            if (event != null) {
                queue = D2TelemetryEvents.add(queue, event, now);
                s.put(LAST_DAILY, String.valueOf(now));
                for (String k : GAMES) s.remove(k);
            }
        }
        queue = D2TelemetryEvents.add(queue, null, now);
        s.put(QUEUE, queue);
        return queue;
    }





    public static void sent(Store s, String sent, int code) {
        if (D2TelemetryEvents.keepAfter(code)) return;
        if (sent != null && sent.equals(s.get(QUEUE))) s.remove(QUEUE);
    }
}
