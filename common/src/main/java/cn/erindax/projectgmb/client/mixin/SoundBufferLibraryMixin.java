package cn.erindax.projectgmb.client.mixin;

import cn.erindax.projectgmb.client.music.MusicPlayer;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {

	@Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
	private void projectgmb$openMusicStream(ResourceLocation location, boolean looping,
			CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
		CompletableFuture<AudioStream> stream = MusicPlayer.openStream(location);
		if (stream != null) {
			cir.setReturnValue(stream);
		}
	}
}
