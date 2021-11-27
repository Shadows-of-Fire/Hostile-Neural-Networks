package shadows.hostilenetworks;

import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraftforge.registries.ObjectHolder;
import shadows.hostilenetworks.gui.DeepLearnerContainer;

public class Hostile {

	@ObjectHolder(HostileNetworks.MODID)
	public static class Items {
		public static final Item BLANK_DATA_MODEL = null;
		public static final Item POLYMER_CLAY = null;
	}

	@ObjectHolder(HostileNetworks.MODID)
	public static class Blocks {

	}

	@ObjectHolder(HostileNetworks.MODID)
	public static class TileEntities {

	}

	@ObjectHolder(HostileNetworks.MODID)
	public static class Containers {
		public static final ContainerType<DeepLearnerContainer> DEEP_LEARNER = null;
	}

}
