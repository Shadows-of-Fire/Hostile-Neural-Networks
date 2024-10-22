package dev.shadowsoffire.hostilenetworks.data;

import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.MiscCodecs;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.json.NBTAdapter;
import dev.shadowsoffire.placebo.json.OptionalStackCodec;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Stores all of the information representing an individual Data Model.
 * 
 * @param entity       The primary entity type of this model. Must be a valid entity.
 * @param variants     All other entity types that match to this model. Invalid entities may be passed to this list for optional compat.
 * @param name         The display name of the data model.
 * @param simCost      FE cost per-tick to run this model in the simulation chamber.
 * @param input        The input itemstack for simulations. Usually the prediction matrix.
 * @param baseDrop     The generic item that is always dropped when simulating this model.
 * @param triviaKey    Lang key for the trivia text shown in the deep learner.
 * @param fabDrops     List of items produced in the Loot Fabricator when processing Predictions.
 * @param requiredData Optional overrides for the required data levels in the model tiers.
 * @param dataPerKill  Optional overrides for the data per kill values in the model tiers.
 */
public record DataModel(EntityType<?> entity, List<EntityType<?>> variants, Component name,
    DisplayData display, int simCost, Ingredient input, ItemStack baseDrop, String triviaKey,
    List<ItemStack> fabDrops, RequiredData requiredData, DataPerKill dataPerKill) implements CodecProvider<DataModel> {

    public static final Codec<DataModel> CODEC = RecordCodecBuilder.<DataModel>create(inst -> inst
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
            RequiredData.CODEC.optionalFieldOf("required_data", RequiredData.EMPTY).forGetter(DataModel::requiredData),
            DataPerKill.CODEC.optionalFieldOf("data_per_kill", DataPerKill.EMPTY).forGetter(DataModel::dataPerKill))
        .apply(inst, DataModel::new)).validate(DataModel::validate);

    public DataModel(DataModel other, List<ItemStack> newResults) {
        this(other.entity, other.variants, other.name, other.display, other.simCost, other.input, other.baseDrop, other.triviaKey, newResults, other.requiredData,
            other.dataPerKill);
    }

    /**
     * Returns the required data for this model to be considered at the given tier.
     * <p>
     * This method respects overrides present in {@link #requiredData}.
     * 
     * @param tier The tier being checked.
     * @return The (potentially overridden) amount of data required to be at the target tier.
     */
    public int getRequiredData(ModelTier tier) {
        return this.requiredData.getRequiredData(tier);
    }

    /**
     * Returns the data per kill for this model when at the given tier.
     * <p>
     * This method respects overrides present in {@link #dataPerKill}.
     * 
     * @param tier The tier of the model.
     * @return The (potentially overridden) amount of data received per kill.
     */
    public int getDataPerKill(ModelTier tier) {
        return this.dataPerKill.getDataPerKill(tier);
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

    /**
     * RequiredData records overrides over the {@link ModelTier} objects for the value of {@link ModelTier#requiredData()}.
     */
    public static record RequiredData(Reference2IntOpenHashMap<DynamicHolder<ModelTier>> overrides) {

        public static RequiredData EMPTY = new RequiredData(new Reference2IntOpenHashMap<>());

        public static final Codec<RequiredData> CODEC = Codec.unboundedMap(tierCodec(), Codec.intRange(0, Integer.MAX_VALUE))
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

    /**
     * DataPerKill records overrides over the {@link ModelTier} objects for the value of {@link ModelTier#dataPerKill()}.
     */
    public static record DataPerKill(Reference2IntOpenHashMap<DynamicHolder<ModelTier>> overrides) {

        public static DataPerKill EMPTY = new DataPerKill(new Reference2IntOpenHashMap<>());

        public static final Codec<DataPerKill> CODEC = Codec.unboundedMap(tierCodec(), Codec.intRange(0, Integer.MAX_VALUE))
            .xmap(Reference2IntOpenHashMap::new, Function.identity())
            .xmap(DataPerKill::new, DataPerKill::overrides);

        public int getDataPerKill(ModelTier tier) {
            return this.overrides.getOrDefault(ModelTierRegistry.INSTANCE.holder(tier), tier.dataPerKill());
        }
    }

    private static List<ItemStack> removeEmptyStacks(List<ItemStack> list) {
        return list.stream().filter(i -> !i.isEmpty()).toList();
    }

    private static Codec<DynamicHolder<ModelTier>> tierCodec() {
        return Codec.STRING.xmap(HostileNetworks::loc, ResourceLocation::getPath).xmap(ModelTierRegistry.INSTANCE::holder, DynamicHolder::getId);
    }

}
