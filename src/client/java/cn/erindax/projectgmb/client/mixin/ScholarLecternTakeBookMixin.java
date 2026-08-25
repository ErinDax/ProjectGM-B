package cn.erindax.projectgmb.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = {
	"io.github.mortuusars.scholar.client.gui.screen.view.LecternSpreadBookViewScreen",
	"io.github.mortuusars.scholar.client.gui.screen.edit.LecternSpreadBookEditScreen"
})
public abstract class ScholarLecternTakeBookMixin {
	@Redirect(
		method = "createBottomButtons",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;mayBuild()Z", remap = true),
		remap = false
	)
	private boolean projectgmb$showTakeBook(LocalPlayer player) {
		return !player.isSpectator();
	}
}
