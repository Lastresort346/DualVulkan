package net.vulkanmod.render.engine;

import com.google.common.collect.Sets;
// RenderPipeline from blaze3d.pipeline is the multi-buffer queue in 1.21.1, not used here
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.descriptor.UBO;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

/**
 * 1.21.1 version: com.mojang.blaze3d.opengl.Uniform and RenderPipeline.UniformDescription
 * don't exist. Using VulkanMod's own VkUniform shim instead.
 */
public class EGlProgram {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Set<String> BUILT_IN_UNIFORMS = Sets.newHashSet("Projection", "Lighting", "Fog", "Globals");
    public static EGlProgram INVALID_PROGRAM = new EGlProgram(-1, "invalid");

    private final Map<String, VkUniform> uniformsByName = new HashMap<>();
    private final int programId;
    private final String debugLabel;

    public EGlProgram(int i, String string) {
        this.programId = i;
        this.debugLabel = string;
    }

    public void setupUniforms(Pipeline pipeline, List<String> uniformNames, List<String> samplers) {
        for (String name : uniformNames) {
            UBO ubo = pipeline.getUBO(name);
            if (ubo != null) {
                uniformsByName.put(name, new VkUniform.Ubo(ubo.binding));
            }
        }

        for (String samplerName : samplers) {
            var imageDescriptor = pipeline.getImageDescriptor(samplerName);
            if (imageDescriptor != null) {
                int binding  = imageDescriptor.getBinding();
                int imageIdx = imageDescriptor.imageIdx;
                uniformsByName.put(samplerName, new VkUniform.Sampler(binding, imageIdx));
            }
        }
    }

    @Nullable
    public VkUniform getUniform(String string) {
        RenderSystem.assertOnRenderThread();
        return this.uniformsByName.get(string);
    }

    public int getProgramId() { return this.programId; }
    public String toString()  { return this.debugLabel; }
    public String getDebugLabel() { return this.debugLabel; }
    public Map<String, VkUniform> getUniforms() { return this.uniformsByName; }
}
