package dev.shadowsoffire.hostilenetworks.tile;

import org.jetbrains.annotations.Nullable;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.block.DataCenterIOPortBlock;
import dev.shadowsoffire.hostilenetworks.tile.proxy.ModelPortHandler;
import dev.shadowsoffire.hostilenetworks.tile.proxy.OffsetRangedPortHandler;
import dev.shadowsoffire.hostilenetworks.util.IOPortMode;
import dev.shadowsoffire.placebo.network.VanillaPacketDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

public class DataCenterIOPortTileEntity extends BlockEntity {

    @Nullable
    protected BlockPos ownerPos = null;

    public DataCenterIOPortTileEntity(BlockPos pos, BlockState state) {
        super(Hostile.TileEntities.IO_PORT, pos, state);
    }

    public IOPortMode getMode() {
        return this.getBlockState().getValue(DataCenterIOPortBlock.MODE);
    }

    @Nullable
    public BlockPos getOwnerPos() {
        return this.ownerPos;
    }

    public void setOwner(BlockPos pos) {
        if (pos.equals(this.ownerPos)) return;
        this.ownerPos = pos.immutable();
        this.sync();
        if (this.level != null) this.level.invalidateCapabilities(this.worldPosition);
    }

    public void clearOwner() {
        if (this.ownerPos == null) return;
        this.ownerPos = null;
        this.sync();
        if (this.level != null) this.level.invalidateCapabilities(this.worldPosition);
    }

    /** Resolves to the owning controller iff it's loaded and its shell is currently valid; otherwise {@code null}. */
    @Nullable
    public DataCenterTileEntity resolveOwner() {
        if (this.ownerPos == null || this.level == null) return null;
        if (this.level.getBlockEntity(this.ownerPos) instanceof DataCenterTileEntity dc && dc.isShellValid()) return dc;
        return null;
    }

    private void sync() {
        VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        this.setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        if (this.ownerPos != null) tag.putLong("owner", this.ownerPos.asLong());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        this.ownerPos = tag.contains("owner") ? BlockPos.of(tag.getLong("owner")) : null;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (be, regs) -> ((DataCenterIOPortTileEntity) be).writeSync());
    }

    public IEnergyStorage getEnergyHandler(Direction side) {
        if (this.getMode() != IOPortMode.ENERGY) return null;
        DataCenterTileEntity owner = this.resolveOwner();
        return owner != null ? owner.getEnergy() : null;
    }

    public IItemHandler getItemHandler(Direction side) {
        DataCenterTileEntity owner = this.resolveOwner();
        if (owner == null) return null;
        return switch (this.getMode()) {
            case MODELS -> new ModelPortHandler(owner.getInventory());
            case INPUTS -> new OffsetRangedPortHandler(owner.getInventory(), DataCenterTileEntity.INPUT_START, DataCenterTileEntity.INPUT_SLOTS);
            case OUTPUTS -> new OffsetRangedPortHandler(owner.getInventory(), DataCenterTileEntity.OUTPUT_START, DataCenterTileEntity.OUTPUT_SLOTS);
            case ENERGY -> null;
        };
    }

    private CompoundTag writeSync() {
        CompoundTag tag = new CompoundTag();
        if (this.ownerPos != null) tag.putLong("owner", this.ownerPos.asLong());
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider regs) {
        this.readSyncTag(pkt.getTag());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs);
        if (this.ownerPos != null) tag.putLong("owner", this.ownerPos.asLong());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider regs) {
        super.handleUpdateTag(tag, regs);
        this.readSyncTag(tag);
    }

    private void readSyncTag(CompoundTag tag) {
        this.ownerPos = tag.contains("owner") ? BlockPos.of(tag.getLong("owner")) : null;
    }
}
