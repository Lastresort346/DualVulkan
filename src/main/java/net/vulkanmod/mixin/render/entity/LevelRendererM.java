package net.vulkanmod.mixin.render.entity;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Entity culling optimization pass — body is currently TODO/disabled.
 * Imports from 1.21.10 GpuBufferSlice/GraphicsResourceAllocator removed for 1.21.1 compat.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererM {
}
