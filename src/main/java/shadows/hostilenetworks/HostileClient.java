package shadows.hostilenetworks;

import net.minecraft.client.gui.ScreenManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import shadows.hostilenetworks.gui.DeepLearnerScreen;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class HostileClient {

	@SubscribeEvent
	public static void init(FMLClientSetupEvent e) {
		e.enqueueWork(() -> {
			ScreenManager.register(Hostile.Containers.DEEP_LEARNER, DeepLearnerScreen::new);
		});
	}

}
