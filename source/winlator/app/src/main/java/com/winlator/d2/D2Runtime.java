package com.winlator.d2;

import com.winlator.R;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.DisplayMetrics;

import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.StringUtils;
import com.winlator.core.TarCompressorUtils;
import com.winlator.core.WineRegistryEditor;
import com.winlator.xconnector.UnixSocketConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;












public final class D2Runtime {
    public static final String ASSET = "d2/runtime.txz";
    private static final String INFO_ASSET = "d2/runtime-info.txt";

    private static final String PREFIX_VERSION = "1";
    public static final String DRIVE_C_SHADOW = "D2Android-Game";

    public interface Progress {
        void update(String text);
    }

    private D2Runtime() {}



    public static File rootDir(Context context) {
        return new File(context.getFilesDir(), "rootfs");
    }

    public static File homeDir(Context context) {
        return new File(rootDir(context), "home/xuser");
    }

    public static File prefixDir(Context context) {
        return new File(homeDir(context), ".wine");
    }

    public static File driveC(Context context) {
        return new File(prefixDir(context), "drive_c");
    }


    public static File downloadsDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }


    static String gameDrivePath(Context context) {
        File game = GameLocator.gameDir(context);
        return game != null ? game.getAbsolutePath() : null;
    }


    public static String dosPath(Context context, File file) {
        String path = file.getAbsolutePath();
        String driveC = driveC(context).getAbsolutePath();
        String downloads = downloadsDir().getAbsolutePath();
        String game = gameDrivePath(context);
        String drive;
        String rest;
        if (path.equals(driveC) || path.startsWith(driveC + "/")) {
            drive = "C:";
            rest = path.substring(driveC.length());
        }
        else if (path.equals(downloads) || path.startsWith(downloads + "/")) {
            drive = "D:";
            rest = path.substring(downloads.length());
        }
        else if (game != null && (path.equals(game) || path.startsWith(game + "/"))) {

            drive = "E:";
            rest = path.substring(game.length());
        }
        else {
            drive = "Z:";
            rest = path;
        }
        rest = rest.replace('/', '\\');
        return drive + (rest.isEmpty() ? "\\" : rest);
    }




    public static String bundledVersion(Context context) {

        String pkg = info(context, "package");
        if (pkg != null && !pkg.equals(context.getPackageName())) {
            android.util.Log.e("D2Runtime", "runtime is for " + pkg + ", not " + context.getPackageName());
            return null;
        }
        return info(context, "version");
    }

    private static String info(Context context, String key) {
        try (InputStream in = context.getAssets().open(INFO_ASSET);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq > 0 && line.substring(0, eq).trim().equals(key)) return line.substring(eq + 1).trim();
            }
        }
        catch (IOException e) {
            return null;
        }
        return null;
    }

    private static File installedMarker(Context context) {
        return new File(rootDir(context), ".d2android/installed");
    }

    public static boolean isInstalled(Context context) {
        String bundled = bundledVersion(context);
        if (bundled == null) return false;
        String installed = readText(installedMarker(context));
        return installed != null && installed.trim().equals(bundled)
            && new File(rootDir(context), "usr/local/bin/box64").isFile()
            && new File(rootDir(context), "opt/wine/bin/wine").isFile();
    }





    public static void install(Context context, Progress progress) throws IOException {
        String version = bundledVersion(context);
        if (version == null) throw new IOException(D2Text.get(context, R.string.launch_no_runtime));
        File root = rootDir(context);
        if (root.isDirectory()) {
            File[] entries = root.listFiles();
            if (entries != null) for (File entry : entries) if (!entry.getName().equals("home")) FileUtils.delete(entry);
        }
        else if (!root.mkdirs()) throw new IOException(D2Text.get(context, R.string.runtime_cannot_create, root));

        long total = 1;
        try {
            total = Math.max(1, Long.parseLong(info(context, "bytes")));
        }
        catch (RuntimeException ignored) {}
        final long totalBytes = total;
        final long[] done = {0};
        final int[] lastPercent = {-1};
        boolean ok = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, ASSET, root, (file, size) -> {
            done[0] += size;
            int percent = (int) Math.min(100, done[0] * 100 / totalBytes);
            if (percent != lastPercent[0]) {
                lastPercent[0] = percent;
                progress.update(D2Text.get(context, R.string.runtime_unpacking, percent));
            }
            return file;
        });
        if (!ok || !new File(root, "usr/local/bin/box64").isFile()) throw new IOException(D2Text.get(context, R.string.runtime_unpack_failed));
        for (String dir : new String[]{"tmp", "home/xuser/.cache", "home/xuser/.config"}) new File(root, dir).mkdirs();
        if (!FileUtils.writeString(installedMarker(context), version + "\n")) throw new IOException(D2Text.get(context, R.string.runtime_finish_failed));
    }


    private static String readText(File file) {
        return file.isFile() ? FileUtils.readString(file) : null;
    }



    private static File prefixMarker(Context context) {
        return new File(prefixDir(context), ".d2android-prefix");
    }

    public static boolean isPrefixReady(Context context) {
        String marker = readText(prefixMarker(context));
        return marker != null && marker.trim().equals(PREFIX_VERSION + "/" + bundledVersion(context))
            && new File(prefixDir(context), "system.reg").isFile();
    }





    public static void setupPrefix(Context context, Progress progress) throws IOException {
        File prefix = prefixDir(context);
        File root = rootDir(context);
        if (!new File(prefix, "system.reg").isFile()) {
            FileUtils.delete(prefix);
            progress.update(D2Text.get(context, R.string.runtime_prefix_creating));
            EnvVars env = guestEnv(context);
            env.put("WINEDLLOVERRIDES", "mscoree,mshtml=d");
            env.remove("DISPLAY");
            new File(root, "tmp/shm").mkdirs();
            File log = logFile(context, "wineboot.log");
            int status = run(env, root, log, 600, root + "/usr/local/bin/box64", "wine", "wineboot", "-i");

            run(env, root, null, 60, root + "/usr/local/bin/box64", "wineserver", "-w");
            if (status != 0 || !new File(prefix, "system.reg").isFile()) {
                throw new IOException(D2Text.get(context, R.string.runtime_prefix_failed, status));
            }
        }
        progress.update(D2Text.get(context, R.string.runtime_prefix_configuring));
        configurePrefix(context);
        FileUtils.writeString(prefixMarker(context), PREFIX_VERSION + "/" + bundledVersion(context) + "\n");
    }

    private static int run(EnvVars env, File workDir, File log, int timeoutSeconds, String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command).directory(workDir).redirectErrorStream(true);
        if (log != null) builder.redirectOutput(log);
        else builder.redirectOutput(new File("/dev/null"));
        Map<String, String> environment = builder.environment();
        for (String name : env) environment.put(name, env.get(name));
        Process process = builder.start();
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return -1;
            }
            return process.exitValue();
        }
        catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return -1;
        }
    }


    static void configurePrefix(Context context) throws IOException {
        File root = rootDir(context);
        File prefix = prefixDir(context);
        File windows = new File(prefix, "drive_c/windows");

        refreshDrives(context);


        File[] dlls = new File(root, "opt/d2/syswow64").listFiles();
        if (dlls == null || dlls.length == 0) throw new IOException(D2Text.get(context, R.string.runtime_no_dxvk));
        for (File dll : dlls) copyFile(dll, new File(windows, "syswow64/" + dll.getName()));
        copyFile(new File(root, "opt/d2/winhandler.exe"), new File(windows, "winhandler.exe"));


        FileUtils.writeString(new File(prefix, ".update-timestamp"), "disable\n");


        File[] userFiles = new File(prefix, "drive_c/users/xuser").listFiles();
        if (userFiles != null) {
            for (File file : userFiles) {
                if (FileUtils.isSymlink(file)) {
                    file.delete();
                    file.mkdirs();
                }
            }
        }

        try (WineRegistryEditor user = new WineRegistryEditor(new File(prefix, "user.reg"))) {
            for (String dll : new String[]{"d3d11", "dxgi"}) {
                user.setStringValue("Software\\Wine\\DllOverrides", dll, "native,builtin");
            }
            user.setStringValue("Software\\Wine\\DllOverrides", "dsound", "builtin");
            user.setStringValue("Software\\Wine\\Drivers", "Audio", "alsa");
            user.setStringValue("Software\\Wine\\Drivers", "Graphics", "x11");

            user.setStringValue("Control Panel\\Mouse", "MouseSpeed", "0");
            user.setStringValue("Control Panel\\Mouse", "MouseThreshold1", "0");
            user.setStringValue("Control Panel\\Mouse", "MouseThreshold2", "0");
        }

        try (WineRegistryEditor system = new WineRegistryEditor(new File(prefix, "system.reg"))) {

            String controlSet = system.getSymlinkValue("System\\CurrentControlSet", "SymbolicLinkValue");
            if (controlSet == null) controlSet = "System\\CurrentControlSet";
            system.setCreateKeyIfNotExist(false);
            for (String service : new String[]{"BITS", "Eventlog", "HTTP", "LanmanServer", "NDIS", "PlugPlay", "RpcSs",
                    "scardsvr", "Schedule", "Spooler", "StiSvc", "TermService", "Winmgmt", "wuauserv", "winebth"}) {
                system.setDwordValue(controlSet + "\\Services\\" + service, "Start", 4);
            }
            system.setCreateKeyIfNotExist(true);

            final String filterData = "02000000000080000100000000000000307069330200000000000000010000000000000000000000307479330000000038000000480000006175647300001000800000aa00389b710100000000001000800000aa00389b71";
            for (String key : new String[]{
                    "Software\\Classes\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}",
                    "Software\\Classes\\Wow6432Node\\CLSID\\{083863F1-70DE-11D0-BD40-00A0C911CE86}\\Instance\\{E30629D1-27E5-11CE-875D-00608CB78066}"}) {
                system.setStringValue(key, "CLSID", "{E30629D1-27E5-11CE-875D-00608CB78066}");
                system.setHexValue(key, "FilterData", filterData);
                system.setStringValue(key, "FriendlyName", "Wave Audio Renderer");
            }
        }

        for (String dir : new String[]{"home/xuser/.cache", "home/xuser/.config"}) new File(root, dir).mkdirs();
    }


    public static void refreshDrives(Context context) {
        File dosdevices = new File(prefixDir(context), "dosdevices");
        dosdevices.mkdirs();
        File[] files = dosdevices.listFiles();
        if (files != null) for (File file : files) if (file.getName().matches("[a-z]:{1,2}")) file.delete();
        FileUtils.symlink("../drive_c", dosdevices.getPath() + "/c:");
        FileUtils.symlink("../../../../", dosdevices.getPath() + "/z:");
        FileUtils.symlink(downloadsDir().getAbsolutePath(), dosdevices.getPath() + "/d:");
        String game = gameDrivePath(context);
        if (game != null) FileUtils.symlink(game, dosdevices.getPath() + "/e:");
    }

    private static void copyFile(File src, File dst) throws IOException {
        dst.getParentFile().mkdirs();
        if (FileUtils.isSymlink(dst) || dst.isFile()) dst.delete();
        if (!FileUtils.copy(src, dst)) throw new IOException("Kann " + src.getName() + " nicht kopieren");
    }




    public static String screenSize(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        int width = Math.max(metrics.widthPixels, metrics.heightPixels);
        int height = Math.min(metrics.widthPixels, metrics.heightPixels);
        int targetHeight = Math.min(height, 1080);
        int targetWidth = Math.round(targetHeight * (float) width / height);
        targetWidth -= targetWidth % 2;
        return targetWidth + "x" + targetHeight;
    }





    public static String guestCommand(String screenSize, String dosDir, String program, String args) {
        return "wine explorer /desktop=nogui," + screenSize + " C:\\windows\\winhandler.exe /dir "
            + StringUtils.escapeDOSPath(dosDir) + " \"" + program + "\"" + (args == null || args.isEmpty() ? "" : " " + args);
    }


    public static EnvVars guestEnv(Context context) {
        String root = rootDir(context).getPath();
        EnvVars env = new EnvVars();

        env.put("HOME", root + "/home/xuser");
        env.put("USER", "xuser");
        env.put("TMPDIR", root + "/tmp");
        env.put("DISPLAY", ":0");
        env.put("LC_ALL", "en_US.UTF-8");
        env.put("PATH", root + "/opt/wine/bin:" + root + "/usr/local/bin:" + root + "/usr/bin");
        env.put("LD_LIBRARY_PATH", root + "/usr/lib");

        env.put("ANDROID_SYSVSHM_SERVER", root + UnixSocketConfig.SYSVSHM_SERVER_PATH);
        env.put("ANDROID_ALSA_SERVER", root + UnixSocketConfig.ALSA_SERVER_PATH);
        env.put("ANDROID_ASERVER_USE_SHM", "true");

        env.put("BOX64_LD_LIBRARY_PATH", root + "/lib/x86_64-linux-gnu");
        env.put("BOX64_NOBANNER", "1");
        env.put("BOX64_DYNAREC", "1");
        env.put("BOX64_UNITYPLAYER", "0");
        env.put("BOX64_DYNACACHE", "0");
        env.put("BOX64_DYNAREC_SAFEFLAGS", "2");
        env.put("BOX64_DYNAREC_FASTNAN", "0");
        env.put("BOX64_DYNAREC_FASTROUND", "0");
        env.put("BOX64_DYNAREC_X87DOUBLE", "1");
        env.put("BOX64_DYNAREC_BIGBLOCK", "0");
        env.put("BOX64_DYNAREC_STRONGMEM", "2");
        env.put("BOX64_DYNAREC_FORWARD", "128");
        env.put("BOX64_DYNAREC_CALLRET", "0");
        env.put("BOX64_DYNAREC_WAIT", "0");
        env.put("BOX64_DYNAREC_NATIVEFLAGS", "0");
        env.put("BOX64_DYNAREC_WEAKBARRIER", "0");

        env.put("WINEPREFIX", root + "/home/xuser/.wine");
        env.put("WINEDEBUG", "-all");
        env.put("WINEESYNC", "1");
        env.put("WINE_DO_NOT_CREATE_DXGI_DEVICE_MANAGER", "1");

        env.put("MESA_DEBUG", "silent");
        env.put("MESA_NO_ERROR", "1");
        env.put("MESA_SHADER_CACHE_DISABLE", "false");
        env.put("MESA_SHADER_CACHE_MAX_SIZE", "512MB");
        env.put("MESA_VK_WSI_PRESENT_MODE", "mailbox");
        env.put("MESA_VK_WSI_NATIVE_MEM_IMPORTED", "1");
        env.put("TU_DEBUG", "noconform");
        env.put("vblank_mode", "0");

        env.put("DXVK_LOG_LEVEL", "none");
        env.put("DXVK_STATE_CACHE_PATH", "Z:\\home\\xuser\\.cache");
        env.put("DXVK_CONFIG_FILE", "Z:\\home\\xuser\\.config\\dxvk.conf");
        return env;
    }


    public static void prepareSession(Context context) {
        File root = rootDir(context);
        File tmp = new File(root, "tmp");
        FileUtils.clear(tmp);
        tmp.mkdirs();
        new File(tmp, "shm").mkdirs();
        File config = new File(root, "home/xuser/.config");
        config.mkdirs();
        new File(root, "home/xuser/.cache").mkdirs();
        FileUtils.writeString(new File(config, "dxvk.conf"), "d3d11.constantBufferRangeCheck = \"True\"\n");
        refreshDrives(context);
    }





    public static void enableDxvkLog(Context context, EnvVars env, boolean warnings) {
        File dir = logFile(context, D2DxvkLog.DIR);
        D2DxvkLog.rotate(dir);
        FileUtils.symlink(dir.getAbsolutePath(), new File(rootDir(context), D2DxvkLog.LINK).getPath());
        env.put("DXVK_LOG_LEVEL", warnings ? "warn" : "error");
        env.put("DXVK_LOG_PATH", D2DxvkLog.WINE_PATH);
    }


    public static File logFile(Context context, String name) {
        File dir = new File(context.getFilesDir(), "logs");
        dir.mkdirs();
        return new File(dir, name);
    }
}
