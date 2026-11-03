
#include <pthread.h>
#include <stdarg.h>
#include <stdio.h>
#include <time.h>

#include "vortek_log.h"

static pthread_mutex_t logMutex = PTHREAD_MUTEX_INITIALIZER;
static FILE* logFile = NULL;
static struct timespec logStart;

void vortekLog_open(const char* path) {
    pthread_mutex_lock(&logMutex);
    if (logFile) {
        fclose(logFile);
        logFile = NULL;
    }
    if (path && path[0]) {
        logFile = fopen(path, "w");
        clock_gettime(CLOCK_MONOTONIC, &logStart);
    }
    pthread_mutex_unlock(&logMutex);
}

void vortekLog(const char* format, ...) {
    pthread_mutex_lock(&logMutex);
    if (logFile) {
        struct timespec now;
        clock_gettime(CLOCK_MONOTONIC, &now);
        long ms = (long)(now.tv_sec - logStart.tv_sec) * 1000 + (now.tv_nsec - logStart.tv_nsec) / 1000000;
        fprintf(logFile, "%ld.%03ld ", ms / 1000, ms % 1000);
        va_list args;
        va_start(args, format);
        vfprintf(logFile, format, args);
        va_end(args);
        fputc('\n', logFile);
        fflush(logFile);
    }
    pthread_mutex_unlock(&logMutex);
}

bool vortekLog_few(int* count, int limit, const char* what) {
    pthread_mutex_lock(&logMutex);
    int n = (*count)++;
    pthread_mutex_unlock(&logMutex);
    if (n == limit) vortekLog("further %s are not logged", what);
    return n < limit;
}
