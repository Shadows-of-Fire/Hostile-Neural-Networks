package shadows.hostilenetworks.tile;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IIntArray;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.HostileConfig;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.util.ModifiableEnergyStorage;
import shadows.placebo.recipe.VanillaPacketDispatcher;

public class LootFabTileEntity extends TileEntity implements ITickableTileEntity {

	protected final FabItemHandler inventory = new FabItemHandler();
	protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.fabPowerCap, HostileConfig.fabPowerCap);
	protected final Object2IntMap<DataModel> savedSelections = new Object2IntOpenHashMap<>();

	protected int runtime = 0;
	protected int currentSel = -1;

	protected IIntArray references = new IIntArray() {

		@Override
		public int get(int pIndex) {
			switch (pIndex) {
			case 0:
				return LootFabTileEntity.this.runtime;
			case 1:
				return LootFabTileEntity.this.energy.getEnergyStored() & 0xFFFF;
			case 2:
				return LootFabTileEntity.this.energy.getEnergyStored() >> 16;
			}
			return -1;
		}

		@Override
		public void set(int pIndex, int pValue) {
			switch (pIndex) {
			case 0:
				LootFabTileEntity.this.runtime = pValue;
				return;
			case 1:
				pValue = (short) pValue & 0xFFFF;
				LootFabTileEntity.this.energy.setEnergy(LootFabTileEntity.this.energy.getEnergyStored() & 0xFFFF0000 | pValue);
				return;
			case 2:
				pValue = (short) pValue & 0xFFFF;
				LootFabTileEntity.this.energy.setEnergy(LootFabTileEntity.this.energy.getEnergyStored() & 0x0000FFFF | pValue << 16);
				return;
			}
		}

		@Override
		public int getCount() {
			return 3;
		}

	};

	public LootFabTileEntity() {
		super(Hostile.TileEntities.LOOT_FABRICATOR);
		this.savedSelections.defaultReturnValue(-1);
	}

	@Override
	public void tick() {
		if (this.level.isClientSide) return;
		this.energy.receiveEnergy(4000, false);
		DataModel dm = MobPredictionItem.getStoredModel(this.inventory.getStackInSlot(0));
		if (dm != null) {
			int selection = this.savedSelections.getInt(dm);
			if (this.currentSel != selection) {
				this.currentSel = selection;
				this.runtime = 0;
				return;
			}
			if (selection != -1) {
				if (this.runtime == 0) {
					ItemStack out = dm.getFabDrops().get(selection).copy();
					if (this.insertInOutput(out, true)) this.runtime = 60;
				} else {
					if (this.energy.getEnergyStored() < HostileConfig.fabPowerCost) return;
					this.energy.setEnergy(this.energy.getEnergyStored() - HostileConfig.fabPowerCost);
					if (--this.runtime == 0) {
						this.insertInOutput(dm.getFabDrops().get(selection).copy(), false);
						this.inventory.getStackInSlot(0).shrink(1);
					}
				}
			} else this.runtime = 0;
		} else this.runtime = 0;
	}

	protected boolean insertInOutput(ItemStack stack, boolean sim) {
		for (int i = 1; i < 17; i++) {
			stack = this.inventory.insertItemInternal(i, stack, sim);
			if (stack.isEmpty()) return true;
		}
		return false;
	}

	public IIntArray getRefHolder() {
		return this.references;
	}

	public FabItemHandler getInventory() {
		return this.inventory;
	}

	public void setSelection(DataModel model, int pId) {
		if (pId == -1) this.savedSelections.removeInt(model);
		else this.savedSelections.put(model, MathHelper.clamp(pId, 0, model.getFabDrops().size() - 1));
		VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return LazyOptional.of(() -> this.inventory).cast();
		if (cap == CapabilityEnergy.ENERGY) return LazyOptional.of(() -> this.energy).cast();
		return super.getCapability(cap, side);
	}

	@Override
	public CompoundNBT save(CompoundNBT tag) {
		tag = super.save(tag);
		tag.put("saved_selections", this.writeSelections(new CompoundNBT()));
		tag.put("inventory", this.inventory.serializeNBT());
		tag.putInt("energy", this.energy.getEnergyStored());
		tag.putInt("runtime", this.runtime);
		tag.putInt("selection", this.currentSel);
		return tag;
	}

	@Override
	public void load(BlockState state, CompoundNBT tag) {
		super.load(state, tag);
		this.readSelections(tag.getCompound("saved_selections"));
		this.inventory.deserializeNBT(tag.getCompound("inventory"));
		this.energy.setEnergy(tag.getInt("energy"));
		this.runtime = tag.getInt("runtime");
		this.currentSel = tag.getInt("selection");
	}

	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		CompoundNBT tag = new CompoundNBT();
		tag.put("saved_selections", this.writeSelections(new CompoundNBT()));
		return new SUpdateTileEntityPacket(this.getBlockPos(), 0, tag);
	}

	@Override
	public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
		this.readSelections(pkt.getTag().getCompound("saved_selections"));
	}

	@Override
	public CompoundNBT getUpdateTag() {
		CompoundNBT tag = super.getUpdateTag();
		tag.put("saved_selections", this.writeSelections(new CompoundNBT()));
		return tag;
	}

	private CompoundNBT writeSelections(CompoundNBT tag) {
		for (Object2IntMap.Entry<DataModel> e : this.savedSelections.object2IntEntrySet()) {
			tag.putInt(e.getKey().getId().toString(), e.getIntValue());
		}
		return tag;
	}

	private void readSelections(CompoundNBT tag) {
		this.savedSelections.clear();
		for (String s : tag.getAllKeys()) {
			DataModel dm = DataModelManager.INSTANCE.getModel(new ResourceLocation(s));
			this.savedSelections.put(dm, MathHelper.clamp(tag.getInt(s), 0, dm.getFabDrops().size() - 1));
		}
	}

	public int getEnergyStored() {
		return this.energy.getEnergyStored();
	}

	public int getRuntime() {
		return this.runtime;
	}

	public int getSelectedDrop(DataModel model) {
		return model == null ? -1 : this.savedSelections.getInt(model);
	}

	public class FabItemHandler extends ItemStackHandler {

		public FabItemHandler() {
			super(17);
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			if (slot == 0) return stack.getItem() == Hostile.Items.PREDICTION;
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

		public ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
			return super.extractItem(slot, amount, simulate);
		}

		public ItemStack insertItemInternal(int slot, ItemStack stack, boolean simulate) {
			return super.insertItem(slot, stack, simulate);
		}
	}

}
