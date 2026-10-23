package com.winlator.d2;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;







final class D2PadFocus {
    private D2PadFocus() {}


    static <T extends View> T frame(T view) {
        view.setFocusable(true);
        float density = view.getResources().getDisplayMetrics().density;
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0x24C8B074);
        focused.setStroke(Math.max(2, Math.round(2 * density)), 0xFFC8B074);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, new android.graphics.drawable.ColorDrawable(0));
        view.setForeground(states);
        return view;
    }


    static void all(View root) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> walk(root));
    }

    private static void walk(View v) {
        if (v.isClickable() && !v.isFocusable() && v.getVisibility() == View.VISIBLE) frame(v);
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i));
        }
    }
}
