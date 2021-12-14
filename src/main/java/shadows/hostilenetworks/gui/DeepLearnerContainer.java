package shadows.hostilenetworks.gui;

import java.util.function.Consumer;

import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.data.CachedModel;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.DeepLearnerItem;
import shadows.placebo.container.PlaceboContainerMenu;

public class DeepLearnerContainer extends PlaceboContainerMenu {

	protected final InteractionHand hand;
	protected final Player player;
	protected final ItemStack deepLearner;
	protected final ItemStackHandler learnerInv;
	protected Consumer<Integer> notifyCallback;

	public DeepLearnerContainer(int id, Inventory pInv, InteractionHand hand) {
		super(Hostile.Containers.DEEP_LEARNER, id, pInv);
		this.hand = hand;
		this.player = pInv.player;
		this.deepLearner = this.player.getItemInHand(hand);
		this.learnerInv = DeepLearnerItem.getItemHandler(this.deepLearner);

		this.addSlot(new DataModelSlot(this.learnerInv, 0, 256, 99));
		this.addSlot(new DataModelSlot(this.learnerInv, 1, 274, 99));
		this.addSlot(new DataModelSlot(this.learnerInv, 2, 256, 117));
		this.addSlot(new DataModelSlot(this.learnerInv, 3, 274, 117));

		this.playerInvStart = this.slots.size();
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int x = 89 + column * 18;
				int y = 153 + row * 18;
				int index = column + row * 9 + 9;
				Slot slot = new Slot(this.player.getInventory(), index, x, y);
				this.addSlot(slot);
			}
		}

		this.hotbarStart = this.slots.size();
		for (int row = 0; row < 9; row++) {
			int index = row;
			Slot slot = new Slot(this.player.getInventory(), index, 89 + row * 18, 211);
			if (hand == InteractionHand.MAIN_HAND && index == this.player.getInventory().selected) {
				slot = new LockedSlot(this.player.getInventory(), index, 89 + row * 18, 211);
			}
			this.addSlot(slot);
		}

		this.mover.registerRule((stack, slot) -> slot < 4, 4, slots.size());
		this.mover.registerRule((stack, slot) -> stack.getItem() instanceof DataModelItem, 0, 4);
		this.registerInvShuffleRules();
	}

	public void setNotifyCallback(Consumer<Integer> r) {
		this.notifyCallback = r;
	}

	@Override
	public boolean stillValid(Player pPlayer) {
		return this.deepLearner.getItem() == Hostile.Items.DEEP_LEARNER && this.player.getItemInHand(this.hand) == this.deepLearner;
	}

	@Override
	public void removed(Player pPlayer) {
		super.removed(pPlayer);
		DeepLearnerItem.saveItems(this.deepLearner, this.learnerInv);
		pPlayer.getInventory().setChanged();
	}

	public boolean hasModels() {
		boolean hasModels = false;
		for (int i = 0; i < 4; i++) {
			if (!this.learnerInv.getStackInSlot(i).isEmpty()) hasModels = true;
		}
		return hasModels;
	}

	public void fillWithModels(CachedModel[] models) {
		for (int i = 0; i < 4; i++) {
			ItemStack stack = this.learnerInv.getStackInSlot(i);
			models[i] = stack.isEmpty() ? null : new CachedModel(stack, i);
		}
	}

	public class DataModelSlot extends SlotItemHandler {

		public DataModelSlot(IItemHandler handler, int index, int x, int y) {
			super(handler, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return stack.getItem() instanceof DataModelItem;
		}

		@Override
		public int getMaxStackSize() {
			return 1;
		}

		@Override
		public void setChanged() {
			super.setChanged();
			if (DeepLearnerContainer.this.notifyCallback != null) {
				DeepLearnerContainer.this.notifyCallback.accept(((Slot) this).index);
			}
		}
	}

	public class LockedSlot extends Slot {

		public LockedSlot(Container inv, int index, int x, int y) {
			super(inv, index, x, y);
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

}
