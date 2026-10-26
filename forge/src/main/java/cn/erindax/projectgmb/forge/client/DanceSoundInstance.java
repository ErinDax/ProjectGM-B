package cn.erindax.projectgmb.forge.client;

import cn.erindax.projectgmb.client.dance.DanceSound;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;

import java.util.concurrent.CompletableFuture;

public final class DanceSoundInstance extends DanceSound {
	public DanceSoundInstance(Opener opener) {
		super(opener);
	}

	@Override
	public CompletableFuture<AudioStream> getStream(SoundBufferLibrary soundBuffers, Sound sound, boolean looping) {
		return openStream();
	}
}
