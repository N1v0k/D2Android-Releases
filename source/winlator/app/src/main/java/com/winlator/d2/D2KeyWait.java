package com.winlator.d2;

import java.util.ArrayDeque;
import java.util.Iterator;












final class D2KeyWait {

    static final long FALLBACK_MS = 80;

    static final long MAX_MS = 300;

    static final long POLL_MS = 20;


    static final int WAIT = 0;

    static final int SHOWN = 1;

    static final int TIMED = 2;

    private static final class Key {
        final long ticket;
        final int skill;
        final boolean left;
        final long sentAt;

        Key(long ticket, int skill, boolean left, long sentAt) {
            this.ticket = ticket;
            this.skill = skill;
            this.left = left;
            this.sentAt = sentAt;
        }
    }


    private final ArrayDeque<Key> unseen = new ArrayDeque<>();
    private long nextTicket = 1;


    long sent(int skill, boolean left, long now) {
        long ticket = nextTicket++;
        unseen.add(new Key(ticket, skill, left, now));
        return ticket;
    }





    void state(int leftSkill, int rightSkill, long now) {
        expire(now);
        while (!unseen.isEmpty()) {
            Key k = unseen.peek();
            if (k.skill < 0 || k.skill != (k.left ? leftSkill : rightSkill)) break;
            unseen.poll();
        }
    }







    int castNow(long ticket, int skill, int current, long sentAt, long now) {
        expire(now);
        boolean open = false;
        for (Key k : unseen) if (k.ticket == ticket) open = true;
        if (!open) return now - sentAt >= MAX_MS || (skill < 0 && now - sentAt >= FALLBACK_MS) ? TIMED : SHOWN;
        if (skill >= 0 && current == skill && now - sentAt >= FALLBACK_MS) return TIMED;
        return WAIT;
    }


    private void expire(long now) {
        for (Iterator<Key> it = unseen.iterator(); it.hasNext(); ) {
            Key k = it.next();
            if (now - k.sentAt >= MAX_MS || (k.skill < 0 && now - k.sentAt >= FALLBACK_MS)) it.remove();
        }
    }
}
