package shadows.hostilenetworks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.util.Hand;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.IForgeRegistry;
import shadows.hostilenetworks.client.DataModelItemStackRenderer;
import shadows.hostilenetworks.gui.DeepLearnerContainer;
import shadows.hostilenetworks.item.BlankDataModelItem;
import shadows.hostilenetworks.item.DataModelItem;
import shadows.hostilenetworks.item.DeepLearnerItem;

@Mod(HostileNetworks.MODID)
public class HostileNetworks {

	public static final String MODID = "hostilenetworks";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public HostileNetworks() {
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
	}

	@SubscribeEvent
	public void registerItems(Register<Item> e) {
		IForgeRegistry<Item> reg = e.getRegistry();
		reg.register(new BlankDataModelItem(new Item.Properties().stacksTo(1)).setRegistryName("blank_data_model"));
		reg.register(new Item(new Item.Properties()).setRegistryName("polymer_clay"));
		reg.register(new DeepLearnerItem(new Item.Properties()).setRegistryName("deep_learner"));
		reg.register(new DataModelItem(new Item.Properties().stacksTo(1).setISTER(() -> DataModelItemStackRenderer::new)).setRegistryName("data_model"));
	}

	@SubscribeEvent
	public void containers(Register<ContainerType<?>> e) {
		IForgeRegistry<ContainerType<?>> reg = e.getRegistry();
		reg.register(new ContainerType<>((IContainerFactory<DeepLearnerContainer>) (id, inv, buf) -> new DeepLearnerContainer(id, inv, buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND)).setRegistryName("deep_learner"));
	}

}
