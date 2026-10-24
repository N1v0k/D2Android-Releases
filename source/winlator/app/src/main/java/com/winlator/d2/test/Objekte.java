package com.winlator.d2.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;











public final class Objekte implements Zwischenziel {

    static final double MAX_LUFT = 12;

    static final double UMWEG = 1.5, UMWEG_PLUS = 2;





    static final double RADIUS = 3.5;

    static final long FRIST_GRUND_MS = 2000, FRIST_JE_SUBTILE_MS = 300, FRIST_MAX_MS = 8000;

    static final int BRUNNEN_PLUS = 10;

    static final long HANDELN_MS = 5000;

    static final long TIPP_NEU_MS = 600;

    static final long SPERRE_MS = 60_000;


    static final long UNTERBRECHUNG_MS = 10_000;
    static final int UNTERBRECHUNGEN = 3;
    static final int FEHLVERSUCHE = 2;

    static final double FEIND_NAH = 15;

    static final int BUDGET_JE_SUBTILE = 40, BUDGET_MAX = 1500;

    static final double GLEICH = 0.01;

    private final Gehirn gehirn;

    public boolean hardcore, schluessel;
    private int gebiet = Integer.MIN_VALUE;
    private final Set<Long> bisGebietswechsel = new HashSet<>();
    private final Map<Long, Integer> fehlversuche = new HashMap<>(), unterbrechungen = new HashMap<>();

    private Zustand.Ziele zieleJetzt;
    private final Map<Long, Long> gesperrt = new HashMap<>();

    private int art;
    private long handelnSeit, tippAm;
    private Gehirn.Absicht tipp;
    private int lebenVorher, manaVorher;
    private boolean erfolg, feindAbbruch;

    public int gewaehlt, genutzt, fehlgeschlagen, unterbrochen;
    public String letzteWahl = "";

    public Objekte(Gehirn gehirn) {
        this.gehirn = gehirn;
    }

    private static final class Kandidat {
        final Zustand.Ding d;
        final double x, y, luft;

        Kandidat(Zustand.Ding d, double x, double y, double luft) {
            this.d = d;
            this.x = x;
            this.y = y;
            this.luft = luft;
        }
    }

    @Override
    public Ziel waehlen(Zustand z, Zustand.Ziele ziele, long now) {
        neuesGebiet(z);
        zieleJetzt = ziele;
        if (!z.weltlage()) return null;
        Objektwahl.Lage l = lage(z);
        List<double[]> feinde = feinde(z, ziele, now);
        List<Kandidat> alle = new ArrayList<>();
        for (Zustand.Ding d : z.objekte) {
            if (d.art != Objektwahl.ART_BEHAELTER && d.art != Objektwahl.ART_SCHREIN && d.art != Objektwahl.ART_BRUNNEN) continue;
            if (bisGebietswechsel.contains(d.id) || z.istAusgelassen(d)) continue;
            Long bis = gesperrt.get(d.id);
            if (bis != null && now < bis) continue;
            double[] w = Gehirn.weltlage(z, d);
            double luft = Math.hypot(w[0] - z.wx, w[1] - z.wy);
            if (luft > MAX_LUFT || Gehirn.jenseits(z, ziele, d)) continue;
            if (feindNaeher(z, feinde, luft)) continue;
            l.feindNah = feindNah(z, feinde, w[0], w[1], luft);
            if (Objektwahl.grund(d.art, d.extra, z.objektDetails.get(d.id), l) != null) continue;
            alle.add(new Kandidat(d, w[0], w[1], luft));
        }
        if (alle.isEmpty()) return null;
        alle.sort((a, b) -> Double.compare(a.luft, b.luft));
        Karte k = gehirn.karte();
        boolean mitKarte = k != null && k.level == z.gebiet && k.feld((int) z.wx, (int) z.wy) != Karte.UNBEKANNT;
        for (int i = 0; i < alle.size() && i < 4; i++) {
            Kandidat c = alle.get(i);
            double weg = c.luft;
            if (mitKarte) {
                int budget = (int) Math.min(BUDGET_MAX, BUDGET_JE_SUBTILE * Math.max(1, c.luft));
                Wegsuche.Weg w = Wegsuche.suche(k, (int) z.wx, (int) z.wy, (int) Math.round(c.x), (int) Math.round(c.y), budget);
                if (w == null || w.abgebrochen) {
                    gesperrt.put(c.d.id, now + SPERRE_MS);
                    continue;
                }
                weg = laenge(z, w);
                if (weg > Math.max(UMWEG * c.luft, c.luft + UMWEG_PLUS) || durchAusgang(ziele, w)) {
                    gesperrt.put(c.d.id, now + SPERRE_MS);
                    continue;
                }
            }
            return ziel(z, c, weg, now);
        }
        return null;
    }

    private Ziel ziel(Zustand z, Kandidat c, double weg, long now) {
        art = c.d.art;
        handelnSeit = tippAm = 0;
        tipp = null;
        erfolg = feindAbbruch = false;
        lebenVorher = z.lebenPct;
        manaVorher = z.manaPct;
        gewaehlt++;
        int[] det = z.objektDetails.get(c.d.id);
        String name = (art == Objektwahl.ART_SCHREIN ? "Schrein " + (det != null ? det[1] : 0)
            : art == Objektwahl.ART_BRUNNEN ? "Brunnen" : "Behälter " + c.d.extra) + " " + c.d.id;
        long frist = Math.min(FRIST_MAX_MS, FRIST_GRUND_MS + Math.round(FRIST_JE_SUBTILE_MS * weg));
        letzteWahl = String.format(Locale.ROOT, "%s: %.0f Subtiles, Weg %.0f", name, c.luft, weg);
        return new Ziel(name, c.d.id, c.x, c.y, RADIUS, frist);
    }

    @Override
    public Ziel nachfuehren(Ziel ziel, Zustand z, long now) {
        Zustand.Ding d = finde(z, ziel.id, art);
        if (d == null) {
            if (tippAm > 0 && art != Objektwahl.ART_BRUNNEN) erfolg = true;
            return null;
        }


        if (z.istAusgelassen(d)) return null;
        double[] w = Gehirn.weltlage(z, d);

        if (tippAm == 0 && feindNaeher(z, feinde(z, zieleJetzt, now), Math.hypot(w[0] - z.wx, w[1] - z.wy))) {
            feindAbbruch = true;
            return null;
        }
        return ziel.bei(w[0], w[1]);
    }

    @Override
    public boolean handeln(Ziel ziel, Zustand z, Gehirn.Absicht a, long now) {
        if (handelnSeit == 0) handelnSeit = now;
        if (tipp != null) {
            if (!tipp.getippt) tippAm = 0;
            tipp = null;
        }


        if (art == Objektwahl.ART_BRUNNEN && tippAm > 0
            && (z.lebenPct >= lebenVorher + BRUNNEN_PLUS || z.manaPct >= manaVorher + BRUNNEN_PLUS)) {
            erfolg = true;
            return false;
        }
        if (now - handelnSeit > HANDELN_MS) return false;
        Zustand.Ding d = finde(z, ziel.id, art);
        if (d == null) return false;
        if (tippAm > 0) {
            if (a.grund.isEmpty()) a.grund = ziel.name + ": warten (" + (now - tippAm) + " ms nach dem Tipp)";
            return true;
        }



        if (!ersterKandidat(z, d, false)) {

            a.richtung = Math.atan2(d.y - z.py, d.x - z.px);
            if (a.grund.isEmpty()) a.grund = ziel.name + ": näher heran";
            return true;
        }
        if (a.tippen != null) return true;


        double[] w = Gehirn.weltlage(z, d);
        double luft = Math.hypot(w[0] - z.wx, w[1] - z.wy);
        Objektwahl.Lage l = lage(z);
        l.feindNah = feindNah(z, feinde(z, zieleJetzt, now), w[0], w[1], luft);
        String nein = Objektwahl.grund(d.art, d.extra, z.objektDetails.get(d.id), l);
        if (nein != null) {
            feindAbbruch = true;
            if (a.grund.isEmpty()) a.grund = ziel.name + ": nicht mehr erlaubt (" + nein + ")";
            return false;
        }
        a.richtung = Double.NaN;
        a.tippen = "interact";
        a.grund = ziel.name + ": Nehmen";
        lebenVorher = z.lebenPct;
        manaVorher = z.manaPct;
        tippAm = now;
        tipp = a;
        return true;
    }

    @Override
    public void ende(Ziel ziel, int wie, long now) {
        if (erfolg) {
            genutzt++;
            bisGebietswechsel.add(ziel.id);
        }
        else if (feindAbbruch || (wie == AUFGEGEBEN && gehirn.imKampf())) {
            unterbrochen++;
            if (unterbrechungen.merge(ziel.id, 1, Integer::sum) >= UNTERBRECHUNGEN) bisGebietswechsel.add(ziel.id);
            else gesperrt.put(ziel.id, now + UNTERBRECHUNG_MS);
        }
        else {
            fehlgeschlagen++;
            int n = fehlversuche.merge(ziel.id, 1, Integer::sum);
            if (n >= FEHLVERSUCHE) bisGebietswechsel.add(ziel.id);
            else gesperrt.put(ziel.id, now + SPERRE_MS);
        }
        handelnSeit = tippAm = 0;
        tipp = null;
        erfolg = feindAbbruch = false;
    }


    public String text() {
        return " · Objekte " + gewaehlt + " (genutzt " + genutzt + ", fehlgeschlagen " + fehlgeschlagen + ", unterbrochen "
            + unterbrochen + ")";
    }


    public static boolean hatSchluessel(Zustand.Figur f) {
        if (f == null) return false;
        for (Zustand.Figur.Teil t : f.gegenstaende) {
            if (t.platz == Zustand.Figur.PLATZ_INVENTAR && t.code != null && t.code.trim().equals("key")) return true;
        }
        return false;
    }

    private void neuesGebiet(Zustand z) {
        if (z.gebiet == gebiet) return;
        gebiet = z.gebiet;
        bisGebietswechsel.clear();
        fehlversuche.clear();
        unterbrechungen.clear();
        gesperrt.clear();
    }

    private Objektwahl.Lage lage(Zustand z) {
        Objektwahl.Lage l = new Objektwahl.Lage();
        l.lebenPct = z.lebenMax > 0 ? z.lebenPct : 0;
        l.manaPct = z.manaMax > 0 ? z.manaPct : 100;
        l.hardcore = hardcore;
        l.schluessel = schluessel;
        l.assassine = z.klasse == 6;
        l.schreinZustaende = z.schreine;
        return l;
    }


    static int einheitentyp(int art) {
        return art == 6 ? 5 : art == 7 ? 0 : 2;
    }





    private static Zustand.Ding finde(Zustand z, long id, int art) {
        for (Zustand.Ding d : z.objekte) if (d.id == id && einheitentyp(d.art) == einheitentyp(art)) return d;
        return null;
    }





    private List<double[]> feinde(Zustand z, Zustand.Ziele ziele, long now) {
        Set<Long> ignoriert = gehirn.ignorierte(now);
        List<double[]> out = new ArrayList<>();
        for (Zustand.Ding m : z.feinde()) {
            if (ignoriert.contains(m.id) || Gehirn.jenseits(z, ziele, m)) continue;
            out.add(Gehirn.weltlage(z, m));
        }
        return out;
    }


    private static boolean feindNah(Zustand z, List<double[]> feinde, double ox, double oy, double luft) {
        for (double[] w : feinde) {
            if (Math.hypot(w[0] - z.wx, w[1] - z.wy) <= FEIND_NAH || Math.hypot(w[0] - ox, w[1] - oy) < luft) return true;
        }
        return false;
    }


    private static boolean feindNaeher(Zustand z, List<double[]> feinde, double luft) {
        for (double[] w : feinde) if (Math.hypot(w[0] - z.wx, w[1] - z.wy) < luft) return true;
        return false;
    }








    static boolean ersterKandidat(Zustand z, Zustand.Ding objekt, boolean ohneDurchgaenge) {
        double reach = (z.gh > 0 ? z.gh : 600) * 0.55, score = z.bodenAbstand(objekt);
        if (score > reach) return false;
        for (Zustand.Ding d : z.items) {
            double t = z.bodenAbstand(d);
            if (t <= reach && t * 0.8 <= score + GLEICH) return false;
        }
        for (Zustand.Ding d : z.objekte) {
            if (d == objekt || (d.id == objekt.id && einheitentyp(d.art) == einheitentyp(objekt.art))) continue;
            if (ohneDurchgaenge && (d.art == 5 || d.art == 6)) continue;
            if (z.istAusgelassen(d) || Objektwahl.nehmenNie(d.art, d.extra, z.objektDetails.get(d.id))) continue;
            double t = z.bodenAbstand(d);
            if (t <= reach && (d.art == 7 || t <= score + GLEICH)) return false;
        }
        for (Zustand.Ding d : z.monster) {

            if (!z.stadt && (d.extra & Zustand.MONSTER_TOWN_NPC) == 0) continue;
            double t = z.bodenAbstand(d);
            if (t <= reach && t <= score + GLEICH) return false;
        }
        return true;
    }


    public static final class Vorn {
        public Zustand.Ding ding;
        public boolean item, gleich;
    }





    public static Vorn vorn(Zustand z, boolean ohneDurchgaenge) {
        double reach = (z.gh > 0 ? z.gh : 600) * 0.55, best = Double.MAX_VALUE;
        Vorn v = null;
        for (Zustand.Ding d : z.items) {
            double t = z.bodenAbstand(d);
            if (t <= reach) v = besser(v, d, true, t * 0.8, best);
            if (v != null && v.ding == d) best = t * 0.8;
        }
        for (Zustand.Ding d : z.objekte) {
            if (ohneDurchgaenge && (d.art == 5 || d.art == 6)) continue;
            if (z.istAusgelassen(d) || Objektwahl.nehmenNie(d.art, d.extra, z.objektDetails.get(d.id))) continue;
            double t = d.art == 7 ? -1 : z.bodenAbstand(d);
            if (z.bodenAbstand(d) > reach) continue;
            v = besser(v, d, false, t, best);
            if (v != null && v.ding == d) best = t;
        }
        for (Zustand.Ding d : z.monster) {

            if (!z.stadt && (d.extra & Zustand.MONSTER_TOWN_NPC) == 0) continue;
            double t = z.bodenAbstand(d);
            if (t > reach) continue;
            v = besser(v, d, false, t, best);
            if (v != null && v.ding == d) best = t;
        }
        return v;
    }

    private static Vorn besser(Vorn v, Zustand.Ding d, boolean item, double score, double best) {
        if (v != null && score > best + GLEICH) return v;
        if (v != null && Math.abs(score - best) <= GLEICH) {
            v.gleich = true;
            return v;
        }
        Vorn n = new Vorn();
        n.ding = d;
        n.item = item;
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

    private static boolean durchAusgang(Zustand.Ziele ziele, Wegsuche.Weg w) {
        if (ziele == null) return false;
        for (Wegsuche.Punkt p : w.knoten) if (Gehirn.nahAusgang(ziele, p.x, p.y)) return true;
        return false;
    }
}
