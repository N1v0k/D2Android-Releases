package com.winlator.d2;

import java.util.ArrayDeque;








public final class D2Materials {
    public static final int PKT_MATERIALS = 10, CMD_MATERIALS = 21;

    public static final int ORDER_DEPOSIT = 1, ORDER_WITHDRAW = 2, ORDER_DEPOSIT_ALL = 3;

    public static final int CAP_POSSIBLE = 1, CAP_SINGLE = 2, CAP_OPEN = 4, CAP_LOCKED = 8;

    public static final int RESULT_NONE = 0, RESULT_OK = 1, RESULT_FULL = 2, RESULT_NOT_ALLOWED = 3, RESULT_LOCKED = 4,
        RESULT_BUSY = 5, RESULT_CURSOR = 7;

    public static final int SAVE_NONE = 0, SAVE_OK = 1, SAVE_NOT_SAVED = 2, SAVE_NOT_CONFIRMED = 3;

    public static final int WHY_NONE = 0, WHY_BROKEN = 1, WHY_UNSAVED = 2, WHY_UNREADABLE = 3, WHY_MISSING = 4, WHY_THREAD = 5,
        WHY_VERSION = 6;

    public static final int NO_ORIGIN = -32768;





    public static final long ANSWER_MS = 3000;

    public static final long STALE_MS = 3000;


    static final String[] CODES = {
        "r01", "r02", "r03", "r04", "r05", "r06", "r07", "r08", "r09", "r10", "r11",
        "r12", "r13", "r14", "r15", "r16", "r17", "r18", "r19", "r20", "r21", "r22",
        "r23", "r24", "r25", "r26", "r27", "r28", "r29", "r30", "r31", "r32", "r33",
        "gcv", "gfv", "gsv", "gzv", "gpv",
        "gcy", "gfy", "gsy", "gly", "gpy",
        "gcb", "gfb", "gsb", "glb", "gpb",
        "gcg", "gfg", "gsg", "glg", "gpg",
        "gcr", "gfr", "gsr", "glr", "gpr",
        "gcw", "gfw", "gsw", "glw", "gpw",
        "skc", "skf", "sku", "skl", "skz",
    };
    public static final int KINDS = CODES.length, RUNES = 33, GEMS_FIRST = 33, SKULLS_FIRST = 63;


    public static int kindOf(String code) {
        if (code == null) return -1;
        String c = code.trim();
        for (int i = 0; i < KINDS; i++) if (CODES[i].equals(c)) return i;
        return -1;
    }

    public static String code(int kind) {
        return kind >= 0 && kind < KINDS ? CODES[kind] : null;
    }





    public static String sound(int kind) {
        if (kind < 0 || kind >= KINDS) return null;
        return kind < RUNES ? "item\\rune.wav" : kind < SKULLS_FIRST ? "item\\gem.wav" : "item\\skull.wav";
    }





    public static int soundKind(int order, int arg, Snapshot s) {
        if (order == ORDER_WITHDRAW) return arg >= 0 && arg < KINDS ? arg : -1;
        if (order == ORDER_DEPOSIT_ALL && s != null) for (int k = 0; k < KINDS; k++) if (s.inventory[k] > 0) return k;
        return -1;
    }


    public static final class Snapshot {
        public final int caps, order, save;
        public final int[] stash = new int[KINDS], inventory = new int[KINDS];

        public final boolean end;
        public final int x, y, done, why;
        public final long time;

        Snapshot(int caps, int order, int save, boolean end, int x, int y, int done, int why, long time) {
            this.caps = caps;
            this.order = order;
            this.save = save;
            this.end = end;
            this.x = x;
            this.y = y;
            this.done = done;
            this.why = why;
            this.time = time;
        }

        public boolean possible() { return (caps & CAP_POSSIBLE) != 0; }
        public boolean open() { return (caps & CAP_OPEN) != 0; }
        public boolean locked() { return (caps & CAP_LOCKED) != 0; }


        public boolean entry() {
            return end && open() && (possible() || locked());
        }





        public boolean origin() {
            return end && x != NO_ORIGIN && y != NO_ORIGIN;
        }
    }







    static final int[] OTHER_LEFT_PANELS = {D2ModState.UI_CUBE, D2ModState.UI_CHARACTER, D2ModState.UI_QUEST,
        D2ModState.UI_PARTY, D2ModState.UI_MERC, D2ModState.UI_TRADE, D2ModState.UI_NPCSHOP, D2ModState.UI_ANVIL,
        D2ModState.UI_WAYPOINT, D2ModState.UI_QUESTSCROLL, D2ModState.UI_SCROLL};








    public static boolean placed(Snapshot s, int gameWidth, int gameHeight, long uiFlags) {
        if (s == null || !s.origin() || gameWidth <= 0 || gameHeight <= 0) return false;
        if ((uiFlags & 1L << D2ModState.UI_STASH) == 0) return false;
        for (int ui : OTHER_LEFT_PANELS) if ((uiFlags & 1L << ui) != 0) return false;
        int left = Math.min(WIN_X, ENTRY_X - ENTRY_MARGIN), right = Math.max(WIN_X + WIN_W, ENTRY_X + ENTRY_SIZE + ENTRY_MARGIN);
        int top = Math.min(WIN_Y, ENTRY_Y - ENTRY_MARGIN), bottom = Math.max(WIN_Y + WIN_H, ENTRY_Y + ENTRY_SIZE + ENTRY_MARGIN);
        return s.x + left >= 0 && s.x + right <= gameWidth && s.y + top >= 0 && s.y + bottom <= gameHeight;
    }


    public static Snapshot parse(byte[] d, int length, long now) {
        if (d == null || length < 12 || length > d.length || (d[5] & 0xFF) != PKT_MATERIALS) return null;
        int n = d[11] & 0xFF, end = 12 + 7 * n;
        if (length < end) return null;
        boolean tail = length >= end + 6;
        Snapshot s = new Snapshot(d[8] & 0xFF, d[9] & 0xFF, d[10] & 0xFF, tail,
            tail ? (short) u16(d, end) : NO_ORIGIN, tail ? (short) u16(d, end + 2) : NO_ORIGIN,
            tail ? d[end + 4] & 0xFF : 0, tail ? d[end + 5] & 0xFF : 0, now);
        for (int i = 0, at = 12; i < n; i++, at += 7) {
            int kind = kindOf(new String(d, at, 4, java.nio.charset.StandardCharsets.ISO_8859_1));
            if (kind < 0) continue;
            s.stash[kind] = u16(d, at + 4);
            s.inventory[kind] = d[at + 6] & 0xFF;
        }
        return s;
    }


    public static byte[] payload(int order, int arg, int count) {
        byte[] p = new byte[12];
        p[0] = (byte) order;
        for (int i = 0; i < 4; i++) {
            p[4 + i] = (byte) (arg >> (8 * i));
            p[8 + i] = (byte) (count >> (8 * i));
        }
        return p;
    }


    static final int MISC_RECORD = 424, MISC_INVFILE = 0x20, MISC_CODE = 0x80;





    public static String[] invFiles(byte[] misc) {
        if (misc == null || misc.length < 4) return null;
        long n = (misc[0] & 0xFFL) | (misc[1] & 0xFFL) << 8 | (misc[2] & 0xFFL) << 16 | (misc[3] & 0xFFL) << 24;
        if (n <= 0 || n * MISC_RECORD + 4 != misc.length) return null;
        String[] out = new String[KINDS];
        for (int i = 0; i < n; i++) {
            int at = 4 + i * MISC_RECORD;
            int kind = kindOf(new String(misc, at + MISC_CODE, 4, java.nio.charset.StandardCharsets.ISO_8859_1));
            if (kind < 0 || out[kind] != null) continue;
            int len = 0;
            while (len < 32 && misc[at + MISC_INVFILE + len] != 0) len++;
            String file = new String(misc, at + MISC_INVFILE, len, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (file.matches("[A-Za-z0-9_]+")) out[kind] = file;
        }
        return out;
    }









    public static final int ENTRY_X = 312, ENTRY_Y = 446, ENTRY_SIZE = 34;

    public static final int ENTRY_MARGIN = 2;




    public static final int[] STASH_GAME = {154, 142, 6, 8}, STASH_BIG = {96, 142, 10, 10};
    public static final int STASH_CELL = 29;





    public static final int[][] STASH_BUTTONS = {{350, 446, 36, 34}, {95, 445, 35, 34}, {178, 445, 128, 34},
        {108, 86, 34, 34}, {341, 86, 34, 34}, {150, 80, 182, 24}, {150, 106, 182, 24}};






    public static final int WIN_X = 92, WIN_Y = 70, WIN_W = 298, WIN_H = 364;
    public static final int TAB_Y = WIN_Y + 30, TAB_H = 24, TAB_W = 108;





    public static final int CELL = 30, GRID_X = 154, GRID_Y = 142;
    public static final int INFO_Y = GRID_Y + 6 * CELL + 14;

    public static final int RECIPE_Y = INFO_Y + 10, RECIPE_H = 20;


    public static boolean onRecipeLine(float lx, float ly) {
        return lx >= WIN_X + 10 && lx < WIN_X + WIN_W - 10 && ly >= RECIPE_Y - 2 && ly < RECIPE_Y + RECIPE_H + 2;
    }
    public static final int BUTTON_W = 128, BUTTON_H = 35, BUTTON_X = WIN_X + (WIN_W - BUTTON_W) / 2,
        BUTTON_Y = WIN_Y + WIN_H - BUTTON_H - 16;

    public static final int TEXT_MARGIN = 8;

    public static final int CHOICE_W = 38, CHOICE_H = 28;
    public static final int TAB_RUNES = 0, TAB_GEMS = 1;

    public static final int HIT_NONE = -1, HIT_WINDOW = -2, HIT_TAB_RUNES = -3, HIT_TAB_GEMS = -4, HIT_DEPOSIT_ALL = -5;


    public static int kindAt(int tab, int column, int row) {
        if (column < 0 || column > 5 || row < 0 || row > 5) return -1;
        if (tab == TAB_RUNES) {
            int k = row * 6 + column;
            return k < RUNES ? k : -1;
        }
        if (row < 5) return GEMS_FIRST + column * 5 + row;
        return column < 5 ? SKULLS_FIRST + column : -1;
    }


    public static int tabOf(int kind) {
        return kind < RUNES ? TAB_RUNES : TAB_GEMS;
    }


    public static int[] cellOf(int kind) {
        if (kind < 0 || kind >= KINDS) return null;
        if (kind < RUNES) return new int[]{kind % 6, kind / 6};
        if (kind < SKULLS_FIRST) return new int[]{(kind - GEMS_FIRST) / 5, (kind - GEMS_FIRST) % 5};
        return new int[]{kind - SKULLS_FIRST, 5};
    }





    public static String name(int kind, String gameText) {
        String code = code(kind);
        if (code == null) return null;
        String plain = D2ItemLabel.plainBase(gameText);
        return plain != null ? plain : code;
    }





    public static String fit(String text, java.util.function.ToIntFunction<String> width, int max) {
        if (text == null || width.applyAsInt(text) <= max) return text;
        for (int n = text.length() - 1; n > 0; n--) {
            String cut = text.substring(0, n).trim() + "...";
            if (width.applyAsInt(cut) <= max) return cut;
        }
        return "";
    }


    public static boolean onEntry(float lx, float ly) {
        return lx >= ENTRY_X - ENTRY_MARGIN && lx < ENTRY_X + ENTRY_SIZE + ENTRY_MARGIN && ly >= ENTRY_Y - ENTRY_MARGIN
            && ly < ENTRY_Y + ENTRY_SIZE + ENTRY_MARGIN;
    }

    public static boolean inWindow(float lx, float ly) {
        return lx >= WIN_X && lx < WIN_X + WIN_W && ly >= WIN_Y && ly < WIN_Y + WIN_H;
    }


    public static int hit(int tab, float lx, float ly) {
        if (!inWindow(lx, ly)) return HIT_NONE;
        if (ly >= TAB_Y - 4 && ly < TAB_Y + TAB_H + 2) {
            float left = WIN_X + (WIN_W - 2 * TAB_W - 6) / 2f;
            if (lx >= left && lx < left + TAB_W) return HIT_TAB_RUNES;
            if (lx >= left + TAB_W + 6 && lx < left + 2 * TAB_W + 6) return HIT_TAB_GEMS;
            return HIT_WINDOW;
        }
        if (lx >= GRID_X && lx < GRID_X + 6 * CELL && ly >= GRID_Y && ly < GRID_Y + 6 * CELL) {
            int kind = kindAt(tab, (int) ((lx - GRID_X) / CELL), (int) ((ly - GRID_Y) / CELL));
            return kind >= 0 ? kind : HIT_WINDOW;
        }
        if (lx >= BUTTON_X && lx < BUTTON_X + BUTTON_W && ly >= BUTTON_Y && ly < BUTTON_Y + BUTTON_H) return HIT_DEPOSIT_ALL;
        return HIT_WINDOW;
    }


    public static int[] choiceAt(int kind) {
        int[] c = cellOf(kind);
        int x = GRID_X + (c[0] + 1) * CELL + 2, y = GRID_Y + c[1] * CELL + (CELL - CHOICE_H) / 2;
        if (x + 3 * CHOICE_W > WIN_X + WIN_W) x = GRID_X + c[0] * CELL - 2 - 3 * CHOICE_W;
        return new int[]{x, y};
    }


    public static int choiceHit(int kind, int inStash, float lx, float ly) {
        int[] at = choiceAt(kind);
        if (ly < at[1] - 4 || ly >= at[1] + CHOICE_H + 4 || lx < at[0] || lx >= at[0] + 3 * CHOICE_W) return -1;
        int choice = (int) ((lx - at[0]) / CHOICE_W);
        return choice == 1 && inStash < 3 ? -1 : choice;
    }


    public static int choiceCount(int choice) {
        return choice == 0 ? 1 : choice == 1 ? 3 : 0;
    }



    public static final int NOTE_NONE = 0, NOTE_INVENTORY_FULL = 1, NOTE_STASH_FULL = 2, NOTE_NOT_NOW = 3,
        NOTE_BUSY = 4, NOTE_LOCKED_BROKEN = 5, NOTE_LOCKED_UNSAVED = 6, NOTE_LOCKED_UNREADABLE = 7, NOTE_LOCKED = 8,
        NOTE_NOT_SAVED = 9, NOTE_NOT_CONFIRMED = 10, NOTE_LOCKED_VERSION = 11, NOTE_CURSOR = 12;


    public static int resultNote(int order, int result, int why) {
        switch (result) {
            case RESULT_FULL: return order == ORDER_WITHDRAW ? NOTE_INVENTORY_FULL : NOTE_STASH_FULL;
            case RESULT_NOT_ALLOWED: return NOTE_NOT_NOW;
            case RESULT_LOCKED: return lockNote(why);
            case RESULT_BUSY: return NOTE_BUSY;
            case RESULT_CURSOR: return NOTE_CURSOR;
            default: return NOTE_NONE;
        }
    }


    public static int lockNote(int why) {
        switch (why) {
            case WHY_BROKEN: return NOTE_LOCKED_BROKEN;
            case WHY_UNSAVED: return NOTE_LOCKED_UNSAVED;
            case WHY_UNREADABLE: return NOTE_LOCKED_UNREADABLE;
            case WHY_VERSION: return NOTE_LOCKED_VERSION;
            default: return NOTE_LOCKED;
        }
    }


    public static int saveWarning(Snapshot s, long now) {
        if (s == null || now - s.time > STALE_MS) return NOTE_NONE;
        return saveBlocked(s);
    }





    public static int saveBlocked(Snapshot s) {
        if (s == null) return NOTE_NONE;
        return s.save == SAVE_NOT_SAVED ? NOTE_NOT_SAVED : s.save == SAVE_NOT_CONFIRMED ? NOTE_NOT_CONFIRMED : NOTE_NONE;
    }




    public static final class Orders {
        private final ArrayDeque<int[]> waiting = new ArrayDeque<>();
        private int[] underWay;
        private int doneAtSend, countsAtLook;
        private long sentAt, changedAt;


        public void add(int order, int arg, int count) {
            int[] last = waiting.peekLast();
            if (order == ORDER_DEPOSIT_ALL) {
                for (int[] o : waiting) if (o[0] == ORDER_DEPOSIT_ALL) return;
            }
            else if (last != null && last[0] == order && last[1] == arg && order == ORDER_WITHDRAW) {
                if (last[2] != 0) last[2] = count == 0 ? 0 : last[2] + count;
                return;
            }
            waiting.addLast(new int[]{order, arg, count});
        }





        public int add(int order, int arg, int count, boolean cursorTaken) {
            if (cursorTaken) return NOTE_CURSOR;
            add(order, arg, count);
            return NOTE_NONE;
        }





        public boolean dropForCursor(boolean cursorTaken) {
            if (!cursorTaken || waiting.isEmpty()) return false;
            waiting.clear();
            return true;
        }

        public boolean hasWaiting() {
            return !waiting.isEmpty();
        }


        public void clear() {
            waiting.clear();
            underWay = null;
        }


        public void clearWaiting() {
            waiting.clear();
        }





        public int[] finished(Snapshot s, long now) {
            if (underWay == null) return null;
            if (s != null && s.end && s.time >= sentAt && s.done != doneAtSend) {
                int[] over = {underWay[0], s.order};
                underWay = null;
                return over;
            }
            if (s != null && s.time >= sentAt && counts(s) != countsAtLook) {
                countsAtLook = counts(s);
                changedAt = Math.max(changedAt, s.time);
            }
            if (now - Math.max(sentAt, changedAt) >= ANSWER_MS) {
                int[] over = {underWay[0], RESULT_NONE};
                underWay = null;
                return over;
            }
            return null;
        }


        public int[] next(Snapshot s, long now, boolean mayGo) {
            if (underWay != null || waiting.isEmpty() || !mayGo || s == null || !s.end) return null;
            underWay = waiting.pollFirst();
            doneAtSend = s.done;
            countsAtLook = counts(s);
            sentAt = changedAt = now;
            return underWay;
        }

        private static int counts(Snapshot s) {
            return java.util.Arrays.hashCode(s.stash) * 31 + java.util.Arrays.hashCode(s.inventory);
        }
    }

    private static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }
}
