package net.vulkanmod.vulkan.fog;

/**
 * VulkanMod fog state holder for 1.21.1 compatibility.
 * In 1.21.1, FogRenderer exposes fog parameters as public static fields.
 * This class mirrors those fields so Uniforms/VRenderSystem can access them uniformly.
 */
public class FogData {
    public float environmentalStart = 0f;
    public float environmentalEnd   = 1024f;
    public float renderDistanceStart = 0f;
    public float renderDistanceEnd   = 1024f;
    public float skyEnd              = 1024f;
    public float cloudEnd            = 1024f;

    public static final FogData DEFAULT = new FogData();
}
