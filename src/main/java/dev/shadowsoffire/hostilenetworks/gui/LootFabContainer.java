package dev.shadowsoffire.hostilenetworks.gui;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.item.DataModelItem;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity;
import dev.shadowsoffire.hostilenetworks.tile.LootFabTileEntity.FabItemHandler;
import dev.shadowsoffire.placebo.menu.BlockEntityMenu;
import dev.shadowsoffire.placebo.menu.FilteredSlot;
import dev.shadowsoffire.placebo.reload.DynamicHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class LootFabContainer extends BlockEntityMenu<LootFabTileEntity> {

    public LootFabContainer(int id, Inventory pInv, BlockPos pos) {
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

    @Override
    public boolean clickMenuButton(Player pPlayer, int pId) {
        DynamicHolder<DataModel> model = DataModelItem.getStoredModel(this.getSlot(0).getItem());
        if (!model.isBound() || pId >= model.get().fabDrops().size()) return false;
        this.tile.setSelection(model, pId);
        return true;
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

}
