package shadows.hostilenetworks;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import shadows.hostilenetworks.client.DeepLearnerHudRenderer;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.gui.DeepLearnerScreen;
import shadows.hostilenetworks.gui.LootFabScreen;
import shadows.hostilenetworks.gui.SimChamberScreen;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.util.ClientEntityCache;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class HostileClient {

	@SubscribeEvent
	public static void init(FMLClientSetupEvent e) {
		e.enqueueWork(() -> {
			MenuScreens.register(Hostile.Containers.DEEP_LEARNER.get(), DeepLearnerScreen::new);
			MenuScreens.register(Hostile.Containers.SIM_CHAMBER.get(), SimChamberScreen::new);
			MenuScreens.register(Hostile.Containers.LOOT_FABRICATOR.get(), LootFabScreen::new);
		});
		MinecraftForge.EVENT_BUS.addListener(HostileClient::tick);
	}

	@SubscribeEvent
	public static void mrl(ModelEvent.RegisterAdditional e) {
		e.register(new ResourceLocation(HostileNetworks.MODID, "item/data_model_base"));
	}

	@SubscribeEvent
	public static void colors(RegisterColorHandlersEvent.Item e) {
		e.register((stack, tint) -> {
			DataModel model = MobPredictionItem.getStoredModel(stack);
			int color = 0xFFFFFF;
			if (model != null) {
				color = model.getNameColor();
			}
			return color;
		}, Hostile.Items.PREDICTION.get());
	}

	@SubscribeEvent
	public static void overlays(RegisterGuiOverlaysEvent e) {
		e.registerAboveAll("deep_learner", new DeepLearnerHudRenderer());
	}

	@SubscribeEvent
	public static void stitch(TextureStitchEvent.Pre e) {
		e.addSprite(new ResourceLocation(HostileNetworks.MODID, "item/empty_learner_slot"));
	}

	public static void tick(ClientTickEvent e) {
		if (e.phase == Phase.START) ClientEntityCache.tick();
	}

}
