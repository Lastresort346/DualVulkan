package net.beryl.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.vulkan.*;
import static org.lwjgl.vulkan.VK10.*;

public class BlitUtil {

    public static void blitFramebuffer(GraphicsPipeline pipeline, VulkanImage image) {
        VTextureSelector.bindTexture(image);
        blitFramebuffer(pipeline);
    }

    public static void blitFramebuffer(GraphicsPipeline pipeline) {
        Renderer.getInstance().bindGraphicsPipeline(pipeline);
        Renderer.getInstance().uploadAndBindUBOs(pipeline);
        vkCmdDraw(Renderer.getCommandBuffer(), 3, 1, 0, 0);
    }

    public static void setupBlitMVP() {
        Matrix4f proj = new Matrix4f().setOrtho(0f, 1f, 0f, 1f, 0f, 1f, true);
        VRenderSystem.applyProjectionMatrix(proj);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        VRenderSystem.applyModelViewMatrix(mv);
        VRenderSystem.calculateMVP();
        mv.popMatrix();
    }
}
