package com.winlator.d2;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;






public final class D2HelpersDialog {
    private D2HelpersDialog() {}

    public static void show(Activity activity, D2TouchOverlay overlay) {
        float density = activity.getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);


        AlertDialog[] shown = new AlertDialog[1];
        LinearLayout panel = D2HelpersPanel.create(activity, overlay::reloadSettings, overlay::showAimPreview, true, overlay.lootScope(),
            () -> {
                if (shown[0] != null) shown[0].dismiss();
                overlay.startEditMode();
            });
        panel.setPadding(pad, 0, pad, pad);
        D2SettingsLook look = D2SettingsLook.get();
        android.view.View title;
        if (look != null) {

            title = new D2GameText(activity, look, look.font30, 0xC8B074, D2Text.get(activity, com.winlator.R.string.helpers_title));
        }
        else {
            TextView text = new TextView(activity);
            text.setText(D2Text.get(activity, com.winlator.R.string.helpers_title));
            text.setTextSize(20);
            D2Style.heading(text);
            title = text;
        }
        title.setPadding(pad, pad, pad, Math.round(4 * density));
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setCustomTitle(title);
        if (look != null) {

            LinearLayout body = new LinearLayout(activity);
            body.setOrientation(LinearLayout.VERTICAL);
            body.addView(panel, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            android.view.View done = D2HelpersPanel.framedText(activity, look,
                D2Text.get(activity, com.winlator.R.string.done).toUpperCase(java.util.Locale.ROOT), () -> {
                    if (shown[0] != null) shown[0].dismiss();
                });
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            dp.gravity = Gravity.END;
            dp.setMargins(pad, Math.round(6 * density), pad, pad);
            body.addView(done, dp);
            builder.setView(body);
        }
        else builder.setView(panel).setPositiveButton(D2Text.get(activity, com.winlator.R.string.done), null);
        AlertDialog dialog = builder.create();
        shown[0] = dialog;

        dialog.setOnKeyListener((d, code, event) -> code == android.view.KeyEvent.KEYCODE_BACK
            && event.getAction() == android.view.KeyEvent.ACTION_UP && D2HelpersPanel.back(panel));
        D2Sound.attach(dialog);
        dialog.show();
        Button done = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (done != null) done.setTextColor(D2Style.GOLD);
        Window window = dialog.getWindow();
        if (window != null) {

            window.setBackgroundDrawable(D2Style.windowBackground(density));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setGravity(Gravity.END);
            DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
            window.setLayout(Math.round(metrics.widthPixels * 0.42f), Math.round(metrics.heightPixels * 0.94f));
        }
    }
}
