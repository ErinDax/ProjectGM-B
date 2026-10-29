package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.seal.Seals;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandSealMixin {
	@Inject(method = "interactAt", at = @At("HEAD"), cancellable = true)
	private void projectgmb$sealedInteractAt(Player player, Vec3 vec, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (Seals.blocksEntity(player, (ArmorStand) (Object) this)) {
			if (hand == InteractionHand.MAIN_HAND) {
				Seals.notifyEntityBlocked(player);
			}
			cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}
