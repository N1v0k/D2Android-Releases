package com.winlator.d2;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;








public final class D2Updates {
    private static final String PREFS = "d2_updates";
    private static final String KEY_LAST_CHECK = "lastCheck", KEY_TAG = "tag", KEY_URL = "url", KEY_BODY = "body",
        KEY_LATER = "later", KEY_SEEN_CODE = "seenVersionCode";

    private D2Updates() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static String installedVersion(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        }
        catch (Exception e) {
            return null;
        }
    }

    static long installedCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        }
        catch (Exception e) {
            return 0;
        }
    }


    public static void start(Context context) {
        Context app = context.getApplicationContext();
        if (com.winlator.BuildConfig.D2_INTERNAL || !D2Settings.updateCheck(app)) return;
        long now = System.currentTimeMillis();
        if (!D2UpdateCheck.due(prefs(app).getLong(KEY_LAST_CHECK, 0), now)) return;
        prefs(app).edit().putLong(KEY_LAST_CHECK, now).apply();
        new Thread(() -> {
            D2UpdateCheck.Release release = fetch();
            D2Trace.log("update check: " + (release == null ? "nothing" : release.tag));
            if (release != null) store(app, release);
        }, "update check").start();
    }

    private static D2UpdateCheck.Release fetch() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(D2UpdateCheck.LATEST_URL).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            if (connection.getResponseCode() != 200) return null;
            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0 && out.size() < 512 * 1024) out.write(buffer, 0, n);
                return D2UpdateCheck.parse(new String(out.toByteArray(), StandardCharsets.UTF_8));
            }
        }
        catch (Exception e) {
            return null;
        }
        finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static void store(Context context, D2UpdateCheck.Release release) {
        prefs(context).edit().putString(KEY_TAG, release.tag).putString(KEY_URL, release.url).putString(KEY_BODY, release.body).apply();
    }


    public static D2UpdateCheck.Release pending(Context context) {
        SharedPreferences p = prefs(context);
        String tag = p.getString(KEY_TAG, null);
        if (tag == null || tag.equals(p.getString(KEY_LATER, null))) return null;
        if (!com.winlator.BuildConfig.D2_INTERNAL && !D2Settings.updateCheck(context)) return null;
        D2UpdateCheck.Release release = new D2UpdateCheck.Release(tag, p.getString(KEY_URL, null), p.getString(KEY_BODY, ""));
        return D2UpdateCheck.isNewer(release, installedVersion(context)) ? release : null;
    }


    public static void later(Context context, D2UpdateCheck.Release release) {
        prefs(context).edit().putString(KEY_LATER, release.tag).apply();
    }





    public static boolean newsAfterUpdate(Context context) {
        long code = installedCode(context), seen = prefs(context).getLong(KEY_SEEN_CODE, 0);
        if (seen != code) prefs(context).edit().putLong(KEY_SEEN_CODE, code).apply();
        return !com.winlator.BuildConfig.D2_INTERNAL && seen > 0 && seen < code;
    }
}
