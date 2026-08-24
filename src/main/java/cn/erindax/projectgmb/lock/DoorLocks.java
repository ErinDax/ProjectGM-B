package cn.erindax.projectgmb.lock;

import cn.erindax.projectgmb.network.SyncDoorLocksPayload;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DoorLocks {

	private DoorLocks() {
	}

	public static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (player.isSpectator()) {
				return InteractionResult.PASS;
			}
			ItemStack stack = player.getItemInHand(hand);
			BlockPos pos = hit.getBlockPos();
			BlockState state = world.getBlockState(pos);
			if (isLockItem(stack) && player.isCreative() && hand == InteractionHand.MAIN_HAND) {
				if (!isLockable(state)) {
					return InteractionResult.PASS;
				}
				if (world instanceof ServerLevel serverLevel) {
					toggle(serverLevel, player, pos, state);
				}
				return InteractionResult.sidedSuccess(world.isClientSide);
			}
			if (isLockable(state) && !canOpen(world, pos, player)) {
				deny(world, pos, player);
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> syncTo(handler.player));
	}

	public static boolean isLockItem(ItemStack stack) {
		return nameMatches(stack, LockNames.lockItemName());
	}

	public static boolean isKeyItem(ItemStack stack) {
		return nameMatches(stack, LockNames.keyItemName());
	}

	public static boolean isLockable(BlockState state) {
		return state.is(BlockTags.WOODEN_DOORS) || state.is(BlockTags.WOODEN_TRAPDOORS);
	}

	public static boolean canOpen(Level level, BlockPos pos, Player player) {
		DoorLockRecord record = get(level, pos);
		if (record == null) {
			return true;
		}
		if (record.owner().equals(player.getUUID())) {
			return true;
		}
		return isKeyItem(player.getMainHandItem()) || isKeyItem(player.getOffhandItem());
	}

	public static DoorLockRecord get(Level level, BlockPos pos) {
		if (level.isClientSide) {
			return DoorLockClient.get(pos);
		}
		if (level instanceof ServerLevel serverLevel) {
			return DoorLockSavedData.get(serverLevel).get(pos);
		}
		return null;
	}

	public static void onBlockRemoved(Level level, BlockPos pos, BlockState state, BlockState newState) {
		if (level.isClientSide || !isLockable(state) || state.is(newState.getBlock())) {
			return;
		}
		if (level instanceof ServerLevel serverLevel) {
			DoorLockSavedData data = DoorLockSavedData.get(serverLevel);
			data.remove(pos);
			data.sync(serverLevel);
		}
	}

	public static void deny(Level level, BlockPos pos, Player player) {
		if (!level.isClientSide) {
			player.displayClientMessage(Component.translatable("message.projectgm_b.door_locked"), true);
			level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.2F);
		}
	}

	private static String stackName(ItemStack stack) {
		return stack.isEmpty() ? "" : stack.getHoverName().getString();
	}

	private static boolean nameMatches(ItemStack stack, String expected) {
		return !stack.isEmpty() && !expected.isEmpty() && stackName(stack).equals(expected);
	}

	private static void toggle(ServerLevel level, Player player, BlockPos pos, BlockState state) {
		DoorLockSavedData data = DoorLockSavedData.get(level);
		List<BlockPos> related = related(level, pos, state);
		boolean locked = false;
		for (BlockPos part : related) {
			if (data.get(part) != null) {
				locked = true;
				break;
			}
		}
		if (locked) {
			for (BlockPos part : related) {
				data.remove(part);
			}
			player.displayClientMessage(Component.translatable("message.projectgm_b.door_unlocked"), true);
			level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 1.2F);
		} else {
			DoorLockRecord record = new DoorLockRecord(player.getUUID());
			for (BlockPos part : related) {
				data.put(part, record);
			}
			player.displayClientMessage(Component.translatable("message.projectgm_b.door_locked_unnamed"), true);
			level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.2F);
		}
		data.sync(level);
	}

	public static void syncTo(ServerPlayer player) {
		if (player.level() instanceof ServerLevel serverLevel) {
			ServerPlayNetworking.send(player, SyncDoorLocksPayload.of(DoorLockSavedData.get(serverLevel).snapshot()));
		}
	}

	private static List<BlockPos> related(Level level, BlockPos pos, BlockState state) {
		Set<BlockPos> out = new LinkedHashSet<>();
		if (state.getBlock() instanceof DoorBlock) {
			BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
			out.add(lower.immutable());
			out.add(lower.above());
			addPartner(level, lower, out);
		} else {
			out.add(pos.immutable());
		}
		return new ArrayList<>(out);
	}

	private static void addPartner(Level level, BlockPos lower, Set<BlockPos> out) {
		BlockState state = level.getBlockState(lower);
		if (!(state.getBlock() instanceof DoorBlock)) {
			return;
		}
		Direction facing = state.getValue(DoorBlock.FACING);
		DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);
		Direction toward = hinge == DoorHingeSide.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
		BlockPos other = lower.relative(toward);
		BlockState otherState = level.getBlockState(other);
		if (otherState.getBlock() instanceof DoorBlock
			&& otherState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
			&& otherState.getValue(DoorBlock.FACING) == facing
			&& otherState.getValue(DoorBlock.HINGE) != hinge) {
			out.add(other.immutable());
			out.add(other.above());
		}
	}
}
