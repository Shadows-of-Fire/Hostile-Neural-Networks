package shadows.hostilenetworks.jei;

import net.minecraft.item.ItemStack;
import shadows.hostilenetworks.data.DataModel;

public class LootFabRecipe {

	final ItemStack input, output;

	public LootFabRecipe(DataModel model, int idx) {
		this.input = model.getPredictionDrop().copy();
		this.output = model.getFabDrops().get(idx).copy();
	}

}
