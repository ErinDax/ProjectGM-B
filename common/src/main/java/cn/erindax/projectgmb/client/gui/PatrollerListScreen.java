package cn.erindax.projectgmb.client.gui;

import cn.erindax.projectgmb.client.render.RemoteTextures;
import cn.erindax.projectgmb.entity.net.PatrollerListActionPayload;
import cn.erindax.projectgmb.entity.net.PatrollerListPayload;
import cn.erindax.projectgmb.network.Network;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatrollerListScreen extends Screen {

	private static final int ROW_HEIGHT = 42;
	private static final int HEADER_HEIGHT = 34;
	private static final int FOOTER_HEIGHT = 40;
	private static final int BUTTON_HEIGHT = 20;
	private static final int ACTION_WIDTH = 44;
	private static final int FACE = 32;
	private static final int GAP = 4;
	private static final int ROW_MAX = 440;
	private static final int SEARCH_WIDTH = 150;
	private static final int TEXT_COLOR = 0xFFFFFF;
	private static final int SUB_COLOR = 0xA0A0A8;
	private static final int ROW_FILL = 0x60202030;
	private static final int ROW_BORDER = 0x50FFFFFF;
	private static final long CONFIRM_MS = 3000L;

	private static String query = "";

	private final PatrollerListPayload data;
	private PatrollerList list;
	private EditBox search;

	public PatrollerListScreen(PatrollerListPayload data) {
		super(Component.translatable("screen.projectgm_b.patrol_list.title"));
		this.data = data;
	}

	@Override
	protected void init() {
		int rowWidth = rowWidth();
		int left = (width - rowWidth) / 2;
		search = new EditBox(font, left + rowWidth - SEARCH_WIDTH, 8, SEARCH_WIDTH, BUTTON_HEIGHT,
			Component.translatable("screen.projectgm_b.patrol_list.search"));
		search.setHint(Component.translatable("screen.projectgm_b.patrol_list.search")
			.withStyle(ChatFormatting.DARK_GRAY));
		search.setMaxLength(32);
		search.setValue(query);
		search.setResponder(value -> {
			query = value;
			refill();
		});
		addRenderableWidget(search);

		list = addRenderableWidget(new PatrollerList());
		refill();

		int buttonWidth = 100;
		int x = (width - buttonWidth * 2 - GAP) / 2;
		int y = height - FOOTER_HEIGHT / 2 - BUTTON_HEIGHT / 2;
		addRenderableWidget(Button.builder(Component.translatable("screen.projectgm_b.patrol_list.refresh"),
				b -> send(PatrollerListActionPayload.Action.REFRESH, ""))
			.bounds(x, y, buttonWidth, BUTTON_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
			.bounds(x + buttonWidth + GAP, y, buttonWidth, BUTTON_HEIGHT).build());
	}

	private void refill() {
		String needle = query.strip().toLowerCase(Locale.ROOT);
		List<Row> rows = new ArrayList<>();
		for (PatrollerListPayload.Entry entry : data.entries()) {
			if (needle.isEmpty() || entry.name().toLowerCase(Locale.ROOT).contains(needle)) {
				rows.add(new Row(entry));
			}
		}
		list.set(rows);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		int left = (width - rowWidth()) / 2;
		graphics.drawString(font, title, left, 14, TEXT_COLOR);
		graphics.drawString(font, Component.translatable("screen.projectgm_b.patrol_list.count",
			data.entries().size()), left + font.width(title) + 8, 14, SUB_COLOR);
		if (list.children().isEmpty()) {
			Component empty = Component.translatable(data.entries().isEmpty()
				? "screen.projectgm_b.patrol_list.empty" : "screen.projectgm_b.patrol_list.no_match");
			graphics.drawCenteredString(font, empty, width / 2, height / 2 - 10, SUB_COLOR);
		}
	}

	private int rowWidth() {
		return Math.min(ROW_MAX, width - 40);
	}

	private static void send(PatrollerListActionPayload.Action action, String name) {
		Network.sendToServer(new PatrollerListActionPayload(action, name));
	}

	static Component dimensionName(String dimension) {
		String key = "screen.projectgm_b.dimension." + dimension.replace(':', '.');
		return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(dimension);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private class PatrollerList extends ContainerObjectSelectionList<Row> {

		PatrollerList() {
			super(Minecraft.getInstance(), PatrollerListScreen.this.width,
				PatrollerListScreen.this.height - HEADER_HEIGHT - FOOTER_HEIGHT, HEADER_HEIGHT, ROW_HEIGHT);
		}

		void set(List<Row> rows) {
			replaceEntries(rows);
			setScrollAmount(0.0);
		}

		@Override
		public int getRowWidth() {
			return rowWidth();
		}

		@Override
		protected int getScrollbarPosition() {
			return (PatrollerListScreen.this.width + getRowWidth()) / 2 + 6;
		}
	}

	private class Row extends ContainerObjectSelectionList.Entry<Row> {

		private final PatrollerListPayload.Entry entry;
		private final Button edit;
		private final Button teleport;
		private final Button remove;
		private final List<Button> buttons;
		private long confirmUntil;

		Row(PatrollerListPayload.Entry entry) {
			this.entry = entry;
			edit = Button.builder(Component.translatable("screen.projectgm_b.patrol_list.edit"),
				b -> send(PatrollerListActionPayload.Action.EDIT, entry.name()))
				.size(ACTION_WIDTH, BUTTON_HEIGHT).build();
			teleport = Button.builder(Component.translatable("screen.projectgm_b.patrol_list.teleport"),
				b -> send(PatrollerListActionPayload.Action.TELEPORT, entry.name()))
				.size(ACTION_WIDTH, BUTTON_HEIGHT).build();
			remove = Button.builder(Component.translatable("screen.projectgm_b.patrol_list.remove"), b -> remove())
				.size(ACTION_WIDTH, BUTTON_HEIGHT).build();
			buttons = List.of(edit, teleport, remove);
		}

		private void remove() {
			long now = Util.getMillis();
			if (now < confirmUntil) {
				send(PatrollerListActionPayload.Action.REMOVE, entry.name());
			} else {
				confirmUntil = now + CONFIRM_MS;
			}
		}

		@Override
		public void render(GuiGraphics graphics, int index, int top, int left, int rowWidth, int rowHeight,
				int mouseX, int mouseY, boolean hovering, float partialTick) {
			int bottom = top + rowHeight - 4;
			int right = left + rowWidth;
			graphics.fill(left, top, right, bottom, ROW_FILL);
			graphics.renderOutline(left, top, rowWidth, bottom - top, ROW_BORDER);

			boolean confirming = Util.getMillis() < confirmUntil;
			remove.setMessage(confirming
				? Component.translatable("screen.projectgm_b.patrol_list.confirm").withStyle(ChatFormatting.RED)
				: Component.translatable("screen.projectgm_b.patrol_list.remove"));
			int buttonY = top + (bottom - top - BUTTON_HEIGHT) / 2;
			remove.setPosition(right - GAP - ACTION_WIDTH, buttonY);
			teleport.setPosition(right - GAP * 2 - ACTION_WIDTH * 2, buttonY);
			edit.setPosition(right - GAP * 3 - ACTION_WIDTH * 3, buttonY);
			for (Button button : buttons) {
				button.render(graphics, mouseX, mouseY, partialTick);
			}

			int faceY = top + (bottom - top - FACE) / 2;
			PlayerFaceRenderer.draw(graphics, RemoteTextures.skin(entry.skin()), left + GAP, faceY, FACE);

			int textX = left + GAP * 2 + FACE;
			int textRight = edit.getX() - GAP;
			int nameY = top + 7;
			Component name = trimmed(Component.literal(entry.name()), textRight - textX - 110);
			graphics.drawString(font, name, textX, nameY, TEXT_COLOR);
			int tagX = textX + font.width(name) + 6;
			tagX = tag(graphics, tagX, nameY - 2, Component.translatable(entry.patrolling()
				? "screen.projectgm_b.patrol_list.tag_on" : "screen.projectgm_b.patrol_list.tag_off"),
				entry.patrolling() ? 0xFF2F5D3A : 0xFF444450, entry.patrolling() ? 0xFF9FE6AD : 0xFFC8C8D0);
			if (entry.hostile()) {
				tag(graphics, tagX + 3, nameY - 2, Component.translatable("screen.projectgm_b.patrol_list.tag_hostile"),
					0xFF6A2C2C, 0xFFFFB0B0);
			}
			Component detail = Component.translatable("screen.projectgm_b.patrol_list.location",
				dimensionName(entry.dimension()), entry.x(), entry.y(), entry.z(), entry.waypoints());
			graphics.drawString(font, trimmed(detail, textRight - textX), textX, top + 22, SUB_COLOR);
		}

		private int tag(GuiGraphics graphics, int x, int y, Component text, int fill, int color) {
			int w = font.width(text) + 6;
			graphics.fill(x, y, x + w, y + 12, fill);
			graphics.drawString(font, text, x + 3, y + 2, color, false);
			return x + w;
		}

		private Component trimmed(Component text, int maxWidth) {
			if (maxWidth <= 0 || font.width(text) <= maxWidth) {
				return text;
			}
			FormattedText cut = font.substrByWidth(text, Math.max(0, maxWidth - font.width("…")));
			return Component.literal(cut.getString() + "…").withStyle(text.getStyle());
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return buttons;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return buttons;
		}
	}
}
