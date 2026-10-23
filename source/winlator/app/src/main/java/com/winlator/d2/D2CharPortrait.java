package com.winlator.d2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;




















public final class D2CharPortrait {
    private D2CharPortrait() {}


    public static final String[] CLASSES = {"AM", "SO", "NE", "PA", "BA", "DZ", "AI"};

    static final int HD = 0, TR = 1, LG = 2, RA = 3, LA = 4, RH = 5, LH = 6, SH = 7, S1 = 8, S2 = 9;

    static final int PLACE_HEAD = 1, PLACE_BODY = 3, PLACE_RIGHT = 4, PLACE_LEFT = 5;

    static final String[] ARMOUR = {"LIT", "LIT", "MED", "HVY"};

    public static final String MODE_LIST = "TN";

    public static final int DIRECTION_LIST = 0;





    static final int[] DCC_TO_COF_16 = {2, 6, 10, 14, 0, 4, 8, 12, 1, 3, 5, 7, 9, 11, 13, 15}, DCC_TO_COF_8 = {1, 3, 5, 7, 0, 2, 4, 6};





    public static int cofDirection(int direction, int count) {
        int d = Math.floorMod(direction, 16);
        if (count >= 16) return d;
        if (count == 8) return d / 2;
        return 0;
    }


    public static int dccDirection(int direction, int count) {
        int[] table = count >= 16 ? DCC_TO_COF_16 : count == 8 ? DCC_TO_COF_8 : null;
        if (table == null) return 0;
        int c = cofDirection(direction, table.length);
        for (int i = 0; i < table.length; i++) if (table[i] == c) return i;
        return 0;
    }





    public static int[] layerOrder(D2Cof cof, int direction, int frame) {
        return cof.priority[cofDirection(direction, cof.directions)][Math.min(frame, cof.framesPerDirection - 1)].clone();
    }


    public static final class Look {
        public final int heroClass;
        public final String name;

        public final int[] graphic = new int[16], colour = new int[16];

        public String head = "", body = "", right = "", left = "";

        Look(int heroClass, String name) {
            this.heroClass = heroClass;
            this.name = name;
        }
    }








    public static Look read(byte[] d) {
        if (d == null || d.length < 768 || u32(d, 0) != 0xAA55AA55 || u32(d, 4) != 96) return null;
        int cls = d[40] & 0xFF;
        if (cls >= CLASSES.length) return null;
        int end = 20;
        while (end < 36 && d[end] != 0) end++;
        Look look = new Look(cls, new String(d, 20, end - 20, StandardCharsets.ISO_8859_1));
        for (int i = 0; i < 16; i++) {
            look.graphic[i] = d[136 + i] & 0xFF;
            look.colour[i] = d[152 + i] & 0xFF;
        }
        int list = -1;
        for (int i = 765; i + 36 <= d.length; i++) {
            if (d[i] == 'i' && d[i + 1] == 'f' && d[i + 32] == 'J' && d[i + 33] == 'M' && skillsPlausible(d, i + 2)) {
                list = i + 32;
                break;
            }
        }
        if (list < 0) return look;
        int count = (d[list + 2] & 0xFF) | (d[list + 3] & 0xFF) << 8, top = 0;
        int version = -1;
        for (int i = list + 4; i + 14 <= d.length && top <= count; i++) {
            if (d[i] != 'J' || d[i + 1] != 'M') continue;
            long s = i * 8L;
            int v = bits(d, s + 48, 10);
            if (version < 0) version = v;
            if (v != version) continue;
            int place = bits(d, s + 58, 3), body = bits(d, s + 61, 4);
            if (place == 6) continue;
            if (++top > count) break;
            String code = code(d, s + 76);
            if (place != 1 || code == null) continue;
            if (body == PLACE_HEAD) look.head = code;
            else if (body == PLACE_BODY) look.body = code;
            else if (body == PLACE_RIGHT) look.right = code;
            else if (body == PLACE_LEFT) look.left = code;
            i += 13;
        }
        return look;
    }


    private static boolean skillsPlausible(byte[] d, int at) {
        for (int k = 0; k < 30; k++) if ((d[at + k] & 0xFF) > 99) return false;
        return true;
    }

    private static String code(byte[] d, long s) {
        if (s + 32 > d.length * 8L) return null;
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < 4; k++) {
            int c = bits(d, s + 8 * k, 8);
            if (c == ' ') break;
            if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'z')) return null;
            sb.append((char) c);
        }
        return sb.length() == 3 ? sb.toString() : null;
    }

    static int bits(byte[] d, long start, int n) {
        int v = 0;
        for (int i = 0; i < n; i++) {
            long b = start + i;
            if ((b >> 3) >= d.length) break;
            v |= ((d[(int) (b >> 3)] >> (b & 7)) & 1) << i;
        }
        return v;
    }

    private static int u32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }




    public static final class Item {
        public final String code, gfx, weaponClass, twoHandedClass;
        public final boolean weapon, shield, helm, oneOrTwoHanded;
        public final int transform;

        Item(String code, String gfx, String weaponClass, String twoHandedClass, boolean weapon, boolean shield, boolean helm,
             boolean oneOrTwoHanded, int transform) {
            this.code = code;
            this.gfx = gfx;
            this.weaponClass = weaponClass;
            this.twoHandedClass = twoHandedClass;
            this.weapon = weapon;
            this.shield = shield;
            this.helm = helm;
            this.oneOrTwoHanded = oneOrTwoHanded;
            this.transform = transform;
        }
    }


    public static Map<String, Item> items(D2Sprites sprites) throws IOException {
        return items(sprites.read("data\\global\\excel\\weapons.txt"), sprites.read("data\\global\\excel\\armor.txt"));
    }


    public static Map<String, Item> items(byte[] weaponsTxt, byte[] armorTxt) {
        Map<String, Item> out = new HashMap<>();
        if (weaponsTxt != null) table(out, weaponsTxt, true);
        if (armorTxt != null) table(out, armorTxt, false);
        return out;
    }

    private static void table(Map<String, Item> out, byte[] txt, boolean weapons) {
        String[] lines = new String(txt, StandardCharsets.ISO_8859_1).split("\r?\n");
        if (lines.length < 2) return;
        String[] head = lines[0].split("\t", -1);
        int code = col(head, "code"), gfx = Math.max(col(head, "alternategfx"), col(head, "alternateGfx")), wclass = col(head, "wclass"),
            two = col(head, "2handedwclass"), oneOrTwo = col(head, "1or2handed"), component = col(head, "component"), transform = col(head, "transform");
        for (int i = 1; i < lines.length; i++) {
            String[] c = lines[i].split("\t", -1);
            String k = get(c, code);
            if (k.isEmpty() || out.containsKey(k)) continue;
            String g = get(c, gfx);
            int comp = parse(get(c, component), -1);
            out.put(k, new Item(k, g.isEmpty() ? k : g, get(c, wclass), get(c, two), weapons, !weapons && comp == SH,
                !weapons && comp == HD, "1".equals(get(c, oneOrTwo)), parse(get(c, transform), 0)));
        }
    }

    private static int col(String[] head, String name) {
        for (int i = 0; i < head.length; i++) if (head[i].trim().equalsIgnoreCase(name)) return i;
        return -1;
    }

    private static String get(String[] c, int i) {
        return i >= 0 && i < c.length ? c[i].trim() : "";
    }

    private static int parse(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }




    public static final class Parts {
        public final String[] token = new String[16];
        public String weaponClass = "HTH";

        public final Item[] item = new Item[16];
    }


    public static Parts parts(Look look, Map<String, Item> items) {
        Parts p = new Parts();
        for (int c : new int[]{HD, TR, LG, RA, LA, S1, S2}) {
            int g = look.graphic[c];
            p.token[c] = g >= 1 && g <= 3 ? ARMOUR[g] : "LIT";
        }
        Item head = items.get(look.head), right = items.get(look.right), left = items.get(look.left);
        if (head != null && head.helm) {
            p.token[HD] = head.gfx.toUpperCase();
            p.item[HD] = head;
        }
        Item weaponRight = right != null && right.weapon ? right : null;
        Item weaponLeft = left != null && left.weapon ? left : null;
        Item shield = left != null && left.shield ? left : right != null && right.shield ? right : null;
        if (weaponRight == null && weaponLeft != null) {
            weaponRight = weaponLeft;
            weaponLeft = null;
        }
        if (weaponRight != null) {
            String wc = weaponRight.weaponClass.toLowerCase();
            boolean bow = wc.equals("bow") || wc.equals("xbw");
            p.token[bow ? LH : RH] = weaponRight.gfx.toUpperCase();
            p.item[bow ? LH : RH] = weaponRight;
            if (weaponLeft != null) {
                p.token[LH] = weaponLeft.gfx.toUpperCase();
                p.item[LH] = weaponLeft;
            }
            p.weaponClass = weaponClass(weaponRight, weaponLeft, shield != null).toUpperCase();
        }
        if (shield != null) {
            p.token[SH] = shield.gfx.toUpperCase();
            p.item[SH] = shield;
        }
        return p;
    }






    static String weaponClass(Item right, Item left, boolean shield) {
        String r = right.weaponClass.toLowerCase();
        if (left == null) return right.oneOrTwoHanded && !shield && !right.twoHandedClass.isEmpty() ? right.twoHandedClass : r;
        String l = left.weaponClass.toLowerCase();
        if (r.equals("ht1") && l.equals("ht1")) return "ht2";
        return "1" + (l.equals("1ht") ? "j" : "s") + (r.equals("1ht") ? "t" : "s");
    }




    public static final class Anim {
        public final int width, height, anchorX, anchorY, frameMs;

        public final int[][] frames;

        Anim(int width, int height, int anchorX, int anchorY, int frameMs, int[][] frames) {
            this.width = width;
            this.height = height;
            this.anchorX = anchorX;
            this.anchorY = anchorY;
            this.frameMs = frameMs;
            this.frames = frames;
        }
    }


    public interface Files {
        D2Cof cof(String name) throws IOException;

        D2Dcc.Dcc dcc(String name) throws IOException;

        byte[] read(String name) throws IOException;

        int[] palette();
    }


    public static Files of(D2Sprites sprites) {
        return new Files() {
            @Override public D2Cof cof(String name) throws IOException { return sprites.cof(name); }
            @Override public D2Dcc.Dcc dcc(String name) throws IOException { return sprites.dcc(name); }
            @Override public byte[] read(String name) throws IOException { return sprites.read(name); }
            @Override public int[] palette() { return sprites.palette(); }
        };
    }


    static final String[] COLOUR_FILES = {"grey", "grey2", "gold", "brown", "greybrown", "invgrey", "invgrey2", "invgreybrown"};





    public static Anim compose(byte[] save, Files files, Map<String, Item> items) throws IOException {
        Look look = read(save);
        return look == null ? null : compose(look, files, items, MODE_LIST, DIRECTION_LIST);
    }


    public static Anim compose(Look look, Files files, Map<String, Item> items, String mode, int direction) throws IOException {
        String cl = CLASSES[look.heroClass];
        Parts parts = parts(look, items);
        String root = "data\\global\\chars\\" + cl + "\\";
        D2Cof cof = files.cof(root + "COF\\" + cl + mode + parts.weaponClass + ".cof");
        if (cof == null && !parts.weaponClass.equals("HTH")) {
            parts.weaponClass = "HTH";
            cof = files.cof(root + "COF\\" + cl + mode + "HTH.cof");
        }
        if (cof == null) return null;
        int dir = direction;
        int[] palette = files.palette();

        D2Dcc.Dcc[] dcc = new D2Dcc.Dcc[16];
        byte[][] maps = new byte[16][];
        D2Cof.Layer[] layerOf = new D2Cof.Layer[16];
        for (D2Cof.Layer layer : cof.layers) {
            int c = layer.component;
            if (c < 0 || c >= 16 || parts.token[c] == null && c != S1 && c != S2 && c < 8) continue;
            String token = parts.token[c] != null ? parts.token[c] : "LIT";
            String part = D2Cof.COMPONENTS[c];
            D2Dcc.Dcc d = files.dcc(root + part + "\\" + cl + part + token + mode + layer.weaponClass + ".dcc");
            if (d == null && !token.equals("LIT") && c != RH && c != LH && c != SH) {
                d = files.dcc(root + part + "\\" + cl + part + "LIT" + mode + layer.weaponClass + ".dcc");
            }
            if (d == null) continue;
            dcc[c] = d;
            layerOf[c] = layer;

            maps[c] = colourMap(files, parts.item[c] != null ? parts.item[c] : c == HD ? null : items.get(look.body), look.colour[c]);
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        for (int c = 0; c < 16; c++) {
            if (dcc[c] == null) continue;
            int d = dccDirection(dir, dcc[c].directions);
            for (D2Dcc.Frame f : dcc[c].frames[d]) {
                if (f == null || f.width <= 0) continue;
                minX = Math.min(minX, f.xMin);
                minY = Math.min(minY, f.yMin);
                maxX = Math.max(maxX, f.xMin + f.width);
                maxY = Math.max(maxY, f.yMin + f.height);
            }
        }
        if (minX == Integer.MAX_VALUE) return null;
        int w = maxX - minX, h = maxY - minY;
        int[][] frames = new int[cof.framesPerDirection][];
        for (int f = 0; f < cof.framesPerDirection; f++) {
            int[] out = new int[w * h];

            for (int c : layerOrder(cof, dir, f)) {
                if (c >= 16 || dcc[c] == null) continue;
                D2Dcc.Dcc d = dcc[c];
                int dd = dccDirection(dir, d.directions);
                D2Dcc.Frame fr = d.frames[dd][Math.min(f, d.framesPerDirection - 1)];
                if (fr == null) continue;
                draw(out, w, h, fr, fr.xMin - minX, fr.yMin - minY, palette, maps[c], layerOf[c]);
            }
            frames[f] = out;
        }

        int rate = cof.animationRate > 0 ? cof.animationRate : 256;
        int frameMs = Math.max(20, Math.round(10240f / rate));
        return new Anim(w, h, -minX, -minY, frameMs, frames);
    }


    static byte[] colourMap(Files files, Item item, int colour) throws IOException {
        if (item == null || colour == 255 || item.transform < 1 || item.transform > COLOUR_FILES.length) return null;
        byte[] d = files.read("data\\global\\items\\palette\\" + COLOUR_FILES[item.transform - 1] + ".dat");
        if (d == null || (colour + 1) * 256 > d.length) return null;
        byte[] map = new byte[256];
        System.arraycopy(d, colour * 256, map, 0, 256);
        return map;
    }


    static void draw(int[] out, int w, int h, D2Dcc.Frame f, int x0, int y0, int[] palette, byte[] map, D2Cof.Layer layer) {
        boolean blend = layer != null && layer.transparent;
        for (int y = 0; y < f.height; y++) {
            int oy = y0 + y;
            if (oy < 0 || oy >= h) continue;
            for (int x = 0; x < f.width; x++) {
                int ox = x0 + x;
                if (ox < 0 || ox >= w) continue;
                int v = f.pixels[y * f.width + x] & 0xFF;
                if (v == 0) continue;
                int argb = palette[map != null ? map[v] & 0xFF : v];
                if (!blend) out[oy * w + ox] = argb;
                else out[oy * w + ox] = over(out[oy * w + ox], argb, alpha(argb, layer.drawEffect));
            }
        }
    }


    static int alpha(int argb, int effect) {
        switch (effect) {
            case 0: return 64;
            case 1: return 128;
            case 2: return 192;
            case 5: return 255;
            default: return Math.max((argb >> 16) & 0xFF, Math.max((argb >> 8) & 0xFF, argb & 0xFF));
        }
    }

    static int over(int below, int above, int a) {
        if (a >= 255) return above;
        int ba = below >>> 24;
        int r = (((above >> 16) & 0xFF) * a + ((below >> 16) & 0xFF) * (255 - a)) / 255;
        int g = (((above >> 8) & 0xFF) * a + ((below >> 8) & 0xFF) * (255 - a)) / 255;
        int b = ((above & 0xFF) * a + (below & 0xFF) * (255 - a)) / 255;
        int outA = Math.max(ba, a);
        return outA << 24 | r << 16 | g << 8 | b;
    }
}
