package com.winlator.d2;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;





















public final class D2SaveRepair {
    private static final String TAG = "D2Saves";

    public static final String ASIDE = ".unterbrochen-";
    private static final String TEMP = ".d2android-repair";
    private static final Pattern BACKUP = Pattern.compile("(.+)_(\\d{8}_\\d{6})");

    public enum Kind { STASH_RENAMED, SHARED_STASH_RENAMED, CHARACTER_RESTORED, CHARACTER_BROKEN, CHARACTER_HARDCORE }





    public static final class Repair {
        public final Kind kind;
        public final String name;
        public final String backup;
        public final String folder;

        Repair(Kind kind, String name, String backup) {
            this(kind, name, backup, null);
        }

        Repair(Kind kind, String name, String backup, String folder) {
            this.kind = kind;
            this.name = name;
            this.backup = backup;
            this.folder = folder;
        }

        @Override
        public String toString() {
            return kind + " " + name + (backup != null ? " from " + backup : "") + (folder != null ? " (" + folder + ")" : "");
        }
    }

    private static List<Repair> last = new ArrayList<>();

    private D2SaveRepair() {
    }


    public static synchronized List<Repair> takeLast() {
        List<Repair> r = last;
        last = new ArrayList<>();
        return r;
    }


    public static synchronized List<Repair> run(File saveDir) {
        List<Repair> done = new ArrayList<>();
        File[] files = saveDir != null ? saveDir.listFiles() : null;
        if (files == null) return done;
        Arrays.sort(files);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        for (File f : files) {
            String name = f.getName();
            if (f.isFile() && name.endsWith(TEMP)) finishTemp(saveDir, f, name.substring(0, name.length() - TEMP.length()));
        }
        files = saveDir.listFiles();
        if (files == null) return done;
        Arrays.sort(files);
        for (File f : files) {
            String lower = f.getName().toLowerCase(Locale.ROOT);
            if (!f.isFile() || !(lower.endsWith(".d2~") || lower.endsWith(".ss~"))) continue;
            String base = f.getName().substring(0, f.getName().length() - 4);
            boolean shared = lower.endsWith(".ss~");
            File target = find(saveDir, base + (shared ? ".sss" : ".d2x"));
            if (target != null) continue;
            File into = new File(saveDir, base + (shared ? ".sss" : ".d2x"));
            if (f.renameTo(into)) done.add(new Repair(shared ? Kind.SHARED_STASH_RENAMED : Kind.STASH_RENAMED, into.getName(), null));
            else Log.w(TAG, "cannot rename " + f.getName());
        }
        for (File f : files) {
            String name = f.getName();
            if (!f.isFile() || !name.toLowerCase(Locale.ROOT).endsWith(".d2s") || D2Saves.completeCharacter(f)) continue;
            String hero = name.substring(0, name.length() - 4);
            done.add(restore(saveDir, hero, stamp));
        }
        for (Repair r : done) Log.w(TAG, String.valueOf(r));
        last.addAll(done);
        return done;
    }






    private static void finishTemp(File dir, File temp, String target) {
        File existing = find(dir, target);
        boolean wasThere = false;
        String[] names = dir.list();
        if (names != null) for (String n : names) if (n.regionMatches(true, 0, target + ASIDE, 0, target.length() + ASIDE.length())) wasThere = true;
        boolean character = target.toLowerCase(Locale.ROOT).endsWith(".d2s");
        if (existing == null && (wasThere || (character && D2Saves.completeCharacter(temp)))) {
            if (temp.renameTo(new File(dir, target))) Log.w(TAG, "moved in " + target + " left by a repair cut off");
            else Log.w(TAG, "cannot move in " + temp.getName());
        }
        else if (!temp.delete()) Log.w(TAG, "cannot delete " + temp.getName());
    }


    private static Repair restore(File saveDir, String hero, String stamp) {
        File backupDir = find(saveDir, "backup");
        File[] folders = backupDir != null ? backupDir.listFiles() : null;
        if (folders == null) return new Repair(Kind.CHARACTER_BROKEN, hero, null);
        List<String> times = new ArrayList<>();
        String newestAll = "";
        for (File folder : folders) {
            Matcher m = BACKUP.matcher(folder.getName());
            if (!folder.isDirectory() || !m.matches()) continue;
            if (m.group(2).compareTo(newestAll) > 0) newestAll = m.group(2);
            if (m.group(1).equalsIgnoreCase(hero)) times.add(m.group(2) + "\n" + folder.getName());
        }
        Collections.sort(times, Collections.reverseOrder());
        for (String t : times) {
            String time = t.substring(0, t.indexOf('\n'));
            File folder = new File(backupDir, t.substring(t.indexOf('\n') + 1));
            File d2s = find(folder, hero + ".d2s");
            if (d2s == null || !D2Saves.completeCharacter(d2s)) continue;
            if (!Boolean.FALSE.equals(D2SaveHeader.hardcore(folder, hero))) {

                return new Repair(Kind.CHARACTER_HARDCORE, hero, time, folder.getAbsolutePath());
            }

            boolean shared = time.equals(newestAll);
            try {
                File[] parts = folder.listFiles();
                if (parts == null) continue;
                List<File> sources = new ArrayList<>();
                for (File p : parts) {
                    String lower = p.getName().toLowerCase(Locale.ROOT);
                    boolean own = lower.startsWith(hero.toLowerCase(Locale.ROOT) + ".") && D2Saves.isSaveFile(p.getName());
                    if (p.isFile() && (own || (shared && lower.endsWith(".sss"))) && !p.getName().equalsIgnoreCase(d2s.getName())) sources.add(p);
                }
                sources.add(d2s);
                for (File source : sources) replace(saveDir, source, stamp);
                return new Repair(Kind.CHARACTER_RESTORED, hero, time);
            }
            catch (IOException e) {
                Log.w(TAG, "restoring " + hero + " from " + folder.getName() + " failed", e);
                return new Repair(Kind.CHARACTER_BROKEN, hero, null);
            }
        }
        return new Repair(Kind.CHARACTER_BROKEN, hero, null);
    }


    private static void replace(File dir, File source, String stamp) throws IOException {
        File existing = find(dir, source.getName());
        File target = existing != null ? existing : new File(dir, source.getName());
        File temp = new File(dir, target.getName() + TEMP);
        copy(source, temp);
        if (existing != null) copy(existing, new File(dir, existing.getName() + ASIDE + stamp));
        if (!temp.renameTo(target)) {

            if (!target.delete() || !temp.renameTo(target)) throw new IOException("cannot move in " + target.getName());
        }
    }

    private static void copy(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[16384];
            for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
            out.flush();
            ((FileOutputStream) out).getFD().sync();
        }
        to.setLastModified(from.lastModified());
    }


    private static File find(File dir, String name) {
        File exact = new File(dir, name);
        if (exact.exists()) return exact;
        File[] entries = dir.listFiles();
        if (entries != null) for (File e : entries) if (e.getName().equalsIgnoreCase(name)) return e;
        return null;
    }
}
