package cn.erindax.projectgmb.dance;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.music.MusicStore;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

final class OszImporter {

	static final String EXTENSION = ".osz";

	private static final int TAIKO = 1;
	private static final int CATCH = 2;
	private static final int MANIA = 3;
	private static final int KEYS = 4;
	private static final int SLIDER_FLAG = 2;
	private static final int SPINNER_FLAG = 8;
	private static final int HOLD_FLAG = 128;
	private static final int KAT_SOUND = 10;
	private static final int FINISH_SOUND = 4;
	private static final int MIN_LONG = 200;
	private static final int JACK_GAP = 150;
	private static final int LANE_GAP = 30;
	private static final int MAX_NAME = 64;
	private static final int MAX_VERSION = 24;
	private static final int MAX_OSU_BYTES = 8 * 1024 * 1024;
	private static final Pattern INVALID = Pattern.compile("[\\\\/:*?\"<>|\\p{Cntrl}]");

	record Result(List<DanceChart> charts, @Nullable String failure) {
	}

	private record Beatmap(int mode, int keys, String title, String version, String audio, float bpm, int offset,
			List<DanceChart.Note> notes) {
	}

	private record Audio(@Nullable String name, @Nullable String failure) {
	}

	private record Timing(double time, double beat, boolean red) {
	}

	private record Hit(double time, double end, double x, int flags, int sound) {
	}

	private static final class Lanes {

		private static final int[][] ORDER = {{0, 1, 2, 3}, {1, 2, 0, 3}, {2, 1, 3, 0}, {3, 2, 1, 0}};

		private final int[] busy = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
		private int lastLane = -1;
		private int lastTime = Integer.MIN_VALUE;

		void place(List<DanceChart.Note> notes, int preferred, int time, int hold, boolean avoidJack) {
			int[] order = ORDER[Mth.clamp(preferred, 0, KEYS - 1)];
			int lane = pick(order, time, avoidJack);
			if (lane < 0 && avoidJack) {
				lane = pick(order, time, false);
			}
			if (lane < 0) {
				return;
			}
			notes.add(new DanceChart.Note(time, lane, hold));
			busy[lane] = time + hold + LANE_GAP;
			lastLane = lane;
			lastTime = time;
		}

		private int pick(int[] order, int time, boolean avoidJack) {
			for (int lane : order) {
				if (time < busy[lane]) {
					continue;
				}
				if (avoidJack && lane == lastLane && (long) time - lastTime < JACK_GAP) {
					continue;
				}
				return lane;
			}
			return -1;
		}
	}

	private OszImporter() {
	}

	static Result load(Path file, Set<String> taken, Path musicDir) {
		try {
			return read(file, taken, musicDir, StandardCharsets.UTF_8);
		} catch (ZipException | IllegalArgumentException exception) {
			try {
				return read(file, taken, musicDir, Charset.forName("GBK"));
			} catch (IOException | RuntimeException retry) {
				ProjectGmB.LOGGER.warn("Failed to read beatmap archive {}", file, retry);
				return new Result(List.of(), "broken");
			}
		} catch (IOException | RuntimeException exception) {
			ProjectGmB.LOGGER.warn("Failed to read beatmap archive {}", file, exception);
			return new Result(List.of(), "broken");
		}
	}

	private static Result read(Path file, Set<String> taken, Path musicDir, Charset charset) throws IOException {
		String base = stripExtension(file.getFileName().toString());
		try (ZipFile zip = new ZipFile(file.toFile(), charset)) {
			List<ZipEntry> entries = zip.stream().filter(entry -> !entry.isDirectory())
				.map(ZipEntry.class::cast).toList();
			List<Beatmap> maps = new ArrayList<>();
			for (ZipEntry entry : entries) {
				if (!entry.getName().toLowerCase(Locale.ROOT).endsWith(".osu")) {
					continue;
				}
				byte[] data = readLimited(zip, entry, MAX_OSU_BYTES);
				if (data == null) {
					continue;
				}
				Beatmap map = parse(new String(data, StandardCharsets.UTF_8));
				if (!map.notes().isEmpty()) {
					maps.add(map);
				}
			}
			if (maps.isEmpty()) {
				return new Result(List.of(), "empty");
			}
			if (maps.stream().anyMatch(OszImporter::isNative)) {
				maps.removeIf(map -> !isNative(map));
			}
			boolean mixed = maps.stream().map(OszImporter::modeTag).distinct().count() > 1;
			maps.sort(Comparator.comparingInt((Beatmap map) -> map.notes().size()));
			List<String> audioFiles = maps.stream().map(map -> map.audio().toLowerCase(Locale.ROOT)).distinct().toList();
			Map<String, Audio> audios = new HashMap<>();
			List<DanceChart> charts = new ArrayList<>();
			String failure = null;
			for (int i = 0; i < maps.size(); i++) {
				Beatmap map = maps.get(i);
				String key = map.audio().toLowerCase(Locale.ROOT);
				Audio audio = audios.get(key);
				if (audio == null) {
					int index = audioFiles.size() > 1 ? audioFiles.indexOf(key) + 1 : 0;
					audio = extract(zip, entries, map.audio(), base, index, musicDir);
					audios.put(key, audio);
				}
				if (audio.name() == null) {
					failure = audio.failure();
					continue;
				}
				String title = map.title().isBlank() ? base : map.title();
				String version = map.version().isBlank() ? "#" + (i + 1) : map.version();
				if (mixed) {
					version = version + " " + modeTag(map);
				}
				boolean single = maps.size() == 1;
				String display = single ? title : title + " [" + version + "]";
				String name = unique(chartName(title, single ? null : version, base), taken);
				taken.add(name);
				charts.add(new DanceChart(name, display, audio.name(), map.bpm(), map.offset(), 1.0F, 0, map.notes(),
					List.of()));
			}
			return new Result(charts, charts.isEmpty() ? failure : null);
		}
	}

	private static Audio extract(ZipFile zip, List<ZipEntry> entries, String audioFile, String base, int index,
			Path musicDir) throws IOException {
		String wanted = baseName(audioFile).toLowerCase(Locale.ROOT);
		ZipEntry entry = null;
		for (ZipEntry candidate : entries) {
			if (!wanted.isEmpty() && baseName(candidate.getName()).toLowerCase(Locale.ROOT).equals(wanted)) {
				entry = candidate;
				break;
			}
		}
		if (entry == null) {
			return new Audio(null, "no_audio");
		}
		String extension = extensionOf(wanted);
		if (!extension.equals(".mp3") && !extension.equals(".ogg")) {
			return new Audio(null, "audio_format");
		}
		byte[] data = readLimited(zip, entry, MusicStore.MAX_BYTES);
		if (data == null) {
			return new Audio(null, "audio_size");
		}
		String suffix = (index > 0 ? " " + index : "") + extension;
		String head = clean(base, MAX_NAME - suffix.length());
		String name = (head.isEmpty() ? "osu" : head) + suffix;
		if (!MusicStore.isValidName(name) || !MusicStore.looksValid(name, data)) {
			return new Audio(null, "audio_format");
		}
		Path target = musicDir.resolve(name);
		if (!Files.isRegularFile(target) || Files.size(target) != data.length
				|| !Arrays.equals(Files.readAllBytes(target), data)) {
			Files.createDirectories(musicDir);
			Files.write(target, data);
		}
		return new Audio(name, null);
	}

	private static Beatmap parse(String text) {
		Map<String, String> general = new HashMap<>();
		Map<String, String> metadata = new HashMap<>();
		Map<String, String> difficulty = new HashMap<>();
		List<String[]> objects = new ArrayList<>();
		List<Timing> timing = new ArrayList<>();
		String section = "";
		for (String raw : text.split("\\r?\\n")) {
			String line = raw.replace("\uFEFF", "").strip();
			if (line.isEmpty() || line.startsWith("//")) {
				continue;
			}
			if (line.startsWith("[") && line.endsWith("]")) {
				section = line.substring(1, line.length() - 1);
				continue;
			}
			if (section.equals("General")) {
				put(general, line);
			} else if (section.equals("Metadata")) {
				put(metadata, line);
			} else if (section.equals("Difficulty")) {
				put(difficulty, line);
			} else if (section.equals("HitObjects")) {
				objects.add(line.split(","));
			} else if (section.equals("TimingPoints")) {
				String[] parts = line.split(",");
				double time = number(parts[0], Double.NaN);
				double beat = parts.length > 1 ? number(parts[1], Double.NaN) : Double.NaN;
				if (!Double.isNaN(time) && !Double.isNaN(beat) && beat != 0) {
					boolean red = parts.length > 6 ? parts[6].strip().equals("1") : beat > 0;
					timing.add(new Timing(time, beat, red && beat > 0));
				}
			}
		}
		timing.sort(Comparator.comparingDouble(Timing::time));
		int mode = (int) number(general.get("Mode"), 0);
		int keys = (int) Math.round(number(difficulty.get("CircleSize"), 0));
		double multiplier = Math.max(0.1, number(difficulty.get("SliderMultiplier"), 1.4));
		List<Hit> hits = new ArrayList<>();
		for (String[] parts : objects) {
			Hit hit = hit(parts, timing, multiplier);
			if (hit != null) {
				hits.add(hit);
			}
		}
		hits.sort(Comparator.comparingDouble(Hit::time));
		List<DanceChart.Note> notes = mode == MANIA ? mania(hits, keys) : mode == TAIKO ? taiko(hits) : spread(hits);
		String title = metadata.getOrDefault("TitleUnicode", "");
		if (title.isBlank()) {
			title = metadata.getOrDefault("Title", "");
		}
		Timing red = timing.stream().filter(Timing::red).findFirst().orElse(null);
		float bpm = red == null ? 120.0F : Mth.clamp((float) (60000.0 / red.beat()), 20.0F, 400.0F);
		int offset = red == null ? 0 : (int) Math.round(red.time());
		return new Beatmap(mode, keys, title.strip(), metadata.getOrDefault("Version", "").strip(),
			general.getOrDefault("AudioFilename", "").strip(), bpm, offset, DanceCharts.cleanNotes(notes));
	}

	@Nullable
	private static Hit hit(String[] parts, List<Timing> timing, double multiplier) {
		if (parts.length < 4) {
			return null;
		}
		double x = number(parts[0], Double.NaN);
		double time = number(parts[2], Double.NaN);
		double type = number(parts[3], Double.NaN);
		if (Double.isNaN(x) || Double.isNaN(time) || Double.isNaN(type)) {
			return null;
		}
		int flags = (int) type;
		int sound = parts.length > 4 ? (int) number(parts[4], 0) : 0;
		double end = time;
		if ((flags & HOLD_FLAG) != 0 && parts.length > 5) {
			end = number(parts[5].split(":")[0], time);
		} else if ((flags & SPINNER_FLAG) != 0 && parts.length > 5) {
			end = number(parts[5], time);
		} else if ((flags & SLIDER_FLAG) != 0 && parts.length > 7) {
			int slides = Math.max(1, (int) number(parts[6], 1));
			double length = Math.max(0, number(parts[7], 0));
			end = time + sliderDuration(time, length, slides, timing, multiplier);
		}
		return new Hit(time, Math.max(time, end), x, flags, sound);
	}

	private static double sliderDuration(double time, double length, int slides, List<Timing> timing,
			double multiplier) {
		double beat = 500.0;
		double velocity = 1.0;
		boolean found = false;
		for (Timing point : timing) {
			if (point.time() > time + 1) {
				break;
			}
			if (point.red()) {
				beat = point.beat();
				velocity = 1.0;
				found = true;
			} else if (point.beat() < 0) {
				velocity = Mth.clamp(-100.0 / point.beat(), 0.1, 10.0);
			}
		}
		if (!found) {
			for (Timing point : timing) {
				if (point.red()) {
					beat = point.beat();
					break;
				}
			}
		}
		return length / (multiplier * 100.0 * velocity) * beat * slides;
	}

	private static List<DanceChart.Note> mania(List<Hit> hits, int keys) {
		List<DanceChart.Note> notes = new ArrayList<>();
		if (keys <= 0) {
			return notes;
		}
		Lanes lanes = new Lanes();
		for (Hit hit : hits) {
			int column = Mth.clamp((int) Math.floor(hit.x() * keys / 512.0), 0, keys - 1);
			int time = (int) Math.round(hit.time());
			int hold = (hit.flags() & HOLD_FLAG) != 0 ? (int) Math.round(hit.end() - hit.time()) : 0;
			if (keys == KEYS) {
				notes.add(new DanceChart.Note(time, column, hold));
			} else {
				lanes.place(notes, column * KEYS / keys, time, hold, false);
			}
		}
		return notes;
	}

	private static List<DanceChart.Note> taiko(List<Hit> hits) {
		List<DanceChart.Note> notes = new ArrayList<>();
		Lanes lanes = new Lanes();
		boolean[] flip = new boolean[2];
		for (Hit hit : hits) {
			int time = (int) Math.round(hit.time());
			boolean roll = (hit.flags() & (SLIDER_FLAG | SPINNER_FLAG)) != 0;
			int length = (int) Math.round(hit.end() - hit.time());
			int hold = roll && length >= MIN_LONG ? length : 0;
			int kind = !roll && (hit.sound() & KAT_SOUND) != 0 ? 1 : 0;
			int first = kind == 1 ? 0 : 1;
			int second = kind == 1 ? 3 : 2;
			if (!roll && (hit.sound() & FINISH_SOUND) != 0) {
				lanes.place(notes, first, time, 0, false);
				lanes.place(notes, second, time, 0, false);
				continue;
			}
			lanes.place(notes, flip[kind] ? second : first, time, hold, false);
			flip[kind] = !flip[kind];
		}
		return notes;
	}

	private static List<DanceChart.Note> spread(List<Hit> hits) {
		List<DanceChart.Note> notes = new ArrayList<>();
		Lanes lanes = new Lanes();
		for (Hit hit : hits) {
			int time = (int) Math.round(hit.time());
			int length = (int) Math.round(hit.end() - hit.time());
			boolean held = (hit.flags() & (SLIDER_FLAG | SPINNER_FLAG)) != 0 && length >= MIN_LONG;
			int preferred = Mth.clamp((int) Math.floor(hit.x() * KEYS / 512.0), 0, KEYS - 1);
			lanes.place(notes, preferred, time, held ? length : 0, true);
		}
		return notes;
	}

	private static boolean isNative(Beatmap map) {
		return map.mode() == MANIA && map.keys() == KEYS;
	}

	private static String modeTag(Beatmap map) {
		return switch (map.mode()) {
			case TAIKO -> "taiko";
			case CATCH -> "catch";
			case MANIA -> map.keys() + "K";
			default -> "osu";
		};
	}

	private static void put(Map<String, String> target, String line) {
		int index = line.indexOf(':');
		if (index > 0) {
			target.put(line.substring(0, index).strip(), line.substring(index + 1).strip());
		}
	}

	private static double number(@Nullable String text, double fallback) {
		if (text == null) {
			return fallback;
		}
		try {
			double value = Double.parseDouble(text.strip());
			return Double.isFinite(value) ? value : fallback;
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	@Nullable
	private static byte[] readLimited(ZipFile zip, ZipEntry entry, int max) throws IOException {
		if (entry.getSize() > max) {
			return null;
		}
		try (InputStream input = zip.getInputStream(entry)) {
			byte[] data = input.readNBytes(max + 1);
			return data.length > max ? null : data;
		}
	}

	private static String chartName(String title, @Nullable String version, String base) {
		String suffix = version == null ? "" : " [" + clean(version, MAX_VERSION) + "]";
		String name = clean(title, MAX_NAME - suffix.length()) + suffix;
		if (!DanceCharts.isValidName(name)) {
			name = clean(base, MAX_NAME - suffix.length()) + suffix;
		}
		return DanceCharts.isValidName(name) ? name : "osu" + suffix;
	}

	private static String unique(String name, Set<String> taken) {
		if (!taken.contains(name)) {
			return name;
		}
		for (int i = 2; ; i++) {
			String suffix = " (" + i + ")";
			String candidate = clean(name, MAX_NAME - suffix.length()) + suffix;
			if (!taken.contains(candidate)) {
				return candidate;
			}
		}
	}

	static String clean(String text, int max) {
		String value = INVALID.matcher(text).replaceAll("_").strip();
		if (value.length() > max) {
			int end = Math.max(0, max);
			if (end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) {
				end--;
			}
			value = value.substring(0, end).strip();
		}
		while (value.endsWith(".")) {
			value = value.substring(0, value.length() - 1).strip();
		}
		return value;
	}

	private static String baseName(String path) {
		int index = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return path.substring(index + 1).strip();
	}

	private static String extensionOf(String name) {
		int index = name.lastIndexOf('.');
		return index < 0 ? "" : name.substring(index).toLowerCase(Locale.ROOT);
	}

	private static String stripExtension(String name) {
		int index = name.lastIndexOf('.');
		return index <= 0 ? name : name.substring(0, index);
	}
}
