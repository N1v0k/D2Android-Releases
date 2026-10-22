package com.winlator.d2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

final class D2GuestOutput {
    static final String FILE = "guest-stderr.log";
    static final String PREV = "guest-stderr.prev.log";
    static final long MAX_BYTES = 4L * 1024 * 1024;
    static final long ZIP_BYTES = 256L * 1024;
    static final long CHECK_MS = 60_000;

    private D2GuestOutput() {}

    static File start(File file, String header) {
        File prev = new File(file.getParentFile(), PREV);
        if (file.isFile()) {
            prev.delete();
            if (!file.renameTo(prev)) file.delete();
        }
        try (FileOutputStream out = new FileOutputStream(file, false)) {
            out.write(("[d2] " + header + "\n").getBytes(StandardCharsets.UTF_8));
            return file;
        }
        catch (IOException e) {
            return null;
        }
    }

    static boolean limit(File file, long max, String when) {
        if (file == null) return false;
        long length = file.length();
        if (length <= max) return false;
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(0);
        }
        catch (IOException e) {
            return false;
        }
        try (FileOutputStream out = new FileOutputStream(file, true)) {
            out.write(("[d2] " + when + ": emptied at " + length + " bytes (limit " + max + ")\n").getBytes(StandardCharsets.UTF_8));
        }
        catch (IOException ignored) {}
        return true;
    }
}
