package net.vulkanmod.render.shadow;

import net.vulkanmod.vulkan.util.MappedBuffer;

import java.nio.ByteBuffer;

/**
 * Static holders for shadow-pass uniform data.
 *
 * Values are written by ShadowPass each frame and consumed by
 * Uniforms.java supplier lambdas registered in PipelineManager.
 */
public final class ShadowUniforms {
    private ShadowUniforms() {}

    // 16 floats for mat4
    private static final float[] sunMVP = new float[16];

    static {
        // Identity matrix default
        sunMVP[0] = 1; sunMVP[5] = 1; sunMVP[10] = 1; sunMVP[15] = 1;
    }

    private static float shadowBias     = 0.002f;
    private static float shadowDarkness = 0.6f;

    // --- Setters (called by ShadowPass) ---

    public static void setSunMVP(float[] mvp) {
        System.arraycopy(mvp, 0, sunMVP, 0, 16);
    }

    public static void setShadowBias(float bias)         { shadowBias     = bias; }
    public static void setShadowDarkness(float darkness) { shadowDarkness = darkness; }

    // --- Getters (consumed by Uniforms.java suppliers) ---

    /** Returns column-major mat4 as float[16] */
    public static float[] getSunMVP()        { return sunMVP; }
    public static float   getShadowBias()    { return shadowBias; }
    public static float   getShadowDarkness(){ return shadowDarkness; }

    /**
     * Write the ShadowMVP matrix into a MappedBuffer (for UBO upload).
     * MappedBuffer.buffer is a ByteBuffer; we write as floats column-major.
     */
    public static void writeSunMVP(MappedBuffer buf) {
        ByteBuffer bb = buf.buffer;
        bb.position(0);
        for (float f : sunMVP) bb.putFloat(f);
        bb.position(0);
    }
}
