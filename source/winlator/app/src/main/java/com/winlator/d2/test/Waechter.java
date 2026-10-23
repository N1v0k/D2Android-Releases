package com.winlator.d2.test;

import java.util.ArrayList;
import java.util.List;









public final class Waechter {
    public static final class Befund {
        public final String art, text;
        public final boolean auffaellig;

        Befund(String art, String text, boolean auffaellig) {
            this.art = art;
            this.text = text;
            this.auffaellig = auffaellig;
        }
    }

    static final long STEHT_MS = 1500, TOT_HAENGT_MS = 25000, FEST_FENSTER_MS = 9000, MELDE_PAUSE_MS = 60000,
        ANGRIFF_MS = 4000;
    static final double BEWEGT_PX = 2.5, SPREIZUNG = Math.toRadians(80);

    private Zustand vorher;
    private long gehenSeit, bewegtAm, totSeit, festAm, gemeldetJoystick = -MELDE_PAUSE_MS, gemeldetAngriff = -MELDE_PAUSE_MS;
    private long joystickSeit, stehtSeit, angriffSeit;

    private double stehtRichtung, stehtSpreizung, stehtX, stehtY;
    private boolean angriffWirkte;
    private double gehenRichtung = Double.NaN;
    private final List<Long> festgefahren = new ArrayList<>();
    private boolean haengtGemeldet;

    public int tode, messbar, unmessbar, joystickOhneWirkung, angriffOhneWirkung;





    public List<Befund> pruefen(Zustand z, Gehirn.Absicht laeuft, long now) {
        List<Befund> out = new ArrayList<>();
        boolean frei = !z.panel && !z.pause && !z.npc && !z.tot();
        boolean joystick = laeuft != null && !Double.isNaN(laeuft.richtung) && !laeuft.angriff && laeuft.tippen == null;
        if (z.weltlage()) {
            messbar++;
            weltlage(z, laeuft, joystick, frei, now, out);
            vorher = z;
        }
        else ohneWeltlage(z, laeuft, joystick, frei, now, out);

        if (z.tot()) {
            if (totSeit == 0) {
                totSeit = now;
                tode++;
                out.add(new Befund("tod", "Figur tot in Gebiet " + z.gebiet + " (Leben 0 von " + z.lebenMax + ", Heiltränke im Gürtel "
                    + z.heilGuertel + ")", true));
            }
            else if (!haengtGemeldet && now - totSeit > TOT_HAENGT_MS) {
                haengtGemeldet = true;
                out.add(new Befund("todesbildschirm_haengt", "Todesbildschirm nach " + (now - totSeit) / 1000
                    + " s und Esc noch da", true));
            }
        }
        else {
            totSeit = 0;
            haengtGemeldet = false;
        }
        return out;
    }


    private void weltlage(Zustand z, Gehirn.Absicht laeuft, boolean joystick, boolean frei, long now, List<Befund> out) {
        if (joystick && frei) {
            if (joystickSeit == 0) joystickSeit = now;
            if (z.steht()) {
                if (stehtSeit == 0) {
                    stehtSeit = now;
                    stehtRichtung = laeuft.richtung;
                    stehtSpreizung = 0;
                    stehtX = z.wx;
                    stehtY = z.wy;
                }
                double d = Math.abs(Math.atan2(Math.sin(laeuft.richtung - stehtRichtung), Math.cos(laeuft.richtung - stehtRichtung)));
                stehtSpreizung = Math.max(stehtSpreizung, d);
                if (Math.hypot(z.wx - stehtX, z.wy - stehtY) > 1) stehtSeit = 0;
            }
            else stehtSeit = 0;
        }
        else joystickSeit = stehtSeit = 0;


        if (stehtSeit > 0 && now - stehtSeit >= STEHT_MS && now - joystickSeit >= STEHT_MS && stehtSpreizung >= SPREIZUNG
                && now - gemeldetJoystick > MELDE_PAUSE_MS) {
            gemeldetJoystick = now;
            joystickOhneWirkung++;
            out.add(new Befund("joystick_ohne_wirkung", "Joystick " + (now - joystickSeit) + " ms gehalten ("
                + Math.round(Math.toDegrees(laeuft.richtung)) + "°, Richtungen über " + Math.round(Math.toDegrees(stehtSpreizung))
                + "° versucht), Spielermodus " + z.modus + " (Stehen) seit " + (now - stehtSeit) + " ms, Lage " + z.wx + "," + z.wy
                + ", Gebiet " + z.gebiet + "; kein Fenster, keine Pause, kein NPC", true));
        }

        boolean angriff = laeuft != null && laeuft.angriff && frei;
        if (angriff) {
            if (angriffSeit == 0) {
                angriffSeit = now;
                angriffWirkte = false;
            }
            if ((z.modus >= 7 && z.modus <= 16) || z.modus == 18 || z.modus == 4) angriffWirkte = true;
            boolean nah = false;
            for (Zustand.Ding d : z.feinde()) if (z.bodenAbstand(d) < 300) nah = true;
            if (!angriffWirkte && nah && now - angriffSeit >= ANGRIFF_MS && now - gemeldetAngriff > MELDE_PAUSE_MS) {
                gemeldetAngriff = now;
                angriffOhneWirkung++;
                out.add(new Befund("angriff_ohne_wirkung", "Angriff " + (now - angriffSeit) + " ms gehalten, Feind in Reichweite, Spielermodus "
                    + z.modus + " (nie Angriff/Fertigkeit)", true));
            }
        }
        else angriffSeit = 0;
    }


    private void ohneWeltlage(Zustand z, Gehirn.Absicht laeuft, boolean walking, boolean frei, long now, List<Befund> out) {
        double moved = z.weltVerschoben(vorher);
        if (Double.isNaN(moved)) unmessbar++;
        else {
            messbar++;
            if (moved > BEWEGT_PX) bewegtAm = now;
        }
        vorher = z;
        if (!walking || laeuft.richtung != gehenRichtung) {
            gehenSeit = walking ? now : 0;
            gehenRichtung = walking ? laeuft.richtung : Double.NaN;
            if (walking) bewegtAm = Math.max(bewegtAm, now - 1);
        }
        if (walking && frei && !Double.isNaN(moved) && gehenSeit > 0 && now - gehenSeit >= STEHT_MS && now - bewegtAm >= STEHT_MS
                && now - festAm >= STEHT_MS) {
            festAm = now;
            festgefahren.add(now);
            while (!festgefahren.isEmpty() && now - festgefahren.get(0) > FEST_FENSTER_MS) festgefahren.remove(0);
            out.add(new Befund("festgefahren", "Joystick " + (now - gehenSeit) + " ms gehalten ("
                + Math.round(Math.toDegrees(laeuft.richtung)) + "°), Welt bewegt sich nicht", false));
            if (festgefahren.size() >= 3 && now - gemeldetJoystick > MELDE_PAUSE_MS) {
                gemeldetJoystick = now;
                joystickOhneWirkung++;
                out.add(new Befund("joystick_ohne_wirkung", "Joystick in " + festgefahren.size() + " Richtungen binnen "
                    + FEST_FENSTER_MS / 1000 + " s ohne Wirkung (kein Fenster, keine Pause)", true));
            }
        }
    }

    public long totSeit() {
        return totSeit;
    }
}
