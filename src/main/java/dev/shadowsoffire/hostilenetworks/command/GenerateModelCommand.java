package dev.shadowsoffire.hostilenetworks.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.serialization.JsonOps;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModel.DataPerKill;
import dev.shadowsoffire.hostilenetworks.data.DataModel.DisplayData;
import dev.shadowsoffire.hostilenetworks.data.DataModel.TierData;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

public class GenerateModelCommand {

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_ENTITY_TYPE = (ctx, builder) -> SharedSuggestionProvider.suggest(BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(ResourceLocation::toString), builder);
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_DATA_MODEL = (ctx, builder) -> SharedSuggestionProvider.suggest(DataModelRegistry.INSTANCE.getKeys().stream().map(ResourceLocation::toString), builder);

    public static final Method dropFromLootTable = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "dropFromLootTable", DamageSource.class, boolean.class);
    public static final MethodHandle DROP_LOOT = lootMethodHandle();

    private record CountedStack(ItemStack stack, AtomicInteger count) {}

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("generate_model_json").requires(c -> c.hasPermission(2))
            .then(Commands.argument("entity_type", ResourceLocationArgument.id()).suggests(SUGGEST_ENTITY_TYPE).then(Commands.argument("max_stack_size", IntegerArgumentType.integer(1, 64)).executes(c -> {
                Player p = c.getSource().getPlayerOrException();
                ResourceLocation name = c.getArgument("entity_type", ResourceLocation.class);
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(name);
                var results = runSimulation(type, p, 7500, c.getArgument("max_stack_size", Integer.class));

                // Formatter::off
                DataModel model = new DataModel((EntityType) type, Collections.emptyList(),
                    Component.translatable(type.getDescriptionId()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(p.getRandom().nextInt(0xFFFFFF)))),
                    DisplayData.DEFAULT, 256, Ingredient.of(Hostile.Items.PREDICTION_MATRIX.value()),
                    new ItemStack(Items.STICK),
                    "hostilenetworks.trivia." + name.getPath(),
                    results,
                    Optional.empty(), Optional.empty());
                // Formatter::on

                write(name.getNamespace(), name.getPath(), model);

                c.getSource().sendSuccess(() -> Component.literal("Data Model JSON generated at " + "datagen/data_models/" + name.getNamespace() + "/" + name.getPath() + ".json"), true);
                return 0;
            }))));

        root.then(Commands.literal("update_model_json").requires(c -> c.hasPermission(2))
            .then(Commands.argument("data_model", ResourceLocationArgument.id()).suggests(SUGGEST_DATA_MODEL).then(Commands.argument("max_stack_size", IntegerArgumentType.integer(1, 64)).executes(c -> {
                Player p = c.getSource().getPlayerOrException();
                ResourceLocation name = c.getArgument("data_model", ResourceLocation.class);
                DataModel model = DataModelRegistry.INSTANCE.getValue(name);
                EntityType<?> type = model.entity();
                var results = runSimulation(type, p, 7500, c.getArgument("max_stack_size", Integer.class));

                DataModel newModel = new DataModel(model, results);

                write(name.getNamespace(), name.getPath(), newModel);

                c.getSource().sendSuccess(() -> Component.literal("Data Model JSON generated at " + "datagen/data_models/" + name.getNamespace() + "/" + name.getPath() + ".json"), true);
                return 0;
            }))));

        root.then(Commands.literal("generate_all").requires(c -> c.hasPermission(2)).then(Commands.argument("max_stack_size", IntegerArgumentType.integer(1, 64)).executes(c -> {
            Player p = c.getSource().getPlayerOrException();
            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                if (!(type.create(p.level()) instanceof Mob)) continue;
                ResourceLocation name = EntityType.getKey(type);

                var results = runSimulation(type, p, 7500, c.getArgument("max_stack_size", Integer.class));

                if (!results.isEmpty()) {
                    // Formatter::off
                    DataModel model = new DataModel((EntityType) type, Collections.emptyList(),
                        Component.translatable(type.getDescriptionId()).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(p.getRandom().nextInt(0xFFFFFF)))),
                        DisplayData.DEFAULT, 256, Ingredient.of(Hostile.Items.PREDICTION_MATRIX.value()),
                        new ItemStack(Items.STICK),
                        "hostilenetworks.trivia." + name.getPath(),
                        results,
                        Optional.empty(), Optional.empty());
                    // Formatter::on

                    write(name.getNamespace(), name.getPath(), model);
                }
            }
            c.getSource().sendSuccess(() -> Component.literal("Data Model JSON files generated at datagen/data_models/"), true);
            return 0;
        })));

        root.then(Commands.literal("datafix_all").requires(c -> c.hasPermission(2)).executes(c -> {
            var resman = c.getSource().getServer().getResourceManager();
            var profiler = c.getSource().getServer().getProfiler();
            Map<ResourceLocation, JsonElement> map = DataModelRegistry.INSTANCE.prepare(resman, profiler);
            for (var entry : map.entrySet()) {
                try {
                    JsonObject out = new JsonObject();
                    JsonObject obj = entry.getValue().getAsJsonObject();
                    // Datafix legacy forge "conditions" to "neoforge:conditions", renaming the type names from forge: to neoforge: along the way.
                    if (obj.has("conditions")) {
                        JsonArray conditions = obj.remove("conditions").getAsJsonArray();
                        JsonArray condOut = new JsonArray();
                        conditions.asList().stream().map(JsonElement::getAsJsonObject).map(condObj -> {
                            JsonObject singleCondition = new JsonObject();
                            singleCondition.addProperty("type", condObj.remove("type").getAsString().replace("forge", "neoforge"));
                            for (String s : condObj.keySet()) {
                                singleCondition.add(s, condObj.get(s));
                            }
                            return singleCondition;
                        }).forEach(condOut::add);
                        out.add("neoforge:conditions", condOut);
                    }

                    // Move the "entity" key
                    out.add("entity", obj.remove("entity"));

                    // Move or create the "variants" key
                    if (obj.has("variants")) {
                        out.add("variants", obj.remove("variants"));
                    }
                    else {
                        out.add("variants", new JsonArray());
                    }

                    // Convert legacy "name" and "name_color" into the new Component "name" field
                    String nameKey = obj.remove("name").getAsString();
                    MutableComponent name = Component.translatable(nameKey);
                    if (obj.has("name_color")) {
                        var colorJson = obj.remove("name_color").getAsJsonPrimitive();
                        TextColor color;
                        if (colorJson.isNumber()) {
                            color = TextColor.fromRgb(colorJson.getAsInt());
                        }
                        else {
                            String str = colorJson.getAsString();
                            if (str.startsWith("0x")) {
                                color = TextColor.fromRgb(Integer.decode(str));
                            }
                            else {
                                color = TextColor.parseColor(str).getOrThrow();
                            }
                        }
                        name = name.withStyle(s -> s.withColor(color));
                    }
                    out.add("name", ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, name).getOrThrow());

                    // Merge legacy values into new "display" field.
                    float scale = removeOrDefault(obj, "gui_scale", 1);
                    float xOff = removeOrDefault(obj, "gui_x_offset", 0);
                    float yOff = removeOrDefault(obj, "gui_y_offset", 0);
                    float zOff = removeOrDefault(obj, "gui_z_offset", 0);
                    JsonObject nbt = obj.has("display_nbt") ? obj.remove("display_nbt").getAsJsonObject() : new JsonObject();

                    DisplayData display = new DisplayData(CompoundTag.CODEC.decode(JsonOps.INSTANCE, nbt).getOrThrow().getFirst(), scale, xOff, yOff, zOff);
                    out.add("display", DisplayData.CODEC.encodeStart(JsonOps.INSTANCE, display).getOrThrow());

                    out.add("sim_cost", obj.remove("sim_cost"));

                    // Convert old ItemStack input into an Ingredient. NBT is no longer legal, so we'll just try to read the item.
                    // Also, since we're supporting a datafix while things aren't loaded, we can't try to load the item.
                    JsonObject input = obj.remove("input").getAsJsonObject();
                    JsonObject newInput = new JsonObject();
                    newInput.add("item", input.get("item")); // Old placebo itemstack codec used "item" as the key
                    out.add("input", newInput);

                    // Convert base drop key "item" to "id" to match ItemStack codec
                    JsonObject base = obj.remove("base_drop").getAsJsonObject();
                    JsonObject newBase = new JsonObject();
                    newBase.add("id", base.remove("item"));
                    for (String s : base.keySet()) {
                        newBase.add(s, base.get(s));
                    }
                    out.add("base_drop", newBase);

                    out.add("trivia", obj.remove("trivia"));

                    // Convert fabricator drops, key-fixing item to id for each element.
                    JsonArray fabDrops = obj.remove("fabricator_drops").getAsJsonArray();
                    JsonArray newFabDrops = new JsonArray();

                    fabDrops.asList().stream().map(JsonElement::getAsJsonObject).map(drop -> {
                        JsonObject newDrop = new JsonObject();
                        newDrop.add("id", drop.remove("item"));
                        for (String s : drop.keySet()) {
                            newDrop.add(s, drop.get(s));
                        }
                        return newDrop;
                    }).forEach(newFabDrops::add);
                    out.add("fabricator_drops", newFabDrops);

                    // Fix tier data and data per kill legacy arrays to the real objects.

                    TierData newTierData = TierData.DEFAULT;
                    if (obj.has("tier_data")) {
                        JsonArray arr = obj.remove("tier_data").getAsJsonArray();
                        TierData data = new TierData(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt(), arr.get(3).getAsInt());
                        newTierData = data;
                    }

                    if (!newTierData.equals(TierData.DEFAULT)) {
                        out.add("tier_data", TierData.CODEC.encodeStart(JsonOps.INSTANCE, newTierData).getOrThrow());
                    }

                    DataPerKill newDpk = DataPerKill.DEFAULT;
                    if (obj.has("data_per_kill")) {
                        JsonArray arr = obj.remove("data_per_kill").getAsJsonArray();
                        DataPerKill data = new DataPerKill(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt(), arr.get(3).getAsInt());
                        newDpk = data;
                    }

                    if (!newDpk.equals(DataPerKill.DEFAULT)) {
                        out.add("data_per_kill", DataPerKill.CODEC.encodeStart(JsonOps.INSTANCE, newDpk).getOrThrow());
                    }

                    // Convert everything else
                    for (String s : obj.keySet()) {
                        out.add(s, obj.get(s));
                    }

                    write(entry.getKey().getNamespace(), entry.getKey().getPath(), out);
                }
                catch (Exception ex) {
                    HostileNetworks.LOGGER.error("Failed to datafix {}", entry.getKey());
                    ex.printStackTrace();
                }
            }
            c.getSource().sendSuccess(() -> Component.literal("Data Model JSON files generated at datagen/data_models/"), true);
            return 0;
        }));

    }

    @SuppressWarnings("unchecked")
    private static float removeOrDefault(JsonObject obj, String key, float value) {
        if (obj.has(key)) {
            return obj.remove(key).getAsFloat();
        }
        return value;
    }

    private static List<ItemStack> runSimulation(EntityType<?> type, Player p, int runs, float maxStackSize) {
        List<ItemEntity> allDrops = new ArrayList<>();
        try {
            Entity entity = type.create(p.level());
            DamageSource src = new DamageSource(p.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC_KILL), p);
            for (int i = 0; i < 2500; i++) {
                entity.moveTo(p.getX(), p.getY(), p.getZ(), 0, 0);
                entity.hurt(src, 1);
                entity.captureDrops(allDrops);
                DROP_LOOT.invoke(entity, src, true);
            }
            entity.remove(RemovalReason.DISCARDED);
        }
        catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        List<ItemStack> items = allDrops.stream().map(ItemEntity::getItem).toList();
        List<CountedStack> count = new ArrayList<>();
        for (ItemStack s : items) {
            boolean found = false;
            for (CountedStack cs : count) {
                if (ItemStack.isSameItemSameComponents(cs.stack, s)) {
                    cs.count.incrementAndGet();
                    found = true;
                    break;
                }
            }
            if (!found) {
                count.add(new CountedStack(s, new AtomicInteger(1)));
            }
        }

        var avgs = count.stream().map(cs -> Pair.of(cs.stack, cs.count.get() / 2500F)).toList();
        float factor = avgs.stream().map(Pair::getRight).max(Float::compareTo).orElse(0F) / maxStackSize;
        return avgs.stream().map(pair -> {
            ItemStack s = pair.getLeft();
            s.setCount(Mth.ceil(pair.getRight() / factor));
            return s;
        }).sorted((s1, s2) -> Integer.compare(s1.getCount(), s2.getCount()) == 0 ? -BuiltInRegistries.ITEM.getKey(s1.getItem()).compareTo(BuiltInRegistries.ITEM.getKey(s2.getItem())) : -Integer.compare(s1.getCount(), s2.getCount()))
            .toList();
    }

    private static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static void write(String namespace, String path, DataModel model) {
        JsonElement json = DataModel.CODEC.encodeStart(JsonOps.INSTANCE, model).getOrThrow(JsonSyntaxException::new);
        if (!"minecraft".equals(namespace)) {
            var condition = new ModLoadedCondition(namespace);
            var arr = new JsonArray();
            arr.add(ICondition.CODEC.encodeStart(JsonOps.INSTANCE, condition).getOrThrow());
            var copy = new JsonObject();
            copy.add("conditions", arr);
            json.getAsJsonObject().entrySet().forEach(entry -> copy.add(entry.getKey(), entry.getValue()));
            json = copy;
        }
        write(namespace, path, json);
    }

    private static void write(String namespace, String path, JsonElement json) {
        File file = new File(FMLPaths.GAMEDIR.get().toFile(), "datagen/data/" + namespace + "/data_models/" + path + ".json");
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            JsonWriter jWriter = new JsonWriter(writer);
            jWriter.setIndent("    ");
            GSON.toJson(json, jWriter);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static MethodHandle lootMethodHandle() {
        dropFromLootTable.setAccessible(true);
        try {
            return MethodHandles.lookup().unreflect(dropFromLootTable);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
