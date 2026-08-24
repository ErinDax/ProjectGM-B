package cn.erindax.projectgmb.client.mixin;

import cn.erindax.projectgmb.client.VanishClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void projectgmb$hideVanished(
		Entity entity,
		double x,
		double y,
		double z,
		float rotationYaw,
		float partialTicks,
		PoseStack poseStack,
		MultiBufferSource buffer,
		int packedLight,
		CallbackInfo ci
	) {
		if (!(entity instanceof Player player)) {
			return;
		}
		if (player == Minecraft.getInstance().player) {
			return;
		}
		if (VanishClient.hiddenFromOthers(player.getUUID())) {
			ci.cancel();
		}
	}
}
