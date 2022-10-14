package shadows.hostilenetworks.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.tile.SimChamberTileEntity;
import shadows.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import shadows.hostilenetworks.tile.SimChamberTileEntity.SimItemHandler;
import shadows.placebo.container.BlockEntityContainer;
import shadows.placebo.container.FilteredSlot;

public class SimChamberContainer extends BlockEntityContainer<SimChamberTileEntity> {

	public SimChamberContainer(int id, Inventory pInv, BlockPos pos) {
		super(Hostile.Containers.SIM_CHAMBER.get(), id, pInv, pos);
		SimItemHandler inventory = this.tile.getInventory();
		this.addSlot(new FilteredSlot(inventory, 0, -13, 1, s -> s.getItem() instanceof DataModelItem));
		this.addSlot(new FilteredSlot(inventory, 1, 176, 7, s -> DataModelItem.matchesInput(this.getSlot(0).getItem(), s)));
		this.addSlot(new FilteredSlot(inventory, 2, 196, 7, s -> false));
		this.addSlot(new FilteredSlot(inventory, 3, 186, 27, s -> false));
		this.addPlayerSlots(pInv, 36, 153);
		this.mover.registerRule((stack, slot) -> slot < 4, 4, this.slots.size());
		this.mover.registerRule((stack, slot) -> stack.getItem() instanceof DataModelItem, 0, 1);
		this.mover.registerRule((stack, slot) -> DataModelItem.matchesInput(this.getSlot(0).getItem(), stack), 1, 2);
		this.registerInvShuffleRules();
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return pPlayer.level.getBlockState(this.pos).getBlock() == Hostile.Blocks.SIM_CHAMBER.get();
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

}