package dev.shadowsoffire.hostilenetworks.data;

import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import dev.shadowsoffire.hostilenetworks.util.DisplayEntity;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;

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

    @Deprecated
    public int getDataGained() {
        return HostileConfig.killModelUpgrade ? this.getModel().getDataGained(this.getTier()) : 0;
    }

    public int getTierData() {
        return this.getModel().getRequiredData(this.getTier());
    }

    @Deprecated
    public int getNextDataPerKill() {
        if (this.getModel() instanceof EntityDataModel entityModel) {
            return entityModel.getDataGained(getNextTier());
        }
        return -1;
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

    public Component getAccuracyComponent() {
        Component numeric = Component.literal(IAttributeExtension.FORMAT.format(100 * this.getAccuracy()) + "%").withColor(this.getTier().colorValue());
        return Component.translatable("hostilenetworks.gui.accuracy", numeric);
    }

    public int getKillsNeeded() {
        return Mth.ceil((this.getNextTierData() - this.data) / (float) this.getDataGained());
    }

    public DisplayEntity getDisplayEntity(Level level, int variant) {
        return variant == 0 ? this.getModel().displayEntity(level) : this.getModel().displayVariants(level).get(variant - 1);
    }

    @Deprecated
    public Entity getEntity(Level level) {
        return this.getEntity(level, 0);
    }

    @Deprecated
    public Entity getEntity(Level level, int variant) {
        DisplayEntity display = this.getDisplayEntity(level, variant);
        return ClientEntityCache.computeIfAbsent(display, level);
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
