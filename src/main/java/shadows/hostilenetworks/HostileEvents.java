package shadows.hostilenetworks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.items.ItemStackHandler;
import shadows.hostilenetworks.Hostile.Items;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.DeepLearnerItem;

@EventBusSubscriber(modid = HostileNetworks.MODID)
public class HostileEvents {

	@SubscribeEvent
	public static void modelAttunement(EntityInteractSpecific e) {
		PlayerEntity player = e.getPlayer();
		ItemStack stack = player.getItemInHand(e.getHand());
		if (stack.getItem() == Hostile.Items.BLANK_DATA_MODEL) {
			if (!player.level.isClientSide) {
				DataModel model = DataModelManager.INSTANCE.getModel(e.getTarget().getType());
				if (model == null) {
					ITextComponent msg = new TranslationTextComponent("hostilenetworks.msg.no_model").withStyle(TextFormatting.RED);
					player.sendMessage(msg, Util.NIL_UUID);
					return;
				}

				ITextComponent msg = new TranslationTextComponent("hostilenetworks.msg.built", model.getName()).withStyle(TextFormatting.GOLD);
				player.sendMessage(msg, Util.NIL_UUID);

				ItemStack modelStack = new ItemStack(Hostile.Items.DATA_MODEL);
				DataModelItem.setStoredModel(modelStack, model);
				player.setItemInHand(e.getHand(), modelStack);
			}
			e.setResult(Result.DENY);
		}
	}

	@SubscribeEvent
	public static void reloads(AddReloadListenerEvent e) {
		e.addListener(DataModelManager.INSTANCE);
	}

	@SubscribeEvent
	public static void kill(LivingDeathEvent e) {
		Entity src = e.getSource().getEntity();
		if (src instanceof ServerPlayerEntity) {
			ServerPlayerEntity p = (ServerPlayerEntity) src;
			p.inventory.items.stream().filter(s -> s.getItem() == Items.DEEP_LEARNER).forEach(dl -> updateModels(dl, e.getEntityLiving().getType(), 0));
			if (p.getOffhandItem().getItem() == Items.DEEP_LEARNER) updateModels(p.getOffhandItem(), e.getEntityLiving().getType(), 0);
		}
	}

	public static void updateModels(ItemStack learner, EntityType<?> type, int bonus) {
		ItemStackHandler handler = DeepLearnerItem.getItemHandler(learner);
		for (int i = 0; i < 4; i++) {
			ItemStack model = handler.getStackInSlot(i);
			if (model.isEmpty()) continue;
			DataModel dModel = DataModelItem.getStoredModel(model);
			if (dModel.getType() == type) {
				int data = DataModelItem.getData(model);
				ModelTier tier = ModelTier.getByData(data);
				DataModelItem.setData(model, data + tier.dataPerKill + bonus);
			}
		}
	}

	@SubscribeEvent
	public static void sync(OnDatapackSyncEvent e) {
		PlayerEntity p = e.getPlayer();
		if (!p.level.isClientSide) {
			DataModelManager.dispatch(p);
		}
	}

}
