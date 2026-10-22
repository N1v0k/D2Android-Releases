package com.winlator.d2;

import android.os.SystemClock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public final class D2Trace {
    private static final int MAX_LINES = 4000;
    private static final ArrayDeque<String> lines = new ArrayDeque<>();
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT);
    private static long lastSecond;
    private static int inSecond, skipped;

    private D2Trace() {}

    public static synchronized void log(String text) {
        long second = SystemClock.uptimeMillis() / 1000;
        if (second != lastSecond) {
            if (skipped > 0) add(skipped + " lines left out");
            lastSecond = second;
            inSecond = skipped = 0;
        }
        if (++inSecond > 60) {
            skipped++;
            return;
        }
        add(text);
    }

    private static void add(String text) {
        String line = TIME.format(new Date()) + " " + text;
        lines.addLast(line);
        D2SpieltestPort.trace(line);
        while (lines.size() > MAX_LINES) lines.removeFirst();
    }

    public static synchronized void writeTo(File file) throws IOException {
        try (FileWriter out = new FileWriter(file)) {
            for (String line : lines) out.write(line + "\n");
        }
    }
}
