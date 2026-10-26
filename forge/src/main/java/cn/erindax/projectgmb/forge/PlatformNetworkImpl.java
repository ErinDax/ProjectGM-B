package cn.erindax.projectgmb.forge;

import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class PlatformNetworkImpl {
	private static final String PROTOCOL = "1";
	private static final Map<ResourceLocation, EventNetworkChannel> CHANNELS = new LinkedHashMap<>();

	private PlatformNetworkImpl() {
	}

	public static void register() {
		Map<ResourceLocation, Network.Serverbound<?>> serverbound = new LinkedHashMap<>();
		for (Network.Serverbound<?> entry : Network.serverbound()) {
			serverbound.put(entry.type().id(), entry);
		}
		Map<ResourceLocation, Network.Clientbound<?>> clientbound = new LinkedHashMap<>();
		for (Network.Clientbound<?> entry : Network.clientbound()) {
			clientbound.put(entry.type().id(), entry);
		}
		Set<ResourceLocation> ids = new LinkedHashSet<>(serverbound.keySet());
		ids.addAll(clientbound.keySet());
		for (ResourceLocation id : ids) {
			EventNetworkChannel channel = NetworkRegistry.newEventChannel(id, () -> PROTOCOL,
				NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));
			Network.Serverbound<?> toServer = serverbound.get(id);
			Network.Clientbound<?> toClient = clientbound.get(id);
			channel.addListener((NetworkEvent.ServerCustomPayloadEvent event) -> receive(event, toServer, toClient));
			CHANNELS.put(id, channel);
		}
	}

	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		ResourceLocation id = payload.type().id();
		if (isPresent(id, player)) {
			player.connection.send(new ClientboundCustomPayloadPacket(id, Network.encode(payload)));
		}
	}

	public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
		return isPresent(type.id(), player);
	}

	public static void sendToServer(CustomPacketPayload payload) {
		ResourceLocation id = payload.type().id();
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		EventNetworkChannel channel = CHANNELS.get(id);
		if (connection != null && channel != null && channel.isRemotePresent(connection.getConnection())) {
			connection.send(new ServerboundCustomPayloadPacket(id, Network.encode(payload)));
		}
	}

	private static boolean isPresent(ResourceLocation id, ServerPlayer player) {
		EventNetworkChannel channel = CHANNELS.get(id);
		return channel != null && channel.isRemotePresent(player.connection.connection);
	}

	private static void receive(NetworkEvent.ServerCustomPayloadEvent event, @Nullable Network.Serverbound<?> toServer,
			@Nullable Network.Clientbound<?> toClient) {
		NetworkEvent.Context context = event.getSource().get();
		FriendlyByteBuf buffer = event.getPayload();
		if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER && toServer != null) {
			ServerPlayer sender = context.getSender();
			if (sender != null) {
				receiveOnServer(toServer, buffer, sender, context);
			}
		} else if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT && toClient != null) {
			receiveOnClient(toClient, buffer, context);
		}
		context.setPacketHandled(true);
	}

	private static <T extends CustomPacketPayload> void receiveOnServer(Network.Serverbound<T> entry,
			FriendlyByteBuf buffer, ServerPlayer sender, NetworkEvent.Context context) {
		T payload = entry.codec().decode(buffer);
		context.enqueueWork(() -> entry.handler().accept(payload, sender));
	}

	private static <T extends CustomPacketPayload> void receiveOnClient(Network.Clientbound<T> entry,
			FriendlyByteBuf buffer, NetworkEvent.Context context) {
		T payload = entry.codec().decode(buffer);
		context.enqueueWork(() -> Network.handleClient(payload));
	}
}
