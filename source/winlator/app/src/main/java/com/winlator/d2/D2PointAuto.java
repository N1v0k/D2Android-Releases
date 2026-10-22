package com.winlator.d2;

import java.util.ArrayDeque;
import java.util.Arrays;

public final class D2PointAuto {
    static final long CONFIRM_MS = 2000;
    static final int TRIES = 2;
    static final int KEYS = 4;

    public interface Out {
        void bind(int skill, int key);

        void noKey(int skill);
    }

    private int whoClass = -1;
    private String whoName;
    private int firstSkill = -1;
    private int[] base;
    private final ArrayDeque<Integer> pending = new ArrayDeque<>();
    private int sentSkill = -1, sentKey = -1, tries;
    private long sentAt;

    public void reset() {
        whoClass = -1;
        whoName = null;
        base = null;
        pending.clear();
        sentSkill = sentKey = -1;
        tries = 0;
    }

    public void step(D2PointPlan.Build build, D2Points points, int characterClass, String name, int[] hotkeySkill,
                     boolean ready, long now, Out out) {
        if (points == null || characterClass < 0 || name == null || points.firstSkill < 0) {
            reset();
            return;
        }
        if (characterClass != whoClass || !name.equals(whoName) || points.firstSkill != firstSkill || base == null) {
            reset();
            whoClass = characterClass;
            whoName = name;
            firstSkill = points.firstSkill;
            base = points.baseLevels.clone();
            return;
        }
        for (int i = 0; i < base.length && i < points.baseLevels.length; i++) {
            int skill = firstSkill + i;
            if (base[i] == 0 && points.baseLevels[i] > 0 && isKey(build, skill) && !pending.contains(skill)) pending.add(skill);
            base[i] = points.baseLevels[i];
        }
        if (pending.isEmpty() || build == null) return;
        int skill = pending.peek();
        if (onKey(hotkeySkill, skill) >= 0) {
            pending.poll();
            sentSkill = -1;
            tries = 0;
            return;
        }
        if (sentSkill == skill && now - sentAt < CONFIRM_MS) return;
        if (!ready) return;
        if (!points.canSend(D2Points.KIND_KEY) || (sentSkill == skill && tries >= TRIES)) {
            give(out, skill);
            return;
        }
        int key = freeKey(hotkeySkill);
        if (key < 0) {
            give(out, skill);
            return;
        }
        if (sentSkill != skill) tries = 0;
        sentSkill = skill;
        sentKey = key;
        sentAt = now;
        tries++;
        out.bind(skill, key);
    }

    private void give(Out out, int skill) {
        pending.poll();
        sentSkill = -1;
        tries = 0;
        out.noKey(skill);
    }

    static boolean isKey(D2PointPlan.Build build, int skill) {
        if (build == null) return false;
        for (D2PointPlan.Key k : build.keys) if (k.skill == skill) return true;
        return false;
    }

    static int onKey(int[] hotkeySkill, int skill) {
        for (int i = 0; hotkeySkill != null && i < hotkeySkill.length; i++) if (hotkeySkill[i] == skill) return i;
        return -1;
    }

    static int freeKey(int[] hotkeySkill) {
        for (int i = 0; hotkeySkill != null && i < KEYS && i < hotkeySkill.length; i++) if (hotkeySkill[i] < 0) return i;
        return -1;
    }

    @Override
    public String toString() {
        return "pending " + pending + ", sent " + sentSkill + " on F" + (sentKey + 1) + " (" + tries + "), base "
            + (base != null ? Arrays.toString(base) : "-");
    }

}
