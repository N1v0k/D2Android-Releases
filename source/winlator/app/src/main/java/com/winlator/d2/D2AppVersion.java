package com.winlator.d2;






public final class D2AppVersion {
    private D2AppVersion() {}


    public static String label(String versionName) {
        if (versionName == null) return null;
        String v = versionName.trim();
        if (v.isEmpty()) return null;
        int cut = v.indexOf("-d2.");
        if (cut <= 0) return v;
        return v.substring(0, cut) + " (" + v.substring(cut + 1) + ")";
    }
}
