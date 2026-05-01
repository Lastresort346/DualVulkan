package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.fog.FogData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {

    /**
     * 1.21.1: setupFog(Camera, FogRenderer.FogMode, float, boolean, float)
     * After fog is set up, read back from RenderSystem to sync VRenderSystem.fogData.
     */
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void onSetupFog(Camera camera, FogRenderer.FogMode fogMode, float renderDistance,
                                   boolean isFoggy, float deltaTime, CallbackInfo ci) {
        FogData fd = VRenderSystem.fogData;
        if (fd == null) {
            fd = new FogData();
            VRenderSystem.fogData = fd;
        }

        // Read fog params from RenderSystem (they were just set by FogRenderer.setupFog)
        float fogStart = RenderSystem.getShaderFogStart();
        float fogEnd   = RenderSystem.getShaderFogEnd();

        fd.renderDistanceStart   = fogStart;
        fd.renderDistanceEnd     = fogEnd;
        fd.environmentalStart    = fogStart;
        fd.environmentalEnd      = fogEnd;
        fd.skyEnd                = fogEnd;
        fd.cloudEnd              = fogEnd;
    }

    /**
     * After fog color is set, sync to VRenderSystem shader color.
     */
    @Inject(method = "levelFogColor", at = @At("TAIL"))
    private static void onLevelFogColor(CallbackInfo ci) {
        float[] fogColor = RenderSystem.getShaderFogColor();
        if (fogColor != null && fogColor.length >= 3) {
            VRenderSystem.setShaderFogColor(fogColor[0], fogColor[1], fogColor[2], 1.0f);
        }
    }
}
