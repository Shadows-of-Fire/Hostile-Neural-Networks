package dev.shadowsoffire.hostilenetworks.tile.proxy;

import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity.DataCenterItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes the Data Center's 25 model slots through a Model IO Port. Insertion routes through the controller's filter
 * (so only {@code DataModelItem}s are accepted), while extraction calls {@code extractInternal} to bypass the
 * controller's menu-side extract block.
 */
public class ModelPortHandler implements ResourceHandler<ItemResource> {

    private final DataCenterItemHandler delegate;

    public ModelPortHandler(DataCenterItemHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return DataCenterTileEntity.MODEL_SLOTS;
    }

    @Override
    public ItemResource getResource(int index) {
        return this.delegate.getResource(index);
    }

    @Override
    public long getAmountAsLong(int index) {
        return this.delegate.getAmountAsLong(index);
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        return this.delegate.getCapacityAsLong(index, resource);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return this.delegate.isValid(index, resource);
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return this.delegate.insert(index, resource, amount, transaction);
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return this.delegate.extractInternal(index, resource, amount, transaction);
    }
}
