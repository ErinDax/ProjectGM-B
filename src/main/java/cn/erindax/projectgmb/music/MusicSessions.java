package cn.erindax.projectgmb.music;

import cn.erindax.projectgmb.music.net.MusicControlPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MusicSessions {

	private static final double NOTIFY_MARGIN = 16.0;
	private static final double LEAVE_MARGIN = 16.0;
	private static final int SYNC_INTERVAL_TICKS = 10;
	private static final long NANOS_PER_MILLI = 1_000_000L;

	public static final class Session {
		private final UUID id;
		private final ResourceKey<Level> dimension;
		private final String track;
		private final int version;
		private final int range;
		@Nullable
		private final UUID player;
		@Nullable
		private final BlockPos pos;
		private final Set<UUID> listeners = new HashSet<>();
		private final long startedAt = System.nanoTime();
		private long pausedAt;
		private long pausedTotal;
		private boolean paused;

		private Session(UUID id, ResourceKey<Level> dimension, String track, int version, int range,
				@Nullable UUID player, @Nullable BlockPos pos) {
			this.id = id;
			this.dimension = dimension;
			this.track = track;
			this.version = version;
			this.range = range;
			this.player = player;
			this.pos = pos;
		}

		public UUID id() {
			return id;
		}

		public String track() {
			return track;
		}

		public boolean paused() {
			return paused;
		}

		private int elapsedMillis() {
			long end = paused ? pausedAt : System.nanoTime();
			long millis = Math.max(0L, end - startedAt - pausedTotal) / NANOS_PER_MILLI;
			return (int) Math.min(Integer.MAX_VALUE, millis);
		}
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final Map<UUID, UUID> BY_PLAYER = new HashMap<>();
	private static final Map<GlobalPos, UUID> BY_BLOCK = new HashMap<>();
	private static int ticks;

	private MusicSessions() {
	}

	@Nullable
	public static Session forPlayer(UUID playerId) {
		UUID id = BY_PLAYER.get(playerId);
		return id == null ? null : SESSIONS.get(id);
	}

	public static boolean playForPlayer(ServerPlayer player, String track, int range) {
		MusicStore.Entry entry = MusicStore.load(track);
		if (entry == null) {
			return false;
		}
		stopForPlayer(player.server, player.getUUID());
		Session session = new Session(UUID.randomUUID(), player.serverLevel().dimension(), track, entry.version(),
			range, player.getUUID(), null);
		SESSIONS.put(session.id, session);
		BY_PLAYER.put(player.getUUID(), session.id);
		sync(player.server, session);
		return true;
	}

	public static boolean playForBlock(ServerLevel level, BlockPos pos, String track, int range) {
		MusicStore.Entry entry = MusicStore.load(track);
		if (entry == null) {
			return false;
		}
		stopForBlock(level, pos);
		Session session = new Session(UUID.randomUUID(), level.dimension(), track, entry.version(), range, null,
			pos.immutable());
		SESSIONS.put(session.id, session);
		BY_BLOCK.put(GlobalPos.of(level.dimension(), pos.immutable()), session.id);
		sync(level.getServer(), session);
		return true;
	}

	public static void pausePlayer(ServerPlayer player) {
		Session session = forPlayer(player.getUUID());
		if (session != null && !session.paused) {
			session.paused = true;
			session.pausedAt = System.nanoTime();
			sendToListeners(player.server, session,
				MusicControlPayload.simple(session.id, MusicControlPayload.Action.PAUSE));
		}
	}

	public static void resumePlayer(ServerPlayer player) {
		Session session = forPlayer(player.getUUID());
		if (session != null && session.paused) {
			session.paused = false;
			session.pausedTotal += System.nanoTime() - session.pausedAt;
			sendToListeners(player.server, session,
				MusicControlPayload.simple(session.id, MusicControlPayload.Action.RESUME));
			sync(player.server, session);
		}
	}

	public static void stopForPlayer(MinecraftServer server, UUID playerId) {
		UUID id = BY_PLAYER.remove(playerId);
		if (id != null) {
			stop(server, id);
		}
	}

	public static void stopForBlock(ServerLevel level, BlockPos pos) {
		UUID id = BY_BLOCK.remove(GlobalPos.of(level.dimension(), pos.immutable()));
		if (id != null) {
			stop(level.getServer(), id);
		}
	}

	public static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty() || ++ticks % SYNC_INTERVAL_TICKS != 0) {
			return;
		}
		for (Session session : List.copyOf(SESSIONS.values())) {
			if (!session.paused) {
				sync(server, session);
			}
		}
	}

	public static void clear() {
		SESSIONS.clear();
		BY_PLAYER.clear();
		BY_BLOCK.clear();
	}

	private static void stop(MinecraftServer server, UUID sessionId) {
		Session session = SESSIONS.remove(sessionId);
		if (session != null) {
			sendToListeners(server, session, MusicControlPayload.simple(session.id, MusicControlPayload.Action.STOP));
			session.listeners.clear();
		}
	}

	private static void sync(MinecraftServer server, Session session) {
		ServerLevel level = server.getLevel(session.dimension);
		if (level == null) {
			return;
		}
		Vec3 origin;
		BlockPos pos;
		int entityId;
		if (session.player != null) {
			ServerPlayer holder = server.getPlayerList().getPlayer(session.player);
			if (holder == null || holder.serverLevel() != level) {
				return;
			}
			origin = holder.position();
			pos = holder.blockPosition();
			entityId = holder.getId();
		} else if (session.pos != null) {
			origin = session.pos.getCenter();
			pos = session.pos;
			entityId = MusicControlPayload.NO_ENTITY;
		} else {
			return;
		}
		double reach = session.range + NOTIFY_MARGIN;
		double leave = reach + LEAVE_MARGIN;
		Iterator<UUID> it = session.listeners.iterator();
		while (it.hasNext()) {
			ServerPlayer listener = server.getPlayerList().getPlayer(it.next());
			if (listener == null) {
				it.remove();
			} else if (listener.serverLevel() != level || listener.position().distanceToSqr(origin) > leave * leave) {
				ServerPlayNetworking.send(listener,
					MusicControlPayload.simple(session.id, MusicControlPayload.Action.STOP));
				it.remove();
			}
		}
		MusicControlPayload play = null;
		for (ServerPlayer target : level.players()) {
			if (session.listeners.contains(target.getUUID())
					|| target.position().distanceToSqr(origin) > reach * reach
					|| !ServerPlayNetworking.canSend(target, MusicControlPayload.TYPE)) {
				continue;
			}
			if (play == null) {
				play = new MusicControlPayload(session.id, MusicControlPayload.Action.PLAY, session.track,
					session.version, session.range, entityId, pos, session.elapsedMillis());
			}
			ServerPlayNetworking.send(target, play);
			session.listeners.add(target.getUUID());
		}
	}

	private static void sendToListeners(MinecraftServer server, Session session, MusicControlPayload payload) {
		for (UUID id : session.listeners) {
			ServerPlayer listener = server.getPlayerList().getPlayer(id);
			if (listener != null) {
				ServerPlayNetworking.send(listener, payload);
			}
		}
	}
}
