package shadows.hostilenetworks.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import shadows.hostilenetworks.HostileNetworks;
import shadows.hostilenetworks.net.DataModelMessage;
import shadows.hostilenetworks.net.DataModelResetMessage;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.network.PacketDistro;

public class DataModelManager extends SimpleJsonResourceReloadListener {

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
	protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
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

	public static void dispatch(Player p) {
		if (p.level.getServer() != null && p.level.getServer().isDedicatedServer()) {
			PacketDistro.sendTo(HostileNetworks.CHANNEL, new DataModelResetMessage(), p);
			for (DataModel dm : INSTANCE.registry.values()) {
				PacketDistro.sendTo(HostileNetworks.CHANNEL, new DataModelMessage(dm), p);
			}
		}
	}

	public void clear() {
		this.registry.clear();
		this.modelsByType.clear();
	}
}
