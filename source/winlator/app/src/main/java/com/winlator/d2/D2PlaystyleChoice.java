package com.winlator.d2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;







final class D2PlaystyleChoice {
    private D2PlaystyleChoice() {}


    static final String KEY_CHOSEN = "swChosen";






    static boolean needed(Map<String, ?> stored) {
        return !Boolean.TRUE.equals(stored.get(KEY_CHOSEN)) && !stored.containsKey(D2Spielweise.KEY_PRESET) && !D2Spielweise.own(stored);
    }


    static String preselected(Map<String, ?> stored) {
        Object p = stored.get(D2Spielweise.KEY_PRESET);
        return D2Spielweise.preset(p instanceof String ? (String) p : null);
    }


    static final class Row {
        final D2Spielweise.Regulator regulator;
        final int classic, enhanced;

        Row(D2Spielweise.Regulator r) {
            regulator = r;
            classic = r.preset(D2Spielweise.CLASSIC);
            enhanced = r.preset(D2Spielweise.ENHANCED);
        }

        boolean differs() {
            return classic != enhanced;
        }
    }


    static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        for (D2Spielweise.Regulator r : D2Spielweise.ALL) if (r.inPresets) rows.add(new Row(r));
        return rows;
    }


    static final String[] STRINGS = {"choice_title", "choice_intro", "choice_classic_sub", "choice_classic_points",
        "choice_enhanced_sub", "choice_enhanced_points", "choice_chosen", "choice_default", "choice_chosen_default",
        "choice_differences", "choice_footer", "choice_go_classic", "choice_go_enhanced", "settings_sw_classic",
        "settings_sw_enhanced", "choice_row_quality", "choice_row_nodrop", "choice_row_runes", "choice_row_boss",
        "choice_row_uniques", "choice_row_gamble", "choice_row_exp", "choice_row_density", "choice_row_inventory", "choice_row_shared", "choice_row_sort", "choice_row_respec",
        "choice_row_ladder", "choice_row_cow", "choice_row_uber", "choice_row_world", "settings_sw_nodrop_one", "settings_sw_nodrop_stage", "settings_sw_exp_0",
        "choice_inv_small", "choice_inv_big", "settings_sw_stage_original", "settings_sw_quality_1",
        "settings_sw_quality_2", "settings_sw_runes_stage", "settings_sw_boss_1", "settings_sw_boss_2",
        "settings_sw_density_0", "settings_sw_density_1", "settings_sw_density_2", "settings_sw_density_3",
        "settings_sw_density_4", "settings_sw_gamble_1", "settings_sw_gamble_2", "settings_sw_exp_1", "settings_sw_exp_2",
        "settings_on", "settings_off", "choice_val_quality_2", "choice_val_runes_2", "choice_val_exp_0", "choice_val_exp_2",
        "choice_val_boss_2"};


    static String label(D2Spielweise.Regulator r, Function<String, String> s) {
        return s.apply("choice_row_" + rowName(r));
    }

    static String rowName(D2Spielweise.Regulator r) {
        if (r == D2Spielweise.QUALITY) return "quality";
        if (r == D2Spielweise.NODROP) return "nodrop";
        if (r == D2Spielweise.RUNES) return "runes";
        if (r == D2Spielweise.BOSS) return "boss";
        if (r == D2Spielweise.UNIQUES) return "uniques";
        if (r == D2Spielweise.GAMBLE) return "gamble";
        if (r == D2Spielweise.EXPERIENCE) return "exp";
        if (r == D2Spielweise.DENSITY) return "density";
        if (r == D2Spielweise.INVENTORY) return "inventory";
        if (r == D2Spielweise.LADDER) return "ladder";
        if (r == D2Spielweise.SHARED) return "shared";
        if (r == D2Spielweise.SORT) return "sort";
        if (r == D2Spielweise.RESPEC) return "respec";
        if (r == D2Spielweise.COW) return "cow";
        if (r == D2Spielweise.UBER) return "uber";
        if (r == D2Spielweise.WORLD) return "world";
        throw new IllegalArgumentException("no row name for " + r.key);
    }





    static String stage(D2Spielweise.Regulator r, int stage, Function<String, String> s) {
        String shorter = s.apply("choice_val_" + rowName(r) + "_" + stage);
        if (shorter != null) return shorter;
        String original = s.apply("settings_sw_stage_original");
        if (r == D2Spielweise.QUALITY) return stage == 0 ? original : s.apply("settings_sw_quality_" + stage);
        if (r == D2Spielweise.NODROP) return stage == 0 ? s.apply("settings_sw_nodrop_one")
            : String.format(Locale.ROOT, s.apply("settings_sw_nodrop_stage"), new int[]{1, 3, 5, 8}[stage]);
        if (r == D2Spielweise.RUNES) return stage == 0 ? original : String.format(Locale.ROOT, s.apply("settings_sw_runes_stage"), 1 << stage);
        if (r == D2Spielweise.BOSS) return stage == 0 ? s.apply("settings_off") : s.apply("settings_sw_boss_" + stage);
        if (r == D2Spielweise.DENSITY) return s.apply("settings_sw_density_" + stage);
        if (r == D2Spielweise.GAMBLE) return stage == 0 ? original : s.apply("settings_sw_gamble_" + stage);
        if (r == D2Spielweise.EXPERIENCE) return s.apply("settings_sw_exp_" + stage);
        if (r == D2Spielweise.INVENTORY) return s.apply(stage == 0 ? "choice_inv_small" : "choice_inv_big");
        return s.apply(stage == 0 ? "settings_off" : "settings_on");
    }




    static final class Art {
        final D2GameFont font16, font24, font30, formal10, exocet10;

        final D2GameFont.Img cardFrame, button, buttonPressed;

        final int[] backdrop;

        Art(D2StartArt start, D2Sprites sprites) throws IOException {
            font16 = start.font16;
            formal10 = start.formal10;
            exocet10 = start.exocet10;
            cardFrame = start.cardFrame;
            button = start.button;
            buttonPressed = start.buttonPressed;
            font24 = D2GameFont.load(sprites, "font24");
            font30 = D2GameFont.load(sprites, "font30");
            if (font24 == null || font30 == null) throw new IOException("choice fonts missing");
            D2Sprites.Frame[] tiles = sprites.dc6(D2StartArt.FRONT_END + "TCPIPscreen.dc6", "Sky");
            int[] screen = tiles != null ? D2StartArt.tiled(tiles, D2StartArt.SCREEN_W, D2StartArt.SCREEN_H) : start.screen.clone();
            for (int i = 0; i < screen.length; i++) screen[i] = D2StartArt.dim(screen[i], 35);
            backdrop = screen;
        }
    }

    static final int GOLD = 0xC8B074, TEXT = 0xD2CEC6, GREY = 0x9A9488, DIM = 0x8A857C, HINT = 0xA0A0A0;

    static final int MAX_COLUMN = 400, BAND = 62;

    static final int MARGIN = 12, GAP = 8;


    static final class Drawn {
        final D2GameFont.Img img;
        final int[] classicCard, enhancedCard;

        final boolean tableBig;

        Drawn(D2GameFont.Img img, int[] classicCard, int[] enhancedCard, boolean tableBig) {
            this.img = img;
            this.classicCard = classicCard;
            this.enhancedCard = enhancedCard;
            this.tableBig = tableBig;
        }


        String presetAt(int y) {
            if (y >= classicCard[0] && y < classicCard[0] + classicCard[1]) return D2Spielweise.CLASSIC;
            if (y >= enhancedCard[0] && y < enhancedCard[0] + enhancedCard[1]) return D2Spielweise.ENHANCED;
            return null;
        }
    }


    static final class Canvas {
        final int w, h;
        final int[] px;

        Canvas(int w, int h) {
            this.w = w;
            this.h = h;
            px = new int[w * h];
        }

        void blit(D2GameFont.Img img, int x, int y) {
            for (int j = 0; j < img.height; j++) {
                int Y = y + j;
                if (Y < 0 || Y >= h) continue;
                for (int i = 0; i < img.width; i++) {
                    int p = img.argb[j * img.width + i], X = x + i;
                    if (p == 0 || X < 0 || X >= w) continue;
                    px[Y * w + X] = over(p, px[Y * w + X]);
                }
            }
        }

        void fill(int x, int y, int fw, int fh, int argb) {
            for (int Y = Math.max(0, y); Y < Math.min(h, y + fh); Y++) {
                for (int X = Math.max(0, x); X < Math.min(w, x + fw); X++) px[Y * w + X] = over(argb, px[Y * w + X]);
            }
        }

        int text(D2GameFont f, String s, int rgb, int x, int y) {
            D2GameFont.Img img = f.render(s, rgb);
            blit(img, x, y);
            return img.width;
        }


        int wrapped(D2GameFont f, String s, int rgb, int x, int y, int maxW, int lineH) {
            for (String l : f.wrap(s, maxW)) {
                text(f, l, rgb, x, y);
                y += lineH;
            }
            return y;
        }

        D2GameFont.Img img() {
            return new D2GameFont.Img(w, h, px);
        }
    }


    static int over(int p, int q) {
        int a = p >>> 24;
        if (a == 255) return p;
        if (a == 0) return q;
        int qa = q >>> 24, oa = a + qa * (255 - a) / 255;
        if (oa == 0) return 0;
        int r = ((p >> 16 & 255) * a + (q >> 16 & 255) * qa * (255 - a) / 255) / oa;
        int g = ((p >> 8 & 255) * a + (q >> 8 & 255) * qa * (255 - a) / 255) / oa;
        int b = ((p & 255) * a + (q & 255) * qa * (255 - a) / 255) / oa;
        return oa << 24 | r << 16 | g << 8 | b;
    }

    private static final int TITLE_H = 36, LINE_H = 13, POINT_H = 14, ROW_H = 18, ROW_H_SMALL = 14;


    static List<String> points(String text) {
        List<String> out = new ArrayList<>();
        for (String l : text.split("\n")) if (!l.trim().isEmpty()) out.add(l.trim());
        return out;
    }

    private static final String DASH = "- ";


    private static int point(Canvas cv, D2GameFont f, String p, int rgb, int x, int y, int maxW) {
        int dash = f.width(DASH);
        List<String> lines = f.wrap(p, maxW - dash);
        if (cv != null) {
            cv.text(f, DASH, rgb, x, y);
            for (int i = 0; i < lines.size(); i++) cv.text(f, lines.get(i), rgb, x + dash, y + i * POINT_H);
        }
        return y + lines.size() * POINT_H;
    }


    static Drawn draw(Art art, Function<String, String> s, int w, String preset) {
        boolean enh = D2Spielweise.ENHANCED.equals(preset);
        int cw = w - 2 * MARGIN;

        String title = s.apply("choice_title");
        D2GameFont titleFont = art.font30.width(title) <= cw ? art.font30 : art.font24;
        List<String> intro = art.formal10.wrap(s.apply("choice_intro"), cw);
        String[][] cards = {
            {s.apply("settings_sw_classic"), s.apply("choice_classic_sub"), s.apply("choice_classic_points")},
            {s.apply("settings_sw_enhanced"), s.apply("choice_enhanced_sub"), s.apply("choice_enhanced_points")}};
        int[] cardH = new int[2];
        for (int c = 0; c < 2; c++) {
            int lines = 0;
            for (String p : points(cards[c][2])) lines += (point(null, art.formal10, p, 0, 0, 0, cw - 24)) / POINT_H;
            int sub = art.formal10.wrap(cards[c][1], cw - 24).size();
            cardH[c] = 40 + sub * POINT_H + 4 + lines * POINT_H + 4;
        }

        List<Row> rows = rows();
        String[][] cells = new String[rows.size()][3];
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            cells[i] = new String[]{label(r.regulator, s), stage(r.regulator, r.classic, s), stage(r.regulator, r.enhanced, s)};
        }
        String headC = s.apply("settings_sw_classic"), headE = s.apply("settings_sw_enhanced");
        int[] cols = columns(art.font16, cells, art.formal10, headC, headE, cw);
        boolean big = cols != null;
        D2GameFont tf = big ? art.font16 : art.formal10;
        if (!big) cols = columns(art.formal10, cells, art.formal10, headC, headE, cw);
        if (cols == null) cols = new int[]{0, cw / 2, cw * 3 / 4};
        int rowH = big ? ROW_H : ROW_H_SMALL;
        List<String> footer = art.formal10.wrap(s.apply("choice_footer"), cw);
        int h = 12 + TITLE_H + intro.size() * LINE_H + 8 + cardH[0] + GAP + cardH[1] + GAP + 6 + 22 + rows.size() * rowH + 2
            + footer.size() * LINE_H + 12;

        Canvas cv = new Canvas(w, h);
        int x0 = MARGIN, y = 12;
        cv.text(titleFont, title, GOLD, x0, titleFont == art.font30 ? y : y + 4);
        y += TITLE_H;
        for (String l : intro) {
            cv.text(art.formal10, l, HINT, x0, y);
            y += LINE_H;
        }
        y += 8;
        int[][] places = new int[2][];
        for (int c = 0; c < 2; c++) {
            boolean on = (c == 1) == enh;
            int ch = cardH[c];
            places[c] = new int[]{y, ch};
            cv.fill(x0, y, cw, ch, on ? 0xD0140F08 : 0xB0000000);
            if (art.cardFrame != null) {
                int[] fr = D2StartArt.frame(art.cardFrame, D2StartArt.CARD_FRAME_EDGE, cw, ch);
                for (int i = 0; i < fr.length; i++) fr[i] = D2StartArt.tint(fr[i], on ? D2StartArt.FRAME_LAST : D2StartArt.FRAME_OTHER);
                cv.blit(new D2GameFont.Img(cw, ch, fr), x0, y);
            }
            else {
                int line = on ? 0xFFC8B074 : 0xFF5A4A30;
                cv.fill(x0, y, cw, 1, line);
                cv.fill(x0, y + ch - 1, cw, 1, line);
                cv.fill(x0, y, 1, ch, line);
                cv.fill(x0 + cw - 1, y, 1, ch, line);
            }
            cv.text(art.font24, cards[c][0], on ? GOLD : GREY, x0 + 12, y + 8);
            String tag = c == 1 ? s.apply(on ? "choice_chosen_default" : "choice_default") : on ? s.apply("choice_chosen") : "";
            if (!tag.isEmpty()) cv.text(art.formal10, tag, on ? GOLD : 0x808080, x0 + cw - 12 - art.formal10.width(tag), y + 14);
            int ly = cv.wrapped(art.formal10, cards[c][1], on ? TEXT : GREY, x0 + 12, y + 34, cw - 24, POINT_H) + 4;
            for (String p : points(cards[c][2])) ly = point(cv, art.formal10, p, on ? TEXT : DIM, x0 + 12, ly, cw - 24);
            y += ch + GAP;
        }
        y += 6;
        cv.text(art.font16, s.apply("choice_differences"), GOLD, x0, y);
        cv.text(art.formal10, headC, GREY, x0 + cols[1], y + 4);
        cv.text(art.formal10, headE, GREY, x0 + cols[2], y + 4);
        y += 22;
        cv.fill(x0, y - 4, cw, 1, 0xFF5A4A30);
        for (int i = 0; i < rows.size(); i++) {
            boolean differs = rows.get(i).differs();
            cv.text(tf, cells[i][0], TEXT, x0, y);
            cv.text(tf, cells[i][1], !enh && differs ? GOLD : DIM, x0 + cols[1], y);
            cv.text(tf, cells[i][2], enh && differs ? GOLD : DIM, x0 + cols[2], y);
            y += rowH;
        }
        y += 2;
        for (String l : footer) {
            cv.text(art.formal10, l, 0x808080, x0, y);
            y += LINE_H;
        }
        return new Drawn(cv.img(), places[0], places[1], big);
    }





    static int[] columns(D2GameFont f, String[][] cells, D2GameFont head, String headC, String headE, int cw) {
        int label = 0, c = head.width(headC), e = head.width(headE);
        for (String[] row : cells) {
            label = Math.max(label, f.width(row[0]));
            c = Math.max(c, f.width(row[1]));
            e = Math.max(e, f.width(row[2]));
        }
        int gap = 10;
        int need = label + gap + c + gap + e;
        if (need > cw) return null;

        int colE = cw - e;
        return new int[]{0, colE - gap - c, colE};
    }


    static D2GameFont.Img button(Art art, Function<String, String> s, String preset, boolean pressed) {
        D2GameFont.Img stone = pressed && art.buttonPressed != null ? art.buttonPressed : art.button;
        Canvas cv = new Canvas(stone.width, stone.height);
        cv.blit(stone, 0, 0);
        String label = s.apply(D2Spielweise.ENHANCED.equals(preset) ? "choice_go_enhanced" : "choice_go_classic");
        D2GameFont.Img l = art.exocet10.render(label, D2GameFont.STONE_RGB);
        cv.blit(l, (stone.width - l.width) / 2 + (pressed ? 1 : 0), (stone.height - l.height) / 2 + 1 + (pressed ? 1 : 0));
        return cv.img();
    }


    static int[] backdrop(Art art, int w, int h) {
        int[] out = new int[w * h];
        float k = Math.max((float) h / D2StartArt.SCREEN_H, (float) w / D2StartArt.SCREEN_W);
        int offX = (int) ((D2StartArt.SCREEN_W * k - w) / 2), offY = (int) ((D2StartArt.SCREEN_H * k - h) / 2);
        for (int y = 0; y < h; y++) {
            int sy = Math.min(D2StartArt.SCREEN_H - 1, (int) ((y + offY) / k));
            for (int x = 0; x < w; x++) {
                int sx = Math.min(D2StartArt.SCREEN_W - 1, (int) ((x + offX) / k));
                out[y * w + x] = art.backdrop[sy * D2StartArt.SCREEN_W + sx];
            }
        }
        return out;
    }
}
