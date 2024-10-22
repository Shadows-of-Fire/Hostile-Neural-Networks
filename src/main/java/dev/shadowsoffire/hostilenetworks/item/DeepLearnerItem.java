package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModelInstance;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerContainer;
import dev.shadowsoffire.hostilenetworks.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
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
            ctx.getPlayer().openMenu(new Provider(ctx.getHand()), buf -> buf.writeBoolean(ctx.getHand() == InteractionHand.MAIN_HAND));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.openMenu(new Provider(hand), buf -> buf.writeBoolean(hand == InteractionHand.MAIN_HAND));
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable("hostilenetworks.info.deep_learner", Color.withColor("hostilenetworks.color_text.hud", Color.WHITE)).withStyle(ChatFormatting.GRAY));
        if (Screen.hasShiftDown()) {
            ComponentItemHandler inv = getItemHandler(stack);
            boolean empty = true;
            for (int i = 0; i < 4; i++)
                if (!inv.getStackInSlot(i).isEmpty()) empty = false;
            if (empty) return;
            list.add(Component.translatable("hostilenetworks.info.dl_contains").withStyle(ChatFormatting.GRAY));
            for (int i = 0; i < 4; i++) {
                DataModelInstance model = new DataModelInstance(inv.getStackInSlot(i), i);
                if (!model.isValid()) continue;
                list.add(Component.translatable("- %s %s", model.getTier().getComponent(), stack.getItem().getName(stack)).withStyle(ChatFormatting.GRAY));
            }
        }
        else {
            ComponentItemHandler inv = getItemHandler(stack);
            boolean empty = true;
            for (int i = 0; i < 4; i++)
                if (!inv.getStackInSlot(i).isEmpty()) empty = false;
            if (empty) return;
            list.add(Component.translatable("hostilenetworks.info.hold_shift", Color.withColor("hostilenetworks.color_text.shift", Color.WHITE)).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem();
    }

    public static ComponentItemHandler getItemHandler(ItemStack stack) {
        return new ComponentItemHandler(stack, Hostile.Components.LEARNER_INV, 4);
    }

    protected class Provider implements MenuProvider {

        private final InteractionHand hand;

        protected Provider(InteractionHand hand) {
            this.hand = hand;
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
            return new DeepLearnerContainer(id, inv, this.hand);
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("hostilenetworks.title.deep_learner");
        }
    }
}
