package com.winlator.d2;

import java.util.ArrayList;
import java.util.List;
















public final class D2RezeptEinlegen {

    public static final int OK = 0, RUNEWORD = 1, MISSING = 2, IN_MATERIALS = 3, CHOOSE = 4, CUBE_FULL = 5, INVENTORY_FULL = 6,
        FETCH = 7, ON_PAGE = 8;
    public static final int CUBE_W = 3, CUBE_H = 4, MAX_ITEMS = 8, CMD_CUBE = 36;

    static final int Q_NORMAL = 2;


    public static final class Plan {
        public final int outcome, zutat, count;
        public final int[] ids;

        public final boolean fromStash;

        public final java.util.Map<String, Integer> fetch;

        public final int page;
        public final boolean shared;

        Plan(int outcome, int zutat, int count, int[] ids, boolean fromStash) {
            this(outcome, zutat, count, ids, fromStash, null);
        }

        Plan(int outcome, int zutat, int count, int[] ids, boolean fromStash, java.util.Map<String, Integer> fetch) {
            this(outcome, zutat, count, ids, fromStash, fetch, 0, false);
        }

        Plan(int outcome, int zutat, int count, int[] ids, boolean fromStash, java.util.Map<String, Integer> fetch, int page,
             boolean shared) {
            this.page = page;
            this.shared = shared;
            this.fetch = fetch;
            this.outcome = outcome;
            this.zutat = zutat;
            this.count = count;
            this.ids = ids;
            this.fromStash = fromStash;
        }
    }

    private D2RezeptEinlegen() {}

    static Plan fail(int outcome, int zutat, int count) {
        return new Plan(outcome, zutat, count, null, false);
    }


    static boolean smallGoods(D2RezeptBestand.Stueck s) {
        return s.w <= 1 && s.h <= 1 && s.quality == Q_NORMAL && s.sockets <= 0 && (s.flags & D2RezeptBestand.F_SOCKETED) == 0;
    }

    private static int order(D2RezeptBestand.Stueck s) {
        return s.place == D2RezeptBestand.PLACE_CUBE ? 0 : s.place == D2RezeptBestand.PLACE_INVENTORY ? 1 : 2;
    }





    public static Plan plan(D2RezeptKatalog.Eintrag e, D2RezeptBestand b, int invW, int invH) {
        if (e.isRuneword()) return fail(RUNEWORD, -1, 0);
        List<D2RezeptBestand.Stueck> items = new ArrayList<>(b.items());

        boolean[] freeCells = new boolean[Math.max(1, invW * invH)];
        for (D2RezeptBestand.Stueck s : items) if (s.place == D2RezeptBestand.PLACE_INVENTORY) mark(freeCells, invW, invH, s.x, s.y, s.w, s.h);
        java.util.Map<String, Integer> matLeft = new java.util.TreeMap<>(b.materials());
        java.util.Map<String, Integer> fetch = new java.util.LinkedHashMap<>();
        boolean[] counted = new boolean[items.size()];
        int fetched = 0, fetchZutat = -1, fetchCount = 0;
        for (int k = 0; k < e.zutaten.size(); k++) {
            D2RezeptKatalog.Zutat z = e.zutaten.get(k);
            int fit = 0;
            for (int i = 0; i < counted.length && fit < z.count; i++) {
                D2RezeptBestand.Stueck s = items.get(i);
                if (!counted[i] && !"box".equals(s.code) && b.fits(z, s)) {
                    counted[i] = true;
                    fit++;
                }
            }
            int lack = z.count - fit;
            if (lack <= 0) continue;
            int there = 0;
            for (java.util.Map.Entry<String, Integer> m : matLeft.entrySet()) if (b.fitsMaterial(z, m.getKey())) there += m.getValue();
            if (there < lack) continue;
            for (java.util.Map.Entry<String, Integer> m : matLeft.entrySet()) {
                if (lack <= 0) break;
                if (m.getValue() <= 0 || !b.fitsMaterial(z, m.getKey())) continue;
                int take = Math.min(lack, m.getValue());
                m.setValue(m.getValue() - take);
                lack -= take;
                fetch.merge(m.getKey(), take, Integer::sum);
                if (fetchZutat < 0 || fetchZutat == k) {
                    fetchZutat = k;
                    fetchCount += take;
                }
                for (int i = 0; i < take; i++) {
                    int[] cell = takeCell(freeCells, invW, invH);
                    if (cell == null) return fail(IN_MATERIALS, k, z.count - fit);

                    items.add(new D2RezeptBestand.Stueck(-1 - fetched++, m.getKey(), Q_NORMAL, 0, D2RezeptBestand.PLACE_INVENTORY,
                        cell[0], cell[1], 1, 1, 0, 0, -1, -1));
                }
            }
        }
        boolean[] used = new boolean[items.size()];
        List<D2RezeptBestand.Stueck> chosen = new ArrayList<>();
        for (int k = 0; k < e.zutaten.size(); k++) {
            D2RezeptKatalog.Zutat z = e.zutaten.get(k);
            List<D2RezeptBestand.Stueck> fit = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                D2RezeptBestand.Stueck s = items.get(i);
                if (!used[i] && !"box".equals(s.code) && b.fits(z, s)) fit.add(s);
            }
            if (fit.size() < z.count) {
                int lack = z.count - fit.size(), stash = 0;
                for (java.util.Map.Entry<String, Integer> m : materials(b)) if (b.fitsMaterial(z, m.getKey())) stash += m.getValue();
                if (stash >= lack) return fail(IN_MATERIALS, k, lack);

                for (D2RezeptBestand.Stueck s : b.pages()) {
                    if (b.fits(z, s) && stash + countOnPages(b, z) >= lack) return new Plan(ON_PAGE, k, lack, null, false, null, s.page, s.shared);
                }
                return fail(MISSING, k, lack);
            }
            fit.sort((x, y) -> Integer.compare(order(x), order(y)));

            int inCube = 0;
            for (D2RezeptBestand.Stueck s : fit) if (s.place == D2RezeptBestand.PLACE_CUBE) inCube++;
            int rest = z.count - Math.min(inCube, z.count);
            boolean free = true, cubeFree = true;
            for (D2RezeptBestand.Stueck s : fit) {
                if (s.place != D2RezeptBestand.PLACE_CUBE) free &= smallGoods(s);
                else cubeFree &= smallGoods(s);
            }
            if (rest > 0 && !free && fit.size() - inCube > rest) return fail(CHOOSE, k, rest);
            if (!cubeFree && inCube > z.count) return fail(CHOOSE, k, z.count);
            for (int j = 0; j < z.count; j++) {
                D2RezeptBestand.Stueck s = fit.get(j);
                used[items.indexOf(s)] = true;
                chosen.add(s);
            }
        }
        if (chosen.size() > MAX_ITEMS) return fail(CUBE_FULL, -1, 0);

        boolean[] cube = new boolean[CUBE_W * CUBE_H];
        for (D2RezeptBestand.Stueck s : chosen) if (s.place == D2RezeptBestand.PLACE_CUBE) mark(cube, CUBE_W, CUBE_H, s.x, s.y, s.w, s.h);
        for (D2RezeptBestand.Stueck s : largeFirst(chosen)) {
            if (s.place != D2RezeptBestand.PLACE_CUBE && !take(cube, CUBE_W, CUBE_H, s.w, s.h)) return fail(CUBE_FULL, -1, 0);
        }


        boolean[] inv = new boolean[Math.max(1, invW * invH)];
        for (D2RezeptBestand.Stueck s : items) if (s.place == D2RezeptBestand.PLACE_INVENTORY) mark(inv, invW, invH, s.x, s.y, s.w, s.h);
        for (D2RezeptBestand.Stueck s : largeFirst(items)) {
            if (s.place == D2RezeptBestand.PLACE_CUBE && !chosen.contains(s) && !take(inv, invW, invH, s.w, s.h)) {
                return fail(INVENTORY_FULL, -1, 0);
            }
        }
        if (!fetch.isEmpty()) return new Plan(FETCH, fetchZutat, fetchCount, null, false, fetch);
        int[] ids = new int[chosen.size()];
        boolean stash = false;
        for (int i = 0; i < ids.length; i++) {
            ids[i] = chosen.get(i).id;
            stash |= chosen.get(i).place == D2RezeptBestand.PLACE_STASH;
        }
        return new Plan(OK, -1, 0, ids, stash);
    }


    static List<D2RezeptBestand.Stueck> largeFirst(List<D2RezeptBestand.Stueck> l) {
        List<D2RezeptBestand.Stueck> out = new ArrayList<>(l);
        out.sort((x, y) -> Integer.compare(Math.max(1, y.w) * Math.max(1, y.h), Math.max(1, x.w) * Math.max(1, x.h)));
        return out;
    }

    private static Iterable<java.util.Map.Entry<String, Integer>> materials(D2RezeptBestand b) {
        return b.materials().entrySet();
    }

    private static void mark(boolean[] used, int w, int h, int x0, int y0, int iw, int ih) {
        for (int y = y0; y < y0 + Math.max(1, ih) && y < h; y++)
            for (int x = x0; x < x0 + Math.max(1, iw) && x < w; x++) if (x >= 0 && y >= 0) used[y * w + x] = true;
    }


    private static int countOnPages(D2RezeptBestand b, D2RezeptKatalog.Zutat z) {
        int n = 0;
        for (D2RezeptBestand.Stueck s : b.pages()) if (b.fits(z, s)) n++;
        return n;
    }


    private static int[] takeCell(boolean[] used, int w, int h) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (!used[y * w + x]) {
                    used[y * w + x] = true;
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }


    private static boolean take(boolean[] used, int w, int h, int iw, int ih) {
        iw = Math.max(1, iw);
        ih = Math.max(1, ih);
        for (int y = 0; y + ih <= h; y++) {
            for (int x = 0; x + iw <= w; x++) {
                boolean free = true;
                for (int j = 0; j < ih && free; j++) for (int i = 0; i < iw && free; i++) free = !used[(y + j) * w + x + i];
                if (free) {
                    mark(used, w, h, x, y, iw, ih);
                    return true;
                }
            }
        }
        return false;
    }





    public static boolean inCube(List<D2RezeptBestand.Stueck> items, int[] ids) {
        if (items == null || ids == null) return false;
        int inCube = 0;
        for (D2RezeptBestand.Stueck s : items) {
            if (s.place != D2RezeptBestand.PLACE_CUBE) continue;
            boolean listed = false;
            for (int id : ids) listed |= s.id == id;
            if (!listed) return false;
            inCube++;
        }
        return inCube == ids.length;
    }


    public static byte[] payload(int[] ids, int request) {
        java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(8 + 4 * ids.length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put((byte) ids.length).put((byte) 0).put((byte) 0).put((byte) 0).putInt(request);
        for (int id : ids) b.putInt(id);
        return b.array();
    }
}
