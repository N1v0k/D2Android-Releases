package com.winlator.xserver;

import android.os.Looper;

import androidx.collection.ArrayMap;

import com.winlator.core.Bitmask;
import com.winlator.core.Callback;
import com.winlator.d2.D2Trace;
import com.winlator.xconnector.ConnectedClient;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.events.ButtonRelease;
import com.winlator.xserver.events.Event;
import com.winlator.xserver.events.KeyRelease;
import com.winlator.xserver.events.PresentCompleteNotify;
import com.winlator.xserver.events.PresentConfigureNotify;
import com.winlator.xserver.events.PresentIdleNotify;

import java.io.IOException;
import java.util.ArrayList;

public class XClient extends ConnectedClient implements XResourceManager.OnResourceLifecycleListener {
    public final XServer xServer;
    private boolean authenticated = false;
    public final Integer resourceIDBase;
    private short sequenceNumber = 0;
    private int requestLength;
    private byte requestData;
    private int initialLength;
    private final ArrayMap<Window, EventListener> eventListeners = new ArrayMap<>();
    private final ArrayList<XResource> resources = new ArrayList<>();
    private final ArrayList<Callback<XClient>> onDestroyListeners = new ArrayList<>();

    public XClient(long nativePtr, int fd, XServer xServer) {
        super(nativePtr, fd);
        this.xServer = xServer;

        try (XLock lock = xServer.lockAll()) {
            resourceIDBase = xServer.resourceIDs.get();
            xServer.windowManager.addOnResourceLifecycleListener(this);
            xServer.pixmapManager.addOnResourceLifecycleListener(this);
            xServer.graphicsContextManager.addOnResourceLifecycleListener(this);
            xServer.cursorManager.addOnResourceLifecycleListener(this);
        }
    }

    public void registerAsOwnerOfResource(XResource resource) {
        resources.add(resource);
    }

    public void setEventListenerForWindow(Window window, Bitmask eventMask) {
        EventListener eventListener = eventListeners.get(window);
        if (eventListener != null) window.removeEventListener(eventListener);
        if (eventMask.isEmpty()) return;
        eventListener = new EventListener(this, eventMask);
        eventListeners.put(window, eventListener);
        window.addEventListener(eventListener);
    }

    public void sendEvent(Event event) {
        // D2Android: an event waits at most a while for a client that reads nothing (stopped in the background,
        // hung), then it is dropped: the UI thread must not hang (ANR), nor the epoll thread (the whole X server); the
        // client is not cut off, the game must not end.
        if (neverDropped(event)) {
            sendEventBlocking(event);
            return;
        }
        boolean ui = Looper.myLooper() == Looper.getMainLooper();
        int sendWaitMs = ui ? EVENT_WAIT_UI_MS : EVENT_WAIT_MS;
        if (eventsBlocked && !isRelease(event)) sendWaitMs = 0;
        int lockWaitMs = sendWaitMs > 0 ? sendWaitMs : EVENT_LOCK_WAIT_BLOCKED_MS;
        int result;
        XStreamLock lock = outputStream.lockForEvent(lockWaitMs, sendWaitMs);
        if (lock == null) result = -1;
        else {
            try {
                event.send(sequenceNumber, outputStream);
                result = outputStream.eventResult();
            }
            catch (IOException e) {
                e.printStackTrace();
                result = XOutputStream.EVENT_SENT;
            }
            finally {
                try {
                    lock.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (result == XOutputStream.EVENT_SENT) {
            eventsBlocked = false;
            return;
        }
        if (result == XOutputStream.EVENT_DROPPED) eventsBlocked = true;
        noteDroppedEvent(event, result == XOutputStream.EVENT_DROPPED
            ? (sendWaitMs > 0 ? "it read nothing for " + sendWaitMs + " ms" : "its socket is still full, no wait")
            : "the stream was busy for " + lockWaitMs + " ms");
    }

    /**
     * D2Android: Present events are never dropped: the guest's Vulkan WSI cannot make up a lost
     * IdleNotify/CompleteNotify (a swap chain image would stay taken, the picture would stand). They come from the
     * Present threads, never from the UI thread, only while the client presents itself (so it reads), and block as before.
     */
    private static boolean neverDropped(Event event) {
        return event instanceof PresentIdleNotify || event instanceof PresentCompleteNotify || event instanceof PresentConfigureNotify;
    }

    /** D2Android: a key or button let go: dropped, it would hang in the game */
    private static boolean isRelease(Event event) {
        return event instanceof KeyRelease || event instanceof ButtonRelease;
    }

    private void sendEventBlocking(Event event) {
        try {
            event.send(sequenceNumber, outputStream);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** D2Android: how long an event waits for room in the client's socket, on the UI thread and elsewhere */
    static final int EVENT_WAIT_UI_MS = 500, EVENT_WAIT_MS = 2000;
    /** D2Android: the wait for the stream lock while the client's socket is known full */
    static final int EVENT_LOCK_WAIT_BLOCKED_MS = 50;
    private final java.util.concurrent.atomic.AtomicInteger droppedTotal = new java.util.concurrent.atomic.AtomicInteger();
    /** D2Android: the last event to this client was dropped for a full socket: the next one does not wait */
    private volatile boolean eventsBlocked;

    /** D2Android: the trace line, for the first dropped event and then every 100th of this client */
    private void noteDroppedEvent(Event event, String why) {
        int total = droppedTotal.incrementAndGet();
        if (total == 1 || total % 100 == 0) {
            D2Trace.log("x: event " + event.getClass().getSimpleName() + " to client " + resourceIDBase + " dropped, " + why
                + " (" + total + " so far)");
        }
    }

    public boolean isInterestedIn(int eventId, Window window) {
        EventListener eventListener = eventListeners.get(window);
        return eventListener != null && eventListener.isInterestedIn(eventId);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public void freeResources() {
        try (XLock lock = xServer.lockAll()) {
            while (!resources.isEmpty()) {
                XResource resource = resources.remove(resources.size()-1);
                if (resource instanceof Window) {
                    xServer.windowManager.destroyWindow(resource.id);
                }
                else if (resource instanceof Pixmap) {
                    xServer.pixmapManager.freePixmap(resource.id);
                }
                else if (resource instanceof GraphicsContext) {
                    xServer.graphicsContextManager.freeGraphicsContext(resource.id);
                }
                else if (resource instanceof Cursor) {
                    xServer.cursorManager.freeCursor(resource.id);
                }
            }

            while (!eventListeners.isEmpty()) {
                int i = eventListeners.size()-1;
                eventListeners.keyAt(i).removeEventListener(eventListeners.removeAt(i));
            }

            xServer.windowManager.removeOnResourceLifecycleListener(this);
            xServer.pixmapManager.removeOnResourceLifecycleListener(this);
            xServer.graphicsContextManager.removeOnResourceLifecycleListener(this);
            xServer.cursorManager.removeOnResourceLifecycleListener(this);
            xServer.resourceIDs.free(resourceIDBase);
        }
    }

    public void generateSequenceNumber() {
        sequenceNumber++;
    }

    public short getSequenceNumber() {
        return sequenceNumber;
    }

    public int getRequestLength() {
        return requestLength;
    }

    public void setRequestLength(int requestLength) {
        this.requestLength = requestLength;
        initialLength = inputStream.available();
    }

    public byte getRequestData() {
        return requestData;
    }

    public void setRequestData(byte requestData) {
        this.requestData = requestData;
    }

    public int getRemainingRequestLength() {
        int actualLength = initialLength - inputStream.available();
        return requestLength - actualLength;
    }

    public void skipRequest() {
        inputStream.skip(getRemainingRequestLength());
    }

    public XInputStream getInputStream() {
        return inputStream;
    }

    public XOutputStream getOutputStream() {
        return outputStream;
    }

    public Bitmask getEventMaskForWindow(Window window) {
        EventListener eventListener = eventListeners.get(window);
        return eventListener != null ? eventListener.eventMask : new Bitmask();
    }

    @Override
    public void onFreeResource(XResource resource) {
        if (resource instanceof Window) eventListeners.remove(resource);
        resources.remove(resource);
    }

    public boolean isValidResourceId(int id) {
        return xServer.resourceIDs.isInInterval(id, resourceIDBase);
    }

    public void addOnDestroyListener(Callback<XClient> onDestroyListener) {
        if (!onDestroyListeners.contains(onDestroyListener)) onDestroyListeners.add(onDestroyListener);
    }

    public void removeOnWindowModificationListener(Callback<XClient> onDestroyListener) {
        onDestroyListeners.remove(onDestroyListener);
    }

    @Override
    public void destroy() {
        super.destroy();

        for (Callback<XClient> onDestroyListener : onDestroyListeners) onDestroyListener.call(this);
    }
}