package dev.shadowsoffire.hostilenetworks.datagen;

import java.util.concurrent.CompletableFuture;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileNetworks;
import dev.shadowsoffire.placebo.datagen.LegacyRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

public class HNNRecipeProvider extends LegacyRecipeProvider {

    public HNNRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, HostileNetworks.MODID);
    }

    @Override
    protected void genRecipes(RecipeOutput out, HolderLookup.Provider regs) {
        // --- Machines / blocks ---
        this.addShaped(loc("data_center"), Hostile.Items.DATA_CENTER, 3, 3,
            Items.ECHO_SHARD, Items.NETHER_STAR, Items.ECHO_SHARD,
            Hostile.Items.SIM_CHAMBER, Tags.Items.OBSIDIANS, Hostile.Items.LOOT_FABRICATOR,
            Tags.Items.OBSIDIANS, Tags.Items.OBSIDIANS, Tags.Items.OBSIDIANS);

        this.addShaped(loc("sim_chamber"), Hostile.Items.SIM_CHAMBER, 3, 3,
            null, Tags.Items.GLASS_PANES, null,
            Items.ENDER_PEARL, Tags.Items.OBSIDIANS, Items.ENDER_PEARL,
            Tags.Items.GEMS_LAPIS, Items.COMPARATOR, Tags.Items.GEMS_LAPIS);

        this.addShaped(loc("loot_fabricator"), Hostile.Items.LOOT_FABRICATOR, 3, 3,
            null, Tags.Items.INGOTS_NETHERITE, null,
            Tags.Items.GEMS_DIAMOND, Tags.Items.OBSIDIANS, Tags.Items.GEMS_DIAMOND,
            Tags.Items.INGOTS_GOLD, Items.COMPARATOR, Tags.Items.INGOTS_GOLD);

        this.addShaped(loc("data_center_io_port"), Hostile.Items.DATA_CENTER_IO_PORT, 3, 3,
            Tags.Items.INGOTS_IRON, Items.BLACK_STAINED_GLASS, Tags.Items.INGOTS_IRON,
            Items.BLACK_STAINED_GLASS, Items.REDSTONE_BLOCK, Items.BLACK_STAINED_GLASS,
            Tags.Items.INGOTS_IRON, Items.BLACK_STAINED_GLASS, Tags.Items.INGOTS_IRON);

        // --- Tools / handhelds ---
        this.addShaped(loc("framework"), Hostile.Items.BLANK_DATA_MODEL, 3, 3,
            Items.CLAY_BALL, Items.REPEATER, Items.CLAY_BALL,
            Tags.Items.DUSTS_REDSTONE, Items.SMOOTH_STONE, Tags.Items.DUSTS_REDSTONE,
            Items.CLAY_BALL, Tags.Items.INGOTS_GOLD, Items.CLAY_BALL);

        this.addShaped(loc("deep_learner"), Hostile.Items.DEEP_LEARNER, 3, 3,
            Tags.Items.OBSIDIANS, Items.REPEATER, Tags.Items.OBSIDIANS,
            Items.REPEATER, Tags.Items.GLASS_PANES, Items.REPEATER,
            Tags.Items.OBSIDIANS, Tags.Items.DUSTS_REDSTONE, Tags.Items.OBSIDIANS);

        this.addShaped(loc("prediction_matrix"), new ItemStack(Hostile.Items.PREDICTION_MATRIX.value(), 16), 3, 3,
            Tags.Items.INGOTS_IRON, Tags.Items.GLASS_PANES, null,
            Tags.Items.GLASS_PANES, Items.CLAY_BALL, Tags.Items.GLASS_PANES,
            null, Tags.Items.GLASS_PANES, Tags.Items.INGOTS_GOLD);

        this.addShaped(loc("fab_directive"), Hostile.Items.FAB_DIRECTIVE, 3, 3,
            null, Hostile.Tags.GENERALIZED_PREDICTIONS, null,
            Items.PAPER, Hostile.Items.PREDICTION_MATRIX, Items.PAPER,
            null, Items.PAPER, null);

        // --- Generalized predictions ---
        this.addShaped(loc("living_matter/overworld_to_nether"), Hostile.Items.NETHER_PREDICTION, 3, 3,
            null, Hostile.Items.OVERWORLD_PREDICTION, null,
            Hostile.Items.OVERWORLD_PREDICTION, Items.NETHERRACK, Hostile.Items.OVERWORLD_PREDICTION,
            null, Hostile.Items.OVERWORLD_PREDICTION, null);

        this.addShaped(loc("living_matter/nether_to_ender"), Hostile.Items.END_PREDICTION, 3, 3,
            null, Hostile.Items.NETHER_PREDICTION, null,
            Hostile.Items.NETHER_PREDICTION, Items.END_STONE, Hostile.Items.NETHER_PREDICTION,
            null, Hostile.Items.NETHER_PREDICTION, null);

        // --- Living matter: overworldian ---
        this.addShapeless(loc("living_matter/overworldian/arrow"), new ItemStack(Items.ARROW, 12),
            Items.STICK, Items.FLINT, Hostile.Items.OVERWORLD_PREDICTION);
        this.addShapeless(loc("living_matter/overworldian/bone"), new ItemStack(Items.BONE, 22),
            Hostile.Items.OVERWORLD_PREDICTION, Items.BONE_MEAL);
        this.addShapeless(loc("living_matter/overworldian/carrot"), new ItemStack(Items.CARROT, 2),
            Hostile.Items.OVERWORLD_PREDICTION, Items.WHEAT_SEEDS);
        this.addShapeless(loc("living_matter/overworldian/cobweb"), new ItemStack(Items.COBWEB, 4),
            Items.STRING, Items.SLIME_BALL, Hostile.Items.OVERWORLD_PREDICTION, Hostile.Items.OVERWORLD_PREDICTION);
        this.addShapeless(loc("living_matter/overworldian/grass"), new ItemStack(Items.SHORT_GRASS, 4),
            ItemTags.LEAVES, Items.DIRT, Hostile.Items.OVERWORLD_PREDICTION);
        this.addShapeless(loc("living_matter/overworldian/gunpowder"), new ItemStack(Items.GUNPOWDER, 16),
            Hostile.Items.OVERWORLD_PREDICTION, Items.COAL);
        this.addShapeless(loc("living_matter/overworldian/iron_ingot"), new ItemStack(Items.IRON_INGOT, 8),
            Hostile.Items.OVERWORLD_PREDICTION, Hostile.Items.OVERWORLD_PREDICTION, Hostile.Items.OVERWORLD_PREDICTION, Hostile.Items.OVERWORLD_PREDICTION, Items.ROTTEN_FLESH);
        this.addShapeless(loc("living_matter/overworldian/potato"), new ItemStack(Items.POTATO, 2),
            Hostile.Items.OVERWORLD_PREDICTION, Items.STICK);
        this.addShapeless(loc("living_matter/overworldian/prismarine"), new ItemStack(Items.PRISMARINE_SHARD, 2),
            Hostile.Items.OVERWORLD_PREDICTION, Items.QUARTZ);
        this.addShapeless(loc("living_matter/overworldian/rotten_flesh"), new ItemStack(Items.ROTTEN_FLESH, 16),
            Hostile.Items.OVERWORLD_PREDICTION, Items.PORKCHOP);
        this.addShapeless(loc("living_matter/overworldian/spider_eye"), new ItemStack(Items.SPIDER_EYE, 2),
            Items.ROTTEN_FLESH, Items.APPLE, Items.RED_MUSHROOM, Hostile.Items.OVERWORLD_PREDICTION);

        // --- Living matter: hellish ---
        this.addShapeless(loc("living_matter/hellish/blaze_powder"), new ItemStack(Items.BLAZE_POWDER, 2),
            Hostile.Items.NETHER_PREDICTION, Items.GUNPOWDER);
        this.addShapeless(loc("living_matter/hellish/blaze_rod"), Items.BLAZE_ROD,
            Items.BONE, Hostile.Items.NETHER_PREDICTION, Hostile.Items.NETHER_PREDICTION);
        this.addShapeless(loc("living_matter/hellish/ghast_tear"), new ItemStack(Items.GHAST_TEAR, 3),
            Items.SPIDER_EYE, Items.SUGAR, Hostile.Items.NETHER_PREDICTION, Hostile.Items.NETHER_PREDICTION);
        this.addShapeless(loc("living_matter/hellish/gold_ingot"), new ItemStack(Items.GOLD_INGOT, 6),
            Items.GLOWSTONE_DUST, Items.IRON_INGOT, Hostile.Items.NETHER_PREDICTION);
        this.addShapeless(loc("living_matter/hellish/nether_wart"), new ItemStack(Items.NETHER_WART, 4),
            Hostile.Items.NETHER_PREDICTION, Items.RED_MUSHROOM);
        this.addShapeless(loc("living_matter/hellish/soul_sand"), new ItemStack(Items.SOUL_SAND, 4),
            Hostile.Items.NETHER_PREDICTION, Items.SAND);

        // --- Living matter: extraterrestrial ---
        this.addShapeless(loc("living_matter/extraterrestrial/chorus_fruit"), Items.CHORUS_FRUIT,
            Hostile.Items.END_PREDICTION, Items.APPLE);
        this.addShapeless(loc("living_matter/extraterrestrial/end_stone"), new ItemStack(Items.END_STONE, 8),
            Items.SANDSTONE, Items.SANDSTONE, Items.ENDER_PEARL, Hostile.Items.END_PREDICTION);
        this.addShapeless(loc("living_matter/extraterrestrial/ender_pearl"), Items.ENDER_PEARL,
            Hostile.Items.END_PREDICTION, Items.EMERALD, Items.SNOWBALL, Items.SLIME_BALL);
    }

    private static ResourceLocation loc(String path) {
        return HostileNetworks.loc(path);
    }
}
