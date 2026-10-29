package com.winlator.d2;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;









public final class D2Mods {
    public static final String FOLDER = "D2Android/Mods";
    static final String KEY_ACTIVE = "modActive";
    static final String JSON = "mod.json", FILES = "files", SAVES = "save", CACHE = "cache";

    static final String NEW_PREFIX = ".neu-";

    static final long SPARE = 64L * 1024 * 1024;

    private D2Mods() {}


    public static File root() {
        return new File(D2Runtime.downloadsDir(), FOLDER);
    }




    public static List<D2ModInfo.Mod> list() {
        List<D2ModInfo.Mod> out = new ArrayList<>();
        File[] dirs = root().listFiles();
        if (dirs == null) return out;
        for (File d : dirs) {
            if (!d.isDirectory() || d.getName().startsWith(".")) continue;
            D2ModInfo.Mod m = read(d);
            if (m != null) out.add(m);
        }
        out.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return out;
    }


    static D2ModInfo.Mod read(File dir) {
        try {
            D2ModInfo.Mod m = D2ModInfo.Mod.fromJson(new String(Files.readAllBytes(new File(dir, JSON).toPath()), StandardCharsets.UTF_8));
            if (!m.id.equals(dir.getName()) || !new File(dir, FILES).isDirectory()) return null;
            m.dir = dir;
            return m;
        }
        catch (IOException | JSONException | RuntimeException e) {
            return null;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE);
    }


    public static D2ModInfo.Mod active(Context context) {
        String id = prefs(context).getString(KEY_ACTIVE, null);
        if (id == null || !D2ModInfo.validId(id)) return null;
        return read(new File(root(), id));
    }


    public static void setActive(Context context, String id) {
        if (id == null) prefs(context).edit().remove(KEY_ACTIVE).apply();
        else prefs(context).edit().putString(KEY_ACTIVE, id).apply();
    }


    public static File saveDir(Context context, File gameDir) {
        D2ModInfo.Mod m = active(context);
        return m != null ? m.saves() : D2Saves.saveDir(gameDir);
    }




    public interface Progress {

        void at(int stage, long done, long total);
    }





    public static D2ModInfo.Mod importArchive(InputStream source, String fileName, Progress progress) throws IOException {
        File root = root();
        if (!root.isDirectory() && !root.mkdirs()) throw new IOException("cannot create " + root);
        cleanLeftovers(root);
        Set<String> taken = new HashSet<>();
        String[] names = root.list();
        if (names != null) for (String n : names) taken.add(n);
        String id = D2ModInfo.newId(fileName, taken);
        File work = new File(root, NEW_PREFIX + id);
        File archive = new File(root, NEW_PREFIX + id + ".archiv");
        try {
            String sha;
            try (InputStream in = source) {
                sha = copyHashing(in, archive, progress);
            }
            progress.at(1, 0, 0);
            D2ModArchive.Plan plan = D2ModArchive.check(D2ModArchive.list(archive));
            long free = root.getUsableSpace();
            if (free < plan.total + SPARE) throw new D2ModArchive.Failure(D2ModArchive.Reason.NO_SPACE,
                String.valueOf((plan.total + SPARE - free) / (1024 * 1024) + 1));
            File files = new File(work, FILES);
            D2ModArchive.unpack(archive, plan, files, (written) -> progress.at(2, written, plan.total));
            boolean archives = false;
            for (String below : plan.files.values()) if (!below.contains("/") && below.toLowerCase(Locale.ROOT).endsWith(".mpq")) archives = true;
            D2ModInfo.Mod mod = new D2ModInfo.Mod(id, D2ModInfo.displayName(fileName), fileName == null ? "" : fileName, sha, "data", System.currentTimeMillis(),
                plan.total, plan.files.size(), plan.data, plan.txt, plan.inventory, archives);
            if (!new File(work, SAVES).mkdirs()) throw new IOException("cannot create saves of " + id);
            Files.write(new File(work, JSON).toPath(), mod.toJson().getBytes(StandardCharsets.UTF_8));
            File done = new File(root, id);
            if (done.exists() || !work.renameTo(done)) throw new IOException("cannot move " + work + " to " + done);
            mod.dir = done;
            return mod;
        }
        finally {
            archive.delete();
            if (work.exists()) D2ModOverlay.remove(work);
        }
    }


    static void cleanLeftovers(File root) {
        File[] all = root.listFiles();
        if (all == null) return;
        for (File f : all) {
            if (!f.getName().startsWith(NEW_PREFIX)) continue;
            try {
                D2ModOverlay.remove(f);
            }
            catch (IOException e) {
                D2Trace.log("mods: leftover not removed: " + f.getName());
            }
        }
    }

    private static String copyHashing(InputStream in, File to, Progress progress) throws IOException {
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("SHA-256");
        }
        catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        byte[] buffer = new byte[256 * 1024];
        long done = 0;
        try (OutputStream out = new FileOutputStream(to)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                sha.update(buffer, 0, n);
                done += n;
                if (done > D2ModArchive.MAX_TOTAL) throw new D2ModArchive.Failure(D2ModArchive.Reason.TOO_LARGE, null);
                progress.at(0, done, 0);
            }
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : sha.digest()) hex.append(String.format(Locale.ROOT, "%02x", b));
        return hex.toString();
    }





    public static void remove(Context context, D2ModInfo.Mod mod) throws IOException {
        D2ModInfo.Mod active = active(context);
        if (active != null && active.id.equals(mod.id)) setActive(context, null);
        File aside = new File(root(), NEW_PREFIX + mod.id + "-weg");
        if (!mod.dir.renameTo(aside)) throw new IOException("cannot move " + mod.dir);
        D2ModOverlay.remove(aside);
    }
}
