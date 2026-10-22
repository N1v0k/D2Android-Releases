package com.winlator.d2;

import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class D2Saves {
    public static final String BACKUP_FOLDER = "D2Android/Sicherungen";
    private static final long MAX_FILE = 8L << 20, MAX_TOTAL = 64L << 20;
    private static final String TEMP = ".d2android-new", ASIDE = ".d2android-old";
    static final String JOURNAL = ".d2android-import";
    private static final String TAG = "D2Saves";
    static int killAfter = -1;

    private static boolean importing, launchedOnce;
    private static int launching;
    private static long launchedAt;
    static long launchGraceMs = 15_000;

    public static synchronized boolean beginImport() {
        if (importing || launching > 0 || (launchedOnce && System.nanoTime() - launchedAt < launchGraceMs * 1_000_000L)) return false;
        importing = true;
        return true;
    }

    public static synchronized void endImport() {
        importing = false;
    }

    public static synchronized boolean importRunning() {
        return importing;
    }

    public static synchronized boolean beginLaunch() {
        if (importing) return false;
        launching++;
        return true;
    }

    public static synchronized void launched() {
        if (launching > 0) launching--;
        launchedOnce = true;
        launchedAt = System.nanoTime();
    }

    public static synchronized void launchFailed() {
        if (launching > 0) launching--;
    }

    private D2Saves() {}

    public static final class Failure extends IOException {
        public enum Reason { UNREADABLE, CANNOT_CREATE, ZIP_TOO_LARGE, NO_CHARACTER, CANNOT_REPLACE, CANNOT_WRITE, FILE_TOO_LARGE, IMPORT_UNFINISHED, BROKEN_ZIP }

        public final Reason reason;
        public final String subject;

        Failure(Reason reason, String subject) {
            super(subject == null ? reason.name() : reason.name() + ": " + subject);
            this.reason = reason;
            this.subject = subject;
        }
    }

    public static File newestCharacter(File dir) {
        File[] files = dir != null ? dir.listFiles() : null;
        File newest = null;
        if (files != null) {
            for (File f : files) {
                if (f.getName().toLowerCase(Locale.ROOT).endsWith(".d2s") && (newest == null || f.lastModified() > newest.lastModified())) newest = f;
            }
        }
        return newest;
    }

    public static boolean completeCharacter(File d2s) {
        long length = d2s != null ? d2s.length() : 0;
        if (length < 16 || length > MAX_FILE) return false;
        byte[] d = new byte[(int) length];
        try (InputStream in = new FileInputStream(d2s)) {
            int at = 0;
            for (int n; at < d.length && (n = in.read(d, at, d.length - at)) > 0; ) at += n;
            if (at != d.length) return false;
        }
        catch (IOException e) {
            return false;
        }
        return completeCharacter(d, d.length);
    }

    static boolean completeCharacter(byte[] d, int n) {
        if (n < 16 || n > d.length || le32(d, 0) != 0xAA55AA55 || le32(d, 8) != n) return false;
        int sum = 0;
        for (int i = 0; i < n; i++) sum = ((sum << 1) | (sum >>> 31)) + (i >= 12 && i < 16 ? 0 : d[i] & 0xFF);
        return sum == le32(d, 12);
    }

    private static int le32(byte[] d, int at) {
        return (d[at] & 0xFF) | (d[at + 1] & 0xFF) << 8 | (d[at + 2] & 0xFF) << 16 | (d[at + 3] & 0xFF) << 24;
    }

    public static long newestCharacterFile(File dir) {
        File[] files = dir != null ? dir.listFiles() : null;
        long newest = 0;
        if (files != null) {
            for (File f : files) {
                if (f.getName().toLowerCase(Locale.ROOT).endsWith(".d2s")) newest = Math.max(newest, f.lastModified());
            }
        }
        return newest;
    }

    public static long newestSaveFile(File dir) {
        File[] files = dir != null ? dir.listFiles() : null;
        long newest = 0;
        if (files != null) for (File f : files) if (isSaveFile(f.getName())) newest = Math.max(newest, f.lastModified());
        return newest;
    }

    public static File saveDir(File gameDir) {
        File[] entries = gameDir.listFiles();
        if (entries != null) {
            for (File entry : entries) if (entry.isDirectory() && entry.getName().equalsIgnoreCase("save")) return entry;
        }
        return new File(gameDir, "save");
    }

    static boolean isSaveFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.isEmpty() || lower.startsWith(".") || lower.contains("/") || lower.contains("\\") || lower.contains("..")) return false;
        return lower.endsWith(".d2s") || lower.endsWith(".d2x") || lower.endsWith(".key") || lower.endsWith(".map")
            || lower.endsWith(".sss") || lower.endsWith(".d2l") || lower.matches(".*\\.ma[0-9]");
    }

    public static synchronized int zip(File saveDir, File out) throws IOException {
        if (!recover(saveDir)) throw new Failure(Failure.Reason.IMPORT_UNFINISHED, null);
        File[] files = saveDir.listFiles();
        int count = 0;
        if (files == null) return 0;
        java.util.Arrays.sort(files);
        File parent = out.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new Failure(Failure.Reason.CANNOT_CREATE, parent.getPath());
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(out))) {
            for (File file : files) {
                if (!file.isFile() || !isSaveFile(file.getName())) continue;
                ZipEntry entry = new ZipEntry(file.getName());
                entry.setTime(file.lastModified());
                zip.putNextEntry(entry);
                try (InputStream in = new FileInputStream(file)) {
                    copy(in, zip, MAX_FILE);
                }
                zip.closeEntry();
                count++;
            }
        }
        if (count == 0 && !out.delete()) out.deleteOnExit();
        return count;
    }

    public static String zipName(String prefix) {
        return prefix + "-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".zip";
    }

    public static File backup(File saveDir, String reason) throws IOException {
        File folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), BACKUP_FOLDER);
        File out = new File(folder, zipName("Sicherung-" + reason));
        return zip(saveDir, out) > 0 ? out : null;
    }

    public static int importZip(InputStream source, File saveDir) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();
        long total = 0;
        boolean character = false;
        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String path = entry.getName().replace('\\', '/');
                String name = path.substring(path.lastIndexOf('/') + 1);
                if (!isSaveFile(name) || path.toLowerCase(Locale.ROOT).contains("backup/")) continue;
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                copy(zip, data, MAX_FILE);
                total += data.size();
                if (total > MAX_TOTAL) throw new Failure(Failure.Reason.ZIP_TOO_LARGE, null);
                files.put(name, data.toByteArray());
                if (name.toLowerCase(Locale.ROOT).endsWith(".d2s")) character = true;
            }
        }
        catch (java.util.zip.ZipException | java.io.EOFException | IllegalArgumentException e) {
            Failure broken = new Failure(Failure.Reason.BROKEN_ZIP, null);
            broken.initCause(e);
            throw broken;
        }
        if (!character) throw new Failure(Failure.Reason.NO_CHARACTER, null);
        if (!saveDir.isDirectory() && !saveDir.mkdirs()) throw new Failure(Failure.Reason.CANNOT_CREATE, saveDir.getPath());
        synchronized (D2Saves.class) {
            if (!recover(saveDir)) throw new Failure(Failure.Reason.IMPORT_UNFINISHED, null);
            replace(files, saveDir);
        }
        return files.size();
    }

    private static void replace(Map<String, byte[]> files, File saveDir) throws IOException {
        File journal = new File(saveDir, JOURNAL);
        List<String> aside = new ArrayList<>(), installed = new ArrayList<>();
        boolean journaled = false;
        try {
            for (Map.Entry<String, byte[]> file : files.entrySet()) {
                try (OutputStream out = new FileOutputStream(new File(saveDir, file.getKey() + TEMP))) {
                    step();
                    out.write(file.getValue());
                }
                step();
            }
            File[] existing = saveDir.listFiles();
            if (existing != null) {
                Arrays.sort(existing);
                for (File old : existing) {
                    String name = old.getName();
                    int dot = name.lastIndexOf('.');
                    if (!old.isFile() || !isSaveFile(name) || dot <= 0) continue;
                    if (!containsIgnoreCase(files, name) && !containsIgnoreCase(files, name.substring(0, dot) + ".d2s")) continue;
                    File moved = new File(saveDir, name + ASIDE);
                    if (moved.exists() || !old.renameTo(moved)) throw new Failure(Failure.Reason.CANNOT_REPLACE, name);
                    step();
                    aside.add(name);
                }
            }
            writeJournal(journal, files.keySet(), aside);
            journaled = true;
            for (String name : files.keySet()) {
                File target = new File(saveDir, name);
                if (target.exists()) throw new Failure(Failure.Reason.CANNOT_REPLACE, name);
                if (!new File(saveDir, name + TEMP).renameTo(target)) throw new Failure(Failure.Reason.CANNOT_WRITE, name);
                step();
                installed.add(name);
            }
        }
        catch (IOException | RuntimeException e) {
            boolean undone = true;
            for (int i = installed.size() - 1; i >= 0 && undone; i--) {
                undone = new File(saveDir, installed.get(i)).renameTo(new File(saveDir, installed.get(i) + TEMP));
                if (undone) step();
            }
            if (undone && journaled && journal.delete()) step();
            recover(saveDir);
            throw e;
        }
        recover(saveDir);
    }

    public static synchronized boolean recover(File saveDir) {
        File journal = new File(saveDir, JOURNAL);
        if (journal.isFile()) {
            List<String> news = new ArrayList<>(), olds = new ArrayList<>();
            try {
                readJournal(journal, news, olds);
            }
            catch (IOException e) {
                Log.w(TAG, "unreadable import journal " + journal, e);
                return false;
            }
            for (String name : news) {
                File temp = new File(saveDir, name + TEMP), target = new File(saveDir, name);
                if (!temp.isFile()) continue;
                if (target.exists()) Log.w(TAG, "import: " + name + " came back during the import, replaced");
                if (!temp.renameTo(target)) return false;
                step();
            }
            for (String name : olds) {
                File old = new File(saveDir, name + ASIDE);
                if (!old.isFile()) continue;
                if (!old.delete()) return false;
                step();
            }
            if (!journal.delete()) return false;
            step();
        }
        File[] entries = saveDir.listFiles();
        if (entries == null) return true;
        boolean done = true;
        for (File file : entries) {
            String name = file.getName();
            if (!file.isFile()) continue;
            if (name.equals(JOURNAL + TEMP) || (name.endsWith(TEMP) && isSaveFile(strip(name, TEMP)))) {
                if (!file.delete()) done = false;
                else step();
            }
            else if (name.endsWith(ASIDE) && isSaveFile(strip(name, ASIDE))) {
                File original = new File(saveDir, strip(name, ASIDE));
                if (original.exists()) Log.w(TAG, "import: kept " + name + ", " + original.getName() + " exists as well");
                else if (!file.renameTo(original)) done = false;
                else step();
            }
        }
        return done;
    }

    private static void writeJournal(File journal, java.util.Collection<String> news, List<String> olds) throws IOException {
        File temp = new File(journal.getPath() + TEMP);
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(temp))) {
            step();
            out.writeInt(news.size());
            for (String name : news) out.writeUTF(name);
            out.writeInt(olds.size());
            for (String name : olds) out.writeUTF(name);
        }
        step();
        if (!temp.renameTo(journal)) throw new Failure(Failure.Reason.CANNOT_WRITE, JOURNAL);
        step();
    }

    private static void readJournal(File journal, List<String> news, List<String> olds) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(journal))) {
            for (int i = in.readInt(); i > 0; i--) news.add(in.readUTF());
            for (int i = in.readInt(); i > 0; i--) olds.add(in.readUTF());
        }
        for (String name : news) if (!isSaveFile(name)) throw new IOException("not a save file: " + name);
        for (String name : olds) if (!isSaveFile(name)) throw new IOException("not a save file: " + name);
    }

    private static String strip(String name, String suffix) {
        return name.substring(0, name.length() - suffix.length());
    }

    static final class Killed extends Error {}

    private static void step() {
        if (killAfter >= 0 && killAfter-- == 0) throw new Killed();
    }

    private static boolean containsIgnoreCase(Map<String, byte[]> files, String name) {
        for (String key : files.keySet()) if (key.equalsIgnoreCase(name)) return true;
        return false;
    }

    private static void copy(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] buffer = new byte[16384];
        long copied = 0;
        int n;
        while ((n = in.read(buffer)) > 0) {
            copied += n;
            if (copied > limit) throw new Failure(Failure.Reason.FILE_TOO_LARGE, null);
            out.write(buffer, 0, n);
        }
    }
}
