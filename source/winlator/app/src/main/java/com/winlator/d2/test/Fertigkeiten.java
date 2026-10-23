package com.winlator.d2.test;

import org.json.JSONArray;
import org.json.JSONObject;














public final class Fertigkeiten {
    public static final int RECORD_SIZE = 572;

    public static final int RANGE_NONE = 0, RANGE_H2H = 1, RANGE_RNG = 2, RANGE_BOTH = 3;

    public static final int CLASS_NONE = -1;

    static final int FIRE_BOLT = 36, FIRE_BOLT_MISSILE = 58;


    public static final class Fertigkeit {
        public final int id, klasse, reichweite, geschoss, zustand, mindeststufe, minMana, manaShift, mana, lvlMana;

        public final boolean angriffOhneMana;

        public final int[] voraussetzungen;




        public final int zweiteHand;

        Fertigkeit(int id, int klasse, int reichweite, int geschoss, int zustand, int mindeststufe, int minMana,
                   int manaShift, int mana, int lvlMana, boolean angriffOhneMana, int[] voraussetzungen, int zweiteHand) {
            this.id = id;
            this.klasse = klasse;
            this.reichweite = reichweite;
            this.geschoss = geschoss;
            this.zustand = zustand;
            this.mindeststufe = mindeststufe;
            this.minMana = minMana;
            this.manaShift = manaShift;
            this.mana = mana;
            this.lvlMana = lvlMana;
            this.angriffOhneMana = angriffOhneMana;
            this.voraussetzungen = voraussetzungen;
            this.zweiteHand = zweiteHand;
        }


        public boolean zweiWaffen() {
            return zweiteHand != 0;
        }





        public int kosten256(int stufe) {
            int level = Math.max(1, stufe);
            int cost = (mana + lvlMana * (level - 1)) << manaShift;
            return Math.max(cost, minMana << 8);
        }


        public double kosten(int stufe) {
            return kosten256(stufe) / 256.0;
        }


        public boolean kostenOhneStufe() {
            return lvlMana <= 0;
        }


        public boolean hatZustand() {
            return zustand >= 0;
        }

        public boolean nahkampf() {
            return reichweite == RANGE_H2H;
        }

        public boolean hatGeschoss() {
            return geschoss >= 0;
        }

        public String toString() {
            return "Fertigkeit " + id + " (Klasse " + klasse + ", Mana " + kosten(1) + (kostenOhneStufe() ? "" : " +je Stufe")
                + (hatZustand() ? ", Zustand " + zustand : "") + (hatGeschoss() ? ", Geschoss " + geschoss : "")
                + (nahkampf() ? ", Nahkampf" : "") + (angriffOhneMana ? ", AttackNoMana" : "") + (zweiWaffen() ? ", zwei Waffen" : "") + ")";
        }
    }

    private final Fertigkeit[] liste;

    private Fertigkeiten(Fertigkeit[] liste) {
        this.liste = liste;
    }





    public static Fertigkeiten lesen(byte[] bin) {
        if (bin == null || bin.length < 4) return null;
        int count = s32(bin, 0);
        if (count <= FIRE_BOLT || (long) count * RECORD_SIZE + 4 != bin.length) return null;
        Fertigkeit[] liste = new Fertigkeit[count];
        for (int id = 0; id < count; id++) {
            int at = 4 + id * RECORD_SIZE;
            liste[id] = new Fertigkeit(id,
                bin[at + 0x0C],
                bin[at + 0x14] & 0xFF,
                u16orNone(bin, at + 0x46),
                u16orNone(bin, at + 0x80),
                u16(bin, at + 0x174),
                u16(bin, at + 0x186),
                bin[at + 0x188] & 0xFF,
                (short) u16(bin, at + 0x18A),
                (short) u16(bin, at + 0x18C),
                (bin[at + 0x07] & 0x10) != 0,
                voraussetzungen((short) u16(bin, at + 0x17E), (short) u16(bin, at + 0x180), (short) u16(bin, at + 0x182)),
                zweiteHand(bin, at));
        }
        Fertigkeit check = liste[FIRE_BOLT];
        if (check.geschoss != FIRE_BOLT_MISSILE || check.kosten256(1) != 640 || check.klasse != 1) return null;
        return new Fertigkeiten(liste);
    }






    public JSONObject alsJson(int klasse, long ms) {
        try {
            JSONArray a = new JSONArray();
            for (Fertigkeit f : liste) {
                if (f == null || (f.klasse != klasse && f.klasse != CLASS_NONE)) continue;
                a.put(new JSONArray().put(f.id).put(f.klasse).put(f.reichweite).put(f.geschoss).put(f.zustand)
                    .put(f.mindeststufe).put(f.minMana).put(f.manaShift).put(f.mana).put(f.lvlMana).put(f.angriffOhneMana ? 1 : 0)
                    .put(f.voraussetzungen.length > 0 ? f.voraussetzungen[0] : -1)
                    .put(f.voraussetzungen.length > 1 ? f.voraussetzungen[1] : -1)
                    .put(f.voraussetzungen.length > 2 ? f.voraussetzungen[2] : -1)
                    .put(f.zweiteHand));
            }
            return new JSONObject().put("t", "fertigkeiten").put("ms", ms).put("klasse", klasse).put("liste", a);
        }
        catch (org.json.JSONException e) {
            return null;
        }
    }


    public static Fertigkeiten ausJson(JSONObject o) {
        JSONArray a = o != null ? o.optJSONArray("liste") : null;
        if (a == null || a.length() == 0) return null;
        int max = -1;
        for (int i = 0; i < a.length(); i++) {
            JSONArray e = a.optJSONArray(i);
            if (e != null) max = Math.max(max, e.optInt(0, -1));
        }
        if (max < 0 || max > 4096) return null;
        Fertigkeit[] liste = new Fertigkeit[max + 1];
        for (int i = 0; i < a.length(); i++) {
            JSONArray e = a.optJSONArray(i);
            if (e == null || e.length() < 11) continue;
            int id = e.optInt(0, -1);
            if (id < 0) continue;
            liste[id] = new Fertigkeit(id, e.optInt(1), e.optInt(2), e.optInt(3, -1), e.optInt(4, -1), e.optInt(5),
                e.optInt(6), e.optInt(7), e.optInt(8), e.optInt(9), e.optInt(10) != 0,
                voraussetzungen(e.optInt(11, -1), e.optInt(12, -1), e.optInt(13, -1)), e.optInt(14, 0));
        }
        return new Fertigkeiten(liste);
    }


    public Fertigkeit get(int id) {
        return id >= 0 && id < liste.length ? liste[id] : null;
    }

    public int anzahl() {
        return liste.length;
    }


    private static int[] voraussetzungen(int a, int b, int c) {
        int n = (a > 0 ? 1 : 0) + (b > 0 ? 1 : 0) + (c > 0 ? 1 : 0), i = 0;
        int[] out = new int[n];
        for (int v : new int[]{a, b, c}) if (v > 0) out[i++] = v;
        return out;
    }


    private static int zweiteHand(byte[] b, int at) {
        for (int k = 0; k < 3; k++) {
            int v = (short) u16(b, at + 0x1E + 2 * k);
            if (v > 0) return v;
        }
        return 0;
    }

    private static int u16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }


    private static int u16orNone(byte[] b, int at) {
        int v = u16(b, at);
        return v == 0xFFFF ? -1 : v;
    }

    private static int s32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
