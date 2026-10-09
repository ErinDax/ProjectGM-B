package cn.erindax.projectgmb.client.dance;

import cn.erindax.projectgmb.client.music.ClientMusic;
import cn.erindax.projectgmb.client.music.Mp3AudioStream;
import cn.erindax.projectgmb.dance.DanceChart;
import cn.erindax.projectgmb.dance.DanceRules;
import cn.erindax.projectgmb.dance.DanceStanding;
import cn.erindax.projectgmb.dance.DanceStats;
import cn.erindax.projectgmb.dance.net.DanceOpenPayload;
import cn.erindax.projectgmb.dance.net.DanceProgressPayload;
import cn.erindax.projectgmb.dance.net.DanceSignalPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.JOrbisAudioStream;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class DanceGame {

	public enum Phase {
		LOADING, WAITING, COUNTDOWN, PLAYING, FINISHED, RESULTS, CLOSED
	}

	public static final byte PENDING = 0;
	public static final byte HIT = 1;
	public static final byte MISSED = 2;
	public static final byte HOLDING = 3;
	public static final byte HELD = 4;
	public static final byte DROPPED = 5;

	public static final int COUNTDOWN_BEATS = 4;

	public static final int EFFECT_MISS = DanceRules.MISS;
	public static final int EFFECT_DROP = 5;
	public static final int EFFECT_HOLD = 6;

	public record Effect(int lane, int kind, long at) {
	}

	private static final int EFFECT_LIMIT = 64;
	private static final long MILLIS = 1_000_000L;
	private static final long AUDIO_WAIT_NANOS = 3000L * MILLIS;
	private static final long LOAD_TIMEOUT_NANOS = 60000L * MILLIS;
	private static final int PROGRESS_INTERVAL = 10;
	private static final int END_DELAY = 1000;

	private final int session;
	private final DanceChart chart;
	private final int audioVersion;
	private final int[] time;
	private final int[] lane;
	private final int[] hold;
	private final byte[] state;
	private final long[] fadeAt;
	private final ArrayDeque<Effect> effects = new ArrayDeque<>();
	private final int lastNoteEnd;
	private final long beatNanos;

	private final boolean[] pressed = new boolean[DanceChart.LANES];
	private final long[] confirmAt = new long[DanceChart.LANES];
	private final int[] counts = new int[5];

	private List<DanceStanding> board;
	private List<DanceStanding> results = List.of();
	private Phase phase = Phase.LOADING;
	private boolean goReceived;
	private boolean readySent;
	private boolean audioFailed;
	private boolean quitted;
	private boolean hidden;
	private long goAt;
	private long countdownStart;
	private long countdownEnd;
	private long playAt;
	private long origin;
	private boolean clockLocked;
	private long pausedAt;
	private long pausedTotal;
	@Nullable
	private DanceSound sound;
	@Nullable
	private CompletableFuture<short[]> synth;
	@Nullable
	private byte[] audioData;
	private int score;
	private int combo;
	private int maxCombo;
	private int firstActive;
	private int lastJudgement = -1;
	private long judgementAt;
	private int ticks;
	@Nullable
	private DanceStats lastSent;
	private long escAt;

	public DanceGame(DanceOpenPayload payload) {
		this.session = payload.session();
		this.chart = payload.chart();
		this.audioVersion = payload.audioVersion();
		this.board = payload.players();
		List<DanceChart.Note> notes = chart.notes();
		int size = notes.size();
		this.time = new int[size];
		this.lane = new int[size];
		this.hold = new int[size];
		this.state = new byte[size];
		this.fadeAt = new long[size];
		for (int i = 0; i < size; i++) {
			DanceChart.Note note = notes.get(i);
			time[i] = note.time();
			lane[i] = note.lane();
			hold[i] = note.hold();
		}
		this.lastNoteEnd = chart.lastNoteEnd();
		this.beatNanos = (long) (Mth.clamp(chart.beatMs(), 350.0F, 800.0F) * MILLIS);
		if (!chart.audio().isEmpty()) {
			if (!ClientMusic.has(chart.audio(), audioVersion)) {
				ClientMusic.request(chart.audio(), audioVersion);
			}
		} else if (!chart.tones().isEmpty()) {
			List<DanceChart.Tone> tones = chart.tones();
			synth = CompletableFuture.supplyAsync(() -> DanceSynth.render(tones), Util.backgroundExecutor());
		}
	}

	public int session() {
		return session;
	}

	@Nullable
	public Effect pollEffect() {
		return effects.poll();
	}

	public long fadeAt(int index) {
		return fadeAt[index];
	}

	public DanceChart chart() {
		return chart;
	}

	public Phase phase() {
		return phase;
	}

	public boolean quitted() {
		return quitted;
	}

	public boolean wantsScreen() {
		return phase != Phase.CLOSED && !hidden;
	}

	public void hide() {
		hidden = true;
	}

	public int noteCount() {
		return time.length;
	}

	public int time(int index) {
		return time[index];
	}

	public int lane(int index) {
		return lane[index];
	}

	public int hold(int index) {
		return hold[index];
	}

	public byte state(int index) {
		return state[index];
	}

	public boolean pressed(int laneIndex) {
		return pressed[laneIndex];
	}

	public boolean holding(int laneIndex) {
		double limit = songTime() + DanceRules.HIT_WINDOW;
		for (int i = firstActive; i < state.length && time[i] <= limit; i++) {
			if (state[i] == HOLDING && lane[i] == laneIndex) {
				return true;
			}
		}
		return false;
	}

	public long confirmAt(int laneIndex) {
		return confirmAt[laneIndex];
	}

	public int lastJudgement() {
		return lastJudgement;
	}

	public long judgementAt() {
		return judgementAt;
	}

	public int combo() {
		return combo;
	}

	public List<DanceStanding> board() {
		return board;
	}

	public List<DanceStanding> results() {
		return results;
	}

	public long escAt() {
		return escAt;
	}

	public void markEscape(long now) {
		escAt = now;
	}

	public long countdownStart() {
		return countdownStart;
	}

	public long beatNanos() {
		return beatNanos;
	}

	public DanceStats stats() {
		return new DanceStats(score, combo, maxCombo, counts[DanceRules.SICK], counts[DanceRules.GOOD],
			counts[DanceRules.BAD], counts[DanceRules.SHIT], counts[DanceRules.MISS]);
	}

	public int expectedLength() {
		return Math.max(chart.duration(), 1);
	}

	public double songTime() {
		long now = clockNow(System.nanoTime());
		return switch (phase) {
			case LOADING, WAITING -> -(double) (COUNTDOWN_BEATS * beatNanos) / MILLIS;
			case COUNTDOWN -> -(double) (countdownEnd - now) / MILLIS;
			default -> (double) (now - origin() - pausedTotal) / MILLIS;
		};
	}

	public float beatPulse() {
		if (phase != Phase.PLAYING && phase != Phase.COUNTDOWN) {
			return 0.0F;
		}
		double beat = (songTime() - chart.offset()) / chart.beatMs();
		if (beat < 0.0) {
			return 0.0F;
		}
		double fraction = beat - Math.floor(beat);
		return (float) Math.max(0.0, 1.0 - fraction * 4.0);
	}

	public void tick() {
		switch (phase) {
			case LOADING -> {
				if (resourcesReady()) {
					markReady();
				} else if (goReceived && System.nanoTime() - goAt > LOAD_TIMEOUT_NANOS) {
					audioFailed = true;
					markReady();
				}
			}
			case COUNTDOWN, PLAYING -> {
				update();
				if (phase == Phase.PLAYING && ++ticks % PROGRESS_INTERVAL == 0) {
					sendProgress(DanceStanding.PLAYING);
				}
			}
			default -> {
			}
		}
	}

	public void update() {
		long now = System.nanoTime();
		trackPause(now);
		if (phase == Phase.COUNTDOWN) {
			if (clockNow(now) < countdownEnd) {
				return;
			}
			beginPlayback(now);
		}
		if (phase != Phase.PLAYING) {
			return;
		}
		double t = songTime();
		judgeTimeouts(t);
		if (finishedPlaying(t)) {
			complete();
		}
	}

	public void onGo() {
		if (goReceived) {
			return;
		}
		goReceived = true;
		goAt = System.nanoTime();
		if (phase == Phase.WAITING) {
			beginCountdown();
		}
	}

	public void updateBoard(List<DanceStanding> standings) {
		board = standings;
	}

	public void showResults(List<DanceStanding> standings) {
		results = standings;
		board = standings;
		stopSound();
		phase = Phase.RESULTS;
		hidden = false;
	}

	public void press(int laneIndex) {
		if (pressed[laneIndex]) {
			return;
		}
		pressed[laneIndex] = true;
		if (phase != Phase.PLAYING) {
			return;
		}
		long now = System.nanoTime();
		double t = songTime();
		for (int i = firstActive; i < time.length; i++) {
			double diff = time[i] - t;
			if (diff > DanceRules.HIT_WINDOW) {
				break;
			}
			if (lane[i] != laneIndex || state[i] != PENDING || diff < -DanceRules.HIT_WINDOW) {
				continue;
			}
			int judgement = DanceRules.judge(diff);
			counts[judgement]++;
			combo++;
			maxCombo = Math.max(maxCombo, combo);
			score += DanceRules.points(judgement, combo);
			state[i] = hold[i] > 0 ? HOLDING : HIT;
			confirmAt[laneIndex] = now;
			showJudgement(judgement, now);
			emit(laneIndex, judgement, now);
			advance();
			return;
		}
	}

	public void release(int laneIndex) {
		if (!pressed[laneIndex]) {
			return;
		}
		pressed[laneIndex] = false;
		if (phase != Phase.PLAYING) {
			return;
		}
		double t = songTime();
		for (int i = firstActive; i < time.length; i++) {
			if (time[i] > t + DanceRules.HIT_WINDOW) {
				break;
			}
			if (lane[i] == laneIndex && state[i] == HOLDING) {
				long now = System.nanoTime();
				if (t >= time[i] + hold[i] - DanceRules.HOLD_GRACE) {
					state[i] = HELD;
					score += DanceRules.HOLD_BONUS;
					emit(laneIndex, EFFECT_HOLD, now);
				} else {
					state[i] = DROPPED;
					fadeAt[i] = now;
					registerMiss(laneIndex, EFFECT_DROP, now);
				}
				advance();
				return;
			}
		}
	}

	public void quit() {
		switch (phase) {
			case LOADING, WAITING, COUNTDOWN, PLAYING -> {
				quitted = true;
				sendProgress(DanceStanding.QUIT);
				phase = Phase.FINISHED;
				stopSound();
			}
			default -> {
			}
		}
	}

	public void dispose() {
		phase = Phase.CLOSED;
		stopSound();
	}

	private boolean resourcesReady() {
		if (!chart.audio().isEmpty()) {
			if (audioData == null && ClientMusic.has(chart.audio(), audioVersion)) {
				audioData = ClientMusic.get(chart.audio());
			}
			return audioData != null;
		}
		return synth == null || synth.isDone();
	}

	private void markReady() {
		if (!readySent) {
			readySent = true;
			ClientPlayNetworking.send(new DanceSignalPayload(session, DanceSignalPayload.READY));
		}
		if (goReceived) {
			beginCountdown();
		} else {
			phase = Phase.WAITING;
		}
	}

	private void beginCountdown() {
		long now = System.nanoTime();
		phase = Phase.COUNTDOWN;
		countdownStart = now;
		countdownEnd = now + COUNTDOWN_BEATS * beatNanos;
		Minecraft.getInstance().getMusicManager().stopPlaying();
	}

	private void beginPlayback(long now) {
		phase = Phase.PLAYING;
		playAt = now;
		pausedTotal = 0L;
		DanceSound.Opener opener = opener();
		if (opener == null) {
			origin = now;
			clockLocked = true;
			return;
		}
		sound = new DanceSound(opener);
		Minecraft.getInstance().getSoundManager().play(sound);
	}

	@Nullable
	private DanceSound.Opener opener() {
		if (audioFailed) {
			return null;
		}
		if (audioData != null) {
			byte[] data = audioData;
			boolean mp3 = chart.audio().toLowerCase(Locale.ROOT).endsWith(".mp3");
			return () -> mp3 ? new Mp3AudioStream(new ByteArrayInputStream(data))
				: new JOrbisAudioStream(new ByteArrayInputStream(data));
		}
		if (synth != null && synth.isDone() && !synth.isCompletedExceptionally()) {
			short[] pcm = synth.join();
			return () -> new PcmAudioStream(pcm, DanceSynth.RATE);
		}
		return null;
	}

	private long origin() {
		if (!clockLocked) {
			long start = sound == null ? 0L : sound.startNanos();
			if (start != 0L) {
				origin = start;
				clockLocked = true;
			} else if (System.nanoTime() - playAt > AUDIO_WAIT_NANOS) {
				origin = playAt;
				clockLocked = true;
			} else {
				return playAt;
			}
		}
		return origin;
	}

	private long clockNow(long now) {
		return pausedAt != 0L ? pausedAt : now;
	}

	private void trackPause(long now) {
		boolean paused = Minecraft.getInstance().isPaused();
		if (paused && pausedAt == 0L) {
			pausedAt = now;
		} else if (!paused && pausedAt != 0L) {
			long duration = now - pausedAt;
			pausedAt = 0L;
			if (phase == Phase.COUNTDOWN) {
				countdownEnd += duration;
			} else if (phase == Phase.PLAYING) {
				pausedTotal += duration;
			}
		}
	}

	private void judgeTimeouts(double t) {
		long now = System.nanoTime();
		for (int i = firstActive; i < time.length; i++) {
			if (time[i] - t > DanceRules.HIT_WINDOW) {
				break;
			}
			if (state[i] == PENDING && t - time[i] > DanceRules.HIT_WINDOW) {
				state[i] = MISSED;
				fadeAt[i] = now;
				registerMiss(lane[i], EFFECT_MISS, now);
			} else if (state[i] == HOLDING && t >= time[i] + hold[i]) {
				state[i] = HELD;
				score += DanceRules.HOLD_BONUS;
				emit(lane[i], EFFECT_HOLD, now);
			}
		}
		advance();
	}

	private void advance() {
		while (firstActive < state.length && state[firstActive] != PENDING && state[firstActive] != HOLDING) {
			firstActive++;
		}
	}

	private void registerMiss(int laneIndex, int kind, long now) {
		counts[DanceRules.MISS]++;
		combo = 0;
		score = Math.max(0, score - DanceRules.MISS_PENALTY);
		showJudgement(DanceRules.MISS, now);
		emit(laneIndex, kind, now);
	}

	private void emit(int laneIndex, int kind, long now) {
		if (effects.size() >= EFFECT_LIMIT) {
			effects.poll();
		}
		effects.add(new Effect(laneIndex, kind, now));
	}

	private void showJudgement(int judgement, long now) {
		lastJudgement = judgement;
		judgementAt = now;
	}

	private boolean finishedPlaying(double t) {
		if (firstActive < state.length || t < lastNoteEnd + END_DELAY) {
			return false;
		}
		return sound == null || !Minecraft.getInstance().getSoundManager().isActive(sound);
	}

	private void complete() {
		sendProgress(DanceStanding.FINISHED);
		phase = Phase.FINISHED;
		stopSound();
	}

	private void sendProgress(int progressState) {
		DanceStats stats = stats();
		if (progressState == DanceStanding.PLAYING && stats.equals(lastSent)) {
			return;
		}
		lastSent = stats;
		ClientPlayNetworking.send(new DanceProgressPayload(session, stats, progressState));
	}

	private void stopSound() {
		if (sound != null) {
			Minecraft.getInstance().getSoundManager().stop(sound);
			sound = null;
		}
	}
}
