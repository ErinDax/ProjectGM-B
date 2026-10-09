package cn.erindax.projectgmb.music;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.music.net.MusicAckPayload;
import cn.erindax.projectgmb.music.net.MusicDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.CRC32;

public final class MusicStore {

	public static final int MAX_BYTES = 24 * 1024 * 1024;
	public static final int CHUNK_BYTES = 32 * 1024;

	private static final int WINDOW = 2;
	private static final int MAX_IN_FLIGHT = 3;
	private static final int MAX_QUEUED = 4;
	private static final long ACK_TIMEOUT_NANOS = 15_000_000_000L;
	private static final long CACHE_BYTES = 128L * 1024 * 1024;

	private static final Pattern NAME = Pattern.compile("[^\\\\/:*?\"<>|\\p{Cntrl}]{1,64}");
	private static final Map<String, Entry> CACHE = new LinkedHashMap<>(16, 0.75F, true);
	private static final ChunkScheduler<UUID> DOWNLOADS =
		new ChunkScheduler<>(CHUNK_BYTES, WINDOW, MAX_IN_FLIGHT, MAX_QUEUED, ACK_TIMEOUT_NANOS);
	private static long cachedBytes;

	public record Entry(byte[] data, int version, long modified, long size) {
	}

	private MusicStore() {
	}

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("projectgm_b").resolve("music");
	}

	public static boolean isSupported(String name) {
		String lower = name.toLowerCase(Locale.ROOT);
		return lower.endsWith(".ogg") || lower.endsWith(".mp3");
	}

	public static boolean isValidName(String name) {
		return NAME.matcher(name).matches() && !name.equals(".") && !name.equals("..") && isSupported(name);
	}

	public static boolean looksValid(String name, byte[] data) {
		if (data.length < 4 || data.length > MAX_BYTES) {
			return false;
		}
		if (name.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
			return data[0] == 'O' && data[1] == 'g' && data[2] == 'g' && data[3] == 'S';
		}
		boolean id3 = data[0] == 'I' && data[1] == 'D' && data[2] == '3';
		boolean sync = (data[0] & 0xFF) == 0xFF && (data[1] & 0xE0) == 0xE0;
		return id3 || sync;
	}

	public static void save(String name, byte[] data) throws IOException {
		Path dir = directory();
		Files.createDirectories(dir);
		Path target = dir.resolve(name);
		Path temp = dir.resolve(name + ".part");
		Files.write(temp, data);
		try {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		}
		uncache(name);
	}

	public static List<String> listAvailable() {
		Path dir = directory();
		if (!Files.isDirectory(dir)) {
			return List.of();
		}
		try (Stream<Path> files = Files.list(dir)) {
			return files
				.filter(Files::isRegularFile)
				.map(p -> p.getFileName().toString())
				.filter(MusicStore::isValidName)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to list music in {}", dir, e);
			return List.of();
		}
	}

	public static boolean exists(String name) {
		if (!isValidName(name)) {
			return false;
		}
		Path file = directory().resolve(name);
		try {
			return Files.isRegularFile(file) && Files.size(file) <= MAX_BYTES;
		} catch (IOException e) {
			return false;
		}
	}

	@Nullable
	public static Entry load(String name) {
		if (!isValidName(name)) {
			return null;
		}
		Path file = directory().resolve(name);
		try {
			if (!Files.isRegularFile(file)) {
				uncache(name);
				return null;
			}
			BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			long modified = attributes.lastModifiedTime().toMillis();
			long size = attributes.size();
			if (size > MAX_BYTES) {
				ProjectGmB.LOGGER.warn("Music file {} is larger than {} bytes and will be ignored", file, MAX_BYTES);
				return null;
			}
			Entry cached = cached(name);
			if (cached != null && cached.modified() == modified && cached.size() == size) {
				return cached;
			}
			byte[] data = Files.readAllBytes(file);
			CRC32 crc = new CRC32();
			crc.update(data);
			Entry entry = new Entry(data, (int) crc.getValue(), modified, size);
			cache(name, entry);
			return entry;
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to read music {}", file, e);
			return null;
		}
	}

	public static void sendTo(ServerPlayer player, String name) {
		Entry entry = load(name);
		if (entry != null && DOWNLOADS.offer(player.getUUID(), name, entry.version(), entry.data())) {
			DOWNLOADS.pump(System.nanoTime(), sink(player.server));
		}
	}

	public static void onAck(ServerPlayer player, MusicAckPayload payload) {
		DOWNLOADS.ack(player.getUUID(), payload.track(), payload.version(), payload.index(), System.nanoTime(),
			sink(player.server));
	}

	public static void dropQueue(UUID player) {
		DOWNLOADS.drop(player);
	}

	public static void tick(MinecraftServer server) {
		if (!DOWNLOADS.isEmpty()) {
			DOWNLOADS.tick(System.nanoTime(), id -> server.getPlayerList().getPlayer(id) != null, sink(server));
		}
	}

	private static ChunkScheduler.Sink<UUID> sink(MinecraftServer server) {
		return (id, name, version, index, total, chunk) -> {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player == null) {
				return false;
			}
			ServerPlayNetworking.send(player, new MusicDataPayload(name, version, index, total, chunk));
			return true;
		};
	}

	public static void ensureDirectory() {
		try {
			Files.createDirectories(directory());
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to create music directory {}", directory(), e);
		}
	}

	@Nullable
	private static synchronized Entry cached(String name) {
		return CACHE.get(name);
	}

	private static synchronized void cache(String name, Entry entry) {
		Entry old = CACHE.put(name, entry);
		if (old != null) {
			cachedBytes -= old.data().length;
		}
		cachedBytes += entry.data().length;
		Iterator<Map.Entry<String, Entry>> it = CACHE.entrySet().iterator();
		while (cachedBytes > CACHE_BYTES && it.hasNext()) {
			Map.Entry<String, Entry> eldest = it.next();
			if (!eldest.getKey().equals(name)) {
				cachedBytes -= eldest.getValue().data().length;
				it.remove();
			}
		}
	}

	private static synchronized void uncache(String name) {
		Entry old = CACHE.remove(name);
		if (old != null) {
			cachedBytes -= old.data().length;
		}
	}
}
