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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.net.DataModelMessage;
import shadows.hostilenetworks.net.DataModelResetMessage;
import shadows.placebo.util.NetworkUtils;
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
		this.clear();
		pObject.forEach((loc, ele) -> {
			try {
				if (ele.getAsJsonObject().entrySet().isEmpty()) return; //Ignore empty files so people can delete models.
				DataModel model = GSON.fromJson(ele, DataModel.class);
				model.setId(loc);
				this.register(model);
			} catch (JsonParseException ex) {
				HostileNetworks.LOGGER.error("Failed to load data model {}.", loc);
				ex.printStackTrace();
			}
		});
	}

	public void register(DataModel model) {
		if (this.registry.containsKey(model.id)) throw new UnsupportedOperationException("Attempted to register duplicate data model " + model.id);
		this.registry.put(model.id, model);
		if (this.modelsByType.containsKey(model.type)) {
			String msg = "Attempted to register two models (%s and %s) for Entity Type %s!";
			throw new UnsupportedOperationException(String.format(msg, model.id, this.modelsByType.get(model.type).id, model.type.getRegistryName()));
		}
		this.modelsByType.put(model.type, model);
	}

	@Nullable
	public DataModel getModel(ResourceLocation id) {
		return this.registry.get(id);
	}

	@Nullable
	public DataModel getModel(EntityType<?> type) {
		return this.modelsByType.get(type);
	}

	public Collection<DataModel> getAllModels() {
		return this.registry.values();
	}

	public static void dispatch(PlayerEntity p) {
		if (p.level.getServer() != null && p.level.getServer().isDedicatedServer()) {
			NetworkUtils.sendTo(HostileNetworks.CHANNEL, new DataModelResetMessage(), p);
			for (DataModel dm : INSTANCE.registry.values()) {
				NetworkUtils.sendTo(HostileNetworks.CHANNEL, new DataModelMessage(dm), p);
			}
		}
	}

	public void clear() {
		this.registry.clear();
		this.modelsByType.clear();
	}
}
