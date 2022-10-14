package shadows.hostilenetworks;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;
import shadows.hostilenetworks.gui.DeepLearnerContainer;
import shadows.hostilenetworks.gui.LootFabContainer;
import shadows.hostilenetworks.gui.SimChamberContainer;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.tile.LootFabTileEntity;
import shadows.hostilenetworks.tile.SimChamberTileEntity;
import shadows.placebo.util.RegObjHelper;

public class Hostile {

	private static final RegObjHelper R = new RegObjHelper(HostileNetworks.MODID);

	public static class Items {
		public static final RegistryObject<Item> BLANK_DATA_MODEL = R.item("BLANK_DATA_MODEL");
		public static final RegistryObject<Item> EMPTY_PREDICTION = R.item("EMPTY_PREDICTION");
		public static final RegistryObject<Item> DEEP_LEARNER = R.item("DEEP_LEARNER");
		public static final RegistryObject<DataModelItem> DATA_MODEL = R.item("DATA_MODEL");
		public static final RegistryObject<MobPredictionItem> PREDICTION = R.item("PREDICTION");
	}

	public static class Blocks {
		public static final RegistryObject<Block> SIM_CHAMBER = R.block("SIM_CHAMBER");
		public static final RegistryObject<Block> LOOT_FABRICATOR = R.block("LOOT_FABRICATOR");
	}

	public static class TileEntities {
		public static final RegistryObject<BlockEntityType<SimChamberTileEntity>> SIM_CHAMBER = R.blockEntity("SIM_CHAMBER");
		public static final RegistryObject<BlockEntityType<LootFabTileEntity>> LOOT_FABRICATOR = R.blockEntity("LOOT_FABRICATOR");
	}

	public static class Containers {
		public static final RegistryObject<MenuType<DeepLearnerContainer>> DEEP_LEARNER = R.menu("DEEP_LEARNER");
		public static final RegistryObject<MenuType<SimChamberContainer>> SIM_CHAMBER = R.menu("SIM_CHAMBER");
		public static final RegistryObject<MenuType<LootFabContainer>> LOOT_FABRICATOR = R.menu("LOOT_FABRICATOR");
	}

}
