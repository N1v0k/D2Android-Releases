#include <jni.h>  // Modified by D2Android (changes 2026-10-09 to 2026-11-04).
#include <malloc.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <dlfcn.h>
#include <stdbool.h>
#include <unistd.h>

#define VK_NO_PROTOTYPES 1
#include <vulkan/vulkan.h>
#include <EGL/egl.h>

#include "winlator.h"
#include "file_utils.h"

EGLContext globalEGLContext = EGL_NO_CONTEXT;

JNIEXPORT jobjectArray JNICALL
Java_com_winlator_core_GPUHelper_vkGetDeviceExtensions(JNIEnv *env, jclass obj) {
    VkInstanceCreateInfo createInfo = {0};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    VkExtensionProperties* properties = NULL;
    jobjectArray stringArray = NULL;
    VkResult result;
    VkInstance instance = VK_NULL_HANDLE;

    void* libvulkan = dlopen(LIBVULKAN_PATH, RTLD_NOW | RTLD_LOCAL);
    if (!libvulkan) goto done;

    PFN_vkCreateInstance vkCreateInstance = dlsym(libvulkan, "vkCreateInstance");
    PFN_vkDestroyInstance vkDestroyInstance = dlsym(libvulkan, "vkDestroyInstance");
    PFN_vkEnumeratePhysicalDevices vkEnumeratePhysicalDevices = dlsym(libvulkan, "vkEnumeratePhysicalDevices");
    PFN_vkEnumerateDeviceExtensionProperties vkEnumerateDeviceExtensionProperties = dlsym(libvulkan, "vkEnumerateDeviceExtensionProperties");

    result = vkCreateInstance(&createInfo, NULL, &instance);
    if (result != VK_SUCCESS) goto done;

    uint32_t deviceCount = 1;
    VkPhysicalDevice physicalDevice;
    result = vkEnumeratePhysicalDevices(instance, &deviceCount, &physicalDevice);
    if (result != VK_SUCCESS && result != VK_INCOMPLETE) goto done;

    uint32_t propertyCount = 0;
    result = vkEnumerateDeviceExtensionProperties(physicalDevice, NULL, &propertyCount, NULL);
    if (result != VK_SUCCESS || propertyCount == 0) goto done;

    properties = calloc(propertyCount, sizeof(VkExtensionProperties));
    result = vkEnumerateDeviceExtensionProperties(physicalDevice, NULL, &propertyCount, properties);
    if (result != VK_SUCCESS) goto done;

    stringArray = (*env)->NewObjectArray(env, propertyCount, (*env)->FindClass(env, "java/lang/String"), 0);
    for (int i = 0; i < propertyCount; i++) {
        jstring string = (*env)->NewStringUTF(env, properties[i].extensionName);
        (*env)->SetObjectArrayElement(env, stringArray, i, string);
    }

done:
    if (instance) vkDestroyInstance(instance, NULL);
    if (properties) free(properties);
    if (!stringArray) stringArray = (*env)->NewObjectArray(env, 0, (*env)->FindClass(env, "java/lang/String"), 0);
    if (libvulkan) dlclose(libvulkan);
    return stringArray;
}


static const char* vkApiVersionFile() {
    static char path[160];
    if (!path[0]) {
        char name[96] = {0};
        FILE* f = fopen("/proc/self/cmdline", "r");
        if (f) {
            fread(name, 1, sizeof(name) - 1, f);
            fclose(f);
        }
        char* colon = strchr(name, ':');
        if (colon) *colon = 0;
        snprintf(path, sizeof(path), "/data/data/%s/cache/.vk-api-version", name[0] ? name : "at.d2android");
    }
    return path;
}

JNIEXPORT jint JNICALL
Java_com_winlator_core_GPUHelper_vkGetApiVersion() {
    int version = 0;
    char* content = fileGetContents(vkApiVersionFile(), NULL, NULL);
    if (content) {
        version = strtol(content, NULL, 10);
        MEMFREE(content);
        if (version > 0) return version;
    }

    VkResult result;
    VkInstance instance = VK_NULL_HANDLE;

    void* libvulkan = dlopen(LIBVULKAN_PATH, RTLD_NOW | RTLD_LOCAL);
    if (!libvulkan) goto done;

    PFN_vkCreateInstance vkCreateInstance = dlsym(libvulkan, "vkCreateInstance");
    PFN_vkDestroyInstance vkDestroyInstance = dlsym(libvulkan, "vkDestroyInstance");
    PFN_vkEnumeratePhysicalDevices vkEnumeratePhysicalDevices = dlsym(libvulkan, "vkEnumeratePhysicalDevices");
    PFN_vkGetPhysicalDeviceProperties vkGetPhysicalDeviceProperties = dlsym(libvulkan, "vkGetPhysicalDeviceProperties");
    PFN_vkGetPhysicalDeviceFeatures vkGetPhysicalDeviceFeatures = dlsym(libvulkan, "vkGetPhysicalDeviceFeatures");

    VkInstanceCreateInfo createInfo = {0};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;

    result = vkCreateInstance(&createInfo, NULL, &instance);
    if (result != VK_SUCCESS) goto done;

    uint32_t deviceCount = 1;
    VkPhysicalDevice physicalDevice;
    result = vkEnumeratePhysicalDevices(instance, &deviceCount, &physicalDevice);
    if (result != VK_SUCCESS && result != VK_INCOMPLETE) goto done;

    VkPhysicalDeviceProperties properties = {0};
    vkGetPhysicalDeviceProperties(physicalDevice, &properties);

    version = properties.apiVersion;
    if (version >= VK_MAKE_VERSION(1, 3, 0)) {
        VkPhysicalDeviceFeatures features = {0};
        vkGetPhysicalDeviceFeatures(physicalDevice, &features);
        VkPhysicalDeviceFeatures requiredFeatures = {VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_FALSE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_FALSE, VK_TRUE, VK_TRUE, VK_FALSE, VK_FALSE, VK_TRUE, VK_TRUE, VK_TRUE, VK_FALSE, VK_TRUE, VK_TRUE, VK_TRUE, VK_TRUE, VK_FALSE, VK_TRUE, VK_TRUE};

        for (int offset = 0; offset < sizeof(VkPhysicalDeviceFeatures); offset += sizeof(VkBool32)) {
            VkBool32 srcValue = *(VkBool32*)(((char*)&features) + offset);
            VkBool32 dstValue = *(VkBool32*)(((char*)&requiredFeatures) + offset);

            if (srcValue != dstValue) {
                version = VK_MAKE_VERSION(1, 2, 0);
                break;
            }
        }
    }

    char value[32] = {0};
    sprintf(value, "%d", version);
    filePutContents(vkApiVersionFile(), value, strlen(value));

done:
    if (instance) vkDestroyInstance(instance, NULL);
    if (libvulkan) dlclose(libvulkan);
    return version;
}



JNIEXPORT jlongArray JNICALL
Java_com_winlator_core_GPUHelper_vkGetDeviceVersions(JNIEnv *env, jclass obj) {
    jlong values[3] = {0};
    int found = 0;
    VkInstance instance = VK_NULL_HANDLE;

    void* libvulkan = dlopen(LIBVULKAN_PATH, RTLD_NOW | RTLD_LOCAL);
    if (!libvulkan) goto done;

    PFN_vkCreateInstance vkCreateInstance = dlsym(libvulkan, "vkCreateInstance");
    PFN_vkDestroyInstance vkDestroyInstance = dlsym(libvulkan, "vkDestroyInstance");
    PFN_vkEnumeratePhysicalDevices vkEnumeratePhysicalDevices = dlsym(libvulkan, "vkEnumeratePhysicalDevices");
    PFN_vkGetPhysicalDeviceProperties vkGetPhysicalDeviceProperties = dlsym(libvulkan, "vkGetPhysicalDeviceProperties");
    if (!vkCreateInstance || !vkDestroyInstance || !vkEnumeratePhysicalDevices || !vkGetPhysicalDeviceProperties) goto done;

    VkInstanceCreateInfo createInfo = {0};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    if (vkCreateInstance(&createInfo, NULL, &instance) != VK_SUCCESS) {
        instance = VK_NULL_HANDLE;
        goto done;
    }

    uint32_t deviceCount = 1;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkResult result = vkEnumeratePhysicalDevices(instance, &deviceCount, &physicalDevice);
    if ((result != VK_SUCCESS && result != VK_INCOMPLETE) || deviceCount == 0 || physicalDevice == VK_NULL_HANDLE) goto done;

    VkPhysicalDeviceProperties properties = {0};
    vkGetPhysicalDeviceProperties(physicalDevice, &properties);
    values[0] = (jlong)(uint32_t)properties.apiVersion;
    values[1] = (jlong)(uint32_t)properties.driverVersion;
    values[2] = (jlong)(uint32_t)properties.vendorID;
    found = 1;

done:
    if (instance) {
        PFN_vkDestroyInstance destroy = libvulkan ? dlsym(libvulkan, "vkDestroyInstance") : NULL;
        if (destroy) destroy(instance, NULL);
    }
    if (libvulkan) dlclose(libvulkan);
    jlongArray out = (*env)->NewLongArray(env, found ? 3 : 0);
    if (out && found) (*env)->SetLongArrayRegion(env, out, 0, 3, values);
    return out;
}


static int appendExternalMemory(char* out, int size, int used, PFN_vkGetPhysicalDeviceExternalBufferProperties get,
                                VkPhysicalDevice physicalDevice, VkExternalMemoryHandleTypeFlagBits handleType, const char* name) {
    VkPhysicalDeviceExternalBufferInfo bufferInfo = {0};
    bufferInfo.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_BUFFER_INFO;
    bufferInfo.handleType = handleType;

    bufferInfo.usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT |
                       VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT | VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT |
                       VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT;
    VkExternalBufferProperties properties = {0};
    properties.sType = VK_STRUCTURE_TYPE_EXTERNAL_BUFFER_PROPERTIES;
    get(physicalDevice, &bufferInfo, &properties);
    VkExternalMemoryProperties* p = &properties.externalMemoryProperties;
    if (used >= size) return used;
    return used + snprintf(out + used, size - used, "%s: features 0x%x (%s%s%s), compatible 0x%x\n", name, p->externalMemoryFeatures,
                           (p->externalMemoryFeatures & VK_EXTERNAL_MEMORY_FEATURE_DEDICATED_ONLY_BIT) ? "dedicated only " : "",
                           (p->externalMemoryFeatures & VK_EXTERNAL_MEMORY_FEATURE_EXPORTABLE_BIT) ? "exportable " : "",
                           (p->externalMemoryFeatures & VK_EXTERNAL_MEMORY_FEATURE_IMPORTABLE_BIT) ? "importable" : "",
                           p->compatibleHandleTypes);
}




JNIEXPORT jstring JNICALL
Java_com_winlator_core_GPUHelper_vkGetMemoryInfo(JNIEnv *env, jclass obj) {
    char out[2048] = {0};
    int used = 0;
    VkInstance instance = VK_NULL_HANDLE;
    VkExtensionProperties* extensions = NULL;
    PFN_vkDestroyInstance vkDestroyInstance = NULL;

    void* libvulkan = dlopen(LIBVULKAN_PATH, RTLD_NOW | RTLD_LOCAL);
    if (!libvulkan) goto done;

    PFN_vkCreateInstance vkCreateInstance = dlsym(libvulkan, "vkCreateInstance");
    vkDestroyInstance = dlsym(libvulkan, "vkDestroyInstance");
    PFN_vkEnumeratePhysicalDevices vkEnumeratePhysicalDevices = dlsym(libvulkan, "vkEnumeratePhysicalDevices");
    PFN_vkGetPhysicalDeviceProperties vkGetPhysicalDeviceProperties = dlsym(libvulkan, "vkGetPhysicalDeviceProperties");
    PFN_vkEnumerateDeviceExtensionProperties vkEnumerateDeviceExtensionProperties = dlsym(libvulkan, "vkEnumerateDeviceExtensionProperties");
    PFN_vkGetPhysicalDeviceProperties2 vkGetPhysicalDeviceProperties2 = dlsym(libvulkan, "vkGetPhysicalDeviceProperties2");
    PFN_vkGetPhysicalDeviceExternalBufferProperties vkGetPhysicalDeviceExternalBufferProperties =
        dlsym(libvulkan, "vkGetPhysicalDeviceExternalBufferProperties");
    if (!vkCreateInstance || !vkDestroyInstance || !vkEnumeratePhysicalDevices || !vkGetPhysicalDeviceProperties) goto done;


    VkApplicationInfo appInfo = {0};
    appInfo.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    appInfo.apiVersion = VK_MAKE_VERSION(1, 1, 0);
    VkInstanceCreateInfo createInfo = {0};
    createInfo.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    createInfo.pApplicationInfo = &appInfo;
    if (vkCreateInstance(&createInfo, NULL, &instance) != VK_SUCCESS) {
        instance = VK_NULL_HANDLE;
        goto done;
    }

    uint32_t deviceCount = 1;
    VkPhysicalDevice physicalDevice = VK_NULL_HANDLE;
    VkResult result = vkEnumeratePhysicalDevices(instance, &deviceCount, &physicalDevice);
    if ((result != VK_SUCCESS && result != VK_INCOMPLETE) || deviceCount == 0 || physicalDevice == VK_NULL_HANDLE) goto done;

    VkPhysicalDeviceProperties properties = {0};
    vkGetPhysicalDeviceProperties(physicalDevice, &properties);
    used += snprintf(out + used, sizeof(out) - used, "device '%s', api %d.%d.%d, driverVersion 0x%x, vendor 0x%x\n",
                     properties.deviceName, VK_VERSION_MAJOR(properties.apiVersion), VK_VERSION_MINOR(properties.apiVersion),
                     VK_VERSION_PATCH(properties.apiVersion), properties.driverVersion, properties.vendorID);
    if (properties.apiVersion < VK_MAKE_VERSION(1, 1, 0)) goto done;


    bool driverProperties = properties.apiVersion >= VK_MAKE_VERSION(1, 2, 0);
    uint32_t extensionCount = 0;
    if (!driverProperties && vkEnumerateDeviceExtensionProperties
        && vkEnumerateDeviceExtensionProperties(physicalDevice, NULL, &extensionCount, NULL) == VK_SUCCESS && extensionCount > 0) {
        extensions = calloc(extensionCount, sizeof(VkExtensionProperties));
        if (extensions && vkEnumerateDeviceExtensionProperties(physicalDevice, NULL, &extensionCount, extensions) == VK_SUCCESS) {
            for (uint32_t i = 0; i < extensionCount; i++) {
                if (strcmp(extensions[i].extensionName, "VK_KHR_driver_properties") == 0) driverProperties = true;
            }
        }
    }
    if (driverProperties && vkGetPhysicalDeviceProperties2 && used < (int)sizeof(out)) {
        VkPhysicalDeviceDriverProperties driver = {0};
        driver.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES;
        VkPhysicalDeviceProperties2 properties2 = {0};
        properties2.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2;
        properties2.pNext = &driver;
        vkGetPhysicalDeviceProperties2(physicalDevice, &properties2);
        used += snprintf(out + used, sizeof(out) - used, "driver id %d '%s' '%s'\n", driver.driverID, driver.driverName, driver.driverInfo);
    }
    if (vkGetPhysicalDeviceExternalBufferProperties) {
        used = appendExternalMemory(out, sizeof(out), used, vkGetPhysicalDeviceExternalBufferProperties, physicalDevice,
                                    VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT, "opaque fd");
        used = appendExternalMemory(out, sizeof(out), used, vkGetPhysicalDeviceExternalBufferProperties, physicalDevice,
                                    VK_EXTERNAL_MEMORY_HANDLE_TYPE_DMA_BUF_BIT_EXT, "dma-buf");
        used = appendExternalMemory(out, sizeof(out), used, vkGetPhysicalDeviceExternalBufferProperties, physicalDevice,
                                    VK_EXTERNAL_MEMORY_HANDLE_TYPE_ANDROID_HARDWARE_BUFFER_BIT_ANDROID, "ahardwarebuffer");
    }

done:
    if (used < (int)sizeof(out)) {

        used += snprintf(out + used, sizeof(out) - used, "dma heaps: system-uncached %s, system %s, ion %s\n",
                         access("/dev/dma_heap/system-uncached", R_OK) == 0 ? "yes" : "no",
                         access("/dev/dma_heap/system", R_OK) == 0 ? "yes" : "no",
                         access("/dev/ion", R_OK) == 0 ? "yes" : "no");
    }
    if (extensions) free(extensions);
    if (instance && vkDestroyInstance) vkDestroyInstance(instance, NULL);
    if (libvulkan) dlclose(libvulkan);
    return (*env)->NewStringUTF(env, out);
}

JNIEXPORT void JNICALL
Java_com_winlator_core_GPUHelper_setGlobalEGLContext(JNIEnv *env, jclass obj) {
    globalEGLContext = eglGetCurrentContext();
}

JNIEXPORT jlong JNICALL
Java_com_winlator_core_GPUHelper_createOffscreenEGLContext(JNIEnv *env, jclass obj,
                                                           jboolean sharedContext) {
    static const EGLint confAttribList[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_NONE,
    };
    static const EGLint ctxAttribList[] = {
        EGL_CONTEXT_CLIENT_VERSION, 3,
        EGL_NONE
    };
    EGLBoolean success;

    EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (!eglDisplay) return 0;

    EGLint major, minor;
    success = eglInitialize(eglDisplay, &major, &minor);
    if (!success) return 0;

    int numConfigs;
    EGLConfig eglConfig;
    success = eglChooseConfig(eglDisplay, confAttribList, &eglConfig, 1, &numConfigs);
    if (!success || numConfigs != 1) return 0;

    EGLContext eglContext = eglCreateContext(eglDisplay, eglConfig, sharedContext ? globalEGLContext : NULL, ctxAttribList);

    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, eglContext);
    return (jlong)eglContext;
}

JNIEXPORT void JNICALL
Java_com_winlator_core_GPUHelper_destroyOffscreenEGLContext(JNIEnv *env, jclass obj,
                                                            jlong contextPtr) {
    if (contextPtr == 0) return;
    EGLDisplay eglDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    eglMakeCurrent(eglDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(eglDisplay, (EGLContext)contextPtr);
}