package com.winlator.d2.test;

import com.winlator.d2.D2Points;

public final class PunkteFrisch {
    public static final long MAX_MS = 1500;
    private long nr = Long.MIN_VALUE;
    private D2Points block;
    private long am;

    public D2Points block(Zustand z, long now) {
        if (z.punkte6 != null && z.punkte6Nr != nr) {
            nr = z.punkte6Nr;
            block = D2Points.parse(z.punkte6, z.punkte6.length, 0);
            am = now;
        }
        return block != null && now - am <= MAX_MS ? block : null;
    }
}
