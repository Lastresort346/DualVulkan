package net.beryl.mixin.shader;

import net.beryl.render.RenderingPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererM {

    @Inject(
        method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;prepare(Lnet/minecraft/world/level/Level;Lnet/minecraft/client/Camera;Lnet/minecraft/world/phys/HitResult;)V",
            shift = At.Shift.AFTER)
    )
    private void beginShaders(DeltaTracker deltaTracker, boolean bl, Camera camera,
                              GameRenderer gameRenderer, LightTexture lightTexture,
                              Matrix4f modelView, Matrix4f projection, CallbackInfo ci) {
        RenderingPipeline.beginRender(modelView, camera, new Vector4f(0f, 0f, 0f, 1f), deltaTracker);
    }
}
