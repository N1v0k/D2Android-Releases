package com.winlator.d2;

import java.io.IOException;








public final class D2Cof {

    public static final String[] COMPONENTS = {"HD", "TR", "LG", "RA", "LA", "RH", "LH", "SH", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8"};


    public static final class Layer {
        public final int component;
        public final boolean shadow, selectable, transparent;
        public final int drawEffect;
        public final String weaponClass;

        Layer(int component, boolean shadow, boolean selectable, boolean transparent, int drawEffect, String weaponClass) {
            this.component = component;
            this.shadow = shadow;
            this.selectable = selectable;
            this.transparent = transparent;
            this.drawEffect = drawEffect;
            this.weaponClass = weaponClass;
        }


        public String code() {
            return component >= 0 && component < COMPONENTS.length ? COMPONENTS[component] : "?";
        }
    }

    public final int directions, framesPerDirection, version, xMin, xMax, yMin, yMax, animationRate;
    public final Layer[] layers;

    public final int[] events;

    public final int[][][] priority;

    private D2Cof(int directions, int framesPerDirection, int version, int xMin, int xMax, int yMin, int yMax,
                  int animationRate, Layer[] layers, int[] events, int[][][] priority) {
        this.directions = directions;
        this.framesPerDirection = framesPerDirection;
        this.version = version;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.animationRate = animationRate;
        this.layers = layers;
        this.events = events;
        this.priority = priority;
    }

    public static D2Cof decode(byte[] d) throws IOException {
        if (d.length < 28) throw new IOException("COF: too short");
        int nLayers = d[0] & 0xFF, frames = d[1] & 0xFF, dirs = d[2] & 0xFF, version = d[3] & 0xFF;
        if (nLayers < 1 || nLayers > 16 || frames < 1 || dirs < 1 || dirs > 64) throw new IOException("COF: header out of range");
        int need = 28 + 9 * nLayers + frames + dirs * frames * nLayers;
        if (d.length < need) throw new IOException("COF: " + d.length + " bytes, " + need + " needed");
        int xMin = s32(d, 8), xMax = s32(d, 12), yMin = s32(d, 16), yMax = s32(d, 20), rate = (d[24] & 0xFF) | (d[25] & 0xFF) << 8;
        Layer[] layers = new Layer[nLayers];
        int p = 28;
        for (int i = 0; i < nLayers; i++, p += 9) {
            StringBuilder wc = new StringBuilder();
            for (int k = 0; k < 4 && d[p + 5 + k] != 0; k++) wc.append((char) (d[p + 5 + k] & 0xFF));
            layers[i] = new Layer(d[p] & 0xFF, d[p + 1] != 0, d[p + 2] != 0, d[p + 3] != 0, d[p + 4] & 0xFF, wc.toString());
        }
        int[] events = new int[frames];
        for (int f = 0; f < frames; f++) events[f] = d[p++] & 0xFF;
        int[][][] priority = new int[dirs][frames][nLayers];
        for (int dir = 0; dir < dirs; dir++) {
            for (int f = 0; f < frames; f++) {
                for (int l = 0; l < nLayers; l++) priority[dir][f][l] = d[p++] & 0xFF;
            }
        }
        return new D2Cof(dirs, frames, version, xMin, xMax, yMin, yMax, rate, layers, events, priority);
    }

    private static int s32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }
}
