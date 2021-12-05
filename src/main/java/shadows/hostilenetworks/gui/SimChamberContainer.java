package shadows.hostilenetworks.gui;

import java.util.function.Predicate;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.SlotItemHandler;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.tile.SimChamberTileEntity;
import shadows.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import shadows.hostilenetworks.tile.SimChamberTileEntity.SimItemHandler;

public class SimChamberContainer extends Container {

	protected final BlockPos pos;
	protected final World level;
	protected SimChamberTileEntity tile;

	public SimChamberContainer(int id, PlayerInventory pInv, BlockPos pos) {
		super(Hostile.Containers.SIM_CHAMBER, id);
		this.pos = pos;
		this.level = pInv.player.level;
		this.tile = (SimChamberTileEntity) this.level.getBlockEntity(pos);
		SimItemHandler inventory = this.tile.getInventory();
		this.addSlot(new FilteredSlot(inventory, 0, -13, 1, s -> s.getItem() instanceof DataModelItem));
		this.addSlot(new FilteredSlot(inventory, 1, 176, 7, s -> DataModelItem.matchesInput(this.getSlot(0).getItem(), s)));
		this.addSlot(new FilteredSlot(inventory, 2, 196, 7, s -> false));
		this.addSlot(new FilteredSlot(inventory, 3, 186, 27, s -> false));

		for (int row = 0; row < 9; row++) {
			this.addSlot(new Slot(pInv, row, 36 + row * 18, 211));
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(pInv, column + row * 9 + 9, 36 + column * 18, 153 + row * 18));
			}
		}
		this.addDataSlots(this.tile.getRefHolder());
	}

	@Override
	public boolean stillValid(PlayerEntity pPlayer) {
		return pPlayer.level.getBlockState(this.pos).getBlock() == Hostile.Blocks.SIM_CHAMBER;
	}

	public int getEnergyStored() {
		return this.tile.getEnergyStored();
	}

	public int getRuntime() {
		return this.tile.getRuntime();
	}

	public boolean didPredictionSucceed() {
		return this.tile.didPredictionSucceed();
	}

	public FailureState getFailState() {
		return this.tile.getFailState();
	}

	@Override
	public ItemStack quickMoveStack(PlayerEntity pPlayer, int pIndex) {
		ItemStack slotStackCopy = ItemStack.EMPTY;
		Slot slot = this.slots.get(pIndex);
		if (slot != null && slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			slotStackCopy = slotStack.copy();
			if (pIndex < 4) {
				if (!this.moveItemStackTo(slotStack, 4, this.slots.size(), false)) return ItemStack.EMPTY;
			} else if (slotStack.getItem() instanceof DataModelItem) {
				if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
			} else if (DataModelItem.matchesInput(this.getSlot(0).getItem(), slotStack)) {
				if (!this.moveItemStackTo(slotStack, 1, 2, false)) return ItemStack.EMPTY;
			} else if (pIndex < 4 + 9) {
				if (!this.moveItemStackTo(slotStack, 4 + 9, this.slots.size(), false)) return ItemStack.EMPTY;
			} else if (!this.moveItemStackTo(slotStack, 4, 13, false)) return ItemStack.EMPTY;

			if (slotStack.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return slotStackCopy;
	}

	protected class FilteredSlot extends SlotItemHandler {

		protected final Predicate<ItemStack> filter;
		protected final int index;

		public FilteredSlot(SimItemHandler handler, int index, int x, int y, Predicate<ItemStack> filter) {
			super(handler, index, x, y);
			this.filter = filter;
			this.index = index;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return this.filter.test(stack);
		}

		@Override
		public int getMaxStackSize() {
			return 64;
		}

		@Override
		public boolean mayPickup(PlayerEntity playerIn) {
			return true;
		}

		@Override
		public ItemStack remove(int amount) {
			return ((SimItemHandler) this.getItemHandler()).extractItemInternal(this.index, amount, false);
		}

	}

}