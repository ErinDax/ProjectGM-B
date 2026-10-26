package cn.erindax.projectgmb.client.gui;

import cn.erindax.projectgmb.client.render.RemoteTextures;
import cn.erindax.projectgmb.entity.net.PatrollerListActionPayload;
import cn.erindax.projectgmb.entity.net.PatrollerSettingsPayload;
import cn.erindax.projectgmb.entity.net.PatrollerUpdatePayload;
import cn.erindax.projectgmb.network.Network;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PatrollerSettingsScreen extends Screen {

	private record Label(Component text, int x, int y, int color) {
	}

	private static final int ROW = 20;
	private static final int GAP = 4;
	private static final int PAD = 8;
	private static final int SECTION = 13;
	private static final int PANEL_MAX = 470;
	private static final int PREVIEW_MAX = 140;
	private static final int ARMS_WIDTH = 72;
	private static final int HEADER = 32;
	private static final int TITLE_COLOR = 0xFFFFFF;
	private static final int LABEL_COLOR = 0xA0A0A8;
	private static final int SECTION_COLOR = 0xE6C35C;
	private static final int HINT_COLOR = 0x70707C;
	private static final int PANEL_FILL = 0xD0181820;
	private static final int PANEL_BORDER = 0x60FFFFFF;
	private static final int PREVIEW_FILL = 0xFF0E0E14;
	private static final int PREVIEW_BORDER = 0x40FFFFFF;
	private static final long CONFIRM_MS = 3000L;
	private static final float DEFAULT_YAW = 25.0F;
	private static final float DRAG_SPEED = 2.5F;

	private final PatrollerSettingsPayload data;
	private final List<Label> labels = new ArrayList<>();

	@Nullable
	private PlayerModel<LivingEntity> wideModel;
	@Nullable
	private PlayerModel<LivingEntity> slimModel;
	private EditBox nameBox;
	private CycleButton<String> skinButton;
	private CycleButton<Boolean> slimButton;
	private CycleButton<Boolean> nameTagButton;
	private CycleButton<Boolean> heldItemsButton;
	private CycleButton<Boolean> patrollingButton;
	private CycleButton<Boolean> aggressiveButton;
	private EditBox pauseBox;
	private EditBox detectBox;
	private EditBox stareBox;
	private Button removeButton;
	private Button backButton;
	private int left;
	private int top;
	private int panelWidth;
	private int panelHeight;
	private int previewX0;
	private int previewY0;
	private int previewX1;
	private int previewY1;
	private int dividerY;
	private float yaw = DEFAULT_YAW;
	private boolean draggingPreview;
	private long removeConfirmUntil;
	private long backConfirmUntil;

	public PatrollerSettingsScreen(PatrollerSettingsPayload data) {
		super(Component.translatable("screen.projectgm_b.patroller.title"));
		this.data = data;
	}

	@Override
	protected void init() {
		PatrollerUpdatePayload kept = nameBox == null ? null : collect(false);
		labels.clear();
		panelWidth = Math.min(PANEL_MAX, width - 16);
		int previewWidth = Math.min(PREVIEW_MAX, panelWidth / 3);
		int body = SECTION * 2 + (ROW + GAP) * 5;
		panelHeight = HEADER + body + GAP + 1 + GAP + ROW + PAD;
		left = (width - panelWidth) / 2;
		top = Math.max(4, (height - panelHeight) / 2);

		previewX0 = left + PAD;
		previewX1 = previewX0 + previewWidth;
		int x = previewX1 + PAD;
		int rightWidth = left + panelWidth - PAD - x;
		int half = (rightWidth - GAP) / 2;
		int y = top + HEADER;
		int bodyBottom = y + body;

		labels.add(new Label(Component.translatable("screen.projectgm_b.patroller.section_look"), x, y + 2,
			SECTION_COLOR));
		y += SECTION;
		Component nameLabel = Component.translatable("screen.projectgm_b.patroller.name");
		int nameLabelWidth = font.width(nameLabel) + 6;
		labels.add(new Label(nameLabel, x, y + 6, LABEL_COLOR));
		nameBox = new EditBox(font, x + nameLabelWidth, y, rightWidth - nameLabelWidth, ROW, nameLabel);
		nameBox.setMaxLength(32);
		nameBox.setValue(kept == null ? data.name() : kept.name());
		addRenderableWidget(nameBox);
		y += ROW + GAP;

		List<String> skins = new ArrayList<>();
		skins.add("");
		skins.addAll(data.skins());
		if (!data.skin().isEmpty() && !skins.contains(data.skin())) {
			skins.add(data.skin());
		}
		skinButton = addRenderableWidget(CycleButton.<String>builder(s ->
				s.isEmpty() ? Component.translatable("screen.projectgm_b.patroller.skin_none") : Component.literal(s))
			.withValues(skins)
			.withInitialValue(kept == null ? data.skin() : kept.skin())
			.create(x, y, rightWidth - ARMS_WIDTH - GAP, ROW,
				Component.translatable("screen.projectgm_b.patroller.skin"), (b, v) -> {}));
		slimButton = addRenderableWidget(CycleButton.<Boolean>builder(slim -> Component.translatable(
				slim ? "screen.projectgm_b.patroller.arms_slim" : "screen.projectgm_b.patroller.arms_wide"))
			.withValues(List.of(false, true))
			.withInitialValue(kept == null ? data.slim() : kept.slim())
			.displayOnlyValue()
			.create(x + rightWidth - ARMS_WIDTH, y, ARMS_WIDTH, ROW,
				Component.translatable("screen.projectgm_b.patroller.arms"), (b, v) -> {}));
		y += ROW + GAP;

		nameTagButton = addRenderableWidget(CycleButton.<Boolean>builder(show -> Component.translatable(
				show ? "screen.projectgm_b.patroller.held_shown" : "screen.projectgm_b.patroller.held_hidden"))
			.withValues(List.of(true, false))
			.withInitialValue(kept == null ? data.nameVisible() : kept.nameVisible())
			.create(x, y, half, ROW, Component.translatable("screen.projectgm_b.patroller.nametag"), (b, v) -> {}));
		heldItemsButton = addRenderableWidget(CycleButton.<Boolean>builder(show -> Component.translatable(
				show ? "screen.projectgm_b.patroller.held_shown" : "screen.projectgm_b.patroller.held_hidden"))
			.withValues(List.of(true, false))
			.withInitialValue(kept == null ? data.showHeldItems() : kept.showHeldItems())
			.create(x + half + GAP, y, half, ROW, Component.translatable("screen.projectgm_b.patroller.held"),
				(b, v) -> {}));
		y += ROW + GAP;

		labels.add(new Label(Component.translatable("screen.projectgm_b.patroller.section_behavior"), x, y + 2,
			SECTION_COLOR));
		y += SECTION;
		patrollingButton = addRenderableWidget(CycleButton.<Boolean>builder(on -> Component.translatable(
				on ? "screen.projectgm_b.patroller.patrol_on" : "screen.projectgm_b.patroller.patrol_off"))
			.withValues(List.of(true, false))
			.withInitialValue(kept == null ? data.patrolling() : kept.patrolling())
			.create(x, y, half, ROW, Component.translatable("screen.projectgm_b.patroller.patrol"), (b, v) -> {}));
		aggressiveButton = addRenderableWidget(CycleButton.onOffBuilder(kept == null ? data.aggressive() : kept.aggressive())
			.create(x + half + GAP, y, half, ROW, Component.translatable("screen.projectgm_b.patroller.aggressive"),
				(b, v) -> {}));
		y += ROW + GAP;

		int third = (rightWidth - GAP * 2) / 3;
		pauseBox = numberBox(x, y, third, kept == null ? data.pauseSeconds() : kept.pauseSeconds(), "pause",
			"screen.projectgm_b.patroller.unit_seconds");
		detectBox = numberBox(x + third + GAP, y, third, kept == null ? data.detectDiameter() : kept.detectDiameter(),
			"detect", "screen.projectgm_b.patroller.unit_blocks");
		stareBox = numberBox(x + (third + GAP) * 2, y, third, kept == null ? data.stareSeconds() : kept.stareSeconds(),
			"stare", "screen.projectgm_b.patroller.unit_seconds");

		int recordY = bodyBottom - ROW - GAP;
		addRenderableWidget(Button.builder(Component.translatable("screen.projectgm_b.patroller.record"), b -> record())
			.bounds(previewX0, recordY, previewWidth, ROW)
			.tooltip(Tooltip.create(Component.translatable("screen.projectgm_b.patroller.record_tip")))
			.build());
		labels.add(new Label(Component.translatable("screen.projectgm_b.patroller.route", data.waypointCount()),
			previewX0, recordY - 11, LABEL_COLOR));
		previewY0 = top + HEADER;
		previewY1 = recordY - 15;

		dividerY = bodyBottom + GAP;
		int footerY = dividerY + 1 + GAP;
		int buttonWidth = Math.min(90, (panelWidth - PAD * 2 - GAP * 2) / 3);
		removeButton = addRenderableWidget(Button.builder(
				Component.translatable("screen.projectgm_b.patroller.remove_long"), b -> remove())
			.bounds(left + PAD, footerY, buttonWidth, ROW).build());
		int rightEdge = left + panelWidth - PAD;
		backButton = addRenderableWidget(Button.builder(backLabel(), b -> back())
			.bounds(rightEdge - buttonWidth, footerY, buttonWidth, ROW).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.projectgm_b.patroller.save")
				.withStyle(ChatFormatting.GREEN), b -> save())
			.bounds(rightEdge - buttonWidth * 2 - GAP, footerY, buttonWidth, ROW).build());

		setInitialFocus(nameBox);
	}

	private EditBox numberBox(int x, int y, int width, int value, String key, String unitKey) {
		Component label = Component.translatable("screen.projectgm_b.patroller." + key + "_short");
		Component unit = Component.translatable(unitKey);
		int labelWidth = font.width(label) + 3;
		int unitWidth = font.width(unit) + 3;
		labels.add(new Label(label, x, y + 6, LABEL_COLOR));
		labels.add(new Label(unit, x + width - unitWidth + 3, y + 6, LABEL_COLOR));
		EditBox box = new EditBox(font, x + labelWidth, y, Math.max(20, width - labelWidth - unitWidth), ROW, label);
		box.setMaxLength(4);
		box.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
		box.setValue(Integer.toString(value));
		box.setTooltip(Tooltip.create(Component.translatable("screen.projectgm_b.patroller." + key + "_tip")));
		addRenderableWidget(box);
		return box;
	}

	private void renderPanel(GuiGraphics graphics) {
		graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL_FILL);
		graphics.renderOutline(left, top, panelWidth, panelHeight, PANEL_BORDER);
		graphics.fill(previewX0, previewY0, previewX1, previewY1, PREVIEW_FILL);
		graphics.renderOutline(previewX0, previewY0, previewX1 - previewX0, previewY1 - previewY0, PREVIEW_BORDER);
		graphics.fill(left + PAD, dividerY, left + panelWidth - PAD, dividerY + 1, PREVIEW_BORDER);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		refreshConfirmLabels();
		renderBackground(graphics);
		renderPanel(graphics);
		super.render(graphics, mouseX, mouseY, partialTick);
		String shownName = nameBox.getValue().isBlank() ? data.name() : nameBox.getValue().strip();
		graphics.drawString(font, Component.literal(shownName), left + PAD, top + 8, TITLE_COLOR);
		Component location = Component.translatable("screen.projectgm_b.patroller.location",
			PatrollerListScreen.dimensionName(data.dimension()), data.x(), data.y(), data.z());
		graphics.drawString(font, location, left + PAD, top + 19, LABEL_COLOR);
		for (Label label : labels) {
			graphics.drawString(font, label.text(), label.x(), label.y(), label.color());
		}
		drawPreview(graphics);
		graphics.drawCenteredString(font, Component.translatable("screen.projectgm_b.patroller.preview_hint"),
			(previewX0 + previewX1) / 2, previewY1 - 11, HINT_COLOR);
	}

	private void drawPreview(GuiGraphics graphics) {
		int boxHeight = previewY1 - previewY0;
		if (boxHeight < 40) {
			return;
		}
		PlayerModel<LivingEntity> model = model(slimButton.getValue());
		ResourceLocation texture = RemoteTextures.skin(skinButton.getValue());
		float scale = (boxHeight - 26) / 2.1F;
		float centerX = (previewX0 + previewX1) / 2.0F;
		float feet = previewY1 - 16;
		graphics.enableScissor(previewX0 + 1, previewY0 + 1, previewX1 - 1, previewY1 - 1);
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(centerX, feet - 1.5F * scale, 100.0F);
		pose.scale(scale, scale, -scale);
		pose.mulPose(Axis.YP.rotationDegrees(yaw));
		Lighting.setupForEntityInInventory();
		VertexConsumer consumer = graphics.bufferSource().getBuffer(model.renderType(texture));
		model.renderToBuffer(pose, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
		graphics.flush();
		Lighting.setupFor3DItems();
		pose.popPose();
		graphics.disableScissor();
	}

	private PlayerModel<LivingEntity> model(boolean slim) {
		Minecraft minecraft = Minecraft.getInstance();
		if (slim) {
			if (slimModel == null) {
				slimModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
				slimModel.young = false;
			}
			return slimModel;
		}
		if (wideModel == null) {
			wideModel = new PlayerModel<>(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
			wideModel.young = false;
		}
		return wideModel;
	}

	private boolean inPreview(double mouseX, double mouseY) {
		return mouseX >= previewX0 && mouseX < previewX1 && mouseY >= previewY0 && mouseY < previewY1;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		if (button == 0 && inPreview(mouseX, mouseY)) {
			draggingPreview = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (draggingPreview) {
			yaw += (float) dragX * DRAG_SPEED;
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingPreview = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	private void refreshConfirmLabels() {
		long now = Util.getMillis();
		removeButton.setMessage(now < removeConfirmUntil
			? Component.translatable("screen.projectgm_b.patroller.confirm_remove").withStyle(ChatFormatting.RED)
			: Component.translatable("screen.projectgm_b.patroller.remove_long"));
		backButton.setMessage(now < backConfirmUntil
			? Component.translatable("screen.projectgm_b.patroller.discard").withStyle(ChatFormatting.YELLOW)
			: backLabel());
	}

	private Component backLabel() {
		return Component.translatable(data.fromList() ? "gui.back" : "gui.cancel");
	}

	private PatrollerUpdatePayload collect(boolean returnToList) {
		return new PatrollerUpdatePayload(data.name(), nameBox.getValue().trim(), skinButton.getValue(),
			slimButton.getValue(), parse(pauseBox, data.pauseSeconds()),
			parse(detectBox, data.detectDiameter()), parse(stareBox, data.stareSeconds()),
			nameTagButton.getValue(), heldItemsButton.getValue(), patrollingButton.getValue(),
			aggressiveButton.getValue(), returnToList);
	}

	private boolean changed() {
		PatrollerUpdatePayload original = new PatrollerUpdatePayload(data.name(), data.name(), data.skin(), data.slim(),
			data.pauseSeconds(), data.detectDiameter(), data.stareSeconds(), data.nameVisible(), data.showHeldItems(),
			data.patrolling(), data.aggressive(), false);
		return !collect(false).equals(original);
	}

	private static int parse(EditBox box, int fallback) {
		try {
			return Integer.parseInt(box.getValue().trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private void save() {
		Network.sendToServer(collect(data.fromList()));
		close();
	}

	private void record() {
		Network.sendToServer(collect(false));
		Network.sendToServer(new PatrollerListActionPayload(PatrollerListActionPayload.Action.RECORD,
			nameBox.getValue().trim()));
		close();
	}

	private void remove() {
		long now = Util.getMillis();
		if (now >= removeConfirmUntil) {
			removeConfirmUntil = now + CONFIRM_MS;
			return;
		}
		Network.sendToServer(new PatrollerListActionPayload(PatrollerListActionPayload.Action.REMOVE, data.name()));
		if (!data.fromList()) {
			close();
		}
	}

	private void back() {
		long now = Util.getMillis();
		if (changed() && now >= backConfirmUntil) {
			backConfirmUntil = now + CONFIRM_MS;
			return;
		}
		if (data.fromList()) {
			Network.sendToServer(new PatrollerListActionPayload(PatrollerListActionPayload.Action.REFRESH, ""));
		} else {
			close();
		}
	}

	private void close() {
		super.onClose();
	}

	@Override
	public void onClose() {
		back();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
