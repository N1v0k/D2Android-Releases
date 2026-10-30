package com.winlator.xenvironment.components;  // Modified by D2Android (changes 2026-10-09 to 2026-10-30).

import androidx.annotation.Keep;

import com.winlator.core.GPUHelper;
import com.winlator.renderer.GPUImage;
import com.winlator.xconnector.ConnectedClient;
import com.winlator.xconnector.ConnectionHandler;
import com.winlator.xconnector.RequestHandler;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xconnector.XConnectorEpoll;
import com.winlator.xconnector.XInputStream;
import com.winlator.xenvironment.EnvironmentComponent;
import com.winlator.xserver.Drawable;
import com.winlator.xserver.Window;
import com.winlator.xserver.XServer;

import java.io.IOException;







public class VortekRendererComponent extends EnvironmentComponent implements ConnectionHandler, RequestHandler {
    private static final byte REQUEST_CODE_CREATE_CONTEXT = 1;
    private static final byte REQUEST_CODE_SEND_EXTRA_DATA = 2;
    public static final short IMAGE_CACHE_SIZE = 256;
    public static final int VK_MAX_VERSION = GPUHelper.vkMakeVersion(1, 3, 128);
    private final XServer xServer;
    private final UnixSocketConfig socketConfig;
    private XConnectorEpoll connector;
    private final Options options;

    static {
        System.loadLibrary("vortekrenderer");
    }


    @Keep
    public static class Options {
        public int vkMaxVersion = VK_MAX_VERSION;

        public short maxDeviceMemory = 0;
        public short imageCacheSize = IMAGE_CACHE_SIZE;
        public byte resourceMemoryType = 0;
        public String[] exposedDeviceExtensions = null;
    }


    public VortekRendererComponent(XServer xServer, UnixSocketConfig socketConfig, Options options, String cacheDir) {
        this.xServer = xServer;
        this.socketConfig = socketConfig;
        this.options = options;
        initVulkanWrapper(cacheDir);
    }

    @Override
    public void start() {
        if (connector != null) return;
        connector = new XConnectorEpoll(socketConfig, this, this);
        connector.setInitialInputBufferCapacity(8);
        connector.setInitialOutputBufferCapacity(0);
        connector.start();
    }

    @Override
    public void stop() {
        if (connector != null) {
            connector.destroy();
            connector = null;
        }
    }

    @Keep
    private int getWindowWidth(int windowId) {
        Window window = xServer.windowManager.getWindow(windowId);
        return window != null ? window.getWidth() : 0;
    }

    @Keep
    private int getWindowHeight(int windowId) {
        Window window = xServer.windowManager.getWindow(windowId);
        return window != null ? window.getHeight() : 0;
    }

    @Keep
    private long getWindowHardwareBuffer(int windowId, boolean useHALPixelFormatBGRA8888) {
        Window window = xServer.windowManager.getWindow(windowId);
        if (window != null) {
            GPUImage texture = GPUImage.createOrObtain(xServer, window.getContent(), false, useHALPixelFormatBGRA8888);
            return texture.getHardwareBufferPtr();
        }
        return 0;
    }

    @Keep
    private void updateWindowContent(int windowId) {
        Window window = xServer.windowManager.getWindow(windowId);
        if (window != null) {
            Drawable drawable = window.getContent();
            synchronized (drawable.renderLock) {
                drawable.forceUpdate();
            }
        }
    }

    @Override
    public void handleConnectionShutdown(ConnectedClient client) {
        if (client.getTag() != null) {
            long contextPtr = (long)client.getTag();
            destroyVkContext(contextPtr);
        }
    }

    @Override
    public void handleNewConnection(ConnectedClient client) {}

    @Override
    public boolean handleRequest(ConnectedClient client) throws IOException {
        XInputStream inputStream = client.getInputStream();
        if (inputStream.available() < 8) return false;
        int requestCode = inputStream.readInt();
        int requestLength = inputStream.readInt();

        if (requestCode == REQUEST_CODE_CREATE_CONTEXT) {
            long contextPtr = createVkContext(client.fd, options);
            if (contextPtr > 0) {
                client.setTag(contextPtr);
            }
            else connector.killConnection(client);
        }
        else if (requestCode > Short.MAX_VALUE && (requestCode >> 16) == REQUEST_CODE_SEND_EXTRA_DATA) {
            int requestId = requestCode & 0xffff;
            long contextPtr = (long)client.getTag();
            boolean success = handleExtraDataRequest(contextPtr, requestId, requestLength);
            if (!success) throw new IOException("Failed to handle extra data request.");
        }

        return true;
    }

    private native long createVkContext(int clientFd, Options options);

    private native void destroyVkContext(long contextPtr);

    private native void initVulkanWrapper(String cacheDir);

    private native boolean handleExtraDataRequest(long contextPtr, int requestCode, int requestLength);
}
