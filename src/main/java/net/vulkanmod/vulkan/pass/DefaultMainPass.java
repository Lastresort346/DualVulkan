package net.vulkanmod.vulkan.pass;

// RenderSystem.getDevice() not available in 1.21.1
import net.vulkanmod.render.engine.VkTextureView;
import net.vulkanmod.render.engine.VkGpuDevice;
import net.vulkanmod.render.engine.VkGpuTexture;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkRect2D;

import java.util.function.IntSupplier;

import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

public class DefaultMainPass implements MainPass {

    public static DefaultMainPass create() {
        return new DefaultMainPass();
    }

    private Framebuffer mainFramebuffer;

    private RenderPass mainRenderPass;
    private RenderPass auxRenderPass;

    private VkGpuTexture[] colorAttachmentTextures;
    private VkTextureView[] colorAttachmentTextureViews;
    IntSupplier imageIdxSupplier;
    private VkGpuTexture depthAttachmentTexture;

    DefaultMainPass() {
        createResources();
    }

    private void createResources() {
        if (this.mainFramebuffer != null) {
            if (this.mainFramebuffer != Renderer.getInstance()
                                                .getSwapChain()) {
                this.mainFramebuffer.cleanUp(true);
            }

            this.mainRenderPass.cleanUp();
            this.auxRenderPass.cleanUp();
        }

        Framebuffer framebuffer;
        if (Renderer.getInstance().getSwapChain().hasImages()) {
            framebuffer = Renderer.getInstance().getSwapChain();
        }
        else {
            framebuffer = Framebuffer.builder(10, 10, 1, true)
                                     .build();
        }

        this.mainFramebuffer = framebuffer;

        createRenderPasses();
        createAttachmentTextures();
    }

    private void createRenderPasses() {
        RenderPass.Builder builder = RenderPass.builder(this.mainFramebuffer);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_DONT_CARE, VK_ATTACHMENT_STORE_OP_STORE);

        this.mainRenderPass = builder.build();

        // Create an auxiliary RenderPass needed in case of main target rebinding
        builder = RenderPass.builder(this.mainFramebuffer);
        builder.getColorAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_STORE_OP_STORE);
        builder.getColorAttachmentInfo().setFinalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        this.auxRenderPass = builder.build();
    }

    @Override
    public void begin(VkCommandBuffer commandBuffer, MemoryStack stack) {
        Framebuffer framebuffer = this.mainFramebuffer;

        VulkanImage colorAttachment = framebuffer.getColorAttachment();
        colorAttachment.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        Renderer.getInstance().beginRenderPass(this.mainRenderPass, framebuffer);

        Renderer.setViewport(0, 0, framebuffer.getWidth(), framebuffer.getHeight(), stack);

        VkRect2D.Buffer pScissor = framebuffer.scissor(stack);
        vkCmdSetScissor(commandBuffer, 0, pScissor);
    }

    @Override
    public void end(VkCommandBuffer commandBuffer) {
        Renderer.getInstance().endRenderPass(commandBuffer);

        if (this.mainFramebuffer == Renderer.getInstance().getSwapChain()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
            }
        }

        int result = vkEndCommandBuffer(commandBuffer);
        if (result != VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer:" + result);
        }
    }

    @Override
    public void cleanUp() {
        this.mainRenderPass.cleanUp();
        this.auxRenderPass.cleanUp();
    }

    @Override
    public void onResize() {
        createResources();
    }

    public void rebindMainTarget() {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        // Do not rebind if the framebuffer is already bound
        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        if (boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            return;

        Renderer.getInstance().endRenderPass(commandBuffer);
        Renderer.getInstance().beginRenderPass(this.auxRenderPass, this.mainFramebuffer);
    }

    @Override
    public void bindAsTexture() {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        // Check if render pass is using the framebuffer
        RenderPass boundRenderPass = Renderer.getInstance().getBoundRenderPass();
        if (boundRenderPass == this.mainRenderPass || boundRenderPass == this.auxRenderPass)
            Renderer.getInstance().endRenderPass(commandBuffer);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.mainFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
        }

        VTextureSelector.bindTexture(this.mainFramebuffer.getColorAttachment());
    }

    @Override
    public Framebuffer getMainFramebuffer() {
        return mainFramebuffer;
    }

    @Override
    public VkGpuTexture getColorAttachment() {
        if (colorAttachmentTextures == null) return null;
        return this.colorAttachmentTextures[this.imageIdxSupplier.getAsInt()];
    }

    @Override
    public VkTextureView getColorAttachmentView() {
        if (colorAttachmentTextureViews == null) return null;
        return this.colorAttachmentTextureViews[this.imageIdxSupplier.getAsInt()];
    }

    @Override
    public VkGpuTexture getDepthAttachment() {
        return depthAttachmentTexture;
    }

    @Override
    public int getColorAttachmentGlId() {
        return (int) this.mainFramebuffer.getColorAttachment().getId();
    }

    private void createAttachmentTextures() {
        SwapChain swapChain = Renderer.getInstance().getSwapChain();
        if (this.mainFramebuffer == swapChain) {
            var swapChainImages = swapChain.getImages();
            int imageCount = swapChainImages.size();
            this.colorAttachmentTextures = new VkGpuTexture[imageCount];
            this.colorAttachmentTextureViews = new VkTextureView[imageCount];

            for (int i = 0; i < imageCount; ++i) {
                VkGpuTexture attachmentTexture = gpuTextureFromVulkanImage(swapChainImages.get(i));
                VkTextureView attachmentTextureView = new VkTextureView(attachmentTexture, 0, 1);
                this.colorAttachmentTextures[i] = attachmentTexture;
                this.colorAttachmentTextureViews[i] = attachmentTextureView;
            }
            this.imageIdxSupplier = Renderer::getCurrentImage;
        } else {
            this.colorAttachmentTextures = new VkGpuTexture[1];
            this.colorAttachmentTextureViews = new VkTextureView[1];

            VkGpuTexture attachmentTexture = gpuTextureFromVulkanImage(this.mainFramebuffer.getColorAttachment());
            VkTextureView attachmentTextureView = new VkTextureView(attachmentTexture, 0, 1);
            this.colorAttachmentTextures[0] = attachmentTexture;
            this.colorAttachmentTextureViews[0] = attachmentTextureView;
            this.imageIdxSupplier = () -> 0;
        }

        if (this.mainFramebuffer.getDepthAttachment() != null) {
            this.depthAttachmentTexture = gpuTextureFromVulkanImage(this.mainFramebuffer.getDepthAttachment());
        }
    }

    private static VkGpuTexture gpuTextureFromVulkanImage(VulkanImage vulkanImage) {
        if (vulkanImage == null) return null;
        // Wrap the VulkanImage in a VkGpuTexture with no GL texture backing
        return new VkGpuTexture(-1, null, vulkanImage.mipLevels) {
            @Override
            public VulkanImage getVulkanImage() {
                return vulkanImage;
            }
        };
    }
}
