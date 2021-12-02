package shadows.hostilenetworks.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
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
		this.tier = ModelTier.getByData(data);
	}

	public DataModel getModel() {
		return model;
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
		DataModelItem.setData(stack, data);
	}

	public int getSlot() {
		return this.slot;
	}

	public float getAccuracy() {
		ModelTier next = this.tier.next();
		int diff = next.data - tier.data;
		float tDiff = next.accuracy - tier.accuracy;
		return tier.accuracy + tDiff * (diff - (next.data - data)) / diff;
	}

	public int getKillsNeeded() {
		ModelTier next = this.tier.next();
		return MathHelper.ceil((next.data - data) / (float) tier.dataPerKill);
	}

	public LivingEntity getEntity(World world) {
		if (this.cachedEntity == null) {
			this.cachedEntity = this.model.type.create(world);
		}
		return this.cachedEntity instanceof LivingEntity ? (LivingEntity) this.cachedEntity : null;
	}

	public ItemStack getPredictionDrop() {
		return this.model.getPredictionDrop(this.tier);
	}

	public ItemStack getSourceStack() {
		return this.stack;
	}

}
