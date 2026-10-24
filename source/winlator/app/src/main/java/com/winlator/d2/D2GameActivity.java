package com.winlator.d2;

import com.winlator.R;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.winlator.core.AppUtils;
import com.winlator.core.EnvVars;
import com.winlator.core.ProcessHelper;
import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.XServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Executors;










public class D2GameActivity extends D2Activity {

    private static final int MENU_HELPERS = 0, MENU_KEYBOARD = 1, MENU_IDLE = 2, MENU_QUIT = 3, MENU_BUILD = 4, MENU_INTRO = 5,
        MENU_SWITCH = 6;

    public static final String EXTRA_DIR = "d2_dir";
    public static final String EXTRA_PROGRAM = "d2_program";
    public static final String EXTRA_ARGS = "d2_args";
    public static final String EXTRA_LOG = "d2_log";
    public static final String EXTRA_WINEDEBUG = "d2_winedebug";


    private D2GuestScreen screen;
    private XServer xServer;
    private WinHandler winHandler;
    private D2TouchOverlay overlay;
    private TextView loadingView;
    private BufferedWriter guestLog;
    private final com.winlator.core.Callback<String> guestLogCallback = this::writeGuestLog;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final D2StallWatch stallWatch = new D2StallWatch();
    private long stallResumedAt, dxvkLimitAt;

    private long presentsAtStart;






    private final Runnable stallTick = new Runnable() {
        @Override
        public void run() {
            long now = SystemClock.uptimeMillis();
            boolean watching = pausedAt == 0 && screen.isStarted() && D2GuestComponent.running()
                && !D2GuestComponent.guestsStopped() && !overlay.gamePaused() && now - stallResumedAt >= D2StallWatch.STALL_MS;
            long standing = stallWatch.check(now, watching,
                com.winlator.xserver.extensions.PresentExtension.presents - presentsAtStart);
            if (standing >= 0) {
                long stateAgo = overlay.lastModStateAt() > 0 ? now - overlay.lastModStateAt() : -1;
                new Thread(() -> traceStall(standing, stateAgo), "d2-stall").start();
            }
            if (now - dxvkLimitAt >= 60_000) {
                dxvkLimitAt = now;
                int emptied = D2DxvkLog.limit(D2Runtime.logFile(D2GameActivity.this, D2DxvkLog.DIR), D2DxvkLog.MAX_BYTES);
                if (emptied > 0) D2Trace.log("dxvk log: " + emptied + " file(s) over " + D2DxvkLog.MAX_BYTES + " bytes emptied");
            }
            if (pausedAt == 0) handler.postDelayed(this, 1000);
        }
    };


    private void traceStall(long standingMs, long stateAgoMs) {
        long now = SystemClock.uptimeMillis();
        D2Trace.log("stall: no new picture for " + standingMs / 1000 + " s while the game runs; last mod state "
            + (stateAgoMs >= 0 ? stateAgoMs / 1000 + " s ago" : "none yet") + "; presents "
            + com.winlator.xserver.extensions.PresentExtension.presents + ", last "
            + (now - com.winlator.xserver.extensions.PresentExtension.lastPresentAt) + " ms ago at "
            + com.winlator.xserver.extensions.PresentExtension.lastPresentWidth + "x"
            + com.winlator.xserver.extensions.PresentExtension.lastPresentHeight + ", notifies idle "
            + com.winlator.xserver.extensions.PresentExtension.idleNotifies + " complete "
            + com.winlator.xserver.extensions.PresentExtension.completeNotifies + " configure "
            + com.winlator.xserver.extensions.PresentExtension.configureNotifies + " (last "
            + (com.winlator.xserver.extensions.PresentExtension.lastConfigureAt > 0
                ? (now - com.winlator.xserver.extensions.PresentExtension.lastConfigureAt) + " ms ago, "
                    + com.winlator.xserver.extensions.PresentExtension.lastConfigureWidth + "x"
                    + com.winlator.xserver.extensions.PresentExtension.lastConfigureHeight
                : "none") + ")");
        File proc = new File("/proc");
        java.util.List<D2GuestProcs.Proc> guests = D2GuestProcs.find(proc, D2Runtime.rootDir(this), android.os.Process.myUid(),
            android.os.Process.myPid(), null);
        int[] pids = new int[guests.size()];
        for (int i = 0; i < pids.length; i++) pids[i] = guests.get(i).pid;
        int game = D2StallSnapshot.gamePid(proc, pids);
        if (game < 0) D2Trace.log("stall: no Game.exe among " + guests.size() + " guest processes");

        String dxvk = D2DxvkLog.tail(D2Runtime.logFile(this, D2DxvkLog.DIR), 5);
        if (dxvk.isEmpty()) D2Trace.log("stall: DXVK log empty");
        else for (String line : dxvk.split("\n")) D2Trace.log("stall: " + line);
        if (game < 0) return;
        for (String line : D2StallSnapshot.packed(D2StallSnapshot.threads(proc, game))) D2Trace.log("stall: Game.exe threads " + line);
    }
    private final Runnable suspendGuest = () -> screen.pauseGuest();

    private static D2GameActivity savingAfterRelaunch;

    private boolean afterRelaunch;

    private D2GameActivity nextWindow;

    private long newestAtPause;

    private volatile long pausedAt;
    private boolean exiting = false;

    private boolean exitRequested = false;

    private boolean exitOnlySaved = false;

    private File saveDir;

    private D2DirectStart.Request directRequest;

    private D2DirectLoading directLoading;

    private boolean switchCharacter;

    private boolean thenInstall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        D2SettingsLook.load(this, null);
        presentsAtStart = com.winlator.xserver.extensions.PresentExtension.presents;
        AppUtils.hideSystemUI(this);
        AppUtils.keepScreenOn(this);

        android.content.IntentFilter on = new android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_ON);
        on.addAction(android.content.Intent.ACTION_USER_PRESENT);
        registerReceiver(screenOn, on);
        screenOnRegistered = true;
        D2KeepAliveService.start(this, false);
        D2GameActivity saving = savingAfterRelaunch;
        if (saving != null) {

            afterRelaunch = true;
            saving.nextWindow = this;
            FrameLayout root = new FrameLayout(this);
            root.setBackgroundColor(Color.BLACK);
            TextView text = new TextView(this);
            text.setText(D2Text.get(this, R.string.relaunch_saving));
            text.setTextSize(18);
            D2Style.text(text);
            text.setGravity(android.view.Gravity.CENTER);
            int pad = Math.round(32 * getResources().getDisplayMetrics().density);
            text.setPadding(pad, pad, pad, pad);
            root.addView(text, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            setContentView(root);
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        ProcessHelper.removeAllDebugCallbacks();
        if (getIntent().getBooleanExtra(EXTRA_LOG, false)) openGuestLog();


        if (getIntent().getBooleanExtra(D2SpieltestPort.EXTRA, false)) {
            D2SpieltestPort.start(new java.io.File(new java.io.File(D2Runtime.driveC(this), D2Runtime.DRIVE_C_SHADOW), "d2android-mod.log"));
        }


        screen = new D2GuestScreen(this, root, true, true);
        xServer = screen.getXServer();
        winHandler = screen.getWinHandler();
        loadingView = screen.getLoadingView();



        overlay = new D2TouchOverlay(this, xServer, winHandler, this::openMenu);
        D2Text.setListener(() -> runOnUiThread(() -> { if (overlay != null) overlay.onLanguageChanged(); }));
        overlay.setKeyboard(this::setKeyboardShown);

        overlay.setModExpected(GameShadow.LOADER_EXE.equals(getIntent().getStringExtra(EXTRA_PROGRAM)));
        screen.addOverlay(overlay);


        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (overlay != null) overlay.setRightShift(navShift(insets));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
                && insets.isVisible(android.view.WindowInsets.Type.statusBars()) && hasWindowFocus()) {
                handler.removeCallbacks(hideBarsInsets);
                handler.postDelayed(hideBarsInsets, D2SystemBars.INSETS_MS);
            }
            return v.onApplyWindowInsets(insets);
        });
        String gameDir = getIntent().getStringExtra(D2LaunchActivity.EXTRA_GAME_DIR);
        if (gameDir != null) overlay.setGameDir(new File(gameDir));
        if (gameDir != null) saveDir = D2Saves.saveDir(new File(gameDir));

        if (gameDir != null && D2SpieltestPort.active()) D2SpieltestPort.saves(D2Saves.saveDir(new File(gameDir)));

        showDirectLoading(root);




        frameLimit = D2Settings.frameRate(this, D2Screen.refreshRates(this));
        displayRefresh = D2Screen.applyLimit(this, frameLimit);

        screen.getXServerView().getRenderer().setFrameLimit(frameLimit);
        D2Trace.log("renderer: at most " + (frameLimit > 0 ? frameLimit + " frames a second" : "as many frames as asked"));

        Executors.newSingleThreadExecutor().execute(this::startEnvironment);
    }


    private int frameLimit;
    private float displayRefresh = -1;

    private void startEnvironment() {
        D2Runtime.prepareSession(this);
        EnvVars env = D2Runtime.guestEnv(this);



        env.put("WINEDLLOVERRIDES", "dsound=b");
        env.put("ALSA_CONFIG_PATH", D2Runtime.rootDir(this).getPath() + "/opt/d2/alsa-d2.conf");
        env.put("D2_ALSA_RATE", "22050");

        D2Runtime.enableDxvkLog(this, env, getIntent().getBooleanExtra(EXTRA_LOG, false));



        String frameRate = D2FrameRate.envValue(frameLimit);
        if (frameRate != null) env.put(D2FrameRate.ENV, frameRate);
        if (env.has(D2FrameRate.ENV)) D2Trace.log("frame rate limit " + env.get(D2FrameRate.ENV) + " fps");

        if (displayRefresh > 0) D2Trace.log("display mode " + Math.round(displayRefresh) + " Hz for the setting of "
            + frameLimit + " fps");
        String wineDebug = getIntent().getStringExtra(EXTRA_WINEDEBUG);
        if (wineDebug != null && !wineDebug.isEmpty()) env.put("WINEDEBUG", wineDebug);

        String dir = getIntent().getStringExtra(EXTRA_DIR);
        String program = getIntent().getStringExtra(EXTRA_PROGRAM);
        String args = getIntent().getStringExtra(EXTRA_ARGS);
        String command = D2Runtime.guestCommand(xServer.screenInfo.toString(), dir != null ? dir : "C:\\windows",
            program != null ? program : "explorer.exe", args);
        writeGuestLog("[d2] " + command);


        java.io.File output = null;
        if (getIntent().getBooleanExtra(D2LaunchActivity.EXTRA_D2_LAUNCH, false)) {
            boolean log = getIntent().getBooleanExtra(EXTRA_LOG, false);
            output = D2GuestOutput.start(D2Runtime.logFile(this, D2GuestOutput.FILE),
                "game start " + new java.util.Date() + (log ? " (d2_log: the output is in guest.log)" : ""));
            if (log) output = null;
        }

        screen.start(command, env, null, (status) -> {

            D2ExitLog.gameEnded(status, backgroundMs(), D2GuestComponent.endReason(), D2GuestComponent.endSinceResumeMs());
            writeGuestLog("[d2] guest ended with status " + status);


            D2Trace.log("game ended with status " + status + " (Wine desktop)");
            D2Diagnose.keepTraceOfEndedRun(this);
            runOnUiThread(this::exit);
        }, output);
    }




    private void showDirectLoading(FrameLayout root) {
        directRequest = GameShadow.LOADER_EXE.equals(getIntent().getStringExtra(EXTRA_PROGRAM))
            ? D2DirectStart.Request.parse(getIntent().getStringExtra(D2DirectStart.EXTRA)) : null;
        if (directRequest == null) return;
        D2Trace.log("direct start: " + directRequest.toExtra());
        final String gameDir = getIntent().getStringExtra(D2LaunchActivity.EXTRA_GAME_DIR);
        directLoading = new D2DirectLoading(this, directRequest.isNew() ? null : directRequest.file, directRequest.difficulty, new D2DirectLoading.Host() {
            @Override public D2ModLink link() { return overlay != null ? overlay.modLink() : null; }
            @Override public boolean inFront() { return pausedAt == 0 && !isFinishing(); }
            @Override public File gameDir() { return gameDir != null ? new File(gameDir) : null; }
            @Override public String message(D2DirectStart.Outcome outcome, D2DirectStart.Report report) { return directMessage(outcome, report); }
            @Override public void arrived() {

                if (overlay != null) overlay.setPaused(true);
            }
            @Override public boolean worldReady() { return overlay != null && overlay.uiReady(); }
            @Override public void ended(D2DirectStart.Outcome outcome, D2DirectStart.Report report) { directEnded(outcome, report); }
        });
        root.addView(directLoading, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        directLoading.start();

        D2Music.keepForGame();
        com.winlator.alsaserver.ALSAMute.muteForLoading(true);
    }

    private String difficultyName(int difficulty) {
        int[] names = {R.string.difficulty_normal, R.string.difficulty_nightmare, R.string.difficulty_hell};
        return D2Text.get(this, names[Math.max(0, Math.min(2, difficulty))]);
    }


    private void directEnded(D2DirectStart.Outcome outcome, D2DirectStart.Report report) {
        boolean shown = directLoading != null && directLoading.messageShown();

        D2Music.fadeOut();
        com.winlator.alsaserver.ALSAMute.muteForLoading(false);
        directLoading = null;
        String text = directMessage(outcome, report);
        if (text != null) {

            if (!shown) Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            return;
        }
        if (outcome == D2DirectStart.Outcome.CANCELLED) return;
        watchGameLeft();
        if (directRequest.isNew()) return;

        final int wanted = directRequest.difficulty;
        handler.postDelayed(() -> {
            D2ResumeState place = overlay != null ? overlay.modLink().resumeState() : null;
            if (place == null || place.difficulty < 0 || place.difficulty > 2 || place.difficulty == wanted || isFinishing()) return;
            D2Trace.log("direct start: in the game in difficulty " + place.difficulty + ", wanted " + wanted);
            Toast.makeText(this, D2Text.get(this, R.string.direct_other_difficulty, difficultyName(place.difficulty), difficultyName(wanted)),
                Toast.LENGTH_LONG).show();
        }, 8000);
    }






    private void watchGameLeft() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (exiting || isFinishing()) return;
                D2DirectStart.Report report = overlay != null ? overlay.modLink().directReport() : null;
                if (report != null && report.state == D2DirectStart.LEFT) {
                    D2Trace.log("direct start: the game was left, back to the start page");
                    if (overlay != null) overlay.gameLeft();
                    D2ExitLog.quit("Spiel im Spiel verlassen, zurück zur Startseite");
                    switchCharacter = true;
                    exit();
                    return;
                }
                handler.postDelayed(this, 300);
            }
        }, 300);
    }


    private String directMessage(D2DirectStart.Outcome outcome, D2DirectStart.Report report) {
        if (outcome == D2DirectStart.Outcome.STOPPED) return D2Text.get(this, R.string.direct_stopped, directReason(report != null ? report.reason : 0));
        if (outcome == D2DirectStart.Outcome.NO_REPORT) return D2Text.get(this, R.string.direct_no_report);
        if (outcome == D2DirectStart.Outcome.TOO_LONG) return D2Text.get(this, R.string.direct_too_long);
        return null;
    }

    private String directReason(int reason) {
        switch (reason) {
            case D2DirectStart.REASON_MARKER: return D2Text.get(this, R.string.direct_reason_marker);
            case D2DirectStart.REASON_VERSION: return D2Text.get(this, R.string.direct_reason_version);
            case D2DirectStart.REASON_NOT_LISTED: return D2Text.get(this, R.string.direct_reason_not_listed);
            case D2DirectStart.REASON_DIFFICULTY: return D2Text.get(this, R.string.direct_reason_difficulty);
            case D2DirectStart.REASON_NO_PROGRESS: return D2Text.get(this, R.string.direct_reason_no_progress);
            case D2DirectStart.REASON_WINDOW: return D2Text.get(this, R.string.direct_reason_window);
            case D2DirectStart.REASON_NOT_PLAYABLE:
                return D2Text.get(this, directRequest != null && directRequest.isNew() ? R.string.direct_reason_new_failed
                    : R.string.direct_reason_not_playable);
            default: return D2Text.get(this, R.string.direct_reason_other, reason);
        }
    }






    private boolean directSwallows(KeyEvent event) {
        if (directLoading == null || !directLoading.showing()) return false;
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                directLoading.end(D2DirectStart.Outcome.CANCELLED, null);
                openMenu();
            }
            return true;
        }
        return code != KeyEvent.KEYCODE_VOLUME_UP && code != KeyEvent.KEYCODE_VOLUME_DOWN && code != KeyEvent.KEYCODE_VOLUME_MUTE;
    }



    private void openMenu() {
        float density = getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(pad, pad, pad, Math.round(8 * density));
        TextView title = new TextView(this);
        title.setText("Diablo II");
        title.setTextSize(20);
        D2Style.heading(title);
        list.addView(title);



        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setScrollbarFadingEnabled(false);
        scroll.setVerticalScrollBarEnabled(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.graphics.drawable.GradientDrawable thumb = new android.graphics.drawable.GradientDrawable();
            thumb.setColor(D2Style.GOLD);
            thumb.setCornerRadius(2 * density);
            scroll.setVerticalScrollbarThumbDrawable(thumb);
            scroll.setScrollBarSize(Math.round(4 * density));
        }
        scroll.addView(list);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(scroll).create();
        D2PadFocus.all(list);



        final boolean withIdle = com.winlator.BuildConfig.D2_INTERNAL;
        String idleBlocked = withIdle ? overlay.idleBlocked() : null;
        boolean pausedAtMenu = overlay.playerPausedNow();

        java.util.List<Integer> ids = new java.util.ArrayList<>();
        java.util.List<String> items = new java.util.ArrayList<>();
        ids.add(MENU_HELPERS);
        items.add(D2Text.get(this, R.string.menu_helpers));
        ids.add(MENU_BUILD);
        items.add(D2Text.get(this, R.string.menu_build));
        ids.add(MENU_INTRO);
        items.add(D2Text.get(this, R.string.menu_intro));
        ids.add(MENU_KEYBOARD);
        items.add(D2Text.get(this, R.string.menu_keyboard));
        if (withIdle) {
            ids.add(MENU_IDLE);
            items.add(idleBlocked == null ? D2Text.get(this, R.string.menu_idle) : D2Text.get(this, R.string.menu_idle_blocked, idleBlocked));
        }


        if (getIntent().getBooleanExtra(D2LaunchActivity.EXTRA_D2_LAUNCH, false) && GameShadow.LOADER_EXE.equals(getIntent().getStringExtra(EXTRA_PROGRAM))) {
            ids.add(MENU_SWITCH);
            items.add(D2Text.get(this, R.string.menu_switch_character));
        }
        ids.add(MENU_QUIT);
        items.add(D2Text.get(this, R.string.menu_quit));
        for (int i = 0; i < items.size(); i++) {
            final int which = ids.get(i);
            TextView item = new TextView(this);
            item.setText(items.get(i));
            item.setTextSize(18);
            D2Style.text(item);
            item.setPadding(0, Math.round(11 * density), 0, Math.round(11 * density));
            D2PadFocus.frame(item);
            if (which == MENU_IDLE && idleBlocked != null) {
                item.setAlpha(0.45f);
                list.addView(item);
                continue;
            }
            item.setOnClickListener((v) -> {
                dialog.dismiss();
                if (which == MENU_HELPERS) D2HelpersDialog.show(this, overlay);
                else if (which == MENU_BUILD) overlay.showBuildPage(this);
                else if (which == MENU_INTRO) overlay.showIntroAgain(pausedAtMenu);
                else if (which == MENU_KEYBOARD) AppUtils.showKeyboard(this);

                else if (com.winlator.BuildConfig.D2_INTERNAL && which == MENU_IDLE) D2IdleDialog.show(this, overlay.idleQuest(), (minutes, retreat) -> overlay.requestIdle(pausedAtMenu, minutes, retreat));
                else {
                    thenInstall = false;
                    confirmExit(which == MENU_SWITCH);
                }
            });
            list.addView(item);
        }
        dialog.setOnShowListener((d) -> AppUtils.hideSystemUI(this));
        immersiveDialog(dialog);
        dialog.show();
        styleDialog(dialog, 0.36f);
    }






    private static void immersiveDialog(AlertDialog dialog) {
        android.view.Window window = dialog.getWindow();
        if (window == null) return;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {


            android.view.WindowInsetsController controller = window.getDecorView().getWindowInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
        else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }





    private int navShift(android.view.WindowInsets insets) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            return D2NavInset.rightShift(insets.isVisible(android.view.WindowInsets.Type.ime()),
                insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.navigationBars()).right);
        }
        return D2NavInset.rightShift(overlay != null && overlay.keyboardShown(), insets.getStableInsetRight());
    }





    private void setKeyboardShown(boolean show) {
        View decor = getWindow().getDecorView();

        decor.post(decor::requestApplyInsets);
        decor.postDelayed(decor::requestApplyInsets, 500);
        android.view.inputmethod.InputMethodManager imm =
            (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return;
        if (!show) {

            imm.hideSoftInputFromWindow(decor.getWindowToken(), 0);
            return;
        }

        D2Keyboard.show(new D2Keyboard.Ime() {
            @Override
            public Boolean visible() {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return null;
                android.view.WindowInsets insets = decor.getRootWindowInsets();
                return insets != null && insets.isVisible(android.view.WindowInsets.Type.ime());
            }

            @Override
            public boolean hide(Runnable done) {
                return imm.hideSoftInputFromWindow(decor.getWindowToken(), 0, new android.os.ResultReceiver(handler) {
                    @Override
                    protected void onReceiveResult(int resultCode, android.os.Bundle resultData) {
                        done.run();
                    }
                });
            }

            @Override
            public void toggle() {
                imm.toggleSoftInput(android.view.inputmethod.InputMethodManager.SHOW_FORCED, 0);
            }

            @Override
            public void postDelayed(Runnable r, long ms) {
                handler.postDelayed(r, ms);
            }
        });
    }


    private void styleDialog(AlertDialog dialog, float widthFraction) {
        D2Sound.attach(dialog);
        android.view.Window window = dialog.getWindow();
        if (window == null) return;
        float density = getResources().getDisplayMetrics().density;
        window.setBackgroundDrawable(D2Style.windowBackground(density));
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        window.setLayout(Math.round(metrics.widthPixels * widthFraction), android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
    }

    private void confirmExit() {
        confirmExit(false);
    }


    private void confirmExit(boolean toLauncher) {
        TextView message = new TextView(this);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        message.setPadding(pad, pad, pad, 0);

        message.setText(overlay.materialsNotSaving() ? D2Text.get(this, R.string.quit_ask_not_saving)
            : overlay.canSaveBeforeExit()
            ? D2Text.get(this, R.string.quit_ask_saves)
            : D2Text.get(this, R.string.quit_ask_lose));
        message.setTextSize(16);
        D2Style.text(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(message)
            .setPositiveButton(D2Text.get(this, toLauncher ? R.string.menu_switch_character
                : thenInstall ? R.string.install_quit_anyway : R.string.menu_quit), (d, w) -> {
                switchCharacter = toLauncher;
                saveAndExitConfirmed();
            })
            .setNegativeButton(D2Text.get(this, R.string.quit_keep_playing), (d, w) -> thenInstall = false)
            .setOnCancelListener((d) -> thenInstall = false)
            .create();
        dialog.show();
        styleDialog(dialog, 0.42f);
    }





    private void saveAndExitOnlySaved() {
        if (!overlay.canSaveBeforeExit()) {
            D2Trace.log("save and exit (d2_nur_gespeichert): cannot save first, the game keeps running");
            D2ExitLog.note("Speichern und beenden (Spieltester) abgelehnt: Speichern nicht möglich, Spiel läuft weiter");
            return;
        }
        saveConfirmed((saved) -> {
            if (saved) {
                D2ExitLog.quit("Speichern und beenden (Spieltester), gespeichert (bestätigt)");
                exit();
                return;
            }

            loadingView.setVisibility(View.GONE);
            overlay.setPaused(true);
            D2Trace.log("save and exit (d2_nur_gespeichert): save not confirmed, the game keeps running");
            D2ExitLog.note("Speichern und beenden (Spieltester) abgebrochen: nicht gespeichert, Spiel läuft weiter");
        });
    }


    private long unconfirmedAt;
    private static final long SECOND_TAP_MS = 30_000;







    private void saveAndExit() {
        switchCharacter = false;

        if (overlay.materialsNotSaving()) {
            D2Trace.log("save and exit: the game cannot save (materials file), asking first");
            confirmExit();
            return;
        }


        if (thenInstall) {
            int request = ++installQuitRequest;
            installQuit(SystemClock.uptimeMillis(), request);
            return;
        }
        saveAndExitConfirmed();
    }


    private int installQuitRequest;


    private static final long INSTALL_WORD_MS = 2000;






    private void installQuit(long since, int request) {

        if (exiting || isFinishing() || isDestroyed() || !thenInstall || request != installQuitRequest) return;
        Boolean loses = overlay.quitLossNow();
        if (loses == null && SystemClock.uptimeMillis() - since < INSTALL_WORD_MS) {
            handler.postDelayed(() -> installQuit(since, request), 100);
            return;
        }
        if (loses == null || loses) {
            D2Trace.log("save and exit for the installation: " + (loses == null ? "no fresh word of the mod" : "cannot save first")
                + ", asking");
            confirmExit();
            return;
        }
        saveAndExitConfirmed();
    }


    private void saveAndExitConfirmed() {
        if (!overlay.canSaveBeforeExit()) {
            D2ExitLog.quit("Beenden ohne Speichern");
            exit();
            return;
        }
        if (unconfirmedAt != 0 && SystemClock.uptimeMillis() - unconfirmedAt < SECOND_TAP_MS) {
            D2ExitLog.quit("Speichern und beenden, Speichern NICHT bestätigt, beendet nach zweitem Tipp");
            exit();
            return;
        }
        saveConfirmed((saved) -> {
            if (saved) {
                D2ExitLog.quit("Speichern und beenden, gespeichert (bestätigt)");
                exit();
                return;
            }
            unconfirmedAt = SystemClock.uptimeMillis();
            loadingView.setVisibility(View.GONE);
            overlay.setPaused(true);
            D2ExitLog.note("Speichern und beenden: Speichern nicht bestätigt, nicht beendet");
            if (thenInstall) {
                askQuitUnconfirmed();
                return;
            }
            Toast.makeText(this, D2Text.get(this, R.string.quit_save_unconfirmed), Toast.LENGTH_LONG).show();
        });
    }





    private void askQuitUnconfirmed() {
        TextView message = new TextView(this);
        int pad = Math.round(16 * getResources().getDisplayMetrics().density);
        message.setPadding(pad, pad, pad, 0);
        message.setText(D2Text.get(this, R.string.install_quit_unconfirmed));
        message.setTextSize(16);
        D2Style.text(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(message)
            .setPositiveButton(D2Text.get(this, R.string.install_quit_anyway), (d, w) -> {
                D2ExitLog.quit("Speichern und beenden für die Installation, Speichern NICHT bestätigt, trotzdem beendet");
                exit();
            })
            .setNegativeButton(D2Text.get(this, R.string.quit_keep_playing), (d, w) -> {
                thenInstall = false;
                unconfirmedAt = 0;
            })
            .setCancelable(false)
            .create();
        dialog.show();
        styleDialog(dialog, 0.42f);
    }


    private static final long SAVE_CONFIRM_MS = 4000;






    private void saveConfirmed(java.util.function.Consumer<Boolean> done) {
        loadingView.setText(D2Text.get(this, R.string.game_saving));
        loadingView.setVisibility(View.VISIBLE);


        if (!overlay.sortSettled()) {
            final long since = SystemClock.uptimeMillis();
            D2Trace.log("save and exit: waiting for the inventory sort");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!overlay.sortSettled() && SystemClock.uptimeMillis() - since < SORT_SETTLE_MS) {
                        handler.postDelayed(this, 200);
                        return;
                    }
                    D2Trace.log("save and exit: sort " + (overlay.sortSettled() ? "over" : "NOT over, saving anyway")
                        + " after " + (SystemClock.uptimeMillis() - since) + " ms");
                    saveConfirmedSorted(done);
                }
            }, 200);
            return;
        }
        saveConfirmedSorted(done);
    }


    private static final long SORT_SETTLE_MS = 25000;

    private void saveConfirmedSorted(java.util.function.Consumer<Boolean> done) {


        if (overlay.heroDead()) {
            overlay.setPaused(false);
            final long since = SystemClock.uptimeMillis();
            final long[] escAt = {0};
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    long now = SystemClock.uptimeMillis();
                    if (overlay.heroDead() && now - since < 5000) {
                        if (now - escAt[0] >= 700 && overlay.respawn()) escAt[0] = now;
                        handler.postDelayed(this, 200);
                        return;
                    }
                    D2Trace.log("save and exit: hero " + (overlay.heroDead() ? "still dead" : "respawned"));
                    saveConfirmedAlive(done);
                }
            }, 300);
            return;
        }
        saveConfirmedAlive(done);
    }

    private void saveConfirmedAlive(java.util.function.Consumer<Boolean> done) {
        final long before = D2Saves.newestCharacterFile(saveDir);
        final long start = SystemClock.uptimeMillis();
        overlay.saveNow();
        D2Trace.log("save and exit: saving, newest save before " + before);
        handler.postDelayed(new Runnable() {
            private boolean toldIncomplete;

            @Override
            public void run() {


                File newest = D2Saves.newestCharacter(saveDir);
                boolean newer = newest != null && newest.lastModified() > before;
                if (newer && D2Saves.completeCharacter(newest)) {
                    D2Trace.log("save and exit: save confirmed after " + (SystemClock.uptimeMillis() - start) + " ms ("
                        + newest.length() + " bytes, complete)");
                    done.accept(true);
                }
                else if (SystemClock.uptimeMillis() - start >= SAVE_CONFIRM_MS) {
                    D2Trace.log("save and exit: " + (newer ? newest.getName() + " not complete (" + newest.length() + " bytes)"
                        : "no save file written") + " in " + SAVE_CONFIRM_MS + " ms");
                    done.accept(false);
                }
                else {
                    if (newer && !toldIncomplete) {
                        toldIncomplete = true;
                        D2Trace.log("save and exit: " + newest.getName() + " newer but not complete yet (" + newest.length()
                            + " bytes), waiting");
                    }
                    handler.postDelayed(this, newer ? 50 : 200);
                }
            }
        }, 200);
    }










    private void saveAndQuitAfterRelaunch() {
        D2Trace.log("window rebuilt by Android while the game runs: save, then quit");
        screen.resumeGuest();
        int chance = overlay.holdForRelaunch();
        Runnable quit = () -> {
            savingAfterRelaunch = null;
            D2GameActivity next = nextWindow;
            if (next != null) next.finishAndRemoveTask();
            exit();
        };
        if (chance != D2Relaunch.CAN_SAVE) {
            if (chance == D2Relaunch.NO_PLUGY) D2ExitLog.quit("Fenster von Android neu aufgebaut: beendet, Speichern nicht möglich (ohne PlugY)");
            else D2ExitLog.quit("Fenster von Android neu aufgebaut: beendet, noch kein Spielzustand (nichts zu speichern)");
            handler.post(quit);
            return;
        }
        final long since = SystemClock.uptimeMillis();
        final long baseline = D2Relaunch.baseline(pausedAt != 0 ? since - pausedAt : -1, newestAtPause, D2Saves.newestCharacterFile(saveDir));
        final Runnable confirm = new Runnable() {
            @Override
            public void run() {
                boolean saved = D2Relaunch.saved(D2Saves.newestCharacter(saveDir), baseline);
                if (!saved && SystemClock.uptimeMillis() - since < D2Relaunch.SORT_WAIT_MS + D2Relaunch.SAVE_WAIT_MS) {
                    handler.postDelayed(this, 200);
                    return;
                }
                if (saved) D2ExitLog.quit("Fenster von Android neu aufgebaut: gespeichert (bestätigt), beendet");
                else D2ExitLog.quit("Fenster von Android neu aufgebaut: Speichern NICHT bestätigt, beendet");

                handler.postDelayed(quit, saved ? D2Relaunch.SAVE_SETTLE_MS : 0);
            }
        };

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!overlay.sortSettled() && SystemClock.uptimeMillis() - since < D2Relaunch.SORT_WAIT_MS) {
                    handler.postDelayed(this, 100);
                    return;
                }
                if (!overlay.sortSettled()) D2Trace.log("relaunch: the inventory sort is not over, saving anyway");
                overlay.saveForRelaunch();
                handler.postDelayed(confirm, 200);
            }
        });
    }

    private void exit() {
        if (overlay != null && !exiting) overlay.beforeExit();
        if (exiting) return;
        exiting = true;
        if (overlay != null) overlay.endRelaunchSave();


        D2ExitGuard guard = D2ExitGuard.arm(D2ExitGuard.TEARDOWN_MS, this::endHungExit);
        screen.destroy(D2ExitText.BY_QUIT, guard);
        D2Trace.log("exit: teardown took " + guard.done() + " ms");
        D2FolderLock.releaseAll();
        closeGuestLog();


        D2Trace.log(switchCharacter ? "exit: back to the character list, the app ends the process" : "exit: the app ends the process");
        D2Diagnose.keepTraceOfEndedRun(this);
        D2KeepAliveService.stop(this);
        if (switchCharacter || thenInstall) {



            Intent launcher = Intent.makeRestartActivityTask(new android.content.ComponentName(this, D2LaunchActivity.class));
            launcher.putExtra(thenInstall && !switchCharacter ? D2KeepAliveService.EXTRA_THEN_INSTALL : D2LaunchActivity.EXTRA_CHARACTERS, true);
            startActivity(launcher);
        }
        else finishAndRemoveTask();
        Runtime.getRuntime().exit(0);
    }







    private void endHungExit(String step, long ms) {
        D2Trace.log("exit: teardown hangs for " + ms + " ms in " + step + ": guests and app process are ended");
        D2ExitLog.note("Beenden: Abbau hing " + ms + " ms bei " + step + ", Spiel und App hart beendet");
        int self = android.os.Process.myPid();
        java.util.List<D2GuestProcs.Proc> guests = D2GuestProcs.find(new File("/proc"), D2Runtime.rootDir(this),
            android.os.Process.myUid(), self, null);
        for (D2GuestProcs.Proc p : guests) android.os.Process.killProcess(p.pid);
        D2Trace.log("exit: " + guests.size() + " guest processes killed " + guests);
        D2Diagnose.keepTraceOfEndedRun(this);
        D2KeepAliveService.stop(this);
        try {
            if (switchCharacter || thenInstall) {
                Intent launcher = Intent.makeRestartActivityTask(new android.content.ComponentName(this, D2LaunchActivity.class));
                launcher.putExtra(thenInstall && !switchCharacter ? D2KeepAliveService.EXTRA_THEN_INSTALL : D2LaunchActivity.EXTRA_CHARACTERS, true);
                startActivity(launcher);
            }
            else finishAndRemoveTask();
        }
        catch (RuntimeException e) {
            D2Trace.log("exit: " + e + " while ending the hung game");
        }
        android.os.Process.killProcess(self);
    }




    @Override
    protected boolean clicks() {
        return false;
    }


    @Override
    protected boolean recreateOnLanguageChange() {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (directLoading != null) D2Music.gameInFront(this, true);
        if (afterRelaunch) return;

        final long presents = com.winlator.xserver.extensions.PresentExtension.presents;
        D2Trace.log("activity resume (guest started " + screen.isStarted() + ", presents so far " + presents + ")");
        D2FrameCounts.restart();
        handler.postDelayed(() -> {
            if (!isFinishing() && pausedAt == 0) D2Trace.log("5 s after resume: "
                + (com.winlator.xserver.extensions.PresentExtension.presents - presents) + " presents, renderer context "
                + com.winlator.renderer.GLRenderer.contextGeneration);
        }, 5000);


        long background = pausedAt != 0 ? SystemClock.uptimeMillis() - pausedAt : 0;
        long written = com.winlator.alsaserver.ALSAMute.unmute();
        if (background > 0) D2Trace.log("audio: unmuted after " + background + " ms in the background, " + written
            + " ms of sound written meanwhile");
        pausedAt = 0;
        handler.removeCallbacks(suspendGuest);

        stallResumedAt = SystemClock.uptimeMillis();
        handler.removeCallbacks(stallTick);
        handler.postDelayed(stallTick, 1000);
        AppUtils.hideSystemUI(this);
        screen.resume();
        D2KeepAliveService.start(this, false);
        if (exitRequested) {
            exitRequested = false;

            handler.postDelayed(exitOnlySaved ? this::saveAndExitOnlySaved : this::saveAndExit, 300);
        }
    }


    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        if (afterRelaunch) return;
        if (intent.getBooleanExtra(D2KeepAliveService.EXTRA_SAVE_AND_EXIT, false)) {
            exitRequested = true;
            exitOnlySaved = intent.getBooleanExtra(D2KeepAliveService.EXTRA_ONLY_SAVED, false);
            thenInstall = intent.getBooleanExtra(D2KeepAliveService.EXTRA_THEN_INSTALL, false);
        }
    }


    private final Runnable hideBars = () -> rehideBars("focus");

    private final Runnable hideBarsScreenOn = () -> rehideBars("screen on");
    private final Runnable hideBarsInsets = () -> rehideBars("insets");




    private static final long BARS_CHECK_MS = 1000;
    private final Runnable barsAfterToggle = () -> {
        if (isFinishing()) return;
        D2Trace.log("system bars: status bar " + (statusBarVisible() ? "VISIBLE" : "hidden") + " after the toggle");
    };

    private final Runnable hideBarsAfterToggle = () -> {
        if (isFinishing()) return;
        AppUtils.hideSystemUI(this);
        D2Trace.log("system bars: hidden after the toggle");
        handler.removeCallbacks(barsAfterToggle);
        handler.postDelayed(barsAfterToggle, BARS_CHECK_MS);
    };


    private boolean statusBarVisible() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return false;
        android.view.WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
        return insets != null && insets.isVisible(android.view.WindowInsets.Type.statusBars());
    }






    private void rehideBars(String why) {
        if (isFinishing() || afterRelaunch) return;
        if (!hasWindowFocus()) {

            if (barsScreenOn) D2Trace.log("system bars: " + why + " without the focus, waiting for it");
            return;
        }
        boolean visible = statusBarVisible();
        if (D2SystemBars.toggleFirst(visible, true, barsToggles, barsScreenOn) && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            barsToggles++;
            boolean anyway = barsScreenOn && !visible;
            barsScreenOn = false;
            android.view.WindowInsetsController controller = getWindow().getDecorView().getWindowInsetsController();

            if (controller != null) controller.show(android.view.WindowInsets.Type.statusBars());
            handler.removeCallbacks(hideBarsAfterToggle);
            handler.postDelayed(hideBarsAfterToggle, D2SystemBars.TOGGLE_MS);
            D2Trace.log("system bars: status bar " + (anyway ? "hidden in the insets (toggled anyway after screen on)" : "VISIBLE")
                + " after " + why + ", requested visible and hidden again");
            return;
        }
        AppUtils.hideSystemUI(this);

        if (visible && barsToggles <= D2SystemBars.MAX_TOGGLES) {
            boolean gaveUp = barsToggles == D2SystemBars.MAX_TOGGLES;
            if (gaveUp) barsToggles++;
            D2Trace.log("system bars: hidden again after " + why + ", status bar VISIBLE"
                + (gaveUp ? ", toggle did not help, plain hide" : ""));
        }
    }


    private int barsToggles;

    private boolean barsScreenOn;


    private final android.content.BroadcastReceiver screenOn = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            D2Trace.log("system bars: " + intent.getAction());
            barsToggles = 0;
            barsScreenOn = true;
            handler.removeCallbacks(hideBarsScreenOn);
            for (long delay : D2SystemBars.SCREEN_ON_MS) handler.postDelayed(hideBarsScreenOn, delay);
        }
    };
    private boolean screenOnRegistered;





    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        handler.removeCallbacks(hideBars);
        if (hasFocus) barsToggles = 0;
        for (long delay : D2SystemBars.hideAfterFocus(hasFocus, !afterRelaunch)) {
            if (delay == 0) hideBars.run();
            else handler.postDelayed(hideBars, delay);
        }
    }

    @Override
    public void onPause() {
        if (directLoading != null) D2Music.gameInFront(this, false);
        if (afterRelaunch) {
            super.onPause();
            return;
        }
        if (!isFinishing() && !exiting) D2KeepAliveService.start(this, true);
        if (saveDir != null) newestAtPause = D2Saves.newestCharacterFile(saveDir);
        super.onPause();
        pausedAt = SystemClock.uptimeMillis();
        handler.removeCallbacks(stallTick);


        com.winlator.alsaserver.ALSAMute.mute();
        D2Trace.log("activity pause (guest started " + screen.isStarted() + ")");
        if (screen.isStarted()) {

            long wait = !isFinishing() ? overlay.onAppPaused() : 0;
            if (wait > 0) handler.postDelayed(suspendGuest, wait);
            else screen.pauseGuest();
            screen.pauseView();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        noteWindow("stop", D2ExitText.WINDOW_STOPPED);
    }


    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        boolean running = D2GuestComponent.running();
        D2Trace.log("trim memory: level " + level + " (guest running " + running + ", " + backgroundMs() + " ms in the background)");

        if (running && D2ExitText.trimWorthNoting(level)) D2ExitLog.note(D2ExitText.trimMemory(level, backgroundMs()));
    }


    private long backgroundMs() {
        long at = pausedAt;
        return at != 0 ? SystemClock.uptimeMillis() - at : -1;
    }





    private void noteWindow(String event, String protocolEvent) {
        if (afterRelaunch) {
            D2Trace.log("activity " + event + " (rebuilt window without a game, finishing " + isFinishing() + ")");
            return;
        }
        long background = backgroundMs();
        boolean running = D2GuestComponent.running();
        D2Trace.log("activity " + event + " (finishing " + isFinishing() + ", changing configurations "
            + isChangingConfigurations() + ", guest started " + screen.isStarted() + ", guest running " + running
            + ", exiting " + exiting + ", " + background + " ms in the background)");
        if (running) D2ExitLog.note(D2ExitText.window(protocolEvent, background, isFinishing(), isChangingConfigurations(), exiting));
    }

    @Override
    protected void onDestroy() {
        if (screenOnRegistered) {
            screenOnRegistered = false;
            try {
                unregisterReceiver(screenOn);
            }
            catch (IllegalArgumentException ignored) {
            }
        }
        noteWindow("destroy", D2ExitText.WINDOW_CLOSED);
        if (afterRelaunch) {
            super.onDestroy();
            return;
        }
        D2Text.setListener(null);
        handler.removeCallbacks(suspendGuest);
        if (D2Relaunch.onDestroy(isChangingConfigurations(), screen.isStarted(), exiting) == D2Relaunch.SAVE_AND_QUIT) {

            savingAfterRelaunch = this;
            saveAndQuitAfterRelaunch();
            super.onDestroy();
            return;
        }
        screen.destroy(D2ExitText.BY_WINDOW_CLOSED);
        D2FolderLock.releaseAll();
        D2KeepAliveService.stop(this);
        closeGuestLog();
        D2SpieltestPort.stop();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (afterRelaunch) return;
        if (overlay != null && overlay.skipIntro()) return;
        openMenu();
    }

    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        if (screen == null) return super.dispatchGenericMotionEvent(event);
        if (directLoading != null && directLoading.showing()) return true;
        if (overlay != null && overlay.onPadMotion(event)) return true;
        if (overlay != null && overlay.onGenericMotionEvent(event)) return true;
        return screen.onGenericMotionEvent(event) || super.dispatchGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (screen == null) return super.dispatchKeyEvent(event);
        if (directSwallows(event)) return true;
        if (overlay != null) overlay.onRealKey(event);
        if (overlay != null && overlay.onPadKey(event)) return true;
        if (overlay != null && overlay.onKeyEvent(event)) return true;
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(event);
        return screen.onKeyEvent(event) || super.dispatchKeyEvent(event);
    }



    private void openGuestLog() {
        try {
            guestLog = new BufferedWriter(new FileWriter(D2Runtime.logFile(this, "guest.log")));
            ProcessHelper.addDebugCallback(guestLogCallback);
        }
        catch (IOException ignored) {}
    }

    private synchronized void writeGuestLog(String line) {
        if (guestLog == null) return;
        try {
            guestLog.write(line);
            guestLog.write('\n');
            guestLog.flush();
        }
        catch (IOException ignored) {}
    }

    private synchronized void closeGuestLog() {
        if (guestLog == null) return;
        ProcessHelper.removeDebugCallback(guestLogCallback);
        try {
            guestLog.close();
        }
        catch (IOException ignored) {}
        guestLog = null;
    }
}
