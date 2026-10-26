package cn.erindax.projectgmb.command;

import cn.erindax.projectgmb.entity.PatrolRecorder;
import cn.erindax.projectgmb.entity.PatrollerEntity;
import cn.erindax.projectgmb.entity.Patrollers;
import cn.erindax.projectgmb.entity.net.PatrollerSettingsHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PatrolCommands {

	private static final String ARG_NAME = "name";
	private static final String LITERAL_ABORT = "abort";

	private PatrolCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> patrol() {
		return Commands.literal("patrol")
			.then(Commands.literal("record")
				.executes(ctx -> finishRecording(ctx.getSource()))
				.then(Commands.literal(LITERAL_ABORT).executes(ctx -> abortRecording(ctx.getSource())))
				.then(Commands.argument(ARG_NAME, StringArgumentType.greedyString())
					.suggests(PatrolCommands::suggestNames)
					.executes(ctx -> startRecording(ctx.getSource(), StringArgumentType.getString(ctx, ARG_NAME)))))
			.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())));
	}

	private static CompletableFuture<Suggestions> suggestNames(CommandContext<CommandSourceStack> ctx,
			SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(
			Patrollers.all(ctx.getSource().getServer()).stream().map(PatrollerEntity::getPatrolName), builder);
	}

	private static int startRecording(CommandSourceStack source, String rawName) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String name = rawName.trim();
		if (!Patrollers.isValidName(name) || name.equals(LITERAL_ABORT)) {
			source.sendFailure(Component.translatable("commands.projectgm_b.patrol.bad_name", name));
			return 0;
		}
		PatrolRecorder.Session current = PatrolRecorder.current(player);
		if (current != null) {
			source.sendFailure(Component.translatable("commands.projectgm_b.patrol.record_busy", current.name()));
			return 0;
		}
		PatrolRecorder.start(player, name);
		return 1;
	}

	private static int finishRecording(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		PatrolRecorder.Session session = PatrolRecorder.current(player);
		if (session == null) {
			source.sendFailure(Component.translatable("commands.projectgm_b.patrol.record_idle"));
			return 0;
		}
		if (session.points().isEmpty()) {
			source.sendFailure(Component.translatable("commands.projectgm_b.patrol.record_no_points"));
			return 0;
		}
		PatrolRecorder.stop(player);
		String name = session.name();
		List<BlockPos> route = List.copyOf(session.points());
		PatrollerEntity existing = Patrollers.findByName(source.getServer(), name);
		if (existing != null) {
			existing.setWaypoints(route);
			source.sendSuccess(() -> Component.translatable("commands.projectgm_b.patrol.route_applied", route.size(),
				name), true);
			return 1;
		}
		if (Patrollers.spawn(source.getLevel(), name, route, source.getRotation().y) == null) {
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.patrol.spawned", name, route.size()),
			true);
		return 1;
	}

	private static int abortRecording(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		PatrolRecorder.Session session = PatrolRecorder.stop(player);
		if (session == null) {
			source.sendFailure(Component.translatable("commands.projectgm_b.patrol.record_idle"));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.patrol.record_aborted", session.name()),
			false);
		return 1;
	}

	private static int list(CommandSourceStack source) throws CommandSyntaxException {
		PatrollerSettingsHandler.openList(source.getPlayerOrException());
		return 1;
	}
}
