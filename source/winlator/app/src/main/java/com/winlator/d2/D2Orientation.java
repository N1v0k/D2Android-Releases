package com.winlator.d2;

import android.app.Activity;
import android.content.pm.ActivityInfo;








public final class D2Orientation {

    public static final int INSTALL = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;

    public static final int INSTALLER = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;

    private D2Orientation() {}

    public static void install(Activity activity) {
        set(activity, INSTALL);
    }

    public static void installer(Activity activity) {
        set(activity, INSTALLER);
    }

    private static void set(Activity activity, int orientation) {
        if (activity.getRequestedOrientation() != orientation) activity.setRequestedOrientation(orientation);
    }


    private static int traced = -1;


    public static synchronized void rotation(int now) {
        String note = rotationNote(traced, now);
        traced = now;
        if (note != null) D2Trace.log(note);
    }


    static float scrollShare(int scrollY, int range) {
        return range <= 0 ? 0 : Math.max(0, Math.min(1, scrollY / (float) range));
    }


    static String rotationNote(int last, int now) {
        if (now < 0 || now > 3 || now == last) return null;
        return "orientation: rotation " + now * 90 + (last < 0 ? "" : " (was " + last * 90 + ")");
    }
}
