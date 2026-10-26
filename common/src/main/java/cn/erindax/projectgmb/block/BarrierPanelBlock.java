package cn.erindax.projectgmb.block;

import cn.erindax.projectgmb.compat.visiblebarriers.VisibleBarriersCompat;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

public class BarrierPanelBlock extends MultifaceBlock {

	public static final MapCodec<BarrierPanelBlock> CODEC = simpleCodec(BarrierPanelBlock::new);

	private static final double THICKNESS = 0.1;
	private static final Map<Direction, VoxelShape> FACE_SHAPES = new EnumMap<>(Map.of(
		Direction.DOWN, box(0, 0, 0, 16, THICKNESS, 16),
		Direction.UP, box(0, 16 - THICKNESS, 0, 16, 16, 16),
		Direction.NORTH, box(0, 0, 0, 16, 16, THICKNESS),
		Direction.SOUTH, box(0, 0, 16 - THICKNESS, 16, 16, 16),
		Direction.WEST, box(0, 0, 0, THICKNESS, 16, 16),
		Direction.EAST, box(16 - THICKNESS, 0, 0, 16, 16, 16)));

	private final ImmutableMap<BlockState, VoxelShape> shapes = getShapeForEachState(BarrierPanelBlock::shapeFor);

	public BarrierPanelBlock(Properties properties) {
		super(properties);
	}

	private static VoxelShape shapeFor(BlockState state) {
		VoxelShape shape = Shapes.empty();
		for (Direction direction : DIRECTIONS) {
			if (hasFace(state, direction)) {
				shape = Shapes.or(shape, FACE_SHAPES.get(direction));
			}
		}
		return shape.isEmpty() ? Shapes.block() : shape;
	}

	@Override
	protected MapCodec<? extends MultifaceBlock> codec() {
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shapes.get(state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return VisibleBarriersCompat.isBarrierVisible() ? RenderShape.MODEL : RenderShape.INVISIBLE;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return true;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return state;
	}

	@Override
	public boolean isValidStateForPlacement(BlockGetter level, BlockState state, BlockPos pos, Direction direction) {
		return isFaceSupported(direction) && (!state.is(this) || !hasFace(state, direction));
	}

	@Override
	protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return context.getItemInHand().is(asItem())
			&& Arrays.stream(DIRECTIONS).anyMatch(direction -> !hasFace(state, direction))
			&& !context.isSecondaryUseActive();
	}

	@Override
	public MultifaceSpreader getSpreader() {
		return null;
	}
}
