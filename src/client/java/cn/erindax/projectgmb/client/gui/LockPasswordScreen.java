package cn.erindax.projectgmb.client.gui;

import cn.erindax.projectgmb.lock.net.LockPasswordPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

public class LockPasswordScreen extends Screen {

	private static final int FIELD_WIDTH = 200;
	private static final int FIELD_HEIGHT = 20;
	private static final int GAP = 4;
	private static final int MAX_LENGTH = 32;

	private final BlockPos pos;
	private final boolean setup;
	private EditBox passwordBox;
	private boolean sent;

	public LockPasswordScreen(BlockPos pos, boolean setup) {
		super(Component.translatable(setup ? "screen.projectgm_b.lock.set_password" : "screen.projectgm_b.lock.enter_password"));
		this.pos = pos;
		this.setup = setup;
	}

	@Override
	protected void init() {
		int x = (width - FIELD_WIDTH) / 2;
		int y = height / 2 - FIELD_HEIGHT;
		passwordBox = new EditBox(font, x, y, FIELD_WIDTH, FIELD_HEIGHT, title);
		passwordBox.setMaxLength(MAX_LENGTH);
		passwordBox.setFormatter((text, offset) -> FormattedCharSequence.forward("*".repeat(text.length()), Style.EMPTY));
		addRenderableWidget(passwordBox);
		setInitialFocus(passwordBox);

		int half = (FIELD_WIDTH - GAP) / 2;
		y += FIELD_HEIGHT + GAP * 2;
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> submit())
			.bounds(x, y, half, FIELD_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
			.bounds(x + half + GAP, y, half, FIELD_HEIGHT).build());
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
			submit();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, height / 2 - FIELD_HEIGHT - 16, 0xFFFFFF);
	}

	private void submit() {
		String password = passwordBox.getValue().strip();
		if (password.isEmpty() || sent) {
			return;
		}
		sent = true;
		ClientPlayNetworking.send(new LockPasswordPayload(pos, password, setup));
		onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
