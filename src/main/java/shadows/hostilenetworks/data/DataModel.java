package shadows.hostilenetworks.data;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.item.MobPredictionItem;

public class DataModel {

	protected ResourceLocation id;
	protected final EntityType<?> type;
	protected final TranslationTextComponent name;
	protected final float guiScale;
	protected final float guiXOff, guiYOff, guiZOff;
	protected final int simCost;
	protected final ItemStack input;
	protected final ItemStack baseDrop;
	protected final String triviaKey;
	protected final List<ItemStack> fabDrops;

	public DataModel(EntityType<?> type, TranslationTextComponent name, float guiScale, float guiXOff, float guiYOff, float guiZOff, int simCost, ItemStack input, ItemStack baseDrop, String triviaKey, List<ItemStack> fabDrops) {
		this.type = type;
		this.name = name;
		this.guiScale = guiScale;
		this.guiYOff = guiYOff;
		this.guiXOff = guiXOff;
		this.guiZOff = guiZOff;
		this.simCost = simCost;
		this.input = input;
		this.baseDrop = baseDrop;
		this.triviaKey = triviaKey;
		this.fabDrops = fabDrops;
	}

	public void setId(ResourceLocation id) {
		if (this.id != null) throw new UnsupportedOperationException("Attempted to change the already set ID of a DataModel!");
		this.id = id;
	}

	public ResourceLocation getId() {
		return id;
	}

	public TranslationTextComponent getName() {
		return this.name;
	}

	public String getTriviaKey() {
		return triviaKey;
	}

	public float getScale() {
		return guiScale;
	}

	public float getYOffset() {
		return this.guiYOff;
	}

	public float getXOffset() {
		return this.guiXOff;
	}

	public float getZOffset() {
		return this.guiZOff;
	}

	public int getSimCost() {
		return this.simCost;
	}

	public EntityType<?> getType() {
		return this.type;
	}

	public ItemStack getInput() {
		return this.input;
	}

	public ItemStack getBaseDrop() {
		return this.baseDrop;
	}

	public List<ItemStack> getFabDrops() {
		return this.fabDrops;
	}

	public ItemStack getPredictionDrop(ModelTier tier) {
		ItemStack stk = new ItemStack(Hostile.Items.PREDICTION);
		MobPredictionItem.setStoredModel(stk, this);
		return stk;
	}

	public int getNameColor() {
		return this.name.getStyle().getColor().getValue();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DataModel && ((DataModel) obj).id.equals(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return String.format("DataModel[%s]", this.id);
	}

	public static class Adapter implements JsonDeserializer<DataModel>, JsonSerializer<DataModel> {

		public static final DataModel.Adapter INSTANCE = new Adapter();

		@Override
		public JsonElement serialize(DataModel src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject obj = new JsonObject();
			obj.addProperty("type", src.type.getRegistryName().toString());
			obj.addProperty("name", src.name.getKey());
			obj.addProperty("gui_scale", src.guiScale);
			obj.addProperty("gui_x_offset", src.guiXOff);
			obj.addProperty("gui_y_offset", src.guiYOff);
			obj.addProperty("gui_z_offset", src.guiZOff);
			obj.addProperty("sim_cost", src.simCost);
			obj.add("base_drop", context.serialize(src.baseDrop));
			obj.addProperty("trivia", src.triviaKey);
			obj.add("fabricator_drops", context.serialize(src.fabDrops));
			return obj;
		}

		@Override
		public DataModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject obj = json.getAsJsonObject();
			EntityType<?> t = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(obj.get("type").getAsString()));
			if (t == null) throw new JsonParseException("DataModel has invalid entity type " + obj.get("type").getAsString());
			TranslationTextComponent name = new TranslationTextComponent(obj.get("name").getAsString());
			if (obj.has("name_color")) name.withStyle(Style.EMPTY.withColor(Color.fromRgb(Integer.decode(obj.get("name_color").getAsString()))));
			else name.withStyle(Style.EMPTY.withColor(TextFormatting.WHITE));
			float guiScale = obj.get("gui_scale").getAsFloat();
			float guiXOff = obj.get("gui_x_offset").getAsFloat();
			float guiYOff = obj.get("gui_y_offset").getAsFloat();
			float guiZOff = obj.get("gui_z_offset").getAsFloat();
			int simCost = obj.get("sim_cost").getAsInt();
			ItemStack input = context.deserialize(obj.get("input"), ItemStack.class);
			ItemStack baseDrop = context.deserialize(obj.get("base_drop"), ItemStack.class);
			String triviaKey = obj.has("trivia") ? obj.get("trivia").getAsString() : "hostilenetworks.trivia.nothing";
			List<ItemStack> fabDrops = context.deserialize(obj.get("fabricator_drops"), new TypeToken<List<ItemStack>>() {
			}.getType());
			return new DataModel(t, name, guiScale, guiXOff, guiYOff, guiZOff, simCost, input, baseDrop, triviaKey, fabDrops);
		}

	}

}
