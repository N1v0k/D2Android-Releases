package com.winlator.d2.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;




















public final class Zielpruefung {
    public static final double UMWEG_FAKTOR = 2, UMWEG_PLUS = 10;

    public static final double NAH = 2.5;

    public static final long NEU_NACH_MS = 1000;
    public static final double BEWEGT = 2.5;
    public static final int SUCHEN_JE_SCHRITT = 2, KNOTEN_MAX = 3000;




    public double suchenBisBoden = Double.MAX_VALUE;


    public static final class Urteil {

        public boolean erreichbar = true;

        public boolean geprueft;

        public boolean sicht = true;

        public Wegsuche.Punkt knoten;

        public List<Wegsuche.Punkt> weg;
        long am;
        double mx, my, fx, fy;
    }

    private final Map<Long, Urteil> urteile = new HashMap<>();

    public int suchen, unerreichbarGefunden;


    public void vergessen() {
        urteile.clear();
    }


    public Urteil urteil(long id) {
        return urteile.get(id);
    }






    public void pruefen(Zustand z, Karte k, List<Zustand.Ding> feinde, long now) {
        if (!z.weltlage() || k == null || k.level != z.gebiet) return;
        Set<Long> tuerZellen = tuerZellen(z, k);
        java.util.ArrayList<Zustand.Ding> nach = new java.util.ArrayList<>(feinde);
        nach.sort((a, b) -> Double.compare(z.bodenAbstand(a), z.bodenAbstand(b)));
        Set<Long> da = new HashSet<>();
        int gesucht = 0;
        for (Zustand.Ding d : nach) {
            da.add(d.id);
            double[] p = Tueren.welt(z, d);
            Urteil u = urteile.get(d.id);
            if (u == null) {
                u = new Urteil();
                urteile.put(d.id, u);
            }
            u.sicht = sichtfrei(k, tuerZellen, z.wx, z.wy, p[0], p[1]);
            double luft = Math.hypot(p[0] - z.wx, p[1] - z.wy);
            if (luft <= NAH && u.sicht) {
                u.erreichbar = true;
                u.geprueft = true;
                u.knoten = null;
                u.weg = java.util.Collections.emptyList();
                u.am = now;
                u.mx = p[0]; u.my = p[1]; u.fx = z.wx; u.fy = z.wy;
                continue;
            }
            boolean alt = !u.geprueft || now - u.am > NEU_NACH_MS || Math.hypot(p[0] - u.mx, p[1] - u.my) > BEWEGT
                || Math.hypot(z.wx - u.fx, z.wy - u.fy) > BEWEGT;
            if (!alt || gesucht >= SUCHEN_JE_SCHRITT || z.bodenAbstand(d) > suchenBisBoden) continue;
            gesucht++;
            suchen++;
            boolean vorher = u.erreichbar || !u.geprueft;
            weg(k, z.wx, z.wy, p[0], p[1], luft, u);
            if (vorher && !u.erreichbar) unerreichbarGefunden++;
            u.geprueft = true;
            u.am = now;
            u.mx = p[0]; u.my = p[1]; u.fx = z.wx; u.fy = z.wy;
        }
        urteile.keySet().retainAll(da);
    }


    static void weg(Karte k, double sx, double sy, double zx, double zy, double luft, Urteil u) {
        int budget = (int) Math.min(KNOTEN_MAX, 40 * luft + 200);
        Wegsuche.Weg w = Wegsuche.suche(k, (int) Math.floor(sx), (int) Math.floor(sy), (int) Math.floor(zx), (int) Math.floor(zy), budget);
        u.knoten = null;
        u.weg = null;
        if (w == null || w.abgebrochen) {
            u.erreichbar = false;
            return;
        }
        u.weg = w.knoten;
        double laenge = 0, ax = sx, ay = sy;
        for (Wegsuche.Punkt p : w.knoten) {
            laenge += Math.hypot(p.x - ax, p.y - ay);
            ax = p.x;
            ay = p.y;
            if (u.knoten == null && Math.hypot(p.x - sx, p.y - sy) > 2) u.knoten = p;
        }
        u.erreichbar = laenge <= UMWEG_FAKTOR * luft + UMWEG_PLUS;
    }







    public static boolean sichtfrei(Karte k, Set<Long> tuerZellen, double sx, double sy, double zx, double zy) {
        int x = (int) Math.floor(sx), y = (int) Math.floor(sy), bx = (int) Math.floor(zx), by = (int) Math.floor(zy);
        double dx = zx - sx, dy = zy - sy;
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1;
        double tDeltaX = dx == 0 ? Double.MAX_VALUE : Math.abs(1 / dx), tDeltaY = dy == 0 ? Double.MAX_VALUE : Math.abs(1 / dy);
        double tMaxX = dx == 0 ? Double.MAX_VALUE : (dx > 0 ? (x + 1 - sx) : (sx - x)) * tDeltaX;
        double tMaxY = dy == 0 ? Double.MAX_VALUE : (dy > 0 ? (y + 1 - sy) : (sy - y)) * tDeltaY;
        int ax = x, ay = y;
        for (int n = Math.abs(bx - x) + Math.abs(by - y) + 2; n > 0 && !(x == bx && y == by); n--) {


            if (Math.min(tMaxX, tMaxY) >= 1 - 1e-9) break;
            if (tMaxX < tMaxY) {
                x += stepX;
                tMaxX += tDeltaX;
            }
            else {
                y += stepY;
                tMaxY += tDeltaY;
            }
            if ((x == ax && y == ay) || (x == bx && y == by)) continue;
            if (k.feld(x, y) == Karte.WAND || (tuerZellen != null && tuerZellen.contains(key(x, y)))) return false;
        }
        return true;
    }


    public static Set<Long> tuerZellen(Zustand z, Karte k) {
        Set<Long> out = new HashSet<>();
        if (!z.weltlage()) return out;
        for (Zustand.Ding d : z.objekte) {
            if (!Tueren.istTuer(d)) continue;
            double[] p = Tueren.welt(z, d);
            for (int[] c : Tueren.oeffnung(k, p[0], p[1], z.wx, z.wy)) out.add(key(c[0], c[1]));
        }
        return out;
    }

    static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }
}
