package com.winlator.d2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;






public final class D2Sprites implements AutoCloseable {

    public static final class Frame {
        public final int width, height, offsetX, offsetY;
        public final int[] argb;

        Frame(int width, int height, int offsetX, int offsetY, int[] argb) {
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.argb = argb;
        }
    }






    static final int[] GAMMA = new int[256];

    static {
        for (int i = 0; i < 256; i++) GAMMA[i] = Math.min(255, Math.round(12 + 243 * (float) Math.pow(i / 255.0, 1 / 1.2)));
        GAMMA[0] = 0;
    }





    private static final String[] ARCHIVES = {"Patch_D2.mpq", "d2exp.mpq", "d2data.mpq", "d2char.mpq"};
    private final List<D2Mpq> archives = new ArrayList<>();
    private int[] palette;

    public D2Sprites(File gameDir) throws IOException {
        for (String name : ARCHIVES) {
            File f = findIgnoringCase(gameDir, name);
            if (f == null) continue;
            try {
                archives.add(new D2Mpq(f));
            }
            catch (IOException e) {

                if (!name.equals("d2char.mpq")) throw e;
            }
        }
        if (archives.isEmpty()) throw new IOException("no archives in " + gameDir);
        byte[] pal = read("data\\global\\palette\\ACT1\\pal.dat");
        if (pal == null || pal.length < 768) throw new IOException("no palette");
        palette = new int[256];
        for (int i = 0; i < 256; i++) {
            palette[i] = 0xFF000000 | GAMMA[pal[i * 3 + 2] & 0xFF] << 16 | GAMMA[pal[i * 3 + 1] & 0xFF] << 8 | GAMMA[pal[i * 3] & 0xFF];
        }
    }

    public byte[] read(String name) throws IOException {
        for (D2Mpq mpq : archives) {
            byte[] data = mpq.read(name);
            if (data != null) return data;
        }
        return null;
    }


    public Frame[] dc6(String name) throws IOException {
        byte[] d = read(name);
        if (d == null) return null;
        return decode(d, palette);
    }





    public Frame[] dc6(String name, String paletteName) throws IOException {
        byte[] d = read(name);
        int[] pal = palette(paletteName);
        return d == null || pal == null ? null : decode(d, pal);
    }

    private final java.util.Map<String, int[]> palettes = new java.util.HashMap<>();


    int[] palette(String name) throws IOException {
        int[] cached = palettes.get(name);
        if (cached != null) return cached;
        byte[] pal = read("data\\global\\palette\\" + name + "\\pal.dat");
        if (pal == null || pal.length < 768) return null;
        int[] out = new int[256];
        for (int i = 0; i < 256; i++) {
            out[i] = 0xFF000000 | GAMMA[pal[i * 3 + 2] & 0xFF] << 16 | GAMMA[pal[i * 3 + 1] & 0xFF] << 8 | GAMMA[pal[i * 3] & 0xFF];
        }
        palettes.put(name, out);
        return out;
    }


    public D2Dcc.Dcc dcc(String name) throws IOException {
        byte[] d = read(name);
        return d == null ? null : D2Dcc.decode(d);
    }


    public D2Cof cof(String name) throws IOException {
        byte[] d = read(name);
        return d == null ? null : D2Cof.decode(d);
    }


    public int[] palette() {
        return palette.clone();
    }

    static Frame[] decode(byte[] d, int[] palette) throws IOException {
        if (d.length < 24 || s32(d, 0) != 6) throw new IOException("not a DC6");
        int directions = s32(d, 16), perDirection = s32(d, 20);
        int count = directions * perDirection;
        Frame[] frames = new Frame[count];
        for (int f = 0; f < count; f++) {
            int p = s32(d, 24 + f * 4);
            int flip = s32(d, p), w = s32(d, p + 4), h = s32(d, p + 8), ox = s32(d, p + 12), oy = s32(d, p + 16), length = s32(d, p + 28);
            int[] px = new int[Math.max(1, w * h)];
            int x = 0, y = flip != 0 ? 0 : h - 1;
            int i = p + 32, end = Math.min(d.length, i + length);
            while (i < end && y >= 0 && y < h) {
                int b = d[i++] & 0xFF;
                if (b == 0x80) {
                    x = 0;
                    y += flip != 0 ? 1 : -1;
                }
                else if ((b & 0x80) != 0) x += b & 0x7F;
                else {
                    for (int k = 0; k < b && i < end; k++, x++, i++) {
                        if (x < w) px[y * w + x] = palette[d[i] & 0xFF];
                    }
                }
            }
            frames[f] = new Frame(Math.max(1, w), Math.max(1, h), ox, oy, px);
        }
        return frames;
    }

    @Override
    public void close() {
        for (D2Mpq mpq : archives) {
            try {
                mpq.close();
            }
            catch (IOException ignored) {}
        }
        archives.clear();
    }

    private static File findIgnoringCase(File dir, String name) {
        File exact = new File(dir, name);
        if (exact.isFile()) return exact;
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) if (f.getName().equalsIgnoreCase(name)) return f;
        return null;
    }

    private static int s32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
