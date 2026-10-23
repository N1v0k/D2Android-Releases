package com.winlator.d2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.winlator.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;










@SuppressLint("ViewConstructor")
final class D2StartPage extends FrameLayout {
    interface Host {
        void start(D2SaveHeader.Info character, int difficulty);
        void longPress(D2SaveHeader.Info character);

        void refused(D2SaveHeader.Info character);

        void button(int which);
    }


    static final class Figure {
        final Bitmap[] frames;
        final int frameMs;

        Figure(Bitmap[] frames, int frameMs) {
            this.frames = frames;
            this.frameMs = frameMs;
        }
    }

    private static final long LOGO_MS = 50;
    private final D2StartArt art;
    private final Host host;
    private final List<D2SaveHeader.Info> characters;
    private final Map<String, Figure> figures = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint pixels = new Paint(), smooth = new Paint(Paint.FILTER_BITMAP_FLAG), add = new Paint();
    private final Rect visible = new Rect();
    private D2StartLayout layout;
    private Bitmap backdrop, hint, count, footer, classic, button, buttonPressed;

    private final View classicTap, licencesTap;

    private final Bitmap[] cardFrames = new Bitmap[3];
    private Bitmap[] blackLeft, blackRight, fireLeft, fireRight;
    private final Backdrop backdropView;
    private final Logo logoView;
    private final ScrollView scroll;
    private final LinearLayout rows;
    private Overlay overlay;
    private final List<Stone> stones = new ArrayList<>();
    private String status = "";
    private List<Bitmap> statusLines = new ArrayList<>();

    private D2StartLayout.Status statusPlace;
    private boolean running;

    D2StartPage(Context context, D2StartArt art, List<D2SaveHeader.Info> characters, Host host) {
        super(context);
        this.art = art;
        this.host = host;
        this.characters = characters;
        setBackgroundColor(Color.BLACK);
        pixels.setFilterBitmap(false);
        add.setFilterBitmap(true);
        add.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
        blackLeft = bitmaps(art.blackLeft);
        blackRight = bitmaps(art.blackRight);
        fireLeft = bitmaps(art.fireLeft);
        fireRight = bitmaps(art.fireRight);
        button = bitmap(art.button);
        buttonPressed = bitmap(art.buttonPressed);


        int hintText = characters.isEmpty() ? R.string.start_hint_empty
            : D2CharacterList.anyStartable(characters) ? R.string.start_hint : R.string.start_hint_none;
        hint = bitmap(art.formal10.render(context.getString(hintText), 0xA0A0A0));

        classic = bitmap(art.formal10.render(context.getString(R.string.start_classic), 0xA0A0A0));
        footer = bitmap(art.formal10.render(context.getString(R.string.start_trademark) + "   " + context.getString(R.string.start_licenses), 0x808080));
        backdropView = new Backdrop(context);
        addView(backdropView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        logoView = new Logo(context);
        addView(logoView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        scroll.setOnScrollChangeListener((v, x, y, ox, oy) -> {
            if (D2StartPage.this.overlay != null) D2StartPage.this.overlay.invalidate();
        });
        addView(scroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0));
        overlay = new Overlay(context);
        addView(overlay, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        int[] labels = {R.string.start_new_character, R.string.start_settings, R.string.settings_saves, R.string.news_title,
            R.string.settings_background};
        for (int i = 0; i < labels.length; i++) {
            Stone stone = new Stone(context, i, bitmap(art.exocet10.render(context.getString(labels[i]), 0)));
            stones.add(stone);
            addView(stone, new LayoutParams(0, 0));
        }
        classicTap = new View(context);
        classicTap.setOnClickListener((v) -> host.button(6));
        addView(classicTap, new LayoutParams(0, 0));
        licencesTap = new View(context);
        licencesTap.setOnClickListener((v) -> host.button(5));
        addView(licencesTap, new LayoutParams(0, 0));
        for (D2SaveHeader.Info c : characters) rows.addView(new Card(context, c, c == characters.get(0)));
    }

    private static Bitmap bitmap(D2GameFont.Img img) {
        return Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888);
    }

    private static Bitmap[] bitmaps(D2Sprites.Frame[] frames) {
        Bitmap[] out = new Bitmap[frames.length];
        for (int i = 0; i < frames.length; i++) out[i] = Bitmap.createBitmap(frames[i].argb, frames[i].width, frames[i].height, Bitmap.Config.ARGB_8888);
        return out;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        layout = new D2StartLayout(w, h);
        int bh = layout.backdropHeight;
        backdrop = Bitmap.createBitmap(art.backdrop(bh), D2StartArt.SCREEN_W, bh, Bitmap.Config.ARGB_8888);
        java.util.Arrays.fill(cardFrames, null);
        updateCount();
        setStatus(status);
        post(this::place);
    }


    private void place() {
        D2StartLayout l = layout;
        if (l == null) return;
        LayoutParams sp = (LayoutParams) scroll.getLayoutParams();
        sp.topMargin = Math.round(l.listTop);
        sp.height = Math.round(l.listBottom - l.listTop);
        scroll.setLayoutParams(sp);
        for (int i = 0; i < rows.getChildCount(); i++) {
            View card = rows.getChildAt(i);
            card.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.round(l.rowHeight)));
        }
        for (Stone stone : stones) {
            LayoutParams p = (LayoutParams) stone.getLayoutParams();
            p.width = Math.round(l.buttonWidth);
            p.height = Math.round(l.buttonHeight);
            p.leftMargin = Math.round(l.width / 2f - l.buttonWidth / 2);
            p.topMargin = Math.round(l.buttonTop(stone.index));
            stone.setLayoutParams(p);
        }
        LayoutParams cp = (LayoutParams) classicTap.getLayoutParams();
        cp.width = Math.round(l.width / 2f);
        cp.height = Math.round(34 * l.u);
        cp.leftMargin = Math.round(l.width / 4f);
        cp.topMargin = Math.round(l.classicY - 8 * l.u);
        classicTap.setLayoutParams(cp);
        View licences = licencesTap;
        LayoutParams lp = (LayoutParams) licences.getLayoutParams();
        lp.width = Math.round(l.width / 2f);
        lp.height = Math.round(34 * l.u);
        lp.leftMargin = Math.round(l.width / 2f);
        lp.topMargin = Math.round(l.footerY - 10 * l.u);
        licences.setLayoutParams(lp);
    }


    private Bitmap cardFrame(int kind, D2StartLayout l, float w, float h) {
        if (art.cardFrame == null) return null;
        Bitmap b = cardFrames[kind];
        if (b == null) {

            int fw = Math.max(2 * D2StartArt.CARD_FRAME_EDGE + 1, Math.round(w / l.scale));
            int fh = Math.max(2 * D2StartArt.CARD_FRAME_EDGE + 1, Math.round(h / l.scale));
            int[] px = D2StartArt.frame(art.cardFrame, D2StartArt.CARD_FRAME_EDGE, fw, fh);
            for (int i = 0; i < px.length; i++) px[i] = D2StartArt.tint(px[i], kind);
            b = cardFrames[kind] = Bitmap.createBitmap(px, fw, fh, Bitmap.Config.ARGB_8888);
        }
        return b;
    }

    private void updateCount() {
        D2StartLayout l = layout;
        boolean more = l != null && characters.size() * l.rowHeight > l.listBottom - l.listTop;
        int n = characters.size();
        if (n == 0) {
            count = null;
            return;
        }
        String text = getResources().getQuantityString(more ? R.plurals.start_count_swipe : R.plurals.start_count, n, n);
        count = bitmap(art.formal10.render(text, 0xA0A0A0));
    }


    void setStatus(String text) {
        status = text == null ? "" : text;
        List<Bitmap> lines = new ArrayList<>();
        D2StartLayout l = layout;
        if (!status.isEmpty() && l != null) {


            statusPlace = l.status(art.formal10, status);
            for (String line : statusPlace.lines) lines.add(bitmap(art.formal10.render(line, 0xC8B074)));
        }
        statusLines = lines;
        backdropView.invalidate();
    }


    void setFigure(String file, Figure figure) {
        figures.put(file, figure);
        for (int i = 0; i < rows.getChildCount(); i++) {
            Card card = (Card) rows.getChildAt(i);
            if (card.character.file.equals(file)) card.invalidate();
        }
    }


    void setRunning(boolean on) {
        running = on;
        handler.removeCallbacksAndMessages(null);
        if (on) handler.post(tick);
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            logoView.invalidate();
            for (int i = 0; i < rows.getChildCount(); i++) {
                View card = rows.getChildAt(i);

                if (card.getLocalVisibleRect(visible)) card.invalidate();
            }
            handler.postDelayed(this, LOGO_MS);
        }
    };

    @Override
    protected void onDetachedFromWindow() {
        setRunning(false);
        super.onDetachedFromWindow();
    }

    private static int frame(long ms, int count) {
        return count <= 1 ? 0 : (int) ((SystemClock.uptimeMillis() / Math.max(1, ms)) % count);
    }


    private final class Backdrop extends View {
        Backdrop(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            D2StartLayout l = layout;
            if (l == null || backdrop == null) return;
            canvas.save();
            canvas.scale(l.scale, l.scale);
            canvas.translate(-l.cutX, 0);
            canvas.drawBitmap(backdrop, 0, 0, smooth);
            canvas.restore();
            float k = l.textScale;
            if (statusLines.isEmpty()) centred(canvas, hint, l.hintY, k);
            else {
                D2StartLayout.Status p = statusPlace;
                float y = p.top;
                for (Bitmap b : statusLines) {
                    centred(canvas, b, y, p.scale);
                    y += p.step;
                }
            }
            if (count != null) centred(canvas, count, l.countY, k);
            Paint line = new Paint();
            line.setColor(Color.rgb(110, 96, 60));
            canvas.drawRect(40 * l.u, l.lineY, l.width - 40 * l.u, l.lineY + Math.max(1, 2 * l.u), line);
            centred(canvas, classic, l.classicY, 1.6f * l.smallScale);
            centred(canvas, footer, l.footerY, 1.4f * l.smallScale);
        }

        private void centred(Canvas canvas, Bitmap b, float y, float k) {
            float w = b.getWidth() * k;
            canvas.drawBitmap(b, null, new RectF(getWidth() / 2f - w / 2, y, getWidth() / 2f + w / 2, y + b.getHeight() * k), smooth);
        }
    }


    private final class Logo extends View {
        Logo(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            D2StartLayout l = layout;
            if (l == null) return;
            int i = frame(LOGO_MS, fireLeft.length);
            canvas.save();
            canvas.scale(l.scale, l.scale);
            canvas.translate(-l.cutX, 0);
            draw(canvas, blackLeft, art.blackLeft, i, smooth);
            draw(canvas, blackRight, art.blackRight, i, smooth);
            draw(canvas, fireLeft, art.fireLeft, i, add);
            draw(canvas, fireRight, art.fireRight, i, add);
            canvas.restore();
        }

        private void draw(Canvas canvas, Bitmap[] b, D2Sprites.Frame[] f, int i, Paint paint) {
            int n = i % b.length;
            canvas.drawBitmap(b[n], D2StartArt.LOGO_X + f[n].offsetX, D2StartArt.LOGO_Y + f[n].offsetY - f[n].height, paint);
        }
    }


    private final class Overlay extends View {
        private final Paint fade = new Paint(), track = new Paint(), thumb = new Paint();

        Overlay(Context context) {
            super(context);
            track.setColor(Color.argb(220, 40, 34, 20));
            thumb.setColor(Color.rgb(200, 176, 116));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            D2StartLayout l = layout;
            if (l == null) return;
            float total = characters.size() * l.rowHeight, view = l.listBottom - l.listTop;
            if (total <= view) return;
            float edge = 90 * l.u;
            fade.setShader(new LinearGradient(0, l.listBottom - edge, 0, l.listBottom, 0, Color.argb(200, 0, 0, 0), Shader.TileMode.CLAMP));
            canvas.drawRect(0, l.listBottom - edge, l.width, l.listBottom, fade);
            canvas.drawRect(l.scrollX - 4 * l.u, l.listTop, l.scrollX + 4 * l.u, l.listBottom, track);
            float h = view * view / total, top = l.listTop + (view - h) * scroll.getScrollY() / Math.max(1, total - view);
            canvas.drawRect(l.scrollX - 6 * l.u, top, l.scrollX + 6 * l.u, top + h, thumb);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }
    }


    private final class Card extends View {
        final D2SaveHeader.Info character;
        private final boolean last;
        private final Bitmap name, when, classLine, whereLine;
        private final Paint fill = new Paint(), frame = new Paint();

        Card(Context context, D2SaveHeader.Info c, boolean last) {
            super(context);
            character = c;
            this.last = last;
            boolean hc = c.hardcore();
            name = bitmap(art.font16.render(c.name.isEmpty() ? c.file : c.name, hc ? 0xD03030 : 0xC8B074));
            when = bitmap(art.formal10.render(ago(context, System.currentTimeMillis(), c.modified), 0x909090));
            classLine = bitmap(art.font16.render(context.getString(R.string.start_class_level, className(context, c.characterClass), c.level), 0xF0F0F0));
            String where = context.getString(R.string.start_where, difficultyName(context, c.defaultDifficulty()), c.lastAct + 1);

            String level = art.levels.get(c.file);
            if (level != null) where = context.getString(R.string.start_where_level, difficultyName(context, c.defaultDifficulty()), level);
            if (hc) where += "   " + context.getString(R.string.launch_character_hardcore);
            whereLine = bitmap(art.font16.render(where, hc ? 0xD03030 : 0x909090));
            fill.setColor(Color.argb(175, 0, 0, 0));
            frame.setStyle(Paint.Style.STROKE);
            frame.setColor(hc ? Color.rgb(170, 30, 30) : last ? Color.rgb(200, 176, 116) : Color.rgb(110, 96, 60));
            boolean startable = c.refusal() == D2SaveHeader.Refusal.NONE;
            if (!startable) setAlpha(0.5f);
            setOnClickListener((v) -> {
                if (startable) host.start(c, c.defaultDifficulty());
                else host.refused(c);
            });
            setOnLongClickListener((v) -> {
                if (startable) host.longPress(c);
                else host.refused(c);
                return true;
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            D2StartLayout l = layout;
            if (l == null) return;
            boolean strong = character.hardcore() || last;
            RectF card = new RectF(l.cardLeft, 0, l.cardRight, l.cardHeight);
            canvas.drawRect(card, fill);

            Bitmap gold = cardFrame(character.hardcore() ? D2StartArt.FRAME_HARDCORE : last ? D2StartArt.FRAME_LAST
                : D2StartArt.FRAME_OTHER, l, card.width(), card.height());
            if (gold != null) canvas.drawBitmap(gold, null, card, pixels);
            else {
                frame.setStrokeWidth((strong ? 3 : 2) * l.u);
                canvas.drawRect(card, frame);
            }
            Figure figure = figures.get(character.file);
            if (figure != null && figure.frames.length > 0) {
                Bitmap b = figure.frames[figure.frameMs > 0 ? frame(figure.frameMs, figure.frames.length) : 0];
                float k = (l.cardHeight - 16 * l.u) / b.getHeight(), w = b.getWidth() * k;
                canvas.drawBitmap(b, null, new RectF(l.figureX - w / 2, 8 * l.u, l.figureX + w / 2, l.cardHeight - 8 * l.u),
                    figure.frameMs > 0 ? pixels : smooth);
            }
            float k = l.textScale;
            text(canvas, name, l.textX, 22 * l.u, k);
            text(canvas, when, l.whenRight - when.getWidth() * k, 26 * l.u, k);
            text(canvas, classLine, l.textX, 78 * l.u, k);
            text(canvas, whereLine, l.textX, 130 * l.u, k);
        }

        private void text(Canvas canvas, Bitmap b, float x, float y, float k) {
            canvas.drawBitmap(b, null, new RectF(x, y, x + b.getWidth() * k, y + b.getHeight() * k), smooth);
        }
    }


    private final class Stone extends View {
        final int index;
        private final Bitmap label;

        private Bitmap sharp;
        private int sharpFactor;

        Stone(Context context, int index, Bitmap label) {
            super(context);
            this.index = index;
            this.label = label;
            setClickable(true);
            setOnClickListener((v) -> host.button(index));
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            D2StartLayout l = layout;
            if (l == null) return;
            canvas.drawBitmap(isPressed() ? buttonPressed : button, null, new RectF(0, 0, getWidth(), getHeight()), pixels);
            float k = l.textScale, w = label.getWidth() * k, h = label.getHeight() * k, shift = isPressed() ? l.textScale : 0;
            float x = getWidth() / 2f - w / 2 - shift, y = getHeight() / 2f - h / 2 + 2 * l.u + shift;
            int n = Math.max(1, (int) Math.ceil(k - 0.01f));
            if (sharp == null || sharpFactor != n) {
                sharp = n == 1 ? label : Bitmap.createScaledBitmap(label, label.getWidth() * n, label.getHeight() * n, false);
                sharpFactor = n;
            }
            canvas.drawBitmap(sharp, null, new RectF(x, y, x + w, y + h), smooth);
        }
    }



    static String className(Context context, int cls) {
        int[] names = {R.string.class_amazon, R.string.class_sorceress, R.string.class_necromancer, R.string.class_paladin,
            R.string.class_barbarian, R.string.class_druid, R.string.class_assassin};
        return cls >= 0 && cls < names.length ? context.getString(names[cls]) : "?";
    }

    static String difficultyName(Context context, int d) {
        int[] names = {R.string.difficulty_normal, R.string.difficulty_nightmare, R.string.difficulty_hell};
        return context.getString(names[Math.max(0, Math.min(2, d))]);
    }


    static String ago(Context context, long now, long then) {
        int[] a = D2StartTexts.ago(now, then);
        switch (a[0]) {
            case D2StartTexts.NOW: return context.getString(R.string.start_ago_now);
            case D2StartTexts.MINUTES: return context.getString(R.string.start_ago_minutes, a[1]);
            case D2StartTexts.HOURS: return context.getString(a[1] == 1 ? R.string.start_ago_hour : R.string.start_ago_hours, a[1]);
            case D2StartTexts.YESTERDAY: return context.getString(R.string.start_ago_yesterday);
            case D2StartTexts.DAYS: return context.getString(R.string.start_ago_days, a[1]);
            case D2StartTexts.WEEKS: return context.getString(a[1] == 1 ? R.string.start_ago_week : R.string.start_ago_weeks, a[1]);
            default: return context.getString(a[1] == 1 ? R.string.start_ago_month : R.string.start_ago_months, a[1]);
        }
    }
}
