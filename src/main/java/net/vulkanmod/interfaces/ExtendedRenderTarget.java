package net.vulkanmod.interfaces;

import net.vulkanmod.vulkan.texture.VulkanImage;

/**
 * Interface mixed into Minecraft's RenderTarget to expose the Vulkan backing image.
 */
public interface ExtendedRenderTarget {
    VulkanImage getColorTarget();
    VulkanImage getDepthTarget();
    boolean isBound();
    net.vulkanmod.vulkan.framebuffer.RenderPass getRenderPass();
}
