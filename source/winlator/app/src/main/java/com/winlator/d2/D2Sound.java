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




    static final String[] SFX_ARCHIVES = {"patch_d2.mpq", "d2exp.mpq", "d2sfx.mpq"};

    private static final long LATE_MS = 1000;

    private static SoundPool sfxPool;
    private static final java.util.Map<String, Integer> sfxIds = new java.util.HashMap<>();
    private static final java.util.Map<Integer, String> sfxLoading = new java.util.HashMap<>();
    private static final java.util.Set<String> sfxStarted = new java.util.HashSet<>();
    private static final java.util.Map<String, Long> sfxWanted = new java.util.HashMap<>();






    static synchronized void play(Context context, String file) {
        if (file == null || !D2Settings.sound(context)) return;
        Integer id = sfxIds.get(file);
        if (id != null) {
            sfxPool.play(id, 1f, 1f, 1, 0, 1f);
            return;
        }
        sfxWanted.put(file, android.os.SystemClock.uptimeMillis());
        if (!sfxStarted.add(file)) return;
        final Context app = context.getApplicationContext();
        if (sfxPool == null) {
            sfxPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            sfxPool.setOnLoadCompleteListener((p, sample, status) -> {
                synchronized (D2Sound.class) {
                    String f = sfxLoading.remove(sample);
                    if (f == null) return;
                    Long wanted = sfxWanted.remove(f);
                    if (status != 0) {
                        sfxStarted.remove(f);
                        new File(app.getCacheDir(), sfxCache(f)).delete();
                        return;
                    }
                    sfxIds.put(f, sample);
                    if (wanted != null && android.os.SystemClock.uptimeMillis() - wanted <= LATE_MS) p.play(sample, 1f, 1f, 1, 0, 1f);
                }
            });
        }
        new Thread(() -> loadSfx(app, file), "GameSound").start();
    }

    private static void loadSfx(Context app, String file) {
        File cached = new File(app.getCacheDir(), sfxCache(file));
        try {
            if (!cached.isFile() || cached.length() == 0) {
                byte[] wav = null;
                for (String archive : SFX_ARCHIVES) {
                    wav = read(GameLocator.gameDir(app), archive, "data\\global\\sfx\\" + file);
                    if (wav != null) break;
                }
                if (wav == null) {
                    D2Trace.log("game sound " + file + " not loaded: not in the archives");
                    failed(file);
                    return;
                }
                File part = new File(app.getCacheDir(), sfxCache(file) + ".part");
                try (FileOutputStream out = new FileOutputStream(part)) {
                    out.write(wav);
                }
                if (!part.renameTo(cached)) throw new IOException("cache not renamed");
            }
            synchronized (D2Sound.class) {
                int id = sfxPool.load(cached.getPath(), 1);
                if (id == 0) {
                    cached.delete();
                    D2Trace.log("game sound " + file + " not loaded: SoundPool refused it");
                    failed(file);
                    return;
                }
                sfxLoading.put(id, file);
            }
        }
        catch (Throwable e) {
            D2Trace.log("game sound " + file + " not loaded: " + e);
            failed(file);
        }
    }


    private static synchronized void failed(String file) {
        sfxStarted.remove(file);
        sfxWanted.remove(file);
    }


    static String sfxCache(String file) {
        return "d2-sfx-" + file.replace('\\', '-').replace('/', '-');
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
        final D2PadPages pad = D2PadDialogs.attach(dialog, window);

        window.setCallback((Window.Callback) java.lang.reflect.Proxy.newProxyInstance(Window.Callback.class.getClassLoader(),
            new Class<?>[]{Window.Callback.class}, (proxy, method, args) -> {
                if ("dispatchTouchEvent".equals(method.getName()) && args != null && args[0] instanceof MotionEvent) {
                    touch(context, (MotionEvent) args[0]);
                    if (((MotionEvent) args[0]).getActionMasked() == MotionEvent.ACTION_DOWN) pad.touched();
                }
                if ("dispatchKeyEvent".equals(method.getName()) && args != null && args[0] instanceof android.view.KeyEvent
                    && pad.key((android.view.KeyEvent) args[0])) return true;
                try {
                    Object result = method.invoke(inner, args);
                    if ("onAttachedToWindow".equals(method.getName())) D2PadDialogs.shown(window, pad);
                    return result;
                }
                catch (java.lang.reflect.InvocationTargetException e) {
                    throw e.getCause();
                }
            }));

        android.view.View decor = window.peekDecorView();
        if (decor != null && decor.isAttachedToWindow()) D2PadDialogs.shown(window, pad);
    }
}
