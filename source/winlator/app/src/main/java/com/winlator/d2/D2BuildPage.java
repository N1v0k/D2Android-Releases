package com.winlator.d2;

import com.winlator.d2.test.Fertigkeiten;

import java.util.function.IntUnaryOperator;

public final class D2BuildPage {
    public static final int DONE = 0, NEXT = 1, LATER = 2;

    private D2BuildPage() {}

    public static int next(D2PointPlan.Build build, Fertigkeiten table, D2Points points) {
        if (build == null || table == null || points == null || points.firstSkill < 0) return -1;
        return D2PointPlan.nextSkill(build, table, points.level, points::baseLevel);
    }

    public static int[] states(D2PointPlan.Build build, Fertigkeiten table, D2Points points) {
        if (build == null || table == null || points == null || points.firstSkill < 0) return null;
        IntUnaryOperator base = points::baseLevel;
        int next = D2PointPlan.nextSkill(build, table, points.level, base);
        int[] out = new int[build.entries.size()];
        boolean nextShown = false;
        for (int i = 0; i < out.length; i++) {
            D2PointPlan.Entry e = build.entries.get(i);
            if (base.applyAsInt(e.skill) >= e.target) out[i] = DONE;
            else if (!nextShown && next >= 0 && e.fromLevel <= points.level
                && D2PointPlan.pointFor(table, e.skill, base, 0) == next) {
                out[i] = NEXT;
                nextShown = true;
            }
            else out[i] = LATER;
        }
        return out;
    }

    public static int outside(D2PointPlan.Build build, Fertigkeiten table, D2Points points) {
        if (build == null || table == null || points == null || points.firstSkill < 0) return 0;
        java.util.Set<Integer> inside = new java.util.HashSet<>();
        for (D2PointPlan.Entry e : build.entries) addWithPrerequisites(table, e.skill, inside, 0);
        int out = 0;
        for (int i = 0; i < points.baseLevels.length; i++) {
            if (points.baseLevels[i] > 0 && !inside.contains(points.firstSkill + i)) out += points.baseLevels[i];
        }
        return out;
    }

    private static void addWithPrerequisites(Fertigkeiten table, int skill, java.util.Set<Integer> into, int depth) {
        if (!into.add(skill) || depth > 8) return;
        Fertigkeiten.Fertigkeit f = table.get(skill);
        if (f != null) for (int pre : f.voraussetzungen) addWithPrerequisites(table, pre, into, depth + 1);
    }

    public static int[][] targets(D2PointPlan.Build build, int stat) {
        if (build == null) return new int[0][];
        return stat == D2PointPlan.STR ? build.strength : stat == D2PointPlan.DEX ? build.dexterity : new int[0][];
    }
}
