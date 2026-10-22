package com.winlator.d2;

import com.winlator.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.content.FileProvider;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class D2Diagnose {
    private static final String[] GAME_LOGS = {"d2android-mod.log", "d2android-loader.log", "PlugY.log",
        "d2android-mod.prev.log", "d2android-loader.prev.log"};
    private static final String PREV_TRACE = "app-trace.prev.txt";

    private D2Diagnose() {}

    public static void share(final Activity activity) {
        new Thread(() -> {
            try {
                File folder = new File(activity.getFilesDir(), "share");
                File[] old = folder.listFiles();
                if (old != null) for (File file : old) file.delete();
                if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException(D2Text.get(activity, R.string.runtime_cannot_create, folder));
                File zip = new File(folder, "D2Android-Diagnose-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".zip");
                File trace = new File(folder, "app-trace.txt");
                D2Trace.writeTo(trace);
                try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                    add(out, trace, "app-trace.txt");
                    File shadow = shadowDir(activity);
                    if (shadow != null) for (String name : GAME_LOGS) add(out, new File(shadow, name), name);
                    add(out, new File(activity.getFilesDir(), PREV_TRACE), PREV_TRACE);
                    for (String name : new String[] {D2GuestOutput.FILE, D2GuestOutput.PREV}) {
                        add(out, D2Runtime.logFile(activity, name), name, D2GuestOutput.ZIP_BYTES);
                    }
                    File dxvk = D2Runtime.logFile(activity, D2DxvkLog.DIR);
                    for (String sub : new String[] {"", D2DxvkLog.PREVIOUS}) {
                        File[] files = new File(dxvk, sub).listFiles();
                        if (files == null) continue;
                        for (File file : files) {
                            if (file.isFile()) addText(out, file, "dxvk/" + (sub.isEmpty() ? "" : sub + "/") + file.getName(), D2DxvkLog.ZIP_BYTES);
                        }
                    }
                    if (shadow != null) for (String name : D2GameLogs.newest(shadow.list())) addGameLog(out, new File(shadow, name), name);
                    add(out, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "D2Android/Protokoll.txt"), "Protokoll.txt");
                    out.putNextEntry(new ZipEntry("info.txt"));
                    out.write(info(activity).getBytes("UTF-8"));
                    out.closeEntry();
                }
                trace.delete();
                Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".FileProvider", zip);
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("application/zip");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.putExtra(Intent.EXTRA_SUBJECT, D2Text.get(activity, R.string.diagnose_subject));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                new Handler(Looper.getMainLooper()).post(() -> activity.startActivity(Intent.createChooser(send, D2Text.get(activity, R.string.diagnose_share))));
            }
            catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(activity, D2Text.get(activity, R.string.diagnose_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        }, "D2Diagnose").start();
    }

    public static void reportBug(Activity activity) {
        String version = null;
        try {
            version = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
        }
        catch (Exception ignored) {
        }
        String url = D2BugReport.url(version, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT);
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
        catch (Exception e) {
            Toast.makeText(activity, D2Text.get(activity, R.string.report_no_browser, D2BugReport.NEW_ISSUE), Toast.LENGTH_LONG).show();
        }
    }

    public static synchronized void keepTraceOfEndedRun(Context context) {
        File file = new File(context.getFilesDir(), PREV_TRACE);
        File part = new File(context.getFilesDir(), PREV_TRACE + ".part");
        try {
            D2Trace.writeTo(part);
            if (!part.renameTo(file)) part.delete();
        }
        catch (IOException e) {
            part.delete();
        }
    }

    private static void addGameLog(ZipOutputStream out, File file, String name) throws IOException {
        if (!file.isFile()) return;
        long length = file.length();
        int read = (int) Math.min(length, D2GameLogs.READ_BYTES);
        byte[] tail = new byte[read];
        try (java.io.RandomAccessFile in = new java.io.RandomAccessFile(file, "r")) {
            in.seek(length - read);
            in.readFully(tail);
        }
        out.putNextEntry(new ZipEntry(name));
        out.write(D2GameLogs.compact(tail, read < length));
        out.closeEntry();
    }

    private static File shadowDir(Context context) {
        File dir = new File(D2Runtime.driveC(context), D2Runtime.DRIVE_C_SHADOW);
        return dir.isDirectory() ? dir : null;
    }

    private static String info(Context context) {
        StringBuilder b = new StringBuilder();
        try {
            b.append("App ").append(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName).append('\n');
        }
        catch (Exception ignored) {
            b.append("App ?\n");
        }
        b.append(D2DeviceInfo.format(deviceFacts(context)));
        b.append("\nSpielhilfen und Einstellungen:\n");
        for (Map.Entry<String, ?> entry : context.getSharedPreferences(D2Settings.PREFS, Context.MODE_PRIVATE).getAll().entrySet()) {
            b.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
        }
        return b.toString();
    }

    private static D2DeviceInfo.Facts deviceFacts(Context context) {
        D2DeviceInfo.Facts f = new D2DeviceInfo.Facts();
        f.manufacturer = Build.MANUFACTURER;
        f.model = Build.MODEL;
        f.androidRelease = Build.VERSION.RELEASE;
        f.sdk = Build.VERSION.SDK_INT;
        if (Build.VERSION.SDK_INT >= 31) {
            f.socManufacturer = Build.SOC_MANUFACTURER;
            f.socModel = Build.SOC_MODEL;
        }
        f.hardware = Build.HARDWARE;
        try {
            D2DeviceProbe probe = D2DeviceProbe.basics();
            probe.readGpu();
            f.gpu = probe.gpuName();
        }
        catch (RuntimeException ignored) {
        }
        f.vulkanVersion = vulkanVersion(context);
        android.app.ActivityManager activities = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activities != null) {
            android.app.ActivityManager.MemoryInfo memory = new android.app.ActivityManager.MemoryInfo();
            activities.getMemoryInfo(memory);
            f.ramTotal = memory.totalMem;
            f.ramFree = memory.availMem;
            f.ramLow = memory.lowMemory;
        }
        if (D2TouchOverlay.isGameRunning()) {
            long guestPss = 0, guestRss = 0;
            boolean pss = true;
            int count = 0;
            for (com.winlator.core.ProcessHelper.PStat process : D2GuestList.find(new File("/proc"), D2Runtime.rootDir(context),
                    android.os.Process.myUid(), android.os.Process.myPid()).processes) {
                if (!process.guestProcess) continue;
                long p = pss(process.pid);
                if (p < 0) pss = false;
                guestPss += Math.max(0, p);
                guestRss += com.winlator.core.ProcessHelper.getMemoryUsage(process.pid);
                count++;
            }
            int self = android.os.Process.myPid();
            long appPss = pss(self);
            f.pss = pss && appPss >= 0;
            f.guestBytes = f.pss ? guestPss : guestRss;
            f.guestProcesses = count;
            f.appBytes = f.pss ? appPss : com.winlator.core.ProcessHelper.getMemoryUsage(self);
        }
        return f;
    }

    private static long pss(int pid) {
        try {
            return D2DeviceInfo.pssBytes(new String(java.nio.file.Files.readAllBytes(new File("/proc/" + pid + "/smaps_rollup").toPath()), "UTF-8"));
        }
        catch (IOException | RuntimeException e) {
            return -1;
        }
    }

    private static int vulkanVersion(Context context) {
        try {
            for (android.content.pm.FeatureInfo feature : context.getPackageManager().getSystemAvailableFeatures()) {
                if (android.content.pm.PackageManager.FEATURE_VULKAN_HARDWARE_VERSION.equals(feature.name)) return feature.version;
            }
            return 0;
        }
        catch (RuntimeException e) {
            return -1;
        }
    }

    private static void add(ZipOutputStream out, File file, String name) throws IOException {
        add(out, file, name, 8L * 1024 * 1024);
    }

    private static void addText(ZipOutputStream out, File file, String name, long max) throws IOException {
        if (!file.isFile()) return;
        out.putNextEntry(new ZipEntry(name));
        try (java.io.RandomAccessFile in = new java.io.RandomAccessFile(file, "r")) {
            long length = in.length();
            in.seek(Math.max(0, length - max));
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) > 0) {
                int kept = 0;
                for (int i = 0; i < n; i++) if (buffer[i] != 0) buffer[kept++] = buffer[i];
                if (kept > 0) out.write(buffer, 0, kept);
            }
        }
        out.closeEntry();
    }

    private static void add(ZipOutputStream out, File file, String name, long max) throws IOException {
        if (!file.isFile()) return;
        out.putNextEntry(new ZipEntry(name));
        long skip = Math.max(0, file.length() - max);
        try (InputStream in = new FileInputStream(file)) {
            long skipped = 0;
            while (skipped < skip) {
                long n = in.skip(skip - skipped);
                if (n <= 0) break;
                skipped += n;
            }
            byte[] buffer = new byte[16384];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        }
        out.closeEntry();
    }
}
