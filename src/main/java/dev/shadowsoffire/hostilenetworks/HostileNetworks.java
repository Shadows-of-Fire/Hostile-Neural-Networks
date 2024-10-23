package dev.shadowsoffire.hostilenetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.hostilenetworks.Hostile.Items;
import dev.shadowsoffire.hostilenetworks.Hostile.Tabs;
import dev.shadowsoffire.hostilenetworks.HostileConfig.ConfigPayload;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.datagen.LootProvider;
import dev.shadowsoffire.hostilenetworks.net.OpenDeepLearnerPayload;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(HostileNetworks.MODID)
public class HostileNetworks {

    public static final String MODID = "hostilenetworks";
    public static final String VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
    public static final Logger LOGGER = LogManager.getLogger(MODID);
    public static Configuration cfg;

    public HostileNetworks(IEventBus bus) {
        bus.register(this);
        cfg = HostileConfig.load();
        Hostile.bootstrap(bus);
        PayloadHelper.registerPayload(new ConfigPayload.Provider());
        PayloadHelper.registerPayload(new OpenDeepLearnerPayload.Provider());
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            TabFillingRegistry.register(Tabs.HNN_TAB.getKey(), Items.SIM_CHAMBER, Items.LOOT_FABRICATOR, Items.DEEP_LEARNER, Items.BLANK_DATA_MODEL, Items.PREDICTION_MATRIX, Items.OVERWORLD_PREDICTION, Items.NETHER_PREDICTION,
                Items.END_PREDICTION, Items.TWILIGHT_PREDICTION, Items.DATA_MODEL, Items.PREDICTION);
        });
        DataModelRegistry.INSTANCE.registerToBus();
        ModelTierRegistry.INSTANCE.registerToBus();
    }

    @SubscribeEvent
    public void caps(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(EnergyStorage.BLOCK, Hostile.TileEntities.LOOT_FABRICATOR, (be, side) -> be.getEnergy());
        e.registerBlockEntity(ItemHandler.BLOCK, Hostile.TileEntities.LOOT_FABRICATOR, (be, side) -> be.getInventory());
        e.registerBlockEntity(EnergyStorage.BLOCK, Hostile.TileEntities.SIM_CHAMBER, (be, side) -> be.getEnergy());
        e.registerBlockEntity(ItemHandler.BLOCK, Hostile.TileEntities.SIM_CHAMBER, (be, side) -> be.getInventory());
    }

    @SubscribeEvent
    public void data(GatherDataEvent e) {
        e.getGenerator().addProvider(true, LootProvider.create(e.getGenerator().getPackOutput(), e.getLookupProvider()));
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
