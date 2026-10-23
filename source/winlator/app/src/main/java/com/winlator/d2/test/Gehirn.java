package com.winlator.d2.test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;














public final class Gehirn {

    public static final class Absicht {

        public double richtung = Double.NaN;
        public boolean angriff;

        public String halten = Kampfwahl.ANGRIFF;




        public boolean neuDruecken;

        public String tippen;

        public int[] tippSpiel;




        public long sperreMs;





        public boolean getippt;
        public String grund = "";
    }


    static final double KAMPF_AN = 400, KAMPF_AUS = 500;




    static final double NEHMEN_SUBTILES = 20;

    static final double SUBTILE_BODEN = Math.hypot(16, 16);

    static final long NEHMEN_HALTEN_MS = 3000;

    static final long NEHMEN_ANPEILEN_MS = 5000;
    private static final long RICHTUNG_DAUER_MS = 9000, NEHMEN_PAUSE_MS = 1500, TRANK_PAUSE_MS = 4000;
    private static final int NEHMEN_VERSUCHE = 4;




    public static final int TRINKEN_PCT = 50, TRINKEN_MAX_PCT = 90;

    static final long STECKT_MS = 900;
    static final double STEHT_SUBTILES = 0.6;
    private static final long FOLGEN_MS = 1000, PROBE_MS = 600, WAND_MAX_MS = 30_000, ZIEL_MAX_MS = 60_000, KEIN_FORTSCHRITT_MS = 30_000;
    private static final double WAND_GRAD = 75;

    private static final long NEU_DRUECKEN_NACH_MS = 6000, LOSLASSEN_MS = 400;

    private static final long ANGRIFF_OHNE_WIRKUNG_MS = 12_000, IGNORIEREN_MS = 30_000;




    static final double REICHWEITE_DRUECKEN = 0.9;






    static final long HERAN_MAX_MS = ANGRIFF_OHNE_WIRKUNG_MS;
    private static final int ZELLE = 20;

    private final Random random;

    public int zielGebiet = 3;




    public boolean gebietLeeren;

    public int trinkenPct = TRINKEN_PCT;





    public boolean zielwahlUebergehtIgnorierte;
    static final double AUSGANG_ABSTAND = 8;





    static final double GRENZE_NAH = 5;
    static final long GRENZE_MAX_MS = 4000, GRENZE_PAUSE_MS = 2000;

    static final long LEER_NACH_MS = 20_000;
    private double richtung;
    private long richtungSeit, letztesNehmen, letzterTrank;
    private int letztesGebiet = -1;
    private final Map<Long, Integer> nehmenVersuche = new HashMap<>();

    private long nehmenId = -1, nehmenSeit, nehmenTippAm;
    private int nehmenGebiet;

    private Absicht nehmenTipp;




    private final java.util.Set<Long> nehmenGleichauf = new java.util.HashSet<>();

    public int aufgehoben, nehmenAufgegeben;
    public int richtungswechsel, hindernisse, ausweichFehlschlaege, neuGedrueckt, zieleGewechselt;


    private double zielX = Double.NaN, zielY = Double.NaN;

    private Karte karte;

    private java.util.List<Wegsuche.Punkt> weg;
    private int wegKnoten;

    private boolean wegTeil;
    private double wegZielX = Double.NaN, wegZielY = Double.NaN, wegStartX, wegStartY;
    private long wegGeplantAm, wegFortschrittAm, letzterKampfAm, karteWartenSeit;
    public static final long KARTE_WARTEN_MS = 4000;

    public static final int KNOTEN_OHNE_KARTE = 4000;
    private double wartenZielX = Double.NaN, wartenZielY = Double.NaN;
    private double wegBesterRest;
    private int neuplanungen;

    public int wegePlanungen, wegeNeuplanungen, wegeOhne;
    public long wegeLaengsteMs;
    public static final double WEG_ABWEICHUNG = 4, KNOTEN_ERREICHT = 2;
    public static final long WEG_KEIN_FORTSCHRITT_MS = 1500;
    public static final int NEUPLANUNGEN_MAX = 5;

    public void setKarte(Karte k) { karte = k; weg = null; }






    private double vorgabeX = Double.NaN, vorgabeY = Double.NaN;
    private String vorgabeName = "";
    private boolean vorgabeEingang;

    public boolean friedlich, aufheben = true;





    public void vorgeben(double x, double y, String name, boolean eingang) {
        if (Double.isNaN(x)) {
            if (!Double.isNaN(vorgabeX)) zielX = Double.NaN;
            vorgabeX = vorgabeY = Double.NaN;
            return;
        }

        boolean genommen = !Double.isNaN(zielX) && name.equals(zielName);
        if (Double.isNaN(vorgabeX) || !name.equals(vorgabeName) || eingang != vorgabeEingang
            || Math.hypot(x - (genommen ? zielX : vorgabeX), y - (genommen ? zielY : vorgabeY)) > 2) zielX = Double.NaN;
        vorgabeX = x;
        vorgabeY = y;
        vorgabeName = name;
        vorgabeEingang = eingang;
    }

    public Abdeckung abdeckung() { return abdeckung; }
    public Karte karte() { return karte; }


    public final Kampfwahl kampfwahl = new Kampfwahl();




    public Zustand.Layout layout;
    private long letzterManaTrank = -TRANK_PAUSE_MS;
    private String zielName = "";

    private boolean zielEingang;
    private long eingangTippAm;
    private int eingangTipps;




    private boolean eingangErsatz;

    private double ersatzX, ersatzY;

    private long zielMaxMs = ZIEL_MAX_MS;
    private long zielSeit, besterAbstandAm;
    private double besterAbstand;

    private final Map<String, Long> schlechteZiele = new HashMap<>();
    private static final long SCHLECHT_MS = 20_000;
    private final Set<Long> besucht = new HashSet<>();
    private final ArrayDeque<double[]> spur = new ArrayDeque<>();
    private long gehenSeit, stehtSeit, loslassenBis, wandSeit, probeAm, probeBis;

    private int wandSeite, wandWechsel, letzteSeite;
    private double probeRest, trefferRest;
    private long wandFreiAm;

    private boolean kampf;
    private long kampfZiel = -1, kampfZielSeit;
    private int kampfZielLeben;
    private final Map<Long, Long> ignoriert = new HashMap<>();

    private boolean angriffGehalten;
    private long heranZiel = -1, heranSeit;
    private int heranLeben;

    public int heranSchritte;


    private Abdeckung abdeckung;

    private final Set<Long> jenseits = new HashSet<>();

    private Zustand.Ziele zieleJetzt;

    private long letzterFeindAm;

    private boolean zielBlock;

    private double sicherX = Double.NaN, sicherY = Double.NaN;
    private boolean grenzeAktiv;
    private long grenzeSeit, grenzePauseBis;

    public int grenzRueckzuege, jenseitsUebergangen;
    private final Set<Long> jenseitsGemeldet = new HashSet<>();

    public String navigation = "";





    public static int trinkSchwelle(int rueckzugPct) {
        return Math.max(TRINKEN_PCT, Math.min(TRINKEN_MAX_PCT, 2 * rueckzugPct));
    }

    public Gehirn(long seed) {
        random = new Random(seed);
        richtung = random.nextDouble() * 2 * Math.PI;
    }


    public void festgefahren(long now) {
        neueRichtung(now, richtung + Math.PI / 2 + random.nextDouble() * Math.PI);
    }

    private void neueRichtung(long now, double r) {
        richtung = (r % (2 * Math.PI) + 2 * Math.PI) % (2 * Math.PI);
        richtungSeit = now;
        richtungswechsel++;
    }

    public double richtung() {
        return richtung;
    }


    public static double bildschirmWinkel(double dx, double dy) {
        return Math.atan2(8 * (dx + dy), 16 * (dx - dy));
    }

    public Absicht denken(Zustand z, Zustand.Ziele ziele, long now) {

        if (pufferAbsicht != null) {
            kampfwahl.puffer.ausgeloest(pufferAbsicht.getippt, pufferAbsichtAm);
            pufferAbsicht = null;
        }
        Absicht a = new Absicht();
        if (richtungSeit == 0) richtungSeit = now;
        if (z.gebiet != letztesGebiet) {
            if (letztesGebiet >= 0 && z.stadt && !z.weltlage()) neueRichtung(now, richtung + Math.PI);
            zielX = Double.NaN;
            schlechteZiele.clear();
            spur.clear();
            wandSeite = 0;
            letzterFeindAm = now;
            sicherX = sicherY = Double.NaN;
            grenzeAktiv = false;
            tuer.vergessen();
        }
        letztesGebiet = z.gebiet;
        if (z.weltlage()) besucht.add(zelle(z.wx, z.wy));

        zieleJetzt = ziele;
        jenseitsBestimmen(z, ziele);
        if (gebietLeeren) abdeckungPflegen(z, ziele);

        if (z.gebiet != pruefungGebiet) {
            zielpruefung.vergessen();
            anlaufSeitJe.clear();
            pruefungGebiet = z.gebiet;
        }
        if (gebietLeeren) zielpruefung.pruefen(z, karte, z.feinde(), now);
        zieleBeurteilen(z);
        long schrittMs = letzterSchritt < 0 ? 0 : Math.max(0, Math.min(1000, now - letzterSchritt));
        letzterSchritt = now;
        if (z.stadt && z.gebiet == zielGebiet && !gebietLeeren) {


            a.grund = "Ziel erreicht: Gebiet " + z.gebiet;
            return a;
        }

        if (z.lebenMax > 0 && z.lebenPct < trinkenPct && z.heilGuertel > 0 && !z.heilWirkt && now - letzterTrank > TRANK_PAUSE_MS) {
            letzterTrank = now;
            a.tippen = "heal";
            a.grund = "Leben " + z.lebenPct + " %: Heiltrank";
        }

        Zustand.Ding feind = friedlich ? null : naechsterFeind(z, now);
        if (feind != null) letzterFeindAm = now;

        if (gebietLeeren && grenze(z, ziele, now, a)) {
            if (kampf) kampfEnde(z, now);
            kampf = false;
            angriffGehalten = false;
            return a;
        }
        if (feind != null) {
            double dist = z.bodenAbstand(feind);





            boolean anlaufWeiter = feind.id == anlaufZiel && nichtDrueckbar.contains(feind.id) && !ausgeschlossen.contains(feind.id);
            if ((dist < KAMPF_AN || (kampf && dist < KAMPF_AUS) || anlaufWeiter) && anlaufen(z, feind, now, a)) return a;
            if ((dist < KAMPF_AN || (kampf && dist < KAMPF_AUS)) && !zielwechsel(z, feind, now, a)) {


                java.util.List<Gruppen.Gruppe> gruppen = Gruppen.bilden(z, ignorierte(now));
                Gruppen.Gruppe gruppe = z.hover == HOVER_MONSTER ? Gruppen.von(gruppen, z.hoverId) : null;
                if (gruppe == null) gruppe = Gruppen.von(gruppen, feind.id);
                kampfGruppe(z, gruppe, now);
                kampf = true;


                Zustand.Ding ziel = angegriffen(z, feind, now);
                if (ziel.id != kampfZiel || ziel.leben < kampfZielLeben) {
                    if (ziel.id != kampfZiel) kampfZielSeit = now;
                    kampfZiel = ziel.id;
                    kampfZielLeben = ziel.leben;
                    if (ziel.leben < 100) kampfZielSeit = now;
                }
                else if (now - kampfZielSeit > ANGRIFF_OHNE_WIRKUNG_MS) {
                    ignoriert.put(ziel.id, now + IGNORIEREN_MS);
                    navigation = "Ziel " + ziel.name + " " + (now - kampfZielSeit) / 1000 + " s ohne Wirkung: übersprungen";
                    kampfZiel = -1;
                    uebersprungen = ziel.id;
                    zieleOhneWirkung++;
                    uebersprungenAm = now;
                    neuGedruecktNachSprung = false;
                }

                String knopf = kampfwahl.knopf(z, layout, kampfZiel, now - kampfZielSeit, gruppe, now);


                if (heran(z, feind, dist, now, a, weiteFuer(knopf, layout))) return a;
                if (knopf != null) kampfKnoepfe.add(knopf.equals(Kampfwahl.ANGRIFF) ? "Angriff" : knopf + (kampfwahl.stark() ? " stark" : ""));
                a.angriff = knopf != null;
                if (knopf != null) a.halten = knopf;
                if (kampfwahl.elementGewechselt) navigation = "Ziel " + feind.name + " ohne Wirkung: anderes Element";
                if (kampfwahl.manaFehlt && a.tippen == null && z.manaGuertel > 0 && !z.manaWirkt && now - letzterManaTrank > TRANK_PAUSE_MS) {
                    letzterManaTrank = now;
                    a.tippen = "mana";
                    a.grund = "Mana " + z.mana + " unter " + kampfwahl.gewuenschteKosten + ": Manatrank";
                }
                letzterKampfAm = now;
                gehenSeit = 0;
                spur.clear();
                if (a.grund.isEmpty()) {
                    String wie = knopf == null ? "Warten (Mana)" : knopf.equals(Kampfwahl.ANGRIFF) ? "Angriff"
                        : "Fertigkeit " + kampfwahl.fertigkeit() + " [" + knopf + "]";
                    a.grund = wie + ": " + feind.name + " (" + Math.round(dist) + ", " + feind.leben + " %)";
                }
                angriffGehalten = a.angriff && !a.neuDruecken;
                if (a.angriff) angriffZaehlen(ziel, schrittMs);
                return a;
            }
        }
        if (kampf) kampfEnde(z, now);
        kampf = false;
        angriffGehalten = false;


        heranZiel = -1;
        kampfZiel = -1;


        if (nehmenId >= 0 && nehmenTippAm > 0 && nehmen(z, now, a)) return a;


        if (a.tippen == null) {
            String puffer = kampfwahl.puffer.tippen(z, layout, kampfwahl.tabelle(), feindAbstand(z), now);
            if (kampfwahl.puffer.befund != null && !kampfwahl.puffer.befund.equals(pufferBefund)) {
                pufferBefund = kampfwahl.puffer.befund;
                navigation = pufferBefund;
            }
            if (puffer != null) {
                pufferAbsicht = a;
                pufferAbsichtAm = now;
                a.tippen = puffer;
                a.sperreMs = Puffer.SPERRE_MS;
                a.richtung = Double.NaN;
                a.grund = kampfwahl.puffer.grund;
                gehenSeit = 0;
                spur.clear();
                return a;
            }
        }

        if (nehmen(z, now, a)) return a;


        if (z.weltlage() && ziele != null && ziele.gebiet == z.gebiet && ziele.bereit != 1) {
            gehen(z, ziele, now, a);
            return a;
        }


        if (gebietLeeren) {
            a.richtung = Double.NaN;
            if (a.grund.isEmpty()) a.grund = "keine Gebietsdaten: stehen";
            return a;
        }


        if (now - richtungSeit > RICHTUNG_DAUER_MS) neueRichtung(now, richtung + (random.nextDouble() - 0.5) * Math.PI);
        a.richtung = richtung;
        if (a.grund.isEmpty()) a.grund = "Erkunden " + Math.round(Math.toDegrees(richtung)) + "°";
        return a;
    }








    private boolean nehmen(Zustand z, long now, Absicht a) {
        if (!aufheben) return false;
        if (nehmenId >= 0 && (z.gebiet != nehmenGebiet || letzterKampfAm > nehmenSeit)) nehmenId = -1;
        if (nehmenId < 0) {
            if (now - letztesNehmen <= NEHMEN_PAUSE_MS) return false;
            Zustand.Ding best = null;
            for (Zustand.Ding d : z.items) {
                if (!gewollt(z, d) || z.bodenAbstand(d) > NEHMEN_SUBTILES * SUBTILE_BODEN) continue;

                if (gebietLeeren && jenseits(z, zieleJetzt, d)) continue;
                Integer tries = nehmenVersuche.get(d.id);
                if (tries != null && tries >= NEHMEN_VERSUCHE) continue;
                if (best == null || z.bodenAbstand(d) < z.bodenAbstand(best)) best = d;
            }
            if (best == null) return false;
            Integer tries = nehmenVersuche.get(best.id);
            nehmenVersuche.put(best.id, tries == null ? 1 : tries + 1);
            nehmenId = best.id;
            nehmenSeit = now;
            nehmenTippAm = 0;
            nehmenTipp = null;
            nehmenGleichauf.clear();
            nehmenGebiet = z.gebiet;
        }
        if (nehmenTipp != null) {

            if (!nehmenTipp.getippt) nehmenTippAm = 0;
            nehmenTipp = null;
        }
        Zustand.Ding d = null;
        for (Zustand.Ding i : z.items) if (i.id == nehmenId) d = i;
        if (d != null && nehmenTippAm > 0 && !nehmenGleichauf.isEmpty()) {

            java.util.Set<Long> da = new java.util.HashSet<>();
            for (Zustand.Ding i : z.items) da.add(i.id);
            for (long id : nehmenGleichauf) if (!da.contains(id)) d = null;
        }
        if (d == null) {
            if (nehmenTippAm > 0) {
                aufgehoben++;
                eigeneBeute.add(nehmenId);
                navigation = "Nehmen: aufgehoben (" + aufgehoben + ", " + (now - nehmenTippAm) + " ms nach dem Tipp)";
            }
            nehmenId = -1;
            return false;
        }
        if (nehmenTippAm > 0 ? now - nehmenTippAm > NEHMEN_HALTEN_MS : now - nehmenSeit > NEHMEN_ANPEILEN_MS) {


            nehmenVersuche.put(d.id, NEHMEN_VERSUCHE);
            nehmenAufgegeben++;
            navigation = "Nehmen: Art " + d.art + " aufgegeben (" + (nehmenTippAm > 0 ? "nach dem Tipp" : "nicht erreicht") + ")";
            nehmenId = -1;
            letztesNehmen = now;
            return false;
        }
        gehenSeit = 0;
        spur.clear();
        if (nehmenTippAm == 0 && !ersterKandidat(z, d, gebietLeeren, null, beute)) {

            a.richtung = Math.atan2(d.y - z.py, d.x - z.px);
            if (a.grund.isEmpty()) a.grund = "Nehmen: hin zu Art " + d.art + " (" + Math.round(z.bodenAbstand(d) / SUBTILE_BODEN) + " Subtiles)";
            return true;
        }
        a.richtung = Double.NaN;
        if (nehmenTippAm == 0 && a.tippen == null) {
            nehmenTippAm = now;
            letztesNehmen = now;
            nehmenTipp = a;
            nehmenGleichauf.clear();
            ersterKandidat(z, d, gebietLeeren, nehmenGleichauf, beute);
            a.tippen = "interact";
            a.grund = "Nehmen: Art " + d.art + beuteText(z, d) + " (" + Math.round(z.bodenAbstand(d) / SUBTILE_BODEN) + " Subtiles)";
        }
        else if (a.grund.isEmpty()) a.grund = "Nehmen: warten, bis Art " + d.art + " weg ist";
        return true;
    }

    private boolean gewollt(Zustand z, Zustand.Ding d) {
        return gewollt(z, d, beute);
    }






    static boolean gewollt(Zustand z, Zustand.Ding d, Beuteliste liste) {
        if (d.art == Zustand.ITEM_GOLD) return !z.goldVoll();
        if (d.art == Zustand.ITEM_POTION) return true;
        Zustand.Beute b = z.beute.get(d.id);
        Beuteliste.Antwort a = b != null ? liste.antwort(b.kategorie, b.qualitaet, b.sockel, b.wertklasse) : null;
        if (a == null) return d.art == Zustand.ITEM_RUNE || d.art == Zustand.ITEM_GEM;
        return a != Beuteliste.Antwort.LIEGEN && (z.zellen < 0 || Beuteliste.passt(z.zellen, b.breite, b.hoehe));
    }


    private String beuteText(Zustand z, Zustand.Ding d) {
        Zustand.Beute b = z.beute.get(d.id);
        int g = b != null ? Beuteliste.gruppe(b.kategorie, b.qualitaet, b.sockel) : -1;
        return g < 0 ? "" : ", " + Beuteliste.NAMEN[g] + " → " + beute.antwort(b.kategorie, b.qualitaet, b.sockel, b.wertklasse);
    }


    public final Beuteliste beute = new Beuteliste();

    public final java.util.Set<Long> eigeneBeute = new java.util.HashSet<>();


    private static final int OBJEKT_PORTAL = 5, OBJEKT_EINGANG = 6, OBJEKT_LEICHE = 7;











    static boolean ersterKandidat(Zustand z, Zustand.Ding item, boolean ohneDurchgaenge, java.util.Set<Long> gleichauf) {
        return ersterKandidat(z, item, ohneDurchgaenge, gleichauf, Beuteliste.VORGABE);
    }

    static boolean ersterKandidat(Zustand z, Zustand.Ding item, boolean ohneDurchgaenge, java.util.Set<Long> gleichauf, Beuteliste liste) {
        double reach = (z.gh > 0 ? z.gh : 600) * 0.55, s = z.bodenAbstand(item);
        if (s > reach) return false;
        double score = s * 0.8;
        for (Zustand.Ding d : z.items) {
            if (d.id == item.id) continue;
            double t = z.bodenAbstand(d);
            if (t > reach || t * 0.8 > score + GLEICH) continue;
            if (t * 0.8 < score - GLEICH || !gewollt(z, d, liste)) return false;
            if (gleichauf != null) gleichauf.add(d.id);
        }
        for (Zustand.Ding d : z.objekte) {
            if (ohneDurchgaenge && (d.art == OBJEKT_PORTAL || d.art == OBJEKT_EINGANG)) continue;
            if (z.istAusgelassen(d)) continue;
            double t = z.bodenAbstand(d);
            if (t <= reach && (d.art == OBJEKT_LEICHE || t <= score + GLEICH)) return false;
        }
        if (z.stadt) for (Zustand.Ding d : z.monster) {
            double t = z.bodenAbstand(d);
            if (t <= reach && t <= score + GLEICH) return false;
        }
        if (gleichauf != null && !gleichauf.isEmpty()) gleichauf.add(item.id);
        return true;
    }


    private void ersatzErreicht(Zustand z) {
        eingangErsatz = true;
        ersatzX = z.wx;
        ersatzY = z.wy;
    }


    private static final double GLEICH = 0.01;


    private double abstandZumWeg(Zustand z) {
        Wegsuche.Punkt k = weg.get(wegKnoten);
        double ax = wegKnoten > 0 ? weg.get(wegKnoten - 1).x : wegStartX, ay = wegKnoten > 0 ? weg.get(wegKnoten - 1).y : wegStartY;
        double vx = k.x - ax, vy = k.y - ay, len2 = vx * vx + vy * vy;
        double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((z.wx - ax) * vx + (z.wy - ay) * vy) / len2));
        return Math.hypot(z.wx - (ax + t * vx), z.wy - (ay + t * vy));
    }












    private boolean heran(Zustand z, Zustand.Ding feind, double dist, long now, Absicht a, double reichweite) {
        if (feind.id != heranZiel || feind.leben < heranLeben) {
            heranZiel = feind.id;
            heranLeben = feind.leben;
            heranSeit = now;
        }
        if (reichweite <= 0) return false;
        double weite = reichweite * (angriffGehalten ? 1 : REICHWEITE_DRUECKEN);
        if (dist <= weite) return false;
        angriffGehalten = false;
        letzterKampfAm = now;
        gehenSeit = 0;
        spur.clear();
        if (now - heranSeit > HERAN_MAX_MS) {
            ignoriert.put(feind.id, now + IGNORIEREN_MS);
            navigation = "Ziel " + feind.name + " " + (now - heranSeit) / 1000 + " s nicht in Reichweite der Zielhilfe: übersprungen";
            heranZiel = -1;
            kampfZiel = -1;
            a.richtung = Double.NaN;
            if (a.grund.isEmpty()) a.grund = "Übersprungen: " + feind.name + " (nicht erreicht)";
            return true;
        }

        kampfZiel = -1;
        heranSchritte++;
        a.richtung = Math.atan2(feind.y - z.py, feind.x - z.px);
        if (a.grund.isEmpty()) a.grund = "Heran: " + feind.name + " (" + Math.round(dist) + " > " + Math.round(weite) + ")";
        return true;
    }







    double weiteFuer(String knopf, Zustand.Layout l) {
        if (l == null) return 0;
        if (knopf == null || knopf.equals(Kampfwahl.ANGRIFF) || l.zielweiteNah <= 0 || l.zielweiteFern <= 0
            || kampfwahl.tabelle() == null) return l.zielweite;
        int range = com.winlator.d2.D2AimReach.range(kampfwahl.tabelle(), kampfwahl.fertigkeit());
        if (range == Fertigkeiten.RANGE_BOTH) return l.zielweite;
        return range == Fertigkeiten.RANGE_H2H ? l.zielweiteNah : l.zielweiteFern;
    }


    private long uebersprungen = -1, uebersprungenAm;
    private boolean neuGedruecktNachSprung;
    static final long ZIELWECHSEL_MS = 1000;








    private boolean zielwechsel(Zustand z, Zustand.Ding feind, long now, Absicht a) {
        if (uebersprungen < 0 || feind.id == uebersprungen) return false;
        Long until = ignoriert.get(uebersprungen);
        Zustand.Ding alt = null;
        for (Zustand.Ding d : z.feinde()) if (d.id == uebersprungen) alt = d;
        if (alt == null || until == null || now >= until) {
            uebersprungen = -1;
            return false;
        }
        a.richtung = Math.atan2(feind.y - z.py, feind.x - z.px);
        if (now - uebersprungenAm < ZIELWECHSEL_MS || neuGedruecktNachSprung) return false;
        if (zielwahlUebergehtIgnorierte || !nimmtAltes(z, alt, feind, layout, weiteFuer(kampfwahl.gewaehlt(), layout))) {
            a.neuDruecken = true;
            neuGedruecktNachSprung = true;
            navigation = "neu drücken: Ziel " + feind.name + " statt des übersprungenen";
            return false;
        }
        navigation = "übersprungenes Ziel noch im Zielbereich: weitergehen";
        return true;
    }






    static boolean nimmtAltes(Zustand z, Zustand.Ding alt, Zustand.Ding feind, Zustand.Layout l) {
        return nimmtAltes(z, alt, feind, l, l != null ? l.zielweite : 0);
    }


    static boolean nimmtAltes(Zustand z, Zustand.Ding alt, Zustand.Ding feind, Zustand.Layout l, double weite) {
        if (l == null || (weite <= 0 && l.zielkegel <= 0)) return false;
        if (weite > 0 && z.bodenAbstand(alt) > weite) return false;
        if (l.zielkegel > 0) {
            double ax = alt.x - z.px, ay = alt.y - z.py, fx = feind.x - z.px, fy = feind.y - z.py;
            double cos = (ax * fx + ay * fy) / Math.max(1e-6, Math.hypot(ax, ay) * Math.hypot(fx, fy));
            if (cos < Math.cos(Math.toRadians(l.zielkegel))) return false;
        }
        return true;
    }





    public java.util.Set<Long> ignorierte(long now) {
        java.util.Set<Long> out = new HashSet<>(jenseits);
        out.addAll(nichtDrueckbar);
        for (Map.Entry<Long, Long> e : ignoriert.entrySet()) if (now < e.getValue()) out.add(e.getKey());
        return out;
    }





    public static double[] weltlage(Zustand z, Zustand.Ding d) {
        double sx = d.x - z.px, sy = d.y - z.py;
        return new double[]{z.wx + (sx / 16 + sy / 8) / 2, z.wy + (sy / 8 - sx / 16) / 2};
    }


    public static boolean jenseits(Zustand z, Zustand.Ziele ziele, Zustand.Ding d) {
        if (ziele == null || ziele.gebiet != z.gebiet || !z.weltlage()) return false;
        double[] w = weltlage(z, d);
        return nahAusgang(ziele, w[0], w[1]) || !imGebiet(ziele, w[0], w[1]);
    }

    private void jenseitsBestimmen(Zustand z, Zustand.Ziele ziele) {
        jenseits.clear();
        if (!gebietLeeren) return;
        for (Zustand.Ding d : z.feinde()) {
            if (!jenseits(z, ziele, d)) continue;
            jenseits.add(d.id);
            if (jenseitsGemeldet.add(d.id)) {
                jenseitsUebergangen++;
                double[] w = weltlage(z, d);
                navigation = "Grenze: " + d.name + " bei " + Math.round(w[0]) + "," + Math.round(w[1]) + " im Ausgangsbereich übergangen";
            }
        }
    }


    private void abdeckungPflegen(Zustand z, Zustand.Ziele ziele) {
        if (karte == null || ziele == null || karte.level != z.gebiet || ziele.gebiet != z.gebiet) return;
        if (abdeckung == null || abdeckung.karte != karte || !abdeckung.passt(ziele)) abdeckung = new Abdeckung(karte, ziele);
        if (z.weltlage()) abdeckung.sehen(z.wx, z.wy, sichtRadius(z));
    }





    public static double sichtRadius(Zustand z) {
        int gw = z.gw > 0 ? z.gw : 800, gh = z.gh > 0 ? z.gh : 600;
        double sicht = Math.min(gw / 2.0 / 32 * Math.sqrt(2), gh / 2.0 / 16 * Math.sqrt(2));
        return Math.min(12, sicht - 4.5);
    }


    public boolean imKampf() {
        return kampf;
    }


    public final Zielpruefung zielpruefung = new Zielpruefung();
    {
        zielpruefung.suchenBisBoden = KAMPF_AUS + 100;
    }
    private int pruefungGebiet = -1;
    private long letzterSchritt = -1;




    public long angriffMs, fernOhneSichtMs, unerreichbarMs;
    public int zieleOhneWirkung;


    boolean fernkampf() {
        if (layout == null || layout.zielweiteNah <= 0) return false;
        return weiteFuer(kampfwahl.gewaehlt(), layout) > layout.zielweiteNah + 1;
    }

    private void angriffZaehlen(Zustand.Ding ziel, long ms) {
        angriffMs += ms;
        Zielpruefung.Urteil u = zielpruefung.urteil(ziel.id);
        if (u == null) return;
        if (fernkampf() && !u.sicht) fernOhneSichtMs += ms;
        if (u.geprueft && !u.erreichbar) unerreichbarMs += ms;
    }


    public String zielpruefungText() {
        return String.format(java.util.Locale.ROOT, "Angriff gehalten %d s, davon Fernkampf ohne Schusslinie %d s, auf Unerreichbare %d s"
            + " · Ziele ohne Wirkung aufgegeben %d · Wegprüfungen %d (unerreichbar %d) · zur Schusslinie gelaufen %d (aufgegeben %d)",
            angriffMs / 1000, fernOhneSichtMs / 1000, unerreichbarMs / 1000, zieleOhneWirkung, zielpruefung.suchen,
            zielpruefung.unerreichbarGefunden, anlaeufe, anlaeufeAufgegeben);
    }





    public boolean gebietLeer(long now) {
        return gebietLeeren && abdeckung != null && abdeckung.gebiet == letztesGebiet && abdeckung.vollstaendig()
            && abdeckung.offen(now) == 0 && now - letzterFeindAm >= LEER_NACH_MS;
    }


    public String abdeckungText(long now) {
        Abdeckung ab = abdeckung;
        if (ab == null || !ab.vollstaendig()) return "keine vollständige Karte";
        return "offen " + ab.offen(now) + ", gesehen " + ab.gesehen() + ", aufgegeben " + ab.aufgegeben() + " (Rechnen bis " + ab.rechnenMs + " ms)";
    }







    private boolean grenze(Zustand z, Zustand.Ziele ziele, long now, Absicht a) {
        if (ziele == null || ziele.gebiet != z.gebiet || !z.weltlage()) {
            grenzeAktiv = false;
            return false;
        }
        Zustand.Ziele.Ausgang naechster = null;
        double nah = Double.MAX_VALUE;
        for (Zustand.Ziele.Ausgang e : ziele.ausgaenge) {
            if (e.art == Zustand.Ziele.WAYPOINT) continue;
            double d = abstandZuAusgang(e, z.wx, z.wy);
            if (d < nah) {
                nah = d;
                naechster = e;
            }
        }
        if (nah >= AUSGANG_ABSTAND) {
            sicherX = z.wx;
            sicherY = z.wy;
        }
        if (grenzeAktiv) {
            if (naechster == null || nah >= AUSGANG_ABSTAND || now - grenzeSeit > GRENZE_MAX_MS) {
                grenzeAktiv = false;
                if (naechster != null && nah < AUSGANG_ABSTAND) grenzePauseBis = now + GRENZE_PAUSE_MS;
                return false;
            }
        }
        else {
            if (naechster == null || nah >= GRENZE_NAH || now < grenzePauseBis) return false;
            grenzeAktiv = true;
            grenzeSeit = now;
            grenzRueckzuege++;
            navigation = "Grenze: zurück ins Gebiet (" + Math.round(nah) + " Subtiles vor " + name(naechster) + ")";
        }
        double ix, iy;
        if (naechster.outX != 0 || naechster.outY != 0) {
            ix = -naechster.outX;
            iy = -naechster.outY;
        }
        else if (!Double.isNaN(sicherX)) {
            ix = sicherX - z.wx;
            iy = sicherY - z.wy;
        }
        else {
            ix = ziele.box[0] + ziele.box[2] / 2.0 - z.wx;
            iy = ziele.box[1] + ziele.box[3] / 2.0 - z.wy;
        }
        a.richtung = bildschirmWinkel(ix, iy);
        a.angriff = false;
        if (a.grund.isEmpty()) a.grund = "Grenze: zurück ins Gebiet (" + Math.round(nah) + " Subtiles vor " + name(naechster) + ")";
        gehenSeit = 0;
        spur.clear();
        weg = null;
        return true;
    }


    static double abstandZuAusgang(Zustand.Ziele.Ausgang e, double x, double y) {
        double dx = Math.max(0, Math.max(e.x0 - x, x - e.x1)), dy = Math.max(0, Math.max(e.y0 - y, y - e.y1));
        return Math.hypot(dx, dy);
    }

    private String pufferBefund;

    private Absicht pufferAbsicht;
    private long pufferAbsichtAm;


    static final int HOVER_MONSTER = 1;


    private long kampfBeginn;
    private int kampfManaVorher;
    private double kampfStaerke;
    private boolean kampfStark;
    private final java.util.Set<String> kampfKnoepfe = new java.util.LinkedHashSet<>();

    public int kaempfe, starkeKaempfe;




    public String kampfMeldung = "";
    public int kampfMeldungen;

    private void kampfMelden(String text) {
        kampfMeldung = text;
        kampfMeldungen++;
    }


    private void kampfGruppe(Zustand z, Gruppen.Gruppe g, long now) {
        if (!kampf) {
            kampfBeginn = now;
            kampfManaVorher = z.mana;
            kampfStaerke = 0;
            kampfStark = false;
            kampfKnoepfe.clear();
            kaempfe++;
        }
        if (g == null) return;
        boolean neu = !kampf || (g.stark() && !kampfStark) || g.staerke >= kampfStaerke + 3;
        if (g.stark() && !kampfStark) starkeKaempfe++;
        kampfStark |= g.stark();
        kampfStaerke = Math.max(kampfStaerke, g.staerke);
        if (neu) kampfMelden("Kampf: " + g.text() + ", Mana " + z.mana);
    }


    private void kampfEnde(Zustand z, long now) {
        kampfMelden("Kampf zu Ende: " + (now - kampfBeginn) / 1000 + " s, Mana " + kampfManaVorher + " → " + z.mana
            + ", Stärke bis " + String.format(java.util.Locale.GERMAN, "%.1f", kampfStaerke) + (kampfStark ? " (stark)" : "")
            + ", Knöpfe " + (kampfKnoepfe.isEmpty() ? "keine" : String.join(", ", kampfKnoepfe)));
    }


    private static double feindAbstand(Zustand z) {
        double nah = Double.MAX_VALUE;
        for (Zustand.Ding d : z.feinde()) nah = Math.min(nah, z.bodenAbstand(d));
        return nah;
    }






    public long gehaltenesZiel = -1;


    private Zustand.Ding angegriffen(Zustand z, Zustand.Ding naechster, long now) {
        if (gehaltenesZiel <= 0) return naechster;
        for (Zustand.Ding d : z.feinde()) {
            if (d.id != gehaltenesZiel) continue;
            Long until = ignoriert.get(d.id);
            return until != null && now < until ? naechster : d;
        }
        return naechster;
    }

    private Zustand.Ding naechsterFeind(Zustand z, long now) {
        Zustand.Ding best = null, bestAnlauf = null;
        double nah = Double.MAX_VALUE, nahAnlauf = Double.MAX_VALUE;
        for (Zustand.Ding d : z.feinde()) {
            Long until = ignoriert.get(d.id);
            if (until != null && now < until) continue;
            if (jenseits.contains(d.id)) continue;
            if (ausgeschlossen.contains(d.id)) continue;
            double dist = z.bodenAbstand(d);

            if (nichtDrueckbar.contains(d.id)) {
                if (dist < nahAnlauf) {
                    nahAnlauf = dist;
                    bestAnlauf = d;
                }
            }
            else if (dist < nah) {
                nah = dist;
                best = d;
            }
        }
        return best != null ? best : bestAnlauf;
    }







    private void zieleBeurteilen(Zustand z) {
        nichtDrueckbar.clear();
        ausgeschlossen.clear();
        if (!gebietLeeren) return;
        boolean fern = fernkampf();
        for (Zustand.Ding d : z.feinde()) {
            Zielpruefung.Urteil u = zielpruefung.urteil(d.id);
            if (u == null || !u.geprueft && u.sicht) continue;
            boolean drueckbar = u.sicht && (u.erreichbar || fern);
            if (drueckbar) {
                anlaufSeitJe.remove(d.id);
                continue;
            }
            nichtDrueckbar.add(d.id);



            if (!u.erreichbar || u.weg == null || u.weg.isEmpty() || (d.extra & GEFAEHRLICH) != 0) ausgeschlossen.add(d.id);
        }
    }






    private boolean anlaufen(Zustand z, Zustand.Ding feind, long now, Absicht a) {
        if (!nichtDrueckbar.contains(feind.id) || ausgeschlossen.contains(feind.id)) {
            anlaufZiel = -1;
            return false;
        }
        Zielpruefung.Urteil u = zielpruefung.urteil(feind.id);

        if (kampf) kampfEnde(z, now);
        kampf = false;
        angriffGehalten = false;
        heranZiel = -1;
        kampfZiel = -1;
        if (feind.id != anlaufZiel) {
            anlaufZiel = feind.id;
            anlaeufe++;
        }

        Long seit = anlaufSeitJe.get(feind.id);
        if (seit == null) anlaufSeitJe.put(feind.id, seit = now);
        if (now - seit > HERAN_MAX_MS) {
            ignoriert.put(feind.id, now + IGNORIEREN_MS);
            anlaufSeitJe.remove(feind.id);
            anlaufZiel = -1;
            anlaeufeAufgegeben++;
            navigation = "Ziel " + feind.name + " hinter Wand/Tür " + HERAN_MAX_MS / 1000 + " s nicht in Schusslinie: übersprungen";
            a.richtung = Double.NaN;
            return true;
        }
        a.angriff = false;
        gehenSeit = 0;
        spur.clear();
        if (tueren(z, now, a, u.weg, 0, false)) return true;
        Wegsuche.Punkt k = u.knoten != null ? u.knoten : u.weg.get(u.weg.size() - 1);
        a.richtung = bildschirmWinkel(k.x - z.wx, k.y - z.wy);
        if (a.grund.isEmpty()) a.grund = "Ziel " + feind.name + " hinter Wand/Tür: hin bis zur Schusslinie (" + u.weg.size() + " Knoten)";
        if (!navigation.startsWith("Ziel " + feind.name + " hinter")) navigation = "Ziel " + feind.name + " hinter Wand/Tür: den Weg entlang";
        return true;
    }


    static final int GEFAEHRLICH = 2 | 4 | 8;

    private final Set<Long> nichtDrueckbar = new HashSet<>(), ausgeschlossen = new HashSet<>();
    private long anlaufZiel = -1;

    private final Map<Long, Long> anlaufSeitJe = new HashMap<>();

    public int anlaeufe, anlaeufeAufgegeben;



    private void gehen(Zustand z, Zustand.Ziele ziele, long now, Absicht a) {

        boolean erledigt = zielBlock && abdeckung != null && abdeckung.zielErledigt();
        if (Double.isNaN(zielX) || erledigt || now - zielSeit > zielMaxMs || (now - besterAbstandAm > KEIN_FORTSCHRITT_MS)) {
            if (!Double.isNaN(zielX) && !erledigt) zielAufgegeben(now);
            waehleZiel(z, ziele, now);
        }
        if (keinZiel(a)) return;
        double dx = zielX - z.wx, dy = zielY - z.wy, rest = Math.hypot(dx, dy);
        if (eingangErsatz && Math.hypot(z.wx - ersatzX, z.wy - ersatzY) > KNOTEN_ERREICHT) {

            eingangErsatz = false;
            weg = null;
        }
        if (zielEingang && (rest < 4 || eingangErsatz) && now - eingangTippAm > 2500 && a.tippen == null) {


            if (++eingangTipps > 4) {
                navigation = "Eingang " + zielName + ": " + (eingangTipps - 1) + " Tipps ohne Gebietswechsel – anderes Ziel";
                schlechteZiele.put(zielName, now + SCHLECHT_MS);
                zieleGewechselt++;
                waehleZiel(z, ziele, now);
                keinZiel(a);
                return;
            }
            eingangTippAm = now;
            a.tippen = "interact";
            a.richtung = Double.NaN;
            a.grund = "Eingang " + zielName + ": antippen";
            navigation = a.grund;
            return;
        }
        if (rest < besterAbstand - 1) {
            besterAbstand = rest;
            besterAbstandAm = now;
        }
        if (zielEingang && (rest < 4 || eingangErsatz)) {

            a.richtung = Double.NaN;
            if (a.grund.isEmpty()) a.grund = "am Eingang " + zielName;
            return;
        }
        if (rest < 3) {

            schlechteZiele.put(zielName, now + SCHLECHT_MS);
            waehleZiel(z, ziele, now);
            if (keinZiel(a)) return;
            dx = zielX - z.wx;
            dy = zielY - z.wy;
            rest = Math.hypot(dx, dy);
        }
        double winkel = bildschirmWinkel(dx, dy);


        if (now < loslassenBis) {
            a.richtung = Double.NaN;
            a.grund = "Joystick kurz loslassen (Figur stand trotz Joystick)";
            gehenSeit = 0;
            return;
        }


        if (tueren(z, now, a)) return;



        if (karte != null && karte.level == z.gebiet && karte.feld((int) z.wx, (int) z.wy) != Karte.UNBEKANNT) {
            boolean neuesZiel = zielX != wegZielX || zielY != wegZielY;
            boolean abweichung = weg != null && now - wegGeplantAm > 500 && abstandZumWeg(z) > WEG_ABWEICHUNG;
            boolean stockt = weg != null && now - wegFortschrittAm > WEG_KEIN_FORTSCHRITT_MS;
            if (weg == null || neuesZiel || abweichung || stockt) {


                if (zielX != wartenZielX || zielY != wartenZielY) { karteWartenSeit = now; wartenZielX = zielX; wartenZielY = zielY; }
                if (!karte.korridorGeladen((int) z.wx, (int) z.wy, (int) Math.round(zielX), (int) Math.round(zielY), 20)
                    && now - karteWartenSeit < KARTE_WARTEN_MS) {
                    a.richtung = bildschirmWinkel(dx, dy);
                    if (a.grund.isEmpty()) a.grund = "Karte lädt: Richtung " + zielName;
                    if (!navigation.startsWith("Karte lädt")) navigation = "Karte lädt: kurz Richtung " + zielName;
                    return;
                }
                if (neuesZiel) neuplanungen = 0;
                else if (weg != null) {
                    wegeNeuplanungen++;

                    if (now - letzterKampfAm > 2000) neuplanungen++;
                }
                if (neuplanungen > NEUPLANUNGEN_MAX) {
                    navigation = "Wegsuche: " + neuplanungen + " Neuplanungen vor " + zielName + ": anderes Ziel";
                    zielAufgegeben(now);
                    weg = null;
                    waehleZiel(z, ziele, now);
                    keinZiel(a);
                    return;
                }
                long t0 = System.nanoTime();
                boolean korridor = karte.korridorGeladen((int) z.wx, (int) z.wy, (int) Math.round(zielX), (int) Math.round(zielY), 20);
                Wegsuche.Weg w = Wegsuche.suche(karte, (int) z.wx, (int) z.wy, (int) Math.round(zielX), (int) Math.round(zielY),
                    korridor ? Wegsuche.MAX_KNOTEN : KNOTEN_OHNE_KARTE);
                long sucheMs = (System.nanoTime() - t0) / 1_000_000;
                if (sucheMs > wegeLaengsteMs) wegeLaengsteMs = sucheMs;
                wegePlanungen++;
                wegZielX = zielX; wegZielY = zielY; wegStartX = z.wx; wegStartY = z.wy;
                wegGeplantAm = now; wegFortschrittAm = now; wegBesterRest = Double.MAX_VALUE;
                if (w != null && w.knoten.isEmpty() && zielEingang) {
                    ersatzErreicht(z);
                    a.richtung = Double.NaN;
                    if (a.grund.isEmpty()) a.grund = "am Eingang " + zielName;
                    return;
                }
                if (w != null && w.knoten.isEmpty()) {

                    navigation = "Weg zu " + zielName + ": schon da (" + Math.round(rest) + " Subtiles vom Ziel)";
                    schlechteZiele.put(zielName, now + SCHLECHT_MS);
                    weg = null;
                    waehleZiel(z, ziele, now);
                    keinZiel(a);
                    return;
                }
                if (w == null && zielBlock) {

                    weg = null;
                    wegeOhne++;
                    navigation = "Wegsuche: kein Weg zu " + zielName + " – nächstes Ziel";
                    zielAufgegeben(now);
                    waehleZiel(z, ziele, now);
                    keinZiel(a);
                    return;
                }
                if (w == null) {
                    weg = null;
                    wegeOhne++;
                    navigation = "Wegsuche: kein Weg zu " + zielName + " – Wandfolgen";
                }
                else {
                    weg = w.knoten;
                    wegTeil = w.abgebrochen;
                    wegKnoten = 0;



                    zielMaxMs = Math.max(zielMaxMs, (now - zielSeit) + ZIEL_MAX_MS + 30L * w.kosten);
                    navigation = "Weg zu " + zielName + ": " + w.knoten.size() + " Knoten, " + w.untersucht + " untersucht in " + sucheMs + " ms" + (korridor ? "" : " (Karte unvollständig)")
                        + (w.abgebrochen ? " (Teilweg)" : "") + (abweichung ? " (Abweichung)" : stockt ? " (stockt)" : "");
                }
            }
            if (weg != null) {


                while (wegKnoten < weg.size() - 1 && Math.hypot(weg.get(wegKnoten).x - z.wx, weg.get(wegKnoten).y - z.wy) < KNOTEN_ERREICHT) {
                    wegKnoten++;
                    wegFortschrittAm = now;
                    wegBesterRest = Double.MAX_VALUE;
                }
                Wegsuche.Punkt k = weg.get(wegKnoten);
                double knotenRest = Math.hypot(k.x - z.wx, k.y - z.wy);
                if (knotenRest < wegBesterRest - 0.5) { wegBesterRest = knotenRest; wegFortschrittAm = now; }

                if (wegKnoten == weg.size() - 1 && knotenRest < KNOTEN_ERREICHT && wegTeil) {

                    weg = null;
                    neuplanungen = 0;
                    navigation = "Teilweg zu " + zielName + " zu Ende: weiter planen (" + Math.round(rest) + " Subtiles)";
                    a.richtung = Double.NaN;
                    if (a.grund.isEmpty()) a.grund = navigation;
                    return;
                }
                if (wegKnoten == weg.size() - 1 && knotenRest < KNOTEN_ERREICHT && zielEingang) {



                    weg = null;
                    ersatzErreicht(z);
                    a.richtung = Double.NaN;
                    if (a.grund.isEmpty()) a.grund = "am Eingang " + zielName + " (" + Math.round(rest) + " Subtiles)";
                    return;
                }
                if (wegKnoten == weg.size() - 1 && knotenRest < KNOTEN_ERREICHT) {
                    navigation = "Weg zu " + zielName + " zu Ende (" + Math.round(rest) + " Subtiles vom Ziel)";
                    schlechteZiele.put(zielName, now + SCHLECHT_MS);
                    weg = null;
                    waehleZiel(z, ziele, now);
                    keinZiel(a);
                    return;
                }
                a.richtung = bildschirmWinkel(k.x - z.wx, k.y - z.wy);
                if (a.grund.isEmpty()) a.grund = "Weg zu " + zielName + " (" + Math.round(rest) + " Subtiles), Knoten " + (wegKnoten + 1) + "/" + weg.size();

                if (z.steht()) {
                    if (stehtSeit == 0) stehtSeit = now;
                    if (now - stehtSeit > NEU_DRUECKEN_NACH_MS) {
                        loslassenBis = now + LOSLASSEN_MS;
                        stehtSeit = 0;
                        neuGedrueckt++;
                        navigation = "Figur steht trotz Joystick (auf dem Weg): loslassen und neu drücken";
                    }
                }
                else stehtSeit = 0;
                return;
            }
        }
        else weg = null;


        if (gehenSeit == 0) {
            gehenSeit = now;
            spur.clear();
            stehtSeit = 0;
        }
        spur.addLast(new double[]{now, z.wx, z.wy});
        while (spur.size() > 2 && now - spur.peekFirst()[0] > STECKT_MS + 400) spur.removeFirst();
        double[] alt = spur.peekFirst();
        boolean genugZeit = now - alt[0] >= STECKT_MS && now - gehenSeit >= STECKT_MS;
        double bewegt = Math.hypot(z.wx - alt[1], z.wy - alt[2]);



        if (z.steht()) {
            if (stehtSeit == 0) stehtSeit = now;
            if (now - stehtSeit > NEU_DRUECKEN_NACH_MS) {
                loslassenBis = now + LOSLASSEN_MS;
                stehtSeit = 0;
                neuGedrueckt++;
                navigation = "Figur steht trotz Joystick: loslassen und neu drücken";
            }
        }
        else stehtSeit = 0;






        boolean steckt = genugZeit && bewegt < STEHT_SUBTILES;
        double tdx = dx / Math.max(rest, 0.001), tdy = dy / Math.max(rest, 0.001);
        boolean probe = wandSeite != 0 && now < probeBis;
        if (wandSeite == 0) {
            if (steckt) {
                hindernisse++;

                boolean weiter = now - wandFreiAm < 5000 && letzteSeite != 0;
                wandSeite = weiter ? letzteSeite : ersteSeite(z, ziele, tdx, tdy);
                if (!weiter) {
                    wandSeit = now;
                    wandWechsel = 0;
                    trefferRest = rest;
                }
                probeAm = now + FOLGEN_MS;
                spur.clear();
                gehenSeit = now;
                navigation = "Hindernis: an der Wand entlang " + (wandSeite > 0 ? "rechts" : "links") + " (" + Math.round(rest) + " Subtiles vor "
                    + zielName + ")";
            }
        }
        else if (probe) {

            if (rest < probeRest - 1.2 && rest < trefferRest - 3) {
                letzteSeite = wandSeite;
                wandFreiAm = now;
                wandSeite = 0;
                navigation = "frei: weiter zu " + zielName + " (" + Math.round(rest) + " Subtiles)";
            }
        }
        else if (now >= probeAm) {
            probeBis = now + PROBE_MS;
            probeAm = probeBis + FOLGEN_MS;
            probeRest = rest;
            probe = true;
        }
        else if (steckt) {
            wandWechsel++;
            hindernisse++;
            spur.clear();
            gehenSeit = now;
            if (wandWechsel >= 3 || now - wandSeit > WAND_MAX_MS) {
                ausweichFehlschlaege++;
                navigation = "festgefahren vor " + zielName + ": anderes Ziel";
                zielAufgegeben(now);
                waehleZiel(z, ziele, now);
                if (keinZiel(a)) return;
                dx = zielX - z.wx;
                dy = zielY - z.wy;
                rest = Math.hypot(dx, dy);
                tdx = dx / Math.max(rest, 0.001);
                tdy = dy / Math.max(rest, 0.001);
                winkel = bildschirmWinkel(dx, dy);
            }
            else {
                wandSeite = -wandSeite;
                probeAm = now + FOLGEN_MS;
                navigation = "Ecke: an der Wand entlang " + (wandSeite > 0 ? "rechts" : "links");
            }
        }
        if (wandSeite != 0 && !probe) {
            double r = Math.toRadians(WAND_GRAD) * wandSeite, c = Math.cos(r), sn = Math.sin(r);
            winkel = bildschirmWinkel(tdx * c - tdy * sn, tdx * sn + tdy * c);
        }

        double step = Math.toRadians(3);
        a.richtung = Math.round(winkel / step) * step;
        a.grund = "Ziel " + zielName + " (" + Math.round(rest) + " Subtiles" + (wandSeite != 0 ? (probe ? ", Probe" : ", Wand " + wandSeite) : "") + ")";
    }









    private boolean tueren(Zustand z, long now, Absicht a) {
        return tueren(z, now, a, weg, wegKnoten, weg != null && now - wegFortschrittAm > TUER_FEST_MS);
    }


    private boolean tueren(Zustand z, long now, Absicht a, java.util.List<Wegsuche.Punkt> w, int knoten, boolean fest) {


        if (tuer.aktiv() && (letzterKampfAm > tuer.seit() || letztesNehmen > tuer.seit() || now - tuerAufrufAm > TUER_UNTERBROCHEN_MS)) {
            tuer.abbrechen();
            tuerenUnterbrochen++;
        }
        tuerAufrufAm = now;
        if (!tuer.aktiv()) {
            if (now < tuerPauseBis || karte == null || karte.level != z.gebiet) return false;
            Zustand.Ding d = Tueren.imWeg(z, karte, w, knoten, tuer, fest);
            if (d == null || !tuer.starten(d.id, z.gebiet, now)) return false;
            double[] p = Tueren.welt(z, d);
            tuerX = p[0];
            tuerY = p[1];
            navigation = "Tür " + d.id + " bei " + Math.round(tuerX) + "," + Math.round(tuerY) + " im Weg: öffnen";
        }
        tuer.ohneDurchgaenge = gebietLeeren;
        long id = tuer.id();
        ObjektBenutzen.Stand s = tuer.schritt(z, a, now);
        switch (s) {
            case HIN:
            case GETIPPT:

                wegFortschrittAm = now;
                gehenSeit = 0;
                spur.clear();
                stehtSeit = 0;
                if (a.grund.isEmpty()) a.grund = s == ObjektBenutzen.Stand.HIN ? "Tür " + id + ": hin" : "Tür " + id + ": öffnen, warten";
                return true;
            case ERFOLG:
                tuerenGeoeffnet++;
                navigation = "Tür " + id + " offen (" + tuerenGeoeffnet + ")";
                wegFortschrittAm = now;
                neuplanungen = 0;

                ignoriert.clear();
                uebersprungen = -1;
                return false;
            case GESPERRT:
                tuerenGesperrt++;
                sperren(z);
                navigation = "Tür " + id + " geht nach " + ObjektBenutzen.VERSUCHE + " Versuchen nicht auf: gesperrt, neuer Weg";
                weg = null;
                neuplanungen = 0;
                tuerPauseBis = now + TUER_PAUSE_MS;
                return false;
            case FEHLSCHLAG:
                navigation = "Tür " + id + ": Versuch " + tuer.fehlversuche(id) + " ohne Erfolg";
                tuerPauseBis = now + TUER_PAUSE_MS;
                return false;
            default:
                return false;
        }
    }


    public final ObjektBenutzen tuer = new ObjektBenutzen();
    public int tuerenGeoeffnet, tuerenGesperrt, tuerenUnterbrochen;
    private long tuerPauseBis, tuerAufrufAm;




    static final long TUER_UNTERBROCHEN_MS = 700;
    private double tuerX, tuerY;

    static final long TUER_FEST_MS = 800;

    static final long TUER_PAUSE_MS = 1000;







    private void sperren(Zustand z) {
        java.util.List<int[]> o = Tueren.oeffnung(karte, tuerX, tuerY, z.wx, z.wy);
        boolean rueckfall = o.isEmpty();
        if (rueckfall) {
            int cx = (int) Math.floor(tuerX), cy = (int) Math.floor(tuerY);
            for (int y = cy - 3; y <= cy + 3; y++) for (int x = cx - 3; x <= cx + 3; x++) o.add(new int[]{x, y});
        }
        int fx = (int) Math.floor(z.wx), fy = (int) Math.floor(z.wy), frei = rueckfall ? 1 : 0;
        for (int[] p : o) if (Math.max(Math.abs(p[0] - fx), Math.abs(p[1] - fy)) > frei) karte.sperren(p[0], p[1]);
    }


    private void zielAufgegeben(long now) {
        schlechteZiele.put(zielName, now + SCHLECHT_MS);
        zieleGewechselt++;
        if (zielBlock && abdeckung != null) abdeckung.fehlschlag();
    }





    private boolean keinZiel(Absicht a) {
        if (!Double.isNaN(zielX)) return false;
        a.richtung = Double.NaN;
        a.grund = "kein sicheres Ziel: stehen";
        gehenSeit = 0;
        wandSeite = 0;
        return true;
    }





    private int ersteSeite(Zustand z, Zustand.Ziele ziele, double tdx, double tdy) {
        double ox = Double.NaN, oy = Double.NaN, best = Double.MAX_VALUE;
        for (Zustand.Ziele.Ausgang e : ziele.ausgaenge) {
            if (name(e).equals(zielName) || e.art != Zustand.Ziele.PASSAGE || Math.hypot(e.x + e.outX * 6 - zielX, e.y + e.outY * 6 - zielY) > 150) continue;
            boolean gleich = false;
            for (Zustand.Ziele.Ausgang t : ziele.ausgaenge) if (name(t).equals(zielName) && t.nach == e.nach) gleich = true;
            double d = Math.hypot(e.x - z.wx, e.y - z.wy);
            if (gleich && d < best) {
                best = d;
                ox = e.x;
                oy = e.y;
            }
        }
        if (Double.isNaN(ox)) {
            ox = ziele.box[0] + ziele.box[2] / 2.0;
            oy = ziele.box[1] + ziele.box[3] / 2.0;
        }
        double cross = tdx * (oy - z.wy) - tdy * (ox - z.wx);
        return cross >= 0 ? 1 : -1;
    }

    private static long zelle(double x, double y) {
        return ((long) Math.floor(x / ZELLE) << 32) ^ ((long) Math.floor(y / ZELLE) & 0xFFFFFFFFL);
    }

    private static boolean istStadt(int gebiet) {
        return gebiet == 1 || gebiet == 40 || gebiet == 75 || gebiet == 103 || gebiet == 109;
    }





    private void waehleZiel(Zustand z, Zustand.Ziele ziele, long now) {
        Zustand.Ziele.Ausgang best = null;
        double bestScore = Double.MAX_VALUE;


        int seitenweg = Gebiete.eingangVon(zielGebiet) != 0 || Gebiete.eingangVon(z.gebiet) != 0
            || Gebiete.weiter(zielGebiet, z.gebiet) ? Gebiete.naechstesZu(z.gebiet, zielGebiet) : 0;
        if (!Double.isNaN(vorgabeX)) {
            vorgabeNehmen(z, now);
            return;
        }
        for (Zustand.Ziele.Ausgang e : ziele.ausgaenge) {
            Long bis = schlechteZiele.get(name(e));
            if (bis != null && now < bis) continue;
            double d = Math.hypot(e.x - z.wx, e.y - z.wy);
            double score;
            if (gebietLeeren) {

                if (e.art != Zustand.Ziele.WAYPOINT || besucht.contains(zelle(e.x, e.y))) continue;
                score = d;
            }
            else if (z.stadt) {
                if (e.art != Zustand.Ziele.PASSAGE || istStadt(e.nach)) continue;
                score = d;
            }
            else if ((e.art == Zustand.Ziele.PASSAGE || e.art == Zustand.Ziele.ENTRANCE) && e.nach == zielGebiet && e.nach != z.gebiet) {

                score = d - 20_000;
            }
            else if ((e.art == Zustand.Ziele.PASSAGE || e.art == Zustand.Ziele.ENTRANCE) && seitenweg != 0 && e.nach == seitenweg
                && !istStadt(e.nach)) {
                score = d - 15_000;
            }
            else if ((e.art == Zustand.Ziele.PASSAGE || e.art == Zustand.Ziele.ENTRANCE) && !istStadt(e.nach)
                && e.nach == Gebiete.naechstes(z.gebiet) && Gebiete.stufe(e.nach) <= Gebiete.stufe(zielGebiet)) {

                score = d - 10_000;
            }
            else if (e.art == Zustand.Ziele.PASSAGE && !istStadt(e.nach) && Gebiete.stufe(z.gebiet) < 0 && e.nach > z.gebiet && e.nach <= zielGebiet) {
                score = d - 5_000;
            }
            else if (e.art == Zustand.Ziele.WAYPOINT && !besucht.contains(zelle(e.x, e.y))) score = d;
            else continue;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        zielBlock = false;
        int[] block = best == null && gebietLeeren && abdeckung != null && abdeckung.gebiet == z.gebiet && z.weltlage()
            ? abdeckung.waehle(z.wx, z.wy, random, now) : null;
        if (best != null) {
            zielName = name(best);


            zielX = best.x + best.outX * 6;
            zielY = best.y + best.outY * 6;
            zielEingang = best.art == Zustand.Ziele.ENTRANCE;
        }
        else if (block != null) {

            zielX = block[0] + 0.5;
            zielY = block[1] + 0.5;
            zielEingang = false;
            zielBlock = true;
            zielName = "Block " + block[0] + "," + block[1];
        }
        else {

            int[] box = ziele.box;
            double bx = Double.NaN, by = Double.NaN, bestFrei = -1;
            for (int i = 0; i < 24 && box[2] > 0 && box[3] > 0; i++) {
                double x = box[0] + 8 + random.nextDouble() * Math.max(1, box[2] - 16);
                double y = box[1] + 8 + random.nextDouble() * Math.max(1, box[3] - 16);
                if (Math.hypot(x - z.wx, y - z.wy) < 25) continue;
                boolean nahStadt = false;
                for (Zustand.Ziele.Ausgang e : ziele.ausgaenge) {

                    if (!gebietLeeren && istStadt(e.nach) && Math.hypot(e.x - x, e.y - y) < 40) nahStadt = true;
                }
                if (nahStadt) continue;
                if (gebietLeeren && nahAusgang(ziele, x, y)) continue;
                double frei = besucht.contains(zelle(x, y)) ? 0 : 1;
                double score = frei * 1000 + Math.min(150, Math.hypot(x - z.wx, y - z.wy)) - random.nextDouble() * 30;
                if (score > bestFrei) {
                    bestFrei = score;
                    bx = x;
                    by = y;
                }
            }
            if (Double.isNaN(bx)) {
                double r = random.nextDouble() * 2 * Math.PI;
                bx = z.wx + Math.cos(r) * 40;
                by = z.wy + Math.sin(r) * 40;


                if (gebietLeeren && (nahAusgang(ziele, bx, by) || !imGebiet(ziele, bx, by))) bx = by = Double.NaN;
            }
            zielX = bx;
            zielY = by;
            zielEingang = false;
            zielName = "Erkunden " + Math.round(bx) + "," + Math.round(by);
        }
        zielSeit = now;
        zielMaxMs = ZIEL_MAX_MS;
        eingangTipps = 0;
        eingangErsatz = false;
        besterAbstand = Math.hypot(zielX - z.wx, zielY - z.wy);
        besterAbstandAm = now;
        wandSeite = letzteSeite = 0;
        navigation = "neues Ziel: " + zielName + " (" + Math.round(besterAbstand) + " Subtiles)";
    }


    private void vorgabeNehmen(Zustand z, long now) {
        zielX = vorgabeX;
        zielY = vorgabeY;
        zielEingang = vorgabeEingang;
        zielBlock = false;
        zielName = vorgabeName;
        zielSeit = now;
        zielMaxMs = ZIEL_MAX_MS;
        eingangTipps = 0;
        eingangErsatz = false;
        besterAbstand = Math.hypot(zielX - z.wx, zielY - z.wy);
        besterAbstandAm = now;
        wandSeite = letzteSeite = 0;
        navigation = "Vorgabe: " + zielName + " (" + Math.round(besterAbstand) + " Subtiles)";
    }


    static boolean imGebiet(Zustand.Ziele ziele, double x, double y) {
        int[] b = ziele.box;
        return b[2] > 0 && b[3] > 0 && x >= b[0] && y >= b[1] && x < b[0] + b[2] && y < b[1] + b[3];
    }


    public static boolean nahAusgang(Zustand.Ziele ziele, double x, double y) {
        for (Zustand.Ziele.Ausgang e : ziele.ausgaenge) {
            if (e.art == Zustand.Ziele.WAYPOINT) continue;
            double dx = Math.max(0, Math.max(e.x0 - x, x - e.x1)), dy = Math.max(0, Math.max(e.y0 - y, y - e.y1));
            if (Math.hypot(dx, dy) < AUSGANG_ABSTAND) return true;
        }
        return false;
    }

    private static String name(Zustand.Ziele.Ausgang e) {
        return e.toString();
    }
}
