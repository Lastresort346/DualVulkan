package net.vulkanmod.render.shadow;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.PipelineManager;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.device.MultiGPUWorkloadDistributor;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.shader.PipelineState;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

/**
 * DualVulkan ShadowPass
 *
 * Renders a depth-only shadow map from the sun's perspective.
 * When a secondary GPU is available, dispatches work via
 * MultiGPUWorkloadDistributor (compute queue / secondary device queue).
 *
 * The resulting depth image (shadowMapImage) is bound to sampler slot 5
 * so dualterrain.fsh can sample it for real-time terrain shadows.
 */
public class ShadowPass {

    // ----- Configuration -----
    public static final int   SHADOW_MAP_SIZE   = 2048;
    public static final int   SHADOW_SAMPLER_SLOT = 5;
    public static final float SHADOW_ORTHO_HALF = 128f;

    // ----- Singleton -----
    private static ShadowPass instance;
    public  static ShadowPass getInstance() { return instance; }

    // ----- Vulkan resources -----
    private VulkanImage      shadowMapImage;
    private Framebuffer      shadowFramebuffer;
    private RenderPass       shadowRenderPass;
    private GraphicsPipeline shadowPipeline;

    // Sun-space MVP, updated each frame
    private final Matrix4f sunMVP    = new Matrix4f();
    private final float[]  sunMVPArr = new float[16];

    private boolean       initialised = false;
    private final AtomicBoolean shadowReady = new AtomicBoolean(false);

    // ----- Lifecycle -----

    public static void create() {
        instance = new ShadowPass();
        instance.init();
    }

    private void init() {
        createShadowResources();
        shadowPipeline = PipelineManager.createShadowPipeline();
        initialised = true;
        Initializer.LOGGER.info("[ShadowPass] Ready — {}x{} depth map | secondary GPU: {}",
                SHADOW_MAP_SIZE, SHADOW_MAP_SIZE,
                DeviceManager.hasSecondaryDevice()
                        ? DeviceManager.secondaryDevice.deviceName : "none (primary fallback)");
    }

    private void createShadowResources() {
        // Let the Framebuffer builder create the depth image for us.
        // 0 color attachments, 1 depth attachment — the builder creates images internally.
        // The default depth format is whatever Vulkan.getDefaultDepthFormat() returns
        // (D32_SFLOAT or D24_UNORM_S8_UINT) — both work for shadow maps.
        shadowFramebuffer = new Framebuffer.Builder("ShadowMap", SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, 0, true)
                .build();

        // Grab the depth image the builder created
        shadowMapImage = shadowFramebuffer.getDepthAttachment();

        // Render pass — depth-only, tagged SHADOW so Renderer routes it to the secondary GPU path.
        shadowRenderPass = RenderPass.builder(shadowFramebuffer)
                .setPassType(RenderPass.PassType.SHADOW)
                .build();
    }

    public void cleanUp() {
        if (!initialised) return;
        shadowPipeline.cleanUp();
        shadowRenderPass.cleanUp();
        shadowFramebuffer.cleanUp();
        // shadowMapImage is owned by shadowFramebuffer and freed when it cleans up
        initialised = false;
    }

    // ----- Per-frame -----

    /** Compute sun MVP, then dispatch the shadow-depth pass (async on secondary GPU if available). */
    public void renderShadowMap() {
        if (!initialised) return;
        updateSunMVP();
        MultiGPUWorkloadDistributor.executeWorkload(
                MultiGPUWorkloadDistributor.WorkloadType.SHADOWS,
                this::recordAndSubmitShadowPass
        );
    }

    private void updateSunMVP() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float celestialAngle = mc.level.getSunAngle(1.0f);
        float sunRad = celestialAngle * (float)(Math.PI * 2.0);

        // Simple sun direction (rotates around X-axis)
        Vector3f sunDir = new Vector3f(
                (float) Math.sin(sunRad),
                (float) Math.cos(sunRad),
                0.0f
        ).normalize();

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Vector3f centre = new Vector3f((float)cam.x, (float)cam.y, (float)cam.z);
        Vector3f sunPos  = new Vector3f(centre).add(new Vector3f(sunDir).mul(SHADOW_ORTHO_HALF * 2f));

        Matrix4f view = new Matrix4f().lookAt(sunPos, centre, new Vector3f(0, 1, 0));
        float h = SHADOW_ORTHO_HALF;
        Matrix4f proj = new Matrix4f().ortho(-h, h, -h, h, 0.1f, h * 6f);

        // Vulkan clip space correction (flip Y, half Z)
        Matrix4f clip = new Matrix4f(
                1,  0,    0, 0,
                0, -1,    0, 0,
                0,  0, 0.5f, 0,
                0,  0, 0.5f, 1
        );

        clip.mul(proj).mul(view, sunMVP);
        sunMVP.get(sunMVPArr);
        ShadowUniforms.setSunMVP(sunMVPArr);
        ShadowUniforms.setShadowBias(0.002f);
        ShadowUniforms.setShadowDarkness(0.6f);
    }

    private void recordAndSubmitShadowPass() {
        try (MemoryStack stack = stackPush()) {
            // Use compute queue — on single-GPU systems this is still on the same physical device
            // but a different queue family, giving us real async execution.
            // On dual-GPU systems MultiGPUWorkloadDistributor routes this to the secondary device.
            CommandPool.CommandBuffer cmd = DeviceManager.getComputeQueue().beginCommands();
            VkCommandBuffer vkCmd = cmd.getHandle();

            // beginRenderPass handles layout transitions internally (UNDEFINED → DEPTH_STENCIL_ATTACHMENT)
            shadowFramebuffer.beginRenderPass(vkCmd, shadowRenderPass, stack);

            // Set viewport / scissor to shadow map size
            VkViewport.Buffer vp = VkViewport.calloc(1, stack);
            vp.get(0).x(0).y(0).width(SHADOW_MAP_SIZE).height(SHADOW_MAP_SIZE).minDepth(0f).maxDepth(1f);
            vkCmdSetViewport(vkCmd, 0, vp);

            VkRect2D.Buffer sc = VkRect2D.calloc(1, stack);
            sc.get(0).offset().set(0, 0);
            sc.get(0).extent().set(SHADOW_MAP_SIZE, SHADOW_MAP_SIZE);
            vkCmdSetScissor(vkCmd, 0, sc);

            // Bind the depth-only shadow pipeline using the default rasterizer state.
            long pipelineHandle = shadowPipeline.getHandle(PipelineState.DEFAULT);
            vkCmdBindPipeline(vkCmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineHandle);

            // End pass — transitions depth image to SHADER_READ_ONLY_OPTIMAL (our finalLayout)
            shadowRenderPass.endRenderPass(vkCmd);

            // Submit and wait (N/N+1 semaphore signalling is the next step)
            DeviceManager.getComputeQueue().submitCommands(cmd);
            DeviceManager.getComputeQueue().waitIdle();

            shadowReady.set(true);
        } catch (Exception e) {
            Initializer.LOGGER.error("[ShadowPass] Recording error: {}", e.getMessage(), e);
        }
    }

    /** Bind the shadow depth image to the sampler slot expected by dualterrain.fsh. */
    public void bindShadowMap() {
        if (!initialised || !shadowReady.get()) return;
        VTextureSelector.bindTexture(SHADOW_SAMPLER_SLOT, shadowMapImage);
    }

    public Matrix4f getSunMVP()       { return sunMVP; }
    public VulkanImage getShadowMap() { return shadowMapImage; }
    public boolean isShadowReady()    { return shadowReady.get(); }

}
