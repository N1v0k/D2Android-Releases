package com.winlator.d2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;












final class D2EffectRules {

    static final int SHRINE = 0, POTION = 1, OWN = 2, HARM = 3, CURSE = 4, AURA = 5, NONE = -1;

    static final int PER_COLUMN = 6, MAX = 2 * PER_COLUMN;

    static final int ICON = 32, BOX = ICON + 6, STEP = 56, TOP = 70, LEFT = 6, COLUMN = 44;

    static final int[] POTIONS = {100, 106, 180, 178, 179};






    static final int[][] HARMS = {{1, -1}, {2, -1}, {11, 44}, {21, 139}, {24, 87}, {52, 82}, {62, 67}};
    static final String[] HARM_PICTURES = {"wms", "yps", null, null, null, null, null};

    static final int[] CURSES = {66, 71, 72, 76, 77, 81, 82, 86, 87, 91};

    static final int PALADIN = 3;

    private D2EffectRules() {}







    static int[][] stateSkills(byte[] skills) {
        if (skills == null || skills.length < 8) return null;
        int count = u16(skills, 0);
        if (count <= 0) return null;
        int size = (skills.length - 4) / count;
        if (size < 0x84) return null;
        int[][] out = new int[2][256];
        Arrays.fill(out[0], -1);
        Arrays.fill(out[1], -1);
        for (int id = 0; id < count; id++) {
            int at = 4 + id * size;
            int own = u16(skills, at + 0x80), target = u16(skills, at + 0x82);
            boolean paladin = skills[at + 0x0C] == PALADIN;
            if (own < 256 && (out[0][own] < 0 || paladin && !paladinAt(skills, size, out[0][own]))) out[0][own] = id;
            if (target < 256 && (out[1][target] < 0 || paladin && !paladinAt(skills, size, out[1][target]))) out[1][target] = id;
        }
        return out;
    }

    private static boolean paladinAt(byte[] skills, int size, int id) {
        return skills[4 + id * size + 0x0C] == PALADIN;
    }


    static int harm(int state) {
        for (int i = 0; i < HARMS.length; i++) if (HARMS[i][0] == state) return i;
        return -1;
    }


    static int skillOf(int state, int[][] stateSkills) {
        if (stateSkills == null || state < 0 || state >= 256) return -1;
        int h = harm(state);
        if (h >= 0) return HARMS[h][1];
        return stateSkills[0][state] >= 0 ? stateSkills[0][state] : stateSkills[1][state];
    }





    static int group(int state, int[][] stateSkills, int skillClass, boolean timed) {
        if (state >= 128 && state <= 137) return SHRINE;
        for (int p : POTIONS) if (p == state) return POTION;
        if (harm(state) >= 0) return HARM;
        int skill = skillOf(state, stateSkills);
        if (skill < 0) return NONE;
        for (int c : CURSES) if (c == skill) return CURSE;
        if (skillClass == PALADIN && !timed) return AURA;
        return OWN;
    }





    static int[] order(int[] states, int[] groups, Map<Integer, Long> firstSeen) {
        List<Integer> shown = new ArrayList<>();
        for (int i = 0; i < states.length; i++) if (groups[i] != NONE) shown.add(i);
        shown.sort((a, b) -> {
            if (groups[a] != groups[b]) return Integer.compare(groups[a], groups[b]);
            long fa = firstSeen.getOrDefault(states[a], 0L), fb = firstSeen.getOrDefault(states[b], 0L);
            return fa != fb ? Long.compare(fa, fb) : Integer.compare(states[a], states[b]);
        });
        int n = Math.min(MAX, shown.size());
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = states[shown.get(i)];
        return out;
    }


    static int[] place(int i) {
        return new int[]{LEFT + (i / PER_COLUMN) * COLUMN, TOP + (i % PER_COLUMN) * STEP};
    }


    static float gone(int left, int full) {
        return full > 0 ? 1f - Math.min(1f, Math.max(0, left) / (float) full) : 0f;
    }


    static int percent(int left, int full) {
        return full > 0 ? Math.round(100f * Math.min(left, full) / full) : 0;
    }

    private static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }
}
