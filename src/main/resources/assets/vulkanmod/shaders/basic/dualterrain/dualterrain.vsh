#version 460
// ============================================================
// DualVulkan Terrain + Shadow — Vertex Shader
// Runs on the PRIMARY GPU (RX 6500 XT).
// Outputs world-space position AND shadow-space UV so the
// fragment shader can look up the shadow map produced by
// the secondary GPU this (or last) frame.
// ============================================================

#include "light.glsl"
#include "fog.glsl"

// ---- UBOs ----
layout(binding = 0) uniform MainUBO {
    mat4 MVP;           // camera model-view-projection
};

layout(binding = 4) uniform ShadowUBO {
    mat4 ShadowMVP;     // sun model-view-projection (written by ShadowPass.java)
};

layout(push_constant) uniform PC {
    vec3 ModelOffset;
};

// ---- Samplers ----
layout(binding = 3) uniform sampler2D Sampler2;  // lightmap (same slot as terrain.vsh)

// ---- Vertex inputs (compressed terrain format) ----
layout(location = 0) in ivec4 Position;
layout(location = 1) in uvec2 UV0;
layout(location = 2) in uint  PackedColor;

// ---- Outputs ----
layout(location = 0) out vec4  vertexColor;
layout(location = 1) out vec2  texCoord0;
layout(location = 2) out float sphericalVertexDistance;
layout(location = 3) out float cylindricalVertexDistance;
layout(location = 4) out vec4  shadowCoord;   // position in shadow (sun) space

const float UV_INV      = 1.0 / 32768.0;
const vec3  POS_INV     = vec3(1.0 / 2048.0);

void main() {
    const vec3 baseOffset = vec3(
        bitfieldExtract(gl_InstanceIndex,       0, 8),
        bitfieldExtract(gl_InstanceIndex >>  8, 0, 8),
        bitfieldExtract(gl_InstanceIndex >> 16, 0, 8)
    );
    vec3 pos = fma(vec3(Position.xyz), POS_INV, ModelOffset + baseOffset);

    gl_Position = MVP * vec4(pos, 1.0);

    // Compute where this fragment lands in the shadow map
    shadowCoord = ShadowMVP * vec4(pos, 1.0);

    sphericalVertexDistance  = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);

    vec4 color = unpackUnorm4x8(PackedColor);
    vertexColor = color * sample_lightmap2(Sampler2, Position.a);

    texCoord0 = UV0 * UV_INV;
}
