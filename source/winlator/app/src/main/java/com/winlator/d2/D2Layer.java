package com.winlator.d2;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;






final class D2Layer {







    static final int MATERIALS = 0, INFO = 1, POINTS = 2, STONES = 3, EFFECTS = 4, WINDOW_INFO = 5, RECIPES = 8, INPUT_BOX = 9;

    static final int RECIPE_PICTURES = 96;




    static final int STAGE_BAR = 1, STAGE_TOOLTIP = 2;


    D2Layer stage(int stage) {
        this.stage = stage;
        return this;
    }

    static final int ALPHA_25 = 0, ALPHA_50 = 1, ALPHA_75 = 2, OPAQUE = 3;

    static final int FONT8 = 0, FONT16 = 1, FORMAL12 = 5, FONT6 = 6, EXOCET10 = 9, EXOCET8 = 11;

    static final int WHITE = 0, RED = 1, GOLD = 4, GREY = 5, BLACK = 6;




    static final int STONE_TEXT = GREY, STONE_FONT = EXOCET10;

    static final int STONE_CORE_RGB = 0x040404, STONE_SHADE_RGB = 0x505050;

    static final int LEFT = 0, CENTRE = 1, RIGHT = 2;

    static final int MAX_OPS = 400, MAX_TEXT = 63, PICTURES = 256, HEAD = 22, PACKET = 248, MAX_PARTS = 32;

    private static final int OP_RECT = 1, OP_TEXT = 2, OP_PIC = 3, OP_LINE = 4, OP_GLYPH = 5, OP_TABLE = 7;

    private final List<byte[]> ops = new ArrayList<>();
    private final String[] table = new String[PICTURES];
    private int drawOps, stage = STAGE_TOOLTIP;


    D2Layer rect(int x, int y, int w, int h, int colour, int alpha) {
        if (w <= 0 || h <= 0) return this;
        add(new byte[]{OP_RECT, lo(x), hi(x), lo(y), hi(y), lo(w), hi(w), lo(h), hi(h), (byte) colour, (byte) alpha}, true);
        return this;
    }


    D2Layer outline(int x, int y, int w, int h, int colour) {
        if (w <= 0 || h <= 0) return this;
        add(new byte[]{OP_LINE, lo(x), hi(x), lo(y), hi(y), lo(w), hi(w), lo(h), hi(h), (byte) colour}, true);
        return this;
    }





    D2Layer text(int x, int y, int font, int colour, int align, String text) {
        if (text == null || text.isEmpty()) return this;
        String t = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
        byte[] op = new byte[9 + 2 * t.length()];
        op[0] = OP_TEXT;
        op[1] = lo(x);
        op[2] = hi(x);
        op[3] = lo(y);
        op[4] = hi(y);
        op[5] = (byte) font;
        op[6] = (byte) colour;
        op[7] = (byte) align;
        op[8] = (byte) t.length();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i) < 0x20 ? ' ' : t.charAt(i);
            op[9 + 2 * i] = lo(c);
            op[10 + 2 * i] = hi(c);
        }
        add(op, true);
        return this;
    }





    D2Layer picture(int no, String path, int frame, int sx, int sy, int sw, int sh, int x, int y, int w, int h) {
        if (no < 0 || no >= PICTURES || path == null || w <= 0 || h <= 0) return this;
        table(no, path);
        add(new byte[]{OP_PIC, (byte) no, (byte) frame, lo(sx), hi(sx), lo(sy), hi(sy), lo(sw), hi(sw), lo(sh), hi(sh),
            lo(x), hi(x), lo(y), hi(y), lo(w), hi(w), lo(h), hi(h), OPAQUE}, true);
        return this;
    }






    D2Layer glyph(int no, String path, int frame, int x, int bottom, int lo, int hi, int colour) {
        if (no < 0 || no >= PICTURES || path == null) return this;
        table(no, path);
        add(new byte[]{OP_GLYPH, (byte) no, (byte) frame, lo(x), hi(x), lo(bottom), hi(bottom), (byte) lo, (byte) hi, (byte) colour}, true);
        return this;
    }

    private void table(int no, String path) {
        if (table[no] != null) return;
        table[no] = path;
        byte[] p = path.getBytes(StandardCharsets.US_ASCII);
        byte[] op = new byte[3 + p.length];
        op[0] = OP_TABLE;
        op[1] = (byte) no;
        op[2] = (byte) p.length;
        System.arraycopy(p, 0, op, 3, p.length);
        add(op, false);
    }

    boolean isEmpty() {
        return drawOps == 0;
    }

    int drawOps() {
        return drawOps;
    }


    boolean sameAs(D2Layer other) {
        if (other == null || other.ops.size() != ops.size()) return false;
        for (int i = 0; i < ops.size(); i++) if (!Arrays.equals(ops.get(i), other.ops.get(i))) return false;
        return true;
    }






    byte[][] packets(int layer, int stand, int gameWidth, int gameHeight, int validMs) {
        if (drawOps > MAX_OPS) return null;
        List<ByteArrayOutputStream> parts = new ArrayList<>();
        ByteArrayOutputStream cur = new ByteArrayOutputStream();
        for (byte[] op : ops) {
            if (HEAD + op.length > PACKET) return null;
            if (HEAD + cur.size() + op.length > PACKET) {
                parts.add(cur);
                cur = new ByteArrayOutputStream();
            }
            cur.write(op, 0, op.length);
        }
        if (cur.size() > 0) parts.add(cur);
        if (parts.size() > MAX_PARTS) return null;
        byte[][] out = new byte[Math.max(1, parts.size())][];
        for (int k = 0; k < out.length; k++) {
            byte[] body = parts.isEmpty() ? new byte[0] : parts.get(k).toByteArray();
            byte[] p = new byte[HEAD + body.length];
            p[0] = 'D';
            p[1] = '2';
            p[2] = 'A';
            p[3] = 'C';
            p[4] = 1;
            p[5] = 33;
            p[8] = (byte) layer;
            p[9] = (byte) stage;
            p[10] = (byte) k;
            p[11] = (byte) parts.size();
            p[12] = (byte) stand;
            p[13] = (byte) (stand >> 8);
            p[14] = (byte) (stand >> 16);
            p[15] = (byte) (stand >> 24);
            p[16] = lo(validMs);
            p[17] = hi(validMs);
            p[18] = lo(gameWidth);
            p[19] = hi(gameWidth);
            p[20] = lo(gameHeight);
            p[21] = hi(gameHeight);
            System.arraycopy(body, 0, p, HEAD, body.length);
            out[k] = p;
        }
        return out;
    }

    private void add(byte[] op, boolean draws) {
        ops.add(op);
        if (draws) drawOps++;
    }

    private static byte lo(int v) {
        return (byte) v;
    }

    private static byte hi(int v) {
        return (byte) (v >> 8);
    }
}
