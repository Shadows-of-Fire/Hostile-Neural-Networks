package shadows.hostilenetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.IForgeRegistry;
import shadows.hostilenetworks.block.LootFabBlock;
import shadows.hostilenetworks.block.SimChamberBlock;
import shadows.hostilenetworks.data.DataModelManager;
import shadows.hostilenetworks.gui.DeepLearnerContainer;
import shadows.hostilenetworks.gui.LootFabContainer;
import shadows.hostilenetworks.gui.SimChamberContainer;
import shadows.hostilenetworks.item.BlankDataModelItem;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.DeepLearnerItem;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.tile.LootFabTileEntity;
import shadows.hostilenetworks.tile.SimChamberTileEntity;
import shadows.placebo.block_entity.TickingBlockEntityType;
import shadows.placebo.container.ContainerUtil;
import shadows.placebo.loot.LootSystem;

@Mod(HostileNetworks.MODID)
public class HostileNetworks {

	public static final String MODID = "hostilenetworks";
	public static final String VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString().substring(1);
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	//Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, MODID))
            .clientAcceptedVersions(s->true)
            .serverAcceptedVersions(s->true)
            .networkProtocolVersion(() -> "1.0.0")
            .simpleChannel();
    //Formatter::on

	public static final CreativeModeTab TAB = new CreativeModeTab(MODID) {

		@Override
		public ItemStack makeIcon() {
			return new ItemStack(Hostile.Blocks.SIM_CHAMBER);
		}

	};

	public HostileNetworks() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		HostileConfig.load();
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		IForgeRegistry<Block> reg = e.getRegistry();
		reg.register(new SimChamberBlock(Block.Properties.of(Material.STONE).lightLevel(s -> 1).strength(4, 3000).noOcclusion()).setRegistryName("sim_chamber"));
		reg.register(new LootFabBlock(Block.Properties.of(Material.STONE).lightLevel(s -> 1).strength(4, 3000).noOcclusion()).setRegistryName("loot_fabricator"));
	}

	@SubscribeEvent
	public void tiles(Register<BlockEntityType<?>> e) {
		IForgeRegistry<BlockEntityType<?>> reg = e.getRegistry();
		reg.register(new TickingBlockEntityType<>(SimChamberTileEntity::new, ImmutableSet.of(Hostile.Blocks.SIM_CHAMBER), false, true).setRegistryName("sim_chamber"));
		reg.register(new TickingBlockEntityType<>(LootFabTileEntity::new, ImmutableSet.of(Hostile.Blocks.LOOT_FABRICATOR), false, true).setRegistryName("loot_fabricator"));
	}

	@SubscribeEvent
	public void registerItems(Register<Item> e) {
		IForgeRegistry<Item> reg = e.getRegistry();
		reg.register(new DeepLearnerItem(new Item.Properties().stacksTo(1).tab(TAB)).setRegistryName("deep_learner"));
		reg.register(new BlankDataModelItem(new Item.Properties().stacksTo(1).tab(TAB)).setRegistryName("blank_data_model"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("empty_prediction"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("overworld_prediction"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("nether_prediction"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("end_prediction"));
		reg.register(new BlockItem(Hostile.Blocks.SIM_CHAMBER, new Item.Properties().tab(TAB)).setRegistryName("sim_chamber"));
		reg.register(new BlockItem(Hostile.Blocks.LOOT_FABRICATOR, new Item.Properties().tab(TAB)).setRegistryName("loot_fabricator"));
		reg.register(new DataModelItem(new Item.Properties().stacksTo(1).tab(TAB)).setRegistryName("data_model"));
		reg.register(new MobPredictionItem(new Item.Properties().tab(TAB)).setRegistryName("prediction"));
	}

	@SubscribeEvent
	public void containers(Register<MenuType<?>> e) {
		IForgeRegistry<MenuType<?>> reg = e.getRegistry();
		reg.register(new MenuType<>((IContainerFactory<DeepLearnerContainer>) (id, inv, buf) -> new DeepLearnerContainer(id, inv, buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)).setRegistryName("deep_learner"));
		reg.register(ContainerUtil.makeType(SimChamberContainer::new).setRegistryName("sim_chamber"));
		reg.register(ContainerUtil.makeType(LootFabContainer::new).setRegistryName("loot_fabricator"));
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		LootSystem.defaultBlockTable(Hostile.Blocks.LOOT_FABRICATOR);
		LootSystem.defaultBlockTable(Hostile.Blocks.SIM_CHAMBER);
		DataModelManager.INSTANCE.registerToBus();
	}

}
