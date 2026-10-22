package com.winlator.xserver.extensions;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import android.util.SparseArray;

import com.winlator.core.GPUHelper;
import com.winlator.renderer.GLRenderer;
import com.winlator.renderer.GPUImage;
import com.winlator.renderer.Texture;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.core.Bitmask;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.Pixmap;
import com.winlator.xserver.Window;
import com.winlator.xserver.WindowManager;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XLock;
import com.winlator.xserver.XResource;
import com.winlator.xserver.XResourceManager;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.BadMatch;
import com.winlator.xserver.errors.BadWindow;
import com.winlator.xserver.errors.XRequestError;
import com.winlator.xserver.events.PresentCompleteNotify;
import com.winlator.xserver.events.PresentConfigureNotify;
import com.winlator.xserver.events.PresentIdleNotify;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PresentExtension extends Extension implements WindowManager.OnWindowModificationListener, XResourceManager.OnResourceLifecycleListener {
    public static final byte MAJOR_VERSION = 1;
    public static final byte MINOR_VERSION = 0;
    public enum CompleteKind {PIXMAP, MSC_NOTIFY}
    public enum CompleteMode {COPY, FLIP, SKIP}
    private static final byte FLAG_WINDOW_DESTROYED = (1<<0);
    private final SparseArray<PresentEvent> events = new SparseArray<>();
    private SyncExtension syncExtension;
    private long eglContextPtr;
    /** D2Android: the renderer context the copy context shares textures with; presents so far (the trace) */
    private int copyGeneration;
    private int copyLoggedGeneration;
    private boolean copyWaitLogged;
    public static volatile long presents;
    /**
     * D2Android: for the trace of a stalled picture (D2StallSnapshot): when the last PresentPixmap came
     * (uptime ms) and its pixmap's size, how many Idle-, Complete- and ConfigureNotify went out, and the last
     * ConfigureNotify (uptime ms, window size)
     */
    public static volatile long lastPresentAt, idleNotifies, completeNotifies, configureNotifies, lastConfigureAt;
    public static volatile int lastPresentWidth, lastPresentHeight, lastConfigureWidth, lastConfigureHeight;
    private ScheduledExecutorService idleNotifyScheduler;

    private static abstract class ClientOpcodes {
        private static final byte QUERY_VERSION = 0;
        private static final byte PRESENT_PIXMAP = 1;
        private static final byte SELECT_INPUT = 3;
    }

    private static class PresentEvent {
        private Window window;
        private XClient client;
        private int id;
        private Bitmask mask;
    }

    public PresentExtension(XServer xServer, byte majorOpcode) {
        super(xServer, majorOpcode);
        xServer.windowManager.addOnWindowModificationListener(this);
        xServer.windowManager.addOnResourceLifecycleListener(this);
    }

    @Override
    public String getName() {
        return "Present";
    }

    @Override
    public byte getEventCount() {
        return 3;
    }

    private void sendConfigureNotify(Window window, int pixmapFlags) {
        if (events.size() == 0) return;
        int mask = (1<<PresentConfigureNotify.PRESENT_CONFIGURE);

        synchronized (events) {
            for (int i = 0; i < events.size(); i++) {
                PresentEvent event = events.valueAt(i);
                if (event.window == window && event.mask.isSet(mask)) {
                    configureNotifies++;
                    lastConfigureAt = android.os.SystemClock.uptimeMillis();
                    lastConfigureWidth = window.getWidth();
                    lastConfigureHeight = window.getHeight();
                    event.client.sendEvent(new PresentConfigureNotify(this, event.id, window, pixmapFlags));
                }
            }
        }
    }

    private void sendIdleNotify(Window window, Pixmap pixmap, int serial, int idleFence) {
        if (idleFence != 0) syncExtension.setTriggered(idleFence);
        if (events.size() == 0) return;
        int mask = (1<<PresentIdleNotify.PRESENT_IDLE);

        synchronized (events) {
            for (int i = 0; i < events.size(); i++) {
                PresentEvent event = events.valueAt(i);
                if (event.window == window && event.mask.isSet(mask)) {
                    idleNotifies++;
                    event.client.sendEvent(new PresentIdleNotify(this, event.id, window, pixmap, serial, idleFence));
                }
            }
        }
    }

    private void sendIdleNotifyScheduled(final Window window, final Pixmap pixmap, final int serial, final int idleFence) {
        final long frameTime = 1000000000L / 60;
        long now = System.nanoTime();

        long lastIdleTime = (long)window.getTag("lastIdleTime", now);
        lastIdleTime = lastIdleTime <= (now - frameTime) ? now + frameTime : lastIdleTime + frameTime;
        long delayNanos = lastIdleTime - now;
        window.setTag("lastIdleTime", lastIdleTime);

        if (idleNotifyScheduler == null) idleNotifyScheduler = Executors.newSingleThreadScheduledExecutor();
        idleNotifyScheduler.schedule(() -> sendIdleNotify(window, pixmap, serial, idleFence), delayNanos, TimeUnit.NANOSECONDS);
    }

    private void sendCompleteNotify(Window window, int serial, CompleteKind kind, CompleteMode mode, long ust, long msc) {
        if (events.size() == 0) return;
        int mask = (1<<PresentCompleteNotify.PRESENT_COMPLETE);

        synchronized (events) {
            for (int i = 0; i < events.size(); i++) {
                PresentEvent event = events.valueAt(i);
                if (event.window == window && event.mask.isSet(mask)) {
                    completeNotifies++;
                    event.client.sendEvent(new PresentCompleteNotify(this, event.id, window, serial, kind, mode, ust, msc));
                }
            }
        }
    }

    private void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(8);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeInt(MAJOR_VERSION);
            outputStream.writeInt(MINOR_VERSION);
            outputStream.writePad(16);
        }
    }

    private void createCopyEGLContext(XClient client) {
        eglContextPtr = GPUHelper.createOffscreenEGLContext(true);
        client.addOnDestroyListener((unused) -> {
            GPUHelper.destroyOffscreenEGLContext(eglContextPtr);
            eglContextPtr = 0;
        });
    }

    private void presentPixmap(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        int pixmapId = inputStream.readInt();
        int serial = inputStream.readInt();
        inputStream.skip(8);
        short xOff = inputStream.readShort();
        short yOff = inputStream.readShort();
        inputStream.skip(8);
        int idleFence = inputStream.readInt();
        inputStream.skip(8);
        long targetMSC = inputStream.readLong();
        inputStream.skip(client.getRemainingRequestLength());

        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        Pixmap pixmap = xServer.pixmapManager.getPixmap(pixmapId);

        Drawable content = window.getContent();
        if (pixmap != null && content.visual.depth != pixmap.drawable.visual.depth) throw new BadMatch();

        final int frameTime = 1000000 / 60;
        long ust = System.nanoTime() / 1000L;
        long msc = ust / frameTime;

        synchronized (content.renderLock) {
            sendCompleteNotify(window, serial, CompleteKind.PIXMAP, CompleteMode.COPY, ust, msc);

            presents++;
            lastPresentAt = android.os.SystemClock.uptimeMillis();
            if (pixmap != null) {
                lastPresentWidth = pixmap.drawable.width;
                lastPresentHeight = pixmap.drawable.height;
            }
            if (pixmap != null) {
                Texture srcTexture = pixmap.drawable.getTexture();
                if (srcTexture instanceof GPUImage) {
                    // D2Android: the copy context must share the renderer's textures. Before the renderer has a
                    // context nothing is copied (the next frame comes soon); for a new renderer context it is made anew.
                    int generation = GLRenderer.contextGeneration;
                    if (generation == 0) {
                        if (!copyWaitLogged) com.winlator.d2.D2Trace.log("present: no renderer context yet, frame not copied");
                        copyWaitLogged = true;
                    }
                    else {
                        if (eglContextPtr != 0 && copyGeneration != generation) {
                            com.winlator.d2.D2Trace.log("present: renderer context " + generation + ", copy context made anew");
                            GPUHelper.destroyOffscreenEGLContext(eglContextPtr);
                            eglContextPtr = 0;
                        }
                        if (eglContextPtr == 0) {
                            createCopyEGLContext(client);
                            copyGeneration = generation;
                            if (copyLoggedGeneration != generation) com.winlator.d2.D2Trace.log("present: copy context "
                                + (eglContextPtr != 0 ? "made" : "FAILED") + " for renderer context " + generation);
                            copyLoggedGeneration = generation;
                        }
                        Texture.setThreadGeneration(copyGeneration);
                        content.setData(null);
                        content.getTexture().copyFromSource(srcTexture);
                        content.forceUpdate();
                    }
                }
                else content.copyArea((short)0, (short)0, xOff, yOff, pixmap.drawable.width, pixmap.drawable.height, pixmap.drawable);

                if (targetMSC > 0) {
                    sendIdleNotifyScheduled(window, pixmap, serial, idleFence);
                }
                else sendIdleNotify(window, pixmap, serial, idleFence);
            }
            else content.forceUpdate();
        }
    }

    private void selectInput(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int eventId = inputStream.readInt();
        int windowId = inputStream.readInt();
        Bitmask mask = new Bitmask(inputStream.readInt());

        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        window.removeTag("lastIdleTime");
        GPUImage.createOrObtain(xServer, window.getContent(), true, true);

        if (eventId > 0) {
            synchronized (events) {
                PresentEvent event = events.get(eventId);
                if (event != null) {
                    if (event.window != window || event.client != client) throw new BadMatch();

                    if (!mask.isEmpty()) {
                        event.mask = mask;
                    }
                    else events.remove(eventId);
                }
                else {
                    event = new PresentEvent();
                    event.id = eventId;
                    event.window = window;
                    event.client = client;
                    event.mask = mask;
                    events.put(eventId, event);
                }
            }
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = client.getRequestData();
        if (syncExtension == null) syncExtension = (SyncExtension)xServer.getExtensionByName("SYNC");

        switch (opcode) {
            case ClientOpcodes.QUERY_VERSION :
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.PRESENT_PIXMAP:
                try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.PIXMAP_MANAGER)) {
                    presentPixmap(client, inputStream, outputStream);
                }
                break;
            case ClientOpcodes.SELECT_INPUT:
                try (XLock lock = xServer.lock(XServer.Lockable.WINDOW_MANAGER)) {
                    selectInput(client, inputStream, outputStream);
                }
                break;
            default:
                throw new BadImplementation();
        }
    }

    @Override
    public void onFreeResource(XResource resource) {
        if (resource instanceof Window) {
            sendConfigureNotify((Window)resource, FLAG_WINDOW_DESTROYED);
            synchronized (events) {
                for (int i = events.size()-1; i >= 0; i--) {
                    PresentEvent event = events.valueAt(i);
                    if (event.window == resource) events.removeAt(i);
                }
            }
        }
    }

    @Override
    public void onUpdateWindowGeometry(Window window, boolean resized) {
        window.removeTag("lastIdleTime");
        sendConfigureNotify(window, 0);
    }
}
