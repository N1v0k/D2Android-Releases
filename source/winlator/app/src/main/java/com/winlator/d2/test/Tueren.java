package com.winlator.d2.test;

import java.util.ArrayList;
import java.util.List;









public final class Tueren {

    public static final int OPERATE_TUER = 8;

    public static final int ART_ANDERES = 0;

    public static final double NAH = 8;





    public static final int OEFFNUNG = 2, ENG = 4, OEFFNUNG_MAX = 6;

    public static final double KREUZT = 1.0;

    public static final int KNOTEN_VORAUS = 3;

    public static final double FEST_NAH = 4;

    private Tueren() {}

    public static boolean istTuer(Zustand.Ding d) {
        return d.art == ART_ANDERES && d.extra == OPERATE_TUER;
    }





    public static double[] welt(Zustand z, Zustand.Ding d) {
        double sx = d.x - z.px, sy = d.y - z.py;
        double a = sx / 16, b = sy / 8;
        return new double[]{z.wx + (a + b) / 2, z.wy + (b - a) / 2};
    }








    public static List<int[]> oeffnung(Karte k, double x, double y, double fx, double fy) {
        List<int[]> out = new ArrayList<>();
        int cx = (int) Math.floor(x), cy = (int) Math.floor(y);
        int[] start = null;
        int achse = 0;
        double best = Double.MAX_VALUE;
        for (int yy = cy - OEFFNUNG; yy <= cy + OEFFNUNG; yy++) {
            for (int xx = cx - OEFFNUNG; xx <= cx + OEFFNUNG; xx++) {
                int a = k.engAchse(xx, yy, ENG);
                if (a == 0) continue;
                boolean ende = a == 1 ? k.engAchse(xx, yy - 1, ENG) != 1 || k.engAchse(xx, yy + 1, ENG) != 1
                    : k.engAchse(xx - 1, yy, ENG) != 2 || k.engAchse(xx + 1, yy, ENG) != 2;
                double wert = (ende ? 0 : 100) + Math.hypot(xx + 0.5 - x, yy + 0.5 - y) - 0.01 * Math.hypot(xx + 0.5 - fx, yy + 0.5 - fy);
                if (wert < best) {
                    best = wert;
                    start = new int[]{xx, yy};
                    achse = a;
                }
            }
        }
        if (start == null) return out;
        out.add(start);
        int dx = achse == 1 ? 1 : 0, dy = achse == 1 ? 0 : 1;
        for (int seite = -1; seite <= 1; seite += 2) {
            for (int i = 1; out.size() < OEFFNUNG_MAX; i++) {
                int xx = start[0] + seite * i * dx, yy = start[1] + seite * i * dy;
                if (k.engAchse(xx, yy, ENG) != achse) break;
                out.add(new int[]{xx, yy});
            }
        }
        return out;
    }






    public static Zustand.Ding imWeg(Zustand z, Karte k, List<Wegsuche.Punkt> weg, int knoten, ObjektBenutzen benutzen, boolean festgefahren) {
        if (!z.weltlage() || k == null) return null;
        Zustand.Ding best = null;
        double bestAbstand = Double.MAX_VALUE;
        for (Zustand.Ding d : z.objekte) {
            if (!istTuer(d) || benutzen.gesperrt(d.id)) continue;
            double[] p = welt(z, d);
            double nah = Math.hypot(p[0] - z.wx, p[1] - z.wy);
            if (nah > NAH || nah >= bestAbstand) continue;
            boolean imWeg = festgefahren && nah <= FEST_NAH;
            if (!imWeg && weg != null && knoten >= 0 && knoten < weg.size()) imWeg = kreuzt(oeffnung(k, p[0], p[1], z.wx, z.wy), z.wx, z.wy, weg, knoten);
            if (imWeg) {
                bestAbstand = nah;
                best = d;
            }
        }
        return best;
    }


    static boolean kreuzt(List<int[]> oeffnung, double sx, double sy, List<Wegsuche.Punkt> weg, int knoten) {
        double ax = sx, ay = sy;
        for (int i = knoten; i < weg.size() && i <= knoten + KNOTEN_VORAUS; i++) {

            double bx = weg.get(i).x, by = weg.get(i).y;
            for (int[] o : oeffnung) if (abstandZurStrecke(o[0] + 0.5, o[1] + 0.5, ax, ay, bx, by) <= KREUZT) return true;
            ax = bx;
            ay = by;
        }
        return false;
    }


    static double abstandZurStrecke(double px, double py, double ax, double ay, double bx, double by) {
        double vx = bx - ax, vy = by - ay, len2 = vx * vx + vy * vy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * vx + (py - ay) * vy) / len2));
        return Math.hypot(px - (ax + t * vx), py - (ay + t * vy));
    }
}
