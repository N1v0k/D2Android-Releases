package com.winlator.d2;

import com.winlator.R;

import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.winlator.core.WineRegistryEditor;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class D2LaunchActivity extends D2Activity {
    public static final String EXTRA_D2_LAUNCH = "d2_launch";
    public static final String EXTRA_GAME_DIR = "d2_game_dir";
    public static final String EXTRA_CHARACTERS = "d2_characters";
    private static final String DEFAULT_GAME_FOLDER = GameLocator.DEFAULT_GAME_FOLDER;
    private static final String REG_KEY = "Software\\Blizzard Entertainment\\Diablo II";
    private static final String KEY_FIRST_BACKUP_DONE = GameLocator.KEY_FIRST_BACKUP_DONE;
    private static final String KEY_BACKED_UP_DIRS = GameLocator.KEY_BACKED_UP_DIRS;
    private static final int PERMISSION_REQUEST_CODE = 4712, BATTERY_REQUEST_CODE = 4713, FOLDER_REQUEST_CODE = 4714,
        NOTIFY_REQUEST_CODE = 4715;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean startPending;
    private volatile boolean startCancelled;
    private final Runnable autoStart = () -> {
        startPending = false;
        prepareAndLaunch();
    };
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView statusView;
    private Button playButton;
    private TextView folderView;
    private Button chooseButton;
    private LinearLayout installBlock;
    private Button installButton;
    private boolean openInstall;
    private boolean interruptedOpened;
    private LinearLayout firstStepBlock;
    private volatile boolean busy = false;
    private boolean spieltest;
    private D2ProgramRun programRun;
    private D2DirectStart.Request pendingDirect;
    private LinearLayout characterBlock;
    private D2StartPage startPage;
    private boolean startPageLoading, resumed;
    private android.view.View appPage;
    private boolean startPageExpected;
    private static final String KEY_START_PAGE = "startPageShown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        D2SettingsLook.load(this, null);
        D2ExitLog.systemExits(this);
        boolean saveAndExit = getIntent().getBooleanExtra(D2KeepAliveService.EXTRA_SAVE_AND_EXIT, false)
            || D2KeepAliveService.ACTION_SAVE_AND_EXIT.equals(getIntent().getAction());
        spieltest = D2SpieltestPort.requested(getIntent());
        programRun = D2ProgramRun.fromIntent(this, getIntent());
        if (com.winlator.BuildConfig.D2_INTERNAL) pendingDirect = D2DirectStart.Request.parse(getIntent().getStringExtra(D2DirectStart.EXTRA));
        if (D2InstallActivity.debugFromPatch(this, getIntent())) {
            startActivity(new Intent(this, D2InstallActivity.class).putExtra(D2InstallActivity.EXTRA_DEBUG_FROM_PATCH, true));
            finish();
            return;
        }
        if (D2TouchOverlay.isGameRunning()) {
            toRunningGame(saveAndExit);
            return;
        }
        if (saveAndExit) {
            finish();
            return;
        }
        D2KeepAliveService.createChannel(this);
        if (com.winlator.BuildConfig.D2_INTERNAL && getIntent().hasExtra(D2Intro.EXTRA_ADB)) {
            if (getIntent().getBooleanExtra(D2Intro.EXTRA_ADB, true)) D2Settings.requestIntro(this);
            else D2Settings.setIntroSeen(this, true);
        }
        D2Updates.start(this, getIntent());
        boolean news = D2Updates.newsAfterUpdate(this, getIntent());
        openInstall = D2LaunchStart.installAfterQuit(getIntent().getBooleanExtra(D2KeepAliveService.EXTRA_THEN_INSTALL, false),
            savedInstanceState != null, (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0);
        getIntent().removeExtra(D2KeepAliveService.EXTRA_THEN_INSTALL);
        buildUI();
        if (getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE).getBoolean(KEY_START_PAGE, false) && programRun == null
            && !updateShown && !openInstall) {
            startPageExpected = true;
            android.widget.FrameLayout black = new android.widget.FrameLayout(this);
            black.setBackgroundColor(android.graphics.Color.BLACK);
            setContentView(black);
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            handler.postDelayed(() -> {
                if (startPageExpected && startPage == null) noStartPage();
            }, 8000);
        }
        for (D2LaunchStart.Step step : D2LaunchStart.steps(hasStoragePermission(), savedInstanceState != null, news, updateShown)) {
            switch (step) {
                case ASK_STORAGE:
                    setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    status(getString(R.string.launch_asking_storage));
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                    break;
                case CHECK_FOLDER:
                    showCheckedFolder();
                    break;
                case NEWS:
                    startActivity(new Intent(this, D2NewsActivity.class));
                    break;
                case UPDATE_WAITING:
                    status(getString(R.string.update_waiting));
                    break;
                case LAUNCH:
                    if (openInstall) showCheckedFolder();
                    else if (getIntent().getBooleanExtra(EXTRA_CHARACTERS, false)) loadCharacters(false);
                    else if (!askBatteryOnce()) autoStartOrList();
                    break;
            }
        }
        if (openInstall && hasStoragePermission()) startActivity(new Intent(this, D2InstallActivity.class));
    }

    private void showCheckedFolder() {
        statusView.setText("");
        new Thread(() -> {
            File dir = GameLocator.gameDir(this);
            GameVersion.Result check = dir != null ? GameLocator.check(this, dir) : null;
            handler.post(() -> {
                if (busy || isFinishing()) return;
                if (check != null && stoppedInstall(dir, check)) {
                    showInterrupted(false);
                    return;
                }
                showInstall(check == null || (check.status != GameVersion.Status.OK && check.status != GameVersion.Status.MODIFIED));
                if (check == null) {
                    showNoGame();
                    return;
                }
                showFirstStep(false);
                showFolder(check.dir, check);
                statusView.setText(check.ok() ? "" : GameLocator.message(this, check));
                loadCharacters(false);
            });
        }, "FolderCheck").start();
    }

    private boolean askNotifications() {
        if (android.os.Build.VERSION.SDK_INT < 33) return false;
        if (androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) return false;
        if (getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE).getBoolean(KEY_NOTIFY_ASKED, false)) return false;
        D2KeepAliveService.createChannel(this);
        try {
            busy = true;
            startActivityForResult(new Intent(this, D2NotifyAskActivity.class), NOTIFY_REQUEST_CODE);
            overridePendingTransition(0, 0);
            return true;
        }
        catch (RuntimeException e) {
            busy = false;
            return false;
        }
    }

    private void notificationsAnswered(Intent data) {
        busy = false;
        boolean asked = data != null && data.getBooleanExtra(D2NotifyAskActivity.EXTRA_ASKED, false);
        boolean on = androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled();
        if (on || !asked) getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE).edit().putBoolean(KEY_NOTIFY_ASKED, true).apply();
        else if (++notifyTries < NOTIFY_TRIES) {
            if (askNotifications()) return;
        }
        else {
            notifyTries = 0;
            status(getString(R.string.launch_notify_open));
            return;
        }
        prepareAndLaunch();
    }

    private static final int NOTIFY_TRIES = 3;
    private int notifyTries;

    private static final String KEY_NOTIFY_ASKED = "notificationsAsked";

    private boolean askBatteryOnce() {
        SharedPreferences d2 = getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE);
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        if (power == null || power.isIgnoringBatteryOptimizations(getPackageName()) || d2.getBoolean("batteryAsked", false)) return false;
        d2.edit().putBoolean("batteryAsked", true).apply();
        try {
            startActivityForResult(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())), BATTERY_REQUEST_CODE);
            return true;
        }
        catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BATTERY_REQUEST_CODE) autoStartOrList();
        else if (requestCode == NOTIFY_REQUEST_CODE) notificationsAnswered(data);
        else if (requestCode == FOLDER_REQUEST_CODE) {
            D2Trace.log("folder picker answered: result " + resultCode + ", " + (data != null && data.getData() != null
                ? data.getData().getAuthority() : "no folder") + "");
            if (resultCode == RESULT_OK || data != null && data.getData() != null) folderPicked(data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST_CODE) return;
        if (hasStoragePermission()) {
            if (pickAfterPermission) {
                pickAfterPermission = false;
                chooseFolder();
            }
            else if (!askBatteryOnce()) autoStartOrList();
        }
        else status(getString(R.string.launch_no_storage));
    }

    private void buildUI() {
        int pad = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(D2Style.BACKGROUND);
        if (com.winlator.BuildConfig.D2_INTERNAL) root.addView(new D2TestBanner(this), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("Diablo II");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(D2Style.GOLD);
        root.addView(title);

        statusView = new TextView(this);
        statusView.setPadding(0, dp(12), 0, dp(12));
        statusView.setTextColor(D2Style.TEXT);
        statusView.setText(R.string.launch_preparing);
        root.addView(statusView, matchWidth());

        addFirstStepBlock(root);
        addFolderBlock(root);
        addInstallBlock(root);

        characterBlock = new LinearLayout(this);
        characterBlock.setOrientation(LinearLayout.VERTICAL);
        characterBlock.setVisibility(android.view.View.GONE);
        root.addView(characterBlock, matchWidth());

        playButton = new Button(this);
        playButton.setText(R.string.launch_play);
        playButton.setOnClickListener((v) -> {
            pendingDirect = null;
            prepareAndLaunch();
        });
        root.addView(playButton, matchWidth());
        addUpdateBlock(root);


        Button settings = new Button(this);
        settings.setText(R.string.settings_title);
        settings.setOnClickListener((v) -> startActivity(new Intent(this, D2SettingsActivity.class)));
        root.addView(settings, matchWidth());

        TextView notice = new TextView(this);
        notice.setText(R.string.about_disclaimer);
        notice.setTextSize(12);
        notice.setPadding(0, dp(16), 0, 0);
        D2Style.hint(notice);
        root.addView(notice, matchWidth());
        TextView licenses = new TextView(this);
        licenses.setText(R.string.licenses_link);
        licenses.setTextSize(12);
        licenses.setPadding(0, dp(6), 0, dp(6));
        licenses.setTextColor(D2Style.GOLD);
        licenses.setOnClickListener((v) -> startActivity(new Intent(this, D2LicensesActivity.class)));
        root.addView(licenses, matchWidth());

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setBackgroundColor(D2Style.BACKGROUND);
        scroll.addView(root);
        appPage = scroll;
        setContentView(scroll);
    }

    private boolean updateShown;

    private void addUpdateBlock(LinearLayout root) {
        D2UpdateCheck.Release release = D2Updates.pending(this);
        updateShown = release != null;
        if (release == null) return;
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(dp(12), dp(10), dp(12), dp(10));
        block.setBackground(D2Style.windowBackground(getResources().getDisplayMetrics().density));
        TextView head = new TextView(this);
        head.setText(getString(R.string.update_available, release.version));
        head.setTextSize(17);
        D2Style.heading(head);
        block.addView(head);
        String preview = D2UpdateCheck.preview(release.body, 4);
        if (!preview.isEmpty()) {
            TextView body = new TextView(this);
            body.setText(preview);
            body.setPadding(0, dp(4), 0, dp(4));
            D2Style.text(body);
            block.addView(body);
        }
        TextView obtainium = new TextView(this);
        obtainium.setText(R.string.update_obtainium);
        obtainium.setTextSize(12);
        D2Style.hint(obtainium);
        block.addView(obtainium);
        LinearLayout row = new LinearLayout(this);
        Button download = new Button(this);
        download.setText(R.string.update_download);
        download.setOnClickListener((v) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(release.url)));
            }
            catch (Exception e) {
                status(getString(R.string.update_no_browser));
            }
        });
        Button later = new Button(this);
        later.setText(R.string.update_later);
        later.setOnClickListener((v) -> {
            D2Updates.later(this, release);
            root.removeView(block);
            updateShown = false;
            status("");
        });
        Button news = new Button(this);
        news.setText(R.string.news_title);
        news.setOnClickListener((v) -> startActivity(new Intent(this, D2NewsActivity.class)));
        row.addView(download, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(news, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        row.addView(later, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        block.addView(row);
        LinearLayout.LayoutParams lp = matchWidth();
        lp.topMargin = dp(10);
        lp.bottomMargin = dp(10);
        root.addView(block, lp);
    }

    private void showNoGame() {
        String chosen = GameLocator.chosenPath(this);
        showFolder(null, null);
        if (chosen == null) {
            showFirstStep(true);
            statusView.setText("");
            return;
        }
        showFirstStep(false);
        showInstall(true);
        statusView.setText(getString(R.string.folder_gone, chosen));
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    private boolean stoppedInstall(File dir, GameVersion.Result check) {
        if (dir == null || !D2InstallPlan.unfinishedInstall(dir)) return false;
        if (!D2InstallFlow.inRoot(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), dir)) return false;
        boolean saves = D2InstallPlan.hasSaves(dir), passed = D2InstallActivity.checkPassed(this, dir);
        if (!saves && !passed) return true;
        return D2InstallFlow.launcherResumes(true, usable(check != null ? check : GameLocator.check(this, dir)), saves, passed);
    }

    private static boolean usable(GameVersion.Result check) {
        return check.status == GameVersion.Status.OK || check.status == GameVersion.Status.MODIFIED;
    }

    private void showInterrupted(boolean open) {
        startIfNoPage = false;
        startProblemOnAppPage();
        noStartPage();
        showFirstStep(false);
        showFolder(null, null);
        showInstall(true);
        if (installButton != null) installButton.setText(R.string.launch_install_resume);
        File dir = GameLocator.gameDir(this);
        status(getString(dir == null || D2InstallPlan.chosenReusable(dir) ? R.string.launch_install_interrupted
            : R.string.launch_install_interrupted_saves));
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        if (open && !isFinishing() && !isDestroyed()) startActivity(new Intent(this, D2InstallActivity.class));
    }

    private void showFirstStep(boolean show) {
        int first = show ? android.view.View.VISIBLE : android.view.View.GONE;
        int rest = show ? android.view.View.GONE : android.view.View.VISIBLE;
        if (firstStepBlock != null) firstStepBlock.setVisibility(first);
        if (show) {
            showInstall(false);
            setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
        if (folderView != null) folderView.setVisibility(rest);
        if (chooseButton != null) chooseButton.setVisibility(rest);
        if (playButton != null) playButton.setVisibility(rest);
    }

    private void addFirstStepBlock(LinearLayout root) {
        firstStepBlock = new LinearLayout(this);
        firstStepBlock.setOrientation(LinearLayout.VERTICAL);
        firstStepBlock.setVisibility(android.view.View.GONE);
        TextView heading = new TextView(this);
        heading.setText(R.string.launch_first_step_title);
        heading.setTextSize(18);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextColor(D2Style.GOLD);
        firstStepBlock.addView(heading, matchWidth());
        TextView text = new TextView(this);
        text.setText(R.string.launch_first_step_text);
        text.setPadding(0, dp(6), 0, dp(10));
        text.setTextColor(D2Style.TEXT);
        firstStepBlock.addView(text, matchWidth());
        Button install = new Button(this);
        install.setText(R.string.launch_install);
        install.setOnClickListener((v) -> {
            if (!busy) startActivity(new Intent(this, D2InstallActivity.class));
        });
        firstStepBlock.addView(install, matchWidth());
        Button choose = new Button(this);
        choose.setText(R.string.folder_choose);
        choose.setOnClickListener((v) -> chooseFolder());
        firstStepBlock.addView(choose, matchWidth());
        root.addView(firstStepBlock, matchWidth());
    }

    private void showInstall(boolean show) {
        if (installButton != null) installButton.setText(R.string.launch_install);
        if (installBlock != null) installBlock.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void addInstallBlock(LinearLayout root) {
        installBlock = new LinearLayout(this);
        installBlock.setOrientation(LinearLayout.VERTICAL);
        installBlock.setVisibility(android.view.View.GONE);
        TextView heading = new TextView(this);
        heading.setText(R.string.launch_install_heading);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextColor(D2Style.GOLD);
        heading.setPadding(0, dp(8), 0, dp(4));
        installBlock.addView(heading, matchWidth());

        Button install = new Button(this);
        install.setText(R.string.launch_install);
        install.setOnClickListener((v) -> {
            if (!busy) startActivity(new Intent(this, D2InstallActivity.class));
        });
        installButton = install;
        installBlock.addView(install, matchWidth());
        root.addView(installBlock, matchWidth());
    }


    @Override
    protected boolean recreateOnLanguageChange() {
        return !busy;
    }

    private void toRunningGame(boolean saveAndExit) {
        startActivity(new Intent(this, D2GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            .putExtra(D2KeepAliveService.EXTRA_SAVE_AND_EXIT, saveAndExit)
            .putExtra(D2KeepAliveService.EXTRA_ONLY_SAVED, getIntent().getBooleanExtra(D2KeepAliveService.EXTRA_ONLY_SAVED, false)));
        finish();
    }

    @Override
    public void onBackPressed() {
        boolean preparingGame = busy && programRun == null;
        if (startPending || preparingGame) {
            startCancelled = true;
            startPending = false;
            handler.removeCallbacks(autoStart);
            if (preparingGame) android.util.Log.i("D2Launch", "Back during the preparation: the game will not start");
            else android.util.Log.i("D2Launch", "Back before the start: game not started");
            finish();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(autoStart);
        startPending = false;
        super.onDestroy();
    }

    private void scheduleAutoStart() {
        if (startCancelled || isFinishing() || isDestroyed()) return;
        startPending = true;
        handler.postDelayed(autoStart, 300);
    }

    private void autoStartOrList() {
        if (pendingDirect != null || spieltest || programRun != null || !D2Settings.modEnabled(this)) {
            noStartPage();
            scheduleAutoStart();
            return;
        }
        loadCharacters(true);
    }

    private void loadCharacters(boolean startIfNone) {
        if (!D2Settings.modEnabled(this) || programRun != null) {
            noStartPage();
            if (startIfNone) scheduleAutoStart();
            return;
        }
        new Thread(() -> {
            File dir = GameLocator.gameDir(this);
            if (dir != null && stoppedInstall(dir, null)) {
                handler.post(() -> {
                    if (isFinishing() || isDestroyed() || startCancelled) return;
                    showInterrupted(startIfNone && !interruptedOpened);
                    if (startIfNone) interruptedOpened = true;
                });
                return;
            }
            java.util.List<D2SaveHeader.Info> characters = dir != null ? D2SaveHeader.list(D2Saves.saveDir(dir)) : new java.util.ArrayList<>();
            handler.post(() -> {
                if (isFinishing() || isDestroyed() || startCancelled) return;
                boolean offer = D2CharacterList.anyStartable(characters);
                showCharacters(offer ? characters : null);
                if (offer) return;
                if (dir != null && !updateShown && !startPageGaveUp) {
                    startIfNoPage = startIfNone;
                    if (android.text.TextUtils.equals(statusView.getText(), getText(R.string.launch_preparing))) statusView.setText("");
                    loadStartPage(characters);
                    return;
                }
                noStartPage();
                if (startIfNone && !busy) scheduleAutoStart();
            });
        }, "CharacterScan").start();
    }

    private void showCharacters(java.util.List<D2SaveHeader.Info> characters) {
        if (characterBlock == null) return;
        if (characters == null) {
            characterBlock.setVisibility(android.view.View.GONE);
            playButton.setText(R.string.launch_play);
            return;
        }
        D2CharacterList.fill(this, characterBlock, characters, (character, difficulty) -> {
            if (busy) return;
            pendingDirect = new D2DirectStart.Request(character.file, difficulty);
            prepareAndLaunch();
        });
        characterBlock.setVisibility(android.view.View.VISIBLE);
        playButton.setText(R.string.launch_play_menu);
        if (android.text.TextUtils.equals(statusView.getText(), getText(R.string.launch_preparing))) statusView.setText("");
        if (!updateShown) loadStartPage(characters);
        else noStartPage();
    }


    private void loadStartPage(java.util.List<D2SaveHeader.Info> characters) {
        if (startPage != null || startPageLoading) return;
        startPageLoading = true;
        final int gen = startPageGeneration;
        new Thread(() -> {
            File dir = GameLocator.gameDir(this);
            if (dir == null) {
                handler.post(this::noStartPage);
                return;
            }
            try (D2Sprites sprites = new D2Sprites(dir)) {
                D2StartArt art = D2StartArt.load(sprites);
                try {
                    D2LevelNames names = D2LevelNames.load(sprites);
                    for (D2SaveHeader.Info c : characters) {
                        String level = D2ResumeArea.levelName(this, dir, c, c.defaultDifficulty(), names);
                        if (level != null) art.levels.put(c.file, level);
                    }
                }
                catch (RuntimeException e) {
                    art.levels.clear();
                    D2Trace.log("no level names on the cards: " + e);
                }
                for (D2SaveHeader.Info c : characters) art.classFigure(c.characterClass);
                handler.post(() -> {
                    if (gen != startPageGeneration) return;
                    startPageFolder = dir.getAbsolutePath();
                    showStartPage(art, characters);
                });
                java.util.Map<String, D2CharPortrait.Item> items = D2CharPortrait.items(sprites);
                D2CharPortrait.Files files = D2CharPortrait.of(sprites);
                for (D2SaveHeader.Info c : characters) {
                    D2Sprites.Frame[] still = art.classFigure(c.characterClass);
                    if (still != null) post(gen, c.file, new D2StartPage.Figure(new android.graphics.Bitmap[]{bitmap(still[0])}, 0));
                }
                File saves = D2Saves.saveDir(dir);
                for (D2SaveHeader.Info c : characters) {
                    if (isFinishing() || isDestroyed() || startPageGaveUp || gen != startPageGeneration) break;
                    try {
                        byte[] save = java.nio.file.Files.readAllBytes(new File(saves, c.file + ".d2s").toPath());
                        D2CharPortrait.Anim anim = D2CharPortrait.compose(save, files, items);
                        if (anim == null || anim.frames.length == 0) continue;
                        android.graphics.Bitmap[] frames = new android.graphics.Bitmap[anim.frames.length];
                        for (int i = 0; i < frames.length; i++) {
                            frames[i] = android.graphics.Bitmap.createBitmap(anim.frames[i], anim.width, anim.height, android.graphics.Bitmap.Config.ARGB_8888);
                        }
                        post(gen, c.file, new D2StartPage.Figure(frames, Math.max(40, anim.frameMs)));
                    }
                    catch (IOException | RuntimeException e) {
                        D2Trace.log("hero " + c.file + " not composed: " + e);
                    }
                }
            }
            catch (IOException | RuntimeException e) {
                D2Trace.log("start page not loaded, the launcher keeps its app look: " + e);
                handler.post(() -> {
                    if (gen == startPageGeneration) noStartPage();
                });
            }
        }, "StartPage").start();
    }

    private void noStartPage() {
        getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE).edit().putBoolean(KEY_START_PAGE, false).apply();
        if (startIfNoPage && startPage == null) {
            startIfNoPage = false;
            if (!busy) scheduleAutoStart();
        }
        if (!startPageExpected || startPage != null || isFinishing() || isDestroyed()) return;
        startPageExpected = false;
        startPageGaveUp = true;
        setContentView(appPage);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private boolean startIfNoPage;

    private volatile boolean startPageGaveUp;

    private void startProblemOnAppPage() {
        if (startPage == null) return;
        startPage.setRunning(false);
        startPage = null;
        startPageGaveUp = true;
        setContentView(appPage);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    private static android.graphics.Bitmap bitmap(D2Sprites.Frame f) {
        return android.graphics.Bitmap.createBitmap(f.argb, f.width, f.height, android.graphics.Bitmap.Config.ARGB_8888);
    }

    private void post(int gen, String file, D2StartPage.Figure figure) {
        handler.post(() -> {
            if (startPage != null && gen == startPageGeneration) startPage.setFigure(file, figure);
        });
    }

    private String startPageFolder;
    private volatile int startPageGeneration;

    private void reloadStartPage() {
        D2Trace.log("start page: the game folder changed, " + D2FolderPick.traceName(startPageFolder) + " -> "
            + D2FolderPick.traceName(GameLocator.chosenPath(this)) + ", built again");
        startPageGeneration++;
        if (startPage != null) startPage.setRunning(false);
        startPage = null;
        startPageLoading = false;
        startPageFolder = null;
        startPageGaveUp = false;
        startPageExpected = true;
        if (GameLocator.gameDir(this) == null) noStartPage();
        else {
            android.widget.FrameLayout black = new android.widget.FrameLayout(this);
            black.setBackgroundColor(android.graphics.Color.BLACK);
            setContentView(black);
            handler.postDelayed(() -> {
                if (startPageExpected && startPage == null) noStartPage();
            }, 8000);
        }
        showCheckedFolder();
    }

    private void showStartPage(D2StartArt art, java.util.List<D2SaveHeader.Info> characters) {
        if (isFinishing() || isDestroyed() || startPage != null || startPageGaveUp) return;
        startIfNoPage = false;
        startPage = new D2StartPage(this, art, characters, new D2StartPage.Host() {
            @Override public void start(D2SaveHeader.Info character, int difficulty) {
                if (busy) return;
                pendingDirect = new D2DirectStart.Request(character.file, difficulty);
                prepareAndLaunch();
            }
            @Override public void longPress(D2SaveHeader.Info character) {
                if (!busy) chooseDifficulty(character);
            }
            @Override public void refused(D2SaveHeader.Info character) {
                Toast.makeText(D2LaunchActivity.this, character.refusal() == D2SaveHeader.Refusal.DEAD_HARDCORE
                    ? R.string.launch_character_dead : R.string.launch_character_menu_only, Toast.LENGTH_SHORT).show();
            }
            @Override public void button(int which) {
                startPageButton(which);
            }
        });
        startPage.setStatus(statusView.getText().toString());
        setContentView(startPage);
        startPageExpected = false;
        getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE).edit().putBoolean(KEY_START_PAGE, true).apply();
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        startPage.setRunning(resumed);
    }

    private void startPageButton(int which) {
        switch (which) {
            case 0:
                if (busy) return;
                pendingDirect = D2DirectStart.Request.NEW;
                prepareAndLaunch();
                break;
            case 1:
                startActivity(new Intent(this, D2SettingsActivity.class));
                break;
            case 2:
                startActivity(new Intent(this, D2SettingsActivity.class).putExtra(D2SettingsActivity.EXTRA_SAVES, true));
                break;
            case 3:
                startActivity(new Intent(this, D2NewsActivity.class));
                break;
            case 4:
                startActivity(new Intent(this, D2BackgroundActivity.class));
                break;
            case 6:
                if (busy) return;
                pendingDirect = null;
                prepareAndLaunch();
                break;
            default:
                startActivity(new Intent(this, D2LicensesActivity.class));
        }
    }

    private void chooseDifficulty(D2SaveHeader.Info character) {
        int open = character.openDifficulty();
        if (open == 0) {
            pendingDirect = new D2DirectStart.Request(character.file, 0);
            prepareAndLaunch();
            return;
        }
        String[] names = new String[open + 1];
        for (int d = 0; d <= open; d++) names[d] = D2StartPage.difficultyName(this, d);
        float density = getResources().getDisplayMetrics().density;
        int pad = Math.round(16 * density);
        TextView title = new TextView(this);
        title.setText(getString(R.string.start_choose_difficulty, character.name));
        title.setTextSize(18);
        D2Style.heading(title);
        title.setPadding(pad, pad, pad, Math.round(4 * density));
        android.widget.ArrayAdapter<String> rows = new android.widget.ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, names) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                TextView row = (TextView) super.getView(position, convertView, parent);
                row.setTextColor(D2Style.TEXT);
                return row;
            }
        };
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setCustomTitle(title)
            .setAdapter(rows, (d, which) -> {
                if (busy) return;
                pendingDirect = new D2DirectStart.Request(character.file, which);
                prepareAndLaunch();
            })
            .create();
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(density));
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        if (pendingPick != null) {
            Runnable pick = pendingPick;
            pendingPick = null;
            pick.run();
        }
        if (!busy && D2FolderPick.reloadStartPage(startPageFolder, GameLocator.chosenPath(this), startPage != null)) reloadStartPage();
        else if (startPage != null) startPage.setRunning(true);
    }

    @Override
    protected void onPause() {
        resumed = false;
        if (startPage != null) startPage.setRunning(false);
        super.onPause();
    }

    private void prepareAndLaunch() {
        if (busy || startCancelled || isFinishing() || isDestroyed()) return;
        if (askNotifications()) return;
        if (!deviceCheckPassed()) return;
        if (programRun != null) {
            runProgram();
            return;
        }
        File found = GameLocator.gameDir(this);
        if (found == null) {
            startProblemOnAppPage();
            showNoGame();
            return;
        }
        showFirstStep(false);
        GameVersion.Result check = GameLocator.check(this, found);
        showFolder(check.dir, check);
        if (stoppedInstall(found, check)) {
            showInterrupted(true);
            return;
        }
        if (check.status == GameVersion.Status.MODIFIED && !GameLocator.accepted(this, check)) {
            startProblemOnAppPage();
            status(GameLocator.message(this, check));
            confirmChangedFiles(check);
            return;
        }
        boolean usable = check.status == GameVersion.Status.OK || check.status == GameVersion.Status.MODIFIED;
        showInstall(!usable);
        if (!usable) {
            startProblemOnAppPage();
            status(GameLocator.message(this, check));
            return;
        }
        final File gameDir = check.dir;
        if (D2Runtime.bundledVersion(this) == null) {
            startProblemOnAppPage();
            status(getString(R.string.launch_no_runtime));
            return;
        }
        if (!GameShadow.isD2DXBundled(this)) {
            startProblemOnAppPage();
            status(getString(R.string.launch_no_d2dx));
            return;
        }
        D2LaunchGate.Refused refused = D2LaunchGate.beginGame(D2TouchOverlay.isGameRunning());
        if (refused != null) android.util.Log.i("D2Launch", "launcher refused: " + refused);
        if (refused == D2LaunchGate.Refused.GAME_RUNNING) {
            toRunningGame(false);
            return;
        }
        if (refused == D2LaunchGate.Refused.GAME_HANDED_OVER) {
            finish();
            return;
        }
        if (refused != null) {
            status(getString(launchRefusedText(refused)));
            return;
        }
        busy = true;
        playButton.setEnabled(false);
        final D2DirectStart.Request direct = pendingDirect;
        final String screenSize = D2Runtime.screenSize(this);
        worker.execute(() -> {
            D2FolderLock.Decision folderLock = D2FolderLock.acquire(getPackageName(), D2SettingsActivity.appLabel(this), gameDir, "game");
            if (folderLock.foreign) {
                D2LaunchGate.gameFailed();
                status(D2SettingsActivity.folderLockText(this, folderLock, true));
                handler.post(() -> {
                    busy = false;
                    playButton.setEnabled(true);
                });
                return;
            }
            try {
                if (!D2Runtime.isInstalled(this)) D2Runtime.install(this, this::status);
                if (!D2Runtime.isPrefixReady(this)) D2Runtime.setupPrefix(this, this::status);
                status(getString(R.string.launch_starting));
                if (D2Saves.recover(D2Saves.saveDir(gameDir))) D2SaveRepair.run(D2Saves.saveDir(gameDir));
                backupOnce(gameDir);
                D2Strings.rememberGameLanguage(this, gameDir);
                String d2dxCfg = GameShadow.buildD2DXConfig(D2Settings.gameSize(screenSize, D2Settings.gameLines(this)), 0);
                File startDir = GameShadow.prepare(this, D2Runtime.driveC(this), gameDir, d2dxCfg);
                tellRepairs(D2SaveRepair.takeLast());
                writeRegistry(startDir, gameDir);
                boolean useMod = D2Settings.modEnabled(this) && new File(startDir, GameShadow.LOADER_EXE).isFile();
                D2DirectStart.Request directStart = useMod ? direct : null;
                try {
                    D2DirectStart.apply(startDir, directStart);
                }
                catch (IOException e) {
                    D2Trace.log("direct start marker: " + e.getMessage());
                    directStart = null;
                }
                try {
                    D2IntroMovies.apply(startDir, directStart != null || D2Settings.skipIntro(this));
                }
                catch (IOException e) {
                    D2Trace.log("intro marker not set: " + e.getMessage());
                }

                Intent intent = new Intent(this, D2GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra(D2GameActivity.EXTRA_DIR, D2Runtime.dosPath(this, startDir));
                intent.putExtra(D2GameActivity.EXTRA_PROGRAM, useMod ? GameShadow.LOADER_EXE : "Game.exe");
                intent.putExtra(D2GameActivity.EXTRA_ARGS, "-3dfx");
                intent.putExtra(EXTRA_D2_LAUNCH, true);
                intent.putExtra(EXTRA_GAME_DIR, gameDir.getPath());
                if (directStart != null) intent.putExtra(D2DirectStart.EXTRA, directStart.toExtra());
                intent.putExtra(D2GameActivity.EXTRA_LOG, com.winlator.BuildConfig.D2_INTERNAL && getIntent().getBooleanExtra(D2GameActivity.EXTRA_LOG, false));
                intent.putExtra(D2SpieltestPort.EXTRA, spieltest);
                intent.putExtra(D2TouchOverlay.EXTRA_MERC_POTIONS, D2TouchOverlay.mercPotionsTestAllowed(this, getIntent()));
                String wineDebug = com.winlator.BuildConfig.D2_INTERNAL ? getIntent().getStringExtra(D2GameActivity.EXTRA_WINEDEBUG) : null;
                if (wineDebug != null) intent.putExtra(D2GameActivity.EXTRA_WINEDEBUG, wineDebug);
                handler.post(() -> {
                    if (startCancelled || isFinishing()) {
                        D2LaunchGate.gameFailed();
                        D2FolderLock.release(gameDir);
                        android.util.Log.i("D2Launch", "launcher closed during the preparation: game not started");
                        return;
                    }
                    D2LaunchGate.gameStarted();
                    startActivity(intent);
                    finish();
                });
            }
            catch (Exception e) {
                D2LaunchGate.gameFailed();
                D2FolderLock.release(gameDir);
                android.util.Log.e("D2Launch", "preparation failed", e);
                status(getString(R.string.launch_failed, e.getMessage()));
                handler.post(() -> {
                    busy = false;
                    playButton.setEnabled(true);
                });
            }
        });
    }

    private static int launchRefusedText(D2LaunchGate.Refused refused) {
        switch (refused) {
            case IMPORT_RUNNING: return R.string.launch_import_running;
            case INSTALL_SETUP: return R.string.launch_install_setup;
            case INSTALL_RUNNING: return R.string.launch_install_running;
            default: return R.string.launch_already_starting;
        }
    }

    private void runProgram() {
        D2LaunchGate.Refused refused = D2LaunchGate.beginProgram();
        if (refused != null) {
            status(getString(launchRefusedText(refused)));
            return;
        }
        busy = true;
        playButton.setEnabled(false);
        worker.execute(() -> {
            try {
                if (!D2Runtime.isInstalled(this)) D2Runtime.install(this, this::status);
                if (!D2Runtime.isPrefixReady(this)) D2Runtime.setupPrefix(this, this::status);
                status(getString(R.string.launch_starting_program, programRun.program));
                Intent intent = new Intent(this, D2GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.putExtra(D2GameActivity.EXTRA_DIR, programRun.dir);
                intent.putExtra(D2GameActivity.EXTRA_PROGRAM, programRun.program);
                if (programRun.args != null) intent.putExtra(D2GameActivity.EXTRA_ARGS, programRun.args);
                intent.putExtra(D2GameActivity.EXTRA_LOG, programRun.log);
                intent.putExtra(D2GameActivity.EXTRA_LOG_NAME, "programrun.log");
                String wineDebug = getIntent().getStringExtra(D2GameActivity.EXTRA_WINEDEBUG);
                if (wineDebug != null) intent.putExtra(D2GameActivity.EXTRA_WINEDEBUG, wineDebug);
                D2LaunchGate.started();
                handler.post(() -> {
                    startActivity(intent);
                    finish();
                });
            }
            catch (Exception e) {
                D2LaunchGate.failed();
                android.util.Log.e("D2Launch", "program run failed", e);
                status(getString(R.string.launch_start_failed, e.getMessage()));
                handler.post(() -> {
                    busy = false;
                    playButton.setEnabled(true);
                });
            }
        });
    }

    private void tellRepairs(java.util.List<D2SaveRepair.Repair> repairs) {
        if (repairs.isEmpty()) return;
        StringBuilder text = new StringBuilder();
        for (D2SaveRepair.Repair r : repairs) {
            String line;
            switch (r.kind) {
                case STASH_RENAMED: line = getString(R.string.repair_stash, strip(r.name)); break;
                case SHARED_STASH_RENAMED: line = getString(R.string.repair_shared_stash); break;
                case CHARACTER_RESTORED: line = getString(R.string.repair_character, r.name, backupTime(r.backup)); break;
                case CHARACTER_HARDCORE: line = getString(R.string.repair_character_hardcore, r.name, r.folder); break;
                default: line = getString(R.string.repair_character_broken, r.name, D2Saves.BACKUP_FOLDER); break;
            }
            D2ExitLog.note(" " + line);
            text.append(text.length() > 0 ? "\n" : "").append(line);
        }
        handler.post(() -> Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show());
    }

    private static String strip(String file) {
        int dot = file.lastIndexOf('.');
        return dot > 0 ? file.substring(0, dot) : file;
    }

    private static String backupTime(String time) {
        if (time == null || time.length() != 15) return String.valueOf(time);
        return time.substring(6, 8) + "." + time.substring(4, 6) + ". " + time.substring(9, 11) + ":" + time.substring(11, 13);
    }

    private void backupOnce(File gameDir) throws IOException {
        SharedPreferences d2 = getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE);
        String path = gameDir.getAbsolutePath();
        java.util.Set<String> done = new java.util.HashSet<>(d2.getStringSet(KEY_BACKED_UP_DIRS, java.util.Collections.emptySet()));
        File defaultDir = GameLocator.resolve(DEFAULT_GAME_FOLDER);
        if (done.isEmpty() && d2.getBoolean(KEY_FIRST_BACKUP_DONE, false) && defaultDir != null) done.add(defaultDir.getAbsolutePath());
        if (done.contains(path)) return;
        try {
            D2Saves.backup(D2Saves.saveDir(gameDir), "vor-erstem-Start");
        }
        catch (IOException e) {
            throw new IOException(getString(R.string.launch_backup_failed, D2SettingsActivity.savesError(this, e)));
        }
        done.add(path);
        d2.edit().putBoolean(KEY_FIRST_BACKUP_DONE, true).putBoolean(D2Settings.KEY_PLUGY_BACKUP_DONE, true)
            .putStringSet(KEY_BACKED_UP_DIRS, done).apply();
    }

    private void writeRegistry(File startDir, File gameDir) {
        String dosDir = D2Runtime.dosPath(this, startDir);
        String dosGameDir = D2Runtime.dosPath(this, gameDir);
        File prefix = D2Runtime.prefixDir(this);
        try (WineRegistryEditor user = new WineRegistryEditor(new File(prefix, "user.reg"))) {
            user.setCreateKeyIfNotExist(true);
            user.setStringValue(REG_KEY, "InstallPath", dosDir + "\\");
            user.setStringValue(REG_KEY, "Save Path", dosGameDir + (dosGameDir.endsWith("\\") ? "" : "\\") + "save\\");
            if (user.getDwordValue(REG_KEY + "\\VideoConfig", "Render") == null) {
                user.setDwordValue(REG_KEY + "\\VideoConfig", "Render", 0);
            }
            user.setDwordValue(REG_KEY, "Mini Panel", 1);
        }
        try (WineRegistryEditor system = new WineRegistryEditor(new File(prefix, "system.reg"))) {
            system.setCreateKeyIfNotExist(true);
            system.setStringValue("Software\\Wow6432Node\\Blizzard Entertainment\\Diablo II", "InstallPath", dosDir + "\\");
            system.setStringValue(REG_KEY, "InstallPath", dosDir + "\\");
        }
    }


    private void addFolderBlock(LinearLayout root) {
        folderView = new TextView(this);
        D2Style.hint(folderView);
        root.addView(folderView, matchWidth());
        chooseButton = new Button(this);
        chooseButton.setText(R.string.folder_choose);
        chooseButton.setOnClickListener((v) -> chooseFolder());
        root.addView(chooseButton, matchWidth());
    }

    private boolean pickAfterPermission;

    private void chooseFolder() {
        if (busy) return;
        if (!hasStoragePermission()) {
            pickAfterPermission = true;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return;
        }
        try {
            startActivityForResult(GameLocator.pickerIntent(), FOLDER_REQUEST_CODE);
        }
        catch (ActivityNotFoundException e) {
            status(getString(R.string.folder_no_picker));
        }
    }

    private void confirmChangedFiles(GameVersion.Result check) {
        styledDialog(getString(R.string.folder_modified_title), GameLocator.message(this, check), getString(R.string.folder_start_anyway), () -> {
            GameLocator.accept(this, check);
            prepareAndLaunch();
        });
    }

    private void styledDialog(String titleText, CharSequence messageText, String positive, Runnable onPositive) {
        int pad = dp(16);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, 0);
        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(18);
        D2Style.heading(title);
        content.addView(title);
        TextView message = new TextView(this);
        message.setText(messageText);
        message.setTextSize(16);
        message.setPadding(0, dp(8), 0, 0);
        D2Style.text(message);
        content.addView(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(content)
            .setPositiveButton(positive, (d, which) -> onPositive.run())
            .setNegativeButton(R.string.cancel, null)
            .create();
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
            window.setLayout(Math.round(getResources().getDisplayMetrics().widthPixels * 0.6f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
    }

    private void showFolder(File dir, GameVersion.Result check) {
        if (folderView == null) return;
        String path = dir != null ? dir.getAbsolutePath() : GameLocator.chosenPath(this);
        if (path == null) {
            folderView.setText("");
            return;
        }
        String line = getString(R.string.folder_line, shortPath(path));
        if (check != null && check.ok()) line += " · " + getString(R.string.folder_ok);
        folderView.setText(line);
    }

    private Runnable pendingPick;

    private void afterResume(Runnable job) {
        if (resumed) job.run();
        else pendingPick = job;
    }

    private void folderPicked(Intent data) {
        File dir = GameLocator.pickedDir(data);
        if (dir == null) {
            status(getString(R.string.folder_no_path));
            afterResume(() -> D2FolderPickDialog.show(this, null, null, null, this::chooseFolder));
            return;
        }
        String before = GameLocator.chosenPath(this);
        GameVersion.Result check = GameLocator.choose(this, dir);
        afterResume(() -> D2FolderPickDialog.show(this, dir, check, before, this::chooseFolder));
        if (GameLocator.chosenPath(this) == null) {
            status(GameLocator.message(this, check));
            return;
        }
        boolean usable = check.status == GameVersion.Status.OK || check.status == GameVersion.Status.MODIFIED;
        if (!usable) return;
        showFirstStep(false);
        showInstall(!usable);
        showFolder(check.dir, check);
        status(GameLocator.message(this, check));
    }

    private static String shortPath(String path) {
        String storage = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        return path.startsWith(storage + "/") ? path.substring(storage.length() + 1) : path;
    }


    private static final String KEY_DEVICE_OK = "deviceCheckOk", KEY_DEVICE_UNTESTED_OK = "deviceCheckUntestedOk";
    private static final long MIN_FREE = 1L << 30;
    private boolean deviceChecked, spaceChecked;

    private boolean deviceCheckPassed() {
        if (deviceChecked && spaceChecked) return true;
        String fake = D2DeviceProbe.fakeFromIntent(this, getIntent());
        android.content.SharedPreferences prefs = getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE);
        int version = appVersion();
        D2DeviceProbe probe = fake != null ? D2DeviceProbe.fake(fake) : D2DeviceProbe.basics();
        if (!deviceChecked) {
            if (fake == null && prefs.getInt(KEY_DEVICE_OK, -1) == version) deviceChecked = true;
            else {
                if (fake == null && !probe.ruledOutEarly()) probe.readGpu();
                D2DeviceCheck.Result result = probe.classify();
                D2Trace.log(probe.describe() + (fake != null ? " (test switch " + fake + ")" : ""));
                String gpu = probe.gpuName() != null ? probe.gpuName() : getString(R.string.check_gpu_unknown);
                if (result.level == D2DeviceCheck.Level.SUPPORTED) {
                    if (fake == null) prefs.edit().putInt(KEY_DEVICE_OK, version).apply();
                    deviceChecked = true;
                }
                else if (result.level == D2DeviceCheck.Level.UNTESTED) {
                    if (fake == null && prefs.getInt(KEY_DEVICE_UNTESTED_OK, -1) == version) deviceChecked = true;
                    else {
                        askStartAnyway(getString(R.string.check_untested_title), probe.gpuName() != null ? getString(R.string.check_untested, gpu)
                            : getString(R.string.check_untested_unknown), () -> {
                            if (fake == null) prefs.edit().putInt(KEY_DEVICE_UNTESTED_OK, version).apply();
                            deviceChecked = true;
                        });
                        return false;
                    }
                }
                else {
                    String why = result.reason == D2DeviceCheck.Reason.ABI ? getString(R.string.check_unsupported_abi)
                        : result.reason == D2DeviceCheck.Reason.PAGES ? getString(R.string.check_unsupported_pages)
                        : getString(R.string.check_unsupported_gpu, android.os.Build.MANUFACTURER, android.os.Build.MODEL, gpu);
                    styledDialog(getString(R.string.check_unsupported_title), why, getString(R.string.helper_diagnose_label),
                        () -> D2Diagnose.share(this));
                    status(getString(R.string.check_unsupported_title));
                    return false;
                }
            }
        }
        if (!spaceChecked) {
            long free = D2DeviceProbe.freeBytes(getFilesDir());
            boolean low = probe.fakeLowSpace || (!D2Runtime.isInstalled(this) && free >= 0 && free < MIN_FREE);
            if (low) {
                String amount = android.text.format.Formatter.formatShortFileSize(this, Math.max(0, free));
                askStartAnyway(getString(R.string.check_space_title), getString(R.string.check_space, amount), () -> spaceChecked = true);
                return false;
            }
            spaceChecked = true;
        }
        return true;
    }

    private void askStartAnyway(String title, String message, Runnable accept) {
        styledDialog(title, message, getString(R.string.check_start_anyway), () -> {
            accept.run();
            prepareAndLaunch();
        });
    }

    private int appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        }
        catch (Exception e) {
            return 0;
        }
    }

    private void status(final String message) {
        handler.post(() -> {
            statusView.setText(message);
            if (startPage != null) startPage.setStatus(message);
        });
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
