package com.winlator.d2;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class D2Tabellen {
    public static final int ITEM_RECORD = 424, TYPE_RECORD = 228;
    static final int I_CODE = 0x80, I_SPEED = 0xD8, I_MINDAM = 0xFE, I_MAXDAM = 0xFF, I_2H_MINDAM = 0x102,
        I_2H_MAXDAM = 0x103, I_STRBONUS = 0x106, I_DEXBONUS = 0x108, I_REQSTR = 0x10A, I_REQDEX = 0x10C, I_2HANDED = 0x11C,
        I_TYPE = 0x11E, I_QUEST = 0x12A, I_1OR2HANDED = 0x13D, I_LEVELREQ = 0x13F;
    static final int T_CODE = 0x00, T_EQUIV1 = 0x04, T_EQUIV2 = 0x06, T_THROWABLE = 0x10, T_CLASS = 0x21;
    public static final int CLASS_ANY = 255;

    public static final class Waffe {
        public final String code;
        public final int minDam, maxDam, min2Dam, max2Dam, speed, strBonus, dexBonus, reqStr, reqDex, reqLevel, type;
        public final boolean twoHanded, oneOrTwoHanded, quest;

        Waffe(byte[] b, int at) {
            code = code(b, at + I_CODE);
            minDam = b[at + I_MINDAM] & 0xFF;
            maxDam = b[at + I_MAXDAM] & 0xFF;
            min2Dam = b[at + I_2H_MINDAM] & 0xFF;
            max2Dam = b[at + I_2H_MAXDAM] & 0xFF;
            speed = s32(b, at + I_SPEED);
            strBonus = (short) u16(b, at + I_STRBONUS);
            dexBonus = (short) u16(b, at + I_DEXBONUS);
            reqStr = u16(b, at + I_REQSTR);
            reqDex = u16(b, at + I_REQDEX);
            reqLevel = b[at + I_LEVELREQ] & 0xFF;
            type = u16(b, at + I_TYPE);
            twoHanded = b[at + I_2HANDED] != 0;
            oneOrTwoHanded = b[at + I_1OR2HANDED] != 0;
            quest = b[at + I_QUEST] != 0;
        }
    }

    public static final class Typ {
        public final String code;
        public final int equiv1, equiv2, klasse;
        public final boolean throwable;

        Typ(byte[] b, int at) {
            code = code(b, at + T_CODE);
            equiv1 = u16(b, at + T_EQUIV1);
            equiv2 = u16(b, at + T_EQUIV2);
            throwable = b[at + T_THROWABLE] != 0;
            klasse = b[at + T_CLASS] & 0xFF;
        }
    }

    private final Map<String, Waffe> waffen = new HashMap<>();
    private final Typ[] typen;

    private D2Tabellen(Typ[] typen) {
        this.typen = typen;
    }

    public static D2Tabellen lesen(byte[] weapons, byte[] itemtypes) {
        int wn = count(weapons, ITEM_RECORD), tn = count(itemtypes, TYPE_RECORD);
        if (wn <= 0 || tn <= 0) return null;
        Typ[] typen = new Typ[tn];
        for (int i = 0; i < tn; i++) typen[i] = new Typ(itemtypes, 4 + i * TYPE_RECORD);
        D2Tabellen t = new D2Tabellen(typen);
        for (int i = 0; i < wn; i++) {
            Waffe w = new Waffe(weapons, 4 + i * ITEM_RECORD);
            if (!w.code.trim().isEmpty()) t.waffen.put(w.code.trim(), w);
        }
        Waffe hax = t.waffe("hax");
        if (hax == null || hax.minDam != 3 || hax.maxDam != 6 || !t.ist(hax.type, "axe")) return null;
        return t;
    }

    public Waffe waffe(String code) {
        return code == null ? null : waffen.get(code.trim());
    }

    public boolean ist(int type, String code) {
        return ist(type, code.trim(), 0);
    }

    private boolean ist(int type, String code, int depth) {
        if (type <= 0 || type >= typen.length || depth > 8) return false;
        Typ t = typen[type];
        return t.code.trim().equals(code) || ist(t.equiv1, code, depth + 1) || ist(t.equiv2, code, depth + 1);
    }

    public int klasse(int type) {
        return klasse(type, 0);
    }

    private int klasse(int type, int depth) {
        if (type <= 0 || type >= typen.length || depth > 8) return CLASS_ANY;
        Typ t = typen[type];
        if (t.klasse != CLASS_ANY) return t.klasse;
        int k = klasse(t.equiv1, depth + 1);
        return k != CLASS_ANY ? k : klasse(t.equiv2, depth + 1);
    }

    public boolean fernkampf(int type) {
        return ist(type, "miss") || ist(type, "thro") || (type > 0 && type < typen.length && typen[type].throwable);
    }

    private static int count(byte[] b, int record) {
        if (b == null || b.length < 4) return -1;
        int n = s32(b, 0);
        return n > 0 && (long) n * record + 4 == b.length ? n : -1;
    }

    static String code(byte[] b, int at) {
        return new String(b, at, 4, StandardCharsets.ISO_8859_1);
    }

    static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }

    static int s32(byte[] b, int at) {
        return u16(b, at) | u16(b, at + 2) << 16;
    }
}
