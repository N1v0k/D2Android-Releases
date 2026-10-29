package com.winlator.d2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;










final class D2CharDelete {
    private D2CharDelete() {}


    static final String TRASH = "Geloescht";

    static final long KEEP_MS = 3L * 24 * 60 * 60 * 1000;
    private static final long DAY_MS = 24L * 60 * 60 * 1000;
    private static final String STAMP = "yyyyMMdd-HHmmss";
    private static final Pattern ZIP_NAME = Pattern.compile("(.+)-(\\d{8}-\\d{6})\\.zip");
    private static final long MAX_FILE = 8L << 20;




    static final String TIMES = "d2android-zeiten.txt";

    static int killAfter = -1;


    static final class Failure extends IOException {
        enum Reason { NO_CHARACTER, BACKUP, COLLISION, BROKEN }

        final Reason reason;

        Failure(Reason reason, String detail) {
            super(reason + (detail != null ? ": " + detail : ""));
            this.reason = reason;
        }
    }


    static final class Deleted {
        final File zip;
        final D2SaveHeader.Info info;
        final long deletedAt;

        Deleted(File zip, D2SaveHeader.Info info, long deletedAt) {
            this.zip = zip;
            this.info = info;
            this.deletedAt = deletedAt;
        }


        int daysLeft(long now) {
            long left = deletedAt + KEEP_MS - now;
            return left <= 0 ? 0 : (int) Math.min(3, (left + DAY_MS - 1) / DAY_MS);
        }
    }




    static boolean belongs(String name, String file) {
        String lower = name.toLowerCase(Locale.ROOT), base = file.toLowerCase(Locale.ROOT) + ".";
        if (!lower.startsWith(base)) return false;
        String ext = lower.substring(base.length());

        int aside = ext.indexOf(".unterbrochen-");
        if (aside > 0) ext = ext.substring(0, aside);
        return ext.equals("d2s") || ext.equals("key") || ext.equals("map") || ext.equals("d2x") || ext.equals("d2~")
            || ext.matches("ma[0-9]");
    }


    static boolean belongsBackup(String folder, String file) {
        String lower = folder.toLowerCase(Locale.ROOT), base = file.toLowerCase(Locale.ROOT) + "_";
        return lower.startsWith(base) && lower.substring(base.length()).matches("\\d{8}_\\d{6}");
    }


    static Map<String, File> files(File saveDir, String file) {
        Map<String, File> out = new LinkedHashMap<>();
        File[] all = saveDir.listFiles();
        if (all == null) return out;
        List<File> sorted = new ArrayList<>();
        Collections.addAll(sorted, all);
        Collections.sort(sorted);
        for (File f : sorted) if (f.isFile() && f.getName().equalsIgnoreCase(file + ".d2s")) out.put(f.getName(), f);
        for (File f : sorted) if (f.isFile() && belongs(f.getName(), file)) out.put(f.getName(), f);
        File backup = child(saveDir, "backup");
        File[] backups = backup != null ? backup.listFiles() : null;
        if (backups != null) {
            List<File> b = new ArrayList<>();
            Collections.addAll(b, backups);
            Collections.sort(b);
            for (File dir : b) {
                if (!dir.isDirectory() || !belongsBackup(dir.getName(), file)) continue;
                File[] inside = dir.listFiles();
                if (inside == null) continue;
                List<File> in = new ArrayList<>();
                Collections.addAll(in, inside);
                Collections.sort(in);
                for (File f : in) if (f.isFile()) out.put(backup.getName() + "/" + dir.getName() + "/" + f.getName(), f);
            }
        }
        return out;
    }

    private static File child(File dir, String name) {
        File[] all = dir.listFiles();
        if (all != null) for (File f : all) if (f.isDirectory() && f.getName().equalsIgnoreCase(name)) return f;
        return null;
    }

    static File trashDir(File saveDir) {
        File t = child(saveDir, TRASH);
        return t != null ? t : new File(saveDir, TRASH);
    }

    static String zipName(String file, long now) {
        SimpleDateFormat f = new SimpleDateFormat(STAMP, Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return file + "-" + f.format(new Date(now)) + ".zip";
    }


    static long deletedAt(String zipName) {
        java.util.regex.Matcher m = ZIP_NAME.matcher(zipName);
        if (!m.matches()) return -1;
        SimpleDateFormat f = new SimpleDateFormat(STAMP, Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        f.setLenient(false);
        try {
            return f.parse(m.group(2)).getTime();
        }
        catch (ParseException e) {
            return -1;
        }
    }







    static synchronized File delete(File saveDir, String file, long now) throws IOException {
        Map<String, File> files = files(saveDir, file);
        if (files.isEmpty() || !files.keySet().iterator().next().toLowerCase(Locale.ROOT).endsWith(".d2s")) {
            throw new Failure(Failure.Reason.NO_CHARACTER, file);
        }
        File trash = trashDir(saveDir);
        if (!trash.isDirectory() && !trash.mkdirs()) throw new Failure(Failure.Reason.BACKUP, trash.getPath());
        File zip = new File(trash, zipName(file, now));
        File part = new File(trash, zip.getName() + ".part");
        try {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(part))) {
                for (Map.Entry<String, File> e : files.entrySet()) {
                    ZipEntry entry = new ZipEntry(e.getKey());
                    entry.setTime(e.getValue().lastModified());
                    out.putNextEntry(entry);
                    try (InputStream in = new FileInputStream(e.getValue())) {
                        copy(in, out);
                    }
                    out.closeEntry();
                }
                StringBuilder times = new StringBuilder();
                for (Map.Entry<String, File> e : files.entrySet()) times.append(e.getValue().lastModified()).append(' ').append(e.getKey()).append('\n');
                out.putNextEntry(new ZipEntry(TIMES));
                out.write(times.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.closeEntry();
            }
            check(part, files);
            if (zip.exists() || !part.renameTo(zip)) throw new Failure(Failure.Reason.BACKUP, zip.getPath());
        }
        catch (IOException | RuntimeException e) {
            if (part.exists() && !part.delete()) part.deleteOnExit();
            throw e instanceof Failure ? (Failure) e : (Failure) new Failure(Failure.Reason.BACKUP, String.valueOf(e)).initCause(e);
        }
        int step = 0;
        List<File> folders = new ArrayList<>();



        List<File> order = new ArrayList<>();
        for (Map.Entry<String, File> e : files.entrySet()) if (temporary(e.getKey())) order.add(e.getValue());
        for (Map.Entry<String, File> e : files.entrySet()) if (!temporary(e.getKey())) order.add(e.getValue());
        for (File f : order) {
            if (killAfter >= 0 && step++ > killAfter) throw new Killed();
            if (!f.delete() && f.exists()) D2Trace.log("delete: " + f.getName() + " stays");
            File parent = f.getParentFile();
            if (parent != null && !parent.equals(saveDir) && !folders.contains(parent)) folders.add(parent);
        }
        for (File dir : folders) if (!dir.delete()) D2Trace.log("delete: folder " + dir.getName() + " stays");
        return zip;
    }

    private static boolean temporary(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return !lower.contains("/") && (lower.endsWith(".d2~") || lower.contains(".unterbrochen-"));
    }


    private static void check(File zip, Map<String, File> files) throws IOException {
        Map<String, Long> sizes = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(zip))) {
            byte[] buf = new byte[8192];
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                if (e.getName().equals(TIMES)) continue;
                long n = 0;
                for (int r; (r = in.read(buf)) > 0; ) n += r;
                sizes.put(e.getName(), n);
            }
        }
        if (sizes.size() != files.size()) throw new Failure(Failure.Reason.BACKUP, "count");
        for (Map.Entry<String, File> e : files.entrySet()) {
            Long n = sizes.get(e.getKey());
            if (n == null || n != e.getValue().length()) throw new Failure(Failure.Reason.BACKUP, e.getKey());
        }
    }


    static final class Killed extends Error {}




    static List<Deleted> pending(File saveDir, long now) {
        List<Deleted> out = new ArrayList<>();
        File trash = child(saveDir, TRASH);
        File[] zips = trash != null ? trash.listFiles() : null;
        if (zips == null) return out;
        for (File z : zips) {
            long at = deletedAt(z.getName());
            if (!z.isFile() || at < 0 || now - at >= KEEP_MS) continue;
            D2SaveHeader.Info info = header(z);
            if (info != null) out.add(new Deleted(z, info, at));
        }
        Collections.sort(out, (a, b) -> Long.compare(b.deletedAt, a.deletedAt));
        return out;
    }


    static D2SaveHeader.Info header(File zip) {
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(zip))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                String n = e.getName();
                if (n.contains("/") || !n.toLowerCase(Locale.ROOT).endsWith(".d2s")) continue;
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                copy(in, data);
                byte[] b = data.toByteArray();
                return D2SaveHeader.parse(b, b.length, n.substring(0, n.length() - 4), e.getTime());
            }
        }
        catch (IOException | RuntimeException e) {
            D2Trace.log("deleted character not read: " + zip.getName() + " " + e);
        }
        return null;
    }





    static synchronized void restore(File saveDir, File zip) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        String times = "";
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(zip))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null; ) {
                if (e.isDirectory()) continue;
                String n = e.getName();
                if (n.startsWith("/") || n.contains("..") || n.contains("\\")) throw new Failure(Failure.Reason.BROKEN, n);
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                copy(in, data);
                if (n.equals(TIMES)) times = new String(data.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                else files.put(n, data.toByteArray());
            }
        }
        catch (java.util.zip.ZipException | java.io.EOFException e) {
            throw (Failure) new Failure(Failure.Reason.BROKEN, zip.getName()).initCause(e);
        }
        boolean character = false;



        List<String> there = new ArrayList<>();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String n = e.getKey();
            if (!n.contains("/") && n.toLowerCase(Locale.ROOT).endsWith(".d2s")) character = true;
            File old = existing(saveDir, n);
            if (old == null) continue;
            if (!java.util.Arrays.equals(Files.readAllBytes(old.toPath()), e.getValue())) throw new Failure(Failure.Reason.COLLISION, n);
            there.add(n);
        }
        if (!character) throw new Failure(Failure.Reason.BROKEN, zip.getName());
        List<File> written = new ArrayList<>();
        try {

            List<String> order = new ArrayList<>(files.keySet());
            String d2s = order.remove(0);
            order.add(d2s);
            for (String n : order) {
                if (there.contains(n)) continue;
                File target = target(saveDir, n);
                File parent = target.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new Failure(Failure.Reason.BACKUP, parent.getPath());
                File part = new File(parent, target.getName() + ".d2android-new");
                try (OutputStream out = new FileOutputStream(part)) {
                    out.write(files.get(n));
                }
                if (!part.renameTo(target)) {
                    if (!part.delete()) part.deleteOnExit();
                    throw new Failure(Failure.Reason.BACKUP, target.getPath());
                }
                written.add(target);
            }
            for (String line : times.split("\n")) {
                int space = line.indexOf(' ');
                if (space <= 0 || !files.containsKey(line.substring(space + 1)) || there.contains(line.substring(space + 1))) continue;
                try {
                    if (!target(saveDir, line.substring(space + 1)).setLastModified(Long.parseLong(line.substring(0, space))))
                        D2Trace.log("restore: time not set " + line);
                }
                catch (NumberFormatException ignored) {

                }
            }
        }
        catch (IOException | RuntimeException e) {
            for (File f : written) if (!f.delete()) f.deleteOnExit();
            throw e;
        }
        if (!zip.delete()) D2Trace.log("restore: zip stays " + zip.getName());
    }


    private static File target(File saveDir, String name) {
        if (!name.contains("/")) return new File(saveDir, name);
        String[] parts = name.split("/");
        File backup = child(saveDir, parts[0]);
        File dir = backup != null ? backup : new File(saveDir, parts[0]);
        for (int i = 1; i < parts.length - 1; i++) dir = new File(dir, parts[i]);
        return new File(dir, parts[parts.length - 1]);
    }


    private static File existing(File saveDir, String name) {
        File t = target(saveDir, name);
        File[] siblings = t.getParentFile() != null ? t.getParentFile().listFiles() : null;
        if (siblings != null) for (File f : siblings) if (f.getName().equalsIgnoreCase(t.getName())) return f;
        return null;
    }





    static File purge(File zip, File backups) throws IOException {
        if (!backups.isDirectory() && !backups.mkdirs()) throw new Failure(Failure.Reason.BACKUP, backups.getPath());
        File to = new File(backups, "Geloescht-" + zip.getName());
        if (to.exists()) to = new File(backups, "Geloescht-" + System.nanoTime() + "-" + zip.getName());
        if (zip.renameTo(to)) return to;


        try (InputStream in = new FileInputStream(zip); OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[8192];
            for (int r; (r = in.read(buf)) > 0; ) out.write(buf, 0, r);
        }
        catch (IOException e) {
            if (!to.delete()) to.deleteOnExit();
            throw e;
        }
        if (to.length() != zip.length()) {
            if (!to.delete()) to.deleteOnExit();
            throw new Failure(Failure.Reason.BACKUP, to.getPath());
        }
        if (!zip.delete()) D2Trace.log("purge: zip stays " + zip.getName());
        return to;
    }


    static List<File> due(File saveDir, long now) {
        List<File> out = new ArrayList<>();
        File trash = child(saveDir, TRASH);
        File[] zips = trash != null ? trash.listFiles() : null;
        if (zips == null) return out;
        for (File z : zips) {
            long at = deletedAt(z.getName());
            if (z.isFile() && at >= 0 && now - at >= KEEP_MS) out.add(z);
        }
        return out;
    }







    static boolean settingKey(String key, int characterClass, String name) {
        String tail = "." + characterClass + "." + name;
        return key.equals("build" + tail) || key.equals("attackRight" + tail)
            || key.equals("lootFilter" + tail) || key.equals("lootHide" + tail);
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        long total = 0;
        for (int r; (r = in.read(buf)) > 0; ) {
            total += r;
            if (total > MAX_FILE) throw new Failure(Failure.Reason.BROKEN, "too large");
            out.write(buf, 0, r);
        }
    }
}
