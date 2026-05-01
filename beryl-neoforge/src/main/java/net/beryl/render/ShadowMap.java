package net.beryl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.ChunkArea;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.buffer.DrawBuffers;
import net.vulkanmod.render.chunk.frustum.VFrustum;
import net.vulkanmod.render.chunk.util.AreaSetQueue;
import net.vulkanmod.render.chunk.util.StaticQueue;
import net.vulkanmod.render.profiling.Profiler;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import net.vulkanmod.vulkan.memory.buffer.IndexBuffer;
import net.vulkanmod.vulkan.memory.buffer.IndirectBuffer;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

public class ShadowMap {
    private final Minecraft mc = Minecraft.getInstance();
    private Vector3f relativeLightPos = new Vector3f();
    private ShadowMapSectionGraph sectionGraph;
    IndirectBuffer[] indirectBuffers;
    private AreaSetQueue chunkAreaQueue;

    public ShadowMap() { allChanged(); allocateIndirectBuffers(); }

    public void allocateIndirectBuffers() {
        if (indirectBuffers != null && indirectBuffers.length == Renderer.getFramesNum()) return;
        if (indirectBuffers != null) Arrays.stream(indirectBuffers).forEach(Buffer::scheduleFree);
        indirectBuffers = new IndirectBuffer[Renderer.getFramesNum()];
        for (int i = 0; i < indirectBuffers.length; i++)
            indirectBuffers[i] = new IndirectBuffer(1_000_000, MemoryTypes.HOST_MEM);
    }

    public void allChanged() {
        WorldRenderer wr = WorldRenderer.getInstance();
        if (wr == null || wr.getSectionGrid() == null) return;
        chunkAreaQueue = new AreaSetQueue(wr.getChunkAreaManager().size);
        sectionGraph = new ShadowMapSectionGraph(WorldRenderer.getLevel(), wr.getSectionGrid(), wr.getTaskDispatcher());
    }

    public void setRelativeLightPos(float x, float y, float z) { relativeLightPos.set(x, y, z); }

    public void renderShadowMap(Camera camera, Matrix4f projection, DeltaTracker deltaTracker) {
        indirectBuffers[Renderer.getCurrentFrame()].reset();

        net.minecraft.world.phys.Vec3 cameraPos = camera.getPosition();
        VRenderSystem.enableDepthTest();
        VRenderSystem.depthFunc(GL11.GL_LEQUAL);

        Profiler profiler = Profiler.getMainProfiler();
        profiler.push("shadow_queue");

        double camX = cameraPos.x, camY = cameraPos.y, camZ = cameraPos.z;
        Matrix4f view = new Matrix4f(RenderingPipeline.lightView);
        Matrix4f VPLight = new Matrix4f(RenderingPipeline.lightProjection).mul(view);
        VPLight.get(RenderingPipeline.LightSpaceMatrixBuffer.buffer);

        VFrustum vFrustum = new VFrustum();
        vFrustum.calculateFrustum(view, projection);
        vFrustum.setCamOffset(camX, camY, camZ);
        sectionGraph.update(camera, vFrustum, false);
        profiler.pop();

        profiler.push("shadow_draw");
        renderOpaqueLayer(RenderType.solid(),         camX, camY, camZ, view, projection);
        renderOpaqueLayer(RenderType.cutout(),        camX, camY, camZ, view, projection);
        renderOpaqueLayer(RenderType.cutoutMipped(),  camX, camY, camZ, view, projection);
        profiler.pop();
    }

    private void renderOpaqueLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projection) {
        TerrainRenderType terrainRenderType = TerrainRenderType.get(renderType);
        boolean indirectDraw = Initializer.CONFIG.indirectDraw;

        VRenderSystem.enableCull();
        VRenderSystem.depthFunc(GL11.GL_LEQUAL);
        GlStateManager._enableDepthTest();
        GlStateManager._depthMask(true);
        VRenderSystem.applyMVP(modelView, projection);
        VRenderSystem.setPrimitiveTopologyGL(GL11.GL_TRIANGLES);

        Renderer renderer = Renderer.getInstance();
        GraphicsPipeline pipeline = PipelineManager.getTerrainShader(terrainRenderType);
        renderer.bindGraphicsPipeline(pipeline);
        VTextureSelector.bindShaderTextures(pipeline);

        IndexBuffer indexBuffer = Renderer.getDrawer().getQuadsIndexBuffer().getIndexBuffer();
        Renderer.getDrawer().bindIndexBuffer(Renderer.getCommandBuffer(), indexBuffer, indexBuffer.indexType.value);

        int currentFrame = Renderer.getCurrentFrame();
        Set<TerrainRenderType> allowed = Initializer.CONFIG.uniqueOpaqueLayer
                ? TerrainRenderType.COMPACT_RENDER_TYPES : TerrainRenderType.SEMI_COMPACT_RENDER_TYPES;

        if (allowed.contains(terrainRenderType)) {
            terrainRenderType.setCutoutUniform();
            Iterator<ChunkArea> it = sectionGraph.getChunkAreaQueue().iterator(false);
            while (it.hasNext()) {
                ChunkArea chunkArea = it.next();
                StaticQueue<RenderSection> queue = chunkArea.sectionQueue;
                DrawBuffers drawBuffers = chunkArea.getDrawBuffers();
                renderer.uploadAndBindUBOs(pipeline);
                if (drawBuffers.getAreaBuffer(terrainRenderType) != null && queue.size() > 0) {
                    drawBuffers.bindBuffers(Renderer.getCommandBuffer(), pipeline, terrainRenderType, camX, camY, camZ);
                    renderer.uploadAndBindUBOs(pipeline);
                    if (indirectDraw)
                        drawBuffers.buildDrawBatchesIndirect(indirectBuffers[currentFrame], queue, terrainRenderType);
                    else
                        drawBuffers.buildDrawBatchesDirect(queue, terrainRenderType);
                }
            }
        }
    }
}
