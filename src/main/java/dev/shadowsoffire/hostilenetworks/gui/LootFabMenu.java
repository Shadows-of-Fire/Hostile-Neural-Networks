package dev.shadowsoffire.hostilenetworks.gui;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity.FabItemHandler;
import dev.shadowsoffire.hostilenetworks.util.FabSelection;
import dev.shadowsoffire.hostilenetworks.util.FabSelection.ProductionMode;
import dev.shadowsoffire.hostilenetworks.util.RedstoneState;
import dev.shadowsoffire.placebo.menu.BlockEntityMenu;
import dev.shadowsoffire.placebo.menu.FilteredSlot;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class LootFabMenu extends BlockEntityMenu<LootFabTileEntity> {

    // --- Button id scheme (single source of truth, also used by LootFabScreen) ---
    /** Empties the entire production queue while staying in Queue mode. */
    public static final int BTN_CLEAR_QUEUE = -3;
    /** Cycles the production mode between Fixed and Queue. */
    public static final int BTN_CYCLE_MODE = -2;
    /** Clears the Fixed-mode selection. */
    public static final int BTN_CLEAR_FIXED = -1;
    /** Drop palette click: ids in {@code [DROP_BASE, QUEUE_REMOVE_BASE)} are clicked drop indices (selected in Fixed, appended in Queue). */
    public static final int DROP_BASE = 0;
    /** Queue removal: ids in {@code [QUEUE_REMOVE_BASE, REDSTONE_BASE)} target queue entry {@code id - QUEUE_REMOVE_BASE}. */
    public static final int QUEUE_REMOVE_BASE = 1000;
    /** Redstone state set: ids in {@code [REDSTONE_BASE, REDSTONE_BASE + RedstoneState.values().length)} set the state from the ordinal. */
    public static final int REDSTONE_BASE = 2000;

    public LootFabMenu(int id, Inventory pInv, BlockPos pos) {
        super(Hostile.Containers.LOOT_FABRICATOR, id, pInv, pos);
        FabItemHandler inv = this.tile.getInventory();
        this.addSlot(new FilteredSlot(inv, 0, 79, 62, s -> s.is(Hostile.Items.PREDICTION)));
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                this.addSlot(new FilteredSlot(inv, 1 + y * 4 + x, 100 + x * 18, 7 + y * 18, s -> false));
            }
        }
        this.addPlayerSlots(pInv, 8, 96);
        this.mover.registerRule((stack, slot) -> slot == 0, 17, this.slots.size());
        this.mover.registerRule((stack, slot) -> stack.getItem() instanceof MobPredictionItem, 0, 1);
        this.mover.registerRule((stack, slot) -> slot < 17, 17, this.slots.size());
        this.registerInvShuffleRules();
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.level().getBlockState(this.pos).is(Hostile.Blocks.LOOT_FABRICATOR);
    }

    /**
     * Routes a clicked button id according to the scheme declared at the top of this class.
     *
     * @see #BTN_CLEAR_QUEUE
     * @see #BTN_CYCLE_MODE
     * @see #BTN_CLEAR_FIXED
     * @see #DROP_BASE
     * @see #QUEUE_REMOVE_BASE
     * @see #REDSTONE_BASE
     */
    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        // Redstone control is model-independent - the player can toggle it whether or not a model is loaded.
        if (pId >= REDSTONE_BASE && pId < REDSTONE_BASE + RedstoneState.values().length) {
            this.setRedstoneState(RedstoneState.values()[pId - REDSTONE_BASE]);
            return true;
        }
        DynamicHolder<DataModel> model = DataModelItem.getStoredModel(this.getSlot(0).getItem());
        if (!model.isBound()) return false;
        if (pId == BTN_CLEAR_QUEUE) {
            this.tile.clearQueue(model);
            return true;
        }
        if (pId == BTN_CYCLE_MODE) {
            this.tile.cycleMode(model);
            return true;
        }
        if (pId == BTN_CLEAR_FIXED) {
            this.tile.setFixedDrop(model, -1);
            return true;
        }
        if (pId >= QUEUE_REMOVE_BASE && pId < REDSTONE_BASE) {
            this.tile.removeFromQueue(model, pId - QUEUE_REMOVE_BASE);
            return true;
        }
        if (pId >= DROP_BASE && pId < QUEUE_REMOVE_BASE && pId < model.get().fabDrops().size()) {
            if (this.tile.getSelection(model.get()).mode() == ProductionMode.QUEUE) {
                this.tile.appendToQueue(model, pId);
            }
            else {
                this.tile.setFixedDrop(model, pId);
            }
            return true;
        }
        return false;
    }

    public void setRedstoneState(RedstoneState state) {
        this.tile.setRedstoneState(state);
    }

    public RedstoneState getRedstoneState() {
        return this.tile.getRedstoneState();
    }

    public int getEnergyStored() {
        return this.tile.getEnergyStored();
    }

    public int getRuntime() {
        return this.tile.getRuntime();
    }

    public int getSelectedDrop(DataModel model) {
        return this.tile.getSelectedDrop(model);
    }

    public FabSelection getSelection(DataModel model) {
        return this.tile.getSelection(model);
    }

}
