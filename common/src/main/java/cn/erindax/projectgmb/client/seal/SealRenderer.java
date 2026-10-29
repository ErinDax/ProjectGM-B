package cn.erindax.projectgmb.client.seal;

import cn.erindax.projectgmb.client.ClientEvents;
import cn.erindax.projectgmb.item.ModItems;
import cn.erindax.projectgmb.seal.Seals;
import cn.erindax.projectgmb.seal.net.SealSelectionPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SealRenderer {

	private static final double MAX_DISTANCE_SQR = 128.0 * 128.0;
	private static final double INFLATE = 0.01;

	@Nullable
	private static BlockPos firstCorner;

	private SealRenderer() {
	}

	public static void init() {
		ClientEvents.AFTER_ENTITIES.register(SealRenderer::render);
	}

	public static void select(SealSelectionPayload payload) {
		firstCorner = payload.present() ? payload.pos() : null;
	}

	public static void clear() {
		firstCorner = null;
	}

	private static void render(PoseStack poseStack, MultiBufferSource buffers, Camera camera, ClientLevel level) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || !isHolding(player)) {
			return;
		}
		List<BoundingBox> boxes = Seals.clientView();
		BlockPos first = firstCorner;
		if (boxes.isEmpty() && first == null) {
			return;
		}
		Vec3 cam = camera.getPosition();
		VertexConsumer lines = buffers.getBuffer(RenderType.lines());
		for (BoundingBox box : boxes) {
			if (distanceSqr(box, cam) <= MAX_DISTANCE_SQR) {
				draw(poseStack, lines, box, cam, 0.75F, 0.3F, 1.0F);
			}
		}
		if (first != null) {
			BlockPos second = minecraft.hitResult instanceof BlockHitResult hit
				&& hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : first;
			draw(poseStack, lines, BoundingBox.fromCorners(first, second), cam, 0.3F, 0.85F, 1.0F);
		}
	}

	private static boolean isHolding(LocalPlayer player) {
		return player.getMainHandItem().is(ModItems.SEALER) || player.getOffhandItem().is(ModItems.SEALER);
	}

	private static void draw(PoseStack poseStack, VertexConsumer lines, BoundingBox box, Vec3 cam,
			float red, float green, float blue) {
		AABB aabb = new AABB(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1)
			.inflate(INFLATE)
			.move(-cam.x, -cam.y, -cam.z);
		LevelRenderer.renderLineBox(poseStack, lines, aabb, red, green, blue, 1.0F);
	}

	private static double distanceSqr(BoundingBox box, Vec3 point) {
		double dx = Math.max(Math.max(box.minX() - point.x, 0.0), point.x - (box.maxX() + 1));
		double dy = Math.max(Math.max(box.minY() - point.y, 0.0), point.y - (box.maxY() + 1));
		double dz = Math.max(Math.max(box.minZ() - point.z, 0.0), point.z - (box.maxZ() + 1));
		return dx * dx + dy * dy + dz * dz;
	}
}
