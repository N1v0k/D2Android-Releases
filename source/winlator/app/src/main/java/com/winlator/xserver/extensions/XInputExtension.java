package com.winlator.xserver.extensions;

import static com.winlator.xserver.XClientRequestHandler.RESPONSE_CODE_SUCCESS;

import com.winlator.core.Bitmask;
import com.winlator.core.Callback;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Window;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XResource;
import com.winlator.xserver.XResourceManager;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.BadImplementation;
import com.winlator.xserver.errors.BadValue;
import com.winlator.xserver.errors.BadWindow;
import com.winlator.xserver.errors.XRequestError;
import com.winlator.xserver.events.XIRawButtonPressNotify;
import com.winlator.xserver.events.XIRawButtonReleaseNotify;
import com.winlator.xserver.events.XIRawMotionNotify;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class XInputExtension extends Extension implements XResourceManager.OnResourceLifecycleListener {
    public static final byte MAJOR_VERSION = 2;
    public static final byte MINOR_VERSION = 1;
    public enum DeviceClass {KEY_CLASS, BUTTON_CLASS, VALUATOR_CLASS}
    public enum DeviceID {ALL_DEVICES, ALL_MASTER_DEVICES, MASTER_POINTER, MASTER_KEYBOARD}
    private static final String MASTER_POINTER_NAME = "Virtual Core Pointer";
    private static final byte POINTER_BUTTON_COUNT = 3;
    private final List<XIEvent> events = new CopyOnWriteArrayList<>();
    private final Callback<XClient> onDestroyClientListener = (client) -> {
        for (int i = events.size()-1; i >= 0; i--) {
            XIEvent event = events.get(i);
            if (event.client == client) events.remove(i);
        }
    };

    public XInputExtension(XServer xServer, byte majorOpcode) {
        super(xServer, majorOpcode);
        xServer.windowManager.addOnResourceLifecycleListener(this);
    }

    private static abstract class ClientOpcodes {
        private static final byte GET_EXTENSION_VERSION = 1;
        private static final byte GET_CLIENT_POINTER = 45;
        private static final byte SELECT_EVENTS = 46;
        private static final byte QUERY_VERSION = 47;
        private static final byte QUERY_DEVICE = 48;
    }

    private static class XIEvent {
        private int deviceId;
        private Window window;
        private XClient client;
        private Bitmask mask;
    }

    @Override
    public String getName() {
        return "XInputExtension";
    }

    @Override
    public byte getEventCount() {
        return 3;
    }

    private void getExtensionVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        client.skipRequest();

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort(MAJOR_VERSION);
            outputStream.writeShort(MINOR_VERSION);
            outputStream.writeByte((byte)1);
            outputStream.writePad(19);
        }
    }

    private void getClientPointer(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)0);
            outputStream.writeShort((short)DeviceID.MASTER_POINTER.ordinal());
            outputStream.writePad(20);
        }
    }

    private void selectEvents(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int windowId = inputStream.readInt();
        short numMasks = inputStream.readShort();
        inputStream.skip(2);

        Window window = xServer.windowManager.getWindow(windowId);
        if (window == null) throw new BadWindow(windowId);

        if (numMasks == 0) throw new BadValue(numMasks);

        for (int i = 0, j; i < numMasks; i++) {
            short deviceId = inputStream.readShort();
            short maskLen = inputStream.readShort();

            Bitmask mask = new Bitmask();
            for (j = 0; j < maskLen; j++) {
                int value = inputStream.readInt();
                mask.set(value << j);
            }

            for (j = events.size()-1; j >= 0; j--) {
                XIEvent event = events.get(j);
                if (event.client == client && event.window == window && event.deviceId == deviceId) {
                    events.remove(j);
                    break;
                }
            }

            XIEvent event = new XIEvent();
            event.deviceId = deviceId;
            event.window = window;
            event.client = client;
            event.mask = mask;

            client.addOnDestroyListener(onDestroyClientListener);
            events.add(event);
        }

        inputStream.skip(client.getRemainingRequestLength());
    }

    private void queryVersion(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort(MAJOR_VERSION);
            outputStream.writeShort(MINOR_VERSION);
            outputStream.writePad(20);
        }
    }

    private void queryDevice(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        inputStream.skip(4);

        final short numDevices = 1;
        final short numClasses = 3;

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS);
            outputStream.writeByte((byte)0);
            outputStream.writeShort(client.getSequenceNumber());
            outputStream.writeInt(0);
            outputStream.writeShort(numDevices);
            outputStream.writePad(22);

            outputStream.writeShort((short)DeviceID.MASTER_POINTER.ordinal());
            outputStream.writeShort((short)1);
            outputStream.writeShort((short)0);
            outputStream.writeShort(numClasses);
            outputStream.writeShort((short)MASTER_POINTER_NAME.length());
            outputStream.writeByte((byte)1);
            outputStream.writeByte((byte)0);
            outputStream.writeString8(MASTER_POINTER_NAME);

            int start = outputStream.length();
            outputStream.writeShort((short)DeviceClass.BUTTON_CLASS.ordinal());
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)DeviceID.MASTER_POINTER.ordinal());
            outputStream.writeShort(POINTER_BUTTON_COUNT);
            outputStream.writeInt(0);
            for (int i = 0; i < POINTER_BUTTON_COUNT; i++) outputStream.writeInt(0);
            int deviceClassLength = (outputStream.length() - start) / 4;
            outputStream.writeIntAt(start + 2, deviceClassLength);

            for (int i = 0; i < 2; i++) {
                outputStream.writeShort((short)DeviceClass.VALUATOR_CLASS.ordinal());
                outputStream.writeShort((short)11);
                outputStream.writeShort((short)DeviceID.MASTER_POINTER.ordinal());
                outputStream.writeShort((short)i);
                outputStream.writeInt(0);
                outputStream.writeFP3232(0);
                outputStream.writeFP3232(0);
                outputStream.writeFP3232(0);
                outputStream.writeInt(0);
                outputStream.writeByte((byte)0);
                outputStream.writePad(3);
            }

            short deviceInfoLength = (short)((outputStream.length() - 32) / 4);
            outputStream.writeIntAt(4, deviceInfoLength);
        }
    }

    @Override
    public void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError {
        int opcode = client.getRequestData();

        switch (opcode) {
            case ClientOpcodes.GET_EXTENSION_VERSION:
                getExtensionVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.GET_CLIENT_POINTER:
                getClientPointer(client, inputStream, outputStream);
                break;
            case ClientOpcodes.SELECT_EVENTS:
                selectEvents(client, inputStream, outputStream);
                break;
            case ClientOpcodes.QUERY_VERSION:
                queryVersion(client, inputStream, outputStream);
                break;
            case ClientOpcodes.QUERY_DEVICE:
                queryDevice(client, inputStream, outputStream);
                break;
            default:
                throw new BadImplementation();
        }
    }

    private boolean isValidDeviceId(int deviceId) {
        return deviceId == DeviceID.MASTER_POINTER.ordinal() ||
               deviceId == DeviceID.ALL_DEVICES.ordinal() ||
               deviceId == DeviceID.ALL_MASTER_DEVICES.ordinal();
    }

    public void sendRawButtonState(int buttonCode, boolean pressed) {
        int mask = pressed ? (1<<XIRawButtonPressNotify.RAW_BUTTON_PRESS) : (1<<XIRawButtonReleaseNotify.RAW_BUTTON_RELEASE);

        for (XIEvent event : events) {
            if (isValidDeviceId(event.deviceId) && event.mask.isSet(mask)) {
                if (pressed) {
                    event.client.sendEvent(new XIRawButtonPressNotify(this, (short)DeviceID.MASTER_POINTER.ordinal(), buttonCode));
                }
                else event.client.sendEvent(new XIRawButtonReleaseNotify(this, (short)DeviceID.MASTER_POINTER.ordinal(), buttonCode));
            }
        }
    }

    public void sendRawMotion(float dx, float dy) {
        final int mask = 1<<XIRawMotionNotify.RAW_MOTION;
        final int valuatorMask = 1 | 2;

        for (XIEvent event : events) {
            if (isValidDeviceId(event.deviceId) && event.mask.isSet(mask)) {
                event.client.sendEvent(new XIRawMotionNotify(this, (short)DeviceID.MASTER_POINTER.ordinal(), new float[]{dx, dy}, valuatorMask));
            }
        }
    }

    @Override
    public void onFreeResource(XResource resource) {
        if (resource instanceof Window) {
            for (int i = events.size()-1; i >= 0; i--) {
                XIEvent event = events.get(i);
                if (event.window == resource) events.remove(i);
            }
        }
    }
}
