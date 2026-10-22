package com.winlator.d2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;

import com.winlator.R;

import java.util.Collections;
import java.util.List;

final class D2IntroView {
    static final class Spot {
        final float cx, cy, r;
        final RectF rect;
        final boolean hidden;
        final String label;

        Spot(float cx, float cy, float r, boolean hidden, String label) {
            this.cx = cx;
            this.cy = cy;
            this.r = r;
            this.rect = null;
            this.hidden = hidden;
            this.label = label;
        }

        Spot(RectF rect) {
            this.cx = rect.centerX();
            this.cy = rect.centerY();
            this.r = 0;
            this.rect = rect;
            this.hidden = false;
            this.label = null;
        }
    }

    interface Host {
        List<Spot> spots(D2Intro.Step step);
        D2Hud hud();
        float textScale();
        void modPause(boolean on);
        boolean appPaused();
        List<float[]> keepClear();
    }

    private static final int DARK = Color.argb(168, 0, 0, 0), RING_DARK = Color.rgb(85, 77, 46), RING_GOLD = Color.rgb(199, 179, 119),
        BOX_FILL = Color.argb(238, 10, 8, 6), PLATE = Color.argb(220, 10, 8, 6);
    private static final int TEXT_BUDGET = 300, PAD = 12, TITLE_H = 18, LINE_H = 13, FOOTER_H = 22;

    private final Context context;
    private final Host host;
    private final float dp;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path dark = new Path();
    private final RectF box = new RectF(), next = new RectF(), skip = new RectF(), tmp = new RectF();

    private D2Intro.Tour tour;
    private boolean ownPause;
    private long pausedAt;
    private final D2Intro.Skip skipGuard = new D2Intro.Skip();
    private boolean interrupted;
    private D2Intro.Step textStep;
    private String title, count, nextText, skipText, skipAgainText;
    private List<String> lines = Collections.emptyList();
    private float textK = -1;
    private List<Spot> spots = Collections.emptyList();
    private D2Intro.Step spotStep;
    private long spotsAt;
    private float boxK = -1;
    private int boxW, boxH;

    D2IntroView(Context context, Host host, float dp) {
        this.context = context;
        this.host = host;
        this.dp = dp;
        stroke.setStyle(Paint.Style.STROKE);
        dark.setFillType(Path.FillType.EVEN_ODD);
    }

    boolean active() {
        return tour != null;
    }

    boolean pausing() {
        return tour != null && ownPause;
    }

    void start(List<D2Intro.Step> steps, boolean pause, long now) {
        D2Settings.setIntroSeen(context, true);
        tour = new D2Intro.Tour(steps);
        skipGuard.reset();
        interrupted = false;
        textStep = null;
        spotStep = null;
        ownPause = pause;
        if (pause) {
            host.modPause(true);
            pausedAt = now;
        }
        D2Trace.log("intro: start, " + steps.size() + " steps" + (pause ? ", paused" : ""));
    }

    void tick(long now, boolean inGame, boolean windowFocus) {
        if (tour == null) return;
        if (!inGame || !windowFocus || host.appPaused()) {
            end(inGame ? "focus or pause" : "game gone");
            int count = D2Intro.interruptionsAfter(true, D2Settings.introInterruptions(context));
            D2Settings.setIntroInterruptions(context, count);
            if (!D2Intro.seenAfter(true, count)) {
                interrupted = true;
                D2Settings.setIntroSeen(context, false);
            }
            return;
        }
        if (ownPause && now - pausedAt >= D2Intro.PAUSE_REPEAT_MS) {
            host.modPause(true);
            pausedAt = now;
        }
    }

    boolean skip() {
        if (tour == null) return false;
        if (tour.last()) endSeen("done with back");
        else if (skipGuard.press(SystemClock.uptimeMillis(), true)) endSeen("skipped with back");
        return true;
    }

    boolean takeInterrupted() {
        boolean was = interrupted;
        interrupted = false;
        return was;
    }

    private void endSeen(String why) {
        D2Settings.setIntroInterruptions(context, D2Intro.interruptionsAfter(false, 0));
        end(why);
    }

    private void end(String why) {
        if (tour == null) return;
        D2Trace.log("intro: end (" + why + ") at " + tour.count());
        tour = null;
        if (ownPause && !host.appPaused()) host.modPause(false);
        ownPause = false;
    }

    boolean onTouch(MotionEvent event) {
        if (tour == null) return false;
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            float x = event.getX(), y = event.getY();
            if (!tour.last() && skip.contains(x, y)) {
                if (skipGuard.press(SystemClock.uptimeMillis(), false)) endSeen("skipped");
            }
            else if (D2Intro.TAP_ANYWHERE || next.contains(x, y)) {
                skipGuard.reset();
                if (!tour.next()) endSeen("done");
            }
        }
        return true;
    }

    void draw(Canvas canvas, int width, int height) {
        D2Intro.Tour t = tour;
        D2Hud hud = host.hud();
        if (t == null || hud == null || !hud.hasFrame()) return;
        float k = host.textScale();
        texts(t, hud, k);
        long now = SystemClock.uptimeMillis();
        if (t.step() != spotStep || now - spotsAt > 250 || k != boxK || width != boxW || height != boxH) place(t, k, width, height, now);
        dark.reset();
        dark.addRect(0, 0, width, height, Path.Direction.CW);
        for (Spot s : spots) {
            if (s.rect != null) dark.addRect(s.rect, Path.Direction.CW);
            else dark.addCircle(s.cx, s.cy, s.r, Path.Direction.CW);
        }
        fill.setColor(DARK);
        canvas.drawPath(dark, fill);
        for (Spot s : spots) {
            if (s.rect != null) {
                tmp.set(s.rect.left - 3 * k, s.rect.top - 3 * k, s.rect.right + 3 * k, s.rect.bottom + 3 * k);
                hud.drawFrame(canvas, tmp, k, Color.TRANSPARENT);
                continue;
            }
            if (s.hidden) {
                fill.setColor(PLATE);
                canvas.drawCircle(s.cx, s.cy, s.r, fill);
                if (s.label != null) hud.drawSmallText(canvas, s.label, s.cx, s.cy, k);
            }
            stroke.setStrokeWidth(3 * dp);
            stroke.setColor(RING_DARK);
            canvas.drawCircle(s.cx, s.cy, s.r + 2 * dp, stroke);
            stroke.setStrokeWidth(1.5f * dp);
            stroke.setColor(RING_GOLD);
            canvas.drawCircle(s.cx, s.cy, s.r + 2 * dp, stroke);
        }
        hud.drawFrame(canvas, box, k, BOX_FILL);
        float x = box.left + PAD * k, y = box.top + PAD * k;
        hud.drawText(canvas, title, x + hud.textWidth(title) * k / 2, y + TITLE_H * k / 2, k);
        y += TITLE_H * k;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            hud.drawSmallText(canvas, line, x + hud.smallTextWidth(line) * k / 2, y + LINE_H * k / 2, k);
            y += LINE_H * k;
        }
        float fy = box.bottom - (PAD + FOOTER_H / 2f) * k;
        hud.drawSmallText(canvas, count, x + hud.smallTextWidth(count) * k / 2, fy, k, 160);
        float nw = (hud.smallTextWidth(nextText) + 16) * k;
        next.set(box.right - PAD * k - nw, fy - FOOTER_H * k / 2, box.right - PAD * k, fy + FOOTER_H * k / 2);
        hud.drawFrame(canvas, next, k, PLATE);
        hud.drawSmallText(canvas, nextText, next.centerX(), fy, k);
        if (!t.last()) {
            boolean armed = skipGuard.armed(now);
            String text = armed ? skipAgainText : skipText;
            float sw = (hud.smallTextWidth(text) + 12) * k;
            skip.set(next.left - 6 * k - sw, next.top, next.left - 6 * k, next.bottom);
            if (armed) hud.drawFrame(canvas, skip, k, PLATE);
            hud.drawSmallText(canvas, text, skip.centerX(), fy, k, armed ? 255 : 170);
        }
        else skip.setEmpty();
    }

    private void place(D2Intro.Tour t, float k, int width, int height, long now) {
        spots = t.step() == D2Intro.Step.WELCOME ? Collections.<Spot>emptyList() : host.spots(t.step());
        spotStep = t.step();
        spotsAt = now;
        boxK = k;
        boxW = width;
        boxH = height;
        float[] bounds = null;
        for (Spot s : spots) {
            float out = s.rect != null ? 3 * k : 4 * dp;
            float l = s.rect != null ? s.rect.left - out : s.cx - s.r - out, top = s.rect != null ? s.rect.top - out : s.cy - s.r - out;
            float r = s.rect != null ? s.rect.right + out : s.cx + s.r + out, b = s.rect != null ? s.rect.bottom + out : s.cy + s.r + out;
            if (bounds == null) bounds = new float[]{l, top, r, b};
            else {
                bounds[0] = Math.min(bounds[0], l);
                bounds[1] = Math.min(bounds[1], top);
                bounds[2] = Math.max(bounds[2], r);
                bounds[3] = Math.max(bounds[3], b);
            }
        }
        float bw = (TEXT_BUDGET + 2 * PAD) * k, bh = (PAD + TITLE_H + lines.size() * LINE_H + 6 + FOOTER_H + PAD) * k;
        float[] at = D2Intro.boxAt(width, height, bw, bh, 8 * dp, bounds, host.keepClear());
        box.set(at[0], at[1], at[0] + bw, at[1] + bh);
    }

    private void texts(D2Intro.Tour t, D2Hud hud, float k) {
        D2Intro.Step step = t.step();
        if (step == textStep && k == textK) return;
        textStep = step;
        textK = k;
        title = D2Text.get(context, titleId(step));
        lines = D2Intro.wrap(D2Text.get(context, textId(step)), hud::smallTextWidth, TEXT_BUDGET);
        count = t.count();
        nextText = D2Text.get(context, t.last() ? R.string.intro_done : R.string.intro_next);
        skipText = D2Text.get(context, R.string.intro_skip);
        skipAgainText = D2Text.get(context, R.string.intro_skip_again);
    }

    private static int titleId(D2Intro.Step step) {
        switch (step) {
            case JOYSTICK: return R.string.intro_joystick_title;
            case ATTACK: return R.string.intro_attack_title;
            case TAKE: return R.string.intro_take_title;
            case POTIONS: return R.string.intro_potions_title;
            case SKILLS: return R.string.intro_skills_title;
            case BAR: return R.string.intro_bar_title;
            default: return R.string.intro_welcome_title;
        }
    }

    private static int textId(D2Intro.Step step) {
        switch (step) {
            case JOYSTICK: return R.string.intro_joystick_text;
            case ATTACK: return R.string.intro_attack_text;
            case TAKE: return R.string.intro_take_text;
            case POTIONS: return R.string.intro_potions_text;
            case SKILLS: return R.string.intro_skills_text;
            case BAR: return com.winlator.BuildConfig.D2_INTERNAL ? R.string.intro_bar_text_internal : R.string.intro_bar_text;
            default: return R.string.intro_welcome_text;
        }
    }
}
