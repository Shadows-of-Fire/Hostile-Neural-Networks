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
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.IForgeRegistry;
import shadows.hostilenetworks.block.SimChamberBlock;
import shadows.hostilenetworks.client.DataModelItemStackRenderer;
import shadows.hostilenetworks.gui.DeepLearnerContainer;
import shadows.hostilenetworks.gui.SimChamberContainer;
import shadows.hostilenetworks.item.BlankDataModelItem;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.DeepLearnerItem;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.tile.SimChamberTileEntity;

@Mod(HostileNetworks.MODID)
public class HostileNetworks {

	public static final String MODID = "hostilenetworks";
	public static final String VERSION = "1.0.0";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final ItemGroup TAB = new ItemGroup(MODID) {

		@Override
		public ItemStack makeIcon() {
			return new ItemStack(Hostile.Blocks.SIM_CHAMBER);
		}

	};

	public HostileNetworks() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		IForgeRegistry<Block> reg = e.getRegistry();
		reg.register(new SimChamberBlock(Block.Properties.of(Material.STONE).lightLevel(s -> 1).strength(4, 3000)).setRegistryName("sim_chamber"));
	}

	@SubscribeEvent
	public void tiles(Register<TileEntityType<?>> e) {
		IForgeRegistry<TileEntityType<?>> reg = e.getRegistry();
		reg.register(new TileEntityType<>(SimChamberTileEntity::new, ImmutableSet.of(Hostile.Blocks.SIM_CHAMBER), null).setRegistryName("sim_chamber"));
	}

	@SubscribeEvent
	public void registerItems(Register<Item> e) {
		IForgeRegistry<Item> reg = e.getRegistry();
		reg.register(new BlankDataModelItem(new Item.Properties().stacksTo(1).tab(TAB)).setRegistryName("blank_data_model"));
		reg.register(new Item(new Item.Properties().tab(TAB)).setRegistryName("polymer_clay"));
		reg.register(new DeepLearnerItem(new Item.Properties().tab(TAB)).setRegistryName("deep_learner"));
		reg.register(new DataModelItem(new Item.Properties().stacksTo(1).tab(TAB).setISTER(() -> DataModelItemStackRenderer::new)).setRegistryName("data_model"));
		reg.register(new BlockItem(Hostile.Blocks.SIM_CHAMBER, new Item.Properties().tab(TAB)).setRegistryName("sim_chamber"));
		reg.register(new MobPredictionItem(new Item.Properties().tab(TAB)).setRegistryName("prediction"));
	}

	@SubscribeEvent
	public void containers(Register<ContainerType<?>> e) {
		IForgeRegistry<ContainerType<?>> reg = e.getRegistry();
		reg.register(new ContainerType<>((IContainerFactory<DeepLearnerContainer>) (id, inv, buf) -> new DeepLearnerContainer(id, inv, buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND)).setRegistryName("deep_learner"));
		reg.register(new ContainerType<>((IContainerFactory<SimChamberContainer>) (id, inv, buf) -> new SimChamberContainer(id, inv, buf.readBlockPos())).setRegistryName("sim_chamber"));
	}

}
