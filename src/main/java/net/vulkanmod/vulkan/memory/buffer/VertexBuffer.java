package net.vulkanmod.vulkan.memory.buffer;

import net.vulkanmod.vulkan.memory.MemoryType;
import net.vulkanmod.vulkan.memory.MemoryTypes;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;

public class VertexBuffer extends Buffer {

    public VertexBuffer(int size) {
        this(size, MemoryTypes.HOST_MEM);
    }

    public VertexBuffer(int size, MemoryType type) {
        super("Vertex buffer", VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, type);
        this.createBuffer(size);
    }

    public void copyToVertexBuffer(int vertexSize, int vertexCount, java.nio.ByteBuffer data) {
        this.copyBuffer(data, data.remaining());
    }

}
