package com.winlator.d2.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;










public final class ObjektBenutzen {

    public enum Stand {

        AUS,

        HIN,

        GETIPPT,

        ERFOLG,

        WEG,

        FEHLSCHLAG,

        GESPERRT
    }


    public static final long ANPEILEN_MS = 5000;

    public static final long HALTEN_MS = 3000;

    public static final double NAH_NACH_FEHLVERSUCH = 2.5;

    public static final int VERSUCHE = 3;

    static final int OBJEKT_PORTAL = 5, OBJEKT_EINGANG = 6, OBJEKT_LEICHE = 7;

    private static final double GLEICH = 0.01;

    private final Map<Long, Integer> fehlversuche = new HashMap<>();
    private final Set<Long> gesperrt = new HashSet<>();
    private long id = -1, seit, tippAm;

    private boolean getippt;
    private int gebiet;

    private Gehirn.Absicht tipp;

    public boolean ohneDurchgaenge;

    public int erfolge, fehlschlaege, sperrungen;

    public boolean aktiv() { return id >= 0; }


    public long id() { return id; }


    public long seit() { return seit; }

    public boolean gesperrt(long objekt) { return gesperrt.contains(objekt); }

    public int fehlversuche(long objekt) {
        Integer n = fehlversuche.get(objekt);
        return n == null ? 0 : n;
    }


    public boolean starten(long objekt, int imGebiet, long now) {
        if (gesperrt.contains(objekt)) return false;
        id = objekt;
        gebiet = imGebiet;
        seit = now;
        getippt = false;
        tipp = null;
        return true;
    }





    public void vergessen() {
        abbrechen();
        fehlversuche.clear();
        gesperrt.clear();
    }


    public void abbrechen() {
        id = -1;
        tipp = null;
    }





    public Stand schritt(Zustand z, Gehirn.Absicht a, long now) {
        if (id < 0) return Stand.AUS;
        if (z.gebiet != gebiet) {
            abbrechen();
            return Stand.AUS;
        }
        if (tipp != null) {

            if (!tipp.getippt) getippt = false;
            tipp = null;
        }
        Zustand.Ding d = null;
        for (Zustand.Ding o : z.objekte) if (o.id == id) d = o;
        if (d == null) {
            Stand s = getippt ? Stand.ERFOLG : Stand.WEG;
            if (s == Stand.ERFOLG) erfolge++;
            id = -1;
            return s;
        }


        boolean anpeilenUm = !getippt && now - seit > ANPEILEN_MS;
        if (getippt ? now - tippAm > HALTEN_MS : now - seit > 2 * ANPEILEN_MS) {
            int n = fehlversuche(id) + 1;
            fehlversuche.put(id, n);
            fehlschlaege++;
            long war = id;
            id = -1;
            if (n >= VERSUCHE) {
                gesperrt.add(war);
                sperrungen++;
                return Stand.GESPERRT;
            }
            return Stand.FEHLSCHLAG;
        }


        boolean fern = fehlversuche(id) > 0 && z.bodenAbstand(d) > NAH_NACH_FEHLVERSUCH * Gehirn.SUBTILE_BODEN;
        if (!getippt && !anpeilenUm && (fern || !ersterKandidat(z, d, ohneDurchgaenge))) {
            a.richtung = Math.atan2(d.y - z.py, d.x - z.px);
            return Stand.HIN;
        }
        a.richtung = Double.NaN;
        if (!getippt && a.tippen == null) {
            getippt = true;
            tippAm = now;
            tipp = a;
            a.tippen = "interact";
        }
        return Stand.GETIPPT;
    }








    public static boolean ersterKandidat(Zustand z, Zustand.Ding objekt, boolean ohneDurchgaenge) {
        double reach = (z.gh > 0 ? z.gh : 600) * 0.55, s = z.bodenAbstand(objekt);
        if (s > reach) return false;
        for (Zustand.Ding d : z.items) {
            double t = z.bodenAbstand(d);
            if (t <= reach && t * 0.8 <= s + GLEICH) return false;
        }
        for (Zustand.Ding d : z.objekte) {
            if (d.id == objekt.id) continue;
            if (ohneDurchgaenge && (d.art == OBJEKT_PORTAL || d.art == OBJEKT_EINGANG)) continue;
            if (z.istAusgelassen(d)) continue;
            double t = z.bodenAbstand(d);
            if (t <= reach && (d.art == OBJEKT_LEICHE || t <= s + GLEICH)) return false;
        }
        if (z.stadt) for (Zustand.Ding d : z.monster) {
            double t = z.bodenAbstand(d);
            if (t <= reach && t <= s + GLEICH) return false;
        }
        return true;
    }
}
