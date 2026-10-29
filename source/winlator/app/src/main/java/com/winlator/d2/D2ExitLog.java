package com.winlator.d2;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;






public final class D2ExitLog {
    private static final String FILE = "D2Android/Protokoll.txt";
    private static final long MAX_SIZE = 200_000;
    private static final String KEY_LAST_EXIT = "lastSystemExit";

    private D2ExitLog() {}






    public static void gameEnded(int status, long backgroundSinceMs, String appReason, long sinceResumeMs) {
        append(D2ExitText.gameEnded(status, backgroundSinceMs, appReason, sinceResumeMs));
    }


    public static void quit(String how) {
        append("App beendet: " + how);
    }


    public static void note(String line) {
        append(line);
    }





    public static String systemExits(Context context) {
        if (Build.VERSION.SDK_INT < 30) return null;
        SharedPreferences prefs = context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE);
        long last = prefs.getLong(KEY_LAST_EXIT, 0);
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return null;
        List<ApplicationExitInfo> exits;
        try {
            exits = am.getHistoricalProcessExitReasons(context.getPackageName(), 0, 8);
        }
        catch (RuntimeException e) {
            return null;
        }
        String newest = null;
        long newestTime = last;
        for (int i = exits.size() - 1; i >= 0; i--) {
            ApplicationExitInfo info = exits.get(i);
            if (info.getTimestamp() <= last) continue;
            String details = (info.getDescription() != null ? " – " + info.getDescription() : "")
                + String.format(Locale.ROOT, " (Wichtigkeit %d, Speicher %d MB)", info.getImportance(), info.getPss() / 1024);

            boolean self = info.getReason() == ApplicationExitInfo.REASON_EXIT_SELF;
            append(D2ExitText.appExit(self, time(info.getTimestamp()), reason(info.getReason()), details));
            if (!self) D2Telemetry.systemExit(context, info.getReason(), info.getTimestamp());
            if (info.getTimestamp() >= newestTime) {
                newestTime = info.getTimestamp();
                if (!self) newest = reason(info.getReason());
            }
        }
        prefs.edit().putLong(KEY_LAST_EXIT, newestTime).apply();
        return newest;
    }

    private static String reason(int reason) {
        switch (reason) {
            case ApplicationExitInfo.REASON_LOW_MEMORY: return "Speichermangel";
            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE: return "zu viel Rechenzeit/Akku im Hintergrund";
            case ApplicationExitInfo.REASON_SIGNALED: return "Signal (vom System beendet)";
            case ApplicationExitInfo.REASON_USER_REQUESTED: return "vom Nutzer beendet";
            case ApplicationExitInfo.REASON_USER_STOPPED: return "Stopp erzwungen";
            case ApplicationExitInfo.REASON_CRASH: return "Absturz (Java)";
            case ApplicationExitInfo.REASON_CRASH_NATIVE: return "Absturz (nativ)";
            case ApplicationExitInfo.REASON_ANR: return "reagierte nicht";
            case ApplicationExitInfo.REASON_EXIT_SELF: return "hat sich selbst beendet";
            case ApplicationExitInfo.REASON_DEPENDENCY_DIED: return "abhängiger Prozess beendet";
            case ApplicationExitInfo.REASON_OTHER: return "anderer Grund";
            default: return "Grund " + reason;
        }
    }

    private static String time(long millis) {
        return new SimpleDateFormat("dd.MM. HH:mm:ss", Locale.ROOT).format(new Date(millis));
    }

    private static synchronized void append(String line) {
        try {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) return;
            boolean keep = !file.isFile() || file.length() < MAX_SIZE;
            try (FileWriter out = new FileWriter(file, keep)) {
                out.write(time(System.currentTimeMillis()) + "  " + line + "\n");
            }
        }
        catch (IOException ignored) {

        }
    }
}
