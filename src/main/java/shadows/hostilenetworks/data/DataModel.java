package shadows.hostilenetworks.data;

import java.util.ArrayList;
import java.util.List;

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

	public DataModel(EntityType<?> type, TranslatableComponent name, float guiScale, float guiXOff, float guiYOff, float guiZOff, int simCost, ItemStack input, ItemStack baseDrop, String triviaKey, List<ItemStack> fabDrops) {
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
		DataModel model = new DataModel(type, name, guiScale, guiXOff, guiYOff, guiZOff, simCost, input, baseDrop, triviaKey, fabDrops);
		return model;
	}

}
