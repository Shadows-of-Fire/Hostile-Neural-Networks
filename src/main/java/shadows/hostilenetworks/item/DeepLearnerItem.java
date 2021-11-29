package shadows.hostilenetworks.item;

import java.util.List;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.ItemStackHandler;
import shadows.hostilenetworks.data.CachedModel;
import shadows.hostilenetworks.gui.DeepLearnerContainer;
import shadows.hostilenetworks.util.Color;
import shadows.placebo.util.ClientUtil;

public class DeepLearnerItem extends Item {

	public DeepLearnerItem(Properties pProperties) {
		super(pProperties);
	}

	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		if (!ctx.getLevel().isClientSide) NetworkHooks.openGui((ServerPlayerEntity) ctx.getPlayer(), new Provider(ctx.getHand()), buf -> buf.writeBoolean(ctx.getHand() == Hand.MAIN_HAND));
		return ActionResultType.CONSUME;
	}

	@Override
	public ActionResult<ItemStack> use(World pLevel, PlayerEntity pPlayer, Hand pHand) {
		if (!pPlayer.level.isClientSide) NetworkHooks.openGui((ServerPlayerEntity) pPlayer, new Provider(pHand), buf -> buf.writeBoolean(pHand == Hand.MAIN_HAND));
		return ActionResult.consume(pPlayer.getItemInHand(pHand));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(ItemStack pStack, World pLevel, List<ITextComponent> list, ITooltipFlag pFlag) {
		list.add(new TranslationTextComponent("hostilenetworks.info.deep_learner", Color.withColor("hostilenetworks.color_text.hud", Color.WHITE)).withStyle(TextFormatting.GRAY));
		if (ClientUtil.isHoldingShift()) {
			ItemStackHandler inv = getItemHandler(pStack);
			boolean empty = true;
			for (int i = 0; i < 4; i++)
				if (!inv.getStackInSlot(i).isEmpty()) empty = false;
			if (empty) return;
			list.add(new TranslationTextComponent("hostilenetworks.info.dl_contains").withStyle(TextFormatting.GRAY));
			for (int i = 0; i < 4; i++) {
				ItemStack stack = inv.getStackInSlot(i);
				if (stack.isEmpty()) continue;
				CachedModel model = new CachedModel(stack, 0);
				if (model.getModel() == null) continue;
				list.add(new TranslationTextComponent("- %s %s", model.getTier().getComponent(), stack.getItem().getName(stack)).withStyle(TextFormatting.GRAY));
			}
		} else {
			ItemStackHandler inv = getItemHandler(pStack);
			boolean empty = true;
			for (int i = 0; i < 4; i++)
				if (!inv.getStackInSlot(i).isEmpty()) empty = false;
			if (empty) return;
			list.add(new TranslationTextComponent("hostilenetworks.info.hold_shift", Color.withColor("hostilenetworks.color_text.shift", Color.WHITE)).withStyle(TextFormatting.GRAY));
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

	protected class Provider implements INamedContainerProvider {

		private final Hand hand;

		protected Provider(Hand hand) {
			this.hand = hand;
		}

		@Override
		public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
			return new DeepLearnerContainer(id, inv, hand);
		}

		@Override
		public ITextComponent getDisplayName() {
			return new TranslationTextComponent("hostilenetworks.title.deep_learner");
		}
	}
}
