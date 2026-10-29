package com.winlator.d2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;












public final class D2InventoryEdge {
    private D2InventoryEdge() {}


    public static final int SMALL_W = 10, SMALL_H = 4, BIG_H = 8;
    static final int PANEL_INVENTORY = 1;


    static boolean outside(int x, int y, int w, int h) {
        return x + w > SMALL_W || y + h > SMALL_H;
    }


    static boolean outsideUnknown(int x, int y) {
        return !(x + D2StashEdge.MAX_W <= SMALL_W && y + D2StashEdge.MAX_H <= SMALL_H);
    }


    static int count(byte[] d, D2StashEdge.Sizes sizes) {
        if (d == null || d.length < 768 || le32(d, 0) != D2SaveHeader.SIGNATURE || le32(d, 4) != D2SaveHeader.VERSION) return -1;
        int list = -1;
        for (int i = 765; i + 36 <= d.length; i++) {
            if (d[i] == 'i' && d[i + 1] == 'f' && d[i + 32] == 'J' && d[i + 33] == 'M') {
                list = i + 32;
                break;
            }
        }
        if (list < 0) return -1;
        int n = 0;
        for (int i = list + 4; i + 13 < d.length; i++) {
            if (d[i] != 'J' || d[i + 1] != 'M') continue;
            long s = i * 8L;
            if (D2StashEdge.bits(d, s + 16 + 23, 1) != 1) continue;
            int version = D2StashEdge.bits(d, s + 48, 10);
            if (version != 0 && version != 1 && version != 2 && version != 100 && version != 101) continue;
            int place = D2StashEdge.bits(d, s + 58, 3), x = D2StashEdge.bits(d, s + 65, 4), y = D2StashEdge.bits(d, s + 69, 4);
            int panel = D2StashEdge.bits(d, s + 73, 3);
            if (place != D2StashEdge.PLACE_STORED || panel != PANEL_INVENTORY) continue;
            boolean ear = D2StashEdge.bits(d, s + 16 + 16, 1) == 1;
            int[] cells = ear ? new int[]{1, 1} : sizes.of(code(d, s + 76));
            if (cells != null ? outside(x, y, cells[0], cells[1]) : outsideUnknown(x, y)) n++;
        }
        return n;
    }





    public static List<D2StashEdge.Place> check(File saveDir, D2StashEdge.Sizes sizes) {
        List<D2StashEdge.Place> out = new ArrayList<>();
        File[] files = saveDir != null ? saveDir.listFiles() : null;
        if (files == null) return out;
        Arrays.sort(files);
        for (File f : files) {
            String name = f.getName();
            if (!f.isFile() || !name.toLowerCase(Locale.ROOT).endsWith(".d2s")) continue;
            int n = count(D2StashEdge.read(f), sizes);
            if (n != 0) out.add(new D2StashEdge.Place(name.substring(0, name.length() - 4), false, n < 0));
        }
        return out;
    }

    private static String code(byte[] d, long s) {
        if (s + 32 > d.length * 8L) return null;
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 4; k++) {
            int c = D2StashEdge.bits(d, s + 8 * k, 8);
            if (c == ' ' || c == 0) break;
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'z')) return null;
            sb.append((char) c);
        }
        return sb.length() == 3 || sb.length() == 4 ? sb.toString() : null;
    }

    private static int le32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
