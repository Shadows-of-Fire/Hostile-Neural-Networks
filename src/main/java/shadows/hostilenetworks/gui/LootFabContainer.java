package shadows.hostilenetworks.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.tile.LootFabTileEntity;
import shadows.hostilenetworks.tile.LootFabTileEntity.FabItemHandler;
import shadows.placebo.container.BlockEntityContainer;
import shadows.placebo.container.FilteredSlot;

public class LootFabContainer extends BlockEntityContainer<LootFabTileEntity> {

	public LootFabContainer(int id, Inventory pInv, BlockPos pos) {
		super(Hostile.Containers.LOOT_FABRICATOR, id, pInv, pos);
		FabItemHandler inv = this.tile.getInventory();
		this.addSlot(new FilteredSlot(inv, 0, 79, 62, s -> s.getItem() == Hostile.Items.PREDICTION));

		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				this.addSlot(new FilteredSlot(inv, 1 + y * 4 + x, 100 + x * 18, 7 + y * 18, s -> false));
			}
		}

		this.addPlayerSlots(pInv, 8, 96);
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return pPlayer.level.getBlockState(this.pos).getBlock() == Hostile.Blocks.LOOT_FABRICATOR;
	}

	@Override
	public boolean clickMenuButton(Player pPlayer, int pId) {
		DataModel model = MobPredictionItem.getStoredModel(this.getSlot(0).getItem());
		if ((model == null) || (pId >= model.getFabDrops().size())) return false;
		this.tile.setSelection(model, pId);
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
		ItemStack slotStackCopy = ItemStack.EMPTY;
		Slot slot = this.slots.get(pIndex);
		if (slot != null && slot.hasItem()) {
			ItemStack slotStack = slot.getItem();
			slotStackCopy = slotStack.copy();
			if (pIndex < 17) {
				if (!this.moveItemStackTo(slotStack, 17, this.slots.size(), false)) return ItemStack.EMPTY;
			} else if (slotStack.getItem() instanceof MobPredictionItem) {
				if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
			} else if (pIndex < 17 + 9) {
				if (!this.moveItemStackTo(slotStack, 17 + 9, this.slots.size(), false)) return ItemStack.EMPTY;
			} else if (!this.moveItemStackTo(slotStack, 17, 17 + 9, false)) return ItemStack.EMPTY;
			if (slotStack.isEmpty()) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}
		}

		return slotStackCopy;
	}

	public int getEnergyStored() {
		return this.tile.getEnergyStored();
	}

	public int getRuntime() {
		return this.tile.getRuntime();
	}

	public int getSelectedDrop(DataModel model) {
		return this.tile.getSelectedDrop(model);
	}

}