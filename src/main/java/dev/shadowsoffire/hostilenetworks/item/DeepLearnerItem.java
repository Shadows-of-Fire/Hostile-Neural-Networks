package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerContainer;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerContainer.DeepLearnerSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ComponentItemHandler;

public class DeepLearnerItem extends Item {

    public DeepLearnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().isClientSide) {
            DeepLearnerSource src = DeepLearnerSource.fromHand(ctx.getHand());
            ctx.getPlayer().openMenu(new Provider(src), buf -> buf.writeByte(src.ordinal()));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        DeepLearnerSource src = DeepLearnerSource.fromHand(hand);
        player.openMenu(new Provider(src), buf -> buf.writeByte(src.ordinal()));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("DL_INV_MARKER"));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    public static ComponentItemHandler getItemHandler(ItemStack stack) {
        return new ComponentItemHandler(stack, Hostile.Components.LEARNER_INV, 4);
    }

    public static class Provider implements MenuProvider {

        private final DeepLearnerSource src;

        public Provider(DeepLearnerSource src) {
            this.src = src;
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new DeepLearnerContainer(id, inv, this.src);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("hostilenetworks.title.deep_learner");
        }
    }
}
