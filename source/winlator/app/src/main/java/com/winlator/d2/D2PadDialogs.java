package com.winlator.d2;

import android.app.Dialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;









final class D2PadDialogs {
    private D2PadDialogs() {}


    static D2PadPages attach(Dialog dialog, Window window) {
        D2PadPages.Page page = new D2PadPages.Page() {
            @Override
            public View padFirstFocus() {
                return safeButton(dialog);
            }

            @Override
            public boolean onPad(int action, View focused) {
                return false;
            }

            @Override
            public void padActive(boolean active) {}
        };


        Runnable back = () -> {
            dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
            dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
        };
        return new D2PadPages(dialog.getContext(), () -> window, () -> page, back);
    }





    static void shown(Window window, D2PadPages pad) {
        View decor = window.peekDecorView();
        if (decor == null) return;
        decor.getViewTreeObserver().addOnTouchModeChangeListener(pad::touchModeChanged);
        D2PadFocus.all(decor);
        pad.shownInPadMode();
    }


    static View safeButton(Dialog dialog) {
        View b = null;
        if (dialog instanceof android.app.AlertDialog) b = ((android.app.AlertDialog) dialog).getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
        else if (dialog instanceof androidx.appcompat.app.AlertDialog)
            b = ((androidx.appcompat.app.AlertDialog) dialog).getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);

        if (b != null && b.getVisibility() != View.VISIBLE && b.getTag() instanceof View) b = (View) b.getTag();
        return b != null && b.getVisibility() == View.VISIBLE && b.isShown() ? b : null;
    }
}
