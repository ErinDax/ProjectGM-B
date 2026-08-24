package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.lock.DoorLockClient;
import cn.erindax.projectgmb.network.SyncDoorLocksPayload;
import cn.erindax.projectgmb.network.VanishSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashSet;

public class ProjectGmBClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SyncDoorLocksPayload.TYPE, (payload, context) ->
			DoorLockClient.replace(payload.toMap())
		);
		ClientPlayNetworking.registerGlobalReceiver(VanishSyncPayload.TYPE, (payload, context) ->
			VanishClient.replace(new HashSet<>(payload.vanished()))
		);
		HostBroadcastClient.register();
		VanishKeyClient.register();
	}
}
