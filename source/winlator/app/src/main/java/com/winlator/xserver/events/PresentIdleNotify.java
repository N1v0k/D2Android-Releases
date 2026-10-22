package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Pixmap;
import com.winlator.xserver.Window;
import com.winlator.xserver.extensions.PresentExtension;

import java.io.IOException;

public class PresentIdleNotify extends Event {
    public static final byte PRESENT_IDLE = 2;
    private final PresentExtension presentExtension;
    private final int eventId;
    private final int windowId;
    private final int pixmapId;
    private final int serial;
    private final int idleFence;

    public PresentIdleNotify(PresentExtension presentExtension, int eventId, Window window, Pixmap pixmap, int serial, int idleFence) {
        super(GENERIC_EVENT_ID);
        this.presentExtension = presentExtension;
        this.eventId = eventId;
        this.windowId = window.id;
        this.pixmapId = pixmap.id;
        this.serial = serial;
        this.idleFence = idleFence;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(code);
            outputStream.writeByte(presentExtension.getMajorOpcode());
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(0);
            outputStream.writeShort(PRESENT_IDLE);
            outputStream.writeShort((short)0);
            outputStream.writeInt(eventId);
            outputStream.writeInt(windowId);
            outputStream.writeInt(serial);
            outputStream.writeInt(pixmapId);
            outputStream.writeInt(idleFence);
        }
    }
}
