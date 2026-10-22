package com.winlator.d2.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Karte {
    public static final class Raum {
        public final int nr, x, y, w, h;
        public final boolean geladen;
        byte[] bits;
        int zeilen;
        boolean[] daRows;

        public Raum(int nr, int x, int y, int w, int h, boolean geladen) {
            this.nr = nr; this.x = x; this.y = y; this.w = w; this.h = h; this.geladen = geladen;
        }

        public boolean vollstaendig() { return bits != null && zeilen == h; }

        public boolean angefangen() { return bits != null; }

        int zeilenBytes() { return (w + 7) / 8; }
    }

    public final int level;
    private final Map<Integer, Raum> raeume = new HashMap<>();
    private int gesamt = -1;

    public Karte(int level) { this.level = level; }

    public void raumliste(int gesamt, List<Raum> teil) {
        this.gesamt = gesamt;
        for (Raum r : teil) {
            Raum old = raeume.get(r.nr);
            if (old == null || old.x != r.x || old.y != r.y || old.w != r.w || old.h != r.h) raeume.put(r.nr, r);
        }
    }

    public void raumTeil(int nr, int x, int y, int w, int h, int ersteZeile, int zeilen, byte[] daten, int offset) {
        if (w <= 0 || h <= 0 || w > 4096 || h > 4096 || ersteZeile < 0 || zeilen < 0) return;
        Raum r = raeume.get(nr);
        if (r == null || r.x != x || r.y != y || r.w != w || r.h != h) {
            r = new Raum(nr, x, y, w, h, true);
            raeume.put(nr, r);
        }
        if (r.bits == null) {
            r.bits = new byte[r.zeilenBytes() * h];
            r.daRows = new boolean[h];
        }
        int rb = r.zeilenBytes();
        for (int i = 0; i < zeilen && ersteZeile + i < h; i++) {
            if (offset + (i + 1) * rb > daten.length) break;
            System.arraycopy(daten, offset + i * rb, r.bits, (ersteZeile + i) * rb, rb);
            if (!r.daRows[ersteZeile + i]) {
                r.daRows[ersteZeile + i] = true;
                r.zeilen++;
            }
        }
    }

    public void raumAusText(int nr, int x, int y, String[] zeilen) {
        int h = zeilen.length, w = zeilen[0].length();
        Raum r = new Raum(nr, x, y, w, h, true);
        raeume.put(nr, r);
        int rb = r.zeilenBytes();
        r.bits = new byte[rb * h];
        r.daRows = new boolean[h];
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                if (zeilen[yy].charAt(xx) == '.') r.bits[yy * rb + xx / 8] |= (byte) (1 << (xx % 8));
            }
            r.daRows[yy] = true;
        }
        r.zeilen = h;
        gesamt = raeume.size();
    }

    public static final int FREI = 1, WAND = 0, UNBEKANNT = -1;

    private final java.util.Set<Long> gesperrt = new java.util.HashSet<>();

    public int feld(int x, int y) {
        Raum r = raum(x, y);
        if (r == null || !r.vollstaendig()) return UNBEKANNT;
        int lx = x - r.x, ly = y - r.y;
        if ((r.bits[ly * r.zeilenBytes() + lx / 8] >> (lx % 8) & 1) == 0) return WAND;
        return gesperrt.isEmpty() || !gesperrt.contains(((long) x << 32) | (y & 0xFFFFFFFFL)) ? FREI : WAND;
    }

    public void sperren(int x, int y) {
        gesperrt.add(((long) x << 32) | (y & 0xFFFFFFFFL));
    }

    public boolean engstelle(int x, int y, int weit) {
        return engAchse(x, y, weit) != 0;
    }

    public int engAchse(int x, int y, int weit) {
        if (feld(x, y) != FREI) return 0;
        int bx = breite(x, y, 1, 0, weit), by = breite(x, y, 0, 1, weit);
        if (bx == 0 && by == 0) return 0;
        if (by == 0) return 1;
        if (bx == 0) return 2;
        return bx <= by ? 1 : 2;
    }

    private int breite(int x, int y, int dx, int dy, int weit) {
        int a = wandAbstand(x, y, -dx, -dy, weit), b = wandAbstand(x, y, dx, dy, weit);
        return a == 0 || b == 0 ? 0 : a + b - 1;
    }

    private int wandAbstand(int x, int y, int dx, int dy, int weit) {
        for (int i = 1; i <= weit; i++) if (feld(x + i * dx, y + i * dy) == WAND) return i;
        return 0;
    }

    public int gesperrtAnzahl() { return gesperrt.size(); }

    public boolean begehbar(int x, int y) { return feld(x, y) == FREI; }

    static boolean frei(Raum r, int x, int y) {
        int lx = x - r.x, ly = y - r.y;
        return (r.bits[ly * r.zeilenBytes() + lx / 8] >> (lx % 8) & 1) != 0;
    }

    public java.util.Collection<Raum> alleRaeume() { return raeume.values(); }

    public Raum raum(int x, int y) {
        for (Raum r : raeume.values()) {
            if (x >= r.x && y >= r.y && x < r.x + r.w && y < r.y + r.h) return r;
        }
        return null;
    }

    public Raum raum(int nr) { return raeume.get(nr); }

    public int anzahlRaeume() { return raeume.size(); }

    public int raeumeGesamt() { return gesamt; }

    public List<Raum> fehlendeRaeume(int x, int y) {
        List<Raum> out = new ArrayList<>();
        for (Raum r : raeume.values()) if (!r.vollstaendig()) out.add(r);
        out.sort((a, b) -> Long.compare(abstand2(a, x, y), abstand2(b, x, y)));
        return out;
    }

    public boolean korridorGeladen(int sx, int sy, int zx, int zy, int rand) {
        if (gesamt < 0 || raeume.size() < gesamt) return false;
        double len = Math.hypot(zx - sx, zy - sy);
        int steps = Math.max(1, (int) (len / 10));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            int x = (int) Math.round(sx + t * (zx - sx)), y = (int) Math.round(sy + t * (zy - sy));
            for (int dy = -rand; dy <= rand; dy += Math.max(1, rand)) {
                for (int dx = -rand; dx <= rand; dx += Math.max(1, rand)) {
                    Raum r = raum(x + dx, y + dy);
                    if (r != null && !r.vollstaendig()) return false;
                }
            }
        }
        return true;
    }

    private static long abstand2(Raum r, int x, int y) {
        long dx = (r.x + r.w / 2) - x, dy = (r.y + r.h / 2) - y;
        return dx * dx + dy * dy;
    }

    public List<Integer> fehlendeTeile(Raum r, int teilBytes) {
        List<Integer> out = new ArrayList<>();
        if (r.bits == null) { out.add(0); return out; }
        int rb = r.zeilenBytes(), rowsPerPart = Math.max(1, teilBytes / rb);
        int parts = (r.h + rowsPerPart - 1) / rowsPerPart;
        for (int p = 0; p < parts; p++) {
            boolean missing = false;
            for (int y = p * rowsPerPart; y < Math.min(r.h, (p + 1) * rowsPerPart); y++) if (!r.daRows[y]) missing = true;
            if (missing) out.add(p);
        }
        return out;
    }
}
