package dev.shadowsoffire.hostilenetworks.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class GhostVertexBuilder implements VertexConsumer {
    private final VertexConsumer wrapped;
    private final int alpha;

    public GhostVertexBuilder(VertexConsumer wrapped, int alpha) {
        this.wrapped = wrapped;
        this.alpha = alpha;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        return this.wrapped.addVertex(x, y, z);
    }

    @Override
    public VertexConsumer setColor(int red, int green, int blue, int alpha) {
        return this.wrapped.setColor(red, green, blue, alpha * this.alpha / 0xFF);
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        return this.wrapped.setUv(u, v);
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        return this.wrapped.setUv1(u, v);
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        return this.wrapped.setUv2(u, v);
    }

    @Override
    public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
        return this.wrapped.setNormal(normalX, normalY, normalZ);
    }

}
