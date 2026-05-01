#version 450

#include "materials.glsl"

layout(binding = 2) uniform sampler2D Sampler0;
layout(binding = 4) uniform sampler2D ShadowMap;

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    mat4 ModelViewMat;
    mat4 LightSpaceMat;
    vec3 LightSpaceOffset;
};

layout(binding = 1) uniform UBO {
    vec4 ColorModulator;
    vec4 SkyColor;
    vec4 FogColor;
    vec3 UpVector;
    float FogStart;
    float FogEnd;
    float FogEnvStart;
    float FogEnvEnd;
    vec3 LightDir;
    vec3 LightColor;
    vec3 ViewPos;
    float LightIntensity;
    float LightVisibility;
    float NightFactor;
    float AmbientLightFactor;
    float MinAmbientLight;
    float FogFactor;
    float ShadowTexelSize;
    float ShadowBias;
    float ShadowDistortion;
};

layout(location = 0) in float vertexDistance;
layout(location = 1) in vec4 vertexColor;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in vec4 posLightSpace;
layout(location = 5) in vec3 fragPos;
layout(location = 6) in vec3 light;
layout(location = 7) in flat Material material;

layout(location = 0) out vec4 fragColor;

#include "fog2.glsl"
#include "lighting.glsl"
#include "shadow.glsl"

void main() {
    vec4 texColor = texture(Sampler0, texCoord0) * vertexColor;

    if (texColor.a < 0.5) {
        discard;
    }

    vec4 color;

    float shadow = ShadowCalculation(ShadowMap, posLightSpace, ShadowTexelSize, ShadowBias);

    vec3 viewDir = normalize(-fragPos);
    vec3 N = normal;
    vec3 V = viewDir;

    vec3 F0 = material.F0;
    float roughness = material.roughness;
    float metallic = material.metallic;

    vec3 albedo = texColor.rgb;

    vec3 radiance = LightColor;

    // Gradually reduce lighting when its low on the horizon
    radiance *= saturate((dot(UpVector, LightDir) - 0.04) * 50); // 1 / 0.02

//    vec3 color1 = LightingGGX(viewDir, normal, LightDir, albedo, radiance, F0, roughness, metallic);
//    vec3 color1 = LightingSphereGGX(viewDir, normal, LightDir, albedo, radiance, F0, roughness, metallic);
    vec3 color1 = LightingSphereGGX2(viewDir, normal, LightDir, albedo, radiance, F0, roughness, metallic, material.lightingType);

    color.rgb = (color1 * shadow * light.z) + albedo * (0.3 * light.y + light.x * vec3(1.0, 0.7, 0.5));
    float NdotU = max(dot(N, UpVector), 0.0);
    NdotU = material.lightingType == 0 ? NdotU : 0.8;
    color.rgb += radiance * 0.05 * NdotU * albedo * shadow * light.z;

    if (material.lightEmission > 0.0) {
        color.rgb += max((texColor.rgb - vec3(0.05)), vec3(0.0)) * material.lightEmission;
    }

    color.a = texColor.a;

    atmospheric_fog(color, fragPos, vertexDistance, FogFactor);

    // View distance fog
    if (vertexDistance > 0.8 * FogEnd) {
        //TODO precompute and optimize
        vec3 fragDir = normalize(fragPos);
        float RoUp = dot(fragDir, UpVector);
        RoUp = abs(RoUp);

        float SoUp = max(dot(UpVector, LightDir), 0.0);
        float RdotL = max(dot(fragDir, LightDir), 0.0);
        vec4 fogColor = vec4(getSkyColor(RdotL, RoUp, SoUp).rgb, 1.0);

        color = fog(color, vertexDistance, FogEnd, fogColor);
    }

//    color.rgb += volLighting(vec3(0.0), fragPos, normalize(fragPos));

    fragColor = color;
}