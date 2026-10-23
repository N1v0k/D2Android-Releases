package com.winlator.d2;







public final class D2Cutout {
    private D2Cutout() {}






    public static float[] clear(float cx, float cy, float hw, float hh, float[][] cutouts, float w, float h, float margin) {
        return clear(cx, cy, hw, hh, cutouts, w, h, margin, null);
    }





    public static float[] clear(float cx, float cy, float hw, float hh, float[][] cutouts, float w, float h, float margin,
                                float[][] neighbours) {
        if (cutouts == null || clearOf(cx, cy, hw, hh, cutouts, margin)) return new float[]{cx, cy};



        float x = w >= 2 * hw ? Math.max(hw, Math.min(w - hw, cx)) : cx;
        float y = h >= 2 * hh ? Math.max(hh, Math.min(h - hh, cy)) : cy;

        float bestX = cx, bestY = cy, bestMove = Float.MAX_VALUE;
        for (float[] c : cutouts) {
            float l = c[0] - margin, t = c[1] - margin, r = c[2] + margin, b = c[3] + margin;
            float[][] spots = {{l - hw, y}, {r + hw, y}, {x, t - hh}, {x, b + hh}};
            for (float[] spot : spots) {
                float move = Math.abs(spot[0] - cx) + Math.abs(spot[1] - cy);
                boolean inside = spot[0] - hw >= 0 && spot[0] + hw <= w && spot[1] - hh >= 0 && spot[1] + hh <= h;
                if (inside && move < bestMove && clearOf(spot[0], spot[1], hw, hh, cutouts, margin)
                    && (neighbours == null || clearOf(spot[0], spot[1], hw, hh, neighbours, 0))) {
                    bestX = spot[0];
                    bestY = spot[1];
                    bestMove = move;
                }
            }
        }

        return new float[]{bestX, bestY};
    }

    private static boolean clearOf(float x, float y, float hw, float hh, float[][] cutouts, float margin) {
        for (float[] c : cutouts) {

            if (x + hw > c[0] - margin && x - hw < c[2] + margin && y + hh > c[1] - margin && y - hh < c[3] + margin) return false;
        }
        return true;
    }
}
