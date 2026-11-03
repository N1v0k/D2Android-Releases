package com.winlator.d2;










public final class D2TeleportReach {

    public static final float MARGIN = 24;

    public static final float BOTTOM = 56;

    public static final float SHORTEST = 0.06f;

    public static final float POWER = 1.4f;

    public static final float NO_STICK = 0.75f;

    public static final long STICK_GRACE_MS = 120;

    private D2TeleportReach() {}





    public static float edge(float dx, float dy, float px, float py, int w, int h) {
        float len = (float) Math.hypot(dx, dy);
        if (len < 1e-3f || w <= 0 || h <= 0) return 0;
        float ux = dx / len, uy = dy / len;
        float left = MARGIN, right = w - MARGIN, top = MARGIN, bottom = h - BOTTOM;
        float t = Float.MAX_VALUE;
        if (ux > 1e-4f) t = Math.min(t, (right - px) / ux);
        else if (ux < -1e-4f) t = Math.min(t, (left - px) / ux);
        if (uy > 1e-4f) t = Math.min(t, (bottom - py) / uy);
        else if (uy < -1e-4f) t = Math.min(t, (top - py) / uy);
        return t == Float.MAX_VALUE ? 0 : Math.max(0, t);
    }






    public static float distance(float push, float edge, int gameHeight) {
        if (edge <= 0) return 0;
        if (push < 0) return edge * NO_STICK;
        float p = Math.min(1, push);
        float shortest = Math.min(edge, gameHeight * SHORTEST);
        return shortest + (edge - shortest) * (float) Math.pow(p, POWER);
    }


    public static float joystickPush(float len, float deadZone, float radius) {
        if (radius <= deadZone) return len > deadZone ? 1 : 0;
        return Math.max(0, Math.min(1, (len - deadZone) / (radius - deadZone)));
    }


    public static float padPush(float lx, float ly, int deadZonePercent) {
        float dz = Math.max(0, Math.min(95, deadZonePercent)) / 100f;
        float m = (float) Math.hypot(lx, ly);
        return Math.max(0, Math.min(1, (m - dz) / (1 - dz)));
    }
}
