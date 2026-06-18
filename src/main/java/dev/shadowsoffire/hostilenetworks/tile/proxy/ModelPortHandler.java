package dev.shadowsoffire.hostilenetworks.tile.proxy;

import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity.DataCenterItemHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Exposes the Data Center's 25 model slots through a Model IO Port. Insertion routes through the controller's filter
 * (so only {@code DataModelItem}s are accepted), while extraction calls {@code extractItemInternal} to bypass the
 * controller's menu-side extract block.
 */
public class ModelPortHandler implements IItemHandler {

    private final DataCenterItemHandler delegate;

    public ModelPortHandler(DataCenterItemHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getSlots() {
        return DataCenterTileEntity.MODEL_SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return this.delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.delegate.extractItemInternal(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.delegate.isItemValid(slot, stack);
    }
}
