package shadows.hostilenetworks;

import net.minecraft.client.gui.ScreenManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import shadows.hostilenetworks.gui.DeepLearnerScreen;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class HostileClient {

	public static int clientTicks = 0;

	@SubscribeEvent
	public static void init(FMLClientSetupEvent e) {
		e.enqueueWork(() -> {
			ScreenManager.register(Hostile.Containers.DEEP_LEARNER, DeepLearnerScreen::new);
		});
		MinecraftForge.EVENT_BUS.addListener(HostileClient::tick);
	}

	@SubscribeEvent
	public static void mrl(ModelRegistryEvent e) {
		ModelLoader.addSpecialModel(new ResourceLocation(HostileNetworks.MODID, "item/data_model_base"));
	}

	public static void tick(ClientTickEvent e) {
		if (e.phase == Phase.START) clientTicks++;
	}

}
