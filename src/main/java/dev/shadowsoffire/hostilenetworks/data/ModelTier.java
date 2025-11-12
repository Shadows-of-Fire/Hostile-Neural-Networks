package dev.shadowsoffire.hostilenetworks.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Record which holds all information needed to described a model tier. Infinitely many model tiers may exist.
 * 
 * @param requiredData The amount of data required for a model to be considered of this tier.
 * @param dataGained   The amount of data added to a model of this tier when a player performs an action that grants data (i.e. killing an entity or mining a
 *                     block).
 * @param color        The color of the tier, used in various GUI elements.
 * @param accuracy     The accuracy level of the tier, which determines the chance a prediction item is generated. Values above 100% are supported.
 * @param canSim       If models of this tier can be simulated in the Simulation Chamber
 * @apiNote When accessing {@link #requiredData()} or {@link #dataGained()}, prefer using {@link EntityDataModel#getRequiredData(ModelTier)} and
 *          {@link EntityDataModel#getDataGained(ModelTier)}.
 */
public record ModelTier(int requiredData, int dataGained, TextColor color, float accuracy, boolean canSim) implements CodecProvider<ModelTier> {

    public static final Codec<ModelTier> NEW_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("required_data").forGetter(ModelTier::requiredData),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("data_gained").forGetter(ModelTier::dataGained),
        TextColor.CODEC.fieldOf("color").forGetter(ModelTier::color),
        Codec.floatRange(0, 64).fieldOf("accuracy").forGetter(ModelTier::accuracy),
        Codec.BOOL.optionalFieldOf("can_sim", true).forGetter(ModelTier::canSim))
        .apply(inst, ModelTier::new));

    public static final Codec<ModelTier> OLD_CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("required_data").forGetter(ModelTier::requiredData),
        Codec.intRange(0, Integer.MAX_VALUE).fieldOf("data_per_kill").forGetter(ModelTier::dataGained),
        TextColor.CODEC.fieldOf("color").forGetter(ModelTier::color),
        Codec.floatRange(0, 64).fieldOf("accuracy").forGetter(ModelTier::accuracy),
        Codec.BOOL.optionalFieldOf("can_sim", true).forGetter(ModelTier::canSim))
        .apply(inst, ModelTier::new));

    public static final Codec<ModelTier> CODEC = Codec.either(NEW_CODEC, OLD_CODEC).xmap(Either::unwrap, Either::left);

    public Component getComponent() {
        return Component.translatable("hostilenetworks.tier." + ModelTierRegistry.INSTANCE.getKey(this).getPath()).withStyle(Style.EMPTY.withColor(this.color));
    }

    public boolean isMax() {
        return ModelTierRegistry.getMaxTier() == this;
    }

    public boolean isMin() {
        return ModelTierRegistry.getMinTier() == this;
    }

    public String name() {
        return ModelTierRegistry.INSTANCE.getKey(this).getPath();
    }

    public DynamicHolder<ModelTier> asHolder() {
        return ModelTierRegistry.INSTANCE.holder(this);
    }

    public int colorValue() {
        return 0xFF000000 | this.color().getValue();
    }

    @Override
    public Codec<? extends ModelTier> getCodec() {
        return CODEC;
    }

}
