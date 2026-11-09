package qa;

import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public final class DataCases {
  public static void run(MinecraftServer server) {
    var ids =
        server.getRecipeManager().getRecipes().stream()
            .map(r -> r.id().identifier().toString())
            .collect(java.util.stream.Collectors.toSet());
    for (String n :
        List.of(
            "liveroot",
            "cicada",
            "firefly",
            "moonworm",
            "steeleaf",
            "raven_feather",
            "torchberries")) {
      Checks.test(
          "hnn_recipe_" + n,
          () -> {
            var id =
                Identifier.parse("hostilenetworks:recipe/living_matter/twilight/" + n + ".json");
            try (var reader = server.getResourceManager().getResourceOrThrow(id).openAsReader()) {
              var recipe =
                  net.minecraft.world.item.crafting.Recipe.CODEC
                      .parse(
                          server
                              .registryAccess()
                              .createSerializationContext(
                                  com.mojang.serialization.JsonOps.INSTANCE),
                          com.google.gson.JsonParser.parseReader(reader))
                      .getOrThrow();
              Checks.require(recipe != null, "Recipe failed to decode");
            }
          });
    }
    Checks.test(
        "hnn_goblin_model",
        () ->
            Checks.require(
                DataModelRegistry.INSTANCE.getValue(
                        Identifier.parse("hostilenetworks:twilightforest/goblin"))
                    != null,
                "Goblin model unavailable"));
  }
}
