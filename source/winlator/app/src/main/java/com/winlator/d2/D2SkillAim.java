package com.winlator.d2;







final class D2SkillAim {
    private D2SkillAim() {}


    static boolean shiftNeeded(boolean skillLeft, boolean onMonster) {
        return skillLeft && !onMonster;
    }







    static final long LEFT_HOLD_MS = 120;





    static boolean leftReleaseMayGo(long pressAt, long now) {
        return pressAt < 0 || now - pressAt >= LEFT_HOLD_MS;
    }


    static final long GROUND_REST_MS = 40;

    static final long GROUND_MAX_MS = 600;









    static boolean groundAimDone(long sinceStart, long sinceArrival) {
        if (sinceStart > GROUND_MAX_MS) return true;
        return sinceArrival >= GROUND_REST_MS;
    }
}
