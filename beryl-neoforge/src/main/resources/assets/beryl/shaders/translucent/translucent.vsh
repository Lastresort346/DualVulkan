#version 450

#include "materials.glsl"

layout(binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    mat4 ProjMat;
    mat4 ModelViewMat;
    mat4 LightSpaceMat;
    vec3 LightSpaceOffset;
    vec3 ViewPos;
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
    vec3 ViewPos2;
    float GameTime;
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

layout(push_constant) uniform pushConstant {
    vec3 ChunkOffset;
};

layout(binding = 3) uniform sampler2D Sampler2;

#define COMPRESSED_VERTEX

#ifdef COMPRESSED_VERTEX
layout (location = 0) in ivec4 Position;
layout (location = 1) in uvec2 UV0;
layout (location = 2) in uint PackedColor;
layout (location = 3) in vec3 Normal;
layout (location = 4) in int BlockId;
#else
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in vec3 Normal;
layout(location = 5) in int BlockId;
#endif

const float UV_INV = 1.0 / 32768.0;
const vec3 POSITION_INV = vec3(1.0 / 2048.0);
const vec3 POSITION_OFFSET = vec3(4.0);

#include "light.glsl"
#include "shadow.glsl"

vec3 getVertexPosition() {
    const vec3 baseOffset = bitfieldExtract(ivec3(gl_InstanceIndex) >> ivec3(0, 16, 8), 0, 8);

    #ifdef COMPRESSED_VERTEX
        return fma(Position.xyz, POSITION_INV, ChunkOffset + baseOffset);
    #else
        return Position.xyz + baseOffset;
    #endif
}

layout(location = 0) out float vertexDistance;
layout(location = 1) out vec4 vertexColor;
layout(location = 2) out vec3 normal;
layout(location = 3) out vec2 texCoord0;
layout(location = 4) out vec4 posLightSpace;
layout(location = 5) out vec3 fragPos;
layout(location = 6) out vec3 light;
layout(location = 7) out flat Material material;

const float LIGHT_CONV = 1.0 / 256.0;

void applyWaving(inout vec4 position) {
    //    const float amplitude = 0.06;
    //    vec4 position = vec4(Position.xyz + baseOffset + ChunkOffset + ViewPos, 1.0);tude * 0.5;
    //    position.y += amplitude * sin(2000 * GameTime + 0.15 * position.x + 0.1 * position.z) - ampli
    //
    //    vec3 normalOffset = vec3(amplitude * 0.15 * cos(2000 * GameTime + 0.15 * position.x + 0.1 * position.z),
    //                            0.0,
    //                            amplitude * 0.1 * cos(2000 * GameTime + 0.15 * position.x + 0.1 * position.z));
    //
    //    position.xyz -= ViewPos;
}

void main() {
    // Compressed vertex
    const vec4 Color = unpackUnorm4x8(PackedColor);
    //    vertexColor = Color * sample_lightmap2(Sampler2, Position.a);
    const uint uv = Position.a;
    const ivec2 UV2 = ivec2(bitfieldExtract(uv, 0, 8), bitfieldExtract(uv, 8, 8));
    texCoord0 = UV0 * UV_INV;

    const vec4 position = vec4(getVertexPosition(), 1.0);

    gl_Position = MVP * position;

    //    posLightSpace = LightSpaceMat * vec4(Position + ChunkOffset - LightSpaceOffset, 1.0);
    posLightSpace = LightSpaceMat * position;
    posLightSpace.xyz = distortShadowClipPos(posLightSpace.xyz);

    fragPos = (ModelViewMat * position).xyz;
//    fragPosWS = position.xyz + ViewPos;

    vertexDistance = length(fragPos);
    vertexColor.rgb = pow(Color.rgb, vec3(2.2));
    vertexColor.a = Color.a;

    float lightY = UV2.y * LIGHT_CONV;
    lightY = lightY * lightY;

    float lightZ = max((lightY - 0.5) / (1.0 - 0.5), 0.0);
    lightY *= AmbientLightFactor;
    lightY = max(lightY - NightFactor, MinAmbientLight);

    float lightX = UV2.x * LIGHT_CONV;
    lightX = lightX * lightX * (1 - lightY);

    light = vec3(lightX * 2, lightY, lightZ);

    // Waving normal update
//    normal = normalize(Normal + normalOffset);
//
//    normal = (ModelViewMat * vec4(normal, 0.0)).xyz;
    normal = (ModelViewMat * vec4(Normal, 0.0)).xyz;

    material = getMaterial(BlockId);
}