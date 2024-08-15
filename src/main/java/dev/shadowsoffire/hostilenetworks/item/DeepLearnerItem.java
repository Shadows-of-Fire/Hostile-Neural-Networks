package dev.shadowsoffire.hostilenetworks.item;

import java.util.List;

import dev.shadowsoffire.hostilenetworks.data.CachedModel;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerContainer;
import dev.shadowsoffire.hostilenetworks.util.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DeepLearnerItem extends Item {

    public DeepLearnerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.getLevel().isClientSide) NetworkHooks.openScreen((ServerPlayer) ctx.getPlayer(), new Provider(ctx.getHand()), buf -> buf.writeBoolean(ctx.getHand() == InteractionHand.MAIN_HAND));
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        if (!pPlayer.level().isClientSide) NetworkHooks.openScreen((ServerPlayer) pPlayer, new Provider(pHand), buf -> buf.writeBoolean(pHand == InteractionHand.MAIN_HAND));
        return InteractionResultHolder.consume(pPlayer.getItemInHand(pHand));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack pStack, Level pLevel, List<Component> list, TooltipFlag pFlag) {
        list.add(Component.translatable("hostilenetworks.info.deep_learner", Color.withColor("hostilenetworks.color_text.hud", Color.WHITE)).withStyle(ChatFormatting.GRAY));
        if (Screen.hasShiftDown()) {
            ItemStackHandler inv = getItemHandler(pStack);
            boolean empty = true;
            for (int i = 0; i < 4; i++)
                if (!inv.getStackInSlot(i).isEmpty()) empty = false;
            if (empty) return;
            list.add(Component.translatable("hostilenetworks.info.dl_contains").withStyle(ChatFormatting.GRAY));
            for (int i = 0; i < 4; i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) continue;
                CachedModel model = new CachedModel(stack, 0);
                if (!model.isValid()) continue;
                list.add(Component.translatable("- %s %s", model.getTier().getComponent(), stack.getItem().getName(stack)).withStyle(ChatFormatting.GRAY));
            }
        }
        else {
            ItemStackHandler inv = getItemHandler(pStack);
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

    public static ItemStackHandler getItemHandler(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return new ItemStackHandler(4);
        ItemStackHandler handler = new ItemStackHandler(4);
        if (stack.hasTag() && stack.getTag().contains("learner_inv")) handler.deserializeNBT(stack.getTag().getCompound("learner_inv"));
        return handler;
    }

    public static void saveItems(ItemStack stack, ItemStackHandler handler) {
        stack.getOrCreateTag().put("learner_inv", handler.serializeNBT());
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
