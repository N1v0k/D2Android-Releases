package com.winlator.d2;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.view.PixelCopy;

import com.winlator.core.Callback;
import com.winlator.core.ProcessHelper;
import com.winlator.widget.XServerView;
import com.winlator.winhandler.ProcessInfo;
import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XServer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class D2TestSession {
    public static final String EXTRA_RUN = "d2test_run";
    private static final long MAX_WINE_LOG_BYTES = 24L * 1024 * 1024;
    private static final long MAX_GAME_LOG_BYTES = 48L * 1024 * 1024;
    private static final int TAIL_LINES = 20000;
    private static final int KEYEVENTF_KEYUP = 0x0002;

    private final android.app.Activity activity;
    private final Runnable requestExit;
    private final JSONObject run;
    private final File resultDir;
    private final long startMillis = System.currentTimeMillis();
    private final Object logLock = new Object();
    private final Object eventLock = new Object();
    private final ArrayDeque<String> wineTail = new ArrayDeque<>();
    private final ArrayList<String> processNames = new ArrayList<>();
    private final AtomicBoolean ended = new AtomicBoolean(false);
    private final Callback<String> logCallback = this::onLogLine;
    private BufferedWriter wineLog;
    private BufferedWriter gameLog;
    private long gameLogBytes = 0;
    private boolean gameStarted = false;
    private final java.util.HashSet<String> knownThreads = new java.util.HashSet<>();
    private final java.util.HashSet<String> gameThreads = new java.util.HashSet<>();
    private BufferedWriter eventLog;
    private long wineLogBytes = 0;
    private long wineLinesDropped = 0;
    private ScheduledExecutorService scheduler;
    private WinHandler winHandler;
    private XServerView xServerView;
    private volatile String endReason = "guest-exit";

    private static volatile boolean attached;

    public static boolean attached() {
        return attached;
    }

    private D2TestSession(android.app.Activity activity, Runnable requestExit, JSONObject run) throws IOException, JSONException {
        this.activity = activity;
        this.requestExit = requestExit;
        this.run = run;
        this.resultDir = new File(run.getString("resultDir"));
        if (!resultDir.isDirectory() && !resultDir.mkdirs()) throw new IOException("Cannot create " + resultDir);
        wineLog = new BufferedWriter(new FileWriter(new File(resultDir, "wine.log")));
        gameLog = new BufferedWriter(new FileWriter(new File(resultDir, "wine-game.log")));
        eventLog = new BufferedWriter(new FileWriter(new File(resultDir, "events.log")));
        ProcessHelper.addDebugCallback(logCallback);
        event("session created: " + run.optString("name"));
        attached = true;
    }

    public static D2TestSession attachIfRequested(android.app.Activity activity, Intent intent, Runnable requestExit) {
        if (!com.winlator.BuildConfig.D2_INTERNAL) return null;
        String json = intent != null ? intent.getStringExtra(EXTRA_RUN) : null;
        if (json == null) return null;
        try {
            return new D2TestSession(activity, requestExit, new JSONObject(json));
        }
        catch (Exception e) {
            return null;
        }
    }

    public void onXServerReady(XServer xServer, WinHandler winHandler, XServerView xServerView) {
        this.winHandler = winHandler;
        this.xServerView = xServerView;

        xServer.windowManager.addOnWindowModificationListener(new WindowManager.OnWindowModificationListener() {
            @Override
            public void onMapWindow(Window window) {
                if (isInteresting(window)) event("map " + describe(window));
            }

            @Override
            public void onUnmapWindow(Window window) {
                if (isInteresting(window)) event("unmap " + describe(window));
            }

            @Override
            public void onUpdateWindowGeometry(Window window, boolean resized) {
                if (resized && window.isApplicationWindow()) event("resize " + describe(window));
            }
        });

        winHandler.setOnGetProcessInfoListener((index, count, processInfo) -> onProcessInfo(index, count, processInfo));

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> winHandler.listProcesses(), 5, 5, TimeUnit.SECONDS);

        JSONArray shots = run.optJSONArray("screenshotsAt");
        if (shots != null) {
            for (int i = 0; i < shots.length(); i++) {
                final int seconds = shots.optInt(i);
                scheduler.schedule(() -> takeScreenshot("t" + seconds), seconds, TimeUnit.SECONDS);
            }
        }

        JSONArray keys = run.optJSONArray("keys");
        if (keys != null) {
            for (int i = 0; i < keys.length(); i++) {
                JSONObject key = keys.optJSONObject(i);
                if (key == null) continue;
                if (!TestPlan.isAllowedKey(key.optInt("vk"))) continue;
                final byte vk = (byte) key.optInt("vk");
                long atMillis = (long) (key.optDouble("at", 0) * 1000);
                scheduler.schedule(() -> {
                    event(String.format(Locale.ROOT, "key vk=0x%02X", vk & 0xFF));
                    winHandler.keyboardEvent(vk, 0);
                    sleep(80);
                    winHandler.keyboardEvent(vk, KEYEVENTF_KEYUP);
                }, atMillis, TimeUnit.MILLISECONDS);
            }
        }

        int timeout = run.optInt("timeoutSeconds", 45);
        scheduler.schedule(this::onTimeout, timeout, TimeUnit.SECONDS);
        event("monitoring started, timeout " + timeout + "s");
    }

    public void onSessionEnded() {
        if (!ended.compareAndSet(false, true)) return;
        attached = false;
        if (scheduler != null) scheduler.shutdownNow();
        ProcessHelper.removeDebugCallback(logCallback);
        event("session ended: " + endReason);

        try {
            JSONObject summary = new JSONObject();
            summary.put("name", run.optString("name"));
            summary.put("endReason", endReason);
            summary.put("durationMs", System.currentTimeMillis() - startMillis);
            summary.put("wineLogBytes", wineLogBytes);
            summary.put("wineLinesDropped", wineLinesDropped);
            writeFile(new File(resultDir, "session.json"), summary.toString(2));
        }
        catch (Exception ignored) {}

        synchronized (logLock) {
            try {
                if (wineLinesDropped > 0) {
                    wineLog.write("\n[d2test] ... " + wineLinesDropped + " lines omitted, last " + wineTail.size() + " lines follow ...\n");
                    for (String line : wineTail) wineLog.write(line + "\n");
                }
                wineLog.close();
                gameLog.close();
            }
            catch (IOException ignored) {}
        }
        synchronized (eventLock) {
            try {
                eventLog.close();
            }
            catch (IOException ignored) {}
        }

        Intent intent = Intent.makeRestartActivityTask(new android.content.ComponentName(activity, D2TestActivity.class));
        intent.putExtra(D2TestActivity.EXTRA_CONTINUE, true);
        activity.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    private void onTimeout() {
        endReason = "timeout";
        event("timeout reached, killing Game.exe");
        takeScreenshot("timeout");
        sleep(1500);
        if (winHandler != null) {
            winHandler.killProcess("Game.exe");
            winHandler.killProcess("D2VidTst.exe");
        }
        sleep(2500);
        activity.runOnUiThread(requestExit);
    }

    private void onProcessInfo(int index, int count, ProcessInfo info) {
        synchronized (processNames) {
            if (index == 0) processNames.clear();
            if (info != null) processNames.add(info.name + "(" + info.pid + (info.wow64Process ? ",32bit" : "") + ")");
            if (count == 0 || index == count - 1) {
                event("processes " + String.join(" ", processNames));
            }
        }
    }

    private void takeScreenshot(final String label) {
        final XServerView view = xServerView;
        if (view == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                int width = view.getWidth(), height = view.getHeight();
                if (width <= 0 || height <= 0) return;
                final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                PixelCopy.request(view, bitmap, (result) -> {
                    if (result != PixelCopy.SUCCESS) {
                        event("screenshot " + label + " failed: " + result);
                        return;
                    }
                    Executors.newSingleThreadExecutor().execute(() -> saveScreenshot(bitmap, label));
                }, new Handler(Looper.getMainLooper()));
            }
            catch (Exception e) {
                event("screenshot " + label + " error: " + e);
            }
        });
    }

    private void saveScreenshot(Bitmap bitmap, String label) {
        Bitmap scaled = bitmap;
        if (bitmap.getWidth() > 1280) {
            int height = Math.round(bitmap.getHeight() * (1280f / bitmap.getWidth()));
            scaled = Bitmap.createScaledBitmap(bitmap, 1280, height, true);
        }
        File file = new File(resultDir, "shot-" + label + ".jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out);
            event("screenshot " + label + " saved");
        }
        catch (IOException e) {
            event("screenshot " + label + " save error: " + e);
        }
    }

    private void onLogLine(String line) {
        synchronized (logLock) {
            if (ended.get()) return;
            recordGameLine(line);
            if (wineLogBytes < MAX_WINE_LOG_BYTES) {
                try {
                    wineLog.write(line);
                    wineLog.write('\n');
                    wineLogBytes += line.length() + 1;
                }
                catch (IOException ignored) {}
            }
            else {
                wineLinesDropped++;
                wineTail.addLast(line);
                if (wineTail.size() > TAIL_LINES) wineTail.removeFirst();
            }
        }
    }

    private void recordGameLine(String line) {
        int colon = line.indexOf(':');
        if (colon != 4) return;
        String thread = line.substring(0, 4);
        if (!gameStarted) {
            if (line.contains("init_peb starting") && (line.contains("Game.exe") || line.contains("D2VidTst.exe"))) {
                gameStarted = true;
                gameThreads.add(thread);
            }
            else {
                knownThreads.add(thread);
                return;
            }
        }
        else if (!knownThreads.contains(thread)) gameThreads.add(thread);
        if (!gameThreads.contains(thread) || gameLogBytes >= MAX_GAME_LOG_BYTES) return;
        try {
            gameLog.write(line);
            gameLog.write('\n');
            gameLogBytes += line.length() + 1;
        }
        catch (IOException ignored) {}
    }

    private void event(String message) {
        String line = String.format(Locale.ROOT, "%7.1fs %s", (System.currentTimeMillis() - startMillis) / 1000f, message);
        synchronized (eventLock) {
            try {
                eventLog.write(line);
                eventLog.write('\n');
                eventLog.flush();
            }
            catch (IOException ignored) {}
        }
    }

    private static boolean isInteresting(Window window) {
        return window.isApplicationWindow() || !window.getName().isEmpty() || !window.getClassName().isEmpty();
    }

    private static String describe(Window window) {
        return String.format(Locale.ROOT, "id=%d name=\"%s\" class=\"%s\" %dx%d pid=%d app=%s dialog=%s",
            window.id, window.getName(), window.getClassName(), window.getWidth(), window.getHeight(),
            window.getProcessId(), window.isApplicationWindow(), window.isDialogBox());
    }

    private static void writeFile(File file, String text) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(text);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException ignored) {}
    }
}
