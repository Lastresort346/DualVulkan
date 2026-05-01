package net.beryl.render;

import net.beryl.render.util.BlitUtil;
import net.beryl.render.util.SUtil;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.lwjgl.vulkan.VK10.*;

public class Bloom {
    Framebuffer[] framebuffers;
    RenderPass[] downsamplePasses;
    RenderPass[] upsamplePasses;
    RenderPass finalBlendPass;
    private final GraphicsPipeline downsampleShader;
    private final GraphicsPipeline blendShader;
    private final GraphicsPipeline finalBlendShader;
    private int width;
    private int height;
    private final int mipCount;

    public Bloom(int mipCount) {
        this.mipCount = mipCount;
        downsampleShader  = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "bloom/downsample/downsample");
        blendShader       = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "bloom/blend/blend");
        finalBlendShader  = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "bloom/finalBlend/finalBlend");
    }

    public void createFramebuffers(int width, int height, Framebuffer target) {
        this.width = width; this.height = height;
        framebuffers    = new Framebuffer[mipCount];
        downsamplePasses = new RenderPass[mipCount];
        upsamplePasses  = new RenderPass[mipCount - 1];

        for (int i = 0; i < mipCount; i++) {
            framebuffers[i] = new Framebuffer.Builder(width >> (i+1), height >> (i+1), 1, false)
                    .setFormat(VK10.VK_FORMAT_R16G16B16A16_SFLOAT).setLinearFiltering(true).build();
            downsamplePasses[i] = new RenderPass.Builder(framebuffers[i]).build();
        }
        for (int i = mipCount - 2; i >= 0; i--) {
            upsamplePasses[i] = new RenderPass.Builder(framebuffers[i]).setLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_LOAD).build();
        }
        finalBlendPass = new RenderPass.Builder(target).build();
    }

    public void render(VkCommandBuffer commandBuffer, Framebuffer in) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            renderDownsamples(stack, commandBuffer, in);
            renderUpsamples(stack, commandBuffer);
            Renderer.setViewport(0, 0, width, height);
        }
    }

    private void renderDownsamples(MemoryStack stack, VkCommandBuffer commandBuffer, Framebuffer in) {
        in.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VTextureSelector.bindTexture(in.getColorAttachment());
        doDownsamplePass(stack, commandBuffer, 0);
        for (int i = 1; i < framebuffers.length; i++) {
            Framebuffer prev = framebuffers[i - 1];
            prev.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VTextureSelector.bindTexture(prev.getColorAttachment());
            doDownsamplePass(stack, commandBuffer, i);
        }
    }

    private void doDownsamplePass(MemoryStack stack, VkCommandBuffer commandBuffer, int i) {
        Framebuffer fb = framebuffers[i];
        fb.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        fb.beginRenderPass(commandBuffer, downsamplePasses[i], stack);
        Renderer.setViewport(0, 0, fb.getWidth(), fb.getHeight());
        BlitUtil.blitFramebuffer(downsampleShader);
        Renderer.getInstance().endRenderPass(commandBuffer);
    }

    private void renderUpsamples(MemoryStack stack, VkCommandBuffer commandBuffer) {
        VRenderSystem.enableBlend();
        VRenderSystem.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        for (int i = framebuffers.length - 2; i >= 0; i--) {
            Framebuffer prev = framebuffers[i + 1];
            prev.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VTextureSelector.bindTexture(prev.getColorAttachment());
            doUpsamplePass(stack, commandBuffer, i);
        }
        VRenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        framebuffers[0].getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
    }

    private void doUpsamplePass(MemoryStack stack, VkCommandBuffer commandBuffer, int i) {
        Framebuffer fb = framebuffers[i];
        fb.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        fb.beginRenderPass(commandBuffer, upsamplePasses[i], stack);
        Renderer.setViewport(0, 0, fb.getWidth(), fb.getHeight());
        BlitUtil.blitFramebuffer(blendShader);
        Renderer.getInstance().endRenderPass(commandBuffer);
    }

    public void blendFramebuffer(MemoryStack stack, VkCommandBuffer commandBuffer, VulkanImage image, Framebuffer target) {
        image.transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        VTextureSelector.bindTexture(3, image);
        VulkanImage bloomImage = framebuffers[0].getColorAttachment();
        VTextureSelector.bindTexture(bloomImage);
        VRenderSystem.disableBlend();

        target.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        Renderer.getInstance().beginRenderPass(finalBlendPass, target);
        Renderer.setViewport(0, 0, target.getWidth(), target.getHeight());
        BlitUtil.blitFramebuffer(finalBlendShader);
        Renderer.getInstance().endRenderPass(commandBuffer);
    }

    public void cleanUp() {
        for (Framebuffer f : framebuffers) f.cleanUp();
        for (RenderPass p : downsamplePasses) p.cleanUp();
        for (RenderPass p : upsamplePasses) p.cleanUp();
        finalBlendPass.cleanUp();
        downsampleShader.cleanUp(); blendShader.cleanUp(); finalBlendShader.cleanUp();
    }
}
