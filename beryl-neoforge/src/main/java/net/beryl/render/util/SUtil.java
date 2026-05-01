package net.beryl.render.util;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.vulkanmod.render.shader.ShaderLoadUtil;
import net.vulkanmod.vulkan.shader.GraphicsPipeline;
import net.vulkanmod.vulkan.shader.Pipeline;

public class SUtil {
    public static final String RESOURCES_PATH = SUtil.class.getResource("/assets/beryl").toExternalForm();
    public static final String SHADERS_PATH = RESOURCES_PATH + "/shaders/";

    public static GraphicsPipeline createGraphicsPipeline(VertexFormat format, String path) {
        String[] split = ShaderLoadUtil.splitPath(path);
        return createGraphicsPipeline(format, split[0], split[1]);
    }

    public static GraphicsPipeline createGraphicsPipeline(VertexFormat format, String path, String name) {
        Pipeline.Builder builder = new Pipeline.Builder(format, path);
        String resolved = ShaderLoadUtil.resolveShaderPath(SHADERS_PATH, path);
        JsonObject config = ShaderLoadUtil.getJsonConfig(resolved, name);
        builder.parseBindings(config);
        ShaderLoadUtil.loadShaders(builder, config, name, resolved);
        return builder.createGraphicsPipeline();
    }
}
