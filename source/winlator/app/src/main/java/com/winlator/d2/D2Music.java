package com.winlator.d2;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;










final class D2Music {
    static final String TITLE = "data\\global\\music\\introedit.wav";
    private static final String CACHE = "d2-title.wav";

    private static final long GRACE_MS = 2500;
    private static final long FADE_MS = 1500;
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static MediaPlayer player;

    private static String playerSource;
    private static boolean preparing, forGame;

    private static boolean gameFront;
    private static int pages;
    private static final Runnable pauseLater = () -> {
        if (pages == 0 && !forGame) pause();
    };

    private D2Music() {}


    static void pageShown(Context context) {
        pages++;
        handler.removeCallbacks(pauseLater);
        if (D2Settings.sound(context)) start(context);
    }


    static void pageHidden() {
        pages = Math.max(0, pages - 1);
        if (pages == 0) handler.postDelayed(pauseLater, GRACE_MS);
    }


    static void soundChanged(Context context) {
        if (D2Settings.sound(context) && (pages > 0 || forGame)) start(context);
        else pause();
    }


    static void keepForGame() {
        forGame = true;
        gameFront = true;
        handler.removeCallbacks(pauseLater);
    }


    static void gameInFront(Context context, boolean front) {
        if (!forGame) return;
        gameFront = front;
        if (front && D2Settings.sound(context)) start(context);
        else if (!front) pause();
    }


    static void fadeOut() {
        forGame = false;
        final MediaPlayer p = player;
        if (p == null) return;
        final long start = android.os.SystemClock.uptimeMillis();
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (player != p) return;
                float left = 1f - (android.os.SystemClock.uptimeMillis() - start) / (float) FADE_MS;
                if (left <= 0) {
                    stop();
                    return;
                }
                try {
                    p.setVolume(left, left);
                }
                catch (IllegalStateException e) {
                    stop();
                    return;
                }
                handler.postDelayed(this, 50);
            }
        });
    }

    private static void start(Context context) {
        final String source = GameLocator.chosenPath(context);
        if (player != null && !java.util.Objects.equals(source, playerSource)) stop();
        if (player != null) {
            try {
                player.setVolume(1f, 1f);
                if (!player.isPlaying()) player.start();
            }
            catch (IllegalStateException e) {
                stop();
            }
            return;
        }
        if (preparing) return;
        preparing = true;
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            File file = cached(app, source);
            handler.post(() -> {
                preparing = false;
                if (file == null || player != null || !D2Settings.sound(app) || (pages == 0 && !(forGame && gameFront))) return;
                if (!java.util.Objects.equals(source, GameLocator.chosenPath(app))) {
                    start(app);
                    return;
                }
                MediaPlayer p = new MediaPlayer();
                try {
                    p.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                    p.setDataSource(file.getPath());
                    p.setLooping(true);
                    p.prepare();
                    p.start();
                    player = p;
                    playerSource = source;
                }
                catch (Exception e) {
                    D2Trace.log("title music not played: " + e);
                    p.release();
                }
            });
        }, "TitleMusic").start();
    }





    private static File cached(Context app, String source) {
        File file = new File(app.getCacheDir(), CACHE);
        File from = new File(app.getCacheDir(), CACHE + ".from");
        if (source != null && file.isFile() && file.length() > 44 && source.equals(readText(from))) return file;
        try {
            File dir = GameLocator.gameDir(app);
            if (dir == null || source == null || !dir.getPath().equals(new File(source).getPath())) return null;
            from.delete();
            long start = android.os.SystemClock.uptimeMillis();
            byte[] wav = D2Sound.read(dir, "d2xmusic.mpq", TITLE);
            if (wav == null) wav = D2Sound.read(dir, "d2music.mpq", TITLE);
            if (wav == null) return null;
            File part = new File(app.getCacheDir(), CACHE + ".part");
            try (FileOutputStream out = new FileOutputStream(part)) {
                out.write(wav);
            }
            if (!part.renameTo(file)) return null;
            try (FileOutputStream out = new FileOutputStream(from)) {
                out.write(source.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            D2Trace.log("title music decoded in " + (android.os.SystemClock.uptimeMillis() - start) + " ms");
            return file;
        }
        catch (Throwable e) {
            D2Trace.log("title music not decoded: " + e);
            return null;
        }
    }

    private static String readText(File file) {
        try {
            return new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static void pause() {
        if (player == null) return;
        try {
            if (player.isPlaying()) player.pause();
        }
        catch (IllegalStateException e) {
            stop();
        }
    }

    private static void stop() {
        MediaPlayer p = player;
        player = null;
        if (p == null) return;
        try {
            p.stop();
        }
        catch (IllegalStateException ignored) {
        }
        p.release();
    }
}
