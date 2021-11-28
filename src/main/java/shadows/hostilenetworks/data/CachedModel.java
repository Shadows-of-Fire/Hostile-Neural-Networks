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
	protected final int kills;
	protected final ModelTier tier;
	private Entity cachedEntity;

	public CachedModel(DataModel model, int kills) {
		this.model = model;
		this.kills = kills;
		this.tier = ModelTier.getByKills(kills);
	}

	public CachedModel(ItemStack stack) {
		this.model = DataModelItem.getStoredModel(stack);
		this.kills = DataModelItem.getKills(stack);
		this.tier = ModelTier.getByKills(kills);
	}

	public DataModel getModel() {
		return model;
	}

	public int getKills() {
		return this.kills;
	}

	public ModelTier getTier() {
		return this.tier;
	}

	public float getAccuracy() {
		ModelTier next = this.tier.next();
		int diff = next.data - tier.data;
		float tDiff = next.accuracy - tier.accuracy;
		return tier.accuracy + tDiff * (diff - (next.data - kills)) / diff;
	}

	public int getKillsNeeded() {
		ModelTier next = this.tier.next();
		return MathHelper.ceil((next.data - kills) / (float) tier.dataPerKill);
	}

	public LivingEntity getEntity(World world) {
		if (this.cachedEntity == null) {
			this.cachedEntity = this.model.type.create(world);
		}
		return this.cachedEntity instanceof LivingEntity ? (LivingEntity) this.cachedEntity : null;
	}

}
