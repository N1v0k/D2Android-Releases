package com.winlator.d2;

import android.content.Context;
import android.content.SharedPreferences;


public final class D2Settings {
    static final String PREFS = "d2launch";
    static final String KEY_RENDERER = "renderer";
    static final String KEY_GAME_LINES = "gameLines";

    static final String KEY_FRAME_RATE = "frameRate";
    static final String KEY_MOD = "mod";

    static final String KEY_UPDATE_CHECK = "updateCheck";
    static final String KEY_AUTO_GOLD = "autoGold";

    static final String KEY_SKIP_INTRO = "skipIntro";

    static final String KEY_APP_LANGUAGE = "appLanguage", KEY_GAME_LANGUAGE = "gameLanguage";

    public static final int LINES_AUTO = 0, LINES_600 = 600, LINES_540 = 540;

    private D2Settings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }



    public static String renderer(Context context) {
        return GameShadow.RENDERER_D2DX;
    }

    public static void setRenderer(Context context, String renderer) {
        prefs(context).edit().putString(KEY_RENDERER, renderer).apply();
    }


    public static int gameLines(Context context) {

        return prefs(context).getInt(KEY_GAME_LINES, LINES_540);
    }

    public static void setGameLines(Context context, int lines) {
        prefs(context).edit().putInt(KEY_GAME_LINES, lines).apply();
    }


    public static int frameRate(Context context) {
        return D2FrameRate.normalize(prefs(context).getInt(KEY_FRAME_RATE, D2FrameRate.UNLIMITED));
    }

    public static void setFrameRate(Context context, int fps) {
        prefs(context).edit().putInt(KEY_FRAME_RATE, D2FrameRate.normalize(fps)).apply();
    }


    public static boolean modEnabled(Context context) {
        return prefs(context).getBoolean(KEY_MOD, true);
    }

    public static void setModEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_MOD, enabled).apply();
    }

    public static String appLanguage(Context context) {
        return prefs(context).getString(KEY_APP_LANGUAGE, D2Text.AUTO);
    }

    public static void setAppLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_APP_LANGUAGE, language).apply();
    }

    public static String gameLanguage(Context context) {
        return prefs(context).getString(KEY_GAME_LANGUAGE, null);
    }

    public static void setGameLanguage(Context context, String language) {
        prefs(context).edit().putString(KEY_GAME_LANGUAGE, language).apply();
    }


    public static boolean skipIntro(Context context) {
        return prefs(context).getBoolean(KEY_SKIP_INTRO, D2IntroMovies.SKIP_BY_DEFAULT);
    }

    public static boolean autoGold(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_GOLD, true);
    }

    public static void setAutoGold(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AUTO_GOLD, enabled).apply();
    }



    static final String KEY_PLUGY = "plugy", KEY_BIG_STASH = "bigStash", KEY_PLUGY_BACKUP_DONE = "plugyBackupDone";


    public static boolean plugy(Context context) {
        return prefs(context).getBoolean(KEY_PLUGY, true);
    }


    public static boolean bigStash(Context context) {
        return prefs(context).getBoolean(KEY_BIG_STASH, false);
    }


    static boolean plugyExtra(Context context, String iniKey, boolean byDefault) {
        return prefs(context).getBoolean("plugy" + iniKey, byDefault);
    }





    static final String KEY_AIM_ASSIST = "aimAssist", KEY_AIM_RANGE = "aimRange", KEY_AIM_RANGE_MELEE = "aimRangeMelee", KEY_AIM_CONE = "aimCone",
        KEY_AIM_BLIND = "aimBlind",
        KEY_HIGHLIGHT = "highlight", KEY_POTIONS = "potions", KEY_ITEMS_NEED_LABELS = "itemsNeedLabels", KEY_RING_SORT = "ringSort",
        KEY_DIRECT_WALK = "directWalk", KEY_TAP_MOVES = "tapMoves", KEY_HUD = "hud", KEY_MENU_OPEN = "menuOpen", KEY_AUTO_PAUSE = "autoPause",
        KEY_LABELS = "itemLabels", KEY_ALWAYS_RUN = "alwaysRun", KEY_ORB_NUMBERS = "orbNumbers",
        KEY_HIDE_POINTER = "hidePointer", KEY_MENU_PINNED = "menuPinned", KEY_AUTO_TRADE = "autoTrade",
        KEY_SKILL_NAMES = "skillNames", KEY_STAMINA = "staminaMode", KEY_AUTO_LIFE = "autoLife", KEY_AUTO_MANA = "autoMana", KEY_AUTO_REJUV = "autoRejuv", KEY_MERC_POTIONS = "mercPotions",
        KEY_JOYSTICK_VISIBLE = "joystickVisible", KEY_PLAYERS = "players", KEY_AUTOSAVE = "autosave", KEY_RESPEC = "respec",
        KEY_MAP_HACK = "mapHack", KEY_LARGE_STACKS = "largeStacks", KEY_INFO_GRID = "infoGrid", KEY_CLOCK_TOP = "clockTop", KEY_GRID_SLOT = "gridSlot",
        KEY_COMPARE_EQUIPPED = "compareEquipped", KEY_COMPARE_CAPABLE = "compareCapable", KEY_LOOT_CAPABLE = "lootCapable",
        KEY_TRIGGER_LEFT = "triggerLeft", KEY_TRIGGER_RIGHT = "triggerRight",
        KEY_PAD_ENABLED = "padEnabled", KEY_PAD_MAP = "padMap", KEY_PAD_DEADZONE = "padDeadzone", KEY_PAD_CURSOR_SPEED = "padCursorSpeed",
        KEY_VALUE_LINE = "valueLine", KEY_VALUE_CAPABLE = "valueCapable",
        KEY_DIFF_LINES = "diffLines", KEY_DIFF_CAPABLE = "diffCapable",
        KEY_DAMAGE_NUMBERS = "damageNumbers", KEY_DAMAGE_STYLE = "damageStyle", KEY_DAMAGE_DPS = "damageDps",
        KEY_DAMAGE_CAPABLE = "damageCapable",
        KEY_MATERIALS = "materials", KEY_MATERIALS_CAPABLE = "materialsCapable", KEY_MATERIALS_BACKED_UP = "materialsBackedUpDirs";

    static final String[] MENU_PINNED_DEFAULT = {"pause", "inv", "portal"};

    public static final int HUD_ORIGINAL = 0, HUD_COMPACT = 1;

    public static final int POTIONS_KEYS = 0, POTIONS_BELT = 1, POTIONS_ALL = 2;

    public static final int RING_SORT_RANK = 0, RING_SORT_NEAR = 1;






    public static final int AIM_RANGE_MIN = 25, AIM_RANGE_MAX = 120, AIM_RANGE_DEFAULT = 75;

    public static final int AIM_RANGE_MELEE_MIN = 15, AIM_RANGE_MELEE_MAX = 120, AIM_RANGE_MELEE_DEFAULT = 25;
    public static final int AIM_CONE_MIN = 10, AIM_CONE_MAX = 90, AIM_CONE_DEFAULT = 55;

    public static final int AIM_BLIND_MIN = 0, AIM_BLIND_MAX = 180, AIM_BLIND_DEFAULT = 60;

    public static final int STAMINA_NORMAL = 0, STAMINA_POTION = 1, STAMINA_UNLIMITED = 2;

    public static final int AUTO_POTION_MAX = 90;




    public static final int AUTO_LIFE_DEFAULT = 40, AUTO_MANA_DEFAULT = 20,

        AUTO_REJUV_DEFAULT = 20, MERC_POTIONS_DEFAULT = 0;


    public static boolean aimAssist(Context context) {
        return prefs(context).getBoolean(KEY_AIM_ASSIST, true);
    }

    public static int aimRange(Context context) {
        return clamp(prefs(context).getInt(KEY_AIM_RANGE, AIM_RANGE_DEFAULT), AIM_RANGE_MIN, AIM_RANGE_MAX);
    }


    public static int aimRangeMelee(Context context) {
        int melee = clamp(prefs(context).getInt(KEY_AIM_RANGE_MELEE, AIM_RANGE_MELEE_DEFAULT), AIM_RANGE_MELEE_MIN, AIM_RANGE_MELEE_MAX);
        return Math.min(melee, aimRange(context));
    }

    public static int aimCone(Context context) {
        return clamp(prefs(context).getInt(KEY_AIM_CONE, AIM_CONE_DEFAULT), AIM_CONE_MIN, AIM_CONE_MAX);
    }


    public static int aimBlind(Context context) {
        return clamp(prefs(context).getInt(KEY_AIM_BLIND, AIM_BLIND_DEFAULT), AIM_BLIND_MIN, AIM_BLIND_MAX);
    }


    public static boolean highlight(Context context) {
        return prefs(context).getBoolean(KEY_HIGHLIGHT, true);
    }

    public static int potions(Context context) {
        int value = prefs(context).getInt(KEY_POTIONS, POTIONS_ALL);
        return value >= POTIONS_KEYS && value <= POTIONS_ALL ? value : POTIONS_ALL;
    }


    public static int triggerLeft(Context context) {
        return D2TriggerAction.of(prefs(context).getInt(KEY_TRIGGER_LEFT, D2TriggerAction.SKILL));
    }

    public static int triggerRight(Context context) {
        return D2TriggerAction.of(prefs(context).getInt(KEY_TRIGGER_RIGHT, D2TriggerAction.SKILL));
    }


    public static boolean padEnabled(Context context) {
        return prefs(context).getBoolean(KEY_PAD_ENABLED, true);
    }


    public static int padDeadzone(Context context) {
        return clamp(prefs(context).getInt(KEY_PAD_DEADZONE, D2Gamepad.DEADZONE_DEFAULT), D2Gamepad.DEADZONE_MIN, D2Gamepad.DEADZONE_MAX);
    }


    public static int padCursorSpeed(Context context) {
        return clamp(prefs(context).getInt(KEY_PAD_CURSOR_SPEED, D2Gamepad.CURSOR_SPEED_DEFAULT), D2Gamepad.CURSOR_SPEED_MIN, D2Gamepad.CURSOR_SPEED_MAX);
    }


    public static String padMap(Context context) {
        return prefs(context).getString(KEY_PAD_MAP, "");
    }

    public static void setPadMap(Context context, String map) {
        prefs(context).edit().putString(KEY_PAD_MAP, map).apply();
    }


    public static void setTrigger(Context context, boolean left, int action) {
        prefs(context).edit().putInt(left ? KEY_TRIGGER_LEFT : KEY_TRIGGER_RIGHT, D2TriggerAction.of(action)).apply();
    }


    public static int ringSort(Context context) {
        return prefs(context).getInt(KEY_RING_SORT, RING_SORT_RANK) == RING_SORT_NEAR ? RING_SORT_NEAR : RING_SORT_RANK;
    }


    public static boolean itemsNeedLabels(Context context) {
        return prefs(context).getBoolean(KEY_ITEMS_NEED_LABELS, true);
    }


    public static boolean directWalk(Context context) {
        return prefs(context).getBoolean(KEY_DIRECT_WALK, true);
    }


    public static boolean tapMoves(Context context) {
        return prefs(context).getBoolean(KEY_TAP_MOVES, false);
    }

    public static int hud(Context context) {
        int value = prefs(context).getInt(KEY_HUD, HUD_COMPACT);
        return value == HUD_ORIGINAL ? HUD_ORIGINAL : HUD_COMPACT;
    }

    static final String KEY_INTRO_SEEN = "introSeen";






    public static boolean introSeen(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.contains(KEY_INTRO_SEEN)) {
            boolean seen = D2Intro.seenByDefault(p.contains(KEY_GAME_LANGUAGE));
            p.edit().putBoolean(KEY_INTRO_SEEN, seen).apply();
            return seen;
        }
        return p.getBoolean(KEY_INTRO_SEEN, false);
    }


    public static void setIntroSeen(Context context, boolean seen) {
        prefs(context).edit().putBoolean(KEY_INTRO_SEEN, seen).apply();
    }

    static final String KEY_INTRO_INTERRUPTIONS = "introInterruptions";





    public static int introInterruptions(Context context) {
        return prefs(context).getInt(KEY_INTRO_INTERRUPTIONS, 0);
    }

    public static void setIntroInterruptions(Context context, int count) {
        prefs(context).edit().putInt(KEY_INTRO_INTERRUPTIONS, count).apply();
    }





    public static void requestIntro(Context context) {
        prefs(context).edit().putBoolean(KEY_INTRO_SEEN, false).putInt(KEY_INTRO_INTERRUPTIONS, 0).apply();
    }


    public static boolean autoPause(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_PAUSE, true);
    }


    public static boolean itemLabels(Context context) {
        return prefs(context).getBoolean(KEY_LABELS, true);
    }


    public static boolean mapHack(Context context) {
        return prefs(context).getBoolean(KEY_MAP_HACK, true);
    }


    public static boolean compareEquipped(Context context) {
        return prefs(context).getBoolean(KEY_COMPARE_EQUIPPED, true);
    }



    public static boolean compareCapable(Context context) {
        return prefs(context).getBoolean(KEY_COMPARE_CAPABLE, false);
    }

    static void setCompareCapable(Context context, boolean capable) {
        putBoolean(context, KEY_COMPARE_CAPABLE, capable);
    }


    public static boolean lootCapable(Context context) {
        return prefs(context).getBoolean(KEY_LOOT_CAPABLE, false);
    }

    static void setLootCapable(Context context, boolean capable) {
        putBoolean(context, KEY_LOOT_CAPABLE, capable);
    }


    public static boolean valueLine(Context context) {
        return prefs(context).getBoolean(KEY_VALUE_LINE, true);
    }



    public static boolean valueCapable(Context context) {
        return prefs(context).getBoolean(KEY_VALUE_CAPABLE, false);
    }

    static void setValueCapable(Context context, boolean capable) {
        putBoolean(context, KEY_VALUE_CAPABLE, capable);
    }


    public static boolean materials(Context context) {
        return prefs(context).getBoolean(KEY_MATERIALS, true);
    }


    public static boolean materialsCapable(Context context) {
        return prefs(context).getBoolean(KEY_MATERIALS_CAPABLE, false);
    }

    static void setMaterialsCapable(Context context) {
        putBoolean(context, KEY_MATERIALS_CAPABLE, true);
    }


    public static boolean materialsBackedUp(Context context, java.io.File gameDir) {
        return gameDir != null && prefs(context).getStringSet(KEY_MATERIALS_BACKED_UP, java.util.Collections.emptySet())
            .contains(gameDir.getAbsolutePath());
    }

    static void setMaterialsBackedUp(Context context, java.io.File gameDir) {
        java.util.Set<String> done = new java.util.HashSet<>(prefs(context).getStringSet(KEY_MATERIALS_BACKED_UP,
            java.util.Collections.emptySet()));
        done.add(gameDir.getAbsolutePath());
        prefs(context).edit().putStringSet(KEY_MATERIALS_BACKED_UP, done).apply();
    }




    public static int damageStyle(Context context) {
        SharedPreferences p = prefs(context);
        boolean hasStyle = p.contains(KEY_DAMAGE_STYLE), hasOld = p.contains(KEY_DAMAGE_NUMBERS);
        return D2DamageStyle.of(hasStyle, hasStyle ? p.getInt(KEY_DAMAGE_STYLE, D2DamageStyle.DEFAULT) : 0,
            hasOld, hasOld && p.getBoolean(KEY_DAMAGE_NUMBERS, true));
    }



    public static boolean diffLines(Context context) {
        return prefs(context).getBoolean(KEY_DIFF_LINES, true);
    }


    public static boolean diffCapable(Context context) {
        return prefs(context).getBoolean(KEY_DIFF_CAPABLE, false);
    }

    static void setDiffCapable(Context context, boolean capable) {
        putBoolean(context, KEY_DIFF_CAPABLE, capable);
    }


    public static boolean damageDps(Context context) {
        return prefs(context).getBoolean(KEY_DAMAGE_DPS, true);
    }



    public static boolean damageCapable(Context context) {
        return prefs(context).getBoolean(KEY_DAMAGE_CAPABLE, false);
    }

    static void setDamageCapable(Context context, boolean capable) {
        putBoolean(context, KEY_DAMAGE_CAPABLE, capable);
    }


    public static boolean largeStacks(Context context) {
        return prefs(context).getBoolean(KEY_LARGE_STACKS, true);
    }

    static final String KEY_MAP_FIGHT_HIDE = "mapFightHide", KEY_MAP_FIGHT_DELAY = "mapFightDelay";
    public static final int MAP_FIGHT_DELAY_MIN = 1, MAP_FIGHT_DELAY_MAX = 10, MAP_FIGHT_DELAY_DEFAULT = 3;


    public static boolean mapFightHide(Context context) {
        return prefs(context).getBoolean(KEY_MAP_FIGHT_HIDE, true);
    }


    public static int mapFightDelay(Context context) {
        return clamp(prefs(context).getInt(KEY_MAP_FIGHT_DELAY, MAP_FIGHT_DELAY_DEFAULT), MAP_FIGHT_DELAY_MIN, MAP_FIGHT_DELAY_MAX);
    }


    public static boolean alwaysRun(Context context) {
        return prefs(context).getBoolean(KEY_ALWAYS_RUN, true);
    }

    public static int staminaMode(Context context) {
        int value = prefs(context).getInt(KEY_STAMINA, STAMINA_NORMAL);
        return value >= STAMINA_NORMAL && value <= STAMINA_UNLIMITED ? value : STAMINA_NORMAL;
    }


    public static int autoLife(Context context) {
        return clamp(prefs(context).getInt(KEY_AUTO_LIFE, AUTO_LIFE_DEFAULT), 0, AUTO_POTION_MAX);
    }


    public static int autoRejuv(Context context) {
        return clamp(prefs(context).getInt(KEY_AUTO_REJUV, AUTO_REJUV_DEFAULT), 0, AUTO_POTION_MAX);
    }


    public static int mercPotions(Context context) {
        return clamp(prefs(context).getInt(KEY_MERC_POTIONS, MERC_POTIONS_DEFAULT), 0, AUTO_POTION_MAX);
    }


    public static int autoMana(Context context) {
        return clamp(prefs(context).getInt(KEY_AUTO_MANA, AUTO_MANA_DEFAULT), 0, AUTO_POTION_MAX);
    }


    public static boolean joystickVisible(Context context) {
        return prefs(context).getBoolean(KEY_JOYSTICK_VISIBLE, true);
    }


    public static int players(Context context) {
        return clamp(prefs(context).getInt(KEY_PLAYERS, 1), 1, 8);
    }


    public static boolean autosave(Context context) {
        return prefs(context).getBoolean(KEY_AUTOSAVE, true);
    }


    public static boolean respec(Context context) {
        return prefs(context).getBoolean(KEY_RESPEC, true);
    }

    static final String KEY_RESPEC_STARTED = "respecStarted";





    public static boolean respecStarted(Context context) {
        return prefs(context).getBoolean(KEY_RESPEC_STARTED, false);
    }

    static void setRespecStarted(Context context, boolean on) {
        prefs(context).edit().putBoolean(KEY_RESPEC_STARTED, on).apply();
    }

    static final String KEY_STATS_PAGES = "statsPages";
    static final String KEY_RESUME = "resume";


    public static boolean resume(Context context) {
        return prefs(context).getBoolean(KEY_RESUME, false);
    }

    static final String KEY_SOUND = "sound";





    public static boolean sound(Context context) {
        return prefs(context).getBoolean(KEY_SOUND, true);
    }

    public static void setSound(Context context, boolean on) {
        prefs(context).edit().putBoolean(KEY_SOUND, on).apply();
    }


    public static boolean statsPages(Context context) {
        return prefs(context).getBoolean(KEY_STATS_PAGES, true);
    }


    public static boolean orbNumbers(Context context) {
        return prefs(context).getBoolean(KEY_ORB_NUMBERS, true);
    }


    public static boolean skillNames(Context context) {
        return prefs(context).getBoolean(KEY_SKILL_NAMES, true);
    }





    public static boolean infoGrid(Context context) {
        return prefs(context).getBoolean(KEY_INFO_GRID, true);
    }


    public static boolean clockTop(Context context) {
        return prefs(context).getBoolean(KEY_CLOCK_TOP, true);
    }

    static final String KEY_XP_BAR = "xpBar", KEY_BOX_OPEN = "boxOpen", KEY_BOX_SLOT = "boxSlot", KEY_XP_LEVEL = "xpLevel";


    public static boolean xpLevel(Context context) {
        return prefs(context).getBoolean(KEY_XP_LEVEL, true);
    }


    public static boolean highlighted(Context context, int kind) {
        if (kind <= 0 || kind >= D2InfoGrid.HIGHLIGHT_KEYS.length) return false;
        return prefs(context).getBoolean(D2InfoGrid.HIGHLIGHT_KEYS[kind], D2InfoGrid.highlightDefault(kind));
    }


    public static boolean xpBar(Context context) {
        return prefs(context).getBoolean(KEY_XP_BAR, true);
    }


    public static boolean boxOpen(Context context) {
        return prefs(context).getBoolean(KEY_BOX_OPEN, false);
    }

    public static void setBoxOpen(Context context, boolean open) {
        prefs(context).edit().putBoolean(KEY_BOX_OPEN, open).apply();
    }


    public static int boxSlot(Context context, int place) {
        return prefs(context).getInt(KEY_BOX_SLOT + (place + 1), -1);
    }


    public static int gridSlot(Context context, int place) {
        return prefs(context).getInt(KEY_GRID_SLOT + (place + 1), -1);
    }


    public static boolean autoTrade(Context context) {
        return prefs(context).getBoolean(KEY_AUTO_TRADE, true);
    }


    static final String KEY_POTION_SHOP = "potionShop", KEY_SHOP_RESERVE_HEAL = "shopReserveHeal",
        KEY_SHOP_RESERVE_MANA = "shopReserveMana", KEY_MIN_GOLD = "minGold";

    public static boolean potionShop(Context context) {
        return prefs(context).getBoolean(KEY_POTION_SHOP, true);
    }

    public static int shopReserve(Context context, boolean mana) {
        return clamp(prefs(context).getInt(mana ? KEY_SHOP_RESERVE_MANA : KEY_SHOP_RESERVE_HEAL, D2PotionShop.RESERVE_DEFAULT),
            0, D2PotionShop.RESERVE_MAX);
    }


    public static int minGoldStep(Context context) {
        return clamp(prefs(context).getInt(KEY_MIN_GOLD, 0), 0, D2PotionShop.MIN_GOLD_STEPS.length - 1);
    }

    public static int minGold(Context context) {
        return D2PotionShop.minGold(minGoldStep(context));
    }


    public static boolean hidePointer(Context context) {
        return prefs(context).getBoolean(KEY_HIDE_POINTER, true);
    }


    public static java.util.Set<String> pinnedMenu(Context context) {
        java.util.Set<String> saved = prefs(context).getStringSet(KEY_MENU_PINNED, null);
        return saved != null ? new java.util.HashSet<>(saved) : new java.util.HashSet<>(java.util.Arrays.asList(MENU_PINNED_DEFAULT));
    }

    static void putPinned(Context context, String key, boolean pinned) {
        java.util.Set<String> set = pinnedMenu(context);
        if (pinned) set.add(key);
        else set.remove(key);
        prefs(context).edit().putStringSet(KEY_MENU_PINNED, set).apply();
    }


    public static boolean menuOpen(Context context) {
        return prefs(context).getBoolean(KEY_MENU_OPEN, true);
    }


    public static boolean updateCheck(Context context) {
        return prefs(context).getBoolean(KEY_UPDATE_CHECK, true);
    }

    static void putBoolean(Context context, String key, boolean value) {
        prefs(context).edit().putBoolean(key, value).apply();
    }

    static void putInt(Context context, String key, int value) {
        prefs(context).edit().putInt(key, value).apply();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }


    public static String gameSize(String screenSize, int lines) {
        if (lines == LINES_AUTO) return null;
        String[] parts = screenSize.split("x");
        float aspect = Integer.parseInt(parts[0]) / (float) Integer.parseInt(parts[1]);
        int width = (int) (lines * aspect);
        width -= width % 2;
        return width + "x" + lines;
    }
}
