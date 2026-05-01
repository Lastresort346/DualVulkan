package net.vulkanmod.render.chunk.build.frapi.compat;

/**
 * 1.21.1 compatibility shim for ItemStackRenderState.FoilType (1.21.10+).
 * In 1.21.1, glint/foil is handled by boolean flags in ItemRenderer.
 */
public enum FoilType {
    NONE,
    STANDARD,
    SPECIAL;

    public boolean hasGlint() {
        return this != NONE;
    }
}
