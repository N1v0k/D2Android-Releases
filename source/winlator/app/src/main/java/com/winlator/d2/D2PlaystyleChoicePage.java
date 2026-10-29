package com.winlator.d2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import java.util.function.Function;








@SuppressLint("ViewConstructor")
final class D2PlaystyleChoicePage extends FrameLayout {
    interface Host {

        void chosen(String preset);
    }

    private static final int MAX_COLUMN = D2PlaystyleChoice.MAX_COLUMN, BAND = D2PlaystyleChoice.BAND;

    private static final int TOP = 18;

    private final D2PlaystyleChoice.Art art;
    private final Host host;
    private final Function<String, String> strings;
    private final int scale;
    private final Paint pixels = new Paint();
    private String preset;
    private D2PlaystyleChoice.Drawn drawn;
    private Bitmap content, backdrop, stone;
    private boolean pressed;
    private final Content contentView;
    private final Band band;
    private final ScrollView scroll;

    D2PlaystyleChoicePage(Context context, D2PlaystyleChoice.Art art, String preselected, Host host) {
        super(context);
        this.art = art;
        this.host = host;
        this.preset = D2Spielweise.preset(preselected);
        scale = Math.max(1, Math.round(context.getResources().getDisplayMetrics().density));
        pixels.setFilterBitmap(false);
        strings = (name) -> {
            int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
            return id != 0 ? context.getString(id) : null;
        };
        setBackgroundColor(Color.BLACK);
        View back = new View(context) {
            @Override protected void onDraw(Canvas canvas) {
                if (backdrop != null) canvas.drawBitmap(backdrop, null, new Rect(0, 0, getWidth(), getHeight()), pixels);
            }
        };
        addView(back, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        contentView = new Content(context);
        scroll.addView(contentView);
        LayoutParams sp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        sp.bottomMargin = BAND * scale;
        sp.topMargin = TOP * scale;
        addView(scroll, sp);
        band = new Band(context);
        addView(band, new LayoutParams(LayoutParams.MATCH_PARENT, BAND * scale, android.view.Gravity.BOTTOM));
    }


    private int columnWidth() {
        return Math.min(MAX_COLUMN, Math.max(1, getWidth() / scale));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w <= 0 || h <= 0) return;
        int gw = Math.max(1, w / scale), gh = Math.max(1, h / scale);
        backdrop = Bitmap.createBitmap(D2PlaystyleChoice.backdrop(art, gw, gh), gw, gh, Bitmap.Config.ARGB_8888);
        redraw();
    }

    private void redraw() {
        if (getWidth() <= 0) return;
        drawn = D2PlaystyleChoice.draw(art, strings, columnWidth(), preset);
        content = Bitmap.createBitmap(drawn.img.argb, drawn.img.width, drawn.img.height, Bitmap.Config.ARGB_8888);
        D2GameFont.Img b = D2PlaystyleChoice.button(art, strings, preset, pressed);
        stone = Bitmap.createBitmap(b.argb, b.width, b.height, Bitmap.Config.ARGB_8888);

        contentView.post(contentView::requestLayout);
        contentView.invalidate();
        band.invalidate();
        invalidate();
    }


    void toggle() {
        choose(D2Spielweise.nextPreset(preset));
    }


    View padFirstFocus() {
        return band;
    }

    private void choose(String p) {
        if (p == null || p.equals(preset)) return;
        preset = p;
        D2Trace.log("playstyle choice: " + p + " tapped");
        redraw();
    }


    private final class Content extends View {
        Content(Context context) {
            super(context);
            setClickable(true);
            setFocusable(true);
        }

        @Override protected void onFocusChanged(boolean gained, int direction, Rect previous) {
            super.onFocusChanged(gained, direction, previous);
            invalidate();
        }

        private int left() {
            return (getWidth() - columnWidth() * scale) / 2;
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int w = MeasureSpec.getSize(widthSpec);
            int h = drawn != null ? drawn.img.height * scale : 0;
            setMeasuredDimension(w, h);
        }

        @Override protected void onDraw(Canvas canvas) {
            if (content == null) return;
            int x = left();
            canvas.drawBitmap(content, null, new Rect(x, 0, x + content.getWidth() * scale, content.getHeight() * scale), pixels);
            if (isFocused() && !isInTouchMode() && drawn != null) {
                int[] card = D2Spielweise.CLASSIC.equals(preset) ? drawn.classicCard : drawn.enhancedCard;
                focus.setColor(0xFFC8B074);
                focus.setStyle(Paint.Style.STROKE);
                focus.setStrokeWidth(scale);
                int l = x + (D2PlaystyleChoice.MARGIN - 3) * scale, r = x + (columnWidth() - D2PlaystyleChoice.MARGIN + 3) * scale;
                canvas.drawRect(l, (card[0] - 3) * scale, r, (card[0] + card[1] + 3) * scale, focus);
            }
        }

        private final Paint focus = new Paint();

        @SuppressLint("ClickableViewAccessibility")
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP && drawn != null) {
                float x = e.getX() - left();

                if (x >= 0 && x < columnWidth() * scale) choose(drawn.presetAt((int) (e.getY() / scale)));
            }
            return true;
        }

        @Override public boolean performClick() {
            super.performClick();
            toggle();
            return true;
        }
    }


    private final class Band extends View {
        private final Paint fill = new Paint();

        Band(Context context) {
            super(context);
            setClickable(true);
            setFocusable(true);
        }

        @Override protected void onFocusChanged(boolean gained, int direction, Rect previous) {
            super.onFocusChanged(gained, direction, previous);
            invalidate();
        }

        private Rect stoneRect() {
            if (stone == null) return new Rect();
            int w = stone.getWidth() * scale, h = stone.getHeight() * scale;
            int x = (getWidth() - w) / 2, y = getHeight() - (BAND - 9) * scale + ((BAND - 9) * scale - h) / 2;
            return new Rect(x, y, x + w, y + h);
        }

        @Override protected void onDraw(Canvas canvas) {
            fill.setColor(0xC0000000);
            canvas.drawRect(0, scale, getWidth(), getHeight(), fill);
            int cw = (columnWidth() - 2 * D2PlaystyleChoice.MARGIN) * scale, x = (getWidth() - cw) / 2;
            fill.setColor(0xFF8C7846);
            canvas.drawRect(x, 0, x + cw, scale, fill);
            if (stone != null) canvas.drawBitmap(stone, null, stoneRect(), pixels);
            if (isFocused() && !isInTouchMode()) {
                fill.setColor(0xFFC8B074);
                Rect r = stoneRect();
                r.inset(-2 * scale, -2 * scale);
                canvas.drawRect(r.left, r.top, r.right, r.top + scale, fill);
                canvas.drawRect(r.left, r.bottom - scale, r.right, r.bottom, fill);
                canvas.drawRect(r.left, r.top, r.left + scale, r.bottom, fill);
                canvas.drawRect(r.right - scale, r.top, r.right, r.bottom, fill);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override public boolean onTouchEvent(MotionEvent e) {
            boolean in = stoneRect().contains((int) e.getX(), (int) e.getY());
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setPressedStone(in);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    setPressedStone(in && pressed);
                    return true;
                case MotionEvent.ACTION_UP:
                    boolean go = pressed && in;
                    setPressedStone(false);
                    if (go) performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    setPressedStone(false);
                    return true;
                default:
                    return true;
            }
        }


        @Override public boolean performClick() {
            super.performClick();
            D2Trace.log("playstyle choice: " + preset + " taken");
            host.chosen(preset);
            return true;
        }

        private void setPressedStone(boolean on) {
            if (pressed == on) return;
            pressed = on;
            D2GameFont.Img b = D2PlaystyleChoice.button(art, strings, preset, pressed);
            stone = Bitmap.createBitmap(b.argb, b.width, b.height, Bitmap.Config.ARGB_8888);
            invalidate();
        }
    }
}
