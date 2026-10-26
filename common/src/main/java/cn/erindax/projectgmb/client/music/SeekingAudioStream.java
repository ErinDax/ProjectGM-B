package cn.erindax.projectgmb.client.music;

import net.minecraft.client.sounds.AudioStream;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class SeekingAudioStream implements AudioStream {

	private static final long MIN_SKIP_MILLIS = 250L;
	private static final int CHUNK_BYTES = 64 * 1024;
	private static final long NANOS_PER_MILLI = 1_000_000L;

	private final AudioStream inner;
	@Nullable
	private ByteBuffer leftover;

	private SeekingAudioStream(AudioStream inner, ByteBuffer leftover) {
		this.inner = inner;
		this.leftover = leftover;
	}

	public static AudioStream seek(AudioStream stream, long offsetMillis, long offsetAt) throws IOException {
		if (target(offsetMillis, offsetAt) < MIN_SKIP_MILLIS) {
			return stream;
		}
		AudioFormat format = stream.getFormat();
		int frameSize = format.getFrameSize() > 0 ? format.getFrameSize() : Math.max(1, format.getChannels()) * 2;
		double bytesPerMilli = format.getSampleRate() * frameSize / 1000.0;
		long skipped = 0L;
		if (stream instanceof Mp3AudioStream mp3) {
			skipped = mp3.skip(goal(offsetMillis, offsetAt, bytesPerMilli, frameSize));
		}
		while (true) {
			long goal = goal(offsetMillis, offsetAt, bytesPerMilli, frameSize);
			if (skipped >= goal) {
				return stream;
			}
			ByteBuffer buffer = stream.read(CHUNK_BYTES);
			if (buffer == null || !buffer.hasRemaining()) {
				return stream;
			}
			long need = goal - skipped;
			if (buffer.remaining() > need) {
				buffer.position(buffer.position() + (int) need);
				return new SeekingAudioStream(stream, buffer);
			}
			skipped += buffer.remaining();
		}
	}

	private static long target(long offsetMillis, long offsetAt) {
		return offsetMillis + Math.max(0L, System.nanoTime() - offsetAt) / NANOS_PER_MILLI;
	}

	private static long goal(long offsetMillis, long offsetAt, double bytesPerMilli, int frameSize) {
		long bytes = (long) (target(offsetMillis, offsetAt) * bytesPerMilli);
		return bytes - bytes % frameSize;
	}

	@Override
	public AudioFormat getFormat() {
		return inner.getFormat();
	}

	@Override
	public ByteBuffer read(int size) throws IOException {
		ByteBuffer buffer = leftover;
		if (buffer != null) {
			leftover = null;
			if (buffer.hasRemaining()) {
				return buffer;
			}
		}
		return inner.read(size);
	}

	@Override
	public void close() throws IOException {
		inner.close();
	}
}
