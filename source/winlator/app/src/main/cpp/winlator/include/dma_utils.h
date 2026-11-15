#ifndef WINLATOR_DMA_UTILS_H  // Modified by D2Android (changes 2026-10-09 to 2026-11-15).
#define WINLATOR_DMA_UTILS_H

#include <fcntl.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <linux/dma-heap.h>
#include <dirent.h>
#include <errno.h>
#include <stdlib.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

#include "ion/ion.h"
#include "ion/ion_4.19.h"







#define DMABUF_KIND_HEAP 0
#define DMABUF_KIND_ION 1
#define DMABUF_KIND_ION_LEGACY 2
#define DMABUF_MAX_SOURCES (2 + 2 * ION_NUM_MAX_HEAPS)
#define DMABUF_NO_HEAP_TYPE 0xffffffffu

typedef struct DmaBufSource {
    int kind;
    unsigned int heapId;
    unsigned int heapType;
    char name[MAX_HEAP_NAME + 32];
} DmaBufSource;

static int safe_ioctl(int fd, uint64_t request, void *arg) {
    int res;

    do {
        res = ioctl(fd, request, arg);
    }
    while (res == -1 && (errno == EINTR || errno == EAGAIN));
    return res;
}



static inline bool ion_is_legacy_abi(int ion_fd) {
    struct ion_handle_data free = {0};
    free.handle = 0;
    return safe_ioctl(ion_fd, ION_IOC_FREE, &free) >= 0 || errno != ENOTTY;
}

static inline void dmabuf_add_heap(DmaBufSource* sources, int* count, const char* heapName) {
    char path[64];
    snprintf(path, sizeof(path), "/dev/dma_heap/%s", heapName);
    int fd = open(path, O_RDONLY | O_CLOEXEC);
    if (fd < 0) return;
    close(fd);
    DmaBufSource* source = &sources[(*count)++];
    source->kind = DMABUF_KIND_HEAP;
    source->heapId = 0;
    source->heapType = DMABUF_NO_HEAP_TYPE;
    snprintf(source->name, sizeof(source->name), "%s", heapName);
}



static inline int dmabuf_list_sources(DmaBufSource* sources, char* ionInfo, size_t ionInfoSize) {
    int count = 0;
    dmabuf_add_heap(sources, &count, "system-uncached");
    dmabuf_add_heap(sources, &count, "system");

    DIR* dir = opendir("/dev/dma_heap");
    if (dir) {
        char names[ION_NUM_MAX_HEAPS][MAX_HEAP_NAME];
        int nameCount = 0;
        struct dirent* entry;
        while ((entry = readdir(dir)) && nameCount < ION_NUM_MAX_HEAPS) {
            if (entry->d_name[0] == '.' || strcmp(entry->d_name, "system") == 0 || strcmp(entry->d_name, "system-uncached") == 0) continue;
            if (strlen(entry->d_name) >= MAX_HEAP_NAME) continue;
            strcpy(names[nameCount++], entry->d_name);
        }
        closedir(dir);
        qsort(names, nameCount, MAX_HEAP_NAME, (int (*)(const void*, const void*))strcmp);
        for (int i = 0; i < nameCount; i++) dmabuf_add_heap(sources, &count, names[i]);
    }

    int ion_fd = open("/dev/ion", O_RDONLY | O_CLOEXEC);
    if (ion_fd < 0) {
        if (ionInfo) snprintf(ionInfo, ionInfoSize, "none (errno %d)", errno);
        return count;
    }

    if (ion_is_legacy_abi(ion_fd)) {
        if (ionInfo) snprintf(ionInfo, ionInfoSize, "abi before 4.12, no heap query, heap ids 0-31 are tried");
        for (unsigned int id = 0; id < ION_NUM_MAX_HEAPS; id++) {
            DmaBufSource* source = &sources[count++];
            source->kind = DMABUF_KIND_ION_LEGACY;
            source->heapId = id;
            source->heapType = DMABUF_NO_HEAP_TYPE;
            snprintf(source->name, sizeof(source->name), "heap id %u", id);
        }
    }
    else {
        struct ion_heap_data heaps[ION_NUM_MAX_HEAPS];
        memset(heaps, 0, sizeof(heaps));
        struct ion_heap_query query = {0};
        query.cnt = ION_NUM_MAX_HEAPS;
        query.heaps = (__u64)(uintptr_t)heaps;
        if (safe_ioctl(ion_fd, ION_IOC_HEAP_QUERY, &query) < 0) {
            if (ionInfo) snprintf(ionInfo, ionInfoSize, "abi 4.12+, heap query failed (errno %d)", errno);
        }
        else {
            unsigned int heapCount = query.cnt < ION_NUM_MAX_HEAPS ? query.cnt : ION_NUM_MAX_HEAPS;
            if (ionInfo) snprintf(ionInfo, ionInfoSize, "abi 4.12+, %u heaps", heapCount);

            for (int pass = 0; pass < 2; pass++) {
                for (unsigned int i = 0; i < heapCount; i++) {
                    bool isSystem = heaps[i].type == ION_HEAP_TYPE_SYSTEM;
                    if ((pass == 0) != isSystem || heaps[i].heap_id >= ION_NUM_MAX_HEAPS) continue;
                    DmaBufSource* source = &sources[count++];
                    source->kind = DMABUF_KIND_ION;
                    source->heapId = heaps[i].heap_id;
                    source->heapType = heaps[i].type;
                    heaps[i].name[MAX_HEAP_NAME - 1] = '\0';
                    snprintf(source->name, sizeof(source->name), "heap id %u '%s' type %u", heaps[i].heap_id, heaps[i].name, heaps[i].type);
                }
            }
        }
    }
    close(ion_fd);
    return count;
}


static inline int dmabuf_alloc_from(const DmaBufSource* source, uint64_t size) {
    int res, err;
    if (source->kind == DMABUF_KIND_HEAP) {
        char path[64];
        snprintf(path, sizeof(path), "/dev/dma_heap/%s", source->name);
        int dma_fd = open(path, O_RDONLY | O_CLOEXEC);
        if (dma_fd < 0) return -errno;

        struct dma_heap_allocation_data alloc_data = {
            .len = size,
            .fd_flags = O_RDWR | O_CLOEXEC
        };

        res = safe_ioctl(dma_fd, DMA_HEAP_IOCTL_ALLOC, &alloc_data);
        err = errno;
        close(dma_fd);

        if (res) return -err;
        return alloc_data.fd;
    }

    int ion_fd = open("/dev/ion", O_RDONLY | O_CLOEXEC);
    if (ion_fd < 0) return -errno;

    if (source->kind == DMABUF_KIND_ION_LEGACY) {
        struct ion_allocation_data alloc_data = {
            .len = size,
            .align = 4096,
            .heap_id_mask = 1u << source->heapId,
            .flags = 0,
            .handle = -1
        };

        res = safe_ioctl(ion_fd, ION_IOC_ALLOC, &alloc_data);
        if (res) {
            err = errno;
            close(ion_fd);
            return -err;
        }

        struct ion_fd_data share = {
            .handle = alloc_data.handle,
            .fd = -1,
        };

        res = safe_ioctl(ion_fd, ION_IOC_SHARE, &share);
        err = errno;


        struct ion_handle_data free = {0};
        free.handle = alloc_data.handle;
        safe_ioctl(ion_fd, ION_IOC_FREE, &free);
        close(ion_fd);

        if (res) return -err;
        return share.fd;
    }

    struct ion_new_allocation_data alloc_data = {
        .len = size,
        .heap_id_mask = 1u << source->heapId,
        .flags = 0,
        .fd = -1,
    };

    res = safe_ioctl(ion_fd, ION_IOC_NEW_ALLOC, &alloc_data);
    err = errno;
    close(ion_fd);

    if (res) return -err;
    return (int)alloc_data.fd;
}



static inline int dmabuf_test_mmap(int fd, uint64_t size) {
    void* data = mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (data == MAP_FAILED) return errno;
    volatile unsigned char* bytes = data;
    bytes[0] = 0x5a;
    int ok = bytes[0] == 0x5a;
    bytes[0] = 0;
    munmap(data, size);
    return ok ? 0 : EIO;
}

static inline uint64_t getTotalSystemMemory() {
    FILE* file = fopen("/proc/meminfo", "r");
    if (!file) return 0;
    uint64_t memTotalKB = 0;

    char line[256];
    while (fgets(line, sizeof(line), file) != NULL) {
        if (strncmp(line, "MemTotal:", 9) == 0) {
            sscanf(line, "MemTotal: %ld kB", &memTotalKB);
            break;
        }
    }

    fclose(file);
    return memTotalKB * 1024;
}

static inline uint64_t getAvailableSystemMemory() {
    FILE* file = fopen("/proc/meminfo", "r");
    if (!file) return 0;
    uint64_t availMemKB = 0;

    char line[256];
    while (fgets(line, sizeof(line), file) != NULL) {
        if (strncmp(line, "MemAvailable:", 13) == 0) {
            sscanf(line, "MemAvailable: %ld kB", &availMemKB);
            break;
        }
        else if (strncmp(line, "MemFree:", 8) == 0 && availMemKB == 0) {
            sscanf(line, "MemFree: %ld kB", &availMemKB);
        }
    }

    fclose(file);
    return availMemKB * 1024;
}

#endif