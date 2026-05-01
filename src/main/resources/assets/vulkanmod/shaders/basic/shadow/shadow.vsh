#version 460
// ============================================================
// DualVulkan Shadow Pass — runs on SECONDARY GPU (RX 550)
// Renders the scene from the sun's perspective to build a
// shadow map that the primary GPU samples each frame.
// ============================================================

layout(binding = 0) uniform ShadowUBO {
    mat4 ShadowMVP;   // sun-space view-projection
    vec3 ModelOffset; // chunk world offset (push constant mirror)
};

// Compressed terrain vertex (same layout as terrain.vsh)
layout(location = 0) in ivec4 Position;
layout(location = 1) in uvec2 UV0;       // unused in depth pass, keep for layout compat
layout(location = 2) in uint  PackedColor; // unused

layout(push_constant) uniform PC {
    vec3 ChunkOffset;
};

const vec3 POSITION_INV = vec3(1.0 / 2048.0);

void main() {
    // Decode compressed position (same as terrain.vsh)
    const vec3 baseOffset = vec3(
        bitfieldExtract(gl_InstanceIndex,       0, 8),
        bitfieldExtract(gl_InstanceIndex >> 8,  0, 8),
        bitfieldExtract(gl_InstanceIndex >> 16, 0, 8)
    );
    vec3 worldPos = fma(vec3(Position.xyz), POSITION_INV, ChunkOffset + baseOffset);

    gl_Position = ShadowMVP * vec4(worldPos, 1.0);
}
