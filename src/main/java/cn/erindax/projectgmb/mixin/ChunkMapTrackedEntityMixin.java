package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.vanish.OpVanish;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class ChunkMapTrackedEntityMixin {
	@Shadow
	@Final
	private Entity entity;

	@Shadow
	public abstract void removePlayer(ServerPlayer player);

	@Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
	private void projectgmb$hideVanished(ServerPlayer player, CallbackInfo ci) {
		if (!(entity instanceof ServerPlayer vanished) || vanished == player) {
			return;
		}
		if (!OpVanish.isVanished(vanished.getUUID())) {
			return;
		}
		removePlayer(player);
		ci.cancel();
	}
}
