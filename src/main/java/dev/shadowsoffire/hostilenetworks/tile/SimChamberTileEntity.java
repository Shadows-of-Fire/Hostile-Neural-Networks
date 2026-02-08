package dev.shadowsoffire.hostilenetworks.tile;

import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.placebo.cap.ModifiableEnergyStorage;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots.IDataAutoRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class SimChamberTileEntity extends BlockEntity implements TickingBlockEntity, IDataAutoRegister {

    protected final SimItemHandler inventory = new SimItemHandler();
    protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.simPowerCap, HostileConfig.simPowerCap);
    protected final SimpleDataSlots data = new SimpleDataSlots();

    protected DataModelInstance currentModel = DataModelInstance.EMPTY;
    protected int runtime = 0;

    protected boolean trainingMode = true;

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

    public SimChamberTileEntity(BlockPos pos, BlockState state) {
        super(Hostile.TileEntities.SIM_CHAMBER, pos, state);
        this.data.addData(() -> this.runtime, v -> this.runtime = v);
        this.data.addData(() -> this.predictionSuccess, v -> this.predictionSuccess = v);
        this.data.addData(() -> this.failState.ordinal(), v -> this.failState = FailureState.values()[v]);
        this.data.addData(() -> this.redstoneState.ordinal(), v -> this.redstoneState = RedstoneState.values()[v]);
        this.data.addData(() -> this.trainingMode ? 1 : 0, v -> this.trainingMode = v != 0);
        this.data.addEnergy(this.energy);
        this.energy.setMaxExtract(0);
    }

    @Override
    public void registerSlots(Consumer<DataSlot> consumer) {
        this.data.register(consumer);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.put("inventory", this.inventory.serializeNBT(regs));
        tag.putInt("energy", this.energy.getEnergyStored());
        tag.putString("model", !this.currentModel.isValid() ? "null" : DataModelRegistry.INSTANCE.getKey(this.currentModel.getModel()).toString());
        tag.putInt("runtime", this.runtime);
        tag.putInt("predSuccess", this.predictionSuccess);
        tag.putInt("failState", this.failState.ordinal());
        tag.putInt("redstoneState", this.redstoneState.ordinal());
        tag.putBoolean("trainingMode", this.trainingMode);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        this.inventory.deserializeNBT(regs, tag.getCompound("inventory"));
        this.energy.setEnergy(tag.getInt("energy"));
        ItemStack model = this.inventory.getStackInSlot(0);
        DataModelInstance cModel = this.getOrLoadModel(model);
        ResourceLocation modelId = ResourceLocation.parse(tag.getString("model"));
        if (cModel.isValid() && DataModelRegistry.INSTANCE.getKey(cModel.getModel()).equals(modelId)) {
            this.currentModel = cModel;
        }
        this.runtime = tag.getInt("runtime");
        this.predictionSuccess = tag.getInt("predSuccess");
        this.failState = FailureState.values()[tag.getInt("failState")];
        this.redstoneState = RedstoneState.values()[tag.getInt("redstoneState")];
        this.trainingMode = tag.getBoolean("trainingMode");
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        ItemStack model = this.inventory.getStackInSlot(0);
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
                        this.runtime = 300;
                        float accuracy = this.currentModel.getAccuracy();
                        this.predictionSuccess = (int) accuracy;
                        if (this.level.random.nextFloat() <= this.currentModel.getAccuracy()) {
                            this.predictionSuccess += 1;
                        }

                        this.inventory.getStackInSlot(1).shrink(1);
                        this.setChanged();
                    }
                }
                else if (this.hasPowerFor(this.currentModel.getModel())) {
                    if (this.getRedstoneState().matches(level.hasNeighborSignal(worldPosition))) {
                        this.failState = FailureState.NONE;
                        if (--this.runtime == 0) {
                            if (!this.trainingMode) { //Inference
                                ItemStack stk = this.inventory.getStackInSlot(2);
                                if (stk.isEmpty())
                                    this.inventory.setStackInSlot(2, this.currentModel.getModel().baseDrop().copy());
                                else stk.grow(1);
                                if (this.predictionSuccess > 0) {
                                    stk = this.inventory.getStackInSlot(3);
                                    if (stk.isEmpty()) {
                                        this.inventory.setStackInSlot(3, this.currentModel.getPredictionDrop().copyWithCount(this.predictionSuccess));
                                    } else {
                                        stk.grow(this.predictionSuccess);
                                    }
                                }
                            }
                            else { // Training
                                ModelTier tier = this.currentModel.getTier();
                                if (!tier.isMax() && HostileConfig.simModelUpgrade > 0) {
                                    int newData = this.currentModel.getData() + 1;
                                    if (!(HostileConfig.simModelUpgrade == 2 && newData > this.currentModel.getNextTierData())) {
                                        this.currentModel.setData(newData);
                                    }
                                }
                                DataModelItem.setIters(model, DataModelItem.getIters(model) + 1);
                            }
                            this.setChanged();
                        }
                        else if (this.runtime != 0) {
                            this.energy.setEnergy(this.energy.getEnergyStored() - this.currentModel.getModel().simCost());
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
        if (!DataModelItem.matchesModelInput(this.inventory.getStackInSlot(0), this.inventory.getStackInSlot(1))) {
            this.failState = FailureState.INPUT;
            return false;
        }

        if (!this.redstoneState.matches(this.level.hasNeighborSignal(this.worldPosition))) {
            this.failState = FailureState.REDSTONE;
            return false;
        }

        DataModel model = this.currentModel.getModel();
        ItemStack nOut = this.inventory.getStackInSlot(2);
        ItemStack pOut = this.inventory.getStackInSlot(3);
        ItemStack nOutExp = model.baseDrop();
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
     * Checks if the system has the power required for the tick cost of a model.
     *
     * @param model The model being checked.
     * @return If the chamber has more power than the sim cost of the model.
     */
    public boolean hasPowerFor(DataModel model) {
        return this.energy.getEnergyStored() >= model.simCost();
    }

    protected DataModelInstance getOrLoadModel(ItemStack stack) {
        if (this.currentModel.getSourceStack() == stack) return this.currentModel;
        else return new DataModelInstance(stack, 0);
    }

    public SimItemHandler getInventory() {
        return this.inventory;
    }

    public IEnergyStorage getEnergy() {
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

    public boolean isTrainingMode() {
        return this.trainingMode;
    }

    public void setTrainingMode(boolean trainingMode) {
        this.trainingMode = trainingMode;
        this.setChanged();
    }

    public class SimItemHandler extends InternalItemHandler {

        public SimItemHandler() {
            super(4);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof DataModelItem;
            else if (slot == 1) return DataModelItem.matchesModelInput(this.getStackInSlot(0), stack);
            return true;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot > 1) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot <= 1) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
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
        REDSTONE("redstone");

        private final String name;

        FailureState(String name) {
            this.name = name;
        }

        public String getKey() {
            return "hostilenetworks.fail." + this.name;
        }
    }

    public enum RedstoneState {

        IGNORED("ignored", ResourceLocation.withDefaultNamespace("textures/item/redstone.png")),
        OFF_WHEN_POWERED("off_when_powered", ResourceLocation.withDefaultNamespace("textures/block/redstone_torch_off.png")),
        ON_WHEN_POWERED("on_when_powered", ResourceLocation.withDefaultNamespace("textures/block/redstone_torch.png"));

        private final String name;
        private final ResourceLocation texture;

        RedstoneState(String name, ResourceLocation texture) {
            this.name = name;
            this.texture = texture;
        }

        public String getKey() {
            return "hostilenetworks.gui.redstone." + name;
        }

        public ResourceLocation getResourceLocation() {
            return texture;
        }

        public boolean matches(boolean power) {
            return switch (this) {
                case IGNORED -> true;
                case OFF_WHEN_POWERED -> !power;
                case ON_WHEN_POWERED -> power;
            };
        }

        public RedstoneState next() {
            return switch (this) {
                case IGNORED -> OFF_WHEN_POWERED;
                case OFF_WHEN_POWERED -> ON_WHEN_POWERED;
                case ON_WHEN_POWERED -> IGNORED;
            };
        }

    }

}
