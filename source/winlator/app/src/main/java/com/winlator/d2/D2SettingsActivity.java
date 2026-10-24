package com.winlator.d2;

import com.winlator.R;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;


import java.io.File;
import java.io.InputStream;






public class D2SettingsActivity extends D2Activity {

    public static final String EXTRA_SAVES = "d2_saves";

    private android.view.View helpersPanel;

    @Override
    public void onBackPressed() {
        if (D2HelpersPanel.back(helpersPanel)) return;
        super.onBackPressed();
    }
    private static final int ID_AUTO = 1000, ID_LANGUAGE = 1100, ID_FRAME_RATE = 1200, IMPORT_REQUEST_CODE = 4720,
        FOLDER_REQUEST_CODE = 4721, INSTALL_REQUEST_CODE = 4722;

    private TextView savesStatus;

    private View loadButton;

    private View playButton;

    private D2SoundButton soundButton;
    private TextView folderStatus;
    private TextView backgroundStatus;
    private final Handler handler = new Handler(Looper.getMainLooper());


    private static final String STATE_GROUP = "d2_group";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String group = savedInstanceState != null ? savedInstanceState.getString(STATE_GROUP) : null;

        if (group == null && getIntent().getBooleanExtra(EXTRA_SAVES, false)) group = D2HelperEntries.SET_SAVES;
        buildPage(group);

        if (D2SettingsLook.get() == null) D2SettingsLook.load(this, () -> {
            if (!isFinishing() && !isDestroyed()) buildPage(D2HelpersPanel.group(helpersPanel));
        });
    }

    @Override
    protected boolean soundButtonOnTop() {
        return false;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);


        if (newConfig.orientation != builtFor && D2SettingsLook.get() != null) buildPage(D2HelpersPanel.group(helpersPanel));
    }


    private int builtFor;


    private void buildPage(String group) {
        builtFor = getResources().getConfiguration().orientation;
        D2SettingsLook look = D2SettingsLook.get();
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, 0);
        if (look != null) {

            root.setBackground(new CoverDrawable(look.backdrop));
        }
        else root.setBackgroundColor(D2Style.BACKGROUND);


        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        View title;
        if (look != null) title = new D2GameText(this, look, look.font30, 0xC8B074, getString(R.string.settings_title));
        else {
            TextView text = new TextView(this);
            text.setText(R.string.settings_title);
            text.setTextSize(22);
            D2Style.heading(text);
            title = text;
        }
        head.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Runnable play = () -> {

            if (D2Saves.importRunning()) {
                D2HelpersPanel.open(helpersPanel, D2HelperEntries.SET_SAVES);
                savesStatus(getString(R.string.saves_import_running));
                return;
            }
            startActivity(new Intent(this, D2LaunchActivity.class));
            finish();
        };
        if (look != null) playButton = new D2GameText.Stone(this, look, true, getString(R.string.settings_play), play);
        else {
            Button button = new Button(this);
            button.setText(getString(R.string.settings_play));
            button.setOnClickListener((v) -> play.run());
            playButton = button;
        }

        soundButton = new D2SoundButton(this);
        soundButton.onChange = () -> {
            if (D2HelperEntries.SET_GAME.equals(D2HelpersPanel.group(helpersPanel))) buildPage(D2HelperEntries.SET_GAME);
        };
        head.addView(soundButton);
        root.addView(head);

        savesStatus = null;
        folderStatus = null;
        backgroundStatus = null;
        loadButton = null;
        helpersPanel = D2HelpersPanel.create(this, null, null, true, null, null, this::hostView);
        root.addView(helpersPanel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout foot = new LinearLayout(this);
        foot.setGravity(android.view.Gravity.CENTER);
        foot.setPadding(0, dp(8), 0, dp(10));
        if (look != null) foot.setBackground(new FootDrawable());
        foot.addView(playButton);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fp.leftMargin = fp.rightMargin = -pad;
        root.addView(foot, fp);
        setContentView(root);
        lockForImport(D2Saves.importRunning());
        if (group != null) D2HelpersPanel.open(helpersPanel, group);
    }


    private final class FootDrawable extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint paint = new android.graphics.Paint();

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect b = getBounds();
            paint.setColor(0xC8000000);
            canvas.drawRect(b, paint);
            paint.setColor(0xFF8C7846);
            canvas.drawRect(b.left, b.top, b.right, b.top + Math.max(2, dp(1)), paint);
        }

        @Override
        public void setAlpha(int alpha) {}

        @Override
        public void setColorFilter(android.graphics.ColorFilter filter) {}

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }


    private static final class CoverDrawable extends android.graphics.drawable.Drawable {
        private final android.graphics.Bitmap picture;
        private final android.graphics.Paint pixels = new android.graphics.Paint();
        private final android.graphics.Rect from = new android.graphics.Rect();

        CoverDrawable(android.graphics.Bitmap picture) {
            this.picture = picture;
            pixels.setFilterBitmap(false);
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect to = getBounds();
            if (to.isEmpty()) return;
            float k = Math.max(to.width() / (float) picture.getWidth(), to.height() / (float) picture.getHeight());
            int w = Math.round(to.width() / k), h = Math.round(to.height() / k);
            from.set((picture.getWidth() - w) / 2, (picture.getHeight() - h) / 2, (picture.getWidth() + w) / 2, (picture.getHeight() + h) / 2);
            canvas.drawColor(D2Style.BACKGROUND);
            canvas.drawBitmap(picture, from, to, pixels);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(android.graphics.ColorFilter filter) {}
        @Override public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        String group = D2HelpersPanel.group(helpersPanel);
        if (group != null) out.putString(STATE_GROUP, group);
    }





    private View hostView(D2HelperEntries.Entry e) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        switch (e.key) {
            case "appLanguage": {
                box.addView(label(e.label));
                box.addView(hint(getString(R.string.settings_language_hint)));
                RadioGroup languageGroup = new RadioGroup(this);
                String[] languages = {D2Text.AUTO, D2Text.GERMAN, D2Text.ENGLISH, D2Text.FRENCH, D2Text.SPANISH, D2Text.ITALIAN};
                int[] languageLabels = {R.string.settings_language_auto, R.string.settings_language_de, R.string.settings_language_en,
                    R.string.settings_language_fr, R.string.settings_language_es, R.string.settings_language_it};
                String current = D2Settings.appLanguage(this);
                for (int i = 0; i < languages.length; i++) {
                    addOption(languageGroup, ID_LANGUAGE + i, getString(languageLabels[i]));
                    if (languages[i].equals(current)) languageGroup.check(ID_LANGUAGE + i);
                }
                languageGroup.setOnCheckedChangeListener((group, id) -> {
                    String chosen = languages[id - ID_LANGUAGE];
                    if (chosen.equals(D2Settings.appLanguage(this))) return;
                    D2Text.choose(this, chosen);
                    recreate();
                });
                box.addView(languageGroup);
                return box;
            }
            case "gameLines": {
                box.addView(label(e.label));
                box.addView(hint(getString(R.string.settings_size_hint)));
                RadioGroup sizeGroup = new RadioGroup(this);
                addOption(sizeGroup, D2Settings.LINES_540, getString(R.string.settings_size_540));
                addOption(sizeGroup, D2Settings.LINES_600, getString(R.string.settings_size_600));
                addOption(sizeGroup, ID_AUTO, getString(R.string.settings_size_auto));
                int lines = D2Settings.gameLines(this);
                sizeGroup.check(lines == D2Settings.LINES_AUTO ? ID_AUTO : lines);
                sizeGroup.setOnCheckedChangeListener((group, id) -> D2Settings.setGameLines(this, id == ID_AUTO ? D2Settings.LINES_AUTO : id));
                box.addView(sizeGroup);
                return box;
            }
            case "frameRate": {

                box.addView(label(e.label));
                box.addView(hint(getString(R.string.settings_frame_rate_hint)));
                RadioGroup frameRateGroup = new RadioGroup(this);

                float[] rates = D2Screen.refreshRates(this);
                int maxHz = Math.round(D2FrameRate.maxRate(rates));
                for (int fps : D2FrameRate.STEPS) {
                    String label = fps == D2FrameRate.UNLIMITED ? getString(R.string.settings_frame_rate_unlimited)
                        : getString(R.string.settings_frame_rate_fps, fps);
                    boolean offered = D2FrameRate.offered(fps, rates);
                    if (!offered) label = getString(R.string.settings_frame_rate_screen, label, maxHz);
                    addOption(frameRateGroup, ID_FRAME_RATE + fps, label);
                    View option = frameRateGroup.findViewById(ID_FRAME_RATE + fps);
                    option.setEnabled(offered);

                    if (!offered) option.setAlpha(0.45f);
                }
                frameRateGroup.check(ID_FRAME_RATE + D2Settings.frameRate(this, rates));
                frameRateGroup.setOnCheckedChangeListener((group, id) -> {
                    if (id != View.NO_ID) D2Settings.setFrameRate(this, id - ID_FRAME_RATE);
                });
                box.addView(frameRateGroup);
                return box;
            }
            case "sound": {
                LinearLayout soundBox = new LinearLayout(this);
                soundBox.setOrientation(LinearLayout.VERTICAL);
                soundBox.addView(check(e.label, D2Settings.sound(this), D2Settings.KEY_SOUND, () -> {
                    D2Music.soundChanged(this);
                    if (soundButton != null) soundButton.update();
                }));
                soundBox.addView(hint(getString(R.string.settings_sound_hint)));
                return soundBox;
            }
            case "skipIntro":
                return check(e.label, D2Settings.skipIntro(this), D2Settings.KEY_SKIP_INTRO);
            case "mod": {
                D2SettingsLook look = D2SettingsLook.get();
                if (look != null) return D2HelpersPanel.gameSwitch(this, look, e.label, D2Settings.modEnabled(this),
                    (on) -> D2Settings.setModEnabled(this, on));
                CheckBox modBox = new CheckBox(this);
                modBox.setText(e.label);
                modBox.setChecked(D2Settings.modEnabled(this));
                D2Style.control(modBox);
                modBox.setOnCheckedChangeListener((view, on) -> D2Settings.setModEnabled(this, on));
                return modBox;
            }
            case "plugy":
                if (!D2PlugY.isBundled(this)) return null;
                box.addView(check(e.label, D2Settings.plugy(this), D2Settings.KEY_PLUGY));
                box.addView(hint(getString(R.string.settings_plugy_hint, D2Saves.BACKUP_FOLDER)));
                return box;
            case "bigStash":
                return D2PlugY.isBundled(this) ? check(e.label, D2Settings.bigStash(this), D2Settings.KEY_BIG_STASH) : null;
            case "autosave":
                return D2PlugY.isBundled(this) ? check(e.label, D2Settings.autosave(this), D2Settings.KEY_AUTOSAVE) : null;
            case "respec":
                return D2PlugY.isBundled(this) ? check(e.label, D2Settings.respec(this), D2Settings.KEY_RESPEC) : null;
            case "statsPages":
                return D2PlugY.isBundled(this) ? check(e.label, D2Settings.statsPages(this), D2Settings.KEY_STATS_PAGES) : null;
            default:

                for (int i = 0; i < D2PlugY.EXTRA_KEYS.length; i++) {
                    String key = "plugy" + D2PlugY.EXTRA_KEYS[i];
                    if (!key.equals(e.key)) continue;
                    if (!D2PlugY.isBundled(this)) return null;
                    String iniKey = D2PlugY.EXTRA_KEYS[i];
                    Runnable changed = !D2PlugY.REGEN_MAP.equals(iniKey) ? null : () -> {

                        if (D2Settings.plugyExtraChanged(this, iniKey, D2Settings.regenMap(this)))
                            Toast.makeText(this, getString(R.string.resume_off_regen), Toast.LENGTH_LONG).show();
                        helpersPanel.post(() -> {
                            if (D2HelperEntries.SET_GAME.equals(D2HelpersPanel.group(helpersPanel))) buildPage(D2HelperEntries.SET_GAME);
                        });
                    };
                    return check(e.label, D2Settings.plugyExtra(this, iniKey, D2PlugY.EXTRA_DEFAULTS[i]), key, changed);
                }
                return null;
            case "background": {
                box.addView(label(e.label));
                backgroundStatus = new TextView(this);
                backgroundStatus.setText(getString(R.string.settings_background_status, D2BackgroundActivity.summary(this)));
                D2Style.hint(backgroundStatus);
                backgroundStatus.setPadding(dp(8), 0, 0, dp(4));
                box.addView(backgroundStatus);
                box.addView(button(getString(R.string.settings_background_open),
                    () -> startActivity(new Intent(this, D2BackgroundActivity.class))), matchWidth());
                return box;
            }
            case "gameFolder": {

                box.addView(label(e.label));
                folderStatus = new TextView(this);
                D2Style.text(folderStatus);
                box.addView(folderStatus);
                showFolder();
                box.addView(button(getString(R.string.folder_choose), this::chooseFolder), matchWidth());
                return box;
            }
            case "install":
                return button(e.label, this::install);
            case "shortcut": {
                box.addView(hint(getString(R.string.settings_shortcut_hint)));
                TextView shortcutStatus = new TextView(this);
                D2Style.text(shortcutStatus);
                box.addView(button(e.label, () -> shortcutStatus.setText(D2GameShortcut.offer(this))), matchWidth());
                box.addView(shortcutStatus);
                return box;
            }
            case "savesShare":
                box.addView(hint(getString(R.string.settings_saves_hint)));
                box.addView(button(e.label, this::shareSaves), matchWidth());
                return box;
            case "savesLoad": {
                loadButton = button(e.label, this::pickSavesZip);
                box.addView(loadButton, matchWidth());
                savesStatus = new TextView(this);
                D2Style.text(savesStatus);
                savesStatus.setText(savesLine);
                box.addView(savesStatus);
                lockForImport(D2Saves.importRunning());
                return box;
            }
            case "news":
                return button(e.label, () -> startActivity(new Intent(this, D2NewsActivity.class)));
            case "updateCheck":
                return check(e.label, D2Settings.updateCheck(this), D2Settings.KEY_UPDATE_CHECK);
            case "licenses":
                return button(e.label, () -> startActivity(new Intent(this, D2LicensesActivity.class)));
            case "about":
                box.addView(label(e.label));
                box.addView(hint(getString(R.string.about_disclaimer)));
                return box;
            case "editButtons":
                box.addView(label(e.label));
                box.addView(hint(getString(R.string.settings_edit_buttons_hint)));
                return box;
        }
    }

    private void chooseFolder() {

        if (D2TouchOverlay.isGameRunning()) {
            folderText(getString(R.string.folder_game_running));
            return;
        }


        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE}, FOLDER_REQUEST_CODE);
            return;
        }
        openFolderPicker();
    }


    private final D2FolderPick.LineKeeper folderKeeper = new D2FolderPick.LineKeeper();

    private Runnable pendingPick;


    private void folderText(String text) {
        folderLine = text;
        if (folderStatus != null) folderStatus.setText(text);
    }


    private View button(String text, Runnable action) {
        D2SettingsLook look = D2SettingsLook.get();

        if (look != null) return new D2GameText.Stone(this, look, true, text.replace(" …", "").replace(" ...", ""), action);
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener((v) -> action.run());
        return button;
    }

    private View label(String text) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look != null) {
            D2GameText game = new D2GameText(this, look, look.font16, 0xD2CEC6, text);
            game.setPadding(dp(8), dp(6), dp(4), 0);
            return game;
        }
        TextView view = new TextView(this);
        view.setText(text);
        D2Style.text(view);
        view.setPadding(dp(8), dp(6), 0, 0);
        return view;
    }

    private View hint(String text) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look != null) {
            D2GameText game = new D2GameText(this, look, look.formal10, D2SettingsLook.HINT_RGB, text);
            game.gain = D2SettingsLook.HINT_GAIN;
            game.setPadding(dp(10), 0, dp(4), dp(4));
            return game;
        }
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12);
        D2Style.hint(view);
        view.setPadding(dp(8), 0, 0, dp(4));
        return view;
    }

    @Override
    protected void onRestart() {
        super.onRestart();


        if (!folderKeeper.dropOnRestart()) D2Trace.log("settings restart after the picker's result: folder line kept");
        else folderLine = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (folderLine == null && folderStatus != null && folderStatus.isAttachedToWindow()) showFolder();
        folderKeeper.resumed();
        if (pendingPick != null) {
            Runnable pick = pendingPick;
            pendingPick = null;
            pick.run();
        }
        if (backgroundStatus != null) backgroundStatus.setText(getString(R.string.settings_background_status, D2BackgroundActivity.summary(this)));
        if (soundButton != null) soundButton.update();
    }


    private View check(String label, boolean checked, String key) {
        return check(label, checked, key, null);
    }

    private View check(String label, boolean checked, String key, Runnable onChange) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look != null) {
            D2GameText row = new D2GameText(this, look, look.font16, 0xD2CEC6, label);
            row.setPadding(dp(8), dp(8), dp(4), dp(4));
            boolean[] on = {checked};
            Runnable show = () -> row.setValue(getString(on[0] ? R.string.settings_on : R.string.settings_off), on[0] ? 0xC8B074 : 0x707070);
            show.run();
            row.setClickable(true);
            row.setOnClickListener((v) -> {
                on[0] = !on[0];
                D2Settings.putBoolean(this, key, on[0]);
                show.run();
                if (onChange != null) onChange.run();
            });
            return row;
        }
        CheckBox box = new CheckBox(this);
        box.setText(label);
        D2Style.control(box);
        box.setChecked(checked);
        box.setOnCheckedChangeListener((view, on) -> {
            D2Settings.putBoolean(this, key, on);
            if (onChange != null) onChange.run();
        });
        return box;
    }


    private void install() {

        if (D2TouchOverlay.isGameRunning()) {

            askQuitThenInstall();
            return;
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE}, INSTALL_REQUEST_CODE);
            return;
        }
        startActivity(new Intent(this, D2InstallActivity.class));
    }






    private void askQuitThenInstall() {
        TextView message = new TextView(this);

        boolean saves = !D2TouchOverlay.quitWouldLose();
        message.setText(getString(saves ? R.string.install_game_running_ask : R.string.install_game_running_ask_lose));
        message.setTextSize(16);
        int pad = dp(16);
        message.setPadding(pad, pad, pad, 0);
        D2Style.text(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(message)
            .setPositiveButton(getString(saves ? R.string.install_quit_and_install : R.string.install_quit_and_install_lose), (d, which) -> {
                if (!D2TouchOverlay.isGameRunning()) {
                    install();
                    return;
                }
                startActivity(new Intent(this, D2GameActivity.class).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    .putExtra(D2KeepAliveService.EXTRA_SAVE_AND_EXIT, true)
                    .putExtra(D2KeepAliveService.EXTRA_THEN_INSTALL, true));
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .create();
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
    }

    private void openFolderPicker() {
        try {
            startActivityForResult(GameLocator.pickerIntent(), FOLDER_REQUEST_CODE);
        }
        catch (android.content.ActivityNotFoundException e) {
            folderText(getString(R.string.folder_no_picker));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == INSTALL_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(this, D2InstallActivity.class));
            }
            else folderText(getString(R.string.launch_no_storage));
            return;
        }
        if (requestCode != FOLDER_REQUEST_CODE) return;
        if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) openFolderPicker();
        else {
            folderKeeper.written();
            folderText(getString(R.string.launch_no_storage));
        }
    }


    private void showFolder() {
        if (folderStatus == null) return;


        if (folderLine == null) {
            File dir = GameLocator.gameDir(this);
            if (dir == null) {
                String chosen = GameLocator.chosenPath(this);
                folderLine = chosen != null ? getString(R.string.folder_gone, chosen) : getString(R.string.saves_no_game_folder);
            }
            else {
                GameVersion.Result check = GameLocator.check(this, dir);
                folderLine = check.dir.getAbsolutePath() + "\n" + GameLocator.message(this, check);
            }
        }
        folderStatus.setText(folderLine);
    }


    private String folderLine, savesLine = "";



    private void savesStatus(String text) {
        handler.post(() -> {
            savesLine = text;
            if (savesStatus != null) savesStatus.setText(text);
        });
    }


    private void shareSaves() {
        File gameDir = GameLocator.gameDir(this);
        if (gameDir == null) {
            savesStatus(getString(R.string.saves_no_game_folder));
            return;
        }
        savesStatus(getString(R.string.saves_packing));
        new Thread(() -> {

            D2FolderLock.Decision lock = D2FolderLock.acquire(getPackageName(), appLabel(this), gameDir, "share");
            if (lock.foreign || lock.unusable) {
                savesStatus(folderLockText(this, lock, false));
                return;
            }
            try {
                File folder = new File(getFilesDir(), "share");
                File[] old = folder.listFiles();
                if (old != null) for (File file : old) file.delete();
                File zip = new File(folder, D2Saves.zipName("Diablo2-Spielstaende"));


                if (!D2TouchOverlay.isGameRunning() && D2Saves.recover(D2Saves.saveDir(gameDir))) D2SaveRepair.run(D2Saves.saveDir(gameDir));
                int count = D2Saves.zip(D2Saves.saveDir(gameDir), zip);
                if (count == 0) {
                    savesStatus(getString(R.string.saves_none_found, D2Saves.saveDir(gameDir)));
                    return;
                }
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".FileProvider", zip);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("application/zip");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.saves_subject));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                handler.post(() -> {
                    savesLine = getResources().getQuantityString(R.plurals.saves_packed, count, count);
                    if (savesStatus != null) savesStatus.setText(savesLine);
                    startActivity(Intent.createChooser(send, getString(R.string.settings_saves_share)));
                });
            }
            catch (Exception e) {
                savesStatus(getString(R.string.saves_share_failed, savesError(e)));
            }
            finally {
                D2FolderLock.release(gameDir);
            }
        }, "D2SavesShare").start();
    }


    private void lockForImport(boolean importing) {
        if (loadButton != null) loadButton.setEnabled(!importing);
        if (playButton != null) playButton.setEnabled(!importing);

        if (importing) handler.postDelayed(() -> lockForImport(D2Saves.importRunning()), 500);
    }

    private void pickSavesZip() {
        if (D2TouchOverlay.isGameRunning()) {
            savesStatus(getString(R.string.saves_game_running));
            return;
        }
        if (D2Saves.importRunning()) {
            savesStatus(getString(R.string.saves_import_running));
            return;
        }
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed", "application/octet-stream"});
        try {
            startActivityForResult(pick, IMPORT_REQUEST_CODE);
        }
        catch (RuntimeException e) {
            savesStatus(getString(R.string.saves_no_picker));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FOLDER_REQUEST_CODE) {
            D2Trace.log("folder picker answered: result " + resultCode + ", " + (data != null && data.getData() != null
                ? data.getData().getAuthority() : "no folder"));
            if (resultCode != RESULT_OK && (data == null || data.getData() == null)) return;
            folderKeeper.written();
            File dir = GameLocator.pickedDir(data);
            if (dir == null) {
                folderText(getString(R.string.folder_no_path));
                pendingPick = () -> D2FolderPickDialog.show(this, null, null, null, this::chooseFolder);
                return;
            }
            String before = GameLocator.chosenPath(this);
            GameVersion.Result check = GameLocator.choose(this, dir);
            boolean usable = check.status == GameVersion.Status.OK || check.status == GameVersion.Status.MODIFIED;

            if (!usable && D2FolderPick.gamesBelow(dir).size() >= 2) folderLine = null;
            else folderText(check.dir.getAbsolutePath() + "\n" + GameLocator.message(this, check));

            pendingPick = () -> D2FolderPickDialog.show(this, dir, check, before, this::chooseFolder);
            return;
        }
        if (requestCode != IMPORT_REQUEST_CODE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();


        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        content.setPadding(pad, pad, pad, 0);
        TextView title = new TextView(this);
        title.setText(getString(R.string.saves_load_title));
        title.setTextSize(18);
        D2Style.heading(title);
        content.addView(title);
        TextView message = new TextView(this);
        message.setText(getString(R.string.saves_load_message, D2Saves.BACKUP_FOLDER));
        message.setTextSize(16);
        message.setPadding(0, dp(8), 0, 0);
        D2Style.text(message);
        content.addView(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(content)
            .setPositiveButton(getString(R.string.saves_load), (d, which) -> importSaves(uri))
            .setNegativeButton(getString(R.string.cancel), null)
            .create();
        dialog.show();
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
    }

    private void importSaves(Uri uri) {
        File gameDir = GameLocator.gameDir(this);
        if (gameDir == null) {
            savesStatus(getString(R.string.saves_no_game_folder));
            return;
        }


        if (D2TouchOverlay.isGameRunning()) {
            savesStatus(getString(R.string.saves_game_running));
            return;
        }
        if (!D2Saves.beginImport()) {
            savesStatus(getString(D2Saves.importRunning() ? R.string.saves_import_running : R.string.saves_game_running));
            return;
        }
        lockForImport(true);
        savesStatus(getString(R.string.saves_loading));
        new Thread(() -> {

            D2FolderLock.Decision lock = D2FolderLock.acquire(getPackageName(), appLabel(this), gameDir, "import");
            if (lock.foreign || lock.unusable) {
                savesStatus(folderLockText(this, lock, false));
                D2Saves.endImport();
                handler.post(() -> lockForImport(false));
                return;
            }
            try {
                File saveDir = D2Saves.saveDir(gameDir);
                if (D2Saves.recover(saveDir)) D2SaveRepair.run(saveDir);
                File backup;
                try {
                    backup = D2Saves.backup(saveDir, "vor-Laden");
                }
                catch (Exception e) {
                    savesStatus(getString(R.string.saves_backup_failed, savesError(e)));
                    return;
                }
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new D2Saves.Failure(D2Saves.Failure.Reason.UNREADABLE, null);
                    int count = D2Saves.importZip(in, saveDir);
                    savesStatus(getString(R.string.saves_loaded, count, backup != null ? getString(R.string.saves_backup_note, D2Saves.BACKUP_FOLDER + "/" + backup.getName()) : ""));
                }
                catch (Exception e) {
                    savesStatus(getString(R.string.saves_load_failed, savesError(e), backup != null ? getString(R.string.saves_backup_paren, backup.getName()) : ""));
                }
            }
            finally {
                D2FolderLock.release(gameDir);
                D2Saves.endImport();
                handler.post(() -> lockForImport(false));
            }
        }, "D2SavesImport").start();
    }


    static String appLabel(android.content.Context context) {
        return String.valueOf(context.getApplicationInfo().loadLabel(context.getPackageManager()));
    }






    static String folderLockText(android.content.Context context, D2FolderLock.Decision lock, boolean play) {
        if (lock.unusable) return context.getString(R.string.folder_lock_unusable);
        if (lock.label == null || lock.label.isEmpty() || lock.since <= 0) {
            return context.getString(play ? R.string.folder_lock_play_plain : R.string.folder_lock_retry_plain);
        }
        String since = android.text.format.DateFormat.getTimeFormat(context).format(new java.util.Date(lock.since));
        return context.getString(play ? R.string.folder_lock_play : R.string.folder_lock_retry, lock.label, since);
    }


    private String savesError(Exception e) {
        return savesError(this, e);
    }


    static String savesError(android.content.Context context, Exception e) {
        if (!(e instanceof D2Saves.Failure)) {

            String message = e.getMessage();
            return message != null && !message.isEmpty() ? message : e.getClass().getSimpleName();
        }
        D2Saves.Failure f = (D2Saves.Failure) e;
        switch (f.reason) {
            case UNREADABLE: return D2Text.get(context, R.string.saves_error_unreadable);
            case CANNOT_CREATE: return D2Text.get(context, R.string.saves_error_create, f.subject);
            case ZIP_TOO_LARGE: return D2Text.get(context, R.string.saves_error_zip_too_large);
            case NO_CHARACTER: return D2Text.get(context, R.string.saves_error_no_character);
            case CANNOT_REPLACE: return D2Text.get(context, R.string.saves_error_replace, f.subject);
            case CANNOT_WRITE: return D2Text.get(context, R.string.saves_error_write, f.subject);
            case FILE_TOO_LARGE: return D2Text.get(context, R.string.saves_error_file_too_large);
            case IMPORT_UNFINISHED: return D2Text.get(context, R.string.saves_error_import_unfinished, D2Saves.BACKUP_FOLDER);
            case BROKEN_ZIP: return D2Text.get(context, R.string.saves_error_broken_zip);
            default: return f.getMessage();
        }
    }

    private void addOption(RadioGroup group, int id, String label) {
        RadioButton button = new RadioButton(this);
        button.setId(id);
        button.setText(label);
        D2Style.control(button);
        group.addView(button);
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
