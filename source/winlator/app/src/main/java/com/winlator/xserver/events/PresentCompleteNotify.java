package com.winlator.xserver.events;

import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;
import com.winlator.xserver.Window;
import com.winlator.xserver.extensions.PresentExtension;

import java.io.IOException;

public class PresentCompleteNotify extends Event {
    public static final byte PRESENT_COMPLETE = 1;
    private final PresentExtension presentExtension;
    private final int eventId;
    private final int windowId;
    private final int serial;
    private final PresentExtension.CompleteKind kind;
    private final PresentExtension.CompleteMode mode;
    private final long ust;
    private final long msc;

    public PresentCompleteNotify(PresentExtension presentExtension, int eventId, Window window, int serial, PresentExtension.CompleteKind kind, PresentExtension.CompleteMode mode, long ust, long msc) {
        super(GENERIC_EVENT_ID);
        this.presentExtension = presentExtension;
        this.eventId = eventId;
        this.windowId = window.id;
        this.serial = serial;
        this.kind = kind;
        this.mode = mode;
        this.ust = ust;
        this.msc = msc;
    }

    @Override
    public void send(short sequenceNumber, XOutputStream outputStream) throws IOException {
        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte(code);
            outputStream.writeByte(presentExtension.getMajorOpcode());
            outputStream.writeShort(sequenceNumber);
            outputStream.writeInt(2);
            outputStream.writeShort(PRESENT_COMPLETE);
            outputStream.writeByte((byte)kind.ordinal());
            outputStream.writeByte((byte)mode.ordinal());
            outputStream.writeInt(eventId);
            outputStream.writeInt(windowId);
            outputStream.writeInt(serial);
            outputStream.writeLong(ust);
            outputStream.writeLong(msc);
        }
    }
}
