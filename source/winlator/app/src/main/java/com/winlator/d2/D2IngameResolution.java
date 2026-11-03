package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;













public final class D2IngameResolution {
    private D2IngameResolution() {}


    public static final String CONFIG = "d2dx_sgd2freeres.json";

    public static final int MODE_640 = 0, MODE_800 = 1, MODE_WIDE = 2;



    static final byte[] CHECK = "\"Ingame Resolution Mode\" : 2,".getBytes(StandardCharsets.US_ASCII);

    static final int CHECK_AT = 0x3F78D, AT = 0x3F7A8;


    static final Map<String, String[]> SHIPPED;
    static {
        Map<String, String[]> m = new HashMap<>();

        m.put("c4edd7b6d2b64373ff6f2f8b7d04437a41b14e57b582d7542f09ec1407ae6f89", new String[]{
            "a03a2484f7b97044c8f7fcb73465f1b02139c41b14812a16c1df2ef0ec935eb5",
            "a816d35a5dc3f98606efd72011f4bd6389422c2cfc641461452b9ec0895af634"});

        m.put("53eab88060db53a34cbff59ee2a223828c862058b590e5167a48369fa631a1e6", new String[]{
            "1d38ecdfe97a5d3f534484f554b6af30ac4122fcb17276bb4d8d1bdb7f1e85cb",
            "20f9b939fb992942c857294b47f5456e25b949f32e8488dbaa75733cd416e647"});

        m.put("50e9a58706c2f9f87cafb975e8698d1928725f9f774a78fb954a5d683c5aa313", new String[]{
            "a519137a405b80df352b67bce7cf1c823d104581beddf7b5df744df89b4052ba",
            "c58e07f1757105e0a83440d05e0f779d1ef9497be6dcef6ab6c2215d73edc2c4"});
        SHIPPED = Collections.unmodifiableMap(m);
    }

    private static final Pattern MODE = Pattern.compile("\"Ingame Resolution Mode\"\\s*:\\s*(\\d+)");


    public static int modeIn(String json) {
        if (json == null) return MODE_WIDE;
        Matcher m = MODE.matcher(json);
        if (!m.find()) return MODE_WIDE;
        String digits = m.group(1);
        return digits.equals("0") ? MODE_640 : digits.equals("1") ? MODE_800 : MODE_WIDE;
    }


    public static int read(File startDir) {
        File config = new File(startDir, CONFIG);
        if (!config.isFile()) return MODE_WIDE;
        try {
            return modeIn(new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8));
        }
        catch (IOException | RuntimeException e) {
            return MODE_WIDE;
        }
    }





    public static boolean resetToWide(File startDir) {
        File config = new File(startDir, CONFIG);
        if (!config.isFile()) return true;
        try {
            String json = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);
            if (modeIn(json) == MODE_WIDE) return true;
            Matcher m = MODE.matcher(json);
            if (!m.find()) return true;
            String wide = json.substring(0, m.start(1)) + MODE_WIDE + json.substring(m.end(1));
            File tmp = new File(startDir, CONFIG + ".tmp");
            Files.write(tmp.toPath(), wide.getBytes(StandardCharsets.UTF_8));
            return tmp.renameTo(config) || (config.delete() && tmp.renameTo(config));
        }
        catch (IOException | RuntimeException e) {
            return false;
        }
    }


    public static String size(int mode) {
        return mode == MODE_640 ? "640x480" : mode == MODE_800 ? "800x600" : null;
    }





    static byte[] patch(byte[] dll, int mode, Map<String, String[]> known) {
        if (mode == MODE_WIDE) return dll;
        if (mode != MODE_640 && mode != MODE_800) return null;
        String[] after = known.get(sha256(dll));
        if (after == null) return null;
        if (dll.length < CHECK_AT + CHECK.length
            || !Arrays.equals(Arrays.copyOfRange(dll, CHECK_AT, CHECK_AT + CHECK.length), CHECK)) return null;
        byte[] out = dll.clone();
        out[AT] = (byte) ('0' + mode);
        return sha256(out).equals(after[mode]) ? out : null;
    }


    public static String apply(File dll, int mode) {
        String size = size(mode);
        if (size == null) return "ingame resolution: the wide picture";
        try {
            byte[] out = patch(Files.readAllBytes(dll.toPath()), mode, SHIPPED);
            if (out == null) return "ingame resolution: " + size + " not kept, glide3x.dll not the expected one";
            Files.write(dll.toPath(), out);
            return "ingame resolution: " + size + " kept from the last game";
        }
        catch (IOException e) {
            return "ingame resolution: " + size + " not kept, " + e.getMessage();
        }
    }

    static String sha256(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(64);
            for (byte b : d) sb.append(String.format("%02x", b & 0xFF));
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
