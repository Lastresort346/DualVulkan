package net.vulkanmod.render.engine;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.memory.buffer.StagingBuffer;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * VulkanMod command encoder bridge for 1.21.1.
 * In 1.21.1, com.mojang.blaze3d.systems.CommandEncoder does not exist.
 * This class provides the texture-upload and render-pass management operations
 * that VulkanMod hooks into via mixins.
 */
public class VkCommandEncoder {
    private final VkGpuDevice device;
    boolean inRenderPass = false;

    public VkCommandEncoder(VkGpuDevice device) {
        this.device = device;
    }

    public VkGpuDevice getDevice() {
        return device;
    }

    /**
     * Write NativeImage data to a GL texture id (1.21.1 path).
     */
    public void writeToTexture(int glTextureId, NativeImage nativeImage) {
        var glTexture = VkGlTexture.getTexture(glTextureId);
        if (glTexture == null) return;
        VTextureSelector.setActiveTexture(0);
        VTextureSelector.bindTexture(glTexture.getVulkanImage());
        VTextureSelector.uploadSubTexture(0, 0,
                nativeImage.getWidth(), nativeImage.getHeight(),
                0, 0, 0, 0,
                nativeImage.getWidth(), MemoryUtil.memByteBuffer(net.vulkanmod.mixin.texture.image.MNativeImage.getPixels(nativeImage), nativeImage.getWidth() * nativeImage.getHeight() * 4));
    }

    /**
     * Write a byte buffer to a GL texture id at a specific mip/region.
     */
    public void writeToTexture(int glTextureId, ByteBuffer byteBuffer, NativeImage.Format format,
                                int level, int arrayLayer, int xOffset, int yOffset, int width, int height) {
        var glTexture = VkGlTexture.getTexture(glTextureId);
        if (glTexture == null) return;
        VTextureSelector.setActiveTexture(0);
        VTextureSelector.bindTexture(glTexture.getVulkanImage());
        VTextureSelector.uploadSubTexture(level, arrayLayer, width, height,
                xOffset, yOffset, 0, 0, width, byteBuffer);
    }

    /**
     * Hook for setup after a render pass ends.
     */
    public void trySetup(VkRenderPass renderPass) {
        // Render pass setup is handled by VkRenderPass itself
    }

    /**
     * Signals that no render pass is active.
     */
    public void endRenderPass() {
        this.inRenderPass = false;
    }
}
