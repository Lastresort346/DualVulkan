package net.beryl.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class ShaderRendererResources {
    public static final VertexFormatElement ELEMENT_POSITION;
    public static final VertexFormatElement ELEMENT_COLOR_UINT;
    public static final VertexFormatElement ELEMENT_UV0;
    public static final VertexFormatElement ELEMENT_NORMAL;
    public static final VertexFormatElement ELEMENT_BLOCK_ID_INT;

    public static final VertexFormat EXT_COMPRESSED_TERRAIN_FORMAT;
    public static final VertexFormat EXT_BLOCK;

    private static final Object2IntOpenHashMap<net.minecraft.world.level.block.Block> blockIdMap = new Object2IntOpenHashMap<>();
    private static final Object2IntOpenHashMap<net.minecraft.world.level.material.Fluid> fluidIdMap = new Object2IntOpenHashMap<>();

    static {
        ELEMENT_POSITION      = new VertexFormatElement(0, 0, VertexFormatElement.Type.SHORT,  VertexFormatElement.Usage.POSITION, 4);
        ELEMENT_COLOR_UINT    = new VertexFormatElement(1, 0, VertexFormatElement.Type.UINT,   VertexFormatElement.Usage.COLOR,    1);
        ELEMENT_UV0           = new VertexFormatElement(2, 0, VertexFormatElement.Type.USHORT, VertexFormatElement.Usage.UV,       2);
        ELEMENT_NORMAL        = new VertexFormatElement(4, 0, VertexFormatElement.Type.BYTE,   VertexFormatElement.Usage.NORMAL,   4);
        ELEMENT_BLOCK_ID_INT  = new VertexFormatElement(5, 0, VertexFormatElement.Type.INT,    VertexFormatElement.Usage.GENERIC,  1);

        EXT_COMPRESSED_TERRAIN_FORMAT = VertexFormat.builder()
                .add("Position", ELEMENT_POSITION)
                .add("UV0",      ELEMENT_UV0)
                .add("Color",    ELEMENT_COLOR_UINT)
                .add("Normal",   ELEMENT_NORMAL)
                .add("BlockID",  ELEMENT_BLOCK_ID_INT)
                .build();

        VertexFormatElement posF  = new VertexFormatElement(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
        VertexFormatElement colB  = new VertexFormatElement(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR,    4);
        VertexFormatElement uv0f  = new VertexFormatElement(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV,       2);
        VertexFormatElement uv2s  = new VertexFormatElement(3, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV,       2);
        VertexFormatElement normB = new VertexFormatElement(4, 0, VertexFormatElement.Type.BYTE,  VertexFormatElement.Usage.NORMAL,   4);
        EXT_BLOCK = VertexFormat.builder()
                .add("Position", posF).add("Color", colB).add("UV0", uv0f)
                .add("UV2", uv2s).add("Normal", normB).add("BlockID", ELEMENT_BLOCK_ID_INT)
                .build();

        blockIdMap.defaultReturnValue(0);
        fluidIdMap.defaultReturnValue(0);
    }

    public static void initBlockIdMap() {
        blockIdMap.clear();
        blockIdMap.defaultReturnValue(0);
        blockIdMap.put(Blocks.WATER, 1);
        blockIdMap.put(Blocks.GRASS_BLOCK, 2);
        blockIdMap.put(Blocks.TALL_GRASS, 2);
        blockIdMap.put(Blocks.FERN, 2);
        blockIdMap.put(Blocks.LARGE_FERN, 2);
        blockIdMap.put(Blocks.OAK_LEAVES, 2);
        blockIdMap.put(Blocks.BIRCH_LEAVES, 2);
        blockIdMap.put(Blocks.SPRUCE_LEAVES, 2);
        blockIdMap.put(Blocks.JUNGLE_LEAVES, 2);
        blockIdMap.put(Blocks.ACACIA_LEAVES, 2);
        blockIdMap.put(Blocks.DARK_OAK_LEAVES, 2);
        blockIdMap.put(Blocks.GLASS, 3);
        blockIdMap.put(Blocks.ICE, 4);
        blockIdMap.put(Blocks.GLOWSTONE, 5);
        fluidIdMap.clear();
        fluidIdMap.put(Fluids.WATER, 1);
        fluidIdMap.put(Fluids.FLOWING_WATER, 1);
        fluidIdMap.put(Fluids.LAVA, 14);
        fluidIdMap.put(Fluids.FLOWING_LAVA, 14);
    }

    public static int getBlockId(BlockState blockState) { return blockIdMap.getInt(blockState.getBlock()); }
    public static int getBlockId(FluidState fluidState) { return fluidIdMap.getInt(fluidState.getType()); }
}
