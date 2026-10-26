package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.reinforce.Reinforcements;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateReinforceMixin {
	@Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
	private void projectgmb$keepReinforced(Player player, BlockGetter level, BlockPos pos,
			CallbackInfoReturnable<Float> cir) {
		if (!player.isCreative() && Reinforcements.isReinforced(player.level(), pos)) {
			cir.setReturnValue(0.0F);
		}
	}
}
