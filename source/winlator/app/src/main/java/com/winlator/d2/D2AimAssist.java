package com.winlator.d2;

final class D2AimAssist {
    final boolean on;
    final int rangePct, meleePct;
    final int coneDeg;
    final int blindDeg;
    final boolean idleDefaults;

    private D2AimAssist(boolean on, int meleePct, int rangePct, int coneDeg, int blindDeg, boolean idleDefaults) {
        this.on = on;
        this.meleePct = meleePct;
        this.rangePct = rangePct;
        this.coneDeg = coneDeg;
        this.blindDeg = blindDeg;
        this.idleDefaults = idleDefaults;
    }

    int reachPct(boolean melee) {
        return melee ? meleePct : rangePct;
    }

    static D2AimAssist now(boolean playerOn, int playerMeleePct, int playerRangePct, int playerConeDeg, boolean idleRuns,
                           int defaultMeleePct, int defaultRangePct, int defaultConeDeg) {
        return now(playerOn, playerMeleePct, playerRangePct, playerConeDeg, 0, idleRuns, defaultMeleePct, defaultRangePct, defaultConeDeg);
    }

    static D2AimAssist now(boolean playerOn, int playerMeleePct, int playerRangePct, int playerConeDeg, int playerBlindDeg,
                           boolean idleRuns, int defaultMeleePct, int defaultRangePct, int defaultConeDeg) {
        int blind = idleRuns ? 0 : playerBlindDeg;
        if (playerOn) return new D2AimAssist(true, Math.min(playerMeleePct, playerRangePct), playerRangePct, playerConeDeg, blind, false);
        if (idleRuns) return new D2AimAssist(true, Math.min(defaultMeleePct, defaultRangePct), defaultRangePct, defaultConeDeg, 0, true);
        return new D2AimAssist(false, Math.min(playerMeleePct, playerRangePct), playerRangePct, playerConeDeg, blind, false);
    }

    static boolean behind(double dx, double dy, double faceX, double faceY, int blindDeg) {
        double length = Math.hypot(dx, dy);
        if (blindDeg <= 0 || length < 1) return false;
        double cos = -(dx * faceX + dy * faceY) / length;
        return cos >= Math.cos(Math.toRadians(blindDeg / 2.0));
    }

    static boolean previewFor(String key) {
        return "aimRangeMelee".equals(key) || "aimRange".equals(key) || "aimCone".equals(key) || "aimBlind".equals(key);
    }
}
