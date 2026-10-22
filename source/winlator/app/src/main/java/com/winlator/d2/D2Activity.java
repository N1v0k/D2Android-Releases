package com.winlator.d2;

import android.content.Context;
import android.media.AudioManager;

import androidx.appcompat.app.AppCompatActivity;

public abstract class D2Activity extends AppCompatActivity {
    private int languageGeneration;
    private D2SoundButton soundButton;

    @Override
    protected void attachBaseContext(Context base) {
        languageGeneration = D2Text.generation();
        super.attachBaseContext(D2Text.wrap(base));
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (immersive()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) getWindow().getAttributes().layoutInDisplayCutoutMode =
                android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            com.winlator.core.AppUtils.hideSystemUI(this);
            android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_ON);
            filter.addAction(android.content.Intent.ACTION_USER_PRESENT);
            registerReceiver(screenOn, filter);
            screenOnRegistered = true;
        }
    }

    @Override
    protected void onDestroy() {
        if (screenOnRegistered) {
            unregisterReceiver(screenOn);
            screenOnRegistered = false;
        }
        barsHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }


    private final android.os.Handler barsHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable hideBars = () -> {
        if (isFinishing() || !hasWindowFocus()) return;
        com.winlator.core.AppUtils.hideSystemUI(this);
    };
    private boolean barsToggleOnce;
    private final Runnable toggleBars = () -> {
        if (isFinishing() || !hasWindowFocus() || !barsToggleOnce) return;
        barsToggleOnce = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            android.view.WindowInsetsController c = getWindow().getDecorView().getWindowInsetsController();
            if (c != null) c.show(android.view.WindowInsets.Type.statusBars());
        }
        barsHandler.postDelayed(hideBars, D2SystemBars.TOGGLE_MS);
    };
    private final android.content.BroadcastReceiver screenOn = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, android.content.Intent intent) {
            barsToggleOnce = true;
            barsHandler.removeCallbacks(hideBars);
            barsHandler.removeCallbacks(toggleBars);
            barsHandler.postDelayed(toggleBars, D2SystemBars.SCREEN_ON_MS[0]);
            for (long delay : D2SystemBars.SCREEN_ON_MS) barsHandler.postDelayed(hideBars, delay + D2SystemBars.TOGGLE_MS);
        }
    };
    private boolean screenOnRegistered;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!immersive()) return;
        barsHandler.removeCallbacks(hideBars);
        if (!hasFocus) return;
        com.winlator.core.AppUtils.hideSystemUI(this);
        barsHandler.postDelayed(hideBars, D2SystemBars.LATE_MS);
        if (barsToggleOnce) barsHandler.postDelayed(toggleBars, D2SystemBars.LATE_MS);
    }

    protected boolean immersive() {
        return clicks();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (D2Settings.sound(this)) D2Sound.prepare(this);
        if (clicks()) D2Music.pageShown(this);
        if (soundButton != null) soundButton.update();
        if (immersive()) {
            com.winlator.core.AppUtils.hideSystemUI(this);
        }
        if (languageGeneration != D2Text.generation() && recreateOnLanguageChange()) recreate();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (clicks()) D2Music.pageHidden();
        barsHandler.removeCallbacks(hideBars);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (clicks()) D2Sound.touch(this, event);
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        if (!soundButtonOnTop()) return;
        android.view.ViewGroup decor = (android.view.ViewGroup) getWindow().getDecorView();
        if (soundButton == null) soundButton = new D2SoundButton(this);
        if (soundButton.getParent() == null) {
            android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.TOP | android.view.Gravity.END);
            decor.addView(soundButton, lp);
        }
        else soundButton.bringToFront();
        soundButton.post(() -> {
            android.view.WindowInsets insets = decor.getRootWindowInsets();
            if (insets == null || soundButton == null) return;
            android.widget.FrameLayout.LayoutParams lp = (android.widget.FrameLayout.LayoutParams) soundButton.getLayoutParams();
            int margin = Math.round(4 * getResources().getDisplayMetrics().density);
            int top = insets.getSystemWindowInsetTop(), right = insets.getSystemWindowInsetRight();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.view.DisplayCutout cut = insets.getDisplayCutout();
                if (cut != null) {
                    top = Math.max(top, cut.getSafeInsetTop());
                    right = Math.max(right, cut.getSafeInsetRight());
                }
            }
            lp.topMargin = top + margin;
            lp.rightMargin = right + margin;
            soundButton.setLayoutParams(lp);
        });
    }

    protected boolean soundButtonOnTop() {
        return clicks();
    }

    protected boolean clicks() {
        return true;
    }

    protected boolean recreateOnLanguageChange() {
        return true;
    }
}
