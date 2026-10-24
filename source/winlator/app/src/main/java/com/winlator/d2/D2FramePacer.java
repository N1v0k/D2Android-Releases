package com.winlator.d2;












public final class D2FramePacer {

    static final double BURST = 2;
    private double perNs;
    private double credit;
    private long lastNs;
    private boolean started, waiting;


    public synchronized void setLimit(int fps) {
        perNs = fps > 0 ? fps / 1e9 : 0;
        started = false;
    }

    private void refill(long now) {
        if (!started) {
            credit = BURST;
            started = true;
        }
        else credit = Math.min(BURST, credit + Math.max(0, now - lastNs) * perNs);
        lastNs = now;
    }

    private long untilCredit() {
        return Math.max(1, (long) Math.ceil((1 - credit) / perNs));
    }





    public synchronized long request(long now) {
        if (perNs == 0) return 0;
        if (waiting) return -1;
        refill(now);
        if (credit >= 1) {
            credit -= 1;
            return 0;
        }
        waiting = true;
        return untilCredit();
    }


    public synchronized long due(long now) {
        if (perNs == 0) {
            waiting = false;
            return 0;
        }
        refill(now);
        if (credit < 1) return untilCredit();
        credit -= 1;
        waiting = false;
        return 0;
    }
}
