package com.winlator.d2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.ArrayList;
import java.util.List;






@SuppressLint("ViewConstructor")
final class D2GameText extends View {
    private final D2SettingsLook look;
    private final D2GameFont font;
    private final int rgb;
    private String text, value;
    private int valueRgb;
    private final List<Bitmap> lines = new ArrayList<>();
    private Bitmap valueBitmap;

    private final List<Bitmap> valueLines = new ArrayList<>();
    private int linesFor = -1;
    private final Paint pixels = new Paint(), pressed = new Paint();
    private final RectF where = new RectF();

    boolean wrapWidth;

    int gain = 100;

    private final int gap;

    D2GameText(Context context, D2SettingsLook look, D2GameFont font, int rgb, String text) {
        super(context);
        this.look = look;
        this.font = font;
        this.rgb = rgb;
        this.text = text == null ? "" : text;
        gap = font == look.formal10 ? 1 : 3;
        pixels.setFilterBitmap(false);
        pressed.setColor(0x30C8B074);
        describe();
    }


    private void describe() {
        setContentDescription(value == null ? text : text + ", " + value);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }


    void setValue(String value, int rgb) {
        this.value = value;
        valueRgb = rgb;
        valueBitmap = value == null ? null : D2SettingsLook.text(look.font16, value, rgb);
        linesFor = -1;
        describe();
        requestLayout();
        invalidate();
    }

    void setText(String text) {
        this.text = text == null ? "" : text;
        linesFor = -1;
        describe();
        requestLayout();
        invalidate();
    }

    private void wrap(int width) {
        if (linesFor == width) return;
        linesFor = width;
        lines.clear();
        valueLines.clear();
        int k = look.scale;
        float all = Math.max(40, width / (float) k);

        boolean beside = valueBitmap == null || valueBitmap.getWidth() + 12 <= all / 2;
        float room = beside ? Math.max(40, all - (valueBitmap != null ? valueBitmap.getWidth() + 12 : 0)) : all;
        for (String line : font.wrap(text, room)) lines.add(D2SettingsLook.text(font, line, rgb, gain));
        if (!beside) for (String line : look.font16.wrap(value, all - 12)) valueLines.add(D2SettingsLook.text(look.font16, line, valueRgb));
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED) width = (font.width(text) + 8) * look.scale;
        else if (wrapWidth) width = Math.min(width, (font.width(text) + 2) * look.scale + getPaddingLeft() + getPaddingRight());
        wrap(width - getPaddingLeft() - getPaddingRight());
        int k = look.scale, h = Math.max(1, lines.size()) * (font.height + gap) * k + getPaddingTop() + getPaddingBottom()
            + valueLines.size() * (look.font16.height + gap) * k;
        setMeasuredDimension(width, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int k = look.scale;
        wrap(getWidth() - getPaddingLeft() - getPaddingRight());
        if (isPressed() && isClickable() && isEnabled()) canvas.drawRect(0, 0, getWidth(), getHeight(), pressed);
        float y = getPaddingTop();
        for (Bitmap b : lines) {
            where.set(getPaddingLeft(), y, getPaddingLeft() + b.getWidth() * k, y + b.getHeight() * k);
            canvas.drawBitmap(b, null, where, pixels);
            y += (font.height + gap) * k;
        }
        if (!valueLines.isEmpty()) {
            float right = getWidth() - getPaddingRight();
            for (Bitmap b : valueLines) {
                where.set(right - b.getWidth() * k, y, right, y + b.getHeight() * k);
                canvas.drawBitmap(b, null, where, pixels);
                y += (look.font16.height + gap) * k;
            }
        }
        else if (valueBitmap != null) {
            float right = getWidth() - getPaddingRight(), top = getPaddingTop() + (font.height - look.font16.height) * k / 2f;
            where.set(right - valueBitmap.getWidth() * k, top, right, top + valueBitmap.getHeight() * k);
            canvas.drawBitmap(valueBitmap, null, where, pixels);
        }
    }


    @SuppressLint("ViewConstructor")
    static final class Stone extends View {
        private final D2SettingsLook look;
        private final Bitmap stone, label;
        private final Paint pixels = new Paint();
        private final RectF where = new RectF();

        Stone(Context context, D2SettingsLook look, boolean wide, String text, Runnable action) {
            super(context);
            this.look = look;
            stone = look.wideStone;
            label = D2SettingsLook.text(look.exocet10, text.toUpperCase(java.util.Locale.ROOT), 0);
            pixels.setFilterBitmap(false);
            setClickable(true);
            setOnClickListener((v) -> action.run());
            D2PadFocus.frame(this);
            setContentDescription(text);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            invalidate();
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            setMeasuredDimension(stone.getWidth() * look.scale, stone.getHeight() * look.scale);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int k = look.scale, shift = isPressed() ? k : 0;
            pixels.setAlpha(isEnabled() ? 255 : 115);
            where.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(stone, null, where, pixels);
            float w = label.getWidth() * k, h = label.getHeight() * k, x = (getWidth() - w) / 2f - shift, y = (getHeight() - h) / 2f + k + shift;
            where.set(x, y, x + w, y + h);
            canvas.drawBitmap(label, null, where, pixels);
        }
    }
}
