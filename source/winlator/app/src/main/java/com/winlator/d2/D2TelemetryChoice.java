package com.winlator.d2;

import java.util.List;
import java.util.Map;
import java.util.function.Function;







final class D2TelemetryChoice {
    private D2TelemetryChoice() {}


    static final String KEY = "telemetry";


    static final String[] STRINGS = {"telemetry_title", "telemetry_intro", "telemetry_in", "telemetry_in_points",
        "telemetry_out", "telemetry_out_points", "telemetry_footer", "telemetry_yes", "telemetry_no"};





    static boolean needed(boolean sends, Map<String, ?> stored) {
        return sends && !(stored.get(KEY) instanceof Boolean);
    }


    static boolean on(Map<String, ?> stored) {
        return Boolean.TRUE.equals(stored.get(KEY));
    }


    static final int BAND = 92, NO_H = 40;

    private static final int TITLE_H = 36, LINE_H = 13, POINT_H = 14;


    static D2GameFont.Img draw(D2PlaystyleChoice.Art art, Function<String, String> s, int w) {
        int m = D2PlaystyleChoice.MARGIN, cw = w - 2 * m;
        String title = s.apply("telemetry_title");
        D2GameFont titleFont = art.font30.width(title) <= cw ? art.font30 : art.font24;

        List<String> titleLines = titleFont.wrap(title, cw);
        int titleH = TITLE_H + (titleLines.size() - 1) * 24;
        List<String> intro = art.formal10.wrap(s.apply("telemetry_intro"), cw);
        String[][] cards = {{s.apply("telemetry_in"), s.apply("telemetry_in_points")},
            {s.apply("telemetry_out"), s.apply("telemetry_out_points")}};
        int[] cardH = new int[2];
        for (int c = 0; c < 2; c++) cardH[c] = 32 + pointLines(art.formal10, cards[c][1], cw - 24) * POINT_H + 8;
        List<String> footer = art.formal10.wrap(s.apply("telemetry_footer"), cw);
        int h = 12 + titleH + intro.size() * LINE_H + 8 + cardH[0] + D2PlaystyleChoice.GAP + cardH[1] + D2PlaystyleChoice.GAP
            + 4 + footer.size() * LINE_H + 12;

        D2PlaystyleChoice.Canvas cv = new D2PlaystyleChoice.Canvas(w, h);
        int y = 12;
        for (int i = 0; i < titleLines.size(); i++)
            cv.text(titleFont, titleLines.get(i), D2PlaystyleChoice.GOLD, m, (titleFont == art.font30 ? y : y + 4) + i * 24);
        y += titleH;
        for (String l : intro) {
            cv.text(art.formal10, l, D2PlaystyleChoice.HINT, m, y);
            y += LINE_H;
        }
        y += 8;
        for (int c = 0; c < 2; c++) {
            int ch = cardH[c];
            cv.fill(m, y, cw, ch, 0xC0000000);
            if (art.cardFrame != null) {
                int[] fr = D2StartArt.frame(art.cardFrame, D2StartArt.CARD_FRAME_EDGE, cw, ch);
                for (int i = 0; i < fr.length; i++) fr[i] = D2StartArt.tint(fr[i], D2StartArt.FRAME_OTHER);
                cv.blit(new D2GameFont.Img(cw, ch, fr), m, y);
            }
            else {
                cv.fill(m, y, cw, 1, 0xFF5A4A30);
                cv.fill(m, y + ch - 1, cw, 1, 0xFF5A4A30);
                cv.fill(m, y, 1, ch, 0xFF5A4A30);
                cv.fill(m + cw - 1, y, 1, ch, 0xFF5A4A30);
            }
            cv.text(art.font16, cards[c][0], D2PlaystyleChoice.GOLD, m + 12, y + 9);
            int ly = y + 30;
            for (String p : D2PlaystyleChoice.points(cards[c][1])) ly = point(cv, art.formal10, p, D2PlaystyleChoice.TEXT, m + 12, ly, cw - 24);
            y += ch + D2PlaystyleChoice.GAP;
        }
        y += 4;
        for (String l : footer) {
            cv.text(art.formal10, l, 0x808080, m, y);
            y += LINE_H;
        }
        return cv.img();
    }

    private static final String DASH = "- ";

    private static int pointLines(D2GameFont f, String points, int maxW) {
        int n = 0;
        for (String p : D2PlaystyleChoice.points(points)) n += f.wrap(p, maxW - f.width(DASH)).size();
        return n;
    }

    private static int point(D2PlaystyleChoice.Canvas cv, D2GameFont f, String p, int rgb, int x, int y, int maxW) {
        int dash = f.width(DASH);
        List<String> lines = f.wrap(p, maxW - dash);
        cv.text(f, DASH, rgb, x, y);
        for (int i = 0; i < lines.size(); i++) cv.text(f, lines.get(i), rgb, x + dash, y + i * POINT_H);
        return y + lines.size() * POINT_H;
    }


    static D2GameFont.Img yes(D2PlaystyleChoice.Art art, Function<String, String> s, boolean pressed) {
        D2GameFont.Img stone = pressed && art.buttonPressed != null ? art.buttonPressed : art.button;
        D2PlaystyleChoice.Canvas cv = new D2PlaystyleChoice.Canvas(stone.width, stone.height);
        cv.blit(stone, 0, 0);
        D2GameFont.Img l = art.exocet10.render(s.apply("telemetry_yes"), D2GameFont.STONE_RGB);
        cv.blit(l, (stone.width - l.width) / 2 + (pressed ? 1 : 0), (stone.height - l.height) / 2 + 1 + (pressed ? 1 : 0));
        return cv.img();
    }


    static D2GameFont.Img no(D2PlaystyleChoice.Art art, Function<String, String> s, boolean pressed) {
        return art.font16.render(s.apply("telemetry_no"), pressed ? D2PlaystyleChoice.GOLD : D2PlaystyleChoice.TEXT);
    }


    static int[] yesPlace(D2GameFont.Img stone, int bw) {
        return new int[]{(bw - stone.width) / 2, 10, stone.width, stone.height};
    }


    static int[] noPlace(D2GameFont.Img stone, int bw) {
        int[] st = yesPlace(stone, bw);
        return new int[]{st[0], st[1] + st[3] + 2, st[2], NO_H};
    }
}
