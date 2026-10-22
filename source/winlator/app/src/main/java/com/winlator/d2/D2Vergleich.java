package com.winlator.d2;

import java.util.ArrayList;
import java.util.List;

public final class D2Vergleich {
    public static final int STATS = 364;
    public static final int STR = 0, ENERGY = 1, DEX = 2, VIT = 3, LIFE = 7, MANA = 9, LIFE_PCT = 76, MANA_PCT = 77,
        FHR = 99, FBR = 102, FCR = 105, LIFE_PER_LEVEL = 216, MANA_PER_LEVEL = 217, MIN = 21, MAX = 22, MIN2 = 23,
        MAX2 = 24, DEMON = 121, UNDEAD = 122;
    public static final int W_IS = 360, W_TWO = 361, W_STRB = 362, W_DEXB = 363;
    public static final int[] ELEM_MIN = {48, 50, 52, 54}, ELEM_MAX = {49, 51, 53, 55};
    public static final int[] RES = {39, 43, 41, 45}, RES_MAX = {40, 44, 42, 46};
    public static final int[] PENALTY = {0, 40, 100};
    public static final String GREEN = "\u00FF" + "c2", RED = "\u00FF" + "c1";

    private D2Vergleich() {
    }

    public static final class Figur {
        public int klasse;
        public int stufe;
        public int schwierigkeit;
        public long basisLeben, basisMana;
        public int lebenJeVit, manaJeEnergie;
        public final int[] werte = new int[STATS];
        public D2Breakpoints.Anim fcr, fhr, fbr;
    }

    public static final class Ergebnis {
        public boolean schaden;
        public final int[] schadenVorher = new int[2], schadenNachher = new int[2], elementVorher = new int[2], elementNachher = new int[2];
        public int untoteVorher, untoteNachher, daemonenVorher, daemonenNachher;
        public int lebenVorher, lebenNachher, manaVorher, manaNachher;
        public final int[] resVorher = new int[4], resNachher = new int[4];
        public final int[] framesVorher = {-1, -1, -1}, framesNachher = {-1, -1, -1};
    }

    static int stat(int[] base, int[] item, int id) {
        return base[id] + (item != null ? item[id] : 0);
    }

    static long max(Figur f, int[] item, boolean life) {
        long base = life ? f.basisLeben : f.basisMana;
        long flat = stat(f.werte, item, life ? LIFE : MANA);
        long attr = (long) stat(f.werte, item, life ? VIT : ENERGY) * (life ? f.lebenJeVit : f.manaJeEnergie) * 64;
        long perLevel = (long) stat(f.werte, item, life ? LIFE_PER_LEVEL : MANA_PER_LEVEL) * f.stufe / 8;
        long pct = base * stat(f.werte, item, life ? LIFE_PCT : MANA_PCT) / 100;
        return (base + flat + attr + perLevel + pct) >> 8;
    }

    public static int resistance(int value, int max, int difficulty) {
        int cap = Math.min(95, 75 + max);
        return Math.max(-100, Math.min(cap, value - PENALTY[difficulty]));
    }

    static void schaden(Figur f, int[] w, int[] out) {
        boolean two = w[W_TWO] != 0;
        long pct = 100 + stat(f.werte, w, STR) * w[W_STRB] / 100 + stat(f.werte, w, DEX) * w[W_DEXB] / 100;
        out[0] = (int) (stat(f.werte, w, two ? MIN2 : MIN) * pct / 100);
        out[1] = (int) (stat(f.werte, w, two ? MAX2 : MAX) * pct / 100);
    }

    static void element(Figur f, int[] w, int[] out) {
        out[0] = out[1] = 0;
        for (int i = 0; i < 4; i++) {
            out[0] += stat(f.werte, w, ELEM_MIN[i]);
            out[1] += stat(f.werte, w, ELEM_MAX[i]);
        }
    }

    public static Ergebnis vergleiche(Figur f, int[] getragen, int[] neu) {
        Ergebnis e = new Ergebnis();
        e.schaden = getragen != null && neu != null && getragen[W_IS] != 0 && neu[W_IS] != 0;
        if (e.schaden) {
            schaden(f, getragen, e.schadenVorher);
            schaden(f, neu, e.schadenNachher);
            element(f, getragen, e.elementVorher);
            element(f, neu, e.elementNachher);
            e.untoteVorher = stat(f.werte, getragen, UNDEAD);
            e.untoteNachher = stat(f.werte, neu, UNDEAD);
            e.daemonenVorher = stat(f.werte, getragen, DEMON);
            e.daemonenNachher = stat(f.werte, neu, DEMON);
        }
        e.lebenVorher = (int) max(f, getragen, true);
        e.lebenNachher = (int) max(f, neu, true);
        e.manaVorher = (int) max(f, getragen, false);
        e.manaNachher = (int) max(f, neu, false);
        for (int i = 0; i < 4; i++) {
            e.resVorher[i] = resistance(stat(f.werte, getragen, RES[i]), stat(f.werte, getragen, RES_MAX[i]), f.schwierigkeit);
            e.resNachher[i] = resistance(stat(f.werte, neu, RES[i]), stat(f.werte, neu, RES_MAX[i]), f.schwierigkeit);
        }
        D2Breakpoints.Anim[] anims = {f.fcr, f.fhr, f.fbr};
        D2Breakpoints.Art[] arts = {D2Breakpoints.Art.FCR, D2Breakpoints.Art.FHR, D2Breakpoints.Art.FBR};
        int[] ids = {FCR, FHR, FBR};
        for (int k = 0; k < 3; k++) {
            if (anims[k] == null) continue;
            e.framesVorher[k] = D2Breakpoints.frames(arts[k], anims[k], stat(f.werte, getragen, ids[k]));
            e.framesNachher[k] = D2Breakpoints.frames(arts[k], anims[k], stat(f.werte, neu, ids[k]));
        }
        return e;
    }

    static final String[] RES_DE = {"Feuer", "Kälte", "Blitz", "Gift"}, RES_EN = {"Fire", "Cold", "Lightning", "Poison"};
    static final String[] BP_DE = {"Zaubertempo", "Trefferholung", "Blocktempo"}, BP_EN = {"Cast rate", "Hit recovery", "Block rate"};

    static String signed(int v) {
        return v > 0 ? "+" + v : String.valueOf(v);
    }

    static String part(int delta, String text) {
        return (delta > 0 ? GREEN : RED) + text;
    }

    public static List<String> zeilen(Ergebnis e, boolean deutsch) {
        List<String> out = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        if (e.schaden) {
            int[][][] pairs = {{e.schadenVorher, e.schadenNachher}, {e.elementVorher, e.elementNachher}};
            String[] names = deutsch ? new String[]{"Schaden", "Element"} : new String[]{"Damage", "Element"};
            for (int k = 0; k < 2; k++) {
                int[] b = pairs[k][0], a = pairs[k][1];
                if (b[0] == a[0] && b[1] == a[1]) continue;
                out.add((a[0] + a[1] > b[0] + b[1] ? GREEN : RED) + names[k] + " " + b[0] + "-" + b[1] + " -> " + a[0] + "-" + a[1]);
            }
            int[][] bonus = {{e.untoteVorher, e.untoteNachher}, {e.daemonenVorher, e.daemonenNachher}};
            String[] vs = deutsch ? new String[]{"an Untoten", "an Dämonen"} : new String[]{"vs undead", "vs demons"};
            for (int k = 0; k < 2; k++) {
                int d = bonus[k][1] - bonus[k][0];
                if (d != 0) parts.add(part(d, vs[k] + " " + signed(d) + "% (" + bonus[k][1] + "%)"));
            }
            if (!parts.isEmpty()) out.add(String.join(" | ", parts));
            parts.clear();
        }
        int life = e.lebenNachher - e.lebenVorher, mana = e.manaNachher - e.manaVorher;
        if (life != 0) parts.add(part(life, (deutsch ? "Leben " : "Life ") + signed(life)));
        if (mana != 0) parts.add(part(mana, "Mana " + signed(mana)));
        if (!parts.isEmpty()) out.add(String.join(" | ", parts));
        parts.clear();
        for (int i = 0; i < 4; i++) {
            int d = e.resNachher[i] - e.resVorher[i];
            if (d != 0) parts.add(part(d, (deutsch ? RES_DE[i] : RES_EN[i]) + " " + signed(d) + " (" + e.resNachher[i] + ")"));
        }
        if (!parts.isEmpty()) out.add(String.join(" | ", parts));
        for (int k = 0; k < 3; k++) {
            int before = e.framesVorher[k], after = e.framesNachher[k];
            if (before < 0 || after < 0 || before == after) continue;
            boolean gain = after < before;
            String what = deutsch ? (gain ? "Stufe erreicht" : "Stufe verloren") : (gain ? "breakpoint reached" : "breakpoint lost");
            out.add((gain ? GREEN : RED) + (deutsch ? BP_DE[k] : BP_EN[k]) + ": " + what + " (" + before + " -> " + after + ")");
        }
        return out;
    }

    public static boolean spielschrift(String line) {
        String text = line.replace(GREEN, "").replace(RED, "");
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c < 0x20 || c > 0x7E) && "äöüÄÖÜß".indexOf(c) < 0) return false;
        }
        return true;
    }
}
