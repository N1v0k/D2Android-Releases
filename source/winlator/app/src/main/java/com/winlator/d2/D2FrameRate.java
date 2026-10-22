package com.winlator.d2;

public final class D2FrameRate {
    public static final String ENV = "DXVK_FRAME_RATE";
    public static final int UNLIMITED = 0;
    public static final int[] STEPS = {UNLIMITED, 144, 120, 90, 60, 40, 30};
    static final float TOLERANCE_HZ = 0.6f;

    private D2FrameRate() {}

    public static int normalize(int stored) {
        for (int step : STEPS) if (step == stored) return stored;
        return UNLIMITED;
    }

    public static final int MAX_ENV = 1000;

    public static boolean validEnvValue(String value) {
        if (value == null || !value.matches("0|[1-9][0-9]{0,3}")) return false;
        return Integer.parseInt(value) <= MAX_ENV;
    }

    public static boolean offered(int step, float[] rates) {
        if (step == UNLIMITED || step <= 60) return true;
        return maxRate(rates) >= step - TOLERANCE_HZ;
    }

    public static int effective(int stored, float[] rates) {
        int fps = normalize(stored);
        return offered(fps, rates) ? fps : UNLIMITED;
    }

    public static float chooseRefresh(int fps, float[] rates) {
        if (fps == UNLIMITED || rates == null || rates.length == 0) return -1;
        float best = -1;
        for (float r : rates) {
            if (r < fps - TOLERANCE_HZ) continue;
            float k = Math.round(r / fps);
            if (k >= 1 && Math.abs(r - k * fps) <= TOLERANCE_HZ * k && (best < 0 || r < best)) best = r;
        }
        if (best > 0) return best;
        for (float r : rates) if (r >= fps - TOLERANCE_HZ && (best < 0 || r < best)) best = r;
        return best > 0 ? best : maxRate(rates);
    }

    static float maxRate(float[] rates) {
        float max = 0;
        if (rates != null) for (float r : rates) max = Math.max(max, r);
        return max;
    }

    public static String envValue(int stored) {
        int fps = normalize(stored);
        return fps == UNLIMITED ? null : Integer.toString(fps);
    }
}
