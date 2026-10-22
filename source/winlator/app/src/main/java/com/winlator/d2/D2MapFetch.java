package com.winlator.d2;

import com.winlator.d2.test.Karte;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public final class D2MapFetch {
    public static final int CMD_MAP = 14, PKT_MAP = 8, TEIL_BYTES = 960, MAX_JE_RUNDE = 40;
    public static final int MAX_SEITE = 1024, MAX_RAEUME = 2048, MAX_BYTES = 8 << 20;
    private long bytes;
    private boolean wanted;

    public interface Sender { void request(int kind, int index, int part); }

    private final Sender sender;
    private Karte karte;
    private int level = -1, gesamt = -1;
    private final TreeMap<Integer, Karte.Raum> raeume = new TreeMap<>();
    private long lastRound;

    public D2MapFetch(Sender sender) { this.sender = sender; }

    public synchronized Karte karte() { return karte; }

    public synchronized void level(int level) {
        if (level == this.level || level <= 0) return;
        this.level = level;
        reset(level);
    }

    public synchronized void reset() { reset(level); }

    private void reset(int level) {
        gesamt = -1;
        raeume.clear();
        bytes = 0;
        karte = level > 0 ? new Karte(level) : null;
    }

    public synchronized boolean tick(long now, boolean wanted) { return tick(now, wanted, Double.NaN, Double.NaN); }

    public synchronized boolean tick(long now, boolean wanted, double wx, double wy) {
        this.wanted = wanted;
        if (!wanted || level <= 0 || now - lastRound < 1000) return false;
        lastRound = now;
        if (gesamt < 0) {
            sender.request(0, 0, 0);
            return true;
        }
        int asked = 0;
        if (raeume.size() < gesamt) {
            sender.request(0, raeume.size(), 0);
            asked++;
        }
        List<Karte.Raum> order = new ArrayList<>(raeume.values());
        if (!Double.isNaN(wx)) {
            order.sort((a, c) -> Double.compare(Math.hypot(a.x + a.w / 2.0 - wx, a.y + a.h / 2.0 - wy), Math.hypot(c.x + c.w / 2.0 - wx, c.y + c.h / 2.0 - wy)));
        }
        for (Karte.Raum r : order) {
            if (asked >= MAX_JE_RUNDE) break;
            if (r.vollstaendig()) continue;
            for (int part : karte.fehlendeTeile(r, TEIL_BYTES)) {
                if (asked >= MAX_JE_RUNDE) break;
                sender.request(1, r.nr, part);
                asked++;
            }
        }
        return asked > 0;
    }

    public synchronized String packet(byte[] data, int length, long now) {
        if (!wanted || length < 12 || D2ModState.packetType(data, length) != PKT_MAP) return null;
        ByteBuffer b = ByteBuffer.wrap(data, 0, length).order(ByteOrder.LITTLE_ENDIAN);
        b.position(8);
        int sub = b.get() & 0xFF;
        int lvl = b.getShort() & 0xFFFF;
        if (sub > 1 || lvl == 0) return null;
        level(lvl);
        try {
            if (sub == 0) {
                int stand = b.get() & 0xFF, total = b.getShort() & 0xFFFF, first = b.getShort() & 0xFFFF, count = b.get() & 0xFF;
                if (total > MAX_RAEUME || count > 60 || first > total) return null;
                gesamt = total;
                List<Karte.Raum> list = new ArrayList<>();
                JSONArray arr = new JSONArray();
                for (int i = 0; i < count && b.remaining() >= 11; i++) {
                    int nr = b.getShort() & 0xFFFF, x = b.getShort() & 0xFFFF, y = b.getShort() & 0xFFFF, w = b.getShort() & 0xFFFF, h = b.getShort() & 0xFFFF;
                    boolean loaded = (b.get() & 1) != 0;
                    if (nr >= total || w == 0 || h == 0 || w > MAX_SEITE || h > MAX_SEITE) continue;
                    Karte.Raum r = new Karte.Raum(nr, x, y, w, h, loaded);
                    Karte.Raum old = raeume.get(nr);
                    if (old != null && (old.x != x || old.y != y || old.w != w || old.h != h)) {
                        reset(lvl);
                        gesamt = total;
                        old = null;
                    }
                    list.add(r);
                    if (old == null) raeume.put(nr, r);
                    arr.put(new JSONArray().put(nr).put(x).put(y).put(w).put(h).put(loaded ? 1 : 0));
                }
                karte.raumliste(total, list);
                return new JSONObject().put("t", "karte").put("ms", now).put("gebiet", lvl).put("stand", stand).put("gesamt", total)
                    .put("erster", first).put("raeume", arr).toString();
            }
            int nr = b.getShort() & 0xFFFF, x = b.getShort() & 0xFFFF, y = b.getShort() & 0xFFFF, w = b.getShort() & 0xFFFF, h = b.getShort() & 0xFFFF;
            int part = b.get() & 0xFF, parts = b.get() & 0xFF, firstRow = b.getShort() & 0xFFFF, rows = b.getShort() & 0xFFFF;
            int rowBytes = (w + 7) / 8;
            Karte.Raum r = raeume.get(nr);
            if (r == null || r.x != x || r.y != y || r.w != w || r.h != h) return null;
            if (w > MAX_SEITE || h > MAX_SEITE || rows == 0 || firstRow + rows > h || b.remaining() < rowBytes * rows) return null;
            int rowsPerPart = Math.max(1, TEIL_BYTES / rowBytes);
            if (parts != (h + rowsPerPart - 1) / rowsPerPart || part >= parts || firstRow != part * rowsPerPart) return null;
            if (!r.vollstaendig() && bytes + (long) rowBytes * h > MAX_BYTES) return null;
            if (!r.angefangen()) bytes += (long) rowBytes * h;
            byte[] bits = new byte[rowBytes * rows];
            b.get(bits);
            karte.raumTeil(nr, x, y, w, h, firstRow, rows, bits, 0);
            return new JSONObject().put("t", "karte").put("ms", now).put("gebiet", lvl).put("raum", nr).put("x", x).put("y", y).put("w", w).put("h", h)
                .put("teil", part).put("teile", parts).put("zeile", firstRow).put("zeilen", rows)
                .put("bits", java.util.Base64.getEncoder().encodeToString(bits)).toString();
        }
        catch (Exception e) {
            return null;
        }
    }

    public synchronized String stand() {
        int complete = 0;
        for (Karte.Raum r : raeume.values()) if (r.vollstaendig()) complete++;
        return "level " + level + ": " + complete + "/" + raeume.size() + " rooms complete of " + gesamt;
    }
}
