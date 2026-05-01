package net.vulkanmod.render.model;

import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

public class CubeModel {

    /** VulkanMod-owned vertex type (replaces ModelPart.Vertex which is package-private in 1.21.1) */
    public record Vertex(Vector3f pos, float u, float v) {}

    /** VulkanMod-owned polygon type (replaces ModelPart.Polygon which is package-private in 1.21.1) */
    public record Polygon(Vertex[] vertices, Direction direction) {
        public org.joml.Vector3f normal() {
            net.minecraft.core.Vec3i n = direction.getNormal();
            return new org.joml.Vector3f(n.getX(), n.getY(), n.getZ());
        }
    }

    private Polygon[] polygons = new Polygon[6];
    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;

    Vector3f[] vertices;
    Vector3f[] transformed = new Vector3f[8];

    public void setVertices(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float q, float r, Set<Direction> set) {
        this.minX = f;
        this.minY = g;
        this.minZ = h;
        this.maxX = f + k;
        this.maxY = g + l;
        this.maxZ = h + m;
        this.polygons = new Polygon[set.size()];
        float s = maxX;
        float t = maxY;
        float u = maxZ;
        f -= n;
        g -= o;
        h -= p;
        s += n;
        t += o;
        u += p;
        if (bl) {
            float v = s;
            s = f;
            f = v;
        }

        this.vertices = new Vector3f[]{
                new Vector3f(f, g, h),
                new Vector3f(s, g, h),
                new Vector3f(s, t, h),
                new Vector3f(f, t, h),
                new Vector3f(f, g, u),
                new Vector3f(s, g, u),
                new Vector3f(s, t, u),
                new Vector3f(f, t, u)
        };

        for (int i1 = 0; i1 < 8; i1++) {
            this.vertices[i1].div(16.0f);
            this.transformed[i1] = new Vector3f(0.0f);
        }

        Vertex vertex1 = new Vertex(transformed[0], 0.0F, 0.0F);
        Vertex vertex2 = new Vertex(transformed[1], 0.0F, 8.0F);
        Vertex vertex3 = new Vertex(transformed[2], 8.0F, 8.0F);
        Vertex vertex4 = new Vertex(transformed[3], 8.0F, 0.0F);
        Vertex vertex5 = new Vertex(transformed[4], 0.0F, 0.0F);
        Vertex vertex6 = new Vertex(transformed[5], 0.0F, 8.0F);
        Vertex vertex7 = new Vertex(transformed[6], 8.0F, 8.0F);
        Vertex vertex8 = new Vertex(transformed[7], 8.0F, 0.0F);

        int idx = 0;
        if (set.contains(Direction.DOWN)) {
            this.polygons[idx++] = new Polygon(new Vertex[]{vertex6, vertex5, vertex1, vertex2}, Direction.DOWN);
        }
        if (set.contains(Direction.UP)) {
            this.polygons[idx++] = new Polygon(new Vertex[]{vertex3, vertex4, vertex8, vertex7}, Direction.UP);
        }
        if (set.contains(Direction.WEST)) {
            this.polygons[idx++] = new Polygon(new Vertex[]{vertex1, vertex5, vertex8, vertex4}, Direction.WEST);
        }
        if (set.contains(Direction.NORTH)) {
            this.polygons[idx++] = new Polygon(new Vertex[]{vertex2, vertex1, vertex4, vertex3}, Direction.NORTH);
        }
        if (set.contains(Direction.EAST)) {
            this.polygons[idx++] = new Polygon(new Vertex[]{vertex6, vertex2, vertex3, vertex7}, Direction.EAST);
        }
        if (set.contains(Direction.SOUTH)) {
            this.polygons[idx] = new Polygon(new Vertex[]{vertex5, vertex6, vertex7, vertex8}, Direction.SOUTH);
        }
    }

    public void transformVertices(Matrix4f matrix) {
        for (int i = 0; i < 8; ++i) {
            this.vertices[i].mulPosition(matrix, this.transformed[i]);
        }
    }

    public Polygon[] getPolygons() { return this.polygons; }
}
