package cn.erindax.projectgmb.fabric;

import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformNetworkImpl {
	private PlatformNetworkImpl() {
	}

	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		ServerPlayNetworking.send(player, payload.type().id(), Network.encode(payload));
	}

	public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
		return ServerPlayNetworking.canSend(player, type.id());
	}

	public static void sendToServer(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload.type().id(), Network.encode(payload));
	}
}
