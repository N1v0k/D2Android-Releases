package com.winlator.d2;

import java.util.List;



















final class D2PadTarget {
    static final int ATTACK = 0, TAKE = 1;

    static final double KEEP = 1.15;

    static final double SPECIAL = 0.9;

    static final long NONE = -1;


    static final class Candidate {
        final Object ref;


        final long key;
        final float dx, dy;
        final double distance;
        final boolean monster, special, behind;

        boolean first;

        Candidate(Object ref, long key, float dx, float dy, double distance, boolean monster, boolean special, boolean behind) {
            this.ref = ref;
            this.key = key;
            this.dx = dx;
            this.dy = dy;
            this.distance = distance;
            this.monster = monster;
            this.special = special;
            this.behind = behind;
        }
    }


    static final class Choice {
        final int what;
        final Object target;


        final long key;
        final String why;

        Choice(int what, Candidate target, String why) {
            this.what = what;
            this.target = target != null ? target.ref : null;
            this.key = target != null ? target.key : NONE;
            this.why = why;
        }
    }







    static Choice choose(List<Candidate> monsters, List<Candidate> takes, boolean stick, double dirX, double dirY,
                         double coneCos, double melee, double skillReach, double takeReach, long previous) {

        Candidate inner = null, innerPrevious = null;
        double innerScore = Double.MAX_VALUE, previousInner = Double.MAX_VALUE;
        for (Candidate m : monsters) {
            if (m.distance > melee) continue;
            double score = m.distance * (m.special ? SPECIAL : 1);
            if (m.key == previous) {
                previousInner = score;
                innerPrevious = m;
            }
            if (score < innerScore) {
                innerScore = score;
                inner = m;
            }
        }
        if (inner != null) {
            Candidate target = innerPrevious != null && previousInner <= innerScore * KEEP ? innerPrevious : inner;
            return new Choice(ATTACK, target, "monster in the inner circle");
        }
        Candidate best = null;
        double bestScore = Double.MAX_VALUE, previousScore = Double.MAX_VALUE;
        Candidate previousCandidate = null;
        if (stick) {

            for (int pass = 0; pass < 2; pass++) {
                List<Candidate> list = pass == 0 ? monsters : takes;
                double reach = pass == 0 ? skillReach : takeReach;
                for (Candidate c : list) {
                    if (c.distance > reach) continue;
                    double cos = c.distance > 1 ? (c.dx * dirX + c.dy * dirY) / Math.hypot(c.dx, c.dy) : 1;
                    if (cos < coneCos) continue;
                    double score = c.distance * (1.5 - cos) * (c.special ? SPECIAL : 1);
                    if (c.key == previous) {
                        previousScore = score;
                        previousCandidate = c;
                    }
                    if (score < bestScore) {
                        bestScore = score;
                        best = c;
                    }
                }
            }
            if (best == null) return new Choice(ATTACK, null, "stick: nothing in the cone, walks on");
        }
        else {

            for (Candidate m : monsters) {
                if (m.distance > skillReach || m.behind) continue;
                double score = m.distance * (m.special ? SPECIAL : 1);
                if (m.key == previous) {
                    previousScore = score;
                    previousCandidate = m;
                }
                if (score < bestScore) {
                    bestScore = score;
                    best = m;
                }
            }
            Candidate take = null;
            for (Candidate t : takes) {
                if (t.distance <= takeReach) {
                    take = t;
                    break;
                }
            }


            if (take != null && !take.first && take.key != previous) {
                for (Candidate t : takes) {
                    if (t.key == previous && t.distance <= takeReach && t.distance <= take.distance * KEEP) {
                        take = t;
                        break;
                    }
                }
            }
            if (take != null) {
                if (take.key == previous) {
                    previousScore = take.distance;
                    previousCandidate = take;
                }
                if (take.distance < bestScore) {
                    bestScore = take.distance;
                    best = take;
                }
            }
            if (best == null) return new Choice(ATTACK, null, "no stick: nothing near");
        }
        if (previousCandidate != null && previousScore <= bestScore * KEEP) best = previousCandidate;
        String why = stick ? "stick: best in the cone" : "no stick: the nearer";
        return new Choice(best.monster ? ATTACK : TAKE, best, why);
    }
}
