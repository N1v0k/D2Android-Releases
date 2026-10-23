package com.winlator.d2;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;









public final class D2PlugYTexts {

    public static final String STRINGS_FILE = "LocalizedStrings" + ".ini";

    static final String[] SECTIONS = {"[STR_STATS_UNASSIGN_WITH_LIMIT]", "[STR_STATS_UNASSIGN_WITHOUT_LIMIT]"};
    private static final Pattern LINE = Pattern.compile("([A-Z]{3})=([^\r\n]*)(\r?\n?)");

    private D2PlugYTexts() {}


    public static String statsHint(String ini, String german, String english) {
        StringBuilder out = new StringBuilder(ini.length() + 256);
        boolean inSection = false;
        int at = 0;
        while (at < ini.length()) {
            int end = ini.indexOf('\n', at);
            end = end < 0 ? ini.length() : end + 1;
            String line = ini.substring(at, end);
            String trimmed = line.trim();
            if (trimmed.startsWith("[")) {
                inSection = false;
                for (String section : SECTIONS) if (trimmed.equals(section)) inSection = true;
            }
            Matcher m = inSection ? LINE.matcher(line) : null;
            if (m != null && m.matches()) {
                String text = "DEU".equals(m.group(1)) ? german : english;
                out.append(m.group(1)).append('=').append(text.replace("%", "%%")).append(m.group(3));
            }
            else out.append(line);
            at = end;
        }
        return out.toString();
    }


    public static byte[] statsHint(byte[] file, String german, String english) {
        if (file == null || file.length < 2 || (file[0] & 0xFF) != 0xFF || (file[1] & 0xFF) != 0xFE) return null;
        String ini = new String(file, 2, file.length - 2, StandardCharsets.UTF_16LE);
        byte[] text = statsHint(ini, german, english).getBytes(StandardCharsets.UTF_16LE);
        byte[] out = new byte[text.length + 2];
        out[0] = (byte) 0xFF;
        out[1] = (byte) 0xFE;
        System.arraycopy(text, 0, out, 2, text.length);
        return out;
    }
}
