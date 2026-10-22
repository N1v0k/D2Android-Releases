package com.winlator.d2;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;
import android.os.StatFs;
import android.system.Os;
import android.system.OsConstants;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class D2DeviceProbe {
    public static final long GPU_TIMEOUT_MS = 1000;

    public final boolean arm64;
    public final long pageSize;
    public final boolean kgsl;
    public String glVendor = "", glRenderer = "";

    private D2DeviceProbe(boolean arm64, long pageSize, boolean kgsl) {
        this.arm64 = arm64;
        this.pageSize = pageSize;
        this.kgsl = kgsl;
    }

    public static D2DeviceProbe basics() {
        boolean arm64 = Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).contains("arm64-v8a");
        long pageSize;
        try {
            pageSize = Os.sysconf(OsConstants._SC_PAGESIZE);
        }
        catch (RuntimeException e) {
            pageSize = 0;
        }
        return new D2DeviceProbe(arm64, pageSize, new File("/dev/kgsl-3d0").exists());
    }

    public boolean ruledOutEarly() {
        return !arm64 || (pageSize > 0 && pageSize != D2DeviceCheck.PAGE_SIZE);
    }

    public void readGpu() {
        String[] out = {"", ""};
        CountDownLatch done = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            EGLDisplay display = EGL14.EGL_NO_DISPLAY;
            EGLContext context = EGL14.EGL_NO_CONTEXT;
            EGLSurface surface = EGL14.EGL_NO_SURFACE;
            try {
                display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
                int[] version = new int[2];
                if (display == EGL14.EGL_NO_DISPLAY || !EGL14.eglInitialize(display, version, 0, version, 1)) return;
                int[] attribs = {EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT, EGL14.EGL_RENDERABLE_TYPE,
                    EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE};
                EGLConfig[] configs = new EGLConfig[1];
                int[] count = new int[1];
                if (!EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, count, 0) || count[0] < 1) return;
                context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                    new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
                surface = EGL14.eglCreatePbufferSurface(display, configs[0], new int[]{EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE}, 0);
                if (context == EGL14.EGL_NO_CONTEXT || !EGL14.eglMakeCurrent(display, surface, surface, context)) return;
                String vendor = GLES20.glGetString(GLES20.GL_VENDOR), renderer = GLES20.glGetString(GLES20.GL_RENDERER);
                out[0] = vendor != null ? vendor : "";
                out[1] = renderer != null ? renderer : "";
            }
            catch (Throwable ignored) {
            }
            finally {
                try {
                    if (display != EGL14.EGL_NO_DISPLAY) {
                        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                        if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface);
                        if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context);
                        EGL14.eglTerminate(display);
                    }
                }
                catch (Throwable ignored) {}
                done.countDown();
            }
        }, "DeviceProbe");
        t.setDaemon(true);
        t.start();
        try {
            if (done.await(GPU_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                glVendor = out[0];
                glRenderer = out[1];
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static final String EXTRA_FAKE = "d2_devicecheck_fake";
    public static final String ADB_TOKEN = "/data/local/tmp/d2android-devicecheck.ok";
    public boolean fakeLowSpace;

    public static String fakeFromIntent(android.content.Context context, android.content.Intent intent) {
        if (!com.winlator.BuildConfig.D2_INTERNAL) return null;
        String fake = intent != null ? intent.getStringExtra(EXTRA_FAKE) : null;
        if (fake == null) return null;
        if ((context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) == 0) return null;
        return D2SpieltestPort.tokenFresh("device check", ADB_TOKEN) ? fake : null;
    }

    public static D2DeviceProbe fake(String which) {
        boolean arm64 = !which.equals("abi32"), kgsl = which.startsWith("adreno") || which.equals("unknown") || which.equals("space");
        D2DeviceProbe p = new D2DeviceProbe(arm64, which.equals("pages16k") ? 16384 : D2DeviceCheck.PAGE_SIZE, kgsl);
        if (which.equals("mali")) { p.glVendor = "ARM"; p.glRenderer = "Mali-G715"; }
        else if (which.startsWith("adreno")) { p.glVendor = "Qualcomm"; p.glRenderer = "Adreno (TM) " + which.substring(6); }
        else if (which.equals("space") || which.equals("pages16k") || which.equals("abi32")) { p.glVendor = "Qualcomm"; p.glRenderer = "Adreno (TM) 740"; }
        p.fakeLowSpace = which.equals("space");
        return p;
    }


    public String gpuName() {
        return glRenderer.isEmpty() ? null : glRenderer;
    }

    public D2DeviceCheck.Result classify() {
        return D2DeviceCheck.classify(arm64, pageSize, glVendor, glRenderer, kgsl);
    }

    public static long freeBytes(File dir) {
        try {
            return new StatFs(dir.getPath()).getAvailableBytes();
        }
        catch (RuntimeException e) {
            return -1;
        }
    }

    public String describe() {
        return "device " + Build.MANUFACTURER + " " + Build.MODEL + ", arm64 " + arm64 + ", page " + pageSize + ", kgsl " + kgsl
            + ", gl '" + glVendor + "' '" + glRenderer + "' -> " + classify();
    }
}
