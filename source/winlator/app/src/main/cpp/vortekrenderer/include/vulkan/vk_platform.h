// Modified by D2Android (changes 2026-10-09 to 2026-10-30).


/*
** Copyright 2014-2024 The Khronos Group Inc.
**
** SPDX-License-Identifier: Apache-2.0
*/


#ifndef VK_PLATFORM_H_
#define VK_PLATFORM_H_

#ifdef __cplusplus
extern "C"
{
#endif





















#if defined(_WIN32)

    #define VKAPI_ATTR
    #define VKAPI_CALL __stdcall
    #define VKAPI_PTR  VKAPI_CALL
#elif defined(__ANDROID__) && defined(__ARM_ARCH) && __ARM_ARCH < 7
    #error "Vulkan is not supported for the 'armeabi' NDK ABI"
#elif defined(__ANDROID__) && defined(__ARM_ARCH) && __ARM_ARCH >= 7 && defined(__ARM_32BIT_STATE)




    #define VKAPI_ATTR __attribute__((pcs("aapcs-vfp")))
    #define VKAPI_CALL
    #define VKAPI_PTR  VKAPI_ATTR
#else

    #define VKAPI_ATTR
    #define VKAPI_CALL
    #define VKAPI_PTR
#endif

#if !defined(VK_NO_STDDEF_H)
    #include <stddef.h>
#endif

#if !defined(VK_NO_STDINT_H)
    #if defined(_MSC_VER) && (_MSC_VER < 1600)
        typedef signed   __int8  int8_t;
        typedef unsigned __int8  uint8_t;
        typedef signed   __int16 int16_t;
        typedef unsigned __int16 uint16_t;
        typedef signed   __int32 int32_t;
        typedef unsigned __int32 uint32_t;
        typedef signed   __int64 int64_t;
        typedef unsigned __int64 uint64_t;
    #else
        #include <stdint.h>
    #endif
#endif

#ifdef __cplusplus
}
#endif

#endif
