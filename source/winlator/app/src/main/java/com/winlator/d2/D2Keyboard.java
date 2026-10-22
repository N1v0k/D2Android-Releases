package com.winlator.d2;

public final class D2Keyboard {
    public interface Ime {
        Boolean visible();

        boolean hide(Runnable done);

        void toggle();

        void postDelayed(Runnable r, long ms);
    }

    public static final long HIDE_WAIT_MS = 400;

    private D2Keyboard() {}

    public static void show(Ime ime) {
        Boolean up = ime.visible();
        if (up != null) {
            if (!up) ime.toggle();
            return;
        }
        boolean[] toggled = {false};
        Runnable toggleOnce = () -> {
            if (toggled[0]) return;
            toggled[0] = true;
            ime.toggle();
        };
        if (!ime.hide(toggleOnce)) toggleOnce.run();
        else ime.postDelayed(toggleOnce, HIDE_WAIT_MS);
    }
}
