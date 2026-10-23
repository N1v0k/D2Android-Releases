package com.winlator.d2;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.winlator.R;

import java.util.List;








final class D2CharacterList {
    interface Listener {
        void start(D2SaveHeader.Info character, int difficulty);
    }

    private static final int[] CLASS_NAMES = {R.string.class_amazon, R.string.class_sorceress, R.string.class_necromancer,
        R.string.class_paladin, R.string.class_barbarian, R.string.class_druid, R.string.class_assassin};
    private static final int[] DIFFICULTIES = {R.string.difficulty_normal, R.string.difficulty_nightmare, R.string.difficulty_hell};

    private D2CharacterList() {}


    static boolean anyStartable(List<D2SaveHeader.Info> characters) {
        for (D2SaveHeader.Info c : characters) if (c.refusal() == D2SaveHeader.Refusal.NONE) return true;
        return false;
    }


    static void fill(Context context, LinearLayout block, List<D2SaveHeader.Info> characters, Listener listener) {
        block.removeAllViews();
        float density = context.getResources().getDisplayMetrics().density;
        TextView heading = new TextView(context);
        heading.setText(R.string.launch_characters);
        heading.setTextSize(18);
        D2Style.heading(heading);
        heading.setPadding(0, Math.round(8 * density), 0, Math.round(4 * density));
        block.addView(heading);
        boolean first = true;
        for (D2SaveHeader.Info c : characters) {
            block.addView(row(context, c, first, listener, density));
            first = false;
        }
    }

    private static View row(Context context, D2SaveHeader.Info c, boolean last, Listener listener, float density) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(8 * density);
        row.setPadding(pad, pad, pad, pad);
        D2SaveHeader.Refusal refusal = c.refusal();
        boolean startable = refusal == D2SaveHeader.Refusal.NONE;
        TextView name = new TextView(context);
        name.setText(c.name.isEmpty() ? c.file : c.name);
        name.setTextSize(18);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextColor(c.hardcore() ? D2Style.RED : last ? D2Style.GOLD : D2Style.TEXT);
        row.addView(name);
        TextView detail = new TextView(context);
        String cls = c.characterClass < CLASS_NAMES.length ? context.getString(CLASS_NAMES[c.characterClass]) : "?";
        StringBuilder line = new StringBuilder(context.getString(R.string.launch_character_line, cls, c.level));
        if (c.hardcore()) line.append(" · ").append(context.getString(R.string.launch_character_hardcore));
        if (refusal == D2SaveHeader.Refusal.DEAD_HARDCORE) line.append(" · ").append(context.getString(R.string.launch_character_dead));
        else if (refusal == D2SaveHeader.Refusal.NAME_NOT_FILE) line.append(" · ").append(context.getString(R.string.launch_character_menu_only));
        detail.setText(line);
        D2Style.hint(detail);
        row.addView(detail);
        if (!startable) {
            row.setAlpha(0.5f);
            return row;
        }
        row.setClickable(true);
        row.setOnClickListener((v) -> listener.start(c, c.defaultDifficulty()));
        if (c.openDifficulty() > 0) {
            LinearLayout buttons = new LinearLayout(context);
            for (int d = 0; d <= c.openDifficulty(); d++) {
                final int difficulty = d;
                Button b = new Button(context);
                b.setText(DIFFICULTIES[d]);
                if (d == c.defaultDifficulty()) b.setTextColor(D2Style.GOLD);
                b.setOnClickListener((v) -> listener.start(c, difficulty));
                buttons.addView(b, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }
            row.addView(buttons);
        }
        return row;
    }
}
