package com.winlator.d2;





public final class D2SkillListPick {
    public static final int WAIT = 0, TAKE = 1, NOTE = 2, BY_HAND = 3, ASK = 4, GIVE_UP = 5;

    static final long OPEN_MS = 1000;

    static final long ANSWER_MS = 1000;

    static final long STATE_MS = 200;

    private D2SkillListPick() {}





    public static int beforeAsk(boolean listOpen, long stateAgeMs, boolean typeSixSeen, long waited) {
        if (clickAllowed(listOpen, stateAgeMs) && typeSixSeen) return ASK;
        return waited < OPEN_MS ? WAIT : GIVE_UP;
    }





    public static int nextRequest(int last, int seen) {
        int next = (seen >= 1 ? seen : last) % 255 + 1;
        return next == last && seen >= 1 && seen != last ? next % 255 + 1 : next;
    }


    public static int decide(int[] answer, int want, long waited) {
        if (answer == null || answer[0] != want) return waited < ANSWER_MS ? WAIT : BY_HAND;
        int entry = answer[1];
        if (entry >= 0) return TAKE;
        return entry == D2Points.LIST_UNKNOWN ? BY_HAND : NOTE;
    }


    public static boolean listOpen(int[] answer) {
        return answer != null && answer[1] != D2Points.LIST_CLOSED && answer[1] != D2Points.LIST_UNKNOWN;
    }


    public static boolean clickAllowed(boolean listOpen, long stateAgeMs) {
        return listOpen && stateAgeMs >= 0 && stateAgeMs <= STATE_MS;
    }
}
