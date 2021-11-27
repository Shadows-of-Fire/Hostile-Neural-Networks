package shadows.hostilenetworks.data;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;

public class DataModel {

	protected ResourceLocation id;
	protected final EntityType<?> type;
	protected final TranslationTextComponent name;
	protected final int maxHealth;
	protected final float guiScale;
	protected final float guiXOff, guiYOff;
	protected final ItemStack baseDrop;
	protected final ItemStack pristineDrop;
	protected final List<TranslationTextComponent> trivia;

	public DataModel(EntityType<?> type, TranslationTextComponent name, int maxHealth, float guiScale, float guiXOff, float guiYOff, ItemStack baseDrop, ItemStack pristineDrop, List<TranslationTextComponent> trivia) {
		this.type = type;
		this.name = name;
		this.maxHealth = maxHealth;
		this.guiScale = guiScale;
		this.guiYOff = guiYOff;
		this.guiXOff = guiXOff;
		this.baseDrop = baseDrop;
		this.pristineDrop = pristineDrop;
		this.trivia = trivia;
	}

	public void setId(ResourceLocation id) {
		if (this.id != null) throw new UnsupportedOperationException("Attempted to change the already set ID of a DataModel!");
		this.id = id;
	}

	public static class Adapter implements JsonDeserializer<DataModel>, JsonSerializer<DataModel> {

		public static final DataModel.Adapter INSTANCE = new Adapter();

		@Override
		public JsonElement serialize(DataModel src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject obj = new JsonObject();
			obj.addProperty("type", src.type.getRegistryName().toString());
			obj.addProperty("name", src.name.getKey());
			obj.addProperty("max_health", src.maxHealth);
			obj.addProperty("gui_scale", src.guiScale);
			obj.addProperty("gui_x_offset", src.guiXOff);
			obj.addProperty("gui_y_offset", src.guiYOff);
			obj.add("base_drop", context.serialize(src.baseDrop));
			obj.add("pristine_drop", context.serialize(src.pristineDrop));
			JsonArray arr = new JsonArray();
			src.trivia.forEach(t -> arr.add(t.getKey()));
			obj.add("trivia", arr);
			return obj;
		}

		@Override
		public DataModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject obj = json.getAsJsonObject();
			EntityType<?> t = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(obj.get("type").getAsString()));
			if (t == null) throw new JsonParseException("DataModel has invalid entity type " + obj.get("type").getAsString());
			TranslationTextComponent name = new TranslationTextComponent(obj.get("name").getAsString());
			int maxHealth = obj.get("max_health").getAsInt();
			float guiScale = obj.get("gui_scale").getAsFloat();
			float guiXOff = obj.get("gui_x_offset").getAsFloat();
			float guiYOff = obj.get("gui_y_offset").getAsFloat();
			ItemStack baseDrop = context.deserialize(obj.get("base_drop"), ItemStack.class);
			ItemStack pristineDrop = context.deserialize(obj.get("pristine_drop"), ItemStack.class);
			List<TranslationTextComponent> trivia = new ArrayList<>();
			if (obj.has("trivia")) {
				JsonArray arr = obj.get("trivia").getAsJsonArray();
				arr.forEach(e -> {
					trivia.add(new TranslationTextComponent(e.getAsString()));
				});
			}
			return new DataModel(t, name, maxHealth, guiScale, guiXOff, guiYOff, baseDrop, pristineDrop, trivia);
		}

	}

}
