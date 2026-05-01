package net.vulkanmod.render.shader;

/**
 * Disabled for 1.21.1: BlendFunction/DepthTestFunction/RenderPipelines don't exist in 1.21.1.
 * The 1.21.1 RenderPipeline is a render call queue, not a pipeline descriptor.
 * Custom pipeline descriptors are registered differently in 1.21.1 (via ShaderInstance).
 */
public class CustomRenderPipelines {
    // TODO: Re-implement custom render pipelines for 1.21.1 API
}
