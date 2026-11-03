package com.winlator.alsaserver;














public final class ALSATrackCheck {

    public static final long REMAKE_PAUSE_MS = 1000;

    private int errors;
    private long droppedBytes;
    private int lastError;
    private long lastRemakeAt = Long.MIN_VALUE / 2;
    private int deviceId;





    public boolean writeFailed(int code, int dropped, long nowMs) {
        errors++;
        lastError = code;
        if (dropped > 0) droppedBytes += dropped;
        if (nowMs - lastRemakeAt < REMAKE_PAUSE_MS) return false;
        lastRemakeAt = nowMs;
        return true;
    }


    public void device(int id) {
        if (id != 0) deviceId = id;
    }


    public void remade() {
        deviceId = 0;
    }





    public String afterResume(boolean initialized, int deviceNow) {
        int before = deviceId;
        device(deviceNow);
        if (!initialized) return "not initialized";
        if (before != 0 && deviceNow != 0 && before != deviceNow) return "device " + before + " -> " + deviceNow;
        return null;
    }


    public int deviceId() {
        return deviceId;
    }


    public String takeErrors() {
        if (errors == 0) return "none";
        String text = errors + " (last " + lastError + ", " + droppedBytes + " bytes left out)";
        errors = 0;
        droppedBytes = 0;
        return text;
    }
}
