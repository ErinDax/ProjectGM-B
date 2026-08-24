package cn.erindax.projectgmb.client;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VanishClient {
	private static final Set<UUID> VANISHED = new HashSet<>();

	private VanishClient() {
	}

	public static void replace(Set<UUID> vanished) {
		VANISHED.clear();
		VANISHED.addAll(vanished);
	}

	public static void clear() {
		VANISHED.clear();
	}

	public static boolean isVanished(UUID playerId) {
		return VANISHED.contains(playerId);
	}

	public static boolean hiddenFromOthers(UUID playerId) {
		return isVanished(playerId);
	}
}
