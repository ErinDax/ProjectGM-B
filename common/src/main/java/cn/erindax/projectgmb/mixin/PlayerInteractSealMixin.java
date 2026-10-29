package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.seal.Seals;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerInteractSealMixin {
	@Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
	private void projectgmb$sealedInteractOn(Entity entity, InteractionHand hand,
			CallbackInfoReturnable<InteractionResult> cir) {
		Player player = (Player) (Object) this;
		if (Seals.blocksEntity(player, entity)) {
			if (hand == InteractionHand.MAIN_HAND) {
				Seals.notifyEntityBlocked(player);
			}
			cir.setReturnValue(InteractionResult.PASS);
		}
	}
}
