package com.winlator.d2.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class GenutzteObjekte {
    static final int ART_TRUHE = 1, ART_SCHREIN = 2;
    static final long PICKUP_MS = 2500, USED_OBJECT_MS = 3000, WEG_MS = 250, NEUES_SPIEL_MS = 3000;
    static final double WEG_GRAD = 50;

    private final Map<Long, Long> bis = new HashMap<>();
    private long id = -1, seit, letzterZustand;
    private int art = -1;
    private double richtung;

    public synchronized void begonnen(long id, int art, double richtung, long now) {
        this.id = id;
        this.art = art;
        this.richtung = richtung;
        seit = now;
    }

    public synchronized void abgebrochen() {
        id = -1;
    }

    public synchronized void joystick(double richtung, long now) {
        if (id < 0 || now - seit <= WEG_MS) return;
        if (Math.cos(richtung - this.richtung) < Math.cos(Math.toRadians(WEG_GRAD))) beenden(now);
    }

    public synchronized void zustand(Zustand z, long now) {
        if (letzterZustand != 0 && now - letzterZustand > NEUES_SPIEL_MS) {
            bis.clear();
            id = -1;
        }
        letzterZustand = now;
        if (id < 0) return;
        boolean da = false;
        for (Zustand.Ding d : z.objekte) if (d.id == id) da = true;
        for (Zustand.Ding d : z.items) if (d.id == id) da = true;
        for (Zustand.Ding d : z.monster) if (d.id == id) da = true;
        if (!da || z.panel || now - seit > PICKUP_MS) beenden(now);
    }

    public synchronized Set<Long> ausgelassen(long now) {
        if (id >= 0 && now - seit > PICKUP_MS) beenden(now);
        Set<Long> out = new HashSet<>();
        for (Iterator<Map.Entry<Long, Long>> it = bis.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, Long> e = it.next();
            if (now >= e.getValue()) it.remove();
            else out.add(e.getKey());
        }
        return Collections.unmodifiableSet(out);
    }

    private void beenden(long now) {
        if (art == ART_TRUHE) bis.put(id, Long.MAX_VALUE);
        if (art == ART_SCHREIN) bis.put(id, now + USED_OBJECT_MS);
        id = -1;
    }
}
