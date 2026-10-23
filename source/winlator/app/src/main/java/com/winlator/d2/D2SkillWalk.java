package com.winlator.d2;








final class D2SkillWalk {

    static final long GRACE_MOUSE_MS = 120;

    static final long GRACE_DIRECT_MS = 150;





    static final long GRACE_ATTACK_TAP_MS = 700;

    private D2SkillWalk() {
    }






    static boolean defers(boolean joystickActive, boolean teleport) {
        return joystickActive && !teleport;
    }





    static long resumeDelay(long pressAt, long now, boolean direct) {
        long grace = direct ? GRACE_DIRECT_MS : GRACE_MOUSE_MS;
        if (pressAt < 0) return grace;
        return Math.max(0, grace - (now - pressAt));
    }





    static long attackTapDelay(long pressAt, long now) {
        if (pressAt < 0) return 0;
        return Math.max(0, GRACE_ATTACK_TAP_MS - (now - pressAt));
    }
}
