#ifndef VORTEK_SHADER_INSPECTOR_H  // Modified by D2Android (changes 2026-10-09 to 2026-10-30).
#define VORTEK_SHADER_INSPECTOR_H

#include "vortek.h"

typedef struct ShaderModule {
    VkShaderModule module;
    uint32_t* code;
    size_t codeSize;
} ShaderModule;

typedef struct ShaderInspector {
    bool checkClipDistance;
    bool convertFormatScaled;
    bool removeImageBoundCheck;
    bool removePointSizeExport;
    bool checkInOutVariablesSize;
} ShaderInspector;

extern ShaderInspector* ShaderInspector_create(VkContext* context, VkPhysicalDevice physicalDevice, VkPhysicalDeviceFeatures* supportedFeatures);
extern VkResult ShaderInspector_inspectShaderStages(ShaderInspector* shaderInspector, VkDevice device, VkPipelineShaderStageCreateInfo* stageInfos, uint32_t stageCount, const VkPipelineVertexInputStateCreateInfo* vertexInputState);
extern VkResult ShaderInspector_createModule(ShaderInspector* shaderInspector, VkDevice device, const uint32_t* code, size_t codeSize, ShaderModule** ppModule);
extern bool isFormatScaled(VkFormat format);

#endif