package net.vulkanmod.render.engine;

import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.memory.MemoryType;
import net.vulkanmod.vulkan.memory.MemoryTypes;
import net.vulkanmod.vulkan.memory.buffer.Buffer;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static org.lwjgl.vulkan.VK10.*;

/**
 * 1.21.1 version: com.mojang.blaze3d.buffers.GpuBuffer doesn't exist.
 * This is a standalone Vulkan buffer wrapper.
 */
public class VkGpuBuffer {
    // Usage flags (matching blaze3d 1.21.2+ values for forward compat)
    public static final int USAGE_COPY_SRC           = 0x0001;
    public static final int USAGE_COPY_DST           = 0x0002;
    public static final int USAGE_VERTEX             = 0x0004;
    public static final int USAGE_INDEX              = 0x0008;
    public static final int USAGE_UNIFORM            = 0x0010;
    public static final int USAGE_UNIFORM_TEXEL_BUFFER = 0x0020;
    public static final int USAGE_MAP_READ           = 0x0040;
    public static final int USAGE_MAP_WRITE          = 0x0080;
    public static final int USAGE_HINT_CLIENT_STORAGE = 0x0100;

    protected boolean closed;
    @Nullable protected final Supplier<String> label;
    private final int usage;
    private final int size;
    Buffer buffer;

    public VkGpuBuffer(VkDebugLabel debugLabel, @Nullable Supplier<String> supplier, int usage, int size) {
        this.label = supplier;
        this.usage = usage;
        this.size = size;

        int vkUsage = 0;
        if ((usage & USAGE_COPY_SRC) != 0)  vkUsage |= VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
        if ((usage & USAGE_COPY_DST) != 0)  vkUsage |= VK_BUFFER_USAGE_TRANSFER_DST_BIT;
        if ((usage & USAGE_VERTEX) != 0)    vkUsage |= VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
        if ((usage & USAGE_INDEX) != 0)     vkUsage |= VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
        if ((usage & USAGE_UNIFORM) != 0)   vkUsage |= VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT;
        if ((usage & USAGE_UNIFORM_TEXEL_BUFFER) != 0) vkUsage |= VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT;

        boolean mappable = (usage & USAGE_MAP_READ) != 0 || (usage & USAGE_MAP_WRITE) != 0
                || (usage & USAGE_HINT_CLIENT_STORAGE) != 0;
        MemoryType memoryType = mappable ? MemoryTypes.HOST_MEM : MemoryTypes.GPU_MEM;

        String bufLabel = supplier != null ? supplier.get() : "buffer";
        this.buffer = new Buffer(bufLabel, vkUsage, memoryType);
        this.buffer.createBuffer(size);
    }

    public boolean isClosed() { return this.closed; }

    public int size() { return size; }
    public int usage() { return usage; }

    public void close() {
        if (!this.closed) {
            this.closed = true;
            MemoryManager.getInstance().addToFreeable(this.buffer);
        }
    }

    public Buffer getBuffer() { return buffer; }

    public static class MappedView implements AutoCloseable {
        private final ByteBuffer data;

        public MappedView(int target, ByteBuffer byteBuffer) {
            this.data = byteBuffer;
        }

        public ByteBuffer data() { return this.data; }

        @Override
        public void close() {}
    }
}
