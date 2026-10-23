package com.winlator.alsaserver;










public final class ALSAMute {
    private static volatile boolean muted;

    private static volatile boolean loading;
    private static long writtenMicros;

    private ALSAMute() {}

    public static boolean isMuted() {
        return muted || loading;
    }


    public static void muteForLoading(boolean on) {
        loading = on;
    }


    public static synchronized void mute() {
        if (!muted) writtenMicros = 0;
        muted = true;
    }


    public static synchronized long unmute() {
        boolean was = muted;
        muted = false;
        long ms = was ? writtenMicros / 1000 : 0;
        writtenMicros = 0;
        return ms;
    }


    public static synchronized void written(long frames, int sampleRate) {
        if (muted && frames > 0 && sampleRate > 0) writtenMicros += frames * 1_000_000L / sampleRate;
    }


    public static final class Track {
        private boolean mutedApplied;


        public float prepared(float own) {
            mutedApplied = isMuted();
            return mutedApplied ? 0f : own;
        }


        public float beforeWrite(float own) {
            boolean now = isMuted();
            if (now == mutedApplied) return -1f;
            mutedApplied = now;
            return now ? 0f : own;
        }
    }
}
