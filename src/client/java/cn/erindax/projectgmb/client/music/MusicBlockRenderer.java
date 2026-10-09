package cn.erindax.projectgmb.client.music;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.client.render.IconBillboard;
import cn.erindax.projectgmb.item.MusicNoteItem;
import cn.erindax.projectgmb.music.MusicBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class MusicBlockRenderer {

	private static final ResourceLocation ICON = ProjectGmB.id("textures/item/music_note.png");
	private static final int MIN_VISIBLE_RANGE = 16;
	private static final float SIZE = 0.5F;
	private static final double ICON_HEIGHT = 0.5 + SIZE / 2.0 + 0.15;
	private static final int HINT_OFFSET = 24;

	private MusicBlockRenderer() {
	}

	public static void init() {
		WorldRenderEvents.AFTER_ENTITIES.register(MusicBlockRenderer::render);
		HudRenderCallback.EVENT.register(MusicBlockRenderer::renderHint);
	}

	private static void render(WorldRenderContext context) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || ClientMusicBlocks.all().isEmpty() || !ClientMusicBlocks.canSee(player)) {
			return;
		}
		Camera camera = context.camera();
		Vec3 cam = camera.getPosition();
		PoseStack poseStack = context.matrixStack();
		MultiBufferSource buffers = context.consumers();
		if (poseStack == null || buffers == null) {
			return;
		}
		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(ICON));
		for (Map.Entry<BlockPos, MusicBlock> entry : ClientMusicBlocks.all().entrySet()) {
			BlockPos pos = entry.getKey();
			double visible = Math.max(entry.getValue().range(), MIN_VISIBLE_RANGE);
			if (pos.distToCenterSqr(cam) > visible * visible) {
				continue;
			}
			IconBillboard.draw(poseStack, consumer, camera, pos.getCenter().add(0.0, ICON_HEIGHT, 0.0).subtract(cam), SIZE);
		}
	}

	private static void renderHint(GuiGraphics graphics, DeltaTracker tracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null || minecraft.options.hideGui
				|| !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
				|| !ClientMusicBlocks.canSee(player)) {
			return;
		}
		MusicBlock block = ClientMusicBlocks.get(hit.getBlockPos());
		if (block == null) {
			return;
		}
		Component text = Component.translatable("screen.projectgm_b.music.block_info",
			MusicNoteItem.displayName(block.track()), block.range());
		int x = graphics.guiWidth() / 2;
		int y = graphics.guiHeight() / 2 + HINT_OFFSET;
		graphics.drawCenteredString(minecraft.font, text, x, y, 0xFFFFFF);
	}
}
