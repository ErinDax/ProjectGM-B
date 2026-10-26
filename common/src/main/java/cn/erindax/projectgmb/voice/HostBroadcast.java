package cn.erindax.projectgmb.voice;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HostBroadcast {
	private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

	private HostBroadcast() {
	}

	public static void setActive(UUID playerId, boolean active) {
		if (active) {
			ACTIVE.add(playerId);
		} else {
			ACTIVE.remove(playerId);
			HostVoicePlugin.closeChannel(playerId);
		}
	}

	public static boolean isActive(UUID playerId) {
		return ACTIVE.contains(playerId);
	}

	public static void remove(UUID playerId) {
		ACTIVE.remove(playerId);
		HostVoicePlugin.closeChannel(playerId);
	}

	public static void clear() {
		ACTIVE.clear();
		HostVoicePlugin.closeAllChannels();
	}
}
