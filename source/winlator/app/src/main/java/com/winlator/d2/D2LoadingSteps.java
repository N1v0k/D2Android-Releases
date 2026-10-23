package com.winlator.d2;







final class D2LoadingSteps {
    static final int FRAMES = 10, LAST = FRAMES - 1;

    static final int DECISION_NONE = 0, DECISION_GOES = 1, DECISION_NOT = 2;





    static final long IN_GAME_LIMIT_MS = 25_000;

    private D2LoadingSteps() {}










    static boolean mayEnd(boolean resumeSettled, boolean uiReady, long inGameMs) {
        return resumeSettled && uiReady || inGameMs >= IN_GAME_LIMIT_MS;
    }









    static int target(boolean link, boolean modAlive, D2DirectStart.Report report, boolean inGame, boolean resumeGoes) {
        if (inGame) return resumeGoes ? LAST : LAST - 1;
        int t = 0;
        if (link) t = 1;
        if (modAlive) t = 2;
        if (report != null && report.state == D2DirectStart.RUNNING) {
            t = 3;
            if (report.step == 1) t = 4;
            else if (report.step == 2 || report.step == 3) t = 5;
            else if (report.step == 5) t = 7;
        }
        return t;
    }


    static int next(int shown, int target) {
        return shown < target ? shown + 1 : shown;
    }


    static boolean world(int shown) {
        return shown >= 5;
    }






    static boolean skippable(int shown, boolean resumeExpected, int decision) {
        return resumeExpected && world(shown) && decision == DECISION_NONE;
    }
}
