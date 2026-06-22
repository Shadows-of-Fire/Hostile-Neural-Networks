package dev.shadowsoffire.hostilenetworks.data;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.shadowsoffire.hostilenetworks.util.DataGained;
import dev.shadowsoffire.hostilenetworks.util.DisplayData;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.hostilenetworks.util.DisplayableBlock;
import dev.shadowsoffire.hostilenetworks.util.RequiredData;
import dev.shadowsoffire.placebo.json.OptionalStackCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Stores all of the information representing an individual Data Model.
 */
public record BlockDataModel(DisplayableBlock block, List<DisplayableBlock> variants, Optional<Component> displayName,
    TextColor nameColor, DisplayData display, int simCost, Ingredient input, ItemStack baseDrop, String triviaKey,
    List<ItemStack> fabDrops, RequiredData requiredData, DataGained dataGained, Optional<BlockAttunement> attunement,
    List<LootItemCondition> upgradeConditions) implements DataModel {

    /** Formats block hardness / blast resistance, trimming trailing zeroes (e.g. {@code 1.50} -> {@code 1.5}, {@code 1200.0} -> {@code 1200}). */
    private static final DecimalFormat STAT_FORMAT = new DecimalFormat("0.##");

    public static final Codec<BlockDataModel> CODEC = RecordCodecBuilder.create(inst -> inst
        .group(
            DisplayableBlock.CODEC.fieldOf("block").forGetter(BlockDataModel::block),
            DisplayableBlock.CODEC.listOf().optionalFieldOf("variants", List.of()).forGetter(BlockDataModel::variants),
            ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(BlockDataModel::displayName),
            TextColor.CODEC.fieldOf("name_color").forGetter(BlockDataModel::nameColor),
            DisplayData.CODEC.optionalFieldOf("display", DisplayData.DEFAULT).forGetter(BlockDataModel::display),
            Codec.intRange(0, Integer.MAX_VALUE / 20).fieldOf("sim_cost").forGetter(BlockDataModel::simCost),
            Ingredient.CODEC_NONEMPTY.fieldOf("input").forGetter(BlockDataModel::input),
            OptionalStackCodec.INSTANCE.fieldOf("base_drop").forGetter(BlockDataModel::baseDrop),
            Codec.STRING.fieldOf("trivia").forGetter(BlockDataModel::triviaKey),
            OptionalStackCodec.INSTANCE.listOf().xmap(BlockDataModel::removeEmptyStacks, Function.identity()).fieldOf("fabricator_drops").forGetter(BlockDataModel::fabDrops),
            RequiredData.CODEC.optionalFieldOf("required_data", RequiredData.EMPTY).forGetter(BlockDataModel::requiredData),
            DataGained.CODEC.optionalFieldOf("data_gained", DataGained.EMPTY).forGetter(BlockDataModel::dataGained),
            BlockAttunement.CODEC.optionalFieldOf("attunement").forGetter(BlockDataModel::attunement),
            LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("upgrade_conditions", List.of()).forGetter(BlockDataModel::upgradeConditions))
        .apply(inst, BlockDataModel::new));

    public BlockDataModel(BlockDataModel other, List<ItemStack> newResults) {
        this(other.block, other.variants, other.displayName, other.nameColor, other.display, other.simCost, other.input, other.baseDrop, other.triviaKey, newResults, other.requiredData,
            other.dataGained, other.attunement, other.upgradeConditions);
    }

    /**
     * Whether mining this block should grant data to the model, evaluated against the {@link LootContext} of the break.
     * <p>
     * Returns {@code true} when no {@code upgrade_conditions} are configured (the default); otherwise every condition
     * must pass. Used to gate blocks that should only upgrade situationally — e.g. ores that must not upgrade when mined
     * with Silk Touch.
     */
    public boolean canUpgrade(LootContext ctx) {
        return this.upgradeConditions.isEmpty() || this.upgradeConditions.stream().allMatch(c -> c.test(ctx));
    }

    @Override
    public Component name() {
        return this.displayName.orElse(this.block.block().getName()).copy().withStyle(s -> s.withColor(this.nameColor));
    }

    @Override
    public List<Component> variantNames() {
        return this.variants.stream().map(b -> b.displayStack().getHoverName()).toList();
    }

    @Override
    public String actionWordKey() {
        return "hostilenetworks.gui.action.mine";
    }

    @Override
    public String dataPerActionKey() {
        return "hostilenetworks.info.dpm";
    }

    @Override
    public int statIconColumn() {
        return 96;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Block models report Hardness ({@link net.minecraft.world.level.block.state.BlockBehaviour#defaultDestroyTime()}),
     * Blast Resistance ({@link Block#getExplosionResistance()}), and Sound Type, paired with the pickaxe / blast / note
     * icon column. Values are bare, mirroring how entity models pair bare numbers with the heart / armor / xp icons.
     */
    @Override
    @SuppressWarnings("deprecation")
    public List<Component> getStatistics(Level level) {
        Block target = this.block.block();
        SoundType sound = target.defaultBlockState().getSoundType();
        return List.of(
            Component.literal(STAT_FORMAT.format(target.defaultDestroyTime())),
            Component.literal(STAT_FORMAT.format(target.getExplosionResistance())),
            soundTypeName(sound));
    }

    /**
     * Resolves a display name for a {@link SoundType} by deriving a translation key from its break sound's registry id.
     */
    private static Component soundTypeName(SoundType sound) {
        ResourceLocation breakSound = BuiltInRegistries.SOUND_EVENT.getKey(sound.getBreakSound());
        String key = "hostilenetworks.sound_type." + breakSound.toString();
        return Component.translatable(key);
    }

    @Override
    public Codec<? extends BlockDataModel> getCodec() {
        return CODEC;
    }

    public boolean hasAttunement() {
        return this.attunement.isPresent();
    }

    public boolean attunesTo(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return (state.getBlock() == this.block.block() || this.variants.stream().anyMatch(v -> state.getBlock() == v.block()))
            && (!this.hasAttunement() || this.attunement.get().matches(level, pos));
    }

    private static List<ItemStack> removeEmptyStacks(List<ItemStack> list) {
        return list.stream().filter(i -> !i.isEmpty()).toList();
    }

    // TODO: Cache these...

    @Override
    public DisplayEntity displayEntity(Level level) {
        return createDisplayEntity(level, this.block);
    }

    @Override
    public List<DisplayEntity> displayVariants(Level level) {
        return this.variants.stream()
            .map(b -> createDisplayEntity(level, b))
            .toList();
    }

    private DisplayEntity createDisplayEntity(Level level, DisplayableBlock block) {
        EntityType<?> entity = EntityType.ITEM;
        CompoundTag tag = new CompoundTag();
        tag.put("Item", block.displayStack().save(level.registryAccess()));
        tag.merge(this.display.nbt());
        return new DisplayEntity(entity, tag, this.display.scale(), this.display.xOffset(), this.display.yOffset(), this.display.zOffset());
    }

}
