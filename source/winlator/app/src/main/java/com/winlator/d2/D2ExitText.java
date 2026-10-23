package com.winlator.d2;





final class D2ExitText {

    static final String BY_WINDOW_CLOSED = "Spielfenster geschlossen (onDestroy)";
    static final String BY_QUIT = "Beenden";
    static final String BY_NEW_RUN = "neuer Spielstart";
    static final String BY_UNSAID = "ohne Angabe";

    static final String WINDOW_STOPPED = "gestoppt (onStop)";
    static final String WINDOW_CLOSED = "geschlossen (onDestroy)";


    static final int TRIM_RUNNING_CRITICAL = 15, TRIM_UI_HIDDEN = 20, TRIM_BACKGROUND = 40;

    private D2ExitText() {}









    static String gameEnded(int status, long backgroundMs, String appReason, long sinceResumeMs) {
        return "Spiel beendet (Status " + status + " des Wine-Desktops), App " + where(backgroundMs)
            + (sinceResumeMs >= 0 ? ", " + sinceResumeMs / 1000 + " s nach dem letzten Fortsetzen" : "")
            + (appReason != null ? " (von der App beendet: " + appReason + ")" : " (nicht von der App)");
    }


    static final long RESUME_WORTH_NOTING_MS = 60_000;









    static String resumed(long stoppedMs, long sleepMs, int guests, java.util.List<String> missing) {
        String after = "nach " + duration(stoppedMs) + " im Hintergrund" + (sleepMs >= 0 ? " (davon Tiefschlaf ~" + duration(sleepMs) + ")" : "");
        if (!missing.isEmpty()) {
            return "Spiel fortgesetzt " + after + ": es fehlen Spielprozesse: " + String.join(", ", missing)
                + " (" + missing.size() + " von " + guests + ")";
        }
        return stoppedMs >= RESUME_WORTH_NOTING_MS ? "Spiel fortgesetzt " + after + ", alle " + guests + " Spielprozesse da" : null;
    }





    static String guest(String name, int pid, int waitStatus) {
        return name + " (pid " + pid + (waitStatus >= 0 ? ", Zombie, " + waitStatus(waitStatus) : "") + ")";
    }


    static String waitStatus(int status) {
        int signal = status & 0x7f;
        return signal != 0 ? "Signal " + signal : "Code " + ((status >> 8) & 0xff);
    }






    static String shortName(String commandLine) {
        String s = commandLine == null ? "" : commandLine.trim();
        int exe = s.toLowerCase(java.util.Locale.ROOT).indexOf(".exe");
        if (exe >= 0) s = s.substring(0, exe + 4);
        else if (s.indexOf(' ') > 0) s = s.substring(0, s.indexOf(' '));
        int cut = Math.max(s.lastIndexOf('\\'), s.lastIndexOf('/'));
        s = s.substring(cut + 1);
        return s.isEmpty() ? "?" : s;
    }


    private static String duration(long ms) {
        if (ms < 60_000) return Math.max(0, ms) / 1000 + " s";
        long minutes = ms / 60_000;
        return minutes < 60 ? minutes + " min" : minutes / 60 + " h " + minutes % 60 + " min";
    }





    static String window(String event, long backgroundMs, boolean finishing, boolean changingConfigurations, boolean exiting) {
        return "Spielfenster " + event + ", App " + where(backgroundMs) + ": finishing " + yesNo(finishing)
            + ", Neuaufbau " + yesNo(changingConfigurations) + ", Beenden läuft " + yesNo(exiting);
    }






    static boolean trimWorthNoting(int level) {
        return level == TRIM_RUNNING_CRITICAL || level >= TRIM_BACKGROUND;
    }


    static String trimMemory(int level, long backgroundMs) {
        return "Speicherdruck (onTrimMemory " + level + "), App " + where(backgroundMs);
    }





    static String appExit(boolean self, String when, String reason, String details) {
        return self ? "App hat sich selbst beendet " + when + details : "App vom System beendet " + when + ": " + reason + details;
    }


    static String taskRemoved(boolean gameRunning) {
        return "App aus den letzten Apps entfernt: App beendet sich samt Spiel (Spiel lief: " + yesNo(gameRunning) + ")";
    }

    private static String where(long backgroundMs) {
        return backgroundMs >= 0 ? "im Hintergrund seit " + backgroundMs / 1000 + " s" : "im Vordergrund";
    }

    private static String yesNo(boolean value) {
        return value ? "ja" : "nein";
    }
}
