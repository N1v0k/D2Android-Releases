package com.winlator.d2;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;







final class D2GameLogs {

    static final int DAYS = 2;

    static final int MAX_BYTES = 64 * 1024;

    static final int READ_BYTES = 2 * 1024 * 1024;

    private static final Pattern NAME = Pattern.compile("D2\\d{6}\\.txt");

    private static final Pattern DUMP = Pattern.compile("^\\s+[0-9A-Fa-f]{8}: .*", Pattern.DOTALL);


    private static final String[] PRIVATE = {"Local IP:", "Computer name:", "COMPUTER NAME:", "USER NAME:"};

    private static final String STARTUP = " Realm startup at ";


    private D2GameLogs() {}


    static List<String> newest(String[] names) {
        List<String> logs = new ArrayList<>();
        if (names != null) for (String name : names) if (NAME.matcher(name).matches()) logs.add(name);
        String[] sorted = logs.toArray(new String[0]);
        Arrays.sort(sorted);
        return Arrays.asList(sorted).subList(Math.max(0, sorted.length - DAYS), sorted.length);
    }








    static byte[] compact(byte[] tail, boolean cut) {
        String text = new String(tail, StandardCharsets.ISO_8859_1);
        String[] lines = text.split("\n", -1);
        StringBuilder b = new StringBuilder();
        int dumpLines = 0;
        for (int i = cut ? 1 : 0; i < lines.length; i++) {
            String line = lines[i];
            if (DUMP.matcher(line).matches()) {
                dumpLines++;
                continue;
            }
            if (dumpLines > 0) {
                b.append("[").append(dumpLines).append(" Zeilen Speicherauszug weggelassen]\n");
                dumpLines = 0;
            }
            b.append(redact(line));
            if (i < lines.length - 1) b.append('\n');
        }
        if (dumpLines > 0) b.append("[").append(dumpLines).append(" Zeilen Speicherauszug weggelassen]\n");
        String out = b.toString();
        if (out.length() > MAX_BYTES) {
            int from = out.length() - MAX_BYTES;
            int nl = out.indexOf('\n', from);
            from = nl >= 0 ? nl + 1 : from;
            out = "[" + from + " Zeichen am Anfang weggelassen]\n" + out.substring(from);
        }
        else if (cut) out = "[Anfang der Datei weggelassen]\n" + out;
        return out.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String redact(String line) {
        int startup = line.indexOf(STARTUP);
        if (startup > 0) return "(entfernt)" + line.substring(startup);
        for (String key : PRIVATE) {
            int at = line.indexOf(key);
            if (at >= 0) return line.substring(0, at + key.length()) + " (entfernt)" + (line.endsWith("\r") ? "\r" : "");
        }
        return line;
    }
}
