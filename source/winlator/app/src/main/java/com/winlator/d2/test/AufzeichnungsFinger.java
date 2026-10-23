package com.winlator.d2.test;

import android.os.SystemClock;

import java.io.IOException;
import java.io.Writer;
import java.util.Locale;






final class AufzeichnungsFinger implements Finger {
    private final Writer out;
    private final long start;
    private final boolean[] unten = new boolean[8];

    AufzeichnungsFinger(Writer out, long start) {
        this.out = out;
        this.start = start;
    }

    private void schreiben(String text) {
        try {
            out.write((SystemClock.uptimeMillis() - start) + " " + text + "\n");
        }
        catch (IOException ignored) {}
    }

    @Override
    public boolean isDown(int finger) {
        return finger >= 0 && finger < unten.length && unten[finger];
    }

    @Override
    public void down(int finger, float x, float y) {
        schreiben(String.format(Locale.ROOT, "%s %d %.0f %.0f", isDown(finger) ? "move" : "down", finger, x, y));
        if (finger >= 0 && finger < unten.length) unten[finger] = true;
    }

    @Override
    public void move(int finger, float x, float y) {
        schreiben(String.format(Locale.ROOT, "move %d %.0f %.0f", finger, x, y));
    }

    @Override
    public void up(int finger) {
        schreiben("up " + finger);
        if (finger >= 0 && finger < unten.length) unten[finger] = false;
    }

    @Override
    public void releaseAll() {
        schreiben("releaseAll");
        java.util.Arrays.fill(unten, false);
    }
}
