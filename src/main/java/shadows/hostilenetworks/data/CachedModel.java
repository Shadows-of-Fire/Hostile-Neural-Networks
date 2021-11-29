package shadows.hostilenetworks.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import shadows.hostilenetworks.item.DataModelItem;

/**
 * A Cached model is a temporarily-deserialized model, used on the client for rendering purposes.
 */
public class CachedModel {

	protected final DataModel model;
	protected final int data;
	protected final ModelTier tier;
	protected final int slot;
	private Entity cachedEntity;

	public CachedModel(DataModel model, int data, int slot) {
		this.model = model;
		this.data = data;
		this.tier = ModelTier.getByData(data);
		this.slot = slot;
	}

	public CachedModel(ItemStack stack, int slot) {
		this.model = DataModelItem.getStoredModel(stack);
		this.data = DataModelItem.getData(stack);
		this.tier = ModelTier.getByData(data);
		this.slot = slot;
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

}
