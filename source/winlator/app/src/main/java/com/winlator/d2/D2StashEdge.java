package com.winlator.d2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

















public final class D2StashEdge {
    private D2StashEdge() {}


    public static final int SMALL_W = 6, SMALL_H = 8, SMALL_H_CLASSIC = 4, BIG = 10;
    static final int PANEL_STASH = 5, PLACE_STORED = 0, PLACE_SOCKET = 6;

    static final int MAX_W = 2, MAX_H = 4;
    private static final int ITEM_RECORD = 424, I_CODE = 0x80, I_WIDTH = 0x10F, I_HEIGHT = 0x110;
    private static final long MAX_FILE = 8L << 20;


    public static final class Place {

        public final String character;

        public final boolean hardcore;

        public final TreeSet<Integer> pages = new TreeSet<>();

        public final boolean unreadable;

        Place(String character, boolean hardcore, boolean unreadable) {
            this.character = character;
            this.hardcore = hardcore;
            this.unreadable = unreadable;
        }

        @Override
        public String toString() {
            return (character != null ? character : hardcore ? "shared (hardcore)" : "shared") + (unreadable ? " unreadable" : "")
                + (pages.isEmpty() ? "" : " pages " + pages);
        }
    }


    public static final class Sizes {
        final Map<String, int[]> cells = new HashMap<>();


        public static Sizes fromBins(byte[]... bins) {
            Sizes s = new Sizes();
            for (byte[] b : bins) {
                if (b == null || b.length < 4) continue;
                int n = le32(b, 0);
                if (n <= 0 || (long) n * ITEM_RECORD + 4 != b.length) continue;
                for (int i = 0; i < n; i++) {
                    int at = 4 + i * ITEM_RECORD;
                    String code = code(b, at + I_CODE);
                    int w = b[at + I_WIDTH] & 0xFF, h = b[at + I_HEIGHT] & 0xFF;
                    if (code != null && w >= 1 && w <= 4 && h >= 1 && h <= 4) s.cells.putIfAbsent(code, new int[]{w, h});
                }
            }
            return s;
        }

        int[] of(String code) {
            int[] c = code != null ? cells.get(code) : null;
            return c;
        }
    }





    static boolean onEdge(int x, int y, int w, int h, int smallH) {
        boolean inBig = x + w <= BIG && y + h <= BIG;
        boolean inSmall = x + w <= SMALL_W && y + h <= smallH;
        return inBig && !inSmall;
    }





    static boolean onEdgeUnknown(int x, int y, int smallH) {
        return x < BIG && y < BIG && !(x + MAX_W <= SMALL_W && y + MAX_H <= smallH);
    }





    static int edgeItems(byte[] d, int from, int to, int smallH, Sizes sizes, Set<Integer> skip) {
        int n = 0;
        for (int i = Math.max(0, from); i + 13 < Math.min(to, d.length); i++) {
            if (d[i] != 'J' || d[i + 1] != 'M' || skip.contains(i)) continue;
            long s = i * 8L;
            if (bits(d, s + 16 + 23, 1) != 1) continue;
            int version = bits(d, s + 48, 10);
            if (version != 0 && version != 1 && version != 2 && version != 100 && version != 101) continue;
            int place = bits(d, s + 58, 3), x = bits(d, s + 65, 4), y = bits(d, s + 69, 4), panel = bits(d, s + 73, 3);
            if (place != PLACE_STORED || panel != PANEL_STASH) continue;
            boolean ear = bits(d, s + 16 + 16, 1) == 1;
            int[] cells = ear ? new int[]{1, 1} : sizes.of(code(d, s + 76));
            if (cells != null ? onEdge(x, y, cells[0], cells[1], smallH) : onEdgeUnknown(x, y, smallH)) n++;
        }
        return n;
    }


    static int[] character(byte[] d, Sizes sizes) {
        if (d == null || d.length < 768 || le32(d, 0) != D2SaveHeader.SIGNATURE || le32(d, 4) != D2SaveHeader.VERSION) return null;
        boolean expansion = (d[D2SaveHeader.STATUS_AT] & D2SaveHeader.EXPANSION) != 0;
        int list = -1;

        for (int i = 765; i + 36 <= d.length; i++) {
            if (d[i] == 'i' && d[i + 1] == 'f' && d[i + 32] == 'J' && d[i + 33] == 'M') {
                list = i + 32;
                break;
            }
        }
        if (list < 0) return null;
        Set<Integer> skip = new HashSet<>();
        skip.add(list);

        for (int i = list + 1; i + 5 < d.length; i++) {
            if (d[i] == 'J' && d[i + 1] == 'M' && (d[i + 4] == 'J' && d[i + 5] == 'M' || d[i + 4] == 'j' && d[i + 5] == 'f'
                || d[i + 4] == 'k' && d[i + 5] == 'f')) skip.add(i);
        }
        int smallH = expansion ? SMALL_H : SMALL_H_CLASSIC;
        return new int[]{expansion ? 1 : 0, edgeItems(d, list + 4, d.length, smallH, sizes, skip)};
    }





    static int[] stashFile(byte[] d, int smallH, Sizes sizes) {
        if (d == null) return null;
        if (d.length == 0) return new int[0];
        boolean personal = d.length >= 6 && d[0] == 'C' && d[1] == 'S' && d[2] == 'T' && d[3] == 'M';
        boolean shared = d.length >= 6 && d[0] == 'S' && d[1] == 'S' && d[2] == 'S' && d[3] == 0;
        if (!personal && !shared) return null;
        List<Integer> starts = new ArrayList<>(), lists = new ArrayList<>();
        for (int i = 6; i + 4 < d.length; i++) {
            int jm = pageList(d, i);
            if (jm < 0) continue;
            starts.add(i);
            lists.add(jm);
            i = jm + 3;
        }
        Set<Integer> skip = new HashSet<>(lists);
        int[] out = new int[starts.size()];
        for (int p = 0; p < out.length; p++) {
            int to = p + 1 < out.length ? starts.get(p + 1) : d.length;
            out[p] = edgeItems(d, lists.get(p) + 4, to, smallH, sizes, skip);
        }
        return out;
    }


    static int pageList(byte[] d, int at) {
        if (d[at] != 'S' || d[at + 1] != 'T') return -1;
        for (int name : new int[]{at + 2, at + 6}) {
            for (int z = name; z < d.length && z <= name + 32; z++) {
                int c = d[z] & 0xFF;
                if (c == 0) {
                    if (z + 3 < d.length && d[z + 1] == 'J' && d[z + 2] == 'M') return z + 1;
                    break;
                }
                if (c < 0x20) break;
            }
        }
        return -1;
    }





    public static List<Place> check(File saveDir, Sizes sizes) {
        return check(saveDir, sizes, true);
    }





    public static List<Place> check(File saveDir, Sizes sizes, boolean plugy) {
        List<Place> out = new ArrayList<>();
        File[] files = saveDir != null ? saveDir.listFiles() : null;
        if (files == null) return out;
        Arrays.sort(files);
        Map<String, File> stashes = new HashMap<>();
        for (File f : files) {
            String lower = f.getName().toLowerCase(Locale.ROOT);
            if (f.isFile() && lower.endsWith(".d2x")) stashes.put(lower.substring(0, lower.length() - 4), f);
        }
        Set<String> seen = new HashSet<>();
        for (File f : files) {
            String name = f.getName(), lower = name.toLowerCase(Locale.ROOT);
            if (!f.isFile() || !lower.endsWith(".d2s")) continue;
            String base = name.substring(0, name.length() - 4);
            seen.add(base.toLowerCase(Locale.ROOT));
            int[] save = character(read(f), sizes);
            if (save == null) {
                out.add(new Place(base, false, true));
                continue;
            }
            Place place = new Place(base, false, false);
            boolean edge = save[1] > 0;
            File d2x = stashes.get(base.toLowerCase(Locale.ROOT));
            if (d2x != null && plugy) {
                int[] pages = stashFile(read(d2x), save[0] == 1 ? SMALL_H : SMALL_H_CLASSIC, sizes);
                if (pages == null) {
                    out.add(new Place(base, false, true));
                    continue;
                }
                for (int p = 0; p < pages.length; p++) if (pages[p] > 0) place.pages.add(p + 1);
                edge |= !place.pages.isEmpty();
            }

            else if (plugy && save[1] > 0) place.pages.add(1);
            if (edge) out.add(place);
        }

        for (File f : files) {
            String lower = f.getName().toLowerCase(Locale.ROOT);
            if (!plugy || !f.isFile() || !lower.endsWith(".sss")) continue;
            boolean hardcore = lower.contains("_hc_");
            int[] pages = stashFile(read(f), SMALL_H, sizes);
            Place place = new Place(null, hardcore, pages == null);
            if (pages != null) for (int p = 0; p < pages.length; p++) if (pages[p] > 0) place.pages.add(p + 1);
            if (place.unreadable || !place.pages.isEmpty()) out.add(place);
        }
        return out;
    }


    static byte[] read(File f) {
        long length = f.length();
        if (length > MAX_FILE) return null;
        byte[] d = new byte[(int) length];
        try (InputStream in = new FileInputStream(f)) {
            int at = 0;
            for (int n; at < d.length && (n = in.read(d, at, d.length - at)) > 0; ) at += n;
            return at == d.length ? d : null;
        }
        catch (IOException e) {
            return null;
        }
    }





    static boolean defaultOn(boolean startedBefore) {
        return !startedBefore;
    }

    private static String code(byte[] d, int at) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 4 && at + k < d.length; k++) {
            int c = d[at + k] & 0xFF;
            if (c == ' ' || c == 0) break;
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'z')) return null;
            sb.append((char) c);
        }
        return sb.length() == 3 || sb.length() == 4 ? sb.toString() : null;
    }

    private static String code(byte[] d, long s) {
        if (s + 32 > d.length * 8L) return null;
        byte[] c = new byte[4];
        for (int k = 0; k < 4; k++) c[k] = (byte) bits(d, s + 8 * k, 8);
        return code(c, 0);
    }

    static int bits(byte[] d, long start, int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            long b = start + i;
            if ((b >> 3) >= d.length) break;
            v |= ((d[(int) (b >> 3)] >> (b & 7)) & 1) << i;
        }
        return v;
    }

    private static int le32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
