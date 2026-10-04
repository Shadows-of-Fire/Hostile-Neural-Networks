package dev.shadowsoffire.hostilenetworks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.shadowsoffire.hostilenetworks.Hostile.Items;
import dev.shadowsoffire.hostilenetworks.Hostile.Tabs;
import dev.shadowsoffire.hostilenetworks.HostileConfig.ConfigPayload;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.ModelTierRegistry;
import dev.shadowsoffire.hostilenetworks.datagen.BlockTagProvider;
import dev.shadowsoffire.hostilenetworks.datagen.HNNRecipeProvider;
import dev.shadowsoffire.hostilenetworks.datagen.LootProvider;
import dev.shadowsoffire.hostilenetworks.net.OpenDeepLearnerPayload;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterIOPortTileEntity;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@Mod(HostileNetworks.MODID)
public class HostileNetworks {

    public static final String MODID = "hostilenetworks";
    public static final String VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
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
            TabFillingRegistry.register(Tabs.HNN_TAB.getKey(), Items.SIM_CHAMBER, Items.LOOT_FABRICATOR, Items.DATA_CENTER, Items.DATA_CENTER_IO_PORT,
                Items.DEEP_LEARNER, Items.BLANK_DATA_MODEL, Items.PREDICTION_MATRIX, Items.FAB_DIRECTIVE,
                Items.OVERWORLD_PREDICTION, Items.NETHER_PREDICTION, Items.END_PREDICTION, Items.TWILIGHT_PREDICTION, Items.DATA_MODEL, Items.PREDICTION);
        });
        DataModelRegistry.INSTANCE.registerToBus();
        ModelTierRegistry.INSTANCE.registerToBus();
    }

    @SubscribeEvent
    public void tickets(RegisterTicketControllersEvent e) {
        e.register(Hostile.Tickets.DATA_CENTER);
    }

    @SubscribeEvent
    public void caps(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.Energy.BLOCK, Hostile.TileEntities.LOOT_FABRICATOR, (be, side) -> be.getEnergy());
        e.registerBlockEntity(Capabilities.Item.BLOCK, Hostile.TileEntities.LOOT_FABRICATOR, (be, side) -> be.getInventory());
        e.registerBlockEntity(Capabilities.Energy.BLOCK, Hostile.TileEntities.SIM_CHAMBER, (be, side) -> be.getEnergy());
        e.registerBlockEntity(Capabilities.Item.BLOCK, Hostile.TileEntities.SIM_CHAMBER, (be, side) -> be.getInventory());
        e.registerBlockEntity(Capabilities.Energy.BLOCK, Hostile.TileEntities.DATA_CENTER, (be, side) -> be.getEnergy());
        e.registerBlockEntity(Capabilities.Item.BLOCK, Hostile.TileEntities.DATA_CENTER, (be, side) -> be.getInventory());
        e.registerBlockEntity(Capabilities.Energy.BLOCK, Hostile.TileEntities.IO_PORT, DataCenterIOPortTileEntity::getEnergyHandler);
        e.registerBlockEntity(Capabilities.Item.BLOCK, Hostile.TileEntities.IO_PORT, DataCenterIOPortTileEntity::getItemHandler);
    }

    @SubscribeEvent
    public void data(GatherDataEvent.Client e) {
        e.getGenerator().addProvider(true, LootProvider.create(e.getGenerator().getPackOutput(), e.getLookupProvider()));
        e.getGenerator().addProvider(true, new BlockTagProvider(
            e.getGenerator().getPackOutput(), e.getLookupProvider()));
        e.getGenerator().addProvider(true, new HNNRecipeProvider(
            e.getGenerator().getPackOutput(), e.getLookupProvider()));
    }

    public static Identifier loc(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    /**
     * Constructs a mutable component with a lang key of the form "type.modid.path", using {@link HostileNetworks#MODID}.
     *
     * @param type The type of language key, "misc", "info", "title", etc...
     * @param path The path of the language key.
     * @param args Translation arguments passed to the created translatable component.
     */
    public static MutableComponent lang(String type, String path, Object... args) {
        return Component.translatable(langKey(type, path), args);
    }

    public static String langKey(String type, String path) {
        return type + "." + MODID + "." + path;
    }
}
