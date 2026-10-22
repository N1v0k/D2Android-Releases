package com.winlator.d2;

public final class D2PointTaps {
    public static final int UNKNOWN = -1, OTHER = 0, SKILL_BOX = 1, TAB = 2;
    static final int PLUS_SLACK = 4;
    public static final int DOUBLE = 5;
    static final long SENT_MS = 3000;

    private final int[] spot = new int[4];
    private int lastStat = -1;
    private long lastUp;
    private boolean lastSingle;
    private int anchorFree = -1, sent;
    private long sentAt;

    public int skillTreeHit(D2Points p, float x, float y) {
        if (p == null || !p.skillTreeKnown()) return UNKNOWN;
        for (int column = 1; column <= 3; column++) {
            for (int row = 1; row <= 6; row++) {
                if (D2PointSpots.skillBox(p, column, row, spot) && inside(spot, x, y, 0)) return SKILL_BOX;
            }
        }
        for (int page = 1; page <= 3; page++) {
            if (D2PointSpots.tab(p, page, spot) && inside(spot, x, y, 0)) return TAB;
        }
        return OTHER;
    }

    public int treeSkillAt(D2Points p, java.util.function.IntUnaryOperator place, float x, float y) {
        if (p == null || !p.skillTreeKnown() || p.firstSkill < 0 || place == null) return -1;
        for (int column = 1; column <= 3; column++) {
            for (int row = 1; row <= 6; row++) {
                if (!D2PointSpots.skillBox(p, column, row, spot) || !inside(spot, x, y, 0)) continue;
                int want = p.tab << 16 | row << 8 | column;
                for (int i = 0; i < p.baseLevels.length; i++) {
                    if (place.applyAsInt(p.firstSkill + i) == want) return p.firstSkill + i;
                }
                return -1;
            }
        }
        return -1;
    }

    public static boolean statsKnown(D2Points p, boolean plugyLoaded) {
        return p != null && p.skillTreeKnown() && p.canSend(D2Points.KIND_STAT) && (p.statsPage >= 0 || !plugyLoaded);
    }

    public int plusHit(D2Points p, boolean plugyLoaded, int gameWidth, float x, float y) {
        if (!statsKnown(p, plugyLoaded) || p.statsPage > 0) return -1;
        for (int stat = 0; stat < 4; stat++) {
            if (D2PointSpots.statPlus(p, gameWidth, stat, spot) && inside(spot, x, y, PLUS_SLACK)) return stat;
        }
        return -1;
    }

    public int tap(int stat, int free, long now, long downAt, long doubleTapMs) {
        boolean second = lastSingle && stat == lastStat && downAt >= lastUp && downAt - lastUp <= doubleTapMs;
        lastStat = stat;
        lastUp = now;
        lastSingle = !second;
        return spend(second ? DOUBLE - 1 : 1, free, now);
    }

    public int hold(int free, long now) {
        lastSingle = false;
        return spend(Integer.MAX_VALUE, free, now);
    }

    private int spend(int want, int free, long now) {
        if (anchorFree < 0 || now - sentAt > SENT_MS) {
            anchorFree = free;
            sent = 0;
        }
        int left = Math.max(0, Math.min(free, anchorFree - sent));
        int n = Math.min(want, left);
        sent += n;
        sentAt = now;
        return n;
    }

    private static boolean inside(int[] r, float x, float y, int slack) {
        return x >= r[0] - slack && x < r[0] + r[2] + slack && y >= r[1] - slack && y < r[1] + r[3] + slack;
    }
}
