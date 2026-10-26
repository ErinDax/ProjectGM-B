package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.PlatformNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Network {
	private static final List<Serverbound<?>> SERVERBOUND = new CopyOnWriteArrayList<>();
	private static final List<Clientbound<?>> CLIENTBOUND = new CopyOnWriteArrayList<>();
	private static final Map<CustomPacketPayload.Type<?>, Consumer<?>> CLIENT_HANDLERS = new ConcurrentHashMap<>();

	private Network() {
	}

	public static <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
		SERVERBOUND.add(new Serverbound<>(type, codec, handler));
	}

	public static <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
		CLIENTBOUND.add(new Clientbound<>(type, codec));
	}

	public static <T extends CustomPacketPayload> void onClient(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
		CLIENT_HANDLERS.put(type, handler);
	}

	public static List<Serverbound<?>> serverbound() {
		return Collections.unmodifiableList(SERVERBOUND);
	}

	public static List<Clientbound<?>> clientbound() {
		return Collections.unmodifiableList(CLIENTBOUND);
	}

	@SuppressWarnings("unchecked")
	public static <T extends CustomPacketPayload> void handleClient(T payload) {
		Consumer<T> handler = (Consumer<T>) CLIENT_HANDLERS.get(payload.type());
		if (handler != null) {
			handler.accept(payload);
		}
	}

	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		PlatformNetwork.send(player, payload);
	}

	public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
		return PlatformNetwork.canSend(player, type);
	}

	public static void sendToServer(CustomPacketPayload payload) {
		PlatformNetwork.sendToServer(payload);
	}

	public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
	}

	public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
			StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
	}
}
