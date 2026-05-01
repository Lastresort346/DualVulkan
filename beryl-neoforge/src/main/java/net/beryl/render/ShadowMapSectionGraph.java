package net.beryl.render;

import net.beryl.BerylMod;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.vulkanmod.Initializer;
import net.vulkanmod.render.chunk.ChunkAreaManager;
import net.vulkanmod.render.chunk.RenderSection;
import net.vulkanmod.render.chunk.SectionGrid;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.RenderRegionBuilder;
import net.vulkanmod.render.chunk.build.TaskDispatcher;
import net.vulkanmod.render.chunk.frustum.VFrustum;
import net.vulkanmod.render.chunk.util.AreaSetQueue;
import net.vulkanmod.render.chunk.util.ResettableQueue;
import net.vulkanmod.render.profiling.Profiler;

public class ShadowMapSectionGraph {
    private final Level level;
    private final SectionGrid sectionGrid;
    private final ChunkAreaManager chunkAreaManager;
    private final TaskDispatcher taskDispatcher;
    private final ResettableQueue<RenderSection> sectionQueue = new ResettableQueue<>();
    private AreaSetQueue chunkAreaQueue;
    private short lastFrame = 0;
    private final ResettableQueue<RenderSection> blockEntitiesSections = new ResettableQueue<>();
    private final ResettableQueue<RenderSection> rebuildQueue = new ResettableQueue<>();
    private VFrustum frustum;
    public RenderRegionBuilder renderRegionCache;
    int nonEmptyChunks;

    public ShadowMapSectionGraph(Level level, SectionGrid sectionGrid, TaskDispatcher taskDispatcher) {
        this.level = level;
        this.sectionGrid = sectionGrid;
        this.chunkAreaManager = sectionGrid.getChunkAreaManager();
        this.taskDispatcher = taskDispatcher;
        this.chunkAreaQueue = new AreaSetQueue(sectionGrid.getChunkAreaManager().size);
        this.renderRegionCache = WorldRenderer.getInstance().renderRegionCache;
    }

    public void update(Camera camera, VFrustum frustum, boolean spectator) {
        Profiler profiler = Profiler.getMainProfiler();
        BlockPos blockpos = camera.getBlockPosition();

        profiler.push("shadow_frustum");
        this.frustum = frustum;
        profiler.pop();

        initUpdate();
        initializeQueueForFullUpdate(camera);

        boolean solidLevel = !spectator || !level.getBlockState(blockpos).isSolidRender(level, blockpos);
        if (solidLevel) updateRenderChunks();
        else updateRenderChunksSpectator();

        scheduleRebuilds();
    }

    private void initializeQueueForFullUpdate(Camera camera) {
        BlockPos blockpos = camera.getBlockPosition();
        float xDir = RenderingPipeline.lightDirWS.x();
        float zDir = RenderingPipeline.lightDirWS.z();
        float dist = Math.min((float) BerylMod.CONFIG.shadowRenderDistance, 24.0f);
        int xOffset = (int)(xDir * dist * 16f);
        int zOffset = (int)(zDir * dist * 16f);
        int x = blockpos.getX() + xOffset;
        int y = blockpos.getY() + 64;
        int z = blockpos.getZ() + zOffset;

        for (int x1 = -2; x1 <= 2; x1++) {
            for (int z1 = -2; z1 <= 2; z1++) {
                RenderSection rs = sectionGrid.getSectionAtBlockPos(x + Mth.roundToward(x1, 8), y, z + Mth.roundToward(z1, 8));
                if (rs != null) {
                    initFirstNode(rs, lastFrame);
                    sectionQueue.add(rs);
                }
            }
        }
    }

    private static void initFirstNode(RenderSection rs, short frame) {
        rs.mainDir = 7;
        rs.sourceDirs = -128;
        rs.directions = -1;
        rs.setLastFrame2(frame);
        rs.visibility |= initVisibility();
        rs.directionChanges = 0;
        rs.steps = 0;
    }

    private static long initVisibility() {
        long vis = 0L;
        for (int dir = 0; dir < 6; dir++) { vis |= 1L << 48 + dir; vis |= 1L << 56 + dir; }
        return vis;
    }

    private void initUpdate() {
        chunkAreaQueue.clear();
        sectionGrid.getChunkAreaManager().resetQueues();
        sectionQueue.clear();
        blockEntitiesSections.clear();
        rebuildQueue.clear();
        ++lastFrame;
        nonEmptyChunks = 0;
    }

    private void updateRenderChunks() {
        while (sectionQueue.hasNext()) {
            RenderSection rs = sectionQueue.poll();
            if (notInFrustum(rs)) continue;
            if (!rs.isCompletelyEmpty()) {
                rs.getChunkArea().sectionQueue.add(rs);
                chunkAreaQueue.add(rs.getChunkArea());
                nonEmptyChunks++;
            }
            if (rs.isDirty()) { rebuildQueue.ensureCapacity(1); rebuildQueue.add(rs); }
            visitAdjacentNodes(rs, (byte)(rs.getVisibilityDirs() & rs.getDirections()));
        }
    }

    private void updateRenderChunksSpectator() {
        while (sectionQueue.hasNext()) {
            RenderSection rs = sectionQueue.poll();
            if (notInFrustum(rs)) continue;
            if (!rs.isCompletelyEmpty()) {
                rs.getChunkArea().sectionQueue.add(rs);
                chunkAreaQueue.add(rs.getChunkArea());
                nonEmptyChunks++;
            }
            if (rs.isDirty()) { rebuildQueue.ensureCapacity(1); rebuildQueue.add(rs); }
            visitAdjacentNodes(rs, (byte)(rs.adjDirs & rs.getDirections()));
        }
    }

    private void scheduleRebuilds() {
        for (int i = 0; i < rebuildQueue.size(); i++) {
            RenderSection s = rebuildQueue.get(i);
            s.rebuildChunkAsync(taskDispatcher, renderRegionCache);
            s.setNotDirty();
        }
        rebuildQueue.clear();
    }

    private boolean notInFrustum(RenderSection rs) {
        return !frustum.testFrustum(rs.xOffset, rs.yOffset, rs.zOffset, rs.xOffset + 16, rs.yOffset + 16, rs.zOffset + 16);
    }

    private void visitAdjacentNodes(RenderSection rs, byte dirs) {
        dirs = (byte)(dirs & rs.adjDirs);
        sectionQueue.ensureCapacity(6);
        checkToAdd(rs, rs.adjDown,  (byte)0, (byte)1, dirs);
        checkToAdd(rs, rs.adjUp,    (byte)1, (byte)0, dirs);
        checkToAdd(rs, rs.adjNorth, (byte)2, (byte)3, dirs);
        checkToAdd(rs, rs.adjSouth, (byte)3, (byte)2, dirs);
        checkToAdd(rs, rs.adjWest,  (byte)4, (byte)5, dirs);
        checkToAdd(rs, rs.adjEast,  (byte)5, (byte)4, dirs);
    }

    private void checkToAdd(RenderSection rs, RenderSection rel, byte dir, byte opp, byte dirs) {
        if ((dirs & (1 << dir)) != 0) addNode(rs, rel, dir, opp);
    }

    private void addNode(RenderSection rs, RenderSection rel, byte dir, byte opp) {
        boolean alreadyVisited = rel.setLastFrame2(lastFrame);
        if (!alreadyVisited) {
            rel.mainDir = dir;
            rel.sourceDirs = (byte)(1 << dir);
            rel.directions = (byte)(rs.directions & ~(1 << opp));
            sectionQueue.add(rel);
        }
        rel.addDir(dir);
    }

    public AreaSetQueue getChunkAreaQueue()                      { return chunkAreaQueue; }
    public ResettableQueue<RenderSection> getSectionQueue()      { return sectionQueue; }
    public ResettableQueue<RenderSection> getBlockEntitiesSections() { return blockEntitiesSections; }
    public short getLastFrame()                                  { return lastFrame; }
}
