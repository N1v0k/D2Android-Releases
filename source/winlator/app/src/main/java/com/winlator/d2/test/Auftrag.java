package com.winlator.d2.test;

import java.util.Locale;









public final class Auftrag {
    public enum Art { LAGER_VERLASSEN, WEGPUNKT_GEHEN, WEGPUNKT_REISE, AUSGANG, EINGANG, GEGNER, SCHREIN, TRUHE, NPC, PUNKT }

    public enum Ergebnis {
        ANGEKOMMEN("angekommen", 0), NICHT_GEFUNDEN("nicht gefunden", 1), BLOCKIERT("blockiert", 1), ZEIT_UM("zeit um", 1),
        ABGEBROCHEN("abgebrochen", 2);

        public final String text;
        public final int exitCode;

        Ergebnis(String text, int exitCode) {
            this.text = text;
            this.exitCode = exitCode;
        }
    }


    public static final String HILFE = "Aufträge: \"lager verlassen\", \"gehe wegpunkt\", \"wegpunkt <Gebiet-Nr. oder englischer Name>\" (vom Wegpunkt des aktuellen Gebiets aus), "
        + "\"gehe ausgang <Gebiet-Nr.>\", \"gehe eingang [<Gebiet-Nr.>]\", \"gehe gegner\", \"gehe schrein\", \"gehe truhe\", "
        + "\"gehe npc <Name>\", \"gehe punkt <x> <y>\" (Subtiles)";


    static final int OBJ_TRUHE = 1, OBJ_SCHREIN = 2, OBJ_WEGPUNKT = 3;

    static final int ORT_PERSON = 1, ORT_WEGPUNKT = 3;

    static final double NEHMEN_REICHWEITE = Wegpunkte.REICHWEITE;

    static final double PUNKT_NAH = 3;

    static final long BLOCKIERT_MS = 40_000;

    static final long BLOCKIERT_KAMPF_MS = 120_000;
    static final double FORTSCHRITT = 2;

    static final long SUCHEN_MS = 10_000;

    static final double PERSON_BESUCHT = 8;

    public final Art art;

    public final int gebiet;

    public final String name;
    public final double px, py;
    public final String text;

    public Ergebnis ergebnis;
    public String grund = "";

    public String unterwegs = "";

    private long startAm = -1, besterAm, suchenSeit = -1;
    private double bester = Double.MAX_VALUE;
    private int startGebiet = -1, besterGebiet = -1;
    private boolean startStadt;
    private int wegpunktAbbrueche = -1;

    private Auftrag(Art art, int gebiet, String name, double px, double py, String text) {
        this.art = art;
        this.gebiet = gebiet;
        this.name = name;
        this.px = px;
        this.py = py;
        this.text = text;
    }


    public static Auftrag lesen(String text) {
        String t = text == null ? "" : text.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        String[] w = t.isEmpty() ? new String[0] : t.split(" ");
        if (t.equals("lager verlassen") || t.equals("leave town")) return new Auftrag(Art.LAGER_VERLASSEN, 0, "", 0, 0, t);
        if (w.length >= 2 && w[0].equals("wegpunkt")) {
            String ziel = t.substring("wegpunkt ".length());
            int g = Wegpunkte.gebietZu(ziel);
            if (g < 0) throw new IllegalArgumentException("kein Wegpunkt \"" + ziel + "\" (Nummer oder eindeutiger Anfang des englischen Namens):\n" + Wegpunkte.liste());
            return new Auftrag(Art.WEGPUNKT_REISE, g, "", 0, 0, t);
        }
        if (w.length >= 2 && w[0].equals("gehe")) {
            switch (w[1]) {
                case "wegpunkt":
                    if (w.length == 2) return new Auftrag(Art.WEGPUNKT_GEHEN, 0, "", 0, 0, t);
                    break;
                case "ausgang":
                    if (w.length == 3) return new Auftrag(Art.AUSGANG, zahl(w[2], "Gebiet"), "", 0, 0, t);
                    break;
                case "eingang":
                    if (w.length <= 3) return new Auftrag(Art.EINGANG, w.length == 3 ? zahl(w[2], "Gebiet") : 0, "", 0, 0, t);
                    break;
                case "gegner":
                    if (w.length == 2) return new Auftrag(Art.GEGNER, 0, "", 0, 0, t);
                    break;
                case "schrein":
                    if (w.length == 2) return new Auftrag(Art.SCHREIN, 0, "", 0, 0, t);
                    break;
                case "truhe":
                    if (w.length == 2) return new Auftrag(Art.TRUHE, 0, "", 0, 0, t);
                    break;
                case "npc":
                    if (w.length >= 3) return new Auftrag(Art.NPC, 0, t.substring("gehe npc ".length()), 0, 0, t);
                    break;
                case "punkt":
                    if (w.length == 4) return new Auftrag(Art.PUNKT, 0, "", zahl(w[2], "x"), zahl(w[3], "y"), t);
                    break;
                default:
            }
        }
        throw new IllegalArgumentException("unbekannter Auftrag \"" + t + "\". " + HILFE);
    }

    private static int zahl(String s, String was) {
        try {
            int n = Integer.parseInt(s);
            if (n >= 0) return n;
        }
        catch (NumberFormatException ignored) {}
        throw new IllegalArgumentException(was + " muss eine Zahl sein: " + s);
    }


    boolean erkundet() {
        return art == Art.GEGNER || art == Art.SCHREIN || art == Art.TRUHE;
    }

    public boolean fertig() {
        return ergebnis != null;
    }

    void ende(Ergebnis e, String warum) {
        if (ergebnis != null) return;
        ergebnis = e;
        grund = warum;
    }


    public String status() {
        return ergebnis != null ? "ergebnis " + ergebnis.text + (grund.isEmpty() ? "" : ": " + grund) : "unterwegs: " + unterwegs;
    }






    public Gehirn.Absicht schritt(Zustand z, Zustand.Ziele ziele, Zustand.Figur figur, Gehirn g, Wegpunkte wp, long now) {
        if (ergebnis != null) return null;
        if (!z.weltlage()) {
            unterwegs = "warte auf die Weltlage (Spieltest-Daten)";
            return warten();
        }
        if (startAm < 0) {
            startAm = besterAm = now;
            startGebiet = z.gebiet;
            startStadt = z.stadt;
        }
        if (z.tot()) {
            ende(Ergebnis.ABGEBROCHEN, "Figur tot");
            return warten();
        }
        boolean zieleDa = ziele != null && ziele.gebiet == z.gebiet && ziele.bereit == 2;
        switch (art) {
            case LAGER_VERLASSEN:
                if (!z.stadt) {
                    if (startStadt && drin(now)) return null;
                    ende(Ergebnis.ANGEKOMMEN, startStadt ? "draußen in Gebiet " + z.gebiet : "schon draußen (Gebiet " + z.gebiet + ")");
                    return warten();
                }
                angekommenSeit = -1;

                g.vorgeben(Double.NaN, 0, "", false);
                g.zielGebiet = Math.max(g.zielGebiet, 2);
                unterwegs = "zum Ausgang der Stadt";
                return blockiertPruefen(z, Double.NaN, "Stadtausgang", now) ? warten() : null;
            case AUSGANG:
            case EINGANG:
                return ausgang(z, ziele, zieleDa, g, now);
            case WEGPUNKT_GEHEN:
            case WEGPUNKT_REISE:
                return wegpunkt(z, ziele, figur, zieleDa, g, wp, now);
            case PUNKT: {
                double d = Math.hypot(px - z.wx, py - z.wy);
                if (d < PUNKT_NAH) {
                    ende(Ergebnis.ANGEKOMMEN, "Punkt " + Math.round(px) + "," + Math.round(py));
                    return warten();
                }
                g.vorgeben(px, py, "Punkt " + Math.round(px) + "," + Math.round(py), false);
                unterwegs = "zum Punkt " + Math.round(px) + "," + Math.round(py) + ", noch " + Math.round(d) + " Subtiles";
                return blockiertPruefen(z, d, "Punkt", now) ? warten() : null;
            }
            case NPC:
                return npc(z, figur, g, now);
            default:
                return suchen(z, ziele, zieleDa, g, now);
        }
    }






    private boolean drin(long now) {
        if (angekommenSeit < 0) angekommenSeit = now;
        return now - angekommenSeit < DRIN_MS;
    }

    private long angekommenSeit = -1;

    private boolean ueberKante;
    static final long DRIN_MS = 1500;


    private static Gehirn.Absicht warten() {
        return new Gehirn.Absicht();
    }

    private Gehirn.Absicht ausgang(Zustand z, Zustand.Ziele ziele, boolean zieleDa, Gehirn g, long now) {
        if (art == Art.AUSGANG ? z.gebiet == gebiet : z.gebiet != startGebiet && (gebiet == 0 || z.gebiet == gebiet)) {
            if (z.gebiet != startGebiet && ueberKante && drin(now)) return null;
            ende(Ergebnis.ANGEKOMMEN, "in Gebiet " + z.gebiet);
            return warten();
        }
        angekommenSeit = -1;
        Zustand.Ziele.Ausgang best = null;
        if (ziele != null && ziele.gebiet == z.gebiet) {
            for (Zustand.Ziele.Ausgang e : ziele.ausgaenge) {
                boolean passt = art == Art.AUSGANG ? (e.art == Zustand.Ziele.PASSAGE || e.art == Zustand.Ziele.ENTRANCE) && e.nach == gebiet
                    : e.art == Zustand.Ziele.ENTRANCE && (gebiet == 0 || e.nach == gebiet);
                if (passt && (best == null || Math.hypot(e.x - z.wx, e.y - z.wy) < Math.hypot(best.x - z.wx, best.y - z.wy))) best = e;
            }
        }
        if (best == null) {
            g.vorgeben(Double.NaN, 0, "", false);
            return nichtDa(zieleDa, "kein " + (art == Art.AUSGANG ? "Ausgang nach Gebiet " + gebiet : gebiet == 0 ? "Eingang"
                : "Eingang nach Gebiet " + gebiet) + " in Gebiet " + z.gebiet + " (" + ausgaenge(ziele) + ")", now);
        }
        suchenSeit = -1;
        boolean eingang = best.art == Zustand.Ziele.ENTRANCE;
        ueberKante = !eingang;

        double x = eingang ? best.x : best.x + best.outX * 6, y = eingang ? best.y : best.y + best.outY * 6;
        String name = (eingang ? "Eingang" : "Ausgang") + " nach " + best.nach;
        g.vorgeben(x, y, name, eingang);
        double d = Math.hypot(x - z.wx, y - z.wy);
        unterwegs = (eingang ? "zum Eingang" : "zum Ausgang") + " nach Gebiet " + best.nach + ", noch " + Math.round(d) + " Subtiles";
        return blockiertPruefen(z, d, name, now) ? warten() : null;
    }

    private Gehirn.Absicht wegpunkt(Zustand z, Zustand.Ziele ziele, Zustand.Figur figur, boolean zieleDa, Gehirn g, Wegpunkte wp,
                                    long now) {
        if (art == Art.WEGPUNKT_REISE) {
            if (z.gebiet == gebiet) {
                ende(Ergebnis.ANGEKOMMEN, z.gebiet == startGebiet ? "schon in Gebiet " + gebiet : "per Wegpunkt in Gebiet " + gebiet);
                return warten();
            }
            if (wegpunktAbbrueche < 0) wegpunktAbbrueche = wp.abbrueche;
            if (wp.abbrueche > wegpunktAbbrueche) {

                ende(Ergebnis.NICHT_GEFUNDEN, "Wegpunkt-Reise nach " + gebiet + " gescheitert: " + wp.abbruch);
                return warten();
            }
            if (wp.aktiv()) {
                unterwegs = "am Wegpunkt: " + wp.meldung;
                return wp.denken(z, now);
            }
        }
        if (Wegpunkte.inReichweite(z) != null) {
            if (art == Art.WEGPUNKT_GEHEN) {
                ende(Ergebnis.ANGEKOMMEN, "Wegpunkt in Reichweite von „Nehmen“ (Gebiet " + z.gebiet + ")");
                return warten();
            }
            g.vorgeben(Double.NaN, 0, "", false);
            wp.starten(z, gebiet, now);
            unterwegs = "am Wegpunkt: " + wp.meldung;
            return wp.denken(z, now);
        }
        double[] ziel = null;

        Zustand.Ding nah = null;
        for (Zustand.Ding d : z.objekte) if (d.art == OBJ_WEGPUNKT && (nah == null || z.bodenAbstand(d) < z.bodenAbstand(nah))) nah = d;
        if (nah != null) ziel = Gehirn.weltlage(z, nah);
        if (ziel == null && ziele != null && ziele.gebiet == z.gebiet) {
            for (Zustand.Ziele.Ausgang e : ziele.ausgaenge) {
                if (e.art == Zustand.Ziele.WAYPOINT && (ziel == null || Math.hypot(e.x - z.wx, e.y - z.wy) < Math.hypot(ziel[0] - z.wx, ziel[1] - z.wy))) {
                    ziel = new double[]{e.x, e.y};
                }
            }
        }
        if (ziel == null && z.stadt && figur != null) {
            for (Zustand.Figur.Ort o : figur.orte) if (o.art == ORT_WEGPUNKT) ziel = new double[]{o.x, o.y};
        }
        if (ziel == null) {
            g.vorgeben(Double.NaN, 0, "", false);
            return nichtDa(zieleDa, "kein Wegpunkt in Gebiet " + z.gebiet, now);
        }
        suchenSeit = -1;
        g.vorgeben(ziel[0], ziel[1], "Wegpunkt", false);
        double d = Math.hypot(ziel[0] - z.wx, ziel[1] - z.wy);
        unterwegs = "zum Wegpunkt, noch " + Math.round(d) + " Subtiles";
        return blockiertPruefen(z, d, "Wegpunkt", now) ? warten() : null;
    }

    private Gehirn.Absicht npc(Zustand z, Zustand.Figur figur, Gehirn g, long now) {
        Zustand.Ding best = null;
        java.util.TreeSet<String> namen = new java.util.TreeSet<>();
        for (Zustand.Ding d : z.monster) {
            if ((d.extra & Zustand.MONSTER_TOWN_NPC) == 0 || d.name == null) continue;
            namen.add(d.name);
            if (!einfach(d.name).contains(einfach(name))) continue;
            if (best == null || z.bodenAbstand(d) < z.bodenAbstand(best)) best = d;
        }
        if (!namen.isEmpty()) gesehen.addAll(namen);
        double reach = (z.gh > 0 ? z.gh : 600) * NEHMEN_REICHWEITE;
        if (best != null) {
            if (z.bodenAbstand(best) <= reach) {
                ende(Ergebnis.ANGEKOMMEN, best.name + " in Reichweite von „Nehmen“");
                return warten();
            }
            suchenSeit = -1;
            double[] w = Gehirn.weltlage(z, best);
            g.vorgeben(w[0], w[1], best.name, false);
            double d = Math.hypot(w[0] - z.wx, w[1] - z.wy);
            unterwegs = "zu " + best.name + ", noch " + Math.round(d) + " Subtiles";
            return blockiertPruefen(z, d, best.name, now) ? warten() : null;
        }




        double[] ziel = null;
        String zielText = null;
        if (figur != null) {
            for (Zustand.Figur.Ort o : figur.orte) {
                if (o.art != ORT_PERSON || !o.geladen()) continue;
                for (Zustand.Ding d : z.monster) if (d.id == o.id && d.name != null) namenJeTxt.put(o.txt, d.name);
            }
            Zustand.Figur.Ort naechste = null;
            for (Zustand.Figur.Ort o : figur.orte) {
                if (o.art != ORT_PERSON) continue;
                String n = namenJeTxt.get(o.txt);
                if (n != null && einfach(n).contains(einfach(name))) {
                    ziel = new double[]{o.x, o.y};
                    zielText = n;
                    break;
                }
                if (n != null) continue;
                if (Math.hypot(o.x - z.wx, o.y - z.wy) < PERSON_BESUCHT) {
                    namenJeTxt.put(o.txt, "?");
                    continue;
                }

                if (o.txt == personTxt) {
                    naechste = o;
                    break;
                }
                if (naechste == null || Math.hypot(o.x - z.wx, o.y - z.wy) < Math.hypot(naechste.x - z.wx, naechste.y - z.wy)) naechste = o;
            }
            if (ziel == null && naechste != null) {
                personTxt = naechste.txt;
                ziel = new double[]{naechste.x, naechste.y};
                zielText = "Person (Zeile " + naechste.txt + ")";
            }
        }
        String bekannt = gesehen.isEmpty() ? "" : " (gesehen: " + String.join(", ", gesehen) + ")";
        if (ziel == null) {
            g.vorgeben(Double.NaN, 0, "", false);
            if (!z.stadt) {
                ende(Ergebnis.NICHT_GEFUNDEN, "kein NPC \"" + name + "\" in Sicht (außerhalb der Stadt sucht der Auftrag nicht)" + bekannt);
                return warten();
            }
            return nichtDa(figur != null, "kein NPC \"" + name + "\" in der Stadt, Namen in der Sprache des Spiels" + bekannt, now);
        }
        suchenSeit = -1;
        g.vorgeben(ziel[0], ziel[1], zielText, false);
        unterwegs = "suche " + name + ": zu " + zielText + " bei " + Math.round(ziel[0]) + "," + Math.round(ziel[1]);
        return blockiertPruefen(z, Math.hypot(ziel[0] - z.wx, ziel[1] - z.wy), zielText, now) ? warten() : null;
    }





    static String einfach(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
            .replace("sch", "sh");
    }

    private final java.util.TreeSet<String> gesehen = new java.util.TreeSet<>();

    private final java.util.Map<Integer, String> namenJeTxt = new java.util.HashMap<>();
    private int personTxt = -1;


    final Gegnerwahl gegnerwahl = new Gegnerwahl();
    private final java.util.Map<Long, Long> gegnerGesperrt = new java.util.HashMap<>();
    private long gegnerId = -1, gegnerAm;

    static final long GEGNER_NEU_MS = 1500;

    static final long GEGNER_SPERRE_MS = 60_000;






    private Zustand.Ding gegner(Zustand z, Zustand.Ziele ziele, Gehirn g, long now) {
        java.util.Set<Long> ignoriert = g.ignorierte(now);
        Karte k = g.karte();
        if (k == null || k.level != z.gebiet || k.feld((int) z.wx, (int) z.wy) == Karte.UNBEKANNT) {
            Zustand.Ding nah = null;
            for (Zustand.Ding d : z.feinde()) {
                if (ignoriert.contains(d.id)) continue;
                if (nah == null || z.bodenAbstand(d) < z.bodenAbstand(nah)) nah = d;
            }
            return nah;
        }

        if (!g.gebietLeeren) g.zielpruefung.pruefen(z, k, z.feinde(), now);
        if (gegnerId >= 0 && now - gegnerAm < GEGNER_NEU_MS) {
            for (Zustand.Ding d : z.feinde()) if (d.id == gegnerId && !ignoriert.contains(d.id)) return d;
        }
        Gegnerwahl.Wahl w = gegnerwahl.waehlen(z, ziele, k, g.zielpruefung, 0, new Gegnerwahl.Filter() {
            @Override public boolean auslassen(Zustand.Ding d) {
                if (ignoriert.contains(d.id)) return true;
                Long bis = gegnerGesperrt.get(d.id);
                return bis != null && now < bis;
            }

            @Override public void ohneWeg(Zustand.Ding d) {
                gegnerGesperrt.put(d.id, now + GEGNER_SPERRE_MS);
            }
        });
        gegnerId = w != null ? w.d.id : -1;
        gegnerAm = now;
        return w != null ? w.d : null;
    }


    private Gehirn.Absicht suchen(Zustand z, Zustand.Ziele ziele, boolean zieleDa, Gehirn g, long now) {
        if (z.stadt) {
            ende(Ergebnis.NICHT_GEFUNDEN, "in der Stadt gibt es keine " + (art == Art.GEGNER ? "Gegner" : art == Art.SCHREIN ? "Schreine" : "Truhen"));
            return warten();
        }

        g.zielGebiet = z.gebiet;
        double[] ziel = null;
        String was;
        if (art == Art.GEGNER) {
            Gruppen.Gruppe best = null;
            for (Gruppen.Gruppe gr : Gruppen.bilden(z, g.ignorierte(now))) if (best == null || gr.abstand < best.abstand) best = gr;
            if (best != null && best.nahe(z, Gehirn.KAMPF_AN) > 0) {
                ende(Ergebnis.ANGEKOMMEN, best.text() + " in Kampfweite");
                return warten();
            }

            Zustand.Ding nah = gegner(z, ziele, g, now);
            if (nah != null) {
                ziel = Gehirn.weltlage(z, nah);
                Gruppen.Gruppe von = Gruppen.von(Gruppen.bilden(z, g.ignorierte(now)), nah.id);
                was = (von != null ? von.text() : "Gegner") + " (" + nah.name + ")";
            }
            else was = best != null && gegnerwahl.unerreichbar > 0 ? "erreichbarer Gegner" : "Gegner";
        }
        else {
            int objArt = art == Art.SCHREIN ? OBJ_SCHREIN : OBJ_TRUHE;
            Zustand.Ding best = null;
            for (Zustand.Ding d : z.objekte) {
                if (d.art != objArt || z.istAusgelassen(d)) continue;
                if (best == null || z.bodenAbstand(d) < z.bodenAbstand(best)) best = d;
            }
            if (best != null && ObjektBenutzen.ersterKandidat(z, best, false)) {
                ende(Ergebnis.ANGEKOMMEN, (art == Art.SCHREIN ? "Schrein" : "Truhe") + " " + best.id + " in Reichweite von „Nehmen“");
                return warten();
            }
            if (best != null) ziel = Gehirn.weltlage(z, best);
            was = art == Art.SCHREIN ? "Schrein" : "Truhe";
        }
        if (ziel == null) {

            g.vorgeben(Double.NaN, 0, "", false);
            unterwegs = "erkunde, kein " + was + " in Sicht";
            return blockiertPruefen(z, Double.NaN, "erkunden", now) ? warten() : null;
        }
        g.vorgeben(ziel[0], ziel[1], was, false);
        double d = Math.hypot(ziel[0] - z.wx, ziel[1] - z.wy);
        unterwegs = "zu " + was + ", noch " + Math.round(d) + " Subtiles";
        return blockiertPruefen(z, d, art.name(), now) ? warten() : null;
    }


    private Gehirn.Absicht nichtDa(boolean vollstaendig, String warum, long now) {
        unterwegs = vollstaendig ? warum : "warte auf die Ziele des Gebiets (" + warum + ")";
        if (!vollstaendig) {
            suchenSeit = -1;
            return warten();
        }
        if (suchenSeit < 0) suchenSeit = now;
        if (now - suchenSeit >= SUCHEN_MS) ende(Ergebnis.NICHT_GEFUNDEN, warum);
        return warten();
    }






    private boolean blockiertPruefen(Zustand z, double rest, String ziel, long now) {
        boolean neu = z.gebiet != besterGebiet || !ziel.equals(besterZiel);


        boolean bewegt = Double.isNaN(ankerX) || Math.hypot(z.wx - ankerX, z.wy - ankerY) > FORTSCHRITT;
        boolean weiter = bewegt || !Double.isNaN(rest) && rest < bester - FORTSCHRITT;
        if (neu || weiter) {
            bester = Double.isNaN(rest) ? Double.MAX_VALUE : Math.min(neu ? Double.MAX_VALUE : bester, rest);
            ankerX = z.wx;
            ankerY = z.wy;
            besterAm = now;
            besterGebiet = z.gebiet;
            besterZiel = ziel;
        }

        boolean kampfHaelt = now - besterAm < BLOCKIERT_KAMPF_MS;
        long seit = now - (kampfHaelt ? Math.max(besterAm, kampfAm) : besterAm);
        if (seit >= BLOCKIERT_MS) {
            ende(Ergebnis.BLOCKIERT, (now - besterAm) / 1000 + " s ohne Fortschritt" + (kampfHaelt ? "" : " (Kampf ohne Ende, --friedlich?)")
                + " bei Lage " + Math.round(z.wx) + "," + Math.round(z.wy) + " in Gebiet " + z.gebiet
                + (Double.isNaN(rest) ? "" : ", noch " + Math.round(rest) + " Subtiles bis " + ziel));
            return true;
        }
        return false;
    }

    private double ankerX = Double.NaN, ankerY = Double.NaN;
    private String besterZiel = "";

    public long kampfAm = -1;

    private static String ausgaenge(Zustand.Ziele ziele) {
        if (ziele == null || ziele.ausgaenge.isEmpty()) return "keine Ausgänge gemeldet";
        StringBuilder b = new StringBuilder("gemeldet:");
        for (Zustand.Ziele.Ausgang e : ziele.ausgaenge) {
            if (e.art == Zustand.Ziele.WAYPOINT) continue;
            b.append(' ').append(e.art == Zustand.Ziele.ENTRANCE ? "Eingang→" : "Ausgang→").append(e.nach);
        }
        return b.toString();
    }
}
