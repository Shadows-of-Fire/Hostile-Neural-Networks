package shadows.hostilenetworks.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.hostilenetworks.HostileNetworks;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.json.PlaceboJsonReloadListener;
import shadows.placebo.json.SerializerBuilder;

public class DataModelManager extends PlaceboJsonReloadListener<DataModel> {

	public static final DataModelManager INSTANCE = new DataModelManager();

	private Map<EntityType<?>, DataModel> modelsByType = new HashMap<>();

	public DataModelManager() {
		super(HostileNetworks.LOGGER, "data_models", true, false);
	}

	@Override
	protected void registerBuiltinSerializers() {
		this.registerSerializer(DEFAULT, new SerializerBuilder<DataModel>("Data Model").withJsonSerializer(src -> {
			JsonObject obj = new JsonObject();
			obj.addProperty("type", src.type.getRegistryName().toString());
			obj.addProperty("name", src.name.getKey());
			obj.addProperty("name_color", src.getNameColor());
			obj.addProperty("gui_scale", src.guiScale);
			obj.addProperty("gui_x_offset", src.guiXOff);
			obj.addProperty("gui_y_offset", src.guiYOff);
			obj.addProperty("gui_z_offset", src.guiZOff);
			obj.addProperty("sim_cost", src.simCost);
			obj.add("input", ItemAdapter.ITEM_READER.toJsonTree(src.input));
			obj.add("base_drop", ItemAdapter.ITEM_READER.toJsonTree(src.baseDrop));
			obj.addProperty("trivia", src.triviaKey);
			obj.add("fabricator_drops", ItemAdapter.ITEM_READER.toJsonTree(src.fabDrops));
			return obj;
		}).withJsonDeserializer(obj -> {
			EntityType<?> t = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(obj.get("type").getAsString()));
			if (t == null) throw new JsonParseException("DataModel has invalid entity type " + obj.get("type").getAsString());
			TranslatableComponent name = new TranslatableComponent(obj.get("name").getAsString());
			if (obj.has("name_color")) name.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(Integer.decode(obj.get("name_color").getAsString()))));
			else name.withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
			float guiScale = obj.get("gui_scale").getAsFloat();
			float guiXOff = obj.get("gui_x_offset").getAsFloat();
			float guiYOff = obj.get("gui_y_offset").getAsFloat();
			float guiZOff = obj.get("gui_z_offset").getAsFloat();
			int simCost = obj.get("sim_cost").getAsInt();
			ItemStack input = ItemAdapter.ITEM_READER.fromJson(obj.get("input"), ItemStack.class);
			ItemStack baseDrop = ItemAdapter.ITEM_READER.fromJson(obj.get("base_drop"), ItemStack.class);
			String triviaKey = obj.has("trivia") ? obj.get("trivia").getAsString() : "hostilenetworks.trivia.nothing";
			List<ItemStack> fabDrops = ItemAdapter.ITEM_READER.fromJson(obj.get("fabricator_drops"), new TypeToken<List<ItemStack>>() {
			}.getType());
			fabDrops.removeIf(ItemStack::isEmpty);
			return new DataModel(t, name, guiScale, guiXOff, guiYOff, guiZOff, simCost, input, baseDrop, triviaKey, fabDrops);
		}).withNetworkSerializer(DataModel::write).withNetworkDeserializer(DataModel::read));
	}

	@Override
	protected <T extends DataModel> void register(ResourceLocation key, T model) {
		super.register(key, model);
		if (this.modelsByType.containsKey(model.type)) {
			String msg = "Attempted to register two models (%s and %s) for Entity Type %s!";
			throw new UnsupportedOperationException(String.format(msg, key, this.modelsByType.get(model.type).getId(), model.type.getRegistryName()));
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

	@Nullable
	public DataModel getForEntity(EntityType<?> type) {
		return this.modelsByType.get(type);
	}

}
