package com.winlator.d2;

import android.os.Handler;
import android.os.Looper;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
















final class D2GamepadInput {

    private static final long TICK_MS = 16;

    static volatile String lastDevice, lastButton;

    private final D2TouchOverlay overlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final D2Gamepad.Stick stick = new D2Gamepad.Stick();
    private final boolean[] down = new boolean[D2Gamepad.COUNT];

    private final boolean[] pressedInLayer2 = new boolean[D2Gamepad.COUNT];

    private final boolean[] swallowed = new boolean[D2Gamepad.COUNT];
    private final boolean[] longDone = new boolean[D2Gamepad.COUNT];
    private final Runnable[] longPress = new Runnable[D2Gamepad.COUNT];

    private final boolean[] deferred = new boolean[D2Gamepad.COUNT];
    private boolean triggerKeys, hatLeft, hatRight, hatUp, hatDown;

    private float lx, ly, rx, ry;
    private final Runnable tick = this::tick;

    D2GamepadInput(D2TouchOverlay overlay) {
        this.overlay = overlay;
    }


    boolean onKey(KeyEvent event) {
        if (!fromPad(event.getSource(), event.getDevice(), true)) return false;
        if (!overlay.padEnabled()) return false;
        int button = D2Gamepad.buttonOf(event.getKeyCode(), event.getFlags());
        if (button == D2Gamepad.NONE) return (event.getFlags() & D2Gamepad.FLAG_FALLBACK) != 0;
        if (D2SpieltestPort.playing()) {
            releaseAll();
            return true;
        }
        int action = event.getAction();
        if (action != KeyEvent.ACTION_DOWN && action != KeyEvent.ACTION_UP) return true;
        if (action == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 0) return true;
        if (button == D2Gamepad.LT || button == D2Gamepad.RT) triggerKeys = true;
        noteDevice(event.getDevice(), button);
        press(button, action == KeyEvent.ACTION_DOWN);
        return true;
    }


    boolean onMotion(MotionEvent event) {
        if (!fromPad(event.getSource(), event.getDevice(), false)) return false;
        if (D2SpieltestPort.playing()) {
            releaseAll();
            return true;
        }
        if (!overlay.padEnabled() || event.getAction() != MotionEvent.ACTION_MOVE) return true;

        if (!triggerKeys) {
            float lt = Math.max(event.getAxisValue(MotionEvent.AXIS_LTRIGGER), event.getAxisValue(MotionEvent.AXIS_BRAKE));
            float rt = Math.max(event.getAxisValue(MotionEvent.AXIS_RTRIGGER), event.getAxisValue(MotionEvent.AXIS_GAS));
            boolean ltDown = D2Gamepad.triggerDown(down[D2Gamepad.LT], lt), rtDown = D2Gamepad.triggerDown(down[D2Gamepad.RT], rt);
            if (ltDown != down[D2Gamepad.LT]) axisButton(event, D2Gamepad.LT, ltDown);
            if (rtDown != down[D2Gamepad.RT]) axisButton(event, D2Gamepad.RT, rtDown);
        }
        float hx = event.getAxisValue(MotionEvent.AXIS_HAT_X), hy = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        if ((hx < -0.5f) != hatLeft) axisButton(event, D2Gamepad.DPAD_LEFT, hatLeft = hx < -0.5f);
        if ((hx > 0.5f) != hatRight) axisButton(event, D2Gamepad.DPAD_RIGHT, hatRight = hx > 0.5f);
        if ((hy < -0.5f) != hatUp) axisButton(event, D2Gamepad.DPAD_UP, hatUp = hy < -0.5f);
        if ((hy > 0.5f) != hatDown) axisButton(event, D2Gamepad.DPAD_DOWN, hatDown = hy > 0.5f);

        lx = event.getAxisValue(MotionEvent.AXIS_X);
        ly = event.getAxisValue(MotionEvent.AXIS_Y);
        rx = event.getAxisValue(MotionEvent.AXIS_Z);
        ry = event.getAxisValue(MotionEvent.AXIS_RZ);
        boolean changed = stick.update(lx, ly, overlay.padDeadzone());
        boolean right = Math.hypot(rx, ry) > overlay.padDeadzone() / 100f;
        if (stick.active || right) {
            overlay.onRealPad();
            overlay.padUsed(true);
        }
        if (changed || right) sendStick();
        return true;
    }

    private boolean testerSeen;





    void playtesterCheck() {
        boolean playing = D2SpieltestPort.playing();
        if (playing && !testerSeen) releaseAll();
        testerSeen = playing;
    }


    void releaseAll() {
        for (int b = 0; b < D2Gamepad.COUNT; b++) {
            if (longPress[b] != null) handler.removeCallbacks(longPress[b]);
            if (b == D2Gamepad.VIEW && longDone[b]) overlay.padWheelCancel();
            if (!down[b]) continue;
            down[b] = false;
            if (deferred[b]) {
                deferred[b] = false;
                continue;
            }

            boolean holds = !swallowed[b] && b != D2Gamepad.LT && D2Gamepad.beltRow(b) < 0 && !D2Gamepad.hasLongPress(b);
            swallowed[b] = false;
            if (holds) overlay.padButton(b, overlay.padAction(b, pressedInLayer2[b]), false);
        }
        hatLeft = hatRight = hatUp = hatDown = false;
        lx = ly = rx = ry = 0;
        if (stick.active) {
            stick.reset();
            sendStick();
        }
        handler.removeCallbacks(tick);
    }

    private static boolean fromPad(int source, InputDevice device, boolean key) {
        int pad = InputDevice.SOURCE_GAMEPAD, joystick = InputDevice.SOURCE_JOYSTICK;
        if ((source & pad) == pad || (source & joystick) == joystick) return true;


        return key && device != null && ((device.getSources() & pad) == pad || (device.getSources() & joystick) == joystick);
    }

    private void axisButton(MotionEvent event, int button, boolean pressed) {
        noteDevice(event.getDevice(), button);
        press(button, pressed);
    }

    private void noteDevice(InputDevice device, int button) {
        if (device != null) lastDevice = device.getName();
        lastButton = D2Gamepad.NAMES[button];
    }

    private void press(int button, boolean pressed) {
        if (down[button] == pressed) return;
        down[button] = pressed;
        if (pressed) {
            overlay.onRealPad();
            overlay.padUsed(true);

            swallowed[button] = overlay.padResume(button)
                || button == D2Gamepad.A && overlay.deathTap();
            if (swallowed[button]) return;
        }
        else if (swallowed[button]) {
            swallowed[button] = false;
            return;
        }
        if (overlay.padInputBox(button, pressed)) {
            if (!pressed) {
                deferredBox[button] = deferred[button] = false;
                if (longPress[button] != null) handler.removeCallbacks(longPress[button]);
            }
            return;
        }
        if (pressed ? overlay.padHoldsForBox(button) : deferredBox[button]) {
            heldOpensBox(button, pressed);
            return;
        }
        if (button == D2Gamepad.LT) {
            overlay.padLayer(pressed);
            return;
        }
        int row = D2Gamepad.beltRow(button);
        if (row >= 0) {
            if (pressed) overlay.padBelt(row, down[D2Gamepad.LT]);
            return;
        }
        if (D2Gamepad.hasLongPress(button)) {
            longOrShort(button, pressed);
            return;
        }
        if (pressed) pressedInLayer2[button] = down[D2Gamepad.LT];
        if (pressed ? D2Gamepad.sortsWhenHeld(button, overlay.padInventoryOpen()) : deferred[button]) {
            heldSorts(button, pressed);
            return;
        }
        overlay.padButton(button, overlay.padAction(button, pressedInLayer2[button]), pressed);
    }


    private final boolean[] deferredBox = new boolean[D2Gamepad.COUNT];


    private void heldOpensBox(int button, boolean pressed) {
        if (pressed) {
            deferredBox[button] = true;
            longDone[button] = false;
            pressedInLayer2[button] = down[D2Gamepad.LT];
            Runnable r = () -> {
                if (!down[button] || !deferredBox[button]) return;
                longDone[button] = true;
                overlay.padOpenBox();
            };
            longPress[button] = r;
            handler.postDelayed(r, D2Gamepad.LONG_MS);
            return;
        }
        deferredBox[button] = false;
        if (longPress[button] != null) handler.removeCallbacks(longPress[button]);
        if (longDone[button]) return;
        int act = overlay.padAction(button, pressedInLayer2[button]);
        overlay.padButton(button, act, true);
        overlay.padButton(button, act, false);
    }


    private void heldSorts(int button, boolean pressed) {
        if (pressed) {
            deferred[button] = true;
            longDone[button] = false;
            Runnable r = () -> {
                if (!down[button] || !deferred[button]) return;
                longDone[button] = true;
                overlay.padSort();
            };
            longPress[button] = r;
            handler.postDelayed(r, D2Gamepad.LONG_MS);
            return;
        }
        deferred[button] = false;
        if (longPress[button] != null) handler.removeCallbacks(longPress[button]);
        if (longDone[button]) return;
        int act = overlay.padAction(button, pressedInLayer2[button]);
        overlay.padButton(button, act, true);
        overlay.padButton(button, act, false);
    }





    private void longOrShort(int button, boolean pressed) {
        boolean view = button == D2Gamepad.VIEW;
        if (pressed) {
            longDone[button] = false;
            pressedInLayer2[button] = down[D2Gamepad.LT];
            Runnable r = () -> {
                if (!down[button]) return;
                longDone[button] = true;
                if (view) overlay.padWheel(true);
                else overlay.padFixed(D2Gamepad.longAction(button));
            };
            longPress[button] = r;
            handler.postDelayed(r, D2Gamepad.LONG_MS);
            return;
        }
        if (longPress[button] != null) handler.removeCallbacks(longPress[button]);
        if (longDone[button]) {
            if (view) overlay.padWheel(false);
            return;
        }
        if (!view) {
            overlay.padFixed(D2Gamepad.shortAction(button));
            return;
        }
        int act = overlay.padAction(button, pressedInLayer2[button]);
        overlay.padButton(button, act, true);
        overlay.padButton(button, act, false);
    }

    private boolean sticksActive() {
        return stick.active || Math.hypot(rx, ry) > overlay.padDeadzone() / 100f;
    }

    private void sendStick() {
        overlay.padTick(stick.active, stick.hasDirection, stick.dirX, stick.dirY, lx, ly, rx, ry);
        handler.removeCallbacks(tick);
        if (sticksActive()) handler.postDelayed(tick, TICK_MS);
    }





    private void tick() {
        if (D2SpieltestPort.playing()) {
            releaseAll();
            return;
        }
        if (!sticksActive()) {
            overlay.padTick(false, false, stick.dirX, stick.dirY, 0, 0, 0, 0);
            return;
        }
        overlay.padTick(stick.active, stick.hasDirection, stick.dirX, stick.dirY, lx, ly, rx, ry);
        handler.postDelayed(tick, TICK_MS);
    }


}
