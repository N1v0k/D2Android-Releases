package com.winlator.d2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;







final class D2PadHint {
    private D2PadHint() {}

    static final int A = D2PadNav.GLYPH_A, B = D2PadNav.GLYPH_B, X = D2PadNav.GLYPH_X, Y = D2PadNav.GLYPH_Y,
        LB = D2PadNav.GLYPH_LB, RB = D2PadNav.GLYPH_RB, START = D2PadNav.GLYPH_START, LEFT_RIGHT = D2PadNav.GLYPH_LEFT_RIGHT;


    static Bitmap picture(D2GameFont font, int[] glyphs, String[] words) {
        int h = font.height, r = h / 2 + 1, gap = h / 2, space = h;
        Bitmap[] texts = new Bitmap[words.length];
        int width = 0;
        for (int i = 0; i < glyphs.length; i++) {
            texts[i] = bitmap(font.render(words[i], 0xA0A0A0));
            width += glyphWidth(glyphs[i], r) + gap + texts[i].getWidth() + (i + 1 < glyphs.length ? space : 0);
        }
        int height = Math.max(h, 2 * r + 1);
        Bitmap out = Bitmap.createBitmap(Math.max(1, width), height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), rim = new Paint(Paint.ANTI_ALIAS_FLAG), mark = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(Color.rgb(34, 28, 20));
        rim.setColor(Color.rgb(200, 176, 116));
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1);
        mark.setColor(Color.rgb(240, 224, 170));
        float x = 0, cy = height / 2f;
        for (int i = 0; i < glyphs.length; i++) {
            int w = glyphWidth(glyphs[i], r);
            RectF stone = new RectF(x + 0.5f, cy - r + 0.5f, x + w - 0.5f, cy + r - 0.5f);
            c.drawRoundRect(stone, r, r, fill);
            c.drawRoundRect(stone, r, r, rim);
            float cx = x + w / 2f;
            if (glyphs[i] == START) {
                float lw = r * 0.9f;
                for (int k = -1; k <= 1; k++) c.drawRect(cx - lw / 2, cy + k * r * 0.4f - 0.5f, cx + lw / 2, cy + k * r * 0.4f + 0.5f, mark);
            }
            else if (glyphs[i] == LEFT_RIGHT) {
                float a = r * 0.45f;
                android.graphics.Path p = new android.graphics.Path();
                p.moveTo(cx - r * 0.85f, cy);
                p.lineTo(cx - r * 0.85f + a, cy - a);
                p.lineTo(cx - r * 0.85f + a, cy + a);
                p.close();
                p.moveTo(cx + r * 0.85f, cy);
                p.lineTo(cx + r * 0.85f - a, cy - a);
                p.lineTo(cx + r * 0.85f - a, cy + a);
                p.close();
                c.drawPath(p, mark);
            }
            else {
                Bitmap letter = bitmap(font.render(label(glyphs[i]), 0xF0E0A0));
                c.drawBitmap(letter, cx - letter.getWidth() / 2f, cy - letter.getHeight() / 2f, null);
            }
            x += w + gap;
            c.drawBitmap(texts[i], x, (height - texts[i].getHeight()) / 2f, null);
            x += texts[i].getWidth() + space;
        }
        return out;
    }

    private static int glyphWidth(int glyph, int r) {
        return glyph == LB || glyph == RB || glyph == LEFT_RIGHT ? 3 * r : 2 * r;
    }

    private static String label(int glyph) {
        switch (glyph) {
            case A: return "A";
            case B: return "B";
            case X: return "X";
            case Y: return "Y";
            case LB: return "LB";
            case RB: return "RB";
            default: return "";
        }
    }

    private static Bitmap bitmap(D2GameFont.Img img) {
        return Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888);
    }





    static final class Bar extends View {
        private final Paint pixels = new Paint(), band = new Paint(), line = new Paint();
        private Bitmap picture;
        private final int scale;

        Bar(Context context) {
            super(context);
            pixels.setFilterBitmap(false);
            band.setColor(Color.argb(235, 10, 8, 5));
            line.setColor(D2Style.FRAME);
            scale = Math.max(2, Math.round(context.getResources().getDisplayMetrics().density * 1.25f));
            setVisibility(GONE);
        }


        void show(Bitmap picture) {
            this.picture = picture;
            setVisibility(picture == null ? GONE : VISIBLE);
            requestLayout();
            invalidate();
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int h = picture == null ? 0 : picture.getHeight() * scale + 2 * Math.round(6 * getResources().getDisplayMetrics().density);
            setMeasuredDimension(MeasureSpec.getSize(widthSpec), h);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (picture == null) return;
            canvas.drawRect(0, 0, getWidth(), getHeight(), band);
            canvas.drawRect(0, 0, getWidth(), Math.max(1, getResources().getDisplayMetrics().density), line);
            int k = scale;
            while (k > 1 && picture.getWidth() * k > getWidth() - 16) k--;
            float w = picture.getWidth() * k, h = picture.getHeight() * k;
            float x = (getWidth() - w) / 2f, y = (getHeight() - h) / 2f;
            canvas.drawBitmap(picture, null, new RectF(x, y, x + w, y + h), pixels);
        }
    }
}
