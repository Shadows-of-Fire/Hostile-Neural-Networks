package dev.shadowsoffire.hostilenetworks.gui;

import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.placebo.menu.PlaceboContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerCopySlot;

public class DeepLearnerContainer extends PlaceboContainerMenu {

    protected final InteractionHand hand;
    protected final Player player;
    protected final ItemStack deepLearner;
    protected final ComponentItemHandler learnerInv;
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

        this.mover.registerRule((stack, slot) -> slot < 4, 4, this.slots.size());
        this.mover.registerRule((stack, slot) -> stack.getItem() instanceof DataModelItem, 0, 4);
        this.registerInvShuffleRules();
    }

    public DeepLearnerContainer(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }

    public void setNotifyCallback(Consumer<Integer> r) {
        this.notifyCallback = r;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.deepLearner.is(Hostile.Items.DEEP_LEARNER) && this.player.getItemInHand(this.hand) == this.deepLearner;
    }

    public boolean hasModels() {
        boolean hasModels = false;
        for (int i = 0; i < 4; i++) {
            if (!this.learnerInv.getStackInSlot(i).isEmpty()) hasModels = true;
        }
        return hasModels;
    }

    public class DataModelSlot extends ItemHandlerCopySlot {

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
        protected void setStackCopy(ItemStack stack) {
            super.setStackCopy(stack);
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
