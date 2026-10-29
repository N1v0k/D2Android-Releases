package com.winlator.d2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;






public final class D2Strings {

    private static final String[] LANGUAGES = {"eng", "esp", "deu", "fra", "por", "ita", "jpn", "kor", "sin", "chi", "pol", "rus"};
    private final byte[][] tables = new byte[3][];
    private final String language;

    public D2Strings(D2Sprites sprites) throws IOException {
        this(sprites, null);
    }





    public D2Strings(D2Sprites sprites, String fixedLanguage) throws IOException {

        String wanted = fixedLanguage;
        if (wanted == null) {
            byte[] use = sprites.read("data\\local\\use");
            int index = use != null && use.length > 0 ? use[0] & 0xFF : 0;
            wanted = index < LANGUAGES.length ? LANGUAGES[index] : "eng";
        }
        String found = null;
        for (String lng : new String[]{wanted, "eng"}) {
            byte[] base = sprites.read("data\\local\\lng\\" + lng + "\\string.tbl");
            byte[] expansion = sprites.read("data\\local\\lng\\" + lng + "\\expansionstring.tbl");

            if (base == null || expansion == null) continue;
            tables[0] = base;
            tables[1] = sprites.read("data\\local\\lng\\" + lng + "\\patchstring.tbl");
            tables[2] = expansion;
            found = lng;
            break;
        }
        language = found;
    }


    D2Strings(byte[] base, byte[] patch, byte[] expansion, String language) {
        tables[0] = base;
        tables[1] = patch;
        tables[2] = expansion;
        this.language = language;
    }






    public static void rememberGameLanguage(android.content.Context context, java.io.File gameDir) {
        try (D2Sprites sprites = new D2Sprites(gameDir)) {
            byte[] use = sprites.read("data\\local\\use");
            int index = use != null && use.length > 0 ? use[0] & 0xFF : 0;
            D2Settings.setGameLanguage(context, index < LANGUAGES.length ? LANGUAGES[index] : "eng");
        }
        catch (Throwable e) {
            android.util.Log.w("D2Strings", "game language not read", e);
        }
    }


    public String language() {
        return language;
    }





    public String get(String key) {
        byte[] wanted = key.getBytes(Charset.forName("windows-1252"));
        for (int table : new int[]{1, 2, 0}) {
            byte[] d = tables[table];
            if (d == null || d.length < 21) continue;


            int count = u16(d, 2), slots = s32(d, 4), base = 21 + count * 2;
            if (slots <= 0) continue;
            for (int i = 0; i < slots; i++) {
                int node = base + i * 17;
                if (node + 17 > d.length || d[node] == 0) continue;
                int keyAt = s32(d, node + 7);
                if (keyAt < 0 || keyAt + wanted.length >= d.length || d[keyAt + wanted.length] != 0) continue;
                boolean same = true;
                for (int j = 0; j < wanted.length && same; j++) same = d[keyAt + j] == wanted[j];
                if (!same) continue;
                int at = s32(d, node + 11), length = u16(d, node + 15), end = at;
                if (at < 0 || at >= d.length) return null;
                while (end < d.length && end - at < Math.max(length, 1) && d[end] != 0) end++;
                return text(d, at, end);
            }
        }
        return null;
    }


    public String get(int id) {
        if (id < 0) return null;
        int table = id < 10000 ? 0 : (id < 20000 ? 1 : 2);
        byte[] d = tables[table];
        int index = id - table * 10000;
        if (d == null || d.length < 21) return null;
        int count = u16(d, 2);
        if (index >= count || 21 + index * 2 + 2 > d.length) return null;
        int node = 21 + count * 2 + u16(d, 21 + index * 2) * 17;
        if (node + 17 > d.length || d[node] == 0) return null;
        int at = s32(d, node + 11), length = u16(d, node + 15);
        if (at < 0 || at >= d.length) return null;
        int end = at;
        while (end < d.length && end - at < Math.max(length, 1) && d[end] != 0) end++;
        return text(d, at, end);
    }






    private static String text(byte[] d, int at, int end) {
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(d, at, end - at)).toString();
        }
        catch (CharacterCodingException e) {
            text = new String(d, at, end - at, Charset.forName("windows-1252"));
        }
        return text.replaceAll("ÿc.", "");
    }

    private static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }

    private static int s32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
