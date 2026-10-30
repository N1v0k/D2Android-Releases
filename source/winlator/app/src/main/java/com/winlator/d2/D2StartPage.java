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

        void delete(D2SaveHeader.Info character);

        void restore(D2CharDelete.Deleted deleted);
        void purge(D2CharDelete.Deleted deleted);

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

    private Bitmap extension;
    private Bitmap backdrop, hint, count, footer, classic, button, buttonPressed;

    private final int licencesFrom, licencesWidth, footerWidth;

    private final View classicTap, licencesTap, modTap;

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


        String lead = context.getString(R.string.start_trademark) + "   ", licences = context.getString(R.string.start_licenses);
        String version = D2AppVersion.label(D2Updates.installedVersion(context));
        String line = lead + licences + (version != null ? "   \u00b7   " + version : "");
        footer = bitmap(art.formal10.render(line, 0x808080));
        licencesFrom = art.formal10.width(lead);
        licencesWidth = art.formal10.width(licences);
        footerWidth = art.formal10.width(line);
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

        int[] labels = {R.string.start_new_character, R.string.start_settings, R.string.settings_saves, R.string.news_title};
        for (int i = 0; i < labels.length; i++) {
            Stone stone = new Stone(context, i, bitmap(art.exocet10.render(context.getString(labels[i]), D2GameFont.STONE_RGB)));
            stones.add(stone);
            addView(stone, new LayoutParams(0, 0));
        }
        classicTap = new View(context);
        classicTap.setOnClickListener((v) -> host.button(6));
        addView(classicTap, new LayoutParams(0, 0));
        licencesTap = new View(context);
        licencesTap.setOnClickListener((v) -> host.button(5));
        addView(licencesTap, new LayoutParams(0, 0));
        modTap = new View(context);
        modTap.setOnClickListener((v) -> host.button(7));
        addView(modTap, new LayoutParams(0, 0));

        for (Stone stone : stones) D2PadFocus.frame(stone);
        D2PadFocus.frame(classicTap);
        D2PadFocus.frame(licencesTap);
        D2PadFocus.frame(modTap);
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
        closeMenu();
        layout = new D2StartLayout(w, h, getResources().getDisplayMetrics().density, getResources().getConfiguration().smallestScreenWidthDp);
        int bh = layout.backdropHeight;
        int[] back = art.backdrop(bh);
        backdrop = Bitmap.createBitmap(back, D2StartArt.SCREEN_W, bh, Bitmap.Config.ARGB_8888);

        int extW = layout.twoColumns ? (int) Math.ceil(w / layout.scale - (D2StartArt.SCREEN_W - layout.cutX)) + 1 : 0;
        extension = extW > 0 ? Bitmap.createBitmap(D2StartArt.extension(back, bh, extW), extW, bh, Bitmap.Config.ARGB_8888) : null;
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
        sp.leftMargin = Math.round(l.listLeft);
        sp.width = Math.round(l.width - l.listLeft);
        scroll.setLayoutParams(sp);
        for (int i = 0; i < rows.getChildCount(); i++) {
            View card = rows.getChildAt(i);
            card.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.round(l.rowHeight)));
        }
        for (Stone stone : stones) {
            LayoutParams p = (LayoutParams) stone.getLayoutParams();
            p.width = Math.round(l.buttonWidth);
            p.height = Math.round(l.buttonHeight);
            p.leftMargin = Math.round(l.menuX - l.buttonWidth / 2);
            p.topMargin = Math.round(l.buttonTop(stone.index));
            stone.setLayoutParams(p);
        }
        LayoutParams cp = (LayoutParams) classicTap.getLayoutParams();
        cp.width = Math.round(l.menuWidth / 2f);
        cp.height = Math.round(34 * l.u);
        cp.leftMargin = Math.round(l.menuX - l.menuWidth / 4f);
        cp.topMargin = Math.round(l.classicY - 8 * l.u);
        classicTap.setLayoutParams(cp);
        View licences = licencesTap;
        LayoutParams lp = (LayoutParams) licences.getLayoutParams();

        float footerLeft = l.menuX - footerWidth * l.footerScale / 2;
        lp.width = Math.round(licencesWidth * l.footerScale + 24 * l.u);
        lp.height = Math.round(34 * l.u);
        lp.leftMargin = Math.round(footerLeft + licencesFrom * l.footerScale - 12 * l.u);
        lp.topMargin = Math.round(l.footerY - 10 * l.u);
        licences.setLayoutParams(lp);
        LayoutParams mp = (LayoutParams) modTap.getLayoutParams();
        mp.width = Math.round(l.modRight - l.modLeft);
        mp.height = Math.round(l.modBottom - l.modTop);
        mp.leftMargin = Math.round(l.modLeft);
        mp.topMargin = Math.round(l.modTop);
        modTap.setLayoutParams(mp);
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
        boolean more = l != null && rows.getChildCount() * l.rowHeight > l.listBottom - l.listTop;
        int n = characters.size();
        if (n == 0) {
            count = null;
            return;
        }
        String text = getResources().getQuantityString(more ? R.plurals.start_count_swipe : R.plurals.start_count, n, n);
        count = bitmap(art.formal10.render(text, 0xA0A0A0));
    }



    private Bitmap modTitle, modSub, modArrow, modGem;





    void setMod(String title, String sub, int gem) {
        modTitle = bitmap(art.font16.render(title, 0xC8B074));
        modSub = bitmap(art.formal10.render(sub, 0xA0A0A0));
        modArrow = bitmap(art.font16.render(">", 0xA0A0A0));
        D2Sprites.Frame g = gem >= 0 && gem < art.gems.length ? art.gems[gem] : null;
        modGem = g != null ? Bitmap.createBitmap(g.argb, g.width, g.height, Bitmap.Config.ARGB_8888) : null;
        backdropView.invalidate();
    }


    private void drawModLine(Canvas canvas, D2StartLayout l) {
        if (modTitle == null) return;
        float left = l.modLeft, right = l.modRight, top = l.modTop, bottom = l.modBottom;
        Paint p = new Paint();
        p.setColor(0xB0000000);
        canvas.drawRect(left, top, right, bottom, p);
        p.setColor(Color.rgb(110, 96, 60));
        float t = Math.max(1, 2 * l.u);
        canvas.drawRect(left, top, right, top + t, p);
        canvas.drawRect(left, bottom - t, right, bottom, p);
        float x = left + 14 * l.u;
        if (modGem != null) {
            float k = 52 * l.u / Math.max(1, modGem.getHeight());
            float w = modGem.getWidth() * k, h = modGem.getHeight() * k, y = (top + bottom) / 2 - h / 2;
            canvas.drawBitmap(modGem, null, new RectF(x, y, x + w, y + h), pixels);
            x += w + 14 * l.u;
        }
        float kt = 2f * l.u, ks = 1.7f * l.u;
        canvas.drawBitmap(modTitle, null, new RectF(x, top + 4 * l.u, x + modTitle.getWidth() * kt, top + 4 * l.u + modTitle.getHeight() * kt), smooth);
        float sy = top + 4 * l.u + modTitle.getHeight() * kt;
        canvas.drawBitmap(modSub, null, new RectF(x, sy, x + modSub.getWidth() * ks, sy + modSub.getHeight() * ks), smooth);
        float aw = modArrow.getWidth() * kt, ah = modArrow.getHeight() * kt, ay = (top + bottom) / 2 - ah / 2;
        canvas.drawBitmap(modArrow, null, new RectF(right - 14 * l.u - aw, ay, right - 14 * l.u, ay + ah), smooth);
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


    private UpdateCard updateCard;





    void setUpdate(String title, String line, Runnable onTap) {
        if (updateCard != null) rows.removeView(updateCard);
        updateCard = null;
        if (title != null) {
            updateCard = new UpdateCard(getContext(), title, line, onTap);
            D2StartLayout l = layout;
            rows.addView(updateCard, 0, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                l != null ? Math.round(l.rowHeight) : LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        updateCount();
        backdropView.invalidate();
        if (overlay != null) overlay.invalidate();
    }



    private D2CharMenu menu;


    void openMenu(D2SaveHeader.Info c) {
        if (menu != null || layout == null) return;
        menu = new D2CharMenu(getContext(), art, layout, c, new D2CharMenu.Actions() {
            @Override public void start(int difficulty) {
                closeMenu();
                host.start(c, difficulty);
            }
            @Override public void delete() {
                if (menu == null) return;
                if (!menu.confirming()) {
                    menu.showConfirm();
                    return;
                }
                closeMenu();
                host.delete(c);
            }
            @Override public void close() {
                closeMenu();
            }
        });
        addView(menu, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        behind(false);
        if (!isInTouchMode()) menu.post(() -> {
            View f = menu != null ? menu.firstFocus() : null;
            if (f != null) f.requestFocus();
        });
        if (padOn) updatePadHint();
    }


    boolean closeMenu() {
        if (menu == null) return false;
        String file = menu.characterFile();
        removeView(menu);
        menu = null;
        behind(true);
        if (!isInTouchMode()) {
            for (int i = 0; i < rows.getChildCount(); i++) {
                View row = rows.getChildAt(i);
                if (row instanceof Card && ((Card) row).character.file.equals(file)) row.requestFocus();
            }
        }
        if (padOn) updatePadHint();
        return true;
    }

    private void behind(boolean focusable) {
        scroll.setDescendantFocusability(focusable ? FOCUS_AFTER_DESCENDANTS : FOCUS_BLOCK_DESCENDANTS);
        for (Stone stone : stones) stone.setFocusable(focusable);
        classicTap.setFocusable(focusable);
        licencesTap.setFocusable(focusable);
        modTap.setFocusable(focusable);
    }


    void setDeleted(List<D2CharDelete.Deleted> deleted) {
        for (int i = rows.getChildCount() - 1; i >= 0; i--) if (rows.getChildAt(i) instanceof DeletedCard) rows.removeViewAt(i);
        D2StartLayout l = layout;
        for (D2CharDelete.Deleted d : deleted) rows.addView(new DeletedCard(getContext(), d), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, l != null ? Math.round(l.rowHeight) : LinearLayout.LayoutParams.WRAP_CONTENT));
        updateCount();
        backdropView.invalidate();
        if (overlay != null) overlay.invalidate();
    }




    private Bitmap padHint;
    private boolean padOn;
    private final android.view.ViewTreeObserver.OnGlobalFocusChangeListener padFocusMoved = (from, to) -> {
        if (padOn) updatePadHint();
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalFocusChangeListener(padFocusMoved);
    }


    View padFirstFocus() {
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            if (row instanceof Card && ((Card) row).last) return row;
        }
        return stones.isEmpty() ? null : stones.get(0);
    }


    boolean padButton(int action, View focused) {
        switch (D2StartPad.onPad(action, padFocus(focused))) {
            case D2StartPad.CARD_MENU:
                focused.performLongClick();
                return true;
            case D2StartPad.SETTINGS:
                host.button(1);
                return true;
            case D2StartPad.CLOSE_MENU:
                closeMenu();
                return true;
            case D2StartPad.SWALLOW:
                return true;
            default:
                return false;
        }
    }


    void setPadActive(boolean on) {
        if (on == padOn) return;
        padOn = on;
        if (on) updatePadHint();
        else {
            padHint = null;
            backdropView.invalidate();
        }
    }

    private int padFocus(View focused) {
        if (menu != null) return D2StartPad.MENU;
        if (focused instanceof DeletedCard) return D2StartPad.DELETED;
        if (focused instanceof Card) {
            Card card = (Card) focused;
            if (!card.startable) return D2StartPad.CARD_LOCKED;
            return card.character.openDifficulty() > 0 ? D2StartPad.CARD_GRADES : D2StartPad.CARD;
        }
        return focused != null && focused.getParent() != null ? D2StartPad.OTHER : D2StartPad.NOTHING;
    }

    private void updatePadHint() {
        View focused = findFocus();
        padHint = padHintPicture(D2StartPad.hints(padFocus(focused)));
        backdropView.invalidate();
    }


    private Bitmap padHintPicture(int[] items) {
        int n = items.length / 2;
        int[] glyphs = new int[n];
        String[] words = new String[n];
        for (int i = 0; i < n; i++) {
            int g = items[2 * i];
            glyphs[i] = g == D2StartPad.GLYPH_A || g == D2StartPad.GLYPH_AA ? D2PadHint.A : g == D2StartPad.GLYPH_Y ? D2PadHint.Y
                : g == D2StartPad.GLYPH_B ? D2PadHint.B : D2PadHint.START;
            words[i] = padWord(items[2 * i + 1]);
        }
        return D2PadHint.picture(art.formal10, glyphs, words);
    }

    private String padWord(int text) {
        int id;
        switch (text) {
            case D2StartPad.TEXT_PLAY: id = R.string.pad_hint_play; break;
            case D2StartPad.TEXT_MENU: id = R.string.pad_hint_menu; break;
            case D2StartPad.TEXT_WHY: id = R.string.pad_hint_why; break;
            case D2StartPad.TEXT_SETTINGS: id = R.string.pad_hint_settings; break;
            case D2StartPad.TEXT_CHOOSE: id = R.string.pad_hint_choose; break;
            case D2StartPad.TEXT_BACK: id = R.string.pad_hint_back; break;
            case D2StartPad.TEXT_RESTORE: id = R.string.pad_hint_restore; break;
            case D2StartPad.TEXT_PURGE: id = R.string.pad_hint_purge; break;
            default: id = R.string.pad_hint_open;
        }
        return getContext().getString(id);
    }


    void setFigure(String file, Figure figure) {
        figures.put(file, figure);
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            if (row instanceof Card && ((Card) row).character.file.equals(file)) row.invalidate();
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
        getViewTreeObserver().removeOnGlobalFocusChangeListener(padFocusMoved);
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
            boolean column = l.left > 0 && !l.twoColumns;
            if (column) sides(canvas);
            canvas.save();
            canvas.scale(l.scale, l.scale);
            canvas.translate(-l.cutX, 0);
            canvas.drawBitmap(backdrop, 0, 0, smooth);
            if (extension != null) canvas.drawBitmap(extension, D2StartArt.SCREEN_W, 0, smooth);
            canvas.restore();
            if (column) edges(canvas, l);
            float k = l.textScale;
            drawModLine(canvas, l);
            if (statusLines.isEmpty()) {
                Bitmap b = padHint != null ? padHint : hint;

                if (l.twoColumns) {


                    float x = l.cardLeft + 8 * l.u, end = Math.min(l.cardRight - 8 * l.u,
                        getWidth() - 130 * getResources().getDisplayMetrics().density);
                    float kh = Math.min(k, Math.max(1, end - x) / b.getWidth());
                    canvas.drawBitmap(b, null, new RectF(x, l.hintY, x + b.getWidth() * kh, l.hintY + b.getHeight() * kh), smooth);
                }
                else centred(canvas, b, l.listX, l.hintY, k);
            } else {
                D2StartLayout.Status p = statusPlace;
                float y = p.top;
                for (Bitmap b : statusLines) {
                    centred(canvas, b, l.listX, y, p.scale);
                    y += p.step;
                }
            }
            if (count != null) centred(canvas, count, l.listX, l.countY(rows.getChildCount()), k);
            if (!l.twoColumns) {
                Paint line = new Paint();
                line.setColor(Color.rgb(110, 96, 60));
                canvas.drawRect(l.left + 40 * l.u, l.lineY, l.right - 40 * l.u, l.lineY + Math.max(1, 2 * l.u), line);
            }
            centred(canvas, classic, l.menuX, l.classicY, l.classicScale);
            centred(canvas, footer, l.menuX, l.footerY, l.footerScale);
        }





        private void sides(Canvas canvas) {

            int inset = backdrop.getWidth() / 10, w = backdrop.getWidth() - 2 * inset;
            int h = Math.min(backdrop.getHeight(), Math.round(w * getHeight() / (float) getWidth()));
            canvas.drawBitmap(backdrop, new android.graphics.Rect(inset, 0, inset + w, h), new RectF(0, 0, getWidth(), getHeight()), smooth);
            Paint dark = new Paint();
            dark.setColor(Color.argb(190, 0, 0, 0));
            canvas.drawRect(0, 0, getWidth(), getHeight(), dark);
        }


        private void edges(Canvas canvas, D2StartLayout l) {
            float soft = 60 * l.u;
            float colLeft = Math.max(0, -l.cutX * l.scale);
            float colRight = getWidth() - colLeft;
            Paint fade = new Paint();
            fade.setShader(new android.graphics.LinearGradient(colLeft, 0, colLeft + soft, 0, Color.argb(190, 0, 0, 0),
                Color.TRANSPARENT, android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(colLeft, 0, colLeft + soft, getHeight(), fade);
            fade.setShader(new android.graphics.LinearGradient(colRight - soft, 0, colRight, 0, Color.TRANSPARENT,
                Color.argb(190, 0, 0, 0), android.graphics.Shader.TileMode.CLAMP));
            canvas.drawRect(colRight - soft, 0, colRight, getHeight(), fade);
        }

        private void centred(Canvas canvas, Bitmap b, float x, float y, float k) {
            float w = b.getWidth() * k;
            canvas.drawBitmap(b, null, new RectF(x - w / 2, y, x + w / 2, y + b.getHeight() * k), smooth);
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
            float total = rows.getChildCount() * l.rowHeight, view = l.listBottom - l.listTop;
            if (total <= view) return;
            float edge = 90 * l.u;
            fade.setShader(new LinearGradient(0, l.listBottom - edge, 0, l.listBottom, 0, Color.argb(200, 0, 0, 0), Shader.TileMode.CLAMP));
            canvas.drawRect(l.cardLeft, l.listBottom - edge, l.cardRight, l.listBottom, fade);
            canvas.drawRect(l.scrollX - 4 * l.u, l.listTop, l.scrollX + 4 * l.u, l.listBottom, track);
            float h = view * view / total, top = l.listTop + (view - h) * scroll.getScrollY() / Math.max(1, total - view);
            canvas.drawRect(l.scrollX - 6 * l.u, top, l.scrollX + 6 * l.u, top + h, thumb);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return false;
        }
    }


    private final class Card extends View implements D2PadFocus.Own {
        final D2SaveHeader.Info character;
        final boolean last, startable;
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
            startable = c.refusal() == D2SaveHeader.Refusal.NONE;
            if (!startable) setAlpha(0.5f);

            setFocusable(true);
            setDefaultFocusHighlightEnabled(false);
            setOnClickListener((v) -> {
                if (startable) host.start(c, c.defaultDifficulty());
                else host.refused(c);
            });
            setOnLongClickListener((v) -> {
                openMenu(c);
                return true;
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            D2StartLayout l = layout;
            if (l == null) return;
            boolean strong = character.hardcore() || last;
            canvas.translate(-l.listLeft, 0);
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
            float k = l.textScale, room = l.cardRight - 16 * l.u - l.textX;
            text(canvas, name, l.textX, 22 * l.u, l.fit(name.getWidth(), room - when.getWidth() * k - 16 * l.u));
            text(canvas, when, l.whenRight - when.getWidth() * k, 26 * l.u, k);
            text(canvas, classLine, l.textX, 78 * l.u, l.fit(classLine.getWidth(), room));
            text(canvas, whereLine, l.textX, 130 * l.u, l.fit(whereLine.getWidth(), room));
            if (isFocused()) D2PadFrame.draw(canvas, card, getResources().getDisplayMetrics().density);
        }

        @Override
        protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect previous) {
            super.onFocusChanged(gainFocus, direction, previous);
            invalidate();
        }

        private void text(Canvas canvas, Bitmap b, float x, float y, float k) {
            canvas.drawBitmap(b, null, new RectF(x, y, x + b.getWidth() * k, y + b.getHeight() * k), smooth);
        }
    }






    private final class DeletedCard extends View implements D2PadFocus.Own {
        private static final long DOUBLE_MS = 450;
        final D2CharDelete.Deleted deleted;
        private final Bitmap name, classLine, daysLine, howLine;
        private final Bitmap still;
        private final Paint fill = new Paint();
        private long lastTap;

        DeletedCard(Context context, D2CharDelete.Deleted d) {
            super(context);
            deleted = d;
            D2SaveHeader.Info c = d.info;
            name = bitmap(art.font16.render(c.name.isEmpty() ? c.file : c.name, 0x909090));
            classLine = bitmap(art.formal10.render(context.getString(R.string.start_class_level, className(context, c.characterClass), c.level), 0x909090));
            int days = Math.max(1, d.daysLeft(System.currentTimeMillis()));
            daysLine = bitmap(art.font16.render(getResources().getQuantityString(R.plurals.char_deleted_days, days, days), 0xD03030));
            howLine = bitmap(art.formal10.render(context.getString(R.string.char_deleted_how), 0xA0A0A0));
            D2Sprites.Frame[] fig = art.classFigure(c.characterClass);
            still = fig != null && fig.length > 0 ? Bitmap.createBitmap(fig[0].argb, fig[0].width, fig[0].height, Bitmap.Config.ARGB_8888) : null;
            fill.setColor(Color.argb(205, 0, 0, 0));
            setFocusable(true);
            setDefaultFocusHighlightEnabled(false);
            setOnClickListener((v) -> {
                long now = SystemClock.uptimeMillis();
                if (now - lastTap < DOUBLE_MS) {
                    lastTap = 0;
                    host.restore(deleted);
                }
                else lastTap = now;
            });
            setOnLongClickListener((v) -> {
                host.purge(deleted);
                return true;
            });
        }

        @Override
        protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect previous) {
            super.onFocusChanged(gainFocus, direction, previous);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            D2StartLayout l = layout;
            if (l == null) return;
            canvas.translate(-l.listLeft, 0);
            RectF card = new RectF(l.cardLeft, 0, l.cardRight, l.cardHeight);
            canvas.drawRect(card, fill);
            Bitmap gold = cardFrame(D2StartArt.FRAME_OTHER, l, card.width(), card.height());
            Paint dim = new Paint();
            dim.setAlpha(110);
            if (gold != null) canvas.drawBitmap(gold, null, card, dim);
            if (still != null) {
                Paint faded = new Paint(Paint.FILTER_BITMAP_FLAG);
                faded.setAlpha(90);
                float k = (l.cardHeight - 16 * l.u) / still.getHeight(), w = still.getWidth() * k;
                canvas.drawBitmap(still, null, new RectF(l.figureX - w / 2, 8 * l.u, l.figureX + w / 2, l.cardHeight - 8 * l.u), faded);
            }
            float k = l.textScale, room = l.cardRight - 16 * l.u - l.textX;
            text(canvas, name, l.textX, 18 * l.u, l.fit(name.getWidth(), room));
            text(canvas, classLine, l.textX, 62 * l.u, Math.min(k * 1.2f, room / Math.max(1, classLine.getWidth())));
            text(canvas, daysLine, l.textX, 98 * l.u, l.fit(daysLine.getWidth(), room));
            text(canvas, howLine, l.textX, 146 * l.u, Math.min(k * 1.2f, room / Math.max(1, howLine.getWidth())));
            if (isFocused()) D2PadFrame.draw(canvas, card, getResources().getDisplayMetrics().density);
        }

        private void text(Canvas canvas, Bitmap b, float x, float y, float k) {
            canvas.drawBitmap(b, null, new RectF(x, y, x + b.getWidth() * k, y + b.getHeight() * k), smooth);
        }
    }


    private final class UpdateCard extends View implements D2PadFocus.Own {
        private final Bitmap title, line;
        private final Paint fill = new Paint();

        UpdateCard(Context context, String title, String line, Runnable onTap) {
            super(context);
            this.title = bitmap(art.font16.render(title, 0xC8B074));
            this.line = line != null ? bitmap(art.formal10.render(line, 0xA0A0A0)) : null;
            fill.setColor(Color.argb(200, 0, 0, 0));
            setContentDescription(title);
            setOnClickListener((v) -> onTap.run());
            setFocusable(true);
            setDefaultFocusHighlightEnabled(false);
        }

        @Override
        protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect previous) {
            super.onFocusChanged(gainFocus, direction, previous);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            D2StartLayout l = layout;
            if (l == null) return;
            canvas.translate(-l.listLeft, 0);
            RectF card = new RectF(l.cardLeft, 0, l.cardRight, l.cardHeight);
            canvas.drawRect(card, fill);
            Bitmap gold = cardFrame(D2StartArt.FRAME_LAST, l, card.width(), card.height());
            if (gold != null) canvas.drawBitmap(gold, null, card, pixels);
            float k = l.textScale, cx = card.centerX();
            float tw = title.getWidth() * k, ty = (line != null ? 48 : 78) * l.u;
            canvas.drawBitmap(title, null, new RectF(cx - tw / 2, ty, cx + tw / 2, ty + title.getHeight() * k), smooth);
            if (line != null) {
                float lw = Math.min(line.getWidth() * k, card.width() - 40 * l.u), lk = lw / line.getWidth(), ly = 120 * l.u;
                canvas.drawBitmap(line, null, new RectF(cx - lw / 2, ly, cx + lw / 2, ly + line.getHeight() * lk), smooth);
            }
            if (isFocused()) D2PadFrame.draw(canvas, card, getResources().getDisplayMetrics().density);
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
