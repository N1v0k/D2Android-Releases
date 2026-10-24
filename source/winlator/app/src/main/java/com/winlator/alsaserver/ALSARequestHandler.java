package com.winlator.alsaserver;  // Modified by D2Android (changes 2026-10-09 to 2026-10-24).

import com.winlator.sysvshm.SysVSharedMemory;
import com.winlator.xconnector.ConnectedClient;
import com.winlator.xconnector.RequestHandler;
import com.winlator.xconnector.XConnectorEpoll;
import com.winlator.xconnector.XInputStream;
import com.winlator.xconnector.XOutputStream;
import com.winlator.xconnector.XStreamLock;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ALSARequestHandler implements RequestHandler {

    private static final int PREPARE_LENGTH = 10;
    private static final int MIN_BUFFER_SIZE_LENGTH = 6;
    private int maxSHMemoryId = 0;

    @Override
    public boolean handleRequest(ConnectedClient client) throws IOException {
        ALSAClient alsaClient = (ALSAClient)client.getTag();
        XInputStream inputStream = client.getInputStream();
        XOutputStream outputStream = client.getOutputStream();

        if (inputStream.available() < 5) return false;
        byte requestCode = inputStream.readByte();
        int requestLength = inputStream.readInt();

        switch (requestCode) {
            case RequestCodes.CLOSE:
                alsaClient.release();
                break;
            case RequestCodes.START:
                alsaClient.start();
                break;
            case RequestCodes.STOP:
                alsaClient.stop();
                break;
            case RequestCodes.PAUSE:
                alsaClient.pause();
                break;
            case RequestCodes.PREPARE: {

                if (inputStream.available() < Math.max(requestLength, PREPARE_LENGTH)) return false;

                byte channels = inputStream.readByte();
                ALSAClient.DataType dataType = toDataType(inputStream.readByte());
                int sampleRate = inputStream.readInt();
                int bufferSize = inputStream.readInt();
                if (requestLength > PREPARE_LENGTH) inputStream.skip(requestLength - PREPARE_LENGTH);

                if (dataType == null) {


                    alsaClient.release();
                    if (ALSAClient.USE_SHARED_MEMORY) {
                        try (XStreamLock lock = outputStream.lock()) {
                            outputStream.writeByte((byte)0);
                        }
                    }
                    break;
                }

                alsaClient.setChannels(channels);
                alsaClient.setDataType(dataType);
                alsaClient.setSampleRate(sampleRate);
                alsaClient.setBufferSize(bufferSize);
                alsaClient.prepare();

                if (ALSAClient.USE_SHARED_MEMORY) createSharedMemory(alsaClient, outputStream);
                break;
            }
            case RequestCodes.WRITE:
                ByteBuffer sharedBuffer = alsaClient.getSharedBuffer();
                if (sharedBuffer != null) {
                    copySharedBuffer(alsaClient, requestLength, outputStream);
                    alsaClient.writeDataToTrack(alsaClient.getAuxBuffer());
                    sharedBuffer.putInt(0, alsaClient.pointer());
                }
                else {
                    if (inputStream.available() < requestLength) return false;
                    alsaClient.writeDataToTrack(inputStream.readByteBuffer(requestLength));
                }
                break;
            case RequestCodes.DRAIN:
                alsaClient.drain();
                break;
            case RequestCodes.POINTER:
                try (XStreamLock lock = outputStream.lock()) {
                    outputStream.writeInt(alsaClient.pointer());
                }
                break;
            case RequestCodes.MIN_BUFFER_SIZE: {

                if (inputStream.available() < Math.max(requestLength, MIN_BUFFER_SIZE_LENGTH)) return false;

                byte channels = inputStream.readByte();
                ALSAClient.DataType dataType = toDataType(inputStream.readByte());
                int sampleRate = inputStream.readInt();
                if (requestLength > MIN_BUFFER_SIZE_LENGTH) inputStream.skip(requestLength - MIN_BUFFER_SIZE_LENGTH);


                int minBufferSize = dataType == null ? 0 :
                    ALSAClient.latencyMillisToBufferSize(alsaClient.options.latencyMillis, channels, dataType, sampleRate);

                try (XStreamLock lock = outputStream.lock()) {
                    outputStream.writeInt(minBufferSize);
                }
                break;
            }
        }
        return true;
    }


    private static ALSAClient.DataType toDataType(byte value) {
        ALSAClient.DataType[] dataTypes = ALSAClient.DataType.values();
        return value >= 0 && value < dataTypes.length ? dataTypes[value] : null;
    }

    private void copySharedBuffer(ALSAClient alsaClient, int requestLength, XOutputStream outputStream) throws IOException {
        ByteBuffer sharedBuffer = alsaClient.getSharedBuffer();
        ByteBuffer auxBuffer = alsaClient.getAuxBuffer();

        auxBuffer.position(0).limit(requestLength);
        sharedBuffer.position(ALSAClient.BUFFER_OFFSET).limit(ALSAClient.BUFFER_OFFSET + requestLength);
        auxBuffer.put(sharedBuffer);

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte((byte)1);
        }
    }

    private void createSharedMemory(ALSAClient alsaClient, XOutputStream outputStream) throws IOException {
        int shmSize = alsaClient.getBufferSizeInBytes() + ALSAClient.BUFFER_OFFSET;
        int fd = SysVSharedMemory.createMemoryFd("alsa-shm"+(++maxSHMemoryId), shmSize);

        if (fd >= 0) {
            ByteBuffer buffer = SysVSharedMemory.mapSHMSegment(fd, shmSize, 0, false);
            if (buffer != null) alsaClient.setSharedBuffer(buffer);
        }

        try (XStreamLock lock = outputStream.lock()) {
            outputStream.writeByte((byte)0);
            outputStream.setAncillaryFd(fd);
        }
        finally {
            if (fd >= 0) XConnectorEpoll.closeFd(fd);
        }
    }
}
