package dev.shadowsoffire.hostilenetworks.client.ghost;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.ARGB;

/**
 * Wraps a {@link VertexConsumer}, scaling the alpha channel of every vertex color by a fixed factor.
 */
public class GhostVertexConsumer implements VertexConsumer {

    private final VertexConsumer wrapped;
    private final int alpha;

    public GhostVertexConsumer(VertexConsumer wrapped, int alpha) {
        this.wrapped = wrapped;
        this.alpha = alpha & 0xFF;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        this.wrapped.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        this.wrapped.setColor(red, green, blue, alpha * this.alpha / 0xFF);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        this.wrapped.setColor(ARGB.multiply(color, ARGB.color(this.alpha, 0xFFFFFF)));
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.wrapped.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        this.wrapped.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        this.wrapped.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        this.wrapped.setNormal(normalX, normalY, normalZ);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        this.wrapped.setLineWidth(width);
        return this;
    }

}
