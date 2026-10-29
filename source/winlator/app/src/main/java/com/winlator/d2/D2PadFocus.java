package com.winlator.d2;

import android.view.View;







final class D2PadFocus {
    private D2PadFocus() {}





    static <T extends View> T frame(T view) {
        view.setFocusable(true);
        view.setDefaultFocusHighlightEnabled(false);
        view.setForeground(new D2PadFrame.Focused(view.getResources().getDisplayMetrics().density));
        return view;
    }


    static void all(View root) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> walk(root));
    }





    interface Own {}


    static void walk(View v) {

        if (v.isClickable() && v.getVisibility() == View.VISIBLE && !(v instanceof Own) && !(v.getForeground() instanceof D2PadFrame.Focused))
            frame(v);
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) walk(g.getChildAt(i));
        }
    }





    static void keep(View box, Runnable rebuild) {
        View focus = box.findFocus();
        java.util.ArrayList<View> before = box.getFocusables(View.FOCUS_FORWARD);
        int at = focus != null ? before.indexOf(focus) : -1;
        rebuild.run();
        if (at < 0) return;
        walk(box);
        java.util.ArrayList<View> after = box.getFocusables(View.FOCUS_FORWARD);
        if (!after.isEmpty()) after.get(Math.min(at, after.size() - 1)).requestFocus();
    }
}
