package net.vulkanmod.render.shader;

/**
 * 1.21.1 replacement for com.mojang.blaze3d.shaders.ShaderType (which was added in 1.21.2+).
 */
public enum ShaderType {
    VERTEX,
    FRAGMENT;

    public String getExtension() {
        return switch (this) {
            case VERTEX -> ".vsh";
            case FRAGMENT -> ".fsh";
        };
    }
}
