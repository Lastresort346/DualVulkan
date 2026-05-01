package net.vulkanmod.render.chunk.build.frapi;

import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

/**
 * 1.21.1 FRAPI stub: The full FRAPI integration used 1.21.10 API surfaces
 * (MeshView, BlockVertexConsumerProvider, FabricLayerRenderState, etc.)
 * that don't exist in 1.21.1's fabric-renderer-api-v1.
 * TODO: Rewrite FRAPI integration for 1.21.1 API.
 */
public class VulkanModRenderer implements Renderer {
    public static final VulkanModRenderer INSTANCE = new VulkanModRenderer();

    private VulkanModRenderer() {}

    @Override
    public MeshBuilder meshBuilder() {
        return null;
    }
}
