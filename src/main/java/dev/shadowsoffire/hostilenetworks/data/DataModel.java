package dev.shadowsoffire.hostilenetworks.data;

import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.MiscCodecs;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.json.NBTAdapter;
import dev.shadowsoffire.placebo.json.OptionalStackCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Stores all of the information representing an individual Data Model.
 * 
 * @param entity      The primary entity type of this model. Must be a valid entity.
 * @param variants    All other entity types that match to this model. Invalid entities may be passed to this list for optional compat.
 * @param name        The display name of the data model.
 * @param simCost     FE cost per-tick to run this model in the simulation chamber.
 * @param input       The input itemstack for simulations. Usually the prediction matrix.
 * @param baseDrop    The generic item that is always dropped when simulating this model.
 * @param triviaKey   Lang key for the trivia text shown in the deep learner.
 * @param fabDrops    List of items produced in the Loot Fabricator when processing Predictions.
 * @param tierData    The amount of data it takes to reach each tier.
 * @param dataPerKill The amount of data granted per kill at each tier.
 */
public record DataModel(EntityType<?> entity, List<EntityType<?>> variants,
    Component name, DisplayData display,
    int simCost, Ingredient input, ItemStack baseDrop, String triviaKey, List<ItemStack> fabDrops, TierData tierData,
    DataPerKill dataPerKill) implements CodecProvider<DataModel> {

    public static final Codec<DataModel> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(DataModel::entity),
            MiscCodecs.OPTIONAL_ENTITY_TYPE_LIST.optionalFieldOf("variants", List.of()).forGetter(DataModel::variants),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(DataModel::name),
            DisplayData.CODEC.optionalFieldOf("display", DisplayData.DEFAULT).forGetter(DataModel::display),
            Codec.intRange(0, Integer.MAX_VALUE / 20).fieldOf("sim_cost").forGetter(DataModel::simCost),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(DataModel::input),
            OptionalStackCodec.INSTANCE.fieldOf("base_drop").forGetter(DataModel::baseDrop),
            Codec.STRING.fieldOf("trivia").forGetter(DataModel::triviaKey),
            OptionalStackCodec.INSTANCE.listOf().xmap(DataModel::removeEmptyStacks, Function.identity()).fieldOf("fabricator_drops").forGetter(DataModel::fabDrops),
            TierData.CODEC.optionalFieldOf("tier_data", TierData.DEFAULT).forGetter(DataModel::tierData),
            DataPerKill.CODEC.optionalFieldOf("data_per_kill", DataPerKill.DEFAULT).forGetter(DataModel::dataPerKill))
        .apply(inst, DataModel::new)).validate(DataModel::validate);

    public DataModel(DataModel other, List<ItemStack> newResults) {
        this(other.entity, other.variants, other.name, other.display, other.simCost, other.input, other.baseDrop, other.triviaKey, newResults, other.tierData,
            other.dataPerKill);
    }

    public int getTierData(ModelTier tier) {
        return this.tierData.forTier(tier);
    }

    public int getDataPerKill(ModelTier tier) {
        return this.dataPerKill.forTier(tier);
    }

    public ItemStack getPredictionDrop() {
        ItemStack stk = new ItemStack(Hostile.Items.PREDICTION);
        DataModelItem.setStoredModel(stk, this);
        return stk;
    }

    public int getNameColor() {
        return this.name.getStyle().getColor().getValue();
    }

    @Override
    public Codec<? extends DataModel> getCodec() {
        return CODEC;
    }

    public static DataResult<DataModel> validate(DataModel model) {
        if (model.name().getStyle().getColor() == null) {
            return DataResult.error(() -> "A data model must supply a color for the name component.");
        }

        return DataResult.success(model);
    }

    /**
     * @param nbt     NBT data applied to the rendered entity.
     * @param scale   Scale factor applied to the rendered entity. 1 = default scale.
     * @param xOffset X offset applied to the rendered entity.
     * @param yOffset Y offset applied to the rendered entity.
     * @param zOffset Z offset applied to the rendered entity.
     */
    public static record DisplayData(CompoundTag nbt, float scale, float xOffset, float yOffset, float zOffset) {

        public static final DisplayData DEFAULT = new DisplayData(new CompoundTag(), 1, 0, 0, 0);

        public static final Codec<DisplayData> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                NBTAdapter.EITHER_CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(DisplayData::nbt),
                Codec.floatRange(0, 5).optionalFieldOf("scale", 1F).forGetter(DisplayData::scale),
                Codec.floatRange(-5, 5).optionalFieldOf("x_offset", 0F).forGetter(DisplayData::xOffset),
                Codec.floatRange(-5, 5).optionalFieldOf("y_offset", 0F).forGetter(DisplayData::yOffset),
                Codec.floatRange(-5, 5).optionalFieldOf("z_offset", 0F).forGetter(DisplayData::zOffset))
            .apply(inst, DisplayData::new));
    }

    public static record TierData(int basic, int advanced, int superior, int selfAware) {

        public static final TierData DEFAULT = new TierData(6, 6 + 48, 6 + 48 + 300, 6 + 48 + 300 + 900);

        public static final Codec<TierData> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.intRange(0, 1048576).fieldOf("basic").forGetter(TierData::basic),
                Codec.intRange(0, 1048576).fieldOf("advanced").forGetter(TierData::advanced),
                Codec.intRange(0, 1048576).fieldOf("superior").forGetter(TierData::superior),
                Codec.intRange(0, 1048576).fieldOf("self_aware").forGetter(TierData::selfAware))
            .apply(inst, TierData::new)).validate(TierData::validate);

        public int forTier(ModelTier tier) {
            return switch (tier) {
                case FAULTY -> 0;
                case BASIC -> this.basic;
                case ADVANCED -> this.advanced;
                case SUPERIOR -> this.superior;
                case SELF_AWARE -> this.selfAware;
            };
        }

        public static DataResult<TierData> validate(TierData data) {
            if (data.basic >= data.advanced) {
                return DataResult.error(() -> "TierData basic threshold must be less than advanced threshold.");
            }
            else if (data.advanced >= data.superior) {
                return DataResult.error(() -> "TierData advanced threshold must be less than superior threshold.");
            }
            else if (data.superior >= data.selfAware) {
                return DataResult.error(() -> "TierData superior threshold must be less than self_aware threshold.");
            }
            return DataResult.success(data);
        }
    }

    public static record DataPerKill(int faulty, int basic, int advanced, int superior) {

        public static final DataPerKill DEFAULT = new DataPerKill(1, 4, 10, 18);

        public static final Codec<DataPerKill> CODEC = RecordCodecBuilder.create(inst -> inst
            .group(
                Codec.intRange(0, 1048576).fieldOf("faulty").forGetter(DataPerKill::faulty),
                Codec.intRange(0, 1048576).fieldOf("basic").forGetter(DataPerKill::basic),
                Codec.intRange(0, 1048576).fieldOf("advanced").forGetter(DataPerKill::advanced),
                Codec.intRange(0, 1048576).fieldOf("superior").forGetter(DataPerKill::superior))
            .apply(inst, DataPerKill::new));

        public int forTier(ModelTier tier) {
            return switch (tier) {
                case FAULTY -> this.faulty;
                case BASIC -> this.basic;
                case ADVANCED -> this.advanced;
                case SUPERIOR -> this.superior;
                case SELF_AWARE -> 0;
            };
        }
    }

    private static List<ItemStack> removeEmptyStacks(List<ItemStack> list) {
        return list.stream().filter(i -> !i.isEmpty()).toList();
    }

}
