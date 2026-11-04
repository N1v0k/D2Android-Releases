package com.winlator.d2;











public final class D2SwapCast {
    public enum Phase { IDLE, SWAP_OUT, CAST, WAIT_END, SWAP_BACK }


    public static final int NOTHING = 0, PRESS_W = 1, CAST = 2, CAST_TAP = 3, END_CAST = 4, DONE = 5, GIVE_UP = 6;


    static final long SWAP_WAIT_MS = 1200;

    static final long CAST_MAX_MS = 6000;

    static final long END_WAIT_MS = 450;

    public static final long TAP_MS = 150;

    static final long BACK_RETRY_MS = 1200;
    static final int BACK_TRIES = 4;

    static final long FACTS_LOST_MS = 3000;

    private Phase phase = Phase.IDLE;
    private int key = -1;
    private boolean held;

    private boolean castWanted;
    private long since;
    private int counterAtStart, counterAtBack, tries;
    private long factsLostAt = -1;
    private String why = "";

    public Phase phase() {
        return phase;
    }

    public boolean active() {
        return phase != Phase.IDLE;
    }


    public int key() {
        return phase == Phase.IDLE ? -1 : key;
    }


    public String why() {
        return why;
    }


    public static boolean marked(int mask, int key) {
        return key >= 0 && key < 16 && (mask & (1 << key)) != 0;
    }


    public static int availableMask(int[] hotkeyLevels) {
        int mask = 0;
        if (hotkeyLevels != null) for (int i = 0; i < hotkeyLevels.length && i < 16; i++) if (hotkeyLevels[i] > 0) mask |= 1 << i;
        return mask;
    }


    public int start(int key, int counter, long now) {
        if (phase != Phase.IDLE) return NOTHING;
        this.key = key;
        held = true;
        castWanted = true;
        counterAtStart = counter;
        since = now;
        factsLostAt = -1;
        phase = Phase.SWAP_OUT;
        why = "";
        return PRESS_W;
    }


    public int released(long now) {
        held = false;
        if (phase == Phase.CAST) {
            phase = Phase.WAIT_END;
            since = now;
        }
        return NOTHING;
    }





    public void castHeld(long now) {
        if (phase != Phase.WAIT_END) return;
        phase = Phase.CAST;
        since = now;
    }





    public void abort(long now) {
        held = false;
        castWanted = false;
        if (phase == Phase.CAST) {
            phase = Phase.WAIT_END;
            since = now;
        }
    }





    public int tick(long now, int counter, int mask, int available) {

        if (counter < 0 && (phase == Phase.WAIT_END || phase == Phase.SWAP_BACK)) {
            if (factsLostAt < 0) factsLostAt = now;
            return now - factsLostAt > FACTS_LOST_MS ? end("no facts", GIVE_UP) : NOTHING;
        }
        if (counter >= 0) factsLostAt = -1;
        switch (phase) {
            case SWAP_OUT:
                if (counter < 0) return end("no facts", GIVE_UP);
                if (counter != counterAtStart && !castWanted) return back(now, counter);
                if (counter != counterAtStart && (available & (1 << key)) != 0) {
                    since = now;
                    if (held) {
                        phase = Phase.CAST;
                        return CAST;
                    }
                    phase = Phase.WAIT_END;
                    since = now + TAP_MS;
                    return CAST_TAP;
                }
                if (now - since > SWAP_WAIT_MS) {
                    if (counter == counterAtStart) return end(castWanted ? "no swap" : "aborted, no swap", castWanted ? GIVE_UP : DONE);
                    return back(now, counter);
                }
                return NOTHING;
            case CAST:
                if (now - since > CAST_MAX_MS) {
                    phase = Phase.WAIT_END;
                    since = now;
                    return END_CAST;
                }
                return NOTHING;
            case WAIT_END:
                if (now - since >= END_WAIT_MS) return back(now, counter);
                return NOTHING;
            case SWAP_BACK:
                if (counter >= 0 && counter != counterAtBack) return end("back", DONE);
                if (now - since > BACK_RETRY_MS) {
                    if (tries >= BACK_TRIES) return end("swap back not seen", GIVE_UP);
                    tries++;
                    since = now;
                    if (counter >= 0) counterAtBack = counter;
                    return PRESS_W;
                }
                return NOTHING;
            default:
                return NOTHING;
        }
    }


    public void cancel() {
        end("cancelled", NOTHING);
    }

    private int back(long now, int counter) {
        phase = Phase.SWAP_BACK;
        counterAtBack = counter;
        tries = 1;
        since = now;
        return PRESS_W;
    }

    private int end(String reason, int result) {
        phase = Phase.IDLE;
        why = reason;
        held = false;
        return result;
    }
}
