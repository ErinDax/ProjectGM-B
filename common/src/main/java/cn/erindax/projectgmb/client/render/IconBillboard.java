package cn.erindax.projectgmb.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

public final class IconBillboard {

	private IconBillboard() {
	}

	public static void draw(PoseStack poseStack, VertexConsumer consumer, Camera camera, Vec3 offset, float size) {
		poseStack.pushPose();
		poseStack.translate(offset.x, offset.y, offset.z);
		poseStack.mulPose(camera.rotation());
		PoseStack.Pose pose = poseStack.last();
		float h = size / 2.0F;
		vertex(consumer, pose, -h, h, 0.0F, 0.0F);
		vertex(consumer, pose, -h, -h, 0.0F, 1.0F);
		vertex(consumer, pose, h, -h, 1.0F, 1.0F);
		vertex(consumer, pose, h, h, 1.0F, 0.0F);
		poseStack.popPose();
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v) {
		consumer.addVertex(pose, x, y, 0.0F)
			.setColor(255, 255, 255, 255)
			.setUv(u, v)
			.setOverlay(OverlayTexture.NO_OVERLAY)
			.setLight(LightTexture.FULL_BRIGHT)
			.setNormal(pose, 0.0F, 0.0F, 1.0F);
	}
}
