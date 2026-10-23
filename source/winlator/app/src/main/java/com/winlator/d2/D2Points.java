package com.winlator.d2;











public final class D2Points {

    public static final int CAP_POINTS = 0x01;

    public static final int CAP_COMPARE = 0x02;

    public static final int CAP_EQUIP = 0x04;

    public static final int CAP_SORT = 0x10;

    public static final int SORT_RUNNING = 0, SORT_DONE = 1, SORT_PARTIAL = 2, SORT_CURSOR = 3, SORT_NOT_STARTED = 4,
        SORT_NO_ROOM = 5;

    public static final int EQUIP_OK = 1, EQUIP_REFUSED = 2, EQUIP_CURSOR = 3, EQUIP_NOT_STARTED = 4;

    public static final int SKILLTREE_KNOWN = 0x01, BASE_STATS_KNOWN = 0x02;

    public static final int STAT_POSSIBLE = 0x04, SKILL_POSSIBLE = 0x08, KEY_POSSIBLE = 0x10;

    public static final int STATS_PAGE_KNOWN = 0x20;

    public static final int CMD_POINTS = 19, KIND_STAT = 0, KIND_SKILL = 1, KIND_KEY = 2;

    static final int LENGTH = 67;

    static final int LENGTH_EQUIP = 69;

    static final int LENGTH_SORT = 72;

    static final int LENGTH_STATS_PAGE = 73;

    static final int LENGTH_CAPS2 = 74;

    static final int LENGTH_ATTACK = 98;

    static final int PLACE_DPS = 99, PLACE_GOLD = 101, PLACE_GOLD_STASH = 105, LENGTH_GRID = 108;


    public final int capabilities;

    public final int counter;

    public final int features;
    public final int level, statPoints, skillPoints;

    public final int[] baseStats;

    public final int firstSkill;

    public final int[] baseLevels;

    public final int tab;

    public final int[] columnX, rowY;

    public final int equipCounter, equipResult;




    public final int sortCounter, sortState, sortMoves;
    public final boolean cursorTaken;




    public final int statsPage;

    private D2Points(int capabilities, int counter, int features, int level, int statPoints, int skillPoints,
                     int[] baseStats, int firstSkill, int[] baseLevels, int tab, int[] columnX, int[] rowY,
                     int equipCounter, int equipResult, int sortCounter, int sortState, int sortMoves,
                     boolean cursorTaken, int statsPage) {
        this.capabilities = capabilities;
        this.counter = counter;
        this.features = features;
        this.level = level;
        this.statPoints = statPoints;
        this.skillPoints = skillPoints;
        this.baseStats = baseStats;
        this.firstSkill = firstSkill;
        this.baseLevels = baseLevels;
        this.tab = tab;
        this.columnX = columnX;
        this.rowY = rowY;
        this.equipCounter = equipCounter;
        this.equipResult = equipResult;
        this.sortCounter = sortCounter;
        this.sortState = sortState;
        this.sortMoves = sortMoves;
        this.cursorTaken = cursorTaken;
        this.statsPage = statsPage;
    }

    public boolean skillTreeKnown() {
        return (features & SKILLTREE_KNOWN) != 0;
    }

    public boolean baseStatsKnown() {
        return (features & BASE_STATS_KNOWN) != 0;
    }


    public boolean canSend(int kind) {
        return kind >= KIND_STAT && kind <= KIND_KEY && (features & (STAT_POSSIBLE << kind)) != 0;
    }





    public static byte[] command(int kind, int what, int key) {
        return new byte[]{'D', '2', 'A', 'C', 1, (byte) CMD_POINTS, 0, 0, (byte) kind, (byte) what, (byte) (what >> 8),
            (byte) (kind == KIND_KEY ? key : 0)};
    }






    public static byte[] command(int kind, int what, int key, boolean move) {
        byte[] plain = command(kind, what, key);
        if (!move || kind != KIND_KEY) return plain;
        byte[] out = java.util.Arrays.copyOf(plain, plain.length + 1);
        out[plain.length] = 1;
        return out;
    }





    public boolean sortSettled() {
        return sortCounter >= 0 && sortState != SORT_RUNNING && !cursorTaken;
    }






    public static boolean inInventoryWindow(float x, float y, float scale, float viewLeft, float viewTop, int gameWidth,
                                            int gameHeight) {
        if (scale <= 0 || gameWidth <= 0 || gameHeight <= 0) return false;
        float gx = (x - viewLeft) / scale, gy = (y - viewTop) / scale;
        float left = (gameWidth - 800) / 2f, top = (gameHeight - 600) / 2f;
        return gx >= left + 400 && gx < left + 800 && gy >= top && gy < top + 600;
    }






    public static boolean ownSkill(D2Points points, int skill) {
        if (skill == 0) return true;
        return points != null && points.firstSkill >= 0 && points.baseLevel(skill) > 0;
    }


    public int baseLevel(int skill) {
        int i = skill - firstSkill;
        return firstSkill >= 0 && i >= 0 && i < baseLevels.length ? baseLevels[i] : 0;
    }





    public static int parseCapabilities(byte[] d, int length, int at) {
        if (at < 0 || length <= at + 1) return -1;
        int n = d[at] & 0xFF;
        return n >= 1 && length >= at + 1 + n ? d[at + 1] & 0xFF : -1;
    }






    public static int parseCapabilities2(byte[] d, int length, int at) {
        if (at < 0 || length <= at) return -1;
        int n = d[at] & 0xFF;
        return n >= LENGTH_CAPS2 && length >= at + 1 + n ? d[at + LENGTH_CAPS2] & 0xFF : -1;
    }






    public static int parseAttack(byte[] d, int length, int at) {
        if (at < 0 || length <= at) return D2AimReach.ATTACK_UNKNOWN;
        int n = d[at] & 0xFF;
        if (n < LENGTH_ATTACK || length < at + 1 + n) return D2AimReach.ATTACK_UNKNOWN;
        int v = d[at + LENGTH_ATTACK] & 0xFF;
        return v == D2AimReach.ATTACK_STRIKES || v == D2AimReach.ATTACK_SHOOTS ? v : D2AimReach.ATTACK_UNKNOWN;
    }





    public static int[] parseGrid(byte[] d, int length, int at) {
        if (at < 0 || length <= at) return null;
        int n = d[at] & 0xFF;
        if (n < LENGTH_GRID || length < at + 1 + n) return null;
        int dps = (d[at + PLACE_DPS] & 0xFF) | (d[at + PLACE_DPS + 1] & 0xFF) << 8;
        return new int[]{dps == 0xFFFF ? -1 : dps, s32(d, at + PLACE_GOLD), s32(d, at + PLACE_GOLD_STASH)};
    }


    static final int PLACE_XP = 109, PLACE_XP_FROM = 113, PLACE_XP_TO = 117, LENGTH_XP = 120;





    public static long[] parseExperience(byte[] d, int length, int at) {
        if (at < 0 || length <= at) return null;
        int n = d[at] & 0xFF;
        if (n < LENGTH_XP || length < at + 1 + n) return null;
        return new long[]{s32(d, at + PLACE_XP) & 0xFFFFFFFFL, s32(d, at + PLACE_XP_FROM) & 0xFFFFFFFFL,
            s32(d, at + PLACE_XP_TO) & 0xFFFFFFFFL};
    }

    private static int s32(byte[] d, int at) {
        return (d[at] & 0xFF) | (d[at + 1] & 0xFF) << 8 | (d[at + 2] & 0xFF) << 16 | (d[at + 3] & 0xFF) << 24;
    }


    public static D2Points parse(byte[] d, int length, int at) {
        if (at < 0 || length <= at) return null;
        int n = d[at] & 0xFF;
        if (n < LENGTH || length < at + 1 + n) return null;
        int p = at + 1;
        int capabilities = d[p] & 0xFF, counter = d[p + 1] & 0xFF, features = d[p + 2] & 0xFF, level = d[p + 3] & 0xFF;
        int statPoints = u16(d, p + 4), skillPoints = u16(d, p + 6);
        int[] stats = new int[4];
        for (int i = 0; i < 4; i++) stats[i] = u16(d, p + 8 + i * 2);
        int first = u16(d, p + 16);
        int[] levels = new int[30];
        for (int i = 0; i < 30; i++) levels[i] = d[p + 18 + i] & 0xFF;
        int tab = d[p + 48] & 0xFF;
        int[] columns = new int[3], rows = new int[6];
        for (int i = 0; i < 3; i++) columns[i] = (short) u16(d, p + 49 + i * 2);
        for (int i = 0; i < 6; i++) rows[i] = (short) u16(d, p + 55 + i * 2);
        int page = n >= LENGTH_STATS_PAGE && (features & STATS_PAGE_KNOWN) != 0 ? d[p + LENGTH_STATS_PAGE - 1] & 0xFF : 0xFF;
        return new D2Points(capabilities, counter, features, level, statPoints, skillPoints, stats,
            first == 0xFFFF ? -1 : first, levels, tab >= 1 && tab <= 3 ? tab : 0, columns, rows,
            n >= LENGTH_EQUIP ? d[p + 67] & 0xFF : -1, n >= LENGTH_EQUIP ? d[p + 68] & 0xFF : -1,
            n >= LENGTH_SORT ? d[p + 69] & 0xFF : -1, n >= LENGTH_SORT ? d[p + 70] & 0x7F : -1,
            n >= LENGTH_SORT ? d[p + 71] & 0xFF : -1, n >= LENGTH_SORT && (d[p + 70] & 0x80) != 0,
            page == 0xFF ? -1 : page);
    }

    private static int u16(byte[] d, int at) {
        return (d[at] & 0xFF) | (d[at + 1] & 0xFF) << 8;
    }

    @Override
    public String toString() {
        return "points level " + level + ", free " + statPoints + "/" + skillPoints + ", base "
            + java.util.Arrays.toString(baseStats) + ", first " + firstSkill + ", tab " + tab + ", counter " + counter
            + ", features " + features + ", caps " + capabilities + ", stats page " + statsPage;
    }
}
