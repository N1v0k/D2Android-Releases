package com.winlator.d2;

public final class D2ResumeState {
    static final int LENGTH = 97;
    static final int OFFSET = 74;

    public static final int STATE_NONE = 0, STATE_RUNNING = 1, STATE_SPOT = 2, STATE_WAYPOINT = 3, STATE_REFUSED = 4;
    public static final int CAPS_ALL = 7;

    public final int level, x, y;
    public final long seed;
    public final int roomX, roomY, roomW, roomH;
    public final int difficulty;
    public final int caps, state, reason;
    public final int guardTenths;

    D2ResumeState(int level, int x, int y, long seed, int roomX, int roomY, int roomW, int roomH, int difficulty, int caps,
                  int state, int reason, int guardTenths) {
        this.level = level;
        this.x = x;
        this.y = y;
        this.seed = seed;
        this.roomX = roomX;
        this.roomY = roomY;
        this.roomW = roomW;
        this.roomH = roomH;
        this.difficulty = difficulty;
        this.caps = caps;
        this.state = state;
        this.reason = reason;
        this.guardTenths = guardTenths;
    }

    public boolean placeKnown() {
        return level > 0 && x > 0 && y > 0 && seed != 0 && roomW > 0 && roomH > 0;
    }

    public static D2ResumeState parse(byte[] d, int length, int at) {
        if (at < 0 || length <= at) return null;
        int n = d[at] & 0xFF;
        if (n < LENGTH || length < at + 1 + n) return null;
        int o = at + 1 + OFFSET;
        int diff = d[o + 18] & 0xFF;
        return new D2ResumeState(u16(d, o), u16(d, o + 2), u16(d, o + 4), u32(d, o + 6), u16(d, o + 10), u16(d, o + 12),
            u16(d, o + 14), u16(d, o + 16), diff == 0xFF ? -1 : diff, d[o + 19] & 0xFF, d[o + 20] & 0xFF, d[o + 21] & 0xFF,
            d[o + 22] & 0xFF);
    }

    private static int u16(byte[] d, int o) {
        return (d[o] & 0xFF) | (d[o + 1] & 0xFF) << 8;
    }

    private static long u32(byte[] d, int o) {
        return (u16(d, o) | (long) u16(d, o + 2) << 16) & 0xFFFFFFFFL;
    }

    @Override
    public String toString() {
        return "resume level " + level + " at " + x + "," + y + " seed " + Long.toHexString(seed) + " room " + roomX + "," + roomY
            + " " + roomW + "x" + roomH + " difficulty " + difficulty + " caps " + caps + " state " + state + " reason " + reason
            + " guard " + guardTenths;
    }
}
