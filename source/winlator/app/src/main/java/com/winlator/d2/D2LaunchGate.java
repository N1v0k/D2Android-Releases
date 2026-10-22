package com.winlator.d2;

public final class D2LaunchGate {
    public enum Refused {
        ALREADY_STARTING,
        GAME_HANDED_OVER,
        GAME_RUNNING,
        IMPORT_RUNNING,
        INSTALL_SETUP,
        INSTALL_RUNNING
    }

    static long graceMs = 15_000;
    private static boolean preparing, startedOnce;
    private static boolean installSetup;
    private static boolean installRun;
    private static long startedAt;

    private D2LaunchGate() {}

    public static synchronized boolean begin() {
        if (preparing || installSetup || installRun || justStarted()) return false;
        preparing = true;
        return true;
    }

    private static boolean justStarted() {
        return startedOnce && System.nanoTime() - startedAt < graceMs * 1_000_000L;
    }

    public static synchronized void started() {
        preparing = false;
        startedOnce = true;
        startedAt = System.nanoTime();
    }

    public static synchronized void failed() {
        preparing = false;
    }

    public static synchronized void screenUp() {
        startedOnce = false;
    }

    public static synchronized Refused beginGame(boolean gameRunning) {
        if (gameRunning) return Refused.GAME_RUNNING;
        if (installSetup) return Refused.INSTALL_SETUP;
        if (installRun) return Refused.INSTALL_RUNNING;
        if (preparing) return Refused.ALREADY_STARTING;
        if (!begin()) return Refused.GAME_HANDED_OVER;
        if (!D2Saves.beginLaunch()) {
            failed();
            return Refused.IMPORT_RUNNING;
        }
        return null;
    }

    public static synchronized void gameStarted() {
        started();
        D2Saves.launched();
    }

    public static synchronized void gameFailed() {
        failed();
        D2Saves.launchFailed();
    }

    public static synchronized boolean beginInstallSetup() {
        if (preparing || justStarted()) return false;
        installSetup = true;
        return true;
    }

    public static synchronized void endInstallSetup() {
        installSetup = false;
    }

    public static final class Busy extends java.io.IOException {
        public Busy() {
            super("a game start holds the launch gate");
        }
    }

    public interface SetUp {
        void run() throws java.io.IOException;
    }

    public static void installSetup(boolean needed, SetUp setUp) throws java.io.IOException {
        if (!needed) return;
        if (!beginInstallSetup()) throw new Busy();
        try {
            setUp.run();
        }
        finally {
            endInstallSetup();
        }
    }

    public static synchronized Refused beginProgram() {
        if (installSetup) return Refused.INSTALL_SETUP;
        if (installRun) return Refused.INSTALL_RUNNING;
        return begin() ? null : Refused.ALREADY_STARTING;
    }

    public static synchronized boolean beginInstallRun() {
        if (preparing || justStarted() || installRun) return false;
        installRun = true;
        return true;
    }

    public static synchronized void endInstallRun() {
        installRun = false;
    }
}
