package com.winlator.d2;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;











public final class D2Gegenstaende {
    public static final int RECORD = 424;
    static final int I_INVFILE = 0x20, I_CODE = 0x80, I_NORM = 0x84, I_UBER = 0x88, I_ULTRA = 0x8C, I_NAME = 0xF4,
        I_WIDTH = 0x10F, I_HEIGHT = 0x110, I_TYPE = 0x11E, I_TYPE2 = 0x120, I_SOCKETS = 0x138, I_LEVELREQ = 0x13F;

    public static final int NORMAL = 0, EXCEPTIONAL = 1, ELITE = 2, NO_TIER = 255;


    public static final class Item {

        public final int index, nameId, width, height, type, type2, maxSockets, levelReq;
        public final String code, invFile, normCode, uberCode, ultraCode;

        Item(byte[] b, int at, int index) {
            this.index = index;
            code = D2Rezepte.code(b, at + I_CODE);
            normCode = D2Rezepte.code(b, at + I_NORM);
            uberCode = D2Rezepte.code(b, at + I_UBER);
            ultraCode = D2Rezepte.code(b, at + I_ULTRA);
            invFile = cString(b, at + I_INVFILE, 32);
            nameId = D2Tabellen.u16(b, at + I_NAME);
            width = b[at + I_WIDTH] & 0xFF;
            height = b[at + I_HEIGHT] & 0xFF;
            type = D2Tabellen.u16(b, at + I_TYPE);
            type2 = D2Tabellen.u16(b, at + I_TYPE2);
            maxSockets = b[at + I_SOCKETS] & 0xFF;
            levelReq = b[at + I_LEVELREQ] & 0xFF;
        }


        public int tier() {
            if (normCode.isEmpty() || uberCode.isEmpty() || ultraCode.isEmpty()) return NO_TIER;
            if (code.equals(normCode) && !(code.equals(uberCode) && code.equals(ultraCode))) return NORMAL;
            if (code.equals(uberCode) && !code.equals(normCode)) return EXCEPTIONAL;
            if (code.equals(ultraCode) && !code.equals(normCode) && !code.equals(uberCode)) return ELITE;
            return NO_TIER;
        }
    }

    private final Item[] items;
    private final Map<String, Item> byCode = new HashMap<>();

    private String[] typeCodes;
    private int[][] typeEquiv;

    static final int TYPE_RECORD = 228;

    private D2Gegenstaende(Item[] items) {
        this.items = items;
        for (Item i : items) if (!i.code.isEmpty()) byCode.putIfAbsent(i.code, i);
    }


    public static D2Gegenstaende lesen(byte[] weapons, byte[] armor, byte[] misc) {
        byte[][] tables = {weapons, armor, misc};
        int total = 0;
        for (byte[] t : tables) {
            int n = count(t);
            if (n < 0) return null;
            total += n;
        }
        Item[] all = new Item[total];
        int k = 0;
        for (byte[] t : tables) {
            int n = count(t);
            for (int i = 0; i < n; i++, k++) all[k] = new Item(t, 4 + i * RECORD, k);
        }
        return new D2Gegenstaende(all);
    }

    public int count() {
        return items.length;
    }


    public Item get(int index) {
        return index >= 0 && index < items.length ? items[index] : null;
    }


    public Item get(String code) {
        return code == null ? null : byCode.get(code.trim());
    }


    public Item nextTier(Item item) {
        if (item == null) return null;
        int t = item.tier();
        return t == NORMAL ? get(item.uberCode) : t == EXCEPTIONAL ? get(item.ultraCode) : null;
    }


    public boolean mitTypen(byte[] itemtypes) {
        if (itemtypes == null || itemtypes.length < 4) return false;
        int n = D2Tabellen.s32(itemtypes, 0);
        if (n <= 0 || (long) n * TYPE_RECORD + 4 != itemtypes.length) return false;
        String[] codes = new String[n];
        int[][] equiv = new int[n][];
        for (int i = 0; i < n; i++) {
            int at = 4 + i * TYPE_RECORD;
            codes[i] = D2Rezepte.code(itemtypes, at);
            equiv[i] = new int[]{D2Tabellen.u16(itemtypes, at + 4), D2Tabellen.u16(itemtypes, at + 6)};
        }
        typeCodes = codes;
        typeEquiv = equiv;
        return true;
    }


    public String typeCode(int type) {
        return typeCodes != null && type >= 0 && type < typeCodes.length ? typeCodes[type] : null;
    }


    public boolean ist(int type, int ancestor) {
        return ist(type, ancestor, 0);
    }

    private boolean ist(int type, int ancestor, int depth) {
        if (typeEquiv == null || type <= 0 || type >= typeEquiv.length || depth > 8) return false;
        if (type == ancestor) return true;
        return ist(typeEquiv[type][0], ancestor, depth + 1) || ist(typeEquiv[type][1], ancestor, depth + 1);
    }


    public boolean ist(Item item, int ancestor) {
        return item != null && (ist(item.type, ancestor) || ist(item.type2, ancestor));
    }


    public boolean ist(int type, String code) {
        if (typeCodes == null) return false;
        for (int t = 0; t < typeCodes.length; t++) if (typeCodes[t].equals(code)) return ist(type, t);
        return false;
    }

    static int count(byte[] b) {
        if (b == null || b.length < 4) return -1;
        int n = D2Tabellen.s32(b, 0);
        return n >= 0 && (long) n * RECORD + 4 == b.length ? n : -1;
    }

    static String cString(byte[] b, int at, int max) {
        int end = at;
        while (end < at + max && end < b.length && b[end] != 0) end++;
        return new String(b, at, end - at, StandardCharsets.ISO_8859_1);
    }
}
