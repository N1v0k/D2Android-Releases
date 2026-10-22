package com.winlator.xconnector;

import com.winlator.xserver.XServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import dalvik.annotation.optimization.CriticalNative;

public class XOutputStream implements XStreamLock {
    private final ReentrantLock lock = new ReentrantLock();
    private final long nativePtr;
    /** D2Android: while an event is sent (lockForEvent): how long a send may wait for room, ms; -1: blocking */
    private int eventTimeoutMs = -1;
    /** D2Android: what the last event send of the lock holder did (EVENT_SENT, EVENT_DROPPED), read under the lock */
    private int eventResult;
    public static final int EVENT_SENT = 0, EVENT_DROPPED = 1;

    static {
        System.loadLibrary("winlator");
    }

    public XOutputStream(int clientFd, int initialCapacity) {
        nativePtr = nativeAllocate(clientFd, initialCapacity);
    }

    public void setAncillaryFd(int ancillaryFd) {
        setAncillaryFd(nativePtr, ancillaryFd);
    }

    public void writeByte(byte value) {
        writeByte(nativePtr, value);
    }

    public void writeShort(short value) {
        writeShort(nativePtr, value);
    }

    public void writeInt(int value) {
        writeInt(nativePtr, value);
    }

    public void writeLong(long value) {
        writeLong(nativePtr, value);
    }

    public void writeString8(String str) {
        byte[] bytes = str.getBytes(XServer.LATIN1_CHARSET);
        int length = -str.length() & 3;
        write(bytes);
        if (length > 0) writePad(length);
    }

    public void write(byte[] data) {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int length) {
        for (int i = offset; i < length; i++) writeByte(nativePtr, data[i]);
    }

    public void writeShortAt(int position, short value) {
        final byte[] data = {
            (byte)(value & 0xff),
            (byte)((value >> 8) & 0xff)
        };
        writeAt(nativePtr, position, data);
    }

    public void writeIntAt(int position, int value) {
        final byte[] data = {
            (byte)(value & 0xff),
            (byte)((value >>> 8) & 0xff),
            (byte)((value >>> 16) & 0xff),
            (byte)((value >>> 24) & 0xff)
        };
        writeAt(nativePtr, position, data);
    }

    public void writeAt(int position, byte[] data) {
        writeAt(nativePtr, position, data);
    }

    public void write(ByteBuffer data) {
        if (data.isDirect()) {
            writeByteBuffer(nativePtr, data, data.position(), data.remaining());
        }
        else {
            for (int i = data.position(), length = data.remaining(); i < length; i++) {
                writeByte(nativePtr, data.get(i));
            }
        }
    }

    public void writePad(int length) {
        writePad(nativePtr, length);
    }

    public void writeFP3232(float value) {
        float tmp;
        tmp = (float)Math.floor(value);
        int integral = (int)tmp;
        tmp = (value - tmp) * (1L<<32);
        int frac = (int)tmp;
        writeInt(integral);
        writeInt(frac);
    }

    public XStreamLock lock() {
        lock.lock();
        return this;
    }

    public void destroy() {
        destroy(nativePtr);
    }

    /**
     * D2Android: the lock for sending an event: waits at most lockWaitMs for the stream (null: another thread held
     * it that long), and its sends wait at most sendWaitMs for the client to make room, then the event is dropped
     * (xoutput_stream.c, XOutputStream_sendTimed; eventResult says which). A client that reads nothing must not block the
     * sending thread (UI: ANR; epoll: the whole X server). Close it like lock; nested lock calls stay in event mode.
     * Taken inside a lock this thread already holds (a reply being written), it stays a blocking send.
     */
    public XStreamLock lockForEvent(int lockWaitMs, int sendWaitMs) {
        try {
            if (!lock.tryLock(lockWaitMs, TimeUnit.MILLISECONDS)) return null;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        eventResult = EVENT_SENT;
        if (lock.getHoldCount() == 1) eventTimeoutMs = sendWaitMs;
        return this;
    }

    /** D2Android: whether the event sent under the lockForEvent this thread holds was dropped (call before closing it) */
    public int eventResult() {
        return eventResult;
    }

    @Override
    public void close() throws IOException {
        try {
            if (eventTimeoutMs >= 0) {
                // D2Android: an event's send, not blocking for long
                int result = sendEventData(nativePtr, eventTimeoutMs);
                if (result < 0) throw new IOException("Failed to send data.");
                if (result > 0) eventResult = EVENT_DROPPED;
            }
            else if (!sendData(nativePtr)) throw new IOException("Failed to send data.");
        }
        finally {
            if (lock.getHoldCount() == 1) eventTimeoutMs = -1; // D2Android: the outermost lock ends the event
            lock.unlock();
        }
    }

    public int length() {
        return length(nativePtr);
    }

    private native long nativeAllocate(int fd, int initialCapacity);

    @CriticalNative
    private static native void setAncillaryFd(long nativePtr, int ancillaryFd);

    @CriticalNative
    private static native void writeByte(long nativePtr, byte value);

    @CriticalNative
    private static native void writeShort(long nativePtr, short value);

    @CriticalNative
    private static native void writeInt(long nativePtr, int value);

    @CriticalNative
    private static native void writeLong(long nativePtr, long value);

    @CriticalNative
    private static native void writePad(long nativePtr, int length);

    private static native void writeAt(long nativePtr, int position, byte[] data);

    private static native void writeByteBuffer(long nativePtr, ByteBuffer data, int offset, int length);

    @CriticalNative
    private static native boolean sendData(long nativePtr);

    @CriticalNative
    private static native int sendEventData(long nativePtr, int timeoutMs); // D2Android: changed

    @CriticalNative
    private static native void destroy(long nativePtr);

    @CriticalNative
    private static native int length(long nativePtr);
}
