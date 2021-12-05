package shadows.hostilenetworks.jei;

import net.minecraft.item.ItemStack;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.data.DataModel;
import shadows.hostilenetworks.data.ModelTier;
import shadows.hostilenetworks.item.DataModelItem;

public class TickingDataModelWrapper {

	final ItemStack model;
	final ItemStack input;
	final ItemStack baseDrop;
	final ItemStack prediction;

	ModelTier currentTier = ModelTier.BASIC;

	public TickingDataModelWrapper(DataModel src) {
		this.model = new ItemStack(Hostile.Items.DATA_MODEL);
		DataModelItem.setStoredModel(this.model, src);
		DataModelItem.setData(this.model, ModelTier.BASIC.data);
		this.input = src.getInput().copy();
		this.baseDrop = src.getBaseDrop().copy();
		this.prediction = src.getPredictionDrop();
	}

	void setTier(ModelTier tier) {
		if (this.currentTier == tier) return;
		DataModelItem.setData(this.model, tier.data);
		this.currentTier = tier;
	}

}
