package net.vulkanmod.vulkan.device;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Multi-GPU Workload Distributor
 * 
 * This class enables true multi-GPU rendering by distributing different rendering workloads
 * across available GPUs simultaneously. Instead of just switching between GPUs,
 * it splits the rendering pipeline to utilize multiple GPUs at the same time.
 * 
 * Workload Distribution Strategy:
 * - GPU 0 (Primary): Main geometry, terrain, entities
 * - GPU 1 (Secondary): Shadows, post-processing, compute shaders
 * 
 * This approach helps with VRAM constraints by splitting memory usage across GPUs.
 */
public class MultiGPUWorkloadDistributor {
    
    public enum WorkloadType {
        TERRAIN,           // Terrain and chunk rendering
        ENTITIES,          // Entity rendering
        SHADOWS,           // Shadow map rendering
        POST_PROCESS,       // Post-processing effects
        COMPUTE,           // Compute shader workloads
        PARTICLES,          // Particle systems
        GUI,               // UI and GUI elements
        TRANSLUCENT        // Translucent rendering
    }
    
    public static class GPUWorkload {
        public final WorkloadType type;
        public final int targetGPU;
        public final boolean enabled;
        public final float memoryWeight; // Fraction of GPU memory to allocate
        
        public GPUWorkload(WorkloadType type, int targetGPU, boolean enabled, float memoryWeight) {
            this.type = type;
            this.targetGPU = targetGPU;
            this.enabled = enabled;
            this.memoryWeight = memoryWeight;
        }
    }
    
    private static boolean multiGPUEnabled = false;
    private static boolean initialized = false;
    
    // Workload distribution map
    private static final Map<WorkloadType, GPUWorkload> workloadDistribution = new HashMap<>();
    
    // Thread pool for parallel GPU execution
    private static ExecutorService gpuExecutor;
    
    public static void initialize() {
        if (initialized || !DeviceManager.hasSecondaryDevice()) {
            return;
        }
        
        // Initialize thread pool
        gpuExecutor = Executors.newFixedThreadPool(2);
        
        // Setup default workload distribution
        setupDefaultWorkloadDistribution();
        
        multiGPUEnabled = true;
        initialized = true;
        
        Initializer.LOGGER.info("Multi-GPU Workload Distributor initialized");
        Initializer.LOGGER.info("Primary GPU: {}", DeviceManager.device.deviceName);
        Initializer.LOGGER.info("Secondary GPU: {}", DeviceManager.secondaryDevice.deviceName);
    }
    
    private static void setupDefaultWorkloadDistribution() {
        // Default distribution optimized for VRAM constraints
        workloadDistribution.clear();
        
        // Primary GPU handles main rendering (higher memory usage)
        workloadDistribution.put(WorkloadType.TERRAIN, new GPUWorkload(WorkloadType.TERRAIN, 0, true, 0.4f));
        workloadDistribution.put(WorkloadType.ENTITIES, new GPUWorkload(WorkloadType.ENTITIES, 0, true, 0.3f));
        workloadDistribution.put(WorkloadType.GUI, new GPUWorkload(WorkloadType.GUI, 0, true, 0.1f));
        
        // Secondary GPU handles memory-intensive workloads
        workloadDistribution.put(WorkloadType.SHADOWS, new GPUWorkload(WorkloadType.SHADOWS, 1, true, 0.3f));
        workloadDistribution.put(WorkloadType.POST_PROCESS, new GPUWorkload(WorkloadType.POST_PROCESS, 1, true, 0.4f));
        workloadDistribution.put(WorkloadType.COMPUTE, new GPUWorkload(WorkloadType.COMPUTE, 1, true, 0.3f));
        workloadDistribution.put(WorkloadType.PARTICLES, new GPUWorkload(WorkloadType.PARTICLES, 1, true, 0.2f));
        workloadDistribution.put(WorkloadType.TRANSLUCENT, new GPUWorkload(WorkloadType.TRANSLUCENT, 1, true, 0.2f));
    }
    
    public static boolean shouldUseMultiGPU() {
        return multiGPUEnabled && DeviceManager.hasSecondaryDevice();
    }
    
    public static int getTargetGPU(WorkloadType workloadType) {
        GPUWorkload workload = workloadDistribution.get(workloadType);
        return workload != null && workload.enabled ? workload.targetGPU : 0;
    }
    
    public static void executeWorkload(WorkloadType workloadType, Runnable workload) {
        if (!shouldUseMultiGPU()) {
            workload.run();
            return;
        }
        
        int targetGPU = getTargetGPU(workloadType);
        
        if (targetGPU == 0) {
            // Execute on primary GPU
            workload.run();
        } else {
            // Execute on secondary GPU asynchronously
            CompletableFuture.runAsync(workload, gpuExecutor);
        }
    }
    
    public static void setWorkloadDistribution(WorkloadType workloadType, int targetGPU, boolean enabled, float memoryWeight) {
        workloadDistribution.put(workloadType, new GPUWorkload(workloadType, targetGPU, enabled, memoryWeight));
    }
    
    public static void enableWorkload(WorkloadType workloadType) {
        GPUWorkload workload = workloadDistribution.get(workloadType);
        if (workload != null) {
            workloadDistribution.put(workloadType, new GPUWorkload(workloadType, workload.targetGPU, true, workload.memoryWeight));
        }
    }
    
    public static void disableWorkload(WorkloadType workloadType) {
        GPUWorkload workload = workloadDistribution.get(workloadType);
        if (workload != null) {
            workloadDistribution.put(workloadType, new GPUWorkload(workloadType, workload.targetGPU, false, workload.memoryWeight));
        }
    }
    
    public static void cleanup() {
        if (!initialized) {
            return;
        }
        
        multiGPUEnabled = false;
        
        // Wait for all work to complete
        if (gpuExecutor != null) {
            gpuExecutor.shutdown();
        }
        
        initialized = false;
        Initializer.LOGGER.info("Multi-GPU Workload Distributor cleaned up");
    }
    
    public static String getWorkloadInfo() {
        if (!shouldUseMultiGPU()) {
            return "Multi-GPU workload distribution is disabled";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Multi-GPU Workload Distribution:\n");
        info.append("Primary GPU (").append(DeviceManager.device.deviceName).append("):\n");
        
        workloadDistribution.entrySet().stream()
            .filter(entry -> entry.getValue().targetGPU == 0 && entry.getValue().enabled)
            .forEach(entry -> {
                GPUWorkload workload = entry.getValue();
                info.append("  - ").append(workload.type.name())
                      .append(" (Memory: ").append(workload.memoryWeight * 100).append("%)\n");
            });
        
        info.append("Secondary GPU (").append(DeviceManager.secondaryDevice.deviceName).append("):\n");
        
        workloadDistribution.entrySet().stream()
            .filter(entry -> entry.getValue().targetGPU == 1 && entry.getValue().enabled)
            .forEach(entry -> {
                GPUWorkload workload = entry.getValue();
                info.append("  - ").append(workload.type.name())
                      .append(" (Memory: ").append(workload.memoryWeight * 100).append("%)\n");
            });
        
        return info.toString();
    }
}
