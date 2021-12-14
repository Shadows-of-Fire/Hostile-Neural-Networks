package shadows.hostilenetworks.data;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import shadows.hostilenetworks.item.DataModelItem;

/**
 * A Cached model is a temporarily-deserialized model. It retains space for a cached entity for rendering.
 */
public class CachedModel {

	protected final ItemStack stack;
	protected final int slot;
	protected final DataModel model;

	protected int data;
	protected ModelTier tier;
	private Entity cachedEntity;

	public CachedModel(ItemStack stack, int slot) {
		this.stack = stack;
		this.slot = slot;
		this.model = DataModelItem.getStoredModel(stack);
		this.data = DataModelItem.getData(stack);
		this.tier = ModelTier.getByData(this.data);
	}

	public DataModel getModel() {
		return this.model;
	}

	public int getData() {
		return this.data;
	}

	public ModelTier getTier() {
		return this.tier;
	}

	public void setData(int data) {
		this.data = data;
		if (this.data > this.tier.next().data) this.tier = this.tier.next();
		DataModelItem.setData(this.stack, data);
	}

	public int getSlot() {
		return this.slot;
	}

	public float getAccuracy() {
		ModelTier next = this.tier.next();
		int diff = next.data - this.tier.data;
		float tDiff = next.accuracy - this.tier.accuracy;
		return this.tier.accuracy + tDiff * (diff - (next.data - this.data)) / diff;
	}

	public int getKillsNeeded() {
		ModelTier next = this.tier.next();
		return Mth.ceil((next.data - this.data) / (float) this.tier.dataPerKill);
	}

	public LivingEntity getEntity(Level world) {
		if (this.cachedEntity == null) {
			this.cachedEntity = this.model.type.create(world);
		}
		return this.cachedEntity instanceof LivingEntity ? (LivingEntity) this.cachedEntity : null;
	}

	public ItemStack getPredictionDrop() {
		return this.model.getPredictionDrop();
	}

	public ItemStack getSourceStack() {
		return this.stack;
	}

}
