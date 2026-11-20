package dev.shadowsoffire.hostilenetworks.block;

import com.mojang.serialization.MapCodec;

import dev.shadowsoffire.hostilenetworks.tile.DataCenterIOPortTileEntity;
import dev.shadowsoffire.hostilenetworks.util.IOPortMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class DataCenterIOPortBlock extends BaseEntityBlock {

    public static final EnumProperty<IOPortMode> MODE = EnumProperty.create("mode", IOPortMode.class);
    /** While locked, the mode cannot be cycled. Toggled by sneak-right-clicking with empty hands. */
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");

    public DataCenterIOPortBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(MODE, IOPortMode.ENERGY).setValue(LOCKED, false));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(MODE, LOCKED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Leave the click to a held item, so that placing pipes or cables against a port never changes it.
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isSecondaryUseActive()) {
            BlockState toggled = state.cycle(LOCKED);
            level.setBlock(pos, toggled, Block.UPDATE_CLIENTS);
            player.displayClientMessage(
                Component.translatable(toggled.getValue(LOCKED) ? "hostilenetworks.io_port.locked" : "hostilenetworks.io_port.unlocked"),
                true);
            return InteractionResult.SUCCESS;
        }

        if (state.getValue(LOCKED)) {
            player.displayClientMessage(
                Component.translatable("hostilenetworks.io_port.mode_locked",
                    Component.translatable(state.getValue(MODE).getTranslationKey())),
                true);
            return InteractionResult.SUCCESS;
        }

        BlockState next = state.cycle(MODE);
        level.setBlock(pos, next, Block.UPDATE_ALL);
        level.invalidateCapabilities(pos);
        player.displayClientMessage(
            Component.translatable("hostilenetworks.io_port.mode_set",
                Component.translatable(next.getValue(MODE).getTranslationKey())),
            true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataCenterIOPortTileEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }
}
