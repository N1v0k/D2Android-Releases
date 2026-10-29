package com.winlator.d2;











final class D2LayerSender {
    interface Out {
        void send(byte[] packet);
    }

    static final long REPEAT_MS = 500, VALID_MS = 1000, FALLBACK_MS = 1500, GAP_MS = 2000;
    private long lastCall;

    private final int layer;
    private D2Layer last;
    private int stand, width, height;
    private long sentAt, shownSince, fellAt;
    private boolean on, fallback, tooBig;
    private String why;

    D2LayerSender(int layer) {
        this.layer = layer;
    }


    boolean update(D2Layer list, int gameWidth, int gameHeight, long now, int drawn, long drawnAt, Out out) {
        return update(list, gameWidth, gameHeight, now, drawn, drawnAt, true, out);
    }







    boolean update(D2Layer list, int gameWidth, int gameHeight, long now, int drawn, long drawnAt, boolean canDraw, Out out) {


        if (on && now - lastCall > GAP_MS) reset();
        lastCall = now;
        if (list == null || list.isEmpty()) {
            if (on) out.send(new D2Layer().packets(layer, ++stand, gameWidth, gameHeight, (int) VALID_MS)[0]);
            reset();
            return false;
        }
        if (!on) {
            on = true;
            shownSince = now;
        }
        if (tooBig) return false;
        boolean changed = last == null || !list.sameAs(last) || gameWidth != width || gameHeight != height;
        if (changed || now - sentAt >= REPEAT_MS) {
            if (changed) stand++;
            byte[][] packets = list.packets(layer, stand, gameWidth, gameHeight, (int) VALID_MS);
            if (packets == null) {
                tooBig = true;
                return fall(now, "the list does not fit command 33 (" + list.drawOps() + " operations)");
            }
            for (byte[] p : packets) out.send(p);
            last = list;
            width = gameWidth;
            height = gameHeight;
            sentAt = now;
        }

        if (layer >= 8) return true;
        boolean drawnBit = drawn >= 0 && (drawn & (1 << layer)) != 0;
        if (fallback) {

            if (drawnBit && drawnAt > fellAt) {
                fallback = false;
                why = null;
                shownSince = now;
                return true;
            }
            return false;
        }
        if (!canDraw) {
            shownSince = now;
            return true;
        }

        boolean reportAfter = drawnAt > shownSince + REPEAT_MS;
        if (now - shownSince >= FALLBACK_MS && reportAfter && !drawnBit) {
            return fall(now, "the mod did not report layer " + layer + " as drawn");
        }
        if (now - shownSince >= 3 * FALLBACK_MS && !reportAfter) return fall(now, "no report of the mod since the layer was shown");
        return true;
    }

    private void reset() {
        on = false;
        fallback = false;
        tooBig = false;
        last = null;
        why = null;
    }

    private boolean fall(long now, String reason) {
        fallback = true;
        fellAt = now;
        why = reason;
        return false;
    }


    String fallbackReason() {
        return fallback ? why : null;
    }
}
