package com.winlator.d2;










public final class D2TeleportHold {

    public static final float CANCEL_DP = 20;

    private boolean dragged, called;
    private float dragX, dragY, dragPush;
    private float joyX, joyY, joyPush;
    private long joyUpAt = -1;


    public void start() {
        dragged = false;
        called = false;
        dragPush = 0;
    }





    public boolean drag(float dx, float dy, float dp, float radius) {
        float len = (float) Math.hypot(dx, dy), zone = CANCEL_DP * dp;
        if (len > zone) {
            dragX = dx / len;
            dragY = dy / len;
            dragPush = D2TeleportReach.joystickPush(len, zone, zone + radius);
            dragged = true;
            called = false;
            return false;
        }
        if (!dragged || called) return false;
        called = true;
        return true;
    }






    public void resume(boolean wentOut, float dx, float dy, float dp, float radius) {
        if (Math.hypot(dx, dy) > CANCEL_DP * dp) {
            drag(dx, dy, dp, radius);
            return;
        }
        if (!wentOut) return;
        dragged = true;
        called = true;
    }


    public boolean dragged() {
        return dragged;
    }


    public boolean calledOff() {
        return called;
    }


    public float[] dragAim() {
        return new float[]{dragX, dragY, dragPush};
    }


    public void joystickUp(float x, float y, float push, long now) {
        joyX = x;
        joyY = y;
        joyPush = push;
        joyUpAt = now;
    }


    public float[] joystickGrace(long now) {
        if (joyUpAt < 0 || now - joyUpAt > D2TeleportReach.STICK_GRACE_MS || now < joyUpAt) return null;
        return new float[]{joyX, joyY, joyPush};
    }





    public float[] letGo(boolean touchCancelled, float[] live, float[] graced) {
        if (touchCancelled || called) return null;
        if (dragged) return dragAim();
        return graced != null ? graced : live;
    }
}
