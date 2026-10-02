package net.neverandy.vr.client;

import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;

/** Writes the faces (as quads) and the edges (as line pairs) of a box. */
final class BoxShapes {

    private BoxShapes() {
    }

    static void faces(IVertexBuilder buffer, Matrix4f m, AxisAlignedBB b, float r, float g, float bl, float a) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        quad(buffer, m, r, g, bl, a, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(buffer, m, r, g, bl, a, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(buffer, m, r, g, bl, a, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(buffer, m, r, g, bl, a, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(buffer, m, r, g, bl, a, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(buffer, m, r, g, bl, a, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    static void edges(IVertexBuilder buffer, Matrix4f m, AxisAlignedBB b, float r, float g, float bl, float a) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
        float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        float[] xs = {x0, x1}, ys = {y0, y1}, zs = {z0, z1};
        for (float y : ys) {
            for (float z : zs) {
                line(buffer, m, r, g, bl, a, x0, y, z, x1, y, z);
            }
        }
        for (float x : xs) {
            for (float z : zs) {
                line(buffer, m, r, g, bl, a, x, y0, z, x, y1, z);
            }
        }
        for (float x : xs) {
            for (float y : ys) {
                line(buffer, m, r, g, bl, a, x, y, z0, x, y, z1);
            }
        }
    }

    private static void quad(IVertexBuilder buffer, Matrix4f m, float r, float g, float b, float a,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        buffer.pos(m, ax, ay, az).color(r, g, b, a).endVertex();
        buffer.pos(m, bx, by, bz).color(r, g, b, a).endVertex();
        buffer.pos(m, cx, cy, cz).color(r, g, b, a).endVertex();
        buffer.pos(m, dx, dy, dz).color(r, g, b, a).endVertex();
    }

    private static void line(IVertexBuilder buffer, Matrix4f m, float r, float g, float b, float a,
                             float ax, float ay, float az, float bx, float by, float bz) {
        buffer.pos(m, ax, ay, az).color(r, g, b, a).endVertex();
        buffer.pos(m, bx, by, bz).color(r, g, b, a).endVertex();
    }
}
