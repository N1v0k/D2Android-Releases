package com.winlator.d2;

import java.io.IOException;







final class D2GameFont {

    static final class Img {
        final int width, height;
        final int[] argb;

        Img(int width, int height, int[] argb) {
            this.width = width;
            this.height = height;
            this.argb = argb;
        }
    }

    private final D2Sprites.Frame[] glyphs;
    private final int[] widths;
    final int height;

    private D2GameFont(D2Sprites.Frame[] glyphs, int[] widths) {
        this.glyphs = glyphs;
        this.widths = widths;
        int h = 0;
        for (D2Sprites.Frame g : glyphs) if (g != null) h = Math.max(h, g.height);
        height = h;
    }


    static D2GameFont load(D2Sprites sprites, String name) throws IOException {
        D2Sprites.Frame[] glyphs = sprites.dc6("data\\local\\font\\latin\\" + name + ".dc6");
        if (glyphs == null || glyphs.length < 256) return null;
        byte[] tbl = sprites.read("data\\local\\font\\latin\\" + name + ".tbl");
        int[] widths = new int[256];
        for (int i = 0; i < 256; i++) {
            widths[i] = tbl != null && tbl.length >= 12 + 14 * 256 ? tbl[12 + i * 14 + 3] & 0xFF : glyphs[i].width;
        }
        return new D2GameFont(glyphs, widths);
    }


    int width(String text) {
        return D2FontText.width(widths, text);
    }






    static final int STONE_RGB = 0x696969;





    Img render(String text, int rgb) {
        return render(text, rgb, 100);
    }





    Img render(String text, int rgb, int gainPercent) {
        text = D2FontText.map(text);
        int w = Math.max(1, width(text)), h = Math.max(1, height);
        int[] out = new int[w * h];
        int cr = rgb >> 16 & 0xFF, cg = rgb >> 8 & 0xFF, cb = rgb & 0xFF;
        int x = 0;
        for (int i = 0; i < text.length(); i++) {
            int c = text.charAt(i);
            D2Sprites.Frame g = glyphs[c];
            for (int gy = 0; g != null && gy < g.height && gy < h; gy++) {
                for (int gx = 0; gx < g.width && x + gx < w; gx++) {
                    int p = g.argb[gy * g.width + gx];
                    if (p == 0 || x + gx < 0) continue;
                    int lum = Math.max(p >> 16 & 0xFF, Math.max(p >> 8 & 0xFF, p & 0xFF));
                    int lit = Math.min(255, lum * gainPercent / 100);
                    int r = cr * lit / 255, gg = cg * lit / 255, b = cb * lit / 255;
                    out[(gy) * w + x + gx] = 0xFF000000 | r << 16 | gg << 8 | b;
                }
            }
            x += widths[c];
        }
        return new Img(w, h, out);
    }


    static int[] inkRows(Img img) {
        int first = -1, last = -1;
        for (int y = 0; y < img.height; y++) {
            for (int x = 0; x < img.width; x++) {
                int p = img.argb[y * img.width + x];
                if ((p >>> 24) == 0 || Math.max(p >> 16 & 0xFF, Math.max(p >> 8 & 0xFF, p & 0xFF)) < 48) continue;
                if (first < 0) first = y;
                last = y;
                break;
            }
        }
        return first < 0 ? new int[]{0, img.height} : new int[]{first, last + 1};
    }


    java.util.List<String> wrap(String text, float maxWidth) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String paragraph : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String next = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && width(next) > maxWidth) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                }
                else line = new StringBuilder(next);
            }
            out.add(line.toString());
        }
        return out;
    }


    static D2GameFont of(D2Sprites.Frame[] glyphs, int[] widths) {
        return new D2GameFont(glyphs, widths);
    }
}
