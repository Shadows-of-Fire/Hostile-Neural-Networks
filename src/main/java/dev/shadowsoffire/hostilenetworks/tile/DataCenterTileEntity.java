package dev.shadowsoffire.hostilenetworks.tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.util.Templates;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.multiblock.DataCenterShell;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.FailureState;
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
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;

/**
 * Controller BE for the Data Center multiblock. Runs up to {@value #MODEL_SLOTS} parallel sim loops + per-model
 * {@link FabSelection} production queues. See {@link DataCenterShell} for the 7×7×7 geometry.
 */
public class DataCenterTileEntity extends BlockEntity implements TickingBlockEntity, IDataAutoRegister {

    public static final int MODEL_SLOTS = 25;
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOTS = 16;
    public static final int TOTAL_SLOTS = MODEL_SLOTS + INPUT_SLOTS + OUTPUT_SLOTS;
    public static final int INPUT_START = MODEL_SLOTS;
    public static final int OUTPUT_START = MODEL_SLOTS + INPUT_SLOTS;
    public static final int RUNTIME_TICKS = 300;
    public static final int SHELL_RECHECK_INTERVAL = 20;
    public static final int DISPLAY_SLOT_COUNT = 12;

    protected final DataCenterItemHandler inventory = new DataCenterItemHandler();
    protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.dataCenterPowerCap, HostileConfig.dataCenterPowerCap);
    protected final Map<DynamicHolder<DataModel>, FabSelection> savedSelections = new HashMap<>();
    protected final SimpleDataSlots data = new SimpleDataSlots();

    protected final int[] runtimes = new int[MODEL_SLOTS];
    protected final int[] predictionSuccess = new int[MODEL_SLOTS];
    protected final FailureState[] failStates = Util.make(new FailureState[MODEL_SLOTS], arr -> Arrays.fill(arr, FailureState.NONE));

    /**
     * Without transactions, validating multi-slot insertions is impossible.
     * So if an insertion fails, we mark a model as "buffering" and lock it until the entire (shared) buffer clears.
     */
    protected final boolean[] buffering = new boolean[MODEL_SLOTS];
    protected final List<ItemStack> bufferedOutputs = new ArrayList<>();

    protected RedstoneState redstoneState = RedstoneState.IGNORED;
    protected boolean shellValid = false;
    protected int shellRecheckTimer = 0;
    protected DataCenterShell.Layout cachedLayout = null;

    protected byte ejectDirIndex = 0;

    /** Bitmask of model slots with {@code runtimes[i] > 0}. Synced so the BER can pick entities without an open GUI. */
    protected int activeSlotsMask = 0;

    // Client-only BER state, generated lazily on first render and mutated only on the client thread.
    public float[][] displaySlotPositions = null;
    public float[] displaySlotPhaseOffsets = null;
    public final int[] displaySlotCycleIdx = Util.make(new int[DISPLAY_SLOT_COUNT], arr -> Arrays.fill(arr, Integer.MIN_VALUE));
    public final int[] displaySlotEntityIdx = Util.make(new int[DISPLAY_SLOT_COUNT], arr -> Arrays.fill(arr, -1));

    /** Force-loaded shell chunks, owned by {@code worldPosition} in {@link Hostile.Tickets#DATA_CENTER}. */
    protected final Set<Long> forcedChunks = new HashSet<>();

    /** IO Port positions in this controller's shell, refreshed on each {@link #recheckShell} pass. */
    protected final Set<BlockPos> ownedPorts = new HashSet<>();

    public DataCenterTileEntity(BlockPos pos, BlockState state) {
        super(Hostile.TileEntities.DATA_CENTER, pos, state);
        for (int i = 0; i < MODEL_SLOTS; i++) {
            final int idx = i;
            this.data.addData(() -> this.runtimes[idx], v -> this.runtimes[idx] = v);
            this.data.addData(() -> this.failStates[idx].ordinal(), v -> this.failStates[idx] = FailureState.BY_ID.apply(v));
        }
        this.data.addData(() -> this.redstoneState.ordinal(), v -> this.redstoneState = RedstoneState.values()[v]);
        this.data.addData(() -> this.shellValid ? 1 : 0, v -> this.shellValid = v != 0);
        this.data.addEnergy(this.energy);
        this.energy.setMaxExtract(0);
    }

    @Override
    public void registerSlots(Consumer<DataSlot> consumer) {
        this.data.register(consumer);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // Only fires on real block removal (LevelChunk#setBlockState), never on chunk unload, so cross-chunk port
        // cleanup is safe here. See setRemoved() for why it must not live there.
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory.getItems());
        this.clearPortOwnersOnBreak();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (--this.shellRecheckTimer <= 0) {
            this.shellRecheckTimer = SHELL_RECHECK_INTERVAL;
            this.recheckShell(level, pos, state);
        }

        if (!this.shellValid) {
            for (int i = 0; i < MODEL_SLOTS; i++) {
                this.runtimes[i] = 0;
                this.failStates[i] = FailureState.SHELL_BROKEN;
            }
            this.maybeSyncActiveMask();
            return;
        }

        boolean powered = level.hasNeighborSignal(pos);
        if (!this.redstoneState.matches(powered)) {
            for (int i = 0; i < MODEL_SLOTS; i++) {
                if (!ItemUtil.getStack(this.inventory, i).isEmpty()) {
                    this.failStates[i] = FailureState.REDSTONE;
                }
            }
            this.maybeSyncActiveMask();
            return;
        }

        if (!this.bufferedOutputs.isEmpty()) {
            for (int i = 0; i < this.bufferedOutputs.size(); i++) {
                ItemStack stack = this.bufferedOutputs.get(i);
                ItemStack remainder = this.insertIntoOutput(stack);
                if (remainder.isEmpty()) {
                    this.bufferedOutputs.remove(i);
                    i--;
                }
                else {
                    this.bufferedOutputs.set(i, remainder);
                }
            }
        }

        boolean anyChanged = false;
        for (int i = 0; i < MODEL_SLOTS; i++) {
            if (this.tickModelSlot(level, i)) anyChanged = true;
        }

        this.maybeSyncActiveMask();

        if (anyChanged) this.setChanged();
    }

    /** Excludes paused (REDSTONE / ENERGY_MID_CYCLE) slots so the BER doesn't draw entities whose sim isn't producing. */
    private void maybeSyncActiveMask() {
        int newMask = 0;
        for (int i = 0; i < MODEL_SLOTS; i++) {
            if (this.runtimes[i] > 0 && this.failStates[i] == FailureState.NONE) newMask |= (1 << i);
        }
        if (newMask != this.activeSlotsMask) {
            this.activeSlotsMask = newMask;
            this.sync();
        }
    }

    private boolean tickModelSlot(Level level, int i) {
        ItemStack stack = ItemUtil.getStack(this.inventory, i);
        if (stack.isEmpty()) {
            if (this.runtimes[i] != 0 || this.failStates[i] != FailureState.MODEL) {
                this.runtimes[i] = 0;
                this.failStates[i] = FailureState.MODEL;
                return true;
            }
            return false;
        }

        DataModelInstance inst = new DataModelInstance(stack, i);
        if (!inst.isValid()) {
            this.runtimes[i] = 0;
            this.failStates[i] = FailureState.MODEL;
            return false;
        }

        ModelTier tier = inst.getTier();
        if (!tier.canSim()) {
            this.runtimes[i] = 0;
            this.failStates[i] = FailureState.FAULTY;
            return false;
        }

        if (!isSelfAware(tier)) {
            this.runtimes[i] = 0;
            this.failStates[i] = FailureState.NOT_SELF_AWARE;
            return false;
        }

        int cost = Mth.ceil(inst.getModel().simCost() * HostileConfig.dataCenterSimCostMultiplier);

        if (this.runtimes[i] == 0) {
            if (!this.canStartFor(inst, cost)) return false;
            this.runtimes[i] = RUNTIME_TICKS;
            this.consumeInput(stack);
            // The +1 chance is the fractional part of accuracy; testing against accuracy itself would always succeed
            // for any value >= 1 since nextFloat() returns [0, 1).
            float accuracy = inst.getAccuracy();
            int floor = (int) accuracy;
            float frac = accuracy - floor;
            this.predictionSuccess[i] = floor + (level.getRandom().nextFloat() < frac ? 1 : 0);
            this.failStates[i] = FailureState.NONE;
            return true;
        }

        if (this.energy.getEnergyStored() < cost) {
            this.failStates[i] = FailureState.ENERGY_MID_CYCLE;
            return false;
        }
        this.energy.setEnergy(this.energy.getEnergyStored() - cost);
        this.failStates[i] = FailureState.NONE;
        this.runtimes[i]--;
        if (this.runtimes[i] == 0) {
            if (this.emitOutputs(stack, inst, this.predictionSuccess[i])) {
                this.failStates[i] = FailureState.BUFFERING;
            }
            DataModelItem.setIters(stack, DataModelItem.getIters(stack) + 1);
            this.inventory.set(i, ItemResource.of(stack), stack.getCount()); // stack is a copy; store it back
        }
        return true;
    }

    private void consumeInput(ItemStack modelStack) {
        for (int s = INPUT_START; s < OUTPUT_START; s++) {
            ItemStack inputStack = ItemUtil.getStack(this.inventory, s);
            if (!inputStack.isEmpty() && DataModelItem.matchesModelInput(modelStack, inputStack)) {
                try (Transaction tx = Transaction.openRoot()) {
                    this.inventory.extract(s, this.inventory.getResource(s), 1, tx);
                    tx.commit();
                }
                return;
            }
        }
    }

    private boolean hasMatchingInput(ItemStack modelStack) {
        for (int s = INPUT_START; s < OUTPUT_START; s++) {
            ItemStack inputStack = ItemUtil.getStack(this.inventory, s);
            if (!inputStack.isEmpty() && DataModelItem.matchesModelInput(modelStack, inputStack)) return true;
        }
        return false;
    }

    /** Sets {@code failStates[slot]} on rejection. */
    private boolean canStartFor(DataModelInstance inst, int cost) {
        int slot = inst.getSlot();

        DynamicHolder<DataModel> holder = DataModelRegistry.INSTANCE.holder(inst.getModel());
        FabSelection sel = this.savedSelections.getOrDefault(holder, FabSelection.EMPTY);
        List<ItemStackTemplate> fabDrops = inst.getModel().fabDrops();
        int dropIdx = sel.current();
        if (this.failStates[slot] == FailureState.BUFFERING && !this.bufferedOutputs.isEmpty()) {
            return false;
        }

        if (sel.isEmpty() || dropIdx < 0 || dropIdx >= fabDrops.size()) {
            this.failStates[slot] = FailureState.NO_SELECTION;
            return false;
        }

        if (!this.hasMatchingInput(inst.getSourceStack())) {
            this.failStates[slot] = FailureState.INPUT;
            return false;
        }

        if (this.energy.getEnergyStored() < cost) {
            this.failStates[slot] = FailureState.ENERGY;
            return false;
        }

        ItemStack expected = Templates.create(fabDrops.get(dropIdx));
        if (this.canAcceptInOutput(expected) && this.canAcceptInOutput(Templates.create(inst.getModel().baseDrop()))) {
            this.failStates[slot] = FailureState.NONE;
            return true;
        }
        this.failStates[slot] = FailureState.OUTPUT;
        return false;
    }

    private boolean emitOutputs(ItemStack modelStack, DataModelInstance inst, int successes) {
        DataModel model = inst.getModel();
        DynamicHolder<DataModel> holder = DataModelRegistry.INSTANCE.holder(model);
        FabSelection sel = this.savedSelections.getOrDefault(holder, FabSelection.EMPTY);
        if (sel.isEmpty()) return false;

        boolean buffering = false;

        ItemStack baseDrop = Templates.create(model.baseDrop());
        if (!baseDrop.isEmpty()) {
            ItemStack remainder = this.insertIntoOutput(baseDrop);
            if (!remainder.isEmpty()) {
                buffering = true;
                this.bufferedOutputs.add(remainder);
            }
        }

        List<ItemStackTemplate> fabDrops = model.fabDrops();
        for (int s = 0; s < successes; s++) {
            int idx = sel.current();
            if (idx < 0 || idx >= fabDrops.size()) break;
            ItemStack drop = Templates.create(fabDrops.get(idx));
            if (!drop.isEmpty()) {
                ItemStack remainder = this.insertIntoOutput(drop);
                if (!remainder.isEmpty()) {
                    buffering = true;
                    this.bufferedOutputs.add(remainder);
                }
            }
            if (sel.mode() == ProductionMode.QUEUE) {
                sel = sel.advanced();
                this.savedSelections.put(holder, sel);
            }
        }

        return buffering;
    }

    /**
     * Inserts into the output slots, bypassing the menu's insert filter.
     *
     * @return The portion of the stack that did not fit (empty if everything was inserted).
     */
    private ItemStack insertIntoOutput(ItemStack stack) {
        int remaining = stack.getCount();
        ItemResource res = ItemResource.of(stack);
        try (Transaction tx = Transaction.openRoot()) {
            for (int slot = OUTPUT_START; slot < TOTAL_SLOTS && remaining > 0; slot++) {
                remaining -= this.inventory.insertInternal(slot, res, remaining, tx);
            }
            tx.commit();
        }
        return remaining <= 0 ? ItemStack.EMPTY : stack.copyWithCount(remaining);
    }

    private boolean canAcceptInOutput(ItemStack stack) {
        if (stack.isEmpty()) return true;

        // The transaction is never committed, so this is a pure simulation.
        try (Transaction tx = Transaction.openRoot()) {
            int amount = stack.getCount();
            ItemResource res = ItemResource.of(stack);
            for (int slot = OUTPUT_START; slot < TOTAL_SLOTS; slot++) {
                if (this.inventory.insertInternal(slot, res, amount, tx) > 0) return true;
            }
            return false;
        }
    }

    private void recheckShell(Level level, BlockPos pos, BlockState state) {
        DataCenterShell.Layout layout = DataCenterShell.findFor(pos, state, level).orElse(null);
        this.cachedLayout = layout;
        boolean wasValid = this.shellValid;
        this.shellValid = layout != null;
        if (level instanceof ServerLevel sl) this.ensureShellChunksForced(sl, layout);
        this.refreshOwnedPorts(level, layout);
        if (this.shellValid != wasValid) this.sync();
    }

    /** Walks the shell's wall + ceiling cells, claims any IO Port BEs as owned, and releases ports no longer in the shell. */
    private void refreshOwnedPorts(Level level, DataCenterShell.Layout layout) {
        Set<BlockPos> current = new HashSet<>();
        if (this.shellValid && layout != null) {
            layout.forEachCell((cursor, kind) -> {
                if (kind != DataCenterShell.CellKind.WALL && kind != DataCenterShell.CellKind.CEILING) return;
                if (level.getBlockEntity(cursor) instanceof DataCenterIOPortTileEntity port) {
                    BlockPos immut = cursor.immutable();
                    current.add(immut);
                    port.setOwner(this.worldPosition);
                }
            });
        }
        for (BlockPos prev : this.ownedPorts) {
            if (!current.contains(prev) && level.getBlockEntity(prev) instanceof DataCenterIOPortTileEntity stale) {
                stale.clearOwner();
            }
        }
        this.ownedPorts.clear();
        this.ownedPorts.addAll(current);
    }

    /** Tickets are non-ticking and per-BE-position, so two Data Centers can independently claim the same chunk. */
    private void ensureShellChunksForced(ServerLevel level, DataCenterShell.Layout layout) {
        Set<Long> needed = computeNeededShellChunks(layout, this.worldPosition);

        Iterator<Long> it = this.forcedChunks.iterator();
        while (it.hasNext()) {
            long key = it.next();
            if (!needed.contains(key)) {
                Hostile.Tickets.DATA_CENTER.forceChunk(level, this.worldPosition, ChunkPos.unpack(key).x(), ChunkPos.unpack(key).z(), false, false);
                it.remove();
            }
        }
        for (Long key : needed) {
            if (this.forcedChunks.add(key)) {
                Hostile.Tickets.DATA_CENTER.forceChunk(level, this.worldPosition, ChunkPos.unpack(key).x(), ChunkPos.unpack(key).z(), true, false);
            }
        }
    }

    private void releaseAllForcedChunks(ServerLevel level) {
        for (Long key : this.forcedChunks) {
            Hostile.Tickets.DATA_CENTER.forceChunk(level, this.worldPosition, ChunkPos.unpack(key).x(), ChunkPos.unpack(key).z(), false, false);
        }
        this.forcedChunks.clear();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level instanceof ServerLevel sl) {
            this.releaseAllForcedChunks(sl);
        }
        this.ownedPorts.clear();
    }

    /**
     * Clears owners from IO ports. Only called when the block is broken (but not from setRemoved) since the owner ref is persisted.
     */
    public void clearPortOwnersOnBreak() {
        if (!(this.level instanceof ServerLevel sl)) return;
        for (BlockPos port : this.ownedPorts) {
            if (sl.isLoaded(port) && sl.getBlockEntity(port) instanceof DataCenterIOPortTileEntity p) {
                p.clearOwner();
            }
        }
    }

    /** Excludes the controller's own chunk, which is loaded by virtue of the BE existing. */
    private static Set<Long> computeNeededShellChunks(DataCenterShell.Layout layout, BlockPos controllerPos) {
        Set<Long> needed = new HashSet<>(4);
        if (layout == null) return needed;
        int minCX = SectionPos.blockToSectionCoord(layout.shellMin().getX());
        int maxCX = SectionPos.blockToSectionCoord(layout.shellMax().getX());
        int minCZ = SectionPos.blockToSectionCoord(layout.shellMin().getZ());
        int maxCZ = SectionPos.blockToSectionCoord(layout.shellMax().getZ());
        int ourCX = SectionPos.blockToSectionCoord(controllerPos.getX());
        int ourCZ = SectionPos.blockToSectionCoord(controllerPos.getZ());
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                if (cx == ourCX && cz == ourCZ) continue;
                needed.add(ChunkPos.pack(cx, cz));
            }
        }
        return needed;
    }

    /** Released persisted tickets whose owner is no longer a Data Center, or whose layout no longer needs the chunk. */
    public static void validateLoadedTickets(ServerLevel level, TicketHelper helper) {
        for (Map.Entry<BlockPos, TicketSet> entry : helper.getBlockTickets().entrySet()) {
            BlockPos owner = entry.getKey();
            TicketSet tickets = entry.getValue();
            BlockState state = level.getBlockState(owner);
            if (!state.is(Hostile.Blocks.DATA_CENTER)) {
                helper.removeAllTickets(owner);
                continue;
            }
            DataCenterShell.Layout layout = DataCenterShell.findFor(owner, state, level).orElse(null);
            Set<Long> needed = computeNeededShellChunks(layout, owner);
            for (long chunk : tickets.normal()) {
                if (!needed.contains(chunk)) helper.removeTicket(owner, chunk, false);
            }
            for (long chunk : tickets.naturalSpawning()) {
                if (!needed.contains(chunk)) helper.removeTicket(owner, chunk, true);
            }
        }
    }

    /**
     * The data center only works with models whose accuracy is at or above 100%.
     * For simplicity we call this "isSelfAware" since it's the only default tier with that accuracy level.
     */
    private static boolean isSelfAware(ModelTier tier) {
        return tier.accuracy() >= 1.0F;
    }

    public FabSelection getSelection(DataModel model) {
        return this.savedSelections.getOrDefault(DataModelRegistry.INSTANCE.holder(model), FabSelection.EMPTY);
    }

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

    public DataCenterItemHandler getInventory() {
        return this.inventory;
    }

    public EnergyHandler getEnergy() {
        return this.energy;
    }

    public int getEnergyStored() {
        return this.energy.getEnergyStored();
    }

    public int getRuntime(int slot) {
        return slot < 0 || slot >= MODEL_SLOTS ? 0 : this.runtimes[slot];
    }

    public FailureState getFailState(int slot) {
        if (slot < 0 || slot >= MODEL_SLOTS) return FailureState.NONE;
        return this.failStates[slot];
    }

    public boolean isShellValid() {
        return this.shellValid;
    }

    public DataCenterShell.Layout getCachedLayout() {
        return this.cachedLayout;
    }

    public void setRedstoneState(RedstoneState state) {
        this.redstoneState = state;
        this.setChanged();
    }

    public RedstoneState getRedstoneState() {
        return this.redstoneState;
    }

    private void sync() {
        VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        this.setChanged();
    }

    @Override
    public void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        this.inventory.serialize(out);
        out.putInt("energy", this.energy.getEnergyStored());
        out.putIntArray("runtimes", this.runtimes);
        out.putIntArray("predSuccess", this.predictionSuccess);
        out.putIntArray("failStates", Arrays.stream(this.failStates).mapToInt(Enum::ordinal).toArray());
        out.putInt("redstoneState", this.redstoneState.ordinal());
        out.putByte("ejectDirIndex", this.ejectDirIndex);
        out.store("saved_selections", CompoundTag.CODEC, this.writeSelections(new CompoundTag()));
        // forcedChunks is not persisted; the ticket controller persists tickets itself + validateLoadedTickets reconciles.
    }

    @Override
    public void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.inventory.deserialize(in);
        this.energy.setEnergy(in.getIntOr("energy", 0));
        int[] readRuntimes = in.getIntArray("runtimes").orElse(new int[MODEL_SLOTS]);
        System.arraycopy(readRuntimes, 0, this.runtimes, 0, Math.min(readRuntimes.length, this.runtimes.length));
        int[] readPred = in.getIntArray("predSuccess").orElse(new int[MODEL_SLOTS]);
        System.arraycopy(readPred, 0, this.predictionSuccess, 0, Math.min(readPred.length, this.predictionSuccess.length));
        int[] readFail = in.getIntArray("failStates").orElse(new int[MODEL_SLOTS]);
        for (int i = 0; i < Math.min(readFail.length, this.failStates.length); i++) {
            this.failStates[i] = FailureState.BY_ID.apply(readFail[i]);
        }
        this.redstoneState = RedstoneState.values()[in.getIntOr("redstoneState", 0)];
        this.ejectDirIndex = in.getByteOr("ejectDirIndex", (byte) 0);
        this.readSelections(in.read("saved_selections", CompoundTag.CODEC).orElse(new CompoundTag()));
    }

    /**
     * Both sync paths (chunk load and {@link #sync()}) carry the full saved state plus the sync-only keys. The renderer
     * reads the stored models from the client-side inventory, so every nearby client must receive it, not only the player
     * who inserted the models through the menu.
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ValueInput in) {
        this.handleUpdateTag(in);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider regs) {
        CompoundTag tag = this.saveCustomOnly(regs);
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        tag.putInt("activeMask", this.activeSlotsMask);
        tag.putBoolean("shellValid", this.shellValid);
        if (this.cachedLayout != null) DataCenterShell.Layout.writeLayout(tag, this.cachedLayout);
        return tag;
    }

    /** Vanilla's default just calls loadAdditional which ignores the sync-only keys getUpdateTag wrote. */
    @Override
    public void handleUpdateTag(ValueInput in) {
        super.handleUpdateTag(in);
        this.readSyncTag(in);
    }

    private void readSyncTag(ValueInput in) {
        this.readSelections(in.read("saved_selections", CompoundTag.CODEC).orElse(new CompoundTag()));
        this.activeSlotsMask = in.getIntOr("activeMask", 0);
        this.shellValid = in.getBooleanOr("shellValid", false);
        CompoundTag layoutTag = new CompoundTag();
        in.getInt("wallFaceOrd").ifPresent(v -> layoutTag.putInt("wallFaceOrd", v));
        in.getInt("shellMinX").ifPresent(v -> layoutTag.putInt("shellMinX", v));
        in.getInt("shellMinY").ifPresent(v -> layoutTag.putInt("shellMinY", v));
        in.getInt("shellMinZ").ifPresent(v -> layoutTag.putInt("shellMinZ", v));
        this.cachedLayout = DataCenterShell.Layout.readLayout(layoutTag, this.worldPosition);
    }

    public int getActiveSlotsMask() {
        return this.activeSlotsMask;
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
            FabSelection sel = FabSelection.CODEC.parse(NbtOps.INSTANCE, value).result().orElse(FabSelection.EMPTY);
            this.savedSelections.put(dm, sel);
        }
    }

    /** 45 slots: [0..24] models, [25..28] inputs, [29..44] output buffer. */
    public class DataCenterItemHandler extends InternalItemHandler {

        public DataCenterItemHandler() {
            super(TOTAL_SLOTS);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (index < INPUT_START) return resource.getItem() instanceof DataModelItem;
            if (index < OUTPUT_START) {
                ItemStack stack = resource.toStack(1);
                for (int m = 0; m < MODEL_SLOTS; m++) {
                    ItemStack modelStack = ItemUtil.getStack(this, m);
                    if (!modelStack.isEmpty() && DataModelItem.matchesModelInput(modelStack, stack)) return true;
                }
                return false;
            }
            return true;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index >= OUTPUT_START) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index < INPUT_START) return 0;
            return super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack oldContents) {
            DataCenterTileEntity.this.setChanged();
        }

        public NonNullList<ItemStack> getItems() {
            return this.stacks;
        }
    }
}
