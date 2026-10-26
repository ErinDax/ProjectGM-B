package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.lock.DoorLocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class BlockRemoveLockMixin {

	@Inject(method = "onRemove", at = @At("HEAD"))
	private void projectgmb$clearDoorLock(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston, CallbackInfo ci) {
		DoorLocks.onBlockRemoved(level, pos, state, newState);
	}
}
