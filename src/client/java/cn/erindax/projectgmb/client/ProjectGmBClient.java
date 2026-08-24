package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.lock.DoorLockClient;
import cn.erindax.projectgmb.network.SyncDoorLocksPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ProjectGmBClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(SyncDoorLocksPayload.TYPE, (payload, context) ->
			DoorLockClient.replace(payload.toMap())
		);
		HostBroadcastClient.register();
	}
}
