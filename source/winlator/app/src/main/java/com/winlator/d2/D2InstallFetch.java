package com.winlator.d2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;








public final class D2InstallFetch {
    public static final String DOWNLOADER_HOST = "downloader.battle.net";

    public static final long DOWNLOADER_MIN = 512L * 1024, DOWNLOADER_MAX = 32L * 1024 * 1024;


    public static class Refused extends IOException {
        public Refused(String message) {
            super(message);
        }
    }

    private D2InstallFetch() {}


    public static void fetchDownloader(String url, File target) throws IOException {
        URL u = new URL(url);
        if (!"https".equals(u.getProtocol()) || !DOWNLOADER_HOST.equals(u.getHost())) throw new Refused("not from https://" + DOWNLOADER_HOST);
        File part = new File(target.getPath() + ".part");
        HttpURLConnection c = (HttpURLConnection) u.openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        try {
            int code = c.getResponseCode();

            if (code != HttpURLConnection.HTTP_OK) throw new Refused("HTTP " + code);
            File dir = target.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
            try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(part)) {
                copy(in, out, DOWNLOADER_MAX);
            }
        }
        finally {
            c.disconnect();
        }
        try {
            checkDownloader(part);
        }
        catch (IOException e) {
            part.delete();
            throw e;
        }
        if (target.exists() && !target.delete()) throw new IOException("cannot replace " + target);
        if (!part.renameTo(target)) throw new IOException("cannot rename " + part);
    }


    public static final String PATCH_URL = "http://ftp.blizzard.com/pub/diablo2exp/patches/PC/LODPatch_113d.exe";
    public static final String PATCH_SHA256 = "918fa5141a4a98f826b1f1f5ea69bf6e20208162d67748baceceaff96e49ae30";


    public static void fetchPatch(File target) throws IOException {
        if (target.isFile() && PATCH_SHA256.equals(sha256(target))) return;
        File part = new File(target.getPath() + ".part");
        HttpURLConnection c = (HttpURLConnection) new URL(PATCH_URL).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setConnectTimeout(20000);
        c.setReadTimeout(30000);
        try {
            int code = c.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new Refused("HTTP " + code);
            File dir = target.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) throw new IOException("cannot create " + dir);
            try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(part)) {
                copy(in, out, DOWNLOADER_MAX);
            }
        }
        finally {
            c.disconnect();
        }
        if (!PATCH_SHA256.equals(sha256(part))) {
            part.delete();
            throw new Refused("checksum differs: LODPatch_113d.exe");
        }
        if (target.exists() && !target.delete()) throw new IOException("cannot replace " + target);
        if (!part.renameTo(target)) throw new IOException("cannot rename " + part);
    }


    public static void checkDownloader(File file) throws IOException {
        long size = file.length();
        if (size < DOWNLOADER_MIN || size > DOWNLOADER_MAX) throw new Refused("implausible size " + size);
        try (InputStream in = new FileInputStream(file)) {
            if (in.read() != 'M' || in.read() != 'Z') throw new Refused("no MZ header");
        }
    }


    public static Map<String, String> parseChecksums(String text) {
        Map<String, String> sums = new LinkedHashMap<>();
        for (String line : text.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int gap = line.indexOf("  ");
            if (gap != 64) throw new IllegalArgumentException("bad checksum line: " + line);
            sums.put(line.substring(66), line.substring(0, 64).toLowerCase(Locale.ROOT));
        }
        return sums;
    }


    public static void verify(File base, Map<String, String> sums) throws IOException {
        for (Map.Entry<String, String> e : sums.entrySet()) {
            File f = new File(base, e.getKey());
            if (!f.isFile()) throw new Refused("missing " + e.getKey());
            if (!sha256(f).equals(e.getValue())) throw new Refused("checksum differs: " + e.getKey());
        }
    }






    public static void verifyProduct(File productDir, Map<String, String> sums, String code) throws IOException {
        verifyProduct(productDir, sums, code, null);
    }





    public static void verifyProduct(File productDir, Map<String, String> sums, String code, String locale) throws IOException {
        String prefix = code + "/";
        int checked = 0;
        for (Map.Entry<String, String> e : sums.entrySet()) {
            if (!e.getKey().startsWith(prefix)) continue;
            String name = e.getKey().substring(prefix.length());
            File f = find(productDir, name, 2, locale);
            if (f == null) throw new Refused("missing " + e.getKey());
            if (!sha256(f).equals(e.getValue())) throw new Refused("checksum differs: " + e.getKey());
            checked++;
        }
        if (checked == 0) throw new Refused("no checksums for " + code);
    }


    public static File find(File dir, String name, int depth, String locale) {
        File direct = new File(dir, name);
        if (direct.isFile()) return direct;
        if (depth == 0) return null;
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) return null;
        java.util.Arrays.sort(subs, (a, b) -> {
            boolean pa = locale != null && a.getName().endsWith(locale), pb = locale != null && b.getName().endsWith(locale);
            return pa == pb ? a.getName().compareTo(b.getName()) : pa ? -1 : 1;
        });
        for (File sub : subs) {
            File f = find(sub, name, depth - 1, locale);
            if (f != null) return f;
        }
        return null;
    }

    public static String sha256(File file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        byte[] buf = new byte[1 << 16];
        try (InputStream in = new FileInputStream(file)) {
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte b : md.digest()) hex.append(String.format(Locale.ROOT, "%02x", b));
        return hex.toString();
    }

    private static void copy(InputStream in, OutputStream out, long max) throws IOException {
        byte[] buf = new byte[1 << 16];
        long total = 0;
        for (int n; (n = in.read(buf)) > 0; ) {
            total += n;
            if (total > max) throw new Refused("larger than " + max + " bytes");
            out.write(buf, 0, n);
        }
    }
}
