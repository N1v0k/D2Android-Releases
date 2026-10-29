package com.winlator.d2;














final class D2StartLayout {
    final int width, height;
    final float u;

    final float left, right;




    static final float FIXED_UNITS = 620 - (35 * 2.2f + 10);




    static final float MIN_HEIGHT_UNITS = 574 + 214 + FIXED_UNITS;




    final float modTop, modBottom;

    final float scale, cutX;

    final int backdropHeight;
    final float hintY, listTop, listBottom, rowHeight, cardHeight, cardLeft, cardRight, figureX, textX, whenRight;

    final float textScale, smallScale;

    final float statusScale;




    final float statusBandTop, statusBandBottom;

    static final float STATUS_MIN_UNITS = 1f;
    final float scrollX, countY, lineY, buttonsTop, buttonWidth, buttonHeight, firstGap, gap, footerY;

    final float classicY;




    final float classicScale, footerScale;

    static final int BUTTONS = 4;

    static final float TWO_COLUMNS_FROM = 1.25f;

    static final float TWO_HEIGHT_UNITS = 1000, TWO_WIDTH_UNITS = 1480;

    final boolean twoColumns;




    final float menuX, menuWidth, modLeft, modRight;




    final float listLeft, listX;

    D2StartLayout(int width, int height) {
        this.width = width;
        this.height = height;
        twoColumns = width >= TWO_COLUMNS_FROM * height;
        u = twoColumns ? Math.min(height / TWO_HEIGHT_UNITS, width / TWO_WIDTH_UNITS)
            : Math.min(width / 900f, height / MIN_HEIGHT_UNITS);
        scale = 1.95f * u;
        backdropHeight = (int) Math.ceil(height / scale) + 1;
        modTop = 450 * u;
        modBottom = 522 * u;
        buttonWidth = 272 * 2.2f * u;
        buttonHeight = 35 * 2.2f * u;
        firstGap = 22 * u;
        gap = 10 * u;
        footerY = height - 34 * u;
        classicY = footerY - 40 * u;
        rowHeight = 214 * u;
        cardHeight = 200 * u;
        textScale = 2 * u;
        statusScale = 2.4f * u;
        smallScale = 1 * u;
        classicScale = 1.6f * 1.08f * smallScale;
        footerScale = 1.4f * 1.08f * smallScale;
        if (twoColumns) {
            menuWidth = Math.max(780 * u, Math.min(width * 0.42f, 900 * u));
            menuX = menuWidth / 2;

            cutX = D2StartArt.SCREEN_W / 2f - menuX / scale;
            modLeft = menuX - 360 * u;
            modRight = menuX + 360 * u;
            left = menuWidth;
            right = width;
            float cardWidth = Math.min(808 * u, right - left - 70 * u);
            cardLeft = left + (right - left - 50 * u) / 2 - cardWidth / 2 + 10 * u;
            cardRight = cardLeft + cardWidth;
            listLeft = left;
            hintY = 44 * u;
            listTop = 96 * u;
            listBottom = height - 62 * u;
            countY = listBottom + 6 * u;
            lineY = -1;
            buttonsTop = 552 * u;
            statusBandTop = 8 * u;
        } else {
            menuWidth = width;
            menuX = width / 2f;
            left = (width - 900 * u) / 2;
            right = left + 900 * u;
            cutX = (D2StartArt.SCREEN_W - width / scale) / 2;
            modLeft = left + 30 * u;
            modRight = right - 30 * u;
            cardLeft = left + 30 * u;
            cardRight = right - 62 * u;
            listLeft = 0;
            hintY = 532 * u;
            listTop = 574 * u;

            float fixed = Math.max(FIXED_UNITS * u, 58 * u + BUTTONS * buttonHeight + firstGap + (BUTTONS - 2) * gap + 70 * u);
            listBottom = Math.max(listTop + 214 * u, height - fixed);
            countY = listBottom + 8 * u;
            lineY = listBottom + 40 * u;
            buttonsTop = listBottom + 58 * u;
            statusBandTop = modBottom + 3 * u;
        }
        listX = twoColumns ? (cardLeft + cardRight) / 2 : width / 2f;
        figureX = cardLeft + 75 * u;
        textX = cardLeft + 170 * u;
        whenRight = cardRight - 20 * u;
        statusBandBottom = listTop - 3 * u;
        scrollX = cardRight + 22 * u;
    }





    float countY(int rows) {
        if (!twoColumns) return countY;
        return Math.min(listBottom, listTop + rows * rowHeight - 6 * u) + 12 * u;
    }





    float fit(float width, float room) {
        return width * textScale <= room ? textScale : Math.max(0.6f * textScale, room / Math.max(1, width));
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
        float step = font.height * 0.9f, band = statusBandBottom - statusBandTop, room = twoColumns ? cardRight - cardLeft - 16 * u : right - left - 60 * u;
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
