package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.hostilenetworks.client.DeepLearnerHudRenderer;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerScreen;
import dev.shadowsoffire.hostilenetworks.gui.LootFabScreen;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberScreen;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class HostileClient {

    @SubscribeEvent
    public static void mrl(ModelEvent.RegisterAdditional e) {
        e.register(ModelResourceLocation.standalone(HostileNetworks.loc("item/data_model_base")));
    }

    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item e) {
        e.register((stack, tint) -> {
            DynamicHolder<DataModel> model = DataModelItem.getStoredModel(stack);
            int color = 0xFFFFFF;
            if (model.isBound()) {
                color = model.get().getNameColor();
            }
            return color;
        }, Hostile.Items.PREDICTION.value());
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiLayersEvent e) {
        e.registerAboveAll(HostileNetworks.loc("deep_learner"), new DeepLearnerHudRenderer());
    }

    @SubscribeEvent
    public static void screens(RegisterMenuScreensEvent e) {
        e.register(Hostile.Containers.DEEP_LEARNER, DeepLearnerScreen::new);
        e.register(Hostile.Containers.SIM_CHAMBER, SimChamberScreen::new);
        e.register(Hostile.Containers.LOOT_FABRICATOR, LootFabScreen::new);
    }

}
