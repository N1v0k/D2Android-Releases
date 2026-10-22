package com.winlator.d2;

final class D2FontText {
    static final String MAPPED = "“”„‚‘’–—→←…≡";
    private static final String[] STAND_IN = {"\"", "\"", "\"", "'", "'", "'", "-", "-", ">", "<", "...", "="};
    private static final String UNKNOWN = "?";

    private D2FontText() {
    }

    static String map(String text) {
        int i = 0, n = text.length();
        while (i < n && text.charAt(i) <= 255) i++;
        if (i == n) return text;
        StringBuilder b = new StringBuilder(n + 8).append(text, 0, i);
        for (; i < n; i++) {
            char c = text.charAt(i);
            if (c <= 255) {
                b.append(c);
                continue;
            }
            int at = MAPPED.indexOf(c);
            if (at >= 0) {
                b.append(STAND_IN[at]);
                continue;
            }
            b.append(UNKNOWN);
            if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(text.charAt(i + 1))) i++;
        }
        return b.toString();
    }

    static int width(int[] glyphWidths, String text) {
        text = map(text);
        int w = 0;
        for (int i = 0; i < text.length(); i++) w += glyphWidths[text.charAt(i)];
        return w;
    }
}
