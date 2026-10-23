package com.winlator.d2.test;

import java.util.Locale;







public final class LeerenMessung {

    static final double SPRUNG = 20;

    static final long LUECKE_MS = 1000;

    private long letzt = -1, ohneKampfMs, rueckzugMs;
    private double weg, wx = Double.NaN, wy;
    private int hindernisseStart;


    public void start(long now, int hindernisse) {
        letzt = now;
        ohneKampfMs = 0;
        rueckzugMs = 0;
        weg = 0;
        wx = Double.NaN;
        hindernisseStart = hindernisse;
    }


    public void zustand(long now, boolean kampf, double x, double y) {
        if (letzt < 0) return;
        long dt = Math.min(LUECKE_MS, Math.max(0, now - letzt));
        letzt = now;
        if (!kampf) ohneKampfMs += dt;
        if (Double.isNaN(x) || Double.isNaN(y)) return;
        if (!Double.isNaN(wx)) {
            double d = Math.hypot(x - wx, y - wy);
            if (d <= SPRUNG) weg += d;
        }
        wx = x;
        wy = y;
    }





    public void rueckzug(long now) {
        if (letzt < 0) return;
        rueckzugMs += Math.min(LUECKE_MS, Math.max(0, now - letzt));
        letzt = now;
        wx = Double.NaN;
    }

    public long rueckzugMs() {
        return rueckzugMs;
    }

    public long ohneKampfMs() {
        return ohneKampfMs;
    }

    public double weg() {
        return weg;
    }





    public String text(long aktivMs, int kills, int hindernisse) {
        long ohneRueckzug = Math.max(0, aktivMs - rueckzugMs);
        return String.format(Locale.ROOT, " · ohne Kampf %.0f %% · Rückzug %d s · Wandfolgen %d · Weg %.0f Subtiles (%.0f je Kill)",
            ohneRueckzug > 0 ? ohneKampfMs * 100.0 / ohneRueckzug : 0, rueckzugMs / 1000, hindernisse - hindernisseStart, weg,
            kills > 0 ? weg / kills : 0);
    }
}
