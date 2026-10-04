package dev.shadowsoffire.hostilenetworks.tile;

import java.util.function.Consumer;
import java.util.function.IntFunction;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.util.Templates;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.util.RedstoneState;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.hostilenetworks.util.ModifiableEnergyStorage;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots.IDataAutoRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ByIdMap;
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

public class SimChamberTileEntity extends BlockEntity implements TickingBlockEntity, IDataAutoRegister {

    protected final SimItemHandler inventory = new SimItemHandler();
    protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.simPowerCap, HostileConfig.simPowerCap);
    protected final SimpleDataSlots data = new SimpleDataSlots();

    protected DataModelInstance currentModel = DataModelInstance.EMPTY;
    protected int runtime = 0;

    /**
     * The amount of successful predictions for the current simulation run.
     * <p>
     * If the value is 0, the prediction failed and no prediction items will be generated.
     * <p>
     * If it is not zero, that many prediction items will be generated. Values higher than one can be generated when accuracy exceeds 100%.
     */
    protected int predictionSuccess = 0;
    protected FailureState failState = FailureState.NONE;
    protected RedstoneState redstoneState = RedstoneState.IGNORED;
    protected SimMode mode = SimMode.INFERENCE;

    public SimChamberTileEntity(BlockPos pos, BlockState state) {
        super(Hostile.TileEntities.SIM_CHAMBER, pos, state);
        this.data.addData(() -> this.runtime, v -> this.runtime = v);
        this.data.addData(() -> this.predictionSuccess, v -> this.predictionSuccess = v);
        this.data.addData(() -> this.failState.ordinal(), v -> this.failState = FailureState.values()[v]);
        this.data.addData(() -> this.redstoneState.ordinal(), v -> this.redstoneState = RedstoneState.values()[v]);
        this.data.addData(() -> this.mode.ordinal(), v -> this.mode = SimMode.values()[v]);
        this.data.addEnergy(this.energy);
        this.energy.setMaxExtract(0);
    }

    @Override
    public void registerSlots(Consumer<DataSlot> consumer) {
        this.data.register(consumer);
    }

    @Override
    public void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        this.inventory.serialize(out);
        out.putInt("energy", this.energy.getEnergyStored());
        out.putString("model", !this.currentModel.isValid() ? "null" : DataModelRegistry.INSTANCE.getKey(this.currentModel.getModel()).toString());
        out.putInt("runtime", this.runtime);
        out.putInt("predSuccess", this.predictionSuccess);
        out.putInt("failState", this.failState.ordinal());
        out.putInt("redstoneState", this.redstoneState.ordinal());
        out.putInt("simMode", this.mode.ordinal());
    }

    @Override
    public void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        this.inventory.deserialize(in);
        this.energy.setEnergy(in.getIntOr("energy", 0));
        ItemStack model = ItemUtil.getStack(this.inventory, 0);
        DataModelInstance cModel = this.getOrLoadModel(model);
        Identifier modelId = Identifier.parse(in.getStringOr("model", "null"));
        if (cModel.isValid() && DataModelRegistry.INSTANCE.getKey(cModel.getModel()).equals(modelId)) {
            this.currentModel = cModel;
        }
        this.runtime = in.getIntOr("runtime", 0);
        this.predictionSuccess = in.getIntOr("predSuccess", 0);
        this.failState = FailureState.values()[in.getIntOr("failState", 0)];
        this.redstoneState = RedstoneState.values()[in.getIntOr("redstoneState", 0)];
        this.mode = SimMode.values()[in.getIntOr("simMode", 0)];
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory.getItems());
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        // Training Mode is unavailable when model upgrading is disabled by config; revert any chamber stuck in it.
        if (this.mode == SimMode.TRAINING && HostileConfig.simModelUpgrade == 0) {
            this.mode = SimMode.INFERENCE;
            this.setChanged();
        }

        ItemStack model = ItemUtil.getStack(this.inventory, 0);
        if (!model.isEmpty()) {
            DataModelInstance oldModel = this.currentModel;
            this.currentModel = this.getOrLoadModel(model);
            if (oldModel != this.currentModel) {
                this.runtime = 0;
            }
            if (this.currentModel.isValid()) {
                if (!this.currentModel.getTier().canSim()) {
                    this.failState = FailureState.FAULTY;
                    this.runtime = 0;
                    return;
                }
                if (this.runtime == 0) {
                    if (this.canStartSimulation()) {
                        this.runtime = this.mode.getRuntime();
                        this.predictionSuccess = this.currentModel.rollPredictions(this.level.getRandom());
                        try (Transaction tx = Transaction.openRoot()) {
                            this.inventory.extractInternal(1, this.inventory.getResource(1), 1, tx);
                            tx.commit();
                        }
                        this.setChanged();
                    }
                }
                else if (this.hasPowerFor(this.currentModel.getModel())) {
                    if (this.getRedstoneState().matches(level.hasNeighborSignal(worldPosition))) {
                        this.failState = FailureState.NONE;
                        if (--this.runtime == 0) {
                            // Inference Mode produces the loot; Training Mode produces nothing here.
                            if (this.mode == SimMode.INFERENCE) {
                                ItemStack baseDrop = Templates.create(this.currentModel.getModel().baseDrop());
                                if (!baseDrop.isEmpty()) {
                                    try (Transaction tx = Transaction.openRoot()) {
                                        this.inventory.insertInternal(2, ItemResource.of(baseDrop), baseDrop.getCount(), tx);
                                        tx.commit();
                                    }
                                }

                                if (this.predictionSuccess > 0) {
                                    ItemStack prediction = this.currentModel.getPredictionDrop().copyWithCount(this.predictionSuccess);
                                    try (Transaction tx = Transaction.openRoot()) {
                                        this.inventory.insertInternal(3, ItemResource.of(prediction), prediction.getCount(), tx);
                                        tx.commit();
                                    }
                                }
                            }
                            // Training Mode upgrades the model's data; Inference Mode does not.
                            if (this.mode == SimMode.TRAINING && this.canUpgrade(this.currentModel)) {
                                int newData = this.currentModel.getData() + 1;
                                this.currentModel.setData(newData);
                            }
                            // The training write above and the iteration counter land on the instance's source stack,
                            // which is a copy of the handler contents. Store it back so the change persists.
                            ItemStack src = this.currentModel.getSourceStack();
                            DataModelItem.setIters(src, DataModelItem.getIters(src) + 1);
                            this.inventory.set(0, ItemResource.of(src), model.getCount());
                            this.setChanged();
                        }
                        else if (this.runtime != 0) {
                            this.energy.setEnergy(this.energy.getEnergyStored() - this.mode.adjustCost(this.currentModel.getModel().simCost()));
                            this.setChanged();
                        }
                    }
                    else {
                        this.failState = FailureState.REDSTONE;
                    }
                }
                else {
                    this.failState = FailureState.ENERGY_MID_CYCLE;
                }
                return;
            }
        }
        this.failState = FailureState.MODEL;
        this.runtime = 0;
    }

    /**
     * Checks if the output slots are clear and there is enough power for a sim run.
     */
    public boolean canStartSimulation() {
        if (!DataModelItem.matchesModelInput(ItemUtil.getStack(this.inventory, 0), ItemUtil.getStack(this.inventory, 1))) {
            this.failState = FailureState.INPUT;
            return false;
        }

        if (!this.redstoneState.matches(this.level.hasNeighborSignal(this.worldPosition))) {
            this.failState = FailureState.REDSTONE;
            return false;
        }

        if (this.mode == SimMode.TRAINING && !this.canUpgrade(this.currentModel)) {
            this.failState = FailureState.CANNOT_TRAIN;
            return false;
        }

        DataModel model = this.currentModel.getModel();
        ItemStack nOut = ItemUtil.getStack(this.inventory, 2);
        ItemStack pOut = ItemUtil.getStack(this.inventory, 3);
        ItemStack nOutExp = Templates.create(model.baseDrop());
        ItemStack pOutExp = this.currentModel.getPredictionDrop();

        if (this.canStack(nOut, nOutExp) && this.canStack(pOut, pOutExp)) {
            if (this.hasPowerFor(model)) {
                this.failState = FailureState.NONE;
                return true;
            }
            else {
                this.failState = FailureState.ENERGY;
                return false;
            }
        }
        else {
            this.failState = FailureState.OUTPUT;
            return false;
        }
    }

    public boolean canStack(ItemStack a, ItemStack b) {
        if (a.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(a, b) && a.getCount() < a.getMaxStackSize();
    }

    /**
     * Checks if the system has the power required for the tick cost of a model, adjusted for the current mode.
     *
     * @param model The model being checked.
     * @return If the chamber has more power than the sim cost of the model.
     */
    public boolean hasPowerFor(DataModel model) {
        return this.energy.getEnergyStored() >= this.mode.adjustCost(model.simCost());
    }

    public boolean canUpgrade(DataModelInstance inst) {
        ModelTier tier = inst.getTier();
        if (tier.isMax() || HostileConfig.simModelUpgrade == 0) {
            return false;
        }

        int newData = inst.getData() + 1;
        if (HostileConfig.simModelUpgrade == 2 && newData > inst.getNextTierData()) {
            return false;
        }

        return true;
    }

    protected DataModelInstance getOrLoadModel(ItemStack stack) {
        // ItemUtil.getStack returns a fresh copy on every call, so the 1.21 identity check would never match and the
        // instance (and runtime) would reset every tick. Compare contents instead; model stacks are never stackable.
        if (ItemStack.isSameItemSameComponents(this.currentModel.getSourceStack(), stack)) return this.currentModel;
        else return new DataModelInstance(stack, 0);
    }

    public SimItemHandler getInventory() {
        return this.inventory;
    }

    public EnergyHandler getEnergy() {
        return energy;
    }

    public int getEnergyStored() {
        return this.energy.getEnergyStored();
    }

    public int getRuntime() {
        return this.runtime;
    }

    public boolean didPredictionSucceed() {
        return this.predictionSuccess > 0;
    }

    public FailureState getFailState() {
        return this.failState;
    }

    public void setRedstoneState(RedstoneState state) {
        this.redstoneState = state;
    }

    public RedstoneState getRedstoneState() {
        return this.redstoneState;
    }

    public void setSimMode(SimMode mode) {
        this.mode = mode;
        this.setChanged();
    }

    public SimMode getSimMode() {
        return this.mode;
    }

    public class SimItemHandler extends InternalItemHandler {

        public SimItemHandler() {
            super(4);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (index == 0) return resource.getItem() instanceof DataModelItem;
            else if (index == 1) return DataModelItem.matchesModelInput(ItemUtil.getStack(this, 0), resource.toStack(1));
            return true;
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index > 1) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (index <= 1) return 0;
            return super.extract(index, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int index, ItemStack oldContents) {
            SimChamberTileEntity.this.setChanged();
        }

        public NonNullList<ItemStack> getItems() {
            return this.stacks;
        }

    }

    public enum FailureState {
        NONE("none"),
        OUTPUT("output"),
        ENERGY("energy"),
        INPUT("input"),
        MODEL("model"),
        FAULTY("faulty"),
        ENERGY_MID_CYCLE("energy_mid_cycle"),
        REDSTONE("redstone"),
        CANNOT_TRAIN("cannot_train"),
        // The following failure states can only be exhibited by the Data Center.
        SHELL_BROKEN("shell_broken"),
        NOT_SELF_AWARE("not_self_aware"),
        NO_SELECTION("no_selection"),
        BUFFERING("buffering");

        public static final IntFunction<FailureState> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

        private final String name;

        FailureState(String name) {
            this.name = name;
        }

        public String getKey() {
            return "hostilenetworks.fail." + this.name;
        }
    }

    /**
     * The operating mode of the Simulation Chamber.
     * <p>
     * In {@link #INFERENCE} the chamber produces loot and predictions; in {@link #TRAINING} it instead upgrades the
     * data model. The two are mutually exclusive - a run never does both.
     * <p>
     * Inference runs 25% faster than Training, while Training draws 20% more energy per tick.
     */
    public enum SimMode {

        INFERENCE("inference", Identifier.withDefaultNamespace("textures/item/ender_eye.png"), 1.25F, 1F),
        TRAINING("training", Identifier.withDefaultNamespace("textures/item/experience_bottle.png"), 1F, 1.2F);

        /**
         * The length of a simulation run at 100% speed, in ticks.
         */
        public static final int BASE_RUNTIME = 300;

        private final String name;
        private final Identifier texture;
        private final float speedMultiplier;
        private final float costMultiplier;
        private final int runtime;

        SimMode(String name, Identifier texture, float speedMultiplier, float costMultiplier) {
            this.name = name;
            this.texture = texture;
            this.speedMultiplier = speedMultiplier;
            this.costMultiplier = costMultiplier;
            this.runtime = Math.round(BASE_RUNTIME / speedMultiplier);
        }

        /**
         * The length of a single simulation run in this mode, in ticks.
         */
        public int getRuntime() {
            return this.runtime;
        }

        public float getSpeedMultiplier() {
            return this.speedMultiplier;
        }

        public float getCostMultiplier() {
            return this.costMultiplier;
        }

        /**
         * Applies this mode's energy cost multiplier to a model's base sim cost.
         */
        public int adjustCost(int baseCost) {
            return Mth.ceil(baseCost * this.costMultiplier);
        }

        public String getKey() {
            return "hostilenetworks.gui.mode." + this.name;
        }

        public Identifier getResourceLocation() {
            return this.texture;
        }

        public SimMode next() {
            return this == INFERENCE ? TRAINING : INFERENCE;
        }

    }

}
