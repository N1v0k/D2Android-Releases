package com.winlator.d2;









final class D2CommandWatch {

    static final int MIN_SENT = 8;
    static final long WINDOW_MS = 3000;
    static final int NOTHING = 0, STALLED = 1, RECOVERED = 2;

    private int lastCounter = -1;
    private int pending;
    private long pendingSince;
    private boolean stalled;


    synchronized void sent(long now) {
        if (pending++ == 0) pendingSince = now;
    }





    synchronized int state(int counter, long now, boolean afterGap) {
        if (afterGap || counter != lastCounter) {
            boolean moved = !afterGap && lastCounter >= 0;
            lastCounter = counter;
            pending = 0;
            if (moved && stalled) {
                stalled = false;
                return RECOVERED;
            }
            return NOTHING;
        }
        if (!stalled && pending >= MIN_SENT && now - pendingSince >= WINDOW_MS) {
            stalled = true;
            return STALLED;
        }
        return NOTHING;
    }


    synchronized void reset() {
        lastCounter = -1;
        pending = 0;
        stalled = false;
    }


    synchronized int pending() {
        return pending;
    }
}
