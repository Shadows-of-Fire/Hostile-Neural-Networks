package shadows.hostilenetworks.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.item.DataModelItem;

@JeiPlugin
public class HostileJeiPlugin implements IModPlugin {

	@Override
	public void registerItemSubtypes(ISubtypeRegistration reg) {
		reg.registerSubtypeInterpreter(Hostile.Items.DATA_MODEL, new ModelSubtypes());
		reg.registerSubtypeInterpreter(Hostile.Items.PREDICTION, new ModelSubtypes());
	}

	private class ModelSubtypes implements IIngredientSubtypeInterpreter<ItemStack> {

		@Override
		public String apply(ItemStack stack, UidContext context) {
			DataModel dm = DataModelItem.getStoredModel(stack);
			if (dm == null) return "NULL";
			return dm.getId().toString();
		}

	}

	@Override
	public ResourceLocation getPluginUid() {
		return new ResourceLocation(HostileNetworks.MODID, "plugin");
	}

}
