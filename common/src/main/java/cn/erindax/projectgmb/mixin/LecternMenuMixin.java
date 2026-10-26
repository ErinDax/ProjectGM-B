package cn.erindax.projectgmb.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.LecternMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LecternMenu.class)
public abstract class LecternMenuMixin {
	@Redirect(method = "clickMenuButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;mayBuild()Z"))
	private boolean projectgmb$adventureTakeBook(Player player) {
		return !player.isSpectator();
	}
}
