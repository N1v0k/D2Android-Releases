package com.winlator.d2;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.winlator.R;

import java.io.File;





final class D2FolderPickDialog {
    private D2FolderPickDialog() {}


    static void show(Activity activity, File picked, GameVersion.Result check, String before, Runnable pickAgain) {
        boolean usable = check != null && (check.status == GameVersion.Status.OK || check.status == GameVersion.Status.MODIFIED);
        D2FolderPick.Outcome outcome = picked == null || check == null ? D2FolderPick.Outcome.NO_PATH
            : D2FolderPick.outcome(picked, usable, check.dir, before, usable ? null : D2FolderPick.gamesBelow(picked));
        String root = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String pickedPath = picked != null ? D2FolderPick.shortPath(picked.getAbsolutePath(), root) : "";
        String found = outcome == D2FolderPick.Outcome.UNCHANGED && !usable ? D2FolderPick.shortPath(before, root)
            : check != null && check.dir != null ? D2FolderPick.shortPath(check.dir.getAbsolutePath(), root) : pickedPath;
        String why = check != null ? GameLocator.message(activity, check) : "";
        D2Trace.log("folder picked: " + D2FolderPick.traceName(picked != null ? picked.getPath() : null) + " -> " + outcome
            + (check != null ? " (" + check.status + ", game " + D2FolderPick.traceName(check.dir != null ? check.dir.getPath() : null) + ")" : ""));
        if (activity.isFinishing() || activity.isDestroyed()) return;
        String title, text;
        switch (outcome) {
            case NO_PATH:
                text = activity.getString(R.string.folder_no_path);
                break;
            case REFUSED:
                text = activity.getString(R.string.folder_pick_refused, pickedPath, why);
                break;
            case UNCHANGED:
                text = activity.getString(R.string.folder_pick_unchanged, found);
                break;
            case SEVERAL: {
                StringBuilder names = new StringBuilder();
                for (File game : D2FolderPick.gamesBelow(picked)) names.append(names.length() > 0 ? ", " : "").append(game.getName());
                text = activity.getString(R.string.folder_pick_several, pickedPath, names.toString());
                break;
            }
            case CHOSEN_BELOW:
                text = activity.getString(R.string.folder_pick_below, found, why);
                break;
            default:
                text = activity.getString(R.string.folder_pick_chosen, found, why);
        }
        switch (D2FolderPick.title(outcome, check != null ? check.status : null)) {
            case NO_PATH: title = activity.getString(R.string.folder_pick_title_no_path); break;
            case WRONG_VERSION: title = activity.getString(R.string.folder_pick_title_version); break;
            case INCOMPLETE: title = activity.getString(R.string.folder_pick_title_incomplete); break;
            case NOT_WRITABLE: title = activity.getString(R.string.folder_pick_title_not_writable); break;
            case UNCHANGED: title = activity.getString(R.string.folder_pick_unchanged_title); break;
            case SEVERAL: title = activity.getString(R.string.folder_pick_title_several); break;
            case CHOSEN: title = activity.getString(R.string.folder_pick_chosen_title); break;
            default: title = activity.getString(R.string.folder_pick_refused_title);
        }
        float density = activity.getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, 0);
        TextView heading = new TextView(activity);
        heading.setText(title);
        heading.setTextSize(18);
        D2Style.heading(heading);
        content.addView(heading);
        TextView message = new TextView(activity);
        message.setText(text);
        message.setTextSize(16);
        message.setPadding(0, Math.round(8 * density), 0, 0);
        D2Style.text(message);
        content.addView(message);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setView(content);
        if (outcome == D2FolderPick.Outcome.REFUSED || outcome == D2FolderPick.Outcome.NO_PATH || outcome == D2FolderPick.Outcome.SEVERAL) {
            builder.setPositiveButton(activity.getString(R.string.folder_pick_again), (d, w) -> pickAgain.run());
            builder.setNegativeButton(activity.getString(R.string.folder_pick_back), null);
        }
        else builder.setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(D2Style.windowBackground(density));
            int width = activity.getResources().getDisplayMetrics().widthPixels;
            window.setLayout(Math.round(width * (width > activity.getResources().getDisplayMetrics().heightPixels ? 0.6f : 0.9f)),
                ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
    }
}
