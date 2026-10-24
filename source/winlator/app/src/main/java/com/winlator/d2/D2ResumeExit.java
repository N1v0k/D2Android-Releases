package com.winlator.d2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;




















final class D2ResumeExit {
    interface Host {

        void send(int kind);


        String text(int which);
    }

    static final int TEXT_TOWN = 0, TEXT_CONTINUE = 1;
    static final int KIND_TOWN = 2;

    static final int NOTHING = 0, PLAY_ON = 1, TOWN = 2;
    static final long LOCK_MS = 600;

    static final float BUTTON_W = 128, BUTTON_H = 35;

    static final float MIN_DP = 48;

    static final int PAD_A = D2Gamepad.A, PAD_START = D2Gamepad.START, PAD_B = D2Gamepad.B;

    private final Host host;
    private boolean shown, chosen;
    private int offer, pressed = NOTHING, chosenAction = NOTHING;
    private long shownAt, drawnAt;

    final RectF town = new RectF(), play = new RectF();
    private final Paint fill = new Paint(), line = new Paint(), label = new Paint();

    D2ResumeExit(Host host) {
        this.host = host;
    }


    void show(int offer, long now) {
        shown = true;
        chosen = false;
        chosenAction = NOTHING;
        this.offer = offer & D2ResumeState.OFFER_TOWN;
        shownAt = now;
        drawnAt = 0;
        pressed = NOTHING;
        town.set(0, 0, 0, 0);
        play.set(0, 0, 0, 0);
        D2Trace.log("resume: arrival pause, " + (this.offer == 0 ? "plain" : "buttons continue and town"));
    }


    void hide() {
        shown = false;
        pressed = NOTHING;
    }


    boolean active() {
        return shown && offer != 0 && !chosen;
    }

    boolean offers(int action) {
        return action == PLAY_ON || action == TOWN && (offer & D2ResumeState.OFFER_TOWN) != 0;
    }


    boolean locked(long time) {
        return active() && time < (drawnAt != 0 ? drawnAt : shownAt) + LOCK_MS;
    }


    int tapAction(float x, float y, long downTime) {
        if (!active()) return PLAY_ON;
        if (locked(downTime)) return NOTHING;
        if (play.contains(x, y)) return PLAY_ON;
        if (town.contains(x, y)) return TOWN;
        return NOTHING;
    }


    int padAction(int button, long time) {
        if (!active()) return button == PAD_A || button == PAD_START ? PLAY_ON : NOTHING;
        if (locked(time)) return NOTHING;
        if (button == PAD_A) return PLAY_ON;
        if (button == PAD_B) return TOWN;
        return NOTHING;
    }


    boolean take(int action, String how) {
        if (action == NOTHING) return false;
        if (action == PLAY_ON) {
            if (active()) D2Trace.log("resume: play on at the place (" + how + ")");
            chosen = true;
            chosenAction = PLAY_ON;
            return true;
        }
        if (!active() || !offers(action)) return false;
        chosen = true;
        chosenAction = action;
        D2Trace.log("resume: way out to town (" + how + ")");
        host.send(KIND_TOWN);
        return true;
    }


    boolean wayOutChosen() {
        return chosenAction == TOWN;
    }


    void down(float x, float y, long downTime) {
        int a = tapAction(x, y, downTime);
        pressed = active() ? a : NOTHING;
    }






    boolean tapped(float x, float y, long downTime) {
        int was = pressed, a = tapAction(x, y, downTime);
        pressed = NOTHING;
        if (active() && (was == NOTHING || a != was)) return false;
        return take(a, "tap");
    }


    boolean pad(int button, long time) {
        return take(padAction(button, time), "pad");
    }


    String tapText(String normal) {
        return active() ? null : normal;
    }


    static float scale(float k, float dp) {
        return Math.max(k, MIN_DP * dp / BUTTON_H);
    }


    float height(float k, float dp) {
        return active() ? BUTTON_H * scale(k, dp) + 8 * dp : 0;
    }


    void layout(float cx, float top, float k, float dp) {
        float s = scale(k, dp), w = BUTTON_W * s, h = BUTTON_H * s, gap = 16 * dp;
        play.set(cx - w - gap / 2, top, cx - gap / 2, top + h);
        town.set(cx + gap / 2, top, cx + gap / 2 + w, top + h);
    }


    void draw(Canvas canvas, D2Hud hud, float cx, float top, float k, float dp, boolean pad, long now) {
        if (!active()) return;
        if (drawnAt == 0) drawnAt = now;
        layout(cx, top, k, dp);
        button(canvas, hud, play, host.text(TEXT_CONTINUE) + (pad ? " (A)" : ""), pressed == PLAY_ON, k, dp);
        button(canvas, hud, town, host.text(TEXT_TOWN) + (pad ? " (B)" : ""), pressed == TOWN, k, dp);
    }


    private void button(Canvas canvas, D2Hud hud, RectF where, String text, boolean down, float k, float dp) {
        if (hud != null && hud.drawMediumButton(canvas, text, down, where)) return;
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(down ? D2Style.FRAME : D2Style.BUTTON);
        canvas.drawRect(where, fill);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(Math.max(1, dp));
        line.setColor(D2Style.FRAME);
        canvas.drawRect(where, line);
        if (hud != null) {
            hud.drawText(canvas, text, where.centerX(), where.centerY(), k, 0xD2CEC6);
            return;
        }
        label.setColor(D2Style.TEXT);
        label.setTextSize(14 * dp);
        label.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, where.centerX(), where.centerY() + 5 * dp, label);
    }
}
