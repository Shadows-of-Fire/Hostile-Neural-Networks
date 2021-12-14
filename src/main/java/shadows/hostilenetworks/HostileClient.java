package shadows.hostilenetworks;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.gui.DeepLearnerScreen;
import shadows.hostilenetworks.gui.LootFabScreen;
import shadows.hostilenetworks.gui.SimChamberScreen;
import shadows.hostilenetworks.item.MobPredictionItem;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class HostileClient {

	public static int clientTicks = 0;

	@SubscribeEvent
	public static void init(FMLClientSetupEvent e) {
		e.enqueueWork(() -> {
			MenuScreens.register(Hostile.Containers.DEEP_LEARNER, DeepLearnerScreen::new);
			MenuScreens.register(Hostile.Containers.SIM_CHAMBER, SimChamberScreen::new);
			MenuScreens.register(Hostile.Containers.LOOT_FABRICATOR, LootFabScreen::new);
		});
		MinecraftForge.EVENT_BUS.addListener(HostileClient::tick);
	}

	@SubscribeEvent
	public static void mrl(ModelRegistryEvent e) {
		ModelLoader.addSpecialModel(new ResourceLocation(HostileNetworks.MODID, "item/data_model_base"));
	}

	@SubscribeEvent
	public static void colors(ColorHandlerEvent.Item e) {
		e.getItemColors().register((stack, tint) -> {
			DataModel model = MobPredictionItem.getStoredModel(stack);
			int color = 0xFFFFFF;
			if (model != null) {
				color = model.getNameColor();
			}
			return color;
		}, Hostile.Items.PREDICTION);
	}

	public static void tick(ClientTickEvent e) {
		if (e.phase == Phase.START) clientTicks++;
	}

}
