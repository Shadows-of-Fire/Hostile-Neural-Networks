package dev.shadowsoffire.hostilenetworks.gui;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.placebo.menu.PlaceboContainerMenu;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class FabDirectiveMenu extends PlaceboContainerMenu {

    protected final InteractionHand hand;
    protected final ItemStack fabDirective;
    protected final Object2IntMap<DynamicHolder<DataModel>> selections;

    public FabDirectiveMenu(int id, Inventory pInv, InteractionHand hand) {
        super(Hostile.Containers.FAB_DIRECTIVE, id, pInv);
        this.hand = hand;
        this.fabDirective = pInv.player.getItemInHand(hand);
        this.selections = new Object2IntOpenHashMap<>();

        // SavedSelections itemSelections = fabDirective.getOrDefault(Hostile.Components.FAB_SELECTIONS, SavedSelections.EMPTY);

        // this.addSlot(new DataModelSlot(this.learnerInv, 0, 256, 99));
        // this.addSlot(new DataModelSlot(this.learnerInv, 1, 274, 99));
        // this.addSlot(new DataModelSlot(this.learnerInv, 2, 256, 117));
        // this.addSlot(new DataModelSlot(this.learnerInv, 3, 274, 117));
        //
        // this.playerInvStart = this.slots.size();
        // for (int row = 0; row < 3; row++) {
        // for (int column = 0; column < 9; column++) {
        // int x = 89 + column * 18;
        // int y = 153 + row * 18;
        // int index = column + row * 9 + 9;
        // Slot slot = new Slot(this.player.getInventory(), index, x, y);
        // this.addSlot(slot);
        // }
        // }
        //
        // this.hotbarStart = this.slots.size();
        // for (int row = 0; row < 9; row++) {
        // int index = row;
        // Slot slot = new Slot(this.player.getInventory(), index, 89 + row * 18, 211);
        // if (source == DeepLearnerSource.MAIN_HAND && index == this.player.getInventory().selected) {
        // slot = new LockedSlot(this.player.getInventory(), index, 89 + row * 18, 211);
        // }
        // this.addSlot(slot);
        // }
        //
        // this.mover.registerRule((stack, slot) -> slot < 4, 4, this.slots.size());
        // this.mover.registerRule((stack, slot) -> stack.getItem() instanceof DataModelItem, 0, 4);
        // this.registerInvShuffleRules();
    }

    public FabDirectiveMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, InteractionHand.values()[buf.readByte()]);
    }

    @Override
    public boolean stillValid(Player player) {
        // TODO Auto-generated method stub
        return false;
    }

}
