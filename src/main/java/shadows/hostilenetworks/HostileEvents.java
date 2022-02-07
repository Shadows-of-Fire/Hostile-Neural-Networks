package shadows.hostilenetworks;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
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
		Player player = e.getPlayer();
		ItemStack stack = player.getItemInHand(e.getHand());
		if (stack.getItem() == Hostile.Items.BLANK_DATA_MODEL) {
			if (!player.level.isClientSide) {
				DataModel model = DataModelManager.INSTANCE.getForEntity(e.getTarget().getType());
				if (model == null) {
					Component msg = new TranslatableComponent("hostilenetworks.msg.no_model").withStyle(ChatFormatting.RED);
					player.sendMessage(msg, Util.NIL_UUID);
					return;
				}

				Component msg = new TranslatableComponent("hostilenetworks.msg.built", model.getName()).withStyle(ChatFormatting.GOLD);
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
		if (src instanceof ServerPlayer p) {
			p.getInventory().items.stream().filter(s -> s.getItem() == Items.DEEP_LEARNER).forEach(dl -> updateModels(dl, e.getEntityLiving().getType(), 0));
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
		DeepLearnerItem.saveItems(learner, handler);
	}
}
