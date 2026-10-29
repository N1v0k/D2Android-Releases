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
final class D2TelemetryChoicePage extends FrameLayout {
    interface Host {

        void answered(boolean yes);
    }

    private static final int TOP = 18, BAND = D2TelemetryChoice.BAND;

    private final D2PlaystyleChoice.Art art;
    private final Host host;
    private final Function<String, String> strings;
    private final int scale;
    private final Paint pixels = new Paint();
    private Bitmap content, backdrop;
    private final View contentView;
    private final Answer yes, no;

    D2TelemetryChoicePage(Context context, D2PlaystyleChoice.Art art, Host host) {
        super(context);
        this.art = art;
        this.host = host;
        scale = Math.max(1, Math.round(context.getResources().getDisplayMetrics().density));
        pixels.setFilterBitmap(false);
        strings = (name) -> {
            int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
            return id != 0 ? context.getString(id) : "";
        };
        setBackgroundColor(Color.BLACK);
        View back = new View(context) {
            @Override protected void onDraw(Canvas canvas) {
                if (backdrop != null) canvas.drawBitmap(backdrop, null, new Rect(0, 0, getWidth(), getHeight()), pixels);
            }
        };
        addView(back, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        contentView = new View(context) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                setMeasuredDimension(MeasureSpec.getSize(widthSpec), content != null ? content.getHeight() * scale : 0);
            }

            @Override protected void onDraw(Canvas canvas) {
                if (content == null) return;
                int x = (getWidth() - content.getWidth() * scale) / 2;
                canvas.drawBitmap(content, null, new Rect(x, 0, x + content.getWidth() * scale, content.getHeight() * scale), pixels);
            }
        };
        scroll.addView(contentView);
        LayoutParams sp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        sp.bottomMargin = BAND * scale;
        sp.topMargin = TOP * scale;
        addView(scroll, sp);
        View band = new View(context) {
            private final Paint fill = new Paint();

            @Override protected void onDraw(Canvas canvas) {
                fill.setColor(0xC0000000);
                canvas.drawRect(0, scale, getWidth(), getHeight(), fill);
                int cw = (columnWidth() - 2 * D2PlaystyleChoice.MARGIN) * scale, x = (getWidth() - cw) / 2;
                fill.setColor(0xFF8C7846);
                canvas.drawRect(x, 0, x + cw, scale, fill);
            }
        };
        addView(band, new LayoutParams(LayoutParams.MATCH_PARENT, BAND * scale, android.view.Gravity.BOTTOM));
        yes = new Answer(context, true);
        no = new Answer(context, false);
        addView(yes);
        addView(no);
    }

    private int columnWidth() {
        return Math.min(D2PlaystyleChoice.MAX_COLUMN, Math.max(1, getWidth() / scale));
    }


    View padFirstFocus() {
        return yes;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w <= 0 || h <= 0) return;
        int gw = Math.max(1, w / scale), gh = Math.max(1, h / scale);
        backdrop = Bitmap.createBitmap(D2PlaystyleChoice.backdrop(art, gw, gh), gw, gh, Bitmap.Config.ARGB_8888);
        D2GameFont.Img c = D2TelemetryChoice.draw(art, strings, columnWidth());
        content = Bitmap.createBitmap(c.argb, c.width, c.height, Bitmap.Config.ARGB_8888);
        D2GameFont.Img stone = D2TelemetryChoice.yes(art, strings, false);
        place(yes, D2TelemetryChoice.yesPlace(stone, gw), h);
        place(no, D2TelemetryChoice.noPlace(stone, gw), h);

        post(() -> {
            contentView.requestLayout();
            yes.requestLayout();
            no.requestLayout();
        });
        invalidate();
    }


    private void place(Answer a, int[] r, int h) {
        LayoutParams lp = new LayoutParams(r[2] * scale, r[3] * scale);
        lp.leftMargin = r[0] * scale;
        lp.topMargin = h - BAND * scale + r[1] * scale;
        a.setLayoutParams(lp);
        a.redraw();
    }


    private final class Answer extends View {
        private final boolean isYes;
        private final Paint focus = new Paint();
        private Bitmap img;
        private boolean pressed;

        Answer(Context context, boolean isYes) {
            super(context);
            this.isYes = isYes;
            setClickable(true);
            setFocusable(true);
            setContentDescription(strings.apply(isYes ? "telemetry_yes" : "telemetry_no"));

            setOnClickListener((v) -> {
                D2Trace.log("statistics question: " + (isYes ? "yes" : "no"));
                host.answered(isYes);
            });
        }

        void redraw() {
            D2GameFont.Img i = isYes ? D2TelemetryChoice.yes(art, strings, pressed) : D2TelemetryChoice.no(art, strings, pressed);
            img = Bitmap.createBitmap(i.argb, i.width, i.height, Bitmap.Config.ARGB_8888);
            invalidate();
        }

        @Override protected void onFocusChanged(boolean gained, int direction, Rect previous) {
            super.onFocusChanged(gained, direction, previous);
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            if (img == null) return;
            int w = img.getWidth() * scale, h = img.getHeight() * scale;
            int x = (getWidth() - w) / 2, y = (getHeight() - h) / 2;
            canvas.drawBitmap(img, null, new Rect(x, y, x + w, y + h), pixels);
            if (isFocused() && !isInTouchMode()) {
                focus.setColor(0xFFC8B074);
                focus.setStyle(Paint.Style.STROKE);
                focus.setStrokeWidth(scale);
                canvas.drawRect(scale / 2f, scale / 2f, getWidth() - scale / 2f, getHeight() - scale / 2f, focus);
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override public boolean onTouchEvent(MotionEvent e) {
            boolean in = e.getX() >= 0 && e.getY() >= 0 && e.getX() < getWidth() && e.getY() < getHeight();
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setPressedImg(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    setPressedImg(in && pressed);
                    return true;
                case MotionEvent.ACTION_UP:
                    boolean go = pressed && in;
                    setPressedImg(false);
                    if (go) performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    setPressedImg(false);
                    return true;
                default:
                    return true;
            }
        }

        private void setPressedImg(boolean on) {
            if (pressed == on) return;
            pressed = on;
            redraw();
        }
    }
}
