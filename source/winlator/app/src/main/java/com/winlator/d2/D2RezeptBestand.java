package com.winlator.d2;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;











public final class D2RezeptBestand {
    public static final int PLACE_INVENTORY = 0, PLACE_BELT = 1, PLACE_BODY = 2, PLACE_CUBE = 3, PLACE_STASH = 4;

    public static final int PLACE_PAGE = 6;
    public static final int NONE = 0, ALMOST = 1, CHAIN = 2, STASH = 3, READY = 4;

    static final int F_ETHEREAL = 1, F_SOCKETED = 2, F_RUNEWORD = 8;


    public static final class Stueck {
        public final int id, quality, flags, place, x, y, w, h;
        public final String code;

        public final int sockets, filled, tier, level;

        public int page;
        public boolean shared;

        public Stueck(int id, String code, int quality, int flags, int place, int x, int y, int w, int h, int sockets, int filled,
                      int tier, int level) {
            this.id = id;
            this.code = code;
            this.quality = quality;
            this.flags = flags;
            this.place = place;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.sockets = sockets;
            this.filled = filled;
            this.tier = tier;
            this.level = level;
        }

        public boolean covers(int cx, int cy) {
            return cx >= x && cx < x + Math.max(1, w) && cy >= y && cy < y + Math.max(1, h);
        }
    }


    public static Stueck onPage(int id, String code, int quality, int flags, int w, int h, int sockets, int filled, int page,
                                boolean shared) {
        Stueck s = new Stueck(id, code, quality, flags, PLACE_PAGE, 0, 0, w, h, sockets, filled, -1, -1);
        s.page = page;
        s.shared = shared;
        return s;
    }


    public static final class Lage {
        public final int status;

        public final int[] have, carried;

        public final int missing, missingCount;
        public final String chainFrom;
        public final int chainCount;

        public final int inInventory, inCube, inStashPage, inMaterials;

        public int inOtherPages, page;
        public boolean shared;

        Lage(int status, int[] have, int[] carried, int missing, int missingCount, String chainFrom, int chainCount, int inInventory,
             int inCube, int inStashPage, int inMaterials) {
            this.status = status;
            this.have = have;
            this.carried = carried;
            this.missing = missing;
            this.missingCount = missingCount;
            this.chainFrom = chainFrom;
            this.chainCount = chainCount;
            this.inInventory = inInventory;
            this.inCube = inCube;
            this.inStashPage = inStashPage;
            this.inMaterials = inMaterials;
        }

        public boolean doable() {
            return status != NONE;
        }
    }

    private final List<Stueck> items;
    private final Map<String, Integer> materials;
    private final D2Gegenstaende tables;

    private final List<Stueck> pages;

    public D2RezeptBestand(List<Stueck> items, Map<String, Integer> materials, D2Gegenstaende tables) {
        this(items, materials, tables, null);
    }

    public D2RezeptBestand(List<Stueck> items, Map<String, Integer> materials, D2Gegenstaende tables, List<Stueck> pages) {
        this.pages = pages != null ? pages : new ArrayList<>();
        this.items = items != null ? items : new ArrayList<>();
        this.materials = materials != null ? materials : new HashMap<>();
        this.tables = tables;
    }

    public List<Stueck> items() {
        return items;
    }


    public List<Stueck> pages() {
        return pages;
    }


    Map<String, Integer> materials() {
        return materials;
    }


    public int material(String code) {
        Integer n = materials.get(code);
        return n != null ? n : 0;
    }




    public static List<Stueck> parse(byte[] data, int length) {
        if (data == null || length > data.length || length < 8) return null;
        try {
            java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(data, 0, length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            b.position(8 + 2 + 4 + 16 + 12 + 1 + 8 + 4);
            int places = b.get() & 0xFF;
            b.position(b.position() + places * 11 + 5);
            int carried = b.get() & 0xFF, start = b.position();
            D2ItemDetails.Item[] details = D2ItemDetails.parse(data, length);
            List<Stueck> out = new ArrayList<>();
            for (int i = 0; i < carried; i++) {
                int at = start + i * 16;
                int id = b.getInt(at);
                String code = new String(data, at + 4, 4, StandardCharsets.ISO_8859_1).trim();
                int quality = data[at + 9] & 0xFF, flags = data[at + 10] & 0xFF, place = data[at + 11] & 0xFF;
                int x = data[at + 13] & 0xFF, y = data[at + 14] & 0xFF, size = data[at + 15] & 0xFF;
                if (place != PLACE_INVENTORY && place != PLACE_CUBE && place != PLACE_STASH) continue;
                D2ItemDetails.Item d = details != null && i < details.length ? details[i] : null;
                out.add(new Stueck(id, code, quality, flags, place, x, y, size >> 4, size & 15, d != null ? d.sockets : -1,
                    d != null ? d.filled : -1, d != null ? (d.tier == D2ItemDetails.UNKNOWN ? D2Gegenstaende.NO_TIER : d.tier) : -1,
                    d != null ? d.level : -1));
            }
            if (start + carried * 16 > length) return null;
            return out;
        }
        catch (RuntimeException e) {
            return null;
        }
    }





    public static List<Stueck> parsePages(byte[] data, int length) {
        if (data == null || length > data.length || length < 8) return null;
        try {
            java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(data, 0, length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            b.position(8 + 2 + 4 + 16 + 12 + 1 + 8 + 4);
            int places = b.get() & 0xFF;
            b.position(b.position() + places * 11 + 5);
            int carried = b.get() & 0xFF;
            b.position(b.position() + carried * 16);
            int shop = b.get() & 0xFF;
            b.position(b.position() + shop * 16 + 1);
            if (!b.hasRemaining()) return null;
            int marker = b.get() & 0xFF;
            if (marker == 1) b.position(b.position() + 4 * carried);
            if (b.remaining() < 2) return null;
            int n = b.get() & 0xFF;
            b.get();
            if (b.remaining() != 16 * n) return null;
            List<Stueck> out = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int at = b.position();
                int id = b.getInt(at);
                String code = new String(data, at + 4, 4, StandardCharsets.ISO_8859_1).trim();
                int size = data[at + 10] & 0xFF;
                out.add(onPage(id, code, data[at + 8] & 0xFF, data[at + 9] & 0xFF, size >> 4, size & 15, data[at + 11] & 0xFF,
                    data[at + 12] & 0xFF, b.getShort(at + 13) & 0xFFFF, data[at + 15] != 0));
                b.position(at + 16);
            }
            return out;
        }
        catch (RuntimeException e) {
            return null;
        }
    }




    boolean fits(D2RezeptKatalog.Zutat z, Stueck s) {
        D2Gegenstaende.Item base = tables != null ? tables.get(s.code) : null;
        if (z.isBase()) return fitsBase(z, s, base);
        if (z.codes != null) {
            boolean any = false;
            for (String c : z.codes) any |= c.equals(s.code);
            if (!any) return false;
        }
        else if (z.type >= 0) {
            if (base == null || !tables.ist(base, z.type)) return false;
        }
        if (z.quality != D2Rezepte.Q_ANY && s.quality != z.quality) {

            return false;
        }
        int f = z.flags;
        if ((f & D2Rezepte.IN_NOSOCKET) != 0 && (s.sockets > 0 || (s.flags & F_SOCKETED) != 0)) return false;
        if ((f & D2Rezepte.IN_SOCKETED) != 0 && (s.flags & F_SOCKETED) == 0 && s.sockets <= 0) return false;
        if ((f & D2Rezepte.IN_ETHEREAL) != 0 && (s.flags & F_ETHEREAL) == 0) return false;
        if ((f & D2Rezepte.IN_NOT_ETHEREAL) != 0 && (s.flags & F_ETHEREAL) != 0) return false;
        int tier = s.tier >= 0 ? s.tier : base != null ? base.tier() : D2Gegenstaende.NO_TIER;
        if ((f & D2Rezepte.IN_BASIC) != 0 && tier != D2Gegenstaende.NORMAL) return false;
        if ((f & D2Rezepte.IN_EXCEPTIONAL) != 0 && tier != D2Gegenstaende.EXCEPTIONAL) return false;
        if ((f & D2Rezepte.IN_ELITE) != 0 && tier != D2Gegenstaende.ELITE) return false;


        return (f & D2Rezepte.IN_UNIQUE_SET) == 0;
    }


    boolean fitsMaterial(D2RezeptKatalog.Zutat z, String code) {
        if (z.isBase() || z.quality != D2Rezepte.Q_ANY) return false;
        if (z.codes != null) {
            for (String c : z.codes) if (c.equals(code)) return true;
            return false;
        }
        D2Gegenstaende.Item i = tables != null ? tables.get(code) : null;
        return z.type >= 0 && i != null && tables.ist(i, z.type);
    }


    private boolean fitsBase(D2RezeptKatalog.Zutat z, Stueck s, D2Gegenstaende.Item base) {
        if (base == null || s.quality < D2Rezepte.Q_LOW || s.quality > 3 || (s.flags & F_RUNEWORD) != 0) return false;
        if (s.sockets != z.baseSockets || s.filled != 0) return false;
        for (int t : z.baseTypes) if (tables.ist(base, t)) return true;
        return false;
    }


    private final Map<D2RezeptKatalog.Eintrag, Lage> lagen = new HashMap<>();


    public Lage lage(D2RezeptKatalog.Eintrag e) {
        Lage l = lagen.get(e);
        if (l == null) lagen.put(e, l = compute(e));
        return l;
    }

    private Lage compute(D2RezeptKatalog.Eintrag e) {
        Lage l = computeLage(e);

        l.inOtherPages = lastOnPages;
        if (lastOnPages > 0 && lastFirst >= 0) {
            l.page = pages.get(lastFirst).page;
            l.shared = pages.get(lastFirst).shared;
        }
        return l;
    }

    private int lastOnPages, lastFirst;

    private Lage computeLage(D2RezeptKatalog.Eintrag e) {
        int n = e.zutaten.size();
        int[] have = new int[n], carried = new int[n];
        boolean[] used = new boolean[items.size()];
        int inInv = 0, inCube = 0, inStash = 0, inMat = 0;
        int[] onPages = new int[n];
        lastFirst = -1;
        boolean[] pageUsed = new boolean[pages.size()];
        Map<String, Integer> matLeft = new java.util.TreeMap<>(materials);
        for (int k = 0; k < n; k++) {
            D2RezeptKatalog.Zutat z = e.zutaten.get(k);
            for (int i = 0; i < items.size() && carried[k] < z.count; i++) {
                if (used[i] || !fits(z, items.get(i))) continue;
                used[i] = true;
                carried[k]++;
                int p = items.get(i).place;
                if (p == PLACE_CUBE) inCube++;
                else if (p == PLACE_STASH) inStash++;
                else inInv++;
            }
            have[k] = carried[k];
            onPages[k] = 0;

            for (Map.Entry<String, Integer> m : matLeft.entrySet()) {
                if (have[k] >= z.count) break;
                if (m.getValue() <= 0 || !fitsMaterial(z, m.getKey())) continue;
                int take = Math.min(m.getValue(), z.count - have[k]);
                m.setValue(m.getValue() - take);
                have[k] += take;
                inMat += take;
            }

            for (int i = 0; i < pages.size() && have[k] < z.count; i++) {
                if (pageUsed[i] || !fits(z, pages.get(i))) continue;
                pageUsed[i] = true;
                have[k]++;
                onPages[k]++;
                if (lastFirst < 0) lastFirst = i;
            }
        }
        lastOnPages = 0;
        for (int v : onPages) lastOnPages += v;

        Map<String, Integer> lackByKind = new java.util.LinkedHashMap<>();
        Map<String, Integer> firstOfKind = new HashMap<>();
        for (int k = 0; k < n; k++) {
            D2RezeptKatalog.Zutat z = e.zutaten.get(k);
            if (have[k] >= z.count) continue;
            String kind = z.fixedCode() != null ? z.fixedCode() : "#" + k;
            lackByKind.merge(kind, z.count - have[k], Integer::sum);
            firstOfKind.putIfAbsent(kind, k);
        }
        int missingKinds = lackByKind.size(), missing = -1;
        for (String kind : lackByKind.keySet()) missing = firstOfKind.get(kind);
        if (missingKinds == 0) {
            boolean allCarried = true;
            for (int k = 0; k < n; k++) allCarried &= carried[k] >= e.zutaten.get(k).count;
            return new Lage(allCarried ? READY : STASH, have, carried, -1, 0, null, 0, inInv, inCube, inStash, inMat);
        }
        if (missingKinds > 1) return new Lage(NONE, have, carried, -1, 0, null, 0, inInv, inCube, inStash, inMat);
        String code = e.zutaten.get(missing).fixedCode();
        int lack = lackByKind.get(code != null ? code : "#" + missing);
        int[] chain = code != null ? chain(code, lack, used, matLeft) : null;
        if (chain != null) {
            return new Lage(CHAIN, have, carried, missing, lack, chainSource, chain[0], inInv, inCube, inStash, inMat);
        }
        return new Lage(ALMOST, have, carried, missing, lack, null, 0, inInv, inCube, inStash, inMat);
    }




    private final Map<String, D2RezeptKatalog.Eintrag> makers = new HashMap<>();
    private String chainSource;


    public void makers(List<D2RezeptKatalog.Eintrag> entries) {
        makers.clear();
        lagen.clear();
        for (D2RezeptKatalog.Eintrag e : entries) {
            if (e.resultCode == null || e.isRuneword()) continue;
            if (e.category != D2RezeptKatalog.CAT_RUNES && e.category != D2RezeptKatalog.CAT_GEMS) continue;
            boolean simple = true;
            for (D2RezeptKatalog.Zutat z : e.zutaten) simple &= z.fixedCode() != null && !z.fixedCode().equals(e.resultCode);
            if (simple) makers.putIfAbsent(e.resultCode, e);
        }
    }






    private int[] chain(String code, int count, boolean[] used, Map<String, Integer> matLeft) {
        Map<String, Integer> pool = new HashMap<>();
        for (int i = 0; i < items.size(); i++) if (!used[i]) pool.merge(items.get(i).code, 1, Integer::sum);
        for (Map.Entry<String, Integer> m : matLeft.entrySet()) if (m.getValue() > 0) pool.merge(m.getKey(), m.getValue(), Integer::sum);
        D2RezeptKatalog.Eintrag first = makers.get(code);
        if (first == null) return null;
        Map<String, Integer> copy = new HashMap<>(pool);
        if (!make(code, count, copy, 0)) return null;
        D2RezeptKatalog.Zutat main = first.zutaten.get(0);
        chainSource = main.fixedCode();
        return new int[]{main.count * count};
    }

    private boolean make(String code, int count, Map<String, Integer> pool, int depth) {
        int there = pool.getOrDefault(code, 0);
        if (there >= count) {
            pool.put(code, there - count);
            return true;
        }
        if (depth >= 3) return false;
        D2RezeptKatalog.Eintrag maker = makers.get(code);
        if (maker == null) return false;
        pool.put(code, 0);
        int need = count - there;
        for (D2RezeptKatalog.Zutat z : maker.zutaten) if (!make(z.fixedCode(), z.count * need, pool, depth + 1)) return false;
        return true;
    }




    public Map<String, Integer> cube() {
        Map<String, Integer> m = new java.util.LinkedHashMap<>();
        for (Stueck s : items) if (s.place == PLACE_CUBE) m.merge(s.code, 1, Integer::sum);
        return m;
    }






    public D2RezeptKatalog.Eintrag cubeRecipe(List<D2RezeptKatalog.Eintrag> entries, int[] missingOut) {
        List<Stueck> cube = new ArrayList<>();
        for (Stueck s : items) if (s.place == PLACE_CUBE) cube.add(s);
        if (cube.isEmpty()) return null;
        D2RezeptKatalog.Eintrag best = null;
        int bestMissing = Integer.MAX_VALUE;
        for (D2RezeptKatalog.Eintrag e : entries) {
            if (e.isRuneword()) continue;
            boolean[] usedCube = new boolean[cube.size()];
            int missing = 0;
            for (D2RezeptKatalog.Zutat z : e.zutaten) {
                int got = 0;
                for (int i = 0; i < cube.size() && got < z.count; i++) {
                    if (usedCube[i] || !fits(z, cube.get(i))) continue;
                    usedCube[i] = true;
                    got++;
                }
                missing += z.count - got;
            }
            boolean all = true;
            for (boolean u : usedCube) all &= u;
            if (!all) continue;
            if (missing == 0) {
                missingOut[0] = 0;
                return e;
            }
            if (missing < bestMissing) {
                bestMissing = missing;
                best = e;
            }
        }
        missingOut[0] = best != null ? bestMissing : 0;
        return best;
    }
}
