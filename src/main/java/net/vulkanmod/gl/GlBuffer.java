package net.vulkanmod.gl;

/**
 * Compatibility alias: GlBuffer -> VkGlBuffer (renamed in 1.21.1 source line).
 */
public class GlBuffer extends VkGlBuffer {
    public GlBuffer() { super(0); }
    public GlBuffer(int id) { super(id); }
}
