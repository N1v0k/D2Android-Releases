package com.winlator.d2;

import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.winlator.d2.test.Finger;

import java.util.function.Consumer;

final class D2IdleFinger implements Finger {
    static final int FINGERS = 3, POINTER_BASE = 8;

    private final Consumer<MotionEvent> dispatch;
    private final boolean[] down = new boolean[FINGERS];
    private final float[] x = new float[FINGERS], y = new float[FINGERS];
    private long downTime;

    D2IdleFinger(Consumer<MotionEvent> dispatch) {
        this.dispatch = dispatch;
    }

    @Override
    public boolean isDown(int finger) {
        return down[finger];
    }

    @Override
    public void down(int finger, float px, float py) {
        if (down[finger]) {
            move(finger, px, py);
            return;
        }
        int before = count();
        down[finger] = true;
        x[finger] = px;
        y[finger] = py;
        long now = SystemClock.uptimeMillis();
        if (before == 0) {
            downTime = now;
            send(MotionEvent.ACTION_DOWN, now);
        }
        else send(MotionEvent.ACTION_POINTER_DOWN | (index(finger) << MotionEvent.ACTION_POINTER_INDEX_SHIFT), now);
    }

    @Override
    public void move(int finger, float px, float py) {
        if (!down[finger]) return;
        x[finger] = px;
        y[finger] = py;
        send(MotionEvent.ACTION_MOVE, SystemClock.uptimeMillis());
    }

    @Override
    public void up(int finger) {
        if (!down[finger]) return;
        long now = SystemClock.uptimeMillis();
        if (count() == 1) send(MotionEvent.ACTION_UP, now);
        else send(MotionEvent.ACTION_POINTER_UP | (index(finger) << MotionEvent.ACTION_POINTER_INDEX_SHIFT), now);
        down[finger] = false;
    }

    @Override
    public void releaseAll() {
        if (count() == 0) return;
        send(MotionEvent.ACTION_CANCEL, SystemClock.uptimeMillis());
        for (int i = 0; i < FINGERS; i++) down[i] = false;
    }

    private int count() {
        int n = 0;
        for (boolean d : down) if (d) n++;
        return n;
    }

    private int index(int finger) {
        int n = 0;
        for (int i = 0; i < finger; i++) if (down[i]) n++;
        return n;
    }

    private void send(int action, long now) {
        int n = count();
        MotionEvent.PointerProperties[] props = new MotionEvent.PointerProperties[n];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[n];
        int k = 0;
        for (int i = 0; i < FINGERS; i++) {
            if (!down[i]) continue;
            props[k] = new MotionEvent.PointerProperties();
            props[k].id = POINTER_BASE + i;
            props[k].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[k] = new MotionEvent.PointerCoords();
            coords[k].x = x[i];
            coords[k].y = y[i];
            coords[k].pressure = 1;
            coords[k].size = 0.1f;
            k++;
        }
        MotionEvent event = MotionEvent.obtain(downTime, now, action, n, props, coords, 0, 0, 1, 1, D2Idle.DEVICE_ID, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0);
        try {
            dispatch.accept(event);
        }
        finally {
            event.recycle();
        }
    }
}
