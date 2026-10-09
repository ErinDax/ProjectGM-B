package cn.erindax.projectgmb.client.gui;

import cn.erindax.projectgmb.client.dance.DanceArrows;
import cn.erindax.projectgmb.client.dance.DanceClient;
import cn.erindax.projectgmb.client.dance.DanceGame;
import cn.erindax.projectgmb.dance.DanceChart;
import cn.erindax.projectgmb.dance.DanceRules;
import cn.erindax.projectgmb.dance.DanceStanding;
import cn.erindax.projectgmb.dance.DanceStats;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class DanceScreen extends Screen {

	private static final int[] LANE_COLORS = {0xFFC24B99, 0xFF00FFFF, 0xFF12FA05, 0xFFF9393F};
	private static final int[] LANE_KEYS = {GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_J, GLFW.GLFW_KEY_K};
	private static final String[] LANE_KEY_NAMES = {"S", "D", "J", "K"};
	private static final int[] JUDGE_COLORS = {0xFF62F0FF, 0xFF6CFF6C, 0xFFFFC04D, 0xFFB07050, 0xFFFF4D4D};
	private static final String[] JUDGE_KEYS = {
		"screen.projectgm_b.dance.judge.sick", "screen.projectgm_b.dance.judge.good", "screen.projectgm_b.dance.judge.bad",
		"screen.projectgm_b.dance.judge.shit", "screen.projectgm_b.dance.judge.miss"
	};
	private static final int RECEPTOR = 0xFFA4A4B8;
	private static final int GOLD = 0xFFFFD84A;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int MUTED = 0xFFA0A0B4;
	private static final int DIM = 0xFF70708A;
	private static final int DANGER = 0xFFFF6B6B;
	private static final int CARD = 0xB80E0E18;
	private static final int LINE = 9;
	private static final long MILLIS = 1_000_000L;
	private static final long ESC_WINDOW = 2000L * MILLIS;
	private static final int MISS_RED = 0xFFFF3A3A;
	private static final int MISS_DARK = 0xFF3A1820;
	private static final long FADE_MS = 320L;
	private static final long FC_FADE_MS = 1200L;
	private static final double SPARK_DRAG = 4.5;

	private final DanceGame game;
	private final List<Spark> sparks = new ArrayList<>();
	private final List<Burst> bursts = new ArrayList<>();
	private final float[] beams = new float[DanceChart.LANES];
	private final long[] missAt = new long[DanceChart.LANES];
	private final long[] holdSparkAt = new long[DanceChart.LANES];
	private final Random random = new Random();
	private long lastFrame;
	private boolean fullComboAlive = true;
	private long fullComboBrokenAt;

	private float ui;
	private float size;
	private float spacing;
	private float receptorY;
	private float lanesLeft;
	private float lanesRight;
	private float panelWidth;
	private float sideScale;
	private boolean compact;

	private record Spark(float x, float y, float vx, float vy, long born, long life, float size, int color,
			float gravity, boolean additive) {
	}

	private record Burst(int lane, int kind, long born) {
	}

	public DanceScreen(DanceGame game) {
		super(Component.translatable("screen.projectgm_b.dance.title"));
		this.game = game;
	}

	public DanceGame game() {
		return game;
	}

	@Override
	protected void init() {
		DanceArrows.ensure();
		layout();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		game.update();
		layout();
		long now = System.nanoTime();
		drawBackdrop(graphics);
		if (game.phase() == DanceGame.Phase.RESULTS) {
			drawResults(graphics);
		} else {
			drawPlayfield(graphics, now);
			drawInfo(graphics, now);
			drawBoard(graphics);
			drawEscKey(graphics);
			drawOverlay(graphics, now);
		}
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			handleEscape();
			return true;
		}
		if (game.phase() != DanceGame.Phase.RESULTS) {
			int lane = lane(keyCode);
			if (lane >= 0) {
				game.press(lane);
				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		int lane = lane(keyCode);
		if (lane >= 0) {
			game.release(lane);
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	private static int lane(int keyCode) {
		for (int lane = 0; lane < LANE_KEYS.length; lane++) {
			if (keyCode == LANE_KEYS[lane]) {
				return lane;
			}
		}
		return -1;
	}

	private void handleEscape() {
		switch (game.phase()) {
			case RESULTS, CLOSED -> DanceClient.close(game);
			case FINISHED -> {
				game.hide();
				if (minecraft != null) {
					minecraft.setScreen(null);
				}
			}
			default -> {
				long now = System.nanoTime();
				if (game.escAt() != 0L && now - game.escAt() < ESC_WINDOW) {
					game.quit();
					if (game.phase() == DanceGame.Phase.CLOSED) {
						DanceClient.close(game);
					}
				} else {
					game.markEscape(now);
				}
			}
		}
	}

	private void layout() {
		ui = Mth.clamp(height / 320.0F, 1.0F, 1.7F);
		size = Mth.clamp(height * 0.13F, 26.0F, 64.0F);
		spacing = size * 1.12F;
		lanesLeft = width / 2.0F - spacing * 2.0F - 6.0F;
		lanesRight = width / 2.0F + spacing * 2.0F + 6.0F;
		receptorY = capTop() - size * 0.62F - height * 0.02F;
		panelWidth = Math.min(lanesLeft - 24.0F, 110.0F * ui + 40.0F);
		compact = panelWidth < 110.0F;
		sideScale = compact ? 1.0F : Math.min(ui, Math.max(1.0F, panelWidth / 130.0F));
	}

	private float laneX(int lane) {
		return width / 2.0F + (lane - 1.5F) * spacing;
	}

	private void drawBackdrop(GuiGraphics graphics) {
		graphics.fillGradient(0, 0, width, height, 0xE00B0B16, 0xE01E0F2E);
		float pulse = game.beatPulse();
		if (pulse > 0.0F) {
			graphics.fill(0, 0, width, height, withAlpha(0xFF9060FF, pulse * 0.07F));
		}
	}

	private void drawPlayfield(GuiGraphics graphics, long now) {
		float pixelsPerMs = (receptorY + size) * game.chart().speed() / 1100.0F;
		consumeEffects(now, pixelsPerMs);
		updateBeams(now);

		int left = Math.round(lanesLeft);
		int right = Math.round(lanesRight);
		graphics.fillGradient(left, 0, right, height, 0x90000000, 0x60000000);
		graphics.fill(left, 0, left + 1, height, 0x40FFFFFF);
		graphics.fill(right - 1, 0, right, height, 0x40FFFFFF);
		for (int lane = 1; lane < DanceChart.LANES; lane++) {
			int x = Math.round(laneX(lane) - spacing / 2.0F);
			graphics.fill(x, 0, x + 1, height, 0x14FFFFFF);
		}
		for (int lane = 0; lane < DanceChart.LANES; lane++) {
			if (beams[lane] <= 0.01F) {
				continue;
			}
			int x0 = Math.round(laneX(lane) - spacing / 2.0F) + 1;
			int x1 = Math.round(laneX(lane) + spacing / 2.0F);
			int bottom = Math.round(receptorY);
			graphics.fillGradient(x0, Math.round(bottom - height * 0.6F), x1, bottom,
				withAlpha(LANE_COLORS[lane], 0.0F), withAlpha(LANE_COLORS[lane], 0.26F * beams[lane]));
		}
		int band = Math.round(size * 0.62F);
		graphics.fillGradient(left + 1, Math.round(receptorY) - band, right - 1, Math.round(receptorY), 0x00FFFFFF,
			0x12FFFFFF);
		graphics.fillGradient(left + 1, Math.round(receptorY), right - 1, Math.round(receptorY) + band, 0x12FFFFFF,
			0x00FFFFFF);

		drawLaneKeys(graphics);
		drawJudgement(graphics, now);
		double songTime = game.songTime();
		drawReceptors(graphics, now);
		drawNotes(graphics, songTime, pixelsPerMs, now);
		drawConfirmGlow(graphics, now);
		drawBursts(graphics, now);
		drawSparks(graphics, now);
	}

	private void drawReceptors(GuiGraphics graphics, long now) {
		float bump = 1.0F + 0.05F * game.beatPulse();
		for (int lane = 0; lane < DanceChart.LANES; lane++) {
			float centerX = laneX(lane);
			float confirm = confirmLevel(lane, now);
			float missFlash = fade(now - missAt[lane], 260L);
			if (missFlash > 0.0F) {
				centerX += (float) Math.sin((now - missAt[lane]) / (double) MILLIS * 0.09) * 3.0F * ui * missFlash;
			}
			if (confirm > 0.0F) {
				float pop = 1.0F + 0.14F * easeOut(confirm);
				DanceArrows.draw(graphics, lane, false, centerX, receptorY, size * pop,
					mix(LANE_COLORS[lane], WHITE, 0.5F * confirm));
				continue;
			}
			boolean held = game.pressed(lane);
			int color = held ? mix(RECEPTOR, LANE_COLORS[lane], 0.55F) : RECEPTOR;
			if (missFlash > 0.0F) {
				color = mix(color, MISS_RED, 0.7F * missFlash);
			}
			DanceArrows.draw(graphics, lane, true, centerX, receptorY, size * (held ? 0.88F : bump), color);
		}
	}

	private float confirmLevel(int lane, long now) {
		float confirm = fade(now - game.confirmAt(lane), 200L);
		if (game.holding(lane)) {
			float pulse = 0.5F + 0.5F * (float) Math.sin(now / (double) MILLIS * 0.025);
			confirm = Math.max(confirm, 0.6F + 0.2F * pulse);
		}
		return confirm;
	}

	private void drawConfirmGlow(GuiGraphics graphics, long now) {
		for (int lane = 0; lane < DanceChart.LANES; lane++) {
			float confirm = confirmLevel(lane, now);
			if (confirm <= 0.0F) {
				continue;
			}
			float centerX = laneX(lane);
			DanceArrows.halo(graphics, lane, centerX, receptorY, size * (1.25F + 0.2F * confirm),
				withAlpha(LANE_COLORS[lane], 0.75F * confirm));
			DanceArrows.sprite(graphics, DanceArrows.GLOW, centerX, receptorY, size * 2.2F,
				withAlpha(LANE_COLORS[lane], 0.35F * confirm), true);
		}
	}

	private void drawNotes(GuiGraphics graphics, double songTime, float pixelsPerMs, long now) {
		for (int i = 0; i < game.noteCount(); i++) {
			byte state = game.state(i);
			if (state == DanceGame.HIT || state == DanceGame.HELD) {
				continue;
			}
			float headY = (float) (receptorY - (game.time(i) - songTime) * pixelsPerMs);
			if (headY < -size) {
				break;
			}
			float tailY = (float) (receptorY - (game.time(i) + game.hold(i) - songTime) * pixelsPerMs);
			if (Math.min(headY, tailY) > height + size) {
				continue;
			}
			int lane = game.lane(i);
			float centerX = laneX(lane);
			if (state == DanceGame.MISSED || state == DanceGame.DROPPED) {
				drawFading(graphics, i, lane, centerX, headY, tailY, state == DanceGame.MISSED, now);
				continue;
			}
			if (game.hold(i) > 0) {
				boolean holding = state == DanceGame.HOLDING;
				drawTrail(graphics, lane, centerX, holding ? receptorY : headY, tailY, holding ? 1.0F : 0.0F, 1.0F);
			}
			if (state != DanceGame.HOLDING) {
				DanceArrows.draw(graphics, lane, false, centerX, headY, size, LANE_COLORS[lane]);
			}
		}
	}

	private void drawFading(GuiGraphics graphics, int index, int lane, float centerX, float headY, float tailY,
			boolean missed, long now) {
		long age = now - game.fadeAt(index);
		float progress = Mth.clamp((float) age / (FADE_MS * MILLIS), 0.0F, 1.0F);
		if (progress >= 1.0F) {
			return;
		}
		float alpha = 1.0F - progress;
		int tint = mix(LANE_COLORS[lane], MISS_DARK, 0.55F + 0.45F * progress);
		if (game.hold(index) > 0 && tailY < headY) {
			drawTrail(graphics, lane, centerX, missed ? headY : Math.min(headY, receptorY), tailY, 0.0F, alpha * 0.6F,
				mix(tint, 0xFF505060, 0.5F));
		}
		if (!missed) {
			return;
		}
		float shake = (float) Math.sin(age / (double) MILLIS * 0.08) * 2.5F * ui * alpha;
		float shrink = 1.0F - 0.3F * easeOut(progress);
		DanceArrows.draw(graphics, lane, false, centerX + shake, headY, size * shrink, withAlpha(tint, alpha));
	}

	private void drawTrail(GuiGraphics graphics, int lane, float centerX, float head, float tail, float energy,
			float alpha) {
		drawTrail(graphics, lane, centerX, head, tail, energy, alpha, LANE_COLORS[lane]);
	}

	private void drawTrail(GuiGraphics graphics, int lane, float centerX, float head, float tail, float energy,
			float alpha, int color) {
		if (tail >= head) {
			return;
		}
		float outer = Math.max(4.0F, size * 0.34F);
		float core = Math.max(2.0F, size * 0.12F);
		fill(graphics, centerX - outer / 2.0F, tail, centerX + outer / 2.0F, head, withAlpha(color, 0.62F * alpha));
		fill(graphics, centerX - outer / 2.0F, tail, centerX - outer / 2.0F + 1.0F, head,
			withAlpha(WHITE, 0.25F * alpha));
		fill(graphics, centerX + outer / 2.0F - 1.0F, tail, centerX + outer / 2.0F, head,
			withAlpha(0xFF000000, 0.35F * alpha));
		fill(graphics, centerX - core / 2.0F, tail, centerX + core / 2.0F, head,
			withAlpha(mix(color, WHITE, 0.6F), (0.35F + 0.45F * energy) * alpha));
		DanceArrows.sprite(graphics, DanceArrows.DOT, centerX, tail, outer * 1.6F,
			withAlpha(color, (0.5F + 0.4F * energy) * alpha), true);
	}

	private void updateBeams(long now) {
		float delta = lastFrame == 0L ? 0.0F : Math.min(0.1F, (now - lastFrame) / 1.0E9F);
		lastFrame = now;
		for (int lane = 0; lane < DanceChart.LANES; lane++) {
			if (game.pressed(lane)) {
				beams[lane] = Math.min(1.0F, beams[lane] + delta * 18.0F);
			} else {
				beams[lane] = Math.max(0.0F, beams[lane] - delta * 7.0F);
			}
			if (game.holding(lane) && now - holdSparkAt[lane] > 45L * MILLIS) {
				holdSparkAt[lane] = now;
				for (int i = 0; i < 2; i++) {
					spawnSpark(laneX(lane), receptorY, randomRange(-70.0F, 70.0F), randomRange(-150.0F, -40.0F),
						randomRange(260.0F, 420.0F), randomRange(3.0F, 5.5F), i == 0 ? WHITE : LANE_COLORS[lane], 0.0F,
						true, now);
				}
			}
		}
	}

	private void consumeEffects(long now, float pixelsPerMs) {
		DanceGame.Effect effect;
		while ((effect = game.pollEffect()) != null) {
			int lane = effect.lane();
			if (lane < 0 || lane >= DanceChart.LANES || now - effect.at() > 500L * MILLIS) {
				continue;
			}
			float centerX = laneX(lane);
			int kind = effect.kind();
			bursts.add(new Burst(lane, kind, now));
			switch (kind) {
				case DanceRules.SICK -> burstSparks(lane, centerX, receptorY, 12, 1.0F, now);
				case DanceRules.GOOD -> burstSparks(lane, centerX, receptorY, 7, 0.8F, now);
				case DanceRules.BAD -> burstSparks(lane, centerX, receptorY, 3, 0.6F, now);
				case DanceGame.EFFECT_HOLD -> burstSparks(lane, centerX, receptorY, 9, 0.9F, now);
				case DanceGame.EFFECT_MISS -> {
					missAt[lane] = now;
					shatter(lane, centerX, receptorY + DanceRules.HIT_WINDOW * pixelsPerMs, now);
				}
				case DanceGame.EFFECT_DROP -> {
					missAt[lane] = now;
					shatter(lane, centerX, receptorY, now);
				}
				default -> {
				}
			}
		}
		if (bursts.size() > 48) {
			bursts.subList(0, bursts.size() - 48).clear();
		}
		if (sparks.size() > 400) {
			sparks.subList(0, sparks.size() - 400).clear();
		}
	}

	private void burstSparks(int lane, float x, float y, int count, float power, long now) {
		for (int i = 0; i < count; i++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			float speed = randomRange(120.0F, 320.0F) * power;
			int color = i % 3 == 0 ? WHITE : mix(LANE_COLORS[lane], WHITE, randomRange(0.0F, 0.4F));
			spawnSpark(x, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
				randomRange(280.0F, 480.0F), randomRange(3.5F, 6.5F) * power, color, 60.0F, true, now);
		}
	}

	private void shatter(int lane, float x, float y, long now) {
		for (int i = 0; i < 7; i++) {
			float vx = randomRange(-110.0F, 110.0F);
			float vy = randomRange(-140.0F, -20.0F);
			int color = mix(LANE_COLORS[lane], MISS_DARK, randomRange(0.45F, 0.8F));
			spawnSpark(x + randomRange(-size * 0.25F, size * 0.25F), y + randomRange(-size * 0.2F, size * 0.2F), vx, vy,
				randomRange(380.0F, 560.0F), randomRange(4.0F, 7.0F), color, 720.0F, false, now);
		}
	}

	private void spawnSpark(float x, float y, float vx, float vy, float lifeMs, float sparkSize, int color,
			float gravity, boolean additive, long now) {
		sparks.add(new Spark(x, y, vx * ui, vy * ui, now, (long) (lifeMs * MILLIS), sparkSize * ui, color,
			gravity * ui, additive));
	}

	private void drawSparks(GuiGraphics graphics, long now) {
		sparks.removeIf(spark -> now - spark.born() >= spark.life());
		for (Spark spark : sparks) {
			float t = (now - spark.born()) / 1.0E9F;
			float life = spark.life() / 1.0E9F;
			float progress = t / life;
			float drag = (float) ((1.0 - Math.exp(-SPARK_DRAG * t)) / SPARK_DRAG);
			float x = spark.x() + spark.vx() * drag;
			float y = spark.y() + spark.vy() * drag + 0.5F * spark.gravity() * t * t;
			float alpha = (float) Math.pow(1.0F - progress, 1.4);
			float sparkSize = spark.size() * (1.0F - 0.5F * progress) * 2.0F;
			DanceArrows.sprite(graphics, DanceArrows.DOT, x, y, sparkSize, withAlpha(spark.color(), alpha), spark.additive());
		}
	}

	private void drawBursts(GuiGraphics graphics, long now) {
		bursts.removeIf(burst -> now - burst.born() > 520L * MILLIS);
		for (Burst burst : bursts) {
			float age = (now - burst.born()) / (float) MILLIS;
			int lane = burst.lane();
			float centerX = laneX(lane);
			int color = LANE_COLORS[lane];
			switch (burst.kind()) {
				case DanceRules.SICK -> {
					ring(graphics, centerX, age, 380.0F, 0.9F, 2.3F, mix(color, WHITE, 0.35F), 0.95F);
					ring(graphics, centerX, age - 70.0F, 320.0F, 0.8F, 1.8F, WHITE, 0.55F);
					glow(graphics, centerX, age, 300.0F, 2.6F, color, 0.7F);
					float star = 1.0F - Mth.clamp(age / 180.0F, 0.0F, 1.0F);
					DanceArrows.sprite(graphics, DanceArrows.STAR, centerX, receptorY, size * (1.4F + 0.9F * (1.0F - star)),
						withAlpha(WHITE, star), true);
				}
				case DanceRules.GOOD -> {
					ring(graphics, centerX, age, 320.0F, 0.9F, 1.9F, color, 0.75F);
					glow(graphics, centerX, age, 240.0F, 2.1F, color, 0.45F);
				}
				case DanceRules.BAD -> ring(graphics, centerX, age, 260.0F, 0.9F, 1.5F, mix(color, 0xFF808090, 0.5F), 0.45F);
				case DanceRules.SHIT -> glow(graphics, centerX, age, 220.0F, 1.6F, 0xFF807060, 0.3F);
				case DanceGame.EFFECT_HOLD -> {
					ring(graphics, centerX, age, 340.0F, 0.9F, 2.0F, mix(color, WHITE, 0.5F), 0.85F);
					glow(graphics, centerX, age, 260.0F, 2.3F, color, 0.55F);
				}
				case DanceGame.EFFECT_MISS, DanceGame.EFFECT_DROP ->
					glow(graphics, centerX, age, 260.0F, 1.7F, MISS_RED, 0.35F);
				default -> {
				}
			}
		}
	}

	private void ring(GuiGraphics graphics, float centerX, float age, float duration, float from, float to, int color,
			float strength) {
		if (age < 0.0F || age >= duration) {
			return;
		}
		float progress = age / duration;
		float eased = easeOut(progress);
		float alpha = strength * (1.0F - progress) * (1.0F - progress);
		DanceArrows.sprite(graphics, DanceArrows.RING, centerX, receptorY, size * (from + (to - from) * eased),
			withAlpha(color, alpha), true);
	}

	private void glow(GuiGraphics graphics, float centerX, float age, float duration, float scale, int color,
			float strength) {
		if (age < 0.0F || age >= duration) {
			return;
		}
		float progress = age / duration;
		DanceArrows.sprite(graphics, DanceArrows.GLOW, centerX, receptorY, size * scale * (0.85F + 0.25F * progress),
			withAlpha(color, strength * (1.0F - progress)), true);
	}

	private float randomRange(float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	private static float easeOut(float value) {
		float inverse = 1.0F - Mth.clamp(value, 0.0F, 1.0F);
		return 1.0F - inverse * inverse * inverse;
	}

	private float infoHeight(float s) {
		float height = 8.0F + LINE * 1.3F * s + 3.0F;
		height += 3.0F * s + 3.0F + LINE * 0.85F * s + 12.0F;
		height += 2.0F + LINE * 1.9F * s + 6.0F + LINE * 1.4F * s + 5.0F + tagHeight(s);
		return height + 8.0F;
	}

	private float tagHeight(float s) {
		return LINE * 0.85F * s + 5.0F;
	}

	private void trackFullCombo(DanceStats stats, long now) {
		boolean alive = stats.miss() == 0;
		if (fullComboAlive && !alive) {
			fullComboBrokenAt = now;
		}
		fullComboAlive = alive;
	}

	private void drawFullComboTag(GuiGraphics graphics, float right, float y, float s, long now) {
		Component label;
		int color;
		float alpha;
		float shake = 0.0F;
		if (fullComboAlive) {
			label = Component.translatable("screen.projectgm_b.dance.fc");
			color = GOLD;
			alpha = 0.8F + 0.2F * game.beatPulse();
		} else {
			long age = now - fullComboBrokenAt;
			if (fullComboBrokenAt == 0L || age >= FC_FADE_MS * MILLIS) {
				return;
			}
			float progress = (float) age / (FC_FADE_MS * MILLIS);
			label = Component.translatable("screen.projectgm_b.dance.fc_broken");
			color = DANGER;
			alpha = progress < 0.4F ? 1.0F : 1.0F - (progress - 0.4F) / 0.6F;
			shake = (float) Math.sin(age / (double) MILLIS * 0.08) * 2.0F * s * (1.0F - progress);
		}
		float scale = 0.85F * s;
		float tagWidth = font.width(label) * scale + 10.0F;
		float tagHeight = tagHeight(s);
		float x0 = right - tagWidth + shake;
		fill(graphics, x0, y, x0 + tagWidth, y + tagHeight, withAlpha(color, 0.18F * alpha));
		outline(graphics, x0, y, tagWidth, tagHeight, withAlpha(color, 0.85F * alpha));
		text(graphics, label, x0 + tagWidth / 2.0F, y + 2.5F, scale, withAlpha(color, alpha), 0);
	}

	private void drawInfo(GuiGraphics graphics, long now) {
		DanceChart chart = game.chart();
		DanceStats stats = game.stats();
		trackFullCombo(stats, now);
		if (compact) {
			Component line = Component.translatable("screen.projectgm_b.dance.label.compact", stats.score(),
				stats.accuracyText());
			text(graphics, Component.literal("♪ " + chart.title()), 6.0F, 6.0F, 1.0F, GOLD, -1);
			text(graphics, line, 6.0F, 17.0F, 1.0F, WHITE, -1);
			return;
		}
		float s = sideScale;
		float x1 = lanesLeft - 16.0F;
		float x0 = x1 - panelWidth;
		float y0 = 10.0F;
		float cardHeight = infoHeight(s);
		card(graphics, x0, y0, x1, y0 + cardHeight, GOLD);
		float pad = 8.0F;
		float inner = panelWidth - pad * 2.0F;
		float y = y0 + pad;

		Component title = Component.literal("♪ " + chart.title());
		text(graphics, trimmed(title, inner / (1.3F * s)), x0 + pad, y, 1.3F * s, GOLD, -1);
		y += LINE * 1.3F * s + 3.0F;

		float progress = Mth.clamp((float) (game.songTime() / game.expectedLength()), 0.0F, 1.0F);
		float barHeight = 3.0F * s;
		fill(graphics, x0 + pad, y, x1 - pad, y + barHeight, 0x40FFFFFF);
		fill(graphics, x0 + pad, y, x0 + pad + inner * progress, y + barHeight, GOLD);
		y += barHeight + 3.0F;
		String time = clock(game.songTime()) + " / " + clock(game.expectedLength());
		text(graphics, Component.literal(time), x1 - pad, y, 0.85F * s, MUTED, 1);
		y += LINE * 0.85F * s + 6.0F;
		fill(graphics, x0 + pad, y, x1 - pad, y + 1.0F, 0x30FFFFFF);
		y += 8.0F;
		text(graphics, Component.literal(String.valueOf(stats.score())), x0 + pad, y, 1.9F * s, WHITE, -1);
		y += LINE * 1.9F * s + 6.0F;
		text(graphics, Component.literal(stats.accuracyText()), x0 + pad, y + LINE * 0.2F * s, 1.2F * s, WHITE, -1);
		text(graphics, Component.literal(stats.grade()), x1 - pad, y, 1.4F * s, gradeColor(stats.grade()), 1);
		y += LINE * 1.4F * s + 5.0F;
		text(graphics, Component.translatable("screen.projectgm_b.dance.label.stats", stats.combo(), stats.miss()), x0 + pad,
			y + 2.5F, 0.95F * s, MUTED, -1);
		drawFullComboTag(graphics, x1 - pad, y, s, now);
	}

	private void drawJudgement(GuiGraphics graphics, long now) {
		int judgement = game.lastJudgement();
		long age = now - game.judgementAt();
		if (judgement < 0 || age < 0L || age > 700L * MILLIS) {
			return;
		}
		float alpha = age < 450L * MILLIS ? 1.0F : 1.0F - (float) (age - 450L * MILLIS) / (250L * MILLIS);
		float pop = age < 90L * MILLIS ? 1.25F - 0.25F * age / (90.0F * MILLIS) : 1.0F;
		float centerX = width / 2.0F;
		float centerY = receptorY * 0.62F;
		float scale = compact ? 2.0F * ui : 2.2F * sideScale;
		centerY -= 8.0F * sideScale * easeOut(age / (700.0F * MILLIS));
		DanceArrows.sprite(graphics, DanceArrows.GLOW, centerX, centerY, LINE * scale * 3.2F,
			withAlpha(JUDGE_COLORS[judgement], 0.28F * alpha), true);
		float textX = centerX - 1.2F * scale * pop;
		text(graphics, Component.translatable(JUDGE_KEYS[judgement]), textX, centerY - LINE * scale * pop / 2.0F,
			scale * pop, withAlpha(JUDGE_COLORS[judgement], alpha), 0);
		if (judgement != DanceRules.MISS && game.combo() >= 3) {
			float comboScale = scale * 0.6F;
			text(graphics, Component.translatable("screen.projectgm_b.dance.combo_pop", game.combo()),
				centerX - 1.2F * comboScale * pop, centerY + LINE * scale * 0.7F, comboScale * pop,
				withAlpha(WHITE, alpha), 0);
		}
	}

	private void drawBoard(GuiGraphics graphics) {
		List<DanceStanding> standings = liveStandings();
		float x0 = lanesRight + 16.0F;
		float boardWidth = Math.min(width - x0 - 12.0F, panelWidth);
		if (standings.size() < 2 || compact || boardWidth < 100.0F) {
			return;
		}
		float s = sideScale * 0.95F;
		float x1 = x0 + boardWidth;
		float y0 = 10.0F;
		float pad = 8.0F;
		float rowHeight = LINE * s + 5.0F;
		float headerHeight = LINE * 1.1F * s + 7.0F;
		int rows = Math.min(standings.size(), Math.max(1, (int) ((height - y0 - 60.0F - headerHeight) / rowHeight)));
		card(graphics, x0, y0, x1, y0 + pad * 2.0F + headerHeight + rows * rowHeight, GOLD);
		text(graphics, Component.translatable("screen.projectgm_b.dance.live"), x0 + pad, y0 + pad, 1.1F * s, GOLD, -1);
		UUID self = selfId();
		float y = y0 + pad + headerHeight;
		for (int i = 0; i < rows; i++) {
			DanceStanding standing = standings.get(i);
			if (standing.id().equals(self)) {
				fill(graphics, x0 + 3.0F, y - 2.0F, x1 - 3.0F, y + rowHeight - 3.0F, 0x40FFD84A);
			}
			boolean gone = standing.status() == DanceStanding.QUIT || standing.status() == DanceStanding.OFFLINE;
			int color = gone ? DIM : WHITE;
			String score = String.valueOf(standing.stats().score());
			float scoreWidth = font.width(score) * s;
			float rankWidth = font.width("00.") * s;
			text(graphics, Component.literal((i + 1) + "."), x0 + pad, y, s, i == 0 ? GOLD : MUTED, -1);
			float nameWidth = (x1 - pad - scoreWidth - 6.0F) - (x0 + pad + rankWidth);
			text(graphics, trimmed(standing.name(), nameWidth / s), x0 + pad + rankWidth, y, s, color, -1);
			text(graphics, Component.literal(score), x1 - pad, y, s,
				standing.status() == DanceStanding.FINISHED ? 0xFF7CFC7C : color, 1);
			y += rowHeight;
		}
	}

	private List<DanceStanding> liveStandings() {
		UUID self = selfId();
		DanceStats mine = game.stats();
		List<DanceStanding> list = new ArrayList<>();
		for (DanceStanding standing : game.board()) {
			list.add(standing.id().equals(self) && standing.status() == DanceStanding.PLAYING
				? standing.withStats(mine) : standing);
		}
		list.sort(DanceStanding.ORDER);
		return list;
	}

	private void drawOverlay(GuiGraphics graphics, long now) {
		float centerX = width / 2.0F;
		float centerY = height * 0.45F;
		switch (game.phase()) {
			case LOADING, WAITING -> {
				int dots = (int) ((now / (400L * MILLIS)) % 4L);
				String key = game.phase() == DanceGame.Phase.LOADING ? "screen.projectgm_b.dance.loading"
					: "screen.projectgm_b.dance.waiting";
				Component status = Component.translatable(key).append(".".repeat(dots));
				Component title = Component.literal(game.chart().title());
				float titleScale = 1.8F * ui;
				float statusScale = 1.05F * ui;
				float cardWidth = Math.max(font.width(title) * titleScale,
					font.width(Component.translatable(key).append("...")) * statusScale) + 40.0F;
				cardWidth = Math.min(cardWidth, width - 24.0F);
				float cardHeight = LINE * titleScale + LINE * statusScale + 30.0F;
				card(graphics, centerX - cardWidth / 2.0F, centerY - cardHeight / 2.0F, centerX + cardWidth / 2.0F,
					centerY + cardHeight / 2.0F, GOLD);
				float y = centerY - cardHeight / 2.0F + 12.0F;
				text(graphics, trimmed(title, (cardWidth - 20.0F) / titleScale), centerX, y, titleScale, GOLD, 0);
				text(graphics, status, centerX, y + LINE * titleScale + 6.0F, statusScale, WHITE, 0);
			}
			case COUNTDOWN -> {
				long elapsed = now - game.countdownStart();
				int index = (int) Math.max(0L, elapsed / game.beatNanos());
				float fraction = (float) (elapsed % game.beatNanos()) / game.beatNanos();
				Component label = index < DanceGame.COUNTDOWN_BEATS - 1
					? Component.literal(String.valueOf(DanceGame.COUNTDOWN_BEATS - 1 - index))
					: Component.translatable("screen.projectgm_b.dance.go");
				float scale = (5.0F - 1.2F * fraction) * ui;
				text(graphics, label, centerX, centerY - LINE * scale / 2.0F, scale, withAlpha(GOLD, 1.0F - fraction * 0.6F), 0);
			}
			case FINISHED -> drawFinished(graphics, centerX, centerY);
			default -> {
			}
		}
		if (game.escAt() != 0L && now - game.escAt() < ESC_WINDOW && game.phase() != DanceGame.Phase.FINISHED) {
			toast(graphics, Component.translatable("screen.projectgm_b.dance.esc"), DANGER);
		}
	}

	private void drawFinished(GuiGraphics graphics, float centerX, float centerY) {
		DanceStats stats = game.stats();
		float titleScale = 1.6F * ui;
		float scoreScale = 2.4F * ui;
		float bodyScale = 1.0F * ui;
		Component title = Component.translatable(game.quitted() ? "screen.projectgm_b.dance.quitted" : "screen.projectgm_b.dance.finished");
		Component detail = Component.translatable("screen.projectgm_b.dance.finished.detail", stats.accuracyText(),
			stats.maxCombo(), stats.grade());
		Component waiting = Component.translatable("screen.projectgm_b.dance.waiting_others");
		float cardWidth = Math.min(width - 24.0F, Math.max(font.width(detail) * bodyScale, font.width(title) * titleScale)
			+ 48.0F);
		float footerScale = footerScale(ui);
		Component hide = Component.translatable("screen.projectgm_b.dance.hint.hide");
		cardWidth = Math.min(width - 24.0F, Math.max(cardWidth,
			keycapWidth(Component.literal("Esc"), footerScale) + font.width(hide) * footerScale + 48.0F));
		float cardHeight = LINE * (titleScale + scoreScale + bodyScale * 2.0F) + 44.0F + footerHeight(ui);
		float y = centerY - cardHeight / 2.0F;
		card(graphics, centerX - cardWidth / 2.0F, y, centerX + cardWidth / 2.0F, y + cardHeight, GOLD);
		y += 12.0F;
		text(graphics, title, centerX, y, titleScale, GOLD, 0);
		y += LINE * titleScale + 6.0F;
		text(graphics, Component.literal(String.valueOf(stats.score())), centerX, y, scoreScale, WHITE, 0);
		y += LINE * scoreScale + 6.0F;
		text(graphics, detail, centerX, y, bodyScale, gradeColor(stats.grade()), 0);
		y += LINE * bodyScale + 6.0F;
		text(graphics, waiting, centerX, y, bodyScale, MUTED, 0);
		y += LINE * bodyScale + 10.0F;
		fill(graphics, centerX - cardWidth / 2.0F + 8.0F, y, centerX + cardWidth / 2.0F - 8.0F, y + 1.0F, 0x20FFFFFF);
		keyHint(graphics, "screen.projectgm_b.dance.hint.hide", centerX, y + 6.0F, footerScale);
	}

	private float keyAlpha() {
		return switch (game.phase()) {
			case LOADING, WAITING, COUNTDOWN -> 1.0F;
			case PLAYING -> {
				float t = (float) game.songTime();
				yield t <= 0.0F ? 1.0F : 1.0F - 0.6F * Mth.clamp(t / 1500.0F, 0.0F, 1.0F);
			}
			default -> 0.0F;
		};
	}

	private float capWidth() {
		return spacing * 0.8F;
	}

	private float capHeight() {
		return capWidth() * 0.82F;
	}

	private float capTop() {
		return height - 6.0F - capHeight();
	}

	private void drawLaneKeys(GuiGraphics graphics) {
		float base = keyAlpha();
		if (base <= 0.0F) {
			return;
		}
		float capWidth = capWidth();
		float capHeight = capHeight();
		float y0 = capTop();
		float labelScale = Math.max(0.7F, capHeight / 42.0F);
		for (int lane = 0; lane < DanceChart.LANES; lane++) {
			boolean held = game.pressed(lane);
			float alpha = held ? 1.0F : base;
			float centerX = laneX(lane);
			float x0 = centerX - capWidth / 2.0F;
			int color = LANE_COLORS[lane];
			int face = held ? mix(0xFF1C1C2A, color, 0.45F) : 0xFF1C1C2A;
			fill(graphics, x0, y0, x0 + capWidth, y0 + capHeight, withAlpha(face, 0.85F * alpha));
			fill(graphics, x0, y0 + capHeight - 2.0F, x0 + capWidth, y0 + capHeight, withAlpha(0xFF000000, 0.5F * alpha));
			outline(graphics, x0, y0, capWidth, capHeight, withAlpha(color, (held ? 1.0F : 0.7F) * alpha));
			DanceArrows.draw(graphics, lane, false, centerX, y0 + capHeight * 0.36F, capHeight * 0.46F,
				withAlpha(held ? WHITE : color, alpha));
			text(graphics, Component.literal(LANE_KEY_NAMES[lane]), centerX, y0 + capHeight * 0.66F, labelScale,
				withAlpha(held ? WHITE : MUTED, alpha), 0);
		}
	}

	private void drawEscKey(GuiGraphics graphics) {
		float alpha = keyAlpha();
		if (alpha <= 0.0F) {
			return;
		}
		Component esc = Component.literal("Esc");
		Component action = Component.translatable("screen.projectgm_b.dance.hint.quit");
		float scale = Math.max(0.8F, capHeight() / 36.0F);
		float keyHeight = (LINE + 5.0F) * scale;
		float y0 = capTop() + capHeight() - keyHeight;
		float total = keycapWidth(esc, scale) + 4.0F * scale + font.width(action) * scale;
		float x = lanesRight + 8.0F;
		if (x + total > width - 6.0F) {
			x = lanesLeft - 8.0F - total;
		}
		x += keycap(graphics, esc, x, y0, scale, alpha) + 4.0F * scale;
		text(graphics, action, x, y0 + 2.5F * scale, scale, withAlpha(MUTED, alpha), -1);
	}

	private void keyHint(GuiGraphics graphics, String actionKey, float centerX, float y, float scale) {
		Component esc = Component.literal("Esc");
		Component action = Component.translatable(actionKey);
		float total = keycapWidth(esc, scale) + 4.0F * scale + font.width(action) * scale;
		float x = centerX - total / 2.0F;
		x += keycap(graphics, esc, x, y, scale, 1.0F) + 4.0F * scale;
		text(graphics, action, x, y + 2.5F * scale, scale, MUTED, -1);
	}

	private float keycapWidth(Component label, float scale) {
		return Math.max(font.width(label) + 6.0F, 13.0F) * scale;
	}

	private float keycap(GuiGraphics graphics, Component label, float x, float y, float scale, float alpha) {
		float keyWidth = keycapWidth(label, scale);
		float keyHeight = (LINE + 5.0F) * scale;
		fill(graphics, x, y, x + keyWidth, y + keyHeight, withAlpha(0xFF2A2A3C, alpha));
		fill(graphics, x, y + keyHeight - 2.0F * scale, x + keyWidth, y + keyHeight, withAlpha(0xFF15151F, alpha));
		outline(graphics, x, y, keyWidth, keyHeight, withAlpha(0x70FFFFFF, alpha));
		text(graphics, label, x + keyWidth / 2.0F, y + 2.0F * scale, scale, withAlpha(WHITE, alpha), 0);
		return keyWidth;
	}

	private void toast(GuiGraphics graphics, Component message, int color) {
		float scale = Mth.clamp(ui * 0.95F, 1.0F, 1.5F);
		float pillWidth = font.width(message) * scale + 24.0F;
		float pillHeight = LINE * scale + 10.0F;
		float y0 = receptorY - size * 0.75F - 6.0F - pillHeight;
		float x0 = width / 2.0F - pillWidth / 2.0F;
		fill(graphics, x0, y0, x0 + pillWidth, y0 + pillHeight, 0xE0180C10);
		outline(graphics, x0, y0, pillWidth, pillHeight, withAlpha(color, 0.8F));
		text(graphics, message, width / 2.0F, y0 + 5.0F, scale, color, 0);
	}

	private float footerScale(float r) {
		return Mth.clamp(0.9F * r, 0.9F, 1.4F);
	}

	private float footerHeight(float r) {
		return (LINE + 5.0F) * footerScale(r) + 12.0F;
	}

	private record ResultsLayout(float r, float x0, float y0, float width, float height, float body, int rows,
			float rowHeight, float rankX, float nameX, float nameWidth, float scoreRight, float accuracyRight,
			float comboRight, float gradeX) {
	}

	private ResultsLayout resultsLayout() {
		List<DanceStanding> standings = game.results();
		float r = Mth.clamp(ui, 1.0F, 1.7F);
		float gap = 18.0F;
		float pad = 14.0F;
		Component nameHeader = Component.translatable("screen.projectgm_b.dance.column.player");
		int nameWidth = font.width(nameHeader);
		int scoreWidth = font.width(Component.translatable("screen.projectgm_b.dance.column.score"));
		int accuracyWidth = Math.max(font.width(Component.translatable("screen.projectgm_b.dance.column.accuracy")),
			font.width("100.00%"));
		int comboWidth = font.width(Component.translatable("screen.projectgm_b.dance.column.combo"));
		int gradeWidth = Math.max(font.width(Component.translatable("screen.projectgm_b.dance.column.grade")),
			font.width("S+") + 10);
		for (DanceStanding standing : standings) {
			nameWidth = Math.max(nameWidth, font.width(displayName(standing)));
			scoreWidth = Math.max(scoreWidth, font.width(String.valueOf(standing.stats().score())));
			comboWidth = Math.max(comboWidth, font.width(String.valueOf(standing.stats().maxCombo())));
		}
		nameWidth = Mth.clamp(nameWidth, 60, 140);
		float badge = 12.0F;
		float content = badge + gap * 0.6F + nameWidth + gap + scoreWidth + gap + accuracyWidth + gap + comboWidth + gap
			+ gradeWidth;
		float titleWidth = font.width(resultsTitle()) * 1.5F;
		float inner = Math.max(content, titleWidth);
		float available = (width - 24.0F) / r - pad * 2.0F;
		if (inner > available) {
			r = Math.max(0.8F, r * available / inner);
		}
		float boxWidth = (inner + pad * 2.0F) * r;
		float rowHeight = 16.0F * r;
		float footer = footerHeight(r);
		float head = 50.0F * r;
		int maxRows = Math.max(1, (int) ((height - head - footer - 40.0F) / rowHeight));
		int rows = Math.min(standings.size(), maxRows);
		float body = head + rows * rowHeight + 8.0F * r;
		float boxHeight = body + footer;
		float x0 = (width - boxWidth) / 2.0F;
		float y0 = Math.max(8.0F, (height - boxHeight) / 2.0F);
		float left = x0 + pad * r + (boxWidth - (inner + pad * 2.0F) * r) / 2.0F + (inner - content) * r / 2.0F;
		float rankX = left;
		float nameX = rankX + (badge + gap * 0.6F) * r;
		float scoreRight = nameX + (nameWidth + gap + scoreWidth) * r;
		float accuracyRight = scoreRight + (gap + accuracyWidth) * r;
		float comboRight = accuracyRight + (gap + comboWidth) * r;
		float gradeX = comboRight + gap * r;
		return new ResultsLayout(r, x0, y0, boxWidth, boxHeight, body, rows, rowHeight, rankX, nameX, nameWidth * r,
			scoreRight, accuracyRight, comboRight, gradeX);
	}

	private Component resultsTitle() {
		return Component.translatable("screen.projectgm_b.dance.results", game.chart().title());
	}

	private Component displayName(DanceStanding standing) {
		Component name = standing.name();
		if (standing.status() == DanceStanding.QUIT) {
			return name.copy().append(" ").append(Component.translatable("screen.projectgm_b.dance.status.quit"));
		}
		if (standing.status() == DanceStanding.OFFLINE) {
			return name.copy().append(" ").append(Component.translatable("screen.projectgm_b.dance.status.offline"));
		}
		return name;
	}

	private void drawResults(GuiGraphics graphics) {
		List<DanceStanding> standings = game.results();
		ResultsLayout layout = resultsLayout();
		float r = layout.r();
		float x0 = layout.x0();
		float y0 = layout.y0();
		float x1 = x0 + layout.width();
		card(graphics, x0, y0, x1, y0 + layout.height(), GOLD);
		text(graphics, resultsTitle(), width / 2.0F, y0 + 11.0F * r, 1.5F * r, GOLD, 0);

		float headerY = y0 + 34.0F * r;
		float headerScale = 0.85F * r;
		text(graphics, Component.translatable("screen.projectgm_b.dance.column.player"), layout.nameX(), headerY, headerScale,
			MUTED, -1);
		text(graphics, Component.translatable("screen.projectgm_b.dance.column.score"), layout.scoreRight(), headerY,
			headerScale, MUTED, 1);
		text(graphics, Component.translatable("screen.projectgm_b.dance.column.accuracy"), layout.accuracyRight(), headerY,
			headerScale, MUTED, 1);
		text(graphics, Component.translatable("screen.projectgm_b.dance.column.combo"), layout.comboRight(), headerY,
			headerScale, MUTED, 1);
		text(graphics, Component.translatable("screen.projectgm_b.dance.column.grade"), layout.gradeX(), headerY, headerScale,
			MUTED, -1);
		fill(graphics, x0 + 8.0F, headerY + 11.0F * r, x1 - 8.0F, headerY + 11.0F * r + 1.0F, 0x30FFFFFF);

		UUID self = selfId();
		float rowTop = y0 + 50.0F * r;
		for (int i = 0; i < layout.rows(); i++) {
			DanceStanding standing = standings.get(i);
			DanceStats stats = standing.stats();
			float top = rowTop + i * layout.rowHeight();
			float textY = top + (layout.rowHeight() - LINE * r) / 2.0F + 0.5F * r;
			boolean mine = standing.id().equals(self);
			if (mine) {
				fill(graphics, x0 + 4.0F, top, x1 - 4.0F, top + layout.rowHeight() - 1.0F, 0x38FFD84A);
				fill(graphics, x0 + 4.0F, top, x0 + 6.0F, top + layout.rowHeight() - 1.0F, GOLD);
			} else if (i % 2 == 1) {
				fill(graphics, x0 + 4.0F, top, x1 - 4.0F, top + layout.rowHeight() - 1.0F, 0x10FFFFFF);
			}
			drawRankBadge(graphics, i, layout.rankX(), top + (layout.rowHeight() - 12.0F * r) / 2.0F, r);
			boolean gone = standing.status() == DanceStanding.QUIT || standing.status() == DanceStanding.OFFLINE;
			int color = gone ? DIM : WHITE;
			text(graphics, trimmed(displayName(standing), layout.nameWidth() / r), layout.nameX(), textY, r, color, -1);
			text(graphics, Component.literal(String.valueOf(stats.score())), layout.scoreRight(), textY, r, color, 1);
			text(graphics, Component.literal(stats.accuracyText()), layout.accuracyRight(), textY, r, color, 1);
			text(graphics, Component.literal(String.valueOf(stats.maxCombo())), layout.comboRight(), textY, r, color, 1);
			String grade = stats.grade();
			text(graphics, Component.literal(grade), layout.gradeX(), textY, r, gradeColor(grade), -1);
			if (stats.fullCombo()) {
				float starX = layout.gradeX() + font.width(grade) * r + 6.0F * r;
				DanceArrows.sprite(graphics, DanceArrows.STAR, starX, textY + LINE * r * 0.4F, 11.0F * r, GOLD, true);
			}
		}
		float body = layout.body();
		fill(graphics, x0 + 8.0F, y0 + body, x1 - 8.0F, y0 + body + 1.0F, 0x20FFFFFF);
		keyHint(graphics, "screen.projectgm_b.dance.hint.close", width / 2.0F, y0 + body + 6.0F, footerScale(r));
	}

	private void drawRankBadge(GuiGraphics graphics, int index, float x, float y, float r) {
		float badge = 12.0F * r;
		int color = switch (index) {
			case 0 -> GOLD;
			case 1 -> 0xFFC8CCD8;
			case 2 -> 0xFFD08A50;
			default -> 0;
		};
		String rank = String.valueOf(index + 1);
		if (color != 0) {
			fill(graphics, x, y, x + badge, y + badge, withAlpha(color, 0.9F));
			fill(graphics, x, y + badge - 1.5F * r, x + badge, y + badge, withAlpha(0xFF000000, 0.35F));
			text(graphics, Component.literal(rank), x + badge / 2.0F, y + (badge - LINE * 0.9F * r) / 2.0F + 0.5F * r,
				0.9F * r, 0xFF1A1420, 0);
		} else {
			text(graphics, Component.literal(rank), x + badge / 2.0F, y + (badge - LINE * 0.9F * r) / 2.0F + 0.5F * r,
				0.9F * r, MUTED, 0);
		}
	}

	private void card(GuiGraphics graphics, float x0, float y0, float x1, float y1, int accent) {
		fill(graphics, x0, y0, x1, y1, CARD);
		outline(graphics, x0, y0, x1 - x0, y1 - y0, 0x30FFFFFF);
		fill(graphics, x0, y0, x1, y0 + 2.0F, withAlpha(accent, 0.85F));
	}

	private void text(GuiGraphics graphics, Component text, float x, float y, float scale, int color, int align) {
		text(graphics, text.getVisualOrderText(), x, y, scale, color, align);
	}

	private void text(GuiGraphics graphics, FormattedCharSequence text, float x, float y, float scale, int color,
			int align) {
		if ((color >>> 24) < 8 || scale <= 0.0F) {
			return;
		}
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(x, y, 0.0F);
		pose.scale(scale, scale, 1.0F);
		int textWidth = font.width(text);
		int offset = align < 0 ? 0 : align == 0 ? -textWidth / 2 : -textWidth;
		graphics.drawString(font, text, offset, 0, color, true);
		pose.popPose();
	}

	private FormattedCharSequence trimmed(Component text, float maxWidth) {
		if (font.width(text) <= maxWidth) {
			return text.getVisualOrderText();
		}
		return Language.getInstance().getVisualOrder(font.substrByWidth(text, Math.max(0, (int) maxWidth)));
	}

	private static void fill(GuiGraphics graphics, float x0, float y0, float x1, float y1, int color) {
		graphics.fill(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1), color);
	}

	private static void outline(GuiGraphics graphics, float x, float y, float w, float h, int color) {
		fill(graphics, x, y, x + w, y + 1.0F, color);
		fill(graphics, x, y + h - 1.0F, x + w, y + h, color);
		fill(graphics, x, y, x + 1.0F, y + h, color);
		fill(graphics, x + w - 1.0F, y, x + w, y + h, color);
	}

	@Nullable
	private UUID selfId() {
		return minecraft == null || minecraft.player == null ? null : minecraft.player.getUUID();
	}

	private static String clock(double millis) {
		int seconds = Math.max(0, (int) (millis / 1000.0));
		return seconds / 60 + ":" + String.format(Locale.ROOT, "%02d", seconds % 60);
	}

	private static int gradeColor(String grade) {
		return switch (grade) {
			case "S+" -> 0xFFFFD700;
			case "S" -> 0xFFFFE066;
			case "A" -> 0xFF7CFC7C;
			case "B" -> 0xFF66CCFF;
			case "C" -> 0xFFFFA64D;
			case "D" -> 0xFFFF5555;
			default -> MUTED;
		};
	}

	private static float fade(long elapsedNanos, long durationMs) {
		long duration = durationMs * MILLIS;
		if (elapsedNanos < 0L || elapsedNanos >= duration) {
			return 0.0F;
		}
		return 1.0F - (float) elapsedNanos / duration;
	}

	private static int withAlpha(int argb, float alpha) {
		int base = (argb >>> 24) & 0xFF;
		int value = Math.round(base * Mth.clamp(alpha, 0.0F, 1.0F));
		return value << 24 | (argb & 0xFFFFFF);
	}

	private static int mix(int from, int to, float amount) {
		float t = Mth.clamp(amount, 0.0F, 1.0F);
		int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * t);
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return a << 24 | r << 16 | g << 8 | b;
	}
}
