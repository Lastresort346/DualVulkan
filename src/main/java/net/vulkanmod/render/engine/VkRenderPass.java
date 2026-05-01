package net.vulkanmod.render.engine;

import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.1 version: com.mojang.blaze3d.systems.RenderPass does not exist.
 * This is a minimal Vulkan render pass wrapper for VulkanMod's 1.21.1 path.
 */
public class VkRenderPass implements AutoCloseable {
    private final VkCommandEncoder encoder;
    private boolean closed = false;
    @Nullable private EGlProgram activePipeline;

    public VkRenderPass(VkCommandEncoder encoder, boolean hasDepthTexture) {
        this.encoder = encoder;
        encoder.inRenderPass = true;
    }

    public void setPipeline(EGlProgram program) {
        this.activePipeline = program;
    }

    public void enableScissor(int x, int y, int width, int height) {
        // Renderer.getDrawer().setScissor(x, y, width, height); // Method doesn't exist in 1.21.1
    }

    public void disableScissor() {
        // Renderer.getDrawer().disableScissor(); // Method doesn't exist in 1.21.1
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            encoder.endRenderPass();
        }
    }

    public boolean isClosed() { return closed; }
}
