package dev.shadowsoffire.hostilenetworks.data;

import java.util.List;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.DataGained;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.hostilenetworks.util.RequiredData;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;

/**
 * Abstract view of a data model. All data models must implement this interface so the Simulation Chamber and Loot Fabricator know how to interact with them.
 */
public sealed interface DataModel extends CodecProvider<DataModel>
    permits BlockDataModel, EntityDataModel {

    /**
     * Returns the display entity for this data model.
     * <p>
     * The display entity is shown in the Data Model's item model, and in the Deep Learner.
     */
    DisplayEntity displayEntity(Level level);

    /**
     * Returns all variant display entities for this data model.
     */
    List<DisplayEntity> displayVariants(Level level);

    Component name();

    TextColor nameColor();

    int simCost();

    Ingredient input();

    ItemStack baseDrop();

    String triviaKey();

    List<ItemStack> fabDrops();

    RequiredData requiredData();

    DataGained dataGained();

    default ItemStack getPredictionDrop() {
        ItemStack stk = new ItemStack(Hostile.Items.PREDICTION);
        DataModelItem.setStoredModel(stk, this);
        return stk;
    }

    /**
     * Returns the required data for this model to be considered at the given tier.
     * <p>
     * This method respects overrides present in {@link #requiredData}.
     * 
     * @param tier The tier being checked.
     * @return The (potentially overridden) amount of data required to be at the target tier.
     */
    default int getRequiredData(ModelTier tier) {
        return this.requiredData().getRequiredData(tier);
    }

    /**
     * Returns the data per kill for this model when at the given tier.
     * <p>
     * This method respects overrides present in {@link #dataPerKill}.
     *
     * @param tier The tier of the model.
     * @return The (potentially overridden) amount of data received per kill.
     */
    default int getDataGained(ModelTier tier) {
        return this.dataGained().getDataGained(tier);
    }

    /**
     * Display names for each variant of this model, used in tooltips.
     * <p>
     * Default returns an empty list. Implementations should populate this with one component per declared variant.
     */
    List<Component> variantNames();

    /**
     * Lang key for the singular world-action verb that grants data to this model — e.g. "kill" for entities, "mine" for blocks.
     * <p>
     * Used in the Deep Learner GUI's "Upgrades to X in N {action}" line, and in tooltips.
     */
    String actionWordKey();

    /**
     * Lang key for the per-action data label shown in item tooltips — e.g. "Data Per Kill" / "Data Per Mine".
     */
    String dataPerActionKey();

    /**
     * The U (horizontal) texture coordinate, within the Deep Learner GUI texture, of this model's 9x9 stat icon column.
     * <p>
     * Each model type that exposes icon-based statistics occupies its own column of three stacked 9x9 icons (entities use
     * the heart / armor / xp column; blocks use a hardness / blast / sound column). A value of {@code -1} disables the
     * stat icons, in which case {@link #getStatistics(Level)} components are rendered as plain text lines instead.
     */
    int statIconColumn();

    /**
     * Returns exactly three statistic components for the Deep Learner's "Statistics" panel.
     *
     * @param level The level used to resolve the statistics (e.g. to instantiate an entity for attribute lookups).
     */
    List<Component> getStatistics(Level level);

}
