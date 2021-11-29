package shadows.hostilenetworks.gui;

import java.util.function.Consumer;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.data.CachedModel;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.DeepLearnerItem;

public class DeepLearnerContainer extends Container {

	protected final Hand hand;
	protected final PlayerEntity player;
	protected final ItemStack deepLearner;
	protected final ItemStackHandler learnerInv;
	protected Consumer<Integer> notifyCallback;

	public DeepLearnerContainer(int id, PlayerInventory pInv, Hand hand) {
		super(Hostile.Containers.DEEP_LEARNER, id);
		this.hand = hand;
		this.player = pInv.player;
		this.deepLearner = player.getItemInHand(hand);
		this.learnerInv = DeepLearnerItem.getItemHandler(this.deepLearner);

		addSlot(new DataModelSlot(learnerInv, 0, 257, 100));
		addSlot(new DataModelSlot(learnerInv, 1, 275, 100));
		addSlot(new DataModelSlot(learnerInv, 2, 257, 118));
		addSlot(new DataModelSlot(learnerInv, 3, 275, 118));

		for (int row = 0; row < 9; row++) {
			int index = row;
			Slot slot = new Slot(player.inventory, index, 89 + row * 18, 211);
			if (hand == Hand.MAIN_HAND && index == player.inventory.selected) {
				slot = new LockedSlot(player.inventory, index, 89 + row * 18, 211);
			}
			addSlot(slot);
		}

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				int x = 89 + column * 18;
				int y = 153 + row * 18;
				int index = column + row * 9 + 9;
				Slot slot = new Slot(player.inventory, index, x, y);
				addSlot(slot);
			}
		}
	}

	public void setNotifyCallback(Consumer<Integer> r) {
		this.notifyCallback = r;
	}

	@Override
	public boolean stillValid(PlayerEntity pPlayer) {
		return deepLearner.getItem() == Hostile.Items.DEEP_LEARNER && this.player.getItemInHand(this.hand) == this.deepLearner;
	}

	@Override
	public void removed(PlayerEntity pPlayer) {
		super.removed(pPlayer);
		DeepLearnerItem.saveItems(this.deepLearner, this.learnerInv);
		pPlayer.inventory.setChanged();
	}

	public boolean hasModels() {
		boolean hasModels = false;
		for (int i = 0; i < 4; i++) {
			if (!learnerInv.getStackInSlot(i).isEmpty()) hasModels = true;
		}
		return hasModels;
	}

	public void fillWithModels(CachedModel[] models) {
		for (int i = 0; i < 4; i++) {
			ItemStack stack = learnerInv.getStackInSlot(i);
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

		public LockedSlot(IInventory inv, int index, int x, int y) {
			super(inv, index, x, y);
		}

		@Override
		public boolean mayPickup(PlayerEntity player) {
			return false;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}

}
