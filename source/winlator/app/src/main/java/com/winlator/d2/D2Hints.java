package com.winlator.d2;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;








public final class D2Hints {





    public enum Topic { RESUME, TAKE, MERC, SORT, MATERIALS, RECIPES, POINTS, NEW_SKILL, PAD, WIN_STASH, WIN_CUBE, WIN_INVENTORY }


    public static final long SHOW_MS = 10000;

    public static final long GAP_MS = 20000;

    public static final long FIGHT_MS = 3000;

    public static final long START_DELAY_MS = 2000;

    public static final String EXTRA_ADB = "d2_intro";


    public static final class Facts {

        public boolean blocked;

        public boolean paused;

        public boolean leftPanel, rightPanel;

        public boolean otherWindow;
        public boolean inventoryOpen, stashOpen, cubeOpen;

        public boolean sortShown, materialsShown, recipesShown;

        public boolean merc;

        public boolean pointsLeft;

        public int skillPoints = -1;

        public boolean takeShown;

        public boolean padUsed;

        public boolean resumeChoice;

        public long foughtAt;

        public long inGameFor;
    }

    private final EnumSet<Topic> seen = EnumSet.noneOf(Topic.class);
    private Topic showing;
    private long shownAt, hiddenAt = Long.MIN_VALUE / 2;
    private int lastSkillPoints = -1;






    private final EnumSet<Topic> pending = EnumSet.noneOf(Topic.class);
    private boolean seenChanged;


    public Topic showing() {
        return showing;
    }


    public boolean seen(Topic t) {
        return seen.contains(t);
    }


    public boolean takeSeenChanged() {
        boolean was = seenChanged;
        seenChanged = false;
        return was;
    }


    public void markSeen(Topic t) {
        if (seen.add(t)) seenChanged = true;
    }


    public void reset() {
        seen.clear();
        seenChanged = true;
    }


    public void update(Facts f, long now) {
        if (f.skillPoints >= 0) {
            if (lastSkillPoints >= 0 && f.skillPoints < lastSkillPoints) pending.add(Topic.NEW_SKILL);
            lastSkillPoints = f.skillPoints;
        }
        if (f.stashOpen && f.materialsShown) pending.add(Topic.MATERIALS);
        if (f.cubeOpen && f.recipesShown) pending.add(Topic.RECIPES);
        pending.removeAll(seen);

        if (showing != null && (now - shownAt >= SHOW_MS || !stillFits(showing, f) || f.foughtAt > shownAt)) hide(now);
        if (showing != null || !mayShow(f, now)) return;
        for (Topic t : Topic.values()) {
            if (seen.contains(t) || !due(t, f)) continue;
            showing = t;
            shownAt = now;

            seen.add(t);
            seenChanged = true;
            pending.remove(t);
            return;
        }
    }


    public void dismiss(long now) {
        if (showing != null) hide(now);
    }


    public void newGame(long now) {
        lastSkillPoints = -1;
        pending.clear();
        if (showing != null) hide(now);
    }

    private void hide(long now) {
        showing = null;
        hiddenAt = now;
    }


    private boolean mayShow(Facts f, long now) {
        if (f.blocked || f.otherWindow || f.inGameFor < START_DELAY_MS) return false;
        if (f.paused && !f.resumeChoice) return false;
        if (f.foughtAt != 0 && now - f.foughtAt < FIGHT_MS) return false;
        return now - hiddenAt >= GAP_MS;
    }


    static boolean due(Topic t, Facts f, EnumSet<Topic> pending) {
        boolean noPanel = !f.leftPanel && !f.rightPanel;
        switch (t) {
            case RESUME: return f.resumeChoice;
            case TAKE: return f.takeShown && noPanel && !f.paused;
            case MERC: return f.merc && noPanel && !f.paused;
            case SORT: return f.inventoryOpen && f.sortShown && !f.paused;

            case MATERIALS: return pending.contains(Topic.MATERIALS) && noPanel && !f.paused;
            case RECIPES: return pending.contains(Topic.RECIPES) && noPanel && !f.paused;
            case POINTS: return f.pointsLeft && noPanel && !f.paused;
            case NEW_SKILL: return pending.contains(Topic.NEW_SKILL) && noPanel && !f.paused;
            case PAD: return f.padUsed && !f.paused;
            default: return false;
        }
    }

    private boolean due(Topic t, Facts f) {
        return due(t, f, pending);
    }


    private static boolean stillFits(Topic t, Facts f) {
        if (f.blocked || f.otherWindow) return false;
        switch (t) {
            case RESUME: return f.resumeChoice;
            case SORT: return f.inventoryOpen && !f.paused;
            default: return !f.paused;
        }
    }




    public String encode() {
        StringBuilder sb = new StringBuilder();
        for (Topic t : seen) {
            if (sb.length() > 0) sb.append(',');
            sb.append(t.name());
        }
        return sb.toString();
    }


    public void decode(String stored) {
        seen.clear();
        if (stored == null) return;
        for (String name : stored.split(",")) {
            for (Topic t : Topic.values()) if (t.name().equals(name.trim())) seen.add(t);
        }
    }


    public static String allSeen() {
        D2Hints h = new D2Hints();
        h.seen.addAll(EnumSet.allOf(Topic.class));
        return h.encode();
    }


    public static boolean seenIn(String stored, Topic t) {
        D2Hints h = new D2Hints();
        h.decode(stored);
        return h.seen(t);
    }


    public static String with(String stored, Topic t) {
        D2Hints h = new D2Hints();
        h.decode(stored);
        h.seen.add(t);
        return h.encode();
    }








    public static float left(float width, float boxW, float margin, List<float[]> panels) {
        float centre = (width - boxW) / 2;
        if (free(centre, centre + boxW, panels)) return centre;
        List<float[]> spans = new ArrayList<>();
        float x = margin;
        List<float[]> sorted = new ArrayList<>(panels);
        sorted.sort((a, b) -> Float.compare(a[0], b[0]));
        for (float[] p : sorted) {
            if (p[0] > x) spans.add(new float[]{x, p[0]});
            x = Math.max(x, p[1]);
        }
        if (width - margin > x) spans.add(new float[]{x, width - margin});
        float[] best = null;
        for (float[] s : spans) if (s[1] - s[0] >= boxW && (best == null || s[1] - s[0] > best[1] - best[0])) best = s;
        return best == null ? Float.NaN : (best[0] + best[1] - boxW) / 2;
    }

    private static boolean free(float l, float r, List<float[]> panels) {
        for (float[] p : panels) if (l < p[1] && r > p[0]) return false;
        return true;
    }



    public interface Width {
        int of(String text);
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





    public static long windows(long panelFlags, int escMenu, boolean ownPause) {
        return ownPause ? panelFlags & ~(1L << escMenu) : panelFlags;
    }
}
