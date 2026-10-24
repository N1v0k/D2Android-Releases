package com.winlator.d2;












final class D2Retarget {

    static final long GAP_MS = 100;

    static final double KEEP_MARGIN_DEG = 10;

    private long changedAt;
    private boolean changed;


    void changed(long now) {
        changedAt = now;
        changed = true;
    }


    long waitMs(long now) {
        if (!changed) return 0;
        long left = changedAt + GAP_MS - now;
        return left > 0 ? left : 0;
    }





    static boolean keeps(double dx, double dy, double dirX, double dirY, int coneDeg) {
        double length = Math.hypot(dx, dy);
        if (length <= 1) return true;
        double limit = Math.min(180, coneDeg + KEEP_MARGIN_DEG);
        return (dx * dirX + dy * dirY) / length >= Math.cos(Math.toRadians(limit));
    }
}
