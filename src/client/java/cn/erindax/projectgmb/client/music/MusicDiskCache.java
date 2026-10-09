package cn.erindax.projectgmb.client.music;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.music.MusicStore;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.CRC32;

final class MusicDiskCache {

	private static final long MAX_BYTES = 512L * 1024 * 1024;
	private static final long STALE_PART_MILLIS = 10L * 60 * 1000;
	private static final String PART = ".part";

	private record Item(Path path, long size, long modified) {
	}

	private MusicDiskCache() {
	}

	static Path directory() {
		return FabricLoader.getInstance().getGameDir().resolve("projectgm_b").resolve("cache").resolve("music");
	}

	@Nullable
	static byte[] read(String track, int version) {
		return read(directory(), track, version);
	}

	static void write(String track, int version, byte[] data) {
		write(directory(), track, version, data, MAX_BYTES);
	}

	@Nullable
	static byte[] read(Path dir, String track, int version) {
		Path file = file(dir, track, version);
		if (file == null || !Files.isRegularFile(file)) {
			return null;
		}
		try {
			if (Files.size(file) > MusicStore.MAX_BYTES) {
				return null;
			}
			byte[] data = Files.readAllBytes(file);
			CRC32 crc = new CRC32();
			crc.update(data);
			if ((int) crc.getValue() != version) {
				Files.deleteIfExists(file);
				return null;
			}
			Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
			return data;
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to read cached music {}", file, e);
			return null;
		}
	}

	static void write(Path dir, String track, int version, byte[] data, long maxBytes) {
		Path file = file(dir, track, version);
		if (file == null || data.length > MusicStore.MAX_BYTES) {
			return;
		}
		Path temp = dir.resolve(file.getFileName() + PART);
		try {
			Files.createDirectories(dir);
			Files.write(temp, data);
			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
			trim(dir, file, maxBytes);
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to cache music {}", file, e);
		}
	}

	@Nullable
	private static Path file(Path dir, String track, int version) {
		if (!MusicStore.isValidName(track)) {
			return null;
		}
		return dir.resolve(String.format(Locale.ROOT, "%08x_%s", version, track));
	}

	private static void trim(Path dir, Path keep, long maxBytes) throws IOException {
		List<Item> items = new ArrayList<>();
		long total = 0L;
		long now = System.currentTimeMillis();
		try (Stream<Path> files = Files.list(dir)) {
			for (Path path : files.filter(Files::isRegularFile).toList()) {
				long size;
				long modified;
				try {
					size = Files.size(path);
					modified = Files.getLastModifiedTime(path).toMillis();
				} catch (IOException e) {
					continue;
				}
				if (path.getFileName().toString().endsWith(PART)) {
					if (now - modified > STALE_PART_MILLIS) {
						Files.deleteIfExists(path);
					}
					continue;
				}
				items.add(new Item(path, size, modified));
				total += size;
			}
		}
		items.sort(Comparator.comparingLong(Item::modified));
		for (Item item : items) {
			if (total <= maxBytes) {
				return;
			}
			if (!item.path().equals(keep)) {
				Files.deleteIfExists(item.path());
				total -= item.size();
			}
		}
	}
}
