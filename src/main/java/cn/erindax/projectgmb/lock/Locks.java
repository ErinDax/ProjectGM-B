package cn.erindax.projectgmb.lock;

import cn.erindax.projectgmb.lock.net.LockSyncPayload;
import cn.erindax.projectgmb.lock.net.LockUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

public final class Locks {

	public record Found(BlockPos pos, LockData data) {
	}

	private Locks() {
	}

	public static boolean isLockable(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof DoorBlock
			|| state.getBlock() instanceof TrapDoorBlock
			|| level.getBlockEntity(pos) instanceof Container;
	}

	public static List<BlockPos> group(BlockPos pos, BlockState state) {
		List<BlockPos> result = new ArrayList<>(2);
		result.add(pos.immutable());
		if (state.getBlock() instanceof DoorBlock) {
			result.add(state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos.above());
		} else if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
			result.add(pos.relative(ChestBlock.getConnectedDirection(state)));
		}
		return result;
	}

	public static BlockPos canonical(BlockPos pos, BlockState state) {
		BlockPos best = null;
		for (BlockPos candidate : group(pos, state)) {
			if (best == null || candidate.compareTo(best) < 0) {
				best = candidate;
			}
		}
		return best;
	}

	@Nullable
	public static Found find(ServerLevel level, BlockPos pos, BlockState state) {
		LockState locks = LockState.get(level);
		for (BlockPos candidate : group(pos, state)) {
			LockData data = locks.get(candidate);
			if (data != null) {
				BlockState there = level.getBlockState(candidate);
				if (!isLockable(level, candidate, there)) {
					remove(level, candidate);
					continue;
				}
				return new Found(candidate, data);
			}
		}
		return null;
	}

	public static void put(ServerLevel level, BlockPos pos, LockData data) {
		LockState.get(level).put(pos, data);
		broadcast(level, new LockUpdatePayload(pos, data.type().ordinal()));
	}

	@Nullable
	public static LockData remove(ServerLevel level, BlockPos pos) {
		LockData removed = LockState.get(level).remove(pos);
		if (removed != null) {
			broadcast(level, new LockUpdatePayload(pos, LockUpdatePayload.REMOVED));
		}
		return removed;
	}

	public static void markDirty(ServerLevel level) {
		LockState.get(level).markDirty();
	}

	public static boolean isOwner(Player player, LockData data) {
		return data.isOwner(player.getUUID());
	}

	public static void sendAll(ServerPlayer player) {
		List<LockSyncPayload.Entry> entries = new ArrayList<>();
		for (Map.Entry<BlockPos, LockData> entry : LockState.get(player.serverLevel()).all().entrySet()) {
			entries.add(new LockSyncPayload.Entry(entry.getKey(), entry.getValue().type()));
		}
		ServerPlayNetworking.send(player, new LockSyncPayload(entries));
	}

	public static void resync(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
		for (BlockPos p : group(pos, state)) {
			player.connection.send(new ClientboundBlockUpdatePacket(level, p));
		}
	}

	public static String hash(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void broadcast(ServerLevel level, LockUpdatePayload payload) {
		for (ServerPlayer player : level.players()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
}
