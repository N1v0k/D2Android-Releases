package com.winlator.d2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.winlator.R;

import java.util.ArrayList;
import java.util.List;










@SuppressLint("ViewConstructor")
final class D2CharMenu extends FrameLayout {
    interface Actions {
        void start(int difficulty);
        void delete();
        void close();
    }

    private final D2StartArt art;
    private final D2StartLayout l;
    private final D2SaveHeader.Info character;
    private final Actions actions;
    private final Paint pixels = new Paint(), smooth = new Paint(Paint.FILTER_BITMAP_FLAG), fill = new Paint(), line = new Paint();
    private final Bitmap stone, stonePressed;
    private final List<Bitmap> texts = new ArrayList<>();
    private final List<float[]> textAt = new ArrayList<>();
    private final RectF box = new RectF();
    private Bitmap frame;
    private boolean confirming;
    private float lineY = -1;

    D2CharMenu(Context context, D2StartArt art, D2StartLayout layout, D2SaveHeader.Info character, Actions actions) {
        super(context);
        this.art = art;
        this.l = layout;
        this.character = character;
        this.actions = actions;
        setWillNotDraw(false);
        pixels.setFilterBitmap(false);
        fill.setColor(Color.argb(235, 0, 0, 0));
        line.setColor(Color.rgb(110, 96, 60));
        stone = bitmap(art.button);
        stonePressed = bitmap(art.buttonPressed);
        showMenu();
    }

    String characterFile() {
        return character.file;
    }

    boolean confirming() {
        return confirming;
    }


    View firstFocus() {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof Item && ((Item) v).first) return v;
        }
        return null;
    }



    private void showMenu() {
        confirming = false;
        removeAllViews();
        texts.clear();
        textAt.clear();
        Context c = getContext();
        float u = l.u, k = l.textScale, w = boxWidth();
        float x0 = (l.width - w) / 2, cx = l.width / 2f, y = 0;
        boolean hc = character.hardcore();
        y = text(art.font16.render(character.name.isEmpty() ? character.file : character.name, hc ? 0xD03030 : 0xC8B074), cx, y + 34 * u, k);
        y = text(art.formal10.render(c.getString(R.string.start_class_level, D2StartPage.className(c, character.characterClass),
            character.level), 0xA0A0A0), cx, y + 6 * u, k * 1.2f);
        y += 26 * u;
        D2SaveHeader.Refusal refusal = character.refusal();
        int open = refusal == D2SaveHeader.Refusal.NONE ? character.openDifficulty() : -1;
        int last = open >= 0 ? character.defaultDifficulty() : -1;
        for (int d = 0; d < 3; d++) {
            final int difficulty = d;
            boolean free = d <= open;
            Item item = new Item(c, art.exocet10.render(D2StartPage.difficultyName(c, d), D2GameFont.STONE_RGB), true, free, d == last,
                free ? () -> actions.start(difficulty) : null);
            add(item, cx - l.buttonWidth / 2, y, l.buttonWidth, l.buttonHeight);
            y += l.buttonHeight + 2 * u;
            String why = d == last ? c.getString(R.string.char_menu_last) : free ? null : lockedText(c, character, d);
            if (why != null) y = text(art.formal10.render(why, d == last ? 0xC8B074 : 0x909090), cx, y, k * 1.2f);
            else y += art.formal10.height * k * 1.2f;
            y += 12 * u;
        }
        lineY = y + 6 * u;
        y += 22 * u;
        float th = Math.max(art.font16.height * k + 24 * u, 48 * getResources().getDisplayMetrics().density);
        Item delete = new Item(c, art.font16.render(c.getString(R.string.char_menu_delete), 0xD03030), false, true, open < 0,
            actions::delete);
        add(delete, x0 + 20 * u, y, w - 40 * u, th);
        y += th;
        Item cancel = new Item(c, art.font16.render(c.getString(R.string.char_menu_cancel), 0xC8B074), false, true, false, actions::close);
        add(cancel, x0 + 20 * u, y, w - 40 * u, th);
        y += th + 20 * u;
        place(x0, w, y, D2StartArt.FRAME_LAST);
    }


    void showConfirm() {
        confirming = true;
        removeAllViews();
        texts.clear();
        textAt.clear();
        Context c = getContext();
        float u = l.u, k = l.textScale, w = boxWidth();
        float x0 = (l.width - w) / 2, cx = l.width / 2f, y = 0;
        String name = character.name.isEmpty() ? character.file : character.name;
        y = text(art.font16.render(c.getString(R.string.char_delete_title, name), 0xD03030), cx, y + 34 * u, k);
        y = text(art.formal10.render(c.getString(R.string.start_class_level, D2StartPage.className(c, character.characterClass),
            character.level), 0xA0A0A0), cx, y + 6 * u, k * 1.2f);
        y += 22 * u;
        for (int id : new int[]{R.string.char_delete_body1, R.string.char_delete_body2, R.string.char_delete_body3}) {
            y = text(art.formal10.render(c.getString(id), 0xC0C0C0), cx, y, k * 1.2f) + 6 * u;
        }
        y += 22 * u;
        lineY = -1;
        Item yes = new Item(c, art.exocet10.render(c.getString(R.string.char_delete_confirm), D2GameFont.STONE_RGB), true, true, false, actions::delete);
        add(yes, cx - l.buttonWidth / 2, y, l.buttonWidth, l.buttonHeight);
        y += l.buttonHeight + 8 * u;
        float th = Math.max(art.font16.height * k + 24 * u, 48 * getResources().getDisplayMetrics().density);
        Item cancel = new Item(c, art.font16.render(c.getString(R.string.char_menu_cancel), 0xC8B074), false, true, true, this::showMenuAgain);
        add(cancel, x0 + 20 * u, y, w - 40 * u, th);
        y += th + 20 * u;
        place(x0, w, y, D2StartArt.FRAME_HARDCORE);
        if (!isInTouchMode()) post(() -> {
            View f = firstFocus();
            if (f != null) f.requestFocus();
        });
    }

    private void showMenuAgain() {
        showMenu();
        if (!isInTouchMode()) post(() -> {
            View f = firstFocus();
            if (f != null) f.requestFocus();
        });
    }


    static String lockedText(Context c, D2SaveHeader.Info character, int difficulty) {
        D2SaveHeader.Refusal refusal = character.refusal();
        if (refusal == D2SaveHeader.Refusal.DEAD_HARDCORE) return difficulty == 0 ? c.getString(R.string.char_menu_dead) : null;
        if (refusal != D2SaveHeader.Refusal.NONE) return difficulty == 0 ? c.getString(R.string.char_menu_menu_only) : null;
        boolean lod = character.expansion();
        if (difficulty == 1) return c.getString(lod ? R.string.char_menu_locked_nightmare : R.string.char_menu_locked_nightmare_classic);
        return c.getString(lod ? R.string.char_menu_locked_hell : R.string.char_menu_locked_hell_classic);
    }

    private float boxWidth() {
        return Math.min(l.width - 24 * l.u, Math.max(l.buttonWidth + 160 * l.u, Math.min(l.cardRight - l.cardLeft, 720 * l.u)));
    }


    private void place(float x0, float w, float h, int kind) {
        float top = Math.max(12 * l.u, (l.height - h) / 2);
        box.set(x0, top, x0 + w, top + h);
        for (float[] t : textAt) t[1] += top;
        if (lineY >= 0) lineY += top;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            LayoutParams p = (LayoutParams) v.getLayoutParams();
            p.topMargin += Math.round(top);
            v.setLayoutParams(p);
        }
        frame = null;
        if (art.cardFrame != null) {
            int fw = Math.max(2 * D2StartArt.CARD_FRAME_EDGE + 1, Math.round(w / l.scale));
            int fh = Math.max(2 * D2StartArt.CARD_FRAME_EDGE + 1, Math.round(h / l.scale));
            int[] px = D2StartArt.frame(art.cardFrame, D2StartArt.CARD_FRAME_EDGE, fw, fh);
            for (int i = 0; i < px.length; i++) px[i] = D2StartArt.tint(px[i], kind);
            frame = Bitmap.createBitmap(px, fw, fh, Bitmap.Config.ARGB_8888);
        }
        invalidate();
    }

    private float text(D2GameFont.Img img, float cx, float y, float k) {
        Bitmap b = bitmap(img);
        float room = boxWidth() - 40 * l.u, kk = Math.min(k, room / Math.max(1, b.getWidth()));
        texts.add(b);
        textAt.add(new float[]{cx - b.getWidth() * kk / 2, y, kk});
        return y + b.getHeight() * kk;
    }

    private void add(Item item, float x, float y, float w, float h) {
        LayoutParams p = new LayoutParams(Math.round(w), Math.round(h));
        p.leftMargin = Math.round(x);
        p.topMargin = Math.round(y);
        addView(item, p);
    }

    private static Bitmap bitmap(D2GameFont.Img img) {
        return Bitmap.createBitmap(img.argb, img.width, img.height, Bitmap.Config.ARGB_8888);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(box, fill);
        if (frame != null) canvas.drawBitmap(frame, null, box, pixels);
        else {
            Paint p = new Paint();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3 * l.u);
            p.setColor(confirming ? Color.rgb(170, 30, 30) : Color.rgb(200, 176, 116));
            canvas.drawRect(box, p);
        }
        for (int i = 0; i < texts.size(); i++) {
            Bitmap b = texts.get(i);
            float[] t = textAt.get(i);
            canvas.drawBitmap(b, null, new RectF(t[0], t[1], t[0] + b.getWidth() * t[2], t[1] + b.getHeight() * t[2]), smooth);
        }
        if (lineY >= 0) canvas.drawRect(box.left + 40 * l.u, lineY, box.right - 40 * l.u, lineY + Math.max(1, 2 * l.u), line);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_UP && !box.contains(event.getX(), event.getY())) actions.close();
        return true;
    }


    private final class Item extends View {
        private final Bitmap label;
        private final boolean isStone;
        final boolean first;
        private Bitmap sharp;
        private int sharpFactor;

        Item(Context context, D2GameFont.Img label, boolean isStone, boolean enabled, boolean first, Runnable onTap) {
            super(context);
            this.label = bitmap(label);
            this.isStone = isStone;
            this.first = first && enabled;
            if (enabled && onTap != null) {
                setClickable(true);
                setOnClickListener((v) -> onTap.run());
                D2PadFocus.frame(this);
            }
            else {
                setFocusable(false);
                setAlpha(0.4f);
            }
        }

        @Override
        protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float k = l.textScale, shift = isPressed() ? k : 0;
            if (isStone) canvas.drawBitmap(isPressed() ? stonePressed : stone, null, new RectF(0, 0, getWidth(), getHeight()), pixels);
            float kk = Math.min(isStone ? k : k * 1.15f, (getWidth() - 16 * l.u) / Math.max(1, label.getWidth()));
            float w = label.getWidth() * kk, h = label.getHeight() * kk;
            float x = getWidth() / 2f - w / 2 - shift, y = getHeight() / 2f - h / 2 + (isStone ? 2 * l.u : 0) + shift;
            int n = Math.max(1, (int) Math.ceil(kk - 0.01f));
            if (sharp == null || sharpFactor != n) {
                sharp = n == 1 ? label : Bitmap.createScaledBitmap(label, label.getWidth() * n, label.getHeight() * n, false);
                sharpFactor = n;
            }
            canvas.drawBitmap(sharp, null, new RectF(x, y, x + w, y + h), smooth);
        }
    }
}
