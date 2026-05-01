package net.vulkanmod.render.engine;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.vulkanmod.gl.VkGlTexture;
import net.vulkanmod.vulkan.texture.SamplerManager;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.vulkan.VK10;

/**
 * 1.21.1 version of VkGpuTexture.
 * In 1.21.1, com.mojang.blaze3d.textures.GpuTexture/GlTexture/TextureFormat don't exist.
 * This class wraps VkGlTexture and provides access to the underlying VulkanImage.
 */
public class VkGpuTexture {
    protected VkGlTexture glTexture;
    protected final int id;
    private final Int2ReferenceMap<VkFbo> fboCache = new Int2ReferenceOpenHashMap<>();
    protected boolean closed = false;
    protected boolean modesDirty = true;

    // Sampler state
    private int magFilter = VK10.VK_FILTER_LINEAR;
    private int minFilter = VK10.VK_FILTER_LINEAR;
    private int addressModeU = VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;
    private int addressModeV = VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;
    private boolean useMipmaps = false;
    private int mipLevels = 1;

    boolean needsClear = false;
    int clearColor = 0;
    float depthClearValue = 1.0f;

    public VkGpuTexture(int id, VkGlTexture glTexture, int mipLevels) {
        this.id = id;
        this.glTexture = glTexture;
        this.mipLevels = mipLevels;
    }

    public void close() {
        if (!this.closed) {
            this.closed = true;
            GlStateManager._deleteTexture(this.id);
            for (VkFbo fbo : this.fboCache.values()) {
                fbo.close();
            }
        }
    }

    public boolean isClosed() { return this.closed; }

    public int glId() { return this.id; }

    public int getMipLevels() { return mipLevels; }

    public int getWidth(int level) {
        VulkanImage img = getVulkanImage();
        return img != null ? img.width >> level : 0;
    }

    public int getHeight(int level) {
        VulkanImage img = getVulkanImage();
        return img != null ? img.height >> level : 0;
    }

    public void flushModeChanges() {
        if (this.modesDirty) {
            int maxLod = this.useMipmaps ? this.mipLevels - 1 : 0;
            long sampler = SamplerManager.getSampler(
                    addressModeU, addressModeV,
                    minFilter, magFilter,
                    VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR,
                    maxLod, false, 0, -1);
            if (glTexture != null && glTexture.getVulkanImage() != null) {
                glTexture.getVulkanImage().setSampler(sampler);
            }
            this.modesDirty = false;
        }
    }

    public void setLinearFilter(boolean linear) {
        this.magFilter = linear ? VK10.VK_FILTER_LINEAR : VK10.VK_FILTER_NEAREST;
        this.minFilter = linear ? VK10.VK_FILTER_LINEAR : VK10.VK_FILTER_NEAREST;
        this.modesDirty = true;
    }

    public void setUseMipmaps(boolean useMipmaps) {
        this.useMipmaps = useMipmaps;
        this.modesDirty = true;
    }

    public void setClearColor(int clearColor) {
        this.needsClear = true;
        this.clearColor = clearColor;
    }

    public void setDepthClearValue(float depthClearValue) {
        this.needsClear = true;
        this.depthClearValue = depthClearValue;
    }

    public boolean needsClear() { return needsClear; }

    public VkFbo getFbo(@Nullable VkGpuTexture depthAttachment) {
        int depthAttachmentId = depthAttachment == null ? 0 : depthAttachment.id;
        return this.fboCache.computeIfAbsent(depthAttachmentId,
                j -> new VkFbo(this, depthAttachment));
    }

    public VulkanImage getVulkanImage() {
        return glTexture != null ? glTexture.getVulkanImage() : null;
    }

    public static VkGpuTexture fromGlId(int glId) {
        VkGlTexture vglTexture = VkGlTexture.getTexture(glId);
        if (vglTexture == null) return null;
        return new VkGpuTexture(glId, vglTexture, 1);
    }
}
