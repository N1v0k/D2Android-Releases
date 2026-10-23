package com.winlator.d2.test;

import com.winlator.d2.D2Tabellen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;












public final class Anlegen {

    public static final long PRUEF_MS = 3000;

    public static final long WARTEN_MS = 5000;

    public static final long NACHLAUF_MS = 5000;

    public static final long ZEIGER_MS = 3000;

    public static final long SPERRE_MS = 60_000;

    public static final int OK = 1, ABGELEHNT = 2, AM_ZEIGER = 3, NICHT_BEGONNEN = 4;

    public enum Art { NICHTS, SENDEN, HALTEN, ENDE }





    public static final class Schritt {
        public Art art = Art.NICHTS;
        public long id;
        public String grund = "";

        public boolean keineAntwort;
    }

    private long letztePruefung = Long.MIN_VALUE / 2, gesendetAm, nachlaufBis = Long.MIN_VALUE / 2, zeigerSeit = -1;
    private boolean wartet;
    private long wartetAuf;
    private int zaehlerVorher;
    private final Set<Long> abgelehnt = new HashSet<>();
    private final Map<Long, Long> gesperrtBis = new HashMap<>();

    public boolean wartet() {
        return wartet;
    }


    public boolean beschaeftigt(long now) {
        return wartet || now < nachlaufBis || zeigerSeit >= 0;
    }


    public boolean brauchtFigur(long now) {
        return !wartet && (now - letztePruefung >= PRUEF_MS || now < nachlaufBis || zeigerSeit >= 0);
    }











    public Schritt schritt(D2Tabellen t, Zustand.Figur figur, Ausruestung.Figur f, boolean kampf, boolean faehig,
                           int zaehler, int ergebnis, long now) {
        Schritt s = new Schritt();
        if (wartet) {
            if (zaehler >= 0 && zaehler != zaehlerVorher) {
                wartet = false;
                s.id = wartetAuf;
                if (ergebnis == AM_ZEIGER) {
                    s.art = Art.ENDE;
                    s.grund = "ID " + wartetAuf;
                    return s;
                }
                if (ergebnis == ABGELEHNT) {
                    abgelehnt.add(wartetAuf);
                    nachlaufBis = now + NACHLAUF_MS;
                    s.art = Art.HALTEN;
                }
                else if (ergebnis != OK) gesperrtBis.put(wartetAuf, now + SPERRE_MS);
                s.grund = ergebnis == OK ? "angelegt" : ergebnis == ABGELEHNT ? "abgelehnt" : "nicht begonnen";
                return s;
            }
            if (now - gesendetAm > WARTEN_MS) {
                wartet = false;
                s.art = Art.ENDE;
                s.id = wartetAuf;
                s.grund = "ID " + wartetAuf;
                s.keineAntwort = true;
                return s;
            }
            s.art = Art.HALTEN;
            return s;
        }
        if (!faehig) return s;

        if (figur != null) zeigerSeit = figur.zeiger != 0 ? (zeigerSeit >= 0 ? zeigerSeit : now) : -1;
        if (zeigerSeit >= 0) {
            if (now - zeigerSeit <= ZEIGER_MS) {
                s.art = Art.HALTEN;
                return s;
            }
            zeigerSeit = -1;
            s.art = Art.ENDE;
            s.id = figur != null ? figur.zeiger : 0;
            s.grund = "ID " + s.id;
            return s;
        }
        if (now < nachlaufBis) {
            s.art = Art.HALTEN;
            return s;
        }
        if (kampf || figur == null || t == null || zaehler < 0 || now - letztePruefung < PRUEF_MS) return s;
        letztePruefung = now;
        Set<Long> ausser = new HashSet<>(abgelehnt);
        for (Map.Entry<Long, Long> e : gesperrtBis.entrySet()) if (now < e.getValue()) ausser.add(e.getKey());
        Zustand.Figur.Teil beste = Ausruestung.bessereWaffe(t, figur, f, ausser);
        if (beste == null) return s;
        wartet = true;
        wartetAuf = beste.id;
        zaehlerVorher = zaehler;
        gesendetAm = now;
        s.art = Art.SENDEN;
        s.id = beste.id;
        s.grund = beste.code;
        return s;
    }
}
