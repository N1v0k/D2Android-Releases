package com.winlator.d2;

import com.winlator.d2.test.Fertigkeiten;










public final class D2AimReach {
    private D2AimReach() {}


    public static final int RANGE_UNKNOWN = -1;

    public static final int ATTACK_UNKNOWN = -1, ATTACK_STRIKES = 0, ATTACK_SHOOTS = 1;


    public static boolean melee(int skillRange, int attack) {
        if (skillRange == Fertigkeiten.RANGE_H2H) return true;
        return skillRange == Fertigkeiten.RANGE_BOTH && attack == ATTACK_STRIKES;
    }


    public static int range(Fertigkeiten table, int skill) {
        Fertigkeiten.Fertigkeit f = table != null ? table.get(skill) : null;
        return f != null ? f.reichweite : RANGE_UNKNOWN;
    }
}
