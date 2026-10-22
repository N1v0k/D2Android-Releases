package com.winlator.xserver.extensions;

import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xserver.XClient;
import com.winlator.xserver.XServer;
import com.winlator.xserver.errors.XRequestError;

import java.io.IOException;

public abstract class Extension {
    public static final byte START_MAJOR_OPCODE = -100;
    private final byte majorOpcode;
    protected final XServer xServer;
    private byte firstEventId;
    private byte firstErrorId;

    public Extension(XServer xServer, byte majorOpcode) {
        this.xServer = xServer;
        this.majorOpcode = majorOpcode;
    }

    public abstract String getName();

    public byte getMajorOpcode() {
        return majorOpcode;
    }

    public final byte getFirstEventId() {
        return firstEventId;
    }

    public final void setFirstEventId(byte firstEventId) {
        this.firstEventId = firstEventId;
    }

    public final byte getFirstErrorId() {
        return firstErrorId;
    }

    public final void setFirstErrorId(byte firstErrorId) {
        this.firstErrorId = firstErrorId;
    }

    public byte getEventCount() {
        return 0;
    }

    public byte getErrorCount() {
        return 0;
    }

    public abstract void handleRequest(XClient client, XInputStream inputStream, XOutputStream outputStream) throws IOException, XRequestError;
}
