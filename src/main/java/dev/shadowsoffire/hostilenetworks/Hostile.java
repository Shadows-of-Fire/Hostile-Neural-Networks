package dev.shadowsoffire.hostilenetworks;

import com.google.common.collect.ImmutableSet;

import dev.shadowsoffire.hostilenetworks.block.LootFabBlock;
import dev.shadowsoffire.hostilenetworks.block.SimChamberBlock;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerContainer;
import dev.shadowsoffire.hostilenetworks.gui.LootFabContainer;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberContainer;
import dev.shadowsoffire.hostilenetworks.item.BlankDataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntityType;
import dev.shadowsoffire.placebo.container.ContainerUtil;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public class Hostile {

    private static final DeferredHelper R = DeferredHelper.create(HostileNetworks.MODID);

    public static class Blocks {
        public static final RegistryObject<Block> SIM_CHAMBER = R.block("sim_chamber", () -> new SimChamberBlock(Block.Properties.of().lightLevel(s -> 1).strength(4, 3000).noOcclusion()));
        public static final RegistryObject<Block> LOOT_FABRICATOR = R.block("loot_fabricator", () -> new LootFabBlock(Block.Properties.of().lightLevel(s -> 1).strength(4, 3000).noOcclusion()));

        private static void bootstrap() {}
    }

    public static class Items {
        public static final RegistryObject<Item> BLANK_DATA_MODEL = R.item("blank_data_model", () -> new BlankDataModelItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<Item> PREDICTION_MATRIX = R.item("prediction_matrix", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> OVERWORLD_PREDICTION = R.item("overworld_prediction", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> NETHER_PREDICTION = R.item("nether_prediction", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> END_PREDICTION = R.item("end_prediction", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> TWILIGHT_PREDICTION = R.item("twilight_prediction", () -> new Item(new Item.Properties()));
        public static final RegistryObject<Item> DEEP_LEARNER = R.item("deep_learner", () -> new DeepLearnerItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<DataModelItem> DATA_MODEL = R.item("data_model", () -> new DataModelItem(new Item.Properties().stacksTo(1)));
        public static final RegistryObject<MobPredictionItem> PREDICTION = R.item("prediction", () -> new MobPredictionItem(new Item.Properties()));
        public static final RegistryObject<BlockItem> SIM_CHAMBER = R.item("sim_chamber", () -> new BlockItem(Hostile.Blocks.SIM_CHAMBER.get(), new Item.Properties()));
        public static final RegistryObject<BlockItem> LOOT_FABRICATOR = R.item("loot_fabricator", () -> new BlockItem(Hostile.Blocks.LOOT_FABRICATOR.get(), new Item.Properties()));

        private static void bootstrap() {}
    }

    public static class TileEntities {
        public static final RegistryObject<BlockEntityType<SimChamberTileEntity>> SIM_CHAMBER = R.blockEntity("sim_chamber",
            () -> new TickingBlockEntityType<>(SimChamberTileEntity::new, ImmutableSet.of(Hostile.Blocks.SIM_CHAMBER.get()), false, true));

        public static final RegistryObject<BlockEntityType<LootFabTileEntity>> LOOT_FABRICATOR = R.blockEntity("loot_fabricator",
            () -> new TickingBlockEntityType<>(LootFabTileEntity::new, ImmutableSet.of(Hostile.Blocks.LOOT_FABRICATOR.get()), false, true));

        private static void bootstrap() {}
    }

    public static class Containers {
        public static final RegistryObject<MenuType<DeepLearnerContainer>> DEEP_LEARNER = R.menu("deep_learner", () -> ContainerUtil.type(DeepLearnerContainer::new));
        public static final RegistryObject<MenuType<SimChamberContainer>> SIM_CHAMBER = R.menu("sim_chamber", () -> ContainerUtil.posType(SimChamberContainer::new));
        public static final RegistryObject<MenuType<LootFabContainer>> LOOT_FABRICATOR = R.menu("loot_fabricator", () -> ContainerUtil.posType(LootFabContainer::new));

        private static void bootstrap() {}
    }

    public static class Tabs {
        public static final ResourceKey<CreativeModeTab> HNN_TAB_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(HostileNetworks.MODID, "tab"));

        public static final RegistryObject<CreativeModeTab> HNN_TAB = R.tab("tab",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.hostilenetworks")).icon(() -> Items.SIM_CHAMBER.get().getDefaultInstance()).build());

        private static void bootstrap() {}
    }

    static void bootstrap() {
        Blocks.bootstrap();
        Items.bootstrap();
        TileEntities.bootstrap();
        Containers.bootstrap();
        Tabs.bootstrap();
    }

}
