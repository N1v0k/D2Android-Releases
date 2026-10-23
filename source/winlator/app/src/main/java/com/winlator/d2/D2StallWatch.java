package com.winlator.d2;








final class D2StallWatch {
    static final long STALL_MS = 5000;
    private long presents = -1, since;
    private boolean reported;





    long check(long nowMs, boolean watching, long presentsNow) {
        if (!watching || presentsNow != presents) {
            if (presentsNow != presents) reported = false;
            presents = presentsNow;
            since = nowMs;
            return -1;
        }
        if (presentsNow <= 0 || reported || nowMs - since < STALL_MS) return -1;
        reported = true;
        return nowMs - since;
    }
}
