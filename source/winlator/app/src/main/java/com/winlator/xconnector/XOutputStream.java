package com.winlator.xconnector;  // Modified by D2Android (changes 2026-10-09 to 2026-10-24).

import com.winlator.xserver.XServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import dalvik.annotation.optimization.CriticalNative;

public class XOutputStream implements XStreamLock {
    private final ReentrantLock lock = new ReentrantLock();
    private final long nativePtr;

    private int eventTimeoutMs = -1;

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


    public int eventResult() {
        return eventResult;
    }

    @Override
    public void close() throws IOException {
        try {
            if (eventTimeoutMs >= 0) {

                int result = sendEventData(nativePtr, eventTimeoutMs);
                if (result < 0) throw new IOException("Failed to send data.");
                if (result > 0) eventResult = EVENT_DROPPED;
            }
            else if (!sendData(nativePtr)) throw new IOException("Failed to send data.");
        }
        finally {
            if (lock.getHoldCount() == 1) eventTimeoutMs = -1;
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
    private static native int sendEventData(long nativePtr, int timeoutMs);

    @CriticalNative
    private static native void destroy(long nativePtr);

    @CriticalNative
    private static native int length(long nativePtr);
}
