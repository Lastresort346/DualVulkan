package net.vulkanmod.render.engine;

/**
 * 1.21.1 compatibility shim replacing com.mojang.blaze3d.opengl.Uniform.
 * Represents a shader uniform binding for VulkanMod's descriptor-set system.
 */
public sealed interface VkUniform permits VkUniform.Ubo, VkUniform.Sampler, VkUniform.Utb {

    record Ubo(int binding) implements VkUniform {}

    record Sampler(int binding, int imageIdx) implements VkUniform {}

    record Utb(int binding, int format, Object textureFormat) implements VkUniform {}
}
