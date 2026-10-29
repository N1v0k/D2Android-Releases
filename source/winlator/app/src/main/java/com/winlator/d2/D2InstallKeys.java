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


    public enum Verdict {

        OK,

        SWAPPED,

        WARN
    }

    public static Verdict verdict(D2CdKey.Kind classic, D2CdKey.Kind lod) {
        if (classic == D2CdKey.Kind.CLASSIC && lod == D2CdKey.Kind.LOD) return Verdict.OK;
        if (classic == D2CdKey.Kind.LOD && lod == D2CdKey.Kind.CLASSIC) return Verdict.SWAPPED;
        return Verdict.WARN;
    }


    public enum Problem { NONE, CHARACTER, CHECKSUM, OTHER_PRODUCT, IS_CLASSIC, IS_LOD, EMPTY, LENGTH }


    public static Problem problem(D2CdKey.Kind kind, boolean classicField) {
        switch (kind) {
            case CLASSIC: return classicField ? Problem.NONE : Problem.IS_CLASSIC;
            case LOD: return classicField ? Problem.IS_LOD : Problem.NONE;
            case BAD_CHARACTER: return Problem.CHARACTER;
            case BAD_CHECKSUM: return Problem.CHECKSUM;
            case EMPTY: return Problem.EMPTY;
            case BAD_LENGTH: return Problem.LENGTH;
            default: return Problem.OTHER_PRODUCT;
        }
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
