package shadows.hostilenetworks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import shadows.hostilenetworks.gui.LootFabContainer;
import shadows.hostilenetworks.tile.LootFabTileEntity;
import shadows.placebo.block_entity.TickingEntityBlock;

public class LootFabBlock extends HorizontalDirectionalBlock implements TickingEntityBlock {

	public LootFabBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		pBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext pContext) {
		return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
		return new LootFabTileEntity(pPos, pState);
	}

	@Override
	public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
		if (pLevel.isClientSide) {
			return InteractionResult.SUCCESS;
		} else {
			NetworkHooks.openGui((ServerPlayer) pPlayer, this.getMenuProvider(pState, pLevel, pPos), pPos);
			return InteractionResult.CONSUME;
		}
	}

	@Override
	public MenuProvider getMenuProvider(BlockState pState, Level pLevel, BlockPos pPos) {
		return new SimpleMenuProvider((id, inv, player) -> new LootFabContainer(id, inv, pPos), new TranslatableComponent(this.getDescriptionId()));
	}

}
