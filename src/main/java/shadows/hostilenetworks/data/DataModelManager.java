package shadows.hostilenetworks.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.minecraft.client.resources.JsonReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import shadows.hostilenetworks.HostileNetworks;
import shadows.placebo.util.json.ItemAdapter;

public class DataModelManager extends JsonReloadListener {

	//Formatter::off
	public static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(ItemStack.class, ItemAdapter.INSTANCE)
			.registerTypeAdapter(DataModel.class, DataModel.Adapter.INSTANCE).create();
	//Formatter::on

	public static final DataModelManager INSTANCE = new DataModelManager();

	private final Map<ResourceLocation, DataModel> registry = new HashMap<>();
	private final Map<EntityType<?>, DataModel> modelsByType = new HashMap<>();

	public DataModelManager() {
		super(GSON, "data_models");
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> pObject, IResourceManager pResourceManager, IProfiler pProfiler) {
		this.registry.clear();
		this.modelsByType.clear();
		pObject.forEach((loc, ele) -> {
			try {
				if (ele.getAsJsonObject().entrySet().isEmpty()) return; //Ignore empty files so people can delete models.
				DataModel model = GSON.fromJson(ele, DataModel.class);
				model.setId(loc);
				register(model);
			} catch (JsonParseException ex) {
				HostileNetworks.LOGGER.error("Failed to load data model {}.", loc);
				ex.printStackTrace();
			}
		});
	}

	protected void register(DataModel model) {
		if (registry.containsKey(model.id)) throw new UnsupportedOperationException("Attempted to register duplicate data model " + model.id);
		registry.put(model.id, model);
		if (modelsByType.containsKey(model.type)) {
			String msg = "Attempted to register two models (%s and %s) for Entity Type %s!";
			throw new UnsupportedOperationException(String.format(msg, model.id, modelsByType.get(model.type).id, model.type.getRegistryName()));
		}
		modelsByType.put(model.type, model);
	}

	@Nullable
	public DataModel getModel(ResourceLocation id) {
		return registry.get(id);
	}

	@Nullable
	public DataModel getModel(EntityType<?> type) {
		return modelsByType.get(type);
	}

	public Collection<DataModel> getAllModels() {
		return registry.values();
	}
}
