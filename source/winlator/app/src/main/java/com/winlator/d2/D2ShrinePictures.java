package com.winlator.d2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class D2ShrinePictures {
    static final int FIRST = 128, LAST = 137;
    static final int FRAME = 8;
    static final String[] FALLBACK = {"ShrineArmor", "ShrineCombat", "ShrineResistLightning", "ShrineResistFire",
        "ShrineResistCold", "ShrineResistPoison", "ShrineSkill", "ShrineManaRegen", "ShrineStamina", "ShrineExperience"};

    static final class Picture {
        final int width, height;
        final int[] argb;

        Picture(int width, int height, int[] argb) {
            this.width = width;
            this.height = height;
            this.argb = argb;
        }
    }

    private D2ShrinePictures() {}

    static String[] files(byte[] statesTxt, byte[] overlayTxt) {
        String[] out = FALLBACK.clone();
        if (statesTxt == null || overlayTxt == null) return out;
        String[][] states = table(statesTxt), overlays = table(overlayTxt);
        int sId = column(states, "id"), sOverlay = column(states, "overlay1");
        int oName = column(overlays, "overlay"), oFile = column(overlays, "filename");
        if (sId < 0 || sOverlay < 0 || oName < 0 || oFile < 0) return out;
        for (int r = 1; r < states.length; r++) {
            int id;
            try {
                id = Integer.parseInt(cell(states[r], sId).trim());
            }
            catch (NumberFormatException e) {
                continue;
            }
            if (id < FIRST || id > LAST) continue;
            String overlay = cell(states[r], sOverlay);
            for (int o = 1; o < overlays.length && !overlay.isEmpty(); o++) {
                if (cell(overlays[o], oName).equalsIgnoreCase(overlay) && !cell(overlays[o], oFile).isEmpty()) {
                    out[id - FIRST] = cell(overlays[o], oFile);
                }
            }
        }
        return out;
    }

    static Picture[] load(D2Sprites s) throws IOException {
        String[] names = files(s.read("data\\global\\excel\\states.txt"), s.read("data\\global\\excel\\overlay.txt"));
        int[] palette = s.palette();
        Picture[] out = new Picture[names.length];
        for (int i = 0; i < names.length; i++) {
            try {
                D2Dcc.Dcc dcc = s.dcc("data\\global\\overlays\\" + names[i] + ".dcc");
                if (dcc == null) continue;
                D2Dcc.Frame f = dcc.frames[0][Math.min(FRAME, dcc.framesPerDirection - 1)];
                out[i] = new Picture(f.width, f.height, D2Dcc.toArgb(f, palette));
            }
            catch (IOException e) {
            }
        }
        return out;
    }

    private static String[][] table(byte[] txt) {
        String[] lines = new String(txt, StandardCharsets.ISO_8859_1).split("\r?\n");
        String[][] out = new String[lines.length][];
        for (int i = 0; i < lines.length; i++) out[i] = lines[i].split("\t", -1);
        return out;
    }

    private static int column(String[][] t, String name) {
        if (t.length == 0) return -1;
        for (int c = 0; c < t[0].length; c++) if (t[0][c].trim().equalsIgnoreCase(name)) return c;
        return -1;
    }

    private static String cell(String[] row, int c) {
        return c < row.length ? row[c].trim() : "";
    }
}
