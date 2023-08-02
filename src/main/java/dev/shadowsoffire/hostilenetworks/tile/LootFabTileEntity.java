package dev.shadowsoffire.hostilenetworks.tile;

import java.util.function.Consumer;

import dev.shadowsoffire.hostilenetworks.Hostile;
import dev.shadowsoffire.hostilenetworks.HostileConfig;
import dev.shadowsoffire.hostilenetworks.data.DataModel;
import dev.shadowsoffire.hostilenetworks.data.DataModelManager;
import dev.shadowsoffire.hostilenetworks.item.MobPredictionItem;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntity;
import dev.shadowsoffire.placebo.cap.InternalItemHandler;
import dev.shadowsoffire.placebo.cap.ModifiableEnergyStorage;
import dev.shadowsoffire.placebo.container.EasyContainerData;
import dev.shadowsoffire.placebo.container.EasyContainerData.IDataAutoRegister;
import dev.shadowsoffire.placebo.recipe.VanillaPacketDispatcher;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

public class LootFabTileEntity extends BlockEntity implements TickingBlockEntity, IDataAutoRegister {

    protected final FabItemHandler inventory = new FabItemHandler();
    protected final ModifiableEnergyStorage energy = new ModifiableEnergyStorage(HostileConfig.fabPowerCap, HostileConfig.fabPowerCap);
    protected final Object2IntMap<DataModel> savedSelections = new Object2IntOpenHashMap<>();
    protected final EasyContainerData data = new EasyContainerData();

    protected int runtime = 0;
    protected int currentSel = -1;

    public LootFabTileEntity(BlockPos pos, BlockState state) {
        super(Hostile.TileEntities.LOOT_FABRICATOR.get(), pos, state);
        this.savedSelections.defaultReturnValue(-1);
        this.data.addData(() -> this.runtime, v -> this.runtime = v);
        this.data.addEnergy(this.energy);
    }

    @Override
    public void registerSlots(Consumer<DataSlot> consumer) {
        this.data.register(consumer);
    }

    @Override
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        DataModel dm = MobPredictionItem.getStoredModel(this.inventory.getStackInSlot(0));
        if (dm != null) {
            int selection = this.savedSelections.getInt(dm);
            if (this.currentSel != selection) {
                this.currentSel = selection;
                this.runtime = 0;
                return;
            }
            if (selection != -1) {
                if (this.runtime == 0) {
                    ItemStack out = dm.getFabDrops().get(selection).copy();
                    if (this.insertInOutput(out, true)) this.runtime = 60;
                }
                else {
                    if (this.energy.getEnergyStored() < HostileConfig.fabPowerCost) return;
                    this.energy.setEnergy(this.energy.getEnergyStored() - HostileConfig.fabPowerCost);
                    if (--this.runtime == 0) {
                        this.insertInOutput(dm.getFabDrops().get(selection).copy(), false);
                        this.inventory.getStackInSlot(0).shrink(1);
                    }
                    this.setChanged();
                }
            }
            else this.runtime = 0;
        }
        else this.runtime = 0;
    }

    protected boolean insertInOutput(ItemStack stack, boolean sim) {
        for (int i = 1; i < 17; i++) {
            stack = this.inventory.insertItemInternal(i, stack, sim);
            if (stack.isEmpty()) return true;
        }
        return false;
    }

    public FabItemHandler getInventory() {
        return this.inventory;
    }

    public void setSelection(DataModel model, int pId) {
        if (pId == -1) this.savedSelections.removeInt(model);
        else this.savedSelections.put(model, Mth.clamp(pId, 0, model.getFabDrops().size() - 1));
        VanillaPacketDispatcher.dispatchTEToNearbyPlayers(this);
        this.setChanged();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.of(() -> this.inventory).cast();
        if (cap == ForgeCapabilities.ENERGY) return LazyOptional.of(() -> this.energy).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        tag.put("inventory", this.inventory.serializeNBT());
        tag.putInt("energy", this.energy.getEnergyStored());
        tag.putInt("runtime", this.runtime);
        tag.putInt("selection", this.currentSel);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.readSelections(tag.getCompound("saved_selections"));
        this.inventory.deserializeNBT(tag.getCompound("inventory"));
        this.energy.setEnergy(tag.getInt("energy"));
        this.runtime = tag.getInt("runtime");
        this.currentSel = tag.getInt("selection");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, t -> ((LootFabTileEntity) t).writeSync());
    }

    private CompoundTag writeSync() {
        CompoundTag tag = new CompoundTag();
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        this.readSelections(pkt.getTag().getCompound("saved_selections"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.put("saved_selections", this.writeSelections(new CompoundTag()));
        return tag;
    }

    private CompoundTag writeSelections(CompoundTag tag) {
        for (Object2IntMap.Entry<DataModel> e : this.savedSelections.object2IntEntrySet()) {
            tag.putInt(e.getKey().getId().toString(), e.getIntValue());
        }
        return tag;
    }

    private void readSelections(CompoundTag tag) {
        this.savedSelections.clear();
        for (String s : tag.getAllKeys()) {
            DataModel dm = DataModelManager.INSTANCE.getValue(new ResourceLocation(s));
            this.savedSelections.put(dm, Mth.clamp(tag.getInt(s), 0, dm.getFabDrops().size() - 1));
        }
    }

    public int getEnergyStored() {
        return this.energy.getEnergyStored();
    }

    public int getRuntime() {
        return this.runtime;
    }

    public int getSelectedDrop(DataModel model) {
        return model == null ? -1 : this.savedSelections.getInt(model);
    }

    public class FabItemHandler extends InternalItemHandler {

        public FabItemHandler() {
            super(17);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.getItem() == Hostile.Items.PREDICTION.get();
            return true;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot > 0) return stack;
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == 0) return ItemStack.EMPTY;
            return super.extractItem(slot, amount, simulate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            LootFabTileEntity.this.setChanged();
        }
    }

}
