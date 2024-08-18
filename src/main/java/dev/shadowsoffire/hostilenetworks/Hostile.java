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
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntityType.TickSide;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

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

    public static class TileEntities {
        public static final Holder<BlockEntityType<?>> SIM_CHAMBER = R.tickingBlockEntity("sim_chamber", SimChamberTileEntity::new, () -> ImmutableSet.of(Hostile.Blocks.SIM_CHAMBER.value()), TickSide.SERVER);
        public static final Holder<BlockEntityType<?>> LOOT_FABRICATOR = R.tickingBlockEntity("loot_fabricator", LootFabTileEntity::new, () -> ImmutableSet.of(Hostile.Blocks.LOOT_FABRICATOR.value()), TickSide.SERVER);

        private static void bootstrap() {}
    }

    public static class Containers {
        public static final Holder<MenuType<?>> DEEP_LEARNER = R.menuWithData("deep_learner", DeepLearnerContainer::new);
        public static final Holder<MenuType<?>> SIM_CHAMBER = R.menuWithPos("sim_chamber", SimChamberContainer::new);
        public static final Holder<MenuType<?>> LOOT_FABRICATOR = R.menuWithPos("loot_fabricator", LootFabContainer::new);

        private static void bootstrap() {}
    }

    public static class Tabs {
        public static final Holder<CreativeModeTab> HNN_TAB = R.creativeTab("tab", b -> b.title(Component.translatable("itemGroup.hostilenetworks")).icon(() -> Items.SIM_CHAMBER.value().getDefaultInstance()));

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
