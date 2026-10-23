package com.winlator.d2;

import org.json.JSONException;
import org.json.JSONObject;


















public final class D2Resume {

    public interface Store {
        String get(String key);

        void put(String key, String value);

        void remove(String key);
    }


    static final int[] WAYPOINT_LEVELS = {1, 40, 75, 103, 109, 3, 4, 5, 6, 27, 29, 32, 35, 48, 42, 57, 43, 44, 52, 76, 77, 78,
        79, 80, 81, 83, 101, 106, 107, 111, 112, 113, 115, 117, 118, 129, 123};
    static final long CONFIRM_MS = 5000;




    static final long FOLLOW_MS = 2000;

    public static final class Entry {
        public final int level, x, y, roomX, roomY, roomW, roomH, difficulty;
        public final long seed, size, time;

        Entry(int level, int x, int y, long seed, int roomX, int roomY, int roomW, int roomH, int difficulty, long size, long time) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.seed = seed;
            this.roomX = roomX;
            this.roomY = roomY;
            this.roomW = roomW;
            this.roomH = roomH;
            this.difficulty = difficulty;
            this.size = size;
            this.time = time;
        }

        String toJson() {
            try {
                return new JSONObject().put("level", level).put("x", x).put("y", y).put("seed", seed).put("rx", roomX)
                    .put("ry", roomY).put("rw", roomW).put("rh", roomH).put("difficulty", difficulty).put("size", size)
                    .put("time", time).toString();
            }
            catch (JSONException e) {
                return null;
            }
        }

        static Entry fromJson(String s) {
            if (s == null) return null;
            try {
                JSONObject o = new JSONObject(s);
                return new Entry(o.getInt("level"), o.getInt("x"), o.getInt("y"), o.getLong("seed"), o.getInt("rx"),
                    o.getInt("ry"), o.getInt("rw"), o.getInt("rh"), o.getInt("difficulty"), o.getLong("size"), o.getLong("time"));
            }
            catch (JSONException e) {
                return null;
            }
        }
    }


    public enum Why { OK, OFF, NOT_SINGLE_PLAYER, IDLE, NO_MOD, DIFFICULTY_UNKNOWN, NO_ENTRY, SAVE_CHANGED, OTHER_MAP, LEVEL }

    private final Store store;
    private String pendingKey, pendingName;
    private D2ResumeState pending;
    private long pendingBefore, pendingUntil, pendingSize, pendingTime;

    private String lastName;
    private D2ResumeState last;

    public D2Resume(Store store) {
        this.store = store;
    }

    static String key(String folder, String name, int difficulty) {
        return "resume|" + folder + "|" + name + "|" + difficulty;
    }

    static boolean isTown(int level) {
        return level == 1 || level == 40 || level == 75 || level == 103 || level == 109;
    }

    static boolean hasWaypoint(int level) {
        for (int l : WAYPOINT_LEVELS) if (l == level) return true;
        return false;
    }


    public void seen(String name, D2ResumeState s) {
        if (name == null || s == null) return;
        lastName = name;
        last = s;
    }


    public void forget() {
        lastName = null;
        last = null;
    }






    public void died(String name) {
        if (pending != null && name != null && name.equals(pendingName)) {
            store.remove(pendingKey);
            pending = null;
        }
        forget();
    }











    public D2ResumeState placeFor(String hero, D2ResumeState fresh, int levelNow, boolean alive) {
        if (fresh != null) return levelNow <= 0 || fresh.level == levelNow ? fresh : null;
        return alive && last != null && hero != null && hero.equals(lastName) && last.level == levelNow ? last : null;
    }





    public void saving(String folder, String name, D2ResumeState s, long fileTimeBefore, long now) {
        pending = null;
        if (folder == null || name == null || name.isEmpty() || s == null || s.difficulty < 0) return;
        if (!isTown(s.level) && !s.placeKnown()) return;
        pending = s;
        pendingKey = key(folder, name, s.difficulty);
        pendingName = name;
        pendingBefore = fileTimeBefore;
        pendingUntil = now + CONFIRM_MS;
        pendingSize = pendingTime = -1;
    }





    public boolean waiting(long now) {
        if (pending != null && now > pendingUntil) pending = null;
        return pending != null;
    }






    public boolean written(long fileSize, long fileTime, long now) {
        if (!waiting(now) || fileTime <= pendingBefore || fileSize <= 0) return false;
        if (fileSize == pendingSize && fileTime == pendingTime) return false;
        D2ResumeState s = pending;
        pendingSize = fileSize;
        pendingTime = fileTime;
        pendingUntil = Math.max(pendingUntil, now + FOLLOW_MS);
        if (isTown(s.level)) store.remove(pendingKey);
        else store.put(pendingKey, new Entry(s.level, s.x, s.y, s.seed, s.roomX, s.roomY, s.roomW, s.roomH, s.difficulty,
            fileSize, fileTime).toJson());
        return true;
    }








    public boolean leftWritten(String folder, String name, long fileSize, long fileTime) {
        D2ResumeState s = last;
        if (folder == null || name == null || !name.equals(lastName) || s == null || s.difficulty < 0 || fileSize <= 0)
            return false;
        if (s.state == D2ResumeState.STATE_RUNNING) return false;
        String k = key(folder, name, s.difficulty);
        if (isTown(s.level)) {
            store.remove(k);
            return true;
        }
        if (!s.placeKnown()) return false;
        store.put(k, new Entry(s.level, s.x, s.y, s.seed, s.roomX, s.roomY, s.roomW, s.roomH, s.difficulty,
            fileSize, fileTime).toJson());
        return true;
    }


    public Why decide(boolean on, boolean singlePlayer, boolean idle, String folder, String name, D2ResumeState s,
                      long fileSize, long fileTime) {
        entry = null;
        if (!on) return Why.OFF;
        if (!singlePlayer) return Why.NOT_SINGLE_PLAYER;
        if (idle) return Why.IDLE;
        if (s == null || (s.caps & D2ResumeState.CAPS_ALL) != D2ResumeState.CAPS_ALL) return Why.NO_MOD;
        if (s.difficulty < 0) return Why.DIFFICULTY_UNKNOWN;
        Entry e = folder == null || name == null ? null : Entry.fromJson(store.get(key(folder, name, s.difficulty)));
        if (e == null) return Why.NO_ENTRY;
        if (e.size != fileSize || e.time != fileTime) return Why.SAVE_CHANGED;
        if (e.seed != s.seed) return Why.OTHER_MAP;
        if (!hasWaypoint(e.level) || isTown(e.level)) return Why.LEVEL;
        entry = e;
        return Why.OK;
    }






    public Entry stored(boolean on, String folder, String name, int difficulty, long fileSize, long fileTime) {
        if (!on || folder == null || name == null) return null;
        Entry e = Entry.fromJson(store.get(key(folder, name, difficulty)));
        if (e == null || e.size != fileSize || e.time != fileTime || !hasWaypoint(e.level) || isTown(e.level)) return null;
        return e;
    }


    public Entry entry;








    public static final class GameWatch {
        public static final long NEW_GAME_MS = 3000;
        private String hero = "";
        private long lastOut, outFocused;


        public String hero() {
            return hero.isEmpty() ? null : hero;
        }





        public boolean step(boolean inGame, String name, boolean focused, long now) {
            if (!inGame) {
                if (lastOut != 0 && focused) outFocused += now - lastOut;
                lastOut = now;
                if (outFocused > NEW_GAME_MS) hero = "";
                return false;
            }
            lastOut = 0;
            outFocused = 0;
            if (name == null || name.equals(hero)) return false;
            hero = name;
            return true;
        }
    }
}
