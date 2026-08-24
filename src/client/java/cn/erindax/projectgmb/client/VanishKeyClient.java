package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.network.VanishTogglePayload;
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

public final class VanishKeyClient {
	private static KeyMapping key;

	private VanishKeyClient() {
	}

	public static void register() {
		key = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.projectgm_b.vanish",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_I,
			"ProjectGM-b"
		));
		ClientTickEvents.END_CLIENT_TICK.register(VanishKeyClient::tick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VanishClient.clear());
		HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
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

	private static void tick(Minecraft client) {
		if (key == null || !key.consumeClick()) {
			return;
		}
		LocalPlayer player = client.player;
		if (player == null || client.screen != null || client.getConnection() == null) {
			return;
		}
		ClientPlayNetworking.send(new VanishTogglePayload(!VanishClient.isVanished(player.getUUID())));
	}
}
