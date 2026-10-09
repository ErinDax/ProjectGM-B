package cn.erindax.projectgmb.client.gui;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.client.render.RemoteTextures;
import cn.erindax.projectgmb.lock.net.KeySkinListPayload;
import cn.erindax.projectgmb.lock.net.KeySkinSelectPayload;
import cn.erindax.projectgmb.skin.TextureStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class KeySkinScreen extends Screen {

	private static final ResourceLocation DEFAULT = ProjectGmB.id("textures/item/key.png");
	private static final int CELL = 40;
	private static final int ICON = 32;
	private static final int GAP = 6;
	private static final int COLUMNS = 6;
	private static final int TOP = 40;

	private final boolean mainHand;
	private final String current;
	private final List<String> skins = new ArrayList<>();
	private int gridLeft;

	public KeySkinScreen(KeySkinListPayload data) {
		super(Component.translatable("screen.projectgm_b.key_skin.title"));
		this.mainHand = data.mainHand();
		this.current = data.current();
		skins.add("");
		skins.addAll(data.skins());
	}

	@Override
	protected void init() {
		int columns = Math.min(COLUMNS, skins.size());
		gridLeft = (width - columns * (CELL + GAP) + GAP) / 2;
		for (int i = 0; i < skins.size(); i++) {
			String skin = skins.get(i);
			int col = i % COLUMNS;
			int row = i / COLUMNS;
			int x = gridLeft + col * (CELL + GAP);
			int y = TOP + row * (CELL + GAP);
			addRenderableWidget(Button.builder(Component.empty(), b -> select(skin))
				.bounds(x, y, CELL, CELL)
				.tooltip(net.minecraft.client.gui.components.Tooltip.create(skin.isEmpty()
					? Component.translatable("screen.projectgm_b.key_skin.default") : Component.literal(skin)))
				.build());
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
			.bounds((width - 100) / 2, height - 32, 100, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, 14, 0xFFFFFF);
		for (int i = 0; i < skins.size(); i++) {
			String skin = skins.get(i);
			int col = i % COLUMNS;
			int row = i / COLUMNS;
			int x = gridLeft + col * (CELL + GAP) + (CELL - ICON) / 2;
			int y = TOP + row * (CELL + GAP) + (CELL - ICON) / 2;
			ResourceLocation texture = skin.isEmpty() ? DEFAULT : RemoteTextures.get(TextureStore.KEYS.kind(), skin);
			if (texture == null) {
				texture = DEFAULT;
			}
			graphics.blit(texture, x, y, ICON, ICON, 0.0F, 0.0F, 16, 16, 16, 16);
			if (skin.equals(current)) {
				graphics.renderOutline(x - 3, y - 3, ICON + 6, ICON + 6, 0xFFFFFF55);
			}
		}
	}

	private void select(String skin) {
		ClientPlayNetworking.send(new KeySkinSelectPayload(mainHand, skin));
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
