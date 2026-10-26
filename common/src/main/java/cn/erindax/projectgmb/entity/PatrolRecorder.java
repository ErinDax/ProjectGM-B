package cn.erindax.projectgmb.entity;

import cn.erindax.projectgmb.platform.ModEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PatrolRecorder {

	public static final class Session {
		private final String name;
		private final List<BlockPos> points = new ArrayList<>();
		private boolean sneaking;

		private Session(String name) {
			this.name = name;
		}

		public String name() {
			return name;
		}

		public List<BlockPos> points() {
			return points;
		}
	}

	private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

	private PatrolRecorder() {
	}

	public static void init() {
		ModEvents.SERVER_TICK.register(PatrolRecorder::tick);
		ModEvents.PLAYER_LEAVE.register(player -> SESSIONS.remove(player.getUUID()));
	}

	@Nullable
	public static Session current(ServerPlayer player) {
		return SESSIONS.get(player.getUUID());
	}

	public static void start(ServerPlayer player, String name) {
		SESSIONS.put(player.getUUID(), new Session(name));
		player.sendSystemMessage(Component.translatable("commands.projectgm_b.patrol.record_started", name));
	}

	@Nullable
	public static Session stop(ServerPlayer player) {
		return SESSIONS.remove(player.getUUID());
	}

	private static void tick(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Session> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			Session session = entry.getValue();
			boolean sneaking = player.isShiftKeyDown();
			if (sneaking && !session.sneaking) {
				record(player, session);
			}
			session.sneaking = sneaking;
		}
	}

	private static void record(ServerPlayer player, Session session) {
		BlockPos pos = BlockPos.containing(player.position());
		List<BlockPos> points = session.points;
		if (!points.isEmpty() && points.get(points.size() - 1).equals(pos)) {
			return;
		}
		points.add(pos);
		player.displayClientMessage(Component.translatable("commands.projectgm_b.patrol.record_point",
			points.size(), pos.getX(), pos.getY(), pos.getZ()), true);
		player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.5F);
	}
}
