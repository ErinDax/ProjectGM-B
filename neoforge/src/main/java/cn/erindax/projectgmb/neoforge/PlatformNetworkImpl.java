package cn.erindax.projectgmb.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PlatformNetworkImpl {
	private PlatformNetworkImpl() {
	}

	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		if (player.connection.hasChannel(payload.type())) {
			PacketDistributor.sendToPlayer(player, payload);
		}
	}

	public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
		return player.connection.hasChannel(type);
	}

	public static void sendToServer(CustomPacketPayload payload) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		if (connection != null && connection.hasChannel(payload.type())) {
			PacketDistributor.sendToServer(payload);
		}
	}
}
