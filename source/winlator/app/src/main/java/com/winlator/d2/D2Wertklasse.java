package com.winlator.d2;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class D2Wertklasse {
    public static final int VERSION = 1, FIXED = 88, PAIRS = 56, MAX = 10;
    public static final int KIND_OTHER = 0, KIND_RUNE = 1, KIND_GEM = 2, KIND_REJUV = 3, KIND_JEWELRY = 4, KIND_QUEST = 5;
    public static final int Q_NORMAL = 2, Q_SUPERIOR = 3;

    static final int RUNEWORD = 8, REJUV = 1, DROP = 2, DROP_GEMS = 2, DROP_RUNES = 8;
    static final int[] RUNES = new int[33];
    static {
        for (int r = 1; r <= 33; r++) RUNES[r - 1] = r >= 30 ? 10 : r >= 26 ? 9 : r >= 24 ? 8 : r >= 20 ? 7 : r >= 9 ? 5 : 3;
    }
    static final int[] GEMS = {1, 1, 2, 4, 6};
    static final int[] JEWELRY = {0, 0, 0, 6, 8, 6, 8, 6};
    static final int[] OTHER = {0, 0, 0, 2, 8, 4, 8, 4};
    static final int[] BASE = {3, 5, 7};
    static final int[] GOLD_AT = {100, 500, 2000, 5000};
    static final int[] GOLD_FACTOR = {0, 0, 0, 4, 0, 8, 0, 8};

    static final int R_RECORD = 288, R_COMPLETE = 0x80, R_SERVER = 0x81, R_ITYPES = 0x86, R_ETYPES = 0x92, R_RUNES = 0x98;

    private D2Wertklasse() {}

    public static int[][] basen(byte[] runes, boolean ladder) {
        if (runes == null || runes.length < 4) return null;
        int n = D2Tabellen.s32(runes, 0);
        if (n <= 0 || (long) n * R_RECORD + 4 != runes.length) return null;
        Set<Long> seen = new LinkedHashSet<>();
        for (int i = 0; i < n; i++) {
            int at = 4 + i * R_RECORD;
            if (runes[at + R_COMPLETE] == 0 || (runes[at + R_SERVER] != 0 && !ladder)) continue;
            boolean excluded = false;
            for (int k = 0; k < 3; k++) excluded |= D2Tabellen.u16(runes, at + R_ETYPES + 2 * k) != 0;
            if (excluded) continue;
            int sockets = 0;
            for (int k = 0; k < 6; k++) if (D2Tabellen.s32(runes, at + R_RUNES + 4 * k) > 0) sockets++;
            if (sockets == 0) continue;
            for (int k = 0; k < 6; k++) {
                int type = D2Tabellen.u16(runes, at + R_ITYPES + 2 * k);
                if (type > 0 && type < 0xFFFF) seen.add((long) type << 8 | sockets);
            }
        }
        List<int[]> pairs = new ArrayList<>();
        for (long p : seen) pairs.add(new int[]{(int) (p >> 8), (int) (p & 0xFF)});
        return pairs.toArray(new int[0][]);
    }

    public static byte[] regeln(int[][] basen) {
        int pairs = basen == null ? 0 : Math.min(basen.length, PAIRS);
        byte[] b = new byte[FIXED - 8 + 3 * pairs];
        int p = 0;
        b[p++] = VERSION;
        b[p++] = RUNEWORD;
        for (int v : RUNES) b[p++] = (byte) v;
        for (int v : GEMS) b[p++] = (byte) v;
        b[p++] = REJUV;
        for (int v : JEWELRY) b[p++] = (byte) v;
        for (int v : OTHER) b[p++] = (byte) v;
        for (int v : BASE) b[p++] = (byte) v;
        b[p++] = DROP;
        b[p++] = DROP_GEMS;
        b[p++] = DROP_RUNES;
        for (int v : GOLD_AT) {
            b[p++] = (byte) v;
            b[p++] = (byte) (v >> 8);
        }
        for (int v : GOLD_FACTOR) b[p++] = (byte) v;
        b[p++] = (byte) pairs;
        for (int i = 0; i < pairs; i++) {
            b[p++] = (byte) basen[i][0];
            b[p++] = (byte) (basen[i][0] >> 8);
            b[p++] = (byte) basen[i][1];
        }
        return b;
    }

    public interface Typen {
        boolean ist(int type);
    }

    public static int note(int kind, int tier, int quality, boolean ethereal, boolean runeword, int sockets, int baseClass,
                           long cost, int difficulty, int[][] basen, Typen typen) {
        if (kind == KIND_QUEST) return -1;
        int q = quality >= 1 && quality <= 8 ? quality - 1 : -1;
        int note = 0;
        switch (kind) {
            case KIND_RUNE:
                if (tier >= 1 && tier <= 33) note = RUNES[tier - 1] - (difficulty > 0 && tier <= DROP_RUNES ? DROP : 0);
                break;
            case KIND_GEM:
                if (tier >= 1 && tier <= 5) note = GEMS[tier - 1] - (difficulty > 0 && tier <= DROP_GEMS ? DROP : 0);
                break;
            case KIND_REJUV:
                note = REJUV;
                break;
            case KIND_JEWELRY:
                note = q >= 0 ? JEWELRY[q] : 0;
                break;
            default:
                note = q >= 0 ? OTHER[q] : 0;
                if (runeword) note = Math.max(note, RUNEWORD);
                if (sockelBasis(quality, ethereal, runeword, sockets, baseClass, basen, typen)) note = Math.max(note, BASE[baseClass - 1]);
                break;
        }
        note = Math.max(0, Math.min(MAX, note));
        long gold = q >= 0 ? cost * GOLD_FACTOR[q] / 16 : 0;
        for (int i = 0; i < GOLD_AT.length; i++) if (GOLD_AT[i] > 0 && gold >= GOLD_AT[i]) note = Math.max(note, i + 1);
        return note;
    }

    static boolean sockelBasis(int quality, boolean ethereal, boolean runeword, int sockets, int baseClass, int[][] basen, Typen typen) {
        if (quality != Q_NORMAL && quality != Q_SUPERIOR) return false;
        if (ethereal || runeword || sockets <= 0 || baseClass < 1 || baseClass > 3 || basen == null) return false;
        for (int i = 0; i < Math.min(basen.length, PAIRS); i++) if (basen[i][1] == sockets && typen.ist(basen[i][0])) return true;
        return false;
    }
}
