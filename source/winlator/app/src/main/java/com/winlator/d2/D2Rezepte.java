package com.winlator.d2;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

















public final class D2Rezepte {
    public static final int RECORD = 328, INPUTS = 7, OUTPUTS = 3, MODS = 5;
    static final int R_ENABLED = 0x00, R_LADDER = 0x01, R_MINDIFF = 0x02, R_CLASS = 0x03, R_OP = 0x04, R_PARAM = 0x08,
        R_VALUE = 0x0C, R_NUMINPUTS = 0x10, R_VERSION = 0x12, R_INPUTS = 0x14, R_OUTPUTS = 0x4C, INPUT = 8, OUTPUT = 84,
        O_MODS = 24, MOD = 12, O_PREFIX = 12, O_SUFFIX = 18;


    public static final int IN_ITEM = 0x1, IN_TYPE = 0x2, IN_NOSOCKET = 0x4, IN_SOCKETED = 0x8, IN_ETHEREAL = 0x10,
        IN_NOT_ETHEREAL = 0x20, IN_UNIQUE_SET = 0x40, IN_UPGRADED = 0x80, IN_BASIC = 0x100, IN_EXCEPTIONAL = 0x200,
        IN_ELITE = 0x400;

    public static final int ANY = 0xFFFF;

    public static final int OUT_MODS = 0x1, OUT_SOCKETS = 0x2, OUT_UNSOCKET = 0x10, OUT_EXCEPTIONAL = 0x80,
        OUT_ELITE = 0x100, OUT_REPAIR = 0x200, OUT_RECHARGE = 0x400;

    public static final int KIND_ITEM = 0xFC, KIND_TYPE = 0xFD, KIND_USEITEM = 0xFE, KIND_USETYPE = 0xFF, KIND_COW = 1,
        KIND_PANDEMONIUM = 2, KIND_FINALE = 3;

    public static final int Q_ANY = 0, Q_LOW = 1, Q_NORMAL = 2, Q_SUPERIOR = 3, Q_MAGIC = 4, Q_SET = 5, Q_RARE = 6,
        Q_UNIQUE = 7, Q_CRAFTED = 8, Q_TEMPERED = 9;


    public static final class Eingabe {
        public final int flags, item, uniqueSet, quality, quantity;

        Eingabe(int flags, int item, int uniqueSet, int quality, int quantity) {
            this.flags = flags;
            this.item = item;
            this.uniqueSet = uniqueSet;
            this.quality = quality;
            this.quantity = Math.max(1, quantity);
        }

        public boolean isType() {
            return (flags & IN_TYPE) != 0;
        }

        public boolean isAny() {
            return item == ANY;
        }

        public boolean has(int flag) {
            return (flags & flag) != 0;
        }
    }


    public static final class Mod {
        public final int property, param, min, max, chance;

        Mod(int property, int param, int min, int max, int chance) {
            this.property = property;
            this.param = param;
            this.min = min;
            this.max = max;
            this.chance = chance;
        }
    }


    public static final class Ausgabe {
        public final int flags, item, uniqueSet, quality, quantity, kind, lvl, plvl, ilvl;

        public final int prefix, suffix;
        public final List<Mod> mods;

        Ausgabe(int flags, int item, int uniqueSet, int quality, int quantity, int kind, int lvl, int plvl, int ilvl,
            int prefix, int suffix, List<Mod> mods) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.flags = flags;
            this.item = item;
            this.uniqueSet = uniqueSet;
            this.quality = quality;
            this.quantity = quantity;
            this.kind = kind;
            this.lvl = lvl;
            this.plvl = plvl;
            this.ilvl = ilvl;
            this.mods = mods;
        }

        public boolean has(int flag) {
            return (flags & flag) != 0;
        }


        public boolean portal() {
            return kind == KIND_COW || kind == KIND_PANDEMONIUM || kind == KIND_FINALE;
        }
    }


    public static final class Rezept {
        public final int row, minDifficulty, version;
        public final boolean enabled, ladder;
        public final List<Eingabe> inputs;
        public final List<Ausgabe> outputs;

        Rezept(int row, boolean enabled, boolean ladder, int minDifficulty, int version, List<Eingabe> inputs,
            List<Ausgabe> outputs) {
            this.row = row;
            this.enabled = enabled;
            this.ladder = ladder;
            this.minDifficulty = minDifficulty;
            this.version = version;
            this.inputs = inputs;
            this.outputs = outputs;
        }


        public Ausgabe output() {
            return outputs.isEmpty() ? null : outputs.get(0);
        }
    }

    private final List<Rezept> rezepte;

    private D2Rezepte(List<Rezept> rezepte) {
        this.rezepte = Collections.unmodifiableList(rezepte);
    }


    public List<Rezept> all() {
        return rezepte;
    }


    public static D2Rezepte lesen(byte[] b) {
        if (b == null || b.length < 4) return null;
        int n = D2Tabellen.s32(b, 0);
        if (n <= 0 || (long) n * RECORD + 4 != b.length) return null;
        List<Rezept> list = new ArrayList<>(n);
        for (int r = 0; r < n; r++) {
            int at = 4 + r * RECORD;
            int numInputs = b[at + R_NUMINPUTS] & 0xFF;
            if (numInputs > INPUTS || (b[at + R_ENABLED] & 0xFF) > 1) return null;
            List<Eingabe> inputs = new ArrayList<>();
            for (int k = 0; k < INPUTS; k++) {
                int i = at + R_INPUTS + k * INPUT;
                int flags = D2Tabellen.u16(b, i), item = D2Tabellen.u16(b, i + 2);
                if (flags == 0 && item == 0) continue;
                inputs.add(new Eingabe(flags, item, D2Tabellen.u16(b, i + 4), b[i + 6] & 0xFF, b[i + 7] & 0xFF));
            }
            List<Ausgabe> outputs = new ArrayList<>();
            for (int k = 0; k < OUTPUTS; k++) {
                int o = at + R_OUTPUTS + k * OUTPUT;
                int kind = b[o + 8] & 0xFF;
                if (kind == 0) continue;
                List<Mod> mods = new ArrayList<>();
                for (int m = 0; m < MODS; m++) {
                    int p = o + O_MODS + m * MOD;
                    int property = D2Tabellen.s32(b, p);
                    if (property < 0) continue;
                    mods.add(new Mod(property, D2Tabellen.u16(b, p + 4), (short) D2Tabellen.u16(b, p + 6),
                        (short) D2Tabellen.u16(b, p + 8), D2Tabellen.u16(b, p + 10)));
                }
                outputs.add(new Ausgabe(D2Tabellen.u16(b, o), D2Tabellen.u16(b, o + 2), D2Tabellen.u16(b, o + 4),
                    b[o + 6] & 0xFF, b[o + 7] & 0xFF, kind, b[o + 9] & 0xFF, b[o + 10] & 0xFF, b[o + 11] & 0xFF,
                    D2Tabellen.u16(b, o + O_PREFIX), D2Tabellen.u16(b, o + O_SUFFIX), mods));
            }
            list.add(new Rezept(r, b[at + R_ENABLED] != 0, b[at + R_LADDER] != 0, b[at + R_MINDIFF] & 0xFF,
                D2Tabellen.u16(b, at + R_VERSION), inputs, outputs));
        }
        return new D2Rezepte(list);
    }


    static String code(byte[] b, int at) {
        int end = at;
        while (end < at + 4 && b[end] != 0 && b[end] != ' ') end++;
        return new String(b, at, end - at, StandardCharsets.ISO_8859_1);
    }
}
