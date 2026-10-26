package cn.erindax.projectgmb.client.dance;

import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class PcmAudioStream implements AudioStream {

	private final short[] samples;
	private final AudioFormat format;
	private int position;

	public PcmAudioStream(short[] samples, int rate) {
		this.samples = samples;
		this.format = new AudioFormat(rate, 16, 1, true, ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
	}

	@Override
	public AudioFormat getFormat() {
		return format;
	}

	@Override
	public ByteBuffer read(int size) {
		int count = Math.max(0, Math.min(size / 2, samples.length - position));
		ByteBuffer buffer = ByteBuffer.allocateDirect(count * 2).order(ByteOrder.nativeOrder());
		for (int i = 0; i < count; i++) {
			buffer.putShort(samples[position++]);
		}
		buffer.flip();
		return buffer;
	}

	@Override
	public void close() {
	}
}
