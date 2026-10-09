package cn.erindax.projectgmb.command;

import cn.erindax.projectgmb.dance.DanceChart;
import cn.erindax.projectgmb.dance.DanceCharts;
import cn.erindax.projectgmb.dance.DanceManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class DanceCommands {

	private static final String ARG_SONG = "song";

	private DanceCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> dance() {
		return Commands.literal("dance")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("true").executes(context -> toggle(context.getSource(), true)))
			.then(Commands.literal("false").executes(context -> toggle(context.getSource(), false)))
			.then(Commands.literal("start")
				.then(Commands.argument(ARG_SONG, StringArgumentType.greedyString())
					.suggests(DanceCommands::suggestSongs)
					.executes(context -> start(context.getSource(),
						StringArgumentType.getString(context, ARG_SONG).strip()))))
			.then(Commands.literal("stop").executes(context -> stop(context.getSource())))
			.then(Commands.literal("reload").executes(context -> reload(context.getSource())));
	}

	private static CompletableFuture<Suggestions> suggestSongs(CommandContext<CommandSourceStack> context,
			SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(DanceCharts.names(), builder);
	}

	private static int toggle(CommandSourceStack source, boolean enabled) {
		MinecraftServer server = source.getServer();
		boolean stopped = DanceManager.setEnabled(server, enabled);
		source.sendSuccess(() -> Component.translatable(enabled ? "commands.projectgm_b.dance.on" : "commands.projectgm_b.dance.off"),
			true);
		if (stopped) {
			source.sendSuccess(() -> Component.translatable("commands.projectgm_b.dance.stopped"), true);
		}
		return 1;
	}

	private static int start(CommandSourceStack source, String song) {
		DanceChart chart = DanceCharts.get(song);
		if (chart == null) {
			source.sendFailure(Component.translatable("commands.projectgm_b.dance.unknown", song));
			return 0;
		}
		DanceManager.StartResult result = DanceManager.start(source.getServer(), chart);
		switch (result.status()) {
			case OK -> {
				source.sendSuccess(() -> Component.translatable("commands.projectgm_b.dance.started", chart.title(),
					result.players()), true);
				return result.players();
			}
			case DISABLED -> source.sendFailure(Component.translatable("commands.projectgm_b.dance.not_enabled"));
			case BUSY -> source.sendFailure(Component.translatable("commands.projectgm_b.dance.busy"));
			case NO_AUDIO -> source.sendFailure(Component.translatable("commands.projectgm_b.dance.no_audio", chart.audio()));
			case NOBODY -> source.sendFailure(Component.translatable("commands.projectgm_b.dance.nobody"));
		}
		return 0;
	}

	private static int stop(CommandSourceStack source) {
		if (!DanceManager.stop(source.getServer())) {
			source.sendFailure(Component.translatable("commands.projectgm_b.dance.idle"));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.dance.stopped"), true);
		return 1;
	}

	private static int reload(CommandSourceStack source) {
		int count = DanceCharts.reload();
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.dance.reloaded", count,
			DanceCharts.directory().toString()), true);
		List<DanceCharts.Failure> failures = DanceCharts.failed();
		if (!failures.isEmpty()) {
			MutableComponent list = Component.empty();
			for (int i = 0; i < failures.size(); i++) {
				DanceCharts.Failure failure = failures.get(i);
				if (i > 0) {
					list.append(Component.translatable("commands.projectgm_b.dance.failure.separator"));
				}
				list.append(Component.translatable("commands.projectgm_b.dance.failure.entry", failure.file(),
					Component.translatable("commands.projectgm_b.dance.failure." + failure.reason())));
			}
			source.sendFailure(Component.translatable("commands.projectgm_b.dance.reload_failed", list));
		}
		return count;
	}

}
