package shadows.hostilenetworks;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.hostilenetworks.item.DataModelItem;

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

}
