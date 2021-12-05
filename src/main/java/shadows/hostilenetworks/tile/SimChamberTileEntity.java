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
				return SimChamberTileEntity.this.runtime;
			case 1:
				return SimChamberTileEntity.this.predictionSuccess ? 1 : 0;
			case 2:
				return SimChamberTileEntity.this.failState.ordinal();
			case 3:
				return SimChamberTileEntity.this.energy.getEnergyStored() & 0xFFFF;
			case 4:
				return SimChamberTileEntity.this.energy.getEnergyStored() >> 16;
			}
			return -1;
		}

		@Override
		public void set(int pIndex, int pValue) {
			switch (pIndex) {
			case 0:
				SimChamberTileEntity.this.runtime = pValue;
				return;
			case 1:
				SimChamberTileEntity.this.predictionSuccess = pValue == 1;
				return;
			case 2:
				SimChamberTileEntity.this.failState = FailureState.values()[pValue];
				return;
			case 3:
				pValue = (short) pValue & 0xFFFF;
				SimChamberTileEntity.this.energy.setEnergy(SimChamberTileEntity.this.energy.getEnergyStored() & 0xFFFF0000 | pValue);
				return;
			case 4:
				pValue = (short) pValue & 0xFFFF;
				SimChamberTileEntity.this.energy.setEnergy(SimChamberTileEntity.this.energy.getEnergyStored() & 0x0000FFFF | pValue << 16);
				return;
			}
		}

		@Override
		public int getCount() {
			return 6;
		}

	};

	public SimChamberTileEntity() {
		super(Hostile.TileEntities.SIM_CHAMBER);
	}

	@Override
	public CompoundNBT save(CompoundNBT tag) {
		tag = super.save(tag);
		tag.put("inventory", this.inventory.serializeNBT());
		tag.putInt("energy", this.energy.getEnergyStored());
		tag.putString("model", this.currentModel == null ? "null" : this.currentModel.getModel().getId().toString());
		tag.putInt("runtime", this.runtime);
		tag.putBoolean("predSuccess", this.predictionSuccess);
		tag.putInt("failState", this.failState.ordinal());
		return tag;
	}

	@Override
	public void load(BlockState state, CompoundNBT tag) {
		super.load(state, tag);
		this.inventory.deserializeNBT(tag.getCompound("inventory"));
		this.energy.setEnergy(tag.getInt("energy"));
		ItemStack model = this.inventory.getStackInSlot(0);
		CachedModel cModel = this.getOrLoadModel(model);
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
		if (this.level.isClientSide) return;
		this.energy.receiveEnergy(4000, false);
		ItemStack model = this.inventory.getStackInSlot(0);
		if (!model.isEmpty()) {
			CachedModel oldModel = this.currentModel;
			this.currentModel = this.getOrLoadModel(model);
			if (oldModel != this.currentModel) {
				this.runtime = 0;
			}
			if (this.currentModel != null) {
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
					}
				} else if (--this.runtime == 0) {
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
				} else if (this.runtime != 0) {
					this.energy.setEnergy(this.energy.getEnergyStored() - this.currentModel.getModel().getSimCost());
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
			} else {
				this.failState = FailureState.ENERGY;
				return false;
			}
		} else {
			this.failState = FailureState.OUTPUT;
			return false;
		}
	}

	public boolean canStack(ItemStack a, ItemStack b) {
		if (a.isEmpty()) return true;
		return a.getItem() == b.getItem() && ItemStack.tagMatches(a, b) && a.getCount() < a.getMaxStackSize();
	}

	public boolean hasPowerFor(DataModel model) {
		return this.energy.getEnergyStored() >= model.getSimCost() * 300;
	}

	@Nullable
	protected CachedModel getOrLoadModel(ItemStack stack) {
		if (this.currentModel == null || this.currentModel.getSourceStack() != stack) {
			CachedModel model = new CachedModel(stack, 0);
			if (model.getModel() != null) return model;
			else return null;
		}
		return this.currentModel;
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return LazyOptional.of(() -> this.inventory).cast();
		if (cap == CapabilityEnergy.ENERGY) return LazyOptional.of(() -> this.energy).cast();
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

		public ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
			return super.extractItem(slot, amount, simulate);
		}

	}

	public enum FailureState {
		NONE("none"),
		OUTPUT("output"),
		ENERGY("energy"),
		INPUT("input"),
		MODEL("model"),
		FAULTY("faulty");

		private final String name;

		FailureState(String name) {
			this.name = name;
		}

		public String getKey() {
			return "hostilenetworks.fail." + this.name;
		}
	}

}
