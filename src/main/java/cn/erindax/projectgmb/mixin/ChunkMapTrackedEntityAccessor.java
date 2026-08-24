package cn.erindax.projectgmb.mixin;

import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface ChunkMapTrackedEntityAccessor {
	@Invoker("removePlayer")
	void projectgmb$removePlayer(ServerPlayer player);

	@Invoker("updatePlayer")
	void projectgmb$updatePlayer(ServerPlayer player);
}
