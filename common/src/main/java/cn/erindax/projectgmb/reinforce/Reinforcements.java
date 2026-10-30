package cn.erindax.projectgmb.reinforce;

import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.platform.ModEvents;
import cn.erindax.projectgmb.reinforce.net.ReinforceSelectionPayload;
import cn.erindax.projectgmb.reinforce.net.ReinforceSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Reinforcements {

	private static final int USE_COOLDOWN_TICKS = 5;

	private static final Map<UUID, GlobalPos> FIRST_CORNERS = new HashMap<>();
	private static final Map<UUID, Integer> LAST_USE = new HashMap<>();

	private static volatile List<BoundingBox> clientBoxes = List.of();

	private Reinforcements() {
	}

	public static void init() {
		Network.clientbound(ReinforceSyncPayload.TYPE, ReinforceSyncPayload.STREAM_CODEC);
		Network.clientbound(ReinforceSelectionPayload.TYPE, ReinforceSelectionPayload.STREAM_CODEC);

		ModEvents.USE_BLOCK.register(Reinforcements::onUseBlock);
		ModEvents.BEFORE_BREAK.register(Reinforcements::onBeforeBreak);

		ModEvents.PLAYER_JOIN.register(Reinforcements::sendAll);
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

	public static boolean isReinforced(Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			return ReinforceState.get(serverLevel).contains(pos);
		}
		return contains(clientBoxes, pos);
	}

	public static void protect(ServerLevel level, List<BlockPos> positions) {
		ReinforceState state = ReinforceState.get(level);
		if (!state.isEmpty()) {
			positions.removeIf(state::contains);
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
		Network.send(player, new ReinforceSyncPayload(List.copyOf(ReinforceState.get(player.serverLevel()).boxes())));
	}

	public static void cancelSelection(ServerPlayer player) {
		if (FIRST_CORNERS.containsKey(player.getUUID())) {
			clearSelection(player);
			player.displayClientMessage(Component.translatable("item.projectgm_b.reinforcer.cancelled"), true);
		}
	}

	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (player.isSpectator() || !player.getItemInHand(hand).is(ModItems.REINFORCER)) {
			return InteractionResult.PASS;
		}
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

	private static boolean onBeforeBreak(Level level, Player player, BlockPos pos, BlockState state,
			@Nullable BlockEntity blockEntity) {
		if (player.isCreative() || !isReinforced(level, pos)) {
			return true;
		}
		player.displayClientMessage(Component.translatable("item.projectgm_b.reinforcer.cannot_break"), true);
		return false;
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
			Network.send(player, new ReinforceSelectionPayload(true, pos));
			player.displayClientMessage(Component.translatable("item.projectgm_b.reinforcer.first",
				pos.getX(), pos.getY(), pos.getZ()), true);
			return;
		}
		clearSelection(player);
		add(player, level, BoundingBox.fromCorners(first.pos(), pos));
	}

	private static void toggle(ServerPlayer player, ServerLevel level, BlockPos pos) {
		int removed = ReinforceState.get(level).removeContaining(pos);
		if (removed > 0) {
			broadcast(level);
			player.displayClientMessage(Component.translatable("item.projectgm_b.reinforcer.removed", removed), true);
			return;
		}
		add(player, level, BoundingBox.fromCorners(pos, pos));
	}

	private static void add(ServerPlayer player, ServerLevel level, BoundingBox box) {
		if (!ReinforceState.get(level).add(box)) {
			player.displayClientMessage(Component.translatable("item.projectgm_b.reinforcer.exists"), true);
			return;
		}
		broadcast(level);
		player.displayClientMessage(Component.translatable("item.projectgm_b.reinforcer.added",
			box.getXSpan(), box.getYSpan(), box.getZSpan()), true);
	}

	private static void clearSelection(ServerPlayer player) {
		if (FIRST_CORNERS.remove(player.getUUID()) != null) {
			Network.send(player, ReinforceSelectionPayload.cleared());
		}
	}

	private static void broadcast(ServerLevel level) {
		ReinforceSyncPayload payload = new ReinforceSyncPayload(List.copyOf(ReinforceState.get(level).boxes()));
		for (ServerPlayer player : level.players()) {
			Network.send(player, payload);
		}
	}
}
