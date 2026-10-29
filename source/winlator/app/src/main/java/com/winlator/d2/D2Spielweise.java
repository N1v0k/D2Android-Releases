package com.winlator.d2;

import java.util.Map;












public final class D2Spielweise {
    private D2Spielweise() {}

    public static final String FILE = "d2android-spielweise";
    public static final String CLASSIC = "classic", ENHANCED = "enhanced";

    public static final String KEY_PRESET = "swPreset";


    public static final class Regulator {
        public final String key;
        public final int number, stages, classic, enhanced;

        public final String plugyKey;

        public final boolean mod;




        public final boolean inPresets;

        final int[] order;




        Regulator needs;

        Regulator(String key, int number, int stages, int classic, int enhanced, String plugyKey, boolean mod) {
            this(key, number, stages, classic, enhanced, plugyKey, mod, true);
        }

        Regulator(String key, int number, int stages, int classic, int enhanced, String plugyKey, boolean mod, boolean inPresets) {
            this(key, number, stages, classic, enhanced, plugyKey, mod, inPresets, null);
        }

        Regulator(String key, int number, int stages, int classic, int enhanced, String plugyKey, boolean mod, boolean inPresets, int[] order) {
            this.order = order;
            this.key = key;
            this.number = number;
            this.stages = stages;
            this.classic = classic;
            this.enhanced = inPresets ? enhanced : classic;
            this.plugyKey = plugyKey;
            this.mod = mod;
            this.inPresets = inPresets;
        }

        public int preset(String preset) {
            return CLASSIC.equals(preset) ? classic : enhanced;
        }


        public int next(int stage) {
            if (order == null) return (stage + 1) % stages;
            for (int i = 0; i < order.length; i++) if (order[i] == stage) return order[(i + 1) % order.length];
            return order[0];
        }
    }


    public static final Regulator QUALITY = new Regulator("swQuality", 1, 3, 0, 2, null, true);

    public static final Regulator NODROP = new Regulator("swNoDrop", 2, 4, 0, 1, null, true);

    public static final Regulator RUNES = new Regulator("swRunes", 3, 4, 0, 2, null, true);

    public static final Regulator BOSS = new Regulator("swBoss", 9, 3, 0, 2, null, true);




    public static final Regulator DENSITY = new Regulator("swDensity", 6, 5, 0, 4, null, true, true, new int[]{0, 4, 1, 2, 3});
    public static final Regulator UNIQUES = new Regulator("swUniques", 10, 2, 0, 1, null, true);
    public static final Regulator GAMBLE = new Regulator("swGamble", 4, 3, 0, 1, null, true);
    public static final Regulator EXPERIENCE = new Regulator("swExp", 5, 3, 0, 2, null, true);
    public static final Regulator LADDER = new Regulator("swLadder", 7, 2, 0, 1, "ActiveLadderRunewords", true);
    public static final Regulator COW = new Regulator("swCow", 8, 2, 0, 1, "ActiveCowPortalWhenCowKingWasKilled", false);
    public static final Regulator UBER = new Regulator("swUber", 11, 2, 0, 1, "ActiveUberQuest", false);
    public static final Regulator WORLD = new Regulator("swWorld", 12, 2, 0, 0, "ActiveWorldEvent", false);





    public static final Regulator INVENTORY = new Regulator("bigInventory", 0, 2, 0, 1, null, false);






    public static final Regulator SHARED = new Regulator("swShared", 0, 2, 0, 1, null, false);
    public static final Regulator SORT = new Regulator("swSort", 0, 2, 0, 1, null, false);
    public static final Regulator RESPEC = new Regulator("respec", 0, 2, 0, 1, null, false);

    public static final Regulator[] ALL = {QUALITY, NODROP, RUNES, BOSS, UNIQUES, GAMBLE, EXPERIENCE, DENSITY, INVENTORY, SHARED, SORT, RESPEC, LADDER, COW, UBER,
        WORLD};


    public static boolean storedAsBoolean(Regulator r) {
        return r.plugyKey != null || r.number == 0;
    }


    public static boolean usable(Regulator r, Map<String, ?> stored) {
        if (r.needs == null) return true;
        String preset = preset((String) stored.get(KEY_PRESET));
        return value(r.needs, stored.get(storeKey(r.needs)), preset) == 1 && usable(r.needs, stored);
    }


    public static Regulator byKey(String key) {
        for (Regulator r : ALL) if (r.key.equals(key)) return r;
        return null;
    }


    public static Regulator byPlugyKey(String iniKey) {
        for (Regulator r : ALL) if (iniKey.equals(r.plugyKey)) return r;
        return null;
    }


    public static String storeKey(Regulator r) {
        return r.plugyKey != null ? "plugy" + r.plugyKey : r.key;
    }


    public static String preset(String stored) {
        return CLASSIC.equals(stored) ? CLASSIC : ENHANCED;
    }





    public static String nextPreset(String stored) {
        return CLASSIC.equals(preset(stored)) ? ENHANCED : CLASSIC;
    }





    public static int value(Regulator r, Object own, String preset) {
        if (own instanceof Boolean) return (Boolean) own ? 1 : 0;
        if (own instanceof Integer && (Integer) own >= 0 && (Integer) own < r.stages) return (Integer) own;
        return r.preset(preset(preset));
    }


    public static boolean own(Map<String, ?> stored) {
        String preset = preset((String) stored.get(KEY_PRESET));
        for (Regulator r : ALL) {
            if (r.inPresets && value(r, stored.get(storeKey(r)), preset) != r.preset(preset)) return true;
        }
        return false;
    }


    public static String file(Map<String, ?> stored) {
        String preset = preset((String) stored.get(KEY_PRESET));
        StringBuilder out = new StringBuilder("# D2Android Spielweise\r\nversion=1\r\n");
        for (Regulator r : ALL) {
            if (!r.mod) continue;
            out.append(r.number).append('=').append(value(r, stored.get(storeKey(r)), preset)).append("\r\n");
        }
        return out.toString();
    }
}
