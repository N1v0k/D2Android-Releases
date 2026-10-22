package com.winlator.d2.test;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;

import java.lang.reflect.Method;

final class Touch implements Finger {
    static final int FINGERS = 10;
    private final Object manager;
    private final Method inject;
    private final float[] xs = new float[FINGERS], ys = new float[FINGERS];
    private final boolean[] down = new boolean[FINGERS];
    private long downTime;

    Touch() throws Exception {
        Class<?> cls;
        try {
            cls = Class.forName("android.hardware.input.InputManagerGlobal");
        }
        catch (ClassNotFoundException e) {
            cls = Class.forName("android.hardware.input.InputManager");
        }
        manager = cls.getDeclaredMethod("getInstance").invoke(null);
        inject = manager.getClass().getMethod("injectInputEvent", InputEvent.class, int.class);
    }

    @Override
    public synchronized boolean isDown(int finger) {
        return down[finger];
    }

    @Override
    public synchronized void down(int finger, float x, float y) {
        if (down[finger]) {
            move(finger, x, y);
            return;
        }
        long now = SystemClock.uptimeMillis();
        int count = count();
        if (count == 0) downTime = now;
        down[finger] = true;
        xs[finger] = x;
        ys[finger] = y;
        int index = indexOf(finger);
        send(count == 0 ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_POINTER_DOWN | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT), now);
    }

    @Override
    public synchronized void move(int finger, float x, float y) {
        if (!down[finger]) return;
        xs[finger] = x;
        ys[finger] = y;
        send(MotionEvent.ACTION_MOVE, SystemClock.uptimeMillis());
    }

    @Override
    public synchronized void up(int finger) {
        if (!down[finger]) return;
        int count = count(), index = indexOf(finger);
        send(count == 1 ? MotionEvent.ACTION_UP : MotionEvent.ACTION_POINTER_UP | (index << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            SystemClock.uptimeMillis());
        down[finger] = false;
    }

    @Override
    public synchronized void releaseAll() {
        for (int f = FINGERS - 1; f >= 0; f--) up(f);
    }

    private int count() {
        int n = 0;
        for (boolean d : down) if (d) n++;
        return n;
    }

    private int indexOf(int finger) {
        int i = 0;
        for (int f = 0; f < finger; f++) if (down[f]) i++;
        return i;
    }

    private void send(int action, long now) {
        int n = count();
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[n];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[n];
        int i = 0;
        for (int f = 0; f < FINGERS; f++) {
            if (!down[f]) continue;
            MotionEvent.PointerProperties p = new MotionEvent.PointerProperties();
            p.id = f;
            p.toolType = MotionEvent.TOOL_TYPE_FINGER;
            MotionEvent.PointerCoords c = new MotionEvent.PointerCoords();
            c.x = xs[f];
            c.y = ys[f];
            c.pressure = 1;
            c.size = 0.05f;
            props[i] = p;
            coords[i] = c;
            i++;
        }
        MotionEvent event = MotionEvent.obtain(downTime, now, action, n, props, coords, 0, 0, 1, 1, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0);
        try {
            inject.invoke(manager, event, 0);
        }
        catch (Exception e) {
            throw new IllegalStateException("injectInputEvent: " + e, e);
        }
        finally {
            event.recycle();
        }
    }
}
