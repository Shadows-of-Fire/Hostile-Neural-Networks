package dev.shadowsoffire.hostilenetworks.jei;

import dev.shadowsoffire.hostilenetworks.data.DataModel;
import net.minecraft.world.item.ItemStack;

public class LootFabRecipe {

	final ItemStack input, output;

	public LootFabRecipe(DataModel model, int idx) {
		this.input = model.getPredictionDrop().copy();
		this.output = model.getFabDrops().get(idx).copy();
	}

}
