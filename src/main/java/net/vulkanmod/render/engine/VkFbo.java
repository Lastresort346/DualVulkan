package net.vulkanmod.render.engine;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.util.FastColor;
import net.vulkanmod.gl.VkGlFramebuffer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL33;

public class VkFbo {
    final int glId;
    final VkGpuTexture colorAttachment;
    final VkGpuTexture depthAttachment;

    protected VkFbo(VkGpuTexture colorAttachment, VkGpuTexture depthAttachment) {
        this.glId = GlStateManager.glGenFramebuffers();
        this.colorAttachment = colorAttachment;
        this.depthAttachment = depthAttachment;

        // Direct access
        VkGlFramebuffer fbo = VkGlFramebuffer.getFramebuffer(this.glId);

        fbo.setAttachmentTexture(GL33.GL_COLOR_ATTACHMENT0, colorAttachment.id);
        if (depthAttachment != null) {
            fbo.setAttachmentTexture(GL33.GL_DEPTH_ATTACHMENT, depthAttachment.id);
        }
    }

    public void bind() {
        VkGlFramebuffer.bindFramebuffer(GL33.GL_FRAMEBUFFER, this.glId);
        clearAttachments();
    }

    protected void clearAttachments() {
        int clear = 0;
        float clearDepth;
        int clearColor;

        if (colorAttachment.needsClear()) {
            clear |= 0x4000;
            clearColor = colorAttachment.clearColor;

            float a = FastColor.ARGB32.alpha(clearColor) / 255.0f;
            float r = FastColor.ARGB32.red(clearColor) / 255.0f;
            float g = FastColor.ARGB32.green(clearColor) / 255.0f;
            float b = FastColor.ARGB32.blue(clearColor) / 255.0f;
            VRenderSystem.setClearColor(r, g, b, a);

            colorAttachment.needsClear = false;
        }

        if (depthAttachment != null && depthAttachment.needsClear()) {
            clear |= 0x100;
            clearDepth = depthAttachment.depthClearValue;

            VRenderSystem.clearDepth(clearDepth);

            depthAttachment.needsClear = false;
        }

        if (clear != 0) {
            Renderer.clearAttachments(clear);
        }
    }

    protected void close() {
        VkGlFramebuffer.deleteFramebuffer(this.glId);
    }

    public boolean needsClear() {
        return this.colorAttachment.needsClear() || (this.depthAttachment != null && this.depthAttachment.needsClear());
    }
}
