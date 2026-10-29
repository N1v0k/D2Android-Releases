package com.winlator.d2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.RectF;

import com.winlator.R;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
















final class D2PointHighlight {

    static final String KEY = "build.";
    private static final long PULSE_MS = 1200;

    private static final float FRAME_SCALE = 2;
    private final int[] frameSpot = new int[4];


    final float[] mapping = new float[3];

    private final Context context;
    private final SharedPreferences prefs;

    private final Consumer<String> note;

    private final boolean statsPages;
    private List<D2PointPlan.Build> builds;
    private final RectF where = new RectF();
    private final int[] skillSpot = new int[4], statSpot = new int[4];


    private int buildClass = -1;
    private String buildName, buildKey, buildChosen;
    private D2PointPlan.Build build;


    private D2Points planned;
    private D2PointPlan.Build plannedFor;
    private int skill = -1, place, stat = -1;
    private final IntUnaryOperator baseLevel = id -> planned.baseLevel(id);


    private int loggedSkill = -1, loggedPlace, loggedTab, loggedStat = -1;

    D2PointHighlight(Context context, SharedPreferences prefs, Consumer<String> note) {
        this.context = context.getApplicationContext();
        this.prefs = prefs;
        this.note = note;
        statsPages = D2Settings.statsPages(context);
    }


    private List<D2PointPlan.Build> builds() {
        if (builds != null) return builds;
        try (InputStream in = context.getAssets().open("builds.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            builds = D2PointPlan.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
        }
        catch (java.io.IOException e) {
            builds = new ArrayList<>();
        }
        return builds;
    }


    D2PointPlan.Build build(int characterClass, String name) {
        if (characterClass != buildClass || (name == null ? buildName != null : !name.equals(buildName))) {
            buildClass = characterClass;
            buildName = name;
            buildKey = name != null && characterClass >= 0 ? KEY + characterClass + "." + name : null;
            buildChosen = null;
            build = null;
        }
        String chosen = buildKey != null ? prefs.getString(buildKey, "") : "";
        if (buildChosen == null || !chosen.equals(buildChosen)) {
            buildChosen = chosen;
            build = D2PointPlan.byId(builds(), chosen, characterClass);
        }
        return build;
    }


    List<D2PointPlan.Build> buildsOf(int characterClass) {
        List<D2PointPlan.Build> out = new ArrayList<>();
        for (D2PointPlan.Build b : builds()) if (b.characterClass == characterClass) out.add(b);
        return out;
    }


    void choose(int characterClass, String name, String id) {
        if (name == null || characterClass < 0) return;
        prefs.edit().putString(KEY + characterClass + "." + name, id).apply();
        D2Trace.log("points: build of " + characterClass + "." + name + " is " + id);
    }






    boolean chosen(int characterClass, String name) {
        if (name == null || characterClass < 0) return false;
        String id = prefs.getString(KEY + characterClass + "." + name, "");
        if (id.isEmpty()) return false;
        for (D2PointPlan.Build b : builds()) if (b.id.equals(id) && b.characterClass == characterClass) return true;
        return false;
    }







    boolean draw(Canvas canvas, D2Hud hud, D2ModState s, D2Points points, String name, float[] m, long now) {
        boolean tree = s != null && s.isOpen(D2ModState.UI_SKILLTREE), sheet = s != null && s.isOpen(D2ModState.UI_CHARACTER);
        if (!tree && !sheet) {
            loggedSkill = loggedStat = -1;
            return off(1, "no skill tree or character screen open");
        }

        if (hud == null || !hud.hasFrame()) return off(2, "no frame graphics of the game (MENU\\boxpieces)");
        if (hud.fertigkeiten == null) return off(3, "no skill table (skills.bin)");
        if (points == null) return off(4, "no fresh points block (type 6 older than 1.5 s or missing)");
        if (s.gameWidth <= 0) return off(5, "no game size");
        D2PointPlan.Build b = build(s.characterClass, name);
        if (b == null) return off(6, "no build for this class");
        if (points != planned || b != plannedFor) plan(b, hud, points);
        boolean skillShown = false, statShown = false;
        if (tree && place != 0) {
            int page = place >> 16 & 0xFF, row = place >> 8 & 0xFF, column = place & 0xFF;

            skillShown = page == points.tab ? D2PointSpots.skillBox(points, column, row, skillSpot)
                : D2PointSpots.tab(points, page, skillSpot);
        }
        boolean statWanted = sheet && stat >= 0
            && (points.statsPage >= 0 ? points.statsPage == 0 : !(s.plugyLoaded && statsPages));
        if (statWanted) statShown = D2PointSpots.statPlus(points, s.gameWidth, stat, statSpot);
        log(b, tree && place != 0 ? skill : -1, points.tab, statWanted ? stat : -1, skillShown, statShown, s, m, canvas);
        if (!skillShown && !statShown) {
            if (tree && skill < 0) return off(7, "skill tree: no skill recommended (no free point or none the server takes)");
            if (tree && place == 0) return off(8, "skill tree: the recommended skill has no place in skilldesc");
            if (tree) return off(9, "skill tree: sites unknown (mod feature bit 0)");
            return off(10, "character screen: no stat point, base stats unknown or plugy pages on");
        }
        lastOff = 0;

        int alpha = (int) (150 + 105 * (0.5 + 0.5 * Math.sin(2 * Math.PI * (now % PULSE_MS) / PULSE_MS)));
        if (skillShown) frame(canvas, hud, skillSpot, m, alpha);
        if (statShown) frame(canvas, hud, statSpot, m, alpha);
        return true;
    }


    private void plan(D2PointPlan.Build b, D2Hud hud, D2Points points) {
        planned = points;
        plannedFor = b;
        skill = points.skillPoints > 0 ? D2PointPlan.nextSkill(b, hud.fertigkeiten, points.level, baseLevel) : -1;
        place = skill >= 0 ? hud.skillTreePlace(skill) : 0;
        stat = points.statPoints > 0 && points.baseStatsKnown() ? D2PointPlan.nextStat(b, points.level, points.baseStats) : -1;
    }


    private boolean off(int why, String text) {
        if (why != lastOff) {
            lastOff = why;
            android.util.Log.i("D2Points", "highlight off: " + text);
            D2Trace.log("points: highlight off: " + text);
        }
        return false;
    }

    private int lastOff = -1;


    private void log(D2PointPlan.Build b, int shownSkill, int tab, int shownStat, boolean skillShown, boolean statShown,
                     D2ModState s, float[] m, Canvas canvas) {
        int shownPlace = shownSkill >= 0 ? place : 0, shownTab = shownSkill >= 0 ? tab : 0;
        if (shownSkill == loggedSkill && shownPlace == loggedPlace && shownTab == loggedTab && shownStat == loggedStat) return;
        loggedSkill = shownSkill;
        loggedPlace = shownPlace;
        loggedTab = shownTab;
        loggedStat = shownStat;
        if (shownSkill < 0 && shownStat < 0) return;
        StringBuilder why = new StringBuilder("points: highlight ").append(b.id);
        if (shownSkill >= 0) {
            why.append(" skill ").append(shownSkill).append(" (tab ").append(shownPlace >> 16 & 0xFF)
                .append(", row ").append(shownPlace >> 8 & 0xFF).append(", column ").append(shownPlace & 0xFF)
                .append(", open tab ").append(tab).append(')');
        }
        if (shownStat >= 0) why.append(shownSkill >= 0 ? ", stat " : " stat ").append(shownStat);

        if (skillShown) spotText(why.append("; skill frame "), skillSpot, m);
        if (statShown) spotText(why.append("; stat frame "), statSpot, m);
        why.append("; game ").append(s.gameWidth).append('x').append(s.gameHeight).append(", scale ").append(m[0])
            .append(", view ").append(canvas.getWidth()).append('x').append(canvas.getHeight());
        android.util.Log.i("D2Points", why.toString());
        D2Trace.log(why.toString());
    }

    private static void spotText(StringBuilder b, int[] spot, float[] m) {
        b.append(spot[0]).append(',').append(spot[1]).append(' ').append(spot[2]).append('x').append(spot[3]).append(" -> view ")
            .append(Math.round(m[1] + spot[0] * m[0])).append(',').append(Math.round(m[2] + spot[1] * m[0]));
    }







    void stepKeys(D2ModState s, D2ModLink link, D2Hud hud, boolean paused, long now) {
        String name = link.characterName();
        boolean known = s != null && name != null && s.characterClass >= 0;
        boolean ready = known && link.singlePlayer() && !paused && !s.isOpen(D2ModState.UI_ESCMENU);
        keyLink = link;
        keyHud = hud;
        keyAuto.step(known ? build(s.characterClass, name) : null, link.points(1500), known ? s.characterClass : -1,
            known ? name : null, s != null ? s.hotkeySkill : null, ready, now, keys);
    }


    private final D2PointAuto keyAuto = new D2PointAuto();
    private D2ModLink keyLink;


    D2ModLink link() {
        return keyLink;
    }
    private D2Hud keyHud;
    private final D2PointAuto.Out keys = new D2PointAuto.Out() {
        @Override
        public void bind(int skill, int key) {
            keyLink.points(D2Points.KIND_KEY, skill, key);
        }

        @Override
        public void noKey(int skill) {
            String skillName = keyHud != null ? keyHud.skillName(skill) : null;
            D2Trace.log("points: no free F-key for skill " + skill);
            note.accept(D2Text.get(context, R.string.note_no_free_key,
                skillName != null && !skillName.isEmpty() ? skillName : "#" + skill));
        }
    };






    D2Layer into;
    D2Frame intoFrame;


    static void layerFrame(D2Layer l, D2Frame f, int[] spot) {
        D2MaterialsLayer.scaledFrame(l, f, spot[0], spot[1], spot[2], spot[3], (int) FRAME_SCALE);
    }


    private void frame(Canvas canvas, D2Hud hud, int[] spot, float[] m, int alpha) {
        float k = m[0];
        D2PointSpots.frame(spot, frameSpot);
        if (into != null) {
            layerFrame(into, intoFrame, frameSpot);
            return;
        }
        where.set(m[1] + frameSpot[0] * k, m[2] + frameSpot[1] * k,
            m[1] + (frameSpot[0] + frameSpot[2]) * k, m[2] + (frameSpot[1] + frameSpot[3]) * k);
        hud.drawFrame(canvas, where, FRAME_SCALE * k, 0, alpha);
    }
}
