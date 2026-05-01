package net.vulkanmod.gl;

/**
 * Compatibility alias: GlRenderbuffer -> VkGlRenderbuffer (renamed in 1.21.1 source line).
 */
public class GlRenderbuffer extends VkGlRenderbuffer {
    public GlRenderbuffer() { super(0); }
    public GlRenderbuffer(int id) { super(id); }
}
