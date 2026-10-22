package com.winlator.d2;

import android.content.Context;
import android.content.SharedPreferences;

final class D2LabelFilter {
    static final String KEY_DROP_LABELS = "dropLabels", KEY_NEAR_ONLY = "labelsNearOnly";
    static final int SECONDS = 5, RADIUS = 12;

    static final String KEY_LOOT = "lootFilter", KEY_LOOT_HIDE = "lootHide";
    static final int LOOT_OFF = 0, LOOT_LOOSE = 1, LOOT_STRICT = 2, LOOT_OWN = 3;
    static final int AMMO = 0x0001, SCROLLS = 0x0002, KEYS = 0x0004, POTIONS_SMALL = 0x0008, POTIONS = 0x0010,
        POTIONS_OTHER = 0x0020, REJUV = 0x0040, GEMS_CHIPPED = 0x0080, GEMS_FLAWED = 0x0100, INFERIOR = 0x0200,
        WHITE = 0x0400, MAGIC = 0x0800, GOLD = 0x1000, SUPERIOR = 0x2000, ALL = 0x3FFF;
    static final String[] OWN_KEYS = {"ammo", "scrolls", "keys", "potionsSmall", "potions", "potionsOther", "rejuv",
        "gemsChipped", "gemsFlawed", "white", "inferior", "superior", "magic", "gold"};
    static final int[] OWN_BITS = {AMMO, SCROLLS, KEYS, POTIONS_SMALL, POTIONS, POTIONS_OTHER, REJUV, GEMS_CHIPPED,
        GEMS_FLAWED, WHITE, INFERIOR, SUPERIOR, MAGIC, GOLD};
    static final int LOOSE = AMMO | SCROLLS | INFERIOR | GOLD, STRICT = LOOSE | WHITE | POTIONS_SMALL | GEMS_CHIPPED;
    static final int GOLD_LOOSE = 50, GOLD_STRICT = 200, GOLD_OWN = 100;

    boolean dropLabels, nearOnly;
    int lootPreset, lootOwn;

    void load(Context context, String scope) {
        dropLabels = get(context, KEY_DROP_LABELS);
        nearOnly = get(context, KEY_NEAR_ONLY);
        SharedPreferences prefs = context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE);
        adopt(prefs, scope);
        lootPreset = preset(prefs, scope);
        lootOwn = own(prefs, scope);
    }

    static String scope(int characterClass, String name) {
        return name == null || name.isEmpty() || characterClass < 0 ? null : "." + characterClass + "." + name;
    }

    static void adopt(SharedPreferences prefs, String scope) {
        if (scope == null || prefs.contains(KEY_LOOT + scope)) return;
        prefs.edit().putInt(KEY_LOOT + scope, prefs.getInt(KEY_LOOT, LOOT_OFF))
            .putInt(KEY_LOOT_HIDE + scope, prefs.getInt(KEY_LOOT_HIDE, 0)).apply();
    }

    static int preset(SharedPreferences prefs, String scope) {
        return prefs.getInt(scope != null && prefs.contains(KEY_LOOT + scope) ? KEY_LOOT + scope : KEY_LOOT, LOOT_OFF);
    }

    static int own(SharedPreferences prefs, String scope) {
        return prefs.getInt(scope != null && prefs.contains(KEY_LOOT_HIDE + scope) ? KEY_LOOT_HIDE + scope : KEY_LOOT_HIDE, 0);
    }

    static void putPreset(SharedPreferences prefs, String scope, int preset) {
        SharedPreferences.Editor e = prefs.edit().putInt(KEY_LOOT, preset);
        if (scope != null) e.putInt(KEY_LOOT + scope, preset);
        e.apply();
    }

    static void putOwn(SharedPreferences prefs, String scope, int own) {
        SharedPreferences.Editor e = prefs.edit().putInt(KEY_LOOT_HIDE, own);
        if (scope != null) e.putInt(KEY_LOOT_HIDE + scope, own);
        e.apply();
    }

    static int lootMask(int preset, int own) {
        switch (preset) {
            case LOOT_LOOSE: return LOOSE;
            case LOOT_STRICT: return STRICT;
            case LOOT_OWN: return own & ALL;
            default: return 0;
        }
    }

    static int lootGold(int preset) {
        return preset == LOOT_LOOSE ? GOLD_LOOSE : preset == LOOT_STRICT ? GOLD_STRICT : preset == LOOT_OWN ? GOLD_OWN : 0;
    }

    static boolean clearLoot(boolean filterWanted, int lastSentMask) {
        return !filterWanted && lastSentMask != 0;
    }

    int lootMask() {
        return lootMask(lootPreset, lootOwn);
    }

    int lootGold() {
        return lootGold(lootPreset);
    }

    static boolean get(Context context, String key) {
        return context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE).getBoolean(key, true);
    }

    boolean any() {
        return dropLabels || nearOnly || lootMask() != 0;
    }

    int bits(boolean namesButton) {
        return (dropLabels ? 1 : 0) | (nearOnly ? 2 : 0) | (namesButton ? 4 : 0);
    }
}
