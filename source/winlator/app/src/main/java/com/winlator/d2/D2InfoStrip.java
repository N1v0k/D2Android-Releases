package com.winlator.d2;

import com.winlator.R;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.BatteryManager;

import com.winlator.renderer.GLRenderer;

import java.io.File;
import java.util.Calendar;









public final class D2InfoStrip {
    private static final int FILL = 0xD8080705;

    private static final int COVER = 0xFF000000;

    private static final int XP_FULL = 0xFFFFFFFF, XP_TENTH_FULL = 0xFF505050, XP_TENTH_EMPTY = 0xFF7A7A7A;

    private static final float XP_TEXT = 1.25f;

    private static final int NORMAL = 0xC8B074, HIGHLIGHTED = 0xFFFFFF;

    private static final int BOX_GAP = 8, BOX_PAD = 5;

    private static final long NEVER = Long.MIN_VALUE;
    private final Context context;


    private boolean grid, xpBar, box, boxOpen, xpLevel;
    private final int[] slots = D2InfoGrid.DEFAULT_SLOTS.clone(), boxSlots = D2InfoGrid.DEFAULT_BOX.clone();

    private final boolean[] highlighted = new boolean[D2InfoGrid.KINDS];

    private long[] xp;

    private long xpHeldSince;
    static final long XP_HOLD_MS = 1500;
    private int batteryPercent = -1, framesPerSecond = -1, lastFrames, players, level;

    private int[] facts;
    private final Paint fill = new Paint();

    private long batteryAt = NEVER, fpsAt = NEVER;
    private File saveDir;

    private long characterSaved, saveCheckedAt = NEVER;
    private String savedCharacter;
    private final RectF plate = new RectF(), area = new RectF();

    public D2InfoStrip(Context context) {
        this.context = context.getApplicationContext();
    }


    public void applySettings() {
        grid = D2Settings.infoGrid(context);
        box = D2Settings.clockTop(context);
        boxOpen = D2Settings.boxOpen(context);
        xpBar = D2Settings.xpBar(context);
        xpLevel = D2Settings.xpLevel(context);
        for (int i = 0; i < D2InfoGrid.SLOTS; i++) slots[i] = D2InfoGrid.slot(D2Settings.gridSlot(context, i), i);
        for (int i = 0; i < D2InfoGrid.BOX_SLOTS; i++) boxSlots[i] = D2InfoGrid.boxSlot(D2Settings.boxSlot(context, i), i);
        for (int k = 0; k < D2InfoGrid.KINDS; k++) highlighted[k] = D2Settings.highlighted(context, k);
        batteryAt = fpsAt = NEVER;
    }


    public boolean gridOn() {
        return grid;
    }


    public boolean xpBarShown() {
        return xpBar && xp != null && D2InfoGrid.xpFilled(xp[0], xp[1], xp[2]) >= 0;
    }


    public boolean boxOn() {
        return box;
    }

    public boolean boxOpen() {
        return boxOpen;
    }


    public void toggleBox() {
        boxOpen = !boxOpen;
        D2Settings.setBoxOpen(context, boxOpen);
    }


    public float[] boxArea(D2Hud hud, int gameWidth, int gameHeight, boolean boxBlocked) {
        if (!box || !boxOpen || hud == null || boxBlocked) return null;
        long wall = System.currentTimeMillis(), mono = monotonicMs();
        int w = boxWidth(hud, wall, mono);
        return w <= 0 ? null : D2InfoGrid.box(gameWidth, gameHeight, w, xpBarShown());
    }


    public void setExperience(long[] xp) {


        if (xp != null && D2InfoGrid.xpFilled(xp[0], xp[1], xp[2]) < 0 && this.xp != null
                && D2InfoGrid.xpFilled(this.xp[0], this.xp[1], this.xp[2]) >= 0) {
            long now = monotonicMs();
            if (xpHeldSince == 0) xpHeldSince = now;
            if (now - xpHeldSince < XP_HOLD_MS) return;
        }
        xpHeldSince = 0;
        this.xp = xp;
    }


    public void setFacts(int[] facts) {
        this.facts = facts;
    }


    public boolean showsDps() {
        return (grid && D2InfoGrid.showsDps(slots)) || (box && boxOpen && D2InfoGrid.showsDps(boxSlots));
    }


    public void setLevel(int level) {
        this.level = level;
    }


    public void setPlayers(int players) {
        this.players = players;
    }


    public void setSaveDir(File dir) {
        saveDir = dir;
        saveCheckedAt = NEVER;
    }







    public String[] pauseLines(long now, boolean appSaves, String character) {
        return pauseLines(now, monotonicMs(), appSaves, character);
    }

    String[] pauseLines(long now, long monoMs, boolean appSaves, String character) {
        String saved;
        if (!appSaves) saved = D2Text.get(context, R.string.strip_only_game_saves);
        else {
            boolean other = character == null ? savedCharacter != null : !character.equals(savedCharacter);
            if (saveDir != null && (other || saveCheckedAt == NEVER || monoMs - saveCheckedAt > 2000)) {
                saveCheckedAt = monoMs;
                savedCharacter = character;
                characterSaved = characterFileTime(saveDir, character);
            }
            saved = characterSaved > 0 ? D2Text.get(context, R.string.strip_saved_ago, age(now - characterSaved)) : null;
        }
        String count = players > 0 ? D2Text.get(context, R.string.strip_players, players) : null;
        if (saved == null) return count == null ? new String[0] : new String[]{count};
        return count == null ? new String[]{saved} : new String[]{saved, count};
    }

    static String age(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s < 60) return s + " s";
        if (s < 3600) return s / 60 + " min";
        return s / 3600 + " h";
    }


    static long characterFileTime(File dir, String character) {
        if (character == null || character.isEmpty()) return 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().equalsIgnoreCase(character + ".d2s")) return f.lastModified();
            }
        }
        return 0;
    }


    public boolean active() {
        return grid || box || xpBar;
    }






    public void draw(Canvas canvas, D2Hud hud, int gameWidth, int gameHeight, float scale, float x0, float y0, boolean staminaKnown,
                     boolean boxBlocked, boolean compact) {
        if (hud == null || gameWidth <= 0 || gameHeight <= 0) return;
        long wall = System.currentTimeMillis(), mono = monotonicMs();
        float done = xp != null ? D2InfoGrid.xpFilled(xp[0], xp[1], xp[2]) : -1f;
        boolean bar = xpBar && done >= 0;
        boolean pieces = hud.hasBarPieces();
        if (grid && staminaKnown) {


            float[] block = D2InfoGrid.block(gameWidth, gameHeight);
            set(plate, block, scale, x0, y0);
            if (pieces) {
                float c = gameWidth / 2f;
                set(area, new float[]{c + D2InfoGrid.LEFT, gameHeight - D2InfoGrid.STONE_TOP, c + D2InfoGrid.RIGHT + 1,
                    gameHeight - D2InfoGrid.TOP}, scale, x0, y0);
                hud.drawStone(canvas, area, scale);
                hud.drawButtonBox(canvas, plate, scale);
            }
            else {
                fill.setColor(COVER);
                canvas.drawRect(plate, fill);
                if (hud.hasFrame()) hud.drawFrame(canvas, plate, scale, FILL);
            }
            float in = 4 * scale, halfW = (plate.width() - 2 * in) / 2, rowH = (plate.height() - 2 * in) / 2;
            for (int i = 0; i < D2InfoGrid.SLOTS; i++) {
                String t = entry(slots[i], wall, mono);
                if (t.isEmpty()) continue;
                float cx = plate.left + in + halfW * (i % 2) + halfW / 2;
                float cy = plate.top + in + rowH * (i / 2) + rowH / 2;

                float room = halfW / scale - 4, wide = hud.smallTextWidth(t);
                float k = wide > room ? scale * Math.max(0.6f, room / wide) : scale;
                hud.drawSmallText(canvas, t, cx, cy, k, 255, highlighted[slots[i]] ? HIGHLIGHTED : NORMAL);
            }
        }
        if (bar) {


            set(area, D2InfoGrid.xpBar(gameWidth, gameHeight, compact), scale, x0, y0);
            if (pieces) hud.drawXpBar(canvas, area, scale);
            else {
                fill.setColor(COVER);
                canvas.drawRect(area, fill);
            }
            float left = area.left + scale, right = area.right - scale, filled = left + (right - left) * done;
            fill.setColor(XP_FULL);
            canvas.drawRect(left, area.top + 2 * scale, filled, area.top + 4 * scale, fill);
            for (int i = 1; i < D2InfoGrid.XP_SECTIONS; i++) {
                float x = left + (right - left) * i / D2InfoGrid.XP_SECTIONS;
                fill.setColor(x <= filled ? XP_TENTH_FULL : XP_TENTH_EMPTY);

                canvas.drawRect(x, area.top + scale, x + Math.max(2, 2 * scale), area.top + 5 * scale, fill);
            }
            hud.drawTinyTextRight(canvas, Math.round(done * 100) + "%", area.right, area.top - 2 * scale, XP_TEXT * scale, NORMAL);

            if (xpLevel && level > 0) hud.drawTinyTextLeft(canvas, D2Text.get(context, R.string.xp_level, level), area.left,
                area.top - 2 * scale, XP_TEXT * scale, NORMAL);
        }

        if (box && boxOpen && staminaKnown && !boxBlocked) {
            int w = boxWidth(hud, wall, mono);
            if (w > 0) {

                set(plate, D2InfoGrid.box(gameWidth, gameHeight, w, bar), scale, x0, y0);
                if (pieces) hud.drawThinBox(canvas, plate, scale);
                else {
                    fill.setColor(COVER);
                    canvas.drawRect(plate, fill);
                    if (hud.hasFrame()) hud.drawFrame(canvas, plate, scale, FILL);
                }
                float x = plate.left + BOX_PAD * scale;
                for (int i = 0; i < boxSlots.length; i++) {
                    String t = entry(boxSlots[i], wall, mono);
                    if (t.isEmpty()) continue;
                    float tw = hud.smallTextWidth(t) * scale;
                    hud.drawSmallText(canvas, t, x + tw / 2, plate.centerY(), scale, 255, highlighted[boxSlots[i]] ? HIGHLIGHTED : NORMAL);
                    x += tw + BOX_GAP * scale;
                }
            }
        }
        if (box && staminaKnown) {

            set(area, D2InfoGrid.arrow(gameWidth, gameHeight), scale, x0, y0);
            hud.drawArrow(canvas, boxOpen, false, area);
        }
    }


    int boxWidth(D2Hud hud, long wall, long mono) {
        int w = 0, n = 0;
        for (int kind : boxSlots) {
            String t = entry(kind, wall, mono);
            if (t.isEmpty()) continue;
            w += hud.smallTextWidth(t);
            n++;
        }
        return n == 0 ? 0 : w + (n - 1) * BOX_GAP + 2 * BOX_PAD;
    }


    String boxText(long wall, long mono) {
        StringBuilder out = new StringBuilder();
        for (int kind : boxSlots) {
            String t = entry(kind, wall, mono);
            if (t.isEmpty()) continue;
            if (out.length() > 0) out.append("   ");
            out.append(t);
        }
        return out.toString();
    }

    private static void set(RectF r, float[] g, float scale, float x0, float y0) {
        r.set(x0 + g[0] * scale, y0 + g[1] * scale, x0 + g[2] * scale, y0 + g[3] * scale);
    }


    static long monotonicMs() {
        return System.nanoTime() / 1_000_000L;
    }





    String entry(int kind, long wallMs, long monoMs) {
        switch (kind) {
            case D2InfoGrid.FPS:
                countFrames(monoMs);
                return framesPerSecond >= 0 ? framesPerSecond + " FPS" : "- FPS";
            case D2InfoGrid.BATTERY:
                readBattery(monoMs);
                return batteryPercent >= 0 ? D2Text.get(context, R.string.grid_battery, batteryPercent) : "-";
            case D2InfoGrid.PLAYERS:
                return players > 0 ? D2Text.get(context, R.string.grid_players, players) : "-";
            case D2InfoGrid.CLOCK: {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(wallMs);
                return D2InfoGrid.clock(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
            }
            case D2InfoGrid.LEVEL:
                return level > 0 ? D2Text.get(context, R.string.grid_level, level) : "-";
            case D2InfoGrid.DPS:
                return facts != null && facts[0] >= 0 ? facts[0] + " DPS" : "- DPS";
            case D2InfoGrid.GOLD:
                return facts != null ? D2Text.get(context, R.string.grid_gold, D2InfoGrid.gold(facts[1])) : "-";
            default:
                return "";
        }
    }


    private void readBattery(long now) {
        if (batteryAt != NEVER && now - batteryAt < 30000) return;
        batteryAt = now;
        try {
            Intent status = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (status == null) return;
            int level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), max = status.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            batteryPercent = level >= 0 && max > 0 ? Math.round(level * 100f / max) : -1;
        }
        catch (RuntimeException ignored) {
            batteryPercent = -1;
        }
    }


    private void countFrames(long now) {
        int frames = GLRenderer.framesDrawn;
        if (fpsAt == NEVER) {
            fpsAt = now;
            lastFrames = frames;
            return;
        }
        long elapsed = now - fpsAt;
        if (elapsed < 1000) return;
        framesPerSecond = Math.round((frames - lastFrames) * 1000f / elapsed);
        fpsAt = now;
        lastFrames = frames;
    }
}
