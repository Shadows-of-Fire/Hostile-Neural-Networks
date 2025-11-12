package dev.shadowsoffire.hostilenetworks.util;

import java.util.function.Function;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

/**
 * This class records overrides over the {@link ModelTier} objects for the value of {@link ModelTier#dataGained()}.
 */
public record DataGained(Reference2IntOpenHashMap<DynamicHolder<ModelTier>> overrides) {

    public static DataGained EMPTY = new DataGained(new Reference2IntOpenHashMap<>());

    public static final Codec<DataGained> CODEC = Codec.unboundedMap(ModelTierRegistry.tierHolderCodec(), Codec.intRange(0, Integer.MAX_VALUE))
        .xmap(Reference2IntOpenHashMap::new, Function.identity())
        .xmap(DataGained::new, DataGained::overrides);

    public int getDataGained(ModelTier tier) {
        return this.overrides.getOrDefault(ModelTierRegistry.INSTANCE.holder(tier), tier.dataGained());
    }
}
