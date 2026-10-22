package com.winlator.d2;

import java.util.Arrays;

public final class D2InstallKeys {
    public enum Check { OK, EMPTY, BAD_CHARACTER, BAD_LENGTH }

    private D2InstallKeys() {}

    public static char[] normalize(char[] typed) {
        char[] out = new char[typed.length];
        int n = 0;
        for (char c : typed) {
            if (c == ' ' || c == '-' || c == '\t') continue;
            out[n++] = Character.toUpperCase(c);
        }
        char[] key = Arrays.copyOf(out, n);
        wipe(out);
        return key;
    }

    public static Check check(char[] key) {
        if (key.length == 0) return Check.EMPTY;
        for (char c : key) {
            if (!((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))) return Check.BAD_CHARACTER;
        }
        return key.length == 16 || key.length == 26 ? Check.OK : Check.BAD_LENGTH;
    }

    public static String masked(char[] key) {
        return "\u2022\u2022\u2022\u2022 (" + key.length + ")";
    }

    public static char[] stdinLine(char[] key) {
        char[] line = Arrays.copyOf(key, key.length + 1);
        line[key.length] = '\n';
        return line;
    }

    public static byte[] lineBytes(char[] key) {
        byte[] b = new byte[key.length + 1];
        for (int i = 0; i < key.length; i++) b[i] = (byte) key[i];
        b[key.length] = '\n';
        return b;
    }

    public static void wipe(byte[] bytes) {
        if (bytes != null) Arrays.fill(bytes, (byte) 0);
    }

    public static void wipe(char[] chars) {
        if (chars != null) Arrays.fill(chars, '\0');
    }
}
