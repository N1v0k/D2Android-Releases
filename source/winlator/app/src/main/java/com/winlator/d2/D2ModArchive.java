package com.winlator.d2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;










public final class D2ModArchive {
    static final int MAX_FILES = 50_000;
    static final long MAX_FILE = 2_000_000_000L, MAX_TOTAL = 4_000_000_000L;

    static final long MAX_RATIO = 200, RATIO_FREE = 1_000_000;
    static final String[] CODE = {".dll", ".exe", ".asi", ".bat", ".cmd", ".com", ".scr", ".ps1", ".vbs", ".js", ".msi", ".sys", ".drv", ".ocx", ".cpl"};

    private D2ModArchive() {}


    public enum Reason { NOT_AN_ARCHIVE, BROKEN, UNSAFE_PATH, LINK, DUPLICATE, TOO_MANY, TOO_LARGE, BOMB, NO_GAME_FILES, AMBIGUOUS, CODE, NO_SPACE }

    public static final class Failure extends IOException {
        public final Reason reason;

        public final String detail;

        public Failure(Reason reason, String detail) {
            super(reason + (detail != null ? ": " + detail : ""));
            this.reason = reason;
            this.detail = detail;
        }
    }


    public static final class Entry {
        final String name;
        final long size, packed;
        final boolean directory, link;

        public Entry(String name, long size, long packed, boolean directory, boolean link) {
            this.name = name;
            this.size = size;
            this.packed = packed;
            this.directory = directory;
            this.link = link;
        }
    }


    public static final class Plan {

        public final String root;

        public final Map<String, String> files;
        public final long total;

        public final boolean txt, data;

        public final boolean inventory;

        public final int leftOut;

        Plan(String root, Map<String, String> files, long total, boolean txt, boolean data, boolean inventory, int leftOut) {
            this.root = root;
            this.files = Collections.unmodifiableMap(files);
            this.total = total;
            this.txt = txt;
            this.data = data;
            this.inventory = inventory;
            this.leftOut = leftOut;
        }
    }




    static String clean(String name) throws Failure {
        if (name == null || name.isEmpty()) throw new Failure(Reason.UNSAFE_PATH, String.valueOf(name));
        String n = name.replace('\\', '/');
        if (n.startsWith("/") || (n.length() > 1 && n.charAt(1) == ':')) throw new Failure(Reason.UNSAFE_PATH, name);
        StringBuilder out = new StringBuilder();
        for (String part : n.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) throw new Failure(Reason.UNSAFE_PATH, name);
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c < 0x20 || c == 0x7F || c == ':' || c == '*' || c == '?' || c == '"' || c == '<' || c == '>' || c == '|')
                    throw new Failure(Reason.UNSAFE_PATH, name);
            }
            if (part.endsWith(".") || part.endsWith(" ")) throw new Failure(Reason.UNSAFE_PATH, name);
            if (out.length() > 0) out.append('/');
            out.append(part);
        }
        if (out.length() == 0) throw new Failure(Reason.UNSAFE_PATH, name);
        return out.toString();
    }

    static boolean isCode(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        for (String ext : CODE) if (lower.endsWith(ext)) return true;
        return false;
    }


    public static Plan check(List<Entry> entries) throws Failure {
        Map<String, String> byLower = new HashMap<>();
        List<String[]> files = new ArrayList<>();
        long total = 0;
        for (Entry e : entries) {
            if (e.link) throw new Failure(Reason.LINK, e.name);
            String path = clean(e.name);
            String lower = path.toLowerCase(Locale.ROOT);
            String before = byLower.put(lower + (e.directory ? "/" : ""), path);
            if (before != null && !e.directory) throw new Failure(Reason.DUPLICATE, e.name);
            if (e.directory) continue;
            if (byLower.containsKey(lower + "/")) throw new Failure(Reason.DUPLICATE, e.name);
            if (files.size() >= MAX_FILES) throw new Failure(Reason.TOO_MANY, null);
            if (e.size < 0 || e.size > MAX_FILE) throw new Failure(Reason.TOO_LARGE, e.name);
            total += e.size;
            if (total > MAX_TOTAL) throw new Failure(Reason.TOO_LARGE, null);
            if (e.size > RATIO_FREE && (e.packed <= 0 || e.size / e.packed > MAX_RATIO)) throw new Failure(Reason.BOMB, e.name);
            files.add(new String[]{e.name, path});
        }

        Set<String> fileNames = new HashSet<>();
        for (String[] f : files) fileNames.add(f[1].toLowerCase(Locale.ROOT));
        for (String[] f : files) {
            String lower = f[1].toLowerCase(Locale.ROOT);
            for (int i = lower.indexOf('/'); i > 0; i = lower.indexOf('/', i + 1)) {
                if (fileNames.contains(lower.substring(0, i))) throw new Failure(Reason.DUPLICATE, f[0]);
            }
        }


        Map<String, String> spelling = new HashMap<>();
        for (String[] f : files) {
            String[] parts = f[1].split("/");
            StringBuilder lowerPrefix = new StringBuilder(), out = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    lowerPrefix.append('/');
                    out.append('/');
                }
                lowerPrefix.append(parts[i].toLowerCase(Locale.ROOT));
                if (i == parts.length - 1) out.append(parts[i]);
                else {
                    String first = spelling.get(lowerPrefix.toString());
                    if (first == null) spelling.put(lowerPrefix.toString(), first = parts[i]);
                    out.append(first);
                }
            }
            f[1] = out.toString();
        }
        String root = root(files);
        Map<String, String> taken = new java.util.LinkedHashMap<>();
        long size = 0;
        boolean txt = false, data = false, inventory = false;
        int leftOut = 0;
        Map<String, Long> sizes = new HashMap<>();
        for (Entry e : entries) if (!e.directory) sizes.put(e.name, e.size);
        for (String[] f : files) {
            if (!f[1].toLowerCase(Locale.ROOT).startsWith(root.toLowerCase(Locale.ROOT))) {
                leftOut++;
                continue;
            }
            String below = f[1].substring(root.length());
            if (isCode(below)) throw new Failure(Reason.CODE, below);
            taken.put(f[0], below);
            size += sizes.get(f[0]);
            String lower = below.toLowerCase(Locale.ROOT);
            if (lower.startsWith("data/")) data = true;
            if (lower.startsWith("data/global/excel/") && lower.endsWith(".txt")) txt = true;
            if (lower.equals("data/global/excel/inventory.txt") || lower.equals("data/global/excel/inventory.bin")) inventory = true;
        }
        return new Plan(root, taken, size, txt, data, inventory, leftOut);
    }





    static String root(List<String[]> files) throws Failure {
        Set<String> roots = new HashSet<>();
        for (String[] f : files) {
            String lower = f[1].toLowerCase(Locale.ROOT);
            String[] parts = lower.split("/");
            for (int depth = 0; depth < parts.length && depth <= 2; depth++) {
                boolean hit = (parts.length > depth + 1 && parts[depth].equals("data"))
                    || (parts.length == depth + 1 && parts[depth].endsWith(".mpq"));
                if (!hit) continue;
                StringBuilder r = new StringBuilder();
                String[] orig = f[1].split("/");
                for (int i = 0; i < depth; i++) r.append(orig[i]).append('/');
                roots.add(r.toString());
                break;
            }
        }
        if (roots.isEmpty()) throw new Failure(Reason.NO_GAME_FILES, null);

        String best = null;
        for (String r : roots) if (best == null || r.length() < best.length()) best = r;
        for (String r : roots) {
            if (!r.toLowerCase(Locale.ROOT).startsWith(best.toLowerCase(Locale.ROOT))) throw new Failure(Reason.AMBIGUOUS, r);
        }
        return best;
    }




    public static String kind(File archive) throws IOException {
        byte[] head = new byte[6];
        try (RandomAccessFile f = new RandomAccessFile(archive, "r")) {
            if (f.length() < 6) return null;
            f.readFully(head);
        }
        if (head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4) return "zip";
        if (head[0] == '7' && head[1] == 'z' && (head[2] & 0xFF) == 0xBC && (head[3] & 0xFF) == 0xAF && head[4] == 0x27 && head[5] == 0x1C) return "7z";
        return null;
    }


    public static List<Entry> list(File archive) throws IOException {
        String kind = kind(archive);
        if (kind == null) throw new Failure(Reason.NOT_AN_ARCHIVE, null);
        List<Entry> out = new ArrayList<>();
        try {
            if (kind.equals("zip")) {
                try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive)) {
                    Enumeration<? extends java.util.zip.ZipEntry> all = zip.entries();
                    while (all.hasMoreElements()) {
                        java.util.zip.ZipEntry e = all.nextElement();
                        out.add(new Entry(e.getName(), e.getSize(), e.getCompressedSize(), e.isDirectory(), false));
                    }
                }


            }
            else {
                try (org.apache.commons.compress.archivers.sevenz.SevenZFile seven = new org.apache.commons.compress.archivers.sevenz.SevenZFile(archive, sevenOptions())) {
                    long total = 0;
                    for (org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry e : seven.getEntries()) {
                        boolean link = e.getHasWindowsAttributes() && (e.getWindowsAttributes() & 0x400) != 0
                            || e.getHasWindowsAttributes() && ((e.getWindowsAttributes() >>> 16) & 0xF000) == 0xA000;


                        out.add(new Entry(e.getName(), e.getSize(), e.getSize() > RATIO_FREE ? e.getSize() : 1, e.isDirectory(), link));
                        if (!e.isDirectory()) total += Math.max(0, e.getSize());
                    }
                    if (total > RATIO_FREE && total / Math.max(1, archive.length()) > MAX_RATIO) throw new Failure(Reason.BOMB, null);
                }
            }
        }
        catch (Failure f) {
            throw f;
        }
        catch (IOException | RuntimeException | OutOfMemoryError e) {
            throw sevenFailure(e);
        }
        return out;
    }





    static final int MAX_7Z_MEMORY_KB = 80 * 1024;

    private static org.apache.commons.compress.archivers.sevenz.SevenZFileOptions sevenOptions() {
        return org.apache.commons.compress.archivers.sevenz.SevenZFileOptions.builder().withMaxMemoryLimitInKb(MAX_7Z_MEMORY_KB).build();
    }


    private static Failure sevenFailure(Throwable e) {
        Failure f = new Failure(e instanceof org.apache.commons.compress.MemoryLimitException ? Reason.TOO_LARGE : Reason.BROKEN, null);
        f.initCause(e);
        return f;
    }


    public interface Progress {
        void at(long written);
    }





    public static void unpack(File archive, Plan plan, File target, Progress progress) throws IOException {
        if (target.exists()) throw new IOException("exists: " + target);
        if (!target.mkdirs()) throw new IOException("cannot create " + target);
        long[] written = {0};
        try {
            if ("zip".equals(kind(archive))) {
                try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive)) {
                    for (Map.Entry<String, String> f : plan.files.entrySet()) {
                        java.util.zip.ZipEntry e = zip.getEntry(f.getKey());
                        if (e == null) throw new Failure(Reason.BROKEN, f.getKey());

                        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
                        try (InputStream in = new java.util.zip.CheckedInputStream(zip.getInputStream(e), crc)) {
                            write(in, new File(target, f.getValue()), e.getSize(), written, progress);
                        }
                        if (e.getCrc() != -1 && crc.getValue() != e.getCrc()) throw new Failure(Reason.BROKEN, f.getKey());
                    }
                }
            }
            else {
                try (org.apache.commons.compress.archivers.sevenz.SevenZFile seven = new org.apache.commons.compress.archivers.sevenz.SevenZFile(archive, sevenOptions())) {
                    org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry e;
                    while ((e = seven.getNextEntry()) != null) {
                        if (e.isDirectory()) continue;
                        String below = plan.files.get(e.getName());
                        if (below == null) continue;
                        InputStream in = new SevenZStream(seven);
                        write(in, new File(target, below), e.getSize(), written, progress);
                    }
                }
            }
        }
        catch (Failure f) {
            throw f;
        }
        catch (org.apache.commons.compress.MemoryLimitException | RuntimeException | OutOfMemoryError e) {
            throw sevenFailure(e);
        }
    }

    private static void write(InputStream in, File file, long size, long[] written, Progress progress) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("cannot create " + parent);
        byte[] buffer = new byte[64 * 1024];
        long left = size;
        try (OutputStream out = new FileOutputStream(file)) {
            int n;
            while ((n = in.read(buffer)) > 0) {
                if (n > left) throw new Failure(Reason.BROKEN, file.getName());
                out.write(buffer, 0, n);
                left -= n;
                written[0] += n;
                if (progress != null) progress.at(written[0]);
            }
        }
        if (left != 0) throw new Failure(Reason.BROKEN, file.getName());
    }


    private static final class SevenZStream extends InputStream {
        private final org.apache.commons.compress.archivers.sevenz.SevenZFile seven;

        SevenZStream(org.apache.commons.compress.archivers.sevenz.SevenZFile seven) {
            this.seven = seven;
        }

        @Override
        public int read() throws IOException {
            return seven.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return seven.read(b, off, len);
        }
    }
}
