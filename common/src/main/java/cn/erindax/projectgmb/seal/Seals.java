package cn.erindax.projectgmb.seal;

import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.lock.Locks;
import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.platform.ModEvents;
import cn.erindax.projectgmb.seal.net.SealSelectionPayload;
import cn.erindax.projectgmb.seal.net.SealSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Seals {

	private static final int USE_COOLDOWN_TICKS = 5;

	private static final Map<UUID, GlobalPos> FIRST_CORNERS = new HashMap<>();
	private static final Map<UUID, Integer> LAST_USE = new HashMap<>();

	private static volatile List<BoundingBox> clientBoxes = List.of();

	private Seals() {
	}

	public static void init() {
		Network.clientbound(SealSyncPayload.TYPE, SealSyncPayload.STREAM_CODEC);
		Network.clientbound(SealSelectionPayload.TYPE, SealSelectionPayload.STREAM_CODEC);

		ModEvents.USE_BLOCK.register(Seals::onUseBlock);

		ModEvents.PLAYER_JOIN.register(Seals::sendAll);
		ModEvents.PLAYER_CHANGED_DIMENSION.register(player -> {
			clearSelection(player);
			sendAll(player);
		});
		ModEvents.PLAYER_RESPAWN.register(player -> {
			clearSelection(player);
			sendAll(player);
		});
		ModEvents.PLAYER_LEAVE.register(player -> {
			FIRST_CORNERS.remove(player.getUUID());
			LAST_USE.remove(player.getUUID());
		});
		ModEvents.SERVER_STOPPED.register(server -> {
			FIRST_CORNERS.clear();
			LAST_USE.clear();
		});
	}

	public static boolean blocks(@Nullable Player player, Level level, BlockPos pos, BlockState state) {
		if (player == null) {
			return false;
		}
		List<BoundingBox> boxes = level instanceof ServerLevel serverLevel ? SealState.get(serverLevel).boxes() : clientBoxes;
		if (boxes.isEmpty()) {
			return false;
		}
		for (BlockPos part : Locks.group(pos, state)) {
			if (contains(boxes, part)) {
				return true;
			}
		}
		return false;
	}

	public static boolean blocksEntity(Player player, Entity entity) {
		if (player.isSpectator()) {
			return false;
		}
		List<BoundingBox> boxes = player.level() instanceof ServerLevel serverLevel
			? SealState.get(serverLevel).boxes() : clientBoxes;
		if (boxes.isEmpty()) {
			return false;
		}
		if (contains(boxes, entity.blockPosition())) {
			return true;
		}
		return entity instanceof HangingEntity hanging
			&& contains(boxes, hanging.getPos().relative(hanging.getDirection().getOpposite()));
	}

	public static void notifyEntityBlocked(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.displayClientMessage(Component.translatable("item.projectgm_b.sealer.blocked_entity"), true);
		}
	}

	public static void notifyBlocked(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.displayClientMessage(Component.translatable("item.projectgm_b.sealer.blocked"), true);
		}
	}

	public static boolean contains(List<BoundingBox> boxes, BlockPos pos) {
		for (BoundingBox box : boxes) {
			if (box.isInside(pos)) {
				return true;
			}
		}
		return false;
	}

	public static boolean same(BoundingBox a, BoundingBox b) {
		return a.minX() == b.minX() && a.minY() == b.minY() && a.minZ() == b.minZ()
			&& a.maxX() == b.maxX() && a.maxY() == b.maxY() && a.maxZ() == b.maxZ();
	}

	public static List<BoundingBox> clientView() {
		return clientBoxes;
	}

	public static void setClientView(List<BoundingBox> boxes) {
		clientBoxes = List.copyOf(boxes);
	}

	public static void sendAll(ServerPlayer player) {
		Network.send(player, new SealSyncPayload(List.copyOf(SealState.get(player.serverLevel()).boxes())));
	}

	public static void cancelSelection(ServerPlayer player) {
		if (FIRST_CORNERS.containsKey(player.getUUID())) {
			clearSelection(player);
			player.displayClientMessage(Component.translatable("item.projectgm_b.sealer.cancelled"), true);
		}
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(ModItems.SEALER) && !player.isSpectator()) {
			if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
					&& !throttled(serverPlayer)) {
				BlockPos pos = hit.getBlockPos().immutable();
				if (player.isSecondaryUseActive()) {
					toggle(serverPlayer, serverLevel, pos);
				} else {
					select(serverPlayer, serverLevel, pos);
				}
			}
			return InteractionResult.SUCCESS;
		}
		if (!player.isSpectator() || !blocks(player, level, hit.getBlockPos(), level.getBlockState(hit.getBlockPos()))) {
			return InteractionResult.PASS;
		}
		notifyBlocked(player);
		return InteractionResult.FAIL;
	}

	private static boolean throttled(ServerPlayer player) {
		int now = player.server.getTickCount();
		Integer last = LAST_USE.put(player.getUUID(), now);
		return last != null && now - last < USE_COOLDOWN_TICKS;
	}

	private static void select(ServerPlayer player, ServerLevel level, BlockPos pos) {
		GlobalPos first = FIRST_CORNERS.get(player.getUUID());
		if (first == null || !first.dimension().equals(level.dimension())) {
			FIRST_CORNERS.put(player.getUUID(), GlobalPos.of(level.dimension(), pos));
			Network.send(player, new SealSelectionPayload(true, pos));
			player.displayClientMessage(Component.translatable("item.projectgm_b.sealer.first",
				pos.getX(), pos.getY(), pos.getZ()), true);
			return;
		}
		clearSelection(player);
		add(player, level, BoundingBox.fromCorners(first.pos(), pos));
	}

	private static void toggle(ServerPlayer player, ServerLevel level, BlockPos pos) {
		int removed = SealState.get(level).removeContaining(pos);
		if (removed > 0) {
			broadcast(level);
			player.displayClientMessage(Component.translatable("item.projectgm_b.sealer.removed", removed), true);
			return;
		}
		add(player, level, BoundingBox.fromCorners(pos, pos));
	}

	private static void add(ServerPlayer player, ServerLevel level, BoundingBox box) {
		if (!SealState.get(level).add(box)) {
			player.displayClientMessage(Component.translatable("item.projectgm_b.sealer.exists"), true);
			return;
		}
		broadcast(level);
		player.displayClientMessage(Component.translatable("item.projectgm_b.sealer.added",
			box.getXSpan(), box.getYSpan(), box.getZSpan()), true);
	}

	private static void clearSelection(ServerPlayer player) {
		if (FIRST_CORNERS.remove(player.getUUID()) != null) {
			Network.send(player, SealSelectionPayload.cleared());
		}
	}

	private static void broadcast(ServerLevel level) {
		SealSyncPayload payload = new SealSyncPayload(List.copyOf(SealState.get(level).boxes()));
		for (ServerPlayer player : level.players()) {
			Network.send(player, payload);
		}
	}
}
