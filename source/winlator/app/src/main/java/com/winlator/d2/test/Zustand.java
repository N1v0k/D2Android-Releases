package com.winlator.d2.test;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Zustand {
    public static final int MONSTER_TOWN_NPC = 16;
    public static final int ITEM_GOLD = 1, ITEM_POTION = 2, ITEM_RUNE = 3, ITEM_GEM = 4;

    public static final class Ding {
        public final long id;
        public final int x, y, art, extra, leben;
        public final String name;

        Ding(long id, int x, int y, int art, int extra, int leben, String name) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.art = art;
            this.extra = extra;
            this.leben = leben;
            this.name = name;
        }
    }

    public long ms;
    public int seq, gebiet, px, py, gw, gh, lebenPct, manaPct, leben, lebenMax, heilGuertel;
    public boolean stadt, pause, panel, npc, heilWirkt;
    public int klasse = -1, links = -1, rechts = -1, mana, manaMax, manaGuertel;
    public boolean manaWirkt, fkBekannt;
    public final int[] fkFertigkeit = new int[16];
    public final boolean[] fkLinks = new boolean[16];
    public int[] puffer;
    public int[] anlegen;
    public byte[] punkte6;
    public long punkte6Nr = -1;
    public final int[] fkStufe = new int[16];
    public String fenster = "0";
    public int hover = -1;
    public long hoverId;
    public static final class Beute {
        public long id;
        public int kategorie, qualitaet, flags, sockel, stufe, breite, hoehe;
        public int wertklasse = -1;
    }
    public final Map<Long, Beute> beute = new HashMap<>();
    public long zellen = -1;
    public int waffen = -1;

    public boolean wirkt(int state) {
        if (puffer == null) return false;
        for (int p : puffer) if (p == state) return true;
        return false;
    }
    public double wx = Double.NaN, wy = Double.NaN;
    public int modus = -1, stufe = -1;
    public long erfahrung = -1, gold = -1;

    public static final long GOLD_JE_STUFE = 10_000;

    public boolean goldVoll() {
        return gold >= 0 && stufe > 0 && gold >= GOLD_JE_STUFE * stufe;
    }
    public final List<Ding> monster = new ArrayList<>(), items = new ArrayList<>(), objekte = new ArrayList<>(), leichen = new ArrayList<>();
    public final Set<Long> ausgelassen = new HashSet<>();
    public String zeile;

    public static Zustand parse(JSONObject o, String line) {
        Zustand z = new Zustand();
        z.zeile = line;
        z.ms = o.optLong("ms");
        z.seq = o.optInt("seq");
        z.gebiet = o.optInt("gebiet");
        z.stadt = o.optBoolean("stadt");
        z.pause = o.optBoolean("pause");
        z.panel = o.optBoolean("panel");
        z.npc = o.optBoolean("npc");
        z.fenster = o.optString("fenster", "0");
        z.hover = o.optInt("hover", -1);
        z.waffen = o.optInt("waffen", -1);
        z.hoverId = o.optLong("hoverId", 0);
        z.px = o.optInt("px");
        z.py = o.optInt("py");
        z.gw = o.optInt("gw");
        z.gh = o.optInt("gh");
        z.lebenPct = o.optInt("lebenPct");
        z.manaPct = o.optInt("manaPct");
        z.leben = o.optInt("leben");
        z.lebenMax = o.optInt("lebenMax");
        z.heilGuertel = o.optInt("heilGuertel");
        z.heilWirkt = o.optBoolean("heilWirkt");
        z.klasse = o.optInt("klasse", -1);
        z.links = o.optInt("links", -1);
        z.rechts = o.optInt("rechts", -1);
        z.mana = o.optInt("mana");
        z.manaMax = o.optInt("manaMax");
        z.manaGuertel = o.optInt("manaGuertel");
        z.manaWirkt = o.optBoolean("manaWirkt");
        java.util.Arrays.fill(z.fkFertigkeit, -1);
        JSONArray fk = o.optJSONArray("fk");
        if (fk != null) {
            z.fkBekannt = true;
            for (int i = 0; i < 16 && i < fk.length(); i++) {
                JSONArray e = fk.optJSONArray(i);
                if (e == null) continue;
                z.fkFertigkeit[i] = e.optInt(0, -1);
                z.fkLinks[i] = e.optInt(1) != 0;
            }
        }
        JSONArray buffs = o.optJSONArray("puffer");
        if (buffs != null) {
            z.puffer = new int[buffs.length()];
            for (int i = 0; i < buffs.length(); i++) z.puffer[i] = buffs.optInt(i, -1);
        }
        JSONArray an = o.optJSONArray("anlegen");
        if (an != null && an.length() >= 7) {
            z.anlegen = new int[7];
            for (int i = 0; i < 7; i++) z.anlegen[i] = an.optInt(i, -1);
        }
        z.punkte6Nr = o.optLong("punkte6Nr", -1);
        String p6 = o.optString("punkte6", "");
        if (p6.length() >= 2 && p6.length() % 2 == 0) {
            z.punkte6 = new byte[p6.length() / 2];
            for (int i = 0; i < z.punkte6.length; i++) z.punkte6[i] = (byte) Integer.parseInt(p6.substring(2 * i, 2 * i + 2), 16);
        }
        JSONArray levels = o.optJSONArray("fkStufe");
        if (levels != null) for (int i = 0; i < 16 && i < levels.length(); i++) z.fkStufe[i] = Math.max(0, levels.optInt(i));
        if (o.has("wx")) {
            z.wx = o.optDouble("wx");
            z.wy = o.optDouble("wy");
            z.modus = o.optInt("modus", -1);
            z.stufe = o.optInt("stufe", -1);
            z.erfahrung = o.optLong("erfahrung", -1);
            z.gold = o.optLong("gold", -1);
        }
        JSONArray m = o.optJSONArray("monster");
        if (m != null) for (int i = 0; i < m.length(); i++) {
            JSONArray a = m.optJSONArray(i);
            z.monster.add(new Ding(a.optLong(0), a.optInt(1), a.optInt(2), 0, a.optInt(4), a.optInt(3), a.optString(5)));
        }
        things(o.optJSONArray("items"), z.items);
        JSONArray bt = o.optJSONArray("beute");
        for (int i = 0; bt != null && i < bt.length(); i++) {
            JSONArray a = bt.optJSONArray(i);
            if (a == null) continue;
            Beute b = new Beute();
            b.id = a.optLong(0);
            b.kategorie = a.optInt(1);
            b.qualitaet = a.optInt(2);
            b.flags = a.optInt(3);
            b.sockel = a.optInt(4);
            b.stufe = a.optInt(5);
            b.breite = a.optInt(6);
            b.hoehe = a.optInt(7);
            b.wertklasse = a.optInt(8, -1);
            z.beute.put(b.id, b);
        }
        z.zellen = o.optLong("zellen", -1);
        things(o.optJSONArray("objekte"), z.objekte);
        things(o.optJSONArray("leichen"), z.leichen);
        JSONArray left = o.optJSONArray("ausgelassen");
        if (left != null) for (int i = 0; i < left.length(); i++) z.ausgelassen.add(left.optLong(i));
        return z;
    }

    private static void things(JSONArray list, List<Ding> into) {
        if (list == null) return;
        for (int i = 0; i < list.length(); i++) {
            JSONArray a = list.optJSONArray(i);
            into.add(new Ding(a.optLong(0), a.optInt(1), a.optInt(2), a.optInt(3), a.optInt(4), 0, null));
        }
    }

    public boolean weltlage() {
        return !Double.isNaN(wx);
    }

    public boolean steht() {
        return modus == 1 || modus == 5;
    }

    public boolean geht() {
        return modus == 2 || modus == 3 || modus == 6;
    }

    public double bodenAbstand(Ding d) {
        return Math.hypot(d.x - px, 2.0 * (d.y - py));
    }

    public boolean tot() {
        return lebenMax > 0 && leben <= 0 && (modus < 0 || modus == 0 || modus == 17);
    }

    public double abstand(Ding d) {
        return Math.hypot(d.x - px, d.y - py);
    }

    public List<Ding> feinde() {
        List<Ding> out = new ArrayList<>();
        for (Ding d : monster) if ((d.extra & MONSTER_TOWN_NPC) == 0 && d.leben > 0) out.add(d);
        return out;
    }

    public double weltVerschoben(Zustand vorher) {
        if (vorher == null) return Double.NaN;
        Map<Long, Ding> alt = new HashMap<>();
        for (Ding d : vorher.items) alt.put(d.id, d);
        for (Ding d : vorher.objekte) alt.put(d.id, d);
        for (Ding d : vorher.leichen) alt.put(d.id, d);
        List<Double> shifts = new ArrayList<>();
        for (List<Ding> list : java.util.Arrays.asList(items, objekte, leichen)) {
            for (Ding d : list) {
                Ding a = alt.get(d.id);
                if (a != null) shifts.add(Math.hypot((d.x - px) - (a.x - vorher.px), (d.y - py) - (a.y - vorher.py)));
            }
        }
        if (shifts.isEmpty()) {
            Map<Long, Ding> m = new HashMap<>();
            for (Ding d : vorher.monster) m.put(d.id, d);
            for (Ding d : monster) {
                Ding a = m.get(d.id);
                if (a != null) shifts.add(Math.hypot((d.x - px) - (a.x - vorher.px), (d.y - py) - (a.y - vorher.py)));
            }
            if (shifts.size() < 2) return Double.NaN;
        }
        shifts.sort(Double::compare);
        return shifts.get(shifts.size() / 2);
    }

    public static final class Ziele {
        public static final int ENTRANCE = 0, PASSAGE = 1, WAYPOINT = 2;

        public static final class Ausgang {
            public final int art, nach, x, y, outX, outY, x0, y0, x1, y1;

            Ausgang(JSONArray a) {
                art = a.optInt(0);
                nach = a.optInt(1);
                x = a.optInt(2);
                y = a.optInt(3);
                outX = a.optInt(4);
                outY = a.optInt(5);
                x0 = a.optInt(6);
                y0 = a.optInt(7);
                x1 = a.optInt(8);
                y1 = a.optInt(9);
            }

            public String toString() {
                return (art == WAYPOINT ? "Wegpunkt" : (art == PASSAGE ? "Durchgang" : "Eingang")) + (art == WAYPOINT ? "" : " nach " + nach)
                    + " bei " + x + "," + y;
            }
        }

        public int gebiet, bereit;
        public int[] box = new int[4];
        public final List<Ausgang> ausgaenge = new ArrayList<>();

        public static Ziele parse(JSONObject o) {
            Ziele z = new Ziele();
            z.gebiet = o.optInt("gebiet");
            z.bereit = o.optInt("bereit");
            JSONArray b = o.optJSONArray("box");
            if (b != null) for (int i = 0; i < 4; i++) z.box[i] = b.optInt(i);
            JSONArray a = o.optJSONArray("ausgaenge");
            if (a != null) for (int i = 0; i < a.length(); i++) z.ausgaenge.add(new Ausgang(a.optJSONArray(i)));
            return z;
        }
    }

    public static final class Figur {
        public static final int ORT_PERSON = 1, ORT_TRUHE = 2, ORT_WEGPUNKT = 3;
        public static final int PLATZ_INVENTAR = 0, PLATZ_GUERTEL = 1, PLATZ_KOERPER = 2, PLATZ_WUERFEL = 3, PLATZ_TRUHE = 4, PLATZ_LADEN = 5;

        public static final class Ort {
            public int art, txt, x, y;
            public long id;

            public boolean geladen() {
                return id != 0;
            }
        }

        public static final class Teil {
            public long id;
            public String code;
            public int kategorie, qualitaet, flags, platz, koerperplatz, x, y, breite, hoehe;

            public boolean identifiziert() {
                return (flags & 4) != 0;
            }
        }

        public int gen, stufe, wertePunkte, fertigkeitPunkte;
        public int voll;
        public static final int VOLL_ORTE = 1, VOLL_GEGENSTAENDE = 2, VOLL_LADEN = 4;
        public long ms, modMs, gold, goldTruhe, zeiger;
        public String name = "";
        public int[] werte = new int[4];
        public final List<Ort> orte = new ArrayList<>();
        public int[] leiche;
        public final List<Teil> gegenstaende = new ArrayList<>(), laden = new ArrayList<>();

        public static final int KATEGORIE_WAFFE = 16, RECHTE_HAND = 4, LINKE_HAND = 5;

        public int waffenInHaenden() {
            int n = 0;
            for (Teil t : gegenstaende) {
                if (t.platz == PLATZ_KOERPER && t.kategorie == KATEGORIE_WAFFE && (t.koerperplatz == RECHTE_HAND || t.koerperplatz == LINKE_HAND)) n++;
            }
            return n;
        }

        public static final String TEST_PREFIX = "st-";

        public boolean istTestfigur(String figur) {
            return figur != null && figur.startsWith(TEST_PREFIX) && figur.equals(name);
        }

        public static Figur parse(JSONObject o) {
            Figur f = new Figur();
            f.ms = o.optLong("ms");
            f.gen = o.optInt("gen");
            f.modMs = o.optLong("modMs");
            f.name = o.optString("name");
            JSONArray w = o.optJSONArray("werte");
            if (w != null) for (int i = 0; i < 4; i++) f.werte[i] = w.optInt(i);
            JSONArray p = o.optJSONArray("punkte");
            if (p != null) {
                f.wertePunkte = p.optInt(0);
                f.fertigkeitPunkte = p.optInt(1);
            }
            f.stufe = o.optInt("stufe");
            f.gold = o.optLong("gold");
            f.goldTruhe = o.optLong("goldTruhe");
            f.zeiger = o.optLong("zeiger");
            f.voll = o.optInt("voll");
            JSONArray orte = o.optJSONArray("orte");
            for (int i = 0; orte != null && i < orte.length(); i++) {
                JSONArray a = orte.optJSONArray(i);
                Ort ort = new Ort();
                ort.art = a.optInt(0);
                ort.txt = a.optInt(1);
                ort.id = a.optLong(2);
                ort.x = a.optInt(3);
                ort.y = a.optInt(4);
                f.orte.add(ort);
            }
            JSONArray l = o.optJSONArray("leiche");
            if (l != null) f.leiche = new int[]{l.optInt(0), l.optInt(1)};
            teile(o.optJSONArray("gegenstaende"), f.gegenstaende);
            teile(o.optJSONArray("laden"), f.laden);
            return f;
        }

        private static void teile(JSONArray a, List<Teil> out) {
            for (int i = 0; a != null && i < a.length(); i++) {
                JSONArray e = a.optJSONArray(i);
                Teil t = new Teil();
                t.id = e.optLong(0);
                t.code = e.optString(1);
                t.kategorie = e.optInt(2);
                t.qualitaet = e.optInt(3);
                t.flags = e.optInt(4);
                t.platz = e.optInt(5);
                t.koerperplatz = e.optInt(6);
                t.x = e.optInt(7);
                t.y = e.optInt(8);
                t.breite = e.optInt(9);
                t.hoehe = e.optInt(10);
                out.add(t);
            }
        }

        public boolean frisch(long jetzt, long hoechstensMs) {
            return jetzt - ms <= hoechstensMs;
        }

        public boolean neu(Figur vorher) {
            return vorher == null || gen != vorher.gen || modMs < vorher.modMs;
        }
    }

    public static final class Spielstaende {
        public String ordner = "";
        public final List<String> namen = new ArrayList<>();
        public final List<Long> geaendert = new ArrayList<>();

        public static Spielstaende parse(JSONObject o) {
            Spielstaende s = new Spielstaende();
            s.ordner = o.optString("ordner");
            JSONArray a = o.optJSONArray("liste");
            for (int i = 0; a != null && i < a.length(); i++) {
                JSONArray e = a.optJSONArray(i);
                if (e == null) continue;
                s.namen.add(e.optString(0));
                s.geaendert.add(e.optLong(1));
            }
            return s;
        }

        public int platz(String name) {
            return namen.indexOf(name);
        }

        public long gespeichert(String name) {
            int i = platz(name);
            return i < 0 ? 0 : geaendert.get(i);
        }
    }

    public static final class Layout {
        public final Map<String, int[]> knoepfe = new HashMap<>();
        public final Set<String> unsichtbar = new HashSet<>();
        public int[] joystick = {370, 760, 180};
        public boolean knoepfeSichtbar = true, pauseGewollt, bearbeiten, joystickAktiv;
        public int w = 2480, h = 1116;
        public int[] spiel;
        public long ms;
        public boolean angriffRechts;
        public int angriffFertigkeit = -1;
        public String angriffName = "";
        public String traenke = "";
        public boolean zielhilfe = true;
        public double zielweite, zielkegel;
        public double zielweiteNah, zielweiteFern;
        public static final String TRAENKE_TASTEN = "tasten";

        public static Layout parse(JSONObject o) {
            Layout l = new Layout();
            l.w = o.optInt("w", l.w);
            l.h = o.optInt("h", l.h);
            l.ms = o.optLong("ms", 0);
            l.knoepfeSichtbar = o.optBoolean("knoepfeSichtbar", true);
            l.pauseGewollt = o.optBoolean("pauseGewollt");
            l.traenke = o.optString("traenke", "");
            l.zielhilfe = o.optBoolean("zielhilfe", true);
            l.zielweite = o.optDouble("zielweite", 0);
            l.zielkegel = o.optDouble("zielkegel", 0);
            l.zielweiteNah = o.optDouble("zielweiteNah", 0);
            l.zielweiteFern = o.optDouble("zielweiteFern", 0);
            l.bearbeiten = o.optBoolean("bearbeiten");
            l.joystickAktiv = o.optBoolean("joystickAktiv");
            JSONArray sp = o.optJSONArray("spiel");
            if (sp != null && sp.length() == 4) l.spiel = new int[]{sp.optInt(0), sp.optInt(1), sp.optInt(2), sp.optInt(3)};
            JSONArray j = o.optJSONArray("joystick");
            if (j != null) l.joystick = new int[]{j.optInt(0), j.optInt(1), j.optInt(2)};
            JSONObject angriff = o.optJSONObject("angriff");
            if (angriff != null) {
                l.angriffRechts = "rechts".equals(angriff.optString("seite", "links"));
                l.angriffFertigkeit = angriff.optInt("fertigkeit", -1);
                l.angriffName = angriff.optString("name", "");
            }
            JSONObject b = o.optJSONObject("knoepfe");
            if (b != null) {
                for (java.util.Iterator<String> it = b.keys(); it.hasNext(); ) {
                    String key = it.next();
                    JSONArray a = b.optJSONArray(key);
                    l.knoepfe.put(key, new int[]{a.optInt(0), a.optInt(1), a.optInt(2), a.optInt(3), a.optInt(4)});
                    if (a.length() > 5 && a.optInt(5, 1) == 0) l.unsichtbar.add(key);
                }
            }
            return l;
        }

        public boolean sichtbar(String id) {
            return !unsichtbar.contains(id);
        }

        public int[] mitte(String id, int fx, int fy) {
            int[] b = knoepfe.get(id);
            return b != null && b[0] > 0 && b[1] > 0 ? new int[]{b[0], b[1]} : new int[]{fx, fy};
        }
    }
}
