package net.vulkanmod.interfaces;

import net.vulkanmod.vulkan.shader.GraphicsPipeline;

/**
 * Interface mixed into Minecraft's ShaderInstance to expose the Vulkan pipeline.
 */
public interface ShaderMixed {
    GraphicsPipeline getGraphicsPipeline();

    /** Alias for backwards compatibility. */
    default GraphicsPipeline getPipeline() {
        return getGraphicsPipeline();
    }
}
