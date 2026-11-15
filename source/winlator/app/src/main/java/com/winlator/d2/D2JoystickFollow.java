package com.winlator.d2;
















public final class D2JoystickFollow {

    static final float REVERSE_SPEED_DP = 300f;

    static final float REVERSE_COS = -0.5f;

    static final float REVERSE_REACH = 0.5f;

    static final float SMOOTH_MS = 20f;

    public float baseX, baseY;

    public float startX, startY, minX, minY, maxX, maxY;

    public boolean fixed;

    public int jumps, edgeHits;
    private float lastX, lastY, vx, vy;
    private long lastT = -1;


    public void start(float x, float y, long t) {
        start(x, y, t, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, false);
    }





    public void start(float x, float y, long t, float left, float top, float right, float bottom, boolean fixed) {
        baseX = lastX = startX = x;
        baseY = lastY = startY = y;
        minX = Math.min(left, x);
        maxX = Math.max(right, x);
        minY = Math.min(top, y);
        maxY = Math.max(bottom, y);
        this.fixed = fixed;
        jumps = edgeHits = 0;
        lastT = t;
        vx = vy = 0;
    }


    public boolean move(float x, float y, long t, float radius, float dp) {
        if (lastT < 0) start(x, y, t);
        if (fixed) return false;
        if (t > lastT) {
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
            jumps++;
            return true;
        }
        if (len > radius) {

            float fx = x - dx / len * radius, fy = y - dy / len * radius;
            float nx = Math.max(Math.min(minX, baseX), Math.min(Math.max(maxX, baseX), fx));
            float ny = Math.max(Math.min(minY, baseY), Math.min(Math.max(maxY, baseY), fy));
            if (nx != fx || ny != fy) edgeHits++;
            baseX = nx;
            baseY = ny;
        }
        return false;
    }
}
