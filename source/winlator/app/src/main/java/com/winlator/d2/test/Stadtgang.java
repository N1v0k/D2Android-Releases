package com.winlator.d2.test;










public final class Stadtgang {
    public enum Schritt { AUS, PORTAL_OEFFNEN, INS_PORTAL, ZUM_HEILER, REDEN, DIALOG_ZU, ZUR_TRUHE, TRUHE, ZUM_PORTAL, ZURUECK }


    public int minHeil = 2;

    public long alleMs;




    public long pauseNachGangMs = 180_000;

    public boolean mitTruhe = true;
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
                    return weiterIn(a, mitTruhe ? Schritt.ZUR_TRUHE : Schritt.ZUM_PORTAL, now, "Dialog zu, Leben " + z.leben + "/" + z.lebenMax);
                }
                if (now - letzterTipp > 1500) {
                    letzterTipp = now;
                    a.tippen = "menu_esc";
                    a.grund = "Stadtgang: Dialog schließen";
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
        return schritt == Schritt.REDEN || schritt == Schritt.DIALOG_ZU;
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
