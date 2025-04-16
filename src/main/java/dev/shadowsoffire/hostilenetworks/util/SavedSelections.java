package dev.shadowsoffire.hostilenetworks.util;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class SavedSelections {

    public static final Codec<SavedSelections> CODEC = Codec.unboundedMap(DataModelRegistry.INSTANCE.holderCodec(), Codec.intRange(0, 65536))
        .xmap(Object2IntOpenHashMap::new, Function.identity()).xmap(SavedSelections::new, s -> s.selections);

    public static final StreamCodec<RegistryFriendlyByteBuf, SavedSelections> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(Object2IntOpenHashMap::new, DataModelRegistry.INSTANCE.holderStreamCodec(), ByteBufCodecs.VAR_INT),
        s -> s.selections,
        SavedSelections::new);

    public static final SavedSelections EMPTY = new SavedSelections(new Object2IntOpenHashMap<>());

    private final Object2IntOpenHashMap<DynamicHolder<DataModel>> selections = new Object2IntOpenHashMap<>();

    public SavedSelections(Object2IntMap<DynamicHolder<DataModel>> selections) {
        this.selections.putAll(selections);
    }

    public Object2IntMap<DynamicHolder<DataModel>> getSelections() {
        return Object2IntMaps.unmodifiable(this.selections);
    }

    public boolean isEmpty() {
        return this.selections.isEmpty();
    }

    public int size() {
        return this.selections.size();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SavedSelections ss && this.selections.equals(ss.selections);
    }

    @Override
    public int hashCode() {
        return this.selections.hashCode();
    }

}
