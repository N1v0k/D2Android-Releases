package com.winlator.d2;

import java.nio.charset.StandardCharsets;











public final class D2LevelNames {
    static final int RECORD = 544, ID = 0x00, NAME = 0xF5, NAME_LENGTH = 40;


    private static volatile D2LevelNames current;

    private final String[] keys;
    private final D2Strings strings;

    private D2LevelNames(String[] keys, D2Strings strings) {
        this.keys = keys;
        this.strings = strings;
    }


    public static D2LevelNames read(byte[] levels, D2Strings strings) {
        if (levels == null || levels.length < 4 + 3 * RECORD) return null;
        int count = (levels[0] & 0xFF) | (levels[1] & 0xFF) << 8 | (levels[2] & 0xFF) << 16 | (levels[3] & 0xFF) << 24;
        if (count <= 2 || (long) count * RECORD + 4 != levels.length) return null;
        String[] keys = new String[count];
        for (int i = 0; i < count; i++) {
            int at = 4 + i * RECORD;
            int id = (levels[at + ID] & 0xFF) | (levels[at + ID + 1] & 0xFF) << 8;
            if (id != i) return null;
            int end = at + NAME;
            while (end < at + NAME + NAME_LENGTH && levels[end] != 0) end++;
            keys[i] = new String(levels, at + NAME, end - at - NAME, StandardCharsets.ISO_8859_1);
        }

        if (!"Blood Moor".equals(keys[2])) return null;

        return new D2LevelNames(keys, strings);
    }


    static D2LevelNames load(D2Sprites sprites) {
        try {
            return read(sprites.read("data\\global\\excel\\levels.bin"), new D2Strings(sprites));
        }
        catch (java.io.IOException e) {
            return null;
        }
    }


    public String name(int level) {
        if (level < 0 || level >= keys.length || keys[level].isEmpty()) return null;
        String text = strings != null ? strings.get(keys[level]) : null;
        return text != null && !text.isEmpty() ? text : keys[level];
    }

    static void setCurrent(D2LevelNames names) {
        current = names;
    }


    static D2LevelNames current() {
        return current;
    }
}
