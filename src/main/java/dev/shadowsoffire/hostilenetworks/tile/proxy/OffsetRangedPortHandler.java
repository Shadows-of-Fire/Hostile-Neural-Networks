package dev.shadowsoffire.hostilenetworks.tile.proxy;

import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity.DataCenterItemHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Pass-through view of a contiguous slot range on the controller's inventory. The underlying handler enforces all
 * per-slot insert/extract semantics; this wrapper only translates local indices to absolute ones. Used for the Input
 * and Output IO Port modes, where the existing handler already permits exactly the behaviour we want.
 */
public class OffsetRangedPortHandler implements IItemHandler {

    private final DataCenterItemHandler delegate;
    private final int offset;
    private final int count;

    public OffsetRangedPortHandler(DataCenterItemHandler delegate, int offset, int count) {
        this.delegate = delegate;
        this.offset = offset;
        this.count = count;
    }

    @Override
    public int getSlots() {
        return this.count;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.delegate.getStackInSlot(this.offset + slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return this.delegate.insertItem(this.offset + slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return this.delegate.extractItem(this.offset + slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.delegate.getSlotLimit(this.offset + slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.delegate.isItemValid(this.offset + slot, stack);
    }
}
