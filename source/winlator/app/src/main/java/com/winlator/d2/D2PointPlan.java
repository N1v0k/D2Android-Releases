package com.winlator.d2;

import com.winlator.d2.test.Fertigkeiten;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;

public final class D2PointPlan {
    public static final int STR = 0, ENE = 1, DEX = 2, VIT = 3;
    public static final int MAX_BASE = 20;
    public static final String IDLE_YES = "ja", IDLE_PARTLY = "teilweise", IDLE_NO = "nein";
    private static final int MAX_DEPTH = 8;

    public static final class Entry {
        public final int fromLevel, skill, target;

        Entry(int fromLevel, int skill, int target) {
            this.fromLevel = fromLevel;
            this.skill = skill;
            this.target = Math.min(target, MAX_BASE);
        }
    }

    public static final class Key {
        public final int skill, mainFrom;
        public final String role;

        Key(int skill, String role, int mainFrom) {
            this.skill = skill;
            this.role = role;
            this.mainFrom = mainFrom;
        }

        public String role(int level) {
            return mainFrom > 0 && level >= mainFrom ? "haupt" : role;
        }
    }

    public static final class Build {
        public final String id;
        public final int characterClass;
        public final List<Entry> entries;
        public final List<Key> keys;
        final int[][] strength, dexterity;
        public final List<String> sources;
        public final String idle, idleReason, weapon;

        Build(String id, int characterClass, List<Entry> entries, List<Key> keys, int[][] strength, int[][] dexterity,
              List<String> sources, String idle, String idleReason, String weapon) {
            this.id = id;
            this.characterClass = characterClass;
            this.entries = entries;
            this.keys = keys;
            this.strength = strength;
            this.dexterity = dexterity;
            this.sources = sources;
            this.idle = idle;
            this.idleReason = idleReason;
            this.weapon = weapon;
        }

        public int strengthTarget(int level) {
            return target(strength, level);
        }

        public int dexterityTarget(int level) {
            return target(dexterity, level);
        }

        private static int target(int[][] table, int level) {
            int t = 0;
            for (int[] row : table) if (level >= row[0]) t = row[1];
            return t;
        }
    }

    private D2PointPlan() {}

    public static List<Build> parse(String json) {
        List<Build> out = new ArrayList<>();
        try {
            JSONArray builds = new JSONObject(json).getJSONArray("builds");
            for (int i = 0; i < builds.length(); i++) {
                JSONObject b = builds.getJSONObject(i);
                List<Entry> entries = new ArrayList<>();
                JSONArray skills = b.getJSONArray("skills");
                for (int k = 0; k < skills.length(); k++) {
                    JSONArray e = skills.getJSONArray(k);
                    entries.add(new Entry(e.getInt(0), e.getInt(1), e.getInt(2)));
                }
                List<Key> keys = new ArrayList<>();
                JSONArray ks = b.optJSONArray("keys");
                for (int k = 0; ks != null && k < ks.length(); k++) {
                    JSONObject key = ks.getJSONObject(k);
                    keys.add(new Key(key.getInt("skill"), key.getString("role"), key.optInt("mainFrom", 0)));
                }
                List<String> sources = new ArrayList<>();
                JSONArray src = b.optJSONArray("sources");
                for (int k = 0; src != null && k < src.length(); k++) sources.add(src.getString(k));
                out.add(new Build(b.getString("id"), b.getInt("class"), Collections.unmodifiableList(entries),
                    Collections.unmodifiableList(keys), table(b.optJSONArray("str")), table(b.optJSONArray("dex")),
                    Collections.unmodifiableList(sources), b.optString("idle", IDLE_YES), b.optString("idleGrund", ""),
                    b.optString("waffe", "")));
            }
        }
        catch (JSONException e) {
            return new ArrayList<>();
        }
        return out;
    }

    private static int[][] table(JSONArray a) throws JSONException {
        if (a == null) return new int[0][];
        int[][] t = new int[a.length()][];
        for (int i = 0; i < t.length; i++) t[i] = new int[]{a.getJSONArray(i).getInt(0), a.getJSONArray(i).getInt(1)};
        return t;
    }

    public static com.winlator.d2.test.Kampfwahl.Rollen rollen(Build b, int level) {
        List<Integer> main = new ArrayList<>(), strong = new ArrayList<>(), nova = new ArrayList<>(), buffs = new ArrayList<>();
        int promoted = 0;
        for (Key k : b.keys) {
            String role = k.role(level);
            if (role.equals("haupt") && k.mainFrom > 0 && level >= k.mainFrom) main.add(promoted++, k.skill);
            else if (role.equals("haupt")) main.add(k.skill);
            else if (role.equals("stark")) strong.add(k.skill);
            else if (role.equals("nova")) nova.add(k.skill);
            else if (role.equals("puffer")) buffs.add(k.skill);
        }
        return new com.winlator.d2.test.Kampfwahl.Rollen(b.characterClass, ints(main), ints(strong), ints(nova), ints(buffs));
    }

    private static int[] ints(List<Integer> l) {
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }

    public static Build firstOfClass(List<Build> builds, int characterClass) {
        for (Build b : builds) if (b.characterClass == characterClass) return b;
        return null;
    }

    public static Build byId(List<Build> builds, String id, int characterClass) {
        for (Build b : builds) if (b.id.equals(id) && b.characterClass == characterClass) return b;
        return firstOfClass(builds, characterClass);
    }

    public static int nextSkill(Build build, Fertigkeiten table, int level, IntUnaryOperator baseLevel) {
        if (build == null || table == null) return -1;
        for (Entry e : build.entries) {
            if (e.fromLevel > level || baseLevel.applyAsInt(e.skill) >= e.target) continue;
            int point = pointFor(table, e.skill, baseLevel, 0);
            if (point >= 0 && takes(table, point, build.characterClass, level, baseLevel)) return point;
        }
        return -1;
    }

    static int pointFor(Fertigkeiten table, int skill, IntUnaryOperator baseLevel, int depth) {
        Fertigkeiten.Fertigkeit f = table.get(skill);
        if (f == null || depth > MAX_DEPTH) return -1;
        for (int pre : f.voraussetzungen) {
            if (baseLevel.applyAsInt(pre) <= 0) return pointFor(table, pre, baseLevel, depth + 1);
        }
        return skill;
    }

    static boolean takes(Fertigkeiten table, int skill, int characterClass, int level, IntUnaryOperator baseLevel) {
        Fertigkeiten.Fertigkeit f = table.get(skill);
        if (f == null || f.klasse != characterClass) return false;
        int base = baseLevel.applyAsInt(skill);
        if (base >= MAX_BASE || level < f.mindeststufe + base) return false;
        for (int pre : f.voraussetzungen) if (baseLevel.applyAsInt(pre) <= 0) return false;
        return true;
    }

    public static int nextStat(Build build, int level, int[] base) {
        if (build == null || base == null || base.length < 4) return -1;
        if (base[STR] < build.strengthTarget(level)) return STR;
        if (base[DEX] < build.dexterityTarget(level)) return DEX;
        return VIT;
    }
}
