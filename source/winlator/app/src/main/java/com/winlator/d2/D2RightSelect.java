package com.winlator.d2;

final class D2RightSelect {
    static final long KEY_SETTLE_MS = 500;
    static final long MIN_WAIT_MS = 120;

    private long rightKeyAt;
    private boolean rightKey;

    void rightKey(long now) {
        rightKeyAt = now;
        rightKey = true;
    }

    private boolean recentKey(long at) {
        return rightKey && at - rightKeyAt < KEY_SETTLE_MS;
    }

    boolean immediate(long now, boolean stateShowsSkill) {
        return stateShowsSkill && !recentKey(now);
    }

    boolean taken(long now, long selectedAt, boolean stateShowsSkill) {
        if (!stateShowsSkill) return false;
        return !recentKey(selectedAt) || now - selectedAt >= MIN_WAIT_MS;
    }

    boolean reselect(long now, long selectedAt, boolean stateShowsSkill, boolean resent) {
        return !resent && !stateShowsSkill && recentKey(selectedAt) && now - selectedAt >= MIN_WAIT_MS;
    }
}
