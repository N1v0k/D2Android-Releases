package com.winlator.d2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.winlator.R;







@SuppressLint("ViewConstructor")
final class D2SoundButton extends View {
    private final Bitmap on = bitmap(D2StartArt.speaker(true)), off = bitmap(D2StartArt.speaker(false));
    private final Paint pixels = new Paint(), text = new Paint(Paint.ANTI_ALIAS_FLAG), pressed = new Paint();
    private final RectF where = new RectF();
    private final String label;
    private Bitmap labelOn, labelOff;
    private D2SettingsLook labelLook;

    Runnable onChange;

    private final int k, gap;

    D2SoundButton(Context context) {
        super(context);
        float density = context.getResources().getDisplayMetrics().density;
        k = Math.max(2, Math.round(density * 1.25f));
        gap = Math.round(5 * density);
        label = context.getString(R.string.sound_button);
        pixels.setFilterBitmap(false);
        text.setTextSize(13 * density);
        text.setColor(D2Style.GOLD);
        pressed.setColor(0x30C8B074);
        int pad = Math.round(6 * density);
        setPadding(pad, pad, pad, pad);
        setClickable(true);
        D2PadFocus.frame(this);
        setOnClickListener((v) -> {
            D2Settings.setSound(context, !D2Settings.sound(context));
            D2Music.soundChanged(context);
            update();
            if (onChange != null) onChange.run();
        });
        update();
    }

    private static Bitmap bitmap(D2GameFont.Img img) {
        return Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888);
    }


    void update() {

        setVisibility(GameLocator.gameDir(getContext()) != null ? VISIBLE : GONE);
        boolean sound = D2Settings.sound(getContext());
        setContentDescription(label + ", " + getContext().getString(sound ? R.string.settings_on : R.string.settings_off));
        requestLayout();
        invalidate();
    }

    private Bitmap labelBitmap(boolean sound) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look == null) return null;
        if (look != labelLook) {
            labelLook = look;
            labelOn = D2SettingsLook.text(look.formal10, label, 0xC8B074);
            labelOff = D2SettingsLook.text(look.formal10, label, 0x8C8C8C);
        }
        return sound ? labelOn : labelOff;
    }

    private float labelWidth(boolean sound) {
        Bitmap b = labelBitmap(sound);
        return b != null ? b.getWidth() * labelLook.scale : text.measureText(label);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        boolean sound = D2Settings.sound(getContext());
        int w = Math.round(on.getWidth() * k + gap + labelWidth(sound)) + getPaddingLeft() + getPaddingRight();
        int h = Math.max(on.getHeight() * k, Math.round(text.getTextSize() * 1.3f)) + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(w, h);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        boolean sound = D2Settings.sound(getContext());
        if (isPressed()) canvas.drawRect(0, 0, getWidth(), getHeight(), pressed);
        Bitmap icon = sound ? on : off;
        float cy = getHeight() / 2f, x = getPaddingLeft(), ih = icon.getHeight() * k;
        where.set(x, cy - ih / 2, x + icon.getWidth() * k, cy + ih / 2);
        canvas.drawBitmap(icon, null, where, pixels);
        x = where.right + gap;
        Bitmap word = labelBitmap(sound);
        if (word != null) {
            float s = labelLook.scale, wh = word.getHeight() * s;
            where.set(x, cy - wh / 2, x + word.getWidth() * s, cy + wh / 2);
            canvas.drawBitmap(word, null, where, pixels);
        }
        else {
            text.setColor(sound ? D2Style.GOLD : 0xFF8C8C8C);
            canvas.drawText(label, x, cy - (text.descent() + text.ascent()) / 2, text);
        }
    }
}
