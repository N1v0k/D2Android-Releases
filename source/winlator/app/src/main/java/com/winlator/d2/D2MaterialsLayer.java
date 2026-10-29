package com.winlator.d2;

import java.util.Locale;









final class D2MaterialsLayer {

    static final int PIC_BOXPIECES = 0, PIC_BLANK = 1, PIC_MEDIUM = 2, PIC_MATERIAL = 3;
    static final String PATH_BOXPIECES = "data\\global\\ui\\MENU\\boxpieces", PATH_BLANK = "data\\global\\ui\\PANEL\\buysellbtn",
        PATH_MEDIUM = "data\\global\\ui\\FrontEnd\\MediumButtonBlank", PATH_ITEMS = "data\\global\\items\\";

    static final int BLANK = 32;

    static final int FRAME_ANVIL = 6;








    static final int PIC_GOLD_FIELD = 91, PIC_GOLD_COIN = 92;
    static final String PATH_GOLD_FIELD = "data\\global\\ui\\PANEL\\TradeStash", PATH_GOLD_COIN = "data\\global\\ui\\Panel\\goldcoinbtn";
    static final int GOLD_CROP_X = 72, GOLD_CROP_Y = 22, GOLD_W = 179, GOLD_H = 20, GOLD_X = 152, GOLD_Y = 61;
    static final int GOLD_COIN_X = 3, GOLD_COIN_W = 20, GOLD_COIN_H = 18, GOLD_LABEL_X = 27, GOLD_AMOUNT_RIGHT = 174;

    static final int DOWN_FONT8 = 7, DOWN_FONT16 = 8, DOWN_EXOCET = 6;




    static final int STONE_DOWN = 2;




    static final int MEDIUM_ROOM = 96;

    static final int PAGE_ROOM = D2PageButtons.STONE_W - 8;





    static final int FRAME_GOLD = 0xAC9C64, FRAME_LIGHT = 0xB8B8B8, FRAME_DARK = 0x040404, FRAME = 2;


    interface Art {

        int[] materialCrop(int kind);


        String materialFile(int kind);


        D2Frame frame();


        int colour(int rgb);


        int width(int font, String text);


        String gameText(String code);


        int charWidth(int font, int c);


        int ink();
    }


    static final class State {
        boolean open, entryDown, pressing, locked;

        boolean entry = true;

        long sharedGold = -1;
        String sharedGoldLabel;
        int tab = D2Materials.TAB_RUNES, selected = -1, choiceKind = -1, downHit = D2Materials.HIT_NONE;
        int originX, originY;
        String title, tabRunes, tabGems, info, deposit, choiceAll;

        String recipeLine;
    }

    private D2MaterialsLayer() {}


    static void build(D2Layer l, Art art, D2Materials.Snapshot s, State st) {
        int ox = st.originX, oy = st.originY;

        if (st.sharedGold >= 0 && !(st.entry && st.open)) sharedGold(l, art, ox + GOLD_X, oy + GOLD_Y, st.sharedGoldLabel, st.sharedGold);
        if (!st.entry) return;
        boolean down = st.open || st.entryDown;

        int ex = ox + D2Materials.ENTRY_X + 1, ey = oy + D2Materials.ENTRY_Y + 1;
        stoneFrame(l, art, ex, ey, BLANK, BLANK);
        l.picture(PIC_BLANK, PATH_BLANK, FRAME_ANVIL + (down ? 1 : 0), 0, 0, 0, 0, ex, ey, BLANK, BLANK);
        if (st.locked) l.rect(ex, ey, BLANK, BLANK, 0, D2Layer.ALPHA_50);
        if (st.open) {
            int gold = art.colour(0xDCBE6E);
            l.outline(ox + D2Materials.ENTRY_X - 1, oy + D2Materials.ENTRY_Y - 1, D2Materials.ENTRY_SIZE + 2, D2Materials.ENTRY_SIZE + 2, gold);
            l.outline(ox + D2Materials.ENTRY_X, oy + D2Materials.ENTRY_Y, D2Materials.ENTRY_SIZE, D2Materials.ENTRY_SIZE, gold);
        }
        if (!st.open) return;

        int wx = ox + D2Materials.WIN_X, wy = oy + D2Materials.WIN_Y;
        l.rect(wx, wy, D2Materials.WIN_W, D2Materials.WIN_H, art.colour(0x090807), D2Layer.OPAQUE);
        int cx = wx + D2Materials.WIN_W / 2, room = D2Materials.WIN_W - 2 * D2Materials.TEXT_MARGIN, titleY = wy + 12;
        String name = st.selected >= 0 ? D2Materials.name(st.selected, art.gameText(D2Materials.code(st.selected))) : null;
        if (name == null) l.text(cx, titleY + DOWN_FONT16, D2Layer.FONT16, D2Layer.GOLD, D2Layer.CENTRE, upper(st.title));
        else if (art.width(D2Layer.FONT16, name) <= room) l.text(cx, titleY + DOWN_FONT16, D2Layer.FONT16, D2Layer.GOLD, D2Layer.CENTRE, name);
        else l.text(cx, titleY + DOWN_FONT8, D2Layer.FONT8, D2Layer.GOLD, D2Layer.CENTRE,
            D2Materials.fit(name, t -> art.width(D2Layer.FONT8, t), room));

        int tabsLeft = wx + (D2Materials.WIN_W - 2 * D2Materials.TAB_W - 6) / 2;
        for (int t = 0; t < 2; t++) {
            boolean on = t == st.tab;
            int tx = tabsLeft + t * (D2Materials.TAB_W + 6), ty = oy + D2Materials.TAB_Y;
            box(l, art, tx, ty, D2Materials.TAB_W, D2Materials.TAB_H, on ? 0x2E2616 : 0x100F0E);
            if (!on) l.rect(tx, ty, D2Materials.TAB_W, D2Materials.TAB_H, 0, D2Layer.ALPHA_50);
            l.text(tx + D2Materials.TAB_W / 2, ty + D2Materials.TAB_H / 2 + DOWN_FONT8, D2Layer.FONT8, on ? D2Layer.GOLD : D2Layer.GREY,
                D2Layer.CENTRE, upper(t == 0 ? st.tabRunes : st.tabGems));
        }

        int cellFill = art.colour(0x16161E), cellLine = art.colour(0x343440), gold = art.colour(0xDCBE6E);
        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 6; column++) {
                int kind = D2Materials.kindAt(st.tab, column, row);
                if (kind < 0) continue;
                int x = ox + D2Materials.GRID_X + column * D2Materials.CELL, y = oy + D2Materials.GRID_Y + row * D2Materials.CELL;
                int inner = D2Materials.CELL - 2;
                l.rect(x + 1, y + 1, inner, inner, cellFill, D2Layer.OPAQUE);
                l.outline(x + 1, y + 1, inner, inner, kind == st.selected ? gold : cellLine);
                int count = s != null ? s.stash[kind] : 0;
                int[] c = art.materialCrop(kind);
                if (c != null) {
                    int pw = Math.min(c[4], inner), ph = Math.min(c[5], inner);
                    int px = x + 1 + (inner - pw) / 2, py = y + 1 + (inner - ph) / 2;
                    material(l, art, kind, px, py, pw, ph);
                    if (count == 0) l.rect(px, py, pw, ph, 0, D2Layer.ALPHA_75);
                }
                else l.text(x + D2Materials.CELL / 2, y + D2Materials.CELL / 2 + DOWN_FONT8, D2Layer.FONT8,
                    count > 0 ? D2Layer.WHITE : D2Layer.GREY, D2Layer.CENTRE, D2Materials.code(kind));
                l.text(x + D2Materials.CELL - 4, y + D2Materials.CELL - 1, D2Layer.FONT8, count > 0 ? D2Layer.WHITE : D2Layer.GREY,
                    D2Layer.RIGHT, Integer.toString(count));
            }
        }

        l.text(cx, oy + D2Materials.INFO_Y + DOWN_FONT8, D2Layer.FONT8, D2Layer.GREY, D2Layer.CENTRE,
            D2Materials.fit(st.info, t -> art.width(D2Layer.FONT8, t), room));

        if (st.recipeLine != null) {
            String line = D2Materials.fit(st.recipeLine, t -> art.width(D2Layer.FONT8, t), room - 16);
            int w = art.width(D2Layer.FONT8, line) + 16, x = cx - w / 2, y = oy + D2Materials.RECIPE_Y;
            box(l, art, x, y, w, D2Materials.RECIPE_H, 0x2E2616);
            l.text(cx, y + D2Materials.RECIPE_H / 2 + DOWN_FONT8, D2Layer.FONT8, D2Layer.GOLD, D2Layer.CENTRE, line);
        }

        boolean pressed = st.pressing && st.downHit == D2Materials.HIT_DEPOSIT_ALL;
        int bx = ox + D2Materials.BUTTON_X, by = oy + D2Materials.BUTTON_Y;
        l.picture(PIC_MEDIUM, PATH_MEDIUM, pressed ? 1 : 0, 0, 0, 0, 0, bx, by, D2Materials.BUTTON_W, D2Materials.BUTTON_H);
        stoneLabel(l, art, st.deposit, bx + D2Materials.BUTTON_W / 2 + (pressed ? 1 : 0), by + D2Materials.BUTTON_H / 2 + (pressed ? 1 : 0),
            MEDIUM_ROOM);

        if (st.choiceKind >= 0) {
            int[] b = D2Materials.choiceAt(st.choiceKind);
            int chx = ox + b[0], chy = oy + b[1];
            box(l, art, chx, chy, 3 * D2Materials.CHOICE_W, D2Materials.CHOICE_H, 0x0A0A0A);
            String[] labels = {"1", "3", upper(st.choiceAll)};
            for (int i = 0; i < 3; i++) {
                boolean can = i != 1 || (s != null && s.stash[st.choiceKind] >= 3);
                l.text(chx + i * D2Materials.CHOICE_W + D2Materials.CHOICE_W / 2, chy + D2Materials.CHOICE_H / 2 + DOWN_FONT16,
                    D2Layer.FONT16, can ? D2Layer.GOLD : D2Layer.GREY, D2Layer.CENTRE, labels[i]);
            }
        }
    }






    static void pageStone(D2Layer l, Art art, String label, int x, int y, boolean pressed, int colour) {
        int frame = pressed ? 1 : 0, h = D2PageButtons.STONE_H, e = D2PageButtons.EDGE, t = D2PageButtons.TILE;
        l.picture(PIC_BLANK, PATH_BLANK, frame, 0, 0, e, h, x, y, e, h);
        for (int i = 0; i < D2PageButtons.TILES; i++) l.picture(PIC_BLANK, PATH_BLANK, frame, e, 0, t, h, x + e + i * t, y, t, h);
        l.picture(PIC_BLANK, PATH_BLANK, frame, e + t, 0, e, h, x + D2PageButtons.STONE_W - e, y, e, h);
        int shift = pressed ? 1 : 0;
        stoneLabel(l, art, label, x + D2PageButtons.STONE_W / 2 + shift, y + h / 2 + shift, PAGE_ROOM, colour);
    }





    static void sharedGold(D2Layer l, Art art, int x, int y, String label, long gold) {
        l.picture(PIC_GOLD_FIELD, PATH_GOLD_FIELD, 0, GOLD_CROP_X, GOLD_CROP_Y, GOLD_W, GOLD_H, x, y, GOLD_W, GOLD_H);
        l.picture(PIC_GOLD_COIN, PATH_GOLD_COIN, 0, 0, 0, 0, 0, x + GOLD_COIN_X, y, GOLD_COIN_W, GOLD_COIN_H);
        String amount = Long.toString(gold);
        int bottom = y + GOLD_H / 2 + DOWN_FONT16, right = x + GOLD_AMOUNT_RIGHT;
        l.text(right, bottom, D2Layer.FONT16, D2Layer.WHITE, D2Layer.RIGHT, amount);
        if (label != null && x + GOLD_LABEL_X + art.width(D2Layer.FONT16, label) + 4 <= right - art.width(D2Layer.FONT16, amount))
            l.text(x + GOLD_LABEL_X, bottom, D2Layer.FONT16, D2Layer.GOLD, D2Layer.LEFT, label);
    }







    static void stoneLabel(D2Layer l, Art art, String text, int cx, int cy, int room) {
        stoneLabel(l, art, text, cx, cy, room, D2Layer.STONE_TEXT);
    }


    static void stoneLabel(D2Layer l, Art art, String text, int cx, int cy, int room, int colour) {
        if (text == null) return;
        boolean small = labelSmall(art, text, room);
        int font = small ? D2Layer.FONT8 : D2Layer.STONE_FONT;
        String t = small ? text : upper(text);
        l.text(cx, cy + STONE_DOWN + (small ? DOWN_FONT8 : DOWN_EXOCET), font, colour, D2Layer.CENTRE, D2InputBox.fit(art, font, t, room));
    }


    static boolean labelSmall(Art art, String text, int room) {
        return art != null && text != null && art.width(D2Layer.STONE_FONT, upper(text)) > room;
    }





    static void stoneFrame(D2Layer l, Art art, int x, int y, int w, int h) {
        int gold = art.colour(FRAME_GOLD), light = art.colour(FRAME_LIGHT), dark = art.colour(FRAME_DARK);
        l.outline(x - 1, y - 1, w + 2, h + 2, gold);
        l.rect(x - 2, y - 2, w + 4, 1, light, D2Layer.OPAQUE);
        l.rect(x + w + 1, y - 1, 1, h + 3, light, D2Layer.OPAQUE);
        l.rect(x - 2, y + h + 1, w + 3, 1, dark, D2Layer.OPAQUE);
        l.rect(x - 2, y - 1, 1, h + 2, dark, D2Layer.OPAQUE);
    }


    static void box(D2Layer l, Art art, int x, int y, int w, int h, int fillRgb) {
        D2Frame f = art.frame();
        if (f == null) {
            l.rect(x, y, w, h, art.colour(fillRgb), D2Layer.OPAQUE);
            return;
        }
        l.rect(x + 1, y + 1, w - 2, h - 2, art.colour(fillRgb), D2Layer.OPAQUE);
        f.pieces(w, h, (frame, sx, sy, pw, ph, dx, dy) ->
            l.picture(PIC_BOXPIECES, PATH_BOXPIECES, frame, sx, sy, pw, ph, x + dx, y + dy, pw, ph));
    }





    static void scaledFrame(D2Layer l, D2Frame f, int x, int y, int w, int h, int s) {
        if (f == null) return;
        f.pieces(Math.round(w / (float) s), Math.round(h / (float) s), (frame, sx, sy, pw, ph, dx, dy) ->
            l.picture(PIC_BOXPIECES, PATH_BOXPIECES, frame, sx, sy, pw, ph, x + dx * s, y + dy * s, pw * s, ph * s));
    }

    private static void material(D2Layer l, Art art, int kind, int x, int y, int w, int h) {
        int[] c = art.materialCrop(kind);
        String file = art.materialFile(kind);
        if (c == null || file == null) return;
        l.picture(PIC_MATERIAL + kind, PATH_ITEMS + file, 0, c[2], c[3], c[4], c[5], x, y, w, h);
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT);
    }
}
