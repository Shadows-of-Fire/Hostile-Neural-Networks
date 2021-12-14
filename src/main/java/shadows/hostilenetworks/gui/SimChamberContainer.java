package shadows.hostilenetworks.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.tile.SimChamberTileEntity;
import shadows.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import shadows.hostilenetworks.tile.SimChamberTileEntity.SimItemHandler;
import shadows.placebo.container.BlockEntityContainer;
import shadows.placebo.container.FilteredSlot;

public class SimChamberContainer extends BlockEntityContainer<SimChamberTileEntity> {

	public SimChamberContainer(int id, Inventory pInv, BlockPos pos) {
		super(Hostile.Containers.SIM_CHAMBER, id, pInv, pos);
		SimItemHandler inventory = this.tile.getInventory();
		this.addSlot(new FilteredSlot(inventory, 0, -13, 1, s -> s.getItem() instanceof DataModelItem));
		this.addSlot(new FilteredSlot(inventory, 1, 176, 7, s -> DataModelItem.matchesInput(this.getSlot(0).getItem(), s)));
		this.addSlot(new FilteredSlot(inventory, 2, 196, 7, s -> false));
		this.addSlot(new FilteredSlot(inventory, 3, 186, 27, s -> false));
		this.addPlayerSlots(pInv, 36, 153);
	}

	@Override
	public boolean stillValid(Player pPlayer) {
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
	public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
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

}