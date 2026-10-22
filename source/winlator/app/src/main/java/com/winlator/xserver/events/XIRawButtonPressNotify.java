package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.extensions.XInputExtension;

import java.io.IOException;

public class XIRawButtonPressNotify extends Event {
    public static final byte RAW_BUTTON_PRESS = 15;
    private final XInputExtension xInputExtension;
    private final short deviceId;
    private final int buttonCode;

    public XIRawButtonPressNotify(XInputExtension xInputExtension, short deviceId, int buttonCode) {
        super(GENERIC_EVENT_ID);
        this.xInputExtension = xInputExtension;
        this.deviceId = deviceId;
        this.buttonCode = buttonCode;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(code);
            outputStream.writeByte(xInputExtension.getMajorOpcode());
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(0);
            outputStream.writeShort(RAW_BUTTON_PRESS);
            outputStream.writeShort(deviceId);
            outputStream.writeInt((int)System.currentTimeMillis());
            outputStream.writeInt(buttonCode);
            outputStream.writeShort(deviceId);
            outputStream.writeShort((short)0);
            outputStream.writeInt(0);
            outputStream.writePad(4);
        }
    }
}
