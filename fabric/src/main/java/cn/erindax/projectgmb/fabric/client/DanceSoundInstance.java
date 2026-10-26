package cn.erindax.projectgmb.fabric.client;

import cn.erindax.projectgmb.client.dance.DanceSound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

public final class DanceSoundInstance extends DanceSound {

	public DanceSoundInstance(Opener opener) {
		super(opener);
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id,
			boolean repeatInstantly) {
		return openStream();
	}
}
