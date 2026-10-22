package com.winlator.d2;

import java.io.File;

public final class D2DxvkLog {
    public static final String DIR = "dxvk";
    public static final String PREVIOUS = "previous";
    public static final String LINK = "home/xuser/.dxvk-log";
    public static final String WINE_PATH = "Z:\\home\\xuser\\.dxvk-log";

    public static final long MAX_BYTES = 1024L * 1024;
    public static final long ZIP_BYTES = 128L * 1024;

    private D2DxvkLog() {}

    public static int limit(File dir, long max) {
        File[] files = dir.listFiles();
        int emptied = 0;
        if (files == null) return 0;
        for (File file : files) {
            if (!file.isFile() || file.length() <= max) continue;
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw")) {
                raf.setLength(0);
                emptied++;
            }
            catch (java.io.IOException ignored) {
            }
        }
        return emptied;
    }

    public static String tail(File dir, int n) {
        File[] files = dir.listFiles();
        if (files == null) return "";
        StringBuilder out = new StringBuilder();
        java.util.Arrays.sort(files);
        for (File file : files) {
            if (!file.isFile()) continue;
            String text;
            try (java.io.RandomAccessFile in = new java.io.RandomAccessFile(file, "r")) {
                long length = in.length();
                int size = (int) Math.min(length, 16384);
                byte[] end = new byte[size];
                in.seek(length - size);
                in.readFully(end);
                text = new String(end, java.nio.charset.StandardCharsets.UTF_8).replace("\0", "");
            }
            catch (java.io.IOException e) {
                continue;
            }
            String[] lines = text.trim().isEmpty() ? new String[0] : text.trim().split("\n");
            for (int i = Math.max(0, lines.length - n); i < lines.length; i++) {
                out.append(file.getName()).append(": ").append(lines[i].trim()).append('\n');
            }
        }
        return out.toString();
    }

    public static void rotate(File dir) {
        File previous = new File(dir, PREVIOUS);
        File[] older = previous.listFiles();
        if (older != null) for (File file : older) file.delete();
        previous.mkdirs();
        File[] last = dir.listFiles();
        if (last != null) for (File file : last) if (file.isFile()) file.renameTo(new File(previous, file.getName()));
    }
}
