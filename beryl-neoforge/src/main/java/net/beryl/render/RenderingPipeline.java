package net.beryl.render;

import net.beryl.BerylMod;
import net.beryl.render.util.BlitUtil;
import net.beryl.render.util.SUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.thread.ThreadBuilderPack;
import net.vulkanmod.render.profiling.Profiler;
import net.vulkanmod.render.util.MathUtil;
import net.vulkanmod.render.vertex.CustomVertexFormat;
import net.vulkanmod.render.vertex.TerrainRenderType;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.framebuffer.SwapChain;
import net.vulkanmod.vulkan.pass.DefaultMainPass;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Uniforms;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import net.vulkanmod.vulkan.util.ColorUtil;
import net.vulkanmod.vulkan.util.MappedBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

public class RenderingPipeline {

    static boolean useShaderPipeline;
    private static Framebuffer hdrFramebuffer;
    private static Framebuffer finalFramebuffer;
    private static Framebuffer shadowFramebuffer;
    private static Framebuffer tempFramebuffer;
    private static RenderPass shadowRenderPass;
    private static RenderPass hdrRenderPass1;
    private static RenderPass hdrRenderPass2;
    private static RenderPass tempRenderPass;
    private static boolean init;
    private static Minecraft mc;

    static float celestialAngle;
    static float sunAngle;
    public static float sunPathRotation = 20.0f;
    public static Matrix4f projection;
    public static Matrix4f view;
    static Matrix4f lightProjection = new Matrix4f();
    public static Matrix4f lightView = new Matrix4f();
    public static Matrix4f lightVP = new Matrix4f();
    public static Vector3f lightPosWS = new Vector3f();
    public static Vector3f lightDirWS = new Vector3f();
    public static Vector3f lightDir = new Vector3f();
    public static MappedBuffer LightSpaceMatrixBuffer = new MappedBuffer(64);
    public static MappedBuffer LightSpaceOffsetBuffer = new MappedBuffer(12);
    public static MappedBuffer CameraPosBuffer = new MappedBuffer(12);
    static MappedBuffer LightDirBuffer = new MappedBuffer(12);
    static MappedBuffer LightColorBuffer = new MappedBuffer(12);
    static MappedBuffer SkyColorBuffer = new MappedBuffer(16);
    public static MappedBuffer UpVectorBuffer = new MappedBuffer(12);

    private static float GameTime;
    public static float NightMultiplier;
    static float LightIntensity = 1.0f;
    static float LightVisibility = 1.0f;
    static float AmbientLightFactor = 1.0f;
    static float MinAmbientLight;
    static float FogFactor;
    static float BloomStrength = 0.04f;

    private static RenderingStage renderingStage = RenderingStage.UNDEFINED;
    private static ShadowMap shadowMap;
    static int shadowMapResolution;
    static float orthoMatrixHalfLength = 160.0f;
    static float ShadowTexelSize;
    static float shadowBias;
    static float shadowDistortion = 1.0f;

    private static GraphicsPipeline finalShader;
    private static GraphicsPipeline terrainShader;
    private static GraphicsPipeline shadowShader;
    private static GraphicsPipeline translucentShader;
    private static GraphicsPipeline entityShaderPipeline;
    private static GraphicsPipeline blitShader;
    private static GraphicsPipeline blitDepthShader;
    private static GraphicsPipeline skyShader;
    private static GraphicsPipeline cloudShader;
    private static GraphicsPipeline currentShader;

    private static Bloom bloom;
    private static ShaderMainPass shaderMainPass;

    static {
        useShaderPipeline = BerylMod.CONFIG != null && BerylMod.CONFIG.shadersOn;
        shadowMapResolution = BerylMod.CONFIG != null ? BerylMod.CONFIG.shadowResolution : 2048;
    }

    public static void setUseShaderPipeline(boolean b) {
        useShaderPipeline = b;
        if (useShaderPipeline) {
            ColorUtil.useGammaCorrection(true);
            VulkanImage.DefaultFormat = VK10.VK_FORMAT_R16G16B16A16_SFLOAT;
            PipelineManager.setTerrainVertexFormat(ShaderRendererResources.EXT_COMPRESSED_TERRAIN_FORMAT);
            ThreadBuilderPack.setTerrainBuilderConstructor(renderType ->
                    new net.beryl.render.build.ExtTerrainBuilder(TerrainRenderType.getRenderType(renderType).bufferSize()));
            Renderer.getInstance().setMainPass(shaderMainPass);
            SwapChain sc = Renderer.getInstance().getSwapChain();
            if (sc.getWidth() != shaderMainPass.hdrFinalFramebuffer.getWidth() ||
                sc.getHeight() != shaderMainPass.hdrFinalFramebuffer.getHeight()) {
                resizeFramebuffers();
            }
            init = false;
        } else {
            ColorUtil.useGammaCorrection(false);
            VulkanImage.DefaultFormat = VK10.VK_FORMAT_R8G8B8A8_UNORM;
            PipelineManager.setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
            ThreadBuilderPack.defaultTerrainBuilderConstructor();
            PipelineManager.setDefaultShader();
            Renderer.getInstance().setMainPass(DefaultMainPass.create());
        }
        Minecraft mc2 = Minecraft.getInstance();
        if (mc2.level != null) { mc2.levelRenderer.allChanged(); mc2.options.save(); }
    }

    public static boolean isUsingShaderPipeline() { return useShaderPipeline; }

    public static void preInit() {
        mc = Minecraft.getInstance();
        net.vulkanmod.vulkan.shader.SPIRVUtils.addIncludePath("/assets/beryl/shaders/");
        net.vulkanmod.vulkan.shader.SPIRVUtils.addIncludePath("/assets/beryl/shaders/include/");
        shaderMainPass = ShaderMainPass.PASS;
        shaderMainPass.init();
        setUseShaderPipeline(useShaderPipeline);
        Renderer.getInstance().addOnResizeCallback(RenderingPipeline::onResize);
    }

    public static void initResources() {
        initUniforms();
        finalShader        = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "final/final");
        blitShader         = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "blit/blit");
        blitDepthShader    = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "blit/blit_depth");
        skyShader          = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX, "sky/sky");
        cloudShader        = SUtil.createGraphicsPipeline(DefaultVertexFormat.POSITION_TEX_COLOR, "clouds/clouds");
        terrainShader      = SUtil.createGraphicsPipeline(ShaderRendererResources.EXT_COMPRESSED_TERRAIN_FORMAT, "terrain/terrain");
        shadowShader       = SUtil.createGraphicsPipeline(ShaderRendererResources.EXT_COMPRESSED_TERRAIN_FORMAT, "shadow/shadow");
        translucentShader  = SUtil.createGraphicsPipeline(ShaderRendererResources.EXT_COMPRESSED_TERRAIN_FORMAT, "translucent/translucent");
        entityShaderPipeline = SUtil.createGraphicsPipeline(DefaultVertexFormat.NEW_ENTITY, "entity/entity");

        if (shadowMap == null) shadowMap = new ShadowMap();

        bloom = new Bloom(5);
        SwapChain swapChain = Renderer.getInstance().getSwapChain();
        createFramebuffers(swapChain.getWidth(), swapChain.getHeight());
        shaderMainPass.setEarlyRenderPass(false);
        shaderMainPass.updateShaders();
        if (Renderer.getInstance().getBoundRenderPass() != null) Renderer.getInstance().endRenderPass();
        PipelineManager.setShaderGetter(RenderingPipeline::getShaderPipeline);
        WorldRenderer.getInstance().addOnAllChangedCallback(RenderingPipeline::onAllChanged);
        shadowMap.allChanged();
        ShaderRendererResources.initBlockIdMap();
        init = true;
    }

    static void initUniforms() {
        Uniforms.mat4f_uniformMap.put("LightSpaceMat",   () -> LightSpaceMatrixBuffer);
        Uniforms.vec1f_uniformMap.put("LightIntensity",  RenderingPipeline::getLightIntensity);
        Uniforms.vec1f_uniformMap.put("LightVisibility", () -> LightVisibility);
        Uniforms.vec1f_uniformMap.put("GameTime",        () -> GameTime);
        Uniforms.vec1f_uniformMap.put("NightMultiplier", () -> NightMultiplier);
        Uniforms.vec1f_uniformMap.put("AmbientLightFactor", () -> AmbientLightFactor);
        Uniforms.vec1f_uniformMap.put("MinAmbientLight", () -> MinAmbientLight);
        Uniforms.vec1f_uniformMap.put("ShadowTexelSize", () -> ShadowTexelSize);
        Uniforms.vec1f_uniformMap.put("ShadowBias",      () -> shadowBias);
        Uniforms.vec1f_uniformMap.put("ShadowDistortion",() -> shadowDistortion);
        Uniforms.vec1f_uniformMap.put("FogFactor",       () -> FogFactor);
        Uniforms.vec1f_uniformMap.put("BloomStrength",   () -> BloomStrength);
        Uniforms.vec3f_uniformMap.put("LightSpaceOffset",() -> LightSpaceOffsetBuffer);
        Uniforms.vec3f_uniformMap.put("LightDir",        RenderingPipeline::getLightDirBuffer);
        Uniforms.vec3f_uniformMap.put("LightColor",      () -> LightColorBuffer);
        Uniforms.vec3f_uniformMap.put("CameraPos",       () -> CameraPosBuffer);
        Uniforms.vec3f_uniformMap.put("UpVector",        () -> UpVectorBuffer);
        Uniforms.vec4f_uniformMap.put("SkyColor",        RenderingPipeline::getSkyColorBuffer);
    }

    public static void clearResources() {
        if (!init) return;
        init = false;
        WorldRenderer.getInstance().clearOnAllChangedCallbacks();
        freeFramebuffers();
        freeShadowFramebuffer();
        for (GraphicsPipeline p : new GraphicsPipeline[]{
                finalShader, terrainShader, shadowShader, translucentShader,
                entityShaderPipeline, blitShader, blitDepthShader, skyShader, cloudShader
        }) { if (p != null) p.scheduleCleanUp(); }
    }

    static void onResize() {
        if (useShaderPipeline) {
            resizeFramebuffers();
            if (shadowMap != null) shadowMap.allocateIndirectBuffers();
        }
    }

    public static void createFramebuffers(int w, int h) {
        createShadowFramebuffers();
        hdrFramebuffer   = new Framebuffer.Builder("beryl_hdr",   w, h, 1, true).setFormat(0x7902).build();
        finalFramebuffer = new Framebuffer.Builder("beryl_final", w, h, 1, false).setFormat(0x7902).build();
        tempFramebuffer  = new Framebuffer.Builder("beryl_temp",  w, h, 1, true).setFormat(0x7902).setLinearFiltering(true).build();

        RenderPass.Builder b = new RenderPass.Builder(hdrFramebuffer);
        b.getDepthAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR, VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
        hdrRenderPass1 = b.build();

        b = new RenderPass.Builder(hdrFramebuffer).setLoadOp(VK10.VK_ATTACHMENT_LOAD_OP_LOAD);
        b.getDepthAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_LOAD, VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
        hdrRenderPass2 = b.build();

        b = new RenderPass.Builder(tempFramebuffer);
        b.getDepthAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR, VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
        tempRenderPass = b.build();

        bloom.createFramebuffers(w, h, finalFramebuffer);
    }

    public static void createShadowFramebuffers() {
        if (shadowFramebuffer != null) freeShadowFramebuffer();
        shadowMapResolution = BerylMod.CONFIG.shadowResolution;
        shadowFramebuffer = new Framebuffer.Builder("beryl_shadow", shadowMapResolution, shadowMapResolution, 1, true)
                .setFormat(VK10.VK_FORMAT_R8G8B8A8_UNORM).setLinearFiltering(false).build();
        RenderPass.Builder b = new RenderPass.Builder(shadowFramebuffer);
        b.getDepthAttachmentInfo().setOps(VK10.VK_ATTACHMENT_LOAD_OP_CLEAR, VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
        shadowRenderPass = b.build();
    }

    public static void resizeFramebuffers() {
        Framebuffer sc = Renderer.getInstance().getSwapChain();
        int w = sc.getWidth(), h = sc.getHeight();
        if (w == 0 && h == 0) return;
        shaderMainPass.initFramebuffer();
        resizeFramebuffers(w, h);
    }

    public static void resizeFramebuffers(int w, int h) {
        if (init) { freeFramebuffers(); createFramebuffers(w, h); }
    }

    private static void freeFramebuffers() {
        if (hdrFramebuffer   != null) { hdrFramebuffer.cleanUp();   hdrFramebuffer   = null; }
        if (finalFramebuffer != null) { finalFramebuffer.cleanUp(); finalFramebuffer = null; }
        if (tempFramebuffer  != null) { tempFramebuffer.cleanUp();  tempFramebuffer  = null; }
        if (hdrRenderPass1   != null) { hdrRenderPass1.cleanUp();   hdrRenderPass1   = null; }
        if (hdrRenderPass2   != null) { hdrRenderPass2.cleanUp();   hdrRenderPass2   = null; }
        if (tempRenderPass   != null) { tempRenderPass.cleanUp();   tempRenderPass   = null; }
    }

    private static void freeShadowFramebuffer() {
        if (shadowFramebuffer != null) { shadowFramebuffer.cleanUp(); shadowFramebuffer = null; }
        if (shadowRenderPass  != null) { shadowRenderPass.cleanUp();  shadowRenderPass  = null; }
    }

    static void onAllChanged() { if (init && shadowMap != null) shadowMap.allChanged(); }

    public static void beginRender(Matrix4f view2, Camera camera, Vector4f clearColor, net.minecraft.client.DeltaTracker deltaTracker) {
        if (!useShaderPipeline) return;
        if (!init) initResources();

        float partialTicks = deltaTracker.getGameTimeDeltaPartialTick(true);
        setShaderGameTime(WorldRenderer.getLevel().getDayTime(), partialTicks);
        updateLight(partialTicks, camera, view2, clearColor);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            if (!Renderer.isRecording()) Renderer.getInstance().beginFrame();

            VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
            Renderer.getInstance().endRenderPass();

            Renderer.getInstance().beginRenderPass(shadowRenderPass, shadowFramebuffer);
            Renderer.setViewport(0, 0, shadowMapResolution, shadowMapResolution);
            Renderer.getInstance().setBoundFramebuffer(shadowFramebuffer);
            setShader(shadowShader);

            Matrix4f lightProj = new Matrix4f(lightProjection);
            java.util.concurrent.CompletableFuture.runAsync(() ->
                    net.vulkanmod.vulkan.device.MultiGPUWorkloadDistributor.executeWorkload(
                            net.vulkanmod.vulkan.device.MultiGPUWorkloadDistributor.WorkloadType.SHADOWS,
                            () -> shadowMap.renderShadowMap(camera, lightProj, deltaTracker)));

            renderingStage = RenderingStage.SHADOW_MAP;
            shadowMap.renderShadowMap(camera, lightProj, deltaTracker);

            Renderer.getInstance().endRenderPass();
            shadowFramebuffer.getDepthAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VTextureSelector.bindTexture(3, shadowFramebuffer.getDepthAttachment());

            Renderer.getInstance().beginRenderPass(hdrRenderPass1, hdrFramebuffer);
            shaderMainPass.setCurrentFramebuffer(hdrFramebuffer);
            SwapChain swapChain = Renderer.getInstance().getSwapChain();
            Renderer.setViewport(0, 0, swapChain.getWidth(), swapChain.getHeight());
            Renderer.getInstance().setBoundFramebuffer(hdrFramebuffer);
            Renderer.clearAttachments(VK10.VK_IMAGE_ASPECT_COLOR_BIT | VK10.VK_IMAGE_ASPECT_DEPTH_BIT);
            renderingStage = RenderingStage.TERRAIN;
            setShader(terrainShader);
        }
    }

    public static void copyAndBindFramebuffer() {}

    public static void endRender() {
        if (!useShaderPipeline) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
            Renderer.getInstance().endRenderPass(commandBuffer);
            renderingStage = RenderingStage.UNDEFINED;
            shadowFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            Profiler profiler = Profiler.getMainProfiler();
            profiler.push("bloom");
            bloom.render(commandBuffer, hdrFramebuffer);
            bloom.blendFramebuffer(stack, commandBuffer, hdrFramebuffer.getColorAttachment(), finalFramebuffer);
            Renderer.getInstance().endRenderPass();
            profiler.pop();

            VRenderSystem.disableBlend();
            VRenderSystem.disableDepthTest();
            finalFramebuffer.getColorAttachment().transitionImageLayout(stack, commandBuffer, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            VTextureSelector.bindTexture(finalFramebuffer.getColorAttachment());
            shaderMainPass.beginFinalRenderPass(stack);
            BlitUtil.blitFramebuffer(finalShader, finalFramebuffer.getColorAttachment());
        }
    }

    private static void updateLight(float partialTicks, Camera camera, Matrix4f camView, Vector4f clearColor) {
        celestialAngle = mc.level.getSunAngle(partialTicks);
        boolean isEnd   = mc.level.dimension().location().getPath().contains("end");
        boolean isNether = mc.level.dimension().location().getPath().contains("nether");

        MinAmbientLight = mc.options.gamma().get().floatValue() * 0.1f + 0.02f;

        if (celestialAngle > 0.3f && celestialAngle < 0.7f) {
            celestialAngle += 0.5f; NightMultiplier = 1.0f;
        } else if (celestialAngle >= 0.25f && celestialAngle <= 0.3f) {
            NightMultiplier = MathUtil.clamp(0.0f, 1.0f, (celestialAngle - 0.25f) / 0.05f);
        } else if (celestialAngle >= 0.7f && celestialAngle <= 0.75f) {
            NightMultiplier = MathUtil.clamp(0.0f, 1.0f, 1.0f - (celestialAngle - 0.7f) / 0.05f);
        } else { NightMultiplier = 0.0f; }

        if (isEnd) celestialAngle = 0.0f;

        lightView.identity()
                .rotateX((float) Math.toRadians(90))
                .rotateZ((float) Math.toRadians(90))
                .rotateY((float) Math.toRadians(celestialAngle * -360f))
                .rotateX((float) Math.toRadians(-sunPathRotation));
        lightPosWS.set(lightView.m02(), lightView.m12(), lightView.m22());

        float angleInterval = 0.2f;
        float t = 360.0f / angleInterval;
        celestialAngle = (float)(Math.floor(celestialAngle * t) / t);

        lightView.identity()
                .rotateX((float) Math.toRadians(90))
                .rotateY((float) Math.toRadians(celestialAngle * -360f))
                .rotateX((float) Math.toRadians(-sunPathRotation));

        lightDirWS.set(lightPosWS.x(), lightPosWS.y(), lightPosWS.z()).normalize();
        lightDirWS.mulPosition(camView, lightDir);

        Vector3f vec3 = new Vector3f(100f).mul(lightView.m02(), lightView.m12(), lightView.m22());
        shadowMap.setRelativeLightPos(vec3.x(), vec3.y(), vec3.z());

        Vector4f upVec4 = new Vector4f(0f, 1f, 0f, 0f).mul(camView);
        Vector3f upVec  = new Vector3f(upVec4.x(), upVec4.y(), upVec4.z());
        upVec.get(UpVectorBuffer.buffer);

        float LdotUp = Math.max(new Vector3f(UpVectorBuffer.buffer.asFloatBuffer()).dot(lightDir), 0f);
        float PI4th  = (float)(Math.PI / 4f);
        shadowBias   = (float) Math.max(Math.acos(LdotUp) - PI4th, 0.0) / PI4th;
        shadowBias   = shadowBias * 6e-4f + 3e-4f;

        LightDirBuffer.buffer.putFloat(0, lightDir.x());
        LightDirBuffer.buffer.putFloat(4, lightDir.y());
        LightDirBuffer.buffer.putFloat(8, lightDir.z());

        Vec3 cameraPos = camera.getPosition();
        CameraPosBuffer.putFloat(0, (float) cameraPos.x);
        CameraPosBuffer.putFloat(4, (float) cameraPos.y);
        CameraPosBuffer.putFloat(8, (float) cameraPos.z);

        float rainLevel = mc.level.getRainLevel(partialTicks);
        LightVisibility = 1.0f - rainLevel;
        if (isEnd) LightVisibility = 0.0f;

        LightIntensity = 0.2f + 0.8f * LightVisibility;

        if (NightMultiplier >= 1.0f) {
            AmbientLightFactor = 0.5f;
            BloomStrength = 0.06f;
            float i = 0.2f * LightIntensity;
            setLightColor(i, i, i);
        } else {
            AmbientLightFactor = LdotUp < 0.25f ? LdotUp * 2.0f + 0.5f : 1.0f;
            BloomStrength = 0.04f;
            float a = 1f - LdotUp; a *= a;
            float i = 8f * (1f - NightMultiplier) * LightIntensity;
            setLightColor(MathUtil.lerp(1.0f, 0.9f, a)*i, MathUtil.lerp(0.9f, 0.3f, a)*i, MathUtil.lerp(0.8f, 0.15f, a)*i);
        }

        FogFactor = 3e-4f + rainLevel * 0.01f;
        float fogColorM = (1f - NightMultiplier) * (0.8f * (1f - rainLevel) + 0.2f);
        VRenderSystem.setShaderFogColor(0.8f * fogColorM, 0.9f * fogColorM, 0.9f * fogColorM, 1f);
        if (isNether) { FogFactor = 0.005f; VRenderSystem.setShaderFogColor(0.6f, 0.6f, 0.6f, 1f); }
        else if (isEnd) { FogFactor = 0.005f; VRenderSystem.setShaderFogColor(0.05f, 0f, 0.05f, 1f); }

        BloomStrength *= BerylMod.CONFIG.bloomIntensity;
        FogFactor *= BerylMod.CONFIG.atmFogIntensity;
        ShadowTexelSize = 1.0f / shadowMapResolution;

        int shadowDist = BerylMod.CONFIG.shadowRenderDistance;
        shadowDistortion = 1.0f - 1.2f / shadowDist;
        orthoMatrixHalfLength = shadowDist * 16.0f;
        lightProjection.setOrthoSymmetric(orthoMatrixHalfLength * 2f, orthoMatrixHalfLength * 2f, -100f, 192f, true);
        lightVP.set(lightProjection).mul(lightView);
        lightVP.get(LightSpaceMatrixBuffer.buffer);
    }

    public static void setShader(GraphicsPipeline p) { currentShader = p; }

    public static GraphicsPipeline getShaderPipeline(TerrainRenderType renderType) {
        if (renderType == TerrainRenderType.TRANSLUCENT && currentShader != shadowShader) {
            VRenderSystem.blendFuncSeparate(
                org.lwjgl.opengl.GL11.GL_SRC_ALPHA, org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA,
                org.lwjgl.opengl.GL11.GL_ONE, org.lwjgl.opengl.GL11.GL_ZERO);
            return translucentShader;
        }
        return currentShader;
    }

    public static void setSkyColor(float r, float g, float b, float a) { ColorUtil.setRGBA_Buffer(SkyColorBuffer, r, g, b, a); }
    public static void setLightColor(float r, float g, float b) {
        LightColorBuffer.putFloat(0, r); LightColorBuffer.putFloat(4, g); LightColorBuffer.putFloat(8, b);
    }
    public static void setShaderGameTime(long l, float f) { GameTime = ((l % 24000L) + f) / 24000.0f; }
    public static float getGameTime()                        { return GameTime; }
    public static RenderingStage getRenderingStage()         { return renderingStage; }
    public static MappedBuffer getSkyColorBuffer()           { return SkyColorBuffer; }
    public static GraphicsPipeline getSkyShader()            { return skyShader; }
    public static GraphicsPipeline getCloudShader()          { return cloudShader; }
    public static GraphicsPipeline getCurrentShader()        { return currentShader; }
    public static MappedBuffer getLightDirBuffer()           { return LightDirBuffer; }
    public static MappedBuffer getLightSpaceMatrixBuffer()   { return LightSpaceMatrixBuffer; }
    public static float getLightIntensity()                  { return LightIntensity; }
}
