package com.winlator.d2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;












public final class D2RezeptBuch {

    public static final int T_TITLE = 300, T_TAB_CAN = 301, T_TAB_CUBE = 302, T_TAB_RUNEWORDS = 303, T_FILTER_ALL = 304,
        T_FILTER_READY = 305, T_FILTER_ALMOST = 306, T_SEARCH_HINT = 307, T_PICK = 308, T_PICK_HINT = 309, T_READY = 310,
        T_STASH = 311, T_MISSING = 312, T_CHAIN = 313, T_PAGE = 314, T_HINT_TOUCH = 315, T_HINT_PAD = 316, T_BACK = 317,
        T_HAVE = 318, T_LACKS = 319, T_INGREDIENTS = 320, T_LADDER_OFF = 321, T_WHERE_INV = 322, T_WHERE_CUBE = 323,
        T_WHERE_STASH = 324, T_WHERE_MAT = 325, T_CUBE_HAS = 326, T_CUBE_MAKES = 327, T_CUBE_ALMOST = 328, T_MATERIALS_LINK = 329,
        T_EMPTY = 330, T_MISSING_N = 331, T_HITS = 332, T_LADDER_MARK = 333, T_STALE = 334, T_STALE_NONE = 335, T_INSERT = 336,
        T_ON_PAGE = 337, T_ON_PAGE_SHARED = 338;

    public static final int TAB_CAN = 0, TAB_CUBE = 1, TAB_RUNEWORDS = 2;

    public static final int FILTER_ALL = 0, FILTER_READY = 1, FILTER_ALMOST = 2;

    public static final int HIT_NONE = -1, HIT_ENTRY = 1, HIT_TAB = 10, HIT_CHIP = 20, HIT_SEARCH = 30, HIT_PICK = 31,
        HIT_BACK = 32, HIT_PICK_CLEAR = 33, HIT_INSERT = 34, HIT_CARD = 40, HIT_PAGE_PREV = 50, HIT_PAGE_NEXT = 51;







    static final int RIM_X = 82, RIM_Y = 61, RIM_W = 317, RIM_H = 430, ROW_Y = 440, CORNER_X = 300;
    static final int WX = 86, WY = 64, WW = 308, WH = 372;
    static final int TAB_Y = WY + 30, TAB_H = 22, CHIP_Y = WY + 58, CHIP_H = 18, SEARCH_Y = WY + 80, SEARCH_H = 18,
        SEARCH_W = 180, CARD_Y = WY + 102, CARD_H = 46, CARD_GAP = 3, PER_PAGE = 5, ICON = 22, MARGIN = 8;

    static final int ENTRY_X = 95, ENTRY_Y = 445, ENTRY_SIZE = 34;

    static final int INSERT_X = (ENTRY_X + ENTRY_SIZE + CORNER_X) / 2 - D2PageButtons.STONE_W / 2, INSERT_Y = ENTRY_Y;

    static final int PIC_STONE = 94, STONE_FRAME = 2, STONE_SX = 6, STONE_SY = 70, STONE_SW = 112, STONE_SH = 98;
    static final String PATH_STONE = "data\\global\\ui\\PANEL\\invchar6";

    static final int BAR_W = 5;

    static final int STRIP_X = 140, STRIP_Y = 160, STRIP_W = 204, STRIP_H = 30;


    public interface Host {

        int[] crop(String file);
    }

    private final D2RezeptKatalog katalog;
    private final D2RezeptKatalog.Texte texte;
    private final Host host;
    private D2RezeptBestand bestand;
    private boolean ladderOn = true;

    private int stockAge = -1;


    private boolean open, pickMode;
    private int tab = TAB_CAN, category = D2RezeptKatalog.CAT_RUNES, filter = FILTER_ALL, page, focus = -1;
    private String query = "";
    private D2RezeptBestand.Stueck picked;
    private D2RezeptKatalog.Eintrag detail;
    private int detailScroll;

    private boolean insertPressed;

    public D2RezeptBuch(D2RezeptKatalog katalog, D2RezeptKatalog.Texte texte, Host host) {
        this.katalog = katalog;
        this.texte = texte;
        this.host = host;
        this.bestand = new D2RezeptBestand(null, null, null);
    }

    public D2RezeptKatalog katalog() {
        return katalog;
    }


    public void bestand(D2RezeptBestand b, boolean ladderOn) {
        this.bestand = b != null ? b : new D2RezeptBestand(null, null, null);
        this.bestand.makers(katalog.all());
        this.ladderOn = ladderOn;
    }

    public D2RezeptBestand bestand() {
        return bestand;
    }

    public boolean ladderOn() {
        return ladderOn;
    }


    public void stockAge(int minutes) {
        stockAge = minutes;
    }



    public boolean isOpen() {
        return open;
    }

    public void open() {
        open = true;
        detail = null;
        pickMode = false;
    }


    public void openFor(String code) {
        open();
        picked = new D2RezeptBestand.Stueck(0, code, D2Rezepte.Q_ANY, 0, -1, 0, 0, 1, 1, -1, -1, -1, -1);
        query = "";
        tab = TAB_CAN;
        page = 0;
        if (list().isEmpty()) tab = TAB_CUBE;
        if (tab == TAB_CUBE) for (D2RezeptKatalog.Eintrag e : filtered(null)) {
            category = e.isRuneword() ? category : e.category;
            break;
        }
        page = 0;
    }

    public void close() {
        open = false;
        detail = null;
        pickMode = false;
        focus = -1;
    }


    public boolean back() {
        if (!open) return false;
        if (pickMode) pickMode = false;
        else if (detail != null) detail = null;
        else close();
        return true;
    }

    public boolean pickMode() {
        return open && pickMode;
    }


    public void picked(D2RezeptBestand.Stueck s) {
        pickMode = false;
        if (s == null) return;
        picked = s;
        detail = null;
        page = 0;
        if (tab == TAB_CAN && list().isEmpty()) tab = TAB_CUBE;
    }

    public void search(String q) {
        query = q != null ? q : "";
        page = 0;
        detail = null;
    }

    public String query() {
        return query;
    }




    public List<D2RezeptKatalog.Eintrag> list() {
        List<D2RezeptKatalog.Eintrag> out = new ArrayList<>();
        if (tab == TAB_CAN) {
            for (D2RezeptKatalog.Eintrag e : filtered(null)) {
                if (!e.machbar || e.isRuneword() && e.ladder && !ladderOn) continue;
                int s = bestand.lage(e).status;
                if (s == D2RezeptBestand.NONE) continue;
                if (filter == FILTER_READY && s != D2RezeptBestand.READY) continue;
                if (filter == FILTER_ALMOST && s == D2RezeptBestand.READY) continue;
                out.add(e);
            }
            Collections.sort(out, (a, b) -> {
                D2RezeptBestand.Lage la = bestand.lage(a), lb = bestand.lage(b);
                if (la.status != lb.status) return Integer.compare(lb.status, la.status);

                if (la.missingCount != lb.missingCount) return Integer.compare(la.missingCount, lb.missingCount);
                return Integer.compare(order(a.category), order(b.category));
            });
        }
        else if (tab == TAB_RUNEWORDS) {
            for (D2RezeptKatalog.Eintrag e : filtered(null)) if (e.isRuneword()) out.add(e);

            List<D2RezeptKatalog.Eintrag> off = new ArrayList<>();
            for (D2RezeptKatalog.Eintrag e : out) if (e.ladder && !ladderOn) off.add(e);
            out.removeAll(off);
            out.addAll(off);
        }
        else {
            boolean narrowed = picked != null || !query.trim().isEmpty();
            for (D2RezeptKatalog.Eintrag e : filtered(null)) {

                if (e.isRuneword() && query.trim().isEmpty()) continue;
                if (!narrowed && e.category != category) continue;
                out.add(e);
            }
            if (narrowed) Collections.sort(out, (a, b) -> Integer.compare(a.category, b.category));
        }
        return out;
    }


    private static int order(int category) {
        return category == D2RezeptKatalog.CAT_RUNEWORD ? -1 : category;
    }

    private List<D2RezeptKatalog.Eintrag> filtered(String unused) {
        List<D2RezeptKatalog.Eintrag> out = new ArrayList<>();
        for (D2RezeptKatalog.Eintrag e : katalog.all()) {
            if (!D2RezeptKatalog.matches(e, query)) continue;
            if (picked != null && !takesPart(e, picked)) continue;
            out.add(e);
        }
        return out;
    }


    private boolean takesPart(D2RezeptKatalog.Eintrag e, D2RezeptBestand.Stueck s) {
        if (s.code.equals(e.resultCode)) return true;
        for (D2RezeptKatalog.Zutat z : e.zutaten) {
            if (z.codes != null) {
                for (String c : z.codes) if (c.equals(s.code)) return true;
            }
            else if (s.place >= 0 ? bestand.fits(z, s) : bestand.fitsMaterial(z, s.code)) return true;
        }
        return false;
    }


    public int[] counts() {
        int all = 0, ready = 0;
        for (D2RezeptKatalog.Eintrag e : filtered(null)) {
            if (!e.machbar || e.isRuneword() && e.ladder && !ladderOn) continue;
            int s = bestand.lage(e).status;
            if (s == D2RezeptBestand.NONE) continue;
            all++;
            if (s == D2RezeptBestand.READY) ready++;
        }
        return new int[]{all, ready, all - ready};
    }

    public int pages() {
        return Math.max(1, (list().size() + PER_PAGE - 1) / PER_PAGE);
    }

    public int tab() {
        return tab;
    }

    public int page() {
        return page;
    }

    public D2RezeptKatalog.Eintrag detail() {
        return detail;
    }


    public D2RezeptKatalog.Eintrag cardEntry(int hit) {
        if (!open || detail != null || hit < HIT_CARD || hit >= HIT_CARD + PER_PAGE) return null;
        List<D2RezeptKatalog.Eintrag> l = list();
        int i = page * PER_PAGE + hit - HIT_CARD;
        return i < l.size() ? l.get(i) : null;
    }


    public void insertPressed(boolean on) {
        insertPressed = on;
    }




    public int hit(float lx, float ly, boolean cubeOpen) {
        if (cubeOpen && lx >= ENTRY_X && lx < ENTRY_X + ENTRY_SIZE && ly >= ENTRY_Y && ly < ENTRY_Y + ENTRY_SIZE) return HIT_ENTRY;
        if (!open || !inWindow(lx, ly)) return HIT_NONE;
        if (ly >= TAB_Y && ly < TAB_Y + TAB_H) {
            int[] x = lastTabEdges != null ? lastTabEdges : tabEdges();
            for (int t = 0; t < 3; t++) if (lx >= x[t] && lx < x[t + 1] - 2) return HIT_TAB + t;
            return HIT_NONE;
        }
        if (detail != null) {
            if (ly >= CHIP_Y - 4 && ly < CHIP_Y + CHIP_H) return HIT_BACK;
            int sx = INSERT_X, sy = INSERT_Y;
            if (!detail.isRuneword() && lx >= sx && lx < sx + D2PageButtons.STONE_W && ly >= sy && ly < sy + D2PageButtons.STONE_H) return HIT_INSERT;
            return HIT_NONE;
        }
        if (ly >= CHIP_Y && ly < CHIP_Y + CHIP_H && tab != TAB_RUNEWORDS) {
            int[][] chips = chipBoxes();
            for (int i = 0; i < chips.length; i++) if (lx >= chips[i][0] && lx < chips[i][0] + chips[i][1]) return HIT_CHIP + i;
            return HIT_NONE;
        }
        if (ly >= SEARCH_Y && ly < SEARCH_Y + SEARCH_H) {
            int x = WX + 10;
            if (picked != null && lx >= x + SEARCH_W - 18 && lx < x + SEARCH_W) return HIT_PICK_CLEAR;
            if (lx >= x && lx < x + SEARCH_W) return HIT_SEARCH;
            if (lx >= x + SEARCH_W + 6 && lx < WX + WW - 10) return HIT_PICK;
            return HIT_NONE;
        }
        for (int i = 0; i < PER_PAGE; i++) {
            int y = CARD_Y + i * (CARD_H + CARD_GAP);
            if (ly >= y && ly < y + CARD_H && lx >= WX + MARGIN && lx < WX + WW - MARGIN) {
                return page * PER_PAGE + i < list().size() ? HIT_CARD + i : HIT_NONE;
            }
        }
        int footer = WY + WH - 18;
        if (ly >= footer && ly < ROW_Y) return lx < WX + WW / 3f ? HIT_PAGE_PREV : lx > WX + 2 * WW / 3f ? HIT_PAGE_NEXT : HIT_NONE;
        return HIT_NONE;
    }


    public static boolean inWindow(float lx, float ly) {
        return lx >= RIM_X && lx < RIM_X + RIM_W && ly >= RIM_Y && ly < RIM_Y + RIM_H && !(lx >= CORNER_X && ly >= ROW_Y);
    }


    public static boolean inCorner(float lx, float ly) {
        return lx >= CORNER_X && lx < RIM_X + RIM_W && ly >= ROW_Y && ly < RIM_Y + RIM_H;
    }


    public boolean tap(int hit) {
        if (hit == HIT_ENTRY) {
            if (open) close();
            else open();
            return false;
        }
        if (!open) return false;
        if (hit >= HIT_TAB && hit < HIT_TAB + 3) {
            tab = hit - HIT_TAB;
            page = 0;
            detail = null;
            focus = -1;
        }
        else if (hit >= HIT_CHIP && hit < HIT_CHIP + 5) {
            int i = hit - HIT_CHIP;
            if (tab == TAB_CAN) filter = i;
            else {
                category = i;
                query = "";
                picked = null;
            }
            page = 0;
        }
        else if (hit == HIT_SEARCH) return true;
        else if (hit == HIT_PICK) pickMode = !pickMode;
        else if (hit == HIT_PICK_CLEAR) {
            picked = null;
            page = 0;
        }
        else if (hit == HIT_BACK) detail = null;
        else if (hit >= HIT_CARD && hit < HIT_CARD + PER_PAGE) {
            List<D2RezeptKatalog.Eintrag> l = list();
            int i = page * PER_PAGE + hit - HIT_CARD;
            if (i < l.size()) {
                detail = l.get(i);
                detailScroll = 0;
            }
        }
        else if (hit == HIT_PAGE_PREV) turn(-1);
        else if (hit == HIT_PAGE_NEXT) turn(1);
        return false;
    }


    public void turn(int by) {
        if (detail != null) {
            detailScroll = Math.max(0, detailScroll + by);
            return;
        }
        page = Math.max(0, Math.min(pages() - 1, page + by));
    }




    public void padMove(int by) {
        if (!open || detail != null) {
            turn(by);
            return;
        }
        int n = list().size();
        if (n == 0) return;
        int f = focus < 0 ? page * PER_PAGE : Math.max(0, Math.min(n - 1, focus + by));
        focus = f;
        page = f / PER_PAGE;
    }


    public void padTab(int by) {
        if (!open) return;
        tab = (tab + by + 3) % 3;
        page = 0;
        detail = null;
        focus = -1;
    }


    public void padChip(int by) {
        if (!open || detail != null) return;
        if (tab == TAB_CAN) filter = (filter + by + 3) % 3;
        else if (tab == TAB_CUBE) {
            category = (category + by + D2RezeptKatalog.CUBE_CATEGORIES) % D2RezeptKatalog.CUBE_CATEGORIES;
            query = "";
            picked = null;
        }
        else return;
        page = 0;
        focus = -1;
    }


    public void padPress() {
        if (!open || focus < 0 || detail != null) return;
        List<D2RezeptKatalog.Eintrag> l = list();
        if (focus < l.size()) {
            detail = l.get(focus);
            detailScroll = 0;
        }
    }

    public int focus() {
        return focus;
    }




    public String materialsLine(String code, String name) {
        int ready = 0, almost = 0;
        for (D2RezeptKatalog.Eintrag e : katalog.all()) {
            boolean in = false;
            for (D2RezeptKatalog.Zutat z : e.zutaten) in |= bestand.fitsMaterial(z, code);
            if (!in || !e.machbar || e.isRuneword() && e.ladder && !ladderOn) continue;
            int s = bestand.lage(e).status;
            if (s == D2RezeptBestand.READY) ready++;
            else if (s != D2RezeptBestand.NONE) almost++;
        }
        return texte.t(T_MATERIALS_LINK, name, ready, almost);
    }


    public String[] cubeStrip() {
        Map<String, Integer> cube = bestand.cube();
        if (cube.isEmpty()) return null;
        StringBuilder has = new StringBuilder();
        for (Map.Entry<String, Integer> c : cube.entrySet()) {
            if (has.length() > 0) has.append(", ");
            if (c.getValue() > 1) has.append(c.getValue()).append(' ');
            has.append(shortName(c.getKey()));
        }
        int[] missing = new int[1];
        D2RezeptKatalog.Eintrag e = bestand.cubeRecipe(katalog.all(), missing);
        String second = e == null ? texte.t(T_EMPTY) : missing[0] == 0 ? texte.t(T_CUBE_MAKES, e.name)
            : texte.t(T_CUBE_ALMOST, e.name, missing[0]);
        return new String[]{texte.t(T_CUBE_HAS, has.toString()), second};
    }


    private String shortName(String code) {
        for (D2RezeptKatalog.Eintrag e : katalog.all()) if (code.equals(e.resultCode)) return D2RezeptKatalog.shortRune(e.name);
        for (D2RezeptKatalog.Eintrag e : katalog.all()) for (D2RezeptKatalog.Zutat z : e.zutaten)
            if (z.codes != null && z.codes.length == 1 && z.codes[0].equals(code)) return D2RezeptKatalog.shortRune(z.label);
        return code;
    }









    private java.util.Map<String, Integer> pictureNumbers;
    static final int PIC_FIRST = 96, PIC_LAST = 255;

    private void numberPictures(D2MaterialsLayer.Art art) {
        java.util.Map<String, Integer> m = new java.util.HashMap<>();
        for (int k = 0; k < D2Materials.KINDS; k++) {
            String f = art.materialFile(k);
            if (f != null) m.put(f, D2MaterialsLayer.PIC_MATERIAL + k);
        }
        java.util.TreeSet<String> rest = new java.util.TreeSet<>();
        rest.add("invbox");
        for (D2RezeptKatalog.Eintrag e : katalog.all()) {
            if (e.resultPicture != null) rest.add(e.resultPicture);
            for (D2RezeptKatalog.Zutat z : e.zutaten) if (z.picture != null) rest.add(z.picture);
        }
        int next = PIC_FIRST;
        for (String f : rest) if (!m.containsKey(f) && next <= PIC_LAST) m.put(f, next++);
        pictureNumbers = m;
    }

    private int pictureNo(String file) {
        Integer n = pictureNumbers != null ? pictureNumbers.get(file) : null;
        return n != null ? n : -1;
    }





    public D2Layer layer(D2MaterialsLayer.Art art, int ox, int oy, boolean cubeOpen, boolean pad) {
        D2Layer l = new D2Layer();
        if (art == null) return l;
        if (pictureNumbers == null) numberPictures(art);
        if (open) window(l, art, ox, oy, pad, cubeOpen);
        if (cubeOpen) {
            entry(l, art, ox, oy);
            if (!open) strip(l, art, ox, oy);
        }
        return l;
    }

    private void entry(D2Layer l, D2MaterialsLayer.Art art, int ox, int oy) {
        int x = ox + ENTRY_X, y = oy + ENTRY_Y, d = open ? 1 : 0;
        D2MaterialsLayer.stoneFrame(l, art, x + 1, y + 1, 32, 32);
        l.picture(D2MaterialsLayer.PIC_BLANK, D2MaterialsLayer.PATH_BLANK, d, 0, 0, 0, 0, x + 1, y + 1, 32, 32);
        item(l, "invbox", x + 6 + d, y + 6 + d, 22, false, 0);
        if (open) {
            int gold = art.colour(0xDCBE6E);
            l.outline(x - 1, y - 1, ENTRY_SIZE + 2, ENTRY_SIZE + 2, gold);
            l.outline(x, y, ENTRY_SIZE, ENTRY_SIZE, gold);
        }
    }

    private void strip(D2Layer l, D2MaterialsLayer.Art art, int ox, int oy) {
        String[] s = cubeStrip();
        if (s == null) return;
        int x = ox + STRIP_X, y = oy + STRIP_Y;
        l.rect(x, y, STRIP_W, STRIP_H, 0, D2Layer.ALPHA_75);
        l.outline(x, y, STRIP_W, STRIP_H, art.colour(0x343440));
        l.text(x + STRIP_W / 2, y + 13, D2Layer.FONT8, D2Layer.WHITE, D2Layer.CENTRE, fit(art, s[0], STRIP_W - 6));
        l.text(x + STRIP_W / 2, y + 26, D2Layer.FONT8, D2Layer.GOLD, D2Layer.CENTRE, fit(art, s[1], STRIP_W - 6));
    }

    private void window(D2Layer l, D2MaterialsLayer.Art art, int ox, int oy, boolean pad, boolean cubeOpen) {
        int x0 = ox + WX, y0 = oy + WY;

        if (cubeOpen) {
            stone(l, ox + RIM_X, oy + RIM_Y, RIM_W, ROW_Y - RIM_Y);
            stone(l, ox + RIM_X, oy + ROW_Y, CORNER_X - RIM_X, RIM_Y + RIM_H - ROW_Y);
        }
        else {
            stone(l, ox + RIM_X, oy + RIM_Y, RIM_W, RIM_H);
            l.outline(ox + RIM_X - 1, oy + RIM_Y - 1, RIM_W + 2, RIM_H + 2, art.colour(0xC7B377));
        }
        l.text(x0 + WW / 2, y0 + 20, D2Layer.FONT16, D2Layer.GOLD, D2Layer.CENTRE, upper(texte.t(T_TITLE)));
        tabs(l, art, ox, oy);
        if (detail != null) {
            detail(l, art, ox, oy);
            return;
        }
        if (tab == TAB_CAN) {
            int[] c = counts();
            chips(l, art, ox, oy, new String[]{upper(texte.t(T_FILTER_ALL, c[0])), upper(texte.t(T_FILTER_READY, c[1])),
                upper(texte.t(T_FILTER_ALMOST, c[2]))}, new int[]{D2Layer.GOLD, D2RezeptKatalog.C_GREEN, D2Layer.GOLD}, filter);
        }
        else if (tab == TAB_CUBE) {
            String[] names = new String[D2RezeptKatalog.CUBE_CATEGORIES];
            int[] colours = new int[names.length];
            for (int i = 0; i < names.length; i++) {
                names[i] = texte.t(D2RezeptKatalog.T_CAT_RUNES + i);
                colours[i] = D2Layer.GOLD;
            }
            boolean narrowed = picked != null || !query.trim().isEmpty();
            chips(l, art, ox, oy, names, colours, narrowed ? -1 : category);
        }
        searchRow(l, art, ox, oy);
        List<D2RezeptKatalog.Eintrag> list = list();
        page = Math.min(page, pages() - 1);
        for (int i = 0; i < PER_PAGE; i++) {
            int k = page * PER_PAGE + i;
            if (k >= list.size()) break;
            card(l, art, ox, oy + CARD_Y + i * (CARD_H + CARD_GAP), list.get(k), pad && k == focus);
        }
        if (pages() > 1) scrollBar(l, art, ox, oy);
        String foot;
        if (pickMode) foot = texte.t(T_PICK_HINT);
        else if (tab == TAB_CAN && stockAge == -2) foot = texte.t(T_STALE_NONE);
        else if (tab == TAB_CAN && stockAge >= 0) foot = texte.t(T_STALE, stockAge);
        else if (list.isEmpty()) foot = texte.t(T_EMPTY);
        else foot = texte.t(T_PAGE, page + 1, pages()) + "  ·  " + texte.t(pad ? T_HINT_PAD : T_HINT_TOUCH);
        l.text(x0 + WW / 2, y0 + WH - 8, D2Layer.FONT8, pickMode ? D2Layer.GOLD : D2Layer.GREY, D2Layer.CENTRE,
            fit(art, foot, WW - 12));
    }


    private static void stone(D2Layer l, int x, int y, int w, int h) {
        for (int ty = 0; ty < h; ty += STONE_SH) {
            for (int tx = 0; tx < w; tx += STONE_SW) {
                int tw = Math.min(STONE_SW, w - tx), th = Math.min(STONE_SH, h - ty);
                l.picture(PIC_STONE, PATH_STONE, STONE_FRAME, STONE_SX, STONE_SY, tw, th, x + tx, y + ty, tw, th);
            }
        }
        l.rect(x, y, w, h, 0, D2Layer.ALPHA_75);
    }


    private void scrollBar(D2Layer l, D2MaterialsLayer.Art art, int ox, int oy) {
        int x = ox + WX + WW - MARGIN + 3, top = oy + CARD_Y + 4, h = PER_PAGE * (CARD_H + CARD_GAP) - 22;
        l.rect(x, top, BAR_W, h, art.colour(0x0A0806), D2Layer.OPAQUE);
        l.outline(x, top, BAR_W, h, art.colour(0x463C28));
        int n = pages(), thumb = Math.max(8, h / n), at = n > 1 ? (h - thumb) * page / (n - 1) : 0;
        l.rect(x + 1, top + at, BAR_W - 2, thumb, art.colour(0xC7B377), D2Layer.OPAQUE);
        chevron(l, art, x + BAR_W / 2, top + h + 4, page < n - 1);
    }


    private static void chevron(D2Layer l, D2MaterialsLayer.Art art, int cx, int y, boolean more) {
        int c = art.colour(more ? 0xC7B377 : 0x504A3C);
        for (int i = 0; i < 5; i++) l.rect(cx - 4 + i, y + i, 9 - 2 * i, 1, c, D2Layer.OPAQUE);
    }


    private int[] tabEdges() {
        return tabEdges(null);
    }

    private int[] tabEdges(D2MaterialsLayer.Art art) {
        int gap = 4;
        int[] w = new int[3];
        int sum = 0;
        for (int t = 0; t < 3; t++) sum += w[t] = art != null ? art.width(D2Layer.FONT8, tabLabel(t)) : tabLabel(t).length() * 7;
        int extra = (WW - 2 * MARGIN - 2 * gap - sum) / 3;
        int[] x = new int[4];
        x[0] = WX + MARGIN;
        for (int t = 0; t < 3; t++) x[t + 1] = x[t] + w[t] + extra + gap;
        lastTabEdges = x;
        return x;
    }

    private int[] lastTabEdges;

    private String tabLabel(int t) {
        return upper(texte.t(t == TAB_CAN ? T_TAB_CAN : t == TAB_CUBE ? T_TAB_CUBE : T_TAB_RUNEWORDS));
    }

    private void tabs(D2Layer l, D2MaterialsLayer.Art art, int ox, int oy) {
        int[] x = tabEdges(art);
        for (int t = 0; t < 3; t++) {
            int tx = ox + x[t], tw = x[t + 1] - x[t] - 4, ty = oy + TAB_Y;
            boolean on = t == tab;
            D2MaterialsLayer.box(l, art, tx, ty, tw, TAB_H, on ? 0x2E2616 : 0x100F0E);
            if (!on) l.rect(tx, ty, tw, TAB_H, 0, D2Layer.ALPHA_50);
            l.text(tx + tw / 2, ty + TAB_H / 2 + D2MaterialsLayer.DOWN_FONT8, D2Layer.FONT8, on ? D2Layer.GOLD : D2Layer.GREY,
                D2Layer.CENTRE, tabLabel(t));
        }
    }


    private int[][] chipBoxes() {
        return lastChips != null ? lastChips : new int[0][];
    }

    private int[][] lastChips;

    private void chips(D2Layer l, D2MaterialsLayer.Art art, int ox, int oy, String[] names, int[] colours, int on) {
        int gap = 2, total = 0;
        int[] w = new int[names.length];
        for (int i = 0; i < names.length; i++) total += w[i] = art.width(D2Layer.FONT8, names[i]) + 8;
        total += gap * (names.length - 1);
        int x = WX + (WW - total) / 2;
        int[][] boxes = new int[names.length][];
        for (int i = 0; i < names.length; i++) {
            boxes[i] = new int[]{x, w[i]};
            if (i == on) D2MaterialsLayer.box(l, art, ox + x, oy + CHIP_Y, w[i], CHIP_H, 0x2E2616);
            else l.outline(ox + x, oy + CHIP_Y, w[i], CHIP_H, art.colour(0x343440));
            l.text(ox + x + w[i] / 2, oy + CHIP_Y + CHIP_H / 2 + D2MaterialsLayer.DOWN_FONT8, D2Layer.FONT8,
                i == on ? colours[i] : D2Layer.GREY, D2Layer.CENTRE, names[i]);
            x += w[i] + gap;
        }
        lastChips = boxes;
    }

    private void searchRow(D2Layer l, D2MaterialsLayer.Art art, int ox, int oy) {
        int x = ox + WX + 10, y = oy + SEARCH_Y;
        l.rect(x, y, SEARCH_W, SEARCH_H, art.colour(0x16161E), D2Layer.OPAQUE);
        boolean active = picked != null || !query.isEmpty();
        l.outline(x, y, SEARCH_W, SEARCH_H, active ? art.colour(0xDCBE6E) : art.colour(0x343440));
        int ty = y + SEARCH_H / 2 + D2MaterialsLayer.DOWN_FONT8;
        if (picked != null) {
            String file = pictureOf(picked.code);
            if (file != null) item(l, file, x + 3, y + 1, 16, false, 0);
            l.text(x + 24, ty, D2Layer.FONT8, D2Layer.WHITE, D2Layer.LEFT, fit(art, nameOf(picked.code), SEARCH_W - 44));
            l.text(x + SEARCH_W - 6, ty, D2Layer.FONT8, D2Layer.GREY, D2Layer.RIGHT, "x");
        }
        else if (query.isEmpty()) l.text(x + 6, ty, D2Layer.FONT8, D2Layer.GREY, D2Layer.LEFT, texte.t(T_SEARCH_HINT));
        else l.text(x + 6, ty, D2Layer.FONT8, D2Layer.WHITE, D2Layer.LEFT, fit(art, query, SEARCH_W - 12));
        int bx = x + SEARCH_W + 6, bw = ox + WX + WW - 10 - bx;
        D2MaterialsLayer.box(l, art, bx, y, bw, SEARCH_H, pickMode ? 0x2E2616 : 0x100F0E);
        l.text(bx + bw / 2, ty, D2Layer.FONT8, pickMode ? D2Layer.GOLD : D2RezeptKatalog.C_TAN, D2Layer.CENTRE,
            fit(art, texte.t(T_PICK), bw - 4));
    }

    private void card(D2Layer l, D2MaterialsLayer.Art art, int ox, int y, D2RezeptKatalog.Eintrag e, boolean focused) {
        int x = ox + WX + MARGIN, w = WW - 2 * MARGIN;
        boolean ladderOff = e.isRuneword() && e.ladder && !ladderOn;
        l.rect(x, y, w, CARD_H, 0, D2Layer.ALPHA_75);
        int gold = art.colour(0xDCBE6E);
        l.outline(x, y, w, CARD_H, focused ? gold : art.colour(0x343440));
        if (focused) l.outline(x + 1, y + 1, w - 2, CARD_H - 2, gold);
        D2RezeptBestand.Lage lage = bestand.lage(e);
        String status = ladderOff ? null : status(e, lage);
        int statusColour = statusColour(lage.status);

        if (status != null) status = fit(art, status, w - 20 - Math.min(90, art.width(D2Layer.FONT8, e.name)));
        int statusW = status != null ? art.width(D2Layer.FONT8, status) : 0;
        String name = fit(art, e.name, w - 12 - statusW - 8);
        l.text(x + 6, y + 15, D2Layer.FONT8, ladderOff ? D2Layer.GREY : e.colour, D2Layer.LEFT, name);
        if (e.isRuneword() && e.ladder) {
            int nx = x + 6 + art.width(D2Layer.FONT8, name) + 6;
            String mark = texte.t(T_LADDER_MARK);
            if (nx + art.width(D2Layer.FONT8, mark) < x + w - statusW - 8) l.text(nx, y + 15, D2Layer.FONT8, D2Layer.GREY, D2Layer.LEFT, mark);
        }
        if (status != null) l.text(x + w - 6, y + 15, D2Layer.FONT8, statusColour, D2Layer.RIGHT, status);
        int ix = x + 6, iy = y + 19;
        if (ladderOff) {

            l.text(x + 6, y + CARD_H - 6, D2Layer.FONT8, D2Layer.GREY, D2Layer.LEFT, fit(art, texte.t(T_LADDER_OFF), w - 12));
            return;
        }
        for (int k = 0; k < e.zutaten.size(); k++) {
            D2RezeptKatalog.Zutat z = e.zutaten.get(k);
            if (ix + ICON + 30 > x + w) break;
            if (k > 0) {
                l.text(ix + 3, iy + 15, D2Layer.FONT8, D2Layer.GREY, D2Layer.CENTRE, "+");
                ix += 8;
            }
            boolean lacks = !ladderOff && lage.have[k] < z.count;
            boolean chained = lacks && lage.status == D2RezeptBestand.CHAIN && k == lage.missing;
            if (z.count > 1) {
                String c = z.count + "x";
                l.text(ix, iy + 15, D2Layer.FONT8, lacks ? (chained ? D2Layer.GOLD : D2RezeptKatalog.C_RED) : D2Layer.WHITE, D2Layer.LEFT, c);
                ix += art.width(D2Layer.FONT8, c) + 1;
            }
            if (z.picture != null) item(l, z.picture, ix, iy, ICON, lacks || ladderOff, lacks ? (chained ? gold : art.colour(0xC83030)) : 0);
            else l.text(ix + ICON / 2, iy + 15, D2Layer.FONT8, D2Layer.GREY, D2Layer.CENTRE, "?");
            ix += ICON + 2;
        }
        if (e.resultPicture != null && !e.isRuneword() && ix + ICON + 13 < x + w) {
            l.text(ix + 6, iy + 15, D2Layer.FONT8, D2Layer.GREY, D2Layer.CENTRE, "=");
            ix += 13;
            item(l, e.resultPicture, ix, iy, ICON, ladderOff, 0);
            ix += ICON;
        }
        String info = ladderOff ? texte.t(T_LADDER_OFF) : where(e, lage);
        if (info != null) {
            int room = x + w - 6 - (ix + 8);
            if (ladderOff) room = w - 12;

            if (room > 20 && art.width(D2Layer.FONT8, info) <= room) {
                l.text(x + w - 6, y + CARD_H - 6, D2Layer.FONT8, D2Layer.GREY, D2Layer.RIGHT, info);
            }
            else if (room > 20) {
                List<String> lines = wrap(art, info, D2Layer.FONT6, room);
                int n = Math.min(2, lines.size());
                for (int i = 0; i < n; i++) {
                    String t = i == 1 && lines.size() > 2 ? lines.get(1) + " " + lines.get(2) : lines.get(i);
                    int ly = y + CARD_H - 5 - (n - 1 - i) * 9;
                    l.text(x + w - 6, ly, D2Layer.FONT6, D2Layer.GREY, D2Layer.RIGHT, fitIn(art, D2Layer.FONT6, t, room));
                }
            }
        }
    }


    String status(D2RezeptKatalog.Eintrag e, D2RezeptBestand.Lage s) {
        switch (s.status) {
            case D2RezeptBestand.READY: return texte.t(T_READY);
            case D2RezeptBestand.STASH:

                if (s.inOtherPages > 0) return texte.t(s.shared ? T_ON_PAGE_SHARED : T_ON_PAGE, s.page);
                return texte.t(T_STASH, s.inMaterials);
            case D2RezeptBestand.CHAIN: {
                D2RezeptKatalog.Zutat z = e.zutaten.get(s.missing);
                return texte.t(T_CHAIN, D2RezeptKatalog.shortRune(z.label), s.chainCount, D2RezeptKatalog.shortRune(nameOf(s.chainFrom)));
            }
            case D2RezeptBestand.ALMOST: {
                D2RezeptKatalog.Zutat z = e.zutaten.get(s.missing);
                String what = z.isBase() ? z.label : D2RezeptKatalog.shortRune(z.label);
                return texte.t(T_MISSING, s.missingCount > 1 ? s.missingCount + " " + what : what);
            }
            default: {
                int n = 0;
                for (int k = 0; k < e.zutaten.size(); k++) n += Math.max(0, e.zutaten.get(k).count - s.have[k]);
                return texte.t(T_MISSING_N, n);
            }
        }
    }

    static int statusColour(int status) {
        switch (status) {
            case D2RezeptBestand.READY: return D2RezeptKatalog.C_GREEN;
            case D2RezeptBestand.STASH: case D2RezeptBestand.CHAIN: return D2Layer.GOLD;
            default: return D2RezeptKatalog.C_RED;
        }
    }


    String where(D2RezeptKatalog.Eintrag e, D2RezeptBestand.Lage s) {
        if (e.isRuneword()) return e.info;
        List<String> parts = new ArrayList<>();
        if (s.inCube > 0) parts.add(texte.t(T_WHERE_CUBE, s.inCube));
        if (s.inInventory > 0) parts.add(texte.t(T_WHERE_INV, s.inInventory));
        if (s.inStashPage > 0) parts.add(texte.t(T_WHERE_STASH, s.inStashPage));
        if (s.inMaterials > 0) parts.add(texte.t(T_WHERE_MAT, s.inMaterials));
        if (parts.isEmpty()) return e.info;
        return String.join(" · ", parts);
    }

    private void detail(D2Layer l, D2MaterialsLayer.Art art, int ox, int oy) {
        int x0 = ox + WX, y = oy + CHIP_Y;
        D2RezeptKatalog.Eintrag e = detail;
        l.text(x0 + 12, y + 12, D2Layer.FONT8, D2Layer.GREY, D2Layer.LEFT, "< " + texte.t(T_BACK));
        y += 20;

        int[] c = e.resultPicture != null ? host.crop(e.resultPicture) : null;
        if (c != null) {
            int bw = Math.min(60, c[4] + 4), bh = Math.min(48, c[5] + 4);
            l.rect(x0 + WW / 2 - bw / 2, y, bw, bh, art.colour(0x16161E), D2Layer.OPAQUE);
            l.outline(x0 + WW / 2 - bw / 2, y, bw, bh, art.colour(0x343440));
            item(l, e.resultPicture, x0 + WW / 2 - bw / 2 + 2, y + 2, Math.min(bw, bh) - 4, false, 0);
            y += bh + 4;
        }

        List<String[]> lines = new ArrayList<>();
        List<int[]> styles = new ArrayList<>();
        for (D2RezeptKatalog.Zeile z : e.preview) {
            for (String part : wrap(art, z.text, z.big ? D2Layer.FONT16 : D2Layer.FONT8, WW - 28)) {
                lines.add(new String[]{part});
                styles.add(new int[]{z.colour, z.big ? 1 : 0});
            }
        }
        if (e.isRuneword() && e.ladder && !ladderOn) {
            lines.add(new String[]{texte.t(T_LADDER_OFF)});
            styles.add(new int[]{D2Layer.GREY, 0});
        }
        int ingredientsH = 18 + Math.min(e.zutaten.size(), 7) * 17, bottom = oy + ROW_Y - 4;
        int room = bottom - ingredientsH - y - 8, used = 0, first = Math.min(detailScroll, Math.max(0, lines.size() - 1));
        int tipTop = y, n = 0;
        for (int i = first; i < lines.size(); i++) {
            int lh = styles.get(i)[1] == 1 ? 16 : 12;
            if (used + lh > room) break;
            used += lh;
            n++;
        }
        l.rect(x0 + 10, tipTop, WW - 20, used + 8, 0, D2Layer.ALPHA_75);
        l.outline(x0 + 10, tipTop, WW - 20, used + 8, art.colour(0x343440));
        int ly = tipTop + 4;
        for (int i = first; i < first + n; i++) {
            boolean big = styles.get(i)[1] == 1;
            ly += big ? 16 : 12;
            l.text(x0 + WW / 2, ly - 1, big ? D2Layer.FONT16 : D2Layer.FONT8, styles.get(i)[0], D2Layer.CENTRE, lines.get(i)[0]);
        }
        if (first + n < lines.size()) chevron(l, art, x0 + WW - 18, tipTop + used + 1, true);
        y = tipTop + used + 14;

        D2RezeptBestand.Lage s = bestand.lage(e);
        l.text(x0 + 12, y + 10, D2Layer.FONT8, D2RezeptKatalog.C_TAN, D2Layer.LEFT, texte.t(T_INGREDIENTS));
        y += 14;
        int rows = 0;
        while (rows < e.zutaten.size() && y + 17 * (rows + 1) <= bottom) rows++;
        if (rows > 0) l.rect(x0 + 10, y - 2, WW - 20, rows * 17 + 4, 0, D2Layer.ALPHA_75);
        for (int k = 0; k < e.zutaten.size() && y + 17 <= bottom; k++) {
            D2RezeptKatalog.Zutat z = e.zutaten.get(k);
            boolean lacks = s.have[k] < z.count;
            if (z.picture != null) item(l, z.picture, x0 + 14, y, 15, lacks, lacks ? art.colour(0xC83030) : 0);
            String label = (z.count > 1 ? z.count + "x " : "") + z.label;
            String mark = texte.t(lacks ? T_LACKS : T_HAVE);
            int markW = art.width(D2Layer.FONT8, mark);
            l.text(x0 + 36, y + 13, D2Layer.FONT8, lacks ? D2Layer.GREY : D2Layer.WHITE, D2Layer.LEFT,
                fit(art, label, WW - 36 - markW - 20));
            l.text(x0 + WW - 12, y + 13, D2Layer.FONT8, lacks ? D2Layer.GREY : D2RezeptKatalog.C_GOLD, D2Layer.RIGHT, mark);
            y += 17;
        }

        if (!e.isRuneword()) D2MaterialsLayer.pageStone(l, art, texte.t(T_INSERT), ox + INSERT_X, oy + INSERT_Y, insertPressed, D2Layer.STONE_TEXT);
    }


    private void item(D2Layer l, String file, int x, int y, int size, boolean dim, int frame) {
        int[] c = host.crop(file);
        int no = pictureNo(file);
        if (c == null || no < 0) return;
        float k = Math.min(1f, Math.min(size / (float) c[4], size / (float) c[5]));
        int w = Math.max(1, Math.round(c[4] * k)), h = Math.max(1, Math.round(c[5] * k));
        int px = x + (size - w) / 2, py = y + (size - h) / 2;
        l.picture(no, D2MaterialsLayer.PATH_ITEMS + file, 0, c[2], c[3], c[4], c[5], px, py, w, h);
        if (dim) l.rect(px, py, w, h, 0, D2Layer.ALPHA_75);
        if (frame != 0) l.outline(x - 1, y - 1, size + 2, size + 2, frame);
    }

    private String pictureOf(String code) {
        for (D2RezeptKatalog.Eintrag e : katalog.all()) {
            if (code.equals(e.resultCode)) return e.resultPicture;
            for (D2RezeptKatalog.Zutat z : e.zutaten) if (z.codes != null) for (String c : z.codes) if (c.equals(code)) return z.picture;
        }
        return null;
    }

    String nameOf(String code) {
        if (code == null) return "";
        for (D2RezeptKatalog.Eintrag e : katalog.all()) if (code.equals(e.resultCode)) return e.name;
        for (D2RezeptKatalog.Eintrag e : katalog.all()) for (D2RezeptKatalog.Zutat z : e.zutaten)
            if (z.codes != null && z.codes.length == 1 && z.codes[0].equals(code)) return z.label;
        return code;
    }

    static String upper(String s) {
        return s == null ? "" : s.toUpperCase(java.util.Locale.ROOT);
    }


    static String fit(D2MaterialsLayer.Art art, String s, int width) {
        return fitIn(art, D2Layer.FONT8, s, width);
    }


    static String fitIn(D2MaterialsLayer.Art art, int font, String s, int width) {
        if (s == null) return "";
        if (art.width(font, s) <= width) return s;
        String t = s;
        while (t.length() > 0 && art.width(font, t + "...") > width) t = t.substring(0, t.length() - 1);
        return t.isEmpty() ? "" : t.trim() + "...";
    }


    static List<String> wrap(D2MaterialsLayer.Art art, String s, int font, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (art.width(font, next) <= width || line.length() == 0) {
                line.setLength(0);
                line.append(next);
            }
            else {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
}
