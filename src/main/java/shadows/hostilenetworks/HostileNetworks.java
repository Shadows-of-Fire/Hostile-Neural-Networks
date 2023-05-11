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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;
import shadows.hostilenetworks.block.LootFabBlock;
import shadows.hostilenetworks.block.SimChamberBlock;
import shadows.hostilenetworks.curios.CuriosCompat;
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
	public static final String VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
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
			return new ItemStack(Hostile.Blocks.SIM_CHAMBER.get());
		}

	};

	public HostileNetworks() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
		HostileConfig.load();
	}

	@SubscribeEvent
	public void register(RegisterEvent e) {
		if (e.getForgeRegistry() == (Object) ForgeRegistries.BLOCKS) blocks();
		if (e.getForgeRegistry() == (Object) ForgeRegistries.BLOCK_ENTITY_TYPES) tiles();
		if (e.getForgeRegistry() == (Object) ForgeRegistries.ITEMS) items();
		if (e.getForgeRegistry() == (Object) ForgeRegistries.MENU_TYPES) containers();
	}

	public void blocks() {
		IForgeRegistry<Block> reg = ForgeRegistries.BLOCKS;
		reg.register("sim_chamber", new SimChamberBlock(Block.Properties.of(Material.STONE).lightLevel(s -> 1).strength(4, 3000).noOcclusion()));
		reg.register("loot_fabricator", new LootFabBlock(Block.Properties.of(Material.STONE).lightLevel(s -> 1).strength(4, 3000).noOcclusion()));
	}

	public void tiles() {
		IForgeRegistry<BlockEntityType<?>> reg = ForgeRegistries.BLOCK_ENTITY_TYPES;
		reg.register("sim_chamber", new TickingBlockEntityType<>(SimChamberTileEntity::new, ImmutableSet.of(Hostile.Blocks.SIM_CHAMBER.get()), false, true));
		reg.register("loot_fabricator", new TickingBlockEntityType<>(LootFabTileEntity::new, ImmutableSet.of(Hostile.Blocks.LOOT_FABRICATOR.get()), false, true));
	}

	public void items() {
		IForgeRegistry<Item> reg = ForgeRegistries.ITEMS;
		reg.register("deep_learner", new DeepLearnerItem(new Item.Properties().stacksTo(1).tab(TAB)));
		reg.register("blank_data_model", new BlankDataModelItem(new Item.Properties().stacksTo(1).tab(TAB)));
		reg.register("empty_prediction", new Item(new Item.Properties().tab(TAB)));
		reg.register("overworld_prediction", new Item(new Item.Properties().tab(TAB)));
		reg.register("nether_prediction", new Item(new Item.Properties().tab(TAB)));
		reg.register("end_prediction", new Item(new Item.Properties().tab(TAB)));
		reg.register("twilight_prediction", new Item(new Item.Properties().tab(TAB)));
		reg.register("sim_chamber", new BlockItem(Hostile.Blocks.SIM_CHAMBER.get(), new Item.Properties().tab(TAB)));
		reg.register("loot_fabricator", new BlockItem(Hostile.Blocks.LOOT_FABRICATOR.get(), new Item.Properties().tab(TAB)));
		reg.register("data_model", new DataModelItem(new Item.Properties().stacksTo(1).tab(TAB)));
		reg.register("prediction", new MobPredictionItem(new Item.Properties().tab(TAB)));
	}

	public void containers() {
		IForgeRegistry<MenuType<?>> reg = ForgeRegistries.MENU_TYPES;
		reg.register("deep_learner", new MenuType<>((IContainerFactory<DeepLearnerContainer>) (id, inv, buf) -> new DeepLearnerContainer(id, inv, buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)));
		reg.register("sim_chamber", ContainerUtil.makeType(SimChamberContainer::new));
		reg.register("loot_fabricator", ContainerUtil.makeType(LootFabContainer::new));
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		LootSystem.defaultBlockTable(Hostile.Blocks.LOOT_FABRICATOR.get());
		LootSystem.defaultBlockTable(Hostile.Blocks.SIM_CHAMBER.get());
		DataModelManager.INSTANCE.registerToBus();
	}

	@SubscribeEvent
	public void imcEvent(InterModEnqueueEvent e) {
		if (ModList.get().isLoaded("curios")) CuriosCompat.sendIMC();
	}
}
