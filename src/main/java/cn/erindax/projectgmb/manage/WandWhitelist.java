package cn.erindax.projectgmb.manage;

import cn.erindax.projectgmb.ProjectGmB;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class WandWhitelist {

	private static final String FILE_NAME = "wand_whitelist.txt";

	private static volatile Set<String> names = Set.of();

	private WandWhitelist() {
	}

	public static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("projectgm_b").resolve(FILE_NAME);
	}

	public static boolean isAllowed(Player player) {
		return names.contains(player.getGameProfile().getName().toLowerCase(Locale.ROOT));
	}

	public static boolean canManage(Player player) {
		return isAllowed(player) || player.hasPermissions(2);
	}

	public static int size() {
		return names.size();
	}

	public static boolean reload() {
		Path path = file();
		try {
			if (!Files.exists(path)) {
				Files.createDirectories(path.getParent());
				Files.write(path, List.of(), StandardCharsets.UTF_8);
			}
			Set<String> loaded = new TreeSet<>();
			for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
				String trimmed = line.strip();
				if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
					loaded.add(trimmed.toLowerCase(Locale.ROOT));
				}
			}
			names = Set.copyOf(loaded);
			return true;
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to read wand whitelist {}", path, e);
			return false;
		}
	}
}
