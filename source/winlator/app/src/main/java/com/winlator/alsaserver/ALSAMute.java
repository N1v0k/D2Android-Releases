package com.winlator.alsaserver;

/**
 * D2Android: silences the game's sound while the app is in the background. Until now only the guest's stop
 * (SIGSTOP, D2GuestComponent.onPause) made it quiet: a guest that runs on (the save's wait before the stop's save
 * of a rebuilt window, or a guest process the stop did not reach) kept playing behind the home screen. Now the game
 * screen mutes every client here on onPause and lifts it on onResume; each client applies it on its own thread with its
 * next write (setVolume, never pause: a paused track would block the guest's blocking writes). While muted it counts
 * how much sound the guest still wrote, for the trace: about the save's wait if the stop works, the whole time if not.
 * Pure Java for the desktop test (mod/test/jvm/AlsaMuteTest.java).
 */
public final class ALSAMute {
    private static volatile boolean muted;
    // D2Android: a second reason, the direct start's loading picture: our title music plays, not the game's menus
    private static volatile boolean loading;
    private static long writtenMicros;

    private ALSAMute() {}

    public static boolean isMuted() {
        return muted || loading;
    }

    /** D2Android: the loading picture of the direct start silences the game (its menus' music) while it shows. */
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
