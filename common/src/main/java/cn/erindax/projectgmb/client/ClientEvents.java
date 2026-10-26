package cn.erindax.projectgmb.client;

import cn.erindax.projectgmb.platform.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ClientEvents {
	public static final Event<Consumer<Minecraft>> TICK = new Event<>();
	public static final Event<Consumer<Minecraft>> DISCONNECT = new Event<>();
	public static final Event<HudRender> HUD = new Event<>();
	public static final Event<LevelRender> AFTER_ENTITIES = new Event<>();

	private static final List<Supplier<KeyMapping>> KEY_MAPPINGS = new CopyOnWriteArrayList<>();

	private ClientEvents() {
	}

	public static void keyMapping(Supplier<KeyMapping> factory) {
		KEY_MAPPINGS.add(factory);
	}

	public static List<Supplier<KeyMapping>> keyMappings() {
		return Collections.unmodifiableList(KEY_MAPPINGS);
	}

	public static void renderHud(GuiGraphics graphics, float partialTick) {
		for (HudRender listener : HUD.listeners()) {
			listener.render(graphics, partialTick);
		}
	}

	public static void renderAfterEntities(PoseStack poseStack, MultiBufferSource buffers, Camera camera,
			ClientLevel level) {
		for (LevelRender listener : AFTER_ENTITIES.listeners()) {
			listener.render(poseStack, buffers, camera, level);
		}
	}

	@FunctionalInterface
	public interface HudRender {
		void render(GuiGraphics graphics, float partialTick);
	}

	@FunctionalInterface
	public interface LevelRender {
		void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, ClientLevel level);
	}
}
