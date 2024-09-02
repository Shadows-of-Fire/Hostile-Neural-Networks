package dev.shadowsoffire.hostilenetworks;

import com.mojang.serialization.Codec;

import dev.shadowsoffire.hostilenetworks.block.LootFabBlock;
import dev.shadowsoffire.hostilenetworks.block.SimChamberBlock;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelRegistry;
import dev.shadowsoffire.hostilenetworks.gui.DeepLearnerContainer;
import dev.shadowsoffire.hostilenetworks.gui.LootFabContainer;
import dev.shadowsoffire.hostilenetworks.gui.SimChamberContainer;
import dev.shadowsoffire.hostilenetworks.item.BlankDataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.DeepLearnerItem;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntityType.TickSide;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

public class Hostile {

    private static final DeferredHelper R = DeferredHelper.create(HostileNetworks.MODID);

    public static class Blocks {
        public static final Holder<Block> SIM_CHAMBER = R.block("sim_chamber", SimChamberBlock::new, p -> p.lightLevel(s -> 1).strength(4, 3000).noOcclusion());
        public static final Holder<Block> LOOT_FABRICATOR = R.block("loot_fabricator", LootFabBlock::new, p -> p.lightLevel(s -> 1).strength(4, 3000).noOcclusion());

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

        private static void bootstrap() {}
    }

    @SuppressWarnings("deprecation")
    public static class TileEntities {
        static {
            ((MappedRegistry<BlockEntityType<?>>) BuiltInRegistries.BLOCK_ENTITY_TYPE).unfreeze();
        }
        public static final BlockEntityType<SimChamberTileEntity> SIM_CHAMBER = R.tickingBlockEntity("sim_chamber", SimChamberTileEntity::new, TickSide.SERVER, Hostile.Blocks.SIM_CHAMBER);
        public static final BlockEntityType<LootFabTileEntity> LOOT_FABRICATOR = R.tickingBlockEntity("loot_fabricator", LootFabTileEntity::new, TickSide.SERVER, Hostile.Blocks.LOOT_FABRICATOR);

        private static void bootstrap() {}
    }

    public static class Containers {
        public static final MenuType<DeepLearnerContainer> DEEP_LEARNER = R.menuWithData("deep_learner", DeepLearnerContainer::new);
        public static final MenuType<SimChamberContainer> SIM_CHAMBER = R.menuWithPos("sim_chamber", SimChamberContainer::new);
        public static final MenuType<LootFabContainer> LOOT_FABRICATOR = R.menuWithPos("loot_fabricator", LootFabContainer::new);

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

        private static void bootstrap() {}
    }

    static void bootstrap(IEventBus bus) {
        bus.register(R);
        Blocks.bootstrap();
        Items.bootstrap();
        TileEntities.bootstrap();
        Containers.bootstrap();
        Tabs.bootstrap();
        Components.bootstrap();
    }

}
