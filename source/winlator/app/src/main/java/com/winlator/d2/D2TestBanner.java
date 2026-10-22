package com.winlator.d2;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class D2TestBanner extends TextView {
    static final String FILE = "D2Android/testlauf.txt";
    private static final long POLL_MS = 2000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = this::poll;
    private boolean running;

    public D2TestBanner(Context context) {
        super(context);
        float dp = context.getResources().getDisplayMetrics().density;
        setTextSize(11);
        setTextColor(D2Style.GOLD);
        setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        setMaxLines(2);
        setEllipsize(TextUtils.TruncateAt.END);
        setMaxWidth(Math.round(context.getResources().getDisplayMetrics().widthPixels * 0.26f));
        int h = Math.round(6 * dp), v = Math.round(3 * dp);
        setPadding(h, v, h, v);
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xD00F0C09);
        background.setStroke(Math.max(1, Math.round(dp)), D2Style.FRAME);
        background.setCornerRadius(3 * dp);
        setBackground(background);
        setClickable(false);
        setFocusable(false);
        setVisibility(GONE);
    }

    static String read() {
        if (!com.winlator.BuildConfig.D2_INTERNAL) return "";
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FILE);
        if (!file.isFile() || file.length() == 0 || file.length() > 4096) return "";
        StringBuilder text = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            for (int n = 0; n < 2 && (line = in.readLine()) != null; ) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (text.length() > 0) text.append('\n');
                text.append(line);
                n++;
            }
        }
        catch (Exception e) {
            return "";
        }
        return text.toString();
    }

    private void poll() {
        if (!running) return;
        String text = read();
        if (!text.contentEquals(getText())) setText(text);
        setVisibility(text.isEmpty() ? GONE : VISIBLE);
        handler.postDelayed(poll, POLL_MS);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override
    protected void onDetachedFromWindow() {
        running = false;
        handler.removeCallbacks(poll);
        super.onDetachedFromWindow();
    }
}
