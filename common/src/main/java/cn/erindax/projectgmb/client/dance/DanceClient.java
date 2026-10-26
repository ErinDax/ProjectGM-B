package cn.erindax.projectgmb.client.dance;

import cn.erindax.projectgmb.client.ClientEvents;
import cn.erindax.projectgmb.client.gui.DanceScreen;
import cn.erindax.projectgmb.dance.net.DanceBoardPayload;
import cn.erindax.projectgmb.dance.net.DanceOpenPayload;
import cn.erindax.projectgmb.dance.net.DanceSignalPayload;
import cn.erindax.projectgmb.network.Network;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class DanceClient {

	@Nullable
	private static DanceGame game;

	private DanceClient() {
	}

	public static void init() {
		Network.onClient(DanceOpenPayload.TYPE, payload ->
			open(Minecraft.getInstance(), payload));
		Network.onClient(DanceSignalPayload.TYPE, payload ->
			signal(Minecraft.getInstance(), payload));
		Network.onClient(DanceBoardPayload.TYPE, payload ->
			board(Minecraft.getInstance(), payload));
		ClientEvents.TICK.register(DanceClient::tick);
		ClientEvents.DISCONNECT.register(client -> reset());
	}

	public static void close(DanceGame closing) {
		if (game == closing) {
			game = null;
		}
		closing.dispose();
		Minecraft client = Minecraft.getInstance();
		if (client.screen instanceof DanceScreen screen && screen.game() == closing) {
			client.setScreen(null);
		}
	}

	public static void reset() {
		DanceGame current = game;
		game = null;
		if (current != null) {
			current.dispose();
		}
	}

	private static void open(Minecraft client, DanceOpenPayload payload) {
		reset();
		DanceGame created = new DanceGame(payload);
		game = created;
		client.setScreen(new DanceScreen(created));
	}

	private static void signal(Minecraft client, DanceSignalPayload payload) {
		DanceGame current = game;
		if (current == null || current.session() != payload.session()) {
			return;
		}
		if (payload.signal() == DanceSignalPayload.GO) {
			current.onGo();
		} else if (payload.signal() == DanceSignalPayload.CLOSE) {
			close(current);
		}
	}

	private static void board(Minecraft client, DanceBoardPayload payload) {
		DanceGame current = game;
		if (current == null || current.session() != payload.session()) {
			return;
		}
		if (!payload.result()) {
			current.updateBoard(payload.standings());
			return;
		}
		current.showResults(payload.standings());
		if (!(client.screen instanceof DanceScreen screen && screen.game() == current)) {
			client.setScreen(new DanceScreen(current));
		}
	}

	private static void tick(Minecraft client) {
		DanceGame current = game;
		if (current == null) {
			return;
		}
		if (client.player == null || client.level == null) {
			reset();
			return;
		}
		current.tick();
		if (current.phase() == DanceGame.Phase.CLOSED) {
			close(current);
			return;
		}
		if (current.wantsScreen() && client.screen == null) {
			client.setScreen(new DanceScreen(current));
		}
	}
}
