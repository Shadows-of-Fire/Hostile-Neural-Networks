package dev.shadowsoffire.hostilenetworks;

import dev.shadowsoffire.hostilenetworks.client.DeepLearnerHudRenderer;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerScreen;
import dev.shadowsoffire.hostilenetworks.gui.LootFabScreen;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberScreen;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT, modid = HostileNetworks.MODID)
public class HostileClient {

    @SubscribeEvent
    public static void init(FMLClientSetupEvent e) {
        e.enqueueWork(() -> {
            MenuScreens.register(Hostile.Containers.DEEP_LEARNER.get(), DeepLearnerScreen::new);
            MenuScreens.register(Hostile.Containers.SIM_CHAMBER.get(), SimChamberScreen::new);
            MenuScreens.register(Hostile.Containers.LOOT_FABRICATOR.get(), LootFabScreen::new);
        });
    }

    @SubscribeEvent
    public static void mrl(ModelEvent.RegisterAdditional e) {
        e.register(new ResourceLocation(HostileNetworks.MODID, "item/data_model_base"));
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
        }, Hostile.Items.PREDICTION.get());
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("deep_learner", new DeepLearnerHudRenderer());
    }

}
