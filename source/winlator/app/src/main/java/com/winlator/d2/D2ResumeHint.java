package com.winlator.d2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.RectF;

import com.winlator.R;

import java.io.File;
import java.io.IOException;

final class D2ResumeHint {
    interface Host {
        void note(String text, long ms);

        String text(int id, Object... args);

        boolean idle();

        boolean focused();
    }

    static final int DECISION_NONE = D2LoadingSteps.DECISION_NONE, DECISION_GOES = D2LoadingSteps.DECISION_GOES,
        DECISION_NOT = D2LoadingSteps.DECISION_NOT;
    private static volatile int decision;
    private static volatile boolean skipNext;

    static int decision() {
        return decision;
    }

    static synchronized boolean skipNext() {
        if (decision != DECISION_NONE) return false;
        skipNext = true;
        return true;
    }

    private static synchronized int decide(boolean goes) {
        int d = goes && !skipNext ? DECISION_GOES : DECISION_NOT;
        if (goes && skipNext) D2Trace.log("resume: skipped by a tap on the loading picture");
        skipNext = false;
        decision = d;
        return d;
    }

    static final long COUNTDOWN_MS = 3000;
    private static final long FILE_POLL_MS = 300, DIFFICULTY_WAIT_MS = 5000, SEND_REPEAT_MS = 500, SEND_GIVE_UP_MS = 4000;

    private final Context context;
    private final D2ModLink link;
    private final Host host;
    private final D2Resume resume;
    private String folder;
    private File saveDir;
    private File pendingFile;

    private final D2Resume.GameWatch games = new D2Resume.GameWatch();
    private long gameSince, countdownUntil, sentAt, lastPoll;
    private boolean decided, cancelled, sending, reported;
    private int lastState = -1;
    private final RectF area = new RectF();

    D2ResumeHint(Context context, D2ModLink link, Host host) {
        this.context = context;
        this.link = link;
        this.host = host;
        final SharedPreferences prefs = context.getSharedPreferences("d2resume", Context.MODE_PRIVATE);
        this.resume = new D2Resume(new D2Resume.Store() {
            @Override public String get(String key) { return prefs.getString(key, null); }
            @Override public void put(String key, String value) { prefs.edit().putString(key, value).commit(); }
            @Override public void remove(String key) { prefs.edit().remove(key).commit(); }
        });
    }

    void setGameDir(File dir) {
        try {
            folder = dir.getCanonicalPath();
        }
        catch (IOException e) {
            folder = dir.getAbsolutePath();
        }
        saveDir = D2Saves.saveDir(dir);
    }

    private File heroFile(String name) {
        return saveDir == null || name == null ? null : new File(saveDir, name + ".d2s");
    }

    void gameLeft() {
        String name = games.hero();
        File f = heroFile(name);
        if (f == null || !f.isFile()) return;
        if (resume.leftWritten(folder, name, f.length(), f.lastModified()))
            D2Trace.log("resume: place remembered with the game's own save at its exit");
        else D2Trace.log("resume: the game's own save at its exit, no place to remember");
    }

    void saving(long now) {
        String name = link.currentCharacterName();
        D2ResumeState s = link.resumeState();
        boolean fresh = name != null && s != null;
        if (name == null) {
            name = games.hero();
            s = null;
        }
        D2ModState m = link.state();
        s = resume.placeFor(name, s, m != null ? m.levelNo : -1, m != null && m.life > 0);
        if (s != null && s.state == D2ResumeState.STATE_RUNNING) return;
        pendingFile = heroFile(name);
        resume.saving(folder, name, s, pendingFile != null ? pendingFile.lastModified() : 0, now);
        lastPoll = now;
        if (!fresh) D2Trace.log("resume: save asked for without a fresh type 6, " + (s != null ? "the last place of this game" : "no place"));
    }

    void confirm(long now) {
        if (pollFile(now)) D2Trace.log("resume: place remembered with the last save before the end");
    }

    private boolean pollFile(long now) {
        lastPoll = now;
        File f = pendingFile;
        return resume.waiting(now) && f != null && resume.written(f.length(), f.lastModified(), now);
    }

    void tick(D2ModState s, long now) {
        String name = link.currentCharacterName();
        if (resume.waiting(now) && now - lastPoll >= FILE_POLL_MS && pollFile(now)) D2Trace.log("resume: place remembered with the save");
        boolean newGame = games.step(s != null, name, host.focused(), now);
        if (s == null || name == null) return;
        if (newGame) {
            gameSince = now;
            decided = cancelled = sending = reported = false;
            countdownUntil = 0;
            lastState = -1;
            resume.forget();
            decision = DECISION_NONE;
        }
        D2ResumeState r = link.resumeState();
        boolean dead = link.heroDeathKnown() ? link.heroDead() : s.life <= 0;
        if (dead) resume.died(name);
        else if (s.life <= 0 || link.heroDead()) resume.forget();
        else resume.seen(name, r);
        if (!decided) {
            if ((r == null || r.difficulty < 0) && now - gameSince < DIFFICULTY_WAIT_MS) return;
            decided = true;
            File f = heroFile(name);
            D2Resume.Why why = resume.decide(D2Settings.resume(context), link.singlePlayer(), host.idle(), folder, name, r,
                f != null ? f.length() : -1, f != null ? f.lastModified() : -1);
            D2Trace.log("resume: " + why + (r != null ? " · " + r : ""));
            if (decide(why == D2Resume.Why.OK) != DECISION_GOES) return;
            sending = true;
            sentAt = 0;
            D2Trace.log("resume: sent at once, " + (now - gameSince) + " ms after the game came");
        }
        if (countdownUntil != 0 && !cancelled && !sending && now >= countdownUntil) {
            sending = true;
            sentAt = 0;
            countdownUntil = 0;
        }
        if (sending) {
            if (r != null && r.state != D2ResumeState.STATE_NONE && r.state != lastState) {
                sending = r.state == D2ResumeState.STATE_RUNNING;
                lastState = r.state;
            }
            if (sending && r != null && r.state == D2ResumeState.STATE_NONE) {
                if (now - sentAt >= SEND_REPEAT_MS) {
                    sentAt = now;
                    D2Resume.Entry e = resume.entry;
                    link.resume(e.level, e.x, e.y, e.seed, e.roomX, e.roomY, e.roomW, e.roomH, e.difficulty);
                }
                if (now - gameSince > DIFFICULTY_WAIT_MS + SEND_GIVE_UP_MS) sending = false;
            }
        }
        if (!reported && r != null && (r.state == D2ResumeState.STATE_SPOT || r.state == D2ResumeState.STATE_WAYPOINT
            || r.state == D2ResumeState.STATE_REFUSED) && lastState == r.state) {
            reported = true;
            String level = levelName(resume.entry != null ? resume.entry.level : r.level);
            D2Trace.log("resume: done, state " + r.state + " reason " + r.reason);
            if (r.state == D2ResumeState.STATE_SPOT) host.note(host.text(R.string.resume_spot, level), 2500);
            else if (r.state == D2ResumeState.STATE_WAYPOINT) host.note(host.text(R.string.resume_waypoint, level), 2500);
            else host.note(host.text(R.string.resume_refused, reasonText(r.reason)), 3000);
        }
    }

    boolean active(long now) {
        return countdownUntil != 0 && !cancelled && now < countdownUntil;
    }

    void shown(RectF rect) {
        area.set(rect);
    }

    boolean hit(float x, float y, long now) {
        return active(now) && area.contains(x, y);
    }

    void cancel() {
        cancelled = true;
        countdownUntil = 0;
        D2Trace.log("resume: cancelled by a tap");
        host.note(host.text(R.string.resume_cancelled), 1500);
    }

    private String levelName(int level) {
        D2LevelNames names = D2LevelNames.current();
        String name = names != null ? names.name(level) : null;
        return name != null ? name : String.valueOf(level);
    }

    private String reasonText(int reason) {
        String[] reasons = context.getResources().getStringArray(R.array.resume_reasons);
        return reason >= 0 && reason < reasons.length ? reasons[reason] : String.valueOf(reason);
    }
}
