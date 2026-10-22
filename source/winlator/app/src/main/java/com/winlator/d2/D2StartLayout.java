package com.winlator.d2;

final class D2StartLayout {
    final int width, height;
    final float u;
    final float scale, cutX;
    final int backdropHeight;
    final float hintY, listTop, listBottom, rowHeight, cardHeight, cardLeft, cardRight, figureX, textX, whenRight;
    final float textScale, smallScale;
    final float statusScale;
    final float statusBandTop, statusBandBottom;
    static final float STATUS_MIN_UNITS = 1f;
    final float scrollX, countY, lineY, buttonsTop, buttonWidth, buttonHeight, firstGap, gap, footerY;
    final float classicY;
    static final int BUTTONS = 5;

    D2StartLayout(int width, int height) {
        this.width = width;
        this.height = height;
        u = width / 900f;
        scale = 1.95f * u;
        cutX = (D2StartArt.SCREEN_W - width / scale) / 2;
        backdropHeight = (int) Math.ceil(height / scale) + 1;
        hintY = 458 * u;
        listTop = 500 * u;
        buttonWidth = 272 * 2.2f * u;
        buttonHeight = 35 * 2.2f * u;
        firstGap = 22 * u;
        gap = 10 * u;
        footerY = height - 34 * u;
        classicY = footerY - 40 * u;
        float fixed = Math.max(620 * u, 58 * u + BUTTONS * buttonHeight + firstGap + (BUTTONS - 2) * gap + 70 * u);
        listBottom = Math.max(listTop + 214 * u, height - fixed);
        countY = listBottom + 8 * u;
        lineY = listBottom + 40 * u;
        buttonsTop = listBottom + 58 * u;
        rowHeight = 214 * u;
        cardHeight = 200 * u;
        cardLeft = 30 * u;
        cardRight = width - 62 * u;
        figureX = 105 * u;
        textX = 200 * u;
        whenRight = width - 82 * u;
        textScale = 2 * u;
        statusScale = 2.4f * u;
        statusBandTop = 446 * u;
        statusBandBottom = listTop - 3 * u;
        smallScale = 1 * u;
        scrollX = width - 40 * u;
    }

    static final class Status {
        final java.util.List<String> lines;
        final float scale, top, step;

        Status(java.util.List<String> lines, float scale, float top, float step) {
            this.lines = lines;
            this.scale = scale;
            this.top = top;
            this.step = step;
        }
    }

    Status status(D2GameFont font, String text) {
        float step = font.height * 0.9f, band = statusBandBottom - statusBandTop, room = width - 60 * u;
        float k = statusScale;
        while (true) {
            java.util.List<String> lines = font.wrap(text, room / k);
            if (lines.isEmpty()) lines = java.util.Collections.singletonList(text.trim());
            int inkTop = D2GameFont.inkRows(font.render(lines.get(0), 0xFFFFFF))[0];
            int inkBottom = D2GameFont.inkRows(font.render(lines.get(lines.size() - 1), 0xFFFFFF))[1];
            float ink = ((lines.size() - 1) * step + inkBottom - inkTop) * k;
            if (ink <= band || k - 0.1f * u < STATUS_MIN_UNITS * u - 0.001f) {
                float top = statusBandTop + Math.max(0, band - ink) / 2 - inkTop * k;
                return new Status(lines, k, top, step * k);
            }
            k -= 0.1f * u;
        }
    }

    float buttonTop(int i) {
        return buttonsTop + i * buttonHeight + (i > 0 ? firstGap + (i - 1) * gap : 0);
    }
}
