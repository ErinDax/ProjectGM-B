package cn.erindax.projectgmb.dance;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.music.MusicStore;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class DanceCharts {

	public static final String SAMPLE = "小星星";

	private static final String EXTENSION = ".json";
	private static final int MIN_GAP = 30;
	private static final int MIN_HOLD = 80;
	private static final Pattern NAME = Pattern.compile("[^\\\\/:*?\"<>|\\p{Cntrl}]{1,64}");
	private static final Pattern PITCH = Pattern.compile("([A-Ga-g])([#b]?)(-?\\d)");
	private static final int[] SEMITONES = {9, 11, 0, 2, 4, 5, 7};
	private static final String[] PITCH_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

	private static final Map<String, DanceChart> CHARTS = new ConcurrentHashMap<>();
	private static volatile List<Failure> failed = List.of();

	public record Failure(String file, String reason) {
	}

	private DanceCharts() {
	}

	public static Path directory() {
		return FabricLoader.getInstance().getConfigDir().resolve("projectgm_b").resolve("dance");
	}

	public static synchronized int reload() {
		Map<String, DanceChart> loaded = new LinkedHashMap<>();
		List<Failure> broken = new ArrayList<>();
		DanceChart sample = TwinkleStar.chart();
		loaded.put(sample.name(), sample);
		Path dir = directory();
		try {
			if (!Files.isDirectory(dir)) {
				Files.createDirectories(dir);
				Files.writeString(dir.resolve(SAMPLE + EXTENSION), toJson(sample), StandardCharsets.UTF_8);
			}
		} catch (IOException exception) {
			ProjectGmB.LOGGER.warn("Failed to prepare dance directory {}", dir, exception);
		}
		if (Files.isDirectory(dir)) {
			try (Stream<Path> files = Files.list(dir)) {
				List<Path> all = files.filter(Files::isRegularFile).sorted().toList();
				for (Path file : all) {
					String fileName = file.getFileName().toString();
					if (!fileName.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
						continue;
					}
					String name = fileName.substring(0, fileName.length() - EXTENSION.length());
					if (!isValidName(name)) {
						broken.add(new Failure(fileName, "name"));
						continue;
					}
					try {
						JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
							.getAsJsonObject();
						loaded.put(name, parse(name, json));
					} catch (IOException | RuntimeException exception) {
						broken.add(new Failure(fileName, "format"));
						ProjectGmB.LOGGER.warn("Failed to load dance chart {}", file, exception);
					}
				}
				Set<String> taken = new HashSet<>(loaded.keySet());
				for (Path file : all) {
					String fileName = file.getFileName().toString();
					if (!fileName.toLowerCase(Locale.ROOT).endsWith(OszImporter.EXTENSION)) {
						continue;
					}
					OszImporter.Result result = OszImporter.load(file, taken, MusicStore.directory());
					for (DanceChart chart : result.charts()) {
						loaded.put(chart.name(), chart);
					}
					if (result.failure() != null) {
						broken.add(new Failure(fileName, result.failure()));
					}
				}
			} catch (IOException exception) {
				ProjectGmB.LOGGER.warn("Failed to list dance charts in {}", dir, exception);
			}
		}
		CHARTS.clear();
		CHARTS.putAll(loaded);
		failed = List.copyOf(broken);
		return loaded.size();
	}

	public static List<String> names() {
		return CHARTS.keySet().stream().sorted(Comparator.comparing((String name) -> !SAMPLE.equals(name))
			.thenComparing(String.CASE_INSENSITIVE_ORDER)).toList();
	}

	public static List<Failure> failed() {
		return failed;
	}

	@Nullable
	public static DanceChart get(String name) {
		return CHARTS.get(name);
	}

	public static boolean isValidName(String name) {
		return NAME.matcher(name).matches() && !name.isBlank() && name.equals(name.strip())
			&& !name.equals(".") && !name.equals("..") && !name.endsWith(".");
	}

	static List<DanceChart.Note> cleanNotes(List<DanceChart.Note> source) {
		List<DanceChart.Note> sorted = new ArrayList<>();
		for (DanceChart.Note note : source) {
			if (note.time() >= 0 && note.lane() >= 0 && note.lane() < DanceChart.LANES) {
				int hold = note.hold() >= MIN_HOLD ? note.hold() : 0;
				sorted.add(new DanceChart.Note(note.time(), note.lane(), hold));
			}
		}
		sorted.sort(Comparator.comparingInt(DanceChart.Note::time).thenComparingInt(DanceChart.Note::lane));
		int[] free = new int[DanceChart.LANES];
		for (int lane = 0; lane < free.length; lane++) {
			free[lane] = Integer.MIN_VALUE;
		}
		List<DanceChart.Note> result = new ArrayList<>();
		for (DanceChart.Note note : sorted) {
			if (note.time() < free[note.lane()]) {
				continue;
			}
			result.add(note);
			free[note.lane()] = note.end() + MIN_GAP;
			if (result.size() >= DanceChart.MAX_NOTES) {
				break;
			}
		}
		return result;
	}

	static DanceChart parse(String name, JsonObject json) {
		String title = GsonHelper.getAsString(json, "title", name);
		String audio = GsonHelper.getAsString(json, "audio", "");
		if (!audio.isEmpty() && !MusicStore.isValidName(audio)) {
			throw new JsonParseException("Invalid audio file name " + audio);
		}
		float bpm = Mth.clamp(GsonHelper.getAsFloat(json, "bpm", 120.0F), 20.0F, 400.0F);
		int offset = GsonHelper.getAsInt(json, "offset", 0);
		float speed = Mth.clamp(GsonHelper.getAsFloat(json, "speed", 1.0F), 0.25F, 4.0F);
		int length = Math.max(0, GsonHelper.getAsInt(json, "length", 0));
		float beatMs = 60000.0F / bpm;

		List<DanceChart.Note> notes = new ArrayList<>();
		for (JsonElement element : GsonHelper.getAsJsonArray(json, "notes", new JsonArray())) {
			JsonObject note = GsonHelper.convertToJsonObject(element, "note");
			boolean beats = note.has("beat");
			double start = beats ? offset + GsonHelper.getAsFloat(note, "beat") * beatMs : GsonHelper.getAsFloat(note, "time");
			double hold = GsonHelper.getAsFloat(note, "hold", 0.0F) * (beats ? beatMs : 1.0F);
			notes.add(new DanceChart.Note((int) Math.round(start), lane(note.get("lane")),
				Math.max(0, (int) Math.round(hold))));
		}

		List<DanceChart.Tone> tones = new ArrayList<>();
		for (JsonElement element : GsonHelper.getAsJsonArray(json, "synth", new JsonArray())) {
			JsonObject tone = GsonHelper.convertToJsonObject(element, "synth");
			boolean beats = tone.has("beat");
			double start = beats ? offset + GsonHelper.getAsFloat(tone, "beat") * beatMs : GsonHelper.getAsFloat(tone, "time");
			double len = beats ? GsonHelper.getAsFloat(tone, "len", 1.0F) * beatMs : GsonHelper.getAsFloat(tone, "len", 300.0F);
			int instrument = instrument(GsonHelper.getAsString(tone, "inst", "piano"));
			int pitch = tone.has("pitch") ? pitch(tone.get("pitch")) : 60;
			float volume = Mth.clamp(GsonHelper.getAsFloat(tone, "vol", 0.6F), 0.0F, 2.0F);
			if (start >= 0 && len > 0 && tones.size() < DanceChart.MAX_TONES) {
				tones.add(new DanceChart.Tone((int) Math.round(start), (int) Math.round(len), pitch, instrument, volume));
			}
		}
		tones.sort(Comparator.comparingInt(DanceChart.Tone::time));
		return new DanceChart(name, title, audio, bpm, offset, speed, length, cleanNotes(notes), tones);
	}

	static String toJson(DanceChart chart) {
		StringBuilder out = new StringBuilder("{\n");
		out.append("  \"title\": ").append(quote(chart.title())).append(",\n");
		out.append("  \"audio\": ").append(quote(chart.audio())).append(",\n");
		out.append("  \"bpm\": ").append(number(chart.bpm())).append(",\n");
		out.append("  \"offset\": ").append(chart.offset()).append(",\n");
		out.append("  \"speed\": ").append(number(chart.speed())).append(",\n");
		if (chart.length() > 0) {
			out.append("  \"length\": ").append(chart.length()).append(",\n");
		}
		float beatMs = chart.beatMs();
		out.append("  \"notes\": [");
		List<DanceChart.Note> notes = chart.notes();
		for (int i = 0; i < notes.size(); i++) {
			DanceChart.Note note = notes.get(i);
			out.append(i == 0 ? "\n" : ",\n").append("    {");
			out.append("\"beat\": ").append(number((note.time() - chart.offset()) / beatMs));
			out.append(", \"lane\": ").append(quote(DanceChart.LANE_NAMES.get(note.lane())));
			if (note.hold() > 0) {
				out.append(", \"hold\": ").append(number(note.hold() / beatMs));
			}
			out.append('}');
		}
		out.append(notes.isEmpty() ? "]" : "\n  ]");
		List<DanceChart.Tone> tones = chart.tones();
		if (!tones.isEmpty()) {
			out.append(",\n  \"synth\": [");
			for (int i = 0; i < tones.size(); i++) {
				DanceChart.Tone tone = tones.get(i);
				out.append(i == 0 ? "\n" : ",\n").append("    {");
				out.append("\"beat\": ").append(number((tone.time() - chart.offset()) / beatMs));
				out.append(", \"len\": ").append(number(tone.length() / beatMs));
				out.append(", \"inst\": ").append(quote(DanceChart.INSTRUMENTS.get(tone.instrument())));
				if (!tone.drum()) {
					out.append(", \"pitch\": ").append(quote(pitchName(tone.pitch())));
				}
				out.append(", \"vol\": ").append(number(tone.volume()));
				out.append('}');
			}
			out.append("\n  ]");
		}
		out.append("\n}\n");
		return out.toString();
	}

	private static int lane(@Nullable JsonElement element) {
		if (element == null || !element.isJsonPrimitive()) {
			throw new JsonParseException("Missing lane");
		}
		JsonPrimitive primitive = element.getAsJsonPrimitive();
		int lane = primitive.isNumber() ? primitive.getAsInt()
			: DanceChart.LANE_NAMES.indexOf(primitive.getAsString().toLowerCase(Locale.ROOT));
		if (lane < 0 || lane >= DanceChart.LANES) {
			throw new JsonParseException("Invalid lane " + primitive);
		}
		return lane;
	}

	private static int instrument(String name) {
		int index = DanceChart.INSTRUMENTS.indexOf(name.toLowerCase(Locale.ROOT));
		if (index < 0) {
			throw new JsonParseException("Unknown instrument " + name);
		}
		return index;
	}

	private static int pitch(JsonElement element) {
		JsonPrimitive primitive = element.getAsJsonPrimitive();
		if (primitive.isNumber()) {
			return Mth.clamp(primitive.getAsInt(), 0, 127);
		}
		Matcher matcher = PITCH.matcher(primitive.getAsString().strip());
		if (!matcher.matches()) {
			throw new JsonParseException("Invalid pitch " + primitive);
		}
		int semitone = SEMITONES[Character.toUpperCase(matcher.group(1).charAt(0)) - 'A'];
		if ("#".equals(matcher.group(2))) {
			semitone++;
		} else if ("b".equals(matcher.group(2))) {
			semitone--;
		}
		int octave = Integer.parseInt(matcher.group(3));
		return Mth.clamp((octave + 1) * 12 + semitone, 0, 127);
	}

	private static String pitchName(int pitch) {
		return PITCH_NAMES[Math.floorMod(pitch, 12)] + (Math.floorDiv(pitch, 12) - 1);
	}

	private static String quote(String value) {
		return new JsonPrimitive(value).toString();
	}

	private static String number(double value) {
		return BigDecimal.valueOf(Math.round(value * 1000.0) / 1000.0).stripTrailingZeros().toPlainString();
	}
}
