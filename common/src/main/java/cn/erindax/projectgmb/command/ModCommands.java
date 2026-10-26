package cn.erindax.projectgmb.command;

import cn.erindax.projectgmb.lock.LockNames;
import cn.erindax.projectgmb.manage.WandWhitelist;
import cn.erindax.projectgmb.platform.ModEvents;
import cn.erindax.projectgmb.skin.TextureStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ModCommands {

	private ModCommands() {
	}

	public static void register() {
		ModEvents.REGISTER_COMMANDS.register(dispatcher ->
			dispatcher.register(Commands.literal("projectgmb")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("reload")
					.executes(ctx -> reload(ctx.getSource())))
				.then(WandCommands.wand())
				.then(DanceCommands.dance())
				.then(PatrolCommands.patrol()))
		);
	}

	private static int reload(CommandSourceStack source) {
		LockNames.reload();
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.reload"), true);
		if (!WandWhitelist.reload()) {
			source.sendFailure(Component.translatable("commands.projectgm_b.reload.failed", WandWhitelist.file().toString()));
		} else {
			int count = WandWhitelist.size();
			source.sendSuccess(() -> Component.translatable("commands.projectgm_b.reload.done", count), true);
		}
		List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
		int keys = reloadTextures(source, TextureStore.KEYS, players);
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.reload.textures", keys), true);
		int skins = reloadTextures(source, TextureStore.SKINS, players);
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.reload.skins", skins), true);
		return 1;
	}

	private static int reloadTextures(CommandSourceStack source, TextureStore store, List<ServerPlayer> players) {
		store.refresh();
		TextureStore.Scan scan = store.scan();
		for (String name : scan.names()) {
			store.sendToAll(players, name);
		}
		for (TextureStore.Problem problem : scan.problems()) {
			Component reason = problem.reason().equals("size")
				? Component.translatable("commands.projectgm_b.reload.texture." + store.kind() + "_size",
					problem.width(), problem.height())
				: Component.translatable("commands.projectgm_b.reload.texture." + problem.reason(), TextureStore.MAX_NAME);
			source.sendFailure(Component.translatable("commands.projectgm_b.reload.texture.skipped",
				store.directory().getFileName() + "/" + problem.file(), reason));
		}
		return scan.names().size();
	}
}
