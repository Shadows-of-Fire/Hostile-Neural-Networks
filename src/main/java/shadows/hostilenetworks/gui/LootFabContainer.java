package shadows.hostilenetworks.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.tile.LootFabTileEntity;
import shadows.hostilenetworks.tile.LootFabTileEntity.FabItemHandler;
import shadows.placebo.container.BlockEntityContainer;
import shadows.placebo.container.FilteredSlot;

public class LootFabContainer extends BlockEntityContainer<LootFabTileEntity> {

	public LootFabContainer(int id, Inventory pInv, BlockPos pos) {
		super(Hostile.Containers.LOOT_FABRICATOR.get(), id, pInv, pos);
		FabItemHandler inv = this.tile.getInventory();
		this.addSlot(new FilteredSlot(inv, 0, 79, 62, s -> s.getItem() == Hostile.Items.PREDICTION.get()));
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				this.addSlot(new FilteredSlot(inv, 1 + y * 4 + x, 100 + x * 18, 7 + y * 18, s -> false));
			}
		}
		this.addPlayerSlots(pInv, 8, 96);
		this.mover.registerRule((stack, slot) -> slot == 0, 17, this.slots.size());
		this.mover.registerRule((stack, slot) -> stack.getItem() instanceof MobPredictionItem, 0, 1);
		this.mover.registerRule((stack, slot) -> slot < 17, 17, this.slots.size());
		this.registerInvShuffleRules();
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return pPlayer.level.getBlockState(this.pos).getBlock() == Hostile.Blocks.LOOT_FABRICATOR.get();
	}

	@Override
	public boolean clickMenuButton(Player pPlayer, int pId) {
		DataModel model = MobPredictionItem.getStoredModel(this.getSlot(0).getItem());
		if (model == null || pId >= model.getFabDrops().size()) return false;
		this.tile.setSelection(model, pId);
		return true;
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