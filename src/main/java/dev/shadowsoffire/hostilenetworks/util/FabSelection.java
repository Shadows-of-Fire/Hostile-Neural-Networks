package dev.shadowsoffire.hostilenetworks.util;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * A single data model's output configuration within a Loot Fabricator: a {@link ProductionMode}, the ordered list of
 * drop indices it applies to, and a cursor tracking progress through that list for {@link ProductionMode#QUEUE}.
 * <p>
 * For {@link ProductionMode#FIXED}, {@link #entries} holds the single chosen drop index and {@link #cursor} is unused.
 * For {@link ProductionMode#QUEUE}, {@link #entries} is the queue and {@link #cursor} is the next entry to fabricate.
 * An empty {@link #entries} list means the fabricator is idle for the associated model.
 */
public record FabSelection(ProductionMode mode, List<Integer> entries, int cursor) {

    /** An empty Fixed selection - the fabricator is idle for the associated model. */
    public static final FabSelection EMPTY = new FabSelection(ProductionMode.FIXED, List.of(), 0);

    public static final Codec<FabSelection> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            ProductionMode.CODEC.fieldOf("mode").forGetter(FabSelection::mode),
            Codec.INT.listOf().fieldOf("entries").forGetter(FabSelection::entries),
            Codec.INT.optionalFieldOf("cursor", 0).forGetter(FabSelection::cursor))
        .apply(inst, FabSelection::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FabSelection> STREAM_CODEC = StreamCodec.composite(
        ProductionMode.STREAM_CODEC, FabSelection::mode,
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), FabSelection::entries,
        ByteBufCodecs.VAR_INT, FabSelection::cursor,
        FabSelection::new);

    /** A {@link ProductionMode#FIXED} selection on a single drop index. */
    public static FabSelection fixed(int index) {
        return new FabSelection(ProductionMode.FIXED, List.of(index), 0);
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /**
     * The drop index this selection currently resolves to, or {@code -1} if it has no entries.
     */
    public int current() {
        if (this.entries.isEmpty()) {
            return -1;
        }
        return this.entries.get(Math.floorMod(this.cursor, this.entries.size()));
    }

    /**
     * Returns a copy with the cursor advanced to the next queue entry, looping at the end.
     * <p>
     * A no-op for {@link ProductionMode#FIXED} selections or empty queues.
     */
    public FabSelection advanced() {
        if (this.mode == ProductionMode.FIXED || this.entries.isEmpty()) {
            return this;
        }
        return new FabSelection(this.mode, this.entries, (this.cursor + 1) % this.entries.size());
    }

    /**
     * The production strategy a Loot Fabricator applies to a data model's {@code fabDrops}.
     */
    public enum ProductionMode implements StringRepresentable {

        FIXED("fixed", ResourceLocation.withDefaultNamespace("textures/item/item_frame.png")),
        QUEUE("queue", ResourceLocation.withDefaultNamespace("textures/item/knowledge_book.png"));

        public static final Codec<ProductionMode> CODEC = StringRepresentable.fromEnum(ProductionMode::values);
        public static final StreamCodec<ByteBuf, ProductionMode> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], ProductionMode::ordinal);

        private final String name;
        private final ResourceLocation texture;

        ProductionMode(String name, ResourceLocation texture) {
            this.name = name;
            this.texture = texture;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public String getKey() {
            return "hostilenetworks.gui.fab_mode." + this.name;
        }

        public ResourceLocation getResourceLocation() {
            return this.texture;
        }

        public ProductionMode next() {
            return this == FIXED ? QUEUE : FIXED;
        }
    }
}
