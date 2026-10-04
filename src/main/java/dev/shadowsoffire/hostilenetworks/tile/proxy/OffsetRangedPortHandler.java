package dev.shadowsoffire.hostilenetworks.tile.proxy;

import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity.DataCenterItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Pass-through view of a contiguous slot range on the controller's inventory. The underlying handler enforces all
 * per-slot insert/extract semantics; this wrapper only translates local indices to absolute ones. Used for the Input
 * and Output IO Port modes, where the existing handler already permits exactly the behaviour we want.
 */
public class OffsetRangedPortHandler implements ResourceHandler<ItemResource> {

    private final DataCenterItemHandler delegate;
    private final int offset;
    private final int count;

    public OffsetRangedPortHandler(DataCenterItemHandler delegate, int offset, int count) {
        this.delegate = delegate;
        this.offset = offset;
        this.count = count;
    }

    @Override
    public int size() {
        return this.count;
    }

    @Override
    public ItemResource getResource(int index) {
        return this.delegate.getResource(this.offset + index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return this.delegate.getAmountAsLong(this.offset + index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return this.delegate.getCapacityAsLong(this.offset + index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return this.delegate.isValid(this.offset + index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return this.delegate.insert(this.offset + index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return this.delegate.extract(this.offset + index, resource, amount, transaction);
    }
}
