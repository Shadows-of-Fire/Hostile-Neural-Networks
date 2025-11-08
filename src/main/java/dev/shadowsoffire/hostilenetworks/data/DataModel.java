package dev.shadowsoffire.hostilenetworks.data;

import java.util.List;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.EntityDataModel.RequiredData;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Abstract view of a data model. All data models must implement this interface so the Simulation Chamber and Loot Fabricator know how to interact with them.
 */
public interface DataModel extends CodecProvider<DataModel> {

    /**
     * Returns the display entity for this data model.
     * <p>
     * The display entity is shown in the Data Model's item model, and in the Deep Learner.
     */
    DisplayEntity displayEntity();

    /**
     * Returns all variant display entities for this data model.
     */
    List<DisplayEntity> displayVariants();

    Component name();

    int simCost();

    Ingredient input();

    ItemStack baseDrop();

    String triviaKey();

    List<ItemStack> fabDrops();

    RequiredData requiredData();

    /**
     * TODO (Breaking): Update json format to have the color be separate from the name.
     */
    default int getNameColor() {
        return this.name().getStyle().getColor().getValue();
    }

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

}
