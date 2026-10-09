package cn.erindax.projectgmb.skin;

import cn.erindax.projectgmb.ProjectGmB;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class TextureStore {

	public static final TextureStore KEYS = new TextureStore("keys");

	public static final int MAX_BYTES = 512 * 1024;
	public static final int MAX_NAME = 32;
	public static final int MIN_KEY_SIZE = 8;
	public static final int MAX_KEY_SIZE = 128;

	private static final String EXTENSION = ".png";
	private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{N}_\\-]([\\p{L}\\p{N}_\\- ]{0,30}[\\p{L}\\p{N}_\\-])?");
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
	private static final int HEADER_BYTES = 24;

	public record Problem(String file, String reason, int width, int height) {
	}

	public record Scan(List<String> names, List<Problem> problems) {
	}

	private record Cached(byte[] data, long modified, long size) {
	}

	private final String kind;
	private final Map<String, Cached> cache = new ConcurrentHashMap<>();
	private final Set<String> warned = ConcurrentHashMap.newKeySet();

	private TextureStore(String kind) {
		this.kind = kind;
	}

	public String kind() {
		return kind;
	}

	public Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("projectgm_b").resolve(kind);
	}

	public static boolean isValidName(String name) {
		return name.length() <= MAX_NAME && NAME.matcher(name).matches();
	}

	public boolean validSize(int width, int height) {
		return width == height && width >= MIN_KEY_SIZE && width <= MAX_KEY_SIZE;
	}

	@Nullable
	public byte[] load(String name, boolean refresh) {
		return load(directory(), name, refresh);
	}

	@Nullable
	byte[] load(Path dir, String name, boolean refresh) {
		if (!isValidName(name)) {
			return null;
		}
		Path file = find(dir, name);
		if (file == null) {
			cache.remove(name);
			return null;
		}
		try {
			BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
			long modified = attributes.lastModifiedTime().toMillis();
			long size = attributes.size();
			Cached cached = cache.get(name);
			if (!refresh && cached != null && cached.modified() == modified && cached.size() == size) {
				return cached.data();
			}
			if (size > MAX_BYTES) {
				cache.remove(name);
				return null;
			}
			byte[] data = Files.readAllBytes(file);
			int[] dimensions = dimensions(data);
			if (dimensions == null || !validSize(dimensions[0], dimensions[1])) {
				cache.remove(name);
				return null;
			}
			cache.put(name, new Cached(data, modified, size));
			return data;
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to read texture {}", file, e);
			return null;
		}
	}

	public void sendTo(ServerPlayer player, String name) {
		byte[] data = load(name, false);
		if (data != null) {
			ServerPlayNetworking.send(player, new TexturePayload(kind, name, data));
		}
	}

	public void sendToAll(Iterable<ServerPlayer> players, String name) {
		byte[] data = load(name, false);
		if (data == null) {
			return;
		}
		TexturePayload payload = new TexturePayload(kind, name, data);
		for (ServerPlayer player : players) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public void sendAllTo(ServerPlayer player) {
		for (String name : listAvailable()) {
			sendTo(player, name);
		}
	}

	public List<String> listAvailable() {
		Scan scan = scan();
		for (Problem problem : scan.problems()) {
			if (warned.add(problem.file() + "|" + problem.reason())) {
				ProjectGmB.LOGGER.warn("Skipped {} texture {}: {} ({}x{})", kind, problem.file(), problem.reason(),
					problem.width(), problem.height());
			}
		}
		return scan.names();
	}

	public Scan scan() {
		return scan(directory());
	}

	Scan scan(Path dir) {
		if (!Files.isDirectory(dir)) {
			return new Scan(List.of(), List.of());
		}
		Map<String, String> names = new TreeMap<>();
		List<Problem> problems = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		try (Stream<Path> files = Files.list(dir)) {
			for (Path path : files.sorted().toList()) {
				String file = path.getFileName().toString();
				if (!Files.isRegularFile(path) || !file.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
					continue;
				}
				String name = file.substring(0, file.length() - EXTENSION.length());
				String reason = check(path, name);
				if (reason == null && !seen.add(name.toLowerCase(Locale.ROOT))) {
					reason = "duplicate";
				}
				if (reason != null) {
					int[] size = reason.equals("size") ? dimensions(readHeader(path)) : null;
					problems.add(new Problem(file, reason, size == null ? 0 : size[0], size == null ? 0 : size[1]));
				} else {
					names.put(name, file);
				}
			}
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to list textures in {}", dir, e);
		}
		return new Scan(List.copyOf(names.keySet()), List.copyOf(problems));
	}

	public void refresh() {
		cache.clear();
		warned.clear();
	}

	public void ensureDirectory() {
		try {
			Files.createDirectories(directory());
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to create texture directory {}", directory(), e);
		}
	}

	@Nullable
	private String check(Path path, String name) {
		if (!isValidName(name)) {
			return "name";
		}
		try {
			if (Files.size(path) > MAX_BYTES) {
				return "too_large";
			}
		} catch (IOException e) {
			return "unreadable";
		}
		int[] size = dimensions(readHeader(path));
		if (size == null) {
			return "not_png";
		}
		return validSize(size[0], size[1]) ? null : "size";
	}

	@Nullable
	private static Path find(Path dir, String name) {
		Path exact = dir.resolve(name + EXTENSION);
		if (Files.isRegularFile(exact)) {
			return exact;
		}
		if (!Files.isDirectory(dir)) {
			return null;
		}
		String wanted = (name + EXTENSION).toLowerCase(Locale.ROOT);
		try (Stream<Path> files = Files.list(dir)) {
			return files.filter(Files::isRegularFile)
				.filter(p -> {
					String file = p.getFileName().toString();
					return file.toLowerCase(Locale.ROOT).equals(wanted)
						&& file.substring(0, file.length() - EXTENSION.length()).equals(name);
				})
				.findFirst()
				.orElse(null);
		} catch (IOException e) {
			return null;
		}
	}

	private static byte[] readHeader(Path path) {
		try (InputStream in = Files.newInputStream(path)) {
			return in.readNBytes(HEADER_BYTES);
		} catch (IOException e) {
			return new byte[0];
		}
	}

	@Nullable
	private static int[] dimensions(byte[] data) {
		if (data.length < HEADER_BYTES || !Arrays.equals(Arrays.copyOf(data, PNG_MAGIC.length), PNG_MAGIC)) {
			return null;
		}
		return new int[] {readInt(data, 16), readInt(data, 20)};
	}

	private static int readInt(byte[] data, int offset) {
		return (data[offset] & 0xFF) << 24 | (data[offset + 1] & 0xFF) << 16 | (data[offset + 2] & 0xFF) << 8
			| data[offset + 3] & 0xFF;
	}
}
