package cn.erindax.projectgmb.client.gui;

import cn.erindax.projectgmb.client.music.MusicUploader;
import cn.erindax.projectgmb.item.MusicNoteItem;
import cn.erindax.projectgmb.music.net.MusicMenuPayload;
import cn.erindax.projectgmb.music.net.MusicNoteActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MusicNoteScreen extends Screen {

	private static final int LIST_WIDTH = 260;
	private static final int ITEM_HEIGHT = 18;
	private static final int LIST_TOP = 30;
	private static final int LIST_BOTTOM_MARGIN = 88;
	private static final int STATUS_Y_FROM_BOTTOM = 84;

	private final boolean mainHand;
	private final List<String> tracks;
	private String selected;
	private int range;
	private boolean playing;
	private boolean paused;
	private TrackList list;
	private Button playButton;
	private Component status = Component.empty();

	public MusicNoteScreen(MusicMenuPayload data) {
		super(Component.translatable("screen.projectgm_b.music.title"));
		this.mainHand = data.mainHand();
		this.tracks = new ArrayList<>(data.tracks());
		this.selected = data.track();
		this.range = MusicNoteItem.clampRange(data.range());
		this.playing = data.playing();
		this.paused = data.paused();
		MusicUploader.reset();
	}

	@Override
	protected void init() {
		int listHeight = Math.max(ITEM_HEIGHT, height - LIST_BOTTOM_MARGIN - LIST_TOP);
		list = new TrackList(minecraft, LIST_WIDTH, listHeight, LIST_TOP, ITEM_HEIGHT);
		list.setX((width - LIST_WIDTH) / 2);
		addRenderableWidget(list);

		addRenderableWidget(new RangeSlider((width - 200) / 2, height - 64, 200, 20));

		int y = height - 34;
		playButton = addRenderableWidget(Button.builder(playLabel(), b -> togglePlay())
			.bounds(width / 2 - 105, y, 70, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("screen.projectgm_b.music.stop"), b -> stop())
			.bounds(width / 2 - 30, y, 60, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
			.bounds(width / 2 + 35, y, 70, 20).build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
		Component current = selected.isEmpty()
			? Component.translatable("item.projectgm_b.music_note.no_track")
			: Component.translatable("item.projectgm_b.music_note.track", MusicNoteItem.displayName(selected));
		graphics.drawCenteredString(font, current, width / 2, 20, 0xAAAAAA);
		if (tracks.isEmpty()) {
			Component hint = Component.translatable("screen.projectgm_b.music.empty");
			graphics.drawWordWrap(font, hint, list.getX() + 10, LIST_TOP + 20, LIST_WIDTH - 20, 0xCCCCCC);
		}
		graphics.drawCenteredString(font, statusLine(), width / 2, height - STATUS_Y_FROM_BOTTOM, 0xAAAAAA);
	}

	private Component statusLine() {
		if (MusicUploader.isWaiting()) {
			return Component.translatable("screen.projectgm_b.music.upload_waiting", MusicNoteItem.displayName(MusicUploader.name()));
		}
		if (MusicUploader.isBusy()) {
			return Component.translatable("screen.projectgm_b.music.uploading", MusicNoteItem.displayName(MusicUploader.name()),
				MusicUploader.percent());
		}
		return status.getString().isEmpty() ? Component.translatable("screen.projectgm_b.music.drop_hint") : status;
	}

	@Override
	public void onFilesDrop(List<Path> paths) {
		if (paths.isEmpty()) {
			return;
		}
		MusicUploader.Result result = MusicUploader.start(paths.get(0), mainHand);
		status = switch (result) {
			case STARTED -> Component.empty();
			case BUSY -> Component.translatable("screen.projectgm_b.music.upload_busy");
			case UNSUPPORTED -> Component.translatable("screen.projectgm_b.music.upload_unsupported");
			case TOO_LARGE -> Component.translatable("screen.projectgm_b.music.upload_too_large");
			case UNREADABLE -> Component.translatable("screen.projectgm_b.music.upload_unreadable");
		};
	}

	private Component playLabel() {
		if (!playing) {
			return Component.translatable("screen.projectgm_b.music.play");
		}
		return Component.translatable(paused ? "screen.projectgm_b.music.resume" : "screen.projectgm_b.music.pause");
	}

	private void togglePlay() {
		if (!playing) {
			if (selected.isEmpty()) {
				return;
			}
			send(MusicNoteActionPayload.Action.PLAY);
			playing = true;
			paused = false;
		} else if (!paused) {
			send(MusicNoteActionPayload.Action.PAUSE);
			paused = true;
		} else {
			send(MusicNoteActionPayload.Action.RESUME);
			paused = false;
		}
		playButton.setMessage(playLabel());
	}

	private void stop() {
		send(MusicNoteActionPayload.Action.STOP);
		playing = false;
		paused = false;
		playButton.setMessage(playLabel());
	}

	private void select(String track) {
		if (!track.equals(selected)) {
			selected = track;
			send(MusicNoteActionPayload.Action.SELECT);
		}
	}

	private void send(MusicNoteActionPayload.Action action) {
		ClientPlayNetworking.send(new MusicNoteActionPayload(mainHand, action, selected, range));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private class TrackList extends ObjectSelectionList<TrackList.Entry> {

		private TrackList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
			super(minecraft, width, height, y, itemHeight);
			for (String track : tracks) {
				Entry entry = new Entry(track);
				addEntry(entry);
				if (track.equals(selected)) {
					setSelected(entry);
				}
			}
		}

		@Override
		public int getRowWidth() {
			return LIST_WIDTH - 20;
		}

		@Override
		protected int getScrollbarPosition() {
			return getX() + LIST_WIDTH - 8;
		}

		private class Entry extends ObjectSelectionList.Entry<Entry> {

			private final String track;

			private Entry(String track) {
				this.track = track;
			}

			@Override
			public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX,
					int mouseY, boolean hovering, float partialTick) {
				int color = track.equals(selected) ? 0x55FF55 : 0xFFFFFF;
				graphics.drawString(font, MusicNoteItem.displayName(track), left + 5, top + (height - 9) / 2, color);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				select(track);
				return super.mouseClicked(mouseX, mouseY, button);
			}

			@Override
			public Component getNarration() {
				return Component.literal(MusicNoteItem.displayName(track));
			}
		}
	}

	private class RangeSlider extends AbstractSliderButton {

		private RangeSlider(int x, int y, int width, int height) {
			super(x, y, width, height, Component.translatable("screen.projectgm_b.music.range", range), toValue(range));
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.translatable("screen.projectgm_b.music.range", range));
		}

		@Override
		protected void applyValue() {
			int newRange = fromValue(value);
			if (newRange != range) {
				range = newRange;
				send(MusicNoteActionPayload.Action.RANGE);
			}
		}

		private static double toValue(int range) {
			return (range - MusicNoteItem.MIN_RANGE) / (double) (MusicNoteItem.MAX_RANGE - MusicNoteItem.MIN_RANGE);
		}

		private static int fromValue(double value) {
			return MusicNoteItem.clampRange(MusicNoteItem.MIN_RANGE
				+ (int) Math.round(value * (MusicNoteItem.MAX_RANGE - MusicNoteItem.MIN_RANGE)));
		}
	}
}
