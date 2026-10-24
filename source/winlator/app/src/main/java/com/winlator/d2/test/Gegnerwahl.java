package com.winlator.d2.test;

import java.util.ArrayList;
import java.util.List;











public final class Gegnerwahl {

    static final double MAX_LUFT = 120;
    static final int KANDIDATEN = 8, BUDGET_JE_SUBTILE = 40, BUDGET_MAX = 4000;

    static final int BUDGET_WAHL = 8000;

    static final double UNBEKANNT = 10_000;

    static final double GRUPPE_RADIUS = 10, GRUPPE_FAKTOR = 0.85;
    static final int GRUPPE_MAX = 3;


    public static final class Wahl {
        public final Zustand.Ding d;
        public final double x, y, luft;
        public double wert;
        public Wegsuche.Weg weg;

        Wahl(Zustand.Ding d, double x, double y, double luft) {
            this.d = d;
            this.x = x;
            this.y = y;
            this.luft = luft;
        }
    }


    public interface Filter {
        boolean auslassen(Zustand.Ding d);

        default void ohneWeg(Zustand.Ding d) {
        }
    }


    public long letzteMs, laengsteMs;

    public int unerreichbar;





    public Wahl waehlen(Zustand z, Zustand.Ziele ziele, Karte k, Zielpruefung pruefung, double abBoden, Filter filter) {
        unerreichbar = 0;
        if (!z.weltlage() || k == null || k.level != z.gebiet || k.feld((int) z.wx, (int) z.wy) == Karte.UNBEKANNT) return null;
        long t0 = System.nanoTime();
        List<Wahl> alle = new ArrayList<>();
        for (Zustand.Ding d : z.feinde()) {
            if (filter != null && filter.auslassen(d)) continue;

            Zielpruefung.Urteil u = pruefung != null ? pruefung.urteil(d.id) : null;
            if (u != null && u.geprueft && !u.erreichbar) {
                unerreichbar++;
                continue;
            }
            if (z.bodenAbstand(d) < abBoden || Gehirn.jenseits(z, ziele, d)) continue;
            double[] w = Gehirn.weltlage(z, d);
            double luft = Math.hypot(w[0] - z.wx, w[1] - z.wy);
            if (luft <= MAX_LUFT) alle.add(new Wahl(d, w[0], w[1], luft));
        }
        Wahl best = null;
        if (!alle.isEmpty()) {
            alle.sort((a, b) -> Double.compare(a.luft, b.luft));
            int rest = BUDGET_WAHL;
            for (int i = 0; i < alle.size() && i < KANDIDATEN && rest > 0; i++) {
                Wahl c = alle.get(i);
                int budget = (int) Math.min(Math.min(BUDGET_MAX, rest), BUDGET_JE_SUBTILE * Math.max(1, c.luft));
                Wegsuche.Weg w = Wegsuche.suche(k, (int) z.wx, (int) z.wy, (int) Math.round(c.x), (int) Math.round(c.y), budget);
                rest -= w != null ? w.untersucht : budget;
                if (w == null) {
                    unerreichbar++;
                    if (filter != null) filter.ohneWeg(c.d);
                    continue;
                }
                if (durchAusgang(ziele, w.knoten)) continue;
                c.weg = w;
                double laenge = w.abgebrochen ? UNBEKANNT + c.luft : laenge(z, w);
                c.wert = laenge * Math.pow(GRUPPE_FAKTOR, Math.min(GRUPPE_MAX, nachbarn(alle, c)));
                if (best == null || c.wert < best.wert) best = c;

                if (best.wert < UNBEKANNT && i + 1 < alle.size() && best.wert < alle.get(i + 1).luft * Math.pow(GRUPPE_FAKTOR, GRUPPE_MAX)) break;
            }
        }
        letzteMs = (System.nanoTime() - t0) / 1_000_000;
        laengsteMs = Math.max(laengsteMs, letzteMs);
        return best;
    }

    private static int nachbarn(List<Wahl> alle, Wahl c) {
        int n = 0;
        for (Wahl o : alle) if (o != c && Math.hypot(o.x - c.x, o.y - c.y) <= GRUPPE_RADIUS) n++;
        return n;
    }


    private static double laenge(Zustand z, Wegsuche.Weg w) {
        double sum = 0, x = z.wx, y = z.wy;
        for (Wegsuche.Punkt p : w.knoten) {
            sum += Math.hypot(p.x - x, p.y - y);
            x = p.x;
            y = p.y;
        }
        return sum;
    }


    static boolean durchAusgang(Zustand.Ziele ziele, List<Wegsuche.Punkt> knoten) {
        if (ziele == null || knoten == null) return false;
        for (Wegsuche.Punkt p : knoten) if (Gehirn.nahAusgang(ziele, p.x, p.y)) return true;
        return false;
    }
}
