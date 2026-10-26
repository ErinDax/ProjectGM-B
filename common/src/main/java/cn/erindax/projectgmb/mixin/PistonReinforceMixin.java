package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.reinforce.Reinforcements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonReinforceMixin {
	@Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
	private static void projectgmb$keepReinforced(BlockState state, Level level, BlockPos pos, Direction movementDirection,
			boolean allowDestroy, Direction pistonFacing, CallbackInfoReturnable<Boolean> cir) {
		if (level instanceof ServerLevel serverLevel && Reinforcements.isReinforced(serverLevel, pos)) {
			cir.setReturnValue(false);
		}
	}
}
