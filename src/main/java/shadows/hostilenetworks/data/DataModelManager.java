package shadows.hostilenetworks.data;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import shadows.hostilenetworks.HostileNetworks;
import shadows.placebo.json.PlaceboJsonReloadListener;

public class DataModelManager extends PlaceboJsonReloadListener<DataModel> {

	public static final DataModelManager INSTANCE = new DataModelManager();

	private Map<EntityType<?>, DataModel> modelsByType = new HashMap<>();

	public DataModelManager() {
		super(HostileNetworks.LOGGER, "data_models", true, false);
	}

	@Override
	protected void registerBuiltinSerializers() {
		this.registerSerializer(DEFAULT, DataModel.SERIALIZER);
	}

	@Override
	protected <T extends DataModel> void register(ResourceLocation key, T model) {
		super.register(key, model);
		if (this.modelsByType.containsKey(model.type)) {
			String msg = "Attempted to register two models (%s and %s) for Entity Type %s!";
			throw new UnsupportedOperationException(String.format(msg, key, this.modelsByType.get(model.type).getId(), EntityType.getKey(model.type)));
		}
		this.modelsByType.put(model.type, model);
	}

	@Override
	protected void beginReload() {
		super.beginReload();
		this.modelsByType = new HashMap<>();
	}

	@Override
	protected void onReload() {
		super.onReload();
		this.modelsByType.clear();
		this.registry.values().forEach(model -> this.modelsByType.put(model.getType(), model));
		this.modelsByType = ImmutableMap.copyOf(this.modelsByType);
	}

	@Override
	protected <T extends DataModel> void validateItem(T item) {
		super.validateItem(item);
		item.validate();
	}

	@Nullable
	public DataModel getForEntity(EntityType<?> type) {
		return this.modelsByType.get(type);
	}

}
