package com.winlator.d2;

/*
 * Java port of the fragment shader "Hyllian's xBR-lv2" from https://github.com/libretro/glsl-shaders
 * (xbr/shaders/xbr-lv2.glsl), licence MIT:
 *
 *   Hyllian's xBR-lv2 Shader
 *   Copyright (C) 2011-2016 Hyllian - sergiogdb@gmail.com
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 *   Incorporates some of the ideas from SABR shader. Thanks to Joshua Street.
 */











public final class D2Xbr {
    private static final float EQ_THRESHOLD = 15f, LV2_COEFFICIENT = 2f;

    private static final float WR = 14.352f, WG = 28.176f, WB = 5.472f;


    private static final float[] AO = {1, -1, -1, 1}, BO = {1, 1, -1, -1}, CO = {1.5f, 0.5f, -0.5f, 0.5f};
    private static final float[] BX = {0.5f, 2, -0.5f, -2}, CX = {1, 1, -0.5f, 0};
    private static final float[] BY = {2, 0.5f, -2, -0.5f}, CY = {2, 0, -1, 0.5f};
    private static final float CI = 0.25f;


    public static final float FILL = 0.9f;

    private D2Xbr() {
    }








    public static int[] fit(int w, int h, float bw, float bh) {
        float s = Math.min(bw / w, bh / h);
        int n = (int) s;
        if (n >= 1 && n >= s * FILL) return new int[]{n, w * n, h * n};
        return new int[]{n + 1, Math.max(1, Math.round(w * s)), Math.max(1, Math.round(h * s))};
    }





    public static int[] scale(int[] argb, int w, int h, int n) {
        if (w <= 0 || h <= 0 || n < 1 || argb.length < w * h) throw new IllegalArgumentException("bad picture or factor");
        if (n == 1) return java.util.Arrays.copyOf(argb, w * h);
        int count = w * h;
        float[][] col = new float[count][4];
        float[] luma = new float[count];
        for (int i = 0; i < count; i++) {
            int c = argb[i];
            float a = (c >>> 24) / 255f;
            col[i][0] = (c >> 16 & 0xFF) / 255f * a;
            col[i][1] = (c >> 8 & 0xFF) / 255f * a;
            col[i][2] = (c & 0xFF) / 255f * a;
            col[i][3] = a;
            luma[i] = col[i][0] * WR + col[i][1] * WG + col[i][2] * WB;
        }
        int ow = w * n;
        int[] out = new int[ow * h * n];
        float delta = 1f / n;
        float[] deltaL = {0.5f * delta, delta, 0.5f * delta, delta}, deltaU = {delta, 0.5f * delta, delta, 0.5f * delta};
        float[] b = new float[4], c = new float[4], d = new float[4], f = new float[4], g = new float[4], hh = new float[4],
            ii = new float[4], i4 = new float[4], i5 = new float[4], h5 = new float[4], f4 = new float[4];
        float[] edr = new float[4], edri = new float[4], edrL = new float[4], edrU = new float[4];
        boolean[] px = new boolean[4];
        float[] res1 = new float[4], res2 = new float[4], m = new float[4];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int iE = at(x, y, w, h), iB = at(x, y - 1, w, h), iD = at(x - 1, y, w, h), iF = at(x + 1, y, w, h),
                    iH = at(x, y + 1, w, h), iA = at(x - 1, y - 1, w, h), iC = at(x + 1, y - 1, w, h),
                    iG = at(x - 1, y + 1, w, h), iI = at(x + 1, y + 1, w, h);
                float e = luma[iE];
                set(b, luma[iB], luma[iD], luma[iH], luma[iF]);
                set(c, luma[iC], luma[iA], luma[iG], luma[iI]);
                set(d, b[1], b[2], b[3], b[0]);
                set(f, b[3], b[0], b[1], b[2]);
                set(g, c[2], c[3], c[0], c[1]);
                set(hh, b[2], b[3], b[0], b[1]);
                set(ii, c[3], c[0], c[1], c[2]);
                set(i4, luma[at(x + 2, y + 1, w, h)], luma[at(x + 1, y - 2, w, h)], luma[at(x - 2, y - 1, w, h)],
                    luma[at(x - 1, y + 2, w, h)]);
                set(i5, luma[at(x + 1, y + 2, w, h)], luma[at(x + 2, y - 1, w, h)], luma[at(x - 1, y - 2, w, h)],
                    luma[at(x - 2, y + 1, w, h)]);
                set(h5, luma[at(x, y + 2, w, h)], luma[at(x + 2, y, w, h)], luma[at(x, y - 2, w, h)],
                    luma[at(x - 2, y, w, h)]);
                set(f4, h5[1], h5[2], h5[3], h5[0]);
                for (int k = 0; k < 4; k++) {
                    float irlv0 = diff(e, f[k]) * diff(e, hh[k]);
                    float irlv1 = irlv0 * (neq(f[k], b[k]) * neq(f[k], c[k]) + neq(hh[k], d[k]) * neq(hh[k], g[k])
                        + eq(e, ii[k]) * (neq(f[k], f4[k]) * neq(f[k], i4[k]) + neq(hh[k], h5[k]) * neq(hh[k], i5[k]))
                        + eq(e, g[k]) + eq(e, c[k]));
                    float irlv2l = diff(e, g[k]) * diff(d[k], g[k]);
                    float irlv2u = diff(e, c[k]) * diff(b[k], c[k]);
                    float wd1 = df(e, c[k]) + df(e, g[k]) + df(ii[k], h5[k]) + df(ii[k], f4[k]) + 4 * df(hh[k], f[k]);
                    float wd2 = df(hh[k], d[k]) + df(hh[k], i5[k]) + df(f[k], i4[k]) + df(f[k], b[k]) + 4 * df(e, ii[k]);
                    edri[k] = step(wd1, wd2) * irlv0;
                    edr[k] = step(wd1 + 0.1f, wd2) * step(0.5f, irlv1);
                    edrL[k] = step(LV2_COEFFICIENT * df(f[k], g[k]), df(hh[k], c[k])) * irlv2l * edr[k];
                    edrU[k] = step(LV2_COEFFICIENT * df(hh[k], c[k]), df(f[k], g[k])) * irlv2u * edr[k];
                    px[k] = df(e, f[k]) <= df(e, hh[k]);
                }
                float[] cE = col[iE], cB = col[iB], cD = col[iD], cF = col[iF], cH = col[iH];
                for (int sy = 0; sy < n; sy++) {
                    float fy = (sy + 0.5f) / n;
                    for (int sx = 0; sx < n; sx++) {
                        float fxp = (sx + 0.5f) / n;
                        for (int k = 0; k < 4; k++) {
                            float fx = AO[k] * fy + BO[k] * fxp, fxL = AO[k] * fy + BX[k] * fxp, fxU = AO[k] * fy + BY[k] * fxp;
                            float fx45i = edri[k] * clamp((fx + delta - CO[k] - CI) / (2 * delta));
                            float fx45 = edr[k] * clamp((fx + delta - CO[k]) / (2 * delta));
                            float fx30 = edrL[k] * clamp((fxL + deltaL[k] - CX[k]) / (2 * deltaL[k]));
                            float fx60 = edrU[k] * clamp((fxU + deltaU[k] - CY[k]) / (2 * deltaU[k]));
                            m[k] = Math.max(Math.max(fx30, fx60), Math.max(fx45, fx45i));
                        }
                        System.arraycopy(cE, 0, res1, 0, 4);
                        mix(res1, px[0] ? cF : cH, m[0]);
                        mix(res1, px[2] ? cD : cB, m[2]);
                        System.arraycopy(cE, 0, res2, 0, 4);
                        mix(res2, px[1] ? cB : cF, m[1]);
                        mix(res2, px[3] ? cH : cD, m[3]);
                        float[] res = cdf(cE, res2) >= cdf(cE, res1) ? res2 : res1;
                        out[(y * n + sy) * ow + x * n + sx] = pack(res);
                    }
                }
            }
        }
        return out;
    }

    private static int at(int x, int y, int w, int h) {
        return Math.max(0, Math.min(h - 1, y)) * w + Math.max(0, Math.min(w - 1, x));
    }

    private static void set(float[] v, float a, float b, float c, float d) {
        v[0] = a;
        v[1] = b;
        v[2] = c;
        v[3] = d;
    }

    private static float df(float a, float b) {
        return Math.abs(a - b);
    }

    private static float diff(float a, float b) {
        return a != b ? 1 : 0;
    }

    private static float eq(float a, float b) {
        return Math.abs(a - b) <= EQ_THRESHOLD ? 1 : 0;
    }

    private static float neq(float a, float b) {
        return 1 - eq(a, b);
    }


    private static float step(float edge, float x) {
        return x >= edge ? 1 : 0;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }


    private static void mix(float[] v, float[] to, float t) {
        for (int i = 0; i < 4; i++) v[i] = v[i] + (to[i] - v[i]) * t;
    }


    private static float cdf(float[] a, float[] b) {
        return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) + Math.abs(a[2] - b[2]) + Math.abs(a[3] - b[3]);
    }

    private static int pack(float[] p) {
        float a = p[3];
        if (a <= 0.5f / 255) return 0;
        int ai = Math.round(Math.min(1, a) * 255);
        int r = Math.round(Math.min(1, p[0] / a) * 255), g = Math.round(Math.min(1, p[1] / a) * 255),
            bl = Math.round(Math.min(1, p[2] / a) * 255);
        return ai << 24 | r << 16 | g << 8 | bl;
    }
}
