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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class DataCenterIOPortBlock extends BaseEntityBlock {

    public static final EnumProperty<IOPortMode> MODE = EnumProperty.create("mode", IOPortMode.class);

    public DataCenterIOPortBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.defaultBlockState().setValue(MODE, IOPortMode.ENERGY));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
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
