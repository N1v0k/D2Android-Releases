package com.winlator.d2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import com.winlator.R;

import java.io.File;

@SuppressLint("ViewConstructor")
final class D2DirectLoading extends FrameLayout {
    interface Host {
        D2ModLink link();
        boolean inFront();
        File gameDir();
        String message(D2DirectStart.Outcome outcome, D2DirectStart.Report report);
        void arrived();
        boolean worldReady();
        void ended(D2DirectStart.Outcome outcome, D2DirectStart.Report report);
    }

    private static final long POLL_MS = 100;
    static final long RESUME_WAIT_MS = 10_000;
    private static final long MESSAGE_MS = 4000;
    private final Host host;
    private final String fileName;
    private final int difficulty;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final D2DirectStart.Watch watch = new D2DirectStart.Watch();
    private final Paint pixels = new Paint();
    private final Rect src = new Rect(), dst = new Rect();
    private long lastPoll = SystemClock.uptimeMillis(), inGameMs;
    private boolean ended, inGame, skipShown, arrivedDone, readyLogged;
    private volatile boolean resumeExpected;
    private volatile Bitmap[] frames;
    private volatile Bitmap textStart, textWorld, textSkip;
    private volatile D2GameFont smallFont;
    private volatile boolean fallback;
    private final Paint plain = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Bitmap message;
    private int shown;

    D2DirectLoading(Context context, String file, int difficulty, Host host) {
        super(context);
        this.host = host;
        this.fileName = file;
        this.difficulty = difficulty;
        setBackgroundColor(0xFF000000);
        setClickable(true);
        setWillNotDraw(false);
        pixels.setFilterBitmap(false);
        pixels.setAntiAlias(false);
    }

    void start() {
        final Context context = getContext();
        final File dir = host.gameDir();
        new Thread(() -> {
            resumeExpected = fileName != null && D2ResumeArea.expected(context, dir, fileName, difficulty);
            if (dir == null) {
                artFailed();
                return;
            }
            try (D2Sprites sprites = new D2Sprites(dir)) {
                D2Sprites.Frame[] f = sprites.dc6("data\\global\\ui\\Loading\\loadingscreen.dc6", "loading");
                D2GameFont big = D2GameFont.load(sprites, "font30"), small = D2GameFont.load(sprites, "font16");
                if (f == null || f.length < D2LoadingSteps.FRAMES) {
                    artFailed();
                    return;
                }
                Bitmap[] b = new Bitmap[D2LoadingSteps.FRAMES];
                for (int i = 0; i < b.length; i++) b[i] = Bitmap.createBitmap(f[i].argb, f[i].width, f[i].height, Bitmap.Config.ARGB_8888);
                textStart = text(big, D2Text.get(context, R.string.direct_loading_starting), 0xC7B377);
                textWorld = text(big, D2Text.get(context, R.string.direct_loading_world), 0xC7B377);
                textSkip = text(small, D2Text.get(context, R.string.direct_loading_skip), 0x8C8C8C);
                smallFont = small;
                frames = b;
                D2Trace.log("direct start: loading animation read, resume expected " + resumeExpected + "");
            }
            catch (Exception e) {
                D2Trace.log("direct start: loading animation not read: " + e + "");
                artFailed();
            }
            handler.post(this::invalidate);
        }, "LoadingArt").start();
        handler.postDelayed(this::poll, POLL_MS);
    }

    private void artFailed() {
        fallback = true;
        handler.post(this::invalidate);
    }

    private static Bitmap text(D2GameFont font, String s, int rgb) {
        if (font == null || s == null) return null;
        D2GameFont.Img t = font.render(s, rgb);
        return t != null && t.width > 0 ? Bitmap.createBitmap(t.argb, t.width, t.height, Bitmap.Config.ARGB_8888) : null;
    }

    boolean showing() {
        return !ended;
    }

    boolean messageShown() {
        return message != null;
    }

    private void poll() {
        if (ended || message != null) return;
        long now = SystemClock.uptimeMillis();
        long step = host.inFront() ? now - lastPoll : 0;
        lastPoll = now;
        D2ModLink link = host.link();
        D2DirectStart.Report report = link != null ? link.directReport() : null;
        D2DirectStart.Outcome outcome = watch.update(report, link != null && link.state() != null, link != null && link.isAlive(), step);
        if (outcome == D2DirectStart.Outcome.IN_GAME) {
            if (!inGame) D2Trace.log("direct start: in the game after " + watch.shownMs() + " ms in front");
            inGame = true;
            long inc = Math.min(step, D2DirectStart.Watch.MAX_STEP_MS);
            inGameMs += inc;
            if (!readyLogged && host.worldReady()) {
                D2Trace.log("direct start: game ready (world drawn, interface rebuilt) after " + inGameMs + " ms in the game");
                readyLogged = true;
            }
            if (resumeDone(link)) return;
        }
        else if (outcome != D2DirectStart.Outcome.WAIT) {
            String text = host.message(outcome, report);
            if (text != null && frames != null) {
                showMessage(text, outcome, report);
                return;
            }
            end(outcome, report);
            return;
        }
        int decision = D2ResumeHint.decision();
        boolean goes = resumeExpected && decision != D2ResumeHint.DECISION_NOT;
        shown = D2LoadingSteps.next(shown, D2LoadingSteps.target(link != null, link != null && link.isAlive(), report, inGame, goes));
        skipShown = D2LoadingSteps.skippable(shown, resumeExpected, decision);
        invalidate();
        handler.postDelayed(this::poll, POLL_MS);
    }

    private boolean resumeDone(D2ModLink link) {
        boolean settled = resumeSettled(link), ready = host.worldReady();
        if (!D2LoadingSteps.mayEnd(settled, ready, inGameMs)) return false;
        if (!settled || !ready)
            D2Trace.log("direct start: in the game for " + inGameMs + " ms, game ready " + ready + ", resume settled "
                + settled + ": the picture goes anyway");
        else D2Trace.log("direct start: world ready, the picture goes after " + inGameMs + " ms in the game");
        end(D2DirectStart.Outcome.IN_GAME, null);
        return true;
    }

    private boolean resumeSettled(D2ModLink link) {
        int decision = D2ResumeHint.decision();
        if (!resumeExpected || decision == D2ResumeHint.DECISION_NOT) return true;
        D2ResumeState r = link != null ? link.resumeState() : null;
        if (decision == D2ResumeHint.DECISION_GOES && r != null) {
            if (r.state == D2ResumeState.STATE_SPOT || r.state == D2ResumeState.STATE_WAYPOINT) {
                if (!arrivedDone) {
                    D2Trace.log("direct start: resumed (state " + r.state + "), arriving paused");
                    host.arrived();
                    arrivedDone = true;
                }
                return true;
            }
            if (r.state == D2ResumeState.STATE_REFUSED) return true;
        }
        if (inGameMs > RESUME_WAIT_MS) {
            if (!arrivedDone) D2Trace.log("direct start: no result of resume within " + RESUME_WAIT_MS + " ms");
            arrivedDone = true;
            return true;
        }
        return false;
    }

    private void showMessage(String text, D2DirectStart.Outcome outcome, D2DirectStart.Report report) {
        D2Trace.log("direct start: " + outcome + " shown in the loading picture");
        message = text(smallFont, text, 0xC8C8C8);
        if (message == null) {
            end(outcome, report);
            return;
        }
        skipShown = false;
        invalidate();
        handler.postDelayed(() -> end(outcome, report), MESSAGE_MS);
        pendingOutcome = outcome;
        pendingReport = report;
    }

    private D2DirectStart.Outcome pendingOutcome;
    private D2DirectStart.Report pendingReport;

    void end(D2DirectStart.Outcome outcome, D2DirectStart.Report report) {
        if (ended) return;
        ended = true;
        handler.removeCallbacksAndMessages(null);
        D2Trace.log("direct start: loading picture away, " + outcome + (report != null ? " (" + report + ")" : "")
            + " after " + watch.shownMs() + " ms in front");
        if (getParent() instanceof android.view.ViewGroup) ((android.view.ViewGroup) getParent()).removeView(this);
        host.ended(outcome, report);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap[] f = frames;
        if (f == null) {
            if (fallback) drawPlain(canvas);
            return;
        }
        int w = getWidth(), h = getHeight();
        Bitmap frame = f[Math.max(0, Math.min(D2LoadingSteps.LAST, shown))];
        int k = Math.max(1, Math.min(h * 72 / 100 / frame.getHeight(), w / frame.getWidth()));
        int t = Math.max(1, (k * 2 + 1) / 3);
        int fw = frame.getWidth() * k, fh = frame.getHeight() * k;
        Bitmap line = message != null ? message : D2LoadingSteps.world(shown) ? textWorld : textStart;
        int lineH = line != null ? line.getHeight() * t : 0;
        int skipH = skipShown && textSkip != null ? textSkip.getHeight() * t : 0;
        int total = fh + lineH + (lineH > 0 ? h / 30 : 0) + (skipH > 0 ? skipH + h / 40 : 0);
        int y = Math.max(0, (h - total) / 2);
        draw(canvas, frame, (w - fw) / 2, y, k);
        y += fh + h / 30;
        if (line != null) draw(canvas, line, (w - line.getWidth() * t) / 2, y, t);
        y += lineH + h / 40;
        if (skipH > 0) draw(canvas, textSkip, (w - textSkip.getWidth() * t) / 2, y, t);
    }

    private String[] plainTexts;

    private void drawPlain(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (plainTexts == null) {
            plainTexts = new String[]{D2Text.get(getContext(), R.string.direct_loading_starting),
                D2Text.get(getContext(), R.string.direct_loading_world), D2Text.get(getContext(), R.string.direct_loading_skip)};
            plain.setTextAlign(Paint.Align.CENTER);
        }
        plain.setColor(D2Style.GOLD);
        plain.setTextSize(h / 12f);
        canvas.drawText("Diablo II", w / 2f, h * 0.42f, plain);
        plain.setColor(D2Style.TEXT);
        plain.setTextSize(h / 22f);
        canvas.drawText(plainTexts[D2LoadingSteps.world(shown) ? 1 : 0], w / 2f, h * 0.55f, plain);
        if (skipShown) canvas.drawText(plainTexts[2], w / 2f, h * 0.65f, plain);
    }

    private void draw(Canvas canvas, Bitmap b, int x, int y, int k) {
        src.set(0, 0, b.getWidth(), b.getHeight());
        dst.set(x, y, x + b.getWidth() * k, y + b.getHeight() * k);
        canvas.drawBitmap(b, src, dst, pixels);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (message != null) end(pendingOutcome, pendingReport);
            else if (skipShown) {
                skipShown = false;
                if (D2ResumeHint.skipNext()) {
                    resumeExpected = false;
                    D2Trace.log("direct start: resume skipped by a tap");
                }
                else D2Trace.log("direct start: tap too late, resume goes on");
                invalidate();
            }
        }
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }
}
