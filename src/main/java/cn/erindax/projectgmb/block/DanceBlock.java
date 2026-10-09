package cn.erindax.projectgmb.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
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

	public static final MapCodec<DanceBlock> CODEC = simpleCodec(DanceBlock::new);

	public DanceBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new DanceBlockEntity(pos, state);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hitResult) {
		if (!(stack.getItem() instanceof BlockItem item) || !player.mayBuild()
				|| !(level.getBlockEntity(pos) instanceof DanceBlockEntity dance)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		BlockState disguise = disguiseFor(item.getBlock(), level, player, hand, stack, hitResult);
		if (disguise == null) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!level.isClientSide()) {
			dance.setDisguise(disguise);
			level.playSound(null, pos, disguise.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide());
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
			BlockHitResult hitResult) {
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
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		BlockState disguise = disguiseAt(level, pos);
		return disguise == null ? Shapes.block() : disguise.getShape(level, pos, context);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		BlockState disguise = disguiseAt(level, pos);
		return disguise == null ? Shapes.block() : disguise.getCollisionShape(level, pos, context);
	}

	@Nullable
	private static BlockState disguiseAt(BlockGetter level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof DanceBlockEntity dance ? dance.disguise() : null;
	}

	@Nullable
	private BlockState disguiseFor(Block block, Level level, Player player, InteractionHand hand, ItemStack stack,
			BlockHitResult hitResult) {
		if (block instanceof DanceBlock) {
			return null;
		}
		BlockState state = block.getStateForPlacement(new BlockPlaceContext(level, player, hand, stack, hitResult));
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
