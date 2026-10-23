package com.winlator.d2;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;













public final class D2Breakpoints {

    public static final int BLOCKS = 256, RECORD = 160;

    public enum Art {

        FCR(100, 75),

        FHR(50, Integer.MAX_VALUE),

        FBR(50, Integer.MAX_VALUE);


        final int base;
        final int cap;

        Art(int base, int cap) {
            this.base = base;
            this.cap = cap;
        }

        public String mode() {
            return this == FCR ? "SC" : this == FHR ? "GH" : "BL";
        }
    }


    public static final class Anim {
        public final int fpd, speed;

        public Anim(int fpd, int speed) {
            this.fpd = fpd;
            this.speed = speed;
        }

        @Override
        public String toString() {
            return fpd + "/" + speed;
        }
    }

    private D2Breakpoints() {
    }






    public static Map<String, Anim> parse(byte[] data) {
        if (data == null) return null;
        Map<String, Anim> out = new HashMap<>();
        int at = 0;
        for (int block = 0; block < BLOCKS; block++) {
            if (at + 4 > data.length) return null;
            long count = u32(data, at);
            at += 4;
            if (count > (data.length - at) / RECORD) return null;
            for (int i = 0; i < count; i++, at += RECORD) {
                int len = 0;
                while (len < 8 && data[at + len] != 0) len++;
                if (len == 0 || len == 8) return null;
                String name = new String(data, at, len, StandardCharsets.US_ASCII);
                long fpd = u32(data, at + 8);
                int speed = u16(data, at + 12), spare = u16(data, at + 14);

                if (fpd <= 0 || fpd > 1000 || spare != 0) return null;
                out.putIfAbsent(name, new Anim((int) fpd, speed));
            }
        }
        return at == data.length ? out : null;
    }


    public static int effective(Art art, int x) {
        if (x <= 0) return 0;
        return Math.min(art.cap, 120 * x / (120 + x));
    }


    public static int frames(Art art, Anim anim, int x) {
        int step = anim.speed * (art.base + effective(art, x)) / 100;
        if (step <= 0) return Integer.MAX_VALUE;
        return (256 * anim.fpd + step - 1) / step - 1;
    }





    public static int[] steps(Art art, Anim anim, int max) {
        List<Integer> out = new ArrayList<>();
        int last = frames(art, anim, 0);
        out.add(last);
        for (int x = 1; x <= max; x++) {
            int f = frames(art, anim, x);
            if (f < last) {
                out.add(x);
                last = f;
            }
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }


    public static int framesOf(int[] table, int x) {
        int f = table[0];
        for (int i = 1; i < table.length && x >= table[i]; i++) f--;
        return f;
    }





    public static final class Table {
        public final String key, cof;
        public final Art art;
        public final int[] values;

        Table(String key, Art art, String cof, int... values) {
            this.key = key;
            this.art = art;
            this.cof = cof;
            this.values = values;
        }
    }


    public static final Map<String, Table> TABLES;

    static {
        Map<String, Table> t = new LinkedHashMap<>();

        put(t, "FCR AM", Art.FCR, "AMSCHTH", 19, 7, 14, 22, 32, 48, 68, 99, 152);
        put(t, "FCR SO", Art.FCR, "SOSCHTH", 13, 9, 20, 37, 63, 105, 200);
        put(t, "FCR SO lightning", Art.FCR, null, 19, 7, 15, 23, 35, 52, 78, 117, 194);
        put(t, "FCR NE", Art.FCR, "NESCHTH", 15, 9, 18, 30, 48, 75, 125);
        put(t, "FCR PA", Art.FCR, "PASCHTH", 15, 9, 18, 30, 48, 75, 125);
        put(t, "FCR BA", Art.FCR, "BASCHTH", 13, 9, 20, 37, 63, 105, 200);
        put(t, "FCR DZ", Art.FCR, "DZSCHTH", 18, 4, 10, 19, 30, 46, 68, 99, 163);
        put(t, "FCR DZ bear", Art.FCR, "TGSCHTH", 16, 7, 15, 26, 40, 63, 99, 163);
        put(t, "FCR DZ wolf", Art.FCR, "40SCHTH", 16, 6, 14, 26, 40, 60, 95, 157);
        put(t, "FCR AI", Art.FCR, "AISCHTH", 16, 8, 16, 27, 42, 65, 102, 174);

        put(t, "FHR AM", Art.FHR, "AMGHHTH", 11, 6, 13, 20, 32, 52, 86, 174, 600);
        put(t, "FHR SO", Art.FHR, "SOGHHTH", 15, 5, 9, 14, 20, 30, 42, 60, 86, 142, 280);
        put(t, "FHR NE", Art.FHR, "NEGHHTH", 13, 5, 10, 16, 26, 39, 56, 86, 152, 377);
        put(t, "FHR PA", Art.FHR, "PAGHHTH", 9, 7, 15, 27, 48, 86, 200);
        put(t, "FHR PA spear", Art.FHR, "PAGH2HT", 13, 3, 7, 13, 20, 32, 48, 75, 129, 280);
        put(t, "FHR PA staff", Art.FHR, "PAGHSTF", 13, 3, 7, 13, 20, 32, 48, 75, 129, 280);
        put(t, "FHR BA", Art.FHR, "BAGHHTH", 9, 7, 15, 27, 48, 86, 200);
        put(t, "FHR DZ", Art.FHR, "DZGHHTH", 13, 5, 10, 16, 26, 39, 56, 86, 152, 377);
        put(t, "FHR DZ swing", Art.FHR, "DZGH1HS", 14, 3, 7, 13, 19, 29, 42, 63, 99, 174, 456);
        put(t, "FHR DZ bear", Art.FHR, "TGGHHTH", 13, 5, 10, 16, 24, 37, 54, 86, 152, 360);
        put(t, "FHR DZ wolf", Art.FHR, "40GHHTH", 7, 9, 20, 42, 86, 280);
        put(t, "FHR AI", Art.FHR, "AIGHHTH", 9, 7, 15, 27, 48, 86, 200);

        put(t, "FBR AM", Art.FBR, "AMBLHTH", 5, 13, 32, 86, 600);
        put(t, "FBR AM swing", Art.FBR, "AMBL1HS", 17, 4, 6, 11, 15, 23, 29, 40, 56, 80, 120, 200, 480);
        put(t, "FBR SO", Art.FBR, "SOBLHTH", 9, 7, 15, 27, 48, 86, 200);
        put(t, "FBR NE", Art.FBR, "NEBLHTH", 11, 6, 13, 20, 32, 52, 86, 174, 600);
        put(t, "FBR PA", Art.FBR, "PABLHTH", 5, 13, 32, 86, 600);
        put(t, "FBR PA holy shield", Art.FBR, null, 2, 86);
        put(t, "FBR BA", Art.FBR, "BABLHTH", 7, 9, 20, 42, 86, 280);
        put(t, "FBR DZ", Art.FBR, "DZBLHTH", 11, 6, 13, 20, 32, 52, 86, 174, 600);
        put(t, "FBR AI", Art.FBR, "AIBLHTH", 5, 13, 32, 86, 600);
        TABLES = Collections.unmodifiableMap(t);
    }

    private static void put(Map<String, Table> t, String key, Art art, String cof, int... values) {
        t.put(key, new Table(key, art, cof, values));
    }





    public static List<String> compare(Map<String, Anim> anims) {
        List<String> out = new ArrayList<>();
        for (Table t : TABLES.values()) {
            if (t.cof == null) continue;
            Anim a = anims.get(t.cof);
            if (a == null) {
                out.add(t.key + ": no record " + t.cof);
                continue;
            }
            int[] got = steps(t.art, a, t.values[t.values.length - 1]);
            if (!Arrays.equals(got, t.values)) {
                out.add(t.key + " (" + t.cof + " " + a + "): computed " + Arrays.toString(got) + ", table " + Arrays.toString(t.values));
            }
        }
        return out;
    }



    public static final int CMD_FRAMES = 28, FRAMES_VERSION = 1;

    public static final String[] WEAPON_CLASSES = {"HTH", "1HS", "2HS", "1HT", "2HT", "STF", "BOW", "XBW", "HT1", "HT2", "1JS",
        "1JT", "1SS", "1ST"};

    public static final String[] CLASS_CODES = {"AM", "SO", "NE", "PA", "BA", "DZ", "AI"};
    public static final int FORM_NONE = 0, FORM_WOLF = 1, FORM_BEAR = 2;
    public static final String[] FORM_CODES = {null, "40", "TG"};

    public static final int[] LIFE_PER_VIT = {12, 8, 8, 12, 16, 8, 12}, MANA_PER_ENERGY = {6, 8, 8, 6, 4, 8, 7};

    public static final int FLAG_NO_CAST = 1;

    public static final int SKILL_LIGHTNING = 49, SKILL_CHAIN_LIGHTNING = 53;


    public static int framesFlags(int angriffSkill) {
        return angriffSkill == SKILL_LIGHTNING || angriffSkill == SKILL_CHAIN_LIGHTNING ? FLAG_NO_CAST : 0;
    }


    public static final int FRAMES_LENGTH = 6 + 3 * 14 * 3;







    public static byte[] frames28(Map<String, Anim> anims, int klasse, int form, int flags) {
        if (klasse < 0 || klasse >= CLASS_CODES.length || form < 0 || form >= FORM_CODES.length) return null;
        if (form != FORM_NONE && klasse != 5) return null;
        String code = form == FORM_NONE ? CLASS_CODES[klasse] : FORM_CODES[form];
        byte[] out = new byte[FRAMES_LENGTH];
        out[0] = FRAMES_VERSION;
        out[1] = (byte) klasse;
        out[2] = (byte) form;
        out[3] = (byte) flags;
        out[4] = (byte) LIFE_PER_VIT[klasse];
        out[5] = (byte) MANA_PER_ENERGY[klasse];
        int at = 6;
        for (Art art : Art.values()) {
            for (String w : WEAPON_CLASSES) {
                Anim a = anims != null ? anims.get(code + art.mode() + w) : null;
                out[at] = (byte) (a != null && a.fpd <= 255 ? a.fpd : 0);
                out[at + 1] = (byte) (a != null ? a.speed : 0);
                out[at + 2] = (byte) (a != null ? a.speed >> 8 : 0);
                at += 3;
            }
        }
        return out;
    }

    private static long u32(byte[] b, int at) {
        return (b[at] & 0xFFL) | (b[at + 1] & 0xFFL) << 8 | (b[at + 2] & 0xFFL) << 16 | (b[at + 3] & 0xFFL) << 24;
    }

    private static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }
}
