package com.winlator.d2;

import com.winlator.R;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public final class D2Effects {
    public static final class Effect {
        public final int state, left, full;
        public final boolean points;

        Effect(int state, int left, int full) {
            this(state, left, full, false);
        }

        Effect(int state, int left, int full, boolean points) {
            this.state = state;
            this.left = left;
            this.full = full;
            this.points = points;
        }
    }

    private static final class Info {
        final int state, skill;
        final String name, picture;
        final int effect;

        Info(int state, String name, int effect, int skill, String picture) {
            this.state = state;
            this.name = name;
            this.effect = effect;
            this.skill = skill;
            this.picture = picture;
        }

        boolean shrine() {
            return state >= 128 && state <= 137;
        }
    }

    private static final Info[] INFO = {
        new Info(128, "ShrId6", R.string.effect_armor, 104, null),
        new Info(129, "ShrId7", R.string.effect_combat, 98, null),
        new Info(130, "ShrId10", R.string.effect_res_lightning, 110, null),
        new Info(131, "ShrId8", R.string.effect_res_fire, 100, null),
        new Info(132, "ShrId9", R.string.effect_res_cold, 105, null),
        new Info(133, "ShrId11", R.string.effect_res_poison, 109, null),
        new Info(134, "ShrId12", R.string.effect_skill, 155, null),
        new Info(135, "ShrId13", R.string.effect_mana_recharge, 120, null),
        new Info(136, "ShrId14", R.string.effect_stamina, 115, null),
        new Info(137, "ShrId15", R.string.effect_experience, -1, "level"),
        new Info(100, "hp3", R.string.effect_life_potion, -1, "hp"),
        new Info(106, "mp3", R.string.effect_mana_potion, -1, "mp"),
        new Info(180, "vps", R.string.effect_stamina_potion, -1, "vps"),
        new Info(178, "yps", R.string.effect_antidote, -1, "yps"),
        new Info(179, "wms", R.string.effect_thawing, -1, "wms"),
    };
    private static final int MAX = D2EffectRules.MAX, ICON = D2EffectRules.ICON;
    private static final int FILL = 0xD8080705, VEIL = 0xA0000000;
    private static final int SHRINE_FILL = 0xFF000000;
    private final Paint black = new Paint();

    {
        black.setColor(SHRINE_FILL);
    }
    private final java.util.Map<Integer, Long> firstSeen = new java.util.HashMap<>();
    private int[] shown = new int[0];

    private Effect[] current = new Effect[0];
    private String message;
    private long messageUntil;
    private final RectF[] hits = new RectF[MAX];
    private final int[] hitState = new int[MAX];
    private int hitCount;
    private long drawnAt;
    private static final long HITS_VALID_MS = 600;
    private final Paint veil = new Paint(), picture = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF box = new RectF();

    private android.content.Context context;
    private D2Hud hud;

    public void setTexts(android.content.Context context, D2Hud hud) {
        this.context = context;
        this.hud = hud;
    }

    private String text(int id, Object... args) {
        return context == null ? "" : D2Text.get(context, id, args);
    }

    private String name(Info info) {
        String name = hud != null ? hud.gameText(info.name) : null;
        return name != null ? name : info.name;
    }

    public D2Effects() {
        for (int i = 0; i < MAX; i++) hits[i] = new RectF();
        veil.setColor(VEIL);
    }

    static Effect[] parse(byte[] d, int length) {
        if (length < 10) return new Effect[0];
        int n = d[9] & 0xFF;
        if (length < 10 + n * 6) return new Effect[0];
        Effect[] out = new Effect[n];
        for (int i = 0; i < n; i++) {
            int at = 10 + i * 6, state = u16(d, at);
            out[i] = new Effect(state & 0x7FFF, u16(d, at + 2), u16(d, at + 4), (state & 0x8000) != 0);
        }
        return out;
    }

    public void update(Effect[] effects, long now) {
        if (effects == current) return;
        java.util.Set<Integer> on = new java.util.HashSet<>();
        int[] states = new int[effects.length], groups = new int[effects.length];
        int[][] skills = hud != null ? hud.stateSkills() : null;
        for (int i = 0; i < effects.length; i++) {
            Effect e = effects[i];
            on.add(e.state);
            if (!firstSeen.containsKey(e.state)) firstSeen.put(e.state, now);
            states[i] = e.state;
            int skill = D2EffectRules.skillOf(e.state, skills);
            groups[i] = picture(e.state) ? D2EffectRules.group(e.state, skills, hud != null ? hud.skillClassOf(skill) : -1,
                e.full > 0) : D2EffectRules.NONE;
        }
        firstSeen.keySet().retainAll(on);
        shown = D2EffectRules.order(states, groups, firstSeen);
        current = effects;
        if (effects.length == 0) hitCount = 0;
    }

    private boolean picture(int state) {
        if (info(state) != null) return true;
        int h = D2EffectRules.harm(state);
        if (h >= 0 && D2EffectRules.HARM_PICTURES[h] != null) return true;
        int skill = D2EffectRules.skillOf(state, hud != null ? hud.stateSkills() : null);
        return skill >= 0 && hud != null && hud.skillIcon(skill) != null;
    }

    private Bitmap icon(D2Hud hud, int state) {
        Info info = info(state);
        Bitmap shrine = info != null && info.shrine() ? hud.shrinePicture(state) : null;
        if (shrine != null) return shrine;
        if (info != null) return info.skill >= 0 ? hud.skillIcon(info.skill) : hud.effectPicture(info.picture);
        int h = D2EffectRules.harm(state);
        if (h >= 0 && D2EffectRules.HARM_PICTURES[h] != null) return hud.effectPicture(D2EffectRules.HARM_PICTURES[h]);
        int skill = D2EffectRules.skillOf(state, hud.stateSkills());
        return skill >= 0 ? hud.skillIcon(skill) : null;
    }

    private static final int[] HARM_TEXTS = {R.string.effect_harm_frozen, R.string.effect_harm_poisoned,
        R.string.effect_harm_chilled, R.string.effect_harm_stunned, R.string.effect_harm_slowed,
        R.string.effect_harm_heal_prevented, R.string.effect_harm_open_wounds};

    private String describe(Effect e) {
        Info info = info(e.state);
        String what;
        if (info != null) what = name(info) + ": " + text(info.effect);
        else if (D2EffectRules.harm(e.state) >= 0) what = text(HARM_TEXTS[D2EffectRules.harm(e.state)]);
        else {
            int skill = D2EffectRules.skillOf(e.state, hud != null ? hud.stateSkills() : null);
            String name = hud != null ? hud.skillName(skill) : null, small = hud != null ? hud.skillShort(skill) : null;
            what = (name != null ? name : "?") + (small != null ? ": " + small : "");
        }
        if (e.points && e.full > 0) {
            return what + " - " + text(R.string.effect_points, D2EffectRules.percent(e.left, e.full), e.left, e.full);
        }
        if (e.full > 0) return what + " - " + text(R.string.effect_left, time(e.left))
            + (info != null && info.shrine() ? " " + text(R.string.effect_curse_ends) : "");
        if (info != null) return what;
        int skill = D2EffectRules.skillOf(e.state, hud != null ? hud.stateSkills() : null);
        int group = D2EffectRules.group(e.state, hud != null ? hud.stateSkills() : null, hud != null ? hud.skillClassOf(skill) : -1, false);
        return what + " - " + text(group == D2EffectRules.AURA ? R.string.effect_while_on : R.string.effect_time_unknown);
    }

    public boolean active(long now) {
        return current.length > 0 || now < messageUntil;
    }

    public boolean tap(float x, float y, long now) {
        int state = hitAt(x, y, now);
        if (state < 0) return false;
        Effect e = find(state);
        if (e != null) {
            message = describe(e);
            messageUntil = now + 4000;
        }
        return true;
    }

    public int hitAt(float x, float y, long now) {
        if (now - drawnAt > HITS_VALID_MS) hitCount = 0;
        for (int i = 0; i < hitCount; i++) if (hits[i].contains(x, y) && isOn(current, hitState[i])) return hitState[i];
        return -1;
    }

    public void draw(Canvas canvas, D2Hud hud, int gameWidth, int gameHeight, float scale, float x0, float y0, long now) {
        hitCount = 0;
        drawnAt = now;
        if (hud == null) return;
        for (int state : shown) {
            if (hitCount >= MAX) break;
            Effect e = find(state);
            if (e == null) continue;
            Bitmap icon = icon(hud, state);
            int[] at = D2EffectRules.place(hitCount);
            box.set(x0 + at[0] * scale, y0 + at[1] * scale, x0 + (at[0] + D2EffectRules.BOX) * scale, y0 + (at[1] + D2EffectRules.BOX) * scale);
            hits[hitCount].set(box);
            hitState[hitCount++] = e.state;
            boolean blink = !e.points && e.full > 0 && e.left <= 100 && (now / 400) % 2 == 0;
            boolean shrine = hud.shrinePicture(e.state) != null && info(e.state) != null && info(e.state).shrine();
            if (hud.hasFrame()) hud.drawFrame(canvas, box, scale, shrine ? SHRINE_FILL : FILL);
            else if (shrine) canvas.drawRect(box, black);
            RectF inner = new RectF(box.left + 3 * scale, box.top + 3 * scale, box.right - 3 * scale, box.bottom - 3 * scale);
            if (icon != null) {
                picture.setAlpha(blink ? 110 : 255);
                canvas.drawBitmap(icon, null, fit(icon, inner), picture);
            }
            if (e.full > 0) {
                float gone = D2EffectRules.gone(e.left, e.full);
                canvas.drawRect(inner.left, inner.top, inner.right, inner.top + inner.height() * gone, veil);
                String under = e.points ? D2EffectRules.percent(e.left, e.full) + "%" : time(e.left);
                hud.drawSmallText(canvas, under, box.centerX(), box.bottom + 7 * scale, scale);
            }
        }
        if (message != null && now < messageUntil) {
            float w = hud.textWidth(message) * scale + 28 * scale, h = 26 * scale, cy = y0 + gameHeight * 0.28f * scale;
            float cx = x0 + gameWidth / 2f * scale;
            box.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
            if (hud.hasFrame()) hud.drawFrame(canvas, box, scale, FILL);
            hud.drawText(canvas, message, cx, cy, scale);
        }
    }

    static String time(int tenths) {
        int s = (tenths + 9) / 10;
        return s >= 60 ? s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60 : Integer.toString(s);
    }

    private static RectF fit(Bitmap b, RectF in) {
        float k = Math.min(in.width() / b.getWidth(), in.height() / b.getHeight());
        float w = b.getWidth() * k, h = b.getHeight() * k;
        return new RectF(in.centerX() - w / 2, in.centerY() - h / 2, in.centerX() + w / 2, in.centerY() + h / 2);
    }

    private Effect find(int state) {
        for (Effect e : current) if (e.state == state) return e;
        return null;
    }

    private static boolean isOn(Effect[] effects, int state) {
        for (Effect e : effects) if (e.state == state) return true;
        return false;
    }

    private static Info info(int state) {
        for (Info i : INFO) if (i.state == state) return i;
        return null;
    }

    private static int u16(byte[] d, int at) {
        return (d[at] & 0xFF) | (d[at + 1] & 0xFF) << 8;
    }
}
