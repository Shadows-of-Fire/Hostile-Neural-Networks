package dev.shadowsoffire.hostilenetworks.tile;

import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.CachedModel;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.ModelTier;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.placebo.cap.ModifiableEnergyStorage;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots;
import dev.shadowsoffire.placebo.menu.SimpleDataSlots.IDataAutoRegister;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class SimChamberTileEntity extends BlockEntity implements TickingBlockEntity, IDataAutoRegister {

    protected final SimItemHandler inventory = new SimItemHandler();
    protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.simPowerCap, HostileConfig.simPowerCap);
    protected final SimpleDataSlots data = new SimpleDataSlots();

    protected CachedModel currentModel = CachedModel.EMPTY;
    protected int runtime = 0;
    protected boolean predictionSuccess = false;
    protected FailureState failState = FailureState.NONE;

    public SimChamberTileEntity(BlockPos pos, BlockState state) {
        super(Hostile.TileEntities.SIM_CHAMBER.get(), pos, state);
        this.data.addData(() -> this.runtime, v -> this.runtime = v);
        this.data.addData(() -> this.predictionSuccess, v -> this.predictionSuccess = v);
        this.data.addData(() -> this.failState.ordinal(), v -> this.failState = FailureState.values()[v]);
        this.data.addEnergy(this.energy);
    }

    @Override
    public void registerSlots(Consumer<DataSlot> consumer) {
        this.data.register(consumer);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", this.inventory.serializeNBT());
        tag.putInt("energy", this.energy.getEnergyStored());
        tag.putString("model", !this.currentModel.isValid() ? "null" : this.currentModel.getModel().getId().toString());
        tag.putInt("runtime", this.runtime);
        tag.putBoolean("predSuccess", this.predictionSuccess);
        tag.putInt("failState", this.failState.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.inventory.deserializeNBT(tag.getCompound("inventory"));
        this.energy.setEnergy(tag.getInt("energy"));
        ItemStack model = this.inventory.getStackInSlot(0);
        CachedModel cModel = this.getOrLoadModel(model);
        String modelId = tag.getString("model");
        if (cModel.isValid() && cModel.getModel().getId().toString().equals(modelId)) {
            this.currentModel = cModel;
        }
        this.runtime = tag.getInt("runtime");
        this.predictionSuccess = tag.getBoolean("predSuccess");
        this.failState = FailureState.values()[tag.getInt("failState")];
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        ItemStack model = this.inventory.getStackInSlot(0);
        if (!model.isEmpty()) {
            CachedModel oldModel = this.currentModel;
            this.currentModel = this.getOrLoadModel(model);
            if (oldModel != this.currentModel) {
                this.runtime = 0;
            }
            if (this.currentModel.isValid()) {
                if (this.currentModel.getTier() == ModelTier.FAULTY) {
                    this.failState = FailureState.FAULTY;
                    this.runtime = 0;
                    return;
                }
                if (this.runtime == 0) {
                    if (this.canStartSimulation()) {
                        this.runtime = 300;
                        this.predictionSuccess = this.level.random.nextFloat() <= this.currentModel.getAccuracy();
                        this.inventory.getStackInSlot(1).shrink(1);
                        this.setChanged();
                    }
                }
                else if (this.hasPowerFor(this.currentModel.getModel())) {
                    this.failState = FailureState.NONE;
                    if (--this.runtime == 0) {
                        ItemStack stk = this.inventory.getStackInSlot(2);
                        if (stk.isEmpty()) this.inventory.setStackInSlot(2, this.currentModel.getModel().getBaseDrop().copy());
                        else stk.grow(1);
                        if (this.predictionSuccess) {
                            stk = this.inventory.getStackInSlot(3);
                            if (stk.isEmpty()) this.inventory.setStackInSlot(3, this.currentModel.getPredictionDrop());
                            else stk.grow(1);
                        }
                        ModelTier tier = this.currentModel.getTier();
                        if (tier != tier.next()) {
                            this.currentModel.setData(this.currentModel.getData() + 1);
                        }
                        DataModelItem.setIters(model, DataModelItem.getIters(model) + 1);
                        this.setChanged();
                    }
                    else if (this.runtime != 0) {
                        this.energy.setEnergy(this.energy.getEnergyStored() - this.currentModel.getModel().getSimCost());
                        this.setChanged();
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
        if (this.inventory.getStackInSlot(1).isEmpty()) {
            this.failState = FailureState.INPUT;
            return false;
        }

        DataModel model = this.currentModel.getModel();
        ItemStack nOut = this.inventory.getStackInSlot(2);
        ItemStack pOut = this.inventory.getStackInSlot(3);
        ItemStack nOutExp = model.getBaseDrop();
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
        return ItemStack.isSameItemSameTags(a, b) && a.getCount() < a.getMaxStackSize();
    }

    /**
     * Checks if the system has the power required for the tick cost of a model.
     *
     * @param model The model being checked.
     * @return If the chamber has more power than the sim cost of the model.
     */
    public boolean hasPowerFor(DataModel model) {
        return this.energy.getEnergyStored() >= model.getSimCost();
    }

    protected CachedModel getOrLoadModel(ItemStack stack) {
        if (this.currentModel.getSourceStack() == stack) return this.currentModel;
        else return new CachedModel(stack, 0);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.of(() -> this.inventory).cast();
        if (cap == ForgeCapabilities.ENERGY) return LazyOptional.of(() -> this.energy).cast();
        return super.getCapability(cap, side);
    }

    public SimItemHandler getInventory() {
        return this.inventory;
    }

    public int getEnergyStored() {
        return this.energy.getEnergyStored();
    }

    public int getRuntime() {
        return this.runtime;
    }

    public boolean didPredictionSucceed() {
        return this.predictionSuccess;
    }

    public FailureState getFailState() {
        return this.failState;
    }

    public class SimItemHandler extends InternalItemHandler {

        public SimItemHandler() {
            super(4);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof DataModelItem;
            else if (slot == 1) return DataModelItem.matchesInput(this.getStackInSlot(0), stack);
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

    }

    public enum FailureState {
        NONE("none"),
        OUTPUT("output"),
        ENERGY("energy"),
        INPUT("input"),
        MODEL("model"),
        FAULTY("faulty"),
        ENERGY_MID_CYCLE("energy_mid_cycle");

        private final String name;

        FailureState(String name) {
            this.name = name;
        }

        public String getKey() {
            return "hostilenetworks.fail." + this.name;
        }
    }

}
