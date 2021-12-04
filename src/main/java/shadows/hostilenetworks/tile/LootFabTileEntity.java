package shadows.hostilenetworks.tile;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIntArray;
import net.minecraftforge.items.ItemStackHandler;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.HostileConfig;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.util.ModifiableEnergyStorage;

public class LootFabTileEntity extends TileEntity implements ITickableTileEntity {

	protected final FabItemHandler inventory = new FabItemHandler();
	protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.fabPowerCap, HostileConfig.fabPowerCap);
	protected final Object2IntMap<DataModel> savedSelections = new Object2IntOpenHashMap<>();

	protected int runtime = 0;

	protected IIntArray references = new IIntArray() {

		@Override
		public int get(int pIndex) {
			switch (pIndex) {
			case 0:
				return runtime;
			case 1:
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
				energy.setEnergy(pValue);
				return;
			}
		}

		@Override
		public int getCount() {
			return 2;
		}

	};

	public LootFabTileEntity() {
		super(Hostile.TileEntities.LOOT_FABRICATOR);
	}

	@Override
	public void tick() {

	}

	public IIntArray getRefHolder() {
		return this.references;
	}

	public FabItemHandler getInventory() {
		return this.inventory;
	}

	public void setSelection(DataModel model, int pId) {
		this.savedSelections.put(model, pId);
	}

	public int getEnergyStored() {
		return this.energy.getEnergyStored();
	}

	public int getRuntime() {
		return this.runtime;
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

	}

}
