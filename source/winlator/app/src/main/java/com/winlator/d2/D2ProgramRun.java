package com.winlator.d2;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;

import java.util.Locale;

public final class D2ProgramRun {
    public static final String EXTRA_DIR = "d2_run_dir";
    public static final String EXTRA_PROGRAM = "d2_run_program";
    public static final String EXTRA_ARGS = "d2_run_args";
    public static final String EXTRA_LOG = "d2_run_log";
    public static final String ADB_TOKEN = "/data/local/tmp/d2android-programrun.ok";
    public static final String DIR_ROOT = "D:\\D2Android\\Installer";
    private static final String[] ALLOWED = {"downloader_d2dv.exe", "downloader_d2xp.exe", "installer.exe", "lodpatch_113d.exe", "game.exe", "d2install.exe"};

    public final String dir, program, args;
    public final boolean log;

    private D2ProgramRun(String dir, String program, String args, boolean log) {
        this.dir = dir;
        this.program = program;
        this.args = args;
        this.log = log;
    }

    public static D2ProgramRun fromIntent(Context context, Intent intent) {
        if (intent == null) return null;
        if (!com.winlator.BuildConfig.D2_INTERNAL) return null;
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null;
        String program = intent.getStringExtra(EXTRA_PROGRAM);
        if (program == null) return null;
        if (!allowed(program)) {
            android.util.Log.w("D2Android", "program run: '" + program + "' is not on the allow list");
            return null;
        }
        String dir = intent.getStringExtra(EXTRA_DIR);
        if (!dirAllowed(dir)) {
            android.util.Log.w("D2Android", "program run: folder must lie below " + DIR_ROOT + ", refused");
            return null;
        }
        if (!D2SpieltestPort.tokenFresh("program run", ADB_TOKEN)) {
            android.util.Log.w("D2Android", "program run: adb token missing or stale, refused");
            return null;
        }
        android.util.Log.i("D2Android", "program run: " + program + " in " + dir);
        return new D2ProgramRun(dir, program, intent.getStringExtra(EXTRA_ARGS), intent.getBooleanExtra(EXTRA_LOG, true));
    }

    static boolean dirAllowed(String dir) {
        if (dir == null) return false;
        String d = dir.toUpperCase(Locale.ROOT);
        if (!(d.equals(DIR_ROOT.toUpperCase(Locale.ROOT)) || d.startsWith(DIR_ROOT.toUpperCase(Locale.ROOT) + "\\"))) return false;
        return !d.contains("..") && !d.contains("/") && d.indexOf(':', 2) < 0;
    }

    static boolean allowed(String program) {
        String name = program.toLowerCase(Locale.ROOT);
        if (name.contains("\\") || name.contains("/")) return false;
        for (String ok : ALLOWED) if (ok.equals(name)) return true;
        return false;
    }
}
