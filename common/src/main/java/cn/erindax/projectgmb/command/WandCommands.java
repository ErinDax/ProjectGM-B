package cn.erindax.projectgmb.command;

import cn.erindax.projectgmb.manage.WandRosterState;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public final class WandCommands {

	private static final String ARG_TARGETS = "targets";
	private static final String ARG_GROUP = "group";

	private WandCommands() {
	}

	public static LiteralArgumentBuilder<CommandSourceStack> wand() {
		return Commands.literal("wand")
			.requires(source -> source.hasPermission(2))
			.then(Commands.literal("hide")
				.then(Commands.argument(ARG_TARGETS, GameProfileArgument.gameProfile())
					.executes(ctx -> hide(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, ARG_TARGETS)))))
			.then(Commands.literal("show")
				.then(Commands.argument(ARG_TARGETS, GameProfileArgument.gameProfile())
					.executes(ctx -> show(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, ARG_TARGETS)))))
			.then(Commands.literal("hidden")
				.executes(ctx -> listHidden(ctx.getSource())))
			.then(Commands.literal("group")
				.then(Commands.argument(ARG_TARGETS, GameProfileArgument.gameProfile())
					.then(Commands.argument(ARG_GROUP, StringArgumentType.greedyString())
						.suggests(WandCommands::suggestGroups)
						.executes(ctx -> group(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, ARG_TARGETS),
							StringArgumentType.getString(ctx, ARG_GROUP))))))
			.then(Commands.literal("ungroup")
				.then(Commands.argument(ARG_TARGETS, GameProfileArgument.gameProfile())
					.executes(ctx -> ungroup(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, ARG_TARGETS)))))
			.then(Commands.literal("groups")
				.executes(ctx -> listGroups(ctx.getSource())));
	}

	private static CompletableFuture<Suggestions> suggestGroups(CommandContext<CommandSourceStack> ctx,
			SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(groups(ctx.getSource()).keySet(), builder);
	}

	private static int hide(CommandSourceStack source, Collection<GameProfile> profiles) {
		WandRosterState roster = WandRosterState.get(source.getServer());
		for (GameProfile profile : profiles) {
			roster.setHidden(profile.getId(), profile.getName(), true);
		}
		report(source, profiles, profiles.size(), "commands.projectgm_b.wand.hide");
		return profiles.size();
	}

	private static int show(CommandSourceStack source, Collection<GameProfile> profiles) {
		WandRosterState roster = WandRosterState.get(source.getServer());
		int changed = 0;
		for (GameProfile profile : profiles) {
			if (roster.isHidden(profile.getId()) && roster.setHidden(profile.getId(), profile.getName(), false)) {
				changed++;
			}
		}
		if (changed == 0) {
			source.sendFailure(Component.translatable("commands.projectgm_b.wand.show_none"));
			return 0;
		}
		report(source, profiles, changed, "commands.projectgm_b.wand.show");
		return changed;
	}

	private static int group(CommandSourceStack source, Collection<GameProfile> profiles, String input) {
		String group = input.strip();
		if (group.isEmpty() || group.length() > WandRosterState.MAX_GROUP) {
			source.sendFailure(Component.translatable("commands.projectgm_b.wand.group_invalid", WandRosterState.MAX_GROUP));
			return 0;
		}
		WandRosterState roster = WandRosterState.get(source.getServer());
		for (GameProfile profile : profiles) {
			roster.setGroup(profile.getId(), profile.getName(), group);
		}
		if (profiles.size() == 1) {
			String name = profiles.iterator().next().getName();
			source.sendSuccess(() -> Component.translatable("commands.projectgm_b.wand.group_one", name, group), true);
		} else {
			int count = profiles.size();
			source.sendSuccess(() -> Component.translatable("commands.projectgm_b.wand.group_many", count, group), true);
		}
		return profiles.size();
	}

	private static int ungroup(CommandSourceStack source, Collection<GameProfile> profiles) {
		WandRosterState roster = WandRosterState.get(source.getServer());
		int changed = 0;
		for (GameProfile profile : profiles) {
			if (!roster.groupOf(profile.getId()).isEmpty() && roster.setGroup(profile.getId(), profile.getName(), "")) {
				changed++;
			}
		}
		if (changed == 0) {
			source.sendFailure(Component.translatable("commands.projectgm_b.wand.ungroup_none"));
			return 0;
		}
		report(source, profiles, changed, "commands.projectgm_b.wand.ungroup");
		return changed;
	}

	private static void report(CommandSourceStack source, Collection<GameProfile> profiles, int count, String key) {
		if (profiles.size() == 1) {
			String name = profiles.iterator().next().getName();
			source.sendSuccess(() -> Component.translatable(key + "_one", name), true);
		} else {
			source.sendSuccess(() -> Component.translatable(key + "_many", count), true);
		}
	}

	private static int listHidden(CommandSourceStack source) {
		List<String> names = new ArrayList<>();
		for (WandRosterState.Member member : WandRosterState.get(source.getServer()).all().values()) {
			if (member.hidden()) {
				names.add(member.name());
			}
		}
		if (names.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.projectgm_b.wand.hidden_empty"), false);
			return 0;
		}
		names.sort(String.CASE_INSENSITIVE_ORDER);
		Component joined = join(names);
		int count = names.size();
		source.sendSuccess(() -> Component.translatable("commands.projectgm_b.wand.hidden_list", count, joined), false);
		return count;
	}

	private static int listGroups(CommandSourceStack source) {
		Map<String, List<String>> groups = groups(source);
		if (groups.isEmpty()) {
			source.sendSuccess(() -> Component.translatable("commands.projectgm_b.wand.groups_empty"), false);
			return 0;
		}
		for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
			List<String> names = entry.getValue();
			names.sort(String.CASE_INSENSITIVE_ORDER);
			Component joined = join(names);
			source.sendSuccess(() -> Component.translatable("commands.projectgm_b.wand.groups_entry", entry.getKey(),
				names.size(), joined), false);
		}
		return groups.size();
	}

	private static Map<String, List<String>> groups(CommandSourceStack source) {
		Map<String, List<String>> groups = new TreeMap<>();
		for (WandRosterState.Member member : WandRosterState.get(source.getServer()).all().values()) {
			if (!member.group().isEmpty()) {
				groups.computeIfAbsent(member.group(), key -> new ArrayList<>()).add(member.name());
			}
		}
		return groups;
	}

	private static Component join(List<String> names) {
		return ComponentUtils.formatList(names, Component.translatable("commands.projectgm_b.wand.separator"),
			Component::literal);
	}
}
