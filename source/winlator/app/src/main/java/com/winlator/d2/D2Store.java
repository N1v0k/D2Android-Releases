package com.winlator.d2;

import android.content.Context;

import java.io.File;













public final class D2Store {
    static final String BASE = "d2", GAME = D2Umzug.GAME, SAVES = D2Umzug.SAVES, MODS = D2Umzug.MODS, BACKUPS = "sicherungen";

    private static volatile File base;

    private D2Store() {
    }


    public static void init(Context context) {
        if (base == null) base = new File(context.getFilesDir(), BASE);
    }


    public static void initForTest(File folder) {
        base = folder;
    }


    static File baseOrNull() {
        return base;
    }


    static File protocol() {
        File b = base;
        return b != null ? new File(b, "Protokoll.txt") : null;
    }

    public static File base() {
        File b = base;
        if (b == null) throw new IllegalStateException("D2Store.init was not called");
        return b;
    }

    public static File game() {
        return new File(base(), GAME);
    }

    public static File saves() {
        return new File(base(), SAVES);
    }

    public static File mods() {
        return new File(base(), MODS);
    }

    public static File backups() {
        File dir = new File(base(), BACKUPS);

        dir.mkdirs();
        return dir;
    }


    public static boolean ready() {
        return base != null && D2Umzug.migrated(base);
    }


    static boolean isGame(File dir) {
        File b = base;
        return b != null && dir != null && dir.getAbsoluteFile().equals(new File(b, GAME).getAbsoluteFile());
    }
}
