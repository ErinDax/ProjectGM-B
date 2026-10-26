package cn.erindax.projectgmb.network;

import cn.erindax.projectgmb.PlatformNetwork;
import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import cn.erindax.projectgmb.network.codec.StreamCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
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
	private static final Map<CustomPacketPayload.Type<?>, StreamCodec<? super FriendlyByteBuf, ?>> CODECS =
		new ConcurrentHashMap<>();

	private Network() {
	}

	public static <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super FriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
		SERVERBOUND.add(new Serverbound<>(type, codec, handler));
		CODECS.put(type, codec);
	}

	public static <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
			StreamCodec<? super FriendlyByteBuf, T> codec) {
		CLIENTBOUND.add(new Clientbound<>(type, codec));
		CODECS.put(type, codec);
	}

	@SuppressWarnings("unchecked")
	public static FriendlyByteBuf encode(CustomPacketPayload payload) {
		StreamCodec<? super FriendlyByteBuf, CustomPacketPayload> codec =
			(StreamCodec<? super FriendlyByteBuf, CustomPacketPayload>) CODECS.get(payload.type());
		if (codec == null) {
			throw new IllegalArgumentException("Unregistered payload " + payload.type().id());
		}
		FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
		codec.encode(buffer, payload);
		return buffer;
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
			StreamCodec<? super FriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
	}

	public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
			StreamCodec<? super FriendlyByteBuf, T> codec) {
	}
}
