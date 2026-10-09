package cn.erindax.projectgmb.client.dance;

import cn.erindax.projectgmb.ProjectGmB;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class DanceSound extends AbstractSoundInstance {

	private static final int INITIAL_READS = 4;

	public interface Opener {
		AudioStream open() throws IOException;
	}

	private final Opener opener;
	private volatile long startNanos;

	public DanceSound(Opener opener) {
		super(ProjectGmB.id("dance"), SoundSource.MASTER, SoundInstance.createUnseededRandom());
		this.opener = opener;
		this.volume = 1.0F;
		this.pitch = 1.0F;
		this.looping = false;
		this.relative = true;
		this.attenuation = SoundInstance.Attenuation.NONE;
		this.x = 0.0;
		this.y = 0.0;
		this.z = 0.0;
	}

	public long startNanos() {
		return startNanos;
	}

	@Override
	public WeighedSoundEvents resolve(SoundManager manager) {
		this.sound = new Sound(this.location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE,
			true, false, 16);
		return new WeighedSoundEvents(this.location, null);
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id,
			boolean repeatInstantly) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return new Timed(opener.open());
			} catch (IOException exception) {
				throw new CompletionException(exception);
			}
		}, Util.backgroundExecutor());
	}

	private final class Timed implements AudioStream {
		private final AudioStream inner;
		private int reads;

		private Timed(AudioStream inner) {
			this.inner = inner;
		}

		@Override
		public AudioFormat getFormat() {
			return inner.getFormat();
		}

		@Override
		public ByteBuffer read(int size) throws IOException {
			ByteBuffer buffer = inner.read(size);
			if (reads < INITIAL_READS) {
				reads++;
				if (reads == INITIAL_READS) {
					startNanos = System.nanoTime();
				}
			}
			return buffer;
		}

		@Override
		public void close() throws IOException {
			inner.close();
		}
	}
}
