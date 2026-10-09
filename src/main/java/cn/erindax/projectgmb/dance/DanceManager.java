package cn.erindax.projectgmb.dance;

import cn.erindax.projectgmb.block.ModBlocks;
import cn.erindax.projectgmb.dance.net.DanceBoardPayload;
import cn.erindax.projectgmb.dance.net.DanceOpenPayload;
import cn.erindax.projectgmb.dance.net.DanceProgressPayload;
import cn.erindax.projectgmb.dance.net.DanceSignalPayload;
import cn.erindax.projectgmb.music.MusicStore;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class DanceManager {

	public enum Start {
		OK, DISABLED, BUSY, NO_AUDIO, NOBODY
	}

	public record StartResult(Start status, int players) {
	}

	private static final int READY_TIMEOUT_TICKS = 600;
	private static final int BOARD_INTERVAL_TICKS = 10;
	private static final int COUNTDOWN_ALLOWANCE_MS = 5000;
	private static final int SYNTH_GRACE_MS = 30000;
	private static final int AUDIO_GRACE_MS = 300000;

	@Nullable
	private static Session active;
	private static int nextId = ThreadLocalRandom.current().nextInt(1, 1 << 20);

	private DanceManager() {
	}

	public static void init() {
		PayloadTypeRegistry.playS2C().register(DanceOpenPayload.TYPE, DanceOpenPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(DanceBoardPayload.TYPE, DanceBoardPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(DanceSignalPayload.TYPE, DanceSignalPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(DanceSignalPayload.TYPE, DanceSignalPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(DanceProgressPayload.TYPE, DanceProgressPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(DanceSignalPayload.TYPE,
			(payload, context) -> onSignal(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(DanceProgressPayload.TYPE,
			(payload, context) -> onProgress(context.player(), payload));

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onLeave(server, handler.player));
		ServerTickEvents.END_SERVER_TICK.register(DanceManager::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> DanceCharts.reload());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> active = null);
	}

	public static boolean enabled(MinecraftServer server) {
		return DanceState.get(server).enabled();
	}

	public static boolean setEnabled(MinecraftServer server, boolean enabled) {
		DanceState.get(server).setEnabled(enabled);
		return !enabled && stop(server);
	}

	public static StartResult start(MinecraftServer server, DanceChart chart) {
		if (!enabled(server)) {
			return new StartResult(Start.DISABLED, 0);
		}
		if (active != null) {
			return new StartResult(Start.BUSY, 0);
		}
		int version = 0;
		if (!chart.audio().isEmpty()) {
			MusicStore.Entry entry = MusicStore.load(chart.audio());
			if (entry == null) {
				return new StartResult(Start.NO_AUDIO, 0);
			}
			version = entry.version();
		}
		List<ServerPlayer> dancers = new ArrayList<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, DanceOpenPayload.TYPE) && isOnDanceBlock(player)) {
				dancers.add(player);
			}
		}
		if (dancers.isEmpty()) {
			return new StartResult(Start.NOBODY, 0);
		}
		Session session = new Session(nextId++, chart);
		for (ServerPlayer player : dancers) {
			session.players.put(player.getUUID(), new Participant(player.getUUID(), displayName(player)));
		}
		DanceOpenPayload payload = new DanceOpenPayload(session.id, chart, version, session.standings());
		for (ServerPlayer player : dancers) {
			ServerPlayNetworking.send(player, payload);
		}
		active = session;
		return new StartResult(Start.OK, dancers.size());
	}

	public static boolean stop(MinecraftServer server) {
		Session session = active;
		if (session == null) {
			return false;
		}
		if (session.started) {
			finish(server, session);
		} else {
			active = null;
			DanceSignalPayload close = new DanceSignalPayload(session.id, DanceSignalPayload.CLOSE);
			forOnline(server, session, player -> ServerPlayNetworking.send(player, close));
		}
		return true;
	}

	public static boolean isOnDanceBlock(ServerPlayer player) {
		if (player.isSpectator() || !player.isAlive()) {
			return false;
		}
		Level level = player.level();
		if (level.getBlockState(player.getOnPos()).is(ModBlocks.DANCE_BLOCK)) {
			return true;
		}
		BlockPos feet = BlockPos.containing(player.getX(), player.getY() + 0.2, player.getZ());
		for (int depth = 0; depth < 4; depth++) {
			BlockPos pos = feet.below(depth);
			BlockState state = level.getBlockState(pos);
			if (state.is(ModBlocks.DANCE_BLOCK)) {
				return true;
			}
			if (depth > 0 && !state.getCollisionShape(level, pos).isEmpty()) {
				return false;
			}
		}
		return false;
	}

	private static Component displayName(ServerPlayer player) {
		return Component.literal(player.getGameProfile().getName());
	}

	private static void onSignal(ServerPlayer player, DanceSignalPayload payload) {
		Session session = active;
		if (payload.signal() != DanceSignalPayload.READY || session == null || session.id != payload.session()) {
			return;
		}
		Participant participant = session.players.get(player.getUUID());
		if (participant == null) {
			return;
		}
		participant.ready = true;
		if (!session.started && session.allReady()) {
			go(player.server, session);
		}
	}

	private static void onProgress(ServerPlayer player, DanceProgressPayload payload) {
		Session session = active;
		if (session == null || session.id != payload.session()) {
			return;
		}
		Participant participant = session.players.get(player.getUUID());
		if (participant == null || participant.status != DanceStanding.PLAYING) {
			return;
		}
		participant.stats = sanitize(payload.stats(), session);
		if (payload.state() == DanceStanding.FINISHED || payload.state() == DanceStanding.QUIT) {
			participant.status = payload.state();
			participant.ready = true;
		}
		session.dirty = true;
		checkProgress(player.server, session);
	}

	private static void onLeave(MinecraftServer server, ServerPlayer player) {
		Session session = active;
		if (session == null) {
			return;
		}
		Participant participant = session.players.get(player.getUUID());
		if (participant == null) {
			return;
		}
		if (participant.status == DanceStanding.PLAYING) {
			participant.status = DanceStanding.OFFLINE;
		}
		participant.ready = true;
		session.dirty = true;
		if (!session.started) {
			if (session.allDone()) {
				active = null;
			} else if (session.allReady()) {
				go(server, session);
			}
			return;
		}
		checkProgress(server, session);
	}

	private static void tick(MinecraftServer server) {
		Session session = active;
		if (session == null) {
			return;
		}
		session.ticks++;
		if (!session.started) {
			if (session.ticks >= READY_TIMEOUT_TICKS) {
				go(server, session);
			}
			return;
		}
		if (session.dirty && session.ticks % BOARD_INTERVAL_TICKS == 0) {
			session.dirty = false;
			DanceBoardPayload board = new DanceBoardPayload(session.id, false, session.standings());
			forOnline(server, session, player -> ServerPlayNetworking.send(player, board));
		}
		if (session.ticks >= session.deadline) {
			finish(server, session);
		}
	}

	private static void go(MinecraftServer server, Session session) {
		session.started = true;
		session.ticks = 0;
		int grace = session.chart.audio().isEmpty() ? SYNTH_GRACE_MS : AUDIO_GRACE_MS;
		session.deadline = (session.chart.duration() + COUNTDOWN_ALLOWANCE_MS + grace) / 50;
		DanceSignalPayload go = new DanceSignalPayload(session.id, DanceSignalPayload.GO);
		forOnline(server, session, player -> ServerPlayNetworking.send(player, go));
	}

	private static void checkProgress(MinecraftServer server, Session session) {
		if (session.started && session.allDone()) {
			finish(server, session);
		}
	}

	private static void finish(MinecraftServer server, Session session) {
		if (active == session) {
			active = null;
		}
		List<DanceStanding> standings = session.standings();
		DanceBoardPayload result = new DanceBoardPayload(session.id, true, standings);
		forOnline(server, session, player -> ServerPlayNetworking.send(player, result));
		for (Participant participant : session.players.values()) {
			ServerPlayer player = server.getPlayerList().getPlayer(participant.id);
			if (player != null) {
				DanceStats stats = participant.stats;
				player.sendSystemMessage(Component.translatable("chat.projectgm_b.dance.stats", stats.sick(), stats.good(),
					stats.bad(), stats.shit(), stats.miss()));
			}
		}
	}

	private static DanceStats sanitize(DanceStats stats, Session session) {
		int notes = session.chart.notes().size();
		int sick = Mth.clamp(stats.sick(), 0, notes);
		int good = Mth.clamp(stats.good(), 0, notes - sick);
		int bad = Mth.clamp(stats.bad(), 0, notes - sick - good);
		int shit = Mth.clamp(stats.shit(), 0, notes - sick - good - bad);
		int hits = sick + good + bad + shit;
		int miss = Mth.clamp(stats.miss(), 0, notes - hits + session.holds);
		int maxCombo = Mth.clamp(stats.maxCombo(), 0, hits);
		int combo = Mth.clamp(stats.combo(), 0, maxCombo);
		int score = (int) Math.min(Math.max(stats.score(), 0), session.maxScore);
		return new DanceStats(score, combo, maxCombo, sick, good, bad, shit, miss);
	}

	private static void forOnline(MinecraftServer server, Session session, Consumer<ServerPlayer> action) {
		for (UUID id : session.players.keySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				action.accept(player);
			}
		}
	}

	private static final class Session {
		private final int id;
		private final DanceChart chart;
		private final long maxScore;
		private final int holds;
		private final Map<UUID, Participant> players = new LinkedHashMap<>();
		private boolean started;
		private boolean dirty;
		private int ticks;
		private int deadline;

		private Session(int id, DanceChart chart) {
			this.id = id;
			this.chart = chart;
			this.maxScore = DanceRules.maxScore(chart);
			this.holds = chart.holdCount();
		}

		private boolean allReady() {
			for (Participant participant : players.values()) {
				if (!participant.ready) {
					return false;
				}
			}
			return true;
		}

		private boolean allDone() {
			for (Participant participant : players.values()) {
				if (participant.status == DanceStanding.PLAYING) {
					return false;
				}
			}
			return true;
		}

		private List<DanceStanding> standings() {
			List<DanceStanding> list = new ArrayList<>();
			for (Participant participant : players.values()) {
				list.add(new DanceStanding(participant.id, participant.name, participant.stats, participant.status));
			}
			list.sort(DanceStanding.ORDER);
			return list;
		}
	}

	private static final class Participant {
		private final UUID id;
		private final Component name;
		private DanceStats stats = DanceStats.EMPTY;
		private int status = DanceStanding.PLAYING;
		private boolean ready;

		private Participant(UUID id, Component name) {
			this.id = id;
			this.name = name;
		}
	}
}
