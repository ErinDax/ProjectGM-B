package cn.erindax.projectgmb.client.music;

import cn.erindax.projectgmb.music.net.MusicAckPayload;
import cn.erindax.projectgmb.music.net.MusicDataPayload;
import cn.erindax.projectgmb.music.net.MusicRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class ClientMusic {

	private static final int MAX_CACHED_TRACKS = 12;
	private static final long REQUEST_TIMEOUT_NANOS = 60_000_000_000L;

	private record Cached(int version, byte[] data) {
	}

	private static final class Assembly {
		private final int version;
		private final byte[][] parts;
		private int received;

		private Assembly(int version, int total) {
			this.version = version;
			this.parts = new byte[total][];
		}
	}

	private static final Map<String, Cached> CACHE = new LinkedHashMap<>(16, 0.75F, true);
	private static final Map<String, Assembly> PENDING = new HashMap<>();
	private static final Map<String, Long> REQUESTED = new HashMap<>();

	private ClientMusic() {
	}

	public static boolean has(String track, int version) {
		Cached cached;
		synchronized (CACHE) {
			cached = CACHE.get(track);
		}
		return cached != null && cached.version == version;
	}

	@Nullable
	public static byte[] get(String track) {
		synchronized (CACHE) {
			Cached cached = CACHE.get(track);
			return cached == null ? null : cached.data;
		}
	}

	public static void request(String track, int version) {
		long now = System.nanoTime();
		Long last = REQUESTED.get(track);
		if (last != null && now - last <= REQUEST_TIMEOUT_NANOS) {
			return;
		}
		REQUESTED.put(track, now);
		Minecraft minecraft = Minecraft.getInstance();
		CompletableFuture.supplyAsync(() -> MusicDiskCache.read(track, version), Util.ioPool())
			.thenAcceptAsync(data -> {
				if (data != null) {
					store(track, version, data);
				} else if (minecraft.getConnection() != null) {
					ClientPlayNetworking.send(new MusicRequestPayload(track));
				}
			}, minecraft);
	}

	public static void receive(MusicDataPayload payload) {
		if (payload.total() <= 0 || payload.index() < 0 || payload.index() >= payload.total()) {
			return;
		}
		Assembly assembly = PENDING.get(payload.track());
		if (assembly == null || assembly.version != payload.version() || assembly.parts.length != payload.total()) {
			assembly = new Assembly(payload.version(), payload.total());
			PENDING.put(payload.track(), assembly);
		}
		ClientPlayNetworking.send(new MusicAckPayload(payload.track(), payload.version(), payload.index()));
		if (assembly.parts[payload.index()] == null) {
			assembly.parts[payload.index()] = payload.data();
			assembly.received++;
		}
		if (assembly.received < assembly.parts.length) {
			return;
		}
		int length = 0;
		for (byte[] part : assembly.parts) {
			length += part.length;
		}
		byte[] data = new byte[length];
		int offset = 0;
		for (byte[] part : assembly.parts) {
			System.arraycopy(part, 0, data, offset, part.length);
			offset += part.length;
		}
		PENDING.remove(payload.track());
		String track = payload.track();
		int version = assembly.version;
		if (!Minecraft.getInstance().isLocalServer()) {
			Util.ioPool().execute(() -> MusicDiskCache.write(track, version, data));
		}
		store(track, version, data);
	}

	private static void store(String track, int version, byte[] data) {
		synchronized (CACHE) {
			CACHE.put(track, new Cached(version, data));
			while (CACHE.size() > MAX_CACHED_TRACKS) {
				String eldest = CACHE.keySet().iterator().next();
				if (MusicPlayer.isTrackInUse(eldest)) {
					break;
				}
				CACHE.remove(eldest);
			}
		}
		REQUESTED.remove(track);
		MusicPlayer.onTrackReady(track);
	}

	public static void clear() {
		synchronized (CACHE) {
			CACHE.clear();
		}
		PENDING.clear();
		REQUESTED.clear();
	}
}
