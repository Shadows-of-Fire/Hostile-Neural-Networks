package dev.shadowsoffire.hostilenetworks.data;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Live instance of a data model parsed from an item stack.
 */
public class DataModelInstance implements TooltipComponent {

    public static final DataModelInstance EMPTY = new DataModelInstance(ItemStack.EMPTY, -1);

    protected final ItemStack stack;
    protected final int slot;
    protected final DynamicHolder<DataModel> model;

    protected int data;
    protected DynamicHolder<ModelTier> tier;

    public DataModelInstance(ItemStack stack, int slot) {
        this.stack = stack;
        this.slot = slot;
        this.model = DataModelItem.getStoredModel(stack);
        this.data = DataModelItem.getData(stack);
        this.tier = ModelTierRegistry.INSTANCE.emptyHolder(); // computed lazily by getTier
    }

    public DataModel getModel() {
        return this.model.get();
    }

    public int getData() {
        return this.data;
    }

    public ModelTier getTier() {
        if (!this.tier.isBound()) {
            this.tier = ModelTierRegistry.getByData(this.model.get(), this.data).asHolder();
        }
        return this.tier.getOptional().orElse(ModelTierRegistry.getMinTier());
    }

    public ModelTier getNextTier() {
        return ModelTierRegistry.next(this.getTier());
    }

    public int getDataPerKill() {
        return HostileConfig.killModelUpgrade ? this.getModel().getDataPerKill(this.getTier()) : 0;
    }

    public int getTierData() {
        return this.getModel().getRequiredData(this.getTier());
    }

    public int getNextDataPerKill() {
        return this.getModel().getDataPerKill(getNextTier());
    }

    public int getNextTierData() {
        return this.getModel().getRequiredData(getNextTier());
    }

    public void setData(int data) {
        this.data = data;
        if (this.data > this.getNextTierData()) {
            this.tier = ModelTierRegistry.next(getTier()).asHolder();
        }
        DataModelItem.setData(this.stack, data);
    }

    public int getSlot() {
        return this.slot;
    }

    public float getAccuracy() {
        if (!HostileConfig.continuousAccuracy || this.getTier().isMax()) {
            return this.getTier().accuracy();
        }

        ModelTier next = this.getNextTier();
        int diff = this.getNextTierData() - this.getTierData();
        float tDiff = next.accuracy() - this.getTier().accuracy();
        return this.getTier().accuracy() + tDiff * (diff - (this.getNextTierData() - this.data)) / diff;
    }

    public int getKillsNeeded() {
        return Mth.ceil((this.getNextTierData() - this.data) / (float) this.getDataPerKill());
    }

    public Entity getEntity(Level level) {
        return this.getEntity(level, 0);
    }

    public Entity getEntity(Level level, int variant) {
        EntityType<?> type = variant == 0 ? this.getModel().entity() : this.getModel().variants().get(variant - 1);
        return ClientEntityCache.computeIfAbsent(type, level, this.getModel().display().nbt());
    }

    public ItemStack getPredictionDrop() {
        return this.getModel().getPredictionDrop();
    }

    public ItemStack getSourceStack() {
        return this.stack;
    }

    public boolean isValid() {
        return this.model.isBound();
    }

}
