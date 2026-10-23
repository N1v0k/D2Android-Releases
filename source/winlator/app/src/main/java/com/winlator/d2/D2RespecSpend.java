package com.winlator.d2;

import com.winlator.d2.test.Fertigkeiten;













public final class D2RespecSpend {
    public static final int OFF = 0, WAITING = 1, RUNNING = 2, DONE = 3, CANCELLED = 4;

    static final long WAIT_MS = 20_000;

    private int state = OFF;
    private int heroClass = -1;
    private String name;
    private long readySince = -1;
    private D2PointSpend spend = new D2PointSpend();
    private String why;
    private int left;


    public void start(int heroClass, String name) {
        this.heroClass = heroClass;
        this.name = name;
        state = WAITING;
        readySince = -1;
        spend = new D2PointSpend();
        why = null;
        left = 0;
    }

    public boolean active() {
        return state == WAITING || state == RUNNING;
    }

    public int state() {
        return state;
    }


    public void cancel(String reason) {
        if (!active()) return;
        state = CANCELLED;
        why = reason;
    }


    public void clear() {
        if (!active()) state = OFF;
    }






    public void step(D2PointPlan.Build build, Fertigkeiten table, D2Points p, int heroClass, String name, boolean ready,
                     long now, D2PointSpend.Out out) {
        if (!active()) return;
        if (heroClass != this.heroClass || name == null || !name.equals(this.name)) {
            cancel("another character");
            return;
        }
        if (build == null) {
            state = DONE;
            why = "no build";
            return;
        }
        if (p == null || p.firstSkill < 0) return;
        boolean free = p.skillPoints > 0 || p.statPoints > 0;
        if (state == WAITING) {
            if (!ready) {
                readySince = -1;
                return;
            }
            if (readySince < 0) readySince = now;
            if (!free) {
                if (now - readySince > WAIT_MS) {
                    state = DONE;
                    why = "nothing free";
                }
                return;
            }
            state = RUNNING;
        }
        if (!spend.waiting()) {
            boolean skill = p.skillPoints > 0 && p.canSend(D2Points.KIND_SKILL)
                && D2PointPlan.nextSkill(build, table, p.level, p::baseLevel) >= 0;
            boolean stat = p.statPoints > 0 && p.canSend(D2Points.KIND_STAT) && p.baseStatsKnown()
                && D2PointPlan.nextStat(build, p.level, p.baseStats) >= 0;
            if (!skill && !stat) {
                state = DONE;
                left = p.skillPoints + p.statPoints;
                if (left > 0) why = "the plan takes no more";
                return;
            }
        }
        spend.step(build, table, p, ready, now, out);
        if ("no effect".equals(spend.why())) {
            state = DONE;
            left = p.skillPoints + p.statPoints;
            why = "no effect";
        }
    }

    public int skillsSpent() {
        return spend.skillsSpent();
    }

    public int statsSpent() {
        return spend.statsSpent();
    }


    public int left() {
        return left;
    }


    public String why() {
        return why;
    }

    @Override
    public String toString() {
        return "respec spend " + state + " " + spend + (why != null ? " (" + why + ")" : "");
    }
}
