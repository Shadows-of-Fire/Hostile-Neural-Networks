package dev.shadowsoffire.hostilenetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.hostilenetworks.Hostile.Items;
import dev.shadowsoffire.hostilenetworks.Hostile.Tabs;
import dev.shadowsoffire.hostilenetworks.HostileConfig.ConfigMessage;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.placebo.loot.LootSystem;
import dev.shadowsoffire.placebo.network.MessageHelper;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

@Mod(HostileNetworks.MODID)
public class HostileNetworks {

    public static final String MODID = "hostilenetworks";
    public static final String VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(MODID, MODID))
        .clientAcceptedVersions(s -> true)
        .serverAcceptedVersions(s -> true)
        .networkProtocolVersion(() -> "1.0.0")
        .simpleChannel();

    public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(MODID, "tab"));

    public HostileNetworks() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        HostileConfig.load();
        Hostile.bootstrap();
        MessageHelper.registerMessage(CHANNEL, 0, new ConfigMessage.Provider());
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            LootSystem.defaultBlockTable(Hostile.Blocks.LOOT_FABRICATOR.get());
            LootSystem.defaultBlockTable(Hostile.Blocks.SIM_CHAMBER.get());
            TabFillingRegistry.register(Tabs.HNN_TAB_KEY, Items.SIM_CHAMBER, Items.LOOT_FABRICATOR, Items.DEEP_LEARNER, Items.BLANK_DATA_MODEL, Items.PREDICTION_MATRIX, Items.OVERWORLD_PREDICTION, Items.NETHER_PREDICTION,
                Items.END_PREDICTION, Items.TWILIGHT_PREDICTION, Items.DATA_MODEL, Items.PREDICTION);
        });
        DataModelRegistry.INSTANCE.registerToBus();
        ModelTierRegistry.INSTANCE.registerToBus();
    }

    public static ResourceLocation loc(String path) {
        return new ResourceLocation(MODID, path);
    }
}
