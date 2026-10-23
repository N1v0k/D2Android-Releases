package com.winlator.d2.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;











public final class Gruppen {

    public static final int CHAMPION = 1, BOSS = 2, DIENER = 4, SUPERUNIQUE = 8;
    static final double ABSTAND = 160, STARK_AB = 6;

    public static final class Gruppe {
        public final List<Zustand.Ding> mitglieder = new ArrayList<>();
        public double staerke, abstand = Double.MAX_VALUE, mitteX, mitteY;

        public int art;

        public int anzahl() {
            return mitglieder.size();
        }

        public boolean stark() {
            return art != 0 || staerke >= STARK_AB;
        }


        public int nahe(Zustand z, double r) {
            int n = 0;
            for (Zustand.Ding d : mitglieder) if (z.bodenAbstand(d) < r) n++;
            return n;
        }

        public boolean enthaelt(long id) {
            for (Zustand.Ding d : mitglieder) if (d.id == id) return true;
            return false;
        }


        public String text() {
            String s = "Gruppe " + anzahl() + ", Stärke " + String.format(Locale.GERMAN, "%.1f", staerke);
            if (!stark()) return s;
            return s + ", stark" + (art == SUPERUNIQUE ? " (Superunique)" : art == BOSS ? " (Boss)" : art == CHAMPION ? " (Champion)" : "");
        }
    }

    private Gruppen() {
    }

    static double gewicht(int flags) {
        if ((flags & SUPERUNIQUE) != 0) return 8;
        if ((flags & BOSS) != 0) return 6;
        if ((flags & CHAMPION) != 0) return 3;
        if ((flags & DIENER) != 0) return 1.5;
        return 1;
    }


    private static int art(int flags) {
        if ((flags & SUPERUNIQUE) != 0) return SUPERUNIQUE;
        if ((flags & BOSS) != 0) return BOSS;
        if ((flags & CHAMPION) != 0) return CHAMPION;
        return 0;
    }

    private static int rang(int art) {
        return art == SUPERUNIQUE ? 3 : art == BOSS ? 2 : art == CHAMPION ? 1 : 0;
    }





    public static List<Gruppe> bilden(Zustand z, Set<Long> ohne) {
        List<Zustand.Ding> feinde = new ArrayList<>();
        for (Zustand.Ding d : z.feinde()) if (ohne == null || !ohne.contains(d.id)) feinde.add(d);
        int n = feinde.size();
        int[] wurzel = new int[n];
        for (int i = 0; i < n; i++) wurzel[i] = i;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Zustand.Ding a = feinde.get(i), b = feinde.get(j);
                if (Math.hypot(a.x - b.x, 2.0 * (a.y - b.y)) < ABSTAND) wurzel[finde(wurzel, i)] = finde(wurzel, j);
            }
        }
        List<Gruppe> out = new ArrayList<>();
        Gruppe[] je = new Gruppe[n];
        for (int i = 0; i < n; i++) {
            int w = finde(wurzel, i);
            if (je[w] == null) {
                je[w] = new Gruppe();
                out.add(je[w]);
            }
            Gruppe g = je[w];
            Zustand.Ding d = feinde.get(i);
            g.mitglieder.add(d);
            g.staerke += gewicht(d.extra) * Math.max(0.25, Math.min(100, d.leben) / 100.0);
            g.abstand = Math.min(g.abstand, z.bodenAbstand(d));
            if (rang(art(d.extra)) > rang(g.art)) g.art = art(d.extra);
            g.mitteX += d.x;
            g.mitteY += d.y;
        }
        for (Gruppe g : out) {
            g.mitteX /= g.anzahl();
            g.mitteY /= g.anzahl();
        }
        return out;
    }

    private static int finde(int[] wurzel, int i) {
        while (wurzel[i] != i) {
            wurzel[i] = wurzel[wurzel[i]];
            i = wurzel[i];
        }
        return i;
    }


    public static Gruppe von(List<Gruppe> gruppen, long id) {
        for (Gruppe g : gruppen) if (g.enthaelt(id)) return g;
        return null;
    }
}
