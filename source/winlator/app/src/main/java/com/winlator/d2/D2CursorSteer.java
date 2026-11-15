package com.winlator.d2;



























final class D2CursorSteer {

    static final long LOST_MS = 2000;

    static final long ARRIVE_MAX_MS = 3000;

    static final long QUIET_MS = 1000;

    static final long CONFIRM_MS = 500;

    static final long SPOT_MS = 120;

    static final int EXACT = 6;

    static final int MAX_STEP = 2000;

    static final int NEAR = 2;


    static final int WAIT = 0, STEP = 1, ASK = 2, LOST = 3, AT = 4, UNREACHABLE = 5, GIVE_UP = 6, EXCUSE = 7;

    static final int W_NONE = 0, W_LOCKED = 1;

    static final int R_NONE = 0, R_PROBE = 1, R_FREE = 2;


    float gainX = 1, gainY = 1;

    final int[] out = new int[2];
    private int fromX, fromY, stepX, stepY;
    private boolean stepOpen, foreign = true, epochKnown, ambiguous;

    private boolean firstStep, excuseCheck;
    private int seenRestarts = -1, stepGameSeq, stepCount, stepEpoch, landX, landY;

    private int seenSuspects;

    private boolean excusedValid;
    private int excusedCount;
    private int seenEpoch, seenOpen = -1;
    private long seenAt, unknownMs, lastTick = -1;
    private boolean locked;
    private int phase, lockCount, lockEpoch;
    private long phaseAt;
    private static final int QUIET = 0, PROBING = 1, CONFIRM = 2;


    void forget() {
        foreign = true;
        excuseCheck = false;
    }






    int watch(long now, D2MoveLedger.Answer a) {
        return suspect(now, a) ? W_LOCKED : W_NONE;
    }







    private boolean suspect(long now, D2MoveLedger.Answer a) {
        if (a.suspects == seenSuspects) return false;
        seenSuspects = a.suspects;
        if (locked) return false;
        lock(now, a);
        return true;
    }


    int landX() {
        return landX;
    }

    int landY() {
        return landY;
    }


    void newTarget() {
        unknownMs = 0;
        lastTick = -1;
    }


    boolean locked() {
        return locked;
    }





    boolean known(long now, D2MoveLedger.Answer a, int open) {
        suspect(now, a);
        return !locked && open == 0;
    }


    int placeX(D2MoveLedger.Answer a) {
        return excusedValid && a.count == excusedCount ? landX : a.x;
    }

    int placeY(D2MoveLedger.Answer a) {
        return excusedValid && a.count == excusedCount ? landY : a.y;
    }


    void excused(D2MoveLedger.Answer a) {
        stepOpen = false;
        foreign = true;
        excusedValid = true;
        excusedCount = a.count;
        seenSuspects = a.suspects;
        unknownMs = 0;
    }


    int excuseCount() {
        return stepCount;
    }

    private static boolean near(int x, int y, int px, int py) {
        return Math.abs(x - px) <= NEAR && Math.abs(y - py) <= NEAR;
    }


    long waiting(long now) {
        return seenOpen > 0 ? now - seenAt : 0;
    }











    int drive(long now, int targetX, int targetY, D2MoveLedger.Answer a, int open, boolean game, int gameSeq, int gx, int gy) {
        if (locked || suspect(now, a)) return WAIT;
        if (a.restarts != seenRestarts) {
            seenRestarts = a.restarts;
            firstStep = true;
        }
        if (!epochKnown) {
            epochKnown = true;
            seenEpoch = a.epoch;
        }
        else if (a.epoch != seenEpoch) {
            seenEpoch = a.epoch;
            ambiguous = true;
        }
        if (open != seenOpen) {
            seenOpen = open;
            seenAt = now;
        }

        if (excuseCheck && stepOpen && open == 1 && a.count == stepCount && a.epoch == stepEpoch && game && gameSeq != stepGameSeq
            && near(gx, gy, landX, landY)) {
            excuseCheck = false;
            return EXCUSE;
        }
        if (open > 0) {
            if (lastTick >= 0) unknownMs += now - lastTick;
            lastTick = now;
            if (now - seenAt >= LOST_MS) {
                lock(now, a);
                return LOST;
            }
            return unknownMs > ARRIVE_MAX_MS ? GIVE_UP : WAIT;
        }
        lastTick = now;
        if (ambiguous || a.count == 0) {
            ambiguous = false;
            foreign = true;
            stepOpen = false;
            out[0] = out[1] = 0;
            return ASK;
        }
        if (excusedValid && a.count != excusedCount) excusedValid = false;
        int px = placeX(a), py = placeY(a);
        if (stepOpen) {
            stepOpen = false;
            excuseCheck = false;
            if (!foreign) {
                int mx = px - fromX, my = py - fromY;
                if (mx == 0 && my == 0) return UNREACHABLE;
                if (Math.abs(stepX) > 8) gainX = learnGain(gainX, mx, stepX);
                if (Math.abs(stepY) > 8) gainY = learnGain(gainY, my, stepY);
            }
        }
        foreign = false;
        int ex = targetX - px, ey = targetY - py;
        if (Math.abs(ex) <= NEAR && Math.abs(ey) <= NEAR) return AT;
        int dx = Math.abs(ex) <= EXACT ? ex : Math.round(ex / gainX);
        int dy = Math.abs(ey) <= EXACT ? ey : Math.round(ey / gainY);
        out[0] = Math.max(-MAX_STEP, Math.min(MAX_STEP, dx));
        out[1] = Math.max(-MAX_STEP, Math.min(MAX_STEP, dy));
        fromX = px;
        fromY = py;
        stepX = out[0];
        stepY = out[1];
        stepOpen = true;
        if (firstStep) {
            firstStep = false;
            landX = px + Math.round(out[0] * gainX);
            landY = py + Math.round(out[1] * gainY);
            stepGameSeq = gameSeq;
            stepCount = a.count;
            stepEpoch = a.epoch;

            excuseCheck = game && near(gx, gy, px, py)
                && (Math.abs(landX - px) > 2 * NEAR || Math.abs(landY - py) > 2 * NEAR);
        }
        return STEP;
    }

    private void lock(long now, D2MoveLedger.Answer a) {
        locked = true;
        excusedValid = false;
        phase = QUIET;
        phaseAt = now;
        lockCount = a.count;
        lockEpoch = a.epoch;
        stepOpen = false;
        excuseCheck = false;
        foreign = true;
    }






    int recover(long now, D2MoveLedger.Answer a, int open) {
        if (!locked) return R_NONE;
        boolean news = a.count != lockCount, extra = a.epoch != lockEpoch;
        lockCount = a.count;
        lockEpoch = a.epoch;
        if (phase == QUIET) {
            if (news || extra) phaseAt = now;
            else if (now - phaseAt >= QUIET_MS) {
                phase = PROBING;
                phaseAt = now;
                return R_PROBE;
            }
        }
        else if (phase == PROBING) {
            if (extra || now - phaseAt >= LOST_MS) {
                phase = QUIET;
                phaseAt = now;
            }
            else if (open == 0) {
                phase = CONFIRM;
                phaseAt = now;
            }
        }
        else if (news || extra || open > 0) {
            phase = QUIET;
            phaseAt = now;
        }
        else if (now - phaseAt >= CONFIRM_MS) {
            locked = false;
            epochKnown = false;
            ambiguous = false;
            seenOpen = -1;
            newTarget();
            return R_FREE;
        }
        return R_NONE;
    }

    static float learnGain(float current, int moved, int sent) {
        float gain = moved / (float) sent;
        return gain >= 0.25f && gain <= 4f ? gain : current;
    }







    static final class Arrival {
        static final int WAIT = 0, DONE = 1, GO_ON = 2, ABORT = 3;
        private final long limitMs;
        private long start = -1, last, knownMs;

        Arrival(long limitMs) {
            this.limitMs = limitMs;
        }





        int tick(long now, boolean done, boolean resting, boolean known, boolean blocked) {
            if (start < 0) start = last = now;
            if (blocked) return ABORT;
            if (done && known) return DONE;
            if (known) knownMs += now - last;
            last = now;
            if (resting) return WAIT;
            if (knownMs > limitMs) return GO_ON;
            if (now - start > ARRIVE_MAX_MS) return ABORT;
            return WAIT;
        }
    }





    static final class Search {
        static final int WAIT = 0, HIT = 1, NEXT = 2, MISS = 3, ABORT = 4;
        private final int spots;
        private int index;
        private Arrival spot = new Arrival(SPOT_MS);

        Search(int spots) {
            this.spots = spots;
        }


        int index() {
            return index;
        }

        int tick(long now, boolean hit, boolean known, boolean blocked) {
            int c = spot.tick(now, hit, false, known, blocked);
            if (c == Arrival.WAIT) return WAIT;
            if (c == Arrival.DONE) return HIT;
            if (c == Arrival.ABORT) return ABORT;
            if (++index >= spots) return MISS;
            spot = new Arrival(SPOT_MS);
            return NEXT;
        }
    }
}
