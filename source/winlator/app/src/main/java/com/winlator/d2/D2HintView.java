package com.winlator.d2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.winlator.R;

import java.util.List;






final class D2HintView {

    static final class Spot {
        final float cx, cy, r;
        final RectF rect;

        Spot(float cx, float cy, float r) {
            this.cx = cx;
            this.cy = cy;
            this.r = r;
            this.rect = null;
        }

        Spot(RectF rect) {
            this.cx = rect.centerX();
            this.cy = rect.centerY();
            this.r = 0;
            this.rect = rect;
        }
    }

    interface Host {

        D2Hud hud();

        float textScale();

        Spot spot(D2Hints.Topic topic);

        List<float[]> panels();

        float top();
    }

    private static final int RING_DARK = Color.rgb(85, 77, 46), RING_GOLD = Color.rgb(199, 179, 119), FILL = Color.argb(225, 10, 8, 6);

    static final int TEXT_BUDGET = 240, PAD = 9, TITLE_H = 18, LINE_H = 13;

    private final Context context;
    private final Host host;
    private float dp;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF box = new RectF(), tmp = new RectF();
    private D2Hints.Topic textTopic;
    private float textK = -1;
    private String title;
    private List<String> lines;

    D2HintView(Context context, Host host, float dp) {
        this.context = context;
        this.host = host;
        this.dp = dp;
        stroke.setStyle(Paint.Style.STROKE);
    }


    void setDp(float dp) {
        this.dp = dp;
    }


    boolean draw(Canvas canvas, D2Hints.Topic topic, int width) {
        box.setEmpty();
        D2Hud hud = host.hud();
        if (topic == null || hud == null || !hud.hasFrame()) return false;
        float k = host.textScale();
        if (topic != textTopic || k != textK) {
            textTopic = topic;
            textK = k;
            title = D2Text.get(context, titleId(topic));
            lines = D2Hints.wrap(D2Text.get(context, textId(topic)), hud::smallTextWidth, TEXT_BUDGET);
        }
        float bw = (TEXT_BUDGET + 2 * PAD) * k, bh = (PAD + TITLE_H + lines.size() * LINE_H + PAD - 2) * k;
        float left = D2Hints.left(width, bw, 8 * dp, host.panels());
        if (Float.isNaN(left)) return false;
        float top = host.top() + 6 * dp;
        Spot spot = host.spot(topic);
        if (spot != null) {
            if (spot.rect != null) {
                tmp.set(spot.rect.left - 3 * k, spot.rect.top - 3 * k, spot.rect.right + 3 * k, spot.rect.bottom + 3 * k);
                hud.drawFrame(canvas, tmp, k, Color.TRANSPARENT);
            }
            else {
                stroke.setStrokeWidth(3 * dp);
                stroke.setColor(RING_DARK);
                canvas.drawCircle(spot.cx, spot.cy, spot.r + 2 * dp, stroke);
                stroke.setStrokeWidth(1.5f * dp);
                stroke.setColor(RING_GOLD);
                canvas.drawCircle(spot.cx, spot.cy, spot.r + 2 * dp, stroke);
            }
        }
        box.set(left, top, left + bw, top + bh);
        hud.drawFrame(canvas, box, k, FILL);
        float x = box.left + PAD * k, y = box.top + PAD * k;
        hud.drawText(canvas, title, x + hud.textWidth(title) * k / 2, y + TITLE_H * k / 2, k);
        y += TITLE_H * k;
        for (String line : lines) {
            hud.drawSmallText(canvas, line, x + hud.smallTextWidth(line) * k / 2, y + LINE_H * k / 2, k);
            y += LINE_H * k;
        }
        return true;
    }


    boolean contains(float x, float y) {
        return !box.isEmpty() && box.contains(x, y);
    }

    static int titleId(D2Hints.Topic t) {
        switch (t) {
            case RESUME: return R.string.hint_resume_title;
            case TAKE: return R.string.hint_take_title;
            case MERC: return R.string.hint_merc_title;
            case SORT: return R.string.hint_sort_title;
            case MATERIALS: return R.string.hint_materials_title;
            case RECIPES: return R.string.hint_recipes_title;
            case POINTS: return R.string.hint_points_title;
            case NEW_SKILL: return R.string.hint_skill_title;
            default: return R.string.hint_pad_title;
        }
    }

    static int textId(D2Hints.Topic t) {
        switch (t) {
            case RESUME: return R.string.hint_resume_text;
            case TAKE: return R.string.hint_take_text;
            case MERC: return R.string.hint_merc_text;
            case SORT: return R.string.hint_sort_text;
            case MATERIALS: return R.string.hint_materials_text;
            case RECIPES: return R.string.hint_recipes_text;
            case POINTS: return R.string.hint_points_text;
            case NEW_SKILL: return R.string.hint_skill_text;
            default: return R.string.hint_pad_text;
        }
    }
}
