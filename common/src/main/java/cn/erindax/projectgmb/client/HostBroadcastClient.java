package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.network.HostBroadcastPayload;
import cn.erindax.projectgmb.network.Network;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class HostBroadcastClient {
	private static KeyMapping key;
	private static boolean lastWanted;
	private static boolean serverActive;

	private HostBroadcastClient() {
	}

	public static void register() {
		ClientEvents.keyMapping(HostBroadcastClient::createKey);
		Network.onClient(HostBroadcastPayload.TYPE, payload ->
			serverActive = payload.active()
		);
		ClientEvents.TICK.register(HostBroadcastClient::tick);
		ClientEvents.DISCONNECT.register(client -> {
			lastWanted = false;
			serverActive = false;
		});
		ClientEvents.HUD.register((graphics, tickDelta) -> {
			if (!serverActive || !holdingKey()) {
				return;
			}
			Minecraft client = Minecraft.getInstance();
			Component text = Component.translatable("hud.projectgm_b.host_broadcast");
			int x = (client.getWindow().getGuiScaledWidth() - client.font.width(text)) / 2;
			graphics.drawString(client.font, text, x, 12, 0xFFFFFF55, true);
		});
	}

	public static boolean shouldForceMic() {
		return serverActive && holdingKey();
	}

	private static KeyMapping createKey() {
		key = new KeyMapping(
			"key.projectgm_b.host_broadcast",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			"ProjectGM-b"
		);
		return key;
	}

	private static boolean holdingKey() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		return key != null
			&& key.isDown()
			&& player != null
			&& client.screen == null;
	}

	private static void tick(Minecraft client) {
		boolean wanted = holdingKey();
		if (wanted == lastWanted) {
			return;
		}
		lastWanted = wanted;
		if (!wanted) {
			serverActive = false;
		}
		if (client.getConnection() == null) {
			return;
		}
		Network.sendToServer(new HostBroadcastPayload(wanted));
	}
}
