package com.winlator.d2;

import java.io.IOException;








final class D2StartArt {
    static final String FRONT_END = "data\\global\\ui\\FrontEnd\\";

    static final String CHAR_SELECT_BOX = "data\\global\\ui\\CharSelect\\charselectbox.dc6";

    static final int CARD_FRAME_EDGE = 4;

    static final int FRAME_OTHER = 0, FRAME_LAST = 1, FRAME_HARDCORE = 2;

    static final int SCREEN_W = 800, SCREEN_H = 600, LOGO_X = 400, LOGO_Y = 120;
    private static final String[] CLASS_DIRS = {"amazon", "sorceress", "necromancer", "paladin", "barbarian", "druid", "assassin"};
    private static final String[] CLASS_CODES = {"am", "so", "ne", "pa", "ba", "dz", "as"};


    final int[] screen;

    final D2Sprites.Frame[] blackLeft, blackRight, fireLeft, fireRight;

    final D2GameFont.Img button, buttonPressed;
    final D2GameFont font16, formal10, exocet10;

    final D2GameFont.Img cardFrame;


    final java.util.Map<String, String> levels = new java.util.HashMap<>();
    private final D2Sprites sprites;
    private final D2Sprites.Frame[][] figures = new D2Sprites.Frame[CLASS_CODES.length][];

    private D2StartArt(D2Sprites sprites) throws IOException {
        this.sprites = sprites;
        D2Sprites.Frame[] tiles = need(sprites.dc6(FRONT_END + "gameselectscreenEXP.dc6", "Sky"));
        screen = tiled(tiles, SCREEN_W, SCREEN_H);
        blackLeft = need(sprites.dc6(FRONT_END + "D2logoBlackLeft.dc6", "Sky"));
        blackRight = need(sprites.dc6(FRONT_END + "D2logoBlackRight.dc6", "Sky"));
        fireLeft = need(sprites.dc6(FRONT_END + "D2logoFireLeft.dc6", "Sky"));
        fireRight = need(sprites.dc6(FRONT_END + "D2logoFireRight.dc6", "Sky"));
        D2Sprites.Frame[] wide = need(sprites.dc6(FRONT_END + "widebuttonblank.dc6", "Sky"));
        if (wide.length < 4) throw new IOException("WideButtonBlank: " + wide.length + " frames");
        button = joined(wide[0], wide[1]);
        buttonPressed = joined(wide[2], wide[3]);
        font16 = needFont(D2GameFont.load(sprites, "font16"));
        formal10 = needFont(D2GameFont.load(sprites, "fontformal10"));
        exocet10 = needFont(D2GameFont.load(sprites, "fontexocet10"));
        D2Sprites.Frame[] box = sprites.dc6(CHAR_SELECT_BOX, "Sky");
        cardFrame = box != null && box.length >= 2 ? joined(box[0], box[1]) : null;
        gems = gems(sprites);
    }


    final D2Sprites.Frame[] gems;
    static final int GEM_ORIGINAL = 0, GEM_CHECKED = 1, GEM_UNCHECKED = 2, GEM_RISK = 3;
    private static final String[] GEM_CODES = {"gpw", "gpg", "gpy", "gpr"};


    static D2Sprites.Frame[] gems(D2Sprites sprites) {
        D2Sprites.Frame[] out = new D2Sprites.Frame[GEM_CODES.length];
        try {
            String[] inv = D2Materials.invFiles(sprites.read("data\\global\\excel\\misc.bin"));
            for (int i = 0; inv != null && i < GEM_CODES.length; i++) {
                int kind = D2Materials.kindOf(GEM_CODES[i]);
                String file = kind >= 0 ? inv[kind] : null;
                D2Sprites.Frame[] f = file != null ? sprites.dc6("data\\global\\items\\" + file + ".dc6") : null;
                if (f != null && f.length > 0) out[i] = f[0];
            }
        }
        catch (IOException | RuntimeException e) {

        }
        return out;
    }


    static D2StartArt load(D2Sprites sprites) throws IOException {
        return new D2StartArt(sprites);
    }

    private static <T> T need(T value) throws IOException {
        if (value == null) throw new IOException("start page graphic missing");
        return value;
    }

    private static D2GameFont needFont(D2GameFont font) throws IOException {
        if (font == null) throw new IOException("start page font missing");
        return font;
    }


    static int[] tiled(D2Sprites.Frame[] tiles, int w, int h) {
        int[] out = new int[w * h];
        java.util.Arrays.fill(out, 0xFF000000);
        int x = 0, y = 0, rowH = 0;
        for (D2Sprites.Frame t : tiles) {
            if (x >= w) {
                x = 0;
                y += rowH;
                rowH = 0;
            }
            for (int ty = 0; ty < t.height && y + ty < h; ty++) {
                for (int tx = 0; tx < t.width && x + tx < w; tx++) {
                    int p = t.argb[ty * t.width + tx];
                    if (p != 0) out[(y + ty) * w + x + tx] = p;
                }
            }
            x += t.width;
            rowH = Math.max(rowH, t.height);
        }
        return out;
    }

    private static D2GameFont.Img joined(D2Sprites.Frame left, D2Sprites.Frame right) {
        int w = left.width + right.width, h = Math.max(left.height, right.height);
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            if (y < left.height) System.arraycopy(left.argb, y * left.width, out, y * w, left.width);
            if (y < right.height) System.arraycopy(right.argb, y * right.width, out, y * w + left.width, right.width);
        }
        return new D2GameFont.Img(w, h, out);
    }





    static int[] frame(D2GameFont.Img src, int edge, int w, int h) {
        int[] out = new int[w * h];
        int sw = src.width, sh = src.height;
        int midW = Math.max(1, sw - 2 * edge), midH = Math.max(1, sh - 2 * edge);
        for (int y = 0; y < h; y++) {
            boolean top = y < edge, bottom = y >= h - edge;
            int sy = top ? y : bottom ? sh - (h - y) : edge + (y - edge) % midH;
            for (int x = 0; x < w; x++) {
                boolean left = x < edge, right = x >= w - edge;
                if (!top && !bottom && !left && !right) continue;
                int sx = left ? x : right ? sw - (w - x) : edge + (x - edge) % midW;
                out[y * w + x] = src.argb[sy * sw + sx];
            }
        }
        return out;
    }


    static int tint(int argb, int kind) {
        int a = argb >>> 24, r = argb >> 16 & 0xFF, g = argb >> 8 & 0xFF, b = argb & 0xFF;
        if (a == 0) return argb;
        if (kind == FRAME_HARDCORE) {
            int v = Math.max(r, Math.max(g, b));
            r = Math.min(255, v * 110 / 100);
            g = v * 22 / 100;
            b = v * 18 / 100;
        } else {
            int k = kind == FRAME_LAST ? 120 : 75;
            r = Math.min(255, r * k / 100);
            g = Math.min(255, g * k / 100);
            b = Math.min(255, b * k / 100);
        }
        return a << 24 | r << 16 | g << 8 | b;
    }


    private static final String[] SPEAKER = {
        "........K..........",
        ".......KGK.....G...",
        "......KGDK......G..",
        ".KKKKKGDDK...G...G.",
        "KGGGGGDDDK....G..G.",
        "KGDDDDDDDK....G..G.",
        "KGDDDDDDDK....G..G.",
        "KGGGGGDDDK....G..G.",
        ".KKKKKGDDK...G...G.",
        "......KGDK......G..",
        ".......KGK.....G...",
        "........K.........."};

    private static final String[] SPEAKER_OFF = {
        "........", "........", "..R...R.", "...R.R..", "....R...", "....R...", "...R.R..", "..R...R.", "........",
        "........", "........", "........"};

    static D2GameFont.Img speaker(boolean on) {
        int w = SPEAKER[0].length(), h = SPEAKER.length;
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            String row = on ? SPEAKER[y] : SPEAKER[y].substring(0, 11) + SPEAKER_OFF[y];
            for (int x = 0; x < w; x++) {
                char c = row.charAt(x);
                int argb = c == 'K' ? 0xFF0A0A0A : c == 'G' ? 0xFFC8B074 : c == 'D' ? 0xFF78643C : c == 'R' ? 0xFFB02828 : 0;

                if (!on && (c == 'G' || c == 'D')) argb = dim(argb, 60);
                out[y * w + x] = argb;
            }
        }
        return new D2GameFont.Img(w, h, out);
    }





    int[] backdrop(int height) {
        int w = SCREEN_W, h = Math.max(SCREEN_H, height);
        int[] out = new int[w * h];
        System.arraycopy(screen, 0, out, 0, w * SCREEN_H);
        int mirror = Math.min(360, h - SCREEN_H);
        for (int y = 0; y < mirror; y++) copyRow(screen, SCREEN_H - 1 - y, out, SCREEN_H + y, 55);
        int ground = 154, from = SCREEN_H + mirror;
        for (int y = 0; from + y < h; y++) {
            int k = y / ground, r = y % ground;
            int src = k % 2 == 0 ? SCREEN_H - ground + r : SCREEN_H - 1 - r;
            copyRow(screen, src, out, from + y, 40);
        }

        for (int i = 0; i < 30; i++) {
            int dim = 100 - 35 * (15 - Math.abs(i - 15)) / 15;
            int y = SCREEN_H - 15 + i;
            if (y >= 0 && y < h) for (int x = 0; x < w; x++) out[y * w + x] = dim(out[y * w + x], dim);
        }
        return out;
    }


    static final int EXTENSION_STRIP = 240;





    static int[] extension(int[] backdrop, int height, int width) {
        int[] out = new int[Math.max(0, width) * height];
        for (int x = 0; x < width; x++) {
            int k = x / EXTENSION_STRIP, r = x % EXTENSION_STRIP;
            int src = k % 2 == 0 ? SCREEN_W - 1 - r : SCREEN_W - EXTENSION_STRIP + r;
            for (int y = 0; y < height; y++) out[y * width + x] = dim(backdrop[y * SCREEN_W + src], 90);
        }
        return out;
    }

    private static void copyRow(int[] src, int srcY, int[] dst, int dstY, int percent) {
        for (int x = 0; x < SCREEN_W; x++) dst[dstY * SCREEN_W + x] = dim(src[srcY * SCREEN_W + x], percent);
    }

    static int dim(int argb, int percent) {
        int r = (argb >> 16 & 0xFF) * percent / 100, g = (argb >> 8 & 0xFF) * percent / 100, b = (argb & 0xFF) * percent / 100;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }


    synchronized D2Sprites.Frame[] classFigure(int characterClass) {
        if (characterClass < 0 || characterClass >= CLASS_CODES.length) return null;
        if (figures[characterClass] == null) {
            try {
                String code = CLASS_CODES[characterClass];
                figures[characterClass] = sprites.dc6(FRONT_END + CLASS_DIRS[characterClass] + "\\" + code + "nu1.dc6", "fechar");
            }
            catch (IOException e) {
                return null;
            }
        }
        return figures[characterClass];
    }
}
