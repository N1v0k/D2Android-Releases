package com.winlator.d2;









public final class D2JoystickFollow {

    static final float REVERSE_SPEED_DP = 300f;

    static final float REVERSE_COS = -0.5f;

    static final float REVERSE_REACH = 0.5f;

    static final float SMOOTH_MS = 20f;

    public float baseX, baseY;
    private float lastX, lastY, vx, vy;
    private long lastT = -1;


    public void start(float x, float y, long t) {
        baseX = lastX = x;
        baseY = lastY = y;
        lastT = t;
        vx = vy = 0;
    }


    public boolean move(float x, float y, long t, float radius, float dp) {
        if (lastT < 0) start(x, y, t);
        else if (t > lastT) {
            float dt = t - lastT, a = dt / (dt + SMOOTH_MS);
            vx += a * ((x - lastX) / dt - vx);
            vy += a * ((y - lastY) / dt - vy);
            lastX = x;
            lastY = y;
            lastT = t;
        }
        float dx = x - baseX, dy = y - baseY;
        float len = (float) Math.hypot(dx, dy), speed = (float) Math.hypot(vx, vy);
        if (len > 0 && speed * 1000 >= REVERSE_SPEED_DP * dp && vx * dx + vy * dy <= REVERSE_COS * speed * len) {
            baseX = x - vx / speed * radius * REVERSE_REACH;
            baseY = y - vy / speed * radius * REVERSE_REACH;
            return true;
        }
        if (len > radius) {
            baseX = x - dx / len * radius;
            baseY = y - dy / len * radius;
        }
        return false;
    }
}
