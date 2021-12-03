package shadows.hostilenetworks.tile;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.HostileConfig;
import shadows.hostilenetworks.data.CachedModel;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.util.ModifiableEnergyStorage;

public class SimChamberTileEntity extends TileEntity implements ITickableTileEntity {

	protected final SimItemHandler inventory = new SimItemHandler();
	protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.simPowerCap, HostileConfig.simPowerCap);

	protected CachedModel currentModel = null;
	protected int runtime = 0;
	protected boolean predictionSuccess = false;
	protected FailureState failState = FailureState.NONE;

	protected IIntArray references = new IIntArray() {

		@Override
		public int get(int pIndex) {
			switch (pIndex) {
			case 0:
				return runtime;
			case 1:
				return predictionSuccess ? 1 : 0;
			case 2:
				return failState.ordinal();
			case 3:
				return energy.getEnergyStored();
			}
			return -1;
		}

		@Override
		public void set(int pIndex, int pValue) {
			switch (pIndex) {
			case 0:
				runtime = pValue;
				return;
			case 1:
				predictionSuccess = pValue == 1;
				return;
			case 2:
				failState = FailureState.values()[pValue];
				return;
			case 3:
				energy.setEnergy(pValue);
				return;
			}
		}

		@Override
		public int getCount() {
			return 4;
		}

	};

	public SimChamberTileEntity() {
		super(Hostile.TileEntities.SIM_CHAMBER);
	}

	@Override
	public CompoundNBT save(CompoundNBT tag) {
		tag = super.save(tag);
		tag.put("inventory", inventory.serializeNBT());
		tag.putInt("energy", this.energy.getEnergyStored());
		tag.putString("model", this.currentModel == null ? "null" : this.currentModel.getModel().getId().toString());
		tag.putInt("runtime", this.runtime);
		tag.putBoolean("predSuccess", predictionSuccess);
		tag.putInt("failState", this.failState.ordinal());
		return tag;
	}

	@Override
	public void load(BlockState state, CompoundNBT tag) {
		super.load(state, tag);
		this.inventory.deserializeNBT(tag.getCompound("inventory"));
		this.energy.setEnergy(tag.getInt("energy"));
		ItemStack model = inventory.getStackInSlot(0);
		CachedModel cModel = getOrLoadModel(model);
		String modelId = tag.getString("model");
		if (cModel != null && cModel.getModel().getId().toString().equals(modelId)) {
			this.currentModel = cModel;
		}
		this.runtime = tag.getInt("runtime");
		this.predictionSuccess = tag.getBoolean("predSuccess");
		this.failState = FailureState.values()[tag.getInt("failState")];
	}

	@Override
	public void tick() {
		if (level.isClientSide) return;
		this.energy.receiveEnergy(4000, false);
		ItemStack model = inventory.getStackInSlot(0);
		if (!model.isEmpty()) {
			CachedModel oldModel = currentModel;
			currentModel = getOrLoadModel(model);
			if (oldModel != currentModel) {
				this.runtime = 0;
			}
			if (currentModel != null) {
				if (currentModel.getTier() == ModelTier.FAULTY) {
					this.failState = FailureState.FAULTY;
					this.runtime = 0;
					return;
				}
				if (this.runtime == 0) {
					if (canStartSimulation()) {
						runtime = 300;
						predictionSuccess = level.random.nextFloat() <= currentModel.getAccuracy();
						inventory.getStackInSlot(1).shrink(1);
					}
				} else if (--this.runtime == 0) {
					ItemStack stk = inventory.getStackInSlot(2);
					if (stk.isEmpty()) inventory.setStackInSlot(2, currentModel.getModel().getBaseDrop().copy());
					else stk.grow(1);
					if (predictionSuccess) {
						stk = inventory.getStackInSlot(3);
						if (stk.isEmpty()) inventory.setStackInSlot(3, currentModel.getPredictionDrop());
						else stk.grow(1);
					}
					ModelTier tier = currentModel.getTier();
					if (tier != tier.next()) {
						currentModel.setData(currentModel.getData() + 1);
					}
					DataModelItem.setIters(model, DataModelItem.getIters(model) + 1);
				} else if (this.runtime != 0) {
					energy.setEnergy(energy.getEnergyStored() - currentModel.getModel().getSimCost());
				}
				return;
			}
		}
		failState = FailureState.MODEL;
		this.runtime = 0;
	}

	/**
	 * Checks if the output slots are clear and there is enough power for a sim run.
	 */
	public boolean canStartSimulation() {
		if (inventory.getStackInSlot(1).isEmpty()) {
			this.failState = FailureState.POLYMER;
			return false;
		}

		DataModel model = currentModel.getModel();
		ItemStack nOut = inventory.getStackInSlot(2);
		ItemStack pOut = inventory.getStackInSlot(3);
		ItemStack nOutExp = model.getBaseDrop();
		ItemStack pOutExp = currentModel.getPredictionDrop();

		if (canStack(nOut, nOutExp) && canStack(pOut, pOutExp)) {
			if (hasPowerFor(model)) {
				failState = FailureState.NONE;
				return true;
			} else {
				failState = FailureState.ENERGY;
				return false;
			}
		} else {
			failState = FailureState.OUTPUT;
			return false;
		}
	}

	public boolean canStack(ItemStack a, ItemStack b) {
		if (a.isEmpty()) return true;
		return a.getItem() == b.getItem() && ItemStack.tagMatches(a, b) && a.getCount() < a.getMaxStackSize();
	}

	public boolean hasPowerFor(DataModel model) {
		return energy.getEnergyStored() >= model.getSimCost() * 300;
	}

	@Nullable
	protected CachedModel getOrLoadModel(ItemStack stack) {
		if (currentModel == null || currentModel.getSourceStack() != stack) {
			CachedModel model = new CachedModel(stack, 0);
			if (model.getModel() != null) return model;
			else return null;
		}
		return currentModel;
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return LazyOptional.of(() -> inventory).cast();
		if (cap == CapabilityEnergy.ENERGY) return LazyOptional.of(() -> energy).cast();
		return super.getCapability(cap, side);
	}

	public SimItemHandler getInventory() {
		return this.inventory;
	}

	public IIntArray getRefHolder() {
		return this.references;
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

	public class SimItemHandler extends ItemStackHandler {

		public SimItemHandler() {
			super(4);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (slot == 0) return stack.getItem() instanceof DataModelItem;
			else if (slot == 1) return stack.getItem() == Hostile.Items.POLYMER_CLAY;
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

		public ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
			return super.extractItem(slot, amount, simulate);
		}

	}

	public enum FailureState {
		NONE("none"),
		OUTPUT("output"),
		ENERGY("energy"),
		POLYMER("polymer"),
		MODEL("model"),
		FAULTY("faulty");

		private final String name;

		FailureState(String name) {
			this.name = name;
		}

		public String getKey() {
			return "hostilenetworks.fail." + name;
		}
	}

}
