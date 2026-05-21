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
     * Button id scheme:
     * <ul>
     *   <li>{@code -3}: empty the entire production queue while staying in Queue mode (model-dependent)</li>
     *   <li>{@code -2}: cycle production mode (model-dependent)</li>
     *   <li>{@code -1}: clear the Fixed selection (model-dependent)</li>
     *   <li>{@code 0..999}: drop click - selected in Fixed mode, appended in Queue mode (model-dependent)</li>
     *   <li>{@code 1000..1999}: remove queue entry at {@code id - 1000} (model-dependent)</li>
     *   <li>{@code 2000..2002}: set redstone state to {@code RedstoneState.values()[id - 2000]} (always available)</li>
     * </ul>
     */
    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        // Redstone control is model-independent - the player can toggle it whether or not a model is loaded.
        if (pId >= 2000 && pId < 2000 + RedstoneState.values().length) {
            this.setRedstoneState(RedstoneState.values()[pId - 2000]);
            return true;
        }
        DynamicHolder<DataModel> model = DataModelItem.getStoredModel(this.getSlot(0).getItem());
        if (!model.isBound()) return false;
        if (pId == -3) {
            this.tile.clearQueue(model);
            return true;
        }
        if (pId == -2) {
            this.tile.cycleMode(model);
            return true;
        }
        if (pId == -1) {
            this.tile.setFixedDrop(model, -1);
            return true;
        }
        if (pId >= 1000 && pId < 2000) {
            this.tile.removeFromQueue(model, pId - 1000);
            return true;
        }
        if (pId >= 0 && pId < model.get().fabDrops().size()) {
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
