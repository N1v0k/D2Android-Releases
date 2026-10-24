package com.winlator.d2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;










public final class D2HelperEntries {

    public static final String TAB_FREQUENT = "Häufig", TAB_AUTO = "Automatik", TAB_DISPLAY = "Anzeige",
        TAB_CONTROL = "Steuerung", TAB_GAME = "Spiel & Hilfe";
    public static final List<String> TABS = Collections.unmodifiableList(java.util.Arrays.asList(
        TAB_FREQUENT, TAB_AUTO, TAB_DISPLAY, TAB_CONTROL, TAB_GAME));


    public enum Kind { CHECK, SLIDER, CHOICE, PINS, BUTTON, TRIGGERS, PAD, HOST }


    public static final String ACTION_DIAGNOSE = "diagnose", ACTION_REPORT = "report", ACTION_INTRO = "intro";


    public interface Text { String get(String name); }


    public static String textName(String tabOrGroup, boolean tab) {
        String s = tabOrGroup.toLowerCase().replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss").replace("&", "and");
        return (tab ? "helper_tab_" : "helper_group_") + s.replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }


    public static final Map<String, List<String>> GROUPS = new LinkedHashMap<>();
    private static final Map<String, String> GROUP_OF = new HashMap<>();

    private static final Map<String, String> DEPENDS = new HashMap<>();

    static {
        group(TAB_AUTO, "Tränke", "potions", "autoLife", "autoRejuv", "autoMana", "mercPotions");
        group(TAB_AUTO, "Aufheben", "autoGold", "pickPotions", "pickScrolls", "pickRunes", "pickGems", "pickJewelry", "beltRefill");
        group(TAB_AUTO, "Sonst", "staminaMode", "autoTrade", "autoPause");
        group(TAB_AUTO, "Händler", "potionShop", "shopReserveHeal", "shopReserveMana", "minGold");
        group(TAB_DISPLAY, "Untere Leiste", "hud", "orbNumbers", "skillNames");
        group(TAB_DISPLAY, "Infoleiste", "infoGrid", "gridSlot1", "gridSlot2", "gridSlot3", "gridSlot4", "clockTop", "boxSlot1",
            "boxSlot2", "boxSlot3", "xpBar", "xpLevel");
        group(TAB_DISPLAY, "Hervorgehoben (weiß)", "hiClock", "hiFps", "hiBattery", "hiPlayers", "hiLevel", "hiDps", "hiGold");
        group(TAB_DISPLAY, "Namen am Boden", "itemLabels", "dropLabels", "labelsNearOnly", "lootFilter", "lootHide");
        group(TAB_DISPLAY, "Gegenstände", "compareEquipped", "valueLine", "diffLines");
        group(TAB_DISPLAY, "Schaden", "damageStyle", "damageDps");
        group(TAB_DISPLAY, "Karte", "mapHack", "mapFightHide", "mapFightDelay");
        group(TAB_DISPLAY, "Sonst", "hidePointer", "joystickVisible");
        group(TAB_CONTROL, "Zielhilfe", "aimAssist", "aimRangeMelee", "aimRange", "aimCone", "aimBlind", "highlight");
        group(TAB_CONTROL, "Gehen und Laufen", "alwaysRun", "directWalk", "tapMoves");
        group(TAB_CONTROL, "Gegenstände", "itemsNeedLabels", "ringSort");
        group(TAB_CONTROL, "Menü oben rechts", "menuPinned");
        group(TAB_CONTROL, "Schultertasten", "triggers");
        group(TAB_CONTROL, "Gamepad", "padEnabled", "padMap", "padDeadzone", "padCursorSpeed");
        group(TAB_GAME, "Spiel", "players", "largeStacks", "materials", "resume");
        group(TAB_GAME, "Hilfe", "intro");
        group(TAB_GAME, "Fehlersuche", "diagnose", "report");
        DEPENDS.put("aimRangeMelee", "aimAssist");
        DEPENDS.put("padMap", "padEnabled");
        DEPENDS.put("padDeadzone", "padEnabled");
        DEPENDS.put("padCursorSpeed", "padEnabled");
        DEPENDS.put("aimRange", "aimAssist");
        DEPENDS.put("aimCone", "aimAssist");
        DEPENDS.put("aimBlind", "aimAssist");
        DEPENDS.put("mapFightDelay", "mapFightHide");
        DEPENDS.put("shopReserveHeal", "potionShop");
        DEPENDS.put("shopReserveMana", "potionShop");
    }

    private static void group(String tab, String name, String... keys) {
        GROUPS.computeIfAbsent(tab, t -> new ArrayList<>()).add(name);
        for (String key : keys) GROUP_OF.put(key, name);
    }


    public static String dependsOn(String key) {
        return key == null ? null : DEPENDS.get(key);
    }


    public static final int TWO_COLUMNS_DP = 900;


    public static int rows(Entry e) {
        switch (e.kind) {
            case SLIDER: return 2;
            case CHOICE: return 1 + e.options.length;
            case PINS: return 1 + (e.options.length / 2 + 1) / 2;
            case PAD: return 3 + e.options.length / 2;
            default: return 1;
        }
    }






    public static int splitAt(int[] heights) {
        int n = heights.length, total = 0;
        for (int h : heights) total += h;
        if (n < 2) return n;
        int best = 1, bestTall = Integer.MAX_VALUE, left = 0;
        for (int k = 1; k < n; k++) {
            left += heights[k - 1];
            int tall = Math.max(left, total - left);
            if (tall < bestTall || (tall == bestTall && left >= total - left)) {
                best = k;
                bestTall = tall;
            }
        }
        return best;
    }





    public static boolean shown(Entry e, boolean internal) {
        return !(internal && ACTION_REPORT.equals(e.action));
    }

    public static final class Entry {
        public final Kind kind;

        public final String key;

        public final String action;
        public final String label;

        public final String section, tab;

        public final boolean frequent;

        public final String lead, info;




        public final String[] options;

        public final String group;

        Entry(Kind kind, String key, String label, String section, String tab, boolean frequent, String lead, String info,
              String... options) {
            this.kind = kind;
            this.key = key;
            this.label = label;
            this.section = section;
            this.tab = tab;
            this.frequent = frequent;
            this.lead = lead;
            this.info = info;
            this.options = options;
            this.action = kind == Kind.BUTTON ? label.replaceAll("^helper_(.*)_label$", "$1") : null;
            this.group = GROUP_OF.get(key != null ? key : action);
        }


        private Entry(Entry e, Text t) {
            kind = e.kind;
            key = e.key;
            action = e.action;
            label = t.get(e.label);
            section = e.section;
            tab = e.tab;
            frequent = e.frequent;
            lead = e.lead == null ? null : t.get(e.lead);
            info = e.info == null ? null : t.get(e.info);
            options = new String[e.options.length];
            for (int i = 0; i < options.length; i++) {
                options[i] = (kind == Kind.PINS || kind == Kind.TRIGGERS) && i % 2 == 0 || kind == Kind.PAD ? e.options[i] : t.get(e.options[i]);
            }
            group = e.group;
        }
    }

    private D2HelperEntries() {}


    public static List<Entry> all() {
        List<Entry> l = new ArrayList<>();
        String s = "Untere Leiste";
        l.add(new Entry(Kind.CHOICE, "hud", "helper_hud_label", s, TAB_DISPLAY, false, null, "helper_hud_info", "helper_hud_opt0", "helper_hud_opt1"));
        l.add(new Entry(Kind.CHECK, "orbNumbers", "helper_orbNumbers_label", s, TAB_DISPLAY, false, null, null));
        l.add(new Entry(Kind.CHECK, "skillNames", "helper_skillNames_label", s, TAB_DISPLAY, false, null, null));

        s = "Infoleiste";
        l.add(new Entry(Kind.CHECK, "infoGrid", "helper_infoGrid_label", s, TAB_DISPLAY, false, null, "helper_infoGrid_info"));
        for (int i = 1; i <= D2InfoGrid.SLOTS; i++) {
            l.add(new Entry(Kind.CHOICE, "gridSlot" + i, "helper_gridSlot" + i + "_label", s, TAB_DISPLAY, false, null, null,
                "helper_gridSlot_opt0", "helper_gridSlot_opt1", "helper_gridSlot_opt2", "helper_gridSlot_opt3",
                "helper_gridSlot_opt4", "helper_gridSlot_opt5", "helper_gridSlot_opt6", "helper_gridSlot_opt7"));
        }

        l.add(new Entry(Kind.CHECK, "clockTop", "helper_clockTop_label", s, TAB_DISPLAY, false, null, "helper_clockTop_info"));
        for (int i = 1; i <= D2InfoGrid.BOX_SLOTS; i++) {
            l.add(new Entry(Kind.CHOICE, "boxSlot" + i, "helper_boxSlot" + i + "_label", s, TAB_DISPLAY, false, null, null,
                "helper_gridSlot_opt0", "helper_gridSlot_opt1", "helper_gridSlot_opt2", "helper_gridSlot_opt3",
                "helper_gridSlot_opt4", "helper_gridSlot_opt5", "helper_gridSlot_opt6", "helper_gridSlot_opt7"));
        }
        l.add(new Entry(Kind.CHECK, "xpBar", "helper_xpBar_label", s, TAB_DISPLAY, false, null, "helper_xpBar_info"));
        l.add(new Entry(Kind.CHECK, "xpLevel", "helper_xpLevel_label", s, TAB_DISPLAY, false, null, null));


        s = "Hervorgehoben (weiß)";
        for (String key : new String[]{"hiClock", "hiFps", "hiBattery", "hiPlayers", "hiLevel", "hiDps", "hiGold"})
            l.add(new Entry(Kind.CHECK, key, "helper_" + key + "_label", s, TAB_DISPLAY, false, null, null));

        s = "Menü oben rechts";
        l.add(new Entry(Kind.PINS, "menuPinned", "helper_menuPinned_label", s, TAB_CONTROL, false, "helper_menuPinned_lead", null, "pause", "helper_pin_pause", "portal", "helper_pin_portal", "inv", "helper_pin_inv", "char", "helper_pin_char", "skills", "helper_pin_skills", "map", "helper_pin_map", D2MenuRow.QUESTS, "helper_pin_quests", "esc", "helper_pin_esc", "names", "helper_pin_names", "run", "helper_pin_run", "keys", "helper_pin_keys", "drawer", "helper_pin_drawer"));


        s = "Schultertasten";
        l.add(new Entry(Kind.TRIGGERS, "triggers", "helper_triggers_label", s, TAB_CONTROL, false, "helper_triggers_lead", null,
            triggerOptions()));
        l.add(new Entry(Kind.CHECK, "hidePointer", "helper_hidePointer_label", s, TAB_DISPLAY, false, null, "helper_hidePointer_info"));


        s = "Gamepad";
        l.add(new Entry(Kind.CHECK, "padEnabled", "helper_padEnabled_label", s, TAB_CONTROL, false, null, "helper_padEnabled_info"));
        l.add(new Entry(Kind.PAD, "padMap", "helper_padMap_label", s, TAB_CONTROL, false, null, "helper_padMap_info",
            padSlots()));
        l.add(new Entry(Kind.SLIDER, "padDeadzone", "helper_padDeadzone_label", s, TAB_CONTROL, false, null, "helper_padDeadzone_info"));
        l.add(new Entry(Kind.SLIDER, "padCursorSpeed", "helper_padCursorSpeed_label", s, TAB_CONTROL, false, null, "helper_padCursorSpeed_info"));

        s = "Zielhilfe";
        l.add(new Entry(Kind.CHECK, "aimAssist", "helper_aimAssist_label", s, TAB_CONTROL, false, null, null));
        l.add(new Entry(Kind.SLIDER, "aimRangeMelee", "helper_aimRangeMelee_label", s, TAB_CONTROL, false, null, "helper_aimRangeMelee_info"));
        l.add(new Entry(Kind.SLIDER, "aimRange", "helper_aimRange_label", s, TAB_CONTROL, false, null, "helper_aimRange_info"));
        l.add(new Entry(Kind.SLIDER, "aimCone", "helper_aimCone_label", s, TAB_CONTROL, false, null, "helper_aimCone_info"));
        l.add(new Entry(Kind.SLIDER, "aimBlind", "helper_aimBlind_label", s, TAB_CONTROL, false, null, "helper_aimBlind_info"));
        l.add(new Entry(Kind.CHECK, "highlight", "helper_highlight_label", s, TAB_CONTROL, false, null, "helper_highlight_info"));

        s = "Tränke";
        l.add(new Entry(Kind.CHOICE, "potions", "helper_potions_label", s, TAB_AUTO, false, null, null, "helper_potions_opt0", "helper_potions_opt1", "helper_potions_opt2"));
        l.add(new Entry(Kind.SLIDER, "autoLife", "helper_autoLife_label", s, TAB_AUTO, true, "helper_autoLife_lead", null));
        l.add(new Entry(Kind.SLIDER, "autoRejuv", "helper_autoRejuv_label", s, TAB_AUTO, false, null, "helper_autoRejuv_info"));
        l.add(new Entry(Kind.SLIDER, "autoMana", "helper_autoMana_label", s, TAB_AUTO, true, null, "helper_autoMana_info"));
        l.add(new Entry(Kind.SLIDER, "mercPotions", "helper_mercPotions_label", s, TAB_AUTO, false, null, "helper_mercPotions_info"));

        s = "Spiel";
        l.add(new Entry(Kind.SLIDER, "players", "helper_players_label", s, TAB_GAME, true, null, "helper_players_info"));
        l.add(new Entry(Kind.CHECK, "largeStacks", "helper_largeStacks_label", s, TAB_GAME, false, null, "helper_largeStacks_info"));
        l.add(new Entry(Kind.CHECK, "materials", "helper_materials_label", s, TAB_GAME, false, null, "helper_materials_info"));
        l.add(new Entry(Kind.CHECK, "resume", "helper_resume_label", s, TAB_GAME, false, null, "helper_resume_info"));
        l.add(new Entry(Kind.CHECK, "mapHack", "helper_mapHack_label", s, TAB_DISPLAY, true, null, "helper_mapHack_info"));
        l.add(new Entry(Kind.CHECK, "mapFightHide", "helper_mapFightHide_label", s, TAB_DISPLAY, false, null, null));
        l.add(new Entry(Kind.SLIDER, "mapFightDelay", "helper_mapFightDelay_label", s, TAB_DISPLAY, false, null, "helper_mapFightDelay_info"));

        s = "Gegenstände";
        l.add(new Entry(Kind.CHECK, "itemLabels", "helper_itemLabels_label", s, TAB_DISPLAY, true, null, "helper_itemLabels_info"));
        l.add(new Entry(Kind.CHECK, "dropLabels", "helper_dropLabels_label", s, TAB_DISPLAY, false, null, null));
        l.add(new Entry(Kind.CHECK, "labelsNearOnly", "helper_labelsNearOnly_label", s, TAB_DISPLAY, false, null, null));

        l.add(new Entry(Kind.CHOICE, "lootFilter", "helper_lootFilter_label", s, TAB_DISPLAY, false, null, "helper_lootFilter_info", "helper_lootFilter_opt0", "helper_lootFilter_opt1", "helper_lootFilter_opt2", "helper_lootFilter_opt3"));
        l.add(new Entry(Kind.PINS, "lootHide", "helper_lootHide_label", s, TAB_DISPLAY, false, "helper_lootHide_lead", null, "ammo", "helper_loot_ammo", "scrolls", "helper_loot_scrolls", "keys", "helper_loot_keys", "potionsSmall", "helper_loot_potionsSmall", "potions", "helper_loot_potions", "potionsOther", "helper_loot_potionsOther", "rejuv", "helper_loot_rejuv", "gemsChipped", "helper_loot_gemsChipped", "gemsFlawed", "helper_loot_gemsFlawed", "white", "helper_loot_white", "inferior", "helper_loot_inferior", "superior", "helper_loot_superior", "magic", "helper_loot_magic", "gold", "helper_loot_gold"));
        l.add(new Entry(Kind.CHECK, "compareEquipped", "helper_compareEquipped_label", s, TAB_DISPLAY, false, null, "helper_compareEquipped_info"));
        l.add(new Entry(Kind.CHECK, "valueLine", "helper_valueLine_label", s, TAB_DISPLAY, false, null, "helper_valueLine_info"));
        l.add(new Entry(Kind.CHECK, "diffLines", "helper_diffLines_label", s, TAB_DISPLAY, false, null, "helper_diffLines_info"));

        s = "Schaden";
        l.add(new Entry(Kind.CHOICE, "damageStyle", "helper_damageStyle_label", s, TAB_DISPLAY, false, null, "helper_damageStyle_info",
            "helper_damageStyle_opt0", "helper_damageStyle_opt1", "helper_damageStyle_opt2"));
        l.add(new Entry(Kind.CHECK, "damageDps", "helper_damageDps_label", s, TAB_DISPLAY, false, null, "helper_damageDps_info"));
        s = "Gegenstände";
        l.add(new Entry(Kind.CHECK, "autoGold", "helper_autoGold_label", s, TAB_AUTO, false, null, null));
        l.add(new Entry(Kind.CHECK, "pickPotions", "helper_pickPotions_label", s, TAB_AUTO, false, null, "helper_pickPotions_info"));
        l.add(new Entry(Kind.CHECK, "pickScrolls", "helper_pickScrolls_label", s, TAB_AUTO, false, null, null));
        l.add(new Entry(Kind.CHECK, "pickRunes", "helper_pickRunes_label", s, TAB_AUTO, false, null, null));
        l.add(new Entry(Kind.CHECK, "pickGems", "helper_pickGems_label", s, TAB_AUTO, false, null, null));
        l.add(new Entry(Kind.CHECK, "pickJewelry", "helper_pickJewelry_label", s, TAB_AUTO, false, null, null));
        l.add(new Entry(Kind.CHECK, "beltRefill", "helper_beltRefill_label", s, TAB_AUTO, false, null, "helper_beltRefill_info"));
        l.add(new Entry(Kind.CHECK, "itemsNeedLabels", "helper_itemsNeedLabels_label", s, TAB_CONTROL, false, null, "helper_itemsNeedLabels_info"));
        l.add(new Entry(Kind.CHOICE, "ringSort", "helper_ringSort_label", s, TAB_CONTROL, false, "helper_ringSort_lead", "helper_ringSort_info", "helper_ringSort_opt0", "helper_ringSort_opt1"));

        s = "Händler";
        l.add(new Entry(Kind.CHECK, "autoTrade", "helper_autoTrade_label", s, TAB_AUTO, false, null, "helper_autoTrade_info"));

        l.add(new Entry(Kind.CHECK, "potionShop", "helper_potionShop_label", s, TAB_AUTO, false, null, "helper_potionShop_info"));
        l.add(new Entry(Kind.SLIDER, "shopReserveHeal", "helper_shopReserveHeal_label", s, TAB_AUTO, false, null, null));
        l.add(new Entry(Kind.SLIDER, "shopReserveMana", "helper_shopReserveMana_label", s, TAB_AUTO, false, null, "helper_shopReserve_info"));
        l.add(new Entry(Kind.SLIDER, "minGold", "helper_minGold_label", s, TAB_AUTO, false, null, "helper_minGold_info"));

        s = "Pause";
        l.add(new Entry(Kind.CHECK, "autoPause", "helper_autoPause_label", s, TAB_AUTO, false, null, "helper_autoPause_info"));

        s = "Gehen und Laufen";
        l.add(new Entry(Kind.CHECK, "alwaysRun", "helper_alwaysRun_label", s, TAB_CONTROL, true, null, null));
        l.add(new Entry(Kind.CHOICE, "staminaMode", "helper_staminaMode_label", s, TAB_AUTO, false, "helper_staminaMode_lead", "helper_staminaMode_info", "helper_staminaMode_opt0", "helper_staminaMode_opt1", "helper_staminaMode_opt2"));
        l.add(new Entry(Kind.CHECK, "directWalk", "helper_directWalk_label", s, TAB_CONTROL, false, null, "helper_directWalk_info"));
        l.add(new Entry(Kind.CHECK, "tapMoves", "helper_tapMoves_label", s, TAB_CONTROL, false, null, "helper_tapMoves_info"));
        l.add(new Entry(Kind.CHECK, "joystickVisible", "helper_joystickVisible_label", s, TAB_DISPLAY, false, null, "helper_joystickVisible_info"));

        s = "Hilfe";
        l.add(new Entry(Kind.BUTTON, null, "helper_intro_label", s, TAB_GAME, false, "helper_intro_lead", null));

        s = "Fehlersuche";
        l.add(new Entry(Kind.BUTTON, null, "helper_diagnose_label", s, TAB_GAME, false, "helper_diagnose_lead", null));
        l.add(new Entry(Kind.BUTTON, null, "helper_report_label", s, TAB_GAME, false, "helper_report_lead", null));
        return l;
    }


    private static String[] triggerOptions() {
        String[] names = {"trigger_action_skill", "trigger_action_heal", "trigger_action_mana", "trigger_action_rejuv",
            "trigger_action_take", "trigger_action_portal", "trigger_action_map", "trigger_action_inventory",
            "trigger_action_attack", "trigger_action_names"};
        String[] options = new String[2 * D2TriggerAction.ALL.length];
        for (int i = 0; i < D2TriggerAction.ALL.length; i++) {
            int action = D2TriggerAction.ALL[i];
            options[2 * i] = Integer.toString(action);

            options[2 * i + 1] = action == D2TriggerAction.ATTACK_OR_TAKE ? "trigger_action_attack_take" : names[action];
        }
        return options;
    }


    private static String[] padSlots() {
        String[] slots = new String[2 * D2Gamepad.ASSIGNABLE.length];
        for (int i = 0; i < D2Gamepad.ASSIGNABLE.length; i++) {
            slots[i] = D2Gamepad.slot(D2Gamepad.ASSIGNABLE[i], false);
            slots[D2Gamepad.ASSIGNABLE.length + i] = D2Gamepad.slot(D2Gamepad.ASSIGNABLE[i], true);
        }
        return slots;
    }


    public static List<Entry> all(Text t) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all()) out.add(new Entry(e, t));
        return out;
    }










    public static final String SET_CONTROL = "steuerung", SET_DISPLAY = "anzeige", SET_LOOT = "beute", SET_POTIONS = "traenke",
        SET_GAME = "spiel", SET_SAVES = "spielstaende", SET_HELP = "hilfe";
    public static final List<String> SETTINGS_GROUPS = Collections.unmodifiableList(java.util.Arrays.asList(
        SET_CONTROL, SET_DISPLAY, SET_LOOT, SET_POTIONS, SET_GAME, SET_SAVES, SET_HELP));

    public static final Map<String, List<String>> SECTIONS = new LinkedHashMap<>();
    private static final Map<String, String[]> PLACE = new HashMap<>();

    public static final int IN_GAME = 0, BEFORE_START = 1, NEXT_START = 2;
    private static final Map<String, Integer> SCOPE = new HashMap<>();

    private static final Map<String, String> KEYWORDS = new HashMap<>();

    static {
        section(SET_CONTROL, "gehen", "alwaysRun", "directWalk", "tapMoves", "staminaMode");
        section(SET_CONTROL, "joystick", "joystickVisible", "hidePointer");
        section(SET_CONTROL, "zielhilfe", "aimAssist", "aimRangeMelee", "aimRange", "aimCone", "aimBlind", "highlight");
        section(SET_CONTROL, "schultertasten", "triggers");
        section(SET_CONTROL, "gamepad", "padEnabled", "padMap", "padDeadzone", "padCursorSpeed");
        section(SET_CONTROL, "menue", "menuPinned", "editButtons");
        section(SET_DISPLAY, "untere_leiste", "hud", "orbNumbers", "skillNames");
        section(SET_DISPLAY, "infoleiste", "infoGrid", "gridSlot1", "gridSlot2", "gridSlot3", "gridSlot4", "clockTop", "boxSlot1",
            "boxSlot2", "boxSlot3", "xpBar", "xpLevel");
        section(SET_DISPLAY, "hervorgehoben", "hiClock", "hiFps", "hiBattery", "hiPlayers", "hiLevel", "hiDps", "hiGold");
        section(SET_DISPLAY, "karte", "mapHack", "mapFightHide", "mapFightDelay");
        section(SET_DISPLAY, "schaden", "damageStyle", "damageDps");
        section(SET_LOOT, "namen", "itemLabels", "dropLabels", "labelsNearOnly");
        section(SET_LOOT, "lootfilter", "lootFilter", "lootHide");
        section(SET_LOOT, "aufheben", "autoGold", "pickPotions", "pickScrolls", "pickRunes", "pickGems", "pickJewelry");
        section(SET_LOOT, "nehmen", "itemsNeedLabels", "ringSort");
        section(SET_LOOT, "ansehen", "compareEquipped", "valueLine", "diffLines");
        section(SET_LOOT, "materialien", "materials");
        section(SET_POTIONS, "traenke", "potions", "autoLife", "autoRejuv", "autoMana", "mercPotions", "beltRefill");
        section(SET_POTIONS, "haendler", "autoTrade", "potionShop", "shopReserveHeal", "shopReserveMana", "minGold");
        section(SET_GAME, "ton", "sound");
        section(SET_GAME, "im_spiel", "players", "autoPause", "largeStacks", "resume");
        section(SET_GAME, "start", "gameLines", "frameRate", "skipIntro", "mod", "appLanguage");
        section(SET_GAME, "plugy", "plugy", "bigStash", "autosave", "respec", "statsPages", "plugyActiveSharedGold", "plugyActiveLadderRunewords", "plugyActiveDisplayItemLevel", "plugyActiveDisplayBaseStatsValue");

        section(SET_GAME, "plugy_extras", "plugyActiveWorldEvent", "plugyActiveUberQuest", "plugyActiveCowPortalWhenCowKingWasKilled", "plugyActiveDoNotCloseNihlathakPortal", "plugyMoveCainNearHarrogathWaypoint", "plugyAlwaysRegenMapInSP");
        section(SET_SAVES, "ordner", "gameFolder", "install", "shortcut");
        section(SET_SAVES, "spielstaende", "savesShare", "savesLoad");
        section(SET_HELP, "hilfe", "intro", "background");
        section(SET_HELP, "fehlersuche", "diagnose", "report");
        section(SET_HELP, "ueber", "news", "updateCheck", "licenses", "about");
        for (String k : new String[]{"gameFolder", "install", "savesShare", "savesLoad"}) SCOPE.put(k, BEFORE_START);
        for (String k : new String[]{"gameLines", "frameRate", "skipIntro", "mod", "appLanguage", "plugy", "bigStash", "autosave",
            "respec", "statsPages"}) SCOPE.put(k, NEXT_START);
        for (String k : new String[]{"plugyActiveSharedGold", "plugyActiveLadderRunewords", "plugyActiveDisplayItemLevel", "plugyActiveDisplayBaseStatsValue", "plugyActiveWorldEvent", "plugyActiveUberQuest", "plugyActiveCowPortalWhenCowKingWasKilled", "plugyActiveDoNotCloseNihlathakPortal", "plugyMoveCainNearHarrogathWaypoint", "plugyAlwaysRegenMapInSP"}) SCOPE.put(k, NEXT_START);
        keywords("aimBlind", "toter winkel blind spot hinter ruecken behind auto aim");
        keywords("damageStyle", "schaden zahlen schadenszahlen damage numbers dmg");
        keywords("damageDps", "dps schaden pro sekunde damage per second");
        keywords("mapHack", "map karte aufdecken minimap reveal automap");
        keywords("lootFilter", "loot filter lootfilter beute ausblenden hide");
        keywords("lootHide", "loot filter lootfilter eigene own");

        keywords("infoGrid", "info raster grid infoleiste akku batterie battery fps bilder frames spieler players stufe level dps gold");
        for (int i = 1; i <= 4; i++) keywords("gridSlot" + i, "raster grid platz slot akku batterie battery fps bilder frames dps gold");
        keywords("clockTop", "uhr zeit clock time box mini leiste panel");
        for (int i = 1; i <= 3; i++) keywords("boxSlot" + i, "box uhr clock platz slot dps gold fps akku battery");
        for (String key : new String[]{"hiClock", "hiFps", "hiBattery", "hiPlayers", "hiLevel", "hiDps", "hiGold"})
            keywords(key, "hervorgehoben hervorheben weiss gold farbe colour color highlight raster box");
        keywords("xpLevel", "stufe level lvl erfahrung experience leiste bar");
        keywords("xpBar", "erfahrung experience xp exp stufe level leiste bar");
        keywords("padEnabled", "gamepad controller pad xbox");
        keywords("padMap", "gamepad controller pad tasten buttons belegung zuweisung");
        keywords("padCursorSpeed", "gamepad controller pad zeiger cursor maus mouse pointer tempo speed geschwindigkeit");
        keywords("bigStash", "truhe stash gross big kiste");
        keywords("plugy", "plugy truhe stash shared geteilt runenwoerter ladder");
        keywords("players", "spieler players schwierigkeit drops");
        keywords("triggers", "schultertasten trigger tasten l r redmagic");
        keywords("menuPinned", "menue knoepfe oben rechts buttons");
        keywords("editButtons", "knoepfe verschieben bearbeiten edit buttons move groesse");
        keywords("aimAssist", "zielhilfe aim ziel automatisch auto target");
        keywords("autoLife", "trank heiltrank leben potion life heal automatisch");
        keywords("autoMana", "trank manatrank mana potion automatisch");
        keywords("autoRejuv", "verjuengung rejuv trank potion");
        keywords("autoGold", "gold aufheben pick up");
        keywords("itemLabels", "namen labels alt boden items gegenstaende");
        keywords("appLanguage", "sprache language deutsch englisch langue idioma lingua francais espanol italiano");
        keywords("gameLines", "groesse size zeilen aufloesung resolution schrift");
        keywords("frameRate", "fps bildrate frame rate akku battery");
        keywords("skipIntro", "intro videos filme movies");
        keywords("sound", "ton sound musik music titelmusik klick click lautsprecher stumm mute leise");
        keywords("background", "hintergrund background akku benachrichtigung notification");
        keywords("savesShare", "spielstaende saves teilen share backup sichern");
        keywords("savesLoad", "spielstaende saves laden load backup");
        keywords("gameFolder", "ordner folder spielordner pfad");
        keywords("install", "installieren install download");
        keywords("shortcut", "verknuepfung shortcut symbol icon startbildschirm home screen launcher desktop");
        keywords("intro", "einfuehrung tour hilfe help");
        keywords("diagnose", "diagnose fehler log logs");
        keywords("report", "fehler melden bug report");
        keywords("licenses", "lizenzen licenses open source");
        keywords("news", "neuigkeiten news aenderungen changelog version");
        keywords("updateCheck", "update neue version");
        keywords("respec", "respec zuruecksetzen punkte reset");
        keywords("statsPages", "werte seiten stats pages widerstaende");
        keywords("plugyActiveSharedGold", "plugy gold geteilt shared");
        keywords("plugyActiveLadderRunewords", "plugy runenwoerter runewords ladder");
        keywords("plugyActiveDisplayItemLevel", "plugy gegenstandsstufe ilvl item level stufe");
        keywords("plugyActiveDisplayBaseStatsValue", "plugy grundwerte base stats werte");
        keywords("plugyActiveWorldEvent", "plugy weltereignis world event diablo klon clone");
        keywords("plugyActiveUberQuest", "plugy uber quest torch fackel");
        keywords("plugyActiveCowPortalWhenCowKingWasKilled", "plugy kuh cow level portal");
        keywords("plugyActiveDoNotCloseNihlathakPortal", "plugy nihlathak portal");
        keywords("plugyMoveCainNearHarrogathWaypoint", "plugy cain harrogath wegpunkt waypoint");
        keywords("plugyAlwaysRegenMapInSP", "plugy karte map neu new regen laden load fortsetzen weiterspielen");
        keywords("autosave", "speichern save automatisch");
        keywords("hud", "leiste hud kugeln orbs kompakt");
        keywords("valueLine", "wert bewertung value");
        keywords("compareEquipped", "vergleich vergleichen compare angelegt");
        keywords("materials", "materialien runen edelsteine edelsteinen lager stash");
        keywords("autoTrade", "handel haendler trade shop verkaufen");
        keywords("potionShop", "traenke auffuellen laden shop kaufen");
        keywords("resume", "fortsetzen weiterspielen resume");
        keywords("autoPause", "pause hintergrund");
    }


    private static void section(String group, String section, String... ids) {
        SECTIONS.computeIfAbsent(group, g -> new ArrayList<>()).add(section);
        for (String id : ids) {
            if (PLACE.put(id, new String[]{group, section}) != null) throw new IllegalStateException("settings place twice: " + id);
        }
    }


    public static java.util.Set<String> placedIds() {
        return Collections.unmodifiableSet(PLACE.keySet());
    }

    private static void keywords(String id, String words) {
        KEYWORDS.put(id, words);
    }


    public static String settingsGroup(String id) {
        String[] p = PLACE.get(id);
        return p != null ? p[0] : null;
    }


    public static String settingsSection(String id) {
        String[] p = PLACE.get(id);
        return p != null ? p[1] : null;
    }


    public static int scope(String id) {
        Integer s = SCOPE.get(id);
        return s != null ? s : IN_GAME;
    }


    public static String groupTitle(String group) {
        return "settings_group_" + group;
    }

    public static String groupDescription(String group) {
        return "settings_group_" + group + "_desc";
    }

    public static String sectionTitle(String section) {
        return "settings_section_" + section;
    }


    public static String id(Entry e) {
        return e.key != null ? e.key : e.action;
    }





    public static List<Entry> hostEntries() {
        List<Entry> l = new ArrayList<>();
        String[][] items = {
            {"sound", "settings_sound"}, {"appLanguage", "settings_language"}, {"gameLines", "settings_size"}, {"frameRate", "settings_frame_rate"},
            {"skipIntro", "settings_skip_intro"}, {"mod", "settings_mod"}, {"plugy", "settings_plugy_use"},
            {"bigStash", "settings_plugy_big_stash"}, {"autosave", "settings_plugy_autosave"}, {"respec", "settings_plugy_respec"},
            {"statsPages", "settings_plugy_stats"}, {"plugyActiveSharedGold", "settings_plugy_ActiveSharedGold"}, {"plugyActiveLadderRunewords", "settings_plugy_ActiveLadderRunewords"}, {"plugyActiveDisplayItemLevel", "settings_plugy_ActiveDisplayItemLevel"}, {"plugyActiveDisplayBaseStatsValue", "settings_plugy_ActiveDisplayBaseStatsValue"}, {"plugyActiveWorldEvent", "settings_plugy_ActiveWorldEvent"}, {"plugyActiveUberQuest", "settings_plugy_ActiveUberQuest"}, {"plugyActiveCowPortalWhenCowKingWasKilled", "settings_plugy_ActiveCowPortalWhenCowKingWasKilled"}, {"plugyActiveDoNotCloseNihlathakPortal", "settings_plugy_ActiveDoNotCloseNihlathakPortal"}, {"plugyMoveCainNearHarrogathWaypoint", "settings_plugy_MoveCainNearHarrogathWaypoint"}, {"plugyAlwaysRegenMapInSP", "settings_plugy_AlwaysRegenMapInSP"}, {"background", "settings_background"}, {"gameFolder", "settings_game_folder"},
            {"install", "settings_install"}, {"shortcut", "settings_shortcut"}, {"savesShare", "settings_saves_share"},
            {"savesLoad", "settings_saves_load"},
            {"news", "news_title"}, {"updateCheck", "settings_update_check"}, {"licenses", "licenses_title"},
            {"about", "settings_about"}, {"editButtons", "settings_edit_buttons"}};
        for (String[] item : items) l.add(new Entry(Kind.HOST, item[0], item[1], null, null, false, null, null));
        return l;
    }


    public static List<Entry> settings(Text t) {
        List<Entry> out = all(t);
        for (Entry e : hostEntries()) out.add(new Entry(e, t));
        return out;
    }


    public static boolean shownIn(Entry e, boolean internal) {
        if (!shown(e, internal)) return false;
        return !(internal && "updateCheck".equals(e.key));
    }


    public static final class Hit {
        public final Entry entry;
        public final String group, section;

        Hit(Entry entry, String group, String section) {
            this.entry = entry;
            this.group = group;
            this.section = section;
        }
    }








    public static List<Hit> search(String query, List<Entry> entries, Text t) {
        List<Hit> out = new ArrayList<>();
        String[] words = normal(query == null ? "" : query).trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) return out;
        for (String group : SETTINGS_GROUPS) {
            for (String section : SECTIONS.get(group)) {
                for (Entry e : entries) {
                    String id = id(e);
                    if (id == null || !group.equals(settingsGroup(id)) || !section.equals(settingsSection(id))) continue;
                    String hay = " " + normal(e.label + " " + (e.lead != null ? e.lead : "") + " " + (e.info != null ? e.info : "")
                        + " " + String.join(" ", e.options) + " " + t.get(groupTitle(group)) + " " + t.get(sectionTitle(section))
                        + " " + KEYWORDS.getOrDefault(id, ""));
                    boolean all = true;
                    for (String w : words) {
                        if (!hay.contains(" " + w)) {
                            all = false;
                            break;
                        }
                    }
                    if (all) out.add(new Hit(e, group, section));
                }
            }
        }
        return out;
    }


    static String normal(String s) {
        String l = s.toLowerCase(java.util.Locale.ROOT).replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        return l.replaceAll("[^a-z0-9]+", " ");
    }


    public static String migrateTab(String oldTab) {
        if (TAB_AUTO.equals(oldTab)) return SET_POTIONS;
        if (TAB_DISPLAY.equals(oldTab)) return SET_DISPLAY;
        if (TAB_CONTROL.equals(oldTab)) return SET_CONTROL;
        if (TAB_GAME.equals(oldTab)) return SET_GAME;
        return SETTINGS_GROUPS.contains(oldTab) ? oldTab : SET_CONTROL;
    }
}
