package dev.shadowsoffire.hostilenetworks.data;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.hostilenetworks.util.DataGained;
import dev.shadowsoffire.hostilenetworks.util.DisplayData;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.hostilenetworks.util.MiscCodecs;
import dev.shadowsoffire.hostilenetworks.util.RequiredData;
import dev.shadowsoffire.placebo.json.OptionalStackCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

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
 * @param attunement   Optional attunement rules for this model.
 */
public record EntityDataModel(EntityType<?> entity, List<EntityType<?>> variants, Optional<Component> displayName, TextColor nameColor,
    DisplayData display, int simCost, Ingredient input, ItemStack baseDrop, String triviaKey,
    List<ItemStack> fabDrops, RequiredData requiredData, DataGained dataGained, Optional<EntityAttunement> attunement) implements DataModel {

    public static final Codec<EntityDataModel> NEW_CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(EntityDataModel::entity),
            MiscCodecs.OPTIONAL_ENTITY_TYPE_LIST.optionalFieldOf("variants", List.of()).forGetter(EntityDataModel::variants),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(EntityDataModel::displayName),
            TextColor.CODEC.fieldOf("name_color").forGetter(EntityDataModel::nameColor),
            DisplayData.CODEC.optionalFieldOf("display", DisplayData.DEFAULT).forGetter(EntityDataModel::display),
            Codec.intRange(0, Integer.MAX_VALUE / 20).fieldOf("sim_cost").forGetter(EntityDataModel::simCost),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(EntityDataModel::input),
            OptionalStackCodec.INSTANCE.fieldOf("base_drop").forGetter(EntityDataModel::baseDrop),
            Codec.STRING.fieldOf("trivia").forGetter(EntityDataModel::triviaKey),
            OptionalStackCodec.INSTANCE.listOf().xmap(EntityDataModel::removeEmptyStacks, Function.identity()).fieldOf("fabricator_drops").forGetter(EntityDataModel::fabDrops),
            RequiredData.CODEC.optionalFieldOf("required_data", RequiredData.EMPTY).forGetter(EntityDataModel::requiredData),
            DataGained.CODEC.optionalFieldOf("data_gained", DataGained.EMPTY).forGetter(EntityDataModel::dataGained),
            EntityAttunement.CODEC.optionalFieldOf("attunement").forGetter(EntityDataModel::attunement))
        .apply(inst, EntityDataModel::new));

    @Deprecated
    public static final Codec<EntityDataModel> OLD_CODEC = RecordCodecBuilder.<EntityDataModel>create(inst -> inst
        .group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(EntityDataModel::entity),
            MiscCodecs.OPTIONAL_ENTITY_TYPE_LIST.optionalFieldOf("variants", List.of()).forGetter(EntityDataModel::variants),
            ComponentSerialization.CODEC.fieldOf("name").forGetter(EntityDataModel::name),
            DisplayData.CODEC.optionalFieldOf("display", DisplayData.DEFAULT).forGetter(EntityDataModel::display),
            Codec.intRange(0, Integer.MAX_VALUE / 20).fieldOf("sim_cost").forGetter(EntityDataModel::simCost),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(EntityDataModel::input),
            OptionalStackCodec.INSTANCE.fieldOf("base_drop").forGetter(EntityDataModel::baseDrop),
            Codec.STRING.fieldOf("trivia").forGetter(EntityDataModel::triviaKey),
            OptionalStackCodec.INSTANCE.listOf().xmap(EntityDataModel::removeEmptyStacks, Function.identity()).fieldOf("fabricator_drops").forGetter(EntityDataModel::fabDrops),
            RequiredData.CODEC.optionalFieldOf("required_data", RequiredData.EMPTY).forGetter(EntityDataModel::requiredData),
            DataGained.CODEC.optionalFieldOf("data_per_kill", DataGained.EMPTY).forGetter(EntityDataModel::dataGained),
            EntityAttunement.CODEC.optionalFieldOf("attunement").forGetter(EntityDataModel::attunement))
        .apply(inst, EntityDataModel::fromOld)).validate(EntityDataModel::validate);

    public static final Codec<EntityDataModel> CODEC = Codec.either(NEW_CODEC, OLD_CODEC).xmap(Either::unwrap, Either::left);

    public EntityDataModel(EntityDataModel other, List<ItemStack> newResults) {
        this(other.entity, other.variants, other.displayName, other.nameColor, other.display, other.simCost, other.input, other.baseDrop, other.triviaKey, newResults, other.requiredData,
            other.dataGained, other.attunement);
    }

    @Deprecated
    public static EntityDataModel fromOld(EntityType<?> entity, List<EntityType<?>> variants, Component name,
        DisplayData display, int simCost, Ingredient input, ItemStack baseDrop, String triviaKey,
        List<ItemStack> fabDrops, RequiredData requiredData, DataGained dataPerKill, Optional<EntityAttunement> attunement) {

        TextColor nameColor = name.getStyle().getColor();
        Preconditions.checkNotNull(nameColor, "[Legacy Deserialization]: A data model must supply a color for the name component.");

        Optional<Component> displayName = Optional.of(name);
        if (name.getContents() instanceof TranslatableContents tc && tc.getKey().equals(entity.getDescriptionId())) {
            displayName = Optional.empty();
        }

        return new EntityDataModel(entity, variants, displayName, nameColor, display, simCost, input, baseDrop, triviaKey, fabDrops, requiredData, dataPerKill, attunement);
    }

    @Override
    public Component name() {
        return this.displayName.orElse(this.entity.getDescription()).copy().withStyle(s -> s.withColor(this.nameColor));
    }

    @Override
    public Codec<? extends EntityDataModel> getCodec() {
        return CODEC;
    }

    @Deprecated
    public Stream<EntityType<?>> entityAndVariants() {
        return Stream.concat(Stream.of(this.entity), this.variants.stream());
    }

    public boolean hasAttunement() {
        return this.attunement.isPresent();
    }

    public boolean attunesTo(ServerPlayer player, Entity entity) {
        return (entity.getType() == this.entity || this.variants.contains(entity.getType()))
            && (!this.hasAttunement() || this.attunement.get().matches(player, entity));
    }

    public static DataResult<EntityDataModel> validate(EntityDataModel model) {
        if (model.name().getStyle().getColor() == null) {
            return DataResult.error(() -> "A data model must supply a color for the name component.");
        }

        return DataResult.success(model);
    }

    private static List<ItemStack> removeEmptyStacks(List<ItemStack> list) {
        return list.stream().filter(i -> !i.isEmpty()).toList();
    }

    // TODO: Cache these...

    @Override
    public DisplayEntity displayEntity(Level level) {
        return createDisplayEntity(this.entity);
    }

    @Override
    public List<DisplayEntity> displayVariants(Level level) {
        return this.variants.stream()
            .map(this::createDisplayEntity)
            .toList();
    }

    private DisplayEntity createDisplayEntity(EntityType<?> type) {
        return new DisplayEntity(type, this.display.nbt(), this.display.scale(), this.display.xOffset(), this.display.yOffset(), this.display.zOffset());

    }

}
