package com.winlator.d2;

import java.util.ArrayList;
import java.util.List;










public final class D2ItemLabel {
    private D2ItemLabel() {}

    public static final int VARIANT_CUT = 0, VARIANT_AFFIXES = 1, VARIANT_TWO_LINES = 2;

    public static final int VARIANT = VARIANT_CUT;





    public static final int WHITE = 0xFFFFFF, GREY = 0x696969, BLUE = 0x6969FF, YELLOW = 0xFFFF64, GREEN = 0x00FF00,
        GOLD = 0xC7B377, ORANGE = 0xFFA800;





    static final boolean OWN_NAME_LAST = true;





    public static boolean drawable(String text) {
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) > 0xFF) return false;
        return true;
    }


    public interface Width {
        int of(String text);
    }


    public static int colour(int quality, int category, boolean ethereal, boolean socketed, boolean runeword) {
        if (category == D2ModItems.CAT_RUNE) return ORANGE;
        if (runeword) return GOLD;
        switch (quality) {
            case D2ModItems.QUALITY_UNIQUE: return GOLD;
            case D2ModItems.QUALITY_SET: return GREEN;
            case D2ModItems.QUALITY_RARE: return YELLOW;
            case D2ModItems.QUALITY_MAGIC: return BLUE;
            case D2ModItems.QUALITY_CRAFTED: return ORANGE;
            default: return ethereal || socketed ? GREY : WHITE;
        }
    }






    public static String text(String name, int quality, boolean runeword, String baseName, Width width, int budget, int variant) {
        List<String> lines = new ArrayList<>();
        for (String line : name.split("\n")) if (!line.trim().isEmpty()) lines.add(line.trim());
        if (lines.isEmpty()) return "";
        String own, other = null;
        boolean named = lines.size() > 1 && (runeword || quality == D2ModItems.QUALITY_UNIQUE || quality == D2ModItems.QUALITY_SET
            || quality == D2ModItems.QUALITY_RARE || quality == D2ModItems.QUALITY_CRAFTED);
        if (named) {
            own = OWN_NAME_LAST ? lines.get(lines.size() - 1) : lines.get(0);
            other = OWN_NAME_LAST ? lines.get(lines.size() - 2) : lines.get(1);
        }
        else own = String.join(" ", lines);
        String base = plainBase(baseName);
        int at = base != null && !named ? own.indexOf(base) : -1;
        switch (variant) {
            case VARIANT_CUT:
                return cut(own, width, budget);
            case VARIANT_TWO_LINES: {
                if (other != null) return cut(own, width, budget) + "\n" + cut(other, width, budget);
                if (fits(own, width, budget) || at < 0) return cut(own, width, budget);
                String first = own.substring(0, at + base.length()).trim(), rest = own.substring(at + base.length()).trim();
                if (!fits(first, width, budget)) first = base;
                return cut(first, width, budget) + (rest.isEmpty() ? "" : "\n" + cut(rest, width, budget));
            }
            default: {
                if (fits(own, width, budget)) return own;
                if (at < 0) return cut(own, width, budget);
                String fromBase = own.substring(at).trim();
                if (fits(fromBase, width, budget)) return fromBase;
                return cut(base, width, budget);
            }
        }
    }






    public static final class Cache {
        private static final int MAX = 64;
        private final java.util.LinkedHashMap<String, String> texts = new java.util.LinkedHashMap<String, String>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, String> eldest) {
                return size() > MAX;
            }
        };

        public String text(String name, int quality, boolean runeword, String baseName, Width width, int budget, int variant) {
            String key = name + '\u0001' + quality + '\u0001' + runeword + '\u0001' + baseName + '\u0001' + budget + '\u0001' + variant;
            String text = texts.get(key);
            if (text == null) {
                text = D2ItemLabel.text(name, quality, runeword, baseName, width, budget, variant);
                texts.put(key, text);
            }
            return text;
        }

        public int size() {
            return texts.size();
        }
    }


    public static final float TAB_MAX_BUTTONS = 2.0f;







    public static boolean tabGrowsRight(float cx, float r, float screenW, float margin, float fullWidth) {
        boolean outRight = cx >= screenW / 2;
        float out = tabMaxWidth(cx, r, screenW, margin, outRight), in = tabMaxWidth(cx, r, screenW, margin, !outRight);
        if (fullWidth <= out) return outRight;
        if (fullWidth <= in) return !outRight;
        return out >= in ? outRight : !outRight;
    }


    public static float tabMaxWidth(float cx, float r, float screenW, float margin, boolean right) {
        float room = right ? screenW - margin - (cx - r) : cx + r - margin;
        return Math.min(TAB_MAX_BUTTONS * 2 * r, Math.max(room, 2 * r));
    }


    static String plainBase(String baseName) {
        if (baseName == null) return null;
        String s = baseName;

        while (s.startsWith("[") && s.length() >= 4 && s.charAt(3) == ']') s = s.substring(4);
        int next = s.indexOf('[');
        if (next > 0) s = s.substring(0, next);
        s = s.trim();
        return s.isEmpty() ? null : s;
    }

    static boolean fits(String text, Width width, int budget) {
        return width.of(text) <= budget;
    }


    static final String CUT_MARK = "...";


    static String cut(String text, Width width, int budget) {
        if (fits(text, width, budget)) return text;
        String t = text;
        while (t.length() > 1 && !fits(t.trim() + CUT_MARK, width, budget)) t = t.substring(0, t.length() - 1);
        return t.trim() + CUT_MARK;
    }
}
