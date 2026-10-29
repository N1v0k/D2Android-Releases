package com.winlator.d2;

import java.util.Arrays;

















public final class D2CdKey {

    public enum Kind {
        EMPTY,

        BAD_LENGTH,

        BAD_CHARACTER,

        BAD_CHECKSUM,

        CLASSIC,

        LOD,

        OTHER_PRODUCT
    }

    public static final class Result {
        public final Kind kind;

        public final char character;

        public final int product;

        Result(Kind kind, char character, int product) {
            this.kind = kind;
            this.character = character;
            this.product = product;
        }
    }


    static final String ALPHABET_16 = "246789BCDEFGHJKMNPRTVWXZ";

    static final String ALPHABET_26 = "246789BCDEFGHJKMNPRTVWXYZ";

    public static final String NEVER = "0 1 3 5 A I L O Q S U";


    static final int[] CLASSIC_16 = {0x06, 0x07}, LOD_16 = {0x0A, 0x0C};
    static final int CLASSIC_26 = 0x18, LOD_26 = 0x19;


    private static final String SUBSTITUTION = ""
        + "947FDA3B12C86E509B548F1E7032A6DCCE149FABD6087253"
        + "B25ED3901F7CA6486245B8CEDF71A03954EC76DAF2910B83"
        + "C78FB059DA6E24313AE81B542FDC6790CD1F8E5B3A907246"
        + "DA7E16B8FC5230493E75BF8C1A4D0692B69418AD7E0CF235"
        + "C78D3B0E6F94A152C6D9B012F734AE853615BC80FE947AD2"
        + "A7BF280DEC169354ABD4385910FC7E26B4DF163E7AC89250"
        + "96701AD23EFC5B48DE56198C2F37B40A9F4016AE237D5B8C"
        + "3E1A2C84B7D0F69572C6A8B0F43E91D5C459A28D3F1E67B0"
        + "A8ED9F30461C7B253C4A2FDE705816B9AC109EDB37F85246"
        + "EA18765C2F0D3B4938E079FC16D25AB43AC4DB9EF6172058";
    private static final byte[] SUB = new byte[480];

    static {
        for (int i = 0; i < SUB.length; i++) SUB[i] = (byte) Character.digit(SUBSTITUTION.charAt(i), 16);
    }

    private D2CdKey() {}


    public static Result check(char[] key) {
        if (key == null || key.length == 0) return new Result(Kind.EMPTY, '\0', -1);
        String alphabet = key.length == 16 ? ALPHABET_16 : ALPHABET_26;
        for (char c : key) {
            if (alphabet.indexOf(c) < 0) {

                if (ALPHABET_26.indexOf(c) < 0 || key.length == 16) return new Result(Kind.BAD_CHARACTER, c, -1);
            }
        }
        if (key.length != 16 && key.length != 26) return new Result(Kind.BAD_LENGTH, '\0', -1);
        int product = key.length == 16 ? product16(key) : product26(key);
        if (product < 0) return new Result(Kind.BAD_CHECKSUM, '\0', -1);
        if (key.length == 16 ? contains(CLASSIC_16, product) : product == CLASSIC_26) return new Result(Kind.CLASSIC, '\0', product);
        if (key.length == 16 ? contains(LOD_16, product) : product == LOD_26) return new Result(Kind.LOD, '\0', product);
        return new Result(Kind.OTHER_PRODUCT, '\0', product);
    }

    private static boolean contains(int[] list, int value) {
        for (int v : list) if (v == value) return true;
        return false;
    }


    static int product16(char[] key) {
        int[] hex = new int[16];
        try {

            int check = 0;
            for (int pair = 0; pair < 8; pair++) {
                int n = ALPHABET_16.indexOf(key[2 * pair]) * 24 + ALPHABET_16.indexOf(key[2 * pair + 1]);
                if (n >= 0x100) {
                    n -= 0x100;
                    check |= 1 << pair;
                }
                hex[2 * pair] = (n >> 4) & 0xF;
                hex[2 * pair + 1] = n & 0xF;
            }
            int sum = 3;
            for (int digit : hex) sum += digit ^ (sum * 2);
            if ((sum & 0xFF) != check) return -1;

            for (int i = 15; i >= 0; i--) {
                int partner = (i > 8 ? i - 9 : 15 - (8 - i)) & 0xF;
                int t = hex[i];
                hex[i] = hex[partner];
                hex[partner] = t;
            }


            int salt = 0x13AC9741;
            for (int i = 15; i >= 0; i--) {
                if (hex[i] <= 7) {
                    hex[i] ^= salt & 7;
                    salt >>>= 3;
                }
                else if (hex[i] <= 9) hex[i] ^= i & 1;
            }
            return hex[0] << 4 | hex[1];
        }
        finally {
            Arrays.fill(hex, 0);
        }
    }


    static int product26(char[] key) {
        int[] digits = new int[52];
        int[] v = new int[4];
        int[] copy = new int[4];
        try {

            int b = 0x21;
            for (char c : key) {
                int a = (b + 0x7B5) % 52;
                b = (a + 0x7B5) % 52;
                int value = ALPHABET_26.indexOf(c);
                digits[a] = value / 5;
                digits[b] = value % 5;
            }



            for (int i = 51; i >= 0; i--) {
                int carry = digits[i];
                for (int k = 3; k >= 0; k--) {
                    long times5 = (v[k] & 0xFFFFFFFFL) * 5;
                    v[k] = carry + (int) times5;
                    carry = (int) (times5 >>> 32);
                }
            }

            int top = 29;
            for (int row = 29; row >= 0; row--) {
                int base = row * 16;
                int c = nibble(v, top);
                if (row < 29) for (int j = 29; j > top; j--) c = SUB[(nibble(v, j) ^ SUB[c + base]) + base];
                for (int j = top - 1; j >= 0; j--) c = SUB[(nibble(v, j) ^ SUB[c + base]) + base];
                int shift = (top & 7) << 2, word = 3 - (top >> 3);
                v[word] = (v[word] & ~(0xF << shift)) | ((SUB[c + base] & 0xF) << shift);
                top--;
            }

            System.arraycopy(v, 0, copy, 0, 4);
            int from = 0;
            for (int i = 0; i < 120; i++) {
                int bit = (copy[3 - (from >> 5)] >>> (from & 31)) & 1;
                int word = 3 - (i >> 5), shift = i & 31;
                v[word] = (v[word] & ~(1 << shift)) | (bit << shift);
                from += 11;
                if (from >= 120) from -= 120;
            }
            return v[0] >> 10;
        }
        finally {
            Arrays.fill(digits, 0);
            Arrays.fill(v, 0);
            Arrays.fill(copy, 0);
        }
    }

    private static int nibble(int[] v, int index) {
        return (v[3 - (index >> 3)] >>> ((index & 7) << 2)) & 0xF;
    }
}
