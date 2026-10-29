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
import java.io.IOException;
import java.io.InputStream;






public class D2SettingsActivity extends D2Activity {

    public static final String EXTRA_SAVES = "d2_saves";

    public static final String EXTRA_MODS = "d2_mods";

    private android.view.View helpersPanel;

    @Override
    public void onBackPressed() {
        if (D2HelpersPanel.back(helpersPanel)) return;
        super.onBackPressed();
    }




    private D2PadHint.Bar padBar;
    private boolean padOn;





    @Override
    D2PadPages.Page padPage() {
        return settingsPad;
    }

    private final D2PadPages.Page settingsPad = new D2PadPages.Page() {
        @Override
        public View padFirstFocus() {
            return D2HelpersPanel.firstFocus(helpersPanel);
        }

        @Override
        public boolean onPad(int action, View focused) {
            if (action == D2PadKeys.LB || action == D2PadKeys.RB) return D2HelpersPanel.step(helpersPanel, action == D2PadKeys.LB ? -1 : 1);
            if (action == D2PadKeys.B) return D2HelpersPanel.padBack(helpersPanel);
            if (action == D2PadKeys.START) {
                if (playButton != null && playButton.isEnabled()) playButton.performClick();
                return true;
            }
            return false;
        }

        @Override
        public void padActive(boolean active) {
            if (active == padOn) return;
            padOn = active;
            if (active) updatePadHint();
            else if (padBar != null) padBar.show(null);
        }
    };

    private void updatePadHint() {
        D2SettingsLook look = D2SettingsLook.get();
        if (padBar == null) return;
        if (look == null) {
            padBar.show(null);
            return;
        }
        View focus = getCurrentFocus();
        boolean stepper = focus instanceof D2PadPages.Stepper && ((D2PadPages.Stepper) focus).padSteps();
        int[] items = D2PadNav.settingsHints(stepper, focus != null && !focus.isInTouchMode(), true);
        int[] glyphs = new int[items.length / 2];
        String[] words = new String[glyphs.length];
        for (int i = 0; i < glyphs.length; i++) {
            glyphs[i] = items[2 * i];
            words[i] = padWord(items[2 * i + 1]);
        }
        padBar.show(D2PadHint.picture(look.formal10, glyphs, words));
    }

    private String padWord(int word) {
        switch (word) {
            case D2PadNav.WORD_STEP: return getString(R.string.pad_hint_step);
            case D2PadNav.WORD_CHOOSE: return getString(R.string.pad_hint_choose);
            case D2PadNav.WORD_GROUP: return getString(R.string.pad_hint_group);
            case D2PadNav.WORD_BACK: return getString(R.string.pad_hint_back);
            case D2PadNav.WORD_PLAY: return getString(R.string.settings_play);
            default: return "";
        }
    }
    private static final int ID_AUTO = 1000, ID_LANGUAGE = 1100, ID_FRAME_RATE = 1200, IMPORT_REQUEST_CODE = 4720,
        FOLDER_REQUEST_CODE = 4721, INSTALL_REQUEST_CODE = 4722, MOD_REQUEST_CODE = 4723;

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

        getWindow().getDecorView().getViewTreeObserver().addOnGlobalFocusChangeListener((from, to) -> {
            if (padOn) updatePadHint();
        });
        String group = savedInstanceState != null ? savedInstanceState.getString(STATE_GROUP) : null;

        if (group == null && getIntent().getBooleanExtra(EXTRA_SAVES, false)) group = D2HelperEntries.SET_SAVES;

        if (group == null && getIntent().getBooleanExtra(EXTRA_MODS, false)) group = D2HelperEntries.SET_MODS;
        buildPage(group);



        if (builtPlain) D2SettingsLook.load(this, () -> {
            if (!isFinishing() && !isDestroyed()) buildPage(D2HelpersPanel.group(helpersPanel));
        });
    }

    @Override
    protected boolean soundButtonOnTop() {
        return false;
    }





    private boolean builtPlain;


    private void buildPage(String group) {
        spielweiseViews.clear();
        D2SettingsLook look = D2SettingsLook.get();
        builtPlain = look == null;
        int pad = dp(16);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, 0);
        root.setClipToPadding(false);
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
        argsLine = argsWarnings = null;
        modsStatus = null;
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

        padBar = new D2PadHint.Bar(this);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.leftMargin = bp.rightMargin = -pad;
        root.addView(padBar, bp);
        setContentView(root);
        if (padOn) updatePadHint();
        lockForImport(D2Saves.importRunning());
        if (group != null) D2HelpersPanel.open(helpersPanel, group);
    }





    private final class FootDrawable extends android.graphics.drawable.Drawable {
        private final android.graphics.Paint paint = new android.graphics.Paint();
        private final android.graphics.Paint shade = new android.graphics.Paint();
        private int shadeTop = Integer.MIN_VALUE, shadeBottom = Integer.MIN_VALUE;

        @Override
        public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect b = getBounds();
            if (b.top != shadeTop || b.bottom != shadeBottom) {
                shadeTop = b.top;
                shadeBottom = b.bottom;
                shade.setShader(new android.graphics.LinearGradient(0, b.top, 0, b.bottom, 0x00000000, 0x99000000,
                    android.graphics.Shader.TileMode.CLAMP));
            }
            canvas.drawRect(b, shade);
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
                String gameLanguage = gameLanguageName();
                if (gameLanguage != null) box.addView(hint(getString(R.string.settings_language_game, gameLanguage)));
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
            case "swPreset":
                spielweiseKeep(box, () -> spielweisePreset(box));
                return spielweisePreset(box);
            case "swQuality": case "swNoDrop": case "swRunes": case "swBoss": case "swUniques": case "swGamble": case "swExp": case "swDensity": case "swLadder": case "swCow": case "swUber":
            case "swWorld": case "swShared": case "swSort":
                return spielweiseRow(D2Spielweise.byKey(e.key), e.label);
            case "argDirect":
                box.addView(hint(getString(R.string.settings_mods_start_hint)));
                box.addView(hint(getString(R.string.settings_mods_by_hand)));
                box.addView(check(e.label, D2Settings.argDirect(this), D2Settings.argKey(this, D2Settings.KEY_ARG_DIRECT), this::argsChanged));
                box.addView(hint(getString(R.string.settings_arg_direct_hint)));
                return box;
            case "argTxt":
                box.addView(check(e.label, D2Settings.argTxt(this), D2Settings.argKey(this, D2Settings.KEY_ARG_TXT), this::argsChanged));
                box.addView(hint(getString(R.string.settings_arg_txt_hint)));
                return box;
            case "argNs":
                box.addView(check(e.label, D2Settings.argNoSound(this), D2Settings.argKey(this, D2Settings.KEY_ARG_NS), this::argsChanged));
                box.addView(hint(getString(R.string.settings_arg_ns_hint)));
                return box;
            case "argsFree":
                return argsFree(box, e.label);
            case "modsList":
                return modsList(box);
            case "modsImport":
                return modsImport(box, e.label);
            case "skipIntro":
                return check(e.label, D2Settings.skipIntro(this), D2Settings.KEY_SKIP_INTRO);
            case "mod":
                return guardedRow(GUARD_MOD, e.label);
            case "plugy":
                if (!D2PlugY.isBundled(this)) return null;
                box.addView(guardedRow(GUARD_PLUGY, e.label));
                box.addView(hint(getString(R.string.settings_plugy_hint, D2Saves.BACKUP_FOLDER)));
                box.addView(hint(getString(R.string.settings_plugy_moved)));
                box.addView(hint(getString(R.string.settings_big_stash_moved)));
                return box;
            case "bigInventory":
                return GameShadow.isModBundled(this) ? guardedRow(GUARD_BIG_INV, e.label) : null;
            case "bigStash":
                return D2PlugY.isBundled(this) ? bigStashRow(e.label) : null;
            case "autosave":
                return D2PlugY.isBundled(this) ? check(e.label, D2Settings.autosave(this), D2Settings.KEY_AUTOSAVE) : null;
            case "respec":
                return D2PlugY.isBundled(this) ? spielweiseRow(D2Spielweise.RESPEC, e.label) : null;
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
                    View row = check(e.label, D2Settings.plugyExtra(this, iniKey, D2PlugY.EXTRA_DEFAULTS[i]), key, changed);

                    if ("ActiveSharedGold".equals(iniKey) && !D2Settings.sharedStash(this)) {
                        row.setEnabled(false);
                        row.setClickable(false);
                        row.setAlpha(0.45f);
                        box.addView(row);
                        box.addView(hint(getString(R.string.settings_shared_gold_off)));
                        return box;
                    }
                    return row;
                }
                return null;
            case "background": {
                box.addView(label(e.label));
                backgroundStatus = new TextView(this);
                backgroundStatus.setText(getString(R.string.settings_background_status, D2BackgroundActivity.summary(this)));
                D2Style.hint(backgroundStatus);
                backgroundStatus.setPadding(dp(8), 0, 0, dp(4));
                box.addView(backgroundStatus);
                box.addView(button(D2ButtonTiers.TEXT, getString(R.string.settings_background_open),
                    () -> startActivity(new Intent(this, D2BackgroundActivity.class))), matchWidth());
                return box;
            }
            case "gameFolder": {

                box.addView(label(e.label));
                folderStatus = new TextView(this);
                D2Style.text(folderStatus);
                box.addView(folderStatus);
                showFolder();
                box.addView(button(D2ButtonTiers.PLAIN, getString(R.string.folder_choose), this::chooseFolder), matchWidth());
                return box;
            }
            case "install":
                return button(D2ButtonTiers.PLAIN, e.label, this::install);
            case "shortcut": {
                box.addView(hint(getString(R.string.settings_shortcut_hint)));
                TextView shortcutStatus = new TextView(this);
                D2Style.text(shortcutStatus);
                box.addView(button(D2ButtonTiers.PLAIN, e.label, () -> shortcutStatus.setText(D2GameShortcut.offer(this))), matchWidth());
                box.addView(shortcutStatus);
                return box;
            }
            case "savesShare":
                box.addView(hint(getString(R.string.settings_saves_hint)));
                box.addView(button(D2ButtonTiers.PLAIN, e.label, this::shareSaves), matchWidth());
                return box;
            case "savesLoad": {
                loadButton = button(D2ButtonTiers.PLAIN, e.label, this::pickSavesZip);
                box.addView(loadButton, matchWidth());
                savesStatus = new TextView(this);
                D2Style.text(savesStatus);
                savesStatus.setText(savesLine);
                box.addView(savesStatus);
                lockForImport(D2Saves.importRunning());
                return box;
            }
            case "news":
                return button(D2ButtonTiers.TEXT, e.label, () -> startActivity(new Intent(this, D2NewsActivity.class)));
            case "updateCheck":
                return check(e.label, D2Settings.updateCheck(this), D2Settings.KEY_UPDATE_CHECK);
            case "telemetry":
                box.addView(check(e.label, D2Settings.telemetry(this), D2Settings.KEY_TELEMETRY, () -> {
                    if (!D2Settings.telemetry(this)) D2Telemetry.clear(this);
                }));
                box.addView(hint(getString(R.string.settings_telemetry_hint)));
                return box;
            case "licenses":
                return button(D2ButtonTiers.TEXT, e.label, () -> startActivity(new Intent(this, D2LicensesActivity.class)));
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


    private View button(int kind, String text, Runnable action) {
        D2SettingsLook look = D2SettingsLook.get();
        if (look != null) return D2ButtonTiers.make(this, look, kind, text, action);
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





    private String gameLanguageName() {
        String iso = D2AppLanguage.gameIso(D2Settings.gameLanguage(this));
        if (iso == null) return null;
        switch (iso) {
            case D2AppLanguage.GERMAN: return getString(R.string.settings_language_de);
            case D2AppLanguage.ENGLISH: return getString(R.string.settings_language_en);
            case D2AppLanguage.FRENCH: return getString(R.string.settings_language_fr);
            case D2AppLanguage.SPANISH: return getString(R.string.settings_language_es);
            case D2AppLanguage.ITALIAN: return getString(R.string.settings_language_it);
            default: {
                java.util.Locale app = getResources().getConfiguration().getLocales().get(0);
                String name = new java.util.Locale(iso).getDisplayLanguage(app);
                return name.isEmpty() ? null : name.substring(0, 1).toUpperCase(app) + name.substring(1);
            }
        }
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
    protected void onPause() {
        pageResumed = false;
        super.onPause();
    }


    private boolean pageResumed;

    @Override
    protected void onResume() {
        super.onResume();
        pageResumed = true;

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




    private TextView argsLine, argsWarnings;


    private View argsFree(LinearLayout box, String label) {
        box.addView(label(label));
        box.addView(hint(getString(R.string.settings_args_free_hint)));
        android.widget.EditText field = new android.widget.EditText(this);
        field.setSingleLine(true);
        field.setText(D2Settings.argsFree(this));
        field.setHint(getString(R.string.settings_args_free_example));
        field.setTextColor(0xFFC8B074);
        field.setHintTextColor(D2Style.HINT);
        field.setTextSize(14);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        field.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE | android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        field.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence t, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence t, int a, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable text) {
                D2Settings.setArgsFree(D2SettingsActivity.this, text.toString());
                argsChanged();
            }
        });
        LinearLayout.LayoutParams fp = matchWidth();
        fp.leftMargin = dp(8);
        box.addView(field, fp);
        argsLine = new TextView(this);
        argsLine.setTextSize(12);
        argsLine.setTextColor(D2Style.GOLD);
        argsLine.setPadding(dp(8), dp(2), 0, dp(2));
        box.addView(argsLine);
        argsWarnings = new TextView(this);
        argsWarnings.setTextSize(12);
        argsWarnings.setTextColor(D2Style.RED);
        argsWarnings.setPadding(dp(8), 0, 0, dp(4));
        box.addView(argsWarnings);
        argsChanged();
        return box;
    }


    private void argsChanged() {
        if (argsLine == null) return;
        argsLine.setText(getString(R.string.settings_args_line, D2Settings.gameArgs(this)));
        D2StartArgs.Parsed parsed = D2StartArgs.parse(D2Settings.argsFree(this));
        StringBuilder warn = new StringBuilder();
        for (Object[] w : parsed.warnings) {
            int id;
            switch ((Integer) w[1]) {
                case D2StartArgs.WARN_WINDOW: id = R.string.settings_args_warn_window; break;
                case D2StartArgs.WARN_RENDERER: id = R.string.settings_args_warn_renderer; break;
                case D2StartArgs.WARN_BATTLENET: id = R.string.settings_args_warn_battlenet; break;
                default: id = R.string.settings_args_warn_fixed;
            }
            if (warn.length() > 0) warn.append('\n');
            warn.append(getString(id, (String) w[0]));
        }
        if (!parsed.dropped.isEmpty()) {
            if (warn.length() > 0) warn.append('\n');
            warn.append(getString(R.string.settings_args_dropped, String.join(" ", parsed.dropped)));
        }
        argsWarnings.setText(warn);
        argsWarnings.setVisibility(warn.length() > 0 ? View.VISIBLE : View.GONE);
    }




    private TextView modsStatus;
    private static volatile String modsLine;
    private static volatile boolean modImportRunning;


    private View modsList(LinearLayout box) {
        box.addView(hint(getString(R.string.settings_mods_intro)));
        D2ModInfo.Mod active = D2Mods.active(this);
        box.addView(gemRow(D2StartArt.GEM_ORIGINAL, getString(R.string.settings_mods_original), active == null, () -> chooseMod(null)));
        box.addView(hint(getString(R.string.settings_mods_original_hint)));
        for (D2ModInfo.Mod mod : D2Mods.list()) {
            boolean on = active != null && active.id.equals(mod.id);
            box.addView(gemRow(D2StartArt.GEM_UNCHECKED, mod.name, on, () -> chooseMod(mod.id)));
            int characters = D2SaveHeader.list(mod.saves()).size();
            box.addView(hint(getString(R.string.settings_mods_data_unchecked, getResources().getQuantityString(R.plurals.settings_mods_characters, characters, characters))));
            String proposed = (mod.direct ? D2StartArgs.DIRECT + " " : "") + (mod.txt ? D2StartArgs.TXT : "");
            if (!proposed.trim().isEmpty()) box.addView(hint(getString(R.string.settings_mods_proposed, proposed.trim())));

            if (mod.inventory) box.addView(hint(getString(R.string.settings_mods_own_grid)));
            else if (mod.archives) box.addView(hint(getString(R.string.settings_mods_archives_grid)));
            if (on) box.addView(hint(getString(R.string.settings_mods_spielweise_off)));
            box.addView(button(D2ButtonTiers.PLAIN, getString(R.string.settings_mods_remove), () -> askRemoveMod(mod)), matchWidth());
        }
        box.addView(hint(getString(R.string.settings_mods_legend)));
        return box;
    }


    private View gemRow(int gem, String name, boolean on, Runnable choose) {
        D2SettingsLook look = D2SettingsLook.get();
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.graphics.Bitmap picture = look != null && look.gems[gem] != null ? look.gems[gem] : null;
        if (picture != null) {
            android.widget.ImageView icon = new android.widget.ImageView(this);
            android.graphics.drawable.BitmapDrawable d = new android.graphics.drawable.BitmapDrawable(getResources(), picture);
            d.setFilterBitmap(false);
            icon.setImageDrawable(d);
            icon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            int k = Math.max(1, look.scale);
            row.addView(icon, new LinearLayout.LayoutParams(picture.getWidth() * k, picture.getHeight() * k));
        }
        View text;
        if (look != null) {
            D2GameText line = new D2GameText(this, look, look.font16, 0xD2CEC6, name);
            line.setPadding(dp(8), dp(8), dp(4), dp(4));
            if (on) line.setValue(getString(R.string.settings_mods_active), 0xC8B074);
            text = line;
        }
        else {
            TextView line = new TextView(this);
            line.setText(on ? name + "  (" + getString(R.string.settings_mods_active) + ")" : name);
            D2Style.text(line);
            line.setPadding(dp(8), dp(8), dp(4), dp(4));
            text = line;
        }
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.setClickable(true);
        row.setOnClickListener((v) -> choose.run());
        return row;
    }


    private void chooseMod(String id) {
        if (D2TouchOverlay.isGameRunning()) {
            Toast.makeText(this, R.string.settings_mods_game_running, Toast.LENGTH_LONG).show();
            return;
        }
        D2ModInfo.Mod before = D2Mods.active(this);
        if (id == null ? before == null : before != null && before.id.equals(id)) return;

        if (!D2Saves.beginImport()) {
            Toast.makeText(this, R.string.settings_mods_busy, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            D2Mods.setActive(this, id);
        }
        finally {
            D2Saves.endImport();
        }
        D2Trace.log("mods: " + (id == null ? "original" : id) + " chosen");
        buildPage(D2HelperEntries.SET_MODS);
    }

    private void askRemoveMod(D2ModInfo.Mod mod) {
        if (D2TouchOverlay.isGameRunning()) {
            Toast.makeText(this, R.string.settings_mods_game_running, Toast.LENGTH_LONG).show();
            return;
        }
        TextView message = new TextView(this);
        message.setText(getString(R.string.settings_mods_remove_ask, mod.name, D2Saves.BACKUP_FOLDER));
        message.setTextSize(16);
        int pad = dp(16);
        message.setPadding(pad, pad, pad, 0);
        D2Style.text(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(message)
            .setPositiveButton(getString(R.string.settings_mods_remove_yes), (d, which) -> removeMod(mod))
            .setNegativeButton(getString(R.string.cancel), null)
            .create();
        dialog.show();
        D2Sound.attach(dialog);
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button b = dialog.getButton(which);
            if (b != null) b.setTextColor(D2Style.GOLD);
        }
        D2ButtonTiers.dialog(dialog, dialog.getButton(AlertDialog.BUTTON_POSITIVE), dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
    }


    private void removeMod(D2ModInfo.Mod mod) {
        File modDir = mod.dir;
        new Thread(() -> {
            String line;


            if (!D2Saves.beginImport()) {
                modsStatus(getString(R.string.settings_mods_busy));
                return;
            }
            D2FolderLock.Decision lock = D2FolderLock.acquire(getPackageName(), appLabel(this), modDir, "remove mod " + mod.id);
            try {
                if (lock.foreign) throw new IOException(folderLockText(this, lock, false));

                if (D2Saves.recover(mod.saves())) D2SaveRepair.run(mod.saves());
                File backup = D2Saves.newestSaveFile(mod.saves()) > 0 ? D2Saves.backup(mod.saves(), "vor-Mod-entfernen-" + mod.id) : null;
                D2Mods.remove(this, mod);
                line = getString(R.string.settings_mods_removed, mod.name)
                    + (backup != null ? " " + getString(R.string.saves_backup_note, D2Saves.BACKUP_FOLDER + "/" + backup.getName()) : "");
                D2Trace.log("mods: " + mod.id + " removed");
            }
            catch (IOException e) {
                line = getString(R.string.settings_mods_remove_failed, String.valueOf(e.getMessage()));
            }
            finally {
                if (!lock.foreign && !lock.unusable) D2FolderLock.release(modDir);
                D2Saves.endImport();
            }
            String shown = line;
            handler.post(() -> {
                modsLine = shown;
                if (!isFinishing() && !isDestroyed()) buildPage(D2HelperEntries.SET_MODS);
            });
        }).start();
    }


    private View modsImport(LinearLayout box, String label) {
        box.addView(hint(getString(R.string.settings_mods_import_hint)));
        box.addView(button(D2ButtonTiers.TEXT, label, this::pickMod), matchWidth());
        modsStatus = new TextView(this);
        D2Style.text(modsStatus);
        modsStatus.setTextSize(13);
        modsStatus.setPadding(dp(8), dp(2), 0, dp(4));
        modsStatus.setText(modsLine != null ? modsLine : "");
        box.addView(modsStatus);
        return box;
    }

    private void modsStatus(String text) {
        modsLine = text;
        handler.post(() -> {
            if (modsStatus != null) modsStatus.setText(text);
        });
    }

    private void pickMod() {
        if (D2TouchOverlay.isGameRunning()) {
            modsStatus(getString(R.string.settings_mods_game_running));
            return;
        }
        if (modImportRunning) {
            modsStatus(getString(R.string.settings_mods_import_running));
            return;
        }
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("*/*");
        pick.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/x-zip-compressed",
            "application/x-7z-compressed", "application/octet-stream"});
        try {
            startActivityForResult(pick, MOD_REQUEST_CODE);
        }
        catch (RuntimeException e) {
            modsStatus(getString(R.string.saves_no_picker));
        }
    }


    private void importMod(Uri uri) {
        String name = displayName(uri);
        modImportRunning = true;
        modsStatus(getString(R.string.settings_mods_import_copying, name, 0));
        new Thread(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                if (in == null) throw new IOException("unreadable");
                long[] shown = {0};
                D2ModInfo.Mod mod = D2Mods.importArchive(in, name, (stage, done, total) -> {
                    long mb = done / (1024 * 1024);
                    if (mb == shown[0] && done != 0) return;
                    shown[0] = mb;
                    if (stage == 0) modsStatus(getString(R.string.settings_mods_import_copying, name, mb));
                    else if (stage == 1) modsStatus(getString(R.string.settings_mods_import_checking, name));
                    else modsStatus(getString(R.string.settings_mods_import_unpacking, name, mb, Math.max(1, total / (1024 * 1024))));
                });


                handler.post(() -> {
                    if (pageResumed && !D2TouchOverlay.isGameRunning() && D2Saves.beginImport()) {
                        try {
                            D2Mods.setActive(this, mod.id);
                        }
                        finally {
                            D2Saves.endImport();
                        }
                        D2Trace.log("mods: " + mod.id + " imported (" + mod.files + " files), chosen");
                        modsLine = getString(R.string.settings_mods_imported, mod.name, mod.files);
                    }
                    else {
                        D2Trace.log("mods: " + mod.id + " imported (" + mod.files + " files), not chosen (page left, game starting or running)");
                        modsLine = getString(R.string.settings_mods_imported_not_chosen, mod.name, mod.files);
                    }
                });
            }
            catch (D2ModArchive.Failure f) {
                modsLine = getString(R.string.settings_mods_import_failed, name, modFailure(f));
                D2Trace.log("mods: import refused: " + f.reason);
            }
            catch (IOException | RuntimeException e) {
                modsLine = getString(R.string.settings_mods_import_failed, name, String.valueOf(e.getMessage()));
                D2Trace.log("mods: import failed: " + e);
            }
            finally {
                modImportRunning = false;
            }
            handler.post(() -> {
                if (!isFinishing() && !isDestroyed()) buildPage(D2HelperEntries.SET_MODS);
            });
        }).start();
    }


    private String displayName(Uri uri) {
        try (android.database.Cursor c = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst() && !c.isNull(0)) return c.getString(0);
        }
        catch (RuntimeException e) {

        }
        String last = uri.getLastPathSegment();
        return last != null ? last : "Mod";
    }

    private String modFailure(D2ModArchive.Failure f) {
        String detail = f.detail != null ? f.detail : "";
        switch (f.reason) {
            case NOT_AN_ARCHIVE: return getString(R.string.settings_mods_fail_not_archive);
            case BROKEN: return getString(R.string.settings_mods_fail_broken);
            case UNSAFE_PATH: case LINK: case DUPLICATE: return getString(R.string.settings_mods_fail_unsafe, detail);
            case TOO_MANY: case TOO_LARGE: case BOMB: return getString(R.string.settings_mods_fail_too_large);
            case NO_GAME_FILES: return getString(R.string.settings_mods_fail_no_game_files);
            case AMBIGUOUS: return getString(R.string.settings_mods_fail_ambiguous);
            case CODE: return getString(R.string.settings_mods_fail_code, detail);
            default: return getString(R.string.settings_mods_fail_space, detail);
        }
    }




    private String[] spielweiseStages(D2Spielweise.Regulator r) {
        if (r == D2Spielweise.QUALITY) return new String[]{getString(R.string.settings_sw_stage_original),
            getString(R.string.settings_sw_quality_1), getString(R.string.settings_sw_quality_2)};
        if (r == D2Spielweise.NODROP) return new String[]{getString(R.string.settings_sw_nodrop_one),
            getString(R.string.settings_sw_nodrop_stage, 3), getString(R.string.settings_sw_nodrop_stage, 5),
            getString(R.string.settings_sw_nodrop_stage, 8)};
        if (r == D2Spielweise.RUNES) return new String[]{getString(R.string.settings_sw_stage_original),
            getString(R.string.settings_sw_runes_stage, 2), getString(R.string.settings_sw_runes_stage, 4),
            getString(R.string.settings_sw_runes_stage, 8)};
        if (r == D2Spielweise.BOSS) return new String[]{getString(R.string.settings_off),
            getString(R.string.settings_sw_boss_1), getString(R.string.settings_sw_boss_2)};
        if (r == D2Spielweise.DENSITY) return new String[]{getString(R.string.settings_sw_density_0), getString(R.string.settings_sw_density_1),
            getString(R.string.settings_sw_density_2), getString(R.string.settings_sw_density_3), getString(R.string.settings_sw_density_4)};
        if (r == D2Spielweise.GAMBLE) return new String[]{getString(R.string.settings_sw_stage_original),
            getString(R.string.settings_sw_gamble_1), getString(R.string.settings_sw_gamble_2)};
        if (r == D2Spielweise.EXPERIENCE) return new String[]{getString(R.string.settings_sw_exp_0),
            getString(R.string.settings_sw_exp_1), getString(R.string.settings_sw_exp_2)};
        return new String[]{getString(R.string.settings_off), getString(R.string.settings_on)};
    }


    private String spielweiseInfo(D2Spielweise.Regulator r) {
        if (r == D2Spielweise.QUALITY) return getString(R.string.settings_sw_quality_info);
        if (r == D2Spielweise.NODROP) return getString(R.string.settings_sw_nodrop_info);
        if (r == D2Spielweise.RUNES) return getString(R.string.settings_sw_runes_info);
        if (r == D2Spielweise.BOSS) return getString(R.string.settings_sw_boss_info);
        if (r == D2Spielweise.UNIQUES) return getString(R.string.settings_sw_uniques_info);
        if (r == D2Spielweise.GAMBLE) return getString(R.string.settings_sw_gamble_info);
        if (r == D2Spielweise.EXPERIENCE) return getString(R.string.settings_sw_exp_info);
        if (r == D2Spielweise.DENSITY) return getString(R.string.settings_sw_density_info);
        if (r == D2Spielweise.LADDER) return getString(R.string.settings_sw_ladder_info);
        if (r == D2Spielweise.COW) return getString(R.string.settings_sw_cow_info);
        if (r == D2Spielweise.UBER) return getString(R.string.settings_sw_uber_info);
        if (r == D2Spielweise.SHARED) return getString(R.string.settings_sw_shared_info);
        if (r == D2Spielweise.SORT) return getString(R.string.settings_sw_sort_info);
        if (r == D2Spielweise.RESPEC) return getString(R.string.settings_sw_respec_info);
        return getString(R.string.settings_sw_world_info);
    }

    private String presetName(String preset) {
        return getString(D2Spielweise.CLASSIC.equals(preset) ? R.string.settings_sw_classic : R.string.settings_sw_enhanced);
    }


    private final java.util.Map<View, Runnable> spielweiseViews = new java.util.LinkedHashMap<>();





    private void spielweiseKeep(View view, Runnable refill) {
        spielweiseViews.put(view, refill);
    }





    private void spielweiseChanged() {
        spielweiseViews.keySet().removeIf((v) -> !v.isAttachedToWindow());

        for (java.util.Map.Entry<View, Runnable> e : new java.util.ArrayList<>(spielweiseViews.entrySet()))
            D2PadFocus.keep(e.getKey(), e.getValue());
    }





    private View spielweisePreset(LinearLayout box) {
        box.removeAllViews();
        String preset = D2Settings.spielweisePreset(this);
        boolean own = D2Settings.spielweiseOwn(this);
        String state = own ? getString(R.string.settings_sw_own, presetName(preset)) : presetName(preset);
        String next = D2Spielweise.nextPreset(preset);
        D2SettingsLook look = D2SettingsLook.get();
        if (look != null) {
            D2GameText row = new D2GameText(this, look, look.font16, 0xD2CEC6, getString(R.string.settings_sw_preset));
            row.setPadding(dp(8), dp(8), dp(4), dp(4));
            row.setValue(state, 0xC8B074);
            row.setClickable(true);
            row.setOnClickListener((v) -> choosePreset(next));
            box.addView(row);
        }
        else {
            Button button = new Button(this);
            button.setText(getString(R.string.settings_sw_preset) + ": " + state);
            button.setOnClickListener((v) -> choosePreset(next));
            box.addView(button);
        }
        boolean classic = D2Spielweise.CLASSIC.equals(preset) && !own;
        box.addView(hint(getString(classic ? R.string.settings_sw_classic_hint : R.string.settings_sw_enhanced_hint)));
        if (!D2Settings.modEnabled(this)) box.addView(hint(getString(R.string.settings_sw_needs_mod)));
        D2ModInfo.Mod played = D2Mods.active(this);
        if (played != null) box.addView(hint(getString(R.string.settings_sw_mod_active, played.name)));

        box.addView(hint(getString(classic ? R.string.settings_sw_back_hint_classic : R.string.settings_sw_back_hint)));
        return box;
    }


    private void choosePreset(String preset) {
        if (bigStashBusy) return;
        if (!D2Settings.spielweiseOwn(this)) {
            applyPreset(preset);
            return;
        }
        TextView message = new TextView(this);
        message.setText(getString(R.string.settings_sw_discard, presetName(preset)));
        message.setTextSize(16);
        int pad = dp(16);
        message.setPadding(pad, pad, pad, 0);
        D2Style.text(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(message)
            .setPositiveButton(getString(R.string.settings_sw_discard_yes), (d, which) -> applyPreset(preset))
            .setNegativeButton(getString(R.string.cancel), null)
            .create();
        dialog.show();
        D2Sound.attach(dialog);
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
        D2ButtonTiers.dialog(dialog, dialog.getButton(AlertDialog.BUTTON_POSITIVE), dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
    }






    private void applyPreset(String preset) {
        if (bigStashBusy) return;
        boolean invOn = D2Settings.bigInventory(this);
        D2Settings.setSpielweisePreset(this, preset);
        if (invOn && !D2Settings.bigInventory(this)) {
            D2Settings.setBigInventory(this, true);
            guardedOff(GUARD_BIG_INV, this::spielweiseChanged);
        }
        spielweiseChanged();
    }






    private static boolean plugyOnly(D2Spielweise.Regulator r) {
        return r.plugyKey != null && !r.mod || r == D2Spielweise.SHARED || r == D2Spielweise.RESPEC;
    }

    private View spielweiseRow(D2Spielweise.Regulator r, String label) {
        boolean plugyOnly = plugyOnly(r);
        if (r.plugyKey != null && !D2PlugY.isBundled(this) && plugyOnly) return null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        spielweiseRow(r, label, box);
        spielweiseKeep(box, () -> spielweiseRow(r, label, box));
        return box;
    }

    private void spielweiseRow(D2Spielweise.Regulator r, String label, LinearLayout box) {
        box.removeAllViews();
        boolean plugyOnly = plugyOnly(r);
        String[] stages = spielweiseStages(r);
        int stage = D2Settings.spielweise(this, r);
        String preset = D2Settings.spielweisePreset(this);

        boolean usable = (!plugyOnly || D2Settings.plugy(this)) && D2Spielweise.usable(r, D2Settings.all(this));
        Runnable next = () -> {
            D2Settings.setSpielweise(this, r, r.next(D2Settings.spielweise(this, r)));
            spielweiseChanged();
        };
        D2SettingsLook look = D2SettingsLook.get();
        if (look != null) {
            D2GameText row = new D2GameText(this, look, look.font16, 0xD2CEC6, label);
            row.setPadding(dp(8), dp(8), dp(4), dp(4));
            row.setValue(stages[stage], stage == 0 ? 0x707070 : 0xC8B074);
            row.setClickable(usable);
            if (usable) row.setOnClickListener((v) -> next.run());
            row.step = (dir) -> {
                int to = D2Settings.spielweise(this, r) + dir;
                if (to < 0 || to >= r.stages) return false;
                D2Settings.setSpielweise(this, r, to);
                spielweiseChanged();
                return true;
            };
            box.addView(row);
        }
        else {
            Button button = new Button(this);
            button.setText(label + ": " + stages[stage]);
            button.setEnabled(usable);
            button.setOnClickListener((v) -> next.run());
            box.addView(button);
        }
        box.addView(hint(spielweiseInfo(r)));
        if (stage != r.preset(preset)) box.addView(hint(getString(R.string.settings_sw_differs, presetName(preset), stages[r.preset(preset)])));
        if (!usable) {
            if (plugyOnly && !D2Settings.plugy(this)) box.addView(hint(getString(R.string.settings_sw_needs_plugy)));
            box.setAlpha(0.45f);
        }
        else if (r == D2Spielweise.LADDER && !D2Settings.plugy(this)) box.addView(hint(getString(R.string.settings_sw_ladder_no_plugy)));
    }




    private boolean bigStashBusy;




    static final int GUARD_BIG_STASH = 0, GUARD_PLUGY = 1, GUARD_MOD = 2;

    static final int GUARD_BIG_INV = 3;

    private boolean guardOn(int kind) {
        if (kind == GUARD_PLUGY) return D2Settings.plugy(this);
        if (kind == GUARD_MOD) return D2Settings.modEnabled(this);
        if (kind == GUARD_BIG_INV) return D2Settings.bigInventory(this);
        return D2Settings.bigStash(this);
    }

    private void guardSet(int kind, boolean on) {
        if (kind == GUARD_PLUGY) D2Settings.putBoolean(this, D2Settings.KEY_PLUGY, on);
        else if (kind == GUARD_MOD) D2Settings.setModEnabled(this, on);
        else if (kind == GUARD_BIG_INV) D2Settings.setBigInventory(this, on);
        else D2Settings.setBigStash(this, on);
    }





    private String guardStays(int kind) {
        if (kind == GUARD_BIG_STASH) return getString(R.string.big_stash_on);
        if (kind == GUARD_BIG_INV) return getString(R.string.big_inv_on);
        return getString(kind == GUARD_PLUGY ? R.string.stash_guard_on_plugy : R.string.stash_guard_on_mod);
    }


    private String guardOff(int kind) {
        if (kind == GUARD_BIG_STASH) return getString(R.string.big_stash_off);
        if (kind == GUARD_BIG_INV) return getString(R.string.big_inv_off);
        return getString(kind == GUARD_PLUGY ? R.string.stash_guard_off_plugy : R.string.stash_guard_off_mod);
    }


    private View bigStashRow(String label) {
        return guardedRow(GUARD_BIG_STASH, label);
    }





    private View guardedRow(int kind, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        Runnable[] fill = new Runnable[1];
        fill[0] = () -> {
            box.removeAllViews();
            boolean on = guardOn(kind), usable = kind != GUARD_BIG_STASH || D2Settings.plugy(this);

            boolean invLanguage = D2BigInventory.languageOk(D2Settings.gameLanguage(this));
            if (kind == GUARD_BIG_INV) usable = on || D2Settings.modEnabled(this) && invLanguage;
            String value = bigStashBusy ? getString(R.string.big_stash_checking) : getString(on ? R.string.settings_on : R.string.settings_off);
            Runnable tap = () -> {
                if (bigStashBusy) return;
                if (!guardOn(kind)) guardSet(kind, true);
                else guardedOff(kind, () -> {
                    if (kind == GUARD_BIG_INV) spielweiseChanged();
                    else if (box.isAttachedToWindow()) D2PadFocus.keep(box, fill[0]);
                });
                D2PadFocus.keep(box, fill[0]);
                if (kind == GUARD_BIG_INV) spielweiseChanged();
            };
            D2SettingsLook look = D2SettingsLook.get();
            if (look != null) {
                D2GameText row = new D2GameText(this, look, look.font16, 0xD2CEC6, label);
                row.setPadding(dp(8), dp(8), dp(4), dp(4));
                row.setValue(value, on ? 0xC8B074 : 0x707070);
                row.setClickable(usable);
                if (usable) row.setOnClickListener((v) -> tap.run());
                row.step = (dir) -> {
                    if (guardOn(kind) == dir > 0 || bigStashBusy) return false;
                    tap.run();
                    return true;
                };
                box.addView(row);
            }
            else {
                Button button = new Button(this);
                button.setText(label + ": " + value);
                button.setEnabled(usable);
                button.setOnClickListener((v) -> tap.run());
                box.addView(button);
            }
            if (kind == GUARD_BIG_INV) {
                box.addView(hint(getString(R.string.settings_big_inventory_info)));

                String preset = D2Settings.spielweisePreset(this);
                int presetStage = D2Spielweise.INVENTORY.preset(preset);
                if ((on ? 1 : 0) != presetStage) box.addView(hint(getString(R.string.settings_sw_differs, presetName(preset),
                    getString(presetStage == 1 ? R.string.settings_on : R.string.settings_off))));
                box.setAlpha(usable ? 1f : 0.45f);
                if (!invLanguage) box.addView(hint(getString(R.string.settings_big_inventory_language)));
                else if (!D2Settings.modEnabled(this)) box.addView(hint(getString(R.string.settings_big_inventory_needs_mod)));
            }
            if (kind == GUARD_BIG_STASH) {
                box.addView(hint(getString(R.string.settings_big_stash_info)));
                box.setAlpha(usable ? 1f : 0.45f);
                if (!usable) box.addView(hint(getString(R.string.settings_sw_needs_plugy)));
            }
        };
        fill[0].run();
        if (kind == GUARD_BIG_INV) spielweiseKeep(box, fill[0]);
        return box;
    }










    private void guardedOff(int kind, Runnable done) {
        boolean big = kind == GUARD_BIG_STASH;

        boolean inv = kind == GUARD_BIG_INV || kind == GUARD_MOD && D2Settings.bigInventory(this);

        boolean plugyNow = D2PlugY.isBundled(this) && D2Settings.modEnabled(this) && D2Settings.plugy(this);
        boolean stash = kind != GUARD_BIG_INV && (big || plugyNow);
        if (!stash && !inv) {
            guardSet(kind, false);
            return;
        }
        if (D2TouchOverlay.isGameRunning()) {
            bigStashMessage(getString(stash ? R.string.stash_guard_game_running : R.string.big_inv_game_running, guardStays(kind)));
            return;
        }
        File gameDir = GameLocator.gameDir(this);
        if (gameDir == null) {
            guardSet(kind, false);
            return;
        }
        bigStashBusy = true;
        new Thread(() -> {
            String message;

            D2FolderLock.Decision lock = D2FolderLock.acquire(getPackageName(), appLabel(this), gameDir, "bigstash");
            if (lock.foreign || lock.unusable) {
                String text = folderLockText(this, lock, false);
                handler.post(() -> {
                    bigStashBusy = false;
                    done.run();
                    bigStashMessage(text);
                });
                return;
            }

            java.util.List<File> modLocks = new java.util.ArrayList<>();
            try {

                java.util.List<D2ModInfo.Mod> folders = new java.util.ArrayList<>();
                folders.add(null);
                for (D2ModInfo.Mod mod : D2Mods.list()) {
                    D2FolderLock.Decision modLock = D2FolderLock.acquire(getPackageName(), appLabel(this), mod.dir, "bigstash mod " + mod.id);
                    if (modLock.foreign) throw new IOException(folderLockText(this, modLock, false));
                    if (!modLock.unusable) modLocks.add(mod.dir);
                    folders.add(mod);
                }
                D2StashEdge.Sizes sizes = stash ? D2BigStash.sizes(gameDir) : null;
                StringBuilder stashWhere = new StringBuilder(), invWhere = new StringBuilder();
                for (D2ModInfo.Mod mod : folders) {
                    File saves = mod != null ? mod.saves() : D2Saves.saveDir(gameDir);
                    boolean judged = D2BigInventory.guarded(mod != null && mod.inventory);
                    if (stash && judged) {
                        java.util.List<D2StashEdge.Place> places = D2StashEdge.check(saves, mod != null ? D2BigStash.sizes(gameDir, mod) : sizes, big);
                        if (!places.isEmpty()) appendWhere(stashWhere, mod, folders.size() > 1, D2BigStash.where(this, places));
                    }
                    if (inv && judged) {
                        java.util.List<D2StashEdge.Place> places = D2BigInventoryGuard.check(gameDir, mod);
                        if (!places.isEmpty()) appendWhere(invWhere, mod, folders.size() > 1, D2BigInventoryGuard.where(this, places));
                    }
                }
                if (stashWhere.length() > 0 || invWhere.length() > 0) {

                    StringBuilder text = new StringBuilder(guardStays(kind));
                    if (stashWhere.length() > 0) text.append(' ').append(getString(big ? R.string.big_stash_reason
                        : R.string.stash_guard_reason)).append("\n\n").append(stashWhere);
                    if (invWhere.length() > 0) text.append(stashWhere.length() > 0 ? "\n\n" : " ")
                        .append(getString(R.string.big_inv_reason)).append("\n\n").append(invWhere);
                    message = text.toString();
                }
                else {

                    String reason = big ? "vor-kleiner-Truhe" : kind == GUARD_BIG_INV ? "vor-kleinem-Inventar"
                        : kind == GUARD_MOD ? "vor-ohne-Spielhilfen" : "vor-ohne-PlugY";
                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (D2ModInfo.Mod mod : folders) {
                        File backup = D2Saves.backup(mod != null ? mod.saves() : D2Saves.saveDir(gameDir),
                            mod != null ? reason + "-" + mod.id : reason);
                        if (backup != null) names.add(D2Saves.BACKUP_FOLDER + "/" + backup.getName());
                    }
                    guardSet(kind, false);
                    message = names.isEmpty() ? null : getString(R.string.stash_guard_off_backup, guardOff(kind), String.join(", ", names));
                }
            }
            catch (Exception e) {
                message = getString(stash ? R.string.stash_guard_failed : R.string.big_inv_failed, guardStays(kind), savesError(e));
            }
            finally {
                for (File dir : modLocks) D2FolderLock.release(dir);
                D2FolderLock.release(gameDir);
            }
            String text = message;
            handler.post(() -> {
                bigStashBusy = false;
                done.run();
                if (text != null) bigStashMessage(text);
            });
        }, "D2BigStash").start();
    }


    private void appendWhere(StringBuilder out, D2ModInfo.Mod mod, boolean named, String lines) {
        if (out.length() > 0) out.append('\n');
        if (named) out.append(mod != null ? mod.name : getString(R.string.settings_mods_original)).append(":\n");
        out.append(lines);
    }


    private void bigStashMessage(String text) {
        if (isFinishing()) return;
        TextView message = new TextView(this);
        message.setText(text);
        message.setTextSize(16);
        int pad = dp(16);
        message.setPadding(pad, pad, pad, 0);
        D2Style.text(message);

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(message);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton(getString(android.R.string.ok), null)
            .create();
        dialog.show();
        D2Sound.attach(dialog);
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
        Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (button != null) button.setTextColor(D2Style.GOLD);
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
        D2Sound.attach(dialog);
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
        D2ButtonTiers.dialog(dialog, dialog.getButton(AlertDialog.BUTTON_POSITIVE), dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
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

        D2ModInfo.Mod mod = D2Mods.active(this);
        File lockDir = mod != null ? mod.dir : gameDir;
        File saves = mod != null ? mod.saves() : D2Saves.saveDir(gameDir);
        new Thread(() -> {

            D2FolderLock.Decision lock = D2FolderLock.acquire(getPackageName(), appLabel(this), lockDir, "share");
            if (lock.foreign || lock.unusable) {
                savesStatus(folderLockText(this, lock, false));
                return;
            }
            try {
                File folder = new File(getFilesDir(), "share");
                File[] old = folder.listFiles();
                if (old != null) for (File file : old) file.delete();
                File zip = new File(folder, D2Saves.zipName("Diablo2-Spielstaende"));


                if (!D2TouchOverlay.isGameRunning() && D2Saves.recover(saves)) D2SaveRepair.run(saves);
                int count = D2Saves.zip(saves, zip);
                if (count == 0) {
                    savesStatus(getString(R.string.saves_none_found, saves));
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
                D2FolderLock.release(lockDir);
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
        if (requestCode == MOD_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) importMod(data.getData());
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
        D2Sound.attach(dialog);
        android.view.Window window = dialog.getWindow();
        if (window != null) window.setBackgroundDrawable(D2Style.windowBackground(getResources().getDisplayMetrics().density));
        for (int which : new int[]{AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE}) {
            Button button = dialog.getButton(which);
            if (button != null) button.setTextColor(D2Style.GOLD);
        }
        D2ButtonTiers.dialog(dialog, dialog.getButton(AlertDialog.BUTTON_POSITIVE), dialog.getButton(AlertDialog.BUTTON_NEGATIVE));
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


        D2ModInfo.Mod mod = D2Mods.active(this);
        File lockDir = mod != null ? mod.dir : gameDir;
        File saveDir = mod != null ? mod.saves() : D2Saves.saveDir(gameDir);
        new Thread(() -> {

            D2FolderLock.Decision lock = D2FolderLock.acquire(getPackageName(), appLabel(this), lockDir, "import");
            if (lock.foreign || lock.unusable) {
                savesStatus(folderLockText(this, lock, false));
                D2Saves.endImport();
                handler.post(() -> lockForImport(false));
                return;
            }
            try {
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
                D2FolderLock.release(lockDir);
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
