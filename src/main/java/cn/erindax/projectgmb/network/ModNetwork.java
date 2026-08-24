package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.vanish.OpVanish;
import cn.erindax.projectgmb.voice.HostBroadcast;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetwork {
	private ModNetwork() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(HostBroadcastPayload.TYPE, HostBroadcastPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(HostBroadcastPayload.TYPE, HostBroadcastPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(VanishTogglePayload.TYPE, VanishTogglePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(VanishSyncPayload.TYPE, VanishSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SyncDoorLocksPayload.TYPE, SyncDoorLocksPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(HostBroadcastPayload.TYPE, (payload, context) -> {
			if (payload.active() && !context.player().hasPermissions(2)) {
				HostBroadcast.remove(context.player().getUUID());
				ServerPlayNetworking.send(context.player(), new HostBroadcastPayload(false));
				return;
			}
			HostBroadcast.setActive(context.player().getUUID(), payload.active());
			ServerPlayNetworking.send(context.player(), new HostBroadcastPayload(HostBroadcast.isActive(context.player().getUUID())));
		});
		ServerPlayNetworking.registerGlobalReceiver(VanishTogglePayload.TYPE, (payload, context) ->
			OpVanish.setVanished(context.player(), payload.active())
		);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
			OpVanish.sendTo(handler.getPlayer())
		);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			HostBroadcast.remove(handler.getPlayer().getUUID());
			OpVanish.remove(server, handler.getPlayer().getUUID());
		});
	}
}
