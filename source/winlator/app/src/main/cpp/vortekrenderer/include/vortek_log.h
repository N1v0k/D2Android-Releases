#ifndef VORTEK_LOG_H
#define VORTEK_LOG_H




#include <stdbool.h>


extern void vortekLog_open(const char* path);


extern void vortekLog(const char* format, ...) __attribute__((format(printf, 1, 2)));





extern bool vortekLog_few(int* count, int limit, const char* what);

#endif
