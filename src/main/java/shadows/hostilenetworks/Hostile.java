package shadows.hostilenetworks;

import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;
import shadows.hostilenetworks.gui.DeepLearnerContainer;
import shadows.hostilenetworks.gui.LootFabContainer;
import shadows.hostilenetworks.gui.SimChamberContainer;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.hostilenetworks.tile.LootFabTileEntity;
import shadows.hostilenetworks.tile.SimChamberTileEntity;

public class Hostile {

	@ObjectHolder(HostileNetworks.MODID)
	public static class Items {
		public static final Item BLANK_DATA_MODEL = null;
		public static final Item EMPTY_PREDICTION = null;
		public static final Item DEEP_LEARNER = null;
		public static final DataModelItem DATA_MODEL = null;
		public static final MobPredictionItem PREDICTION = null;
	}

	@ObjectHolder(HostileNetworks.MODID)
	public static class Blocks {
		public static final Block SIM_CHAMBER = null;
		public static final Block LOOT_FABRICATOR = null;
	}

	@ObjectHolder(HostileNetworks.MODID)
	public static class TileEntities {
		public static final TileEntityType<SimChamberTileEntity> SIM_CHAMBER = null;
		public static final TileEntityType<LootFabTileEntity> LOOT_FABRICATOR = null;
	}

	@ObjectHolder(HostileNetworks.MODID)
	public static class Containers {
		public static final ContainerType<DeepLearnerContainer> DEEP_LEARNER = null;
		public static final ContainerType<SimChamberContainer> SIM_CHAMBER = null;
		public static final ContainerType<LootFabContainer> LOOT_FABRICATOR = null;
	}

}
