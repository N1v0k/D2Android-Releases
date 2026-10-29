package com.winlator.d2;

















final class D2LeftRestore {

    static final long PAUSE_MS = 150;

    static final long PAUSE_WAITING_MS = 900;

    static final long CHECK_MS = 1000;




    static final long CONFIRM_MS = 700;
    static final int TRIES = 3;

    static final long STALE_MS = 2000;
    static final int NONE = -1, GAVE_UP = -2;

    private int saved = NONE;
    private long dueAt = -1, sentAt = -1;
    private int tries;

    private int presses;
    private long idleSince = -1;


    void pressed(int keySkill, int leftNow) {
        presses++;
        if (saved < 0 && leftNow >= 0 && leftNow != keySkill) saved = leftNow;
        dueAt = -1;
        sentAt = -1;
        tries = 0;
    }


    int token() {
        return presses;
    }


    void ended(long now, boolean waiting) {
        if (saved >= 0 && sentAt < 0) dueAt = now + (waiting ? PAUSE_WAITING_MS : PAUSE_MS);
    }





    void ended(long now, boolean waiting, int token) {
        if (token == presses) ended(now, waiting);
    }


    int attackNow(long now) {
        if (saved < 0 || dueAt < 0) return NONE;
        dueAt = -1;
        sentAt = now;
        tries++;
        return saved;
    }


    int step(long now, int leftNow) {
        return step(now, leftNow, true);
    }






    int step(long now, int leftNow, boolean held) {
        if (saved < 0) return NONE;
        if (held || dueAt >= 0 || sentAt >= 0) idleSince = -1;
        else if (idleSince < 0) idleSince = now;
        else if (now - idleSince >= STALE_MS) {
            idleSince = -1;
            dueAt = now;
        }
        if (sentAt >= 0 && leftNow == saved && now - sentAt >= CONFIRM_MS) {
            clear();
            return NONE;
        }
        if (dueAt >= 0 && now >= dueAt) {
            dueAt = -1;
            sentAt = now;
            tries++;
            return saved;
        }
        if (sentAt >= 0 && now - sentAt >= CHECK_MS) {
            if (tries < TRIES) {
                sentAt = now;
                tries++;
                return saved;
            }
            clear();
            return GAVE_UP;
        }
        return NONE;
    }


    int saved() {
        return saved;
    }


    void clear() {
        idleSince = -1;
        saved = NONE;
        dueAt = sentAt = -1;
        tries = 0;
    }
}
