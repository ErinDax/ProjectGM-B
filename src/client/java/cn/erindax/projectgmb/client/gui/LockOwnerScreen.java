package cn.erindax.projectgmb.client.gui;

import cn.erindax.projectgmb.lock.LockMode;
import cn.erindax.projectgmb.lock.LockType;
import cn.erindax.projectgmb.lock.net.LockOwnerUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

public class LockOwnerScreen extends Screen {

	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int GAP = 4;

	private final BlockPos pos;
	private final LockType type;
	private final LockMode initialMode;
	private CycleButton<LockMode> modeButton;

	public LockOwnerScreen(BlockPos pos, LockType type, LockMode mode) {
		super(Component.translatable("screen.projectgm_b.lock.owner_title"));
		this.pos = pos;
		this.type = type;
		this.initialMode = mode;
	}

	@Override
	protected void init() {
		int x = (width - FIELD_WIDTH) / 2;
		int y = height / 2 - FIELD_HEIGHT - GAP;
		int half = (FIELD_WIDTH - GAP) / 2;

		modeButton = addRenderableWidget(CycleButton.<LockMode>builder(mode -> Component.translatable(
				mode == LockMode.SINGLE ? "screen.projectgm_b.lock.mode_single" : "screen.projectgm_b.lock.mode_every"))
			.withValues(List.of(LockMode.EVERY, LockMode.SINGLE))
			.withInitialValue(initialMode)
			.withTooltip(mode -> net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
				mode == LockMode.SINGLE ? "screen.projectgm_b.lock.mode_single_hint" : "screen.projectgm_b.lock.mode_every_hint")))
			.create(x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.translatable("screen.projectgm_b.lock.mode"), (b, v) -> {}));
		y += FIELD_HEIGHT + GAP;

		addRenderableWidget(Button.builder(Component.translatable("screen.projectgm_b.lock.remove"), b -> remove())
			.bounds(x, y, FIELD_WIDTH, FIELD_HEIGHT).build());
		y += FIELD_HEIGHT + GAP * 2;

		addRenderableWidget(Button.builder(Component.translatable("screen.projectgm_b.lock.save"), b -> save())
			.bounds(x, y, half, FIELD_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
			.bounds(x + half + GAP, y, half, FIELD_HEIGHT).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		int top = height / 2 - FIELD_HEIGHT - GAP;
		graphics.drawCenteredString(font, title, width / 2, top - 28, 0xFFFFFF);
		graphics.drawCenteredString(font, Component.translatable(
				type == LockType.KEY ? "item.projectgm_b.key_lock" : "item.projectgm_b.password_lock"),
			width / 2, top - 14, 0xA0A0A0);
	}

	private void save() {
		ClientPlayNetworking.send(new LockOwnerUpdatePayload(pos, modeButton.getValue().ordinal(), false));
		onClose();
	}

	private void remove() {
		ClientPlayNetworking.send(new LockOwnerUpdatePayload(pos, modeButton.getValue().ordinal(), true));
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
