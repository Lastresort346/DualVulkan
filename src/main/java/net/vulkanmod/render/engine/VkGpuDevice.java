package net.vulkanmod.render.engine;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.vulkanmod.Initializer;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.interfaces.shader.ExtendedRenderPipeline;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.shader.ShaderLoadUtil;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.converter.GLSLParser;
import net.vulkanmod.vulkan.shader.converter.Lexer;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.vulkan.VK10;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * VulkanMod GPU device bridge for 1.21.1.
 * In 1.21.1, com.mojang.blaze3d.systems.GpuDevice does not exist.
 * This class manages Vulkan pipeline compilation and device queries directly.
 */
@SuppressWarnings("NullableProbludes")
public class VkGpuDevice {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static VkGpuDevice instance;

    private final VkCommandEncoder encoder;
    private final VkDebugLabel debugLabels;
    private final int maxSupportedTextureSize;
    private final int uniformOffsetAlignment;
    private final Map<RenderPipeline, EGlProgram> pipelineCache = new IdentityHashMap<>();

    public VkGpuDevice() {
        this.encoder = new VkCommandEncoder(this);
        this.debugLabels = new VkDebugLabel();
        this.maxSupportedTextureSize = DeviceManager.deviceProperties.limits().maxImageDimension2D();
        this.uniformOffsetAlignment = (int) DeviceManager.deviceProperties.limits().minUniformBufferOffsetAlignment();
        instance = this;
    }

    public static VkGpuDevice getInstance() {
        return instance;
    }

    public VkCommandEncoder createCommandEncoder() {
        return encoder;
    }

    public int getMaxTextureSize() {
        return maxSupportedTextureSize;
    }

    public String getVendor() {
        return DeviceManager.device.deviceName;
    }

    public String getRenderer() {
        return "VulkanMod/" + Initializer.getVersion();
    }

    public String getBackendName() {
        return "Vulkan";
    }

    public String getVersion() {
        return DeviceManager.device.vkVersion;
    }

    /**
     * Compile or retrieve a cached Vulkan pipeline for the given RenderPipeline descriptor.
     */
    public EGlProgram compilePipeline(RenderPipeline renderPipeline) {
        return pipelineCache.computeIfAbsent(renderPipeline, this::doCompilePipeline);
    }

    private EGlProgram doCompilePipeline(RenderPipeline renderPipeline) {
        try {
            ExtendedRenderPipeline extended = (ExtendedRenderPipeline) renderPipeline;
            GraphicsPipeline pipeline = extended.getPipeline();

            if (pipeline == null) {
                // TODO: Implement proper pipeline loading for RenderPipeline
                pipeline = null; // Placeholder for now
                extended.setPipeline(pipeline);
            }

            EGlProgram program = new EGlProgram(pipeline.hashCode(), "vulkan_pipeline");
            program.setupUniforms(pipeline, null, null);
            return program;
        } catch (Exception e) {
            LOGGER.error("Failed to compile pipeline {}", renderPipeline.toString(), e);
            return EGlProgram.INVALID_PROGRAM;
        }
    }

    public void clearPipelineCache() {
        pipelineCache.clear();
    }
}
