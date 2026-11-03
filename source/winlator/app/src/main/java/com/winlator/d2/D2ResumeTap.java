package com.winlator.d2;









final class D2ResumeTap {
    static final int DECISION_NONE = D2LoadingSteps.DECISION_NONE, DECISION_GOES = D2LoadingSteps.DECISION_GOES,
        DECISION_NOT = D2LoadingSteps.DECISION_NOT;

    static final int TAP_NOTHING = 0, TAP_SKIP = 1, TAP_TOWN = 2;

    static final int TOWN_WAIT = 0, TOWN_SENT = 1, TOWN_NOT = 2;

    private int decision = DECISION_NONE, town = TOWN_WAIT;
    private boolean skip, townAsked, arrived, skipped;


    synchronized void reset() {
        decision = DECISION_NONE;
        townAsked = arrived = skipped = false;
        town = TOWN_WAIT;
    }

    synchronized int decision() {
        return decision;
    }


    synchronized int tap() {
        if (decision == DECISION_NONE) {
            skip = true;
            return TAP_SKIP;
        }
        if (decision == DECISION_GOES && !arrived && !townAsked) {
            townAsked = true;
            return TAP_TOWN;
        }
        return TAP_NOTHING;
    }


    synchronized int decide(boolean goes) {
        skipped = goes && skip;
        decision = goes && !skip ? DECISION_GOES : DECISION_NOT;
        skip = false;
        return decision;
    }


    synchronized boolean skipped() {
        return skipped;
    }


    synchronized boolean arrive() {
        arrived = true;
        return townAsked;
    }

    synchronized boolean arrived() {
        return arrived;
    }

    synchronized boolean townAsked() {
        return townAsked;
    }


    synchronized void town(boolean sent) {
        town = sent ? TOWN_SENT : TOWN_NOT;
    }

    synchronized int town() {
        return town;
    }
}
