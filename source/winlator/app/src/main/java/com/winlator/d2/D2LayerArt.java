package com.winlator.d2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;






final class D2LayerArt implements D2InfoLayer.Art {
    private final int[] palette;
    private final int[][] crops = new int[D2Materials.KINDS][];
    private final String[] files = new String[D2Materials.KINDS];
    private final D2Frame frame;
    private final Map<Integer, int[]> widths = new HashMap<>();
    private final Map<Integer, Integer> colours = new HashMap<>();
    private final Function<String, String> strings;
    private final boolean barPieces;

    private final Map<String, Object[]> effectRefs = new HashMap<>();


    String effectPath(String name) {
        Object[] r = name != null ? effectRefs.get(name) : null;
        return r != null ? (String) r[0] : null;
    }


    int[] effectCrop(String name) {
        Object[] r = name != null ? effectRefs.get(name) : null;
        return r != null ? (int[]) r[1] : null;
    }
    private final int[][] arrowCrops;

    private D2LayerArt(D2Sprites s, Function<String, String> strings) throws IOException {
        this.strings = strings;
        palette = s.palette();
        String[] inv = D2Materials.invFiles(s.read("data\\global\\excel\\misc.bin"));
        for (int k = 0; inv != null && k < D2Materials.KINDS; k++) {
            D2Sprites.Frame[] f = inv[k] != null ? s.dc6("data\\global\\items\\" + inv[k] + ".dc6") : null;
            if (f == null || f.length == 0) continue;
            crops[k] = crop(f[0]);
            files[k] = inv[k];
        }
        frame = D2Frame.load(s);

        D2Sprites.Frame[] ctrl = s.dc6("data\\global\\ui\\PANEL\\800ctrlpnl7.dc6");
        barPieces = ctrl != null && ctrl.length >= 4 && ctrl[1].width >= 125 && ctrl[1].height >= 48 && ctrl[2].width >= 84
            && ctrl[2].height >= 20 && ctrl[3].width >= 124 && ctrl[3].height >= 15;

        String[][] effects = {{"vps", "data\\global\\items\\invvps"}, {"yps", "data\\global\\items\\invyps"},
            {"wms", "data\\global\\items\\invwms"}, {"hp", "data\\global\\items\\invhp3"}, {"mp", "data\\global\\items\\invmp3"}};
        for (String[] e : effects) {
            D2Sprites.Frame[] f = s.dc6(e[1] + ".dc6");
            if (f != null && f.length > 0) effectRefs.put(e[0], new Object[]{e[1], crop(f[0])});
        }
        D2Sprites.Frame[] level = s.dc6("data\\global\\ui\\PANEL\\level.dc6");
        if (level != null && level.length > 0) {
            effectRefs.put("level", new Object[]{"data\\global\\ui\\PANEL\\level", new int[]{level[0].width, level[0].height, 0, 0,
                level[0].width, level[0].height}});
        }
        D2Sprites.Frame[] arrow = s.dc6("data\\global\\ui\\PANEL\\menubutton.dc6");
        arrowCrops = new int[4][];
        for (int i = 0; arrow != null && i < Math.min(4, arrow.length); i++) arrowCrops[i] = crop(arrow[i]);
        widths.put(D2Layer.FONT8, widths(s, "font8"));
        widths.put(D2Layer.FONT16, widths(s, "font16"));
        widths.put(D2Layer.FONT6, widths(s, "font6"));
        widths.put(D2Layer.EXOCET10, widths(s, "fontexocet10"));
        widths.put(D2Layer.EXOCET8, widths(s, "fontexocet8"));

        int[] formal12 = widths(s, "fontformal12");
        if (formal12 != null) widths.put(D2Layer.FORMAL12, formal12);
    }


    static D2LayerArt load(D2Sprites s, Function<String, String> strings) {
        try {
            D2LayerArt a = new D2LayerArt(s, strings);
            for (int[] w : a.widths.values()) if (w == null) return null;
            return a;
        }
        catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static int[] widths(D2Sprites s, String font) throws IOException {
        byte[] tbl = s.read("data\\local\\font\\latin\\" + font + ".tbl");
        if (tbl == null || tbl.length < 12 + 14 * 256) return null;
        int[] w = new int[256];
        for (int i = 0; i < 256; i++) w[i] = tbl[12 + i * 14 + 3] & 0xFF;
        return w;
    }


    static int[] crop(D2Sprites.Frame f) {
        int minX = f.width, minY = f.height, maxX = -1, maxY = -1;
        for (int y = 0; y < f.height; y++) {
            for (int x = 0; x < f.width; x++) {
                if (f.argb[y * f.width + x] == 0) continue;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < 0) return new int[]{f.width, f.height, 0, 0, f.width, f.height};
        return new int[]{f.width, f.height, minX, minY, maxX - minX + 1, maxY - minY + 1};
    }

    @Override
    public int[] materialCrop(int kind) {
        return kind >= 0 && kind < crops.length ? crops[kind] : null;
    }

    @Override
    public String materialFile(int kind) {
        return kind >= 0 && kind < files.length ? files[kind] : null;
    }

    @Override
    public D2Frame frame() {
        return frame;
    }

    @Override
    public int colour(int rgb) {
        Integer known = colours.get(rgb);
        if (known != null) return known;
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255, best = 0;
        long bestD = Long.MAX_VALUE;
        for (int i = 0; i < palette.length; i++) {
            int c = palette[i];
            long dr = (c >> 16 & 255) - r, dg = (c >> 8 & 255) - g, db = (c & 255) - b;
            long d = 3 * dr * dr + 4 * dg * dg + 2 * db * db;
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        colours.put(rgb, best);
        return best;
    }

    @Override
    public int width(int font, String text) {
        int[] w = widths.get(font);
        return w == null || text == null ? 0 : D2FontText.width(w, text);
    }

    @Override
    public boolean barPieces() {
        return barPieces;
    }

    @Override
    public int[] arrowCrop(int index) {
        return index >= 0 && index < arrowCrops.length ? arrowCrops[index] : null;
    }

    @Override
    public int charWidth(int font, int c) {
        int[] w = widths.get(font);
        return w == null || c < 0 || c > 255 ? 0 : w[c];
    }

    @Override
    public int ink() {
        if (ink < 0) {
            int best = 1;
            for (int i = 1; i < palette.length; i++) {
                int c = palette[i], b = (c >> 16 & 255) + (c >> 8 & 255) + (c & 255);
                int bb = (palette[best] >> 16 & 255) + (palette[best] >> 8 & 255) + (palette[best] & 255);
                if (b < bb) best = i;
            }
            ink = best;
        }
        return ink;
    }

    private int ink = -1;

    @Override
    public String gameText(String code) {
        return strings != null ? strings.apply(code) : null;
    }
}
