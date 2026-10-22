package com.winlator.d2;

import java.io.IOException;

public final class D2Dcc {
    static final int[] WIDTHS = {0, 1, 2, 4, 6, 8, 10, 12, 14, 16, 20, 24, 26, 28, 30, 32};
    static final int SIGNATURE = 0x74;
    static final int MAX_BOX = 4096;

    public static final class Frame {
        public final int xMin, yMin, width, height;
        public final byte[] pixels;

        Frame(int xMin, int yMin, int width, int height, byte[] pixels) {
            this.xMin = xMin;
            this.yMin = yMin;
            this.width = width;
            this.height = height;
            this.pixels = pixels;
        }
    }

    public static final class Dcc {
        public final int directions, framesPerDirection;
        public final Frame[][] frames;

        Dcc(int directions, int framesPerDirection, Frame[][] frames) {
            this.directions = directions;
            this.framesPerDirection = framesPerDirection;
            this.frames = frames;
        }
    }

    private D2Dcc() {}

    static final class Bits {
        private final byte[] data;
        private long pos;
        private final long end;

        Bits(byte[] data, long startBit, long endBit) {
            this.data = data;
            this.pos = startBit;
            this.end = endBit;
        }

        int bits(int n) throws IOException {
            if (n == 0) return 0;
            if (pos + n > end) throw new IOException("DCC: a stream ended early");
            long v = 0;
            for (int i = 0; i < n; i++, pos++) v |= (long) ((data[(int) (pos >> 3)] >> (pos & 7)) & 1) << i;
            return (int) v;
        }

        int signed(int n) throws IOException {
            int v = bits(n);
            return n > 0 && n < 32 && (v & (1 << (n - 1))) != 0 ? v - (1 << n) : v;
        }

        long position() {
            return pos;
        }

        long end() {
            return end;
        }
    }

    public static Dcc decode(byte[] d) throws IOException {
        try {
            return decodeChecked(d);
        }
        catch (RuntimeException e) {
            throw new IOException("DCC: broken (" + e + ")", e);
        }
    }

    private static Dcc decodeChecked(byte[] d) throws IOException {
        if (d.length < 15 || (d[0] & 0xFF) != SIGNATURE) throw new IOException("not a DCC");
        int directions = d[2] & 0xFF;
        int perDirection = u32(d, 3);
        if (directions < 1 || directions > 32 || perDirection < 1 || perDirection > 256 || 15 + 4 * directions > d.length) {
            throw new IOException("DCC: header out of range");
        }
        Frame[][] frames = new Frame[directions][];
        for (int dir = 0; dir < directions; dir++) {
            int start = u32(d, 15 + 4 * dir);
            int end = dir + 1 < directions ? u32(d, 15 + 4 * (dir + 1)) : d.length;
            if (start < 0 || end > d.length || start >= end) throw new IOException("DCC: direction " + dir + " out of range");
            frames[dir] = direction(d, start, end, perDirection);
        }
        return new Dcc(directions, perDirection, frames);
    }

    private static final class Head {
        int width, height, xMin, yMin;
        int cellsX, cellsY;
        int[] cellW, cellH;
        int x0, y0;
    }

    private static final class Entry {
        final int[] value = new int[4];
        int frame, cell;
    }

    private static Frame[] direction(byte[] d, int start, int end, int count) throws IOException {
        Bits b = new Bits(d, (long) start * 8, (long) end * 8);
        b.bits(32);
        int flags = b.bits(2);
        int v0 = WIDTHS[b.bits(4)], wb = WIDTHS[b.bits(4)], hb = WIDTHS[b.bits(4)], xb = WIDTHS[b.bits(4)], yb = WIDTHS[b.bits(4)];
        int ob = WIDTHS[b.bits(4)], cb = WIDTHS[b.bits(4)];
        Head[] heads = new Head[count];
        long optional = 0;
        int xMin = Integer.MAX_VALUE, yMin = Integer.MAX_VALUE, xMax = Integer.MIN_VALUE, yMax = Integer.MIN_VALUE;
        for (int f = 0; f < count; f++) {
            Head h = new Head();
            b.bits(v0);
            h.width = b.bits(wb);
            h.height = b.bits(hb);
            int x = b.signed(xb), y = b.signed(yb);
            optional += b.bits(ob);
            b.bits(cb);
            boolean bottomUp = b.bits(1) == 1;
            if (h.width < 1 || h.height < 1 || h.width > 2048 || h.height > 2048) throw new IOException("DCC: frame size " + h.width + "x" + h.height);
            h.xMin = x;
            h.yMin = bottomUp ? y : y - h.height + 1;
            xMin = Math.min(xMin, h.xMin);
            yMin = Math.min(yMin, h.yMin);
            xMax = Math.max(xMax, h.xMin + h.width - 1);
            yMax = Math.max(yMax, h.yMin + h.height - 1);
            heads[f] = h;
        }
        if (optional > 0) {
            long p = ((b.position() + 7) & ~7L) + optional * 8;
            if (p > b.end()) throw new IOException("DCC: optional bytes past the direction");
            b = new Bits(d, p, b.end());
        }
        long equalSize = (flags & 2) != 0 ? b.bits(20) : 0;
        long maskSize = b.bits(20);
        long encodingSize = 0, rawSize = 0;
        if ((flags & 1) != 0) {
            encodingSize = b.bits(20);
            rawSize = b.bits(20);
        }
        int[] values = new int[256];
        int nValues = 0;
        for (int i = 0; i < 256; i++) if (b.bits(1) == 1) values[nValues++] = i;
        long p = b.position();
        Bits equal = new Bits(d, p, p + equalSize);
        p += equalSize;
        Bits mask = new Bits(d, p, p + maskSize);
        p += maskSize;
        Bits encoding = new Bits(d, p, p + encodingSize);
        p += encodingSize;
        Bits raw = new Bits(d, p, p + rawSize);
        p += rawSize;
        if (p > b.end()) throw new IOException("DCC: streams longer than the direction");
        Bits codes = new Bits(d, p, b.end());

        long boxW = (long) xMax - xMin + 1, boxH = (long) yMax - yMin + 1;
        if (boxW > MAX_BOX || boxH > MAX_BOX) throw new IOException("DCC: direction " + boxW + "x" + boxH);
        int width = (int) boxW, height = (int) boxH;
        int dirCellsX = 1 + (width - 1) / 4, dirCellsY = 1 + (height - 1) / 4;
        for (Head h : heads) layout(h, xMin, yMin);

        Entry[] cellBuffer = new Entry[dirCellsX * dirCellsY];
        java.util.ArrayList<Entry> buffer = new java.util.ArrayList<>();
        int[] read = new int[4];
        for (int f = 0; f < count; f++) {
            Head h = heads[f];
            int cy0 = h.y0 / 4, cx0 = h.x0 / 4;
            for (int cy = 0; cy < h.cellsY; cy++) {
                for (int cx = 0; cx < h.cellsX; cx++) {
                    int cell = (cy0 + cy) * dirCellsX + cx0 + cx;
                    Entry old = cellBuffer[cell];
                    boolean same = false;
                    int pixelMask = 0x0F;
                    if (old != null) {
                        if (equalSize > 0) same = equal.bits(1) == 1;
                        if (!same) pixelMask = mask.bits(4);
                    }
                    if (same) continue;
                    int nBits = Integer.bitCount(pixelMask);
                    boolean rawPixels = nBits > 0 && encodingSize > 0 && encoding.bits(1) == 1;
                    int last = 0, decoded = 0;
                    read[0] = read[1] = read[2] = read[3] = 0;
                    for (int i = 0; i < nBits; i++) {
                        int v;
                        if (rawPixels) v = raw.bits(8);
                        else {
                            v = last;
                            int step;
                            do {
                                step = codes.bits(4);
                                v += step;
                            } while (step == 15);
                        }
                        if (v == last) break;
                        read[i] = last = v;
                        decoded++;
                    }
                    Entry e = new Entry();
                    int next = decoded - 1;
                    for (int i = 0; i < 4; i++) {
                        if ((pixelMask & (1 << i)) != 0) e.value[i] = next >= 0 ? read[next--] : 0;
                        else e.value[i] = old != null ? old.value[i] : 0;
                    }
                    e.frame = f;
                    e.cell = cy * h.cellsX + cx;
                    cellBuffer[cell] = e;
                    buffer.add(e);
                }
            }
        }
        for (Entry e : buffer) {
            for (int i = 0; i < 4; i++) {
                if (e.value[i] >= nValues) throw new IOException("DCC: colour " + e.value[i] + " of " + nValues);
                e.value[i] = values[e.value[i]];
            }
        }
        check(equal, "equal cells");
        check(mask, "pixel mask");
        check(encoding, "encoding type");
        check(raw, "raw pixels");

        byte[] canvas = new byte[width * height];
        int[] lastX = new int[cellBuffer.length], lastY = new int[cellBuffer.length];
        int[] lastW = new int[cellBuffer.length], lastH = new int[cellBuffer.length];
        int next = 0;
        Frame[] out = new Frame[count];
        for (int f = 0; f < count; f++) {
            Head h = heads[f];
            int cy0 = h.y0 / 4, cx0 = h.x0 / 4;
            int y = h.y0;
            for (int cy = 0; cy < h.cellsY; cy++) {
                int x = h.x0, ch = h.cellH[cy];
                for (int cx = 0; cx < h.cellsX; cx++) {
                    int cw = h.cellW[cx];
                    int cell = (cy0 + cy) * dirCellsX + cx0 + cx;
                    Entry e = next < buffer.size() ? buffer.get(next) : null;
                    if (e != null && e.frame == f && e.cell == cy * h.cellsX + cx) {
                        next++;
                        if (e.value[0] == e.value[1]) fill(canvas, width, x, y, cw, ch, (byte) e.value[0]);
                        else {
                            int nb = e.value[1] == e.value[2] ? 1 : 2;
                            for (int yy = 0; yy < ch; yy++) {
                                for (int xx = 0; xx < cw; xx++) canvas[(y + yy) * width + x + xx] = (byte) e.value[codes.bits(nb)];
                            }
                        }
                    }
                    else if (cw != lastW[cell] || ch != lastH[cell]) fill(canvas, width, x, y, cw, ch, (byte) 0);
                    else copy(canvas, width, lastX[cell], lastY[cell], x, y, cw, ch);
                    lastX[cell] = x;
                    lastY[cell] = y;
                    lastW[cell] = cw;
                    lastH[cell] = ch;
                    x += cw;
                }
                y += ch;
            }
            byte[] px = new byte[h.width * h.height];
            for (int yy = 0; yy < h.height; yy++) System.arraycopy(canvas, (h.y0 + yy) * width + h.x0, px, yy * h.width, h.width);
            out[f] = new Frame(h.xMin, h.yMin, h.width, h.height, px);
        }
        if (next != buffer.size()) throw new IOException("DCC: " + (buffer.size() - next) + " cells of the pixel buffer unused");
        if (codes.end() - codes.position() >= 40) {
            throw new IOException("DCC: " + (codes.end() - codes.position()) + " bits of pixel codes left over");
        }
        return out;
    }

    private static void layout(Head h, int dirXMin, int dirYMin) {
        h.x0 = h.xMin - dirXMin;
        h.y0 = h.yMin - dirYMin;
        h.cellW = cells(h.x0, h.width);
        h.cellH = cells(h.y0, h.height);
        h.cellsX = h.cellW.length;
        h.cellsY = h.cellH.length;
    }

    static int[] cells(int offset, int size) {
        int first = 4 - offset % 4;
        int n;
        if (size - first <= 1) n = 1;
        else {
            int rest = size - first - 1;
            n = 2 + rest / 4;
            if (rest % 4 == 0) n--;
        }
        int[] out = new int[n];
        if (n == 1) out[0] = size;
        else {
            out[0] = first;
            for (int i = 1; i < n - 1; i++) out[i] = 4;
            out[n - 1] = size - first - 4 * (n - 2);
        }
        return out;
    }

    private static void fill(byte[] canvas, int width, int x, int y, int w, int h, byte v) {
        for (int yy = 0; yy < h; yy++) java.util.Arrays.fill(canvas, (y + yy) * width + x, (y + yy) * width + x + w, v);
    }

    private static void copy(byte[] canvas, int width, int fromX, int fromY, int x, int y, int w, int h) {
        if (fromX == x && fromY == y) return;
        byte[] tmp = new byte[w * h];
        for (int yy = 0; yy < h; yy++) System.arraycopy(canvas, (fromY + yy) * width + fromX, tmp, yy * w, w);
        for (int yy = 0; yy < h; yy++) System.arraycopy(tmp, yy * w, canvas, (y + yy) * width + x, w);
    }

    private static void check(Bits s, String name) throws IOException {
        if (s.position() != s.end()) throw new IOException("DCC: " + name + " stream: " + (s.end() - s.position()) + " bits left");
    }

    public static int[] toArgb(Frame frame, int[] palette) {
        return toArgb(frame, palette, null);
    }

    public static int[] toArgb(Frame frame, int[] palette, byte[] map) {
        int[] out = new int[frame.pixels.length];
        for (int i = 0; i < out.length; i++) {
            int v = frame.pixels[i] & 0xFF;
            if (v == 0) continue;
            out[i] = palette[map != null ? map[v] & 0xFF : v];
        }
        return out;
    }

    public static Frame remap(Frame frame, byte[] map) {
        byte[] px = new byte[frame.pixels.length];
        for (int i = 0; i < px.length; i++) {
            int v = frame.pixels[i] & 0xFF;
            px[i] = v == 0 ? 0 : map[v];
        }
        return new Frame(frame.xMin, frame.yMin, frame.width, frame.height, px);
    }

    private static int u32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
