package com.winlator.d2;

public final class D2TriggerAction {
    private D2TriggerAction() {}

    public static final int SKILL = 0, HEAL = 1, MANA = 2, REJUV = 3, TAKE = 4, PORTAL = 5, MAP = 6, INVENTORY = 7,
        ATTACK = 8,
        NAMES = 9,
        SKILL_F1 = 10, SKILL_F8 = 17,
        ESC_MENU = 18, APP_MENU = 19, WEAPON_SWAP = 20, RUN_TOGGLE = 21,
        ATTACK_OR_TAKE = 22;
    public static final int KEY = -1, NOTHING = -2;
    public static final int[] ALL = {SKILL, ATTACK, ATTACK_OR_TAKE, HEAL, MANA, REJUV, TAKE, PORTAL, MAP, INVENTORY, NAMES};
    public static final int[] PAD = {ATTACK_OR_TAKE, ATTACK, SKILL_F1, 11, 12, 13, 14, 15, 16, SKILL_F8, HEAL, MANA, REJUV,
        TAKE, PORTAL, MAP, INVENTORY, NAMES, WEAPON_SWAP, RUN_TOGGLE, ESC_MENU, APP_MENU, NOTHING};

    public static int of(int stored) {
        for (int action : ALL) if (action == stored) return action;
        return SKILL;
    }

    public static int ofPad(int stored, int fallback) {
        for (int action : PAD) if (action == stored) return action;
        return fallback;
    }

    public static boolean isSkillKey(int action) {
        return action >= SKILL_F1 && action <= SKILL_F8;
    }

    public static int skillKeyIndex(int action) {
        return action - SKILL_F1;
    }

    public static int resolve(int action, boolean controlsVisible, boolean playtest) {
        int a = playtest ? SKILL : (action == NOTHING ? NOTHING : ofPad(action, of(action)));
        if (a == NOTHING) return NOTHING;
        if (controlsVisible) return a;
        if (a == SKILL || isSkillKey(a)) return KEY;
        if (a == TAKE || a == ATTACK || a == ATTACK_OR_TAKE) return NOTHING;
        return a;
    }

    public static boolean held(int resolved) {
        return resolved == SKILL || resolved == KEY || resolved == ATTACK || resolved == NAMES || isSkillKey(resolved)
            || resolved == ATTACK_OR_TAKE;
    }
}
