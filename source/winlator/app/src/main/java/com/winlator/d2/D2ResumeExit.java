package com.winlator.d2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;





















final class D2ResumeExit {
    interface Host {

        void send(int kind);


        String text(int which);
    }

    static final int TEXT_TOWN = 0;
    static final int KIND_TOWN = 2;

    static final int NOTHING = 0, PLAY_ON = 1, TOWN = 2;
    static final long LOCK_MS = 600;

    static final float LINE_H = 16;

    static final int PRESSED_RGB = 0x8C7A4E;

    static final float MIN_DP = 48;

    static final int PAD_A = D2Gamepad.A, PAD_START = D2Gamepad.START, PAD_B = D2Gamepad.B;

    private final Host host;
    private boolean shown, chosen;
    private int offer, pressed = NOTHING, chosenAction = NOTHING;
    private long shownAt, drawnAt;

    final RectF town = new RectF();
    private final Paint label = new Paint();

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
        D2Trace.log("resume: arrival pause, " + (this.offer == 0 ? "plain" : "tap plays on, line back to town"));
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
        return town.contains(x, y) ? TOWN : PLAY_ON;
    }


    int padAction(int button, long time) {
        if (!active()) return button == PAD_A || button == PAD_START ? PLAY_ON : NOTHING;
        if (locked(time)) return NOTHING;
        if (button == PAD_A || button == PAD_START) return PLAY_ON;
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


    float height(float k, float dp) {
        return active() ? Math.max(LINE_H * k, MIN_DP * dp) + 4 * dp : 0;
    }


    void layout(float cx, float top, float textW, float k, float dp) {
        float w = Math.max(textW + 24 * dp, MIN_DP * dp), h = Math.max(LINE_H * k, MIN_DP * dp);
        town.set(cx - w / 2, top, cx + w / 2, top + h);
    }





    void draw(Canvas canvas, D2Hud hud, float cx, float top, float k, float dp, boolean pad, long now) {
        if (!active()) return;
        if (drawnAt == 0) drawnAt = now;
        String text = host.text(TEXT_TOWN) + (pad ? " (B)" : "");
        boolean down = pressed == TOWN;
        if (hud != null) {
            layout(cx, top, hud.textWidth(text) * k, k, dp);
            if (down) hud.drawText(canvas, text, cx, town.centerY(), k, PRESSED_RGB);
            else hud.drawText(canvas, text, cx, town.centerY(), k);
            return;
        }
        label.setTextSize(14 * dp);
        label.setTextAlign(Paint.Align.CENTER);
        label.setColor(down ? D2Style.FRAME : D2Style.GOLD);
        layout(cx, top, label.measureText(text), k, dp);
        canvas.drawText(text, cx, town.centerY() + 5 * dp, label);
    }
}
