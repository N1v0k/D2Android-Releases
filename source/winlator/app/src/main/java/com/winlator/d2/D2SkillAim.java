package com.winlator.d2;

final class D2SkillAim {
    private D2SkillAim() {}

    static boolean shiftNeeded(boolean skillLeft, boolean onMonster) {
        return skillLeft && !onMonster;
    }
}
