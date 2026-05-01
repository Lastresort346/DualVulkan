package net.beryl.render;

import net.beryl.render.util.BlitUtil;
import net.beryl.render.util.SUtil;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.pass.MainPass;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class ShaderMainPass implements MainPass {
    public static final ShaderMainPass PASS = new ShaderMainPass();

    public Framebuffer hdrFinalFramebuffer;
    public RenderPass  renderPass;
    public RenderPass  rebindPass;
    public RenderPass  finalPass;

    private GraphicsPipeline blitGammaTriShader;
    private GraphicsPipeline blitShader;
    private boolean earlyRenderPass = true;
    private Framebuffer currentFramebuffer;

    @Override
    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
        if (earlyRenderPass) {
            Framebuffer fb = hdrFinalFramebuffer;
            fb.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
            Renderer.getInstance().beginRenderPass(renderPass, fb);
            Renderer.clearAttachments(VK10.VK_IMAGE_ASPECT_COLOR_BIT | VK10.VK_IMAGE_ASPECT_DEPTH_BIT);
            Renderer.getInstance().setBoundFramebuffer(fb);
            VkViewport.Buffer vp = fb.viewport(stack);
            VK10.vkCmdSetViewport(commandBuffer, 0, vp);
            VkRect2D.Buffer sc = fb.scissor(stack);
            VK10.vkCmdSetScissor(commandBuffer, 0, sc);
            currentFramebuffer = fb;
        }
    }

    @Override
    public void end(VkCommandBuffer commandBuffer) {
        Renderer.getInstance().endRenderPass(commandBuffer);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            hdrFinalFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            SwapChain swapChain = Renderer.getInstance().getSwapChain();
            if (swapChain.hasImages()) {
                swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
                Renderer.getInstance().beginRenderPass(finalPass, swapChain);
                VRenderSystem.disableDepthTest();
                VRenderSystem.disableCull();
                VRenderSystem.disableBlend();
                BlitUtil.blitFramebuffer(blitGammaTriShader, hdrFinalFramebuffer.getColorAttachment());
                Renderer.getInstance().endRenderPass(commandBuffer);
                swapChain.getColorAttachment().transitionImageLayout(stack, commandBuffer, org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            }
        }
        int result = VK10.vkEndCommandBuffer(commandBuffer);
        if (result != VK10.VK_SUCCESS) throw new RuntimeException("Failed to record command buffer: " + result);
    }

    @Override
    public void rebindMainTarget() {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Framebuffer bound = Renderer.getInstance().getBoundFramebuffer();
        if (bound != currentFramebuffer) {
            Renderer.getInstance().endRenderPass(commandBuffer);
            Renderer.getInstance().beginRenderPass(rebindPass, currentFramebuffer);
        }
    }

    @Override
    public void bindAsTexture() {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        RenderPass bound = Renderer.getInstance().getBoundRenderPass();
        if (bound == renderPass) Renderer.getInstance().endRenderPass(commandBuffer);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            hdrFinalFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }
        VTextureSelector.bindTexture(hdrFinalFramebuffer.getColorAttachment());
    }

    public void beginFinalRenderPass(MemoryStack stack) {
        Renderer.getInstance().beginRenderPass(renderPass, hdrFinalFramebuffer);
        currentFramebuffer = hdrFinalFramebuffer;
    }

    public void init() { initShaders(); initFramebuffer(); }

    public void initShaders() {
        blitGammaTriShader = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "blit/blitGammaTri");
        blitShader         = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "blit/blit");
    }

    public void updateShaders() { cleanUpShaders(); initShaders(); }

    public void initFramebuffer() {
        if (hdrFinalFramebuffer != null) { hdrFinalFramebuffer.cleanUp(); renderPass.cleanUp(); }

        Framebuffer sc = Renderer.getInstance().getSwapChain();
        hdrFinalFramebuffer = new Framebuffer.Builder("beryl_hdr", sc.getWidth(), sc.getHeight(), 1, true)
                .setFormat(0x7902).build();

        RenderPass.Builder b = new RenderPass.Builder(hdrFinalFramebuffer);
        b.getColorAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR, VK10.VK_ATTACHMENT_STORE_OP_STORE);
        b.getDepthAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR, VK10.VK_ATTACHMENT_STORE_OP_STORE);
        renderPass = b.build();

        b = RenderPass.builder(sc);
        b.getColorAttachmentInfo().setFinalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        b.getColorAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR, VK10.VK_ATTACHMENT_STORE_OP_STORE);
        finalPass = b.build();

        b = RenderPass.builder(hdrFinalFramebuffer);
        b.getColorAttachmentInfo().setFinalLayout(VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        b.getColorAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_LOAD, VK10.VK_ATTACHMENT_STORE_OP_STORE);
        b.getDepthAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_LOAD, VK10.VK_ATTACHMENT_STORE_OP_STORE);
        rebindPass = b.build();
    }

    public void setEarlyRenderPass(boolean v)           { earlyRenderPass = v; }
    public void setCurrentFramebuffer(Framebuffer fb)   { currentFramebuffer = fb; }

    @Override public Framebuffer getMainFramebuffer() { return hdrFinalFramebuffer; }

    @Override
    public int getColorAttachmentGlId() {
        return hdrFinalFramebuffer != null && hdrFinalFramebuffer.getColorAttachment() != null
                ? hdrFinalFramebuffer.getColorAttachment().getImageView() != 0 ? -1 : -1
                : -1;
    }

    @Override public void onResize() { initFramebuffer(); }

    public void cleanUp() { hdrFinalFramebuffer.cleanUp(); renderPass.cleanUp(); cleanUpShaders(); }
    private void cleanUpShaders() { blitGammaTriShader.cleanUp(); blitShader.cleanUp(); }
}
