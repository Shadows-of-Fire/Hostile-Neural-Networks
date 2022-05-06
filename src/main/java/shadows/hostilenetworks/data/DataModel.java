package shadows.hostilenetworks.data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.hostilenetworks.Hostile;
import shadows.hostilenetworks.item.MobPredictionItem;
import shadows.placebo.json.ItemAdapter;
import shadows.placebo.json.PlaceboJsonReloadListener.TypeKeyedBase;

public class DataModel extends TypeKeyedBase<DataModel> {

	protected final EntityType<?> type;
	protected final TranslatableComponent name;
	protected final float guiScale;
	protected final float guiXOff, guiYOff, guiZOff;
	protected final int simCost;
	protected final ItemStack input;
	protected final ItemStack baseDrop;
	protected final String triviaKey;
	protected final List<ItemStack> fabDrops;
	protected final int[] tierData, dataPerKill;

	public DataModel(EntityType<?> type, TranslatableComponent name, float guiScale, float guiXOff, float guiYOff, float guiZOff, int simCost, ItemStack input, ItemStack baseDrop, String triviaKey, List<ItemStack> fabDrops, int[] tierData, int[] dataPerKill) {
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
		this.tierData = tierData;
		this.dataPerKill = dataPerKill;
	}

	public TranslatableComponent getName() {
		return this.name;
	}

	public String getTriviaKey() {
		return this.triviaKey;
	}

	public float getScale() {
		return this.guiScale;
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

	public int getTierData(ModelTier tier) {
		return this.tierData[tier.ordinal()];
	}

	public int getDataPerKill(ModelTier tier) {
		return this.dataPerKill[tier.ordinal()];
	}

	public ItemStack getPredictionDrop() {
		ItemStack stk = new ItemStack(Hostile.Items.PREDICTION);
		MobPredictionItem.setStoredModel(stk, this);
		return stk;
	}

	public int getNameColor() {
		return this.name.getStyle().getColor().getValue();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DataModel && ((DataModel) obj).id.equals(this.id);
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public String toString() {
		return String.format("DataModel[%s]", this.id);
	}

	public DataModel validate() {
		Preconditions.checkNotNull(type, "Invalid entity type!");
		Preconditions.checkNotNull(name, "Invalid entity name!");
		Preconditions.checkArgument(guiScale > 0, "Invalid gui scale!");
		Preconditions.checkArgument(simCost > 0, "Invalid simulation cost!");
		Preconditions.checkArgument(input != null && !input.isEmpty(), "Invalid input item!");
		Preconditions.checkArgument(baseDrop != null && !baseDrop.isEmpty(), "Invalid base drop!");
		Preconditions.checkNotNull(triviaKey, "Invalid trivia key!");
		Preconditions.checkNotNull(fabDrops, "Missing fabricator drops!");
		fabDrops.forEach(t -> Preconditions.checkArgument(t != null && !t.isEmpty(), "Invalid fabricator drop!"));
		Preconditions.checkArgument(tierData != null && tierData.length == 5, "Invalid tier data!");
		Preconditions.checkArgument(dataPerKill != null && dataPerKill.length == 5, "Invalid data per kill!");
		for (int i = 0; i < 4; i++) {
			if (dataPerKill[i] <= 0) throw new IllegalArgumentException("Data per kill may not be zero or negative!");
			if (tierData[i] >= tierData[i + 1]) throw new IllegalArgumentException("Malformed tier data, all values must be ascending!");
		}
		return this;
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(this.type.getRegistryName().toString());
		buf.writeUtf(this.name.getKey());
		buf.writeUtf(this.name.getStyle().getColor().serialize());
		buf.writeFloat(this.guiScale);
		buf.writeFloat(this.guiXOff);
		buf.writeFloat(this.guiYOff);
		buf.writeFloat(this.guiZOff);
		buf.writeInt(this.simCost);
		buf.writeItem(this.input);
		buf.writeItem(this.baseDrop);
		buf.writeUtf(this.triviaKey);
		buf.writeVarInt(this.fabDrops.size());
		for (ItemStack i : this.fabDrops)
			buf.writeItem(i);
		for (int i = 0; i < 5; i++) {
			buf.writeShort(this.tierData[i]);
			buf.writeShort(this.dataPerKill[i]);
		}
	}

	public static DataModel read(FriendlyByteBuf buf) {
		EntityType<?> type = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(buf.readUtf()));
		TranslatableComponent name = new TranslatableComponent(buf.readUtf());
		name.withStyle(Style.EMPTY.withColor(TextColor.parseColor(buf.readUtf())));
		float guiScale = buf.readFloat();
		float guiXOff = buf.readFloat();
		float guiYOff = buf.readFloat();
		float guiZOff = buf.readFloat();
		int simCost = buf.readInt();
		ItemStack input = buf.readItem();
		ItemStack baseDrop = buf.readItem();
		String triviaKey = buf.readUtf();
		int dropSize = buf.readVarInt();
		List<ItemStack> fabDrops = new ArrayList<>(dropSize);
		for (int i = 0; i < dropSize; i++) {
			fabDrops.add(buf.readItem());
		}
		int[] tierData = new int[5], dataPerKill = new int[5];
		for (int i = 0; i < 5; i++) {
			tierData[i] = buf.readShort();
			dataPerKill[i] = buf.readShort();
		}

		DataModel model = new DataModel(type, name, guiScale, guiXOff, guiYOff, guiZOff, simCost, input, baseDrop, triviaKey, fabDrops, tierData, dataPerKill);
		return model;
	}

	public JsonObject write() {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", this.type.getRegistryName().toString());
		obj.addProperty("name", this.name.getKey());
		obj.addProperty("name_color", this.getNameColor());
		obj.addProperty("gui_scale", this.guiScale);
		obj.addProperty("gui_x_offset", this.guiXOff);
		obj.addProperty("gui_y_offset", this.guiYOff);
		obj.addProperty("gui_z_offset", this.guiZOff);
		obj.addProperty("sim_cost", this.simCost);
		obj.add("input", ItemAdapter.ITEM_READER.toJsonTree(this.input));
		obj.add("base_drop", ItemAdapter.ITEM_READER.toJsonTree(this.baseDrop));
		obj.addProperty("trivia", this.triviaKey);
		obj.add("fabricator_drops", ItemAdapter.ITEM_READER.toJsonTree(this.fabDrops));
		obj.add("tier_data", ItemAdapter.ITEM_READER.toJsonTree(this.tierData));
		obj.add("data_per_kill", ItemAdapter.ITEM_READER.toJsonTree(this.dataPerKill));
		return obj;
	}

	public static DataModel read(JsonObject obj) {
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

		int[] tierData = ModelTier.defaultData();
		if (obj.has("tier_data")) {
			JsonArray arr = obj.get("tier_data").getAsJsonArray();
			tierData = Stream.of(new JsonPrimitive(0), arr.get(0), arr.get(1), arr.get(2), arr.get(3)).mapToInt(JsonElement::getAsShort).toArray();
		}
		int[] dataPerKill = ModelTier.defaultDataPerKill();
		if (obj.has("data_per_kill")) {
			JsonArray arr = obj.get("data_per_kill").getAsJsonArray();
			dataPerKill = Stream.of(arr.get(0), arr.get(1), arr.get(2), arr.get(3), new JsonPrimitive(0)).mapToInt(JsonElement::getAsShort).toArray();
		}

		return new DataModel(t, name, guiScale, guiXOff, guiYOff, guiZOff, simCost, input, baseDrop, triviaKey, fabDrops, tierData, dataPerKill).validate();
	}

}
