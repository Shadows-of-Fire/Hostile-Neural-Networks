package dev.shadowsoffire.hostilenetworks.util;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

/**
 * RequiredData records overrides over the {@link ModelTier} objects for the value of {@link ModelTier#requiredData()}.
 */
public record RequiredData(Reference2IntOpenHashMap<DynamicHolder<ModelTier>> overrides) {

    public static RequiredData EMPTY = new RequiredData(new Reference2IntOpenHashMap<>());

    public static final Codec<RequiredData> CODEC = Codec.unboundedMap(ModelTierRegistry.tierHolderCodec(), Codec.intRange(0, Integer.MAX_VALUE))
        .xmap(Reference2IntOpenHashMap::new, Function.identity())
        .xmap(RequiredData::new, RequiredData::overrides)
        .validate(RequiredData::validate);

    public int getRequiredData(ModelTier tier) {
        return this.overrides.getOrDefault(ModelTierRegistry.INSTANCE.holder(tier), tier.requiredData());
    }

    public static DataResult<RequiredData> validate(RequiredData data) {
        int last = -1;
        for (ModelTier tier : ModelTierRegistry.getSortedTiers()) {
            int reqData = data.getRequiredData(tier);
            if (reqData <= last) {
                DynamicHolder<ModelTier> holder = ModelTierRegistry.INSTANCE.holder(tier);
                int _last = last; // Lambda requires effective finals
                return DataResult.error(() -> "Tier Data overrides must preserve the same ordering as the main tier list. "
                    + "Currently, the override for tier " + holder.getId().getPath() + " is invalid. Expected a value greater than " + _last + ", but got " + reqData);
            }
            last = reqData;
        }
        return DataResult.success(data);
    }
}
