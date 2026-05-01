package net.vulkanmod.gl;

/**
 * Compatibility alias: GlFramebuffer -> VkGlFramebuffer (renamed in 1.21.1 source line).
 */
public class GlFramebuffer extends VkGlFramebuffer {
    GlFramebuffer(int id) { super(id); }
}
