package cn.erindax.projectgmb.vanish;

import cn.erindax.projectgmb.mixin.ChunkMapAccessor;
import cn.erindax.projectgmb.mixin.ChunkMapTrackedEntityAccessor;
import cn.erindax.projectgmb.network.VanishSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OpVanish {
	private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();

	private OpVanish() {
	}

	public static boolean isVanished(UUID playerId) {
		return VANISHED.contains(playerId);
	}

	public static boolean canVanish(ServerPlayer player) {
		return player.hasPermissions(2) && player.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
	}

	public static void setVanished(ServerPlayer player, boolean vanished) {
		boolean next = vanished && canVanish(player);
		boolean changed = next ? VANISHED.add(player.getUUID()) : VANISHED.remove(player.getUUID());
		if (changed) {
			refreshTracking(player);
			sync(player.server);
		} else {
			sendTo(player);
		}
	}

	public static void remove(MinecraftServer server, UUID playerId) {
		if (!VANISHED.remove(playerId)) {
			return;
		}
		ServerPlayer player = server.getPlayerList().getPlayer(playerId);
		if (player != null) {
			refreshTracking(player);
		}
		sync(server);
	}

	public static void tick(MinecraftServer server) {
		for (UUID id : Set.copyOf(VANISHED)) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null || !canVanish(player)) {
				remove(server, id);
			}
		}
	}

	public static void sendTo(ServerPlayer player) {
		ServerPlayNetworking.send(player, VanishSyncPayload.of(VANISHED));
	}

	public static void clear() {
		VANISHED.clear();
	}

	private static void sync(MinecraftServer server) {
		VanishSyncPayload payload = VanishSyncPayload.of(VANISHED);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private static void refreshTracking(ServerPlayer vanished) {
		Object tracked = ((ChunkMapAccessor) vanished.serverLevel().getChunkSource().chunkMap)
			.projectgmb$entityMap()
			.get(vanished.getId());
		if (tracked == null) {
			return;
		}
		ChunkMapTrackedEntityAccessor accessor = (ChunkMapTrackedEntityAccessor) tracked;
		boolean hidden = isVanished(vanished.getUUID());
		for (ServerPlayer other : vanished.serverLevel().players()) {
			if (other == vanished) {
				continue;
			}
			if (hidden) {
				accessor.projectgmb$removePlayer(other);
			} else {
				accessor.projectgmb$updatePlayer(other);
			}
		}
	}
}
