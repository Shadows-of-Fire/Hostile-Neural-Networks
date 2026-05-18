package dev.shadowsoffire.hostilenetworks.data;

import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * A data-load {@link ICondition} that passes only while block data models are enabled in the config.
 * <p>
 * It is bound to {@link HostileConfig#enableBlockDataModels}. Every built-in block data model json carries this
 * condition, so while the option is disabled (the default) those models are never loaded into the
 * {@link DataModelRegistry} at all.
 * <p>
 * The condition has no fields, so its codec is a {@link MapCodec#unit unit codec} and the json form is simply
 * {@code { "type": "hostilenetworks:block_data_models_enabled" }}.
 */
public class BlockDataModelsCondition implements ICondition {

    public static final BlockDataModelsCondition INSTANCE = new BlockDataModelsCondition();

    public static final MapCodec<BlockDataModelsCondition> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(IContext context) {
        return HostileConfig.enableBlockDataModels;
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    @Override
    public String toString() {
        return "block_data_models_enabled";
    }
}
