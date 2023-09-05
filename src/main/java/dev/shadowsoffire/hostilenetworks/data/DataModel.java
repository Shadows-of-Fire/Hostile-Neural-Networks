package dev.shadowsoffire.hostilenetworks.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.placebo.codec.CodecProvider;
import dev.shadowsoffire.placebo.json.ItemAdapter;
import dev.shadowsoffire.placebo.json.NBTAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Stores all of the information representing an individual Data Model.
 * 
 * @param type        The primary entity type of this model. Must be a valid entity.
 * @param subtypes    All other entity types that match to this model. Invalid entities may be passed to this list for optional compat.
 * @param name        The display name of the data model.
 * @param displayNbt  NBT data applied to the entity when it is being rendered.
 * @param guiScale    Scale applied to the entity when it is being rendered.
 * @param guiXOff     X offset applied to the entity when it is being rendered.
 * @param guiYOff     Y offset applied to the entity when it is being rendered.
 * @param guiZOff     Z offset applied to the entity when it is being rendered.
 * @param simCost     FE cost per-tick to run this model in the simulation chamber.
 * @param input       The input itemstack for simulations. Usually the prediction matrix.
 * @param baseDrop    The generic item that is always dropped when simulating this model.
 * @param triviaKey   Lang key for the trivia text shown in the deep learner.
 * @param fabDrops    List of items produced in the Loot Fabricator when processing Predictions.
 * @param tierData    The amount of data it takes to reach each tier.
 * @param dataPerKill The amount of data granted per kill at each tier.
 */
public record DataModel(EntityType<? extends LivingEntity> type, List<EntityType<? extends LivingEntity>> subtypes,
    MutableComponent name, CompoundTag displayNbt, float guiScale, float guiXOff, float guiYOff, float guiZOff,
    int simCost, ItemStack input, ItemStack baseDrop, String triviaKey, List<ItemStack> fabDrops, int[] tierData,
    int[] dataPerKill) implements CodecProvider<DataModel> {

    public static final Codec<DataModel> CODEC = new DataModelCodec();

    public DataModel(DataModel other, List<ItemStack> newResults) {
        this(other.type, other.subtypes, other.name, other.displayNbt, other.guiScale, other.guiYOff, other.guiXOff, other.guiZOff, other.simCost, other.input, other.baseDrop, other.triviaKey, newResults, other.tierData,
            other.dataPerKill);
    }

    public int getTierData(ModelTier tier) {
        return this.tierData[tier.ordinal()];
    }

    public int getDataPerKill(ModelTier tier) {
        return this.dataPerKill[tier.ordinal()];
    }

    public ItemStack getPredictionDrop() {
        ItemStack stk = new ItemStack(Hostile.Items.PREDICTION.get());
        DataModelItem.setStoredModel(stk, this);
        return stk;
    }

    public int getNameColor() {
        return this.name.getStyle().getColor().getValue();
    }

    public DataModel validate(ResourceLocation key) {
        Preconditions.checkNotNull(this.type, "Invalid entity type!");
        Preconditions.checkNotNull(this.name, "Invalid entity name!");
        Preconditions.checkNotNull(this.name.getStyle().getColor(), "Invalid entity name color!");
        Preconditions.checkArgument(this.guiScale > 0, "Invalid gui scale!");
        Preconditions.checkArgument(this.simCost > 0, "Invalid simulation cost!");
        Preconditions.checkArgument(this.input != null && !this.input.isEmpty(), "Invalid input item!");
        Preconditions.checkNotNull(this.baseDrop, "Invalid base drop!");
        Preconditions.checkNotNull(this.triviaKey, "Invalid trivia key!");
        Preconditions.checkNotNull(this.fabDrops, "Missing fabricator drops!");
        this.fabDrops.forEach(t -> Preconditions.checkArgument(t != null && !t.isEmpty(), "Invalid fabricator drop!"));
        Preconditions.checkArgument(this.tierData != null && this.tierData.length == 5, "Invalid tier data!");
        Preconditions.checkArgument(this.dataPerKill != null && this.dataPerKill.length == 5, "Invalid data per kill!");
        for (int i = 0; i < 4; i++) {
            if (this.dataPerKill[i] <= 0) throw new IllegalArgumentException("Data per kill may not be zero or negative!");
            if (this.tierData[i] >= this.tierData[i + 1]) throw new IllegalArgumentException("Malformed tier data, all values must be ascending!");
        }
        return this;
    }

    @Override
    public Codec<? extends DataModel> getCodec() {
        return CODEC;
    }

    /**
     * DataModel codec that handles everything in json form because I don't know how to convert this to a codec.
     */
    public static class DataModelCodec implements Codec<DataModel> {

        @Override
        public <T> DataResult<T> encode(DataModel input, DynamicOps<T> ops, T prefix) {
            JsonObject obj = new JsonObject();
            ResourceLocation key = EntityType.getKey(input.type);
            obj.addProperty("entity", key.toString());
            obj.add("variants", ItemAdapter.ITEM_READER.toJsonTree(input.subtypes.stream().map(EntityType::getKey).toList()));
            obj.addProperty("name", ((TranslatableContents) input.name.getContents()).getKey());
            obj.addProperty("name_color", input.name.getStyle().getColor().serialize());
            obj.add("display_nbt", NBTAdapter.EITHER_CODEC.encodeStart(JsonOps.INSTANCE, input.displayNbt).get().left().get());
            obj.addProperty("gui_scale", input.guiScale);
            obj.addProperty("gui_x_offset", input.guiXOff);
            obj.addProperty("gui_y_offset", input.guiYOff);
            obj.addProperty("gui_z_offset", input.guiZOff);
            obj.addProperty("sim_cost", input.simCost);
            obj.add("input", ItemAdapter.ITEM_READER.toJsonTree(input.input));
            obj.add("base_drop", ItemAdapter.ITEM_READER.toJsonTree(input.baseDrop));
            obj.addProperty("trivia", input.triviaKey);
            JsonArray fabDrops = ItemAdapter.ITEM_READER.toJsonTree(input.fabDrops).getAsJsonArray();
            for (JsonElement e : fabDrops) {
                JsonObject drop = e.getAsJsonObject();
                ResourceLocation itemName = new ResourceLocation(drop.get("item").getAsString());
                if (!"minecraft".equals(itemName.getNamespace()) && !key.getNamespace().equals(itemName.getNamespace())) {
                    drop.addProperty("optional", true);
                }
            }
            obj.add("fabricator_drops", fabDrops);
            obj.add("tier_data", ItemAdapter.ITEM_READER.toJsonTree(Arrays.copyOfRange(input.tierData, 1, 5)));
            obj.add("data_per_kill", ItemAdapter.ITEM_READER.toJsonTree(Arrays.copyOfRange(input.dataPerKill, 0, 4)));
            return DataResult.success(JsonOps.INSTANCE.convertTo(ops, obj));
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public <T> DataResult<Pair<DataModel, T>> decode(DynamicOps<T> ops, T input) {
            JsonObject obj = ops.convertTo(JsonOps.INSTANCE, input).getAsJsonObject();
            String eTypeStr = obj.get("entity").getAsString();
            EntityType<? extends LivingEntity> t = (EntityType) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(eTypeStr));
            if (t == EntityType.PIG && !"minecraft:pig".equals(eTypeStr)) throw new JsonParseException("DataModel has invalid entity type " + eTypeStr);
            List<EntityType<? extends LivingEntity>> subtypes = new ArrayList<>();
            if (obj.has("variants")) {
                for (JsonElement json : obj.get("variants").getAsJsonArray()) {
                    EntityType<? extends LivingEntity> st = (EntityType) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(json.getAsString()));
                    if (st != EntityType.PIG || "minecraft:pig".equals(json.getAsString())) subtypes.add(st);
                    // Intentionally ignore invalid entries here, so that modded entities can be added as subtypes without hard deps.
                }
            }
            MutableComponent name = Component.translatable(obj.get("name").getAsString());
            if (obj.has("name_color")) {
                String colorStr = obj.get("name_color").getAsString();
                var color = TextColor.parseColor(colorStr);
                name = name.withStyle(Style.EMPTY.withColor(color));
            }
            else name.withStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));
            float guiScale = obj.get("gui_scale").getAsFloat();
            float guiXOff = obj.get("gui_x_offset").getAsFloat();
            float guiYOff = obj.get("gui_y_offset").getAsFloat();
            float guiZOff = obj.get("gui_z_offset").getAsFloat();
            int simCost = obj.get("sim_cost").getAsInt();
            ItemStack inputItem = ItemAdapter.ITEM_READER.fromJson(obj.get("input"), ItemStack.class);
            ItemStack baseDrop = ItemAdapter.ITEM_READER.fromJson(obj.get("base_drop"), ItemStack.class);
            if (baseDrop.isEmpty()) {
                baseDrop = new ItemStack(Items.BARRIER);
                baseDrop.setHoverName(Component.translatable("hostilenetworks.info.no_base_drop"));
            }
            String triviaKey = obj.has("trivia") ? obj.get("trivia").getAsString() : "hostilenetworks.trivia.nothing";
            List<ItemStack> fabDrops = ItemAdapter.ITEM_READER.fromJson(obj.get("fabricator_drops"), new TypeToken<List<ItemStack>>(){}.getType());
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
            CompoundTag displayNbt = new CompoundTag();
            if (obj.has("display_nbt")) {
                displayNbt = NBTAdapter.EITHER_CODEC.decode(JsonOps.INSTANCE, obj.get("display_nbt")).result().get().getFirst();
            }

            return DataResult.success(Pair.of(new DataModel(t, subtypes, name, displayNbt, guiScale, guiXOff, guiYOff, guiZOff, simCost, inputItem, baseDrop, triviaKey, fabDrops, tierData, dataPerKill), input));
        }

    }

}
