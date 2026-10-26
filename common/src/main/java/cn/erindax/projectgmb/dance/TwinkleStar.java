package cn.erindax.projectgmb.dance;

import java.util.ArrayList;
import java.util.List;

final class TwinkleStar {

	private static final float BPM = 100.0F;
	private static final int BEAT = 600;
	private static final int INTRO = 4;
	private static final int PHRASE = 8;

	private static final int[][] MELODY = {
		{72, 72, 79, 79, 81, 81, 79},
		{77, 77, 76, 76, 74, 74, 72},
		{79, 79, 77, 77, 76, 76, 74},
		{79, 79, 77, 77, 76, 76, 74},
		{72, 72, 79, 79, 81, 81, 79},
		{77, 77, 76, 76, 74, 74, 72}
	};

	private static final int[][] LANES = {
		{0, 0, 3, 3, 2, 2, 3},
		{3, 3, 2, 2, 1, 1, 0},
		{0, 0, 1, 1, 2, 2, 3},
		{3, 3, 2, 2, 1, 1, 0},
		{0, 0, 2, 2, 3, 3, 2},
		{3, 3, 2, 2, 1, 1, 0}
	};

	private static final String[] CHORDS = {"CCFC", "FCGC", "CFCG", "CFCG", "CCFC", "FCGC"};

	private TwinkleStar() {
	}

	static DanceChart chart() {
		List<DanceChart.Note> notes = new ArrayList<>();
		List<DanceChart.Tone> tones = new ArrayList<>();
		for (int phrase = 0; phrase < MELODY.length; phrase++) {
			int start = INTRO + phrase * PHRASE;
			for (int step = 0; step < MELODY[phrase].length; step++) {
				boolean last = step == MELODY[phrase].length - 1;
				int beat = start + step;
				int pitch = MELODY[phrase][step];
				notes.add(new DanceChart.Note(beat * BEAT, LANES[phrase][step], last ? BEAT : 0));
				tones.add(tone(beat, last ? 1.9 : 0.95, pitch, DanceChart.PIANO, 0.5F));
				if (phrase >= 4) {
					tones.add(tone(beat, last ? 1.9 : 0.9, pitch - 12, DanceChart.LEAD, 0.14F));
				}
			}
			for (int half = 0; half < 4; half++) {
				int beat = start + half * 2;
				char chord = CHORDS[phrase].charAt(half);
				for (int pitch : chord(chord)) {
					tones.add(tone(beat, 2.0, pitch, DanceChart.PAD, 0.09F));
				}
				tones.add(tone(beat, 0.9, root(chord), DanceChart.BASS, 0.36F));
				tones.add(tone(beat + 1, 0.9, fifth(chord), DanceChart.BASS, 0.3F));
			}
		}
		int end = INTRO + MELODY.length * PHRASE;
		for (int beat = 0; beat < end; beat++) {
			boolean down = beat % 2 == 0;
			tones.add(tone(beat, 0.5, 36, down ? DanceChart.KICK : DanceChart.SNARE, down ? 0.75F : 0.42F));
			tones.add(tone(beat, 0.2, 0, DanceChart.HAT, 0.09F));
			tones.add(tone(beat + 0.5, 0.2, 0, DanceChart.HAT, 0.13F));
		}
		tones.add(tone(end, 0.5, 36, DanceChart.KICK, 0.75F));
		for (int pitch : chord('C')) {
			tones.add(tone(end, 4.0, pitch, DanceChart.PAD, 0.1F));
		}
		tones.add(tone(end, 3.0, 36, DanceChart.BASS, 0.36F));
		tones.add(tone(end, 3.0, 72, DanceChart.PIANO, 0.42F));
		tones.add(tone(end, 3.0, 67, DanceChart.PIANO, 0.3F));
		tones.add(tone(end, 3.0, 64, DanceChart.PIANO, 0.3F));
		tones.sort((left, right) -> Integer.compare(left.time(), right.time()));
		return new DanceChart(DanceCharts.SAMPLE, "小星星", "", BPM, 0, 0.9F, 0, notes, tones);
	}

	private static DanceChart.Tone tone(double beat, double beats, int pitch, int instrument, float volume) {
		return new DanceChart.Tone((int) Math.round(beat * BEAT), (int) Math.round(beats * BEAT), pitch, instrument,
			volume);
	}

	private static int[] chord(char name) {
		return switch (name) {
			case 'F' -> new int[] {60, 65, 69};
			case 'G' -> new int[] {59, 62, 67};
			default -> new int[] {60, 64, 67};
		};
	}

	private static int root(char name) {
		return switch (name) {
			case 'F' -> 41;
			case 'G' -> 43;
			default -> 36;
		};
	}

	private static int fifth(char name) {
		return switch (name) {
			case 'F' -> 48;
			case 'G' -> 50;
			default -> 43;
		};
	}
}
