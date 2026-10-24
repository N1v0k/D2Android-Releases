package com.winlator.d2.test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;






















final class WineFinger implements Finger {
    private static final int MOD_PORT = ModPorts.CMD;
    private static final int UNIT_PLAYER = 0, UNIT_MONSTER = 1, UNIT_OBJECT = 2, UNIT_ITEM = 4, UNIT_TILE = 5;
    private static final int OBJECT_PORTAL = 5, OBJECT_ENTRANCE = 6, OBJECT_CORPSE = 7;

    boolean ohneDurchgaenge;





    boolean modAuftrag(String was) {
        String[] w = was.split(" ");
        if (w.length != 2) return false;
        try {
            long n = Long.parseLong(w[1]);
            if (w[0].equals("npc") && n >= 0 && n < 16) {
                send(cmd(8, 0, 0).put((byte) n));
                return true;
            }
            if (w[0].equals("reparieren") && n > 0) {
                send(cmd(29, 0, 0).put(new byte[8]).putInt((int) n));
                return true;
            }
        }
        catch (NumberFormatException ignored) {}
        return false;
    }

    private final DatagramSocket socket;
    private final InetAddress local;
    private final Fahrer.Protokoll protokoll;
    private final boolean[] down = new boolean[Touch.FINGERS];
    private final float[] xs = new float[Touch.FINGERS], ys = new float[Touch.FINGERS];
    private volatile Zustand zustand;
    private volatile Zustand.Layout layout = new Zustand.Layout();

    volatile Fertigkeiten fertigkeiten;

    private int pressSkill = -1;
    private double walkDx, walkDy;
    private boolean walking;
    private long lastWalk, lastAttack;

    private long attackId = -1;
    private int attackMissing;

    int leerDruecke;

    private volatile java.util.Set<Long> ignoriert = java.util.Collections.emptySet();

    void ignorieren(java.util.Set<Long> ids) {
        ignoriert = ids;
    }


    private int attackButton;

    WineFinger(Fahrer.Protokoll protokoll) throws IOException {
        this.protokoll = protokoll;
        socket = new DatagramSocket();
        local = InetAddress.getByName("127.0.0.1");
        Thread t = new Thread(this::keepAlive, "wine-finger");
        t.setDaemon(true);
        t.start();
    }


    private final GenutzteObjekte genutzt = new GenutzteObjekte();

    private double joyRichtung;


    void zustand(Zustand z) {
        zustand = z;
        genutzt.zustand(z, System.nanoTime() / 1_000_000);
    }


    java.util.Set<Long> ausgelassen() {
        return genutzt.ausgelassen(System.nanoTime() / 1_000_000);
    }

    void layout(Zustand.Layout l) {
        layout = l;
    }

    @Override
    public synchronized boolean isDown(int finger) {
        return down[finger];
    }

    @Override
    public synchronized void down(int finger, float x, float y) {
        if (down[finger]) {
            move(finger, x, y);
            return;
        }
        down[finger] = true;
        xs[finger] = x;
        ys[finger] = y;
        if (finger == Fahrer.F_JOYSTICK) joystick(x, y, 0);
        else if (finger == Fahrer.F_ANGRIFF) attackDown(x, y);
        else tap(x, y);
    }

    @Override
    public synchronized void move(int finger, float x, float y) {
        if (!down[finger]) return;
        xs[finger] = x;
        ys[finger] = y;
        if (finger == Fahrer.F_JOYSTICK) joystick(x, y, 1);
    }

    @Override
    public synchronized void up(int finger) {
        if (!down[finger]) return;
        down[finger] = false;
        if (finger == Fahrer.F_ANGRIFF) attackUp();
        if (finger == Fahrer.F_JOYSTICK && walking) {
            walking = false;
            send(cmd(4, 0, 0).put((byte) 2).put((byte) 0).putShort((short) 0).putShort((short) 0));
        }
    }

    @Override
    public synchronized void releaseAll() {
        for (int f = Touch.FINGERS - 1; f >= 0; f--) up(f);
    }




    private void joystick(float x, float y, int phase) {
        Zustand.Layout l = layout;
        double dx = x - l.joystick[0], dy = y - l.joystick[1];
        double len = Math.hypot(dx, dy);
        if (len < 10) return;
        Zustand z = zustand;
        double distance = (z != null && z.gh > 0 ? z.gh : 600) * 0.22;
        walkDx = dx / len * distance;
        walkDy = dy / len * distance;
        joyRichtung = Math.atan2(dy, dx);
        genutzt.joystick(joyRichtung, System.nanoTime() / 1_000_000);
        walking = true;
        sendWalk(walking && phase == 0 ? 0 : 1);
    }

    private void sendWalk(int phase) {
        lastWalk = System.nanoTime() / 1_000_000;
        send(cmd(4, 0, 0).put((byte) phase).put((byte) 0).putShort((short) Math.round(walkDx)).putShort((short) Math.round(walkDy)));
    }


    private void keepAlive() {
        while (true) {
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
                return;
            }
            synchronized (this) {
                long now = System.nanoTime() / 1_000_000;
                if (walking && now - lastWalk >= 200) sendWalk(1);
                if (down[Fahrer.F_ANGRIFF] && now - lastAttack >= 200) attack();
            }
        }
    }





    private void attack() {
        lastAttack = System.nanoTime() / 1_000_000;
        Zustand z = zustand;
        if (z == null || attackId == 0) return;
        Zustand.Ding held = null;
        for (Zustand.Ding d : z.feinde()) if (d.id == attackId && !ignoriert.contains(d.id)) held = d;
        if (held != null) {
            attackMissing = 0;
            unit(1, attackId);
            highlight(attackId);
            return;
        }
        if (attackId > 0 && ++attackMissing < 3) return;
        attackMissing = 0;
        Zustand.Ding best = pickTarget(z);
        attackId = best != null ? best.id : -1;
        if (best != null) unit(0, best.id);
        highlight(attackId);
    }





    private void highlight(long id) {
        send(cmd(3, 0, 0).putInt(id > 0 ? UNIT_MONSTER : 0).putInt(id > 0 ? (int) id : -1));
    }






    private Zustand.Ding pickTarget(Zustand z) {
        Zustand.Layout l = layout;
        double weite = reichweite(z, l);
        double range = weite > 0 ? weite : Double.MAX_VALUE;
        boolean cone = l.zielweite > 0 && l.zielkegel > 0 && walking;
        double minCos = Math.cos(Math.toRadians(l.zielkegel)), dirLen = Math.hypot(walkDx, walkDy);
        Zustand.Ding best = null;
        double bestScore = Double.MAX_VALUE;
        for (Zustand.Ding d : z.feinde()) {
            if (ignoriert.contains(d.id)) continue;
            double dx = d.x - z.px, dy = d.y - z.py, distance = z.bodenAbstand(d);
            if (distance > range) continue;
            double score = distance;
            if (cone && distance > 1 && dirLen > 0) {
                double cos = (dx * walkDx + dy * walkDy) / (Math.hypot(dx, dy) * dirLen);
                if (cos < minCos) continue;
                score = distance * (1.5 - cos);
            }
            if (score < bestScore) {
                bestScore = score;
                best = d;
            }
        }
        return best;
    }






    private double reichweite(Zustand z, Zustand.Layout l) {
        if (l.zielweiteNah <= 0 || l.zielweiteFern <= 0 || fertigkeiten == null || z == null) return l.zielweite;
        byte[] p6 = z.punkte6;
        int attack = p6 != null ? com.winlator.d2.D2Points.parseAttack(p6, p6.length, 0) : com.winlator.d2.D2AimReach.ATTACK_UNKNOWN;
        int skill = pressSkill >= 0 ? pressSkill : z.links;
        boolean nah = com.winlator.d2.D2AimReach.melee(com.winlator.d2.D2AimReach.range(fertigkeiten, skill), attack);
        return nah ? l.zielweiteNah : l.zielweiteFern;
    }





    private boolean insLeere(Zustand z, String button) {
        if (layout.zielweite <= 0 || z == null || pickTarget(z) != null) return false;
        attackId = 0;
        leerDruecke++;
        protokoll.event("wine", "Druck auf " + button + " ins Leere: niemand in Reichweite " + Math.round(reichweite(z, layout))
            + " / Kegel " + Math.round(layout.zielkegel) + "° der Zielhilfe (" + leerDruecke + ". Mal)");
        return true;
    }





    private void attackDown(float x, float y) {
        genutzt.abgebrochen();
        String button = nearestButton(x, y);
        Zustand z = zustand;
        if (button != null && button.matches("F[1-4]") && z != null) {
            int index = button.charAt(1) - '1';
            key("F" + (index + 1));
            attackButton = z.fkLinks[index] ? 0 : 1;
            pressSkill = z.fkFertigkeit[index];
            lastAttack = System.nanoTime() / 1_000_000 - 120;
            protokoll.event("wine", "Fertigkeit " + z.fkFertigkeit[index] + " über " + button + " (" + (attackButton == 1 ? "rechts" : "links") + ")");
            insLeere(z, button);
            return;
        }
        attackButton = 0;
        pressSkill = -1;
        if (insLeere(z, "Angriff")) {
            lastAttack = System.nanoTime() / 1_000_000;
            return;
        }
        attack();
    }


    synchronized long gehaltenesZiel() {
        return down[Fahrer.F_ANGRIFF] && attackId > 0 ? attackId : -1;
    }


    private void attackUp() {
        if (attackId > 0) unit(2, attackId);
        if (attackId > 0) highlight(-1);
        attackId = -1;
        attackMissing = 0;
    }


    private void unit(int phase, long id) {
        send(cmd(2, 0, 0).put((byte) attackButton).put((byte) phase).put((byte) 0).put((byte) 0).putInt(UNIT_MONSTER).putInt((int) id));
    }


    private String nearestButton(float x, float y) {
        String button = null;
        double bestD = 90;
        for (java.util.Map.Entry<String, int[]> e : layout.knoepfe.entrySet()) {
            double d = Math.hypot(e.getValue()[0] - x, e.getValue()[1] - y);
            if (d < bestD) {
                bestD = d;
                button = e.getKey();
            }
        }
        return button;
    }

    private void tap(float x, float y) {
        String button = nearestButton(x, y);
        Zustand z = zustand;
        Zustand.Layout l = layout;
        if (button == null && z != null && z.panel && !z.pause && l.spiel != null && x >= l.spiel[0] && y >= l.spiel[1]
                && x < l.spiel[0] + l.spiel[2] && y < l.spiel[1] + l.spiel[3]) {

            int gx = Math.round((x - l.spiel[0]) * (z.gw > 0 ? z.gw : 800) / (float) l.spiel[2]);
            int gy = Math.round((y - l.spiel[1]) * (z.gh > 0 ? z.gh : 600) / (float) l.spiel[3]);
            click(gx, gy, 1);
            protokoll.event("wine", "Tipp ins Spiel bei " + gx + "," + gy + " (Fenster offen, xdotool)");
            return;
        }
        if (button == null) {

            send(cmd(6, 0, 0).put((byte) 0));
            protokoll.event("wine", "Tipp ohne Knopf bei " + Math.round(x) + "," + Math.round(y) + " → fortsetzen (Befehl 6)");
            return;
        }
        switch (button) {
            case "interact": interact(); break;
            case "menu_portal": send(cmd(7, 0, 0).put((byte) 0)); break;
            case "heal": send(cmd(1, 0, 0).put((byte) 0).put((byte) 0)); break;
            case "mana": send(cmd(1, 0, 0).put((byte) 1).put((byte) 0)); break;
            case "menu_pause": send(cmd(6, 0, 0).put((byte) 1)); break;
            case "menu_esc":
            case "hidden0": key("Escape"); break;
            case "menu_quests": key("q"); break;
            case "F1": case "F2": case "F3": case "F4": skillTap(button.charAt(1) - '1'); break;
            default: protokoll.event("wine", "Knopf " + button + " hat unter Wine keine Wirkung");
        }
    }






    private void skillTap(int index) {
        Zustand z = zustand;
        if (z == null) return;
        key("F" + (index + 1));
        final int button = z.fkLinks[index] ? 0 : 1;
        Zustand.Ding target = null;
        for (Zustand.Ding d : z.feinde()) {
            if (ignoriert.contains(d.id)) continue;
            if (target == null || z.bodenAbstand(d) < z.bodenAbstand(target)) target = d;
        }
        final long id = target != null ? target.id : -1;
        final int x = z.px, y = z.py - 40;
        protokoll.event("wine", "Tipp Fertigkeit " + z.fkFertigkeit[index] + " über F" + (index + 1)
            + (id > 0 ? " auf Monster " + id : " (Klick über der Figur)"));
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(80);
            }
            catch (InterruptedException e) {
                return;
            }
            synchronized (this) {
                if (id > 0) {
                    send(cmd(2, 0, 0).put((byte) button).put((byte) 3).put((byte) 0).put((byte) 0).putInt(UNIT_MONSTER).putInt((int) id));
                    return;
                }
            }
            click(x, y, button == 1 ? 3 : 1);
        }, "wine-skill-tap");
        t.setDaemon(true);
        t.start();
    }


    private void click(int x, int y, int mouseButton) {
        try {
            String display = System.getProperty("d2t.display", ":5");
            ProcessBuilder pb = new ProcessBuilder("xdotool", "mousemove", String.valueOf(240 + x), String.valueOf(73 + y),
                "click", String.valueOf(mouseButton)).redirectErrorStream(true);
            pb.environment().put("DISPLAY", display);
            pb.start().getInputStream().close();
        }
        catch (IOException e) {
            protokoll.event("wine", "xdotool: " + e);
        }
    }


    private void interact() {
        Zustand z = zustand;
        if (z == null) return;
        double range = (z.gh > 0 ? z.gh : 600) * 0.55;
        List<Object[]> list = new ArrayList<>();
        for (Zustand.Ding d : z.items) {
            double s = z.bodenAbstand(d);
            if (s <= range) list.add(new Object[]{s * 0.8, UNIT_ITEM, d.id});
        }
        long now = System.nanoTime() / 1_000_000;
        java.util.Set<Long> left = genutzt.ausgelassen(now);
        for (Zustand.Ding d : z.objekte) {
            double s = z.bodenAbstand(d);
            if (ohneDurchgaenge && (d.art == OBJECT_PORTAL || d.art == OBJECT_ENTRANCE)) continue;

            if (d.art != OBJECT_ENTRANCE && d.art != OBJECT_CORPSE && left.contains(d.id)) continue;
            if (Objektwahl.nehmenNie(d.art, d.extra, z.objektDetails.get(d.id))) continue;
            int type = d.art == OBJECT_ENTRANCE ? UNIT_TILE : d.art == OBJECT_CORPSE ? UNIT_PLAYER : UNIT_OBJECT;

            if (s <= range) list.add(new Object[]{d.art == OBJECT_CORPSE ? -1.0 : s, type, d.id, d.art});
        }
        for (Zustand.Ding d : z.monster) {

            if (!z.stadt && (d.extra & Zustand.MONSTER_TOWN_NPC) == 0) continue;
            double s = z.bodenAbstand(d);
            if (s <= range) list.add(new Object[]{s, UNIT_MONSTER, d.id});
        }
        if (list.isEmpty()) {
            protokoll.event("wine", "Nehmen: nichts in Reichweite");
            return;
        }
        list.sort((a, b) -> Double.compare((Double) a[0], (Double) b[0]));
        Object[] best = list.get(0);
        protokoll.event("wine", "Nehmen: Einheit Typ " + best[1] + " ID " + best[2]);

        genutzt.begonnen((Long) best[2], (Integer) best[1], best.length > 3 ? (Integer) best[3] : -1, joyRichtung, now);

        send(cmd(15, 0, 0).put((byte) (int) (Integer) best[1]).put((byte) 0).put((byte) 0).put((byte) 0).putInt((int) (long) (Long) best[2]));
    }


    public void anlegen(long id) {
        protokoll.event("wine", "Anlegen: ID " + id);
        send(cmd(22, 0, 0).put((byte) 4).put((byte) 0).put((byte) 0).put((byte) 0).putInt((int) id));
    }


    public void punkte(byte[] befehl, String text) {
        protokoll.event("wine", "Befehl 19: " + text);
        try {
            socket.send(new DatagramPacket(befehl, befehl.length, local, MOD_PORT));
        }
        catch (IOException e) {
            protokoll.event("wine", "Mod-Befehl: " + e);
        }
    }

    private void key(String name) {
        try {
            String display = System.getProperty("d2t.display", ":5");
            ProcessBuilder pb = new ProcessBuilder("xdotool", "key", name).redirectErrorStream(true);
            pb.environment().put("DISPLAY", display);
            pb.start().getInputStream().close();
            protokoll.event("wine", "Taste " + name + " (xdotool, DISPLAY " + display + ")");
        }
        catch (IOException e) {
            protokoll.event("wine", "xdotool: " + e);
        }
    }

    private static ByteBuffer cmd(int command, int a, int b) {
        ByteBuffer buf = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 'D').put((byte) '2').put((byte) 'A').put((byte) 'C').put((byte) 1).put((byte) command).put((byte) a).put((byte) b);
        return buf;
    }

    private void send(ByteBuffer buf) {
        try {
            socket.send(new DatagramPacket(buf.array(), buf.position(), local, MOD_PORT));
        }
        catch (IOException e) {
            protokoll.event("wine", "Mod-Befehl: " + e);
        }
    }
}
