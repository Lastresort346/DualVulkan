package net.beryl.render.build;

import net.beryl.render.ShaderRendererResources;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.vulkanmod.render.vertex.TerrainBufferBuilder;
import org.lwjgl.system.MemoryUtil;

public class ExtTerrainBuilder extends TerrainBufferBuilder {
    public ExtTerrainBuilder(int size) { super(size); }

    @Override
    public void setBlockAttributes(BlockState blockState) {
        super.setBlockAttributes(blockState);
    }

    public void setFluidBlockAttributes(FluidState fluidState) {
    }
}
