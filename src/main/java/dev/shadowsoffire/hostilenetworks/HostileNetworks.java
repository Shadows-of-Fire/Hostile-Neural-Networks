package dev.shadowsoffire.hostilenetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.hostilenetworks.Hostile.Items;
import dev.shadowsoffire.hostilenetworks.Hostile.Tabs;
import dev.shadowsoffire.hostilenetworks.data.DataModelManager;
import dev.shadowsoffire.placebo.loot.LootSystem;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(HostileNetworks.MODID)
public class HostileNetworks {

    public static final String MODID = "hostilenetworks";
    public static final String VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    // Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(MODID, MODID))
        .clientAcceptedVersions(s -> true)
        .serverAcceptedVersions(s -> true)
        .networkProtocolVersion(() -> "1.0.0")
        .simpleChannel();
    // Formatter::on

    public static final ResourceKey<CreativeModeTab> TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(MODID, "tab"));

    public HostileNetworks() {
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
        HostileConfig.load();
        Hostile.bootstrap();
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            LootSystem.defaultBlockTable(Hostile.Blocks.LOOT_FABRICATOR.get());
            LootSystem.defaultBlockTable(Hostile.Blocks.SIM_CHAMBER.get());
            TabFillingRegistry.register(Tabs.HNN_TAB_KEY, Items.SIM_CHAMBER, Items.LOOT_FABRICATOR, Items.DEEP_LEARNER, Items.BLANK_DATA_MODEL, Items.PREDICTION_MATRIX, Items.OVERWORLD_PREDICTION, Items.NETHER_PREDICTION,
                Items.END_PREDICTION, Items.TWILIGHT_PREDICTION, Items.DATA_MODEL, Items.PREDICTION);
        });
        DataModelManager.INSTANCE.registerToBus();
    }
}
