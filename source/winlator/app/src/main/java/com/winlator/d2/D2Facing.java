package com.winlator.d2;









final class D2Facing {
    private D2Facing() {
    }


    static float[] screen(int direction) {
        if (direction < 0 || direction >= 64) return null;
        double a = direction * 2 * Math.PI / 64 + Math.PI / 4;
        double wx = Math.cos(a), wy = Math.sin(a);
        double sx = wx - wy, sy = (wx + wy) / 2, length = Math.hypot(sx, sy);
        return new float[]{(float) (sx / length), (float) (sy / length)};
    }


    static int direction(double screenX, double screenY) {
        double a = Math.atan2(screenY - screenX / 2, screenY + screenX / 2) - Math.PI / 4;
        return (int) Math.floorMod(Math.round(a / (2 * Math.PI) * 64), 64);
    }
}
