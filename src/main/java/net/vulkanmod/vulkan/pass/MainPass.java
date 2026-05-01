package net.vulkanmod.vulkan.pass;

import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.render.engine.VkTextureView;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface MainPass {

    void begin(VkCommandBuffer commandBuffer, MemoryStack stack);

    void end(VkCommandBuffer commandBuffer);

    void cleanUp();

    void onResize();

    default void mainTargetBindWrite() {}

    default void mainTargetUnbindWrite() {}

    default void rebindMainTarget() {}

    default void bindAsTexture() {}

    default Framebuffer getMainFramebuffer() {
        return null;
    }

    default VkGpuTexture getColorAttachment() {
        return null;
    }

    default VkTextureView getColorAttachmentView() {
        return null;
    }

    default VkGpuTexture getDepthAttachment() {
        return null;
    }

    int getColorAttachmentGlId();
}
