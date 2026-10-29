package com.winlator.d2.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;







public final class Wegsuche {
    public static final int WAND_NAH = 50, UNBEKANNT = 60, KNOTEN_ABSTAND = 6;

    public static final int MAX_KNOTEN = 50_000;





    public static final long MAX_MS = 250;

    public static final int RAND_AUSSEN = 2;




    public static final int ZIEL_AUSSEN_NAH = 12;

    public static final class Punkt {
        public final int x, y;
        public Punkt(int x, int y) { this.x = x; this.y = y; }
        @Override public String toString() { return x + "," + y; }
    }


    public static final class Weg {
        public final List<Punkt> knoten;
        public final int kosten, untersucht;

        public Punkt ziel;

        public final boolean abgebrochen;

        public boolean zeitAus;
        Weg(List<Punkt> knoten, int kosten, int untersucht, boolean abgebrochen) {
            this.knoten = knoten; this.kosten = kosten; this.untersucht = untersucht; this.abgebrochen = abgebrochen;
        }
    }

    private static final int[] DX = {1, -1, 0, 0, 1, 1, -1, -1}, DY = {0, 0, 1, -1, 1, -1, 1, -1};

    private static long key(int x, int y) { return ((long) x << 32) | (y & 0xFFFFFFFFL); }


    static int feldKosten(Karte k, int x, int y) {
        int f = k.feld(x, y);
        if (f == Karte.WAND) return -1;
        int extra = f == Karte.UNBEKANNT ? UNBEKANNT : 0;
        for (int i = 0; i < 8; i++) {
            if (k.feld(x + DX[i], y + DY[i]) == Karte.WAND) { extra += WAND_NAH; break; }
        }
        return extra;
    }

    public static Weg suche(Karte k, int sx, int sy, int zx, int zy) {
        return suche(k, sx, sy, zx, zy, MAX_KNOTEN);
    }

    public static Weg suche(Karte k, int sx, int sy, int zx, int zy, int maxKnoten) {
        return suche(k, sx, sy, zx, zy, maxKnoten, MAX_MS);
    }


    public static Weg suche(Karte k, int sx, int sy, int zx, int zy, int maxKnoten, long maxMs) {
        long bis = maxMs > 0 ? System.nanoTime() + maxMs * 1_000_000L : Long.MAX_VALUE;
        if (k.feld(zx, zy) == Karte.WAND) {

            Punkt p = freiNahe(k, zx, zy, 6);
            if (p == null) return null;
            zx = p.x; zy = p.y;
        }
        boolean zielAussen = k.ausserhalb(zx, zy, RAND_AUSSEN);
        Map<Long, Integer> g = new HashMap<>();
        Map<Long, Long> von = new HashMap<>();
        PriorityQueue<long[]> offen = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
        long start = key(sx, sy), ziel = key(zx, zy);
        g.put(start, 0);
        offen.add(new long[]{heuristik(sx, sy, zx, zy), start});
        int untersucht = 0;
        long best = start;
        long bestH = heuristik(sx, sy, zx, zy);
        while (!offen.isEmpty()) {
            long[] top = offen.poll();
            long cur = top[1];
            int cx = (int) (cur >> 32), cy = (int) cur;
            int gc = g.get(cur);
            if (top[0] - heuristik(cx, cy, zx, zy) > gc) continue;
            if (cur == ziel) return mitZiel(new Weg(pfad(von, start, ziel), gc, untersucht, false), zx, zy);
            if (++untersucht > maxKnoten) return mitZiel(new Weg(pfad(von, start, best), g.get(best), untersucht, true), zx, zy);
            if ((untersucht & 255) == 0 && System.nanoTime() > bis) {
                Weg w = mitZiel(new Weg(pfad(von, start, best), g.get(best), untersucht, true), zx, zy);
                w.zeitAus = true;
                return w;
            }
            long h = heuristik(cx, cy, zx, zy);
            if (h < bestH) { bestH = h; best = cur; }
            for (int i = 0; i < 8; i++) {
                int nx = cx + DX[i], ny = cy + DY[i];
                int extra = feldKosten(k, nx, ny);
                if (extra < 0) continue;

                if (extra >= UNBEKANNT && k.feld(nx, ny) == Karte.UNBEKANNT && k.ausserhalb(nx, ny, RAND_AUSSEN)
                    && !(zielAussen && Math.max(Math.abs(nx - zx), Math.abs(ny - zy)) <= ZIEL_AUSSEN_NAH)) continue;

                if (i >= 4 && (k.feld(cx + DX[i], cy) == Karte.WAND || k.feld(cx, cy + DY[i]) == Karte.WAND)) continue;
                int ng = gc + (i < 4 ? 10 : 14) + extra;
                long nk = key(nx, ny);
                Integer old = g.get(nk);
                if (old != null && old <= ng) continue;
                g.put(nk, ng);
                von.put(nk, cur);
                offen.add(new long[]{ng + heuristik(nx, ny, zx, zy), nk});
            }
        }
        return null;
    }

    private static Weg mitZiel(Weg w, int zx, int zy) { w.ziel = new Punkt(zx, zy); return w; }






    public static final double GEWICHT = 1.5;

    static long heuristik(int x, int y, int zx, int zy) {
        int dx = Math.abs(zx - x), dy = Math.abs(zy - y);
        return Math.round(GEWICHT * (10L * Math.max(dx, dy) + 4L * Math.min(dx, dy)));
    }

    static Punkt freiNahe(Karte k, int x, int y, int radius) {
        for (int r = 1; r <= radius; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    if (k.feld(x + dx, y + dy) == Karte.FREI) return new Punkt(x + dx, y + dy);
                }
            }
        }
        return null;
    }


    private static List<Punkt> pfad(Map<Long, Long> von, long start, long ziel) {
        List<Punkt> voll = new ArrayList<>();
        for (long k = ziel; k != start; k = von.get(k)) voll.add(new Punkt((int) (k >> 32), (int) k));
        Collections.reverse(voll);
        List<Punkt> out = new ArrayList<>();
        int lastDx = 0, lastDy = 0, seit = 0;
        int px = (int) (start >> 32), py = (int) start;
        for (int i = 0; i < voll.size(); i++) {
            Punkt p = voll.get(i);
            int dx = Integer.signum(p.x - px), dy = Integer.signum(p.y - py);
            boolean wechsel = i > 0 && (dx != lastDx || dy != lastDy);
            if (wechsel && !out.isEmpty() && out.get(out.size() - 1) != voll.get(i - 1)) out.add(voll.get(i - 1));
            else if (wechsel && out.isEmpty()) out.add(voll.get(i - 1));
            seit = wechsel ? 0 : seit + 1;
            if (seit >= KNOTEN_ABSTAND) { out.add(p); seit = 0; }
            lastDx = dx; lastDy = dy; px = p.x; py = p.y;
        }
        Punkt last = voll.isEmpty() ? null : voll.get(voll.size() - 1);
        if (last != null && (out.isEmpty() || out.get(out.size() - 1) != last)) out.add(last);
        return out;
    }
}
