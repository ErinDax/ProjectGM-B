package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.seal.Seals;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateSealMixin {
	@Shadow
	protected abstract BlockState asState();

	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void projectgmb$sealedUse(Level level, Player player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> cir) {
		if (Seals.blocks(player, level, hit.getBlockPos(), asState())) {
			if (player.getItemInHand(hand).isEmpty()) {
				Seals.notifyBlocked(player);
			}
			cir.setReturnValue(InteractionResult.PASS);
		}
	}
}
