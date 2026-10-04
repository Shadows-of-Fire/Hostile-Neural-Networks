package dev.shadowsoffire.hostilenetworks.item;

import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerMenu;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerMenu.DeepLearnerSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemAccessItemHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class DeepLearnerItem extends Item {

    public DeepLearnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().isClientSide()) {
            DeepLearnerSource src = DeepLearnerSource.fromHand(ctx.getHand());
            ctx.getPlayer().openMenu(new Provider(src), buf -> buf.writeByte(src.ordinal()));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        DeepLearnerSource src = DeepLearnerSource.fromHand(hand);
        player.openMenu(new Provider(src), buf -> buf.writeByte(src.ordinal()));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> list, TooltipFlag flag) {
        list.accept(Component.literal("DL_INV_MARKER"));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    public static ItemAccessItemHandler getItemHandler(ItemStack stack) {
        return new ItemAccessItemHandler(ItemAccess.forStack(stack), Hostile.Components.LEARNER_INV, 4);
    }

    /**
     * Overwrites the contents of a slot of a learner inventory handler, regardless of what is currently stored there.
     */
    public static void setStackInSlot(ItemAccessItemHandler handler, int slot, ItemStack stack) {
        try (Transaction tx = Transaction.openRoot()) {
            ItemResource current = handler.getResource(slot);
            if (!current.isEmpty()) handler.extract(slot, current, handler.getAmountAsInt(slot), tx);
            if (!stack.isEmpty()) handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
        }
    }

    public static class Provider implements MenuProvider {

        private final DeepLearnerSource src;

        public Provider(DeepLearnerSource src) {
            this.src = src;
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new DeepLearnerMenu(id, inv, this.src);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("hostilenetworks.title.deep_learner");
        }
    }
}
