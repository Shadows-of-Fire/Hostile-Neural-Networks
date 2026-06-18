package dev.shadowsoffire.hostilenetworks.gui;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.DataCenterTileEntity.DataCenterItemHandler;
import dev.shadowsoffire.hostilenetworks.tile.SimChamberTileEntity.FailureState;
import dev.shadowsoffire.hostilenetworks.util.FabSelection;
import dev.shadowsoffire.hostilenetworks.util.FabSelection.ProductionMode;
import dev.shadowsoffire.hostilenetworks.util.RedstoneState;
import dev.shadowsoffire.placebo.menu.BlockEntityMenu;
import dev.shadowsoffire.placebo.menu.FilteredSlot;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class DataCenterMenu extends BlockEntityMenu<DataCenterTileEntity> {

    public static final int REDSTONE_BASE = 2000;

    public static final int FAB_CYCLE_MODE_BASE = 4000;
    public static final int FAB_CLEAR_FIXED_BASE = 4100;
    public static final int FAB_CLEAR_QUEUE_BASE = 4200;
    public static final int FAB_STRIDE = 100;
    public static final int FAB_DROP_BASE = 5000;
    public static final int FAB_QUEUE_REMOVE_BASE = 8000;

    public static final int MODEL_GRID_X = 9;
    public static final int MODEL_GRID_Y = 21;
    public static final int INPUT_GRID_X = 132;
    public static final int INPUT_GRID_Y = 20;
    public static final int OUTPUT_GRID_X = 132;
    public static final int OUTPUT_GRID_Y = 48;
    public static final int PLAYER_INV_X = 35;
    public static final int PLAYER_INV_Y = 146;

    public DataCenterMenu(int id, Inventory pInv, BlockPos pos) {
        super(Hostile.Containers.DATA_CENTER, id, pInv, pos);
        DataCenterItemHandler inv = this.tile.getInventory();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int idx = row * 5 + col;
                this.addSlot(new FilteredSlot(inv, idx, MODEL_GRID_X + col * 18, MODEL_GRID_Y + row * 20, s -> s.getItem() instanceof DataModelItem));
            }
        }
        for (int col = 0; col < DataCenterTileEntity.INPUT_SLOTS; col++) {
            int idx = DataCenterTileEntity.INPUT_START + col;
            this.addSlot(new FilteredSlot(inv, idx, INPUT_GRID_X + col * 18, INPUT_GRID_Y, this::isAnyInstalledModelInput));
        }
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int idx = DataCenterTileEntity.OUTPUT_START + row * 4 + col;
                this.addSlot(new FilteredSlot(inv, idx, OUTPUT_GRID_X + col * 18, OUTPUT_GRID_Y + row * 18, s -> false));
            }
        }
        this.addPlayerSlots(pInv, PLAYER_INV_X, PLAYER_INV_Y - 6);
        int firstPlayerSlot = DataCenterTileEntity.TOTAL_SLOTS;
        this.mover.registerRule((stack, slot) -> slot < firstPlayerSlot, firstPlayerSlot, this.slots.size());
        this.mover.registerRule((stack, slot) -> stack.getItem() instanceof DataModelItem, 0, DataCenterTileEntity.MODEL_SLOTS);
        this.mover.registerRule((stack, slot) -> this.isAnyInstalledModelInput(stack), DataCenterTileEntity.INPUT_START, DataCenterTileEntity.OUTPUT_START);
        this.registerInvShuffleRules();
    }

    public DataCenterTileEntity getTile() {
        return this.tile;
    }

    private boolean isAnyInstalledModelInput(ItemStack stack) {
        DataCenterItemHandler inv = this.tile.getInventory();
        for (int m = 0; m < DataCenterTileEntity.MODEL_SLOTS; m++) {
            ItemStack modelStack = inv.getStackInSlot(m);
            if (!modelStack.isEmpty() && DataModelItem.matchesModelInput(modelStack, stack)) return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return pPlayer.level().getBlockState(this.pos).is(Hostile.Blocks.DATA_CENTER);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= REDSTONE_BASE && id < REDSTONE_BASE + RedstoneState.values().length) {
            this.tile.setRedstoneState(RedstoneState.values()[id - REDSTONE_BASE]);
            return true;
        }

        int slot;
        if ((slot = matchSimple(id, FAB_CYCLE_MODE_BASE)) >= 0) {
            DynamicHolder<DataModel> holder = boundModelAt(slot);
            if (holder != null) this.tile.cycleMode(holder);
            return true;
        }
        if ((slot = matchSimple(id, FAB_CLEAR_FIXED_BASE)) >= 0) {
            DynamicHolder<DataModel> holder = boundModelAt(slot);
            if (holder != null) this.tile.setFixedDrop(holder, -1);
            return true;
        }
        if ((slot = matchSimple(id, FAB_CLEAR_QUEUE_BASE)) >= 0) {
            DynamicHolder<DataModel> holder = boundModelAt(slot);
            if (holder != null) this.tile.clearQueue(holder);
            return true;
        }
        if ((slot = matchComposite(id, FAB_DROP_BASE)) >= 0) {
            DynamicHolder<DataModel> holder = boundModelAt(slot);
            if (holder != null) {
                int dropIdx = (id - FAB_DROP_BASE) % FAB_STRIDE;
                FabSelection sel = this.tile.getSelection(holder.get());
                if (sel.mode() == ProductionMode.QUEUE) this.tile.appendToQueue(holder, dropIdx);
                else this.tile.setFixedDrop(holder, dropIdx);
            }
            return true;
        }
        if ((slot = matchComposite(id, FAB_QUEUE_REMOVE_BASE)) >= 0) {
            DynamicHolder<DataModel> holder = boundModelAt(slot);
            if (holder != null) {
                int queuePos = (id - FAB_QUEUE_REMOVE_BASE) % FAB_STRIDE;
                this.tile.removeFromQueue(holder, queuePos);
            }
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    private static int matchSimple(int id, int base) {
        if (id < base || id >= base + DataCenterTileEntity.MODEL_SLOTS) return -1;
        return id - base;
    }

    private static int matchComposite(int id, int base) {
        if (id < base || id >= base + DataCenterTileEntity.MODEL_SLOTS * FAB_STRIDE) return -1;
        return (id - base) / FAB_STRIDE;
    }

    private DynamicHolder<DataModel> boundModelAt(int slotIdx) {
        if (slotIdx < 0 || slotIdx >= DataCenterTileEntity.MODEL_SLOTS) return null;
        DynamicHolder<DataModel> holder = DataModelItem.getStoredModel(this.tile.getInventory().getStackInSlot(slotIdx));
        return holder.isBound() ? holder : null;
    }

    public FabSelection getSelection(int slotIdx) {
        DynamicHolder<DataModel> holder = this.boundModelAt(slotIdx);
        if (holder == null) return FabSelection.EMPTY;
        return this.tile.getSelection(holder.get());
    }

    public DynamicHolder<DataModel> getModelHolder(int slotIdx) {
        DynamicHolder<DataModel> holder = this.boundModelAt(slotIdx);
        return holder != null ? holder : dev.shadowsoffire.hostilenetworks.data.DataModelRegistry.INSTANCE.emptyHolder();
    }

    public int getEnergyStored() {
        return this.tile.getEnergyStored();
    }

    public int getRuntime(int slot) {
        return this.tile.getRuntime(slot);
    }

    public FailureState getFailState(int slot) {
        return this.tile.getFailState(slot);
    }

    public RedstoneState getRedstoneState() {
        return this.tile.getRedstoneState();
    }

    public boolean isShellValid() {
        return this.tile.isShellValid();
    }

}
