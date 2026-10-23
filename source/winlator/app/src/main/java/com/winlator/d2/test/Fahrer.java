package com.winlator.d2.test;







public final class Fahrer {

    public interface Protokoll {
        void event(String art, String text);
    }


    public static final class Verhalten {

        public boolean todWeiter = true;

        public boolean pauseBeenden = true;

        public boolean fensterSchliessen = true;






        public boolean fensterDurchlassen;
    }

    public static final int F_JOYSTICK = 0, F_ANGRIFF = 1, F_TIPP = 2;

    private long joystickAm;
    private boolean joystickBestaetigt;
    private static final long TIPP_MS = 80;

    private final Finger finger;
    private final Protokoll protokoll;
    public final Verhalten verhalten = new Verhalten();
    private Gehirn.Absicht laeuft;
    private double joystickRichtung = Double.NaN;
    private long tippHochAm, letzterEsc, letzteAbsichtLog;
    private String letzterGrund = "";

    public int tipps, nehmenTipps;

    static final String LEISTE = "menu_toggle";




    public boolean leisteAufgeklappt;
    private long leisteGetipptAm = -10_000, leisteLayoutMs = -1;

    private String nachholen, nachholenWarum;
    private long nachholenBis;
    private boolean unerreichbar;

    static final long NACHHOLEN_MS = 5000;

    static final long LEISTE_WARTEN_MS = 3000;

    public Fahrer(Finger finger, Protokoll protokoll) {
        this.finger = finger;
        this.protokoll = protokoll;
    }


    public Gehirn.Absicht laeuft() {
        return laeuft;
    }


    public void takt(long now) {
        if (tippHochAm > 0 && now >= tippHochAm) {
            finger.up(F_TIPP);
            tippHochAm = 0;
        }
    }







    public boolean vorbereiten(Zustand z, Zustand.Layout l, long now, long totSeit) {
        if (z.tot()) {
            loslassen("tot");

            if (verhalten.todWeiter && now - totSeit > 3000 && now - letzterEsc > 6000) {
                letzterEsc = now;
                tippen(l.knoepfe.containsKey("menu_esc") ? "menu_esc" : "hidden0", "Esc auf dem Todesbildschirm", l, now);
            }
            return false;
        }
        if (l.pauseGewollt || z.pause) {
            loslassen("pausiert");
            if (verhalten.pauseBeenden && now - letzterEsc > 3000) {
                letzterEsc = now;
                tippenBei(l.w / 2, l.h / 2, "Pause beenden (Tipp in die Mitte)", now);
            }
            return false;
        }
        if (z.panel && !z.npc) {
            loslassen("Fenster offen " + z.fenster);
            if (verhalten.fensterDurchlassen) return true;
            if (verhalten.fensterSchliessen && now - letzterEsc > 2000) {
                letzterEsc = now;
                tippen("hidden0", "Fenster schließen", l, now);
            }
            return false;
        }
        return true;
    }


    public void anwenden(Gehirn.Absicht a, Zustand.Layout l, long now) {
        laeuft = a;

        String key = a.grund.contains(" (") ? a.grund.substring(0, a.grund.indexOf(" (")) : a.grund;
        if (!key.equals(letzterGrund) && (now - letzteAbsichtLog > 1000 || !a.grund.startsWith("Erkunden"))) {
            letzterGrund = key;
            letzteAbsichtLog = now;
            protokoll.event("absicht", a.grund);
        }


        if (a.tippen != null && a.sperreMs > 0) {
            if (finger.isDown(F_JOYSTICK)) finger.up(F_JOYSTICK);
            if (finger.isDown(F_ANGRIFF)) finger.up(F_ANGRIFF);
            joystickRichtung = Double.NaN;
            gehalten = null;
            a.getippt = tippen(a.tippen, a.grund, l, now);
            if (a.getippt) sperreBis = now + a.sperreMs;
            return;
        }
        if (now < sperreBis) {
            if (finger.isDown(F_JOYSTICK)) finger.up(F_JOYSTICK);
            if (finger.isDown(F_ANGRIFF)) finger.up(F_ANGRIFF);
            joystickRichtung = Double.NaN;
            gehalten = null;
            return;
        }

        if (Double.isNaN(a.richtung)) {
            if (finger.isDown(F_JOYSTICK)) finger.up(F_JOYSTICK);
            joystickRichtung = Double.NaN;
        }
        else {
            int jx = l.joystick[0], jy = l.joystick[1];
            float r = Math.max(60, l.joystick[2]) * 0.9f;
            if (finger.isDown(F_JOYSTICK) && l.ms > joystickAm) {




                if (l.joystickAktiv) joystickBestaetigt = true;
                else if (joystickBestaetigt) {
                    finger.up(F_JOYSTICK);
                    joystickBestaetigt = false;
                    protokoll.event("loslassen", "Joystick in der App nicht mehr gehalten (Touch abgebrochen?): neu drücken");
                }
            }
            if (!finger.isDown(F_JOYSTICK)) {
                finger.down(F_JOYSTICK, jx, jy);
                joystickRichtung = Double.NaN;
                joystickAm = now;
                joystickBestaetigt = false;
            }
            if (a.richtung != joystickRichtung) {
                joystickRichtung = a.richtung;
                finger.move(F_JOYSTICK, jx + (float) Math.cos(a.richtung) * r * 0.5f, jy + (float) Math.sin(a.richtung) * r * 0.5f);
                finger.move(F_JOYSTICK, jx + (float) Math.cos(a.richtung) * r, jy + (float) Math.sin(a.richtung) * r);
            }
        }


        String halten = a.angriff ? haltKnopf(a.halten, l) : null;
        if (a.neuDruecken && finger.isDown(F_ANGRIFF)) {

            finger.up(F_ANGRIFF);
            gehalten = null;
        }
        else if (halten == null) {
            if (finger.isDown(F_ANGRIFF)) finger.up(F_ANGRIFF);
            gehalten = null;
        }
        else if (!finger.isDown(F_ANGRIFF) || !halten.equals(gehalten)) {
            if (finger.isDown(F_ANGRIFF)) finger.up(F_ANGRIFF);
            int[] c = halten.equals(Kampfwahl.ANGRIFF) ? l.mitte("attack", 2180, 756) : l.mitte(halten, 0, 0);
            finger.down(F_ANGRIFF, c[0], c[1]);
            gehalten = halten;
        }

        if (a.tippen != null) a.getippt = tippen(a.tippen, a.grund, l, now);
        else if (a.tippSpiel != null) a.getippt = tippenImSpiel(a.tippSpiel, a.grund, l, now);
        else nachholen(l, now);
    }







    public boolean nachholen(Zustand.Layout l, long now) {
        if (nachholen == null) return false;
        if (now > nachholenBis) {
            protokoll.event("tipp", nachholenWarum + " [" + nachholen + "]: nicht nachgeholt (Zeit um)");
            nachholen = null;
            return false;
        }
        tippen(nachholen, nachholenWarum + " (nachgeholt)", l, now);
        return nachholen != null;
    }


    public boolean nachholenOffen() {
        return nachholen != null;
    }


    private String gehalten;

    private long sperreBis;





    public static String haltKnopf(String wanted, Zustand.Layout l) {
        if (wanted == null || wanted.equals(Kampfwahl.ANGRIFF)) return Kampfwahl.ANGRIFF;
        int[] b = l.knoepfe.get(wanted);
        return b != null && b[0] > 0 && b[1] > 0 && l.sichtbar(wanted) ? wanted : Kampfwahl.ANGRIFF;
    }


    public void loslassen(String warum) {
        if (finger.isDown(F_JOYSTICK) || finger.isDown(F_ANGRIFF)) protokoll.event("loslassen", warum);
        finger.up(F_JOYSTICK);
        finger.up(F_ANGRIFF);
        gehalten = null;
        joystickRichtung = Double.NaN;
        laeuft = null;
    }


    public void allesLoslassen() {
        finger.releaseAll();
        gehalten = null;
        nachholen = null;
        joystickRichtung = Double.NaN;
        tippHochAm = 0;
        laeuft = null;
    }









    public boolean tippen(String button, String warum, Zustand.Layout l, long now) {
        boolean ok = tippenOhneNachholen(button, warum, l, now);



        if (ok) {
            if (button.equals(nachholen)) nachholen = null;
        }
        else if (!unerreichbar && button.startsWith("menu_") && !button.equals(LEISTE) && !button.equals("menu_esc")
                && !button.equals(nachholen)) {
            nachholen = button;
            nachholenWarum = warum;
            nachholenBis = now + NACHHOLEN_MS;
        }
        return ok;
    }

    private boolean tippenOhneNachholen(String button, String warum, Zustand.Layout l, long now) {
        unerreichbar = false;


        if ((button.equals("heal") || button.equals("mana")) && Zustand.Layout.TRAENKE_TASTEN.equals(l.traenke)) {
            protokoll.event("tipp", warum + " [" + button + "]: Tränke über feste Tasten, kein Tipp (Automatik der App)");
            unerreichbar = true;
            return false;
        }
        if (!l.sichtbar(button)) {
            String twin = escZwilling(button);
            if (twin != null && l.sichtbar(twin) && l.knoepfe.containsKey(twin)) button = twin;
            else if (button.startsWith("menu_") && !button.equals(LEISTE) && l.sichtbar(LEISTE) && l.knoepfe.containsKey(LEISTE)) {


                boolean neuesLayout = l.ms == 0 || l.ms != leisteLayoutMs;
                if (now - leisteGetipptAm >= LEISTE_WARTEN_MS && neuesLayout
                        && tippenBei(l.mitte(LEISTE, 0, 0), warum + " [" + button + "]: Leiste aufklappen [" + LEISTE + "]", now)) {
                    leisteGetipptAm = now;
                    leisteLayoutMs = l.ms;
                    leisteAufgeklappt = true;
                }
                return false;
            }
            else {
                protokoll.event("tipp", warum + " [" + button + "]: Knopf unsichtbar, kein Tipp");
                if (button.equals(nachholen)) nachholen = null;
                unerreichbar = true;
                return false;
            }
        }
        int[] fallback = fallback(button);
        boolean ok = tippenBei(l.mitte(button, fallback[0], fallback[1]), warum + " [" + button + "]", now);

        if (ok && button.equals("interact")) nehmenTipps++;
        return ok;
    }

    private boolean tippenBei(int[] c, String warum, long now) {
        return tippenBei(c[0], c[1], warum, now);
    }






    public boolean tippenImSpiel(int[] p, String warum, Zustand.Layout l, long now) {
        if (l.spiel == null || p[2] <= 0 || p[3] <= 0) {
            protokoll.event("tipp", warum + ": kein Spielbild im Layout, Tipp entfällt");
            return false;
        }
        int x = l.spiel[0] + Math.round((float) p[0] * l.spiel[2] / p[2]), y = l.spiel[1] + Math.round((float) p[1] * l.spiel[3] / p[3]);
        return tippenBei(x, y, warum + " (Spiel " + p[0] + "," + p[1] + ")", now);
    }

    public boolean tippenBei(int x, int y, String warum, long now) {
        if (finger.isDown(F_TIPP)) return false;
        tipps++;
        protokoll.event("tipp", warum + " bei " + x + "," + y);
        finger.down(F_TIPP, x, y);
        tippHochAm = now + TIPP_MS;
        return true;
    }


    static String escZwilling(String button) {
        if (button.equals("menu_esc")) return "hidden0";
        if (button.equals("hidden0")) return "menu_esc";
        return null;
    }


    static int[] fallback(String button) {
        switch (button) {
            case "interact": return new int[]{1974, 1000};
            case "heal": return new int[]{1666, 816};
            case "mana": return new int[]{1652, 626};
            case "menu_esc": return new int[]{2030, 86};
            case "hidden0": return new int[]{2396, 86};
            case "menu_portal": return new int[]{2162, 84};
            case "menu_pause": return new int[]{2297, 84};
            default: return new int[]{2180, 756};
        }
    }
}
