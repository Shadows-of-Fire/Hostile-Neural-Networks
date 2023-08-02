package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.hostilenetworks.client.DeepLearnerHudRenderer;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerScreen;
import dev.shadowsoffire.hostilenetworks.gui.LootFabScreen;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberScreen;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.util.ClientEntityCache;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

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

    public static void tick(ClientTickEvent e) {
        if (e.phase == Phase.START) ClientEntityCache.tick();
    }

}
