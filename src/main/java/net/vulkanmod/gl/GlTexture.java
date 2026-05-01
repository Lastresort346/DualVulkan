package net.vulkanmod.gl;

/**
 * Compatibility alias: GlTexture -> VkGlTexture (renamed in 1.21.1 source line).
 * All usages delegate to VkGlTexture static methods.
 */
public class GlTexture extends VkGlTexture {
    public GlTexture() { super(0); }
    public GlTexture(int id) { super(id); }
}
