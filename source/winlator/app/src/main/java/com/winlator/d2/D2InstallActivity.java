package com.winlator.d2;

import com.winlator.R;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;

import com.winlator.core.EnvVars;
import com.winlator.xserver.Window;
import com.winlator.xserver.XLock;
import com.winlator.xserver.XServer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;









public class D2InstallActivity extends D2Activity {
    private D2InstallPlan.Language language;
    private TextView spaceView, statusView;

    private Button accessButton;
    private Button nextButton;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean busy;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private D2GuestScreen screen;
    private TextView progressView;

    private final D2InstallSteps steps = new D2InstallSteps();

    private android.view.View cover;

    private android.view.View programCancel;
    private TextView coverStatus, coverTotal;
    private android.widget.ProgressBar coverBar;
    private LinearLayout coverSteps;
    private D2InstallPlan.Product running;
    private boolean stopped;

    private boolean askedMenu, askedNoProgress, noStartChosen;
    private android.app.AlertDialog noStartDialog;

    private boolean installed;

    private File installTarget;

    private char[] keyClassic, keyLod;

    private boolean languageLocked;

    private boolean onFirstPage;
    private D2InstallFailure.Kind shownFailure;
    private Object[] shownFailureArgs;

    private boolean retryKeys;

    private int generation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        language = D2InstallPlan.defaultLanguage(Locale.getDefault());
        portrait();

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUI();
        if (D2TouchOverlay.isGameRunning()) {

            statusView.setTextColor(D2Style.RED);
            statusView.setText(R.string.install_game_running);
            nextButton.setEnabled(false);
        }
    }


    @Override
    protected boolean recreateOnLanguageChange() {
        return false;
    }





    private void work(Runnable job) {
        try {
            worker.execute(() -> {
                try {
                    job.run();
                }
                catch (RuntimeException e) {
                    android.util.Log.w("D2Install", "unexpected", e);
                    failInstall(D2InstallFailure.Kind.RUNTIME, String.valueOf(e));
                }
            });
        }
        catch (java.util.concurrent.RejectedExecutionException ignored) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        if (screen != null) screen.resume();
        else if (onFirstPage && !busy) buildUI();
        else if (shownFailure == D2InstallFailure.Kind.NO_ACCESS && hasStorage()) buildUI();
        else if (shownFailure == D2InstallFailure.Kind.NO_SPACE || shownFailure == D2InstallFailure.Kind.INSTALLER_NO_SPACE)
            buildFailPage(shownFailure, shownFailureArgs);
    }

    private void buildUI() {
        releaseRun();
        portrait();
        int pad = dp(24);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.install_title);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(D2Style.GOLD);
        page.addView(title);

        TextView intro = text(getString(R.string.install_intro), D2Style.TEXT);
        intro.setPadding(0, dp(8), 0, dp(16));
        page.addView(intro, matchWidth());

        onFirstPage = true;
        shownFailure = null;
        retryKeys = false;

        File download = download();
        boolean access = hasStorage();
        File chosen = GameLocator.gameDir(this);
        File awaiting = access ? D2InstallPlan.findAwaitingPatch(download, chosen) : null;

        File finished = access && awaiting == null ? D2InstallPlan.findFinished(download, chosen) : null;

        languageLocked = false;
        if (finished != null) {
            File game = finished;
            page.addView(heading(getString(R.string.install_found_title)));
            page.addView(text(getString(R.string.install_found_text, D2InstallPlan.ROOT_FOLDER + "/" + game.getName()), D2Style.TEXT), matchWidth());
            Button use = new Button(this);
            use.setText(R.string.install_found_use);
            use.setOnClickListener((v) -> useNewInstall(game));
            page.addView(use, matchWidth());
        }
        if (awaiting != null) {

            language = D2InstallFlow.resumeLanguage(D2InstallPlan.folderLanguage(awaiting), language);
            languageLocked = D2InstallPlan.folderLanguage(awaiting) != null;
            page.addView(heading(getString(R.string.install_resume_patch_title)));
            page.addView(text(getString(R.string.install_resume_patch_text), D2Style.TEXT), matchWidth());
            Button resume = new Button(this);
            resume.setText(R.string.install_resume_patch);
            resume.setOnClickListener((v) -> {
                resume.setEnabled(false);
                resumePatch(awaiting);
            });
            page.addView(resume, matchWidth());

            Button anew = new Button(this);
            anew.setText(R.string.install_start_anew);
            anew.setOnClickListener((v) -> confirmAnew(R.string.install_anew_question_keep, () -> D2InstallPlan.discardUnfinished(awaiting)));
            page.addView(anew, matchWidth());
        }
        else if (access && (D2InstallPlan.interrupted(download) || D2InstallPlan.installerLanguage(download) != null
            || D2InstallPlan.stoppedWithSaves(chosen))) {



            D2InstallFlow.ResumeNote resumeNote = D2InstallFlow.resumeNote(D2InstallPlan.interrupted(download),
                D2InstallPlan.stoppedWithSaves(chosen), finished != null);
            if (resumeNote != D2InstallFlow.ResumeNote.NONE) {
                TextView note = text(resumeNote == D2InstallFlow.ResumeNote.NEW_FOLDER
                    ? getString(R.string.install_resume_note_saves, D2InstallPlan.ROOT_FOLDER + "/" + D2InstallPlan.freeGameDir(download, chosen).getName())
                    : getString(R.string.install_resume_note), D2Style.GOLD);
                note.setPadding(0, 0, 0, dp(12));
                page.addView(note, matchWidth());
            }


            D2InstallPlan.Language stored = D2InstallPlan.installerLanguage(download);
            if (stored != null) {
                language = stored;
                languageLocked = true;
            }

            Button anew = new Button(this);
            anew.setText(R.string.install_start_anew);

            int question = D2InstallPlan.stoppedWithSaves(chosen) ? R.string.install_anew_question_saves
                : R.string.install_anew_question_all;
            anew.setOnClickListener((v) -> confirmAnew(question, () -> D2InstallPlan.discardAll(download)));
            page.addView(anew, matchWidth());
        }

        page.addView(heading(getString(R.string.install_language)));
        RadioGroup languages = new RadioGroup(this);
        for (D2InstallPlan.Language l : D2InstallPlan.Language.values()) {
            RadioButton b = new RadioButton(this);
            b.setId(radioId(l));
            String name = getString(l == D2InstallPlan.Language.EN_US ? R.string.install_lang_en : R.string.install_lang_de);
            b.setText(l.available ? name : getString(R.string.install_lang_soon, name));
            boolean usable = l.available && (!languageLocked || l == language);
            b.setTextColor(usable ? D2Style.TEXT : D2Style.HINT);
            b.setEnabled(usable);
            b.setChecked(l == language);
            b.setOnCheckedChangeListener((v, on) -> {
                if (on) language = l;
            });
            languages.addView(b);
        }
        page.addView(languages, matchWidth());
        if (languageLocked) page.addView(text(getString(R.string.install_language_locked), D2Style.HINT), matchWidth());

        page.addView(heading(getString(R.string.install_space)));
        spaceView = text("", D2Style.TEXT);
        page.addView(spaceView, matchWidth());

        accessButton = new Button(this);
        accessButton.setText(R.string.install_action_access);
        accessButton.setOnClickListener((v) -> askStorage());
        page.addView(accessButton, matchWidth());

        statusView = text("", D2Style.HINT);
        statusView.setPadding(0, dp(12), 0, dp(12));
        page.addView(statusView, matchWidth());

        nextButton = new Button(this);
        nextButton.setText(R.string.install_next);
        nextButton.setOnClickListener((v) -> fetchDownloaders());
        page.addView(nextButton, matchWidth());

        if (awaiting != null) nextButton.setVisibility(android.view.View.GONE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(D2Style.BACKGROUND);
        scroll.addView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
        checkSpace();
    }


    private void checkSpace() {
        boolean access = hasStorage();
        if (accessButton != null) accessButton.setVisibility(access ? android.view.View.GONE : android.view.View.VISIBLE);
        if (!access) {
            spaceView.setText(R.string.install_fail_no_access);
            spaceView.setTextColor(D2Style.RED);
            nextButton.setEnabled(false);
            return;
        }
        File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        long usable = download.getUsableSpace();
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        String free = D2InstallPlan.formatGb(usable, locale);

        long needed = neededAtStart();
        String need = D2InstallPlan.formatGb(needed, locale);
        boolean ok = D2InstallFlow.enough(usable, needed);
        spaceView.setText(getString(ok ? R.string.install_space_ok : R.string.install_space_low, free, need));
        spaceView.setTextColor(ok ? D2Style.TEXT : D2Style.RED);
        nextButton.setEnabled(ok && !busy && !D2TouchOverlay.isGameRunning());
    }


    private long neededAtStart() {
        File download = download();
        return D2InstallFlow.neededAtStart(D2InstallPlan.installerBytes(download),
            D2InstallPlan.reusableBytes(D2InstallPlan.freeGameDir(download, GameLocator.gameDir(this))));
    }


    private long neededBeforeKeys() {
        return D2InstallFlow.neededBeforeKeys(D2InstallPlan.reusableBytes(D2InstallPlan.freeGameDir(download(), GameLocator.gameDir(this))));
    }



    private void confirmAnew(int question, Runnable discard) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setMessage(question)
            .setPositiveButton(R.string.install_start_anew, (d, w) -> work(() -> {
                discard.run();
                runOnUiThread(this::buildUI);
            }))
            .setNegativeButton(R.string.install_anew_keep, null)
            .create();
        styleDialog(dialog);
    }


    private void styleDialog(android.app.AlertDialog dialog) {
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
            window.setLayout(Math.round(getResources().getDisplayMetrics().widthPixels * 0.9f), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        for (int which : new int[]{android.app.AlertDialog.BUTTON_POSITIVE, android.app.AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
        android.widget.TextView message = dialog.findViewById(android.R.id.message);
        if (message != null) message.setTextColor(D2Style.TEXT);
    }






    private void fetchDownloaders() {
        if (busy) return;
        if (!hasStorage()) {
            failInstall(D2InstallFailure.Kind.NO_ACCESS);
            return;
        }
        if (D2TouchOverlay.isGameRunning()) {
            statusView.setTextColor(D2Style.RED);
            statusView.setText(R.string.install_game_running);
            return;
        }
        onFirstPage = false;
        busy = true;
        stopped = false;
        nextButton.setEnabled(false);
        steps.at(D2InstallSteps.Step.FETCH);
        D2InstallPlan.Language lang = language;
        File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        work(() -> {
            D2InstallFailure.Kind failure = null;
            String result;
            try {
                D2InstallPlan.markInstallerLanguage(download, lang);
                D2InstallPlan.Product[] products = D2InstallPlan.Product.values();
                for (int i = 0; i < products.length; i++) {
                    D2InstallPlan.Product p = products[i];
                    int n = i + 1;
                    runOnUiThread(() -> statusView.setText(getString(R.string.install_fetching, n, products.length)));
                    D2InstallFetch.fetchDownloader(D2InstallPlan.downloaderUrl(lang, p), downloaderFile(download, p));
                }
                result = null;
            }
            catch (D2InstallFetch.Refused e) {
                result = e.getMessage();
                failure = D2InstallFailure.Kind.REFUSED;
            }
            catch (IOException e) {
                result = e.getMessage();

                failure = D2InstallFlow.fetchFailure(false, hasStorage(), download.getUsableSpace());
            }
            String text = result;
            D2InstallFailure.Kind kind = failure;
            runOnUiThread(() -> {
                busy = false;
                if (kind == null) {
                    runDownloader(0);
                    return;
                }
                failInstall(kind, text);
            });
        });
    }





    private void buildKeysPage() {
        buildKeysPage(null);
    }


    private void buildKeysPage(String notice) {
        releaseRun();
        portrait();
        steps.at(D2InstallSteps.Step.KEYS);
        int pad = dp(24);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(pad, pad, pad, pad);
        TextView title = heading(getString(R.string.install_keys_title));
        title.setTextSize(22);
        page.addView(title);
        page.addView(text(getString(R.string.install_keys_intro), D2Style.TEXT), matchWidth());
        android.widget.EditText classic = keyField(getString(R.string.install_key_classic));
        android.widget.EditText lod = keyField(getString(R.string.install_key_lod));
        classic.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
        classic.setOnEditorActionListener((v, action, event) -> lod.requestFocus());
        lod.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        page.addView(heading(getString(R.string.install_key_classic)));
        page.addView(classic, matchWidth());
        page.addView(heading(getString(R.string.install_key_lod)));
        page.addView(lod, matchWidth());
        TextView status = text("", D2Style.HINT);
        status.setPadding(0, dp(12), 0, dp(12));
        if (notice != null) {
            status.setTextColor(D2Style.RED);
            status.setText(notice);
        }
        page.addView(status, matchWidth());
        keysStatusView = status;
        Button install = new Button(this);
        keysButton = install;
        install.setText(R.string.install_start);
        install.setOnClickListener((v) -> {

            if (!hasStorage()) {
                failInstall(D2InstallFailure.Kind.NO_ACCESS);
                return;
            }

            if (!D2InstallFlow.enough(download().getUsableSpace(), neededBeforeKeys())) {
                retryKeys = true;
                failInstall(D2InstallFailure.Kind.NO_SPACE);
                return;
            }
            char[] c = takeKey(classic), l = takeKey(lod);
            D2InstallKeys.Check cc = D2InstallKeys.check(c), lc = D2InstallKeys.check(l);
            if (cc != D2InstallKeys.Check.OK || lc != D2InstallKeys.Check.OK) {
                D2InstallKeys.wipe(c);
                D2InstallKeys.wipe(l);
                status.setTextColor(D2Style.RED);
                status.setText(getString(R.string.install_keys_bad, keyProblem(cc), keyProblem(lc)));
                return;
            }
            wipeKeys();
            keyClassic = c;
            keyLod = l;
            status.setTextColor(D2Style.TEXT);
            status.setText(getString(R.string.install_keys_ok, D2InstallKeys.masked(c), D2InstallKeys.masked(l)));
            install.setEnabled(false);
            handler.postDelayed(this::installAll, 1500);
        });
        page.addView(install, matchWidth());
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(D2Style.BACKGROUND);
        scroll.addView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }


    private TextView keysStatusView;

    private Button keysButton;


    private void ensureRuntime() throws IOException {
        setUpRuntime(this::showSetupProgress);
    }





    private void setUpRuntime(D2Runtime.Progress progress) throws IOException {

        D2LaunchGate.installSetup(!D2Runtime.isInstalled(this) || !D2Runtime.isPrefixReady(this), () -> {
            if (!D2Runtime.isInstalled(this)) D2Runtime.install(this, progress);
            if (!D2Runtime.isPrefixReady(this)) D2Runtime.setupPrefix(this, progress);
        });
    }





    private void installBlocked() {
        if (isFinishing() || isDestroyed() || keysStatusView == null || keysButton == null) return;
        keysStatusView.setTextColor(D2Style.RED);

        keysStatusView.setText(getString(D2TouchOverlay.isGameRunning() ? R.string.install_game_running : R.string.install_game_starting));
        keysButton.setText(R.string.install_next);
        keysButton.setEnabled(true);
        keysButton.setOnClickListener((v) -> {
            keysButton.setEnabled(false);
            keysStatusView.setTextColor(D2Style.TEXT);
            keysStatusView.setText("");
            installAll();
        });
    }

    private void showSetupProgress(String text) {
        runOnUiThread(() -> {
            TextView line = keysStatusView != null ? keysStatusView : statusView;
            if (line == null) return;
            line.setTextColor(D2Style.TEXT);
            line.setText(text);
        });
    }




    private static final String TOOLS_FOLDER = "D2Install";
    private static final String OLD_GAME_EXE = "Game.exe.114b";




    private static final String PIPE_PREFIX = "d2k-";
    private static final String INSTALLER_ARGS = "1800 --no-start " + D2InstallPlan.NO_START_SECONDS + " --key-from %s --run %s\\Installer.exe --user=Player --destination=%s"
        + " --silent=true --noui=true --skippostlaunch=true --nopatchnotes=true";



    private File toolsDir() {
        return new File(D2Runtime.driveC(this), TOOLS_FOLDER);
    }


    private static final String KEY_CHECK_PASSED = "installCheckPassed";

    static void rememberCheckPassed(android.content.Context context, File dir) {
        context.getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE).edit().putString(KEY_CHECK_PASSED, dir.getAbsolutePath()).apply();
    }

    static void forgetCheckPassed(android.content.Context context, File dir) {
        if (checkPassed(context, dir)) context.getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE).edit().remove(KEY_CHECK_PASSED).apply();
    }

    static boolean checkPassed(android.content.Context context, File dir) {
        return dir != null && dir.getAbsolutePath().equals(
            context.getSharedPreferences(D2Settings.PREFS, MODE_PRIVATE).getString(KEY_CHECK_PASSED, null));
    }

    private File download() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }


    private File installerExeDir(D2InstallPlan.Product product) {
        File base = D2InstallPlan.installerDir(download(), product);
        File exe = D2InstallFetch.find(base, "Installer.exe", 1, language.locale);
        return exe != null ? exe.getParentFile() : base;
    }





    private java.util.Map<String, String> finalReference() throws IOException {
        return reference(language);
    }

    private java.util.Map<String, String> reference(D2InstallPlan.Language lang) throws IOException {
        try (java.io.InputStream in = getAssets().open("checksums-113d-" + lang.locale + ".txt")) {
            return GameVersion.parseList(in);
        }
        catch (java.io.FileNotFoundException e) {
            if (lang == D2InstallPlan.Language.EN_US) return GameLocator.reference(this);
            throw new IOException("no reference list for " + lang.locale);
        }
    }


    private File target() {
        return installTarget != null ? installTarget : D2InstallPlan.gameDir(download());
    }


    private void copyTools() throws IOException {
        File tools = toolsDir();
        if (!tools.isDirectory() && !tools.mkdirs()) throw new IOException("cannot create " + tools);
        for (String name : new String[]{"d2install.exe", "placeholder-game.exe"}) {
            try (java.io.InputStream in = getAssets().open("d2/install/" + name);
                 java.io.OutputStream out = new java.io.FileOutputStream(new File(tools, name))) {
                byte[] buf = new byte[1 << 16];
                for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
            }
        }
    }


    private void installAll() {
        if (keyClassic == null || keyLod == null) return;
        if (D2TouchOverlay.isGameRunning()) {
            failInstall(D2InstallFailure.Kind.RUNTIME, getString(R.string.install_game_running));
            return;
        }
        stopped = false;
        retryKeys = false;



        installTarget = D2InstallPlan.freeGameDir(download(), GameLocator.gameDir(this));
        File target = installTarget;
        work(() -> {
            try {


                D2InstallPlan.beginInstall(target, language);
                forgetCheckPassed(this, target);



                ensureRuntime();
                copyTools();
                runOnUiThread(() -> runInstaller(D2InstallPlan.Product.CLASSIC));
            }
            catch (D2LaunchGate.Busy e) {
                runOnUiThread(this::installBlocked);
            }
            catch (IOException e) {
                failInstall(D2InstallFailure.Kind.RUNTIME, e.getMessage());
            }
        });
    }

    private void runInstaller(D2InstallPlan.Product product) {
        char[] key = product == D2InstallPlan.Product.CLASSIC ? keyClassic : keyLod;
        if (key == null) return;
        steps.at(D2InstallSteps.install(product));

        if (!holdRun()) {
            installBlocked();
            return;
        }
        KeyPipe pipe;
        try {
            pipe = new KeyPipe(key);
        }
        catch (IOException e) {
            failInstall(D2InstallFailure.Kind.RUNTIME, e.getMessage());
            return;
        }
        keyPipe = pipe;
        String dir = D2Runtime.dosPath(this, installerExeDir(product));
        String target = D2Runtime.dosPath(this, target());
        String args = String.format(Locale.ROOT, INSTALLER_ARGS, pipe.dosPath(), dir, target);
        new File(toolsDir(), "d2install.log").delete();
        askedMenu = false;
        askedNoProgress = false;
        noStartChosen = false;
        runGuest(getString(R.string.install_installing, product.folder), dir, "C:\\D2Install\\d2install.exe", args, null, () -> {
            pipe.close();
            dismissNoStart();



            String helper = helperResult();
            File game = target();
            String expected = product == D2InstallPlan.Product.CLASSIC ? "d2data.mpq" : "d2exp.mpq";

            D2InstallFlow.AfterInstaller next = D2InstallFlow.afterInstaller(helper, noStartChosen, new File(game, expected).isFile(),
                download().getUsableSpace());
            D2Trace.log("install: " + product.code + " ended: " + next + (helper != null ? " (" + helper + ")" : ""));
            if (next == D2InstallFlow.AfterInstaller.WRONG_KEY || next == D2InstallFlow.AfterInstaller.NO_START) {
                wipeKeys();
                discardTarget();

                buildKeysPage(next == D2InstallFlow.AfterInstaller.NO_START ? getString(R.string.install_key_no_start, product.folder)
                    : getString(R.string.install_key_rejected));
                return;
            }
            if (next == D2InstallFlow.AfterInstaller.NO_SPACE) {
                wipeKeys();
                retryKeys = false;
                failInstall(D2InstallFailure.Kind.INSTALLER_NO_SPACE, product.folder);
                return;
            }
            if (next == D2InstallFlow.AfterInstaller.INSTALLER) {
                wipeKeys();
                failInstall(D2InstallFailure.Kind.INSTALLER, product.folder, helper != null ? helper : "-");
                return;
            }

            if (product == D2InstallPlan.Product.CLASSIC) { D2InstallKeys.wipe(keyClassic); keyClassic = null; }
            else { D2InstallKeys.wipe(keyLod); keyLod = null; }
            if (product == D2InstallPlan.Product.CLASSIC) {



                work(() -> {
                    killGuests();
                    writeInstallPath(game);
                    runOnUiThread(() -> runInstaller(D2InstallPlan.Product.LOD));
                });
            }
            else {
                try {
                    D2InstallPlan.markInstalled(game, language);
                }
                catch (IOException e) {
                    android.util.Log.w("D2Install", "no marker: " + e.getMessage());
                }
                patch();
            }
        });
        D2GuestScreen current = screen;
        handler.postDelayed(() -> watchStart(current), 5000);
    }







    private void watchStart(D2GuestScreen current) {
        if (current == null || current != screen || stopped) return;
        String state = D2InstallPlan.helperState(helperLog());
        if ("progress".equals(state)) {
            dismissNoStart();
            return;
        }

        boolean menu = "start-menu".equals(state), none = "no-progress".equals(state);
        if ((menu && !askedMenu || none && !askedNoProgress) && (noStartDialog == null || !noStartDialog.isShowing())) {
            if (menu) askedMenu = true;
            else askedNoProgress = askedMenu = true;
            D2Trace.log("install: " + state + " after the dialogs, asking");
            askNoStart(menu ? R.string.install_start_menu_question : R.string.install_no_start_question);
        }
        handler.postDelayed(() -> watchStart(current), 5000);
    }

    private void askNoStart(int question) {
        TextView message = text(getString(question), D2Style.TEXT);
        message.setPadding(dp(20), dp(16), dp(20), dp(4));
        noStartDialog = new android.app.AlertDialog.Builder(this)
            .setView(message)
            .setPositiveButton(R.string.install_check_keys, (d, w) -> {


                List<String> log = helperLog();
                if ("progress".equals(D2InstallPlan.helperState(log)) || D2InstallPlan.helperResult(log) == null) {
                    D2Trace.log("install: the installer started after all, nothing ended");
                    if (progressView != null) progressView.setText(getString(R.string.install_started_after_all));
                    return;
                }
                noStartChosen = true;
                D2Trace.log("install: the player checks the keys, ending the installer");
                work(this::killGuests);
            })
            .setNegativeButton(R.string.install_keep_waiting, (d, w) -> D2Trace.log("install: the player waits on"))
            .create();
        styleDialog(noStartDialog);
    }


    private List<String> helperLog() {
        try {
            return java.nio.file.Files.readAllLines(new File(toolsDir(), "d2install.log").toPath(), java.nio.charset.StandardCharsets.ISO_8859_1);
        }
        catch (IOException e) {
            return java.util.Collections.emptyList();
        }
    }

    private void dismissNoStart() {
        if (noStartDialog != null && noStartDialog.isShowing()) noStartDialog.dismiss();
        noStartDialog = null;
    }





    private String helperResult() {
        try {
            return D2InstallPlan.helperResult(java.nio.file.Files.readAllLines(new File(toolsDir(), "d2install.log").toPath(),
                java.nio.charset.StandardCharsets.ISO_8859_1));
        }
        catch (IOException e) {
            return "no helper log";
        }
    }


    private void patch() {
        File game = target();
        steps.at(D2InstallSteps.Step.PATCH);
        work(() -> {
            try {
                killGuests();


                if (!patchAgainClean && gameExeIs113d(game)) {
                    runOnUiThread(this::finishInstall);
                    return;
                }
                patchAgainClean = false;
                File patchFile = new File(D2InstallPlan.root(download()), D2InstallPlan.INSTALLER_FOLDER + "/LODPatch_113d.exe");
                D2InstallFetch.fetchPatch(patchFile);
                File exe = new File(game, "Game.exe"), old = new File(game, OLD_GAME_EXE);
                if (!old.isFile() && !exe.renameTo(old)) throw new IOException("cannot rename Game.exe");
                keepPatchArchive(game);
                copyFile(new File(toolsDir(), "placeholder-game.exe"), exe);
                copyFile(patchFile, new File(game, "LODPatch_113d.exe"));
                writeInstallPath(game);
                runOnUiThread(() -> runGuest(getString(R.string.install_patching), D2Runtime.dosPath(this, game),
                    "C:\\D2Install\\d2install.exe", "900 --linger --run " + D2Runtime.dosPath(this, game) + "\\LODPatch_113d.exe", null, this::finishInstall));
            }
            catch (IOException e) {
                failInstall(D2InstallFailure.Kind.PATCH, e.getMessage());
            }
        });
    }


    private static final String PATCH_ARCHIVE = "Patch_D2.mpq", PATCH_ARCHIVE_OLD = "Patch_D2.mpq.114b", PATCH_ARCHIVE_NONE = "Patch_D2.mpq.none";

    private static final String REPATCHED_MARKER = ".d2android-repatched";







    private static void keepPatchArchive(File game) throws IOException {
        File archive = new File(game, PATCH_ARCHIVE), kept = new File(game, PATCH_ARCHIVE_OLD), none = new File(game, PATCH_ARCHIVE_NONE);
        if (kept.isFile()) copyFile(kept, archive);
        else if (none.isFile()) {
            if (archive.exists() && !archive.delete()) throw new IOException("cannot remove " + archive);
        }
        else if (archive.isFile()) {
            File part = new File(game, PATCH_ARCHIVE_OLD + ".part");
            copyFile(archive, part);
            if (!part.renameTo(kept)) throw new IOException("cannot rename " + part);
        }
        else if (!none.createNewFile()) throw new IOException("cannot create " + none);
    }


    private static void dropPatchBackups(File game) {
        new File(game, PATCH_ARCHIVE_OLD).delete();
        new File(game, PATCH_ARCHIVE_NONE).delete();
        new File(game, REPATCHED_MARKER).delete();
    }


    private volatile boolean patchAgainClean;


    private boolean gameExeIs113d(File game) throws IOException {
        String want = GameLocator.reference(this).get("Game.exe");
        File current = new File(game, "Game.exe");
        return want != null && current.isFile() && want.equalsIgnoreCase(GameVersion.sha256(current));
    }


    private void finishInstall() {
        File game = target();
        steps.at(D2InstallSteps.Step.CHECK);
        int gen = generation;
        work(() -> {
            String problem = null;
            boolean patchFailed = false;
            try {
                killGuests();
                new File(game, "LODPatch_113d.exe").delete();
                File log = new File(game, "bnupdate.log");
                String text = log.isFile() ? new String(java.nio.file.Files.readAllBytes(log.toPath()), java.nio.charset.StandardCharsets.ISO_8859_1) : "";

                D2InstallFlow.AfterPatch after = D2InstallFlow.afterPatch(text.toLowerCase(Locale.ROOT).contains("succeeded"), gameExeIs113d(game));
                if (after == D2InstallFlow.AfterPatch.PATCH_FAILED) {
                    problem = "LODPatch_113d.exe: no \"succeeded\" in bnupdate.log";
                    patchFailed = true;
                }
                else {
                    for (String name : D2InstallCheck.VARYING) D2MpqAttributes.hide(new File(game, name));
                    problem = D2InstallCheck.verify(game, finalReference());

                    if (problem != null && D2InstallPlan.folderLanguage(game) == null) {
                        D2InstallPlan.Language other = language == D2InstallPlan.Language.EN_US ? D2InstallPlan.Language.DE_DE : D2InstallPlan.Language.EN_US;
                        if (D2InstallCheck.verify(game, reference(other)) == null) {
                            language = other;
                            problem = null;
                        }
                    }


                    File repatched = new File(game, REPATCHED_MARKER);
                    D2InstallFlow.AfterCheck checked = D2InstallFlow.afterCheck(problem == null,
                        new File(game, PATCH_ARCHIVE_OLD).isFile() || new File(game, PATCH_ARCHIVE_NONE).isFile(), repatched.isFile());
                    if (checked == D2InstallFlow.AfterCheck.DONE) dropPatchBackups(game);
                    else if (checked == D2InstallFlow.AfterCheck.REPATCH) {
                        if (!repatched.isFile() && !repatched.createNewFile()) throw new IOException("cannot create " + repatched);
                        patchFailed = true;
                        patchAgainClean = true;
                        D2Trace.log("install: the check failed after the patch (" + problem + "), one clean patch again");
                    }



                    if (problem == null && !D2InstallPlan.installFinished(game)) {
                        android.util.Log.w("D2Install", "cannot remove or neutralise the marker in " + game);
                        rememberCheckPassed(this, game);
                    }
                    else if (problem != null) forgetCheckPassed(this, game);
                }
            }
            catch (IOException e) {
                problem = e.getMessage();
            }
            String failed = problem;
            boolean onlyPatch = patchFailed;
            runOnUiThread(() -> {
                if (stopped || gen != generation || isFinishing() || isDestroyed()) return;
                if (failed != null) {
                    failInstall(onlyPatch ? D2InstallFailure.Kind.PATCH : D2InstallFailure.Kind.CHECK, failed);
                    return;
                }
                askUseNewInstall(game);
            });
        });
    }


    private void askUseNewInstall(File game) {
        buildUI();
        String folder = D2InstallPlan.ROOT_FOLDER + "/" + game.getName();
        statusView.setText(getString(R.string.install_done, folder));
        int pad = dp(16);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, 0);
        TextView title = new TextView(this);
        title.setText(R.string.install_use_title);
        title.setTextSize(18);
        D2Style.heading(title);
        content.addView(title);
        TextView message = new TextView(this);
        message.setText(getString(R.string.install_use_message, folder));
        message.setTextSize(16);
        message.setPadding(0, dp(8), 0, 0);
        D2Style.text(message);
        content.addView(message);

        android.widget.CheckBox cleanup = new android.widget.CheckBox(this);
        cleanup.setText(R.string.install_delete_installers);
        cleanup.setChecked(true);
        D2Style.control(cleanup);
        content.addView(cleanup);
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
            .setView(content)
            .setPositiveButton(R.string.install_use_yes, (d, w) -> {
                if (cleanup.isChecked()) deleteInstallers();
                useNewInstall(game);
            })
            .setNegativeButton(R.string.install_use_no, (d, w) -> {
                if (cleanup.isChecked()) deleteInstallers();
                buildUI();
            })
            .setCancelable(false)
            .create();
        styleDialog(dialog);
    }






    private void useNewInstall(File game) {
        GameVersion.Result r = GameLocator.choose(this, game);
        if (!r.ok()) {
            statusView.setText(GameLocator.message(this, r));
            return;
        }
        installed = true;
        startActivity(new android.content.Intent(this, D2LaunchActivity.class));
        finish();
    }


    private void deleteInstallers() {
        File dir = new File(D2InstallPlan.root(download()), D2InstallPlan.INSTALLER_FOLDER);
        new Thread(() -> deleteTree(dir), "d2-install-cleanup").start();
    }

    private static void deleteTree(File f) {
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteTree(c);
        f.delete();
    }


    private void failInstall(D2InstallFailure.Kind kind, Object... args) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            D2Trace.log("install: stopped, " + kind);
            generation++;
            busy = false;
            wipeKeys();
            closeScreen();


            if (D2InstallFlow.discard(kind, D2InstallPlan.awaitsPatch(installTarget))) discardTarget();
            releaseRun();
            buildFailPage(kind, args);
        });
    }







    private void discardTarget() {
        File unfinished = installTarget;
        boolean ours = runHeld;
        work(() -> {
            if (ours) killGuests();
            if (unfinished == null) return;
            File chosen = GameLocator.gameDir(this);
            if (chosen != null && chosen.getAbsoluteFile().equals(unfinished.getAbsoluteFile())) return;
            if (D2InstallPlan.discardUnfinished(unfinished)) android.util.Log.i("D2Install", "removed the unfinished " + unfinished);
        });
    }


    private void writeInstallPath(File game) {
        String dos = D2Runtime.dosPath(this, game) + "\\";
        String key = "Software\\Blizzard Entertainment\\Diablo II";
        File prefix = D2Runtime.prefixDir(this);
        try (com.winlator.core.WineRegistryEditor user = new com.winlator.core.WineRegistryEditor(new File(prefix, "user.reg"))) {
            user.setCreateKeyIfNotExist(true);
            user.setStringValue(key, "InstallPath", dos);
        }
        try (com.winlator.core.WineRegistryEditor system = new com.winlator.core.WineRegistryEditor(new File(prefix, "system.reg"))) {
            system.setCreateKeyIfNotExist(true);
            system.setStringValue("Software\\Wow6432Node\\Blizzard Entertainment\\Diablo II", "InstallPath", dos);
            system.setStringValue(key, "InstallPath", dos);
        }
    }

    private KeyPipe keyPipe;







    private final class KeyPipe {
        final File path;
        private volatile boolean done;

        KeyPipe(char[] key) throws IOException {
            byte[] random = new byte[12];
            new java.security.SecureRandom().nextBytes(random);
            StringBuilder name = new StringBuilder(PIPE_PREFIX);
            for (byte b : random) name.append(String.format(Locale.ROOT, "%02x", b));


            File tmp = toolsDir();
            if (!tmp.isDirectory() && !tmp.mkdirs()) throw new IOException("cannot create " + tmp);
            path = new File(tmp, name.toString());
            try {
                android.system.Os.mkfifo(path.getPath(), 0600);
            }
            catch (android.system.ErrnoException e) {
                throw new IOException(e.getMessage());
            }
            byte[] line = D2InstallKeys.lineBytes(key);
            Thread writer = new Thread(() -> {
                java.io.FileDescriptor fd = null;
                try {
                    fd = android.system.Os.open(path.getPath(), android.system.OsConstants.O_WRONLY, 0);
                    android.system.Os.write(fd, line, 0, line.length);
                }
                catch (android.system.ErrnoException | java.io.InterruptedIOException ignored) {

                }
                finally {
                    D2InstallKeys.wipe(line);
                    if (fd != null) {
                        try { android.system.Os.close(fd); } catch (android.system.ErrnoException ignored) { }
                    }
                    path.delete();
                    done = true;
                }
            }, "d2-key-pipe");
            writer.setDaemon(true);
            writer.start();
        }

        String dosPath() {
            return "C:\\" + TOOLS_FOLDER + "\\" + path.getName();
        }


        void close() {
            if (!done && path.exists()) {
                try {
                    java.io.FileDescriptor r = android.system.Os.open(path.getPath(),
                        android.system.OsConstants.O_RDONLY | android.system.OsConstants.O_NONBLOCK, 0);
                    android.system.Os.close(r);
                }
                catch (android.system.ErrnoException ignored) {

                }
            }
            path.delete();
        }
    }

    private static void copyFile(File from, File to) throws IOException {
        java.nio.file.Files.copy(from.toPath(), to.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }


    private void runGuest(String label, String dosDir, String program, String args, String stdin, Runnable then) {
        if (stopped) return;
        if (!holdRun()) {
            failInstall(D2InstallFailure.Kind.RUNTIME, getString(R.string.launch_already_starting));
            return;
        }
        closeScreen();
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(D2Style.BACKGROUND);
        setContentView(frame);
        screen = new D2GuestScreen(this, frame, false, false);
        progressView = text(label, D2Style.GOLD);
        progressView.setBackgroundColor(0xC0000000);
        progressView.setPadding(dp(12), dp(6), dp(12), dp(6));
        frame.addView(progressView, statusParams());
        addCover(frame, false);
        D2GuestScreen current = screen;
        String screenSize = current.getXServer().screenInfo.toString();
        work(() -> {
            try {
                killGuests();
                setUpRuntime((t) -> runOnUiThread(() -> progressView.setText(t)));
                D2Runtime.prepareSession(this);
                current.start(D2Runtime.guestCommand(screenSize, dosDir, program, args), D2Runtime.guestEnv(this), stdin,
                    (status) -> runOnUiThread(() -> {
                        if (current != screen || stopped) return;
                        D2Trace.log("install: " + program.substring(program.lastIndexOf('\\') + 1) + " ended (" + status + ")");
                        closeScreen();
                        then.run();
                    }));
            }
            catch (IOException e) {

                failInstall(D2InstallFailure.Kind.RUNTIME, e instanceof D2LaunchGate.Busy ? getString(R.string.launch_already_starting) : e.getMessage());
            }
        });
    }

    private android.widget.EditText keyField(String hint) {
        android.widget.EditText field = new android.widget.EditText(this);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setTextColor(D2Style.TEXT);
        field.setHintTextColor(D2Style.HINT);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        field.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        field.setSaveEnabled(false);
        return field;
    }


    private static char[] takeKey(android.widget.EditText field) {
        android.text.Editable e = field.getText();
        char[] typed = new char[e.length()];
        e.getChars(0, e.length(), typed, 0);
        e.replace(0, e.length(), new String(new char[e.length()]));
        e.clear();
        char[] key = D2InstallKeys.normalize(typed);
        D2InstallKeys.wipe(typed);
        return key;
    }

    private String keyProblem(D2InstallKeys.Check check) {
        switch (check) {
            case OK: return getString(R.string.install_key_ok);
            case EMPTY: return getString(R.string.install_key_empty);
            case BAD_CHARACTER: return getString(R.string.install_key_bad_char);
            default: return getString(R.string.install_key_bad_length);
        }
    }

    private void wipeKeys() {
        if (keyPipe != null) {
            keyPipe.close();
            keyPipe = null;
        }
        D2InstallKeys.wipe(keyClassic);
        D2InstallKeys.wipe(keyLod);
        keyClassic = keyLod = null;
    }


    private static final D2InstallPlan.Product[] DOWNLOAD_ORDER = {D2InstallPlan.Product.LOD, D2InstallPlan.Product.CLASSIC};





    private void runDownloader(int index) {
        if (stopped) return;
        if (index >= DOWNLOAD_ORDER.length) {
            closeScreen();
            buildKeysPage();
            return;
        }


        D2InstallPlan.Product next = DOWNLOAD_ORDER[index];
        steps.at(D2InstallSteps.download(next));
        int gen = generation;
        work(() -> {
            boolean ready;
            try {
                D2InstallFetch.verifyProduct(D2InstallPlan.installerDir(download(), next), installerSums(), next.code, language.locale);
                ready = true;
            }
            catch (IOException e) {
                ready = false;
            }
            boolean skip = ready;
            runOnUiThread(() -> {
                if (stopped || gen != generation) return;
                if (skip) {
                    D2Trace.log("install: " + next.code + " already downloaded and verified");
                    runDownloader(index + 1);
                }
                else startDownloaderRun(index);
            });
        });
    }


    private java.util.Map<String, String> installerSums() throws IOException {
        try (java.io.InputStream in = getAssets().open("install/installer-sha256-" + language.locale + ".txt")) {
            return D2InstallFetch.parseChecksums(new String(com.winlator.core.StreamUtils.copyToByteArray(in),
                java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void startDownloaderRun(int index) {
        if (!holdRun()) {
            buildUI();
            statusView.setText(getString(R.string.install_fetch_failed, getString(D2TouchOverlay.isGameRunning()
                ? R.string.install_game_running : R.string.launch_already_starting)));
            return;
        }
        D2InstallPlan.Product product = DOWNLOAD_ORDER[index];
        running = product;
        lastChange = 0;
        closeScreen();
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(D2Style.BACKGROUND);
        setContentView(frame);
        screen = new D2GuestScreen(this, frame, false, false);
        progressView = text(getString(R.string.install_download_progress, product.folder, 0), D2Style.GOLD);
        progressView.setBackgroundColor(0xC0000000);
        progressView.setPadding(dp(12), dp(6), dp(12), dp(6));
        frame.addView(progressView, statusParams());
        Button cancel = new Button(this);
        cancel.setText(R.string.install_cancel);
        cancel.setOnClickListener((v) -> cancelDownloads());
        frame.addView(cancel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.BOTTOM | android.view.Gravity.END));

        cancel.setVisibility(android.view.View.GONE);
        programCancel = cancel;
        addCover(frame, true);
        D2GuestScreen current = screen;
        String screenSize = current.getXServer().screenInfo.toString();
        work(() -> {
            try {
                setUpRuntime((t) -> runOnUiThread(() -> progressView.setText(t)));
                killGuests();
                D2Runtime.prepareSession(this);
                EnvVars env = D2Runtime.guestEnv(this);
                String dir = "D:\\" + D2InstallPlan.ROOT_FOLDER + "\\" + D2InstallPlan.INSTALLER_FOLDER + "\\" + product.folder;
                String exe = downloaderFile(new File("/"), product).getName();
                String command = D2Runtime.guestCommand(screenSize, dir, exe, "--saveas=" + dir + " --autolaunch=0");
                current.start(command, env, null, (status) -> runOnUiThread(() -> {
                    if (current == screen && running == product) downloaderEnded(index, -1);
                }));
                runOnUiThread(() -> pollProgress(current, product, index));
            }
            catch (IOException e) {

                String why = e instanceof D2LaunchGate.Busy ? getString(R.string.launch_already_starting) : e.getMessage();
                runOnUiThread(() -> failInstall(D2InstallFailure.Kind.RUNTIME, why));
            }
        });
    }


    private int lastPercent = -2;
    private long lastChange;


    private void pollProgress(D2GuestScreen current, D2InstallPlan.Product product, int index) {
        if (current != screen || stopped) return;
        int percent = -1;
        XServer xServer = current.getXServer();
        try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
            for (Window w : xServer.windowManager.rootWindow.getChildren()) {
                percent = Math.max(percent, D2InstallPlan.downloaderProgress(w.getName()));
                for (Window c : w.getChildren()) percent = Math.max(percent, D2InstallPlan.downloaderProgress(c.getName()));
            }
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (percent != lastPercent || lastChange == 0) {
            lastPercent = percent;
            lastChange = now;
        }
        if (percent >= 0) {
            steps.percent(percent);
            progressView.setText(getString(R.string.install_download_progress, product.folder, percent));
        }

        if (D2InstallSteps.stalled(lastChange, now)) {
            progressView.setText(getString(R.string.install_stalled, (now - lastChange) / 60_000L));
        }
        if (percent >= 100) {
            downloaderEnded(index, percent);
            return;
        }
        handler.postDelayed(() -> pollProgress(current, product, index), 1000);
    }





    private void downloaderEnded(int index, int percent) {
        running = null;
        D2InstallPlan.Product product = DOWNLOAD_ORDER[index];
        D2Trace.log("install: downloader " + product.code + " ended at " + percent + " %");
        closeScreen();
        File download = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String list = "install/installer-sha256-" + language.locale + ".txt";
        int gen = generation;
        work(() -> {
            String error = null;
            try {
                killGuests();
                String text;
                try (java.io.InputStream in = getAssets().open(list)) {
                    text = new String(com.winlator.core.StreamUtils.copyToByteArray(in), java.nio.charset.StandardCharsets.UTF_8);
                }
                D2InstallFetch.verifyProduct(D2InstallPlan.installerDir(download, product), D2InstallFetch.parseChecksums(text), product.code, language.locale);
            }
            catch (IOException e) {
                error = e.getMessage();
            }
            String failed = error;
            runOnUiThread(() -> {
                if (stopped || gen != generation) return;
                if (failed == null) {
                    runDownloader(index + 1);
                    return;
                }

                D2InstallFailure.Kind kind = D2InstallFailure.afterDownload(percent);
                if (kind == D2InstallFailure.Kind.DOWNLOAD_DAMAGED) work(() -> deleteTree(D2InstallPlan.installerDir(download, product)));
                failInstall(kind, product.folder, failed);
            });
        });
    }


    private void cancelDownloads() {
        generation++;
        stopped = true;
        running = null;
        closeScreen();
        work(this::killGuests);
        buildUI();
        statusView.setText(R.string.install_cancelled);
    }









    private void killGuests() {
        killGuests(true);
    }


    private void killGuests(boolean saveRegistry) {
        File proc = new File("/proc"), rootfs = D2Runtime.rootDir(this);
        int uid = android.os.Process.myUid(), self = android.os.Process.myPid();
        java.util.List<D2GuestProcs.Proc> found = D2GuestProcs.find(proc, rootfs, uid, self, null);
        if (found.isEmpty()) return;
        java.util.List<Integer> servers = new java.util.ArrayList<>(), others = new java.util.ArrayList<>();
        for (D2GuestProcs.Proc p : found) (p.server ? servers : others).add(p.pid);
        if (saveRegistry) {
            for (int pid : servers) {
                try {
                    android.system.Os.kill(pid, android.system.OsConstants.SIGTERM);
                }
                catch (android.system.ErrnoException ignored) {

                }
            }
        }
        long end = System.currentTimeMillis() + (saveRegistry ? WINESERVER_SAVE_MS : 0);
        for (int pid : servers) {
            while (isAlive(pid) && System.currentTimeMillis() < end) {
                try {
                    Thread.sleep(100);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (saveRegistry && isAlive(pid)) D2Trace.log("install: wineserver " + pid + " did not end on SIGTERM");
        }
        for (int pid : servers) android.os.Process.killProcess(pid);
        for (int pid : others) android.os.Process.killProcess(pid);

        java.util.Set<Integer> ended = new java.util.HashSet<>(servers);
        ended.addAll(others);
        long gone = System.currentTimeMillis() + GUESTS_GONE_MS;
        for (int pid : ended) {
            while (isAlive(pid) && System.currentTimeMillis() < gone) {
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        java.util.List<D2GuestProcs.Proc> late = D2GuestProcs.find(proc, rootfs, uid, self, ended);
        for (D2GuestProcs.Proc p : late) android.os.Process.killProcess(p.pid);
        int alive = 0;
        for (int pid : ended) if (isAlive(pid)) alive++;
        D2Trace.log("install: ended " + found + (late.isEmpty() ? "" : ", then " + late)
            + (alive == 0 ? "" : ", " + alive + " still alive after " + GUESTS_GONE_MS + " ms"));
    }


    private static final long GUESTS_GONE_MS = 2000;


    private static boolean isAlive(int pid) {
        try {
            String stat = new String(java.nio.file.Files.readAllBytes(new File("/proc/" + pid + "/stat").toPath()),
                java.nio.charset.StandardCharsets.ISO_8859_1);
            int close = stat.lastIndexOf(')');
            return close < 0 || close + 2 >= stat.length() || stat.charAt(close + 2) != 'Z';
        }
        catch (IOException | RuntimeException e) {
            return false;
        }
    }


    private static final long WINESERVER_SAVE_MS = 8000;

    private void closeScreen() {
        cover = null;
        programCancel = null;
        coverSteps = null;
        if (screen == null) return;
        D2GuestScreen old = screen;
        screen = null;
        old.destroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (screen != null) screen.pauseView();
    }

    @Override
    protected void onDestroy() {
        stopped = true;
        dismissNoStart();
        wipeKeys();
        boolean runActive = screen != null, held = runHeld;
        runHeld = false;
        closeScreen();

        if (runActive || !installed) {
            new Thread(() -> {
                killGuests(false);
                if (held) D2LaunchGate.endInstallRun();
            }).start();
        }
        else if (held) D2LaunchGate.endInstallRun();
        worker.shutdownNow();
        super.onDestroy();
    }


    private boolean runHeld;


    private boolean holdRun() {
        if (runHeld) return true;
        if (D2TouchOverlay.isGameRunning()) return false;
        if (!D2LaunchGate.beginInstallRun()) return false;
        runHeld = true;
        return true;
    }


    private void releaseRun() {
        if (!runHeld) return;
        runHeld = false;
        try {
            worker.execute(() -> {
                try {
                    killGuests();
                }
                finally {
                    D2LaunchGate.endInstallRun();
                }
            });
        }
        catch (java.util.concurrent.RejectedExecutionException e) {
            D2LaunchGate.endInstallRun();
        }
    }


    static File downloaderFile(File download, D2InstallPlan.Product product) {
        return new File(D2InstallPlan.installerDir(download, product), String.format(Locale.ROOT, "Downloader_%s.exe", product.code));
    }

    private static int radioId(D2InstallPlan.Language l) {
        return 0x68000 + l.ordinal();
    }




    private void portrait() {
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }






    private void addCover(FrameLayout frame, boolean cancellable) {
        int pad = dp(24);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(pad, pad, pad, pad);
        TextView title = heading(getString(R.string.install_cover_title));
        title.setTextSize(22);
        page.addView(title, matchWidth());
        TextView intro = text(getString(R.string.install_cover_intro), D2Style.TEXT);
        intro.setPadding(0, dp(6), 0, dp(16));
        page.addView(intro, matchWidth());
        coverSteps = new LinearLayout(this);
        coverSteps.setOrientation(LinearLayout.VERTICAL);
        coverSteps.setPadding(dp(16), dp(12), dp(16), dp(12));
        android.graphics.drawable.GradientDrawable box = new android.graphics.drawable.GradientDrawable();
        box.setColor(0xFF16120D);
        box.setStroke(dp(1) * 2, D2Style.FRAME);
        coverSteps.setBackground(box);
        page.addView(coverSteps, matchWidth());
        coverStatus = text("", D2Style.GOLD);
        coverStatus.setPadding(0, dp(16), 0, dp(8));
        page.addView(coverStatus, matchWidth());
        coverBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        coverBar.setMax(100);
        coverBar.setProgressTintList(android.content.res.ColorStateList.valueOf(D2Style.GOLD));
        coverBar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(D2Style.FRAME));
        page.addView(coverBar, matchWidth());
        coverTotal = text("", D2Style.HINT);
        page.addView(coverTotal, matchWidth());
        if (cancellable) {
            Button cancel = new Button(this);
            cancel.setText(R.string.install_cancel);
            cancel.setOnClickListener((v) -> cancelDownloads());
            LinearLayout.LayoutParams lp = matchWidth();
            lp.topMargin = dp(24);
            page.addView(cancel, lp);
        }
        TextView show = text(getString(R.string.install_show_program), D2Style.GOLD);
        show.setPaintFlags(show.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        show.setPadding(0, dp(24), 0, dp(12));
        show.setOnClickListener((v) -> showProgram(frame));
        page.addView(show, matchWidth());
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setBackgroundColor(D2Style.BACKGROUND);
        scroll.setFillViewport(true);
        scroll.addView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        cover = scroll;
        frame.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        portrait();

        progressView.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence t, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence t, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable e) {
                refreshCover();
            }
        });
        refreshCover();
    }

    private static final int[] STEP_TEXT = {R.string.install_step_fetch, R.string.install_step_dl_lod, R.string.install_step_dl_classic,
        R.string.install_step_keys, R.string.install_step_inst_classic, R.string.install_step_inst_lod, R.string.install_step_patch,
        R.string.install_step_check};


    private void refreshCover() {
        if (cover == null || coverSteps == null) return;
        coverSteps.removeAllViews();
        for (D2InstallSteps.Step step : D2InstallSteps.Step.values()) {
            int state = steps.state(step);
            String mark = state == D2InstallSteps.DONE ? "✓  " : state == D2InstallSteps.NOW ? "▸  " : "·  ";
            String name = getString(STEP_TEXT[step.ordinal()]);
            if (state == D2InstallSteps.NOW && steps.percent() >= 0) name += "   " + steps.percent() + " %";
            TextView row = text(mark + name, state == D2InstallSteps.DONE ? D2Style.TEXT : state == D2InstallSteps.NOW ? D2Style.GOLD : D2Style.HINT);
            row.setTextSize(15);
            if (state == D2InstallSteps.NOW) row.setTypeface(Typeface.DEFAULT_BOLD);
            row.setPadding(0, dp(5), 0, dp(5));
            coverSteps.addView(row, matchWidth());
        }
        if (progressView != null) coverStatus.setText(progressView.getText());
        int total = steps.overall();
        coverBar.setProgress(total);
        coverTotal.setText(getString(R.string.install_cover_total, total));
    }


    private void showProgram(FrameLayout frame) {
        if (cover == null) return;
        D2Trace.log("install: the player shows the program");
        cover.setVisibility(android.view.View.GONE);
        if (programCancel != null) programCancel.setVisibility(android.view.View.VISIBLE);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        Button back = new Button(this);
        back.setText(R.string.install_back_to_page);
        back.setOnClickListener((v) -> {
            frame.removeView(back);
            if (cover != null) cover.setVisibility(android.view.View.VISIBLE);
            if (programCancel != null) programCancel.setVisibility(android.view.View.GONE);
            portrait();
        });
        frame.addView(back, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.BOTTOM | android.view.Gravity.START));
    }





    private void buildFailPage(D2InstallFailure.Kind kind, Object... args) {
        portrait();
        onFirstPage = false;
        shownFailure = kind;
        shownFailureArgs = args;
        int pad = dp(24);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(pad, pad, pad, pad);
        TextView title = heading(getString(R.string.install_failed_title));
        title.setTextSize(22);
        page.addView(title, matchWidth());
        boolean space = kind == D2InstallFailure.Kind.NO_SPACE || kind == D2InstallFailure.Kind.INSTALLER_NO_SPACE;
        Object[] shown = space ? spaceArgs(kind == D2InstallFailure.Kind.NO_SPACE && !retryKeys) : readable(args);
        String raw = getString(getResources().getIdentifier(kind.text, "string", getPackageName())), why;
        try {
            why = String.format(getResources().getConfiguration().getLocales().get(0), raw, shown);
        }
        catch (java.util.IllegalFormatException e) {
            why = raw.replaceAll("\\(?%\\d\\$[sd]\\)?", "").trim();
        }
        TextView reason = text(why, D2Style.TEXT);
        reason.setTextSize(16);
        reason.setPadding(dp(16), dp(14), dp(16), dp(14));
        android.graphics.drawable.GradientDrawable box = new android.graphics.drawable.GradientDrawable();
        box.setColor(0xFF1E0E0C);
        box.setStroke(dp(1) * 2, D2Style.RED);
        reason.setBackground(box);
        LinearLayout.LayoutParams rp = matchWidth();
        rp.topMargin = dp(12);
        page.addView(reason, rp);
        java.util.List<Button> buttons = new java.util.ArrayList<>();
        statusView = text("", D2Style.HINT);
        for (D2InstallFailure.Action action : kind.actions) {
            Button b = new Button(this);
            b.setText(getResources().getIdentifier(action.label, "string", getPackageName()));
            b.setOnClickListener((v) -> {

                if (action == D2InstallFailure.Action.PATCH) {
                    for (Button other : buttons) other.setEnabled(false);
                    statusView.setTextColor(D2Style.TEXT);
                    statusView.setText(R.string.install_patching);
                }
                act(action);
            });
            buttons.add(b);
            LinearLayout.LayoutParams bp = matchWidth();
            bp.topMargin = dp(action == kind.actions[0] ? 20 : 8);
            page.addView(b, bp);
        }
        statusView.setPadding(0, dp(12), 0, 0);
        page.addView(statusView, matchWidth());
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.setBackgroundColor(D2Style.BACKGROUND);
        scroll.addView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }


    private Object[] spaceArgs(boolean atStart) {
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        long needed = atStart ? neededAtStart() : neededBeforeKeys();
        return new Object[]{D2InstallPlan.formatGb(download().getUsableSpace(), locale), D2InstallPlan.formatGb(needed, locale)};
    }


    private static Object[] readable(Object[] args) {
        if (args == null) return new Object[0];
        Object[] out = args.clone();
        for (int i = 0; i < out.length; i++) if (out[i] == null || String.valueOf(out[i]).trim().isEmpty()) out[i] = "-";
        return out;
    }


    private void act(D2InstallFailure.Action action) {
        switch (action) {
            case AGAIN:
                stopped = false;
                if (retryKeys) buildKeysPage();
                else buildUI();
                break;
            case ANEW:
                File folder = installTarget;
                confirmAnew(R.string.install_anew_question_keep, () -> {
                    if (folder != null) D2InstallPlan.discardUnfinished(folder);
                });
                break;
            case KEYS:
                stopped = false;
                buildKeysPage();
                break;
            case PATCH:
                stopped = false;
                resumePatch(installTarget);
                break;
            case ACCESS:
                askStorage();
                break;
            case STORAGE:
                try {
                    startActivity(new android.content.Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
                }
                catch (android.content.ActivityNotFoundException e) {
                    startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS));
                }
                break;
            default:
                finish();
        }
    }



    private static final int STORAGE_REQUEST = 0x613;

    private boolean hasStorage() {
        return androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }


    private void askStorage() {
        if (hasStorage()) {
            buildUI();
            return;
        }
        if (asked && !androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.fromParts("package", getPackageName(), null));
            startActivity(intent);
            return;
        }
        asked = true;
        androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_REQUEST);
    }


    private boolean asked;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_REQUEST && screen == null) buildUI();
    }




    private void resumePatch(File dir) {
        if (busy) return;
        if (dir == null || !D2InstallPlan.awaitsPatch(dir)) {
            buildUI();
            return;
        }
        if (D2TouchOverlay.isGameRunning()) {
            failInstall(D2InstallFailure.Kind.RUNTIME, getString(R.string.install_game_running));
            return;
        }
        onFirstPage = false;

        language = D2InstallFlow.resumeLanguage(D2InstallPlan.folderLanguage(dir), language);
        installTarget = dir;
        if (!holdRun()) {
            failInstall(D2InstallFailure.Kind.RUNTIME, getString(R.string.launch_already_starting));
            return;
        }
        busy = true;
        keysStatusView = null;
        if (statusView != null) statusView.setText(R.string.install_patching);
        work(() -> {
            try {
                ensureRuntime();
                copyTools();
                runOnUiThread(() -> {
                    busy = false;
                    patch();
                });
            }
            catch (D2LaunchGate.Busy e) {
                runOnUiThread(() -> busy = false);
                failInstall(D2InstallFailure.Kind.RUNTIME, getString(R.string.launch_already_starting));
            }
            catch (IOException e) {
                runOnUiThread(() -> busy = false);
                failInstall(D2InstallFailure.Kind.RUNTIME, e.getMessage());
            }
        });
    }


    private FrameLayout.LayoutParams statusParams() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.Gravity.TOP | android.view.Gravity.END);
        if (GameLocator.gameDir(this) != null) lp.topMargin = dp(56);
        return lp;
    }

    private TextView heading(String s) {
        TextView t = text(s, D2Style.GOLD);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(12), 0, dp(4));
        return t;
    }

    private TextView text(String s, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        return t;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
