package com.winlator.d2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GameVersion {
    public enum Status { OK, NOT_A_GAME_DIR, WRONG_VERSION, MODIFIED, MISSING, NOT_WRITABLE }

    public static final class Result {
        public final Status status;
        public final File dir;
        public final String detail;

        Result(Status status, File dir, String detail) {
            this.status = status;
            this.dir = dir;
            this.detail = detail;
        }

        public boolean ok() {
            return status == Status.OK;
        }

        @Override
        public String toString() {
            return status + (detail != null ? " (" + detail + ")" : "") + " " + dir;
        }
    }

    static final String[] HASHED = {"Game.exe", "Bnclient.dll", "D2CMP.dll", "D2Client.dll", "D2Common.dll",
        "D2DDraw.dll", "D2Direct3D.dll", "D2Game.dll", "D2Gdi.dll", "D2Glide.dll", "D2Lang.dll", "D2Launch.dll",
        "D2MCPClient.dll", "D2Multi.dll", "D2Net.dll", "D2Win.dll", "D2gfx.dll", "D2sound.dll", "Fog.dll", "Storm.dll"};
    static final String[] OPTIONAL = {"D2VidTst.exe", "d2video.mpq"};
    private static final int VS_FIXEDFILEINFO_SIGNATURE = 0xFEEF04BD;
    private static final long MAX_EXE_SIZE = 16 << 20;

    private final Map<String, String> reference;
    private final List<String> names = new ArrayList<>();

    public GameVersion(Map<String, String> reference) {
        this.reference = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : reference.entrySet()) {
            this.reference.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().toLowerCase(Locale.ROOT));
            names.add(entry.getKey());
        }
    }

    public static Map<String, String> parseList(InputStream in) throws IOException {
        Map<String, String> list = new LinkedHashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\s+", 2);
            if (parts.length == 2 && parts[0].matches("[0-9a-fA-F]{64}")) list.put(parts[1].trim(), parts[0]);
        }
        return list;
    }

    public Result check(File dir) {
        return check(dir, true);
    }

    public Result check(File dir, boolean needWritable) {
        if (dir == null || !dir.isDirectory()) return new Result(Status.NOT_A_GAME_DIR, dir, null);
        Map<String, File> files = list(dir);
        if (!files.containsKey("game.exe")) {
            File found = null;
            int count = 0;
            for (File sub : files.values()) {
                if (sub.isDirectory() && list(sub).containsKey("game.exe")) {
                    found = sub;
                    count++;
                }
            }
            if (count != 1) return new Result(Status.NOT_A_GAME_DIR, dir, null);
            dir = found;
            files = list(dir);
        }
        try {
            File gameExe = files.get("game.exe");
            List<String> modified = new ArrayList<>();
            if (!matches(gameExe, "game.exe")) {
                String version = versionName(readVersion(gameExe));
                if (!"1.13d".equals(version)) return new Result(Status.WRONG_VERSION, dir, version);
                modified.add(gameExe.getName());
            }
            List<String> missing = new ArrayList<>();
            for (String name : names) {
                if (isOptional(name)) continue;
                File file = files.get(name.toLowerCase(Locale.ROOT));
                if (file == null || !file.isFile() || file.length() == 0) missing.add(name);
            }
            if (!missing.isEmpty()) return new Result(Status.MISSING, dir, String.join(", ", missing));
            for (String name : HASHED) {
                String key = name.toLowerCase(Locale.ROOT);
                if (key.equals("game.exe")) continue;
                if (reference.containsKey(key) && !matches(files.get(key), key)) modified.add(files.get(key).getName());
            }
            if (needWritable && !writable(dir)) return new Result(Status.NOT_WRITABLE, dir, null);
            if (!modified.isEmpty()) return new Result(Status.MODIFIED, dir, String.join(", ", modified));
        }
        catch (IOException e) {
            return new Result(Status.NOT_A_GAME_DIR, dir, e.getMessage());
        }
        return new Result(Status.OK, dir, "1.13d");
    }

    private static boolean isOptional(String name) {
        for (String optional : OPTIONAL) if (optional.equalsIgnoreCase(name)) return true;
        return false;
    }

    private boolean matches(File file, String key) throws IOException {
        String want = reference.get(key);
        return want != null && file != null && want.equals(sha256(file));
    }

    static boolean writable(File dir) {
        File save = new File(dir, "save");
        if (!save.isDirectory() && !save.mkdirs()) return false;
        File probe = new File(save, ".d2android-probe");
        try {
            if (!probe.exists() && !probe.createNewFile()) return false;
            return probe.delete();
        }
        catch (IOException | SecurityException e) {
            return false;
        }
    }

    private static Map<String, File> list(File dir) {
        Map<String, File> map = new LinkedHashMap<>();
        File[] files = dir.listFiles();
        if (files == null) return map;
        java.util.Arrays.sort(files);
        for (File file : files) map.put(file.getName().toLowerCase(Locale.ROOT), file);
        return map;
    }

    public static String sha256(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1 << 16];
            int n;
            while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", b & 0xFF));
            return hex.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    static String readVersion(File exe) throws IOException {
        if (exe.length() > MAX_EXE_SIZE) return null;
        byte[] data = new byte[(int) exe.length()];
        try (InputStream in = new FileInputStream(exe)) {
            int at = 0, n;
            while (at < data.length && (n = in.read(data, at, data.length - at)) > 0) at += n;
        }
        return versionOf(data);
    }

    static String versionOf(byte[] data) {
        for (int i = 0; i + 16 <= data.length; i += 4) {
            if (le32(data, i) != VS_FIXEDFILEINFO_SIGNATURE) continue;
            int ms = le32(data, i + 8), ls = le32(data, i + 12);
            return (ms >>> 16) + "." + (ms & 0xFFFF) + "." + (ls >>> 16) + "." + (ls & 0xFFFF);
        }
        return null;
    }

    public static String versionName(String fileVersion) {
        if (fileVersion == null) return null;
        switch (fileVersion) {
            case "1.0.13.64": return "1.13d";
            case "1.0.13.60": return "1.13c";
            case "1.14.0.64": return "1.14a";
            case "1.14.1.68": return "1.14b";
            case "1.14.2.70": return "1.14c";
            case "1.14.3.71": return "1.14d";
            default:
                if (fileVersion.startsWith("1.0.12.")) return "1.12";
                if (fileVersion.startsWith("1.0.11.")) return "1.11";
                if (fileVersion.startsWith("1.0.10.")) return "1.10";
                return fileVersion;
        }
    }

    private static int le32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
