package dev.shadowsoffire.hostilenetworks.tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.util.Templates;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.FabSelection;
import dev.shadowsoffire.hostilenetworks.util.FabSelection.ProductionMode;
import dev.shadowsoffire.hostilenetworks.util.RedstoneState;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.hostilenetworks.util.ModifiableEnergyStorage;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots.IDataAutoRegister;
import dev.shadowsoffire.placebo.network.VanillaPacketDispatcher;
import dev.shadowsoffire.placebo.dynreg.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

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
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory.getItems());
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        DynamicHolder<DataModel> dm = DataModelItem.getStoredModel(ItemUtil.getStack(this.inventory, 0));
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
                    ItemStack out = Templates.create(dm.get().fabDrops().get(selection));
                    if (this.tryInsertInOutput(out, true)) {
                        this.runtime = 0;
                        this.tryInsertInOutput(out, false);
                        try (Transaction tx = Transaction.openRoot()) {
                            // The public extract blocks slot 0 (menu-side); consume via the internal path like 1.21's shrink.
                            this.inventory.extractInternal(0, this.inventory.getResource(0), 1, tx);
                            tx.commit();
                        }
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

    protected boolean tryInsertInOutput(ItemStack stack, boolean sim) {
        int remaining = stack.getCount();
        ItemResource res = ItemResource.of(stack);
        try (Transaction tx = Transaction.openRoot()) {
            for (int i = 1; i < 17 && remaining > 0; i++) {
                remaining -= this.inventory.insertInternal(i, res, remaining, tx);
            }
            if (remaining == 0 && !sim) tx.commit();
        }
        return remaining == 0;
    }

    public FabItemHandler getInventory() {
        return this.inventory;
    }

    public EnergyHandler getEnergy() {
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

    public void setFixedDrop(DynamicHolder<DataModel> model, int index) {
        if (index == -1) {
            this.savedSelections.remove(model);
        }
        else {
            this.savedSelections.put(model, FabSelection.fixed(Mth.clamp(index, 0, model.get().fabDrops().size() - 1)));
        }
        this.sync();
    }

    public void appendToQueue(DynamicHolder<DataModel> model, int index) {
        DataModel dm = model.get();
        if (dm == null || index < 0 || index >= dm.fabDrops().size()) return;
        FabSelection sel = this.savedSelections.getOrDefault(model, FabSelection.EMPTY);
        List<Integer> entries = new ArrayList<>(sel.entries());
        entries.add(index);
        this.savedSelections.put(model, new FabSelection(ProductionMode.QUEUE, entries, sel.cursor()));
        this.sync();
    }

    public void clearQueue(DynamicHolder<DataModel> model) {
        FabSelection sel = this.savedSelections.get(model);
        if (sel == null || sel.mode() != ProductionMode.QUEUE) return;
        this.savedSelections.put(model, new FabSelection(ProductionMode.QUEUE, List.of(), 0));
        this.sync();
    }

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
    public void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("saved_selections", CompoundTag.CODEC, this.writeSelections(new CompoundTag()));
        this.inventory.serialize(out);
        out.putInt("energy", this.energy.getEnergyStored());
        out.putInt("runtime", this.runtime);
        out.putInt("selection", this.currentSel);
        out.putInt("redstoneState", this.redstoneState.ordinal());
    }

    @Override
    public void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.readSelections(in.read("saved_selections", CompoundTag.CODEC).orElse(new CompoundTag()));
        this.inventory.deserialize(in);
        this.energy.setEnergy(in.getIntOr("energy", 0));
        this.runtime = in.getIntOr("runtime", 0);
        this.currentSel = in.getIntOr("selection", -1);
        this.redstoneState = RedstoneState.values()[in.getIntOr("redstoneState", 0)];
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
    public void onDataPacket(Connection net, ValueInput in) {
        this.readSelections(in.read("saved_selections", CompoundTag.CODEC).orElse(new CompoundTag()));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = super.getUpdateTag(regs);
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        return tag;
    }

    @Override
    public void handleUpdateTag(ValueInput in) {
        super.handleUpdateTag(in);
        this.readSelections(in.read("saved_selections", CompoundTag.CODEC).orElse(new CompoundTag()));
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
        for (String s : tag.keySet()) {
            DynamicHolder<DataModel> dm = DataModelRegistry.INSTANCE.holder(Identifier.tryParse(s));
            Tag value = tag.get(s);
            if (value == null) continue;
            FabSelection sel;
            if (value instanceof IntTag intTag) {
                sel = FabSelection.fixed(intTag.intValue());
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
        public boolean isValid(int index, ItemResource resource) {
            if (index == 0) return resource.is(Hostile.Items.PREDICTION);
            return true;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index > 0) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index == 0) return 0;
            return super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack oldContents) {
            LootFabTileEntity.this.setChanged();
        }

        public NonNullList<ItemStack> getItems() {
            return this.stacks;
        }
    }

}
