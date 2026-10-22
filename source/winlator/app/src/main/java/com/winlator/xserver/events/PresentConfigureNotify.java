package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Window;
import com.winlator.xserver.extensions.PresentExtension;

import java.io.IOException;

public class PresentConfigureNotify extends Event {
    public static final byte PRESENT_CONFIGURE = 0;
    private final PresentExtension presentExtension;
    private final int eventId;
    private final int windowId;
    private final short x;
    private final short y;
    private final short width;
    private final short height;
    private final short pixmapWidth;
    private final short pixmapHeight;
    private final int pixmapFlags;

    public PresentConfigureNotify(PresentExtension presentExtension, int eventId, Window window, int pixmapFlags) {
        super(GENERIC_EVENT_ID);
        this.presentExtension = presentExtension;
        this.eventId = eventId;
        this.windowId = window.id;
        this.x = window.getX();
        this.y = window.getY();
        this.width = window.getWidth();
        this.height = window.getHeight();
        this.pixmapWidth = width;
        this.pixmapHeight = height;
        this.pixmapFlags = pixmapFlags;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(code);
            outputStream.writeByte(presentExtension.getMajorOpcode());
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(2);
            outputStream.writeShort(PRESENT_CONFIGURE);
            outputStream.writeShort((short)0);
            outputStream.writeInt(eventId);
            outputStream.writeInt(windowId);
            outputStream.writeShort(x);
            outputStream.writeShort(y);
            outputStream.writeShort(width);
            outputStream.writeShort(height);
            outputStream.writeShort((short)0);
            outputStream.writeShort((short)0);
            outputStream.writeShort(pixmapWidth);
            outputStream.writeShort(pixmapHeight);
            outputStream.writeInt(pixmapFlags);
        }
    }
}
