// Modified by D2Android (changes 2026-10-09 to 2026-11-15).
#include "resource_memory.h"
#include "sysvshared_memory.h"
#include "vulkan_helper.h"
#include "dma_utils.h"
#include "vortek_log.h"

#include "native_handle.h"

extern int AHardwareBuffer_getFd(AHardwareBuffer* hardwareBuffer);
extern const native_handle_t* AHardwareBuffer_getNativeHandle(const AHardwareBuffer* buffer);
extern DeviceMemoryInfo deviceMemoryInfo;


#define ALLOCATIONS_LOGGED 3
#define FAILURES_LOGGED 8
static int loggedWays[3] = {0};
static int loggedFailures = 0;
static const char* wayNames[3] = {"opaque fd", "dma-buf", "ahardwarebuffer"};

static void logWay(int way, VkMemoryAllocateInfo* memoryInfo, int fd) {
    if (vortekLog_few(&loggedWays[way], ALLOCATIONS_LOGGED, "allocations of this way"))
        vortekLog("host-visible memory via %s: %llu bytes, type %u, fd %d", wayNames[way],
                  (unsigned long long)memoryInfo->allocationSize, memoryInfo->memoryTypeIndex, fd);
}

static void logFailure(const char* step, VkResult result, VkMemoryAllocateInfo* memoryInfo) {
    if (vortekLog_few(&loggedFailures, FAILURES_LOGGED, "allocation failures"))
        vortekLog("host-visible memory failed at %s: result %d, %llu bytes, type %u", step, result,
                  (unsigned long long)memoryInfo->allocationSize, memoryInfo->memoryTypeIndex);
}


static VkResult importDmaBuf(VkDevice device, VkMemoryAllocateInfo* memoryInfo, int fd, VkDeviceMemory* memory) {
    int dupFd = dup(fd);
    if (dupFd < 0) return VK_ERROR_INVALID_EXTERNAL_HANDLE;
    VkImportMemoryFdInfoKHR memoryImportInfo = {0};
    memoryImportInfo.sType = VK_STRUCTURE_TYPE_IMPORT_MEMORY_FD_INFO_KHR;
    memoryImportInfo.fd = dupFd;
    memoryImportInfo.handleType = VK_EXTERNAL_MEMORY_HANDLE_TYPE_DMA_BUF_BIT_EXT;

    const void* next = memoryInfo->pNext;
    memoryInfo->pNext = &memoryImportInfo;
    VkResult result = vulkanWrapper.vkAllocateMemory(device, memoryInfo, NULL, memory);
    memoryInfo->pNext = next;
    if (result != VK_SUCCESS) {
        CLOSEFD(dupFd);
        *memory = VK_NULL_HANDLE;
    }
    return result;
}

static const char* dmaBufKindNames[3] = {"dma_heap", "ion", "ion (abi before 4.12)"};






static int chooseDmaBufSource(VkContext* context, VkDevice device, VkMemoryAllocateInfo* memoryInfo, VkDeviceMemory* memory) {
    DmaBufSource sources[DMABUF_MAX_SOURCES];
    char ionInfo[96] = "";
    int count = dmabuf_list_sources(sources, ionInfo, sizeof(ionInfo));
    context->dmaBufSourceChosen = true;
    vortekLog("dma-buf: choosing the source for host-visible memory (test with %llu bytes, type %u): %d candidates, /dev/ion %s",
              (unsigned long long)memoryInfo->allocationSize, memoryInfo->memoryTypeIndex, count, ionInfo);

    for (int i = 0; i < count; i++) {
        DmaBufSource* source = &sources[i];
        const char* kind = dmaBufKindNames[source->kind];
        int fd = dmabuf_alloc_from(source, memoryInfo->allocationSize);
        if (fd < 0) {
            vortekLog("dma-buf candidate %s %s: no buffer, errno %d (%s)", kind, source->name, -fd, strerror(-fd));
            continue;
        }
        int mapError = dmabuf_test_mmap(fd, memoryInfo->allocationSize);
        if (mapError) {
            vortekLog("dma-buf candidate %s %s: buffer, mmap failed: errno %d (%s)", kind, source->name, mapError, strerror(mapError));
            CLOSEFD(fd);
            continue;
        }
        VkResult result = importDmaBuf(device, memoryInfo, fd, memory);
        if (result != VK_SUCCESS) {
            vortekLog("dma-buf candidate %s %s: buffer and mmap ok, Vulkan import failed: result %d", kind, source->name, result);
            CLOSEFD(fd);
            continue;
        }
        vortekLog("dma-buf candidate %s %s: buffer, mmap and Vulkan import ok, chosen", kind, source->name);
        context->dmaBufSource = malloc(sizeof(DmaBufSource));
        if (context->dmaBufSource) memcpy(context->dmaBufSource, source, sizeof(DmaBufSource));
        return fd;
    }
    vortekLog("dma-buf: no candidate works, host-visible memory comes from ahardwarebuffer");
    return -1;
}





#define HARDWARE_BUFFERS_INSPECTED 2
static int inspectedHardwareBuffers = 0;

static const char* accessModeName(int flags) {
    switch (flags & O_ACCMODE) {
        case O_RDONLY: return "read-only";
        case O_WRONLY: return "write-only";
        case O_RDWR: return "read/write";
        default: return "?";
    }
}

static void inspectHardwareBuffer(AHardwareBuffer* hardwareBuffer, int fd, uint64_t size) {
    if (!vortekLog_few(&inspectedHardwareBuffers, HARDWARE_BUFFERS_INSPECTED, "ahardwarebuffer inspections")) return;

    if (inspectedHardwareBuffers == 1) {
        char domain[128] = "?";
        FILE* file = fopen("/proc/self/attr/current", "r");
        if (file) {
            if (!fgets(domain, sizeof(domain), file)) strcpy(domain, "?");
            fclose(file);
            domain[strcspn(domain, "\n")] = '\0';
        }
        vortekLog("ahardwarebuffer check: selinux domain of the app %s", domain);
    }

    AHardwareBuffer_Desc desc = {0};
    AHardwareBuffer_describe(hardwareBuffer, &desc);
    const native_handle_t* handle = AHardwareBuffer_getNativeHandle(hardwareBuffer);
    vortekLog("ahardwarebuffer check (%llu bytes): width %u, stride %u, usage 0x%llx, native handle fds %d, ints %d, sent fd %d",
              (unsigned long long)size, desc.width, desc.stride, (unsigned long long)desc.usage,
              handle ? handle->numFds : -1, handle ? handle->numInts : -1, fd);

    for (int i = 0; handle && i < handle->numFds && i < 4; i++) {
        int handleFd = handle->data[i];
        char linkPath[64], target[256] = "?";
        snprintf(linkPath, sizeof(linkPath), "/proc/self/fd/%d", handleFd);
        ssize_t length = readlink(linkPath, target, sizeof(target) - 1);
        target[length > 0 ? length : 0] = '\0';
        int flags = fcntl(handleFd, F_GETFL);
        off_t end = lseek(handleFd, 0, SEEK_END);
        int seekError = end < 0 ? errno : 0;
        lseek(handleFd, 0, SEEK_SET);
        vortekLog("ahardwarebuffer check: fd[%d] %d -> '%s', %s (flags 0x%x), size %lld%s", i, handleFd,
                  length > 0 ? target : "?", flags < 0 ? "?" : accessModeName(flags), flags,
                  (long long)end, seekError ? " (lseek failed)" : "");
    }

    int mapError = dmabuf_test_mmap(fd, size);
    void* data = mmap(NULL, size, PROT_READ, MAP_SHARED, fd, 0);
    int readOnlyError = data == MAP_FAILED ? errno : 0;
    if (data != MAP_FAILED) munmap(data, size);
    vortekLog("ahardwarebuffer check: mmap read/write %s %d (%s), read-only %s %d (%s)",
              mapError ? "failed, errno" : "ok", mapError, mapError ? strerror(mapError) : "",
              readOnlyError ? "failed, errno" : "ok", readOnlyError, readOnlyError ? strerror(readOnlyError) : "");

    void* address = NULL;
    int lockResult = AHardwareBuffer_lock(hardwareBuffer, AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN | AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN,
                                          -1, NULL, &address);
    if (lockResult == 0) AHardwareBuffer_unlock(hardwareBuffer, NULL);
    vortekLog("ahardwarebuffer check: AHardwareBuffer_lock result %d, address %s", lockResult, address ? "set" : "none");
}

static ResourceMemory* internalAllocate() {
    ResourceMemory* resourceMemory = calloc(1, sizeof(ResourceMemory));
    resourceMemory->buffer = VK_NULL_HANDLE;
    resourceMemory->memory = VK_NULL_HANDLE;
    resourceMemory->fd = -1;
    return resourceMemory;
}

static AHardwareBuffer* allocateHardwareBuffer(int size) {
    AHardwareBuffer_Desc buffDesc = {0};
    buffDesc.width = size;
    buffDesc.height = 1;
    buffDesc.layers = 1;
    buffDesc.usage = AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN | AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN;
    buffDesc.format = AHARDWAREBUFFER_FORMAT_BLOB;

    AHardwareBuffer* hardwareBuffer = NULL;
    AHardwareBuffer_allocate(&buffDesc, &hardwareBuffer);

    return hardwareBuffer;
}

ResourceMemory* ResourceMemory_allocate(VkContext* context, VkDevice device, VkMemoryAllocateInfo* memoryInfo) {
    uint64_t maxAllocationSize = (VkDeviceSize)context->maxDeviceMemory << 20;
    if (maxAllocationSize == 0) maxAllocationSize = deviceMemoryInfo.maxAllocationSize;
    if ((context->totalAllocationSize + memoryInfo->allocationSize) >= maxAllocationSize) {

        logFailure(isHostVisibleMemory(memoryInfo->memoryTypeIndex) ? "the app's limit (maxDeviceMemory)" : "the app's limit (maxDeviceMemory), not host-visible",
                   VK_ERROR_OUT_OF_DEVICE_MEMORY, memoryInfo);
        return NULL;
    }

    VkResult result;
    ResourceMemory* resourceMemory = internalAllocate();
    if (isHostVisibleMemory(memoryInfo->memoryTypeIndex)) {
        bool hasExternalMemoryFd = context->hasExternalMemoryFd;
        bool hasExternalMemoryDMABuf = context->hasExternalMemoryDMABuf;

        switch (context->resourceMemoryType) {
            case RESOURCE_MEMORY_TYPE_OPAQUE_FD:
                hasExternalMemoryFd = true;
                hasExternalMemoryDMABuf = false;
                break;
            case RESOURCE_MEMORY_TYPE_DMA_BUF:
                hasExternalMemoryFd = false;
                hasExternalMemoryDMABuf = true;
                break;
            case RESOURCE_MEMORY_TYPE_AHARDWAREBUFFER:
                hasExternalMemoryFd = false;
                hasExternalMemoryDMABuf = false;
                break;
        }

        if (hasExternalMemoryFd) {
            VkExternalMemoryBufferCreateInfo externalMemoryBufferInfo = {0};
            externalMemoryBufferInfo.sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_BUFFER_CREATE_INFO;
            externalMemoryBufferInfo.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT;

            VkBufferCreateInfo bufferInfo = {0};
            bufferInfo.sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
            bufferInfo.pNext = &externalMemoryBufferInfo;
            bufferInfo.size = memoryInfo->allocationSize;
            bufferInfo.usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT |
                               VK_BUFFER_USAGE_TRANSFER_DST_BIT |
                               VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT |
                               VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT |
                               VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT |
                               VK_BUFFER_USAGE_STORAGE_BUFFER_BIT |
                               VK_BUFFER_USAGE_INDEX_BUFFER_BIT |
                               VK_BUFFER_USAGE_VERTEX_BUFFER_BIT |
                               VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;
            bufferInfo.sharingMode = VK_SHARING_MODE_EXCLUSIVE;

            result = vulkanWrapper.vkCreateBuffer(device, &bufferInfo, NULL, &resourceMemory->buffer);
            if (result != VK_SUCCESS) {
                logFailure("opaque fd: vkCreateBuffer", result, memoryInfo);
                goto error;
            }

            VkMemoryRequirements memReqs = {0};
            vulkanWrapper.vkGetBufferMemoryRequirements(device, resourceMemory->buffer, &memReqs);

            VkExportMemoryAllocateInfo exportMemoryInfo = {0};
            exportMemoryInfo.sType = VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO;
            exportMemoryInfo.handleTypes = VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT;

            VkMemoryDedicatedAllocateInfo memoryDedicatedInfo = {0};
            memoryDedicatedInfo.sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO;
            memoryDedicatedInfo.pNext = &exportMemoryInfo;
            memoryDedicatedInfo.image = VK_NULL_HANDLE;
            memoryDedicatedInfo.buffer = resourceMemory->buffer;

            memoryInfo->pNext = &memoryDedicatedInfo;
            memoryInfo->allocationSize = memReqs.size;
            result = vulkanWrapper.vkAllocateMemory(device, memoryInfo, NULL, &resourceMemory->memory);
            if (result != VK_SUCCESS) {
                logFailure("opaque fd: vkAllocateMemory", result, memoryInfo);
                goto error;
            }

            VkMemoryGetFdInfoKHR getFdInfo = {0};
            getFdInfo.sType = VK_STRUCTURE_TYPE_MEMORY_GET_FD_INFO_KHR;
            getFdInfo.memory = resourceMemory->memory;
            getFdInfo.handleType = VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT;

            int fd = -1;
            result = vulkanWrapper.vkGetMemoryFd(device, &getFdInfo, &fd);
            if (result != VK_SUCCESS || fd <= 0) {
                logFailure("opaque fd: vkGetMemoryFd", result, memoryInfo);
                goto error;
            }
            resourceMemory->fd = fd;
            logWay(0, memoryInfo, fd);
        }
        else {
            if ((getMemoryPropertyFlags(memoryInfo->memoryTypeIndex) & VK_MEMORY_PROPERTY_HOST_CACHED_BIT)) {
                memoryInfo->memoryTypeIndex = getMemoryTypeIndex(0x7FFFFFFF, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
            }

            bool useExternalMemoryAHB = !hasExternalMemoryDMABuf;
            if (hasExternalMemoryDMABuf) {

                int fd = -1;
                if (!context->dmaBufSourceChosen) {
                    fd = chooseDmaBufSource(context, device, memoryInfo, &resourceMemory->memory);
                }
                else if (context->dmaBufSource) {
                    fd = dmabuf_alloc_from(context->dmaBufSource, memoryInfo->allocationSize);
                    if (fd < 0) {

                        char step[112];
                        snprintf(step, sizeof(step), "dma-buf: no buffer from the chosen source (errno %d %s), next ahardwarebuffer",
                                 -fd, strerror(-fd));
                        logFailure(step, VK_SUCCESS, memoryInfo);
                        fd = -1;
                    }
                    else {
                        result = importDmaBuf(device, memoryInfo, fd, &resourceMemory->memory);
                        if (result != VK_SUCCESS) {
                            logFailure("dma-buf: vkAllocateMemory (import), next ahardwarebuffer", result, memoryInfo);
                            CLOSEFD(fd);
                            fd = -1;
                        }
                    }
                }

                if (fd >= 0) {
                    result = VK_SUCCESS;
                    resourceMemory->fd = fd;
                    logWay(1, memoryInfo, fd);
                }
                else useExternalMemoryAHB = true;
            }

            if (useExternalMemoryAHB) {
                AHardwareBuffer* hardwareBuffer = allocateHardwareBuffer(memoryInfo->allocationSize);
                if (!hardwareBuffer) {
                    logFailure("ahardwarebuffer: AHardwareBuffer_allocate", VK_SUCCESS, memoryInfo);
                    goto error;
                }
                resourceMemory->hardwareBuffer = hardwareBuffer;

                int fd = AHardwareBuffer_getFd(hardwareBuffer);
                if (fd <= 0) {
                    logFailure("ahardwarebuffer: AHardwareBuffer_getFd", VK_SUCCESS, memoryInfo);
                    goto error;
                }

                VkImportAndroidHardwareBufferInfoANDROID memoryImportInfo = {0};
                memoryImportInfo.sType = VK_STRUCTURE_TYPE_IMPORT_ANDROID_HARDWARE_BUFFER_INFO_ANDROID;
                memoryImportInfo.buffer = hardwareBuffer;

                memoryInfo->pNext = &memoryImportInfo;
                result = vulkanWrapper.vkAllocateMemory(device, memoryInfo, NULL, &resourceMemory->memory);
                if (result != VK_SUCCESS) {
                    logFailure("ahardwarebuffer: vkAllocateMemory (import)", result, memoryInfo);
                    goto error;
                }


                resourceMemory->fd = dup(fd);
                logWay(2, memoryInfo, resourceMemory->fd);
                inspectHardwareBuffer(hardwareBuffer, fd, memoryInfo->allocationSize);
            }
        }
    }
    else {
        result = vulkanWrapper.vkAllocateMemory(device, memoryInfo, NULL, &resourceMemory->memory);
        if (result != VK_SUCCESS) goto error;
    }

    if (result == VK_SUCCESS) context->totalAllocationSize += memoryInfo->allocationSize;
    resourceMemory->allocationSize = memoryInfo->allocationSize;
    return resourceMemory;

error:
    ResourceMemory_free(context, device, resourceMemory);
    return NULL;
}

void ResourceMemory_free(VkContext* context, VkDevice device, ResourceMemory* resourceMemory) {
    if (!resourceMemory) return;

    if (resourceMemory->buffer) {
        vulkanWrapper.vkDestroyBuffer(device, resourceMemory->buffer, NULL);
        resourceMemory->buffer = VK_NULL_HANDLE;
    }

    CLOSEFD(resourceMemory->fd);
    if (resourceMemory->memory) {
        vulkanWrapper.vkFreeMemory(device, resourceMemory->memory, NULL);
        resourceMemory->memory = VK_NULL_HANDLE;

        uint64_t totalAllocationSize = context->totalAllocationSize;
        totalAllocationSize = totalAllocationSize > resourceMemory->allocationSize ? (totalAllocationSize - resourceMemory->allocationSize) : 0;
        context->totalAllocationSize = totalAllocationSize;
    }

    if (resourceMemory->hardwareBuffer) {
        AHardwareBuffer_release(resourceMemory->hardwareBuffer);
        resourceMemory->hardwareBuffer = NULL;
    }

    resourceMemory->allocationSize = 0;
    MEMFREE(resourceMemory);
}