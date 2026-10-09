package cn.erindax.projectgmb.command;

import cn.erindax.projectgmb.lock.LockNames;
import cn.erindax.projectgmb.manage.WandWhitelist;
import cn.erindax.projectgmb.skin.TextureStore;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class ModCommands {

	private ModCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
			dispatcher.register(Commands.literal("projectgmb")
				.requires(source -> source.hasPermission(2))
				.then(Commands.literal("reload")
					.executes(ctx -> reload(ctx.getSource())))
				.then(WandCommands.wand())
				.then(DanceCommands.dance()))
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
		int keys = reloadKeyTextures(source, source.getServer().getPlayerList().getPlayers());
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.reload.textures", keys), true);
		return 1;
	}

	private static int reloadKeyTextures(CommandSourceStack source, List<ServerPlayer> players) {
		TextureStore store = TextureStore.KEYS;
		store.refresh();
		TextureStore.Scan scan = store.scan();
		for (String name : scan.names()) {
			store.sendToAll(players, name);
		}
		for (TextureStore.Problem problem : scan.problems()) {
			Component reason = problem.reason().equals("size")
				? Component.translatable("commands.projectgm_b.reload.texture.keys_size", problem.width(), problem.height())
				: Component.translatable("commands.projectgm_b.reload.texture." + problem.reason(), TextureStore.MAX_NAME);
			source.sendFailure(Component.translatable("commands.projectgm_b.reload.texture.skipped",
				store.directory().getFileName() + "/" + problem.file(), reason));
		}
		return scan.names().size();
	}
}
