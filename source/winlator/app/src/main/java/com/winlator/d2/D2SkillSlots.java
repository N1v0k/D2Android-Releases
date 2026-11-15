package com.winlator.d2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;









public final class D2SkillSlots {
    public static final int PLACES = 16;

    public static final boolean ASSIGN_RIGHT = true;

    public static final int[] NONE = new int[PLACES];
    public static final boolean[] NO_LEFT = new boolean[PLACES];

    static final Set<Integer> AURAS = new HashSet<>(Arrays.asList(98, 99, 100, 102, 103, 104, 105, 108, 109, 110, 113, 114,
        115, 118, 119, 120, 122, 123, 124, 125));

    static {
        Arrays.fill(NONE, -1);
    }

    private final int[] skills = new int[PLACES];
    private int attack;

    private D2SkillSlots() {
        Arrays.fill(skills, -1);
    }


    public static String key(int characterClass, String name) {
        return characterClass < 0 || name == null || name.isEmpty() ? null : "slots." + characterClass + "." + name;
    }

    public static boolean isAura(int skill) {
        return AURAS.contains(skill);
    }


    public static boolean left(int skill) {
        return !isAura(skill);
    }


    public static D2SkillSlots parse(String text) {
        if (text == null) return null;
        String[] parts = text.split(";");
        if (parts.length != 2) return null;
        String[] places = parts[0].split(",");
        if (places.length != PLACES) return null;
        D2SkillSlots s = new D2SkillSlots();
        try {
            for (int i = 0; i < PLACES; i++) {
                int v = Integer.parseInt(places[i].trim());
                if (v < -1 || v > 1023) return null;
                s.skills[i] = v;
            }
            int a = Integer.parseInt(parts[1].trim());
            if (a < 0 || a > 1023 || isAura(a)) return null;
            s.attack = a;
        }
        catch (NumberFormatException e) {
            return null;
        }
        return s;
    }

    public String format() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < PLACES; i++) b.append(i == 0 ? "" : ",").append(skills[i]);
        return b.append(';').append(attack).toString();
    }






    public static D2SkillSlots takeOver(int[] hotkeySkill, int attackRight, int leftSkill) {
        D2SkillSlots s = new D2SkillSlots();
        for (int i = 0; hotkeySkill != null && i < PLACES && i < hotkeySkill.length; i++) {
            s.skills[i] = hotkeySkill[i] >= 0 && hotkeySkill[i] <= 1023 ? hotkeySkill[i] : -1;
        }
        s.attack = attackRight >= 0 && !isAura(attackRight) ? attackRight : leftSkill >= 0 && !isAura(leftSkill) ? leftSkill : 0;
        return s;
    }

    public int skill(int place) {
        return place >= 0 && place < PLACES ? skills[place] : -1;
    }


    public int[] skills() {
        return skills;
    }

    public int attack() {
        return attack;
    }

    public void set(int place, int skill) {
        if (place >= 0 && place < PLACES) skills[place] = skill >= 0 && skill <= 1023 ? skill : -1;
    }


    public boolean setAttack(int skill) {
        if (skill < 0 || skill > 1023 || isAura(skill)) return false;
        attack = skill;
        return true;
    }
}
