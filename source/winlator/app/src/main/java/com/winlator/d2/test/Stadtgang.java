package com.winlator.d2.test;










public final class Stadtgang {
    public enum Schritt {
        AUS, PORTAL_OEFFNEN, INS_PORTAL, ZUM_HEILER, REDEN, DIALOG_ZU, ZUM_SCHMIED, REDEN_SCHMIED, LADEN_OEFFNEN, REPARIEREN,
        LADEN_ZU, ZUM_HAENDLER, REDEN_HAENDLER, VERKAUFEN, EINKAUFEN, ZUR_TRUHE, TRUHE, EINLAGERN, TRUHE_ZU, ZUM_PORTAL, ZURUECK
    }







    public static final class Einkauf {
        public final int heil, mana, heilGuertel, manaGuertel, rollen, portaleVorher;
        public final boolean foliant;
        public final long minGold;

        public Einkauf(int heil, int mana, int heilGuertel, int manaGuertel, int rollen, int portaleVorher, long minGold) {
            this(heil, mana, heilGuertel, manaGuertel, false, rollen, portaleVorher, minGold);
        }

        public Einkauf(int heil, int mana, int heilGuertel, int manaGuertel, boolean foliant, int rollen, int portaleVorher, long minGold) {
            this.foliant = foliant;
            this.heil = Math.max(0, heil);
            this.mana = Math.max(0, mana);
            this.heilGuertel = Math.max(0, heilGuertel);
            this.manaGuertel = Math.max(0, manaGuertel);
            this.rollen = Math.max(0, rollen);
            this.portaleVorher = portaleVorher;
            this.minGold = Math.max(0, minGold);
        }

        public boolean traenke() {
            return heil > 0 || mana > 0 || heilGuertel > 0 || manaGuertel > 0;
        }

        public boolean nichts() {
            return !traenke() && !foliant && rollen == 0;
        }

        @Override public String toString() {
            return "Heil " + heil + " (Gürtel " + heilGuertel + "), Mana " + mana + " (Gürtel " + manaGuertel + "), "
                + (foliant ? "Foliant, " : "") + "Rollen " + rollen + ", Min Gold " + minGold;
        }
    }


    public interface Laden {
        Einkauf bedarf(Zustand z, Zustand.Figur f);
    }


    public static final int FOLIANT_MAX = 20;

    public static final int ROLLEN_OHNE_FOLIANT = 5;






    public static int rollenBedarf(int foliant, int rollen, int freieZellen) {
        if (foliant >= 0) return Math.max(0, FOLIANT_MAX - foliant);
        return Math.max(0, Math.min(ROLLEN_OHNE_FOLIANT - Math.max(0, rollen), Math.max(0, freieZellen)));
    }







    public static Einkauf bedarf(Zustand z, int[] spalten, int reserveHeil, int reserveMana, long minGold) {
        if (z.guertel.length < 16 || z.guertelReihen < 0 || z.heilInventar < 0 || z.manaInventar < 0 || z.freieZellen() < 0) return null;
        int frei = z.freieZellen();
        com.winlator.d2.D2PotionShop.Need n = com.winlator.d2.D2PotionShop.need(z.guertel, z.guertelReihen, spalten, z.heilInventar,
            z.manaInventar, reserveHeil, reserveMana, frei);


        int zellenDanach = frei - (n.heal + n.mana) + (n.healBelt + n.manaBelt);

        boolean foliant = z.foliant < 0 && zellenDanach >= 2;
        int rollen = foliant ? FOLIANT_MAX : rollenBedarf(z.foliant, z.rollen, zellenDanach);
        return new Einkauf(n.heal, n.mana, n.healBelt, n.manaBelt, foliant, rollen, z.portale, minGold);
    }


    public int minHeil = 2;

    public long alleMs;




    public long pauseNachGangMs = 180_000;

    public boolean mitTruhe = true;





    public boolean mitSchmied;

    public int reparaturen;
    public String reparatur = "";
    public boolean rotUebrig;

    private long auftrag;
    static final int AUFTRAG_FERTIG = 2;

    static final int ENDE_FERTIG = 0, ENDE_GOLD = 1, ENDE_LADEN_ZU = 4, ENDE_ZEIT = 8, ENDE_NICHT_VERFUEGBAR = 9, ENDE_TEILWEISE = 11;

    static final int ENDE_KEINE_WARE = 2, ENDE_ABGELEHNT = 3, ENDE_INVENTAR_VOLL = 10;

    static final long REPARIEREN_MAX_MS = 20_000;
    public int gaenge, abbrueche;





    public Laden laden;
    public int einkaeufe, gekauftHeil, gekauftMana, gekauftRollen;

    public int summeHeil, summeMana, summeRollen;
    public String einkauf = "";
    public boolean ohneHeiltrank, ohneRollen, gekauftFoliant;




    public boolean traenkePflicht;





    public java.util.Map<Long, Beuteliste.Antwort> beute;

    public int verkauft, eingelagert;




    public static final long GOLD_RESERVE = 5000;
    private long goldAuftrag, goldVorher = -1;
    public long goldEingelagert;
    public boolean truheVoll, vollPflicht;
    public String abgabe = "";
    static final int LISTE_MAX = 40;

    private boolean verkaufFertig, einlagern;
    private long[] liste = new long[0];

    static final int BEFEHL_VERKAUFEN = 17, BEFEHL_TRUHE = 18, ENDE_TRUHE_VOLL = 6;

    static final int LA_ID = 0, LA_CMD = 1, LA_STATE = 2, LA_REASON = 3, LA_A = 4, LA_B = 5;

    static final int BEFEHL_EINKAUF = 16;

    private Einkauf bestellt;
    private int einkaufTeil;

    private int rollenAuftrag;
    private long auftragAm;

    private boolean beimHaendler;

    public String meldung = "", abbruch;

    public int lebenVorher = -1, lebenNachher = -1;

    static final int PORTAL = 5;
    private static final long PORTAL_WARTEN_MS = 6000, SCHRITT_MAX_MS = 45_000, TIPP_ABSTAND_MS = 2500;
    private static final double NAH_SUBTILES = 4;

    private Schritt schritt = Schritt.AUS;
    private long seit, letzterTipp, letzterGang;
    private int versuche;
    private boolean truheOffen, portalGetippt;

    private double letzteX = Double.NaN, letzteY = Double.NaN;

    private double ankunftX = Double.NaN, ankunftY = Double.NaN;
    private long bewegtAm, ausweichenBis;
    private int ausweichSeite = 1;

    public boolean aktiv() {
        return schritt != Schritt.AUS;
    }

    public Schritt schritt() {
        return schritt;
    }


    public boolean faellig(Zustand z, long now) {
        if (aktiv() || z.stadt || !z.weltlage()) return false;
        for (Zustand.Ding d : z.feinde()) if (z.bodenAbstand(d) < 600) return false;
        if (letzterGang == 0) letzterGang = now - pauseNachGangMs;
        if (alleMs > 0 && now - letzterGang >= alleMs) return true;
        return z.lebenMax > 0 && z.heilGuertel < minHeil && now - letzterGang >= pauseNachGangMs;
    }

    public void starten(long now) {
        weiter(Schritt.PORTAL_OEFFNEN, now, "Stadtgang: Portal öffnen");
        auftrag = 0;
        rotUebrig = false;
        ohneHeiltrank = ohneRollen = traenkePflicht = gekauftFoliant = false;
        truheVoll = vollPflicht = verkaufFertig = einlagern = false;
        goldAuftrag = 0;
        abgabe = "";
        gekauftHeil = gekauftMana = gekauftRollen = 0;
        einkauf = "";
        bestellt = null;
        beimHaendler = false;
        einkaufTeil = 0;
        reparatur = "";
        abbruch = null;
        truheOffen = portalGetippt = false;
        ankunftX = ankunftY = Double.NaN;

        letzterGang = now;
    }


    public void beenden() {
        schritt = Schritt.AUS;
        portalGetippt = false;
    }


    public void beobachten(Zustand z) {
        if (schritt == Schritt.TRUHE && z.panel && !z.npc) truheOffen = true;
    }

    public Gehirn.Absicht denken(Zustand z, Zustand.Figur f, long now) {
        Gehirn.Absicht a = new Gehirn.Absicht();
        a.grund = "Stadtgang: " + schritt;
        if (schritt != Schritt.AUS && now - seit > grenzeMs()) return abbrechen(a, "Zeit um bei " + schritt, now);
        switch (schritt) {
            case PORTAL_OEFFNEN: {
                if (z.stadt) return weiterIn(a, Schritt.ZUM_HEILER, now, "schon in der Stadt");
                if (portal(z) != null) return weiterIn(a, Schritt.INS_PORTAL, now, "Portal offen");
                if (versuche == 0 || now - letzterTipp > PORTAL_WARTEN_MS) {
                    if (versuche >= 2) return abbrechen(a, "kein Portal nach zwei Tipps (Rollen?)", now);
                    versuche++;
                    letzterTipp = now;
                    a.tippen = "menu_portal";
                    a.grund = "Stadtgang: Portal öffnen (Versuch " + versuche + ")";
                }
                return a;
            }
            case INS_PORTAL: {
                if (z.stadt) {
                    lebenVorher = z.leben;
                    ankunftX = z.wx;
                    ankunftY = z.wy;
                    return weiterIn(a, Schritt.ZUM_HEILER, now, "in der Stadt, Leben " + z.leben + "/" + z.lebenMax);
                }
                return zumPortal(z, f, a, now, "ins Portal");
            }
            case ZUM_HEILER: {
                Zustand.Figur.Ort o = ort(f, Zustand.Figur.ORT_PERSON, heiler(z.gebiet));
                if (o == null) return abbrechen(a, "kein Heiler (txt " + heiler(z.gebiet) + ") in den Orten", now);
                if (Math.hypot(o.x - z.wx, o.y - z.wy) < NAH_SUBTILES) return weiterIn(a, Schritt.REDEN, now, "beim Heiler");
                gehe(z, o.x, o.y, now, a, "zum Heiler " + o.txt + (o.geladen() ? "" : " (Startplatz)"));
                return a;
            }
            case REDEN: {

                if (z.npc || z.panel) {
                    lebenNachher = z.leben;
                    return weiterIn(a, Schritt.DIALOG_ZU, now, "Dialog offen, Leben " + z.leben + "/" + z.lebenMax);
                }
                if (now - letzterTipp > TIPP_ABSTAND_MS) {
                    if (versuche >= 3) return abbrechen(a, "Heiler antwortet nicht", now);
                    versuche++;
                    letzterTipp = now;
                    a.tippen = "interact";
                    a.grund = "Stadtgang: mit dem Heiler reden";
                }
                return a;
            }
            case DIALOG_ZU: {
                if (!z.npc && !z.panel) {
                    if (lebenNachher < z.leben) lebenNachher = z.leben;
                    Schritt naechster = mitSchmied && z.gelbOderRot() ? Schritt.ZUM_SCHMIED : nachDemLaden(z, f);
                    return weiterIn(a, naechster, now, "Dialog zu, Leben " + z.leben + "/" + z.lebenMax);
                }
                if (now - letzterTipp > 1500) {
                    letzterTipp = now;
                    a.tippen = "menu_esc";
                    a.grund = "Stadtgang: Dialog schließen";
                }
                return a;
            }
            case ZUM_SCHMIED: {
                int txt = schmied(z.gebiet);
                Zustand.Figur.Ort o = txt < 0 ? null : ort(f, Zustand.Figur.ORT_PERSON, txt);
                if (o == null) return ohneReparatur(a, "kein Schmied in den Orten" + (txt < 0 ? "" : " (txt " + txt + ")"), now);
                if (Math.hypot(o.x - z.wx, o.y - z.wy) < NAH_SUBTILES) return weiterIn(a, Schritt.REDEN_SCHMIED, now, "beim Schmied");
                gehe(z, o.x, o.y, now, a, "zum Schmied " + o.txt + (o.geladen() ? "" : " (Startplatz)"));
                return a;
            }
            case REDEN_SCHMIED: {
                if (z.offen(Zustand.UI_LADEN)) return weiterIn(a, Schritt.REPARIEREN, now, "Laden offen");
                if (z.menueZeile(Zustand.MENUE_HANDELN_REPARIEREN) >= 0) return weiterIn(a, Schritt.LADEN_OEFFNEN, now, "Menü offen");
                if (now - letzterTipp > TIPP_ABSTAND_MS) {
                    letzterTipp = now;
                    if (z.npc) {

                        a.tippen = "menu_esc";
                        a.grund = "Stadtgang: Begrüßung des Schmieds überspringen";
                        return a;
                    }
                    if (versuche >= 3) return ohneReparatur(a, "Schmied antwortet nicht", now);
                    versuche++;
                    a.tippen = "interact";
                    a.grund = "Stadtgang: mit dem Schmied reden";
                }
                return a;
            }
            case LADEN_OEFFNEN: {
                if (z.offen(Zustand.UI_LADEN)) return weiterIn(a, beimHaendler ? nachLadenOffen(f) : Schritt.REPARIEREN, now, "Laden offen");
                int zeile = beimHaendler ? handelsZeile(z) : z.menueZeile(Zustand.MENUE_HANDELN_REPARIEREN);
                if (zeile < 0) {
                    return beimHaendler ? ohneEinkauf(a, "keine Menüzeile Handeln", now) : ohneReparatur(a, "keine Menüzeile Handeln/Reparieren", now);
                }
                if (now - letzterTipp > TIPP_ABSTAND_MS) {
                    if (versuche >= 3) return beimHaendler ? ohneEinkauf(a, "Laden geht nicht auf", now) : ohneReparatur(a, "Laden geht nicht auf", now);
                    versuche++;
                    letzterTipp = now;
                    a.modAuftrag = "npc " + zeile;
                    a.grund = "Stadtgang: Handeln/Reparieren (Zeile " + zeile + ")";
                }
                return a;
            }
            case REPARIEREN: {
                long[] r = z.reparatur;
                if (auftrag != 0 && r != null && r[0] == auftrag && r[1] == AUFTRAG_FERTIG) {
                    reparaturen++;
                    rotUebrig = r[4] != 0;
                    reparatur = grundText((int) r[2]) + ", " + r[3] + " Teile" + (rotUebrig ? ", kaputtes Teil übrig" : "");
                    return weiterIn(a, Schritt.LADEN_ZU, now, "Reparieren: " + reparatur);
                }
                if (auftrag != 0 && now - seit > REPARIEREN_MAX_MS) return ohneReparatur(a, "keine Antwort der Mod", now);
                if (auftrag == 0 || (now - letzterTipp > TIPP_ABSTAND_MS && (r == null || r[0] != auftrag))) {
                    if (versuche >= 3) return ohneReparatur(a, "Auftrag kommt nicht an", now);
                    if (auftrag == 0) auftrag = (now % 1_000_000_000L) + 1;
                    versuche++;
                    letzterTipp = now;
                    a.modAuftrag = "reparieren " + auftrag;
                    a.grund = "Stadtgang: reparieren (Auftrag " + auftrag + ")";
                }
                return a;
            }
            case LADEN_ZU: {
                if (!z.npc && !z.panel && !z.offen(Zustand.UI_LADEN) && !z.offen(Zustand.UI_NPC_MENUE)) {
                    return weiterIn(a, beimHaendler ? nachDemSchmied() : nachDemLaden(z, f), now, "Laden zu");
                }
                if (now - letzterTipp > 1500) {
                    letzterTipp = now;
                    a.tippen = "menu_esc";
                    a.grund = "Stadtgang: Laden schließen";
                }
                return a;
            }
            case ZUM_HAENDLER: {
                int txt = haendler(z.gebiet);
                Zustand.Figur.Ort o = txt < 0 ? null : ort(f, Zustand.Figur.ORT_PERSON, txt);
                if (o == null) return ohneEinkauf(a, "kein Händler in den Orten" + (txt < 0 ? "" : " (txt " + txt + ")"), now);
                if (Math.hypot(o.x - z.wx, o.y - z.wy) < NAH_SUBTILES) return weiterIn(a, Schritt.REDEN_HAENDLER, now, "beim Händler");
                gehe(z, o.x, o.y, now, a, "zum Händler " + o.txt + (o.geladen() ? "" : " (Startplatz)"));
                return a;
            }
            case REDEN_HAENDLER: {
                if (z.offen(Zustand.UI_LADEN)) return weiterIn(a, nachLadenOffen(f), now, "Laden offen");
                if (handelsZeile(z) >= 0) return weiterIn(a, Schritt.LADEN_OEFFNEN, now, "Menü offen");
                if (now - letzterTipp > TIPP_ABSTAND_MS) {
                    letzterTipp = now;
                    if (z.npc) {

                        a.tippen = "menu_esc";
                        a.grund = "Stadtgang: Begrüßung des Händlers überspringen";
                        return a;
                    }
                    if (versuche >= 3) return ohneEinkauf(a, "Händler antwortet nicht", now);
                    versuche++;
                    a.tippen = "interact";
                    a.grund = "Stadtgang: mit dem Händler reden";
                }
                return a;
            }
            case VERKAUFEN:
                return abgeben(z, a, now, true);
            case EINKAUFEN:
                return einkaufen(z, a, now);
            case ZUR_TRUHE: {
                Zustand.Figur.Ort o = ort(f, Zustand.Figur.ORT_TRUHE, -1);
                if (o == null) return abbrechen(a, "keine Truhe in den Orten", now);
                if (Math.hypot(o.x - z.wx, o.y - z.wy) < NAH_SUBTILES) return weiterIn(a, Schritt.TRUHE, now, "bei der Truhe");
                gehe(z, o.x, o.y, now, a, "zur Truhe");
                return a;
            }
            case TRUHE: {

                if (einlagern && z.offen(UI_TRUHE)) {
                    liste = abgabeListe(beute, f, false);
                    goldAuftrag = goldUeber(z, f);
                    return weiterIn(a, Schritt.EINLAGERN, now, "Truhe offen, " + liste.length + " Teile einlagern");
                }

                if (!einlagern && truheOffen && !z.panel) return weiterIn(a, Schritt.ZUM_PORTAL, now, "Truhe geöffnet und geschlossen");
                if ((einlagern || !truheOffen) && now - letzterTipp > TIPP_ABSTAND_MS) {
                    if (versuche >= 3) return abbrechen(a, "Truhe geht nicht auf", now);
                    versuche++;
                    letzterTipp = now;
                    a.tippen = "interact";
                    a.grund = "Stadtgang: Truhe öffnen";
                }
                return a;
            }
            case EINLAGERN:
                return abgeben(z, a, now, false);
            case TRUHE_ZU: {
                if (!z.panel && !z.offen(UI_TRUHE)) return weiterIn(a, nachDemLaden(z, f), now, "Truhe zu");
                if (now - letzterTipp > 1500) {
                    if (++versuche > 6) return abbrechen(a, "Truhe geht nicht zu", now);
                    letzterTipp = now;
                    a.tippen = "menu_esc";
                    a.grund = "Stadtgang: Truhe schließen";
                }
                return a;
            }
            case ZUM_PORTAL: {
                if (!z.stadt) return weiterIn(a, Schritt.ZURUECK, now, "zurück draußen");
                return zumPortal(z, f, a, now, "zurück durchs Portal");
            }
            case ZURUECK: {
                gaenge++;
                schritt = Schritt.AUS;
                meldung = "Stadtgang " + gaenge + " fertig (Leben beim Heiler " + lebenVorher + " → " + lebenNachher + ")";
                return a;
            }
            default:
                return a;
        }
    }





    private Schritt nachDemLaden(Zustand z, Zustand.Figur f) {

        if (beute != null && !einlagern && (abgabeListe(beute, f, false).length > 0 || goldUeber(z, f) > 0)) {
            einlagern = true;
            auftrag = 0;
            versuche = 0;
            return Schritt.ZUR_TRUHE;
        }
        if ((laden != null || beute != null) && bestellt == null && !beimHaendler) {


            long zellenVorher = z.zellen;
            if (z.zellen >= 0 && beute != null && f != null) z.zellen &= ~verkaufZellen(beute, f);
            Einkauf e = laden != null ? laden.bedarf(z, f) : null;
            z.zellen = zellenVorher;

            if ((e == null || e.nichts()) && abgabeListe(beute, f, true).length > 0) e = new Einkauf(0, 0, 0, 0, 0, z.portale, 0);
            if (e != null && (!e.nichts() || abgabeListe(beute, f, true).length > 0)) {
                bestellt = e;
                beimHaendler = true;
                einkaufTeil = 0;
                auftrag = 0;
                versuche = 0;
                meldung = "Stadtgang: Einkauf " + e;
                return Schritt.ZUM_HAENDLER;
            }
        }
        if (traenkePflicht && bestellt == null) {
            ohneHeiltrank = true;
            einkauf = "nichts zu kaufen (Daten fehlen)";
        }

        if (laden != null && bestellt == null && z.portale == 0) {
            ohneRollen = true;
            einkauf += (einkauf.isEmpty() ? "" : ", ") + "kein Portal mehr";
        }
        return nachDemSchmied();
    }


    private Schritt nachLadenOffen(Zustand.Figur f) {
        if (!verkaufFertig && beute != null) {
            auftrag = 0;
            liste = abgabeListe(beute, f, true);
            if (liste.length > 0) return Schritt.VERKAUFEN;
        }
        return Schritt.EINKAUFEN;
    }






    private long goldUeber(Zustand z, Zustand.Figur f) {
        if (laden == null || beute == null || z.gold < 0) return 0;
        Einkauf e = laden.bedarf(z, f);
        if (e == null) return 0;
        return Math.max(0, z.gold - (e.minGold + GOLD_RESERVE));
    }


    public static boolean abgabeDa(java.util.Map<Long, Beuteliste.Antwort> beute, Zustand.Figur f) {
        if (beute == null || beute.isEmpty() || f == null) return false;
        return abgabeListe(beute, f, true).length > 0 || abgabeListe(beute, f, false).length > 0;
    }


    static long verkaufZellen(java.util.Map<Long, Beuteliste.Antwort> beute, Zustand.Figur f) {
        long m = 0;
        for (Zustand.Figur.Teil t : f.gegenstaende) {
            if (t.platz != Zustand.Figur.PLATZ_INVENTAR || beute.get(t.id) != Beuteliste.Antwort.VERKAUFEN) continue;
            for (int y = t.y; y < t.y + Math.max(1, t.hoehe) && y < 4; y++)
                for (int x = t.x; x < t.x + Math.max(1, t.breite) && x < 10; x++)
                    if (x >= 0 && y >= 0) m |= 1L << (y * 10 + x);
        }
        return m;
    }





    public static long[] abgabeListe(java.util.Map<Long, Beuteliste.Antwort> beute, Zustand.Figur f, boolean verkaufen) {
        if (beute == null || f == null) return new long[0];
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (Zustand.Figur.Teil t : f.gegenstaende) {
            if (t.platz != Zustand.Figur.PLATZ_INVENTAR || ids.size() >= LISTE_MAX) continue;
            Beuteliste.Antwort an = beute.get(t.id);
            if (an == null || an == Beuteliste.Antwort.LIEGEN) continue;
            if ((an == Beuteliste.Antwort.VERKAUFEN) == verkaufen) ids.add(t.id);
        }
        long[] r = new long[ids.size()];
        for (int i = 0; i < r.length; i++) r[i] = ids.get(i);
        return r;
    }





    private Gehirn.Absicht abgeben(Zustand z, Gehirn.Absicht a, long now, boolean verkaufen) {
        Schritt danach = verkaufen ? Schritt.EINKAUFEN : Schritt.TRUHE_ZU;
        int befehl = verkaufen ? BEFEHL_VERKAUFEN : BEFEHL_TRUHE;
        if (liste.length == 0 && (verkaufen || goldAuftrag <= 0)) {
            if (verkaufen) verkaufFertig = true;
            return weiterIn(a, danach, now, (verkaufen ? "nichts zu verkaufen" : "nichts einzulagern"));
        }
        long[] r = z.ladenAuftrag;
        if (auftrag != 0 && r != null && r[LA_ID] == auftrag && r[LA_CMD] == befehl && r[LA_STATE] == AUFTRAG_FERTIG) {
            int grund = (int) r[LA_REASON];

            long goldWeg = !verkaufen && goldAuftrag > 0 && goldVorher >= 0 && z.gold >= 0 ? Math.max(0, goldVorher - z.gold) : 0;
            String text = (verkaufen ? "verkauft " : "eingelagert ") + r[LA_A] + " von " + liste.length
                + (!verkaufen && goldAuftrag > 0 ? ", Gold " + goldWeg + " von " + goldAuftrag : "") + " (" + ladenGrund(grund) + ")";
            goldEingelagert += goldWeg;


            if (grund != ENDE_TRUHE_VOLL) for (long id : liste) beute.remove(id);
            abgabe += (abgabe.isEmpty() ? "" : ", ") + text;
            if (verkaufen) {
                verkauft += (int) r[LA_A];
                verkaufFertig = true;
            }
            else {
                eingelagert += (int) r[LA_A];
                if (grund == ENDE_TRUHE_VOLL) truheVoll = true;

                if (grund == ENDE_ABGELEHNT) {
                    auftrag = 0;
                    return abbrechen(a, "Einlagern abgelehnt, Teil auf dem Zeiger? (" + text + ")", now);
                }
            }
            auftrag = 0;
            return weiterIn(a, danach, now, (verkaufen ? "Verkaufen: " : "Einlagern: ") + text);
        }
        if (auftrag != 0 && now - auftragAm > abgabeFrist(verkaufen)) {
            abgabe += (abgabe.isEmpty() ? "" : ", ") + (verkaufen ? "Verkaufen" : "Einlagern") + ": keine Antwort der Mod";
            if (verkaufen) verkaufFertig = true;
            auftrag = 0;
            return weiterIn(a, danach, now, abgabe);
        }
        boolean angekommen = r != null && r[LA_ID] == auftrag;
        if (auftrag == 0 || (!angekommen && now - letzterTipp > TIPP_ABSTAND_MS)) {
            if (versuche >= 3) {
                if (verkaufen) verkaufFertig = true;
                auftrag = 0;
                abgabe += (abgabe.isEmpty() ? "" : ", ") + (verkaufen ? "Verkaufen" : "Einlagern") + ": Auftrag kommt nicht an";
                return weiterIn(a, danach, now, abgabe);
            }
            if (auftrag == 0) {
                auftrag = (now % 1_000_000_000L) + 7;
                auftragAm = now;
                goldVorher = z.gold;
            }
            versuche++;
            letzterTipp = now;
            StringBuilder ids = new StringBuilder();
            for (long id : liste) ids.append(ids.length() == 0 ? "" : ",").append(id);

            a.modAuftrag = verkaufen ? "verkaufen " + auftrag + " " + ids
                : "truhe " + auftrag + " " + Math.max(0, goldAuftrag) + " " + (ids.length() == 0 ? "-" : ids);
            a.grund = "Stadtgang: " + (verkaufen ? "Beute verkaufen" : "Beute einlagern") + " (Auftrag " + auftrag + ", " + liste.length + " Teile)";
        }
        return a;
    }


    static int handelsZeile(Zustand z) {
        int zeile = z.menueZeile(Zustand.MENUE_HANDELN);
        return zeile >= 0 ? zeile : z.menueZeile(Zustand.MENUE_HANDELN_REPARIEREN);
    }


    private long grenzeMs() {

        if (schritt == Schritt.VERKAUFEN || schritt == Schritt.EINLAGERN) return 10_000 + abgabeFrist(schritt == Schritt.VERKAUFEN);
        if (schritt != Schritt.EINKAUFEN || bestellt == null) return SCHRITT_MAX_MS;
        return 10_000 + fristMs(traenkeTeile(bestellt)) + (bestellt.foliant ? fristMs(1) : 0) + fristMs(bestellt.rollen);
    }


    static int traenkeTeile(Einkauf e) {
        return e.heil + e.mana + e.heilGuertel + e.manaGuertel;
    }


    static long fristMs(int teile) {
        return 3000 + 1300L * Math.max(0, teile);
    }






    private Gehirn.Absicht einkaufen(Zustand z, Gehirn.Absicht a, long now) {
        if (bestellt == null) return ohneEinkauf(a, "nichts bestellt", now);
        long[] r = z.ladenAuftrag;
        if (auftrag != 0 && r != null && r[LA_ID] == auftrag && r[LA_CMD] == BEFEHL_EINKAUF && r[LA_STATE] == AUFTRAG_FERTIG) {
            int grund = (int) r[LA_REASON];
            if (einkaufTeil == 1) {
                einkauf += (einkauf.isEmpty() ? "" : ", ") + "Foliant " + r[LA_A] + " (" + ladenGrund(grund) + ")";
                gekauftFoliant = r[LA_A] > 0;
            }
            else if (einkaufTeil == 0) {
                gekauftHeil = (int) r[LA_A];
                gekauftMana = (int) r[LA_B];
                summeHeil += gekauftHeil;
                summeMana += gekauftMana;
                einkauf = "Tränke " + gekauftHeil + " Heil, " + gekauftMana + " Mana (" + ladenGrund(grund) + ")";
                if (bestellt.heil > 0 && gekauftHeil == 0) ohneHeiltrank = true;
            }
            else {
                gekauftRollen = (int) r[LA_A];
                summeRollen += gekauftRollen;
                einkauf += (einkauf.isEmpty() ? "" : ", ") + "Rollen " + gekauftRollen + " (" + ladenGrund(grund) + ")";
                if (bestellt.portaleVorher <= 0 && gekauftRollen == 0 && !gekauftFoliant) ohneRollen = true;
            }
            auftrag = 0;
            einkaufTeil++;
            versuche = 0;
            letzterTipp = 0;
        }
        if (einkaufTeil == 0 && !bestellt.traenke()) einkaufTeil = 1;
        if (einkaufTeil == 1 && !bestellt.foliant) einkaufTeil = 2;
        if (einkaufTeil == 2 && auftrag == 0 && versuche == 0) {

            rollenAuftrag = bestellt.foliant && !gekauftFoliant ? Math.min(bestellt.rollen, rollenBedarf(-1, z.rollen, z.freieZellen()))
                : bestellt.rollen;
        }
        if (einkaufTeil == 2 && rollenAuftrag == 0) einkaufTeil = 3;


        if (einkaufTeil >= 1 && einkaufTeil < 3 && !verkauftRollen(z.gebiet)) {
            einkauf += (einkauf.isEmpty() ? "" : ", ") + "Rollen: hier nicht";
            if (bestellt.portaleVorher <= 0) ohneRollen = true;
            einkaufTeil = 3;
        }
        if (einkaufTeil >= 3) {

            if (bestellt.portaleVorher <= 0 && gekauftRollen == 0 && !gekauftFoliant) ohneRollen = true;


            if (traenkePflicht && gekauftHeil == 0) ohneHeiltrank = true;
            einkaeufe++;
            return weiterIn(a, Schritt.LADEN_ZU, now, "Einkauf: " + einkauf);
        }
        boolean angekommen = r != null && r[LA_ID] == auftrag;
        int teile = einkaufTeil == 0 ? traenkeTeile(bestellt) : einkaufTeil == 1 ? 1 : rollenAuftrag;
        if (auftrag != 0 && now - auftragAm > fristMs(teile)) {
            return einkaufAbbruch(a, "keine Antwort der Mod", now);
        }
        if (auftrag == 0 || (!angekommen && now - letzterTipp > TIPP_ABSTAND_MS)) {
            if (versuche >= 3) return einkaufAbbruch(a, "Auftrag kommt nicht an", now);
            if (auftrag == 0) {
                auftrag = (now % 1_000_000_000L) + 1 + einkaufTeil;
                auftragAm = now;
            }
            versuche++;
            letzterTipp = now;
            Einkauf e = bestellt;
            a.modAuftrag = einkaufTeil == 0
                ? "traenke " + auftrag + " " + e.heil + " " + e.mana + " " + e.heilGuertel + " " + e.manaGuertel + " " + e.minGold
                : einkaufTeil == 1 ? "foliant " + auftrag + " " + e.minGold : "rollen " + auftrag + " " + rollenAuftrag + " " + e.minGold;
            a.grund = "Stadtgang: " + TEIL[einkaufTeil] + " kaufen (Auftrag " + auftrag + ")";
        }
        return a;
    }


    private Gehirn.Absicht einkaufAbbruch(Gehirn.Absicht a, String warum, long now) {
        if ((einkaufTeil == 0 && bestellt.heil > 0) || (traenkePflicht && gekauftHeil == 0)) ohneHeiltrank = true;
        if ((bestellt.rollen > 0 || bestellt.foliant) && bestellt.portaleVorher <= 0 && gekauftRollen == 0 && !gekauftFoliant) ohneRollen = true;
        einkauf += (einkauf.isEmpty() ? "" : ", ") + TEIL[einkaufTeil] + ": " + warum;
        einkaeufe++;
        auftrag = 0;
        einkaufTeil = 3;
        return weiterIn(a, Schritt.LADEN_ZU, now, "Einkauf: " + einkauf);
    }





    private long abgabeFrist(boolean verkaufen) {
        return verkaufen ? fristMs(liste.length) : fristMs(2 * liste.length) + 31 * 600L;
    }


    static final int UI_TRUHE = 0x19;


    private static final String[] TEIL = {"Tränke", "Foliant", "Rollen"};


    private Gehirn.Absicht ohneEinkauf(Gehirn.Absicht a, String warum, long now) {
        einkaeufe++;
        einkauf += (einkauf.isEmpty() ? "" : ", ") + "nicht eingekauft: " + warum;
        if ((bestellt != null && bestellt.heil > 0 && einkaufTeil == 0) || (traenkePflicht && gekauftHeil == 0)) ohneHeiltrank = true;
        if (bestellt != null && (bestellt.rollen > 0 || bestellt.foliant) && bestellt.portaleVorher <= 0) ohneRollen = true;
        einkaufTeil = 3;
        return weiterIn(a, Schritt.LADEN_ZU, now, "Einkauf: " + einkauf);
    }


    static String ladenGrund(int grund) {
        switch (grund) {
            case ENDE_FERTIG: return "fertig";
            case ENDE_GOLD: return "Gold-Limit erreicht";
            case ENDE_KEINE_WARE: return "keine Ware";
            case ENDE_ABGELEHNT: return "abgelehnt";
            case ENDE_LADEN_ZU: return "Laden zu";
            case ENDE_ZEIT: return "Zeit um";
            case ENDE_NICHT_VERFUEGBAR: return "nicht verfügbar";
            case ENDE_INVENTAR_VOLL: return "Inventar voll";
            case ENDE_TRUHE_VOLL: return "Truhe voll";
            default: return "Grund " + grund;
        }
    }


    static int haendler(int gebiet) {
        switch (gebiet) {
            case 1: return 148;
            case 40: return 202;
            case 75: return 255;
            case 103: return 405;
            case 109: return 513;
            default: return -1;
        }
    }


    static boolean verkauftRollen(int gebiet) {
        return gebiet != 40;
    }


    private Schritt nachDemSchmied() {
        return mitTruhe ? Schritt.ZUR_TRUHE : Schritt.ZUM_PORTAL;
    }





    private Gehirn.Absicht ohneReparatur(Gehirn.Absicht a, String warum, long now) {
        reparaturen++;
        reparatur = "nicht repariert: " + warum;
        rotUebrig = true;
        return weiterIn(a, Schritt.LADEN_ZU, now, "Reparieren: " + reparatur);
    }


    static String grundText(int grund) {
        switch (grund) {
            case ENDE_FERTIG: return "alles repariert";
            case ENDE_TEILWEISE: return "teilweise (Gold)";
            case ENDE_GOLD: return "nichts bezahlbar";
            case ENDE_LADEN_ZU: return "Laden zu";
            case ENDE_ZEIT: return "Zeit um";
            case ENDE_NICHT_VERFUEGBAR: return "nicht verfügbar";
            default: return "Grund " + grund;
        }
    }


    static int schmied(int gebiet) {
        switch (gebiet) {
            case 1: return 154;
            case 40: return 178;
            default: return -1;
        }
    }


    private Gehirn.Absicht zumPortal(Zustand z, Zustand.Figur f, Gehirn.Absicht a, long now, String was) {
        Zustand.Ding p = portal(z);
        if (p == null) {
            if (!z.stadt) return abbrechen(a, "Portal nicht mehr da", now);


            if (!Double.isNaN(ankunftX)) {
                gehe(z, ankunftX, ankunftY, now, a, "Portal suchen (zum Ankunftsort)");
                return a;
            }
            Zustand.Figur.Ort w = ort(f, Zustand.Figur.ORT_WEGPUNKT, -1);
            if (w != null) gehe(z, w.x, w.y, now, a, "Portal suchen (zum Wegpunkt)");
            return a;
        }
        double d = z.abstand(p);
        if (d > 120) {
            a.richtung = Math.atan2(p.y - z.py, p.x - z.px);
            a.grund = "Stadtgang: " + was + " (" + Math.round(d) + " px)";
            return a;
        }
        if (now - letzterTipp > TIPP_ABSTAND_MS) {
            if (versuche >= 4) return abbrechen(a, "Portal nimmt nicht an", now);
            versuche++;
            letzterTipp = now;
            a.tippen = "interact";
            a.grund = "Stadtgang: " + was;
            portalGetippt = true;
        }
        return a;
    }





    public boolean portalGetippt() {
        return portalGetippt;
    }

    public void portalAngekommen() {
        portalGetippt = false;
    }






    public boolean fensterErlaubt() {
        return schritt == Schritt.REDEN || schritt == Schritt.DIALOG_ZU || schritt == Schritt.REDEN_SCHMIED
            || schritt == Schritt.LADEN_OEFFNEN || schritt == Schritt.REPARIEREN || schritt == Schritt.LADEN_ZU
            || schritt == Schritt.REDEN_HAENDLER || schritt == Schritt.EINKAUFEN || schritt == Schritt.VERKAUFEN
            || (schritt == Schritt.TRUHE && einlagern) || schritt == Schritt.EINLAGERN || schritt == Schritt.TRUHE_ZU;
    }


    static Zustand.Ding portal(Zustand z) {
        Zustand.Ding best = null;
        for (Zustand.Ding d : z.objekte) if (d.art == PORTAL && (best == null || z.abstand(d) < z.abstand(best))) best = d;
        return best;
    }


    static int heiler(int gebiet) {
        switch (gebiet) {
            case 40: return 178;
            case 75: return 255;
            case 103: return 405;
            case 109: return 513;
            default: return 148;
        }
    }


    static Zustand.Figur.Ort ort(Zustand.Figur f, int art, int txt) {
        if (f == null) return null;
        Zustand.Figur.Ort best = null;
        for (Zustand.Figur.Ort o : f.orte) {
            if (o.art != art || (txt >= 0 && o.txt != txt)) continue;
            if (best == null || (o.geladen() && !best.geladen())) best = o;
        }
        return best;
    }


    private void gehe(Zustand z, double x, double y, long now, Gehirn.Absicht a, String was) {
        double dx = x - z.wx, dy = y - z.wy;
        if (Double.isNaN(letzteX) || Math.hypot(z.wx - letzteX, z.wy - letzteY) > 0.6) {
            letzteX = z.wx;
            letzteY = z.wy;
            bewegtAm = now;
        }
        double winkel = Gehirn.bildschirmWinkel(dx, dy);
        if (now - bewegtAm > 1000 && now >= ausweichenBis) {
            ausweichenBis = now + 1500;
            ausweichSeite = -ausweichSeite;
            bewegtAm = now;
        }
        if (now < ausweichenBis) winkel += ausweichSeite * Math.toRadians(80);
        a.richtung = winkel;
        a.grund = "Stadtgang: " + was + " (" + Math.round(Math.hypot(dx, dy)) + " Subtiles)";
    }

    private Gehirn.Absicht weiterIn(Gehirn.Absicht a, Schritt s, long now, String warum) {
        weiter(s, now, "Stadtgang: " + warum + " → " + s);
        return a;
    }

    private void weiter(Schritt s, long now, String text) {
        schritt = s;
        seit = now;
        versuche = 0;
        letzterTipp = 0;
        bewegtAm = now;
        letzteX = letzteY = Double.NaN;
        meldung = text;
    }

    private Gehirn.Absicht abbrechen(Gehirn.Absicht a, String warum, long now) {
        abbruch = warum;
        abbrueche++;
        meldung = "Stadtgang abgebrochen: " + warum;
        schritt = Schritt.AUS;
        letzterGang = now;
        a.richtung = Double.NaN;
        return a;
    }
}
