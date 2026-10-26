package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.reinforce.Reinforcements;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ExplosionReinforceMixin {
	@Shadow
	@Final
	private Level level;

	@Shadow
	@Final
	private ObjectArrayList<BlockPos> toBlow;

	@Inject(method = "finalizeExplosion", at = @At("HEAD"))
	private void projectgmb$keepReinforced(boolean spawnParticles, CallbackInfo ci) {
		if (level instanceof ServerLevel serverLevel) {
			Reinforcements.protect(serverLevel, toBlow);
		}
	}
}
