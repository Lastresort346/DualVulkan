package net.vulkanmod.render.engine;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 1.21.1 stub: debug labels for Vulkan objects.
 * GlShaderModule/GlBuffer from com.mojang.blaze3d.opengl don't exist in 1.21.1.
 */
public class VkDebugLabel {
    private static final Logger LOGGER = LogUtils.getLogger();

    public void applyLabel(VkGpuBuffer glBuffer) {}
    public void applyLabel(VkGpuTexture glTexture) {}
}
