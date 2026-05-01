package net.vulkanmod.vulkan.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.util.FastColor;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.vulkan.Renderer;
import org.lwjgl.vulkan.VK10;

import java.util.function.Consumer;

/**
 * 1.21.1 version of ScreenshotUtil.
 * RenderSystem.getDevice()/GpuBuffer/CommandEncoder/TextureFormat removed.
 */
public abstract class ScreenshotUtil {

    public static void takeScreenshot(RenderTarget renderTarget, int mipLevel, Consumer<NativeImage> consumer) {
        int width  = renderTarget.width;
        int height = renderTarget.height;

        Renderer.getInstance().flushCmds();

        VkGpuTexture colorAttachment = (VkGpuTexture) Renderer.getInstance()
                .getMainPass().getColorAttachment();

        if (colorAttachment == null || colorAttachment.getVulkanImage() == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        }

        boolean isBgraFormat = colorAttachment.getVulkanImage().format == VK10.VK_FORMAT_B8G8R8A8_UNORM;

        NativeImage nativeImage = new NativeImage(width, height, false);
        // Read from the Vulkan image directly via readback
        // TODO: implement GPU readback; for now fill with a placeholder
        int size = mipLevel * mipLevel;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                nativeImage.setPixelRGBA(x, y, 0xFF000000);
            }
        }
        consumer.accept(nativeImage);
    }
}
