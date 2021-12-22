package shadows.hostilenetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.IForgeRegistry;
import shadows.hostilenetworks.block.LootFabBlock;
import shadows.hostilenetworks.block.SimChamberBlock;
import shadows.hostilenetworks.client.DataModelItemStackRenderer;
import shadows.hostilenetworks.gui.DeepLearnerContainer;
import shadows.hostilenetworks.gui.LootFabContainer;
import shadows.hostilenetworks.gui.SimChamberContainer;
import shadows.hostilenetworks.item.BlankDataModelItem;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.DeepLearnerItem;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.net.DataModelMessage;
import shadows.hostilenetworks.net.DataModelResetMessage;
import shadows.hostilenetworks.tile.LootFabTileEntity;
import shadows.hostilenetworks.tile.SimChamberTileEntity;
import shadows.placebo.loot.LootSystem;
import shadows.placebo.util.NetworkUtils;

@Mod(HostileNetworks.MODID)
public class HostileNetworks {

	public static final String MODID = "hostilenetworks";
	public static final String VERSION = "1.0.3";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	//Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, MODID))
            .clientAcceptedVersions(s->true)
            .serverAcceptedVersions(s->true)
            .networkProtocolVersion(() -> "1.0.0")
            .simpleChannel();
    //Formatter::on

	public static final ItemGroup TAB = new ItemGroup(MODID) {

		@Override
		public ItemStack makeIcon() {
			return new ItemStack(Hostile.Blocks.SIM_CHAMBER);
		}

	};

	public HostileNetworks() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		NetworkUtils.registerMessage(CHANNEL, 0, new DataModelResetMessage());
		NetworkUtils.registerMessage(CHANNEL, 1, new DataModelMessage());
		HostileConfig.load();
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		IForgeRegistry<Block> reg = e.getRegistry();
		reg.register(new SimChamberBlock(Block.Properties.of(Material.STONE).lightLevel(s -> 1).strength(4, 3000).noOcclusion()).setRegistryName("sim_chamber"));
		reg.register(new LootFabBlock(Block.Properties.of(Material.STONE).lightLevel(s -> 1).strength(4, 3000).noOcclusion()).setRegistryName("loot_fabricator"));
	}

	@SubscribeEvent
	public void tiles(Register<TileEntityType<?>> e) {
		IForgeRegistry<TileEntityType<?>> reg = e.getRegistry();
		reg.register(new TileEntityType<>(SimChamberTileEntity::new, ImmutableSet.of(Hostile.Blocks.SIM_CHAMBER), null).setRegistryName("sim_chamber"));
		reg.register(new TileEntityType<>(LootFabTileEntity::new, ImmutableSet.of(Hostile.Blocks.LOOT_FABRICATOR), null).setRegistryName("loot_fabricator"));
	}

	@SubscribeEvent
	public void registerItems(Register<Item> e) {
		IForgeRegistry<Item> reg = e.getRegistry();
		reg.register(new DeepLearnerItem(new Item.Properties().tab(TAB)).setRegistryName("deep_learner"));
		reg.register(new BlankDataModelItem(new Item.Properties().stacksTo(1).tab(TAB)).setRegistryName("blank_data_model"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("empty_prediction"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("overworld_prediction"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("nether_prediction"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("end_prediction"));
		reg.register(new BlockItem(Hostile.Blocks.SIM_CHAMBER, new Item.Properties().tab(TAB)).setRegistryName("sim_chamber"));
		reg.register(new BlockItem(Hostile.Blocks.LOOT_FABRICATOR, new Item.Properties().tab(TAB)).setRegistryName("loot_fabricator"));
		reg.register(new DataModelItem(new Item.Properties().stacksTo(1).tab(TAB).setISTER(() -> DataModelItemStackRenderer::new)).setRegistryName("data_model"));
		reg.register(new MobPredictionItem(new Item.Properties().tab(TAB)).setRegistryName("prediction"));
	}

	@SubscribeEvent
	public void containers(Register<ContainerType<?>> e) {
		IForgeRegistry<ContainerType<?>> reg = e.getRegistry();
		reg.register(new ContainerType<>((IContainerFactory<DeepLearnerContainer>) (id, inv, buf) -> new DeepLearnerContainer(id, inv, buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND)).setRegistryName("deep_learner"));
		reg.register(new ContainerType<>((IContainerFactory<SimChamberContainer>) (id, inv, buf) -> new SimChamberContainer(id, inv, buf.readBlockPos())).setRegistryName("sim_chamber"));
		reg.register(new ContainerType<>((IContainerFactory<LootFabContainer>) (id, inv, buf) -> new LootFabContainer(id, inv, buf.readBlockPos())).setRegistryName("loot_fabricator"));
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		LootSystem.defaultBlockTable(Hostile.Blocks.LOOT_FABRICATOR);
		LootSystem.defaultBlockTable(Hostile.Blocks.SIM_CHAMBER);
	}

}
