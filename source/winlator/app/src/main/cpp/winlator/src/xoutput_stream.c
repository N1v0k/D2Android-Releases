#include <errno.h>  // Modified by D2Android (changes 2026-10-09 to 2026-10-30).
#include <malloc.h>
#include <poll.h>
#include <sys/socket.h>
#include <time.h>
#include <string.h>
#include <jni.h>
#include <unistd.h>

#include "winlator.h"
#include "socket_utils.h"

typedef struct XOutputStream {
    int fd;
    int ancillaryFd;
    struct {
        void* data;
        int capacity;
        int position;
        int limit;
    } buffer;
} XOutputStream;

static void ensureSpaceIsAvailable(XOutputStream* outputStream, int length) {
    if ((outputStream->buffer.capacity - outputStream->buffer.position) >= length) return;

    int newCapacity = outputStream->buffer.capacity + length;
    void* newData = realloc(outputStream->buffer.data, newCapacity);
    outputStream->buffer.capacity = newCapacity;
    outputStream->buffer.limit = newCapacity;
    outputStream->buffer.data = newData;
}

static XOutputStream* XOutputStream_allocate(int fd, int initialCapacity) {
    XOutputStream* outputStream = calloc(1, sizeof(XOutputStream));
    outputStream->fd = fd;
    outputStream->buffer.data = calloc(initialCapacity, 1);
    outputStream->buffer.capacity = initialCapacity;
    outputStream->buffer.limit = initialCapacity;
    return outputStream;
}

static void XOutputStream_destroy(XOutputStream* outputStream) {
    if (!outputStream) return;
    MEMFREE(outputStream->buffer.data);
    MEMFREE(outputStream);
}








static jboolean XOutputStream_send(XOutputStream* outputStream) {
    if (outputStream->buffer.position == 0) return JNI_TRUE;
    int total = outputStream->buffer.position;
    char* data = outputStream->buffer.data;
    outputStream->buffer.position = 0;
    outputStream->buffer.limit = outputStream->buffer.capacity;

    int sent = 0;
    if (outputStream->ancillaryFd > 0) {
        int ancillaryFd = outputStream->ancillaryFd;
        outputStream->ancillaryFd = 0;
        int bytesSent;
        do bytesSent = send_fds(outputStream->fd, &ancillaryFd, 1, data, total);
        while (bytesSent < 0 && errno == EINTR);
        if (bytesSent < 0) return JNI_FALSE;
        sent = bytesSent;
    }
    while (sent < total) {
        ssize_t bytesSent = write(outputStream->fd, data + sent, total - sent);
        if (bytesSent < 0) {
            if (errno == EINTR) continue;
            return JNI_FALSE;
        }
        if (bytesSent == 0) return JNI_FALSE;
        sent += (int) bytesSent;
    }
    return JNI_TRUE;
}

static long long monotonicMs(void) {
    struct timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return (long long) now.tv_sec * 1000 + now.tv_nsec / 1000000;
}









static int XOutputStream_sendTimed(XOutputStream* outputStream, int timeoutMs) {
    if (outputStream->buffer.position == 0) return 0;
    if (outputStream->ancillaryFd > 0) return XOutputStream_send(outputStream) ? 0 : -1;
    int total = outputStream->buffer.position;
    char* data = outputStream->buffer.data;
    outputStream->buffer.position = 0;
    outputStream->buffer.limit = outputStream->buffer.capacity;

    long long deadline = monotonicMs() + timeoutMs;
    int sent = 0;
    while (sent < total) {
        ssize_t bytesSent = send(outputStream->fd, data + sent, total - sent, MSG_DONTWAIT | MSG_NOSIGNAL);
        if (bytesSent > 0) {
            sent += (int) bytesSent;
            continue;
        }
        if (bytesSent < 0 && errno == EINTR) continue;
        if (bytesSent == 0 || (errno != EAGAIN && errno != EWOULDBLOCK)) return -1;
        int wait = -1;
        if (sent == 0) {
            long long left = deadline - monotonicMs();
            if (left <= 0) return 1;
            wait = (int) left;
        }
        struct pollfd pfd = {.fd = outputStream->fd, .events = POLLOUT};
        if (poll(&pfd, 1, wait) < 0 && errno != EINTR) return -1;
    }
    return 0;
}

JNIEXPORT jlong JNICALL
Java_com_winlator_xconnector_XOutputStream_nativeAllocate(JNIEnv *env, jobject obj, jint fd,
                                                          jint initialCapacity) {
    XOutputStream* outputStream = XOutputStream_allocate(fd, initialCapacity);
    return (jlong)outputStream;
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_setAncillaryFd(jlong nativePtr, jint ancillaryFd) {
    ((XOutputStream*)nativePtr)->ancillaryFd = ancillaryFd;
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_writeByte(jlong nativePtr, jbyte value) {
    XOutputStream* outputStream = (XOutputStream*)nativePtr;
    ensureSpaceIsAvailable(outputStream, 1);
    *(jbyte*)(outputStream->buffer.data + outputStream->buffer.position++) = value;
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_writeShort(jlong nativePtr, jshort value) {
    XOutputStream* outputStream = (XOutputStream*)nativePtr;
    ensureSpaceIsAvailable(outputStream, 2);
    *(jshort*)(outputStream->buffer.data + outputStream->buffer.position) = value;
    outputStream->buffer.position += 2;
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_writeInt(jlong nativePtr, jint value) {
    XOutputStream* outputStream = (XOutputStream*)nativePtr;
    ensureSpaceIsAvailable(outputStream, 4);
    *(jint*)(outputStream->buffer.data + outputStream->buffer.position) = value;
    outputStream->buffer.position += 4;
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_writeLong(jlong nativePtr, jlong value) {
    XOutputStream* outputStream = (XOutputStream*)nativePtr;
    ensureSpaceIsAvailable(outputStream, 8);
    *(jlong*)(outputStream->buffer.data + outputStream->buffer.position) = value;
    outputStream->buffer.position += 8;
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_writePad(jlong nativePtr, jint length) {
    XOutputStream* outputStream = (XOutputStream*)nativePtr;
    ensureSpaceIsAvailable(outputStream, length);
    memset(outputStream->buffer.data + outputStream->buffer.position, 0, length);
    outputStream->buffer.position += length;
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_writeAt(JNIEnv *env, jclass obj,
                                                   jlong nativePtr, jint position, jbyteArray data) {
    XOutputStream* outputStream = (XOutputStream*)nativePtr;
    jbyte* dataPtr = (*env)->GetByteArrayElements(env, data, 0);
    jsize length = (*env)->GetArrayLength(env, data);
    memcpy(outputStream->buffer.data + position, dataPtr, length);
    (*env)->ReleaseByteArrayElements(env, data, dataPtr, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_writeByteBuffer(JNIEnv *env, jclass obj,
                                                           jlong nativePtr, jobject data,
                                                           jint offset, jint length) {
    XOutputStream* outputStream = (XOutputStream*)nativePtr;
    void* dataAddr = (*env)->GetDirectBufferAddress(env, data);
    ensureSpaceIsAvailable(outputStream, length);
    memcpy(outputStream->buffer.data + outputStream->buffer.position, dataAddr + offset, length);
    outputStream->buffer.position += length;
}

JNIEXPORT jboolean JNICALL
Java_com_winlator_xconnector_XOutputStream_sendData(jlong nativePtr) {
    return XOutputStream_send((XOutputStream*)nativePtr);
}


JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_XOutputStream_sendEventData(jlong nativePtr, jint timeoutMs) {
    return XOutputStream_sendTimed((XOutputStream*)nativePtr, timeoutMs);
}

JNIEXPORT void JNICALL
Java_com_winlator_xconnector_XOutputStream_destroy(jlong nativePtr) {
    XOutputStream_destroy((XOutputStream*)nativePtr);
}

JNIEXPORT jint JNICALL
Java_com_winlator_xconnector_XOutputStream_length(jlong nativePtr) {
    XOutputStream* outputStream = (XOutputStream*)nativePtr;
    return outputStream->buffer.position;
}