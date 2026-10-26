package cn.erindax.projectgmb.dance;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

public record DanceChart(String name, String title, String audio, float bpm, int offset, float speed, int length,
		List<Note> notes, List<Tone> tones) {

	public static final int LANES = 4;
	public static final List<String> LANE_NAMES = List.of("left", "down", "up", "right");

	public static final int PIANO = 0;
	public static final int LEAD = 1;
	public static final int BASS = 2;
	public static final int PAD = 3;
	public static final int KICK = 4;
	public static final int SNARE = 5;
	public static final int HAT = 6;
	public static final List<String> INSTRUMENTS = List.of("piano", "lead", "bass", "pad", "kick", "snare", "hat");

	public static final int MAX_NOTES = 10000;
	public static final int MAX_TONES = 20000;

	public record Note(int time, int lane, int hold) {

		public static final StreamCodec<ByteBuf, Note> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Note::time,
			ByteBufCodecs.VAR_INT, Note::lane,
			ByteBufCodecs.VAR_INT, Note::hold,
			Note::new);

		public int end() {
			return time + hold;
		}
	}

	public record Tone(int time, int length, int pitch, int instrument, float volume) {

		public static final StreamCodec<ByteBuf, Tone> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Tone::time,
			ByteBufCodecs.VAR_INT, Tone::length,
			ByteBufCodecs.VAR_INT, Tone::pitch,
			ByteBufCodecs.VAR_INT, Tone::instrument,
			ByteBufCodecs.FLOAT, Tone::volume,
			Tone::new);

		public boolean drum() {
			return instrument == KICK || instrument == SNARE || instrument == HAT;
		}
	}

	private static final StreamCodec<ByteBuf, List<Note>> NOTES = Note.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_NOTES));
	private static final StreamCodec<ByteBuf, List<Tone>> TONES = Tone.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TONES));

	public static final StreamCodec<ByteBuf, DanceChart> STREAM_CODEC = StreamCodec.of(DanceChart::write,
		DanceChart::read);

	public DanceChart {
		notes = List.copyOf(notes);
		tones = List.copyOf(tones);
	}

	public float beatMs() {
		return 60000.0F / bpm;
	}

	public int lastNoteEnd() {
		int end = 0;
		for (Note note : notes) {
			end = Math.max(end, note.end());
		}
		return end;
	}

	public int toneEnd() {
		int end = 0;
		for (Tone tone : tones) {
			end = Math.max(end, tone.time() + tone.length());
		}
		return end;
	}

	public int duration() {
		int notesEnd = notes.isEmpty() ? 0 : lastNoteEnd() + 1000;
		int tonesEnd = tones.isEmpty() ? 0 : toneEnd() + 500;
		return Math.max(length, Math.max(notesEnd, tonesEnd));
	}

	public int holdCount() {
		int count = 0;
		for (Note note : notes) {
			if (note.hold() > 0) {
				count++;
			}
		}
		return count;
	}

	private static void write(ByteBuf buf, DanceChart chart) {
		ByteBufCodecs.STRING_UTF8.encode(buf, chart.name);
		ByteBufCodecs.STRING_UTF8.encode(buf, chart.title);
		ByteBufCodecs.STRING_UTF8.encode(buf, chart.audio);
		ByteBufCodecs.FLOAT.encode(buf, chart.bpm);
		ByteBufCodecs.VAR_INT.encode(buf, chart.offset);
		ByteBufCodecs.FLOAT.encode(buf, chart.speed);
		ByteBufCodecs.VAR_INT.encode(buf, chart.length);
		NOTES.encode(buf, chart.notes);
		TONES.encode(buf, chart.tones);
	}

	private static DanceChart read(ByteBuf buf) {
		String name = ByteBufCodecs.STRING_UTF8.decode(buf);
		String title = ByteBufCodecs.STRING_UTF8.decode(buf);
		String audio = ByteBufCodecs.STRING_UTF8.decode(buf);
		float bpm = ByteBufCodecs.FLOAT.decode(buf);
		int offset = ByteBufCodecs.VAR_INT.decode(buf);
		float speed = ByteBufCodecs.FLOAT.decode(buf);
		int length = ByteBufCodecs.VAR_INT.decode(buf);
		List<Note> notes = NOTES.decode(buf);
		List<Tone> tones = TONES.decode(buf);
		return new DanceChart(name, title, audio, bpm, offset, speed, length, notes, tones);
	}
}
