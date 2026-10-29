package com.winlator.d2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;









final class D2PadPages {

    interface Page {

        View padFirstFocus();


        boolean onPad(int action, View focused);


        void padActive(boolean active);
    }


    interface Stepper {

        boolean padSteps();


        boolean padStep(int direction);
    }

    private final Context context;
    private final java.util.function.Supplier<Window> window;
    private final java.util.function.Supplier<Page> page;

    private final Runnable back;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean active;

    private View pressed;
    private boolean longDone;
    private final Runnable longPress = this::longPressNow;

    private final java.util.Set<Integer> downs = new java.util.HashSet<>();

    D2PadPages(Context context, java.util.function.Supplier<Window> window, java.util.function.Supplier<Page> page, Runnable back) {
        this.context = context;
        this.window = window;
        this.page = page;
        this.back = back;
    }

    private void longPressNow() {
        View v = pressed;

        if (v == null || !v.isAttachedToWindow() || !v.isFocused() || !v.hasWindowFocus()) {
            cancel();
            return;
        }
        v.setPressed(false);
        if (v.isLongClickable()) longDone = v.performLongClick();
    }

    private View focused() {
        Window w = window.get();
        return w != null ? w.getCurrentFocus() : null;
    }


    boolean key(KeyEvent e) {
        int action = D2PadKeys.ours(e.getKeyCode(), e.getSource(), e.getFlags());
        if (action == D2PadKeys.NONE) return false;
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN, first = down && e.getRepeatCount() == 0;
        boolean button = !D2PadKeys.direction(action);
        if (button && first) downs.add(e.getKeyCode());


        if (button && !down && !downs.remove(e.getKeyCode())) return true;
        Page page = this.page.get();
        if (page == null) return false;


        if (!down && e.isCanceled()) {
            cancel();
            return button;
        }
        if (first) {
            active = true;
            page.padActive(true);
        }
        View focused = focused();
        if (pressed != null && pressed != focused) cancel();
        boolean unseen = focused == null || focused.isInTouchMode();
        if (!button || action == D2PadKeys.A) {
            if (unseen) {
                if (first) focusFirst(page);
                return true;
            }


            if (button && (keepsNativeA(focused)
                || !(focused instanceof android.widget.AdapterView) && !focused.hasOnClickListeners() && !focused.isLongClickable()))
                return false;
            if (button) return a(focused, down, first);

            if ((action == D2PadKeys.LEFT || action == D2PadKeys.RIGHT) && focused instanceof Stepper && ((Stepper) focused).padSteps()) {
                if (down && ((Stepper) focused).padStep(action == D2PadKeys.LEFT ? -1 : 1)) D2Sound.click(context);
                return true;
            }
            return false;
        }



        if (!down) {
            View on = unseen ? null : focused;
            handler.post(() -> {
                if (!page.onPad(action, on) && action == D2PadKeys.B) back.run();
            });
        }
        return true;
    }


    private static boolean keepsNativeA(View v) {
        return v instanceof android.widget.EditText
            || v instanceof android.widget.TextView && ((android.widget.TextView) v).getMovementMethod() != null;
    }

    private void focusFirst(Page page) {
        View first = page.padFirstFocus();
        if (first != null && first.requestFocusFromTouch()) return;

        Window w = window.get();
        if (w != null) w.getDecorView().requestFocusFromTouch();
    }

    private boolean a(View focused, boolean down, boolean first) {
        if (down) {
            if (first) {
                pressed = focused;
                longDone = false;
                focused.setPressed(true);
                handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout());
            }
            return true;
        }
        handler.removeCallbacks(longPress);
        View v = pressed;
        pressed = null;
        if (v == null) return true;
        v.setPressed(false);
        if (!longDone && v == focused) {
            D2Sound.click(context);
            handler.post(() -> {
                if (!v.isAttachedToWindow()) return;
                if (v instanceof android.widget.AdapterView) {

                    android.widget.AdapterView<?> list = (android.widget.AdapterView<?>) v;
                    int at = list.getSelectedItemPosition();
                    if (at != android.widget.AdapterView.INVALID_POSITION)
                        list.performItemClick(list.getSelectedView(), at, list.getItemIdAtPosition(at));
                }
                else v.performClick();
            });
        }
        return true;
    }


    void touched() {
        cancel();
        if (!active) return;
        active = false;
        Page page = this.page.get();
        if (page != null) page.padActive(false);
    }


    void cancel() {
        handler.removeCallbacks(longPress);
        if (pressed != null) pressed.setPressed(false);
        pressed = null;
    }







    void touchModeChanged(boolean inTouchMode) {
        if (inTouchMode) return;
        handler.post(() -> {
            Page page = this.page.get();
            Window w = window.get();
            if (page == null || w == null || w.getDecorView().isInTouchMode() || !w.getDecorView().hasWindowFocus()) return;
            active = true;
            page.padActive(true);
            View first = page.padFirstFocus();
            if (first != null) first.requestFocus();
        });
    }





    void shownInPadMode() {
        handler.post(() -> {
            Page page = this.page.get();
            Window w = window.get();
            if (page == null || w == null || w.getDecorView().isInTouchMode()) return;
            active = true;
            page.padActive(true);
            View first = page.padFirstFocus();
            if (first != null) first.requestFocus();
        });
    }

    boolean active() {
        return active;
    }
}
