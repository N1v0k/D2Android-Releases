package com.winlator.d2;











public final class D2PotionShop {
    public static final int COMMAND = 16;
    public static final int RESERVE_MAX = 10, RESERVE_DEFAULT = 5;

    public static final int[] MIN_GOLD_STEPS = {0, 100, 250, 500, 1000, 2500, 5000, 10000, 25000, 50000, 100000};

    static final long ARRIVAL_MS = 2000;

    public static final int STATE_NONE = 0, STATE_RUNNING = 1, STATE_DONE = 2;
    public static final int END_DONE = 0, END_GOLD = 1, END_NO_WARE = 2, END_REFUSED = 3, END_CLOSED = 4, END_BAD_LIST = 5,
        END_STASH_FULL = 6, END_NO_OWN_PAGE = 7, END_TIME = 8, END_UNAVAILABLE = 9, END_INVENTORY_FULL = 10;


    public static final class Job {
        public final long id;
        public final int command, state, a, b, reason;

        public Job(long id, int command, int state, int a, int b, int reason) {
            this.id = id;
            this.command = command;
            this.state = state;
            this.a = a;
            this.b = b;
            this.reason = reason;
        }

        public boolean running() {
            return state == STATE_RUNNING;
        }
    }


    public static final class Need {
        public final int heal, mana, healBelt, manaBelt;

        Need(int heal, int mana, int healBelt, int manaBelt) {
            this.heal = heal;
            this.mana = mana;
            this.healBelt = healBelt;
            this.manaBelt = manaBelt;
        }


        public boolean nothing() {
            return heal == 0 && mana == 0 && healBelt == 0 && manaBelt == 0;
        }
    }





    public static Need need(int[] belt, int rows, int[] columnKind, int healInventory, int manaInventory, int reserveHeal,
                            int reserveMana, int freeCells) {
        rows = Math.max(1, Math.min(4, rows));
        int[] kind = new int[4];
        boolean healKnown = false, manaKnown = false;
        for (int c = 0; c < 4; c++) {
            kind[c] = belt[c] != D2ModState.BELT_NONE ? belt[c] : (columnKind != null ? columnKind[c] : 0);
            healKnown |= kind[c] == D2ModState.BELT_HEAL;
            manaKnown |= kind[c] == D2ModState.BELT_MANA;
        }
        for (int c = 0; c < 4; c++) {
            if (kind[c] != 0) continue;
            if (!healKnown) {
                kind[c] = D2ModState.BELT_HEAL;
                healKnown = true;
            }
            else if (!manaKnown) {
                kind[c] = D2ModState.BELT_MANA;
                manaKnown = true;
            }
        }
        int[] room = new int[2];
        for (int c = 0; c < 4; c++) {
            if (kind[c] != D2ModState.BELT_HEAL && kind[c] != D2ModState.BELT_MANA) continue;
            int used = 0;
            for (int r = 0; r < rows; r++) if (belt[c + 4 * r] != D2ModState.BELT_NONE) used++;
            room[kind[c] - 1] += rows - used;
        }
        int[] inventory = {healInventory, manaInventory};
        int[] reserve = {clamp(reserveHeal), clamp(reserveMana)};
        int[] buy = new int[2], toBelt = new int[2];
        int cells = Math.max(0, freeCells);
        for (int k = 0; k < 2; k++) {


            int fromInventory = Math.min(room[k], inventory[k]);
            cells += fromInventory;
            int forBelt = room[k] - fromInventory;
            int forReserve = Math.min(Math.max(0, reserve[k] - (inventory[k] - fromInventory)), cells);
            cells -= forReserve;
            buy[k] = forBelt + forReserve;
            toBelt[k] = room[k];
        }
        return new Need(buy[0], buy[1], toBelt[0], toBelt[1]);
    }

    static int clamp(int reserve) {
        return Math.max(0, Math.min(RESERVE_MAX, reserve));
    }


    public static int minGold(int step) {
        return MIN_GOLD_STEPS[Math.max(0, Math.min(MIN_GOLD_STEPS.length - 1, step))];
    }



    private long nextId;
    private long sentId, sentAt = Long.MIN_VALUE / 2;
    private boolean sentSeen, sentReported;

    public D2PotionShop(long seed) {

        nextId = (seed & 0xFFFFFFFFL) == 0 ? 1 : seed & 0xFFFFFFFFL;
    }


    public long start(long now) {
        long id = nextId;
        nextId = (nextId + 1) & 0xFFFFFFFFL;
        if (nextId == 0) nextId = 1;
        sentId = id;
        sentAt = now;
        sentSeen = sentReported = false;
        return id;
    }


    public long sentId() {
        return sentId;
    }





    public boolean busy(Job job, long now) {
        if (job != null && job.id == sentId && sentId != 0) sentSeen = true;
        if (job != null && job.running()) return true;
        return sentId != 0 && !sentSeen && now - sentAt < ARRIVAL_MS;
    }


    public Job finished(Job job) {
        if (job == null || sentId == 0 || job.id != sentId || job.state != STATE_DONE || sentReported) return null;
        sentReported = true;
        return job;
    }
}
