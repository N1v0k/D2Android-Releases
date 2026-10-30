package com.winlator.d2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;






public final class D2TelemetryState {
    static final String QUEUE = "queue", CRASHES = "crashes", LAST_DAILY = "lastDaily", CRASH_DAY = "crashDay",
        CRASH_COUNT = "crashCount", STARTED = "started", END_GAME = "endGame", END_APP = "endApp", END_ERROR = "endError",
        MINUTES = "minutes";





    static final String PHASE = "phase", PHASE_AT = "phaseAt", GAME_AT = "gameAt", RUN_PSS10 = "runPss10",
        RUN_PSS_END = "runPssEnd", RUN_PSS_MIN = "runPssMin", DAY_PSS10 = "dayPss10", DAY_PSS_END = "dayPssEnd",
        DAY_PSS_MIN = "dayPssMin", RUN_AUDIO = "runAudio", DAY_AUDIO = "dayAudio",
        RUN_FOLDED = "runFolded";
    private static final String[] GAMES = {STARTED, END_GAME, END_APP, END_ERROR, MINUTES, DAY_PSS10, DAY_PSS_END, DAY_PSS_MIN, DAY_AUDIO};
    private static final String[] RUN = {PHASE, PHASE_AT, GAME_AT, RUN_PSS10, RUN_PSS_END, RUN_PSS_MIN, RUN_AUDIO, RUN_FOLDED};


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


    private static long number(Store s, String key, long none) {
        return s.get(key) == null ? none : number(s, key);
    }

    private static void add(Store s, String key, long by) {
        s.put(key, String.valueOf(number(s, key) + by));
    }


    public static void clear(Store s) {
        s.remove(QUEUE);
        s.remove(CRASHES);
        for (String k : GAMES) s.remove(k);
        for (String k : RUN) s.remove(k);
    }


    public static void phase(Store s, String phase, long now) {
        s.put(PHASE, phase);
        s.put(PHASE_AT, String.valueOf(now));
    }





    public static void pss(Store s, int minute, int mb) {
        if (mb < 0) return;
        s.put(RUN_PSS_END, String.valueOf(mb));
        s.put(RUN_PSS_MIN, String.valueOf(Math.max(0, minute)));
        if (minute >= D2TelemetryEvents.PSS_AT_MIN && s.get(RUN_PSS10) == null) s.put(RUN_PSS10, String.valueOf(mb));
    }


    public static void audio(Store s, boolean ok) {
        s.put(RUN_AUDIO, ok ? "1" : "0");
    }






    public static void run(Store s, Map<String, Object> props, long time) {
        long phaseAt = number(s, PHASE_AT);
        if (s.get(PHASE) == null || phaseAt <= 0 || time < phaseAt) {
            props.put("phase", "unknown");
            return;
        }
        String phase = D2TelemetryEvents.phase(s.get(PHASE));
        props.put("phase", phase);
        props.put("secs", D2TelemetryEvents.seconds(phaseAt, time));
        boolean inGame = D2TelemetryEvents.PHASE_LOADING.equals(phase) || D2TelemetryEvents.PHASE_GAME.equals(phase)
            || D2TelemetryEvents.PHASE_RUNNING.equals(phase);
        if (!inGame) return;
        long gameAt = number(s, GAME_AT);
        if (gameAt > 0) props.put("playSecs", D2TelemetryEvents.seconds(gameAt, time));
        D2TelemetryEvents.putPss(props, (int) number(s, RUN_PSS10, -1), (int) number(s, RUN_PSS_END, -1),
            (int) number(s, RUN_PSS_MIN, -1));
        if (s.get(RUN_AUDIO) != null) props.put("audioOk", (int) number(s, RUN_AUDIO));
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


    public static void gameStarted(Store s, long now) {
        gameStarted(s, now, true);
    }





    public static void gameStarted(Store s, long now, boolean worldSignal) {
        foldRun(s);
        gameStarted(s);
        s.remove(RUN_PSS10);
        s.remove(RUN_PSS_END);
        s.remove(RUN_PSS_MIN);
        s.remove(RUN_AUDIO);
        s.remove(RUN_FOLDED);
        s.put(GAME_AT, String.valueOf(now));
        phase(s, worldSignal ? D2TelemetryEvents.PHASE_LOADING : D2TelemetryEvents.PHASE_RUNNING, now);
    }


    public static void gameEnded(Store s, int end, int minutes) {
        add(s, end == 0 ? END_GAME : end == 1 ? END_APP : END_ERROR, 1);
        add(s, MINUTES, Math.max(0, minutes));
        foldRun(s);
    }





    static void foldRun(Store s) {
        if (s.get(RUN_FOLDED) != null) return;
        s.put(RUN_FOLDED, "1");

        if (s.get(RUN_AUDIO) != null) s.put(DAY_AUDIO, s.get(RUN_AUDIO));

        long last = number(s, RUN_PSS_END, -1), min = number(s, RUN_PSS_MIN, -1);
        if (last >= 0 && min >= number(s, DAY_PSS_MIN, -1)) {
            s.put(DAY_PSS_END, String.valueOf(last));
            s.put(DAY_PSS_MIN, String.valueOf(min));
            long at10 = number(s, RUN_PSS10, -1);
            if (at10 >= 0) s.put(DAY_PSS10, String.valueOf(at10));
            else s.remove(DAY_PSS10);
        }
    }

    public static D2TelemetryEvents.Sessions sessions(Store s) {
        return new D2TelemetryEvents.Sessions((int) number(s, STARTED), (int) number(s, END_GAME), (int) number(s, END_APP),
            (int) number(s, END_ERROR), (int) number(s, MINUTES), (int) number(s, DAY_PSS10, -1),
            (int) number(s, DAY_PSS_END, -1), (int) number(s, DAY_PSS_MIN, -1), (int) number(s, DAY_AUDIO, -1));
    }





    public static String prepare(Store s, long now, Daily daily) {
        return prepare(s, now, daily, true);
    }





    public static String prepare(Store s, long now, Daily daily, boolean runEnded) {
        if (runEnded) foldRun(s);
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
