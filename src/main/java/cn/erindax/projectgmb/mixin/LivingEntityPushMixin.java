package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.vanish.OpVanish;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityPushMixin {
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private void projectgmb$vanishNoPush(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof Player player && OpVanish.isVanished(player.getUUID())) {
			cir.setReturnValue(false);
		}
	}
}
