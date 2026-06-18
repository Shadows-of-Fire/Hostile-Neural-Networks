package dev.shadowsoffire.hostilenetworks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.hostilenetworks.block.DataCenterBlock;
import dev.shadowsoffire.hostilenetworks.block.LootFabBlock;
import dev.shadowsoffire.hostilenetworks.block.SimChamberBlock;
import dev.shadowsoffire.hostilenetworks.data.BlockDataModelsCondition;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.data.EntityDataModel;
import dev.shadowsoffire.hostilenetworks.gui.DataCenterMenu;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerMenu;
import dev.shadowsoffire.hostilenetworks.gui.FabDirectiveMenu;
import dev.shadowsoffire.hostilenetworks.gui.LootFabMenu;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberMenu;
import dev.shadowsoffire.hostilenetworks.item.BlankDataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.item.FabDirectiveItem;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;
import dev.shadowsoffire.hostilenetworks.util.SavedSelections;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntityType.TickSide;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class Hostile {

    private static final DeferredHelper R = DeferredHelper.create(HostileNetworks.MODID);

    public static class Blocks {
        public static final Holder<Block> SIM_CHAMBER = R.block("sim_chamber", SimChamberBlock::new, p -> p
            .lightLevel(s -> 1).strength(4, 3000).noOcclusion().isRedstoneConductor((state, lvl, pos) -> false));

        public static final Holder<Block> LOOT_FABRICATOR = R.block("loot_fabricator", LootFabBlock::new, p -> p
            .lightLevel(s -> 1).strength(4, 3000).noOcclusion().isRedstoneConductor((state, lvl, pos) -> false));

        public static final Holder<Block> DATA_CENTER = R.block("data_center", DataCenterBlock::new, p -> p
            .lightLevel(s -> 1).strength(5, 6000).noOcclusion().isRedstoneConductor((state, lvl, pos) -> false));

        private static void bootstrap() {}
    }

    public static class Items {
        public static final Holder<Item> BLANK_DATA_MODEL = R.item("blank_data_model", BlankDataModelItem::new, p -> p.stacksTo(1));
        public static final Holder<Item> PREDICTION_MATRIX = R.item("prediction_matrix", Item::new);
        public static final Holder<Item> OVERWORLD_PREDICTION = R.item("overworld_prediction", Item::new);
        public static final Holder<Item> NETHER_PREDICTION = R.item("nether_prediction", Item::new);
        public static final Holder<Item> END_PREDICTION = R.item("end_prediction", Item::new);
        public static final Holder<Item> TWILIGHT_PREDICTION = R.item("twilight_prediction", Item::new);
        public static final Holder<Item> DEEP_LEARNER = R.item("deep_learner", DeepLearnerItem::new, p -> p.stacksTo(1));
        public static final Holder<Item> DATA_MODEL = R.item("data_model", DataModelItem::new, p -> p.stacksTo(1));
        public static final Holder<Item> PREDICTION = R.item("prediction", MobPredictionItem::new);
        public static final Holder<Item> SIM_CHAMBER = R.blockItem("sim_chamber", Blocks.SIM_CHAMBER);
        public static final Holder<Item> LOOT_FABRICATOR = R.blockItem("loot_fabricator", Blocks.LOOT_FABRICATOR);
        public static final Holder<Item> DATA_CENTER = R.blockItem("data_center", Blocks.DATA_CENTER);
        public static final Holder<Item> FAB_DIRECTIVE = R.item("fab_directive", FabDirectiveItem::new, p -> p.stacksTo(1));

        private static void bootstrap() {}
    }

    public static class TileEntities {
        public static final BlockEntityType<SimChamberTileEntity> SIM_CHAMBER = R.tickingBlockEntity("sim_chamber", SimChamberTileEntity::new, TickSide.SERVER, Hostile.Blocks.SIM_CHAMBER);
        public static final BlockEntityType<LootFabTileEntity> LOOT_FABRICATOR = R.tickingBlockEntity("loot_fabricator", LootFabTileEntity::new, TickSide.SERVER, Hostile.Blocks.LOOT_FABRICATOR);
        public static final BlockEntityType<DataCenterTileEntity> DATA_CENTER = R.tickingBlockEntity("data_center", DataCenterTileEntity::new, TickSide.SERVER, Hostile.Blocks.DATA_CENTER);

        private static void bootstrap() {}
    }

    public static class Containers {
        public static final MenuType<DeepLearnerMenu> DEEP_LEARNER = R.menuWithData("deep_learner", DeepLearnerMenu::new);
        public static final MenuType<SimChamberMenu> SIM_CHAMBER = R.menuWithPos("sim_chamber", SimChamberMenu::new);
        public static final MenuType<LootFabMenu> LOOT_FABRICATOR = R.menuWithPos("loot_fabricator", LootFabMenu::new);
        public static final MenuType<DataCenterMenu> DATA_CENTER = R.menuWithPos("data_center", DataCenterMenu::new);
        public static final MenuType<FabDirectiveMenu> FAB_DIRECTIVE = R.menuWithData("fab_directive", FabDirectiveMenu::new);

        private static void bootstrap() {}
    }

    public static class Tabs {
        public static final Holder<CreativeModeTab> HNN_TAB = R.creativeTab("tab", b -> b.title(Component.translatable("itemGroup.hostilenetworks")).icon(() -> Items.SIM_CHAMBER.value().getDefaultInstance()));

        private static void bootstrap() {}
    }

    public static class Components {
        /**
         * Stored data model, used by {@link DataModelItem} and {@link MobPredictionItem}. The underlying holder may be unbound.
         */
        public static final DataComponentType<DynamicHolder<DataModel>> DATA_MODEL = R.component("data_model", b -> b
            .persistent(DataModelRegistry.INSTANCE.holderCodec())
            .networkSynchronized(DataModelRegistry.INSTANCE.holderStreamCodec()));

        /**
         * The amount of data captured in an individual {@link DataModelItem}, which determines the model tier.
         */
        public static final DataComponentType<Integer> DATA = R.component("data", b -> b
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

        /**
         * The number of iterations a {@link DataModelItem} has been run in the simulation chamber. Serves no gameplay purpose.
         */
        public static final DataComponentType<Integer> ITERATIONS = R.component("iterations", b -> b
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

        /**
         * The inventory of a {@link DeepLearnerItem}.
         */
        public static final DataComponentType<ItemContainerContents> LEARNER_INV = R.component("learner_inv", b -> b
            .persistent(ItemContainerContents.CODEC)
            .networkSynchronized(ItemContainerContents.STREAM_CODEC));

        /**
         * The stored selections retained by a {@link FabDirectiveItem}. This is a map of {@link EntityDataModel} to the index of the drop in the fab drops list.
         */
        public static final DataComponentType<SavedSelections> FAB_SELECTIONS = R.component("fab_selections", b -> b
            .persistent(SavedSelections.CODEC)
            .networkSynchronized(SavedSelections.STREAM_CODEC));

        private static void bootstrap() {}
    }

    public static class Tags {
        /**
         * The set of items that will receive the signature HNN colors as part of their item tooltips. Lime/Aqua borders with a gray center.
         */
        public static final TagKey<Item> CUSTOM_TOOLTIP_BORDER = TagKey.create(Registries.ITEM, HostileNetworks.loc("custom_tooltip_colors"));

        /**
         * Set of all Generalized <X> Prediction items. Used in crafting recipes.
         */
        public static final TagKey<Item> GENERALIZED_PREDICTIONS = TagKey.create(Registries.ITEM, HostileNetworks.loc("generalized_predictions"));

        /**
         * Block tag of materials the Data Center accepts for its 7×7 floor. Default contents (via datagen): {@code #c:obsidians}.
         */
        public static final TagKey<Block> DATA_CENTER_FLOOR = TagKey.create(Registries.BLOCK, HostileNetworks.loc("data_center_floor"));

        /**
         * Block tag of materials the Data Center accepts for its walls + ceiling. Default contents (via datagen):
         * {@code minecraft:black_stained_glass}.
         */
        public static final TagKey<Block> DATA_CENTER_WALL = TagKey.create(Registries.BLOCK, HostileNetworks.loc("data_center_wall"));
    }

    public static class Conditions {

        /**
         * Data-load condition gating {@link BlockDataModelsCondition the block data models}. All built-in block data
         * model jsons carry this condition, so they only load when the corresponding config option is enabled.
         */
        public static final MapCodec<BlockDataModelsCondition> BLOCK_DATA_MODELS_ENABLED = R.custom("block_data_models_enabled", NeoForgeRegistries.Keys.CONDITION_CODECS,
            BlockDataModelsCondition.CODEC);

        private static void bootstrap() {}
    }

    public static class Tickets {

        /**
         * Chunk-loading ticket controller for the Data Center multiblock. Ensures that the 7x7 is always loaded as to avoid chunk boundary issues.
         */
        public static final TicketController DATA_CENTER = new TicketController(HostileNetworks.loc("data_center"), DataCenterTileEntity::validateLoadedTickets);
    }

    static void bootstrap(IEventBus bus) {
        bus.register(R);
        Blocks.bootstrap();
        Items.bootstrap();
        TileEntities.bootstrap();
        Containers.bootstrap();
        Tabs.bootstrap();
        Components.bootstrap();
        Conditions.bootstrap();
    }

}
