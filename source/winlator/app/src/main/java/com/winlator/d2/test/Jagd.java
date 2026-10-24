package com.winlator.d2.test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
















public final class Jagd implements Zwischenziel {

    static final double RADIUS = 15;

    static final long FRIST_MS = 8000;

    static final long SPERRE_MS = 60_000, IGNORIEREN_MS = 30_000;

    static final int FEHLER_PAUSE = 3;
    static final long FEHLER_FENSTER_MS = 30_000, PAUSE_MS = 20_000;

    static final double NAH = 25;

    static final int AUFGEGEBEN_JE_GEBIET = 5;








    public static volatile boolean an = true;

    private final Gehirn gehirn;
    private final Gegnerwahl wahl = new Gegnerwahl();
    private final Map<Long, Long> gesperrt = new HashMap<>();
    private final Map<Long, Integer> verschwunden = new HashMap<>();
    private final ArrayDeque<Long> fehler = new ArrayDeque<>();
    private long pauseBis;
    private double letzteNaehe = Double.MAX_VALUE;
    private int gebiet = -1, aufgegebenImGebiet;
    private Zustand.Ziele letzteZiele;

    public int gewaehlt, erreicht, aufgegeben, weg;




    public int unerreichbar, ausgang;

    private long ausgeschlossenId = -1;
    private int ausgeschlossenWarum;
    static final int AUS_UNERREICHBAR = 2, AUS_AUSGANG = 3;

    public Jagd(Gehirn gehirn) {
        this.gehirn = gehirn;
    }

    @Override
    public Ziel waehlen(Zustand z, Zustand.Ziele ziele, long now) {
        if (!an) return null;
        if (z.gebiet != gebiet) {
            gebiet = z.gebiet;
            aufgegebenImGebiet = 0;
        }
        letzteZiele = ziele;
        if (now < pauseBis || !z.weltlage() || aufgegebenImGebiet >= AUFGEGEBEN_JE_GEBIET) return null;

        Karte k = gehirn.karte();
        if (k == null || k.level != z.gebiet || k.feld((int) z.wx, (int) z.wy) == Karte.UNBEKANNT) return null;

        java.util.Set<Long> ignoriert = gehirn.uebersprungene(now);
        Gegnerwahl.Wahl best = wahl.waehlen(z, ziele, k, gehirn.zielpruefung, Gehirn.KAMPF_AN, new Gegnerwahl.Filter() {
            @Override public boolean auslassen(Zustand.Ding d) {
                if (ignoriert.contains(d.id)) return true;
                Long bis = gesperrt.get(d.id);
                return bis != null && now < bis;
            }

            @Override public void ohneWeg(Zustand.Ding d) {
                gesperrt.put(d.id, now + SPERRE_MS);
            }
        });
        if (best == null) return null;
        gewaehlt++;
        letzteNaehe = best.luft;
        return new Ziel("Jagd " + best.d.id + " (" + wahl.letzteMs + " ms)", best.d.id, best.x, best.y, RADIUS, FRIST_MS);
    }

    @Override
    public Ziel nachfuehren(Ziel ziel, Zustand z, long now) {
        for (Zustand.Ding d : z.feinde()) {
            if (d.id != ziel.id) continue;
            if (Gehirn.jenseits(z, letzteZiele, d)) return null;



            Zielpruefung.Urteil u = gehirn.zielpruefung.urteil(d.id);
            int aus = u != null && u.geprueft && !u.erreichbar ? AUS_UNERREICHBAR
                : Gegnerwahl.durchAusgang(letzteZiele, gehirn.geplanterWeg()) ? AUS_AUSGANG : 0;
            if (aus != 0) {
                ausgeschlossenId = d.id;
                ausgeschlossenWarum = aus;
                return null;
            }
            double[] w = Gehirn.weltlage(z, d);
            letzteNaehe = Math.hypot(w[0] - z.wx, w[1] - z.wy);
            return ziel.bei(w[0], w[1]);
        }
        return null;
    }

    @Override
    public void ende(Ziel ziel, int wie, long now) {
        boolean ausgeschlossen = ziel.id == ausgeschlossenId;
        ausgeschlossenId = -1;

        if (wie == ERREICHT || (wie == AUFGEGEBEN && gehirn.imKampf())) {
            erreicht++;
            return;
        }
        if (wie == AUFGEGEBEN) {
            aufgegeben++;
            aufgegebenImGebiet++;
            gesperrt.put(ziel.id, now + SPERRE_MS);
            fehler(now);
            return;
        }
        if (ausgeschlossen) {
            if (ausgeschlossenWarum == AUS_UNERREICHBAR) unerreichbar++;
            else ausgang++;
            gesperrt.put(ziel.id, now + SPERRE_MS);
            return;
        }
        weg++;
        if (letzteNaehe <= NAH) return;
        int n = verschwunden.merge(ziel.id, 1, Integer::sum);
        if (n >= 2) gesperrt.put(ziel.id, now + IGNORIEREN_MS);
        fehler(now);
    }


    public String text() {
        if (!an) return " · Jagd aus";
        return " · Jagden " + gewaehlt + " (erreicht " + erreicht + ", aufgegeben " + aufgegeben + ", weg " + weg
            + (unerreichbar > 0 ? ", unterwegs unerreichbar " + unerreichbar : "")
            + (ausgang > 0 ? ", Weg durch Ausgang " + ausgang : "") + ", Wahl bis " + wahl.laengsteMs + " ms)";
    }

    private void fehler(long now) {
        fehler.addLast(now);
        while (!fehler.isEmpty() && now - fehler.peekFirst() > FEHLER_FENSTER_MS) fehler.removeFirst();
        if (fehler.size() >= FEHLER_PAUSE) {
            pauseBis = now + PAUSE_MS;
            fehler.clear();
        }
    }
}
