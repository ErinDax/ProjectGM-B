package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.platform.ModEvents;
import cn.erindax.projectgmb.vanish.OpVanish;
import cn.erindax.projectgmb.voice.HostBroadcast;

public final class ModNetwork {
	private ModNetwork() {
	}

	public static void register() {
		Network.clientbound(HostBroadcastPayload.TYPE, HostBroadcastPayload.CODEC);
		Network.clientbound(VanishSyncPayload.TYPE, VanishSyncPayload.CODEC);
		Network.clientbound(SyncDoorLocksPayload.TYPE, SyncDoorLocksPayload.CODEC);
		Network.serverbound(HostBroadcastPayload.TYPE, HostBroadcastPayload.CODEC, (payload, player) -> {
			if (payload.active() && !player.hasPermissions(2)) {
				HostBroadcast.remove(player.getUUID());
				Network.send(player, new HostBroadcastPayload(false));
				return;
			}
			HostBroadcast.setActive(player.getUUID(), payload.active());
			Network.send(player, new HostBroadcastPayload(HostBroadcast.isActive(player.getUUID())));
		});
		Network.serverbound(VanishTogglePayload.TYPE, VanishTogglePayload.CODEC, (payload, player) ->
			OpVanish.setVanished(player, payload.active())
		);
		ModEvents.PLAYER_JOIN.register(OpVanish::sendTo);
		ModEvents.PLAYER_LEAVE.register(player -> {
			HostBroadcast.remove(player.getUUID());
			OpVanish.remove(player.server, player.getUUID());
		});
	}
}
