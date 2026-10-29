package com.winlator.d2;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;







final class D2PadFrame {
    private D2PadFrame() {}

    static final int OUTER = 0xFFECD48C, SEAM = 0xFF1A1208, INNER = 0xFF8C7440, CORNER = 0xFFFFECAA, FILL = 0x1EE8CF8A;
    private static final int GLOW = 0x60E8CF8A;


    static void draw(Canvas canvas, RectF r, float density) {
        if (r.width() < 4 || r.height() < 4) return;
        int o = Math.max(2, Math.round(1.75f * density)), seam = Math.max(1, Math.round(0.6f * density));
        int inner = Math.max(1, Math.round(0.6f * density)), glow = Math.round(6 * density);
        int cornerLen = Math.round(12 * density), cornerT = o + Math.max(1, Math.round(density));
        float l = Math.round(r.left), t = Math.round(r.top), rr = Math.round(r.right), b = Math.round(r.bottom);
        Paint p = new Paint();
        p.setColor(FILL);
        canvas.drawRect(l, t, rr, b, p);

        float g0 = o + seam + inner;
        glowBand(canvas, l + g0, t + g0, l + g0, t + g0 + glow, rr - g0, true);
        glowBand(canvas, l + g0, b - g0, l + g0, b - g0 - glow, rr - g0, true);
        glowBand(canvas, l + g0, t + g0, l + g0 + glow, t + g0, b - g0, false);
        glowBand(canvas, rr - g0, t + g0, rr - g0 - glow, t + g0, b - g0, false);
        ring(canvas, p, OUTER, l, t, rr, b, o);
        ring(canvas, p, SEAM, l + o, t + o, rr - o, b - o, seam);
        ring(canvas, p, INNER, l + o + seam, t + o + seam, rr - o - seam, b - o - seam, inner);

        p.setColor(CORNER);
        float len = Math.min(cornerLen, Math.min(r.width(), r.height()) / 3);
        canvas.drawRect(l, t, l + len, t + cornerT, p);
        canvas.drawRect(l, t, l + cornerT, t + len, p);
        canvas.drawRect(rr - len, t, rr, t + cornerT, p);
        canvas.drawRect(rr - cornerT, t, rr, t + len, p);
        canvas.drawRect(l, b - cornerT, l + len, b, p);
        canvas.drawRect(l, b - len, l + cornerT, b, p);
        canvas.drawRect(rr - len, b - cornerT, rr, b, p);
        canvas.drawRect(rr - cornerT, b - len, rr, b, p);
    }

    private static void ring(Canvas canvas, Paint p, int color, float l, float t, float r, float b, float w) {
        if (r - l <= 2 * w || b - t <= 2 * w) return;
        p.setColor(color);
        canvas.drawRect(l, t, r, t + w, p);
        canvas.drawRect(l, b - w, r, b, p);
        canvas.drawRect(l, t + w, l + w, b - w, p);
        canvas.drawRect(r - w, t + w, r, b - w, p);
    }


    private static void glowBand(Canvas canvas, float x0, float y0, float x1, float y1, float end, boolean horizontal) {
        Paint g = new Paint();
        g.setShader(new LinearGradient(x0, y0, x1, y1, GLOW, 0, Shader.TileMode.CLAMP));
        if (horizontal) canvas.drawRect(x0, Math.min(y0, y1), end, Math.max(y0, y1), g);
        else canvas.drawRect(Math.min(x0, x1), y0, Math.max(x0, x1), end, g);
    }


    static final class Focused extends Drawable {
        private final float density;
        private boolean focused;
        private final RectF where = new RectF();

        Focused(float density) {
            this.density = density;
        }

        @Override
        public boolean isStateful() {
            return true;
        }


        @Override
        public boolean hasFocusStateSpecified() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            boolean now = false;
            for (int s : state) if (s == android.R.attr.state_focused) now = true;
            if (now == focused) return false;
            focused = now;
            invalidateSelf();
            return true;
        }

        @Override
        public void draw(Canvas canvas) {
            if (!focused) return;
            Rect b = getBounds();
            where.set(b);
            D2PadFrame.draw(canvas, where, density);
        }

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(ColorFilter colorFilter) {}

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
