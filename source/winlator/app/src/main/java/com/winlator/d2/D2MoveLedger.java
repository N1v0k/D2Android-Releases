package com.winlator.d2;


















public final class D2MoveLedger {

    public static final class Answer {
        public final int count, x, y, epoch, restarts, suspects;

        Answer(int count, int x, int y, int epoch, int restarts, int suspects) {
            this.count = count;
            this.x = x;
            this.y = y;
            this.epoch = epoch;
            this.restarts = restarts;
            this.suspects = suspects;
        }
    }

    private int asked;
    private Answer last = new Answer(0, 0, 0, 0, 0, 0);

    private boolean excusePending;
    private int excuseX, excuseY, excuseNear, zeroOpen;


    public synchronized void asked(boolean zero) {
        asked++;
        if (excusePending && zero) zeroOpen++;
    }

    public synchronized void asked() {
        asked(false);
    }


    public synchronized void answered(int x, int y) {
        int n = last.count + 1, epoch = last.epoch, suspects = last.suspects;
        boolean extra = asked < n;
        if (extra) {
            asked = n;
            epoch++;
        }
        if (excusePending) {
            boolean inRange = Math.abs(x - excuseX) <= excuseNear && Math.abs(y - excuseY) <= excuseNear;
            if (extra || !inRange) excusePending = false;
            else if (zeroOpen > 0) zeroOpen--;
            else {
                excusePending = false;
                suspects++;
            }
        }
        last = new Answer(n, x, y, epoch, last.restarts, suspects);
    }


    public synchronized void restart() {
        asked = last.count;
        excusePending = false;
        last = new Answer(last.count, last.x, last.y, last.epoch + 1, last.restarts + 1, last.suspects);
    }


    public synchronized void forget() {
        asked = last.count;
        excusePending = false;
    }






    public synchronized boolean excuse(int count, int landX, int landY, int near) {
        if (last.count != count || asked - last.count != 1) return false;
        asked--;
        excusePending = true;
        excuseX = landX;
        excuseY = landY;
        excuseNear = near;
        zeroOpen = 0;
        return true;
    }


    public synchronized boolean excusePending() {
        return excusePending;
    }


    public synchronized Answer last() {
        return last;
    }


    public synchronized int open(Answer a) {
        return Math.max(0, asked - a.count);
    }

    public synchronized int open() {
        return open(last);
    }
}
