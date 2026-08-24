package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.network.HostBroadcastPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
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
		key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.projectgm_b.host_broadcast",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_B,
			"ProjectGM-b"
		));
		ClientPlayNetworking.registerGlobalReceiver(HostBroadcastPayload.TYPE, (payload, context) ->
			serverActive = payload.active()
		);
		ClientTickEvents.END_CLIENT_TICK.register(HostBroadcastClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			lastWanted = false;
			serverActive = false;
		});
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
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
		ClientPlayNetworking.send(new HostBroadcastPayload(wanted));
	}
}
