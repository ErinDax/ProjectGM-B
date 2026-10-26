package cn.erindax.projectgmb.client.mixin;

import cn.erindax.projectgmb.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void projectgmb$hideMusicNote(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
			PoseStack poseStack, MultiBufferSource buffers, int light, CallbackInfo ci) {
		if (stack.is(ModItems.MUSIC_NOTE) && entity != Minecraft.getInstance().player) {
			ci.cancel();
		}
	}
}
