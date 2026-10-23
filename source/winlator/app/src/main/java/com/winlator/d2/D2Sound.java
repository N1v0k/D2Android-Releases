package com.winlator.d2;

import android.app.Dialog;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.Window;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;








final class D2Sound {
    static final String CLICK = "data\\global\\sfx\\cursor\\button.wav";
    private static final String CACHE = "d2-click.wav";
    private static SoundPool pool;
    private static volatile int clickId;
    private static volatile boolean ready, loading;

    private static float downX, downY;
    private static long downAt;
    private static final long TAP_MS = 400;

    private D2Sound() {}


    static void click(Context context) {
        if (!D2Settings.sound(context)) return;
        prepare(context);
        if (ready) pool.play(clickId, 1f, 1f, 1, 0, 1f);
    }





    static synchronized void prepare(Context context) {
        if (ready || loading) return;
        loading = true;
        final Context app = context.getApplicationContext();
        if (pool == null) {
            pool = new SoundPool.Builder().setMaxStreams(3).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            pool.setOnLoadCompleteListener((p, id, status) -> {
                if (status == 0) clickId = id;
                ready = status == 0;

                if (status != 0) new File(app.getCacheDir(), CACHE).delete();
                loading = false;
            });
        }
        new Thread(() -> {
            File cached = new File(app.getCacheDir(), CACHE);
            try {
                if (!cached.isFile() || cached.length() == 0) {
                    byte[] wav = read(GameLocator.gameDir(app), "d2sfx.mpq", CLICK);
                    if (wav == null) {
                        loading = false;
                        return;
                    }

                    File part = new File(app.getCacheDir(), CACHE + ".part");
                    try (FileOutputStream out = new FileOutputStream(part)) {
                        out.write(wav);
                    }
                    if (!part.renameTo(cached)) throw new IOException("cache not renamed");
                }
                int id = pool.load(cached.getPath(), 1);
                if (id == 0) {
                    cached.delete();
                    loading = false;
                    D2Trace.log("click sound not loaded: SoundPool refused " + cached.getName());
                    return;
                }
            }
            catch (Throwable e) {
                loading = false;
                D2Trace.log("click sound not loaded: " + e);
            }
        }, "ClickSound").start();
    }





    static void touch(Context context, android.view.MotionEvent event) {
        int action = event.getActionMasked();
        if (action == android.view.MotionEvent.ACTION_DOWN) {
            downX = event.getRawX();
            downY = event.getRawY();
            downAt = event.getEventTime();
        }
        else if (action == android.view.MotionEvent.ACTION_UP) {
            float slop = android.view.ViewConfiguration.get(context).getScaledTouchSlop();
            if (Math.abs(event.getRawX() - downX) <= slop && Math.abs(event.getRawY() - downY) <= slop
                && event.getEventTime() - downAt <= TAP_MS) click(context);
        }
    }


    static byte[] read(File gameDir, String archive, String name) throws IOException {
        if (gameDir == null) return null;
        File[] files = gameDir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (!f.getName().equalsIgnoreCase(archive)) continue;
            try (D2Mpq mpq = new D2Mpq(f)) {
                return mpq.read(name);
            }
        }
        return null;
    }


    static void attach(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        final Window.Callback inner = window.getCallback();
        if (inner == null) return;
        final Context context = dialog.getContext();

        window.setCallback((Window.Callback) java.lang.reflect.Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),
            new Class<?>[]{Window.Callback.class}, (proxy, method, args) -> {
                if ("dispatchTouchEvent".equals(method.getName()) && args != null && args[0] instanceof MotionEvent)
                    touch(context, (MotionEvent) args[0]);
                try {
                    return method.invoke(inner, args);
                }
                catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            }));
    }
}
