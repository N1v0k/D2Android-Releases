package com.winlator.d2;

final class D2HeldAttack {
    static final long INTERVAL_MS = 100;
    private long searchedAt;
    private boolean searched;

    void pressed(long now) {
        searchedAt = now;
        searched = true;
    }

    boolean due(long now, boolean inPlace, boolean clickPending) {
        if (!inPlace || clickPending) return false;
        if (searched && now - searchedAt < INTERVAL_MS) return false;
        searchedAt = now;
        searched = true;
        return true;
    }

    static boolean traceTargetChange(long goneId, boolean next) {
        return goneId > 0 || next;
    }

    static boolean walksWhenNobody(boolean aimOn, boolean joystickActive, boolean hasDirection, boolean directWalk,
                                   boolean canWalk, boolean canActOnUnits) {
        return aimOn && joystickActive && hasDirection && directWalk && canWalk && canActOnUnits;
    }

    static boolean goldWhileHeld(boolean attackAction, boolean attackWalking, long targetId, boolean attackButtonHeld) {
        return attackAction && attackWalking && targetId < 0 && attackButtonHeld;
    }

    static boolean pickupYields(boolean pickupForHeld, boolean targetInReach) {
        return pickupForHeld && targetInReach;
    }
}
