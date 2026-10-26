package cn.erindax.projectgmb.client.music;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sounds.AudioStream;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Mp3AudioStream implements AudioStream {

	private static final int MAX_FRAME_BYTES = 1152 * 2 * 2;
	private static final int WARMUP_FRAMES = 2;

	private final Bitstream bitstream;
	private final Decoder decoder = new Decoder();
	private final AudioFormat format;
	@Nullable
	private Header pending;
	private boolean finished;

	public Mp3AudioStream(InputStream input) throws IOException {
		this.bitstream = new Bitstream(input);
		this.pending = readHeader();
		if (pending == null) {
			throw new IOException("MP3 stream contains no audio frames");
		}
		int channels = pending.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
		this.format = new AudioFormat(pending.frequency(), 16, channels, true,
			ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
	}

	@Nullable
	private Header readHeader() throws IOException {
		try {
			return bitstream.readFrame();
		} catch (BitstreamException e) {
			throw new IOException(e);
		}
	}

	@Override
	public AudioFormat getFormat() {
		return format;
	}

	public long skip(long bytes) throws IOException {
		long skipped = 0L;
		int frameSize = Math.max(1, format.getFrameSize());
		while (!finished) {
			Header header = pending != null ? pending : readHeader();
			pending = null;
			if (header == null) {
				finished = true;
				break;
			}
			long frameBytes = (long) samplesPerFrame(header) * frameSize;
			if (skipped + frameBytes > bytes) {
				pending = header;
				break;
			}
			if (skipped + frameBytes * (WARMUP_FRAMES + 1) > bytes) {
				try {
					decoder.decodeFrame(header, bitstream);
				} catch (DecoderException e) {
					throw new IOException(e);
				}
			}
			bitstream.closeFrame();
			skipped += frameBytes;
		}
		return skipped;
	}

	private static int samplesPerFrame(Header header) {
		if (header.layer() == 1) {
			return 384;
		}
		return header.layer() == 3 && header.version() != Header.MPEG1 ? 576 : 1152;
	}

	@Override
	public ByteBuffer read(int size) throws IOException {
		ByteBuffer out = ByteBuffer.allocateDirect(size + MAX_FRAME_BYTES).order(ByteOrder.nativeOrder());
		while (!finished && out.position() < size) {
			Header header = pending != null ? pending : readHeader();
			pending = null;
			if (header == null) {
				finished = true;
				break;
			}
			SampleBuffer samples;
			try {
				samples = (SampleBuffer) decoder.decodeFrame(header, bitstream);
			} catch (DecoderException e) {
				throw new IOException(e);
			}
			short[] pcm = samples.getBuffer();
			int length = Math.min(samples.getBufferLength(), out.remaining() / 2);
			for (int i = 0; i < length; i++) {
				out.putShort(pcm[i]);
			}
			bitstream.closeFrame();
		}
		out.flip();
		return out;
	}

	@Override
	public void close() throws IOException {
		try {
			bitstream.close();
		} catch (BitstreamException e) {
			throw new IOException(e);
		}
	}
}
