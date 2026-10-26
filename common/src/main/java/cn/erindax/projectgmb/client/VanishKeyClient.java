package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.network.Network;
import cn.erindax.projectgmb.network.VanishTogglePayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class VanishKeyClient {
	private static KeyMapping key;

	private VanishKeyClient() {
	}

	public static void register() {
		ClientEvents.keyMapping(VanishKeyClient::createKey);
		ClientEvents.TICK.register(VanishKeyClient::tick);
		ClientEvents.DISCONNECT.register(client -> VanishClient.clear());
		ClientEvents.HUD.register((graphics, tickDelta) -> {
			Minecraft client = Minecraft.getInstance();
			LocalPlayer player = client.player;
			if (player == null || !VanishClient.isVanished(player.getUUID())) {
				return;
			}
			Component text = Component.translatable("hud.projectgm_b.vanish");
			int x = (client.getWindow().getGuiScaledWidth() - client.font.width(text)) / 2;
			graphics.drawString(client.font, text, x, 24, 0xFFAAAAAA, true);
		});
	}

	private static KeyMapping createKey() {
		key = new KeyMapping(
			"key.projectgm_b.vanish",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_I,
			"ProjectGM-b"
		);
		return key;
	}

	private static void tick(Minecraft client) {
		if (key == null || !key.consumeClick()) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.screen != null || client.getConnection() == null) {
			return;
		}
		Network.sendToServer(new VanishTogglePayload(!VanishClient.isVanished(player.getUUID())));
	}
}
