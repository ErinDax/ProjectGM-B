package cn.erindax.projectgmb.client.mixin;

import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LecternScreen.class)
public abstract class LecternScreenMixin {
	@Redirect(method = "createMenuControls", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;mayBuild()Z"))
	private boolean projectgmb$showTakeBook(LocalPlayer player) {
		return !player.isSpectator();
	}
}
