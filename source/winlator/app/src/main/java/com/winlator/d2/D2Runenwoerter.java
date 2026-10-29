package com.winlator.d2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;











public final class D2Runenwoerter {
    public static final int RECORD = 288, GEM_RECORD = 192;
    static final int R_COMPLETE = 0x80, R_SERVER = 0x81, R_ITYPES = 0x86, R_ETYPES = 0x92, R_RUNES = 0x98, R_MODS = 0xB0;
    static final int G_ITEM = 0x28, G_WEAPON = 0x30, G_HELM = 0x60, G_SHIELD = 0x90;


    public static final class Runenwort {
        public final int row;
        public final String nameKey;
        public final boolean complete, ladder;

        public final int[] types, excluded;

        public final int[] runes;
        public final List<D2Werte.Wert> werte;

        Runenwort(int row, String nameKey, boolean complete, boolean ladder, int[] types, int[] excluded, int[] runes,
            List<D2Werte.Wert> werte) {
            this.row = row;
            this.nameKey = nameKey;
            this.complete = complete;
            this.ladder = ladder;
            this.types = types;
            this.excluded = excluded;
            this.runes = runes;
            this.werte = werte;
        }
    }


    public static final class Steckteil {
        public final int item;
        public final List<D2Werte.Wert> waffe, ruestung, schild;

        Steckteil(int item, List<D2Werte.Wert> waffe, List<D2Werte.Wert> ruestung, List<D2Werte.Wert> schild) {
            this.item = item;
            this.waffe = waffe;
            this.ruestung = ruestung;
            this.schild = schild;
        }
    }

    private final List<Runenwort> words;
    private final List<Steckteil> gems;

    private D2Runenwoerter(List<Runenwort> words, List<Steckteil> gems) {
        this.words = Collections.unmodifiableList(words);
        this.gems = Collections.unmodifiableList(gems);
    }


    public List<Runenwort> all() {
        return words;
    }


    public Steckteil steckteil(int item) {
        for (Steckteil s : gems) if (s.item == item) return s;
        return null;
    }


    public static D2Runenwoerter lesen(byte[] runes, byte[] gemsBin) {
        int n = D2Werte.count(runes, RECORD);
        if (n < 0) return null;
        List<Runenwort> list = new ArrayList<>(n);
        for (int r = 0; r < n; r++) {
            int at = 4 + r * RECORD;
            int[] types = new int[6], excluded = new int[3];
            for (int k = 0; k < 6; k++) types[k] = D2Tabellen.u16(runes, at + R_ITYPES + 2 * k);
            for (int k = 0; k < 3; k++) excluded[k] = D2Tabellen.u16(runes, at + R_ETYPES + 2 * k);
            List<Integer> rs = new ArrayList<>();
            for (int k = 0; k < 6; k++) {
                int item = D2Tabellen.s32(runes, at + R_RUNES + 4 * k);
                if (item > 0) rs.add(item);
            }
            int[] runeItems = new int[rs.size()];
            for (int k = 0; k < runeItems.length; k++) runeItems[k] = rs.get(k);
            list.add(new Runenwort(r, D2Gegenstaende.cString(runes, at, 64), runes[at + R_COMPLETE] != 0,
                runes[at + R_SERVER] != 0, types, excluded, runeItems, mods(runes, at + R_MODS, 7)));
        }
        List<Steckteil> gems = new ArrayList<>();
        int ng = D2Werte.count(gemsBin, GEM_RECORD);
        for (int g = 0; g < ng; g++) {
            int at = 4 + g * GEM_RECORD;
            gems.add(new Steckteil(D2Tabellen.s32(gemsBin, at + G_ITEM), mods(gemsBin, at + G_WEAPON, 3),
                mods(gemsBin, at + G_HELM, 3), mods(gemsBin, at + G_SHIELD, 3)));
        }
        return new D2Runenwoerter(list, gems);
    }

    private static List<D2Werte.Wert> mods(byte[] b, int at, int count) {
        List<D2Werte.Wert> list = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            int p = at + 16 * k, property = D2Tabellen.s32(b, p);
            if (property < 0) continue;
            list.add(new D2Werte.Wert(property, D2Tabellen.s32(b, p + 4), D2Tabellen.s32(b, p + 8), D2Tabellen.s32(b, p + 12)));
        }
        return list;
    }
}
