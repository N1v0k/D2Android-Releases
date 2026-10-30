package com.winlator.d2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;












final class D2Umzug {
    static final String MARKER = ".d2android-fertig";

    static final String ORIGIN = ".d2android-quelle";
    static final String STAGING = ".import-", ASIDE = ".alt-", ADOPT = ".uebernahme-";

    static final String GAME = "game", SAVES = "saves", MODS = "mods";

    static final String CHECKING = ":pruefen";

    static final long RESERVE = 300L << 20;
    private static final int BUFFER = 256 << 10;
    private static final long PROGRESS_STEP = 8L << 20;


    static int killAfterFiles = -1;

    static long freeForTest = -1;

    interface Progress {

        void at(String what, long done, long total);
    }


    static final class NoSpace extends IOException {
        final long needed, free;

        NoSpace(long needed, long free) {
            super("not enough room: " + needed + " bytes needed, " + free + " free");
            this.needed = needed;
            this.free = free;
        }
    }


    static final class Mismatch extends IOException {
        Mismatch(String what) {
            super(what);
        }
    }

    private D2Umzug() {
    }






    static java.util.Map<String, String> rekeyed(Iterable<String> keys, java.util.Map<String, String> folders, String prefix) {
        java.util.Map<String, String> out = new java.util.LinkedHashMap<>();
        for (String k : keys) {
            if (!k.startsWith(prefix)) continue;
            for (java.util.Map.Entry<String, String> f : folders.entrySet()) {
                String head = prefix + f.getKey() + "|";
                if (k.startsWith(head)) {
                    out.put(k, prefix + f.getValue() + "|" + k.substring(head.length()));
                    break;
                }
            }
        }
        return out;
    }

    static boolean ready(File dir) {
        return dir != null && new File(dir, MARKER).isFile();
    }






    static void migrate(File gameSource, File modsSource, File base, Progress progress) throws IOException {

        if (!ready(new File(base, SAVES))) copyIn(saveFolder(gameSource), base, SAVES, null, progress);
        finishAdoption(base);
        if (!ready(new File(base, GAME))) {
            if (gameSource == null || !gameSource.isDirectory()) throw new IOException("no game folder to move in");
            if (inside(gameSource, base)) adopt(gameSource, base);
            else copyIn(gameSource, base, GAME, new String[]{"save"}, progress);
        }
        modsIn(modsSource, base, progress);
    }






    static void importGame(File source, File base, Progress progress) throws IOException {
        if (!base.isDirectory() && !base.mkdirs()) throw new IOException("cannot create " + base);
        finishAdoption(base);
        recover(base, GAME);
        File saves = new File(base, SAVES);

        if (!ready(saves) || empty(saves)) {
            File from = saveFolder(source);
            if (from != null || !ready(saves)) copyIn(from, base, SAVES, null, progress);
        }
        if (inside(source, base)) {
            File target = new File(base, GAME);
            File aside = null;
            if (target.exists()) {
                aside = new File(base, ASIDE + GAME + "-" + System.currentTimeMillis());
                if (!target.renameTo(aside)) throw new IOException("cannot set " + target + " aside");
            }
            adopt(source, base);
            if (aside != null) delete(aside);
        }
        else copyIn(source, base, GAME, new String[]{"save"}, progress);
        modsIn(null, base, progress);
    }






    static long deleteGameFiles(File gameDir) throws IOException {
        long freed = gameFilesSize(gameDir);
        File[] list = gameDir.listFiles();
        if (list == null) throw new IOException("cannot list " + gameDir);
        for (File f : list) if (!f.getName().equalsIgnoreCase("save")) delete(f);
        String[] left = gameDir.list();
        if (left != null && left.length == 0)
            gameDir.delete();
        return freed;
    }


    static long gameFilesSize(File gameDir) throws IOException {
        return gameDir != null && gameDir.isDirectory() ? size(gameDir, new String[]{"save"}) : 0;
    }





    static void mirrorOut(File saves, File mirrorBase) throws IOException {
        if (!ready(saves)) throw new IOException("the saves are not in the app yet");
        copyIn(saves, mirrorBase, SAVES, null, (w, d, t) -> {});
    }






    static void mirrorIn(File mirrorBase, File base) throws IOException {
        File mirror = new File(mirrorBase, SAVES);
        if (!ready(mirror)) throw new IOException("no mirror of the saves (d2_saves_spiegel aus first)");
        copyIn(mirror, base, SAVES, null, (w, d, t) -> {});
        delete(mirror);
    }





    static String origin(File base) {
        File f = new File(new File(base, GAME), ORIGIN);
        if (!ready(new File(base, GAME)) || !f.isFile()) return null;
        try {
            return new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8").trim();
        }
        catch (IOException e) {
            return null;
        }
    }





    static File zipTree(File dir, File zip) throws IOException {
        java.util.List<File> files = new java.util.ArrayList<>();
        collect(dir, files);
        if (files.isEmpty()) return null;
        File parent = zip.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("cannot create " + parent);
        File part = new File(zip.getPath() + ".part");
        byte[] buf = new byte[BUFFER];
        try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(new FileOutputStream(part))) {
            for (File f : files) {
                out.putNextEntry(new java.util.zip.ZipEntry(dir.toPath().relativize(f.toPath()).toString().replace(File.separatorChar, '/')));
                try (InputStream in = new FileInputStream(f)) {
                    for (int r; (r = in.read(buf)) > 0; ) out.write(buf, 0, r);
                }
                out.closeEntry();
            }
        }
        if (!part.renameTo(zip)) throw new IOException("cannot rename " + part);
        return zip;
    }

    private static void collect(File dir, java.util.List<File> out) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) collect(f, out);
            else if (!f.getName().equals(MARKER)) out.add(f);
        }
    }


    static boolean empty(File dir) {
        String[] names = dir.list();
        if (names != null) for (String n : names) if (!n.equals(MARKER)) return false;
        return true;
    }





    private static void modsIn(File modsSource, File base, Progress progress) throws IOException {
        File mods = new File(base, MODS);
        if (ready(mods)) return;
        if (mods.isDirectory() && !empty(mods)) {
            if (!new File(mods, MARKER).createNewFile()) throw new IOException("cannot write the marker in " + mods);
            return;
        }
        copyIn(modsSource, base, MODS, new String[]{".neu-"}, progress);
    }


    static boolean hasCharacters(File saves) {
        String[] names = saves.list();
        if (names != null) for (String n : names) if (n.toLowerCase(Locale.ROOT).endsWith(".d2s")) return true;
        return false;
    }

    private static boolean inside(File dir, File base) {
        return dir != null && dir.getAbsolutePath().startsWith(base.getAbsolutePath() + File.separator);
    }







    private static void adopt(File source, File base) throws IOException {
        File held = new File(base, ADOPT + GAME + "-" + System.currentTimeMillis());
        if (!source.renameTo(held)) throw new IOException("cannot take over " + source);
        finishAdoption(base);
        if (!ready(new File(base, GAME))) throw new IOException("cannot take over " + held);
    }


    static boolean heldInstallation(File base) {
        String[] held = base.list((d, n) -> n.startsWith(ADOPT + GAME + "-"));
        return held != null && held.length > 0;
    }


    static void finishAdoption(File base) throws IOException {
        File[] held = base.listFiles((d, n) -> n.startsWith(ADOPT + GAME + "-"));
        if (held == null) return;
        File target = new File(base, GAME);
        for (File h : held) {
            if (target.exists() || !ready(new File(base, SAVES))) continue;
            File saves = saveFolder(h);
            if (saves != null) delete(saves);
            if (!ready(h) && !new File(h, MARKER).createNewFile()) throw new IOException("cannot write the marker in " + h);
            if (!h.renameTo(target)) throw new IOException("cannot rename " + h + " to " + target);
        }
    }


    static boolean migrated(File base) {
        return ready(new File(base, GAME)) && ready(new File(base, SAVES)) && ready(new File(base, MODS));
    }


    static File saveFolder(File gameDir) {
        File[] entries = gameDir != null ? gameDir.listFiles() : null;
        if (entries != null) for (File e : entries) if (e.isDirectory() && e.getName().equalsIgnoreCase("save")) return e;
        return null;
    }






    static void copyIn(File source, File base, String name, String[] skip, Progress progress) throws IOException {
        if (!base.isDirectory() && !base.mkdirs()) throw new IOException("cannot create " + base);
        recover(base, name);
        File target = new File(base, name);
        long total = source != null && source.isDirectory() ? size(source, skip) : 0;
        long free = freeForTest >= 0 ? freeForTest : base.getUsableSpace();
        if (total + RESERVE > free) throw new NoSpace(total + RESERVE, free);
        File staging = new File(base, STAGING + name + "-" + System.currentTimeMillis());
        if (!staging.mkdirs()) throw new IOException("cannot create " + staging);
        long[] done = {0, 0};
        int[] files = {0};
        if (source != null && source.isDirectory()) {
            copyTree(source, staging, skip, true, name, total, done, files, progress);
            long[] checked = {0, 0};
            check(source, staging, skip, true, name + CHECKING, total, checked, progress);
        }
        progress.at(name, total, total);
        if (source != null && GAME.equals(name)) {
            try (java.io.Writer w = new java.io.OutputStreamWriter(new FileOutputStream(new File(staging, ORIGIN)), "UTF-8")) {
                w.write(source.getAbsolutePath());
            }
        }
        if (!new File(staging, MARKER).createNewFile()) throw new IOException("cannot write the marker in " + staging);
        File aside = null;
        if (target.exists()) {
            aside = new File(base, ASIDE + name + "-" + System.currentTimeMillis());
            if (!target.renameTo(aside)) throw new IOException("cannot set " + target + " aside");
        }
        if (!staging.renameTo(target)) {
            if (aside != null)
                aside.renameTo(target);
            throw new IOException("cannot rename " + staging + " to " + target);
        }
        if (aside != null) delete(aside);
    }





    static void recover(File base, String name) {
        File[] entries = base.listFiles();
        if (entries == null) return;
        File target = new File(base, name);
        Arrays.sort(entries, (a, b) -> b.getName().compareTo(a.getName()));
        for (File e : entries) {
            String n = e.getName();
            if (n.startsWith(STAGING + name + "-")) delete(e);
            else if (n.startsWith(ASIDE + name + "-")) {
                if (!target.exists() && ready(e) && e.renameTo(target)) continue;
                delete(e);
            }
        }
    }

    private static boolean skipped(String fileName, String[] skip) {
        if (fileName.equals(MARKER)) return true;
        if (skip == null) return false;
        String lower = fileName.toLowerCase(Locale.ROOT);
        for (String s : skip) {
            String t = s.toLowerCase(Locale.ROOT);
            if (t.endsWith("-") ? lower.startsWith(t) : lower.equals(t)) return true;
        }
        return false;
    }

    private static long size(File dir, String[] skip) throws IOException {
        long sum = 0;
        File[] list = dir.listFiles();
        if (list == null) throw new IOException("cannot list " + dir);
        for (File f : list) {
            if (skip != null && skipped(f.getName(), skip)) continue;
            sum += f.isDirectory() ? size(f, null) : f.length();
        }
        return sum;
    }

    private static void copyTree(File from, File to, String[] skip, boolean top, String name, long total, long[] done,
                                 int[] files, Progress progress) throws IOException {
        File[] list = from.listFiles();
        if (list == null) throw new IOException("cannot list " + from);
        for (File f : list) {
            if (top && skipped(f.getName(), skip)) continue;
            File out = new File(to, f.getName());
            if (f.isDirectory()) {
                if (!out.mkdirs()) throw new IOException("cannot create " + out);
                copyTree(f, out, null, false, name, total, done, files, progress);
                continue;
            }
            if (killAfterFiles >= 0 && files[0] >= killAfterFiles) throw new IOException("test: killed after " + files[0] + " files");
            copyFile(f, out, name, total, done, progress);
            files[0]++;
        }
    }

    private static void copyFile(File from, File to, String name, long total, long[] done, Progress progress) throws IOException {
        byte[] buffer = new byte[BUFFER];
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
                done[0] += n;
                if (done[0] - done[1] >= PROGRESS_STEP) {
                    done[1] = done[0];
                    progress.at(name, done[0], total);
                }
            }
        }

        to.setLastModified(from.lastModified());
    }


    private static void check(File from, File to, String[] skip, boolean top, String what, long total, long[] done,
                              Progress progress) throws IOException {
        File[] list = from.listFiles();
        if (list == null) throw new IOException("cannot list " + from);
        for (File f : list) {
            if (top && skipped(f.getName(), skip)) continue;
            File c = new File(to, f.getName());
            if (f.isDirectory()) {
                if (!c.isDirectory()) throw new Mismatch("missing folder " + c);
                check(f, c, null, false, what, total, done, progress);
            }
            else {
                if (!c.isFile() || c.length() != f.length() || !Arrays.equals(sha256(f), sha256(c))) {
                    throw new Mismatch("copy differs: " + c);
                }
                done[0] += f.length();
                if (done[0] - done[1] >= PROGRESS_STEP) {
                    done[1] = done[0];
                    progress.at(what, done[0], total);
                }
            }
        }
    }

    static byte[] sha256(File f) throws IOException {
        try (InputStream in = new FileInputStream(f)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER];
            int n;
            while ((n = in.read(buffer)) > 0) md.update(buffer, 0, n);
            return md.digest();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    static void delete(File f) {
        File[] list = f.isDirectory() ? f.listFiles() : null;
        if (list != null) for (File c : list) delete(c);

        f.delete();
    }
}
