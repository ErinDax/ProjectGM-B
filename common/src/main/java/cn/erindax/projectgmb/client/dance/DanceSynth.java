package cn.erindax.projectgmb.client.dance;

import cn.erindax.projectgmb.dance.DanceChart;

import java.util.List;

public final class DanceSynth {

	public static final int RATE = 44100;

	private static final int MAX_SECONDS = 360;
	private static final int TABLE_SIZE = 8192;
	private static final float[] SINE = new float[TABLE_SIZE];

	private static final float[] PIANO = {1.0F, 0.5F, 0.28F, 0.14F, 0.08F, 0.04F};
	private static final float[] LEAD = {1.0F, 0.0F, 0.33F, 0.0F, 0.2F, 0.0F, 0.14F};
	private static final float[] BASS = {1.0F, 0.45F, 0.2F, 0.08F};
	private static final float[] PAD = {1.0F, 0.5F, 0.33F, 0.25F, 0.2F};

	static {
		for (int i = 0; i < TABLE_SIZE; i++) {
			SINE[i] = (float) Math.sin(2.0 * Math.PI * i / TABLE_SIZE);
		}
	}

	private DanceSynth() {
	}

	public static short[] render(List<DanceChart.Tone> tones) {
		long endMs = 0;
		for (DanceChart.Tone tone : tones) {
			endMs = Math.max(endMs, (long) tone.time() + tone.length() + 400L);
		}
		int total = (int) Math.min((endMs + 300L) * RATE / 1000L, (long) MAX_SECONDS * RATE);
		float[] mix = new float[Math.max(total, 1)];
		Noise noise = new Noise(0x5DEECE66DL);
		for (DanceChart.Tone tone : tones) {
			if (tone.time() < 0 || tone.length() <= 0) {
				continue;
			}
			int start = (int) ((long) tone.time() * RATE / 1000L);
			int length = (int) ((long) tone.length() * RATE / 1000L);
			if (start >= mix.length) {
				continue;
			}
			double freq = 440.0 * Math.pow(2.0, (tone.pitch() - 69) / 12.0);
			float volume = tone.volume();
			switch (tone.instrument()) {
				case DanceChart.PIANO -> voice(mix, start, length, freq, volume, PIANO, 0.004, 0.45, 0.05, 0.12, 0.0015,
					0.0, 1.6);
				case DanceChart.LEAD -> voice(mix, start, length, freq, volume, LEAD, 0.01, 0.3, 0.7, 0.08, 0.0, 0.004,
					0.0);
				case DanceChart.BASS -> voice(mix, start, length, freq, volume, BASS, 0.004, 0.18, 0.45, 0.06, 0.0, 0.0,
					0.8);
				case DanceChart.PAD -> voice(mix, start, length, freq, volume, PAD, 0.08, 0.6, 0.6, 0.25, 0.004, 0.0,
					0.0);
				case DanceChart.KICK -> kick(mix, start, volume, noise);
				case DanceChart.SNARE -> snare(mix, start, volume, noise);
				case DanceChart.HAT -> hat(mix, start, volume, noise);
				default -> {
				}
			}
		}
		short[] out = new short[mix.length];
		for (int i = 0; i < mix.length; i++) {
			out[i] = (short) Math.round(Math.tanh(mix[i] * 0.9) * 30000.0);
		}
		return out;
	}

	private static void voice(float[] mix, int start, int length, double freq, float volume, float[] harmonics,
			double attack, double decay, double sustain, double release, double detune, double vibrato,
			double brightDecay) {
		int releaseSamples = Math.max(1, (int) (release * RATE));
		int total = length + releaseSamples;
		int count = harmonics.length;
		double[] phase = new double[count];
		double[] phaseDetuned = new double[count];
		double[] fade = new double[count];
		double[] fadeStep = new double[count];
		for (int h = 0; h < count; h++) {
			fade[h] = 1.0;
			fadeStep[h] = Math.exp(-brightDecay * h / RATE);
		}
		double releaseLevel = 1.0;
		for (int i = 0; i < total; i++) {
			int index = start + i;
			if (index >= mix.length) {
				break;
			}
			double t = (double) i / RATE;
			double envelope = t < attack ? t / attack : sustain + (1.0 - sustain) * Math.exp(-(t - attack) / decay);
			if (i < length) {
				releaseLevel = envelope;
			} else {
				envelope = releaseLevel * (1.0 - (double) (i - length) / releaseSamples);
			}
			double wobble = vibrato > 0.0 && t > 0.15 ? 1.0 + vibrato * sine(5.5 * t) : 1.0;
			double sample = 0.0;
			for (int h = 0; h < count; h++) {
				float amplitude = harmonics[h];
				double step = freq * (h + 1) * wobble / RATE;
				if (amplitude == 0.0F || step >= 0.45) {
					continue;
				}
				phase[h] = (phase[h] + step) % 1.0;
				double value = sine(phase[h]);
				if (detune > 0.0) {
					phaseDetuned[h] = (phaseDetuned[h] + step * (1.0 + detune)) % 1.0;
					value = (value + sine(phaseDetuned[h])) * 0.5;
				}
				sample += amplitude * fade[h] * value;
				fade[h] *= fadeStep[h];
			}
			mix[index] += (float) (sample * envelope * volume * 0.5);
		}
	}

	private static void kick(float[] mix, int start, float volume, Noise noise) {
		int total = (int) (0.35 * RATE);
		double phase = 0.0;
		for (int i = 0; i < total && start + i < mix.length; i++) {
			double t = (double) i / RATE;
			double freq = 45.0 + 110.0 * Math.exp(-t / 0.03);
			phase = (phase + freq / RATE) % 1.0;
			double body = sine(phase) * Math.exp(-t / 0.12);
			double click = noise.next() * Math.exp(-t / 0.002) * 0.3;
			mix[start + i] += (float) ((body + click) * volume);
		}
	}

	private static void snare(float[] mix, int start, float volume, Noise noise) {
		int total = (int) (0.25 * RATE);
		double previous = 0.0;
		double phase = 0.0;
		for (int i = 0; i < total && start + i < mix.length; i++) {
			double t = (double) i / RATE;
			double white = noise.next();
			double bright = white - previous * 0.5;
			previous = white;
			phase = (phase + 190.0 / RATE) % 1.0;
			double value = bright * Math.exp(-t / 0.06) * 0.8 + sine(phase) * Math.exp(-t / 0.04) * 0.5;
			mix[start + i] += (float) (value * volume);
		}
	}

	private static void hat(float[] mix, int start, float volume, Noise noise) {
		int total = (int) (0.08 * RATE);
		double previous = 0.0;
		for (int i = 0; i < total && start + i < mix.length; i++) {
			double t = (double) i / RATE;
			double white = noise.next();
			double value = (white - previous) * Math.exp(-t / 0.015);
			previous = white;
			mix[start + i] += (float) (value * volume * 0.6);
		}
	}

	private static double sine(double phase) {
		double wrapped = phase - Math.floor(phase);
		return SINE[(int) (wrapped * TABLE_SIZE) & (TABLE_SIZE - 1)];
	}

	private static final class Noise {
		private long state;

		private Noise(long seed) {
			this.state = seed;
		}

		private double next() {
			state ^= state << 13;
			state ^= state >>> 7;
			state ^= state << 17;
			return ((state >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
		}
	}
}
