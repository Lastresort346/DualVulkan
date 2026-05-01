package net.vulkanmod.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.shadow.ShadowPass;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;
import net.vulkanmod.vulkan.shader.SPIRVUtils;

import java.util.function.Function;

import static net.vulkanmod.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;

public abstract class PipelineManager {
    private static final String shaderPath = SPIRVUtils.class.getResource("/assets/vulkanmod/shaders/").toExternalForm();
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    static GraphicsPipeline terrainShaderEarlyZ;
    static GraphicsPipeline terrainShader;
    static GraphicsPipeline dualTerrainShader;   // terrain + shadow map sampling
    static GraphicsPipeline fastBlitPipeline;
    static GraphicsPipeline cloudsPipeline;

    private static Function<TerrainRenderType, GraphicsPipeline> shaderGetter;

    /** Whether to use the dual-GPU terrain shader (requires shadow pass to be active). */
    private static boolean dualShaderActive = false;

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();

        // If a secondary GPU is present, start the shadow pass and switch to dual shader
        if (DeviceManager.hasSecondaryDevice()) {
            ShadowPass.create();
            dualTerrainShader = createDualTerrainPipeline();
            setDualShader();
            dualShaderActive = true;
        }
    }

    public static void setDefaultShader() {
        setShaderGetter(renderType ->
                renderType == TerrainRenderType.TRANSLUCENT ? terrainShaderEarlyZ : terrainShader);
    }

    /**
     * Switch to the shadow-sampling terrain shader.
     * TRANSLUCENT still uses the earlyZ pipeline (transparency sorting).
     */
    public static void setDualShader() {
        setShaderGetter(renderType ->
                renderType == TerrainRenderType.TRANSLUCENT ? terrainShaderEarlyZ : dualTerrainShader);
    }

    private static void createBasicPipelines() {
        terrainShaderEarlyZ = createPipeline("terrain", "terrain", "terrain_Z",  TERRAIN_VERTEX_FORMAT);
        terrainShader       = createPipeline("terrain", "terrain", "terrain",    TERRAIN_VERTEX_FORMAT);
        fastBlitPipeline    = createPipeline("blit",    "blit",    "blit",       CustomVertexFormat.NONE);
        cloudsPipeline      = createPipeline("clouds",  "clouds",  "clouds",     CustomVertexFormat.NONE);
    }

    // ----- Pipeline factory helpers -----

    private static GraphicsPipeline createPipeline(String baseName, String vertName, String fragName,
                                                    VertexFormat vertexFormat) {
        String pathB = String.format("basic/%s/%s", baseName, baseName);
        String pathV = String.format("basic/%s/%s", baseName, vertName);
        String pathF = String.format("basic/%s/%s", baseName, fragName);

        Pipeline.Builder builder = new Pipeline.Builder(vertexFormat, pathB);
        builder.parseBindingsJSON();

        SPIRVUtils.SPIRV vert = compileShaderAbsoluteFile(
                String.format("%s%s.vsh", shaderPath, pathV), SPIRVUtils.ShaderKind.VERTEX_SHADER);
        SPIRVUtils.SPIRV frag = compileShaderAbsoluteFile(
                String.format("%s%s.fsh", shaderPath, pathF), SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        builder.setSPIRVs(vert, frag);

        return builder.createGraphicsPipeline();
    }

    /**
     * Compile the depth-only shadow pipeline (shadow.vsh / shadow.fsh).
     * Called by ShadowPass.init() — public so ShadowPass can invoke it.
     */
    public static GraphicsPipeline createShadowPipeline() {
        String base = "basic/shadow/shadow";
        Pipeline.Builder builder = new Pipeline.Builder(TERRAIN_VERTEX_FORMAT, base);
        builder.parseBindingsJSON();

        SPIRVUtils.SPIRV vert = compileShaderAbsoluteFile(
                shaderPath + "basic/shadow/shadow.vsh", SPIRVUtils.ShaderKind.VERTEX_SHADER);
        SPIRVUtils.SPIRV frag = compileShaderAbsoluteFile(
                shaderPath + "basic/shadow/shadow.fsh", SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        builder.setSPIRVs(vert, frag);

        return builder.createGraphicsPipeline();
    }

    /**
     * Compile the dual-terrain pipeline (dualterrain.vsh / dualterrain.fsh).
     * Samples the shadow map from slot 5 in addition to the normal terrain pass.
     */
    private static GraphicsPipeline createDualTerrainPipeline() {
        String base = "basic/dualterrain/dualterrain";
        Pipeline.Builder builder = new Pipeline.Builder(TERRAIN_VERTEX_FORMAT, base);
        builder.parseBindingsJSON();

        SPIRVUtils.SPIRV vert = compileShaderAbsoluteFile(
                shaderPath + "basic/dualterrain/dualterrain.vsh", SPIRVUtils.ShaderKind.VERTEX_SHADER);
        SPIRVUtils.SPIRV frag = compileShaderAbsoluteFile(
                shaderPath + "basic/dualterrain/dualterrain.fsh", SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        builder.setSPIRVs(vert, frag);

        return builder.createGraphicsPipeline();
    }

    // ----- Accessors -----

    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        return shaderGetter.apply(renderType);
    }

    public static void setShaderGetter(Function<TerrainRenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public static GraphicsPipeline getTerrainDirectShader(RenderType renderType)   { return terrainShader; }
    public static GraphicsPipeline getTerrainIndirectShader(RenderType renderType) { return terrainShaderEarlyZ; }
    public static GraphicsPipeline getFastBlitPipeline()   { return fastBlitPipeline; }
    public static GraphicsPipeline getCloudsPipeline()     { return cloudsPipeline; }
    public static boolean          isDualShaderActive()    { return dualShaderActive; }

    public static void destroyPipelines() {
        terrainShaderEarlyZ.cleanUp();
        terrainShader.cleanUp();
        fastBlitPipeline.cleanUp();
        cloudsPipeline.cleanUp();
        if (dualTerrainShader != null) dualTerrainShader.cleanUp();
        if (ShadowPass.getInstance() != null) ShadowPass.getInstance().cleanUp();
    }
}
