package com.winlator.d2;












public final class D2DesktopSize {
    private D2DesktopSize() {}


    public static final int MAX_LINES = 1080;





    public static int[] of(int w, int h) {
        int width = Math.max(w, h), height = Math.min(w, h);
        int targetHeight = Math.min(height, MAX_LINES);
        int targetWidth = Math.round(targetHeight * (float) width / height);
        targetWidth -= targetWidth % 2;


        int fourThree = targetWidth * 3 / 4;
        if (fourThree < targetHeight - 1) targetHeight = fourThree;
        return new int[]{targetWidth, targetHeight};
    }





    public static String askedGame(String desktop, int lines) {
        if (lines == 0) return null;
        String[] parts = desktop.split("x");
        float aspect = Integer.parseInt(parts[0]) / (float) Integer.parseInt(parts[1]);
        int width = (int) (lines * aspect);
        width -= width % 2;
        return width + "x" + lines;
    }










    public static int[] gameFor(int desktopWidth, int desktopHeight, int askedWidth, int askedHeight) {
        int aw = Math.min(askedWidth, desktopWidth), ah = Math.min(askedHeight, desktopHeight);
        java.util.TreeSet<Long> set = new java.util.TreeSet<>();
        set.add(key(640, 480));
        set.add(key(800, 600));
        set.add(key(aw, ah));
        Long[] list = set.toArray(new Long[0]);
        boolean first640 = list[0] == key(640, 480);
        int minConfig = first640 ? 0 : 1, maxConfig = list.length + minConfig;
        int mode = 2;
        if (mode < minConfig || mode >= maxConfig) mode = minConfig;
        int id = mode == 0 ? 0 : mode + 1;
        if (id == 0) return new int[]{640, 480};
        if (id == 1) return new int[]{800, 600};
        long pick = list[first640 ? id - 1 : id - 2];
        return new int[]{(int) (pick >> 32), (int) pick};
    }

    private static long key(int w, int h) {
        return ((long) w << 32) | h;
    }
}
