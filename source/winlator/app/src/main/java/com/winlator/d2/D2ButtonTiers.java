package com.winlator.d2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;








final class D2ButtonTiers {
    static final int MAIN = 1, PLAIN = 2, TEXT = 3;
    private static final int GOLD = 0xC8B074;

    private D2ButtonTiers() {}


    static View make(Context context, D2SettingsLook look, int kind, String text, Runnable action) {
        if (kind == TEXT) return TapText.make(context, look, text, action);

        String label = text.replace(" …", "").replace(" ...", "");
        if (kind == PLAIN && look.plainUp != null) return new PlainStone(context, look, label, action);
        return new D2GameText.Stone(context, look, true, label, action);
    }








    static void dialog(android.app.Dialog dialog, Button positive, Button negative) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look == null || positive == null || negative == null || !(positive.getParent() instanceof ViewGroup)) return;

        if (positive.getVisibility() != View.VISIBLE || negative.getVisibility() != View.VISIBLE) return;
        ViewGroup bar = (ViewGroup) positive.getParent();
        Context context = positive.getContext();
        android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        int need = look.wideStone.getWidth() * look.scale + Math.round(64 * metrics.density);
        if (need > metrics.widthPixels * 0.95f) return;

        String label = positive.getText().toString().replace(" …", "").replace(" ...", "").toUpperCase(java.util.Locale.ROOT);
        if (look.exocet10.width(label) > look.wideStone.getWidth() - 2 * D2StoneButton.EDGE) return;
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            int width = window.getAttributes().width;
            if (width > 0 && width < need) window.setLayout(need, window.getAttributes().height);
        }
        View cancel = make(context, look, TEXT, negative.getText().toString(), negative::performClick);
        View confirm = make(context, look, MAIN, positive.getText().toString(), positive::performClick);
        confirm.setEnabled(positive.isEnabled());
        Row row = new Row(context, cancel, confirm);
        positive.setVisibility(View.GONE);
        negative.setVisibility(View.GONE);
        negative.setTag(cancel);
        for (int i = 0; i < bar.getChildCount(); i++) {
            View child = bar.getChildAt(i);
            if (!(child instanceof Button)) child.setVisibility(View.GONE);
        }
        bar.addView(row, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }





    @SuppressLint("ViewConstructor")
    private static final class Row extends LinearLayout {
        private final View cancel, confirm;
        private boolean stacked;

        Row(Context context, View cancel, View confirm) {
            super(context);
            this.cancel = cancel;
            this.confirm = confirm;
            int p = Math.round(8 * context.getResources().getDisplayMetrics().density);
            setPadding(p, 0, p, p);
            arrange(false);
        }

        private void arrange(boolean stack) {
            stacked = stack;
            removeAllViews();
            setOrientation(stack ? VERTICAL : HORIZONTAL);
            setGravity(stack ? Gravity.CENTER_HORIZONTAL : Gravity.END | Gravity.CENTER_VERTICAL);
            addView(stack ? confirm : cancel);
            addView(stack ? cancel : confirm);
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            int room = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight();
            int need = 0;
            for (View v : new View[]{cancel, confirm}) {
                v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                need += v.getMeasuredWidth();
            }
            boolean stack = MeasureSpec.getMode(widthSpec) != MeasureSpec.UNSPECIFIED && need > room;
            if (stack != stacked) arrange(stack);
            super.onMeasure(widthSpec, heightSpec);
        }
    }





    @SuppressLint("ViewConstructor")
    static final class PlainStone extends View {
        private final D2SettingsLook look;
        private final Bitmap label;
        private Bitmap up, down;

        private final int ownTiles;
        private int tiles;
        private final Paint pixels = new Paint();
        private final RectF where = new RectF();

        PlainStone(Context context, D2SettingsLook look, String text, Runnable action) {
            super(context);
            this.look = look;
            label = D2SettingsLook.text(look.exocet10, text.toUpperCase(java.util.Locale.ROOT), D2GameFont.STONE_RGB);
            ownTiles = D2StoneButton.plainTiles(label.getWidth());
            widen(ownTiles);
            pixels.setFilterBitmap(false);
            setClickable(true);
            setOnClickListener((v) -> action.run());
            D2PadFocus.frame(this);
            setContentDescription(text);
        }

        private void widen(int tiles) {
            if (tiles == this.tiles) return;
            this.tiles = tiles;
            int w = D2StoneButton.plainWidth(tiles), h = D2StoneButton.PLAIN_SQUARE;
            up = Bitmap.createBitmap(D2StoneButton.widen(look.plainUp, h, tiles), w, h, Bitmap.Config.ARGB_8888);
            down = Bitmap.createBitmap(D2StoneButton.widen(look.plainDown, h, tiles), w, h, Bitmap.Config.ARGB_8888);
            requestLayout();
            invalidate();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            post(() -> even(getRootView()));
        }


        private static void even(View root) {
            java.util.List<PlainStone> all = new java.util.ArrayList<>();
            collect(root, all);
            int most = 0;
            for (PlainStone s : all) most = Math.max(most, s.ownTiles);
            for (PlainStone s : all) {
                s.widen(most);
                ViewGroup.LayoutParams lp = s.getLayoutParams();
                if (lp instanceof LinearLayout.LayoutParams && ((LinearLayout.LayoutParams) lp).gravity != Gravity.CENTER_HORIZONTAL) {
                    ((LinearLayout.LayoutParams) lp).gravity = Gravity.CENTER_HORIZONTAL;
                    s.setLayoutParams(lp);
                }
                else if (lp instanceof android.widget.FrameLayout.LayoutParams
                    && ((android.widget.FrameLayout.LayoutParams) lp).gravity != Gravity.CENTER_HORIZONTAL) {
                    ((android.widget.FrameLayout.LayoutParams) lp).gravity = Gravity.CENTER_HORIZONTAL;
                    s.setLayoutParams(lp);
                }
            }
        }

        private static void collect(View v, java.util.List<PlainStone> out) {
            if (v instanceof PlainStone) out.add((PlainStone) v);
            else if (v instanceof ViewGroup) {
                ViewGroup g = (ViewGroup) v;
                for (int i = 0; i < g.getChildCount(); i++) collect(g.getChildAt(i), out);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            invalidate();
        }

        @Override
        protected void onMeasure(int widthSpec, int heightSpec) {
            setMeasuredDimension(up.getWidth() * look.scale, up.getHeight() * look.scale);
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int k = look.scale;
            boolean pressed = isPressed() && isEnabled();
            pixels.setAlpha(isEnabled() ? 255 : 115);
            where.set(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(pressed ? down : up, null, where, pixels);
            int edge = D2StoneButton.PLAIN_PAD - D2StoneButton.EDGE;
            int ls = D2StoneButton.labelScale(label.getWidth(), up.getWidth() - 2 * edge, k);
            float w = label.getWidth() * ls, h = label.getHeight() * ls, shift = pressed ? k : 0;
            float x = Math.round((getWidth() - w) / 2f - shift), y = Math.round((getHeight() - h) / 2f + shift);
            where.set(x, y, x + w, y + h);
            canvas.drawBitmap(label, null, where, pixels);
        }
    }


    private static final class TapText {
        static D2GameText make(Context context, D2SettingsLook look, String text, Runnable action) {
            D2GameText view = new D2GameText(context, look, look.font16, GOLD, text);
            view.wrapWidth = true;
            float density = context.getResources().getDisplayMetrics().density;
            int side = Math.round(10 * density), tall = Math.round(48 * density);
            int rest = Math.max(Math.round(6 * density), (tall - (look.font16.height + 3) * look.scale) / 2);
            view.setPadding(side, rest, side, rest);
            view.setClickable(true);
            view.setOnClickListener((v) -> action.run());
            D2PadFocus.frame(view);
            return view;
        }
    }
}
