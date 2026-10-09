package cn.erindax.projectgmb.client.dance;

import cn.erindax.projectgmb.ProjectGmB;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class DanceArrows {

	public static final int GLOW = 0;
	public static final int RING = 1;
	public static final int STAR = 2;
	public static final int DOT = 3;

	private static final ResourceLocation TEXTURE = ProjectGmB.id("dance/arrows");
	private static final int CELL = 64;
	private static final int WIDTH = CELL * 4;
	private static final int HEIGHT = CELL * 4;
	private static final int ROW_NOTE = 0;
	private static final int ROW_RECEPTOR = 1;
	private static final int ROW_HALO = 2;
	private static final int ROW_SPRITE = 3;
	private static final float HALO_SHRINK = 0.6F;
	private static final float ROUND = 0.07F;
	private static final float OUTLINE = 0.13F;
	private static final float[] XS = {0.0F, 0.82F, 0.0F, -0.82F};
	private static final float[] YS = {-0.82F, 0.0F, 0.82F, 0.0F};

	private static boolean ready;

	private DanceArrows() {
	}

	public static void ensure() {
		if (ready) {
			return;
		}
		NativeImage image = new NativeImage(WIDTH, HEIGHT, true);
		for (int lane = 0; lane < 4; lane++) {
			paintArrow(image, lane, ROW_NOTE);
			paintArrow(image, lane, ROW_RECEPTOR);
			paintHalo(image, lane);
		}
		for (int sprite = 0; sprite < 4; sprite++) {
			paintSprite(image, sprite);
		}
		DynamicTexture texture = new DynamicTexture(image);
		Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
		texture.setFilter(true, false);
		ready = true;
	}

	public static void draw(GuiGraphics graphics, int lane, boolean receptor, float centerX, float centerY, float size,
			int argb) {
		blit(graphics, lane, receptor ? ROW_RECEPTOR : ROW_NOTE, centerX, centerY, size, argb, false);
	}

	public static void halo(GuiGraphics graphics, int lane, float centerX, float centerY, float size, int argb) {
		blit(graphics, lane, ROW_HALO, centerX, centerY, size / HALO_SHRINK, argb, true);
	}

	public static void sprite(GuiGraphics graphics, int sprite, float centerX, float centerY, float size, int argb,
			boolean additive) {
		blit(graphics, sprite, ROW_SPRITE, centerX, centerY, size, argb, additive);
	}

	private static void blit(GuiGraphics graphics, int column, int row, float centerX, float centerY, float size,
			int argb, boolean additive) {
		float alpha = (argb >>> 24) / 255.0F;
		if (alpha <= 0.01F || size <= 0.5F) {
			return;
		}
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(centerX - size / 2.0F, centerY - size / 2.0F, 0.0F);
		pose.scale(size / CELL, size / CELL, 1.0F);
		RenderSystem.enableBlend();
		if (additive) {
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		} else {
			RenderSystem.defaultBlendFunc();
		}
		graphics.setColor(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, alpha);
		graphics.blit(TEXTURE, 0, 0, column * CELL, row * CELL, CELL, CELL, WIDTH, HEIGHT);
		graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		pose.popPose();
	}

	private static void paintArrow(NativeImage image, int lane, int row) {
		boolean receptor = row == ROW_RECEPTOR;
		float half = CELL / 2.0F;
		for (int py = 0; py < CELL; py++) {
			for (int px = 0; px < CELL; px++) {
				float x = (px + 0.5F - half) / half;
				float y = (py + 0.5F - half) / half;
				float distance = signedDistance(x, y) - ROUND;
				float coverage = Mth.clamp(0.5F - distance * half, 0.0F, 1.0F);
				if (coverage <= 0.0F) {
					put(image, lane, row, px, py, 0.0F, 0.0F);
					continue;
				}
				float inner = Mth.clamp(0.5F - (distance + OUTLINE) * half, 0.0F, 1.0F);
				float fill;
				if (receptor) {
					fill = 0.62F;
				} else {
					float gradient = 1.0F - 0.24F * (y + 1.0F) / 2.0F;
					float shine = Mth.clamp(1.0F - Math.abs(distance + OUTLINE + 0.09F) / 0.045F, 0.0F, 1.0F);
					fill = Math.min(1.0F, gradient + 0.18F * shine * (y < 0.2F ? 1.0F : 0.4F));
				}
				float edge = receptor ? 0.08F : 0.1F;
				put(image, lane, row, px, py, edge + (fill - edge) * inner, coverage);
			}
		}
	}

	private static void paintHalo(NativeImage image, int lane) {
		float half = CELL / 2.0F;
		for (int py = 0; py < CELL; py++) {
			for (int px = 0; px < CELL; px++) {
				float x = (px + 0.5F - half) / half;
				float y = (py + 0.5F - half) / half;
				float distance = signedDistance(x / HALO_SHRINK, y / HALO_SHRINK) * HALO_SHRINK;
				float radius = (float) Math.sqrt(x * x + y * y);
				float edge = Mth.clamp((1.0F - radius) / 0.12F, 0.0F, 1.0F);
				float outside = Math.max(distance, 0.0F);
				float alpha = (float) Math.exp(-(outside / 0.16F) * (outside / 0.16F)) * edge;
				put(image, lane, ROW_HALO, px, py, 1.0F, alpha);
			}
		}
	}

	private static void paintSprite(NativeImage image, int sprite) {
		float half = CELL / 2.0F;
		for (int py = 0; py < CELL; py++) {
			for (int px = 0; px < CELL; px++) {
				float x = (px + 0.5F - half) / half;
				float y = (py + 0.5F - half) / half;
				float radius = (float) Math.sqrt(x * x + y * y);
				float edge = Mth.clamp((1.0F - radius) / 0.1F, 0.0F, 1.0F);
				float alpha = switch (sprite) {
					case GLOW -> gauss(radius, 0.42F);
					case RING -> gauss(radius - 0.8F, 0.07F);
					case STAR -> Math.max(gauss(Math.abs(x), 0.05F) * gauss(Math.abs(y), 0.55F),
						gauss(Math.abs(y), 0.05F) * gauss(Math.abs(x), 0.55F)) + 0.6F * gauss(radius, 0.16F);
					default -> gauss(radius, 0.34F);
				};
				put(image, sprite, ROW_SPRITE, px, py, 1.0F, Mth.clamp(alpha, 0.0F, 1.0F) * edge);
			}
		}
	}

	private static float gauss(float value, float width) {
		float scaled = value / width;
		return (float) Math.exp(-scaled * scaled);
	}

	private static void put(NativeImage image, int column, int row, int px, int py, float value, float alpha) {
		int grey = Math.round(Mth.clamp(value, 0.0F, 1.0F) * 255.0F);
		int a = Math.round(Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F);
		image.setPixelRGBA(column * CELL + px, row * CELL + py, a << 24 | grey << 16 | grey << 8 | grey);
	}

	private static float signedDistance(float x, float y) {
		float best = Float.MAX_VALUE;
		boolean inside = false;
		for (int i = 0, j = XS.length - 1; i < XS.length; j = i++) {
			float ax = XS[j];
			float ay = YS[j];
			float bx = XS[i];
			float by = YS[i];
			float ex = bx - ax;
			float ey = by - ay;
			float wx = x - ax;
			float wy = y - ay;
			float t = Mth.clamp((wx * ex + wy * ey) / (ex * ex + ey * ey), 0.0F, 1.0F);
			float dx = wx - ex * t;
			float dy = wy - ey * t;
			best = Math.min(best, dx * dx + dy * dy);
			if ((ay > y) != (by > y) && x < ex * (y - ay) / ey + ax) {
				inside = !inside;
			}
		}
		float distance = (float) Math.sqrt(best);
		return inside ? -distance : distance;
	}
}
