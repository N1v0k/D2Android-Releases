package com.winlator.d2;

import com.winlator.R;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.app.NotificationManagerCompat;

public class D2BackgroundActivity extends D2Activity {
    private static final String PHANTOM_FLAG = "settings_enable_monitor_phantom_procs";
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, pad, pad, pad);
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(D2Style.BACKGROUND);
        scrollView.addView(content);
        setContentView(scrollView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        content.removeAllViews();

        TextView title = new TextView(this);
        title.setText(getString(R.string.background_title));
        title.setTextSize(24);
        D2Style.heading(title);
        content.addView(title);
        TextView intro = new TextView(this);
        intro.setText(getString(R.string.background_intro));
        D2Style.hint(intro);
        intro.setPadding(0, dp(4), 0, dp(8));
        content.addView(intro);

        boolean notify = notificationsAllowed(this);
        addPoint(notify ? D2Style.GREEN : D2Style.RED, getString(R.string.background_notify),
            notify ? getString(R.string.background_notify_on)
                : getString(R.string.background_notify_off),
            notify ? null : getString(R.string.background_notify_allow), this::openNotificationSettings);

        boolean battery = batteryUnrestricted(this);
        addPoint(battery ? D2Style.GREEN : D2Style.RED, getString(R.string.background_battery),
            battery ? getString(R.string.background_battery_on)
                : getString(R.string.background_battery_off),
            battery ? null : getString(R.string.background_battery_allow), this::askBattery);

        Boolean limit = childProcessLimitActive(this);
        if (limit != null) {
            String text, button = null;
            Runnable action = null;
            if (!limit) text = getString(R.string.background_limit_off);
            else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                text = getString(R.string.background_limit_adb);
            }
            else if (!developerOptionsOn(this)) {
                text = getString(R.string.background_limit_dev_off);
                button = getString(R.string.background_about_phone);
                action = () -> open(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
            }
            else {
                text = getString(R.string.background_limit_dev_on);
                button = getString(R.string.background_dev_options);
                action = () -> open(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            }
            addPoint(limit ? D2Style.RED : D2Style.GREEN, getString(R.string.background_limit), text, button, action);
        }

        addPoint(D2Style.GOLD, getString(R.string.background_lock),
            getString(R.string.background_lock_text), null, null);
    }

    private void addPoint(int color, String name, String text, String buttonText, Runnable action) {
        TextView heading = new TextView(this);
        heading.setText("●  " + name);
        heading.setTextSize(18);
        heading.setTextColor(D2Style.TEXT);
        android.text.SpannableString dot = new android.text.SpannableString(heading.getText());
        dot.setSpan(new android.text.style.ForegroundColorSpan(color), 0, 1, 0);
        heading.setText(dot);
        heading.setPadding(0, dp(16), 0, dp(2));
        content.addView(heading);
        TextView body = new TextView(this);
        body.setText(text);
        D2Style.hint(body);
        content.addView(body);
        if (buttonText != null && action != null) {
            Button button = new Button(this);
            button.setText(buttonText);
            button.setOnClickListener((v) -> action.run());
            content.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }


    public static String summary(Context context) {
        int done = 0, total = 2;
        if (notificationsAllowed(context)) done++;
        if (batteryUnrestricted(context)) done++;
        Boolean limit = childProcessLimitActive(context);
        if (limit != null) {
            total++;
            if (!limit) done++;
        }
        return context.getString(R.string.background_summary, done, total);
    }

    public static boolean notificationsAllowed(Context context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true;
        NotificationChannel channel = nm.getNotificationChannel(D2KeepAliveService.CHANNEL_ID);
        return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    public static boolean batteryUnrestricted(Context context) {
        PowerManager power = context.getSystemService(PowerManager.class);
        return power != null && power.isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static Boolean childProcessLimitActive(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null;
        String value = null;
        try {
            value = Settings.Global.getString(context.getContentResolver(), PHANTOM_FLAG);
        }
        catch (RuntimeException ignored) {}
        if (value == null || value.isEmpty()) value = systemProperty("persist.sys.fflag.override." + PHANTOM_FLAG);
        if (value == null || value.isEmpty()) return true;
        return Boolean.parseBoolean(value);
    }

    private static boolean developerOptionsOn(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
    }

    private static String systemProperty(String key) {
        try {
            return (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
        }
        catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }


    private void openNotificationSettings() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        boolean appAllowed = NotificationManagerCompat.from(this).areNotificationsEnabled();
        Intent intent;
        if (appAllowed && nm != null && nm.getNotificationChannel(D2KeepAliveService.CHANNEL_ID) != null) {
            intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_CHANNEL_ID, D2KeepAliveService.CHANNEL_ID);
        }
        else intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        if (!open(intent)) openAppDetails();
    }

    private void askBattery() {
        if (!open(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())))) {
            if (!open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) openAppDetails();
        }
    }

    private void openAppDetails() {
        open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }

    private boolean open(Intent intent) {
        try {
            startActivity(intent);
            return true;
        }
        catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
