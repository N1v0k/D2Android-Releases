package com.winlator.d2;

import java.util.EnumSet;
import java.util.regex.Pattern;







public final class D2GpuPath {
    private D2GpuPath() {}





    public static final int MIN_VULKAN = vkVersion(1, 1, 0);








    static final EnumSet<D2DeviceCheck.Reason> PUBLIC = EnumSet.of(D2DeviceCheck.Reason.MALI,
        D2DeviceCheck.Reason.XCLIPSE, D2DeviceCheck.Reason.POWERVR);


    static final EnumSet<D2DeviceCheck.Reason> FAMILIES = EnumSet.of(D2DeviceCheck.Reason.MALI,
        D2DeviceCheck.Reason.XCLIPSE, D2DeviceCheck.Reason.POWERVR, D2DeviceCheck.Reason.OTHER_GPU);


    private static final Pattern DXT = Pattern.compile("\\bdxt\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern B_SERIES = Pattern.compile("\\bbxm\\b|b-series", Pattern.CASE_INSENSITIVE);


    static boolean powervrDxt(String renderer) {
        return renderer != null && DXT.matcher(renderer).find() && !B_SERIES.matcher(renderer).find();
    }


    public static boolean tested(D2DeviceCheck.Reason family, String renderer) {
        return PUBLIC.contains(family) && (family != D2DeviceCheck.Reason.POWERVR || powervrDxt(renderer));
    }

    public static boolean released(D2DeviceCheck.Reason family, String renderer, boolean internal) {
        return FAMILIES.contains(family) && (internal || tested(family, renderer));
    }

    public static int vkVersion(int major, int minor, int patch) {
        return (major << 22) | (minor << 12) | patch;
    }


    public static String vkName(int version) {
        return (version >>> 22) + "." + ((version >> 12) & 0x3ff) + "." + (version & 0xfff);
    }


    public static final class Tier {
        public final String name;

        public final int vortekMemoryMb;

        public final int dxvkMemoryMb;

        public final int atlasLayers;

        Tier(String name, int vortekMemoryMb, int dxvkMemoryMb, int atlasLayers) {
            this.name = name;
            this.vortekMemoryMb = vortekMemoryMb;
            this.dxvkMemoryMb = dxvkMemoryMb;
            this.atlasLayers = atlasLayers;
        }

        @Override
        public String toString() {
            return name + " vortek=" + vortekMemoryMb + "MB dxvk=" + (dxvkMemoryMb > 0 ? dxvkMemoryMb + "MB" : "-") + " atlas=" + atlasLayers;
        }
    }










    static final long SMALL_MAX = 4608L << 20, MEDIUM_MAX = 6656L << 20;
    static final Tier SMALL = new Tier("small", 1024, 512, 512);
    static final Tier MEDIUM = new Tier("medium", 2048, 1024, 1024);
    static final Tier LARGE = new Tier("large", 4096, 0, 2048);


    public static Tier tier(long totalMem) {
        if (totalMem <= 0 || totalMem <= SMALL_MAX) return SMALL;
        return totalMem <= MEDIUM_MAX ? MEDIUM : LARGE;
    }
}
