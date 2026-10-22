package com.winlator.d2.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class Abdeckung {
    static final int ZELLE = 20, BLOCK = 4, JE_ZELLE = ZELLE / BLOCK, BLOECKE = JE_ZELLE * JE_ZELLE;
    public static final byte UNGUELTIG = 0, OFFEN = 1, GESEHEN = 2, AUFGEGEBEN = 3;
    static final long NEU_RECHNEN_MS = 2000;
    static final int FEHLSCHLAEGE = 2;
    private static final int KEINER = Integer.MIN_VALUE;

    private static final class Zelle {
        final int cx, cy;
        final int[] vx = new int[BLOECKE], vy = new int[BLOECKE];
        final byte[] stand = new byte[BLOECKE], fehl = new byte[BLOECKE];
        boolean sicher;
        boolean gerechnet;

        Zelle(int cx, int cy) {
            this.cx = cx;
            this.cy = cy;
            java.util.Arrays.fill(vx, KEINER);
            java.util.Arrays.fill(vy, KEINER);
        }
    }

    public final int gebiet;
    final Karte karte;
    private final Zustand.Ziele ziele;
    private final Map<Long, Zelle> zellen = new HashMap<>();
    private final int cx0, cy0, cx1, cy1;
    private long gerechnetAm = Long.MIN_VALUE;
    private boolean alleGerechnet;
    private Zelle zielZelle;
    private int zielBlock = -1;
    public long rechnenMs;

    public Abdeckung(Karte karte, Zustand.Ziele ziele) {
        this.karte = karte;
        this.ziele = ziele;
        this.gebiet = ziele.gebiet;
        int[] b = ziele.box;
        cx0 = Math.floorDiv(b[0], ZELLE);
        cy0 = Math.floorDiv(b[1], ZELLE);
        cx1 = Math.floorDiv(b[0] + Math.max(1, b[2]) - 1, ZELLE);
        cy1 = Math.floorDiv(b[1] + Math.max(1, b[3]) - 1, ZELLE);
    }

    public boolean passt(Zustand.Ziele z) {
        return z.gebiet == gebiet && java.util.Arrays.equals(z.box, ziele.box) && z.ausgaenge.size() == ziele.ausgaenge.size();
    }

    public void vergessen() {
        zellen.clear();
        gerechnetAm = Long.MIN_VALUE;
        alleGerechnet = false;
        zielZelle = null;
        zielBlock = -1;
    }

    public int nachsuchen(java.util.List<double[]> orte, double r) {
        int n = 0;
        for (Zelle c : zellen.values()) {
            for (int b = 0; b < BLOECKE; b++) {
                if (c.stand[b] == AUFGEGEBEN) {
                    c.stand[b] = OFFEN;
                    c.fehl[b] = 0;
                    n++;
                    continue;
                }
                if (c.stand[b] != GESEHEN) continue;
                for (double[] o : orte) {
                    if (Math.hypot(c.vx[b] + 0.5 - o[0], c.vy[b] + 0.5 - o[1]) <= r) {
                        c.stand[b] = OFFEN;
                        n++;
                        break;
                    }
                }
            }
        }
        zielZelle = null;
        zielBlock = -1;
        return n;
    }

    public boolean vollstaendig() {
        return karte != null && karte.level == gebiet && ziele.box[2] > 0 && ziele.box[3] > 0
            && karte.raeumeGesamt() >= 0 && karte.anzahlRaeume() >= karte.raeumeGesamt();
    }

    private static long schluessel(int cx, int cy) {
        return ((long) cx << 32) ^ (cy & 0xFFFFFFFFL);
    }

    private Zelle zelle(int cx, int cy) {
        if (cx < cx0 || cx > cx1 || cy < cy0 || cy > cy1) return null;
        long k = schluessel(cx, cy);
        Zelle c = zellen.get(k);
        if (c == null) {
            c = new Zelle(cx, cy);
            zellen.put(k, c);
        }
        if (!c.gerechnet) rechnen(c);
        return c;
    }

    private void rechnen(Zelle c) {
        int x0 = c.cx * ZELLE, y0 = c.cy * ZELLE;
        java.util.List<Karte.Raum> raeume = new java.util.ArrayList<>();
        boolean sicher = true;
        for (Karte.Raum r : karte.alleRaeume()) {
            if (r.x >= x0 + ZELLE || r.y >= y0 + ZELLE || r.x + r.w <= x0 || r.y + r.h <= y0) continue;
            raeume.add(r);
            if (!r.vollstaendig()) sicher = false;
        }
        int[] box = ziele.box;
        for (int b = 0; b < BLOECKE; b++) {
            int bx = x0 + (b % JE_ZELLE) * BLOCK, by = y0 + (b / JE_ZELLE) * BLOCK;
            double mx = bx + BLOCK / 2.0, my = by + BLOCK / 2.0, best = Double.MAX_VALUE;
            int rx = KEINER, ry = KEINER;
            for (int y = by; y < by + BLOCK; y++) {
                for (int x = bx; x < bx + BLOCK; x++) {
                    if (x < box[0] || y < box[1] || x >= box[0] + box[2] || y >= box[1] + box[3]) continue;
                    if (!zaehlt(raeume, x, y)) continue;
                    double d = Math.hypot(x + 0.5 - mx, y + 0.5 - my);
                    if (d < best) {
                        best = d;
                        rx = x;
                        ry = y;
                    }
                }
            }
            c.vx[b] = rx;
            c.vy[b] = ry;
            if (rx == KEINER) c.stand[b] = UNGUELTIG;
            else if (c.stand[b] == UNGUELTIG) c.stand[b] = OFFEN;
        }
        c.sicher = sicher;
        c.gerechnet = true;
    }

    private boolean zaehlt(java.util.List<Karte.Raum> raeume, int x, int y) {
        Karte.Raum in = null;
        for (Karte.Raum r : raeume) {
            if (x >= r.x && y >= r.y && x < r.x + r.w && y < r.y + r.h) {
                in = r;
                break;
            }
        }
        if (in == null) return false;
        if (in.vollstaendig() && !Karte.frei(in, x, y)) return false;
        return !Gehirn.nahAusgang(ziele, x + 0.5, y + 0.5);
    }

    private void aktualisieren(long now) {
        if (!vollstaendig()) return;
        if (alleGerechnet && now - gerechnetAm < NEU_RECHNEN_MS) return;
        long t0 = System.nanoTime();
        for (int cy = cy0; cy <= cy1; cy++) {
            for (int cx = cx0; cx <= cx1; cx++) {
                Zelle c = zellen.get(schluessel(cx, cy));
                if (c == null || !c.gerechnet) zelle(cx, cy);
                else if (!c.sicher) rechnen(c);
            }
        }
        alleGerechnet = true;
        gerechnetAm = now;
        rechnenMs = Math.max(rechnenMs, (System.nanoTime() - t0) / 1_000_000);
    }

    public void sehen(double wx, double wy, double r) {
        if (!vollstaendig()) return;
        int ax = (int) Math.floor((wx - r) / ZELLE), bx = (int) Math.floor((wx + r) / ZELLE);
        int ay = (int) Math.floor((wy - r) / ZELLE), by = (int) Math.floor((wy + r) / ZELLE);
        for (int cy = ay; cy <= by; cy++) {
            for (int cx = ax; cx <= bx; cx++) {
                Zelle c = zelle(cx, cy);
                if (c == null) continue;
                for (int b = 0; b < BLOECKE; b++) {
                    if (c.stand[b] != OFFEN) continue;
                    if (Math.hypot(c.vx[b] + 0.5 - wx, c.vy[b] + 0.5 - wy) <= r) c.stand[b] = GESEHEN;
                }
            }
        }
    }

    public int[] waehle(double wx, double wy, Random random, long now) {
        aktualisieren(now);
        zielZelle = null;
        zielBlock = -1;
        if (!vollstaendig()) return null;
        Zelle best = null;
        double bestD = Double.MAX_VALUE;
        for (Zelle c : zellen.values()) {
            if (!hatOffene(c)) continue;
            double d = Math.hypot(c.cx * ZELLE + ZELLE / 2.0 - wx, c.cy * ZELLE + ZELLE / 2.0 - wy) + random.nextDouble() * 0.01;
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        if (best == null) return null;
        double mx = best.cx * ZELLE + ZELLE / 2.0, my = best.cy * ZELLE + ZELLE / 2.0, nah = Double.MAX_VALUE;
        for (int b = 0; b < BLOECKE; b++) {
            if (best.stand[b] != OFFEN) continue;
            double d = Math.hypot(best.vx[b] + 0.5 - mx, best.vy[b] + 0.5 - my);
            if (d < nah) {
                nah = d;
                zielBlock = b;
            }
        }
        zielZelle = best;
        return new int[]{best.vx[zielBlock], best.vy[zielBlock]};
    }

    private static boolean hatOffene(Zelle c) {
        for (byte s : c.stand) if (s == OFFEN) return true;
        return false;
    }

    public boolean zielErledigt() {
        return zielZelle != null && zielZelle.stand[zielBlock] != OFFEN;
    }

    public void fehlschlag() {
        if (zielZelle == null || zielZelle.stand[zielBlock] != OFFEN) return;
        if (++zielZelle.fehl[zielBlock] >= FEHLSCHLAEGE) zielZelle.stand[zielBlock] = AUFGEGEBEN;
        zielZelle = null;
        zielBlock = -1;
    }

    public int offen(long now) {
        aktualisieren(now);
        return alleGerechnet ? zaehle(OFFEN) : -1;
    }

    public int gesehen() {
        return zaehle(GESEHEN);
    }

    public int aufgegeben() {
        return zaehle(AUFGEGEBEN);
    }

    private int zaehle(byte stand) {
        int n = 0;
        for (Zelle c : zellen.values()) for (byte s : c.stand) if (s == stand) n++;
        return n;
    }

    public int stand(int x, int y) {
        Zelle c = zelle(Math.floorDiv(x, ZELLE), Math.floorDiv(y, ZELLE));
        if (c == null) return -1;
        int b = (Math.floorMod(y, ZELLE) / BLOCK) * JE_ZELLE + Math.floorMod(x, ZELLE) / BLOCK;
        return c.stand[b];
    }

    public int[] vertreter(int x, int y) {
        Zelle c = zelle(Math.floorDiv(x, ZELLE), Math.floorDiv(y, ZELLE));
        if (c == null) return null;
        int b = (Math.floorMod(y, ZELLE) / BLOCK) * JE_ZELLE + Math.floorMod(x, ZELLE) / BLOCK;
        return c.vx[b] == KEINER ? null : new int[]{c.vx[b], c.vy[b]};
    }

    public java.util.List<int[]> alleVertreter(long now) {
        aktualisieren(now);
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (Zelle c : zellen.values()) {
            for (int b = 0; b < BLOECKE; b++) if (c.vx[b] != KEINER) out.add(new int[]{c.vx[b], c.vy[b], c.stand[b]});
        }
        return out;
    }
}
