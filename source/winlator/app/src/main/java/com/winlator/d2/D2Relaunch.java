package com.winlator.d2;

import java.io.File;

final class D2Relaunch {
    private D2Relaunch() {}

    static final int DESTROY = 0;
    static final int SAVE_AND_QUIT = 1;

    static final long SAVE_WAIT_MS = 4000;

    static final int CAN_SAVE = 0, NO_PLUGY = 1, NO_STATE = 2;

    static int saveChance(boolean stateNow, boolean plugyNow, Boolean plugyLast) {
        boolean plugy = stateNow ? plugyNow : plugyLast != null && plugyLast;
        if (plugy) return CAN_SAVE;
        return stateNow || plugyLast != null ? NO_PLUGY : NO_STATE;
    }

    static final long SORT_WAIT_MS = 2000;
    static final long SAVE_SETTLE_MS = 500;
    static final long RECENT_PAUSE_MS = 5000;

    static long baseline(long pausedAgoMs, long newestAtPause, long newestAtDestroy) {
        return pausedAgoMs >= 0 && pausedAgoMs <= RECENT_PAUSE_MS ? newestAtPause : newestAtDestroy;
    }

    static boolean saved(File newest, long baseline) {
        long changed = newest != null ? newest.lastModified() : 0;
        return saved(changed, baseline, changed > baseline && D2Saves.completeCharacter(newest));
    }

    static boolean saved(long newestNow, long baseline, boolean complete) {
        return newestNow > baseline && complete;
    }

    static int onDestroy(boolean changing, boolean started, boolean exiting, boolean testSession) {
        return changing && started && !exiting && !testSession ? SAVE_AND_QUIT : DESTROY;
    }
}
