package com.winlator.d2;











public final class D2TeleportAim {
    public static final int OK = 0, CORRECTED = 1, NONE = 2;

    public static final long FRESH_MS = 300;

    public static final long ASK_MS = 100;

    public static final long RETRY_MS = 600;
    public static final int PKT_TELEPORT = 11, CMD_TELEPORT = 27;

    private D2TeleportAim() {}


    public static final class Answer {
        public final int status, screenX, screenY, tileX, tileY, fromX, fromY;

        public final boolean busy;
        public final int busyStarts;
        public final long receivedAt;

        public Answer(int status, int screenX, int screenY, int tileX, int tileY, int fromX, int fromY, boolean busy,
                      int busyStarts, long receivedAt) {
            this.status = status;
            this.screenX = screenX;
            this.screenY = screenY;
            this.tileX = tileX;
            this.tileY = tileY;
            this.fromX = fromX;
            this.fromY = fromY;
            this.busy = busy;
            this.busyStarts = busyStarts;
            this.receivedAt = receivedAt;
        }





        public boolean samePoint(Answer o) {
            return o != null && tileX == o.tileX && tileY == o.tileY && fromX == o.fromX && fromY == o.fromY
                && screenX == o.screenX && screenY == o.screenY;
        }
    }





    public static byte[] request(float dirX, float dirY, int distance) {
        float len = (float) Math.hypot(dirX, dirY);
        if (len < 1e-3f || distance <= 0) return null;
        int dx = Math.round(dirX / len * 100), dy = Math.round(dirY / len * 100), d = Math.min(0xFFFF, distance);
        return new byte[]{(byte) dx, (byte) dy, (byte) d, (byte) (d >> 8)};
    }


    public static Answer parse(byte[] d, int length, long now) {
        if (d == null || length < 25 || length > d.length || (d[5] & 0xFF) != PKT_TELEPORT) return null;
        int status = d[8] & 0xFF;
        if (status > NONE) return null;
        return new Answer(status, (short) u16(d, 9), (short) u16(d, 11), u16(d, 13), u16(d, 15), u16(d, 17), u16(d, 19),
            d[23] != 0, d[24] & 0xFF, now);
    }

    private static int u16(byte[] d, int at) {
        return (d[at] & 0xFF) | (d[at + 1] & 0xFF) << 8;
    }





    public static boolean current(Answer a, long now, long since) {
        return a != null && a.receivedAt >= since && now - a.receivedAt <= FRESH_MS;
    }


    public static boolean aimable(Answer a, long now, long since) {
        return current(a, now, since) && a.status != NONE && !(a.tileX == a.fromX && a.tileY == a.fromY);
    }






    public static boolean press(Answer a, long now, long since, Answer pressed, long pressedAt) {
        if (!aimable(a, now, since) || a.busy) return false;
        if (pressed == null) return true;
        if (a.busyStarts != pressed.busyStarts) return true;
        if (a.fromX != pressed.fromX || a.fromY != pressed.fromY) return true;
        return now - pressedAt >= RETRY_MS;
    }





    public static int marker(Answer a, long now, long since) {
        if (!current(a, now, since) || (a.tileX == a.fromX && a.tileY == a.fromY)) return NONE;
        return a.status;
    }
}
