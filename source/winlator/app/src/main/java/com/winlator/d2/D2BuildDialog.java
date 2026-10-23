package com.winlator.d2;

import com.winlator.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;
















final class D2BuildDialog {


    private static final String[] CLASS_KEYS = {"Amazon", "Sorceress", "Necromancer", "Paladin", "Barbarian", "Druid", "Assassin"};

    private static final int[] CLASS_NAMES = {R.string.class_amazon, R.string.class_sorceress, R.string.class_necromancer,
        R.string.class_paladin, R.string.class_barbarian, R.string.class_druid, R.string.class_assassin};

    private D2BuildDialog() {}





    static void show(Activity activity, D2Hud hud, D2ModState s, D2Points points, String name, D2PointHighlight builds,
                     Runnable spendAfterRespec) {
        float density = activity.getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, 0, pad, pad);
        int cls = s != null ? s.characterClass : -1;
        if (cls < 0 || name == null) {
            root.addView(text(activity, D2Text.get(activity, R.string.build_no_character)));
        }
        else {
            String className = className(activity, hud, cls);
            root.addView(heading(activity, points != null
                ? D2Text.get(activity, R.string.build_head, name, className, points.level)
                : D2Text.get(activity, R.string.build_head_nolevel, name, className)));
            if (points != null) {
                root.addView(hint(activity, D2Text.get(activity, R.string.build_open_points, points.statPoints, points.skillPoints)));
            }
            List<D2PointPlan.Build> list = builds.buildsOf(cls);
            if (list.isEmpty()) root.addView(text(activity, D2Text.get(activity, R.string.build_none)));
            else {
                LinearLayout details = new LinearLayout(activity);
                details.setOrientation(LinearLayout.VERTICAL);
                D2PointPlan.Build current = builds.build(cls, name);
                root.addView(heading(activity, D2Text.get(activity, R.string.build_choice)));
                RadioGroup choice = new RadioGroup(activity);
                for (int i = 0; i < list.size(); i++) {
                    RadioButton button = new RadioButton(activity);
                    button.setId(View.generateViewId());
                    D2PointPlan.Build b = list.get(i);
                    String buildName = D2Text.get(activity, texts(activity, b.id)[0]);

                    button.setText(com.winlator.BuildConfig.D2_INTERNAL ? D2Text.get(activity, R.string.build_choice_idle, buildName,
                        D2Text.get(activity, idleLabel(b.idle))) : buildName);
                    D2Style.control(button);
                    choice.addView(button);
                    if (list.get(i) == current) button.setChecked(true);
                }
                choice.setOnCheckedChangeListener((group, id) -> {
                    int index = group.indexOfChild(group.findViewById(id));
                    if (index < 0 || index >= list.size()) return;
                    builds.choose(cls, name, list.get(index).id);
                    fill(activity, details, hud, s, points, list.get(index));
                });
                root.addView(choice);
                D2ModLink link = builds.link();
                boolean respec = respecAvailable(activity, s, link);
                root.addView(hint(activity, D2Text.get(activity, respec ? R.string.build_choice_hint_respec : R.string.build_choice_hint)));
                if (respec) root.addView(respecButton(activity, link, spendAfterRespec), new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                root.addView(details);
                fill(activity, details, hud, s, points, current);
            }
        }
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(root);
        TextView title = new TextView(activity);
        title.setText(D2Text.get(activity, R.string.build_title));
        title.setTextSize(20);
        D2Style.heading(title);
        title.setPadding(pad, pad, pad, Math.round(4 * density));
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setCustomTitle(title)
            .setView(scroll)
            .setPositiveButton(D2Text.get(activity, R.string.done), null)
            .create();
        dialog.show();
        D2Sound.attach(dialog);
        Button done = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (done != null) done.setTextColor(D2Style.GOLD);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(D2Style.windowBackground(density));
            window.setGravity(Gravity.CENTER);
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            window.setLayout(Math.round(metrics.widthPixels * 0.62f), Math.round(metrics.heightPixels * 0.94f));
        }
    }


    private static void fill(Activity a, LinearLayout box, D2Hud hud, D2ModState s, D2Points points, D2PointPlan.Build b) {
        box.removeAllViews();
        int[] t = texts(a, b.id);
        com.winlator.d2.test.Fertigkeiten table = hud != null ? hud.fertigkeiten : null;

        String reason = !b.idleReason.isEmpty() ? D2Text.get(a, textId(a, "build_idle_reason_" + b.idleReason, R.string.build_no_text)) : "";
        int noLine = com.winlator.d2.test.Kampfwahl.eigeneListe(b.characterClass) ? R.string.build_idle_line_nein
            : R.string.build_idle_line_nein_attack;
        TextView idle = text(a, D2PointPlan.IDLE_NO.equals(b.idle) ? D2Text.get(a, noLine, reason)
            : D2PointPlan.IDLE_PARTLY.equals(b.idle) ? D2Text.get(a, R.string.build_idle_line_teilweise, reason)
            : D2Text.get(a, R.string.build_idle_line_ja));
        idle.setPadding(0, Math.round(8 * a.getResources().getDisplayMetrics().density), 0, 0);
        if (com.winlator.BuildConfig.D2_INTERNAL) box.addView(idle);
        if (!b.weapon.isEmpty()) {
            box.addView(text(a, D2Text.get(a, R.string.build_weapon, D2Text.get(a, textId(a, "build_weapon_" + b.weapon, R.string.build_no_text)))));
        }
        int outside = D2BuildPage.outside(b, table, points);
        if (outside > 0) {
            TextView row = text(a, D2Text.get(a, R.string.build_outside, outside));
            row.setTextColor(D2Style.GOLD);
            box.addView(row);
        }
        box.addView(heading(a, D2Text.get(a, R.string.build_strengths)));
        box.addView(text(a, D2Text.get(a, t[1])));
        box.addView(heading(a, D2Text.get(a, R.string.build_weaknesses)));
        box.addView(text(a, withoutIdle(D2Text.get(a, t[2]))));
        box.addView(heading(a, D2Text.get(a, R.string.build_tips)));
        box.addView(text(a, D2Text.get(a, t[3])));

        box.addView(heading(a, D2Text.get(a, R.string.build_plan)));
        int[] states = D2BuildPage.states(b, table, points);
        int next = D2BuildPage.next(b, table, points);
        for (int i = 0; i < b.entries.size(); i++) {
            D2PointPlan.Entry e = b.entries.get(i);
            String line = D2Text.get(a, R.string.build_plan_row, e.fromLevel, skillName(hud, e.skill), e.target);
            if (points != null) line += " " + D2Text.get(a, R.string.build_plan_now, points.baseLevel(e.skill));
            if (states != null) {
                int st = states[i];
                line += " – " + D2Text.get(a, st == D2BuildPage.DONE ? R.string.build_state_done
                    : st == D2BuildPage.NEXT ? R.string.build_state_next : R.string.build_state_later);
                if (st == D2BuildPage.NEXT && next >= 0 && next != e.skill) {
                    line += " " + D2Text.get(a, R.string.build_state_first, skillName(hud, next));
                }
            }
            TextView row = text(a, line);
            if (states != null && states[i] == D2BuildPage.NEXT) row.setTextColor(D2Style.GOLD);
            else if (states != null && states[i] == D2BuildPage.DONE) row.setAlpha(0.6f);
            box.addView(iconRow(a, hud, e.skill, row));
        }

        box.addView(heading(a, D2Text.get(a, R.string.build_stats)));
        box.addView(text(a, D2Text.get(a, R.string.build_stat_rule, rule(a, D2BuildPage.targets(b, D2PointPlan.STR)),
            rule(a, D2BuildPage.targets(b, D2PointPlan.DEX)))));

        box.addView(heading(a, D2Text.get(a, R.string.build_keys)));
        for (int k = 0; k < 4; k++) {
            int skill = s != null ? s.hotkeySkill[k] : -1;
            TextView row = text(a, "F" + (k + 1) + ": " + (skill >= 0 ? skillName(hud, skill) : D2Text.get(a, R.string.build_key_free)));
            box.addView(iconRow(a, hud, skill, row));
        }
        List<String> keys = new ArrayList<>();
        int level = points != null ? points.level : 1;
        for (D2PointPlan.Key k : b.keys) keys.add(skillName(hud, k.skill) + " (" + D2Text.get(a, role(k.role(level))) + ")");
        box.addView(hint(a, D2Text.get(a, R.string.build_keys_of_build, android.text.TextUtils.join(", ", keys))));

        box.addView(heading(a, D2Text.get(a, R.string.build_gear)));
        box.addView(hint(a, D2Text.get(a, R.string.build_gear_later)));

        if (!b.sources.isEmpty()) {
            box.addView(heading(a, D2Text.get(a, R.string.build_sources)));
            TextView links = text(a, android.text.TextUtils.join("\n", b.sources));
            Linkify.addLinks(links, Linkify.WEB_URLS);
            links.setLinkTextColor(D2Style.GOLD);
            links.setMovementMethod(LinkMovementMethod.getInstance());
            box.addView(links);
        }
    }


    private static String rule(Activity a, int[][] steps) {
        if (steps.length == 0) return D2Text.get(a, R.string.build_stat_nothing);
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < steps.length; i++) {
            parts.add(i == 0 && steps[i][0] <= 1 ? D2Text.get(a, R.string.build_stat_upto, steps[i][1])
                : D2Text.get(a, R.string.build_stat_from, steps[i][0], steps[i][1]));
        }
        return android.text.TextUtils.join(", ", parts);
    }

    private static int role(String role) {
        switch (role) {
            case "haupt": return R.string.build_role_main;
            case "stark": return R.string.build_role_strong;
            case "nova": return R.string.build_role_nova;
            case "taste": return R.string.build_role_key;
            default: return R.string.build_role_buff;
        }
    }

    private static int idleLabel(String idle) {
        return D2PointPlan.IDLE_NO.equals(idle) ? R.string.build_idle_nein
            : D2PointPlan.IDLE_PARTLY.equals(idle) ? R.string.build_idle_teilweise : R.string.build_idle_ja;
    }





    static boolean respecAvailable(Context c, D2ModState s, D2ModLink link) {
        return s != null && s.plugyLoaded && link != null && link.singlePlayer() && D2Settings.respecStarted(c);
    }





    private static View respecButton(Activity a, D2ModLink link, Runnable spendAfterRespec) {
        Button button = new Button(a);
        button.setText(D2Text.get(a, R.string.build_respec));
        D2Style.button(button, a.getResources().getDisplayMetrics().density);
        long[] armedUntil = {0};
        button.setOnClickListener(v -> {
            long now = SystemClock.uptimeMillis();
            if (armedUntil[0] < now) {
                armedUntil[0] = now + 4000;
                button.setText(D2Text.get(a, R.string.build_respec_again));
                D2Style.buttonText(button, D2Style.RED);
                button.postDelayed(() -> {
                    if (armedUntil[0] == 0 || SystemClock.uptimeMillis() < armedUntil[0]) return;
                    button.setText(D2Text.get(a, R.string.build_respec));
                    D2Style.buttonText(button, D2Style.GOLD);
                }, 4100);
                return;
            }
            armedUntil[0] = 0;
            for (int code = D2ModLink.PLUGY_UNASSIGN_STR; code <= D2ModLink.PLUGY_UNASSIGN_SKILLS; code++) link.plugy(code);
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            if (spendAfterRespec == null) {
                button.setText(D2Text.get(a, R.string.build_respec_done));
                D2Style.buttonText(button, D2Style.GOLD);
                button.setEnabled(false);
                return;
            }
            offerSpend(a, button, spendAfterRespec);
        });
        return button;
    }





    private static void offerSpend(Activity a, Button button, Runnable spend) {
        button.setText(D2Text.get(a, R.string.build_spend));
        D2Style.buttonText(button, D2Style.GOLD);
        long[] armedUntil = {0};
        button.setOnClickListener(v -> {
            long now = SystemClock.uptimeMillis();
            if (armedUntil[0] < now) {
                armedUntil[0] = now + 4000;
                button.setText(D2Text.get(a, R.string.build_spend_again));
                D2Style.buttonText(button, D2Style.RED);
                button.postDelayed(() -> {
                    if (armedUntil[0] == 0 || SystemClock.uptimeMillis() < armedUntil[0]) return;
                    button.setText(D2Text.get(a, R.string.build_spend));
                    D2Style.buttonText(button, D2Style.GOLD);
                }, 4100);
                return;
            }
            armedUntil[0] = 0;
            spend.run();
            v.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            button.setText(D2Text.get(a, R.string.build_spend_started));
            D2Style.buttonText(button, D2Style.GOLD);
            button.setEnabled(false);
        });
    }





    static int[] texts(Context c, String id) {
        String key = "build_" + id.replace('-', '_');
        return new int[]{textId(c, key, R.string.build_unnamed), textId(c, key + "_strengths", R.string.build_no_text),
            textId(c, key + "_weaknesses", R.string.build_no_text), textId(c, key + "_tips", R.string.build_no_text)};
    }





    static String withoutIdle(String text) {
        return com.winlator.BuildConfig.D2_INTERNAL ? text : dropIdleLines(text);
    }

    static String dropIdleLines(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (line.contains("Idle")) continue;
            if (out.length() > 0) out.append('\n');
            out.append(line);
        }
        return out.toString();
    }


    private static int textId(Context c, String name, int fallback) {
        int id = c.getResources().getIdentifier(name, "string", c.getPackageName());
        return id != 0 ? id : fallback;
    }

    private static String className(Activity a, D2Hud hud, int cls) {
        if (cls >= CLASS_KEYS.length) return "#" + cls;
        String game = hud != null ? hud.gameText(CLASS_KEYS[cls]) : null;
        return game != null && !game.isEmpty() ? game : D2Text.get(a, CLASS_NAMES[cls]);
    }

    private static String skillName(D2Hud hud, int skill) {
        String n = hud != null ? hud.skillName(skill) : null;
        return n != null && !n.isEmpty() ? n : "#" + skill;
    }


    private static View iconRow(Activity a, D2Hud hud, int skill, TextView text) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int size = Math.round(28 * a.getResources().getDisplayMetrics().density);
        ImageView icon = new ImageView(a);
        Bitmap b = hud != null && skill >= 0 ? hud.skillIcon(skill) : null;
        if (b != null) icon.setImageBitmap(b);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMarginEnd(size / 3);
        row.addView(icon, lp);
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.setPadding(0, size / 8, 0, size / 8);
        return row;
    }

    private static TextView heading(Activity a, String s) {
        TextView v = new TextView(a);
        v.setText(s);
        v.setTextSize(16);
        D2Style.heading(v);
        v.setPadding(0, Math.round(12 * a.getResources().getDisplayMetrics().density), 0, 0);
        return v;
    }

    private static TextView text(Activity a, String s) {
        TextView v = new TextView(a);
        v.setText(s);
        v.setTextSize(14);
        D2Style.text(v);
        return v;
    }

    private static TextView hint(Activity a, String s) {
        TextView v = new TextView(a);
        v.setText(s);
        v.setTextSize(13);
        D2Style.hint(v);
        return v;
    }
}
