package com.winlator.d2;

import java.util.Locale;







public final class D2DeviceInfo {

    public static final class Facts {
        public String manufacturer, model, androidRelease, socManufacturer, socModel, hardware, gpu;
        public int sdk = -1;

        public int vulkanVersion = -1;
        public long ramTotal = -1, ramFree = -1;
        public boolean ramLow;

        public long guestBytes = -1, appBytes = -1;
        public int guestProcesses;

        public boolean pss;
    }

    private D2DeviceInfo() {}


    public static String format(Facts f) {
        StringBuilder b = new StringBuilder();
        b.append("Gerät ").append(text(join(f.manufacturer, f.model))).append(", Android ").append(text(f.androidRelease));
        if (f.sdk > 0) b.append(" (API ").append(f.sdk).append(')');
        b.append('\n');
        String soc = join(f.socManufacturer, f.socModel);
        b.append("SoC ").append(text(soc));
        if (!blank(f.hardware) && !f.hardware.equalsIgnoreCase(f.socModel == null ? "" : f.socModel)) b.append(" (Hardware ").append(f.hardware).append(')');
        b.append('\n');
        b.append("GPU ").append(text(f.gpu)).append(", Vulkan ").append(vulkan(f.vulkanVersion)).append('\n');
        b.append("RAM ").append(bytes(f.ramTotal)).append(" gesamt, ").append(bytes(f.ramFree)).append(" frei");
        if (f.ramTotal > 0) b.append(f.ramLow ? " (knapp)" : " (nicht knapp)");
        b.append('\n');
        if (f.guestBytes >= 0) {
            long all = f.guestBytes + Math.max(0, f.appBytes);
            b.append("RAM Spiel ").append(bytes(all)).append(f.pss ? " PSS" : " RSS-Summe").append(" (Gast ").append(bytes(f.guestBytes)).append(" in ")
                .append(f.guestProcesses).append(f.guestProcesses == 1 ? " Prozess" : " Prozessen").append(", App ")
                .append(bytes(f.appBytes)).append(")\n");
        }
        else b.append("RAM Spiel: kein Spiel läuft\n");
        return b.toString();
    }





    public static long pssBytes(String smapsRollup) {
        if (smapsRollup == null) return -1;
        for (String line : smapsRollup.split("\n")) {
            if (!line.startsWith("Pss:")) continue;
            String[] parts = line.substring(4).trim().split("\\s+");
            try {
                return Long.parseLong(parts[0]) * 1024;
            }
            catch (RuntimeException e) {
                return -1;
            }
        }
        return -1;
    }


    static String vulkan(int version) {
        if (version < 0) return "?";
        if (version == 0) return "keins";
        return (version >>> 22) + "." + ((version >> 12) & 0x3FF) + "." + (version & 0xFFF);
    }


    static String bytes(long value) {
        if (value < 0) return "?";
        double mb = value / (1024.0 * 1024.0);
        if (mb < 1024) return Math.round(mb) + " MB";
        return String.format(Locale.ROOT, "%.1f GB", mb / 1024.0);
    }


    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty() || s.equalsIgnoreCase("unknown");
    }

    private static String join(String a, String b) {
        if (blank(a)) return blank(b) ? "" : b.trim();
        if (blank(b)) return a.trim();

        return b.trim().regionMatches(true, 0, a.trim(), 0, a.trim().length()) ? b.trim() : a.trim() + " " + b.trim();
    }

    private static String text(String s) {
        return blank(s) ? "?" : s.trim();
    }
}
