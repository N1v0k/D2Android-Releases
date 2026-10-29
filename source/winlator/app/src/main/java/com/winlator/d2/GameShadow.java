package com.winlator.d2;

import android.content.Context;

import com.winlator.R;
import com.winlator.core.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;









public final class GameShadow {

    public static final String RENDERER_D2GL = "d2gl";
    public static final String RENDERER_D2DX = "d2dx";
    private static final String DIR_NAME = "D2Android-Game";
    private static final String D2DX_ASSET = "d2/d2dx/glide3x.dll";

    private static final String MOD_ASSET_DIR = "d2/mod/";
    public static final String LOADER_EXE = "D2Android.exe";
    private static final String[] MOD_FILES = {LOADER_EXE, "d2android-mod.dll"};

    private GameShadow() {}

    public static boolean isD2DXBundled(Context context) {
        try (java.io.InputStream ignored = context.getAssets().open(D2DX_ASSET)) {
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }

    public static boolean isModBundled(Context context) {
        try (java.io.InputStream ignored = context.getAssets().open(MOD_ASSET_DIR + LOADER_EXE)) {
            return true;
        }
        catch (IOException e) {
            return false;
        }
    }






    static void plugyStatsHint(Context context, File strings) {
        if (!strings.isFile()) return;
        try {
            byte[] file = java.nio.file.Files.readAllBytes(strings.toPath());
            byte[] out = D2PlugYTexts.statsHint(file, textIn(context, java.util.Locale.GERMAN, R.string.plugy_stats_plus_hint),
                textIn(context, java.util.Locale.ENGLISH, R.string.plugy_stats_plus_hint));
            if (out != null) java.nio.file.Files.write(strings.toPath(), out);
        }
        catch (IOException e) {
            android.util.Log.w("D2PlugY", "PlugY's strings not changed", e);
        }
    }


    private static String textIn(Context context, java.util.Locale locale, int id) {
        android.content.res.Configuration config = new android.content.res.Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config).getString(id);
    }





    public static File prepare(Context context, File driveC, File gameDir, String d2dxCfg) throws IOException {
        return prepare(context, driveC, gameDir, d2dxCfg, D2Mods.active(context));
    }


    public static File prepare(Context context, File driveC, File gameDir, String d2dxCfg, D2ModInfo.Mod mod) throws IOException {


        File saveDir = mod != null ? mod.saves() : D2Saves.saveDir(gameDir);
        if (!D2Saves.recover(saveDir)) {
            throw new IOException(D2Text.get(context, R.string.saves_error_import_unfinished, D2Saves.BACKUP_FOLDER));
        }


        D2SaveRepair.run(saveDir);
        File shadowDir = new File(driveC, DIR_NAME);
        if (!shadowDir.isDirectory() && !shadowDir.mkdirs()) throw new IOException("Cannot create " + shadowDir);


        File[] existing = shadowDir.listFiles();
        if (existing != null) {
            for (File file : existing) if (FileUtils.isSymlink(file)) file.delete();
        }

        D2ModOverlay.clear(shadowDir, (id) -> D2ModInfo.validId(id) && new File(D2Mods.root(), id).isDirectory()
            ? new File(new File(D2Mods.root(), id), D2ModInfo.CACHE) : null);

        File glide = new File(shadowDir, "glide3x.dll");
        FileUtils.copy(context, D2DX_ASSET, glide);
        if (!glide.isFile()) throw new IOException("D2DX fehlt in dieser App-Version");


        if (isModBundled(context)) {
            for (String name : MOD_FILES) FileUtils.copy(context, MOD_ASSET_DIR + name, new File(shadowDir, name));
        }

        boolean plugy = isModBundled(context) && D2Settings.modEnabled(context) && D2Settings.plugy(context);
        D2PlugY.install(context, shadowDir, plugy);
        if (plugy) plugyStatsHint(context, new File(new File(shadowDir, "PlugY"), D2PlugYTexts.STRINGS_FILE));


        FileUtils.writeString(new File(shadowDir, "d2dx.cfg"), d2dxCfg != null ? d2dxCfg : defaultD2DXConfig());


        java.util.List<String> laid = mod == null ? java.util.Collections.emptyList()
            : D2ModOverlay.lay(shadowDir, mod.files(), gameDir, mod.cache(), mod.id, GameShadow::isProvided);
        if (mod != null) D2Trace.log("mod " + mod.id + " laid over the game folder: " + laid);
        File[] entries = gameDir.listFiles();
        if (entries == null) throw new IOException("Spielordner nicht lesbar: " + gameDir);
        for (File entry : entries) {
            String name = entry.getName();
            if (isProvided(name)) continue;
            if (D2ModOverlay.nameIn(laid, name.toLowerCase(Locale.ROOT)) != null) continue;
            File link = new File(shadowDir, name);
            if (link.exists() || FileUtils.isSymlink(link)) continue;
            FileUtils.symlink(entry, link);
        }
        return shadowDir;
    }





    private static boolean isProvided(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.equals("glide3x.dll") || lower.equals("d2dx.cfg") || lower.equals("ddraw.dll")
            || lower.startsWith("d2dx_sgd2freeres") || lower.startsWith("_d2test") || lower.startsWith("d2android")
            || lower.equals("plugy.dll") || lower.equals("plugy.ini") || lower.equals("plugy");
    }






    public static String defaultD2DXConfig() {
        return buildD2DXConfig(null, 0);
    }


    public static String buildD2DXConfig(String gameSize, int filtering) {
        String size = "[-1,-1]";
        if (gameSize != null) {
            String[] parts = gameSize.split("x");
            size = "[" + Integer.parseInt(parts[0]) + "," + Integer.parseInt(parts[1]) + "]";
        }
        return "# Written by D2Android (D2LaunchActivity)\r\n"
            + "[window]\r\nscale=1\r\nposition=[-1,-1]\r\nframeless=true\r\n\r\n"
            + "[game]\r\nsize=" + size + "\r\nfiltering=" + filtering + "\r\n\r\n"
            + "[optouts]\r\nnologo=true\r\nnocompatmodefix=true\r\n";
    }
}
