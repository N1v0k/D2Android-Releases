package com.winlator.d2;

import android.content.Context;

import java.io.File;
import java.io.IOException;







final class D2ResumeArea {
    private D2ResumeArea() {}





    static void moveKeys(Context context, java.util.Map<String, String> folders) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("d2resume", Context.MODE_PRIVATE);
        java.util.Map<String, ?> all = prefs.getAll();
        java.util.Map<String, String> moved = D2Umzug.rekeyed(all.keySet(), folders, "resume|");
        if (moved.isEmpty()) return;
        android.content.SharedPreferences.Editor edit = prefs.edit();
        for (java.util.Map.Entry<String, String> m : moved.entrySet()) {
            Object value = all.get(m.getKey());
            if (value instanceof String) edit.putString(m.getValue(), (String) value);
            edit.remove(m.getKey());
        }
        edit.commit();
        D2Trace.log("move into the app: " + moved.size() + " resume place(s) moved along");
    }


    static String key(File dir) {
        try {
            return dir.getCanonicalPath();
        }
        catch (IOException e) {
            return dir.getAbsolutePath();
        }
    }


    static String levelName(Context context, File gameDir, D2SaveHeader.Info character, int difficulty, D2LevelNames names) {
        if (names == null) return null;
        D2Resume.Entry e = entry(context, gameDir, character, difficulty);
        return e != null ? names.name(e.level) : null;
    }


    static boolean expected(Context context, File gameDir, String file, int difficulty) {
        if (gameDir == null || file == null) return false;
        D2SaveHeader.Info info = D2SaveHeader.read(new File(D2Mods.saveDir(context, gameDir), file + ".d2s"));
        return entry(context, gameDir, info, difficulty) != null;
    }

    private static D2Resume.Entry entry(Context context, File gameDir, D2SaveHeader.Info character, int difficulty) {
        if (gameDir == null || character == null || !D2Settings.resume(context)) return null;

        D2ModInfo.Mod mod = D2Mods.active(context);
        String folder = mod != null ? key(mod.saves()) : key(gameDir);
        File save = new File(D2Mods.saveDir(context, gameDir), character.file + ".d2s");
        android.content.SharedPreferences prefs = context.getSharedPreferences("d2resume", Context.MODE_PRIVATE);
        D2Resume resume = new D2Resume(new D2Resume.Store() {
            @Override public String get(String key) { return prefs.getString(key, null); }
            @Override public void put(String key, String value) { }
            @Override public void remove(String key) { }
        });
        return resume.stored(true, folder, character.name, difficulty, save.length(), save.lastModified(),
            (character.status & 4) != 0);
    }
}
