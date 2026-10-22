package com.winlator.d2.test;

public final class Puffer {
    public static final double FEIND_ABSTAND = 400;
    public static final long SPERRE_MS = 300;
    static final long BESTAETIGUNG_MS = 1500, PAUSE_MS = 60_000;
    static final int VERSUCHE = 3;
    public static final int ENERGIESCHILD = 58, GEWITTER = 57, KAMPFRUF = 155;
    public static final int TOTENBESCHWOERER = 2, KNOCHENRUESTUNG = 68;

    static int[][] gruppen(int klasse) {
        switch (klasse) {
            case Kampfwahl.ZAUBERIN:
                return new int[][]{{Kampfwahl.FROSTRUESTUNG, Kampfwahl.SHIVER, Kampfwahl.CHILLING}, {ENERGIESCHILD}, {GEWITTER}};
            case Kampfwahl.BARBAR:
                return new int[][]{{Kampfwahl.SCHREI}, {Kampfwahl.KAMPFBEFEHLE}, {KAMPFRUF}};
            case TOTENBESCHWOERER:
                return new int[][]{{KNOCHENRUESTUNG}};
            default:
                return new int[0][];
        }
    }

    private int zusatzKlasse = -2;
    private int[][] zusatzGruppen;

    public void zusatz(int klasse, int[] puffer) {
        java.util.List<int[]> out = new java.util.ArrayList<>(java.util.Arrays.asList(gruppen(klasse)));
        for (int skill : puffer != null ? puffer : new int[0]) {
            boolean drin = false;
            for (int[] g : out) for (int s : g) drin |= s == skill;
            if (!drin && out.size() < 8) out.add(new int[]{skill});
        }
        zusatzKlasse = klasse;
        zusatzGruppen = out.toArray(new int[0][]);
    }

    private int[][] gruppenFuer(int klasse) {
        return klasse == zusatzKlasse && zusatzGruppen != null ? zusatzGruppen : gruppen(klasse);
    }

    private final long[] tippAm = new long[8], pauseBis = new long[8];
    private final int[] fehl = new int[8];
    private int klasse = -2;
    private int angefragt = -1;
    public String grund = "";
    public int tipps, bestaetigt, pausen;
    public String befund;

    private static int slot(Zustand z, Zustand.Layout l, Fertigkeiten t, int[] gruppe) {
        int best = -1;
        double bestCost = -1;
        for (int skill : gruppe) {
            int slot = Kampfwahl.slot(z, l, t, skill);
            if (slot < 0 || !t.get(skill).hatZustand()) continue;
            double cost = Kampfwahl.kosten(z, slot, t.get(skill));
            if (cost > bestCost) {
                bestCost = cost;
                best = slot;
            }
        }
        return best;
    }

    public String tippen(Zustand z, Zustand.Layout l, Fertigkeiten t, double feindAbstand, long now) {
        if (t == null || !z.fkBekannt || z.puffer == null || z.klasse < 0) return null;
        if (z.klasse != klasse) {
            klasse = z.klasse;
            java.util.Arrays.fill(tippAm, 0);
            java.util.Arrays.fill(pauseBis, 0);
            java.util.Arrays.fill(fehl, 0);
            angefragt = -1;
        }
        int[][] gruppen = gruppenFuer(z.klasse);
        for (int g = 0; g < gruppen.length && g < tippAm.length; g++) {
            int slot = slot(z, l, t, gruppen[g]);
            if (slot < 0) continue;
            Fertigkeiten.Fertigkeit f = t.get(z.fkFertigkeit[slot]);
            if (z.wirkt(f.zustand)) {
                if (tippAm[g] > 0) bestaetigt++;
                tippAm[g] = 0;
                fehl[g] = 0;
                continue;
            }
            if (now < pauseBis[g]) continue;
            if (tippAm[g] > 0) {
                if (now - tippAm[g] < BESTAETIGUNG_MS) return null;
                tippAm[g] = 0;
                if (++fehl[g] >= VERSUCHE) {
                    fehl[g] = 0;
                    pauseBis[g] = now + PAUSE_MS;
                    pausen++;
                    befund = "Puffer " + f.id + " [F" + (slot + 1) + "]: Zustand " + f.zustand + " nach " + VERSUCHE
                        + " Tipps nicht erschienen, " + PAUSE_MS / 1000 + " s Pause";
                    grund = befund;
                    continue;
                }
            }
            double cost = Kampfwahl.kosten(z, slot, f);
            if (feindAbstand < FEIND_ABSTAND || z.mana < cost) continue;
            angefragt = g;
            grund = "Puffer " + f.id + " [F" + (slot + 1) + "] erneuern (Zustand " + f.zustand + ", Mana " + z.mana + ")";
            return "F" + (slot + 1);
        }
        return null;
    }

    public void ausgeloest(boolean gedrueckt, long am) {
        int g = angefragt;
        angefragt = -1;
        if (g < 0 || !gedrueckt) return;
        tippAm[g] = am;
        tipps++;
    }

    public double reserve(Zustand z, Zustand.Layout l, Fertigkeiten t, long now) {
        double r = 0;
        int[][] gruppen = gruppenFuer(z.klasse);
        for (int g = 0; g < gruppen.length; g++) {
            if (g < pauseBis.length && z.klasse == klasse && now < pauseBis[g]) continue;
            int slot = slot(z, l, t, gruppen[g]);
            if (slot >= 0) r += Kampfwahl.kosten(z, slot, t.get(z.fkFertigkeit[slot]));
        }
        return r;
    }
}
