package dev.shadowsoffire.hostilenetworks.client;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * A {@link MultiBufferSource} that gives every {@link RenderType} its own buffer, so requesting one type never ends
 * another's batch.
 * <p>
 * Workaround for a GeckoLib crash: against vanilla's shared-buffer {@link MultiBufferSource#immediate immediate} source,
 * a render layer's mid-render {@code getBuffer} ends the base entity's batch, and GeckoLib's revival
 * ({@code GeoRenderer#checkAndRefreshBuffer}) can't see through HNN's {@link GhostVertexBuilder} wrapper to re-fetch
 * it, so the dead buffer is reused and crashes. See https://github.com/Shadows-of-Fire/Hostile-Neural-Networks/pull/118
 * <p>
 * Note that this ignores {@link RenderType#canConsolidateConsecutiveGeometry()}, so using it with those render types will cause weird things to happen.
 */
public class PerTypeBufferSource implements MultiBufferSource {

    private final Map<RenderType, ByteBufferBuilder> byteBuffers = new LinkedHashMap<>();
    private final Map<RenderType, BufferBuilder> startedBuilders = new LinkedHashMap<>();

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        BufferBuilder builder = this.startedBuilders.get(type);
        if (builder != null) return builder;
        ByteBufferBuilder bytes = this.byteBuffers.computeIfAbsent(type, t -> new ByteBufferBuilder(t.bufferSize()));
        builder = new BufferBuilder(bytes, type.mode(), type.format());
        this.startedBuilders.put(type, builder);
        return builder;
    }

    public void endBatch() {
        for (Map.Entry<RenderType, BufferBuilder> entry : this.startedBuilders.entrySet()) {
            RenderType type = entry.getKey();
            MeshData mesh = entry.getValue().build();
            if (mesh != null) {
                if (type.sortOnUpload()) {
                    mesh.sortQuads(this.byteBuffers.get(type), RenderSystem.getVertexSorting());
                }
                type.draw(mesh);
            }
        }
        this.startedBuilders.clear();
    }
}
