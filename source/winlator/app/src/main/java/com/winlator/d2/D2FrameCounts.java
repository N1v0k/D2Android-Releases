package com.winlator.d2;

import java.util.concurrent.atomic.AtomicInteger;

public final class D2FrameCounts {
    public static final AtomicInteger overlayDraws = new AtomicInteger(), modStates = new AtomicInteger(),
        glFrames = new AtomicInteger(), glContent = new AtomicInteger(), glPointer = new AtomicInteger(),
        glPointerSkipped = new AtomicInteger();
    public static final long EVERY_MS = 60_000;
    private static long since = -1;

    private D2FrameCounts() {}

    public static synchronized String due(long nowMs) {
        if (since < 0) {
            since = nowMs;
            reset();
            return null;
        }
        if (nowMs - since < EVERY_MS) return null;
        String line = line(nowMs - since, overlayDraws.getAndSet(0), modStates.getAndSet(0), glFrames.getAndSet(0),
            glContent.getAndSet(0), glPointer.getAndSet(0), glPointerSkipped.getAndSet(0));
        since = nowMs;
        return line;
    }

    static String line(long ms, int overlay, int states, int frames, int content, int pointer, int skipped) {
        return "rates: overlay " + rate(overlay, ms) + "/s, mod states " + rate(states, ms) + "/s, gl frames "
            + rate(frames, ms) + "/s (asked: content " + rate(content, ms) + "/s, pointer " + rate(pointer, ms)
            + "/s, hidden pointer skipped " + rate(skipped, ms) + "/s) over " + (ms / 1000) + " s";
    }

    private static String rate(int count, long ms) {
        if (ms <= 0) return "0";
        long tenths = Math.round(count * 10000.0 / ms);
        return (tenths / 10) + "." + (tenths % 10);
    }

    private static void reset() {
        overlayDraws.set(0);
        modStates.set(0);
        glFrames.set(0);
        glContent.set(0);
        glPointer.set(0);
        glPointerSkipped.set(0);
    }

    public static synchronized void restart() {
        since = -1;
    }
}
