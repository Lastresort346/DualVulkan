package net.vulkanmod.interfaces;

import net.vulkanmod.vulkan.texture.VulkanImage;

/**
 * Interface mixed into Minecraft's AbstractTexture to expose the backing VulkanImage.
 */
public interface VAbstractTextureI {
    VulkanImage getVulkanImage();
    void setVulkanImage(VulkanImage image);
    void bindTexture();
}
