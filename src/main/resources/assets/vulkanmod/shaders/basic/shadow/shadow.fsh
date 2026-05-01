#version 450
// ============================================================
// DualVulkan Shadow Pass — Fragment
// Depth-only: no colour output. The depth buffer IS the shadow map.
// Alpha-tested geometry (leaves, glass) could do a discard here;
// for now we keep solid geometry only (simplest correct result).
// ============================================================

// No colour outputs — depth is written automatically by the hardware.
// layout(location = 0) out float depth;  // NOT needed; Vulkan writes gl_FragDepth implicitly

void main() {
    // Nothing — depth attachment is written by the fixed-function rasterizer.
}
