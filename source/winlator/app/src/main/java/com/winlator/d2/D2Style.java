package com.winlator.d2;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;

public final class D2Style {
    public static final int BACKGROUND = 0xFF0F0C09;
    public static final int FRAME = 0xFF5A4A30;
    public static final int TEXT = 0xFFD2CEC6;
    public static final int HINT = 0xFF8F8A80;
    public static final int BUTTON = 0xFF221C14;
    public static final int GOLD = 0xFFC7B377;
    public static final int GREEN = 0xFF00FF00;
    public static final int RED = 0xFFFF4D4D;

    private static volatile D2Hud gameGraphics;

    private D2Style() {}

    public static void useGameFrame(D2Hud graphics) {
        gameGraphics = graphics;
    }

    public static Drawable windowBackground(float density) {
        D2Hud graphics = gameGraphics;
        if (graphics != null && graphics.hasFrame()) return graphics.frameDrawable(Math.max(1, density * 0.75f), BACKGROUND);
        GradientDrawable d = new GradientDrawable();
        d.setColor(BACKGROUND);
        d.setStroke(Math.max(1, Math.round(density)), FRAME);
        d.setCornerRadius(4 * density);
        return d;
    }

    public static void text(TextView view) {
        view.setTextColor(TEXT);
    }

    public static void hint(TextView view) {
        view.setTextColor(HINT);
    }

    public static void heading(TextView view) {
        view.setTextColor(GOLD);
        view.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
    }

    public static void control(CompoundButton button) {
        button.setTextColor(TEXT);
        button.setButtonTintList(tint());
    }

    public static void button(Button button, float density) {
        GradientDrawable ground = new GradientDrawable();
        ground.setColor(enabledOr(BUTTON, BACKGROUND));
        ground.setStroke(Math.max(1, Math.round(density)), enabledOr(FRAME, HINT & 0x60FFFFFF));
        ground.setCornerRadius(2 * density);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(FRAME), ground, null));
        buttonText(button, GOLD);
        button.setAllCaps(false);
        int h = Math.round(14 * density), v = Math.round(8 * density);
        button.setPadding(h, v, h, v);
        button.setStateListAnimator(null);
    }

    public static void buttonText(Button button, int color) {
        button.setTextColor(enabledOr(color, HINT));
    }

    private static ColorStateList enabledOr(int enabled, int disabled) {
        return new ColorStateList(new int[][]{{-android.R.attr.state_enabled}, {}}, new int[]{disabled, enabled});
    }

    public static void slider(SeekBar bar) {
        bar.setProgressTintList(ColorStateList.valueOf(GOLD));
        bar.setThumbTintList(ColorStateList.valueOf(GOLD));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(FRAME));
    }

    private static ColorStateList tint() {
        return new ColorStateList(
            new int[][]{{android.R.attr.state_checked}, {}},
            new int[]{GOLD, HINT});
    }
}
