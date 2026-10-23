package com.winlator.d2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public final class IniText {
    private IniText() {}


    public static String set(String text, String section, String key, String value) {
        String newline = text.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\r?\n", -1)));
        if (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) lines.remove(lines.size() - 1);

        String wantedSection = "[" + section.toLowerCase(Locale.ROOT) + "]";
        int sectionStart = -1, sectionEnd = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim().toLowerCase(Locale.ROOT);
            if (trimmed.startsWith("[")) {
                if (sectionStart >= 0) {
                    sectionEnd = i;
                    break;
                }
                if (trimmed.equals(wantedSection)) sectionStart = i;
            }
        }

        String entry = key + "=" + value;
        if (sectionStart < 0) {
            if (!lines.isEmpty() && !lines.get(lines.size() - 1).trim().isEmpty()) lines.add("");
            lines.add("[" + section + "]");
            lines.add(entry);
        }
        else {
            int insertAt = sectionStart + 1;
            boolean replaced = false;
            for (int i = sectionStart + 1; i < sectionEnd; i++) {
                String line = lines.get(i);
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) insertAt = i + 1;
                int eq = trimmed.indexOf('=');
                if (eq > 0 && !trimmed.startsWith(";") && !trimmed.startsWith("#")
                    && trimmed.substring(0, eq).trim().equalsIgnoreCase(key)) {
                    lines.set(i, entry);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) lines.add(insertAt, entry);
        }
        return String.join(newline, lines) + newline;
    }
}
