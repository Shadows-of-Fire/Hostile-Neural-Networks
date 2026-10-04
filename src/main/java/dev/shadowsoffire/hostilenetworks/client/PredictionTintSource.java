package dev.shadowsoffire.hostilenetworks.client;

import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Tints a Prediction (or Data Model) item by the {@code nameColor} of its stored {@link DataModel}.
 */
public final class PredictionTintSource implements ItemTintSource {

    public static final PredictionTintSource INSTANCE = new PredictionTintSource();
    public static final MapCodec<PredictionTintSource> MAP_CODEC = MapCodec.unit(INSTANCE);

    private PredictionTintSource() {}

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        DynamicHolder<DataModel> holder = DataModelItem.getStoredModel(stack);
        if (!holder.isBound()) return 0xFFFFFFFF;
        return 0xFF000000 | holder.get().nameColor().getValue();
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return MAP_CODEC;
    }
}
