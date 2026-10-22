package com.winlator.d2;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class D2NotifyAskActivity extends Activity {
    private static final long WAIT_MS = 1200;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable close = this::finish;
    public static final String EXTRA_ASKED = "d2_notify_asked";
    private boolean asked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        D2KeepAliveService.createChannel(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(close, WAIT_MS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(close);
        if (!isFinishing()) asked = true;
        super.onPause();
    }

    @Override
    public void finish() {
        setResult(RESULT_OK, new android.content.Intent().putExtra(EXTRA_ASKED, asked));
        super.finish();
        overridePendingTransition(0, 0);
    }
}
