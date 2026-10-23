package com.winlator.d2;

import com.winlator.d2.test.Fertigkeiten;























public final class D2PointSpend {
    static final long CONFIRM_MS = 1000, SETTLE_MS = 2500, PAUSE_MS = 30_000;
    static final int PER_SECOND = 10, MISSES = 2, PAUSES = 3;


    public interface Out {
        void send(int kind, int what);

        default void log(String text) {}
    }

    private D2Points sentWith;

    private D2Points atDeadline;
    private int sentKind = -1, sentWhat, misses, pauses, gaveUpAt = -1;
    private long sentAt = Long.MIN_VALUE / 2, pauseUntil;
    private int skills, stats;
    private String why;






    public void step(D2PointPlan.Build build, Fertigkeiten table, D2Points p, boolean ready, long now, Out out) {
        if (p == null || p.firstSkill < 0) return;
        if (sentKind >= 0) {
            if (p != sentWith && confirmed(p)) {
                if (sentKind == D2Points.KIND_SKILL) skills++;
                else stats++;
                sentKind = -1;
                atDeadline = null;
                misses = 0;
                pauses = 0;
            }
            else if (now - sentAt < CONFIRM_MS) return;
            else if (atDeadline == null) {
                atDeadline = p;
                return;
            }
            else if (p == atDeadline || now - sentAt < SETTLE_MS) return;
            else if (p.counter != sentWith.counter) {

                sentKind = -1;
                atDeadline = null;
            }
            else {
                sentKind = -1;
                atDeadline = null;
                if (++misses >= MISSES) {
                    misses = 0;
                    if (++pauses >= PAUSES) {
                        gaveUpAt = p.level;
                        why = "no effect";
                        out.log("points: spending without effect, no more tries until the next level-up");
                    }
                    else {
                        pauseUntil = now + PAUSE_MS;
                        out.log("points: twice without effect, pause " + PAUSE_MS / 1000 + " s (" + pauses + ")");
                    }
                }
                return;
            }
        }
        if (gaveUpAt >= 0) {
            if (p.level <= gaveUpAt) return;
            gaveUpAt = -1;
            pauses = 0;
            why = null;
        }
        if (now < pauseUntil || !ready || now - sentAt < 1000 / PER_SECOND) return;
        if (build == null) {
            if (p.skillPoints > 0 || p.statPoints > 0) why = "no build";
            return;
        }
        int kind = -1, what = -1;
        if (p.skillPoints > 0 && p.canSend(D2Points.KIND_SKILL)) {
            what = D2PointPlan.nextSkill(build, table, p.level, p::baseLevel);
            if (what >= 0) kind = D2Points.KIND_SKILL;
        }
        if (kind < 0 && p.statPoints > 0 && p.canSend(D2Points.KIND_STAT) && p.baseStatsKnown()) {
            what = D2PointPlan.nextStat(build, p.level, p.baseStats);
            if (what >= 0) kind = D2Points.KIND_STAT;
        }
        if (kind < 0) {
            if ((p.skillPoints > 0 && !p.canSend(D2Points.KIND_SKILL)) || (p.statPoints > 0 && !p.canSend(D2Points.KIND_STAT)))
                why = "not possible";
            return;
        }
        sentWith = p;
        sentKind = kind;
        sentWhat = what;
        sentAt = now;
        out.send(kind, what);
    }


    private boolean confirmed(D2Points p) {
        if (sentKind == D2Points.KIND_SKILL) {
            return p.skillPoints < sentWith.skillPoints || p.baseLevel(sentWhat) > sentWith.baseLevel(sentWhat);
        }
        return p.statPoints < sentWith.statPoints
            || (p.baseStatsKnown() && sentWhat >= 0 && sentWhat < 4 && p.baseStats[sentWhat] > sentWith.baseStats[sentWhat]);
    }


    public boolean waiting() {
        return sentKind >= 0;
    }


    public int skillsSpent() {
        return skills;
    }

    public int statsSpent() {
        return stats;
    }


    public String why() {
        return why;
    }

    @Override
    public String toString() {
        return "spent " + skills + " skill, " + stats + " stat" + (sentKind >= 0 ? ", waiting for " + sentKind + "/" + sentWhat : "")
            + (why != null ? ", " + why : "");
    }
}
