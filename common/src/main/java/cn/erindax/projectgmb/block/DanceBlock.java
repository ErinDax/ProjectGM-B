package cn.erindax.projectgmb.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class DanceBlock extends BaseEntityBlock {

	public DanceBlock(Properties properties) {
		super(properties);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DanceBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult) {
		InteractionResult result = useItemOn(player.getItemInHand(hand), level, pos, player, hand, hitResult);
		if (result == InteractionResult.PASS && hand == InteractionHand.MAIN_HAND) {
			return useWithoutItem(level, pos, player);
		}
		return result;
	}

	private InteractionResult useItemOn(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand,
			BlockHitResult hitResult) {
		if (!(stack.getItem() instanceof BlockItem item) || !player.mayBuild()
				|| !(level.getBlockEntity(pos) instanceof DanceBlockEntity dance)) {
			return InteractionResult.PASS;
		}
		BlockState disguise = disguiseFor(item.getBlock(), player, hand, stack, hitResult);
		if (disguise == null) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			dance.setDisguise(disguise);
			level.playSound(null, pos, disguise.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	private InteractionResult useWithoutItem(Level level, BlockPos pos, Player player) {
		if (!player.isSecondaryUseActive() || !player.mayBuild()
				|| !(level.getBlockEntity(pos) instanceof DanceBlockEntity dance) || dance.disguise() == null) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide()) {
			dance.setDisguise(null);
			level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState disguise = disguiseAt(level, pos);
		return disguise == null ? Shapes.block() : disguise.getShape(level, pos, context);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		BlockState disguise = disguiseAt(level, pos);
		return disguise == null ? Shapes.block() : disguise.getCollisionShape(level, pos, context);
	}

	@Nullable
	private static BlockState disguiseAt(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof DanceBlockEntity dance ? dance.disguise() : null;
	}

	@Nullable
	private BlockState disguiseFor(Block block, Player player, InteractionHand hand, ItemStack stack,
			BlockHitResult hitResult) {
		if (block instanceof DanceBlock) {
			return null;
		}
		BlockState state = block.getStateForPlacement(new BlockPlaceContext(player, hand, stack, hitResult));
		if (state == null || !state.is(block)) {
			state = block.defaultBlockState();
		}
		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			state = state.setValue(BlockStateProperties.WATERLOGGED, false);
		}
		if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
			return null;
		}
		return state;
	}
}
