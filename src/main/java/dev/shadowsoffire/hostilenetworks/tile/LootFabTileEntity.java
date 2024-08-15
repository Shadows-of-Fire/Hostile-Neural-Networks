package dev.shadowsoffire.hostilenetworks.tile;

import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.placebo.cap.ModifiableEnergyStorage;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots.IDataAutoRegister;
import dev.shadowsoffire.placebo.network.VanillaPacketDispatcher;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class LootFabTileEntity extends BlockEntity implements TickingBlockEntity, IDataAutoRegister {

    protected final FabItemHandler inventory = new FabItemHandler();
    protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.fabPowerCap, HostileConfig.fabPowerCap);
    protected final Object2IntMap<DynamicHolder<DataModel>> savedSelections = new Object2IntOpenHashMap<>();
    protected final SimpleDataSlots data = new SimpleDataSlots();

    protected int runtime = 0;
    protected int currentSel = -1;

    public LootFabTileEntity(BlockPos pos, BlockState state) {
        super(Hostile.TileEntities.LOOT_FABRICATOR.get(), pos, state);
        this.savedSelections.defaultReturnValue(-1);
        this.data.addData(() -> this.runtime, v -> this.runtime = v);
        this.data.addEnergy(this.energy);
    }

    @Override
    public void registerSlots(Consumer<DataSlot> consumer) {
        this.data.register(consumer);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        DynamicHolder<DataModel> dm = DataModelItem.getStoredModel(this.inventory.getStackInSlot(0));
        if (dm.isBound()) {
            int selection = this.getSelectedDrop(dm.get());
            if (this.currentSel != selection) {
                this.currentSel = selection;
                this.runtime = 0;
                return;
            }
            if (selection != -1) {
                if (this.runtime >= 60) {
                    ItemStack out = dm.get().fabDrops().get(selection).copy();
                    if (this.insertInOutput(out, true)) {
                        this.runtime = 0;
                        this.insertInOutput(out, false);
                        this.inventory.getStackInSlot(0).shrink(1);
                        this.setChanged();
                    }
                }
                else {
                    if (this.energy.getEnergyStored() < HostileConfig.fabPowerCost) return;
                    this.energy.setEnergy(this.energy.getEnergyStored() - HostileConfig.fabPowerCost);
                    this.runtime++;
                    this.setChanged();
                }
            }
            else this.runtime = 0;
        }
        else this.runtime = 0;
    }

    protected boolean insertInOutput(ItemStack stack, boolean sim) {
        for (int i = 1; i < 17; i++) {
            stack = this.inventory.insertItemInternal(i, stack, sim);
            if (stack.isEmpty()) return true;
        }
        return false;
    }

    public FabItemHandler getInventory() {
        return this.inventory;
    }

    public void setSelection(DynamicHolder<DataModel> model, int selection) {
        if (selection == -1) this.savedSelections.removeInt(model);
        else this.savedSelections.put(model, Mth.clamp(selection, 0, model.get().fabDrops().size() - 1));
        VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        this.setChanged();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.of(() -> this.inventory).cast();
        if (cap == ForgeCapabilities.ENERGY) return LazyOptional.of(() -> this.energy).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        tag.put("inventory", this.inventory.serializeNBT());
        tag.putInt("energy", this.energy.getEnergyStored());
        tag.putInt("runtime", this.runtime);
        tag.putInt("selection", this.currentSel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readSelections(tag.getCompound("saved_selections"));
        this.inventory.deserializeNBT(tag.getCompound("inventory"));
        this.energy.setEnergy(tag.getInt("energy"));
        this.runtime = tag.getInt("runtime");
        this.currentSel = tag.getInt("selection");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, t -> ((LootFabTileEntity) t).writeSync());
    }

    private CompoundTag writeSync() {
        CompoundTag tag = new CompoundTag();
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.readSelections(pkt.getTag().getCompound("saved_selections"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        return tag;
    }

    private CompoundTag writeSelections(CompoundTag tag) {
        for (Object2IntMap.Entry<DynamicHolder<DataModel>> e : this.savedSelections.object2IntEntrySet()) {
            tag.putInt(e.getKey().getId().toString(), e.getIntValue());
        }
        return tag;
    }

    private void readSelections(CompoundTag tag) {
        this.savedSelections.clear();
        for (String s : tag.getAllKeys()) {
            DynamicHolder<DataModel> dm = DataModelRegistry.INSTANCE.holder(new ResourceLocation(s));
            this.savedSelections.put(dm, tag.getInt(s));
        }
    }

    public int getEnergyStored() {
        return this.energy.getEnergyStored();
    }

    public int getRuntime() {
        return this.runtime;
    }

    /**
     * Returns the index of the selected drop for a given data model.
     * 
     * @param model The model to check
     * @return The index of the selected drop, or -1 if no selection is present.
     */
    public int getSelectedDrop(DataModel model) {
        if (model == null) return -1;
        int index = this.savedSelections.getInt(DataModelRegistry.INSTANCE.holder(model));
        if (index >= model.fabDrops().size()) return -1;
        return index;
    }

    public class FabItemHandler extends InternalItemHandler {

        public FabItemHandler() {
            super(17);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.getItem() == Hostile.Items.PREDICTION.get();
            return true;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot > 0) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            LootFabTileEntity.this.setChanged();
        }
    }

}
