#version 450
// ============================================================
// DualVulkan Terrain + Shadow — Fragment Shader
// Runs on the PRIMARY GPU (RX 6500 XT).
// Samples the shadow map produced by the SECONDARY GPU (RX 550)
// each frame to apply real sun shadows to terrain.
// ============================================================

#include "light.glsl"
#include "fog.glsl"

// ---- Samplers ----
layout(binding = 2) uniform sampler2D   Sampler0;     // block atlas
layout(binding = 5) uniform sampler2DShadow ShadowMap; // depth from secondary GPU

// ---- UBO (fog + alpha cutout — same as terrain.fsh) ----
layout(binding = 1) uniform FogUBO {
    vec4  FogColor;
    float FogEnvironmentalStart;
    float FogEnvironmentalEnd;
    float FogRenderDistanceStart;
    float FogRenderDistanceEnd;
    float FogSkyEnd;
    float FogCloudsEnd;
    float AlphaCutout;
};

// ---- Shadow-specific UBO ----
layout(binding = 6) uniform ShadowParamsUBO {
    float ShadowBias;       // depth bias to reduce acne (typically 0.002)
    float ShadowDarkness;   // 0.0 = no shadowing visible, 1.0 = full black shadows
};

// ---- Inputs from vertex shader ----
layout(location = 0) in vec4  vertexColor;
layout(location = 1) in vec2  texCoord0;
layout(location = 2) in float sphericalVertexDistance;
layout(location = 3) in float cylindricalVertexDistance;
layout(location = 4) in vec4  shadowCoord;

layout(location = 0) out vec4 fragColor;

// ---- PCF shadow lookup (3x3, 9 taps) ----
// sampler2DShadow automatically does depth comparison; textureProj returns [0,1].
float sampleShadow(vec4 sc) {
    // Perspective divide into NDC, then remap depth to [0,1]
    vec3 projCoords = sc.xyz / sc.w;

    // Vulkan NDC: XY in [-1,+1], Z in [0,1].  Convert XY to UV [0,1].
    vec2 uv = projCoords.xy * 0.5 + 0.5;

    // Outside the shadow frustum → fully lit
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0 || projCoords.z > 1.0)
        return 1.0;

    float compareDepth = projCoords.z - ShadowBias;

    // 3×3 PCF — textureProj with sampler2DShadow does hw comparison
    ivec2 texSize = textureSize(ShadowMap, 0);
    vec2  texelSize = 1.0 / vec2(texSize);

    float shadow = 0.0;
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            vec4 shadowSampleCoord = vec4(
                uv + vec2(x, y) * texelSize,
                compareDepth,
                1.0
            );
            shadow += texture(ShadowMap, shadowSampleCoord.xyz);
        }
    }
    shadow /= 9.0;
    return shadow;
}

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;

    if (color.a < AlphaCutout) discard;

    // Shadow factor: 1.0 = fully lit, 0.0 = fully in shadow
    float shadowFactor = sampleShadow(shadowCoord);

    // Blend: darken shadowed areas but keep ambient so it's not pitch black
    float ambientMin  = 1.0 - ShadowDarkness;
    float lightFactor = mix(ambientMin, 1.0, shadowFactor);
    color.rgb *= lightFactor;

    fragColor = apply_fog(color,
        sphericalVertexDistance, cylindricalVertexDistance,
        FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd,
        FogColor);
}
