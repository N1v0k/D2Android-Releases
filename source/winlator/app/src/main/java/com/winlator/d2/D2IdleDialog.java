package com.winlator.d2;

import com.winlator.R;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.winlator.core.AppUtils;






final class D2IdleDialog {
    static final String KEY_DURATION = "idleDurationMin", KEY_RETREAT = "idleRetreat";
    static final int[] DURATIONS = {15, 30, 60};
    static final int RETREAT_MIN = 10, RETREAT_MAX = 60, RETREAT_DEFAULT = D2Idle.RETREAT_PERCENT;


    interface Start {
        void start(int minutes, int retreatPercent);
    }

    private D2IdleDialog() {}

    private static android.widget.ScrollView scrolling(Activity activity, LinearLayout content) {
        android.widget.ScrollView scroll = new android.widget.ScrollView(activity);
        scroll.addView(content);
        return scroll;
    }


    static void show(Activity activity, String quest, Start onStart) {
        SharedPreferences prefs = activity.getSharedPreferences(D2Settings.PREFS, Activity.MODE_PRIVATE);
        float density = activity.getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, 0, pad, pad);

        root.addView(heading(activity, D2Text.get(activity, R.string.idle_goal)));
        RadioGroup goal = new RadioGroup(activity);
        RadioButton clear = new RadioButton(activity);
        clear.setText(quest != null ? D2Text.get(activity, R.string.idle_goal_quest, quest) : D2Text.get(activity, R.string.idle_goal_clear));
        D2Style.control(clear);
        goal.addView(clear);
        clear.setChecked(true);
        root.addView(goal);
        root.addView(hint(activity, D2Text.get(activity, quest != null ? R.string.idle_goal_quest_hint : R.string.idle_goal_hint)));

        root.addView(heading(activity, D2Text.get(activity, R.string.idle_duration)));
        RadioGroup duration = new RadioGroup(activity);
        duration.setOrientation(RadioGroup.HORIZONTAL);
        int lastMinutes = prefs.getInt(KEY_DURATION, DURATIONS[0]);
        for (int minutes : DURATIONS) {
            RadioButton button = new RadioButton(activity);
            button.setId(1000 + minutes);
            button.setText(minutes + " min");
            D2Style.control(button);
            duration.addView(button);
        }
        duration.check(1000 + (contains(DURATIONS, lastMinutes) ? lastMinutes : DURATIONS[0]));
        root.addView(duration);
        root.addView(hint(activity, D2Text.get(activity, R.string.idle_duration_hint)));

        root.addView(heading(activity, D2Text.get(activity, R.string.idle_retreat)));
        int retreat = Math.max(RETREAT_MIN, Math.min(RETREAT_MAX, prefs.getInt(KEY_RETREAT, RETREAT_DEFAULT)));
        TextView retreatLabel = new TextView(activity);
        D2Style.text(retreatLabel);
        retreatLabel.setText(retreatText(activity, retreat));
        root.addView(retreatLabel);
        SeekBar bar = new SeekBar(activity);
        D2Style.slider(bar);
        bar.setMax((RETREAT_MAX - RETREAT_MIN) / 5);
        bar.setProgress((retreat - RETREAT_MIN) / 5);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                retreatLabel.setText(retreatText(activity, RETREAT_MIN + progress * 5));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(bar);

        root.addView(hint(activity, D2Text.get(activity, R.string.idle_dialog_hint)));

        TextView title = new TextView(activity);
        title.setText(D2Text.get(activity, R.string.idle_title));
        title.setTextSize(20);
        D2Style.heading(title);
        title.setPadding(pad, pad, pad, Math.round(4 * density));
        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setCustomTitle(title)


            .setView(scrolling(activity, root))
            .setPositiveButton(D2Text.get(activity, R.string.idle_start), (d, which) -> {
                int minutes = duration.getCheckedRadioButtonId() - 1000;
                int percent = RETREAT_MIN + bar.getProgress() * 5;
                prefs.edit().putInt(KEY_DURATION, minutes).putInt(KEY_RETREAT, percent).apply();
                onStart.start(minutes, percent);
            })
            .setNegativeButton(D2Text.get(activity, R.string.cancel), null)
            .create();
        D2Sound.attach(dialog);
        dialog.setOnShowListener((d) -> AppUtils.hideSystemUI(activity));
        dialog.show();
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(D2Style.windowBackground(density));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setGravity(Gravity.CENTER);
            window.setLayout(Math.round(activity.getResources().getDisplayMetrics().widthPixels * 0.42f),
                WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    private static String retreatText(Activity activity, int percent) {
        return D2Text.get(activity, R.string.idle_retreat_label, percent);
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) if (v == value) return true;
        return false;
    }

    private static TextView heading(Activity activity, String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(17);
        D2Style.heading(view);
        view.setPadding(0, Math.round(10 * activity.getResources().getDisplayMetrics().density), 0, 0);
        return view;
    }

    private static TextView hint(Activity activity, String text) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(12);
        D2Style.hint(view);
        return view;
    }
}
