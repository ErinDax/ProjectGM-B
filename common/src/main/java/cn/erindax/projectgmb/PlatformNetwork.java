package cn.erindax.projectgmb;

import cn.erindax.projectgmb.network.codec.CustomPacketPayload;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformNetwork {
	private PlatformNetwork() {
	}

	@ExpectPlatform
	public static void send(ServerPlayer player, CustomPacketPayload payload) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static boolean canSend(ServerPlayer player, CustomPacketPayload.Type<?> type) {
		throw new AssertionError();
	}

	@ExpectPlatform
	public static void sendToServer(CustomPacketPayload payload) {
		throw new AssertionError();
	}
}
