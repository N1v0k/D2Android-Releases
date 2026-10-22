package com.winlator.d2;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;

import com.winlator.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public final class GameLocator {
    private static final int MAX_DEPTH = 4;

    public static final String DEFAULT_GAME_FOLDER = "D2Android/Diablo II";

    public static final String KEY_GAME_DIR = "gameDir";
    private static final String KEY_ACCEPTED = "gameDirAccepted";
    private static final String CHECKSUMS_ASSET = "checksums-113d.txt";
    private static Map<String, String> reference;

    private GameLocator() {}

    static final String KEY_BACKED_UP_DIRS = "backedUpGameDirs";
    static final String KEY_FIRST_BACKUP_DONE = "firstStartBackupDone";
    private static final String KEY_ADOPTED = "gameDirAdopted";

    public static File gameDir(Context context) {
        adoptUsedFolder(context);
        String chosen = chosenPath(context);
        if (chosen == null) return null;
        File dir = new File(chosen);
        return dir.isDirectory() ? dir : null;
    }

    private static void adoptUsedFolder(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_ADOPTED, false)) return;
        if (!com.winlator.BuildConfig.D2_INTERNAL || prefs.getString(KEY_GAME_DIR, null) != null) {
            prefs.edit().putBoolean(KEY_ADOPTED, true).apply();
            return;
        }
        java.util.Set<String> used = new java.util.HashSet<>(prefs.getStringSet(KEY_BACKED_UP_DIRS, java.util.Collections.emptySet()));
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String defaultPath = new File(downloads, DEFAULT_GAME_FOLDER).getAbsolutePath();
        if (used.isEmpty() && prefs.getBoolean(KEY_FIRST_BACKUP_DONE, false)) used.add(defaultPath);
        if (used.isEmpty()) {
            prefs.edit().putBoolean(KEY_ADOPTED, true).apply();
            return;
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        java.util.List<String> candidates = new java.util.ArrayList<>();
        candidates.add(new File(downloads, prefs.getString("gameFolder", DEFAULT_GAME_FOLDER)).getAbsolutePath());
        candidates.add(defaultPath);
        if (used.size() == 1) candidates.addAll(used);
        boolean missing = false;
        for (String path : candidates) {
            if (!used.contains(path)) continue;
            File dir = new File(path);
            if (dir.isDirectory() && isGameDir(dir)) {
                prefs.edit().putString(KEY_GAME_DIR, dir.getAbsolutePath()).putBoolean(KEY_ADOPTED, true).apply();
                return;
            }
            missing = true;
        }
        if (!missing) prefs.edit().putBoolean(KEY_ADOPTED, true).apply();
    }

    public static String chosenPath(Context context) {
        return context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE).getString(KEY_GAME_DIR, null);
    }

    public static GameVersion.Result check(Context context, File dir) {
        try {
            return new GameVersion(reference(context)).check(dir);
        }
        catch (IOException e) {
            return new GameVersion.Result(GameVersion.Status.NOT_A_GAME_DIR, dir, e.getMessage());
        }
    }

    public static GameVersion.Result choose(Context context, File dir) {
        GameVersion.Result result = check(context, dir);
        if (result.status == GameVersion.Status.OK || result.status == GameVersion.Status.MODIFIED) {
            context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_GAME_DIR, result.dir.getAbsolutePath()).apply();
            String before = D2Settings.gameLanguage(context);
            D2Strings.rememberGameLanguage(context, result.dir);
            if (!java.util.Objects.equals(before, D2Settings.gameLanguage(context))) D2Text.gameLanguageChanged(context);
        }
        return result;
    }

    public static boolean accepted(Context context, GameVersion.Result result) {
        return acceptKey(result).equals(context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE).getString(KEY_ACCEPTED, null));
    }

    public static void accept(Context context, GameVersion.Result result) {
        context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ACCEPTED, acceptKey(result)).apply();
    }

    private static String acceptKey(GameVersion.Result result) {
        return result.dir.getAbsolutePath() + "|" + result.detail;
    }

    static synchronized Map<String, String> reference(Context context) throws IOException {
        if (reference == null) {
            try (InputStream in = context.getAssets().open(CHECKSUMS_ASSET)) {
                reference = GameVersion.parseList(in);
            }
        }
        return reference;
    }

    public static Intent pickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
            DocumentsContract.buildDocumentUri(GameFolderUri.EXTERNAL_STORAGE, "primary:Download/D2Android"));
        return intent;
    }

    public static File pickedDir(Intent data) {
        Uri uri = data != null ? data.getData() : null;
        if (uri == null) return null;
        String path;
        try {
            path = GameFolderUri.toPath(uri.getAuthority(), DocumentsContract.getTreeDocumentId(uri),
                Environment.getExternalStorageDirectory().getAbsolutePath());
        }
        catch (IllegalArgumentException e) {
            return null;
        }
        return path != null ? new File(path) : null;
    }

    public static String message(Context context, GameVersion.Result result) {
        String detail = result.detail;
        switch (result.status) {
            case OK: return context.getString(R.string.folder_ok);
            case WRONG_VERSION:
                return detail != null ? context.getString(R.string.folder_wrong_version, detail) : context.getString(R.string.folder_unknown_version);
            case MODIFIED: return context.getString(R.string.folder_modified, detail);
            case MISSING:
                return detail != null && detail.toLowerCase(java.util.Locale.ROOT).contains("d2exp.mpq")
                    ? context.getString(R.string.folder_no_lod) : context.getString(R.string.folder_missing, detail);
            case NOT_WRITABLE: return context.getString(R.string.folder_not_writable);
            default: return context.getString(R.string.folder_not_game);
        }
    }

    public static File find() {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return find(downloads, MAX_DEPTH);
    }

    public static File resolve(String relativeFolder) {
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File dir = new File(downloads, relativeFolder);
        return dir.isDirectory() && isGameDir(dir) ? dir : null;
    }

    public static java.util.List<File> findAll() {
        java.util.ArrayList<File> result = new java.util.ArrayList<>();
        collect(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), MAX_DEPTH, result);
        return result;
    }

    private static void collect(File dir, int depth, java.util.List<File> out) {
        if (dir == null || !dir.isDirectory() || depth < 0) return;
        if (isGameDir(dir)) {
            out.add(dir);
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) return;
        java.util.Arrays.sort(files);
        for (File file : files) {
            if (file.isDirectory() && !file.getName().startsWith(".")) collect(file, depth - 1, out);
        }
    }

    public static boolean isGameDir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return false;
        boolean hasGame = false, hasData = false;
        for (File file : files) {
            String name = file.getName().toLowerCase();
            if (name.equals("game.exe")) hasGame = true;
            else if (name.equals("d2data.mpq")) hasData = true;
        }
        return hasGame && hasData;
    }

    private static File find(File dir, int depth) {
        if (dir == null || !dir.isDirectory() || depth < 0) return null;
        if (isGameDir(dir)) return dir;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (!file.isDirectory() || file.getName().startsWith(".")) continue;
            File result = find(file, depth - 1);
            if (result != null) return result;
        }
        return null;
    }
}
