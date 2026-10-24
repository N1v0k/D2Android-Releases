package com.winlator.d2.test;










public final class Stadtgang {
    public enum Schritt {
        AUS, PORTAL_OEFFNEN, INS_PORTAL, ZUM_HEILER, REDEN, DIALOG_ZU, ZUM_SCHMIED, REDEN_SCHMIED, LADEN_OEFFNEN, REPARIEREN,
        LADEN_ZU, ZUR_TRUHE, TRUHE, ZUM_PORTAL, ZURUECK
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

    static final long REPARIEREN_MAX_MS = 20_000;
    public int gaenge, abbrueche;

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
        if (schritt != Schritt.AUS && now - seit > SCHRITT_MAX_MS) return abbrechen(a, "Zeit um bei " + schritt, now);
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
                    Schritt naechster = mitSchmied && z.gelbOderRot() ? Schritt.ZUM_SCHMIED : nachDemSchmied();
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
                if (z.offen(Zustand.UI_LADEN)) return weiterIn(a, Schritt.REPARIEREN, now, "Laden offen");
                int zeile = z.menueZeile(Zustand.MENUE_HANDELN_REPARIEREN);
                if (zeile < 0) return ohneReparatur(a, "keine Menüzeile Handeln/Reparieren", now);
                if (now - letzterTipp > TIPP_ABSTAND_MS) {
                    if (versuche >= 3) return ohneReparatur(a, "Laden geht nicht auf", now);
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
                    return weiterIn(a, nachDemSchmied(), now, "Laden zu");
                }
                if (now - letzterTipp > 1500) {
                    letzterTipp = now;
                    a.tippen = "menu_esc";
                    a.grund = "Stadtgang: Laden schließen";
                }
                return a;
            }
            case ZUR_TRUHE: {
                Zustand.Figur.Ort o = ort(f, Zustand.Figur.ORT_TRUHE, -1);
                if (o == null) return abbrechen(a, "keine Truhe in den Orten", now);
                if (Math.hypot(o.x - z.wx, o.y - z.wy) < NAH_SUBTILES) return weiterIn(a, Schritt.TRUHE, now, "bei der Truhe");
                gehe(z, o.x, o.y, now, a, "zur Truhe");
                return a;
            }
            case TRUHE: {

                if (truheOffen && !z.panel) return weiterIn(a, Schritt.ZUM_PORTAL, now, "Truhe geöffnet und geschlossen");
                if (!truheOffen && now - letzterTipp > TIPP_ABSTAND_MS) {
                    if (versuche >= 3) return abbrechen(a, "Truhe geht nicht auf", now);
                    versuche++;
                    letzterTipp = now;
                    a.tippen = "interact";
                    a.grund = "Stadtgang: Truhe öffnen";
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
            || schritt == Schritt.LADEN_OEFFNEN || schritt == Schritt.REPARIEREN || schritt == Schritt.LADEN_ZU;
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
