package com.winlator.d2;










public final class D2StartupControls {

    public static final long NO_MOD_MS = 30_000;

    private D2StartupControls() {}





    public static boolean reveal(boolean modExpected, boolean modReports, boolean graphicsDone, long sinceStartMs) {
        if (modReports) return false;
        if (!modExpected) return graphicsDone || sinceStartMs >= NO_MOD_MS;
        return sinceStartMs >= NO_MOD_MS;
    }


    public static final int OUT_OF_GAME = 0, UNKNOWN = 1, KEEP = 2;








    public static int withoutState(boolean modReports, boolean focused) {
        if (modReports) return OUT_OF_GAME;
        return focused ? UNKNOWN : KEEP;
    }






    public static boolean entryWaits(int without, boolean focused) {
        return without == OUT_OF_GAME && focused;
    }


    public static final long SETTLE_MS = 700;








    public static final class Entry {
        private boolean waiting;
        private long since = -1;

        private boolean drawnBefore, drawnLast;

        private long undrawnSince = -1;
        private boolean signalIgnored, gaveUp;

        private boolean retried;


        public void outside() {
            if (!waiting) drawnBefore = drawnLast;
            waiting = true;
            since = -1;
            undrawnSince = -1;
        }


        public boolean settled(long now) {
            return settled(now, false, false);
        }








        public boolean settled(long now, boolean signal, boolean drawn) {
            if (signal && !drawn && drawnLast) newGame();
            if (signal && !signalIgnored) {
                if (!drawn) {
                    retried = false;
                    if (undrawnSince < 0) undrawnSince = now;
                    if (now - undrawnSince <= SIGNAL_GRACE_MS) {
                        waiting = true;
                        drawnBefore = false;
                        since = -1;
                        return false;
                    }
                    signalIgnored = gaveUp = true;
                }
                else {
                    undrawnSince = -1;
                    if (waiting && !drawnBefore && !retried) {
                        waiting = false;
                        drawnLast = true;
                        return true;
                    }
                }
            }
            drawnLast = signal && drawn;
            if (!waiting) return true;
            if (since < 0) since = now;
            if (now - since < SETTLE_MS) return false;
            waiting = false;
            return true;
        }


        public boolean gaveUp() {
            boolean g = gaveUp;
            gaveUp = false;
            return g;
        }





        public void retrySignal() {
            signalIgnored = false;
            undrawnSince = -1;
            retried = true;
        }


        public void newGame() {
            signalIgnored = retried = false;
            undrawnSince = -1;
            drawnBefore = drawnLast = false;
        }
    }


    public static final long SIGNAL_GRACE_MS = 15_000;






    public static final class Clock {
        private final long startedAt;
        private long awayAt = -1, awayTotal;

        public Clock(long now) {
            startedAt = now;
        }


        public void focus(boolean focused, long now) {
            if (!focused) {
                if (awayAt < 0) awayAt = now;
            }
            else if (awayAt >= 0) {
                awayTotal += now - awayAt;
                awayAt = -1;
            }
        }

        public boolean focused() {
            return awayAt < 0;
        }


        public long sinceStart(long now) {
            return (awayAt >= 0 ? awayAt : now) - startedAt - awayTotal;
        }
    }
}
