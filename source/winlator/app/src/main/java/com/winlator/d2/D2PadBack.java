package com.winlator.d2;














public final class D2PadBack {
    private D2PadBack() {}


    public static final int NONE = 0, ESC = 1, END_TALK = 2, RING = 3, WHEEL = 4;










    public static int action(boolean editMode, boolean paused, boolean wheelOpen, boolean ringOpen, boolean npcAround,
                             long panels, boolean assigning) {
        if (editMode || paused) return NONE;
        if (wheelOpen) return WHEEL;
        if (ringOpen) return RING;
        if (npcAround) return END_TALK;
        if (panels != 0 || assigning) return ESC;
        return NONE;
    }





    public static final int MAX_ESC = 4;

    public static final long WINDOW_MS = 2000;





    public static final long SAME_STATE_MS = 350;




    public static final long MIN_GAP_MS = 200;





    public static boolean repeatSwallowed(long lastEscAt, long now, boolean endingTalk) {
        return endingTalk || (lastEscAt >= 0 && now - lastEscAt < SAME_STATE_MS);
    }






    public static final class EndTalk {
        private long started = -1, lastAt;
        private int sent;
        private long lastSignature;
        private boolean pending;


        public void start(long now, long signature) {
            started = now;
            lastAt = now;
            sent = 1;
            lastSignature = signature;
            pending = true;
        }


        public void sentNow(long now) {
            if (!pending) return;
            pending = false;
            lastAt = now;
        }

        public void cancel() {
            started = -1;
        }

        public boolean active() {
            return started >= 0;
        }






        public boolean escNow(boolean npcAround, long signature, long now) {
            if (started < 0) return false;
            if (!npcAround || now - started > WINDOW_MS || sent >= MAX_ESC) {
                started = -1;
                return false;
            }
            if (pending) return false;
            long since = now - lastAt;
            if (since < MIN_GAP_MS) return false;
            if (signature == lastSignature && since < SAME_STATE_MS) return false;
            sent++;
            lastAt = now;
            lastSignature = signature;
            pending = true;
            return true;
        }

        public int sent() {
            return sent;
        }
    }


    public static long signature(boolean talking, long panels, int[] menuIds) {
        long h = talking ? 1 : 0;
        h = h * 31 + panels;
        if (menuIds != null) for (int id : menuIds) h = h * 31 + id;
        return h;
    }
}
