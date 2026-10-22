package com.winlator.d2;

import android.app.Activity;
import android.view.Display;
import android.view.WindowManager;

final class D2Screen {
    private D2Screen() {}

    static float[] refreshRates(Activity activity) {
        Display.Mode[] modes = sameSizeModes(activity.getWindowManager().getDefaultDisplay());
        float[] out = new float[modes.length];
        for (int i = 0; i < modes.length; i++) out[i] = modes[i].getRefreshRate();
        return out;
    }

    private static Display.Mode[] sameSizeModes(Display display) {
        Display.Mode current = display.getMode();
        java.util.List<Display.Mode> out = new java.util.ArrayList<>();
        for (Display.Mode m : display.getSupportedModes()) if (sameSize(m, current)) out.add(m);
        return out.toArray(new Display.Mode[0]);
    }

    static float applyLimit(Activity activity, int fps) {
        if (fps == D2FrameRate.UNLIMITED) return -1;
        Display.Mode[] same = sameSizeModes(activity.getWindowManager().getDefaultDisplay());
        float[] rates = new float[same.length];
        for (int i = 0; i < same.length; i++) rates[i] = same[i].getRefreshRate();
        float rate = D2FrameRate.chooseRefresh(fps, rates);
        if (rate <= 0) return -1;
        for (Display.Mode m : same) {
            if (m.getRefreshRate() == rate) {
                WindowManager.LayoutParams params = activity.getWindow().getAttributes();
                params.preferredDisplayModeId = m.getModeId();
                activity.getWindow().setAttributes(params);
                return rate;
            }
        }
        return -1;
    }

    private static boolean sameSize(Display.Mode a, Display.Mode b) {
        return a.getPhysicalWidth() == b.getPhysicalWidth() && a.getPhysicalHeight() == b.getPhysicalHeight();
    }
}
