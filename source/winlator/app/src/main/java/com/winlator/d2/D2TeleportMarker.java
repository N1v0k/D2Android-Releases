package com.winlator.d2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;






final class D2TeleportMarker {

    static final int GOLD = 0xFF7B704D, DARK_GOLD = 0xFF554D2E, EDGE = 0xFF141414;

    static final int RED = 0xFFB0281E;

    static final float RX = 18, RY = 9;

    private static final RectF oval = new RectF();

    private D2TeleportMarker() {}





    static void draw(Canvas canvas, float cx, float cy, float k, boolean impossible, Paint fill, Paint stroke) {
        float width = stroke.getStrokeWidth(), rx = RX * k, ry = RY * k;
        oval.set(cx - rx, cy - ry, cx + rx, cy + ry);
        stroke.setStrokeWidth(3.5f * k);
        stroke.setColor(EDGE);
        canvas.drawOval(oval, stroke);
        stroke.setStrokeWidth(2f * k);
        stroke.setColor(impossible ? RED : GOLD);
        canvas.drawOval(oval, stroke);
        oval.inset(1.2f * k, 0.6f * k);
        stroke.setStrokeWidth(0.8f * k);
        stroke.setColor(impossible ? EDGE : DARK_GOLD);
        canvas.drawOval(oval, stroke);
        if (impossible) {

            float dx = rx * 0.45f, dy = ry * 0.45f;
            for (int pass = 0; pass < 2; pass++) {
                stroke.setStrokeWidth((pass == 0 ? 3.5f : 2f) * k);
                stroke.setColor(pass == 0 ? EDGE : RED);
                canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, stroke);
                canvas.drawLine(cx - dx, cy + dy, cx + dx, cy - dy, stroke);
            }
        }
        else {
            oval.set(cx - 2.5f * k, cy - 1.25f * k, cx + 2.5f * k, cy + 1.25f * k);
            fill.setColor(EDGE);
            canvas.drawOval(oval, fill);
            oval.inset(0.8f * k, 0.4f * k);
            fill.setColor(GOLD);
            canvas.drawOval(oval, fill);
        }
        stroke.setStrokeWidth(width);
    }





    static void drawCorrection(Canvas canvas, float wx, float wy, float tx, float ty, float k, Paint stroke) {
        float dx = wx - tx, dy = wy - ty;

        float e = (float) Math.sqrt(dx * dx / (RX * RX * k * k) + dy * dy / (RY * RY * k * k));
        if (e <= 1) return;
        float width = stroke.getStrokeWidth(), sx = tx + dx / e, sy = ty + dy / e;
        stroke.setStrokeWidth(2.4f * k);
        stroke.setColor(EDGE);
        canvas.drawLine(sx, sy, wx, wy, stroke);
        stroke.setStrokeWidth(1f * k);
        stroke.setColor(DARK_GOLD);
        canvas.drawLine(sx, sy, wx, wy, stroke);
        oval.set(wx - 1.8f * k, wy - 0.9f * k, wx + 1.8f * k, wy + 0.9f * k);
        canvas.drawOval(oval, stroke);
        stroke.setStrokeWidth(width);
    }
}
