package com.winlator.d2.test;

import com.winlator.d2.D2Tabellen;







public final class Ausruestung {

    public static final double GEWINN = 1.2;

    public static final int MINDERWERTIG = 1, NORMAL = 2, UEBERLEGEN = 3, MAGISCH = 4;


    public static final class Figur {

        public int klasse;
        public int stufe;

        public int staerke, geschick;

        public int grundStaerke = -1, grundGeschick = -1;

        public boolean linkeHandLeer;
    }

    private Ausruestung() {}





    public static String nichtAnlegbar(D2Tabellen t, Figur f, String code, boolean aetherisch) {
        D2Tabellen.Waffe w = t != null ? t.waffe(code) : null;
        if (w == null) return "keine Waffe";
        if (w.quest) return "Quest-Gegenstand";
        if (aetherisch) return "ätherisch (Haltbarkeit unbekannt)";
        if (t.fernkampf(w.type)) return "Wurf- oder Fernwaffe";
        int k = t.klasse(w.type);
        if (k != D2Tabellen.CLASS_ANY && k != f.klasse) return "andere Klasse";
        if (f.grundStaerke < 0 || f.grundGeschick < 0) return "Grundwerte unbekannt";
        if (w.reqStr > f.grundStaerke) return "Stärke " + w.reqStr + " nötig";
        if (w.reqDex > f.grundGeschick) return "Geschick " + w.reqDex + " nötig";
        if (w.reqLevel > f.stufe) return "Stufe " + w.reqLevel + " nötig";
        if (zweihaendig(w, f) && !f.linkeHandLeer) return "zweihändig, linke Hand belegt";
        return null;
    }


    static boolean zweihaendig(D2Tabellen.Waffe w, Figur f) {
        return w.twoHanded && !(w.oneOrTwoHanded && f.klasse == 4);
    }






    public static double wert(D2Tabellen t, Figur f, String code, int qualitaet) {
        D2Tabellen.Waffe w = t != null ? t.waffe(code) : null;
        if (w == null) return 0;
        boolean beide = w.twoHanded && (!w.oneOrTwoHanded || f.klasse != 4 || f.linkeHandLeer);
        double schaden = beide && w.max2Dam > 0 ? (w.min2Dam + w.max2Dam) / 2.0 : (w.minDam + w.maxDam) / 2.0;
        double attribute = 1 + (w.strBonus * (double) f.staerke + w.dexBonus * (double) f.geschick) / 10000;
        double tempo = 100.0 / Math.max(50, 100 + w.speed);
        return schaden * attribute * tempo * qualitaetsfaktor(qualitaet);
    }


    static double qualitaetsfaktor(int qualitaet) {
        if (qualitaet == MINDERWERTIG) return 0.7;
        if (qualitaet == UEBERLEGEN) return 1.1;
        if (qualitaet >= MAGISCH) return 1.2;
        return 1.0;
    }


    public static final int RECHTE_HAND = 4, LINKE_HAND = 5;

    static final int KATEGORIE_WAFFE = 16;

    static final int FLAG_AETHERISCH = 1;






    public static Figur figur(Zustand.Figur figur, int klasse, int[] grundwerte) {
        Figur f = new Figur();
        f.klasse = klasse;
        f.stufe = figur.stufe;
        f.staerke = figur.werte[0];
        f.geschick = figur.werte[2];
        if (grundwerte != null && grundwerte.length >= 3) {
            f.grundStaerke = grundwerte[0];
            f.grundGeschick = grundwerte[2];
        }
        f.linkeHandLeer = amKoerper(figur, LINKE_HAND) == null;
        return f;
    }


    public static Zustand.Figur.Teil amKoerper(Zustand.Figur figur, int platz) {
        for (Zustand.Figur.Teil t : figur.gegenstaende) {
            if (t.platz == Zustand.Figur.PLATZ_KOERPER && t.koerperplatz == platz) return t;
        }
        return null;
    }






    public static Zustand.Figur.Teil bessereWaffe(D2Tabellen t, Zustand.Figur figur, Figur f) {
        return bessereWaffe(t, figur, f, java.util.Collections.emptySet());
    }


    public static Zustand.Figur.Teil bessereWaffe(D2Tabellen t, Zustand.Figur figur, Figur f, java.util.Set<Long> ausser) {
        if (t == null || figur.zeiger != 0 || (figur.voll & Zustand.Figur.VOLL_GEGENSTAENDE) != 0) return null;
        Zustand.Figur.Teil hand = amKoerper(figur, RECHTE_HAND);
        Zustand.Figur.Teil beste = null;
        double besterWert = 0;
        for (Zustand.Figur.Teil teil : figur.gegenstaende) {
            if (teil.platz != Zustand.Figur.PLATZ_INVENTAR || teil.kategorie != KATEGORIE_WAFFE || ausser.contains(teil.id)) continue;
            boolean aetherisch = (teil.flags & FLAG_AETHERISCH) != 0;
            if (!besser(t, f, teil.code, teil.qualitaet, aetherisch, hand != null ? hand.code : null, hand != null ? hand.qualitaet : 0)) continue;
            double w = wert(t, f, teil.code, teil.qualitaet);
            if (w > besterWert) {
                besterWert = w;
                beste = teil;
            }
        }
        return beste;
    }





    public static boolean besser(D2Tabellen t, Figur f, String neu, int neuQualitaet, boolean neuAetherisch, String alt,
                                 int altQualitaet) {
        if (nichtAnlegbar(t, f, neu, neuAetherisch) != null) return false;
        double n = wert(t, f, neu, neuQualitaet);
        double a = alt == null ? 0 : wert(t, f, alt, altQualitaet);
        return n > 0 && n >= a * GEWINN;
    }
}
