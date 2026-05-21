package dev.shadowsoffire.hostilenetworks.tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.FabSelection;
import dev.shadowsoffire.hostilenetworks.util.FabSelection.ProductionMode;
import dev.shadowsoffire.hostilenetworks.util.RedstoneState;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.placebo.cap.ModifiableEnergyStorage;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots.IDataAutoRegister;
import dev.shadowsoffire.placebo.network.VanillaPacketDispatcher;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class LootFabTileEntity extends BlockEntity implements TickingBlockEntity, IDataAutoRegister {

    protected final FabItemHandler inventory = new FabItemHandler();
    protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.fabPowerCap, HostileConfig.fabPowerCap);
    protected final Map<DynamicHolder<DataModel>, FabSelection> savedSelections = new HashMap<>();
    protected final SimpleDataSlots data = new SimpleDataSlots();

    protected int runtime = 0;
    protected int currentSel = -1;
    protected RedstoneState redstoneState = RedstoneState.IGNORED;

    public LootFabTileEntity(BlockPos pos, BlockState state) {
        super(Hostile.TileEntities.LOOT_FABRICATOR, pos, state);
        this.data.addData(() -> this.runtime, v -> this.runtime = v);
        this.data.addData(() -> this.redstoneState.ordinal(), v -> this.redstoneState = RedstoneState.values()[v]);
        this.data.addEnergy(this.energy);
        this.energy.setMaxExtract(0);
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
                // Redstone control pauses progression and production without resetting the runtime - so the player
                // can freely edit a queue without the fabricator consuming a prediction mid-edit.
                if (!this.redstoneState.matches(level.hasNeighborSignal(this.worldPosition))) return;
                if (this.runtime >= 60) {
                    ItemStack out = dm.get().fabDrops().get(selection).copy();
                    if (this.insertInOutput(out, true)) {
                        this.runtime = 0;
                        this.insertInOutput(out, false);
                        this.inventory.getStackInSlot(0).shrink(1);
                        this.advanceQueue(dm);
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

    public IEnergyStorage getEnergy() {
        return this.energy;
    }

    public Map<DynamicHolder<DataModel>, FabSelection> getSelections() {
        return this.savedSelections;
    }

    public void setSelections(Map<DynamicHolder<DataModel>, FabSelection> selections) {
        this.savedSelections.clear();
        this.savedSelections.putAll(selections);
        this.sync();
    }

    /**
     * Returns the {@link FabSelection} configured for the given model, or {@link FabSelection#EMPTY} if none exists.
     */
    public FabSelection getSelection(DataModel model) {
        return this.savedSelections.getOrDefault(DataModelRegistry.INSTANCE.holder(model), FabSelection.EMPTY);
    }

    /**
     * Toggles the model between Fixed and Queue production modes, carrying the currently-resolved drop across the swap.
     */
    public void cycleMode(DynamicHolder<DataModel> model) {
        FabSelection sel = this.savedSelections.getOrDefault(model, FabSelection.EMPTY);
        FabSelection updated;
        if (sel.mode().next() == ProductionMode.QUEUE) {
            // The Fixed pick (if any) becomes the queue's first entry.
            updated = new FabSelection(ProductionMode.QUEUE, List.copyOf(sel.entries()), 0);
        }
        else {
            int current = sel.current();
            updated = current == -1 ? FabSelection.EMPTY : FabSelection.fixed(current);
        }
        this.savedSelections.put(model, updated);
        this.runtime = 0;
        this.sync();
    }

    /**
     * Sets the model's Fixed-mode drop. An index of {@code -1} clears the model's selection entirely.
     */
    public void setFixedDrop(DynamicHolder<DataModel> model, int index) {
        if (index == -1) {
            this.savedSelections.remove(model);
        }
        else {
            this.savedSelections.put(model, FabSelection.fixed(Mth.clamp(index, 0, model.get().fabDrops().size() - 1)));
        }
        this.sync();
    }

    /**
     * Appends a drop index to the model's production queue, switching it to Queue mode if needed.
     */
    public void appendToQueue(DynamicHolder<DataModel> model, int index) {
        DataModel dm = model.get();
        if (dm == null || index < 0 || index >= dm.fabDrops().size()) return;
        FabSelection sel = this.savedSelections.getOrDefault(model, FabSelection.EMPTY);
        List<Integer> entries = new ArrayList<>(sel.entries());
        entries.add(index);
        this.savedSelections.put(model, new FabSelection(ProductionMode.QUEUE, entries, sel.cursor()));
        this.sync();
    }

    /**
     * Empties the model's production queue while keeping it in Queue mode (ready to receive new appends).
     */
    public void clearQueue(DynamicHolder<DataModel> model) {
        FabSelection sel = this.savedSelections.get(model);
        if (sel == null || sel.mode() != ProductionMode.QUEUE) return;
        this.savedSelections.put(model, new FabSelection(ProductionMode.QUEUE, List.of(), 0));
        this.sync();
    }

    /**
     * Removes the queue entry at the given position, keeping the cursor pointing at the same upcoming entry.
     */
    public void removeFromQueue(DynamicHolder<DataModel> model, int pos) {
        FabSelection sel = this.savedSelections.get(model);
        if (sel == null || pos < 0 || pos >= sel.entries().size()) return;
        List<Integer> entries = new ArrayList<>(sel.entries());
        entries.remove(pos);
        int cursor = sel.cursor();
        if (pos < cursor) cursor--;
        cursor = entries.isEmpty() ? 0 : Math.floorMod(cursor, entries.size());
        this.savedSelections.put(model, new FabSelection(ProductionMode.QUEUE, entries, cursor));
        this.sync();
    }

    /**
     * After a Queue-mode model fabricates an item, steps its cursor to the next entry.
     */
    private void advanceQueue(DynamicHolder<DataModel> model) {
        FabSelection sel = this.savedSelections.get(model);
        if (sel != null && sel.mode() == ProductionMode.QUEUE) {
            this.savedSelections.put(model, sel.advanced());
            VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        }
    }

    private void sync() {
        VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        this.setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        tag.put("inventory", this.inventory.serializeNBT(regs));
        tag.putInt("energy", this.energy.getEnergyStored());
        tag.putInt("runtime", this.runtime);
        tag.putInt("selection", this.currentSel);
        tag.putInt("redstoneState", this.redstoneState.ordinal());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        this.readSelections(tag.getCompound("saved_selections"));
        this.inventory.deserializeNBT(regs, tag.getCompound("inventory"));
        this.energy.setEnergy(tag.getInt("energy"));
        this.runtime = tag.getInt("runtime");
        this.currentSel = tag.getInt("selection");
        this.redstoneState = RedstoneState.values()[tag.getInt("redstoneState")];
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (be, regs) -> ((LootFabTileEntity) be).writeSync());
    }

    private CompoundTag writeSync() {
        CompoundTag tag = new CompoundTag();
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider regs) {
        this.readSelections(pkt.getTag().getCompound("saved_selections"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs);
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        return tag;
    }

    private CompoundTag writeSelections(CompoundTag tag) {
        for (Map.Entry<DynamicHolder<DataModel>, FabSelection> e : this.savedSelections.entrySet()) {
            Tag encoded = FabSelection.CODEC.encodeStart(NbtOps.INSTANCE, e.getValue()).getOrThrow();
            tag.put(e.getKey().getId().toString(), encoded);
        }
        return tag;
    }

    private void readSelections(CompoundTag tag) {
        this.savedSelections.clear();
        for (String s : tag.getAllKeys()) {
            DynamicHolder<DataModel> dm = DataModelRegistry.INSTANCE.holder(ResourceLocation.tryParse(s));
            Tag value = tag.get(s);
            FabSelection sel;
            if (value instanceof IntTag intTag) {
                // Legacy format: a bare int index, pre-dating production modes.
                sel = FabSelection.fixed(intTag.getAsInt());
            }
            else {
                sel = FabSelection.CODEC.parse(NbtOps.INSTANCE, value).result().orElse(FabSelection.EMPTY);
            }
            this.savedSelections.put(dm, sel);
        }
    }

    public int getEnergyStored() {
        return this.energy.getEnergyStored();
    }

    public int getRuntime() {
        return this.runtime;
    }

    public void setRedstoneState(RedstoneState state) {
        this.redstoneState = state;
        this.setChanged();
    }

    public RedstoneState getRedstoneState() {
        return this.redstoneState;
    }

    /**
     * Returns the index of the selected drop for a given data model.
     * 
     * @param model The model to check
     * @return The index of the selected drop, or -1 if no selection is present.
     */
    public int getSelectedDrop(DataModel model) {
        if (model == null) return -1;
        FabSelection sel = this.savedSelections.get(DataModelRegistry.INSTANCE.holder(model));
        if (sel == null) return -1;
        int index = sel.current();
        if (index < 0 || index >= model.fabDrops().size()) return -1;
        return index;
    }

    public class FabItemHandler extends InternalItemHandler {

        public FabItemHandler() {
            super(17);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.is(Hostile.Items.PREDICTION);
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

        public NonNullList<ItemStack> getItems() {
            return this.stacks;
        }
    }

}
