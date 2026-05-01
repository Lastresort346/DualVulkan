package net.vulkanmod.render.engine;

/**
 * 1.21.1 stub: com.mojang.blaze3d.textures.GpuTextureView doesn't exist in 1.21.1.
 * Wrap VkGpuTexture for mip/layer range views.
 */
public class VkTextureView implements AutoCloseable {
    private boolean closed;
    private final VkGpuTexture texture;
    private final int baseMip;
    private final int mipLevels;

    public VkTextureView(VkGpuTexture gpuTexture, int baseMip, int mipLevels) {
        this.texture = gpuTexture;
        this.baseMip = baseMip;
        this.mipLevels = mipLevels;
    }

    public VkGpuTexture texture() { return texture; }
    public int baseMip() { return baseMip; }
    public int mipLevels() { return mipLevels; }
    public boolean isClosed() { return closed; }

    @Override
    public void close() { closed = true; }
}
