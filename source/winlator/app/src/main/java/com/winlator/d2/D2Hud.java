package com.winlator.d2;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.File;
import java.io.IOException;










public final class D2Hud {

    public static final int CENTER_HALF = 235, STRIP = 47, PIECE_W = 117, PIECE_H = 104, ORB = 80;

    private static final int BAND_H = STRIP + 2;

    public static final int ICON_CHAR = 0, ICON_INV = 2, ICON_SKILLS = 4, ICON_MAP = 8, ICON_QUESTS = 12, ICON_MENU = 14;

    private final Bitmap angel, demon, lifeOrb, manaOrb, lifeGlass, manaGlass, band;
    private final Bitmap[] mini, run, arrow;

    private final Bitmap hand;

    private final Bitmap pointerHand;
    private final int pointerHandX, pointerHandY;

    private final Bitmap levelCross;

    private final Bitmap mercIcon;

    private final Bitmap cubeIcon;

    private final Bitmap[] duro;
    private final Bitmap[] font, smallFont;

    private final Bitmap[] stoneFont;
    private final int[] stoneFontWidth;

    private final Bitmap[] healPotions = new Bitmap[6], manaPotions = new Bitmap[6];

    private final Bitmap[] rejuvPotions = new Bitmap[2];

    private final Bitmap[] materialPictures = new Bitmap[D2Materials.KINDS];

    private final D2LayerArt layerArt;


    private final Bitmap[] blankButton, mediumButton;

    private final Bitmap[] anvilButton;

    private final String language;
    private final D2Strings strings;


    public String gameText(String key) {
        return strings.get(key);
    }


    D2LayerArt layerArt() {
        return layerArt;
    }

    public String language() {
        return language;
    }


    private final String[] skillName;

    private final String[] skillShort;

    private final int[][] stateSkills;

    public final com.winlator.d2.test.Fertigkeiten fertigkeiten;

    private final boolean[] aimingSkills;

    private final boolean[] passiveSkills;

    public final D2Tabellen tabellen;


    public final int[][] wertBasen, wertBasenLadder;


    public final java.util.Map<String, D2Breakpoints.Anim> animationen;

    private final int[] skillClass, skillCel;

    private final int[] skillPlace;
    private final Bitmap[][] skillIcons = new Bitmap[8][];
    private static final String[] ICON_FILES = {"Am", "So", "Ne", "Pa", "Ba", "Dr", "As", ""};
    private final int[] fontWidth, smallFontWidth;


    private final Bitmap[] gameButton;

    private final D2Frame frame;
    private final android.util.LongSparseArray<Bitmap> frames = new android.util.LongSparseArray<>();

    private final int[][] frameLines;
    private final java.util.HashMap<Integer, Bitmap> rings = new java.util.HashMap<>();

    private final java.util.HashMap<String, Bitmap> effectPictures = new java.util.HashMap<>();

    private final Bitmap[] shrinePictures = new Bitmap[D2ShrinePictures.LAST - D2ShrinePictures.FIRST + 1];
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final Paint pixelPaint = new Paint(), goldPixelText = new Paint();

    private final android.util.LongSparseArray<Bitmap> sharp = new android.util.LongSparseArray<>();
    private final java.util.HashSet<Long> sharpPending = new java.util.HashSet<>();
    private java.util.concurrent.ThreadPoolExecutor sharpWorker;

    private volatile java.lang.ref.WeakReference<android.view.View> sharpView;

    private static final int SHARP_RUN = 100, SHARP_ARROW = 200, SHARP_CROSS = 300, SHARP_MERC = 400, SHARP_CUBE = 500, SHARP_SKILL = 1000;
    private final Paint goldText = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint shadowText = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final android.util.SparseArray<Paint> colourTexts = new android.util.SparseArray<>();

    private final Paint redPixelText = new Paint();

    private final Paint stonePixelText = new Paint();

    private static final int STONE_CELL = 18, STONE_BASE = 9;

    private static final int SMALL_CELL = 14;
    private final Rect src = new Rect();
    private final RectF dst = new RectF();


    private final int[] buttonBox, xpPiece, stonePiece;
    static final int BUTTON_BOX = 33, XP_PIECE_W = 40, XP_PIECE_H = 6, STONE_W = 124, STONE_H = 6;

    private final android.util.LongSparseArray<Bitmap> barPieces = new android.util.LongSparseArray<>();

    private final Bitmap[] tinyFont;
    private final int[] tinyFontWidth;

    private D2Hud(D2Sprites s) throws IOException {
        D2Sprites.Frame[] ctrl = need(s, "data\\global\\ui\\PANEL\\800ctrlpnl7.dc6", 6);
        D2Sprites.Frame[] orbs = need(s, "data\\global\\ui\\PANEL\\hlthmana.dc6", 2);
        D2Sprites.Frame[] glass = need(s, "data\\global\\ui\\PANEL\\overlap.dc6", 2);
        D2Sprites.Frame[] frame = need(s, "data\\global\\ui\\PANEL\\800BorderFrame.dc6", 9);

        angel = mirror(bitmap(ctrl[0]));
        demon = mirror(bitmap(ctrl[5]));
        lifeOrb = mirror(bitmap(orbs[0]));
        manaOrb = mirror(bitmap(orbs[1]));
        lifeGlass = mirror(bitmap(glass[0]));
        manaGlass = mirror(bitmap(glass[1]));
        band = ornamentBand(frame[3], frame[8]);

        mini = croppedAll(need(s, "data\\global\\ui\\PANEL\\minipanelbtn.dc6", 16));
        run = croppedAll(need(s, "data\\global\\ui\\PANEL\\runbutton.dc6", 4));
        arrow = croppedAll(need(s, "data\\global\\ui\\PANEL\\menubutton.dc6", 4));
        D2Sprites.Frame[] hands = s.dc6("data\\global\\ui\\CURSOR\\ohand.dc6");
        hand = hands != null && hands.length > 0 ? cropped(hands[hands.length - 1]) : null;

        pointerHand = hands != null && hands.length > 0 ? bitmap(hands[0]) : null;
        pointerHandX = pointerHand != null ? hands[0].offsetX : 0;
        pointerHandY = pointerHand != null ? hands[0].offsetY - hands[0].height : 0;
        D2Sprites.Frame[] levelFrames = s.dc6("data\\global\\ui\\PANEL\\level.dc6");
        levelCross = levelFrames != null && levelFrames.length > 0 ? cropped(levelFrames[0]) : null;
        D2Sprites.Frame[] mercFrames = s.dc6("data\\global\\ui\\Hireables\\RogueIcon.dc6");
        mercIcon = mercFrames != null && mercFrames.length > 0 ? cropped(mercFrames[0]) : null;
        D2Sprites.Frame[] cubeFrames = s.dc6("data\\global\\items\\invbox.dc6");
        cubeIcon = cubeFrames != null && cubeFrames.length > 0 ? cropped(cubeFrames[0]) : null;
        D2Sprites.Frame[] duroFrames = s.dc6("data\\global\\ui\\PANEL\\DuroIcon.dc6");
        duro = new Bitmap[duroFrames != null ? duroFrames.length : 0];
        for (int i = 0; i < duro.length; i++) duro[i] = cropped(duroFrames[i]);
        font = bitmaps(need(s, "data\\local\\font\\latin\\font16.dc6", 256));
        fontWidth = widths(s.read("data\\local\\font\\latin\\font16.tbl"), 10);
        smallFont = bitmaps(need(s, "data\\local\\font\\latin\\font8.dc6", 256));
        smallFontWidth = widths(s.read("data\\local\\font\\latin\\font8.tbl"), 6);
        D2Sprites.Frame[] exocet = s.dc6("data\\local\\font\\latin\\fontexocet10.dc6");
        stoneFont = exocet != null && exocet.length >= 256 ? bitmaps(exocet) : null;
        stoneFontWidth = stoneFont != null ? widths(s.read("data\\local\\font\\latin\\fontexocet10.tbl"), 9) : null;
        D2Sprites.Frame[] tiny = s.dc6("data\\local\\font\\latin\\font6.dc6");
        byte[] tinyTbl = tiny != null && tiny.length >= 256 ? s.read("data\\local\\font\\latin\\font6.tbl") : null;
        tinyFont = tinyTbl != null ? bitmaps(tiny) : null;
        tinyFontWidth = tinyTbl != null ? widths(tinyTbl, 5) : null;


        buttonBox = crop(ctrl[1], 40, 15, BUTTON_BOX, BUTTON_BOX);
        int[] xpLeft = crop(ctrl[1], 89, 14, 36, XP_PIECE_H), xpRight = crop(ctrl[2], 80, 14, 4, XP_PIECE_H);
        if (xpLeft != null && xpRight != null) {
            xpPiece = new int[XP_PIECE_W * XP_PIECE_H];
            for (int y = 0; y < XP_PIECE_H; y++) {
                System.arraycopy(xpLeft, y * 36, xpPiece, y * XP_PIECE_W, 36);
                System.arraycopy(xpRight, y * 4, xpPiece, y * XP_PIECE_W + 36, 4);
            }
        }
        else xpPiece = null;
        stonePiece = crop(ctrl[3], 0, 9, STONE_W, STONE_H);
        D2Sprites.Frame[] buttonFrames = s.dc6("data\\global\\ui\\PANEL\\buysellbtn.dc6");
        gameButton = buttonFrames != null && buttonFrames.length >= 2 && square(buttonFrames[0]) && square(buttonFrames[1])
            ? new Bitmap[]{pageStone(buttonFrames[0]), pageStone(buttonFrames[1])} : null;
        for (int tier = 1; tier <= 5; tier++) {
            D2Sprites.Frame[] heal = s.dc6("data\\global\\items\\invhp" + tier + ".dc6");
            D2Sprites.Frame[] mana = s.dc6("data\\global\\items\\invmp" + tier + ".dc6");
            healPotions[tier] = heal != null && heal.length > 0 ? cropped(heal[0]) : null;
            manaPotions[tier] = mana != null && mana.length > 0 ? cropped(mana[0]) : null;
        }
        for (int i = 0; i < 2; i++) {
            D2Sprites.Frame[] rejuv = s.dc6("data\\global\\items\\invrp" + (i == 0 ? "s" : "l") + ".dc6");
            rejuvPotions[i] = rejuv != null && rejuv.length > 0 ? cropped(rejuv[0]) : null;
        }
        this.frame = D2Frame.load(s);
        frameLines = this.frame != null ? this.frame.lines() : null;

        for (String code : new String[]{"vps", "yps", "wms"}) {
            D2Sprites.Frame[] f = s.dc6("data\\global\\items\\inv" + code + ".dc6");
            if (f != null && f.length > 0) effectPictures.put(code, cropped(f[0]));
        }

        String[] inv = D2Materials.invFiles(s.read("data\\global\\excel\\misc.bin"));
        for (int k = 0; inv != null && k < D2Materials.KINDS; k++) {
            D2Sprites.Frame[] f = inv[k] != null ? s.dc6("data\\global\\items\\" + inv[k] + ".dc6") : null;
            if (f != null && f.length > 0) materialPictures[k] = cropped(f[0]);
        }
        D2Sprites.Frame[] stashButtons = s.dc6("data\\global\\ui\\PANEL\\buysellbtn.dc6");
        blankButton = stashButtons != null && stashButtons.length >= 2 ? new Bitmap[]{bitmap(stashButtons[0]), bitmap(stashButtons[1])} : null;
        int anvil = D2MaterialsLayer.FRAME_ANVIL;
        anvilButton = stashButtons != null && stashButtons.length > anvil + 1
            ? new Bitmap[]{bitmap(stashButtons[anvil]), bitmap(stashButtons[anvil + 1])} : null;
        D2Sprites.Frame[] medium = s.dc6("data\\global\\ui\\FrontEnd\\MediumButtonBlank.dc6");
        mediumButton = medium != null && medium.length >= 2 ? new Bitmap[]{bitmap(medium[0]), bitmap(medium[1])} : null;
        layerArt = D2LayerArt.load(s, this::gameText);

        try {
            D2ShrinePictures.Picture[] shrines = D2ShrinePictures.load(s);
            for (int i = 0; i < shrines.length && i < shrinePictures.length; i++) {
                if (shrines[i] != null) {
                    shrinePictures[i] = Bitmap.createBitmap(shrines[i].argb, shrines[i].width, shrines[i].height, Bitmap.Config.ARGB_8888);
                }
            }
        }
        catch (IOException e) {

        }
        D2Sprites.Frame[] levelUp = s.dc6("data\\global\\ui\\PANEL\\level.dc6");
        if (levelUp != null && levelUp.length > 0) effectPictures.put("level", bitmap(levelUp[0]));
        D2Strings strings = new D2Strings(s);
        this.strings = strings;
        D2LevelNames.setCurrent(D2LevelNames.read(s.read("data\\global\\excel\\levels.bin"), strings));
        language = strings.language();

        byte[] skills = s.read("data\\global\\excel\\skills.bin");
        byte[] descs = s.read("data\\global\\excel\\skilldesc.bin");
        fertigkeiten = com.winlator.d2.test.Fertigkeiten.lesen(skills);
        aimingSkills = D2SkillTargets.read(skills);
        passiveSkills = D2SkillTargets.passives(skills);
        tabellen = D2Tabellen.lesen(s.read("data\\global\\excel\\weapons.bin"), s.read("data\\global\\excel\\itemtypes.bin"));
        byte[] runes = s.read("data\\global\\excel\\runes.bin");
        wertBasen = D2Wertklasse.basen(runes, false);
        wertBasenLadder = D2Wertklasse.basen(runes, true);
        java.util.Map<String, D2Breakpoints.Anim> anims = D2Breakpoints.parse(s.read("data\\global\\animdata.d2"));
        java.util.List<String> differ = anims != null ? D2Breakpoints.compare(anims) : null;
        if (differ != null && !differ.isEmpty()) android.util.Log.w("D2Hud", "animdata.d2 differs from the hand tables: " + differ);
        animationen = differ != null && differ.isEmpty() ? anims : null;
        int count = skills != null && skills.length > 4 ? s32(skills, 0) : 0;
        int descCount = descs != null && descs.length > 4 ? s32(descs, 0) : 0;
        skillClass = new int[Math.max(0, count)];
        skillCel = new int[Math.max(0, count)];
        skillPlace = new int[Math.max(0, count)];
        skillName = new String[Math.max(0, count)];
        skillShort = new String[Math.max(0, count)];
        stateSkills = D2EffectRules.stateSkills(skills);
        if (count > 0 && descCount > 0) {
            int size = (skills.length - 4) / count, descSize = (descs.length - 4) / descCount;
            for (int id = 0; id < count; id++) {
                int at = 4 + id * size;
                skillClass[id] = skills[at + 0x0C];
                int desc = (skills[at + 0x194] & 0xFF) | (skills[at + 0x195] & 0xFF) << 8;
                skillCel[id] = desc < descCount ? descs[4 + desc * descSize + 7] & 0xFF : -1;

                if (desc < descCount) {
                    int at3 = 4 + desc * descSize;
                    skillPlace[id] = (descs[at3 + 2] & 0xFF) << 16 | (descs[at3 + 3] & 0xFF) << 8 | (descs[at3 + 4] & 0xFF);
                }

                if (desc < descCount) {
                    int at2 = 4 + desc * descSize + 8;
                    skillName[id] = strings.get((descs[at2] & 0xFF) | (descs[at2 + 1] & 0xFF) << 8);

                    skillShort[id] = strings.get((descs[at2 + 2] & 0xFF) | (descs[at2 + 3] & 0xFF) << 8);
                }
            }
        }
        for (int i = 0; i < ICON_FILES.length; i++) {
            D2Sprites.Frame[] frames = s.dc6(skillIconFile(i));
            if (frames != null) skillIcons[i] = bitmaps(frames);
        }

        ColorMatrix gold = new ColorMatrix(new float[]{
            0.78f, 0, 0, 0, 0,
            0, 0.70f, 0, 0, 0,
            0, 0, 0.47f, 0, 0,
            0, 0, 0, 1, 0});
        goldText.setColorFilter(new ColorMatrixColorFilter(gold));
        pixelPaint.setFilterBitmap(false);
        goldPixelText.setFilterBitmap(false);
        goldPixelText.setColorFilter(new ColorMatrixColorFilter(gold));


        redPixelText.setFilterBitmap(false);
        float stone = (D2GameFont.STONE_RGB & 0xFF) / 255f;
        stonePixelText.setFilterBitmap(false);
        stonePixelText.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
            stone, 0, 0, 0, 0,
            0, stone, 0, 0, 0,
            0, 0, stone, 0, 0,
            0, 0, 0, 1, 0})));
        redPixelText.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
            1.00f, 0, 0, 0, 0,
            0, 0.25f, 0, 0, 0,
            0, 0, 0.20f, 0, 0,
            0, 0, 0, 1, 0})));

        shadowText.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0, 0,
            0, 0, 0, 0.85f, 0})));
    }


    public static D2Hud load(File gameDir) throws IOException {
        try (D2Sprites sprites = new D2Sprites(gameDir)) {
            D2Hud hud = new D2Hud(sprites);
            D2SpieltestPort.skills(hud.fertigkeiten);
            return hud;
        }
    }





    public void draw(Canvas canvas, int gameWidth, int gameHeight, float scale, float x0, float y0, int life, int mana) {
        int left = gameWidth / 2 - CENTER_HALF, right = gameWidth / 2 + CENTER_HALF;
        int bottom = gameHeight;


        band(canvas, 0, left, bottom - BAND_H, scale, x0, y0, true);
        band(canvas, right, gameWidth, bottom - BAND_H, scale, x0, y0, false);
        int top = bottom - PIECE_H;



        image(canvas, angel, left - PIECE_W, top, scale, x0, y0);
        orb(canvas, lifeOrb, left - PIECE_W + 5, top + 8, life, scale, x0, y0);
        image(canvas, lifeGlass, left - PIECE_W + 5, top + 8, scale, x0, y0);

        image(canvas, demon, right, top, scale, x0, y0);
        orb(canvas, manaOrb, right + 32, top + 8, mana, scale, x0, y0);
        image(canvas, manaGlass, right + 30, top + 4, scale, x0, y0);
    }





    public void drawDurability(Canvas canvas, int[] locations, int[] states, float right, float top, float scale, Paint box, Paint frame) {
        if (locations.length == 0 || duro.length < 33) return;
        float pad = 3 * scale, gap = 2 * scale, w = pad * 2 - gap, h = 0;
        Bitmap[] icons = new Bitmap[locations.length];
        for (int i = 0; i < locations.length; i++) {
            int part = duroPart(locations[i]);
            int colour = states[i] >= 2 ? 2 : 1;
            icons[i] = part >= 0 ? duro[colour * 11 + part] : null;
            if (icons[i] == null) continue;
            w += icons[i].getWidth() * scale + gap;
            h = Math.max(h, icons[i].getHeight() * scale);
        }
        if (h == 0) return;
        h += pad * 2;
        dst.set(right - w, top, right, top + h);
        if (this.frame != null) {

            RectF box3 = new RectF(dst);
            box3.inset(-2 * scale, -2 * scale);
            drawFrame(canvas, box3, scale, box.getColor());
        }
        else {
            canvas.drawRect(dst, box);
            canvas.drawRect(dst, frame);
        }
        float x = right - w + pad;
        for (Bitmap icon : icons) {
            if (icon == null) continue;
            float iw = icon.getWidth() * scale, ih = icon.getHeight() * scale;
            RectF where = new RectF(x, top + (h - ih) / 2, x + iw, top + (h + ih) / 2);
            canvas.drawBitmap(icon, null, where, paint);
            x += iw + gap;
        }
    }


    private static int duroPart(int location) {
        switch (location) {
            case 1: return 1;
            case 3: return 4;
            case 4: return 6;
            case 5: return 8;
            case 8: return 2;
            case 9: return 3;
            case 10: return 0;
            default: return -1;
        }
    }


    public Bitmap pointerHand() {
        return pointerHand;
    }


    public int[] pointerHandOffset() {
        return new int[]{pointerHandX, pointerHandY};
    }


    public Bitmap handIcon() {
        return hand;
    }



    public int skillPassive(int skillId) {
        boolean[] p = passiveSkills;
        if (p == null || skillId < 0 || skillId >= p.length) return -1;
        return p[skillId] ? 1 : 0;
    }

    public String skillName(int skillId) {
        return skillId >= 0 && skillId < skillName.length ? skillName[skillId] : null;
    }


    public String skillShort(int skillId) {
        return skillId >= 0 && skillId < skillShort.length ? skillShort[skillId] : null;
    }


    public int skillClassOf(int skillId) {
        return skillId >= 0 && skillId < skillClass.length ? skillClass[skillId] : -1;
    }


    public int[][] stateSkills() {
        return stateSkills;
    }



    public Bitmap rejuvPotion(boolean full) {
        return rejuvPotions[full ? 1 : 0];
    }

    public Bitmap potion(boolean heal, int tier) {
        return tier >= 1 && tier <= 5 ? (heal ? healPotions : manaPotions)[tier] : null;
    }


    public int smallTextWidth(String text) {
        return D2FontText.width(smallFontWidth, text);
    }


    public void drawSmallText(Canvas canvas, String text, float cx, float cy, float scale) {
        drawSmallText(canvas, text, cx, cy, scale, 255);
    }


    public void drawSmallText(Canvas canvas, String text, float cx, float cy, float scale, int alpha) {
        int old = goldText.getAlpha();
        goldText.setAlpha(alpha);
        smallGlyphs(canvas, text, cx, cy, scale, goldText);
        goldText.setAlpha(old);
    }


    public void drawSmallText(Canvas canvas, String text, float cx, float cy, float scale, int alpha, int rgb) {
        Paint with = colourText(rgb);
        int old = with.getAlpha();
        with.setAlpha(alpha);
        smallGlyphs(canvas, text, cx, cy, scale, with);
        with.setAlpha(old);
    }

    private void smallGlyphs(Canvas canvas, String text, float cx, float cy, float scale, Paint with) {
        float x = cx - smallTextWidth(text) * scale / 2, y = cy - 7 * scale;
        text = D2FontText.map(text);
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            Bitmap g = smallFont[c];
            dst.set(x, y + (14 - g.getHeight()) * scale, x + g.getWidth() * scale, y + 14 * scale);
            canvas.drawBitmap(g, null, dst, with);
            x += smallFontWidth[c] * scale;
        }
    }


    private Paint colourText(int rgb) {
        Paint p = colourTexts.get(rgb);
        if (p == null) {
            p = new Paint(Paint.FILTER_BITMAP_FLAG);
            p.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                ((rgb >> 16) & 255) / 255f, 0, 0, 0, 0,
                0, ((rgb >> 8) & 255) / 255f, 0, 0, 0,
                0, 0, (rgb & 255) / 255f, 0, 0,
                0, 0, 0, 1, 0})));
            colourTexts.put(rgb, p);
        }
        return p;
    }


    public Bitmap effectPicture(String name) {
        if ("hp".equals(name)) return healPotions[3];
        if ("mp".equals(name)) return manaPotions[3];
        return name != null ? effectPictures.get(name) : null;
    }


    public Bitmap shrinePicture(int state) {
        int i = state - D2ShrinePictures.FIRST;
        return i >= 0 && i < shrinePictures.length ? shrinePictures[i] : null;
    }


    public boolean hasRing() {
        return frameLines != null;
    }





    public void drawRing(Canvas canvas, float cx, float cy, float r, float k) {
        int d = Math.max(2 * D2Frame.LINE + 2, Math.round(2 * r / k));
        Bitmap b = rings.get(d);
        if (b == null) {
            if (rings.size() > 32) rings.clear();
            b = Bitmap.createBitmap(D2Frame.ring(d, frameLines), d, d, Bitmap.Config.ARGB_8888);
            rings.put(d, b);
        }
        dst.set(cx - r, cy - r, cx + r, cy + r);
        canvas.drawBitmap(b, null, dst, paint);
    }


    public boolean skillAims(int skillId) {
        return aimingSkills != null && skillId >= 0 && skillId < aimingSkills.length && aimingSkills[skillId];
    }

    public boolean hasFrame() {
        return frame != null;
    }






    public void drawFrame(Canvas canvas, RectF where, float k, int fill) {
        drawFrame(canvas, where, k, fill, 255);
    }


    public void drawFrame(Canvas canvas, RectF where, float k, int fill, int alpha) {
        Bitmap b = frameBitmap(Math.round(where.width() / k), Math.round(where.height() / k), fill);
        paint.setAlpha(alpha);
        canvas.drawBitmap(b, null, where, paint);
        paint.setAlpha(255);
    }






    public void drawFrameSharp(Canvas canvas, RectF where, int s, int fill) {
        Bitmap b = frameBitmap(Math.round(where.width() / s), Math.round(where.height() / s), fill);
        dst.set(where.left, where.top, where.left + b.getWidth() * s, where.top + b.getHeight() * s);
        canvas.drawBitmap(b, null, dst, pixelPaint);
    }


    private Bitmap frameBitmap(int width, int height, int fill) {
        int w = Math.max(2 * D2Frame.LINE, width), h = Math.max(2 * D2Frame.LINE, height);
        long key = (long) w << 48 | (long) h << 32 | (fill & 0xFFFFFFFFL);
        Bitmap b = frames.get(key);
        if (b == null) {
            if (frames.size() > 64) frames.clear();
            b = Bitmap.createBitmap(frame.compose(w, h, fill), w, h, Bitmap.Config.ARGB_8888);
            frames.put(key, b);
        }
        return b;
    }





    public android.graphics.drawable.Drawable frameDrawable(final float k, final int fill) {
        return new android.graphics.drawable.Drawable() {
            private final RectF where = new RectF();

            @Override
            public void draw(Canvas canvas) {
                where.set(getBounds());
                drawFrame(canvas, where, k, fill);
            }

            @Override
            public boolean getPadding(Rect padding) {
                int p = Math.round(k * (D2Frame.LINE + 2));
                padding.set(p, p, p, p);
                return true;
            }

            @Override
            public void setAlpha(int alpha) {}

            @Override
            public void setColorFilter(android.graphics.ColorFilter filter) {}

            @Override
            public int getOpacity() {
                return android.graphics.PixelFormat.TRANSLUCENT;
            }
        };
    }


    public Bitmap materialPicture(int kind) {
        return kind >= 0 && kind < materialPictures.length ? materialPictures[kind] : null;
    }


    public boolean drawBlankButton(Canvas canvas, boolean pressed, RectF where, Paint with) {
        if (blankButton == null) return false;
        canvas.drawBitmap(blankButton[pressed ? 1 : 0], null, where, with != null ? with : paint);
        return true;
    }


    public boolean drawAnvilButton(Canvas canvas, boolean pressed, RectF where, Paint with) {
        if (anvilButton == null) return false;
        canvas.drawBitmap(anvilButton[pressed ? 1 : 0], null, where, with != null ? with : paint);
        return true;
    }





    public boolean drawMediumButton(Canvas canvas, String text, boolean pressed, RectF where) {
        if (mediumButton == null) return false;
        drawStoneButton(canvas, mediumButton, text, pressed, false, where);
        return true;
    }

    public boolean hasGameButton() {
        return gameButton != null;
    }






    public void drawGameButton(Canvas canvas, String text, boolean pressed, boolean warn, RectF where) {
        drawStoneButton(canvas, gameButton, text, pressed, warn, where, true);
    }









    private void drawStoneButton(Canvas canvas, Bitmap[] stone, String text, boolean pressed, boolean warn, RectF where) {
        drawStoneButton(canvas, stone, text, pressed, warn, where, false);
    }





    private void drawStoneButton(Canvas canvas, Bitmap[] stone, String text, boolean pressed, boolean warn, RectF where,
                                 boolean fill) {
        int sw = stone[0].getWidth(), sh = stone[0].getHeight();

        int s = fill ? Math.max(1, (int) (where.height() / sh)) : D2StoneButton.scale(where.height(), sh);
        int[] at = fill ? new int[]{Math.round(where.left), Math.round(where.top), Math.round(where.width()), Math.round(where.height())}
            : D2StoneButton.stone(where.centerX(), where.centerY(), sw, sh, s);
        dst.set(at[0], at[1], at[0] + at[2], at[1] + at[3]);
        canvas.drawBitmap(stone[pressed ? 1 : 0], null, dst, pixelPaint);
        int shift = pressed ? s : 0;

        stoneText(canvas, text, at[0] + at[2] / 2f + shift, at[1] + at[3] / 2f + shift, s, sw, warn);
    }








    private void stoneText(Canvas canvas, String text, float cx, float cy, int s, int room, boolean warn) {
        if (text == null) return;
        String upper = text.toUpperCase(java.util.Locale.ROOT);

        int fit = room == D2Materials.BUTTON_W ? D2MaterialsLayer.MEDIUM_ROOM : room - 12;
        if (stoneFont != null && D2FontText.width(stoneFontWidth, upper) <= fit) {
            glyphsSharp(canvas, upper, cx, cy, s, stoneFont, stoneFontWidth, STONE_CELL, STONE_BASE, warn ? redPixelText : stonePixelText);
            return;
        }
        int w = D2FontText.width(smallFontWidth, text), ls = w <= fit ? s : D2StoneButton.labelScale(w, room, s);
        Paint with = warn ? redPixelText : stoneFont != null ? stonePixelText : pixelPaint;
        glyphsSharp(canvas, text, cx, cy, ls, smallFont, smallFontWidth, SMALL_CELL, SMALL_CELL / 2, with);
    }





    public void drawStoneSign(Canvas canvas, boolean plus, float cx, float cy, float scale) {
        int arm = D2InputBox.SIGN_ARM;
        for (int pass = 0; pass < 2; pass++) {
            int o = pass == 0 ? 1 : 0;
            signPaint.setColor(0xFF000000 | (pass == 0 ? D2Layer.STONE_SHADE_RGB : D2Layer.STONE_CORE_RGB));
            canvas.drawRect(cx - (arm + o) * scale, cy - (1.5f + o) * scale, cx + (arm + o) * scale, cy + (1.5f + o) * scale, signPaint);
            if (plus) canvas.drawRect(cx - (1.5f + o) * scale, cy - (arm + o) * scale, cx + (1.5f + o) * scale, cy + (arm + o) * scale, signPaint);
        }
    }

    private final Paint signPaint = new Paint();





    public int skillTreePlace(int skillId) {
        if (skillId < 0 || skillId >= skillPlace.length) return 0;
        int v = skillPlace[skillId], page = v >> 16 & 0xFF, row = v >> 8 & 0xFF, column = v & 0xFF;
        return page >= 1 && page <= 3 && row >= 1 && row <= 6 && column >= 1 && column <= 3 ? v : 0;
    }


    String skillIconPath(int skillId) {
        if (skillIcon(skillId) == null) return null;
        int cls = skillClass[skillId];
        String file = skillIconFile(cls >= 0 && cls < 7 ? cls : 7);
        return file.substring(0, file.length() - ".dc6".length());
    }


    private static String skillIconFile(int i) {
        return "data\\global\\ui\\SPELLS\\" + ICON_FILES[i] + "Skillicon.dc6";
    }


    int skillIconFrame(int skillId) {
        return skillIcon(skillId) != null ? skillCel[skillId] : -1;
    }


    public Bitmap skillIcon(int skillId) {
        if (skillId < 0 || skillId >= skillClass.length) return null;
        int cls = skillClass[skillId], cel = skillCel[skillId];
        Bitmap[] file = skillIcons[cls >= 0 && cls < 7 ? cls : 7];
        return file != null && cel >= 0 && cel < file.length ? file[cel] : null;
    }


    public void drawMiniIcon(Canvas canvas, int icon, boolean pressed, RectF where) {
        int index = Math.min(mini.length - 1, icon + (pressed ? 1 : 0));
        drawSharp(canvas, mini[index], index, where, 255);
    }


    public void drawRunIcon(Canvas canvas, boolean running, boolean pressed, RectF where) {
        int index = (running ? 2 : 0) + (pressed ? 1 : 0);
        drawSharp(canvas, run[index], SHARP_RUN + index, where, 255);
    }


    public void drawLevelCross(Canvas canvas, RectF where) {
        if (levelCross != null) drawSharp(canvas, levelCross, SHARP_CROSS, where, 255);
    }


    public boolean drawMercIcon(Canvas canvas, RectF where) {
        if (mercIcon == null) return false;
        drawSharp(canvas, mercIcon, SHARP_MERC, where, 255);
        return true;
    }


    public boolean drawCubeIcon(Canvas canvas, RectF where) {
        if (cubeIcon == null) return false;
        drawSharp(canvas, cubeIcon, SHARP_CUBE, where, 255);
        return true;
    }


    public int miniIconWidth() {
        return mini[0].getWidth();
    }


    public boolean drawMenuSkillIcon(Canvas canvas, int skillId, RectF where, int alpha) {
        Bitmap icon = skillIcon(skillId);
        if (icon == null) return false;
        drawSharp(canvas, icon, SHARP_SKILL + skillId, where, alpha);
        return true;
    }





    public void prepareMenuPictures(RectF iconBox, RectF arrowBox, int portalSkill, android.view.View view) {
        sharpView = new java.lang.ref.WeakReference<>(view);
        for (int icon : new int[]{ICON_CHAR, ICON_INV, ICON_SKILLS, ICON_MAP, ICON_QUESTS, ICON_MENU}) {
            for (int i = icon; i <= icon + 1 && i < mini.length; i++) sharp(mini[i], i, factor(mini[i], iconBox));
        }
        for (int i = 0; i < run.length; i++) sharp(run[i], SHARP_RUN + i, factor(run[i], iconBox));
        for (int i = 0; i < arrow.length; i++) sharp(arrow[i], SHARP_ARROW + i, factor(arrow[i], arrowBox));
        Bitmap portal = skillIcon(portalSkill);
        if (portal != null) sharp(portal, SHARP_SKILL + portalSkill, factor(portal, iconBox));
    }







    private void drawSharp(Canvas canvas, Bitmap src, int id, RectF where, int alpha) {
        int[] fit = D2Xbr.fit(src.getWidth(), src.getHeight(), where.width(), where.height());
        int n = fit[0], w = fit[1], h = fit[2];
        float left = Math.round(where.centerX() - w / 2f), top = Math.round(where.centerY() - h / 2f);
        Bitmap b = sharp(src, id, n);
        if (b == null) b = src;
        boolean exact = b.getWidth() == w && b.getHeight() == h;
        Paint with = exact ? pixelPaint : paint;
        with.setAlpha(alpha);
        if (exact) canvas.drawBitmap(b, left, top, with);
        else {
            dst.set(left, top, left + w, top + h);
            canvas.drawBitmap(b, null, dst, with);
        }
        with.setAlpha(255);
    }


    private static int factor(Bitmap src, RectF where) {
        return D2Xbr.fit(src.getWidth(), src.getHeight(), where.width(), where.height())[0];
    }





    private Bitmap sharp(Bitmap src, int id, int n) {
        if (n <= 1) return src;
        final long key = (long) id << 8 | n;
        synchronized (sharp) {
            Bitmap b = sharp.get(key);
            if (b != null || !sharpPending.add(key)) return b;
            if (sharpWorker == null) {

                sharpWorker = new java.util.concurrent.ThreadPoolExecutor(0, 1, 2, java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.LinkedBlockingQueue<>(), r -> {
                        Thread t = new Thread(r, "D2Hud-xbr");
                        t.setDaemon(true);
                        return t;
                    });
            }
        }
        final int w = src.getWidth(), h = src.getHeight();
        final int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        sharpWorker.execute(() -> {
            Bitmap b = Bitmap.createBitmap(D2Xbr.scale(pixels, w, h, n), w * n, h * n, Bitmap.Config.ARGB_8888);
            synchronized (sharp) {
                sharp.put(key, b);
                sharpPending.remove(key);
            }
            java.lang.ref.WeakReference<android.view.View> ref = sharpView;
            android.view.View view = ref != null ? ref.get() : null;
            if (view != null) view.postInvalidate();
        });
        return null;
    }






    public void drawOrbNumbers(Canvas canvas, int gameWidth, int gameHeight, float scale, float x0, float y0,
                               boolean compact, int life, int maxLife, int mana, int maxMana, boolean showLife,
                               boolean showMana) {
        int top = gameHeight - PIECE_H + 8;
        int lifeX, manaX;
        if (compact) {
            lifeX = gameWidth / 2 - CENTER_HALF - PIECE_W + 5;
            manaX = gameWidth / 2 + CENTER_HALF + 32;
        }
        else {

            lifeX = PIECE_W - 5 - ORB;
            manaX = gameWidth - PIECE_W + PIECE_W - 32 - ORB;
        }
        float cy = y0 + (top - 9) * scale;
        if (showLife) numbers(canvas, life + " / " + maxLife, x0 + (lifeX + ORB / 2f) * scale, cy, scale);
        if (showMana && maxMana > 0) numbers(canvas, mana + " / " + maxMana, x0 + (manaX + ORB / 2f) * scale, cy, scale);
    }

    private void numbers(Canvas canvas, String text, float cx, float cy, float scale) {
        float edge = Math.max(1, scale * 0.8f);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) glyphs(canvas, text, cx + dx * edge, cy + dy * edge, scale, shadowText);
            }
        }
        glyphs(canvas, text, cx, cy, scale, paint);
    }


    public void drawArrow(Canvas canvas, boolean open, boolean pressed, RectF where) {
        int index = (open ? 2 : 0) + (pressed ? 1 : 0);
        drawSharp(canvas, arrow[index], SHARP_ARROW + index, where, 255);
    }


    public int textWidth(String text) {
        return D2FontText.width(fontWidth, text);
    }


    public void drawText(Canvas canvas, String text, float cx, float cy, float scale) {
        glyphs(canvas, text, cx, cy, scale, goldText);
    }


    public void drawText(Canvas canvas, String text, float cx, float cy, float scale, int rgb) {
        glyphs(canvas, text, cx, cy, scale, colourText(rgb));
    }


    public void drawTextSharp(Canvas canvas, String text, float cx, float cy, int s) {
        glyphsSharp(canvas, text, cx, cy, s, font, fontWidth, 16, 8, goldPixelText);
    }


    public void drawSmallTextSharp(Canvas canvas, String text, float cx, float cy, int s) {
        glyphsSharp(canvas, text, cx, cy, s, smallFont, smallFontWidth, SMALL_CELL, SMALL_CELL / 2, goldPixelText);
    }


    private void glyphsSharp(Canvas canvas, String text, float cx, float cy, int s, Bitmap[] glyphs, int[] widths,
                             int full, int base, Paint with) {
        int tw = D2FontText.width(widths, text);
        float x = Math.round(cx - tw * s / 2f), y = Math.round(cy - base * s);
        text = D2FontText.map(text);
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            Bitmap g = glyphs[c];
            dst.set(x, y + (full - g.getHeight()) * s, x + g.getWidth() * s, y + full * s);
            canvas.drawBitmap(g, null, dst, with);
            x += widths[c] * s;
        }
    }

    private void glyphs(Canvas canvas, String text, float cx, float cy, float scale, Paint with) {
        float x = cx - textWidth(text) * scale / 2, y = cy - 8 * scale;
        text = D2FontText.map(text);
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            Bitmap g = font[c];
            dst.set(x, y + (16 - g.getHeight()) * scale, x + g.getWidth() * scale, y + 16 * scale);
            canvas.drawBitmap(g, null, dst, with);
            x += fontWidth[c] * scale;
        }
    }



    private void image(Canvas canvas, Bitmap b, int gx, int gy, float scale, float x0, float y0) {
        dst.set(x0 + gx * scale, y0 + gy * scale, x0 + (gx + b.getWidth()) * scale, y0 + (gy + b.getHeight()) * scale);
        canvas.drawBitmap(b, null, dst, paint);
    }


    private void orb(Canvas canvas, Bitmap b, int gx, int gy, int percent, float scale, float x0, float y0) {
        int p = Math.max(0, Math.min(100, percent));
        if (p == 0) return;
        int h = b.getHeight(), visible = Math.max(1, Math.round(h * p / 100f));
        src.set(0, h - visible, b.getWidth(), h);
        dst.set(x0 + gx * scale, y0 + (gy + h - visible) * scale, x0 + (gx + b.getWidth()) * scale, y0 + (gy + h) * scale);
        canvas.drawBitmap(b, src, dst, paint);
    }


    private void band(Canvas canvas, int gx0, int gx1, int gy, float scale, float x0, float y0, boolean anchorRight) {
        int w = band.getWidth();
        if (gx1 <= gx0) return;
        if (anchorRight) {
            for (int x = gx1; x > gx0; x -= w) {
                int from = Math.max(gx0, x - w);
                src.set(w - (x - from), 0, w, band.getHeight());
                dst.set(x0 + from * scale, y0 + gy * scale, x0 + x * scale, y0 + (gy + band.getHeight()) * scale);
                canvas.drawBitmap(band, src, dst, paint);
            }
        }
        else {
            for (int x = gx0; x < gx1; x += w) {
                int to = Math.min(gx1, x + w);
                src.set(0, 0, to - x, band.getHeight());
                dst.set(x0 + x * scale, y0 + gy * scale, x0 + to * scale, y0 + (gy + band.getHeight()) * scale);
                canvas.drawBitmap(band, src, dst, paint);
            }
        }
    }


    private static int[] widths(byte[] tbl, int fallback) {
        int[] w = new int[256];
        for (int i = 0; i < 256; i++) w[i] = tbl != null && tbl.length >= 12 + 14 * 256 ? tbl[12 + i * 14 + 3] & 0xFF : fallback;
        return w;
    }

    private static Bitmap ornamentBand(D2Sprites.Frame leftHalf, D2Sprites.Frame rightHalf) {
        int h = BAND_H;
        int[] px = new int[392 * h];
        copy(leftHalf, 60, 71 - h, 196, h, px, 392, 0);
        copy(rightHalf, 0, 71 - h, 196, h, px, 392, 196);
        for (int i = 0; i < px.length; i++) if (px[i] == 0) px[i] = Color.BLACK;

        int[] flipped = new int[px.length];
        for (int y = 0; y < h; y++) System.arraycopy(px, y * 392, flipped, (h - 1 - y) * 392, 392);
        return Bitmap.createBitmap(flipped, 392, h, Bitmap.Config.ARGB_8888);
    }

    private static void copy(D2Sprites.Frame f, int sx, int sy, int w, int h, int[] out, int stride, int dx) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int fx = sx + x, fy = sy + y;
                if (fx < f.width && fy < f.height) out[y * stride + dx + x] = f.argb[fy * f.width + fx];
            }
        }
    }


    private static int[] crop(D2Sprites.Frame f, int x, int y, int w, int h) {
        if (f == null || x + w > f.width || y + h > f.height) return null;
        int[] out = new int[w * h];
        for (int r = 0; r < h; r++) System.arraycopy(f.argb, (y + r) * f.width + x, out, r * w, w);
        return out;
    }


    public boolean hasBarPieces() {
        return buttonBox != null && xpPiece != null && stonePiece != null;
    }





    public void drawButtonBox(Canvas canvas, RectF where, float k) {
        drawBarPiece(canvas, where, k, 1, buttonBox, BUTTON_BOX, BUTTON_BOX, 4);
    }


    public void drawXpBar(Canvas canvas, RectF where, float k) {
        drawBarPiece(canvas, where, k, 2, xpPiece, XP_PIECE_W, XP_PIECE_H, 4);
    }


    public void drawThinBox(Canvas canvas, RectF where, float k) {
        drawBarPiece(canvas, where, k, 3, xpPiece, XP_PIECE_W, XP_PIECE_H, 2);
    }


    public void drawStone(Canvas canvas, RectF where, float k) {
        drawBarPiece(canvas, where, k, 4, stonePiece, STONE_W, STONE_H, 0);
    }

    private void drawBarPiece(Canvas canvas, RectF where, float k, int kind, int[] piece, int pw, int ph, int edge) {
        int w = Math.max(2 * edge + 1, Math.round(where.width() / k)), h = Math.max(2 * edge + 1, Math.round(where.height() / k));
        if (kind == 2) h = ph;
        long key = (long) kind << 40 | (long) w << 20 | h;
        Bitmap b = barPieces.get(key);
        if (b == null) {
            if (barPieces.size() > 32) barPieces.clear();
            int[] px = kind == 2 ? D2InfoGrid.stretchWide(piece, pw, ph, edge, w)
                : kind == 4 ? D2InfoGrid.stretch(piece, pw, ph, 0, w, h, 0)
                : D2InfoGrid.stretch(piece, pw, ph, edge, w, h, 0xFF000000);
            b = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
            barPieces.put(key, b);
        }
        canvas.drawBitmap(b, null, where, pixelPaint);
    }


    public int tinyTextWidth(String text) {
        return tinyFont != null ? D2FontText.width(tinyFontWidth, text) : smallTextWidth(text);
    }


    public void drawTinyTextRight(Canvas canvas, String text, float right, float bottom, float scale, int rgb) {
        drawTinyText(canvas, text, right, bottom, scale, rgb, true);
    }


    public void drawTinyTextLeft(Canvas canvas, String text, float left, float bottom, float scale, int rgb) {
        drawTinyText(canvas, text, left, bottom, scale, rgb, false);
    }

    private void drawTinyText(Canvas canvas, String text, float at, float bottom, float scale, int rgb, boolean right) {
        Bitmap[] glyphs = tinyFont != null ? tinyFont : smallFont;
        int[] widths = tinyFont != null ? tinyFontWidth : smallFontWidth;
        Paint with = colourText(rgb);
        text = D2FontText.map(text);
        if (text.isEmpty()) return;

        int last = text.charAt(text.length() - 1);
        float x = right ? at - (D2FontText.width(widths, text) - widths[last] + glyphs[last].getWidth()) * scale : at;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            Bitmap g = glyphs[c];
            dst.set(x, bottom - g.getHeight() * scale, x + g.getWidth() * scale, bottom);
            canvas.drawBitmap(g, null, dst, with);
            x += widths[c] * scale;
        }
    }

    private static D2Sprites.Frame[] need(D2Sprites s, String name, int frames) throws IOException {
        D2Sprites.Frame[] f = s.dc6(name);
        if (f == null || f.length < frames) throw new IOException("missing " + name);
        return f;
    }

    private static boolean square(D2Sprites.Frame f) {
        return f.width == D2PageButtons.SQUARE && f.height == D2PageButtons.SQUARE;
    }


    private static Bitmap pageStone(D2Sprites.Frame f) {
        int e = D2PageButtons.EDGE, t = D2PageButtons.TILE, w = D2PageButtons.STONE_W, h = f.height;
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            System.arraycopy(f.argb, y * f.width, px, y * w, e);
            for (int i = 0; i < D2PageButtons.TILES; i++) System.arraycopy(f.argb, y * f.width + e, px, y * w + e + i * t, t);
            System.arraycopy(f.argb, y * f.width + e + t, px, y * w + w - e, e);
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
    }

    private static Bitmap bitmap(D2Sprites.Frame f) {
        return Bitmap.createBitmap(f.argb, f.width, f.height, Bitmap.Config.ARGB_8888);
    }


    private static Bitmap cropped(D2Sprites.Frame f) {
        int minX = f.width, minY = f.height, maxX = -1, maxY = -1;
        for (int y = 0; y < f.height; y++) {
            for (int x = 0; x < f.width; x++) {
                if (f.argb[y * f.width + x] != 0) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) return bitmap(f);
        return Bitmap.createBitmap(bitmap(f), minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private static Bitmap[] croppedAll(D2Sprites.Frame[] frames) {
        Bitmap[] out = new Bitmap[frames.length];
        for (int i = 0; i < frames.length; i++) out[i] = cropped(frames[i]);
        return out;
    }

    private static Bitmap[] bitmaps(D2Sprites.Frame[] frames) {
        Bitmap[] out = new Bitmap[frames.length];
        for (int i = 0; i < frames.length; i++) out[i] = bitmap(frames[i]);
        return out;
    }

    private static int s32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    private static Bitmap mirror(Bitmap b) {
        Matrix m = new Matrix();
        m.setScale(-1, 1);
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, false);
    }
}
