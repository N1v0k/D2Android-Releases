package com.winlator.d2;

import java.util.ArrayList;
import java.util.List;

public final class D2Intro {

    public static final boolean PAUSE = true;
    public static final boolean SHOW_TO_EXISTING = true;
    public static final boolean TAP_ANYWHERE = true;
    public static final Step[] STEPS = {Step.WELCOME, Step.JOYSTICK, Step.ATTACK, Step.TAKE, Step.POTIONS, Step.SKILLS, Step.BAR};

    public static final String EXTRA_ADB = "d2_intro";

    public static final long START_DELAY_MS = 1000;
    public static final long PAUSE_REPEAT_MS = 1000;
    public static final long SKIP_CONFIRM_MS = 3000;
    public static final long SKIP_CONFIRM_MIN_MS = 400;
    public static final int MAX_INTERRUPTIONS = 3;

    public enum Step { WELCOME, JOYSTICK, ATTACK, TAKE, POTIONS, SKILLS, BAR }

    public enum Presence {
        SHOWN,
        HIDDEN,
        ABSENT
    }

    public interface PresenceOf {
        Presence of(Step step);
    }

    public interface Width {
        int of(String text);
    }

    private D2Intro() {}

    public static final class Conditions {
        public boolean seen;
        public boolean graphics, layout;
        public boolean heroAlive;
        public boolean idle, editMode, gameWindow, ringOpen, paused;
        public boolean playtester, testPlan;
        public boolean singlePlayer;
        public boolean windowFocus;
        public boolean fingers;
        public long stateFor;
        public boolean buttonsBehind;
    }

    public static final class Skip {
        private long armedAt = -1;

        public boolean press(long now, boolean back) {
            if (armed(now)) {
                if (!back && now - armedAt < SKIP_CONFIRM_MIN_MS) {
                    armedAt = now;
                    return false;
                }
                armedAt = -1;
                return true;
            }
            armedAt = now;
            return false;
        }

        public boolean armed(long now) {
            return armedAt >= 0 && now - armedAt <= SKIP_CONFIRM_MS;
        }

        public void reset() {
            armedAt = -1;
        }
    }

    public static int interruptionsAfter(boolean interrupted, int before) {
        return interrupted ? before + 1 : 0;
    }

    public static boolean seenAfter(boolean interrupted, int interruptionsNow) {
        return !interrupted || interruptionsNow >= MAX_INTERRUPTIONS;
    }

    public static boolean liftMenuPause(boolean playerPausedAtMenu, boolean paused) {
        return paused && !playerPausedAtMenu;
    }

    public static boolean shouldStart(Conditions c) {
        return !c.seen && c.graphics && c.layout && c.heroAlive && !c.idle && !c.editMode && !c.gameWindow && !c.ringOpen
            && !c.paused && !c.playtester && !c.testPlan && c.singlePlayer && c.windowFocus && !c.fingers && c.stateFor >= START_DELAY_MS
            && !c.buttonsBehind;
    }

    public static void windowsInto(Conditions c, long panelFlags, int escMenu, boolean ownPause, boolean npc, long appliedWindows) {
        long open = windows(panelFlags, escMenu, ownPause);
        c.gameWindow = open != 0 || npc;
        c.buttonsBehind = open != appliedWindows;
    }

    public static boolean seenByDefault(boolean playedBefore) {
        return !SHOW_TO_EXISTING && playedBefore;
    }

    public static long windows(long panelFlags, int escMenu, boolean ownPause) {
        return ownPause ? panelFlags & ~(1L << escMenu) : panelFlags;
    }

    public static boolean pauses(boolean canPause, boolean singlePlayer) {
        return PAUSE && canPause && singlePlayer;
    }

    public static List<Step> steps(Step[] all, PresenceOf presence) {
        List<Step> steps = new ArrayList<>();
        for (Step step : all) if (step == Step.WELCOME || presence.of(step) != Presence.ABSENT) steps.add(step);
        return steps;
    }

    public static final class Tour {
        private final List<Step> steps;
        private int at;

        public Tour(List<Step> steps) {
            this.steps = steps;
        }

        public Step step() {
            return steps.get(at);
        }

        public int index() {
            return at;
        }

        public int size() {
            return steps.size();
        }

        public boolean last() {
            return at == steps.size() - 1;
        }

        public boolean next() {
            if (last()) return false;
            at++;
            return true;
        }

        public String count() {
            return (at + 1) + "/" + steps.size();
        }
    }

    public static float[] boxAt(float w, float h, float bw, float bh, float margin, float[] spot, List<float[]> keep) {
        float midX = (w - bw) / 2, midY = (h - bh) / 2;
        if (spot == null) {
            float[] middle = {midX, midY};
            return free(middle, bw, bh, keep) ? middle : nearestFree(w, h, bw, bh, margin, keep, midX, midY);
        }
        float spotX = (spot[0] + spot[2]) / 2;
        float otherX = spotX >= w / 2 ? Math.max(margin, w / 4 - bw / 2) : Math.min(w - margin - bw, 3 * w / 4 - bw / 2);
        float top = margin, bottom = h - margin - bh;
        float[][] candidates = {{otherX, midY}, {otherX, top}, {otherX, bottom}, {midX, midY}, {midX, top}, {midX, bottom}};
        List<float[]> all = new ArrayList<>(keep);
        all.add(spot);
        for (float[] c : candidates) {
            if (c[0] < margin - 0.5f || c[1] < margin - 0.5f || c[0] + bw > w - margin + 0.5f || c[1] + bh > h - margin + 0.5f) continue;
            if (free(c, bw, bh, all)) return c;
        }
        return new float[]{midX, midY};
    }

    private static float[] nearestFree(float w, float h, float bw, float bh, float margin, List<float[]> keep, float x, float y) {
        for (float[] k : keep) {
            float[] below = {x, k[3] + margin};
            if (below[1] + bh <= h - margin && free(below, bw, bh, keep)) return below;
        }
        return new float[]{x, y};
    }

    private static boolean free(float[] at, float bw, float bh, List<float[]> rects) {
        for (float[] r : rects) if (at[0] < r[2] && at[0] + bw > r[0] && at[1] < r[3] && at[1] + bh > r[1]) return false;
        return true;
    }

    public static List<String> wrap(String text, Width width, int budget) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            if (word.isEmpty()) continue;
            String tried = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && width.of(tried) > budget) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
            else {
                line.setLength(0);
                line.append(tried);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }
}
