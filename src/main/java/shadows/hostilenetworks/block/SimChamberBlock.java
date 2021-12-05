package shadows.hostilenetworks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import shadows.hostilenetworks.gui.SimChamberContainer;
import shadows.hostilenetworks.tile.SimChamberTileEntity;

public class SimChamberBlock extends HorizontalBlock {

	public SimChamberBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
		pBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext pContext) {
		return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public ActionResultType use(BlockState pState, World pLevel, BlockPos pPos, PlayerEntity pPlayer, Hand pHand, BlockRayTraceResult pHit) {
		if (pLevel.isClientSide) {
			return ActionResultType.SUCCESS;
		} else {
			NetworkHooks.openGui((ServerPlayerEntity) pPlayer, this.getMenuProvider(pState, pLevel, pPos), pPos);
			return ActionResultType.CONSUME;
		}
	}

	@Override
	public INamedContainerProvider getMenuProvider(BlockState pState, World pLevel, BlockPos pPos) {
		return new SimpleNamedContainerProvider((id, inv, player) -> new SimChamberContainer(id, inv, pPos), new TranslationTextComponent(this.getDescriptionId()));
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new SimChamberTileEntity();
	}

}
